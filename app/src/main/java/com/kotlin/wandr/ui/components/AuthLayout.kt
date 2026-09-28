package com.kotlin.wandr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kotlin.wandr.ui.theme.WandrTheme

/**
 * Shared frame of the Login and Sign up screens, in the style of the onboarding mockup:
 * soft green gradient, header, the form inside a white card ([form]) and actions below ([footer]).
 * Scrolls and moves up with the keyboard (imePadding), so small phones can reach the button.
 */
@Composable
fun AuthLayout(
    eyebrow: String,
    title: String,
    subtitle: String,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    form: @Composable ColumnScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = modifier.background(WandrTheme.colors.heroGradient),
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.xl),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WandrTheme.spacing.screen, vertical = WandrTheme.spacing.xxl),
        ) {
            AuthHeader(eyebrow = eyebrow, title = title, subtitle = subtitle)
            WandrCard(modifier = Modifier.fillMaxWidth(), contentPadding = WandrTheme.spacing.xl) {
                Column(verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.lg), content = form)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm),
                content = footer,
            )
        }
    }
}

@Composable
fun AuthHeader(eyebrow: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm),
        modifier = modifier,
    ) {
        // TODO: when the bear is in res/drawable, replace the compass with:
        // Image(painterResource(R.drawable.wandr_bear), contentDescription = null, Modifier.size(96.dp))
        Box(contentAlignment = Alignment.Center) {
            IconCircle(Icons.Rounded.Explore, size = 88.dp, containerColor = MaterialTheme.colorScheme.surface)
        }
        StatusBadge(text = eyebrow.uppercase(), modifier = Modifier.padding(top = WandrTheme.spacing.sm))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = WandrTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
