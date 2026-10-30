package com.kotlin.wandr.ui.feature.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.core.location.LocationProvider
import com.kotlin.wandr.core.location.UserLocation
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.data.repository.AnalyticsRepository
import com.kotlin.wandr.data.repository.QuestRepository
import com.kotlin.wandr.data.repository.StorageRepository
import com.kotlin.wandr.domain.model.ActiveQuest
import com.kotlin.wandr.domain.model.GeoPoint
import com.kotlin.wandr.domain.model.LiveDistance
import com.kotlin.wandr.domain.model.distanceMetersTo
import com.kotlin.wandr.domain.model.ObjectiveResult
import com.kotlin.wandr.domain.model.QuestDropoffReport
import com.kotlin.wandr.domain.model.StepRisk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LocationStatus {
    OFF,
    SEARCHING,
    NO_FIX,
    LIVE,
}

data class ActiveQuestUiState(
    val isLoading: Boolean = true,
    val quests: List<ActiveQuest> = emptyList(),
    val isShowingSavedData: Boolean = false,
    val submittingObjectiveId: String? = null,
    val abandoningQuestId: String? = null,
    val lastResult: ObjectiveResult? = null,
    val errorMessage: String? = null,
    val dropoff: QuestDropoffReport? = null,
    val liveDistance: LiveDistance? = null,
    val locationStatus: LocationStatus = LocationStatus.OFF,
) {
    fun riskiestStepFor(questId: String): StepRisk? = dropoff?.riskiestStepFor(questId)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActiveQuestViewModel @Inject constructor(
    private val questRepository: QuestRepository,
    private val storageRepository: StorageRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveQuestUiState())
    val uiState: StateFlow<ActiveQuestUiState> = _uiState.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0)

    init {
        refreshTrigger
            .flatMapLatest { questRepository.activeQuests() }
            .onEach { resource ->
                _uiState.update { state ->
                    when (resource) {
                        Resource.Loading -> state.copy(isLoading = true)
                        is Resource.Success -> state.copy(
                            isLoading = false,
                            quests = resource.data,
                            isShowingSavedData = resource.isStale,
                        )
                        is Resource.Error -> state.copy(isLoading = false, errorMessage = resource.error.message)
                    }
                }
            }
            .launchIn(viewModelScope)

        loadDropoffHints()
    }

    private fun loadDropoffHints() {
        viewModelScope.launch {
            analyticsRepository.questDropoff()
                .onSuccess { report -> _uiState.update { it.copy(dropoff = report) } }
        }
    }

    fun refresh() = refreshTrigger.update { it + 1 }

    private var distanceJob: Job? = null

    fun trackDistanceTo(destination: GeoPoint) {
        distanceJob?.cancel()
        _uiState.update { it.copy(locationStatus = LocationStatus.SEARCHING) }
        distanceJob = flow {
            emit(locationProvider.currentLocation()) // a first fix right away
            emitAll(locationProvider.locationUpdates(minDistanceMeters = DISTANCE_STEP_METERS, intervalMs = GPS_INTERVAL_MS))
        }
            .onEach { location ->
                val distance = location.distanceTo(destination)
                _uiState.update {
                    it.copy(
                        liveDistance = distance,
                        locationStatus = if (distance == null) LocationStatus.NO_FIX else LocationStatus.LIVE,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun stopTrackingDistance() {
        distanceJob?.cancel()
        distanceJob = null
        _uiState.update { it.copy(locationStatus = LocationStatus.OFF) }
    }

    private fun UserLocation.distanceTo(destination: GeoPoint): LiveDistance? = when (source) {
        UserLocation.Source.FALLBACK -> null
        UserLocation.Source.LAST_KNOWN -> LiveDistance(point.distanceMetersTo(destination), isApproximate = true)
        UserLocation.Source.CURRENT -> LiveDistance(point.distanceMetersTo(destination))
    }

    fun completeObjective(questId: String, objectiveId: String, photoJpeg: ByteArray? = null) {
        if (_uiState.value.submittingObjectiveId != null) return
        val objective = _uiState.value.quests
            .firstOrNull { it.questId == questId }
            ?.objectives?.firstOrNull { it.id == objectiveId }
        if (objective?.requiresPhoto == true && photoJpeg == null) {
            _uiState.update { it.copy(errorMessage = "This step needs a photo") }
            return
        }
        _uiState.update { it.copy(submittingObjectiveId = objectiveId, errorMessage = null) }
        viewModelScope.launch {
            val result = runCatching {
                val photoPath = photoJpeg?.let {
                    storageRepository.uploadQuestPhoto(questId, objectiveId, it).getOrThrow()
                }
                questRepository.completeObjective(questId, objectiveId, photoPath).getOrThrow()
            }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(submittingObjectiveId = null, lastResult = it) },
                    onFailure = { state.copy(submittingObjectiveId = null, errorMessage = it.appError.message) },
                )
            }
        }
    }

    fun abandonQuest(questId: String) {
        if (_uiState.value.abandoningQuestId != null) return
        _uiState.update { it.copy(abandoningQuestId = questId, errorMessage = null) }
        viewModelScope.launch {
            val result = questRepository.abandonQuest(questId)
            _uiState.update {
                it.copy(abandoningQuestId = null, errorMessage = result.exceptionOrNull()?.appError?.message)
            }
        }
    }

    fun onResultShown() = _uiState.update { it.copy(lastResult = null) }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    private companion object {
        const val DISTANCE_STEP_METERS = 10f
        const val GPS_INTERVAL_MS = 5_000L
    }
}
