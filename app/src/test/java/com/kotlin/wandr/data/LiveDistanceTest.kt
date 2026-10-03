package com.kotlin.wandr.data

import com.kotlin.wandr.domain.model.GeoPoint
import com.kotlin.wandr.domain.model.LiveDistance
import com.kotlin.wandr.domain.model.distanceMetersTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Context-aware: distance from the GPS to the quest's place, calculated on the phone. */
class LiveDistanceTest {

    private val plazaBolivar = GeoPoint(4.5981, -74.0760)
    private val museoDelOro = GeoPoint(4.6018, -74.0720)

    @Test
    fun `haversine gives the real distance between two places in Bogota`() {
        // Plaza de Bolivar → Museo del Oro is about 600 m in a straight line
        val meters = plazaBolivar.distanceMetersTo(museoDelOro)
        assertEquals(605.0, meters, 25.0)
        assertEquals(0.0, plazaBolivar.distanceMetersTo(plazaBolivar), 0.001)
    }

    @Test
    fun `labels round the distance and announce the arrival`() {
        assertEquals("350 m away", LiveDistance(347.0).label)
        assertEquals("2.4 km away", LiveDistance(2_430.0).label)
        assertEquals("You're here!", LiveDistance(60.0).label)
        assertEquals("350 m away (approx.)", LiveDistance(347.0, isApproximate = true).label)
    }

    @Test
    fun `arriving means being within 100 meters`() {
        assertTrue(LiveDistance(LiveDistance.ARRIVAL_RADIUS_METERS).hasArrived)
        assertFalse(LiveDistance(LiveDistance.ARRIVAL_RADIUS_METERS + 1).hasArrived)
    }
}
