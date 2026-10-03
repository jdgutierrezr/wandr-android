package com.kotlin.wandr.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kotlin.wandr.BuildConfig
import com.kotlin.wandr.ui.catalog.ComponentCatalog
import com.kotlin.wandr.ui.feature.auth.AfterAuthDestination
import com.kotlin.wandr.ui.feature.auth.LoginRoute
import com.kotlin.wandr.ui.feature.auth.SignUpRoute
import com.kotlin.wandr.ui.feature.events.ExploreRoute
import kotlinx.serialization.Serializable

/** Every screen of the app is a route. Type-safe: arguments are properties of the class. */
@Serializable data object LoginDestination
@Serializable data object SignUpDestination
@Serializable data object OnboardingDestination
@Serializable data object HomeDestination
@Serializable data object ExploreDestination
@Serializable data object CatalogDestination

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
        composable<HomeDestination> {
            com.kotlin.wandr.ui.feature.home.HomeRoute(
                onExplore = { navController.navigate(ExploreDestination) },
            )
        }
        composable<ExploreDestination> {
            ExploreRoute(
                onHome = { navController.popBackStack() },
            )
        }
        composable<CatalogDestination> { ComponentCatalog(onBack = { navController.popBackStack() }) }
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
private fun ComingSoon(name: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text("$name screen coming soon", style = MaterialTheme.typography.titleMedium)
    }
}
