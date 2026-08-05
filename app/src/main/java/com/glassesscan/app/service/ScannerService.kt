package com.glassesscan.app.service

import android.app.*
import android.content.Context
import android.content.Intent
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
import com.glassesscan.app.data.settings.SettingsManager
import com.glassesscan.app.util.LocationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch

class ScannerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var bleScannerHelper: BleScannerHelper
    private lateinit var repository: ScannerRepositoryImpl
    private lateinit var historyManager: HistoryManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var locationHelper: LocationHelper
    private lateinit var vibrator: Vibrator

    override fun onCreate() {
        super.onCreate()
        bleScannerHelper = BleScannerHelper(this)
        repository = ScannerRepositoryImpl(bleScannerHelper)
        historyManager = HistoryManager(this)
        settingsManager = SettingsManager(this)
        locationHelper = LocationHelper(this)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification("Monitoring for Smart Glasses...")
        startForeground(1, notification)

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
                    val metaGlasses = devices.filter { it.isMetaGlasses }
                    if (metaGlasses.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    val shouldVibrate = settingsManager.vibrateOnDetect.first()
                    val isZoneMode = settingsManager.zoneSecurityMode.first()

                    metaGlasses.forEach { metaDevice ->
                        if (canSaveToHistory(metaDevice.address, now)) {
                            serviceScope.launch {
                                try {
                                    val location = locationHelper.getLastLocation()
                                    val deviceWithLoc = metaDevice.copy(
                                        latitude = location?.latitude,
                                        longitude = location?.longitude,
                                        isZoneAlert = isZoneMode
                                    )
                                    historyManager.saveDetection(deviceWithLoc)
                                    lastSavedMap[metaDevice.address] = now

                                    if (shouldVibrate) {
                                        triggerVibration()
                                    }
                                    
                                    val title = if (isZoneMode) "CRITICAL: Zone Security Breach" else "Meta Glasses Detected!"
                                    showDetectionNotification(title, "A pair of smart glasses is very close to you.")
                                } catch (e: Exception) {}
                            }
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
            val channel = NotificationChannel(
                "scanner_channel",
                "Scanner Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
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
        val notification = NotificationCompat.Builder(this, "scanner_channel")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(2, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
