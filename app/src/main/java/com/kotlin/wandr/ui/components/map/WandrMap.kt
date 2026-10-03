package com.kotlin.wandr.ui.components.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kotlin.wandr.domain.model.GeoPoint

/** A pin on the map, in the app's own terms (no map SDK types). */
data class MapMarker(
    val id: String,
    val position: GeoPoint,
    val title: String,
    val subtitle: String? = null,
)

/**
 * Adapter pattern: the map the screens expect. They only know this interface, [GeoPoint] and
 * [MapMarker]; [GoogleMapAdapter] translates them to the Google Maps SDK. Changing the map
 * provider means writing another adapter, not touching the screens.
 */
interface WandrMap {
    /**
     * Draws the map around [center] with a circle of [radiusKm] and one pin per marker.
     * [showUserLocation] draws the "you are here" dot on [center].
     */
    @Composable
    fun Content(
        center: GeoPoint,
        radiusKm: Double,
        markers: List<MapMarker>,
        showUserLocation: Boolean,
        onMarkerClick: (MapMarker) -> Unit,
        modifier: Modifier,
    )
}
