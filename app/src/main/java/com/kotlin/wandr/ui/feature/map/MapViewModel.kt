package com.kotlin.wandr.ui.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.location.LocationProvider
import com.kotlin.wandr.core.location.UserLocation
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.data.repository.PlaceRepository
import com.kotlin.wandr.data.repository.QuestRepository
import com.kotlin.wandr.domain.model.Place
import com.kotlin.wandr.domain.model.PlaceCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class MapUiState(
    val isLoading: Boolean = true,
    val userLocation: UserLocation? = null,
    val places: List<Place> = emptyList(),
    /** null = every category. */
    val selectedCategory: PlaceCategory? = null,
    val radiusKm: Double = QuestRepository.DEFAULT_RADIUS_KM,
    val isShowingSavedData: Boolean = false,
    val errorMessage: String? = null,
)

/** Map: places around the user, filtered by category and radius. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private data class Query(val category: PlaceCategory?, val radiusKm: Double, val tick: Int)

    private val query = MutableStateFlow(Query(null, QuestRepository.DEFAULT_RADIUS_KM, 0))

    init {
        query
            .flatMapLatest { q ->
                flow {
                    emit(Resource.Loading)
                    val location = locationProvider.currentLocation()
                    _uiState.update { it.copy(userLocation = location) }
                    emitAll(placeRepository.nearbyPlaces(location.point, q.radiusKm, q.category))
                }
            }
            .onEach { resource ->
                _uiState.update { state ->
                    when (resource) {
                        Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> state.copy(
                            isLoading = false,
                            places = resource.data,
                            isShowingSavedData = resource.isStale,
                        )
                        is Resource.Error -> state.copy(isLoading = false, errorMessage = resource.error.message)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun selectCategory(category: PlaceCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
        query.update { it.copy(category = category) }
    }

    fun setRadius(radiusKm: Double) {
        _uiState.update { it.copy(radiusKm = radiusKm) }
        query.update { it.copy(radiusKm = radiusKm) }
    }

    /** Also re-reads the location (e.g. after the user grants the permission). */
    fun refresh() = query.update { it.copy(tick = it.tick + 1) }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }
}
