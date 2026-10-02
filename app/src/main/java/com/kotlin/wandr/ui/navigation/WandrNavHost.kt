package com.kotlin.wandr.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.kotlin.wandr.BuildConfig
import com.kotlin.wandr.ui.catalog.ComponentCatalog
import com.kotlin.wandr.ui.components.LinkButton
import com.kotlin.wandr.ui.components.map.WandrMap
import com.kotlin.wandr.ui.feature.auth.AfterAuthDestination
import com.kotlin.wandr.ui.feature.auth.LoginRoute
import com.kotlin.wandr.ui.feature.auth.SignUpRoute
import com.kotlin.wandr.ui.feature.map.MapRoute
import com.kotlin.wandr.ui.feature.analytics.QuestDropoffRoute
import com.kotlin.wandr.ui.feature.quest.ActiveQuestRoute
import com.kotlin.wandr.ui.feature.quest.ActiveQuestsRoute
import com.kotlin.wandr.ui.feature.quest.QuestDetailRoute
import kotlinx.serialization.Serializable

/** Every screen of the app is a route. Type-safe: arguments are properties of the class. */
@Serializable data object LoginDestination
@Serializable data object SignUpDestination
@Serializable data object OnboardingDestination
@Serializable data object HomeDestination
@Serializable data object MapDestination
@Serializable data object CatalogDestination

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
fun WandrNavHost(map: WandrMap, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = LoginDestination) {

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
        composable<OnboardingDestination> { ComingSoon("Onboarding") }
        // TODO: remove these links when Home has its own navigation
        composable<HomeDestination> {
            ComingSoon(
                "Home",
                links = listOf(
                    "My quests in progress" to { navController.navigate(ActiveQuestsDestination) },
                    "Abandonment funnel (BQ8)" to { navController.navigate(QuestDropoffDestination) },
                ),
            )
        }
        composable<CatalogDestination> { ComponentCatalog(onBack = { navController.popBackStack() }) }

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
