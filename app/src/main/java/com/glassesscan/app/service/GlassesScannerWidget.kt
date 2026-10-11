package com.glassesscan.app.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.glassesscan.app.MainActivity
import com.glassesscan.app.R

class GlassesScannerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, "SECURE (100%)", "No recording devices nearby")
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, status: String, detail: String) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setTextViewText(R.id.widget_status, "Safety Score: $status")
            views.setTextViewText(R.id.widget_detail, detail)

            // Dynamic colors based on threat level
            if (status.contains("THREAT")) {
                views.setTextColor(R.id.widget_status, android.graphics.Color.parseColor("#FF3B30"))
            } else if (status.contains("CAUTION")) {
                views.setTextColor(R.id.widget_status, android.graphics.Color.parseColor("#FFCC00"))
            } else {
                views.setTextColor(R.id.widget_status, android.graphics.Color.parseColor("#34C759"))
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        fun updateAllWidgets(context: Context, status: String, detail: String) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, GlassesScannerWidget::class.java)
            )
            for (id in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id, status, detail)
            }
        }
    }
}
