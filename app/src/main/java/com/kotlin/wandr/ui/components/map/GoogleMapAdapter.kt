package com.kotlin.wandr.ui.components.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.kotlin.wandr.domain.model.GeoPoint
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log2

/** [WandrMap] drawn with Google Maps. The only file that knows the Google Maps SDK. */
@Singleton
class GoogleMapAdapter @Inject constructor() : WandrMap {

    @Composable
    override fun Content(
        center: GeoPoint,
        radiusKm: Double,
        markers: List<MapMarker>,
        showUserLocation: Boolean,
        onMarkerClick: (MapMarker) -> Unit,
        modifier: Modifier,
    ) {
        val target = center.toLatLng()
        val zoom = zoomFor(radiusKm)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(target, zoom)
        }
        // Follow the user and the radius filter
        LaunchedEffect(target, zoom) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, zoom))
        }
        val radiusColor = MaterialTheme.colorScheme.primary

        GoogleMap(modifier = modifier, cameraPositionState = cameraPositionState) {
            Circle(
                center = target,
                radius = radiusKm * 1000, // meters
                fillColor = radiusColor.copy(alpha = 0.08f),
                strokeColor = radiusColor,
                strokeWidth = 2f,
            )
            if (showUserLocation) UserDot(target)
            markers.forEach { marker ->
                key(marker.id) {
                    Marker(
                        state = rememberUpdatedMarkerState(position = marker.position.toLatLng()),
                        title = marker.title,
                        snippet = marker.subtitle,
                        onClick = {
                            onMarkerClick(marker)
                            false // Not consumed: the map still shows the title bubble
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun UserDot(position: LatLng) {
        MarkerComposable(
            state = rememberUpdatedMarkerState(position = position),
            anchor = Offset(0.5f, 0.5f),
            title = "You are here",
        ) {
            Box(
                Modifier
                    .size(18.dp)
                    .background(Color.White, CircleShape)
                    .padding(3.dp)
                    .background(UserDotBlue, CircleShape)
            )
        }
    }

    private fun GeoPoint.toLatLng() = LatLng(latitude, longitude)

    /** Zoom that fits the whole radius on a phone: 1 km ≈ 14.3, and one level less each time it doubles. */
    private fun zoomFor(radiusKm: Double): Float = (14.3 - log2(radiusKm)).toFloat()

    private companion object {
        val UserDotBlue = Color(0xFF1A73E8)
    }
}
