package com.glassesscan.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
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
        
        return LocalizedContextWrapper(base = context, localizedContext = newContext)
    }

    private class LocalizedContextWrapper(
        base: Context,
        private val localizedContext: Context
    ) : ContextWrapper(base) {
        override fun getResources(): Resources = localizedContext.resources
        override fun getAssets(): AssetManager = localizedContext.assets
    }
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
