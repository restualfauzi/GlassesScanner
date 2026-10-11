package com.glassesscan.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocationHelper(context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Location? = withContext(Dispatchers.IO) {
        try {
            Tasks.await(fusedLocationClient.lastLocation)
        } catch (_: Exception) {
            null
        } catch (_: Throwable) {
            null
        }
    }
}
