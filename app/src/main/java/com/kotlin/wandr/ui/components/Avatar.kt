package com.kotlin.wandr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kotlin.wandr.ui.theme.OnlineGreen
import com.kotlin.wandr.ui.theme.WandrTheme

/**
 * Round profile photo. Shows the person's initials underneath, so there is always something
 * visible while the photo loads, if it fails, or if the user has no photo.
 * [isOnline] draws the small green dot of "Friends on Quest".
 */
@Composable
fun Avatar(
    name: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    isOnline: Boolean = false,
    showRing: Boolean = false,
) {
    Box(modifier = modifier.size(size)) {
        val ring = if (showRing) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .then(ring)
                .clip(CircleShape)
                .background(WandrTheme.colors.successContainer),
        ) {
            Text(
                text = initials(name),
                style = TextStyle(fontSize = (size.value * 0.36f).sp),
                color = MaterialTheme.colorScheme.primary,
            )
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(size),
                )
            }
        }
        if (isOnline) {
            Box(
                Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .background(OnlineGreen, CircleShape)
            )
        }
    }
}

/** Overlapping avatars plus "+12", as in "Mateo & 2 friends are nearby". */
@Composable
fun AvatarStack(
    people: List<Pair<String, String?>>,
    modifier: Modifier = Modifier,
    extraCount: Int = 0,
    size: Dp = 28.dp,
    maxVisible: Int = 3,
) {
    val overlap = size * 0.3f
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(-overlap)) {
        people.take(maxVisible).forEach { (name, url) ->
            Avatar(
                name = name,
                imageUrl = url,
                size = size,
                modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
            )
        }
        val hidden = extraCount + (people.size - maxVisible).coerceAtLeast(0)
        if (hidden > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .background(WandrTheme.colors.locked, CircleShape),
            ) {
                Text("+$hidden", style = MaterialTheme.typography.labelSmall, color = WandrTheme.colors.onLocked)
            }
        }
    }
}

private fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
