package com.glassesscan.app

import android.app.Activity
import android.app.Application
import com.glassesscan.app.util.AdMobHelper
import com.google.firebase.FirebaseApp

class GlassesScanApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        AdMobHelper.initialize(this)
    }

    fun showAppOpenAd(activity: Activity) {
        AdMobHelper.showAppOpenAd(activity)
    }
}
