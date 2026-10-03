package com.kotlin.wandr.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.error.appError
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.data.repository.AuthRepository
import com.kotlin.wandr.data.repository.ProfileRepository
import com.kotlin.wandr.domain.model.EarnedBadge
import com.kotlin.wandr.domain.model.QuestHistoryItem
import com.kotlin.wandr.domain.model.StreakSummary
import com.kotlin.wandr.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val badges: List<EarnedBadge> = emptyList(),
    val history: List<QuestHistoryItem> = emptyList(),
    /** BQ4: weekly streak, quests per week and achievements. Null until it loads. */
    val streak: StreakSummary? = null,
    val isStreakLoading: Boolean = false,
    /** The streak summary could not be loaded (it needs a connection): show "Try again". */
    val streakFailed: Boolean = false,
    val isShowingSavedData: Boolean = false,
    /** A quest was just completed somewhere in the app: show the celebration. */
    val celebration: AppEvent.QuestCompleted? = null,
    val isSignedOut: Boolean = false,
    val errorMessage: String? = null,
)

/** Profile / Side Quests: level, XP, streak, tier, badges, quest history and the BQ4 streak summary. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    eventBus: AppEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0)

    private var streakJob: Job? = null

    init {
        // Observer: these are Room flows, so when the CacheInvalidator refreshes the cache
        // after a quest is completed, this screen updates by itself.
        refreshTrigger
            .flatMapLatest {
                combine(
                    profileRepository.observeProfile(),
                    profileRepository.observeBadges(),
                    profileRepository.observeQuestHistory(),
                ) { profile, badges, history -> Triple(profile, badges, history) }
            }
            .onEach { (profile, badges, history) -> reduce(profile, badges, history) }
            .launchIn(viewModelScope)

        // Observer: a quest completed anywhere in the app changes the streak and the weekly
        // numbers, so the summary is downloaded again and the celebration is shown.
        eventBus.subscribe<AppEvent.QuestCompleted>()
            .onEach { event ->
                _uiState.update { it.copy(celebration = event) }
                loadStreak()
            }
            .launchIn(viewModelScope)

        loadStreak()
    }

    fun refresh() {
        refreshTrigger.update { it + 1 }
        loadStreak()
    }

    /** Downloads the BQ4 summary. Called on start, on refresh, on "Try again" and after a quest. */
    fun loadStreak() {
        // The newest request wins: a quest finished while loading must not be missed
        streakJob?.cancel()
        _uiState.update { it.copy(isStreakLoading = true, streakFailed = false) }
        streakJob = viewModelScope.launch {
            profileRepository.streakSummary()
                .onSuccess { summary ->
                    _uiState.update { it.copy(isStreakLoading = false, streak = summary) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isStreakLoading = false, streakFailed = true, errorMessage = error.appError.message)
                    }
                }
        }
    }

    fun onCelebrationShown() = _uiState.update { it.copy(celebration = null) }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
                .onSuccess { _uiState.update { it.copy(isSignedOut = true) } }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.appError.message) } }
        }
    }

    private fun reduce(
        profile: Resource<UserProfile>,
        badges: Resource<List<EarnedBadge>>,
        history: Resource<List<QuestHistoryItem>>,
    ) = _uiState.update { state ->
        val all = listOf(profile, badges, history)
        state.copy(
            isLoading = all.any { it is Resource.Loading },
            profile = (profile as? Resource.Success)?.data ?: state.profile,
            badges = (badges as? Resource.Success)?.data ?: state.badges,
            history = (history as? Resource.Success)?.data ?: state.history,
            isShowingSavedData = all.any { it is Resource.Success && it.isStale },
            errorMessage = all.filterIsInstance<Resource.Error>().firstOrNull()?.error?.message ?: state.errorMessage,
        )
    }
}
