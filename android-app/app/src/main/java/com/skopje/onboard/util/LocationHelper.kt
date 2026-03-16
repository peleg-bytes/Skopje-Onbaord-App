package com.skopje.onboard.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class LocationResult(val lat: Double?, val lng: Double?, val status: GpsStatus)

enum class GpsStatus { ACQUIRING, LOCKED, UNAVAILABLE }

class LocationHelper(private val context: Context) {
    private val client: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getLocation(): LocationResult {
        if (!hasPermission()) return LocationResult(null, null, GpsStatus.UNAVAILABLE)
        return suspendCancellableCoroutine { cont ->
            val token = CancellationTokenSource()
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        cont.resume(LocationResult(loc.latitude, loc.longitude, GpsStatus.LOCKED))
                    } else {
                        cont.resume(LocationResult(null, null, GpsStatus.ACQUIRING))
                    }
                }
                .addOnFailureListener {
                    cont.resume(LocationResult(null, null, GpsStatus.UNAVAILABLE))
                }
        }
    }
}
