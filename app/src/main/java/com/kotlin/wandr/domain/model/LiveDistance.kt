package com.kotlin.wandr.domain.model

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

/**
 * Straight-line distance to [other] in meters (haversine formula). Computed on the phone:
 * no maps service is called.
 */
fun GeoPoint.distanceMetersTo(other: GeoPoint): Double {
    val lat1 = Math.toRadians(latitude)
    val lat2 = Math.toRadians(other.latitude)
    val dLat = lat2 - lat1
    val dLng = Math.toRadians(other.longitude - longitude)
    val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
}

/**
 * Context-aware: how far the user is from the quest's place right now, from the phone's GPS.
 * [isApproximate] is true when the fix is the last one the device remembers, not a fresh one.
 */
data class LiveDistance(val meters: Double, val isApproximate: Boolean = false) {

    val hasArrived: Boolean get() = meters <= ARRIVAL_RADIUS_METERS

    /** "You're here!", "350 m away", "2.4 km away". */
    val label: String
        get() = when {
            hasArrived -> "You're here!"
            meters < 1_000 -> "${(meters / 10).roundToInt() * 10} m away"
            else -> String.format(Locale.US, "%.1f km away", meters / 1_000)
        } + if (isApproximate && !hasArrived) " (approx.)" else ""

    companion object {
        /** Close enough to the place to say the user arrived. */
        const val ARRIVAL_RADIUS_METERS = 100.0
    }
}
