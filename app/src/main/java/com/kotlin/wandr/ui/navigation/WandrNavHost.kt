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
import com.kotlin.wandr.BuildConfig
import com.kotlin.wandr.ui.catalog.ComponentCatalog
import com.kotlin.wandr.ui.components.MainTab
import com.kotlin.wandr.ui.feature.auth.AfterAuthDestination
import com.kotlin.wandr.ui.feature.auth.LoginRoute
import com.kotlin.wandr.ui.feature.auth.SignUpRoute
import com.kotlin.wandr.ui.feature.profile.ProfileRoute
import kotlinx.serialization.Serializable

/** Every screen of the app is a route. Type-safe: arguments are properties of the class. */
@Serializable data object LoginDestination
@Serializable data object SignUpDestination
@Serializable data object OnboardingDestination
@Serializable data object HomeDestination
@Serializable data object CatalogDestination
@Serializable data object ProfileDestination

/**
 * The navigation graph. To add a screen:
 * 1. Create a `@Serializable` destination above.
 * 2. Add a `composable<ThatDestination> { ThatRoute(...) }` block below.
 * 3. Navigate to it with `navController.navigate(ThatDestination)`.
 */
@Composable
fun WandrNavHost(navController: NavHostController = rememberNavController()) {
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

        // Placeholders until those screens exist
        composable<OnboardingDestination> { ComingSoon("Onboarding") }
        // TODO: remove the Profile link when Home has the bottom bar
        composable<HomeDestination> {
            ComingSoon("Home", linkText = "Open Profile", onLink = { navController.navigate(ProfileDestination) })
        }
        composable<CatalogDestination> { ComponentCatalog(onBack = { navController.popBackStack() }) }

        composable<ProfileDestination> {
            ProfileRoute(
                onSignedOut = { navController.leaveToLogin() },
                onSelectTab = { tab -> navController.openTab(tab) },
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
        else -> HomeDestination
    }
    navigate(target) {
        launchSingleTop = true
        popUpTo(HomeDestination)
    }
}

@Composable
private fun ComingSoon(name: String, linkText: String? = null, onLink: () -> Unit = {}) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text("$name screen coming soon", style = MaterialTheme.typography.titleMedium)
        if (linkText != null) TextButton(onClick = onLink) { Text(linkText) }
    }
}
