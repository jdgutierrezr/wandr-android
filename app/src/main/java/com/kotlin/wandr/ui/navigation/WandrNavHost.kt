package com.kotlin.wandr.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.kotlin.wandr.BuildConfig
import com.kotlin.wandr.ui.catalog.ComponentCatalog
import com.kotlin.wandr.ui.components.MainTab
import com.kotlin.wandr.ui.feature.auth.AfterAuthDestination
import com.kotlin.wandr.ui.feature.auth.LoginRoute
import com.kotlin.wandr.ui.feature.auth.SignUpRoute
import com.kotlin.wandr.ui.feature.events.ExploreRoute
import com.kotlin.wandr.ui.feature.home.HomeRoute
import com.kotlin.wandr.ui.feature.profile.ProfileRoute
import com.kotlin.wandr.ui.components.map.WandrMap
import com.kotlin.wandr.ui.feature.map.MapRoute
import com.kotlin.wandr.ui.feature.analytics.QuestDropoffRoute
import com.kotlin.wandr.ui.feature.quest.ActiveQuestRoute
import com.kotlin.wandr.ui.feature.quest.ActiveQuestsRoute
import com.kotlin.wandr.ui.feature.quest.QuestDetailRoute
import com.kotlin.wandr.ui.feature.session.SessionViewModel
import com.kotlin.wandr.ui.feature.session.SplashRoute
import com.kotlin.wandr.domain.model.SessionState
import kotlinx.serialization.Serializable


// =============================================================
// DESTINATIONS
// =============================================================

@Serializable
data object SplashDestination

@Serializable
data object LoginDestination

@Serializable
data object SignUpDestination

@Serializable
data object OnboardingDestination

@Serializable
data object HomeDestination

@Serializable
data object ExploreDestination

@Serializable
data object MapDestination

@Serializable
data object CatalogDestination

@Serializable
data object ProfileDestination


// =============================================================
// QUEST / PROGRESS
// =============================================================

@Serializable
data class QuestDetailDestination(
    val questId: String,
)

@Serializable
data class ActiveQuestDestination(
    val questId: String,
)

@Serializable
data object ActiveQuestsDestination

@Serializable
data object QuestDropoffDestination


// =============================================================
// NAV HOST
// =============================================================

@Composable
fun WandrNavHost(
    map: WandrMap,
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {

    val sessionState by sessionViewModel.sessionState
        .collectAsStateWithLifecycle()


    // =========================================================
    // SESSION GATE
    // =========================================================

    LaunchedEffect(sessionState) {
        if (
            sessionState == SessionState.SignedOut &&
            navController.isInsideApp()
        ) {
            navController.leaveToLogin()
        }
    }


    NavHost(
        navController = navController,
        startDestination = SplashDestination,
    ) {


        // =====================================================
        // SPLASH
        // =====================================================

        composable<SplashDestination> {

            SplashRoute(
                sessionState = sessionState,

                onSignedIn = {
                    navController.leaveSplashTo(
                        HomeDestination
                    )
                },

                onSignedOut = {
                    navController.leaveSplashTo(
                        LoginDestination
                    )
                },
            )
        }


        // =====================================================
        // LOGIN
        // =====================================================

        composable<LoginDestination> {

            LoginRoute(

                onLoggedIn = {
                    navController.leaveAuthTo(it)
                },

                onGoToSignUp = {
                    navController.navigate(
                        SignUpDestination
                    )
                },

                onOpenCatalog =
                    if (BuildConfig.DEBUG) {
                        {
                            navController.navigate(
                                CatalogDestination
                            )
                        }
                    } else {
                        null
                    },
            )
        }


        // =====================================================
        // SIGN UP
        // =====================================================

        composable<SignUpDestination> {

            SignUpRoute(

                onSignedUp = {
                    navController.leaveAuthTo(it)
                },

                onGoToLogin = {
                    navController.popBackStack()
                },
            )
        }


        // =====================================================
        // MAP
        // =====================================================

        composable<MapDestination> {

            MapRoute(
                map = map,
                onBack = {
                    navController.popBackStack()
                },
            )
        }


        // =====================================================
        // ONBOARDING
        // =====================================================

        val shortcutLinks = listOf(
            "Discovery map" to {
                navController.navigate(
                    MapDestination
                )
            },

            "My quests in progress" to {
                navController.navigate(
                    ActiveQuestsDestination
                )
            },

            "Abandonment funnel (BQ8)" to {
                navController.navigate(
                    QuestDropoffDestination
                )
            },
        )

        composable<OnboardingDestination> {

            ComingSoon(
                "Onboarding",
                links = shortcutLinks,
            )
        }


        // =====================================================
        // HOME
        // =====================================================

        composable<HomeDestination> {

            HomeRoute(
                onSelectTab = { tab ->
                    navController.openTab(tab)
                },
            )
        }


        // =====================================================
        // EXPLORE
        // =====================================================

        composable<ExploreDestination> {

            ExploreRoute(
                onSelectTab = { tab ->
                    navController.openTab(tab)
                },
            )
        }

        // =====================================================
        // CATALOG
        // =====================================================

        composable<CatalogDestination> {

            ComponentCatalog(
                onBack = {
                    navController.popBackStack()
                },
            )
        }


        // =====================================================
        // PROFILE
        // =====================================================

        composable<ProfileDestination> {

            ProfileRoute(

                onSignedOut = {
                    navController.leaveToLogin()
                },

                onSelectTab = { tab ->
                    navController.openTab(tab)
                },

                onOpenTeamAnalytics =
                    if (BuildConfig.DEBUG) {
                        {
                            navController.navigate(
                                QuestDropoffDestination
                            )
                        }
                    } else {
                        null
                    },
            )
        }


        // =====================================================
        // QUEST DETAIL
        // =====================================================

        composable<QuestDetailDestination> {

            QuestDetailRoute(

                onBack = {
                    navController.popBackStack()
                },

                onOpenTracker = { questId ->

                    navController.navigate(
                        ActiveQuestDestination(
                            questId
                        )
                    ) {

                        popUpTo<QuestDetailDestination> {
                            inclusive = true
                        }
                    }
                },
            )
        }


        // =====================================================
        // ACTIVE QUEST
        // =====================================================

        composable<ActiveQuestDestination> { entry ->

            ActiveQuestRoute(

                questId =
                    entry
                        .toRoute<ActiveQuestDestination>()
                        .questId,

                onBack = {
                    navController.popBackStack()
                },
            )
        }


        // =====================================================
        // ACTIVE QUESTS
        // =====================================================

        composable<ActiveQuestsDestination> {

            ActiveQuestsRoute(

                onBack = {
                    navController.popBackStack()
                },

                onOpenQuest = { questId ->

                    navController.navigate(
                        ActiveQuestDestination(
                            questId
                        )
                    )
                },
            )
        }


        // =====================================================
        // QUEST DROPOFF
        // =====================================================

        composable<QuestDropoffDestination> {

            QuestDropoffRoute(

                onBack = {
                    navController.popBackStack()
                },

                onOpenQuest = { questId ->

                    navController.navigate(
                        QuestDetailDestination(
                            questId
                        )
                    )
                },
            )
        }
    }
}


// =============================================================
// AUTH NAVIGATION
// =============================================================

private fun NavHostController.leaveAuthTo(
    destination: AfterAuthDestination,
) {

    val target: Any =
        when (destination) {

            AfterAuthDestination.ONBOARDING ->
                OnboardingDestination

            AfterAuthDestination.HOME ->
                HomeDestination
        }

    navigate(target) {

        popUpTo(
            LoginDestination
        ) {
            inclusive = true
        }
    }
}


// =============================================================
// SPLASH NAVIGATION
// =============================================================

private fun NavHostController.leaveSplashTo(
    destination: Any,
) {

    navigate(destination) {

        popUpTo(
            SplashDestination
        ) {
            inclusive = true
        }
    }
}


// =============================================================
// SESSION CHECK
// =============================================================

private fun NavHostController.isInsideApp(): Boolean {

    val current =
        currentBackStackEntry
            ?.destination
            ?: return false

    return !current.hasRoute<SplashDestination>() &&
            !current.hasRoute<LoginDestination>() &&
            !current.hasRoute<SignUpDestination>()
}


// =============================================================
// LOGOUT
// =============================================================

private fun NavHostController.leaveToLogin() {

    navigate(LoginDestination) {

        popUpTo(
            graph.id
        ) {
            inclusive = true
        }
    }
}


// =============================================================
// BOTTOM NAVIGATION
// =============================================================

private fun NavHostController.openTab(
    tab: MainTab,
) {
    val target: Any =
        when (tab) {

            MainTab.HOME ->
                HomeDestination

            MainTab.EXPLORE ->
                ExploreDestination

            MainTab.MAP ->
                MapDestination

            MainTab.SAVED ->
                HomeDestination

            MainTab.PROFILE ->
                ProfileDestination
        }

    navigate(target) {
        launchSingleTop = true

        popUpTo(HomeDestination) {
            saveState = true
        }

        restoreState = true
    }
}


// =============================================================
// COMING SOON
// =============================================================

@Composable
private fun ComingSoon(
    name: String,
    links: List<Pair<String, () -> Unit>> = emptyList(),
) {

    Column(
        verticalArrangement =
            Arrangement.Center,

        horizontalAlignment =
            Alignment.CenterHorizontally,

        modifier =
            Modifier.fillMaxSize(),
    ) {

        Text(
            "$name screen coming soon",
            style =
                MaterialTheme.typography.titleMedium,
        )

        links.forEach { (text, onClick) ->

            TextButton(
                onClick = onClick,
            ) {
                Text(text)
            }
        }
    }
}