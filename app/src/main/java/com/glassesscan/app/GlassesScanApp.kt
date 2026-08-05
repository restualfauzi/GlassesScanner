package com.glassesscan.app

import android.app.Application
import com.glassesscan.app.util.AdMobHelper
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.appopen.AppOpenAd

class GlassesScanApp : Application() {
    private var appOpenAd: AppOpenAd? = null

    override fun onCreate() {
        super.onCreate()
        AdMobHelper.initialize(this)
        loadAppOpenAd()
    }

    private fun loadAppOpenAd() {
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            this,
            "ca-app-pub-1211461896441011/8401524721",
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                }
            }
        )
    }

    fun showAppOpenAd(activity: android.app.Activity) {
        appOpenAd?.show(activity)
        loadAppOpenAd() // Load next one
    }
}
