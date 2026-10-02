package com.kotlin.wandr.ui.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.location.LocationProvider
import com.kotlin.wandr.core.location.UserLocation
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.data.repository.PlaceRepository
import com.kotlin.wandr.data.repository.QuestRepository
import com.kotlin.wandr.domain.model.Place
import com.kotlin.wandr.domain.model.Quest
import com.kotlin.wandr.ui.components.map.MapMarker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class MapUiState(
    val isLoading: Boolean = true,
    val userLocation: UserLocation? = null,
    /** Closest first, each with its distance. */
    val quests: List<Quest> = emptyList(),
    /** One pin per quest. */
    val markers: List<MapMarker> = emptyList(),
    val radiusKm: Double = QuestRepository.DEFAULT_RADIUS_KM,
    /** Offline: show "No internet connection. Showing saved quests" (QS1). */
    val isShowingSavedData: Boolean = false,
    val errorMessage: String? = null,
)

/** Map: quests around the user, filtered by radius. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val questRepository: QuestRepository,
    private val placeRepository: PlaceRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private data class Query(val radiusKm: Double, val tick: Int)

    private val query = MutableStateFlow(Query(QuestRepository.DEFAULT_RADIUS_KM, 0))

    init {
        query
            .flatMapLatest { q ->
                _uiState.update { it.copy(isLoading = true) }
                val location = locationProvider.currentLocation()
                _uiState.update { it.copy(userLocation = location) }
                // The places are only needed for their coordinates, so a failure there just means no pins
                combine(
                    questRepository.nearbyQuests(location.point, q.radiusKm),
                    placeRepository.nearbyPlaces(location.point, q.radiusKm),
                ) { quests, places -> quests to (places as? Resource.Success)?.data.orEmpty() }
            }
            .onEach { (quests, places) ->
                _uiState.update { state ->
                    when (quests) {
                        Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> state.copy(
                            isLoading = false,
                            quests = quests.data,
                            markers = markersFor(quests.data, places),
                            isShowingSavedData = quests.isStale,
                        )
                        is Resource.Error -> state.copy(isLoading = false, errorMessage = quests.error.message)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun setRadius(radiusKm: Double) {
        _uiState.update { it.copy(radiusKm = radiusKm) }
        query.update { it.copy(radiusKm = radiusKm) }
    }

    /** Also re-reads the location (e.g. after the user grants the permission). */
    fun refresh() = query.update { it.copy(tick = it.tick + 1) }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    /** `nearby_quests` does not return coordinates, so each quest is pinned on its place. */
    private fun markersFor(quests: List<Quest>, places: List<Place>): List<MapMarker> {
        val placesById = places.associateBy { it.id }
        return quests.mapNotNull { quest ->
            placesById[quest.placeId]?.let { place ->
                MapMarker(id = quest.id, position = place.location, title = quest.title, subtitle = place.name)
            }
        }
    }
}
