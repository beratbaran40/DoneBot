package com.todoapp.mobile.ui.groups.groupsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.todoapp.mobile.BuildConfig
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

@Composable
fun GroupAvatar(
    avatarUrl: String?,
    avatarVersion: Long,
    name: String,
    isAdmin: Boolean,
    onPickAvatar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val absoluteUrl =
        avatarUrl?.let {
            val base = BuildConfig.BASE_URL.trimEnd('/')
            val path = it.trimStart('/')
            "$base/$path?v=$avatarVersion"
        }
    Box(
        modifier =
        modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(TDTheme.colors.lightPending)
            .then(
                if (isAdmin) {
                    Modifier.clickable(onClick = onPickAvatar)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (absoluteUrl != null) {
            AsyncImage(
                model = absoluteUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(96.dp),
            )
        } else {
            TDText(
                text =
                name
                    .split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .uppercase(),
                style = TDTheme.typography.heading4,
                color = TDTheme.colors.pendingGray,
            )
        }
    }
}
