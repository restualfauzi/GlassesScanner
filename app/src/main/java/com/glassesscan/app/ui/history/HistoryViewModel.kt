package com.glassesscan.app.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.glassesscan.app.data.history.HistoryManager
import com.glassesscan.app.data.settings.SettingsManager
import com.glassesscan.app.domain.model.DetectedDevice
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val historyManager: HistoryManager,
    private val settingsManager: SettingsManager
) : ViewModel() {
    val history: StateFlow<List<DetectedDevice>> = historyManager.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun clearHistory() {
        viewModelScope.launch {
            historyManager.clearHistory()
        }
    }

    fun updateDeviceTag(address: String, tag: String) {
        viewModelScope.launch {
            settingsManager.setDeviceTag(address, tag)
            historyManager.updateDeviceTag(address, tag)
        }
    }

    fun exportHistoryToPdf(context: android.content.Context) {
        val currentHistory = history.value
        if (currentHistory.isEmpty()) return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val fileName = "Privacy_Audit_Report_${System.currentTimeMillis()}.pdf"
            val file = java.io.File(context.cacheDir, fileName)

            try {
                val pdfDocument = android.graphics.pdf.PdfDocument()
                var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
                var page = pdfDocument.startPage(pageInfo)
                var canvas = page.canvas
                val paint = android.graphics.Paint()

                // Header
                paint.color = android.graphics.Color.BLUE
                paint.textSize = 24f
                paint.isFakeBoldText = true
                canvas.drawText("Glasses Scanner - Privacy Audit Report", 50f, 50f, paint)

                paint.color = android.graphics.Color.BLACK
                paint.textSize = 14f
                paint.isFakeBoldText = false
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                canvas.drawText("Generated on: $dateStr", 50f, 80f, paint)
                
                canvas.drawLine(50f, 100f, 545f, 100f, paint)

                // Table Header
                paint.textSize = 12f
                paint.isFakeBoldText = true
                var yPos = 130f
                canvas.drawText("Time", 50f, yPos, paint)
                canvas.drawText("Device Name / MAC", 150f, yPos, paint)
                canvas.drawText("Signal", 400f, yPos, paint)
                canvas.drawText("Threat", 470f, yPos, paint)

                canvas.drawLine(50f, 140f, 545f, 140f, paint)
                yPos = 160f

                paint.isFakeBoldText = false
                currentHistory.forEach { device ->
                    if (yPos > 800f) { // Page break
                        pdfDocument.finishPage(page)
                        pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        yPos = 50f
                    }

                    val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(device.timestamp))
                    val threatLevel = if (device.isMetaGlasses) "HIGH" else "LOW"
                    val deviceText = (device.name ?: "Unknown") + " (" + device.address + ")"
                    
                    canvas.drawText(time, 50f, yPos, paint)
                    canvas.drawText(deviceText.take(40), 150f, yPos, paint)
                    canvas.drawText("${device.rssi} dBm", 400f, yPos, paint)
                    
                    if (device.isMetaGlasses) {
                        paint.color = android.graphics.Color.RED
                        paint.isFakeBoldText = true
                    }
                    canvas.drawText(threatLevel, 470f, yPos, paint)
                    
                    paint.color = android.graphics.Color.BLACK
                    paint.isFakeBoldText = false
                    
                    yPos += 25f
                }

                pdfDocument.finishPage(page)
                
                val outStream = java.io.FileOutputStream(file)
                pdfDocument.writeTo(outStream)
                pdfDocument.close()
                outStream.close()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Privacy Audit Report")
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    context.startActivity(android.content.Intent.createChooser(intent, "Export Audit Report"))
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

class HistoryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            val historyManager = HistoryManager(context.applicationContext)
            val settingsManager = SettingsManager(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(historyManager, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
