package com.kotlin.wandr.ui.feature.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
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

/** What the GPS part of Active Quest is doing, so the screen never stays silent. */
enum class LocationStatus {
    /** Not following the GPS (no permission, or the screen is not visible). */
    OFF,

    /** Waiting for the first reading. */
    SEARCHING,

    /** The phone has no position at all (location turned off, or the emulator has none set). */
    NO_FIX,

    /** [ActiveQuestUiState.liveDistance] is up to date. */
    LIVE,
}

data class ActiveQuestUiState(
    val isLoading: Boolean = true,
    val quests: List<ActiveQuest> = emptyList(),
    val isShowingSavedData: Boolean = false,
    /** Step being sent (show a spinner on it). */
    val submittingObjectiveId: String? = null,
    val abandoningQuestId: String? = null,
    /** Last checked step: "2 of 3", or the XP / level / badges when the quest was completed. */
    val lastResult: ObjectiveResult? = null,
    val errorMessage: String? = null,
    /** Smart feature: BQ8 drop-off data, used to warn on the riskiest step. Null = no hint. */
    val dropoff: QuestDropoffReport? = null,
    /** Context-aware: distance from the user's GPS to the quest's place. Null = unknown. */
    val liveDistance: LiveDistance? = null,
    val locationStatus: LocationStatus = LocationStatus.OFF,
) {
    /** The step of [questId] where most people give up, or null if there is no data. */
    fun riskiestStepFor(questId: String): StepRisk? = dropoff?.riskiestStepFor(questId)
}

/** Active Quest Tracker. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActiveQuestViewModel @Inject constructor(
    private val questRepository: QuestRepository,
    private val storageRepository: StorageRepository,
    private val eventBus: AppEventBus,
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

    /**
     * Smart feature: reuses the BQ8 analytics (where users abandon quests) to warn the user on
     * the step where most people give up. It is only a hint, so a failure is silent: no data
     * or no connection simply means no hint.
     */
    private fun loadDropoffHints() {
        viewModelScope.launch {
            analyticsRepository.questDropoff()
                .onSuccess { report -> _uiState.update { it.copy(dropoff = report) } }
        }
    }

    fun refresh() = refreshTrigger.update { it + 1 }

    private var distanceJob: Job? = null

    /**
     * Context-aware + sensor: follows the phone's GPS and keeps [ActiveQuestUiState.liveDistance]
     * up to date. The screen calls it when it is visible and calls [stopTrackingDistance] when it
     * leaves, so the GPS does not drain the battery in the background.
     */
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

    /** No permission or no fix (FALLBACK = center of Bogota) must not pretend to know the distance. */
    private fun UserLocation.distanceTo(destination: GeoPoint): LiveDistance? = when (source) {
        UserLocation.Source.FALLBACK -> null
        UserLocation.Source.LAST_KNOWN -> LiveDistance(point.distanceMetersTo(destination), isApproximate = true)
        UserLocation.Source.CURRENT -> LiveDistance(point.distanceMetersTo(destination))
    }

    /**
     * Checks a step. If it needs a photo, pass the JPEG taken with the camera in [photoJpeg]:
     * it is uploaded to Storage first and its path is sent to `complete_objective`.
     */
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

    /**
     * BQ8 funnel: the user tapped "Navigate". The screen opens the maps app; this only publishes
     * NAVIGATION_STARTED for the QuestFunnelTracker (Observer).
     */
    fun startNavigation(questId: String) {
        viewModelScope.launch { eventBus.publish(AppEvent.NavigationStarted(questId)) }
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
        /** New GPS readings only after moving this much or waiting this long (QS5: battery). */
        const val DISTANCE_STEP_METERS = 10f
        const val GPS_INTERVAL_MS = 5_000L
    }
}
