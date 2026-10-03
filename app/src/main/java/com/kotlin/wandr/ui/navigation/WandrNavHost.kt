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
import com.kotlin.wandr.ui.feature.profile.ProfileRoute
import com.kotlin.wandr.ui.components.LinkButton
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

/** Every screen of the app is a route. Type-safe: arguments are properties of the class. */
/** First screen: waits for the saved session and opens Home or Login (session gate). */
@Serializable data object SplashDestination
@Serializable data object LoginDestination
@Serializable data object SignUpDestination
@Serializable data object OnboardingDestination
@Serializable data object HomeDestination
@Serializable data object ExploreDestination
@Serializable data object MapDestination
@Serializable data object CatalogDestination
@Serializable data object ProfileDestination

// Quest / Progress (BQ8)
/** `questId` is read by QuestDetailViewModel through SavedStateHandle (QUEST_ID_ARG). */
@Serializable data class QuestDetailDestination(val questId: String)
@Serializable data class ActiveQuestDestination(val questId: String)
@Serializable data object ActiveQuestsDestination
@Serializable data object QuestDropoffDestination

/**
 * The navigation graph. To add a screen:
 * 1. Create a `@Serializable` destination above.
 * 2. Add a `composable<ThatDestination> { ThatRoute(...) }` block below.
 * 3. Navigate to it with `navController.navigate(ThatDestination)`.
 */
@Composable
fun WandrNavHost(
    map: WandrMap,
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()

    // Session gate: if the session ends anywhere in the app (logout, expired or revoked token),
    // go back to Login. The splash and the auth screens handle their own navigation.
    LaunchedEffect(sessionState) {
        if (sessionState == SessionState.SignedOut && navController.isInsideApp()) {
            navController.leaveToLogin()
        }
    }

    NavHost(navController = navController, startDestination = SplashDestination) {

        composable<SplashDestination> {
            SplashRoute(
                sessionState = sessionState,
                // Saved session restored: no password needed
                onSignedIn = { navController.leaveSplashTo(HomeDestination) },
                onSignedOut = { navController.leaveSplashTo(LoginDestination) },
            )
        }

        composable<LoginDestination> {
            LoginRoute(
                onLoggedIn = { navController.leaveAuthTo(it) },
                onGoToSignUp = { navController.navigate(SignUpDestination) },
                // Only in debug builds: the component catalog for the team
                onOpenCatalog = if (BuildConfig.DEBUG) ({ navController.navigate(CatalogDestination) }) else null,
            )
        }

        composable<SignUpDestination> {
            SignUpRoute(
                onSignedUp = { navController.leaveAuthTo(it) },
                // Go back instead of stacking a second Login on top
                onGoToLogin = { navController.popBackStack() },
            )
        }

        composable<MapDestination> {
            MapRoute(map = map, onBack = { navController.popBackStack() })
        }

        // Placeholders until those screens exist
        val shortcutLinks = listOf(
            "Discovery map" to { navController.navigate(MapDestination) },
            "My quests in progress" to { navController.navigate(ActiveQuestsDestination) },
            "Abandonment funnel (BQ8)" to { navController.navigate(QuestDropoffDestination) },
        )
        composable<OnboardingDestination> {
            ComingSoon("Onboarding", links = shortcutLinks)
        }
        // TODO: remove these links when Home has its own navigation
        composable<HomeDestination> {
            ComingSoon(
                "Home",
                links = listOf(
                    "Open Profile" to { navController.navigate(ProfileDestination) },
                    "Discovery map" to { navController.navigate(MapDestination) },
                    "My quests in progress" to { navController.navigate(ActiveQuestsDestination) },
                    "Abandonment funnel (BQ8)" to { navController.navigate(QuestDropoffDestination) },
                ),
            )
        }
        composable<CatalogDestination> { ComponentCatalog(onBack = { navController.popBackStack() }) }

        composable<ProfileDestination> {
            ProfileRoute(
                onSignedOut = { navController.leaveToLogin() },
                onSelectTab = { tab -> navController.openTab(tab) },
                // BQ8 is Type 3 (features analysis): an internal tool for the team, not for users.
                // Only debug builds show the entry; the release app never does.
                onOpenTeamAnalytics = if (BuildConfig.DEBUG) ({ navController.navigate(QuestDropoffDestination) }) else null,
            )
        }

        // ---------- Quest / Progress (BQ8) ----------

        composable<QuestDetailDestination> {
            QuestDetailRoute(
                onBack = { navController.popBackStack() },
                // Back from the tracker returns to where the user found the quest, not to its details
                onOpenTracker = { questId ->
                    navController.navigate(ActiveQuestDestination(questId)) {
                        popUpTo<QuestDetailDestination> { inclusive = true }
                    }
                },
            )
        }

        composable<ActiveQuestDestination> { entry ->
            ActiveQuestRoute(
                questId = entry.toRoute<ActiveQuestDestination>().questId,
                onBack = { navController.popBackStack() },
            )
        }

        composable<ActiveQuestsDestination> {
            ActiveQuestsRoute(
                onBack = { navController.popBackStack() },
                onOpenQuest = { questId -> navController.navigate(ActiveQuestDestination(questId)) },
            )
        }

        composable<QuestDropoffDestination> {
            QuestDropoffRoute(
                onBack = { navController.popBackStack() },
                onOpenQuest = { questId -> navController.navigate(QuestDetailDestination(questId)) },
            )
        }
    }
}

/** After logging in, Back must not return to Login / Sign up, so the auth screens are removed. */
private fun NavHostController.leaveAuthTo(destination: AfterAuthDestination) {
    val target: Any = when (destination) {
        AfterAuthDestination.ONBOARDING -> OnboardingDestination
        AfterAuthDestination.HOME -> HomeDestination
    }
    navigate(target) {
        popUpTo(LoginDestination) { inclusive = true }
    }
}

/** The splash is shown only once: Back must not return to it. */
private fun NavHostController.leaveSplashTo(destination: Any) {
    navigate(destination) {
        popUpTo(SplashDestination) { inclusive = true }
    }
}

/** True on any screen that needs a session (not the splash, Login or Sign up). */
private fun NavHostController.isInsideApp(): Boolean {
    val current = currentBackStackEntry?.destination ?: return false
    return !current.hasRoute<SplashDestination>() &&
        !current.hasRoute<LoginDestination>() &&
        !current.hasRoute<SignUpDestination>()
}

/** After signing out, Back must not return to the app: the whole back stack is removed. */
private fun NavHostController.leaveToLogin() {
    navigate(LoginDestination) {
        popUpTo(graph.id) { inclusive = true }
    }
}

/**
 * Bottom bar tabs. Each tab replaces the current one instead of stacking.
 * Tabs whose screen does not exist yet fall back to Home.
 */
private fun NavHostController.openTab(tab: MainTab) {
    val target: Any = when (tab) {
        MainTab.PROFILE -> ProfileDestination
        MainTab.MAP -> MapDestination
        else -> HomeDestination
    }
    navigate(target) {
        launchSingleTop = true
        popUpTo(HomeDestination)
    }
}

@Composable
private fun ComingSoon(name: String, links: List<Pair<String, () -> Unit>> = emptyList()) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text("$name screen coming soon", style = MaterialTheme.typography.titleMedium)
        links.forEach { (text, onClick) -> TextButton(onClick = onClick) { Text(text) } }
    }
}
