package com.glassesscan.app.util

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.LocaleList
import java.util.*

object LocaleUtils {
    fun wrap(context: Context, languageCode: String): ContextWrapper {
        val locale = if (languageCode == "zh") Locale.CHINESE else Locale(languageCode)
        Locale.setDefault(locale)
        
        val configuration = context.resources.configuration
        val newContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            val localeList = LocaleList(locale)
            configuration.setLocales(localeList)
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
        
        return ContextWrapper(newContext)
    }
}
