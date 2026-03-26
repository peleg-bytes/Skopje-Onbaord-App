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
import kotlinx.coroutines.withTimeoutOrNull
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
        return withTimeoutOrNull(7000L) { getLocationInner() }
            ?: LocationResult(null, null, GpsStatus.UNAVAILABLE)
    }

    private suspend fun getLocationInner(): LocationResult {
        val fresh = withTimeoutOrNull(4500L) {
            suspendCancellableCoroutine<LocationResult> { cont ->
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
        if (fresh != null && fresh.lat != null && fresh.lng != null) return fresh
        return withTimeoutOrNull(2500L) {
            suspendCancellableCoroutine { cont ->
                client.lastLocation
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
        } ?: LocationResult(null, null, GpsStatus.UNAVAILABLE)
    }

    /**
     * Cheaper refresh for background ticks: cached location first, then a short balanced-accuracy fix.
     * Bounded time so counting UI and submit are never blocked for long.
     */
    suspend fun getLocationLight(): LocationResult {
        if (!hasPermission()) return LocationResult(null, null, GpsStatus.UNAVAILABLE)
        val cached = withTimeoutOrNull(1200L) {
            suspendCancellableCoroutine<LocationResult?> { cont ->
                client.lastLocation
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            cont.resume(LocationResult(loc.latitude, loc.longitude, GpsStatus.LOCKED))
                        } else {
                            cont.resume(null)
                        }
                    }
                    .addOnFailureListener { cont.resume(null) }
            }
        }
        if (cached != null && cached.lat != null && cached.lng != null) return cached
        return withTimeoutOrNull(4500L) {
            suspendCancellableCoroutine { cont ->
                val token = CancellationTokenSource()
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token)
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
        } ?: LocationResult(null, null, GpsStatus.UNAVAILABLE)
    }
}
