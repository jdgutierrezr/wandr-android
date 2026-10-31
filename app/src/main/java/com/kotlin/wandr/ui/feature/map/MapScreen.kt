package com.kotlin.wandr.ui.feature.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlin.wandr.R
import com.kotlin.wandr.core.location.LocationProvider
import com.kotlin.wandr.core.location.UserLocation
import com.kotlin.wandr.domain.model.GeoPoint
import com.kotlin.wandr.domain.model.Quest
import com.kotlin.wandr.ui.components.QuestRow
import com.kotlin.wandr.ui.components.WandrChip
import com.kotlin.wandr.ui.components.MainTab
import com.kotlin.wandr.ui.components.WandrBottomBar
import com.kotlin.wandr.ui.components.WandrTopBar
import com.kotlin.wandr.ui.feature.quest.QuestDetailSheet
import com.kotlin.wandr.ui.components.map.MapMarker
import com.kotlin.wandr.ui.components.map.WandrMap
import com.kotlin.wandr.ui.theme.WandrTheme
import kotlinx.coroutines.launch
import java.util.Locale

private val RADIUS_OPTIONS_KM = listOf(1.0, 3.0, 5.0, 10.0)

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@Composable
fun MapRoute(
    map: WandrMap,
    onSelectTab: (MainTab) -> Unit,
    onOpenTracker: (questId: String) -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) viewModel.refresh()
    }
    LaunchedEffect(Unit) {
        val hasPermission = LOCATION_PERMISSIONS.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermission) permissionLauncher.launch(LOCATION_PERMISSIONS)
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    var openQuestId by rememberSaveable { mutableStateOf<String?>(null) }

    MapScreen(
        state = state,
        map = map,
        snackbarHostState = snackbarHostState,
        onRadiusChange = viewModel::setRadius,
        onSelectTab = onSelectTab,
        onOpenQuest = { openQuestId = it },
    )

    openQuestId?.let { id ->
        QuestDetailSheet(
            questId = id,
            onDismiss = { openQuestId = null },
            onOpenTracker = onOpenTracker,
        )
    }
}

@Composable
fun MapScreen(
    state: MapUiState,
    map: WandrMap,
    snackbarHostState: SnackbarHostState,
    onRadiusChange: (Double) -> Unit,
    onSelectTab: (MainTab) -> Unit,
    onOpenQuest: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val spacing = WandrTheme.spacing

    Scaffold(
        topBar = { WandrTopBar(title = "Discovery Map") },
        bottomBar = { WandrBottomBar(selected = MainTab.MAP, onSelect = onSelectTab) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.padding(horizontal = spacing.screen, vertical = spacing.sm),
            ) {
                RADIUS_OPTIONS_KM.forEach { km ->
                    WandrChip(
                        label = "${km.toInt()} km",
                        selected = km == state.radiusKm,
                        onClick = { onRadiusChange(km) },
                    )
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                state.userLocation?.let { location ->
                    map.Content(
                        center = location.point,
                        radiusKm = state.radiusKm,
                        markers = state.markers,
                        showUserLocation = location.source != UserLocation.Source.FALLBACK,
                        onMarkerClick = { marker ->
                            val index = state.quests.indexOfFirst { it.id == marker.id }
                            if (index >= 0) scope.launch { listState.animateScrollToItem(index) }
                            onOpenQuest(marker.id)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (state.isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            if (state.isShowingSavedData) Notice("No internet connection. Showing saved quests")
            when (state.userLocation?.source) {
                UserLocation.Source.FALLBACK -> Notice("Location unavailable. Showing quests around the center of Bogotá")
                UserLocation.Source.LAST_KNOWN -> Notice("Using your last known location")
                else -> Unit
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = spacing.screen, vertical = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                modifier = Modifier.weight(1f),
            ) {
                items(state.quests, key = { it.id }) { quest ->
                    QuestRow(
                        title = quest.title,
                        subtitle = listOfNotNull(quest.placeName, quest.distanceKm?.let(::formatDistance)).joinToString(" · "),
                        imageUrl = quest.coverImageUrl,
                        xp = quest.pointsReward,
                        onClick = { onOpenQuest(quest.id) },
                    )
                }
                if (state.quests.isEmpty() && !state.isLoading) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.sm),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.wandr_bear_map),
                                contentDescription = null,
                                modifier = Modifier.height(160.dp),
                            )
                            Text(
                                "No quests within ${state.radiusKm.toInt()} km. Try a bigger radius.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WandrTheme.colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Notice(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = WandrTheme.colors.onLocked,
        modifier = Modifier
            .fillMaxWidth()
            .background(WandrTheme.colors.locked)
            .padding(horizontal = WandrTheme.spacing.screen, vertical = WandrTheme.spacing.sm),
    )
}

private fun formatDistance(km: Double): String = String.format(Locale.US, "%.1f km", km)

private object PreviewMap : WandrMap {
    @Composable
    override fun Content(
        center: GeoPoint,
        radiusKm: Double,
        markers: List<MapMarker>,
        showUserLocation: Boolean,
        onMarkerClick: (MapMarker) -> Unit,
        modifier: Modifier,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.background(WandrTheme.colors.successContainer)) {
            Text("Map · ${markers.size} pins")
        }
    }
}

private fun previewQuest(id: String, title: String, placeName: String, distanceKm: Double) = Quest(
    id = id,
    title = title,
    description = null,
    difficultyLevel = 2,
    estimatedDurationMin = 45,
    pointsReward = 40,
    coverImageUrl = null,
    placeId = "place-$id",
    placeName = placeName,
    tags = emptyList(),
    distanceKm = distanceKm,
)

@Preview(showBackground = true, name = "Offline, no permission")
@Composable
private fun MapScreenPreview() {
    WandrTheme {
        MapScreen(
            state = MapUiState(
                isLoading = false,
                userLocation = UserLocation(LocationProvider.BOGOTA_CENTER, UserLocation.Source.FALLBACK),
                quests = listOf(
                    previewQuest("q1", "Discover pre-Columbian gold", "Museo del Oro", 1.4),
                    previewQuest("q2", "Climb Monserrate", "Cerro de Monserrate", 2.9),
                ),
                isShowingSavedData = true,
            ),
            map = PreviewMap,
            snackbarHostState = SnackbarHostState(),
            onRadiusChange = {},
            onSelectTab = {},
            onOpenQuest = {},
        )
    }
}
