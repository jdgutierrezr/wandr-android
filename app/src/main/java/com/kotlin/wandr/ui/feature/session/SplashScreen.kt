package com.kotlin.wandr.ui.feature.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kotlin.wandr.domain.model.SessionState
import com.kotlin.wandr.ui.theme.WandrTheme

/**
 * First screen of the app. Shows the brand while Supabase restores the saved session, then
 * opens Home (already logged in) or Login. The decision is [splashRoute].
 */
@Composable
fun SplashRoute(
    sessionState: SessionState,
    onSignedIn: () -> Unit,
    onSignedOut: () -> Unit,
) {
    LaunchedEffect(sessionState) {
        when (sessionState.splashRoute()) {
            SessionRoute.HOME -> onSignedIn()
            SessionRoute.LOGIN -> onSignedOut()
            null -> Unit // still loading: keep showing the splash
        }
    }
    SplashScreen()
}

@Composable
fun SplashScreen() {
    Column(
        verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(WandrTheme.colors.heroGradient),
    ) {
        Text("Wandr", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text(
            "Restoring your session…",
            style = MaterialTheme.typography.bodyMedium,
            color = WandrTheme.colors.textSecondary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    WandrTheme { SplashScreen() }
}
