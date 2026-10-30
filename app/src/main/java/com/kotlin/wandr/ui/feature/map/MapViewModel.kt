package com.kotlin.wandr.ui.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.location.LocationProvider
import com.kotlin.wandr.core.location.UserLocation
import com.kotlin.wandr.core.network.ConnectivityObserver
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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class MapUiState(
    val isLoading: Boolean = true,
    val userLocation: UserLocation? = null,
    val quests: List<Quest> = emptyList(),
    val markers: List<MapMarker> = emptyList(),
    val radiusKm: Double = QuestRepository.DEFAULT_RADIUS_KM,
    val isShowingSavedData: Boolean = false,
    val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val questRepository: QuestRepository,
    private val placeRepository: PlaceRepository,
    private val locationProvider: LocationProvider,
    private val connectivity: ConnectivityObserver,
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

        connectivity.status.drop(1).filter { it }.onEach { refresh() }.launchIn(viewModelScope)
    }

    fun setRadius(radiusKm: Double) {
        _uiState.update { it.copy(radiusKm = radiusKm) }
        query.update { it.copy(radiusKm = radiusKm) }
    }

    fun refresh() = query.update { it.copy(tick = it.tick + 1) }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    private fun markersFor(quests: List<Quest>, places: List<Place>): List<MapMarker> {
        val placesById = places.associateBy { it.id }
        return quests.mapNotNull { quest ->
            placesById[quest.placeId]?.let { place ->
                MapMarker(id = quest.id, position = place.location, title = quest.title, subtitle = place.name)
            }
        }
    }
}
