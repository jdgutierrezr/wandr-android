package com.kotlin.wandr.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kotlin.wandr.ui.theme.WandrTheme

/**
 * Text field with the Wandr look: white, rounded, thin border that turns green on focus.
 * Stateless: the value lives in the ViewModel and every keystroke goes back through [onValueChange].
 */
@Composable
fun WandrTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        singleLine = true,
        enabled = enabled,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        shape = MaterialTheme.shapes.medium,
        colors = wandrFieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Password field with an eye button. Whether it is visible is UI-only state, so it lives here. */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Password",
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    var isVisible by rememberSaveable { mutableStateOf(false) }
    WandrTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        leadingIcon = Icons.Outlined.Lock,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        onImeAction = onImeAction,
        enabled = enabled,
        supportingText = supportingText,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(
                    if (isVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (isVisible) "Hide password" else "Show password",
                )
            }
        },
    )
}

/** "Search quests, friends, places…" bar from the Discovery Map. */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search quests, friends, places…",
    onVoiceSearch: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = onVoiceSearch?.let {
            { IconButton(onClick = it) { Icon(Icons.Rounded.Mic, contentDescription = "Voice search") } }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = MaterialTheme.shapes.medium,
        colors = wandrFieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun wandrFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = WandrTheme.colors.locked,
    unfocusedBorderColor = WandrTheme.colors.border,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = WandrTheme.colors.textSecondary,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = WandrTheme.colors.textSecondary,
)

@Preview(showBackground = true, backgroundColor = 0xFFFCFAF6)
@Composable
private fun TextFieldsPreview() {
    WandrTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchField(value = "", onValueChange = {}, onVoiceSearch = {})
            WandrTextField(value = "valentina@example.com", onValueChange = {}, label = "Email", leadingIcon = Icons.Outlined.Email)
            PasswordTextField(value = "password123", onValueChange = {})
        }
    }
}
