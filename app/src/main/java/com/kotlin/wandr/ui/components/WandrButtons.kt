package com.kotlin.wandr.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sos
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotlin.wandr.ui.theme.WandrTheme

private val ButtonHeight = 52.dp

/** Icon + text, and the icon can go before or after the text ("Continue →"). */
@Composable
private fun ButtonContent(text: String, leadingIcon: ImageVector?, trailingIcon: ImageVector?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WandrTheme.spacing.sm)) {
        if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
        if (trailingIcon != null) Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}

/** Main action of a screen ("Accept Quest"). While [isLoading] it shows a spinner and cannot be pressed twice. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.medium,
        // While loading the button is disabled, but it keeps its green so the spinner is visible
        colors = if (isLoading) {
            ButtonDefaults.buttonColors(
                disabledContainerColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            ButtonDefaults.buttonColors()
        },
        modifier = modifier.fillMaxWidth().height(ButtonHeight),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        } else {
            ButtonContent(text, leadingIcon, trailingIcon)
        }
    }
}

/** Second option under the main one ("Give me another one"): white with a border. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, WandrTheme.colors.border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier.fillMaxWidth().height(ButtonHeight),
    ) {
        ButtonContent(text, leadingIcon, trailingIcon)
    }
}

/** Destructive or urgent action ("Need Help / SOS"). */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = WandrTheme.colors.dangerContainer,
            contentColor = WandrTheme.colors.danger,
        ),
        modifier = modifier.height(ButtonHeight),
    ) {
        ButtonContent(text, leadingIcon, null)
    }
}

/** Link-like action, e.g. "Don't have an account? Sign up" or "Skip for now". */
@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFCFAF6)
@Composable
private fun ButtonsPreview() {
    WandrTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(text = "Accept Quest", onClick = {}, leadingIcon = Icons.Rounded.CheckCircle)
            PrimaryButton(text = "Continue", onClick = {}, trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward)
            PrimaryButton(text = "Loading", onClick = {}, isLoading = true)
            SecondaryButton(text = "Give me another one", onClick = {}, leadingIcon = Icons.Rounded.Refresh)
            DangerButton(text = "Need Help / SOS", onClick = {}, leadingIcon = Icons.Rounded.Sos)
            LinkButton(text = "Skip for now", onClick = {})
        }
    }
}
