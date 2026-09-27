package com.kotlin.wandr.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.core.location.LocationProvider
import com.kotlin.wandr.core.location.UserLocation
import com.kotlin.wandr.core.strategy.FetchPolicy
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.data.repository.QuestRepository
import com.kotlin.wandr.data.repository.TagRepository
import com.kotlin.wandr.domain.model.Quest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** "Because you liked [becauseYouLiked]" card. */
data class QuestRecommendation(val quest: Quest, val becauseYouLiked: String)

data class HomeUiState(
    val isLoading: Boolean = true,
    val allQuests: List<Quest> = emptyList(),
    val selectedTag: String? = null,
    val recommendations: List<QuestRecommendation> = emptyList(),
    /** Offline: show "No internet connection. Showing saved activities" (QS1). */
    val isShowingSavedData: Boolean = false,
    /** FALLBACK / LAST_KNOWN: tell the user the location may be off (QS11). */
    val locationSource: UserLocation.Source? = null,
    val errorMessage: String? = null,
) {
    val quests: List<Quest> get() = selectedTag?.let { tag -> allQuests.filter { tag in it.tags } } ?: allQuests
    val availableTags: List<String> get() = allQuests.flatMap { it.tags }.distinct().sorted()
}

/** Discovery Engine: quests near the user, tag filters and recommendations from their interests. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val questRepository: QuestRepository,
    private val tagRepository: TagRepository,
    private val locationProvider: LocationProvider,
    eventBus: AppEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0)

    init {
        refreshTrigger
            .flatMapLatest { nearbyQuests() }
            .combine(interestNames()) { resource, interests -> reduce(resource, interests) }
            .launchIn(viewModelScope)

        // Back online after showing saved data: load fresh quests without the user asking
        eventBus.subscribe<AppEvent.ConnectivityChanged>()
            .filter { it.isOnline && _uiState.value.isShowingSavedData }
            .onEach { refresh() }
            .launchIn(viewModelScope)
    }

    fun refresh() = refreshTrigger.update { it + 1 }

    /** null = no filter. */
    fun selectTag(tag: String?) = _uiState.update { it.copy(selectedTag = tag) }

    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    private fun nearbyQuests(): Flow<Resource<List<Quest>>> = flow {
        emit(Resource.Loading)
        val location = locationProvider.currentLocation()
        _uiState.update { it.copy(locationSource = location.source) }
        emitAll(questRepository.nearbyQuests(location.point))
    }

    /** Names of the tags the user picked in onboarding. */
    private fun interestNames(): Flow<Set<String>> = combine(
        tagRepository.observeInterestIds(),
        tagRepository.observeTags(FetchPolicy.CACHE_FIRST),
    ) { ids, tags ->
        (tags as? Resource.Success)?.data.orEmpty().filter { it.id in ids }.map { it.name }.toSet()
    }

    private fun reduce(resource: Resource<List<Quest>>, interests: Set<String>) = _uiState.update { state ->
        when (resource) {
            Resource.Loading -> state.copy(isLoading = true)
            is Resource.Error -> state.copy(isLoading = false, errorMessage = resource.error.message)
            is Resource.Success -> state.copy(
                isLoading = false,
                allQuests = resource.data,
                isShowingSavedData = resource.isStale,
                recommendations = recommend(resource.data, interests),
                selectedTag = state.selectedTag?.takeIf { tag -> resource.data.any { tag in it.tags } },
            )
        }
    }

    private fun recommend(quests: List<Quest>, interests: Set<String>): List<QuestRecommendation> =
        quests.mapNotNull { quest ->
            quest.tags.firstOrNull { it in interests }?.let { QuestRecommendation(quest, becauseYouLiked = it) }
        }.take(MAX_RECOMMENDATIONS)

    private companion object {
        const val MAX_RECOMMENDATIONS = 5
    }
}
