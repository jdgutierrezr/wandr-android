package com.kotlin.wandr.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotlin.wandr.core.location.LocationProvider
import com.kotlin.wandr.core.location.UserLocation
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.data.mapper.toRecommendedQuest
import com.kotlin.wandr.data.repository.QuestRepository
import com.kotlin.wandr.domain.model.Quest
import com.kotlin.wandr.domain.model.RecommendedQuest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class HomeUiState(
    val isLoading: Boolean = true,

    // ---------------------------------------------------------
    // REGULAR ACTIVITIES
    // ---------------------------------------------------------

    val allQuests: List<Quest> = emptyList(),

    val selectedTag: String? = null,

    // ---------------------------------------------------------
    // MYSTERY QUEST
    // ---------------------------------------------------------

    /*
     * Values expected by the backend:
     *
     * relaxed   -> Low
     * balanced  -> Medium
     * energetic -> High
     */
    val mysteryEnergyLevel: String = "balanced",

    val mysteryRadiusKm: Double = 3.0,

    /*
     * Indicates whether the current Mystery Quest
     * has already been revealed.
     */
    val mysteryRevealed: Boolean = false,

    /*
     * Index of the currently displayed recommendation.
     * Used by Skip.
     */
    val mysteryQuestIndex: Int = 0,

    /*
     * Recommendations returned by the backend.
     */
    val recommendations: List<RecommendedQuest> = emptyList(),

    // ---------------------------------------------------------
    // OTHER STATE
    // ---------------------------------------------------------

    val isShowingSavedData: Boolean = false,

    val locationSource: UserLocation.Source? = null,

    val isLoadingMystery: Boolean = false,

    val errorMessage: String? = null,
) {

    /*
     * ---------------------------------------------------------
     * REGULAR ACTIVITIES
     * ---------------------------------------------------------
     */

    val quests: List<Quest>
        get() = selectedTag?.let { tag ->
            allQuests.filter { tag in it.tags }
        } ?: allQuests

    val availableTags: List<String>
        get() = allQuests
            .flatMap { it.tags }
            .distinct()
            .sorted()

    /*
     * ---------------------------------------------------------
     * MYSTERY QUEST
     * ---------------------------------------------------------
     */

    /*
     * Current Mystery Quest.
     *
     * The backend returns a list of recommendations and
     * mysteryQuestIndex determines which one is currently shown.
     */
    val mysteryQuest: RecommendedQuest?
        get() = recommendations.getOrNull(mysteryQuestIndex)

    /*
     * These three properties are aliases used by HomeScreen.
     *
     * The internal state remains in backend terminology.
     */

    val selectedEnergyLevel: String
        get() = when (mysteryEnergyLevel) {
            "relaxed" -> "low"
            "balanced" -> "medium"
            "energetic" -> "high"
            else -> "medium"
        }

    val selectedRadiusKm: Double
        get() = mysteryRadiusKm

    val isMysteryRevealed: Boolean
        get() = mysteryRevealed
}


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val questRepository: QuestRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> =
        _uiState.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0)

    init {
        refreshTrigger
            .flatMapLatest {
                nearbyQuests()
            }
            .onEach { resource ->
                reduceNearbyQuests(resource)
            }
            .launchIn(viewModelScope)
    }

    // =========================================================
    // REGULAR ACTIVITIES
    // =========================================================

    fun refresh() {
        refreshTrigger.update { it + 1 }
    }

    fun selectTag(tag: String?) {
        _uiState.update {
            it.copy(
                selectedTag = tag
            )
        }
    }

    // =========================================================
    // MYSTERY QUEST FILTERS
    // =========================================================

    /*
     * Public method used by HomeScreen.
     *
     * HomeScreen uses:
     *
     * low
     * medium
     * high
     *
     * while the backend expects:
     *
     * relaxed
     * balanced
     * energetic
     */
    fun selectEnergyLevel(energyLevel: String) {

        val backendValue = when (energyLevel.lowercase()) {
            "low" -> "relaxed"
            "medium" -> "balanced"
            "high" -> "energetic"

            /*
             * Also allow backend values directly.
             */
            "relaxed" -> "relaxed"
            "balanced" -> "balanced"
            "energetic" -> "energetic"

            else -> return
        }

        selectMysteryEnergy(backendValue)
    }

    /*
     * Internal Mystery Quest energy selector.
     */
    fun selectMysteryEnergy(energyLevel: String) {

        if (
            energyLevel != "relaxed" &&
            energyLevel != "balanced" &&
            energyLevel != "energetic"
        ) {
            return
        }

        _uiState.update {
            it.copy(
                mysteryEnergyLevel = energyLevel,
                mysteryRevealed = false,
                mysteryQuestIndex = 0,
            )
        }

        loadMysteryQuests()
    }

    /*
     * Public method used by HomeScreen.
     */
    fun selectRadius(radiusKm: Double) {
        setMysteryRadius(radiusKm)
    }

    /*
     * Internal radius selector.
     */
    fun setMysteryRadius(radiusKm: Double) {

        if (radiusKm <= 0) {
            return
        }

        _uiState.update {
            it.copy(
                mysteryRadiusKm = radiusKm,
                mysteryRevealed = false,
                mysteryQuestIndex = 0,
            )
        }

        loadMysteryQuests()
    }

    // =========================================================
    // MYSTERY QUEST ACTIONS
    // =========================================================

    /*
     * Reveals the current Mystery Quest.
     */
    fun revealMysteryQuest() {

        if (_uiState.value.mysteryQuest == null) {
            return
        }

        _uiState.update {
            it.copy(
                mysteryRevealed = true
            )
        }
    }

    /*
     * Skip the current Mystery Quest.
     *
     * If another recommendation is already loaded,
     * move to it.
     *
     * If all recommendations have been consumed,
     * ask the backend for another batch.
     */
    fun skipMysteryQuest() {

        val state = _uiState.value

        if (state.recommendations.isEmpty()) {
            return
        }

        val nextIndex =
            if (
                state.mysteryQuestIndex + 1 <
                state.recommendations.size
            ) {
                state.mysteryQuestIndex + 1
            } else {
                0
            }

        /*
         * We reached the end of the current recommendations.
         */
        if (
            nextIndex == 0 &&
            state.mysteryQuestIndex + 1 >=
            state.recommendations.size
        ) {

            loadMysteryQuests()

        } else {

            _uiState.update {
                it.copy(
                    mysteryQuestIndex = nextIndex,
                    mysteryRevealed = false,
                )
            }
        }
    }

    /*
     * Allows reloading Mystery Quest without
     * changing the selected filters.
     */
    fun refreshMysteryQuest() {
        loadMysteryQuests()
    }

    // =========================================================
    // ERROR
    // =========================================================

    fun onErrorShown() {

        _uiState.update {
            it.copy(
                errorMessage = null
            )
        }
    }

    // =========================================================
    // REGULAR QUESTS
    // =========================================================

    private fun nearbyQuests(): Flow<Resource<List<Quest>>> = flow {

        emit(Resource.Loading)

        val location =
            locationProvider.currentLocation()

        _uiState.update {
            it.copy(
                locationSource = location.source
            )
        }

        emitAll(
            questRepository.nearbyQuests(
                location.point
            )
        )
    }

    private fun reduceNearbyQuests(
        resource: Resource<List<Quest>>,
    ) {

        when (resource) {

            Resource.Loading -> {

                _uiState.update {
                    it.copy(
                        isLoading = true
                    )
                }
            }

            is Resource.Error -> {

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = resource.error.message,
                    )
                }
            }

            is Resource.Success -> {

                _uiState.update { current ->

                    current.copy(
                        isLoading = false,

                        /*
                         * IMPORTANT:
                         * Regular activities remain independent
                         * from Mystery Quest.
                         */
                        allQuests = resource.data,

                        isShowingSavedData =
                            resource.isStale,

                        selectedTag =
                            current.selectedTag?.takeIf { tag ->

                                resource.data.any { quest ->
                                    tag in quest.tags
                                }
                            },
                    )
                }

                /*
                 * Once regular activities are loaded,
                 * load Mystery Quest independently.
                 */
                loadMysteryQuests()
            }
        }
    }

    // =========================================================
    // MYSTERY QUEST BACKEND
    // =========================================================

    private fun loadMysteryQuests() {

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isLoadingMystery = true,
                    errorMessage = null,
                )
            }

            try {

                val state = _uiState.value

                val location =
                    locationProvider.currentLocation()

                _uiState.update {
                    it.copy(
                        locationSource = location.source
                    )
                }

                /*
                 * Backend receives:
                 *
                 * - user location
                 * - selected radius
                 * - selected energy level
                 */
                val result =
                    questRepository.recommendedQuests(
                        location = location.point,
                        radiusKm = state.mysteryRadiusKm,
                        energyLevel = state.mysteryEnergyLevel,
                    )

                val recommendations =
                    result
                        .getOrNull()
                        ?.map { dto ->
                            dto.toRecommendedQuest()
                        }
                        ?: emptyList()

                _uiState.update {
                    it.copy(
                        recommendations = recommendations,
                        mysteryQuestIndex = 0,
                        mysteryRevealed = false,
                        isLoadingMystery = false,
                    )
                }

            } catch (exception: Exception) {

                _uiState.update {
                    it.copy(
                        isLoadingMystery = false,
                        errorMessage =
                            exception.message
                                ?: "Unable to load Mystery Quest.",
                    )
                }
            }
        }
    }
}