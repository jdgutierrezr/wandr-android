
package com.kotlin.wandr.ui.feature.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.location.LocationProvider
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.data.repository.EventRepository
import com.kotlin.wandr.data.repository.QuestRepository
import com.kotlin.wandr.domain.model.Event
import com.kotlin.wandr.domain.model.Quest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class ExploreUiState(
    val isLoadingEvents: Boolean = true,
    val isLoadingQuests: Boolean = true,
    val events: List<Event> = emptyList(),
    val quests: List<Quest> = emptyList(),
    val filteredEvents: List<Event> = emptyList(),
    val filteredQuests: List<Quest> = emptyList(),
    val isShowingSavedData: Boolean = false,
    val errorMessage: String? = null,
) {
    val isLoading: Boolean
        get() = isLoadingEvents || isLoadingQuests
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val questRepository: QuestRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0)

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow("All")

    private val debouncedSearchQuery = searchQuery
        .debounce(300)
        .mapLatest { it.trim() }

    init {
        refreshTrigger
            .flatMapLatest { upcomingEvents() }
            .onEach { resource -> reduceEvents(resource) }
            .launchIn(viewModelScope)

        refreshTrigger
            .flatMapLatest { nearbyQuests() }
            .onEach { resource -> reduceQuests(resource) }
            .launchIn(viewModelScope)

        combine(
            uiState
                .map { state -> state.events to state.quests }
                .distinctUntilChanged(),
            debouncedSearchQuery,
            selectedCategory,
        ) { eventAndQuestLists, query, category ->
            val events = eventAndQuestLists.first
            val quests = eventAndQuestLists.second

            Triple(events, quests, query to category)
        }
            .mapLatest { result ->
                val events = result.first
                val quests = result.second
                val query = result.third.first
                val category = result.third.second

                val filteredEvents = filterEvents(events, query)
                val filteredQuests = filterQuests(quests, query, category)

                filteredEvents to filteredQuests
            }
            .flowOn(Dispatchers.Default)
            .onEach { (filteredEvents, filteredQuests) ->
                _uiState.update {
                    it.copy(
                        filteredEvents = filteredEvents,
                        filteredQuests = filteredQuests,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        selectedCategory.value = category
    }

    fun refresh() {
        refreshTrigger.update { it + 1 }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun filterEvents(
        events: List<Event>,
        query: String,
    ): List<Event> {
        return events.filter { event ->
            query.isBlank() || listOfNotNull(
                event.title,
                event.description,
                event.placeName,
            ).joinToString(" ").contains(query, ignoreCase = true)
        }
    }

    private fun filterQuests(
        quests: List<Quest>,
        query: String,
        category: String,
    ): List<Quest> {
        return quests.filter { quest ->
            val matchesSearch = query.isBlank() || listOfNotNull(
                quest.title,
                quest.description,
                quest.placeName,
                quest.tags.joinToString(" "),
            ).joinToString(" ").contains(query, ignoreCase = true)

            val matchesCategory = category == "All" ||
                    quest.tags.any { it.equals(category, ignoreCase = true) }

            matchesSearch && matchesCategory
        }
    }

    private fun upcomingEvents(): Flow<Resource<List<Event>>> = flow {
        emit(Resource.Loading)
        emitAll(eventRepository.upcomingEvents())
    }

    private fun nearbyQuests(): Flow<Resource<List<Quest>>> = flow {
        emit(Resource.Loading)
        val location = locationProvider.currentLocation()
        emitAll(
            questRepository.nearbyQuests(
                location = location.point,
                radiusKm = QuestRepository.DEFAULT_RADIUS_KM,
            )
        )
    }

    private fun reduceEvents(resource: Resource<List<Event>>) {
        when (resource) {
            Resource.Loading -> _uiState.update {
                it.copy(isLoadingEvents = true)
            }

            is Resource.Error -> _uiState.update {
                it.copy(
                    isLoadingEvents = false,
                    errorMessage = resource.error.message,
                )
            }

            is Resource.Success -> _uiState.update {
                it.copy(
                    isLoadingEvents = false,
                    events = resource.data,
                    isShowingSavedData = it.isShowingSavedData || resource.isStale,
                )
            }
        }
    }

    private fun reduceQuests(resource: Resource<List<Quest>>) {
        when (resource) {
            Resource.Loading -> _uiState.update {
                it.copy(isLoadingQuests = true)
            }

            is Resource.Error -> _uiState.update {
                it.copy(
                    isLoadingQuests = false,
                    errorMessage = resource.error.message,
                )
            }

            is Resource.Success -> _uiState.update {
                it.copy(
                    isLoadingQuests = false,
                    quests = resource.data,
                    isShowingSavedData = it.isShowingSavedData || resource.isStale,
                )
            }
        }
    }
}
