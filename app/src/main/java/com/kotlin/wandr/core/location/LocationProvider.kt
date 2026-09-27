package com.kotlin.wandr.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kotlin.wandr.domain.model.GeoPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Where the user is, and how sure we are about it. */
data class UserLocation(
    val point: GeoPoint,
    val source: Source,
) {
    enum class Source {
        /** Fresh GPS / network fix. */
        CURRENT,

        /** The last fix the device remembers (QS11: GPS lost). */
        LAST_KNOWN,

        /** No permission or no fix at all: center of Bogota. */
        FALLBACK,
    }
}

interface LocationProvider {
    fun hasPermission(): Boolean
    suspend fun currentLocation(): UserLocation

    /** Updates only after moving [minDistanceMeters] (QS5: save battery). */
    fun locationUpdates(minDistanceMeters: Float = 50f, intervalMs: Long = 30_000): Flow<UserLocation>

    companion object {
        val BOGOTA_CENTER = GeoPoint(latitude = 4.6097, longitude = -74.0817)
    }
}

@Singleton
class FusedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationProvider {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    override fun hasPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): UserLocation {
        if (!hasPermission()) return fallback()
        val current = runCatching {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
        }.getOrNull()
        if (current != null) {
            return UserLocation(GeoPoint(current.latitude, current.longitude), UserLocation.Source.CURRENT)
        }
        val last = runCatching { client.lastLocation.await() }.getOrNull()
        if (last != null) {
            return UserLocation(GeoPoint(last.latitude, last.longitude), UserLocation.Source.LAST_KNOWN)
        }
        return fallback()
    }

    @SuppressLint("MissingPermission")
    override fun locationUpdates(minDistanceMeters: Float, intervalMs: Long): Flow<UserLocation> {
        if (!hasPermission()) return flowOf(fallback())
        return callbackFlow {
            val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
                .setMinUpdateDistanceMeters(minDistanceMeters)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let {
                        trySend(UserLocation(GeoPoint(it.latitude, it.longitude), UserLocation.Source.CURRENT))
                    }
                }
            }
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            awaitClose { client.removeLocationUpdates(callback) }
        }
    }

    private fun fallback() = UserLocation(LocationProvider.BOGOTA_CENTER, UserLocation.Source.FALLBACK)
}
