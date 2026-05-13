// Detekt mis-flags trailing code after `?: return@rememberLauncherForActivityResult` as unreachable.
@file:Suppress("UnreachableCode")

package com.todoapp.mobile.ui.groups.groupsettings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
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
    onAvatarPicked: (ByteArray, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val picker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            val cr = context.contentResolver
            val mime = cr.getType(uri) ?: "image/jpeg"
            val bytes =
                runCatching { cr.openInputStream(uri)?.use { it.readBytes() } }
                    .getOrNull() ?: return@rememberLauncherForActivityResult
            onAvatarPicked(bytes, mime)
        }
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
                    Modifier.clickable {
                        picker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }
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
