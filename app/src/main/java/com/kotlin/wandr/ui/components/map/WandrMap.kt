package com.kotlin.wandr.ui.components.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kotlin.wandr.domain.model.GeoPoint

data class MapMarker(
    val id: String,
    val position: GeoPoint,
    val title: String,
    val subtitle: String? = null,
)

interface WandrMap {
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
