package com.kotlin.wandr.domain.model

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

fun GeoPoint.distanceMetersTo(other: GeoPoint): Double {
    val lat1 = Math.toRadians(latitude)
    val lat2 = Math.toRadians(other.latitude)
    val dLat = lat2 - lat1
    val dLng = Math.toRadians(other.longitude - longitude)
    val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
}
data class LiveDistance(val meters: Double, val isApproximate: Boolean = false) {

    val hasArrived: Boolean get() = meters <= ARRIVAL_RADIUS_METERS

    val label: String
        get() = when {
            hasArrived -> "You're here!"
            meters < 1_000 -> "${(meters / 10).roundToInt() * 10} m away"
            else -> String.format(Locale.US, "%.1f km away", meters / 1_000)
        } + if (isApproximate && !hasArrived) " (approx.)" else ""

    companion object {
        const val ARRIVAL_RADIUS_METERS = 100.0
    }
}
