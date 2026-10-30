package com.kotlin.wandr.ui.feature.quest

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.error.AppError
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.core.location.LocationProvider
import com.kotlin.wandr.core.location.UserLocation
import com.kotlin.wandr.core.storage.PhotoGallery
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.data.repository.AnalyticsRepository
import com.kotlin.wandr.data.repository.QuestRepository
import com.kotlin.wandr.domain.model.ActiveQuest
import com.kotlin.wandr.domain.model.GeoPoint
import com.kotlin.wandr.domain.model.LiveDistance
import com.kotlin.wandr.domain.model.distanceMetersTo
import com.kotlin.wandr.domain.model.ObjectiveResult
import com.kotlin.wandr.domain.model.QuestDropoffReport
import com.kotlin.wandr.domain.model.StepOutcome
import com.kotlin.wandr.domain.model.StepRisk
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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
    /** Steps checked offline, waiting to be sent. */
    val pendingObjectiveIds: Set<String> = emptySet(),
    val notice: String? = null,
    /** Photo that could not be kept offline: the user decides what to do with it. */
    val unsavedPhoto: UnsavedPhoto? = null,
) {
    fun riskiestStepFor(questId: String): StepRisk? = dropoff?.riskiestStepFor(questId)
}

class UnsavedPhoto(val jpeg: ByteArray)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActiveQuestViewModel @Inject constructor(
    private val questRepository: QuestRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val locationProvider: LocationProvider,
    private val photoGallery: PhotoGallery,
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

        questRepository.pendingObjectiveIds()
            .onEach { ids -> _uiState.update { it.copy(pendingObjectiveIds = ids) } }
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
            val result = questRepository.checkObjective(questId, objectiveId, photoJpeg)
            _uiState.update { state ->
                result.fold(
                    onSuccess = { outcome ->
                        when (outcome) {
                            is StepOutcome.Synced -> state.copy(submittingObjectiveId = null, lastResult = outcome.result)
                            StepOutcome.Queued -> state.copy(submittingObjectiveId = null, notice = SAVED_OFFLINE)
                        }
                    },
                    onFailure = { error ->
                        if (error.appError == AppError.NoSpace && photoJpeg != null) {
                            state.copy(submittingObjectiveId = null, unsavedPhoto = UnsavedPhoto(photoJpeg))
                        } else {
                            state.copy(submittingObjectiveId = null, errorMessage = error.appError.message)
                        }
                    },
                )
            }
        }
    }

    fun savePhotoToGallery() {
        val photo = _uiState.value.unsavedPhoto ?: return
        _uiState.update { it.copy(unsavedPhoto = null) }
        viewModelScope.launch {
            val saved = photoGallery.save(photo.jpeg)
            _uiState.update {
                it.copy(notice = if (saved) SAVED_TO_GALLERY else "The photo could not be saved")
            }
        }
    }

    fun dismissUnsavedPhoto() = _uiState.update { it.copy(unsavedPhoto = null) }

    fun onNoticeShown() = _uiState.update { it.copy(notice = null) }

    fun completeObjectiveWithPhoto(questId: String, objectiveId: String, photo: Bitmap) {
        if (_uiState.value.submittingObjectiveId != null) return
        viewModelScope.launch {
            // compressing on the main thread freezes the screen
            val jpeg = withContext(Dispatchers.Default) { photo.toJpeg() }
            completeObjective(questId, objectiveId, jpeg)
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
        const val SAVED_OFFLINE = "Saved on the phone. It will sync when you're back online"
        const val SAVED_TO_GALLERY = "Photo saved in your gallery. Check the step again when you're online"
    }
}

private fun Bitmap.toJpeg(): ByteArray = ByteArrayOutputStream().use { out ->
    compress(Bitmap.CompressFormat.JPEG, 85, out)
    out.toByteArray()
}
