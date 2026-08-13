package com.glassesscan.app.service

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.glassesscan.app.R

class GlassesScannerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, "SCANNER IDLE", "Safe Environment")
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, status: String, detail: String) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setTextViewText(R.id.widget_status, status)
            views.setTextViewText(R.id.widget_detail, detail)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        fun updateAllWidgets(context: Context, status: String, detail: String) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, GlassesScannerWidget::class.java)
            )
            for (id in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id, status, detail)
            }
        }
    }
}
