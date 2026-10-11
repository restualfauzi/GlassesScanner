package com.glassesscan.app.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.glassesscan.app.MainActivity
import com.glassesscan.app.R
import com.glassesscan.app.data.history.HistoryManager
import com.glassesscan.app.data.repository.ScannerRepositoryImpl
import com.glassesscan.app.data.scanner.BleScannerHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

import android.os.Vibrator
import android.os.VibrationEffect
import com.glassesscan.app.data.scanner.WifiScannerHelper
import com.glassesscan.app.data.settings.SettingsManager
import com.glassesscan.app.util.LocationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch

class ScannerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var bleScannerHelper: BleScannerHelper
    private lateinit var wifiScannerHelper: WifiScannerHelper
    private lateinit var repository: ScannerRepositoryImpl
    private lateinit var historyManager: HistoryManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var locationHelper: LocationHelper
    private lateinit var vibrator: Vibrator

    override fun onCreate() {
        super.onCreate()
        bleScannerHelper = BleScannerHelper(this)
        wifiScannerHelper = WifiScannerHelper(this)
        repository = ScannerRepositoryImpl(bleScannerHelper, wifiScannerHelper)
        historyManager = HistoryManager(this)
        settingsManager = SettingsManager(this)
        locationHelper = LocationHelper(this)
        vibrator = getSystemService(Vibrator::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification("Monitoring for Smart Glasses...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, notification)
        }

        startBackgroundScanning()

        return START_STICKY
    }

    private fun startBackgroundScanning() {
        serviceScope.launch {
            repository.scanDevices()
                .catch { e ->
                    // Log error and stop foreground if necessary, but don't crash
                    println("Background scan error: ${e.message}")
                }
                .collect { devices ->
                    val now = System.currentTimeMillis()
                    val shouldVibrate = settingsManager.vibrateOnDetect.first()
                    val shouldAutoSave = settingsManager.autoSaveHistory.first()
                    val isZoneMode = settingsManager.zoneSecurityMode.first()
                    val ignoredDevices = settingsManager.ignoredDevices.first()

                    devices.forEach { device ->
                        // Determine if we should alert for this device
                        val isIgnored = ignoredDevices.contains(device.address)
                        val isThreat = (device.isMetaGlasses || isZoneMode) && !isIgnored
                        
                        if (isThreat && canSaveToHistory(device.address, now)) {
                            lastSavedMap[device.address] = now
                            
                            serviceScope.launch {
                                try {
                                    val location = locationHelper.getLastLocation()
                                    val deviceWithLoc = device.copy(
                                        latitude = location?.latitude,
                                        longitude = location?.longitude,
                                        isZoneAlert = isZoneMode && device.isMetaGlasses
                                    )
                                    if (shouldAutoSave) {
                                        historyManager.saveDetection(deviceWithLoc)
                                    }

                                    if (shouldVibrate) {
                                        triggerVibration()
                                    }
                                    
                                    val deviceName = device.customTag ?: device.name ?: "Camera / Smart Glasses"
                                    val title = when {
                                        device.isHighBandwidth -> "RED ALERT: Active Video Streaming Detected"
                                        isZoneMode && !device.isMetaGlasses -> "Device Detected in Zone"
                                        isZoneMode -> "CRITICAL: Zone Security Breach"
                                        else -> "ALERT: $deviceName Detected!"
                                    }
                                    
                                    val message = if (device.isHighBandwidth) {
                                        "$deviceName is actively using high-bandwidth Wi-Fi. It might be streaming video right now!"
                                    } else {
                                        "$deviceName was identified nearby. Signal strength: ${device.rssi} dBm."
                                    }
                                    
                                    showDetectionNotification(title, message)
                                    sendWearOSAlert(title, message)
                                } catch (e: Exception) {}
                            }
                        }
                    }
                }
        }
    }

    private val lastSavedMap = mutableMapOf<String, Long>()
    private fun canSaveToHistory(address: String, now: Long): Boolean {
        val lastSaved = lastSavedMap[address] ?: 0L
        return now - lastSaved > 30000 // 30 seconds gap
    }

    private fun triggerVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Silent Foreground Service Channel
            val serviceChannel = NotificationChannel(
                "scanner_channel",
                "Scanner Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(serviceChannel)

            // High Priority Pop-Up / Heads-Up Banner Channel
            val alertChannel = NotificationChannel(
                "threat_alert_channel",
                "Camera Glasses Threat Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pop-up banner alerts at the top of screen when smart glasses are detected"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun sendWearOSAlert(title: String, message: String) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val messageClient = com.google.android.gms.wearable.Wearable.getMessageClient(this@ScannerService)
                val nodeClient = com.google.android.gms.wearable.Wearable.getNodeClient(this@ScannerService)
                
                nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                    val payload = "$title|$message".toByteArray(Charsets.UTF_8)
                    for (node in nodes) {
                        messageClient.sendMessage(node.id, "/glasses_alert", payload)
                    }
                }
            } catch (e: Exception) {
                // Wear OS not connected or unavailable
            }
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, ScannerService::class.java).apply { action = "STOP_SERVICE" }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "scanner_channel")
            .setContentTitle("Glasses Scanner Active")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Placeholder
            .setContentIntent(pendingIntent)
            .addAction(0, "STOP", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showDetectionNotification(title: String, message: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "threat_alert_channel")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(Notification.DEFAULT_ALL)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setFullScreenIntent(openAppPendingIntent, false) // Triggers Heads-Up Pop-Up Banner at top of screen
            .addAction(0, "OPEN RADAR", openAppPendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        val notificationId = (System.currentTimeMillis() % 10000).toInt()
        manager.notify(notificationId, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
