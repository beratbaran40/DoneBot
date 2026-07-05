// Detekt mis-flags trailing code after `?: return@rememberLauncherForActivityResult` as unreachable.
@file:Suppress("UnreachableCode")

package com.todoapp.mobile.ui.groups.grouptaskdetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.common.components.ImageCropOverlay
import com.todoapp.mobile.ui.home.PendingPhoto
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TaskPhotosSection(
    photoUrls: List<String>,
    onPick: (ByteArray, String) -> Unit,
    onDelete: (Long) -> Unit,
    pendingUploads: List<PendingPhoto> = emptyList(),
    onCancelPending: (Int) -> Unit = {},
    onReport: ((String) -> Unit)? = null,
) {
    var cropSource by remember { mutableStateOf<String?>(null) }
    val picker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            // Route the picked image through the square crop overlay; onPick fires with cropped bytes.
            cropSource = uri.toString()
        }
    var viewerUrl by remember { mutableStateOf<String?>(null) }
    var viewerPhotoId by remember { mutableStateOf<Long?>(null) }
    var viewerRelativeUrl by remember { mutableStateOf<String?>(null) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TDText(
                text = stringResource(R.string.photos),
                style = TDTheme.typography.subheading2,
                color = TDTheme.colors.onBackground,
            )
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            TDText(
                text = "(${photoUrls.size + pendingUploads.size})",
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.gray,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                AddPhotoTile(onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                })
            }
            items(photoUrls, key = { it }) { relativeUrl ->
                val url = absoluteUrl(relativeUrl)
                val photoId = photoIdFromUrl(relativeUrl)
                PhotoTile(
                    url = url,
                    onLongPress = {
                        viewerUrl = url
                        viewerPhotoId = photoId
                        viewerRelativeUrl = relativeUrl
                    },
                )
            }
            itemsIndexed(pendingUploads) { index, pending ->
                PendingPhotoTile(
                    bytes = pending.bytes,
                    onCancel = { onCancelPending(index) },
                )
            }
        }
    }

    val currentViewerUrl = viewerUrl
    if (currentViewerUrl != null) {
        PhotoViewerDialog(
            url = currentViewerUrl,
            onDismiss = {
                viewerUrl = null
                viewerPhotoId = null
                viewerRelativeUrl = null
            },
            onDelete =
            viewerPhotoId?.let {
                {
                    onDelete(it)
                    viewerUrl = null
                    viewerPhotoId = null
                    viewerRelativeUrl = null
                }
            },
            onReport =
            if (onReport != null) {
                {
                    viewerRelativeUrl?.let(onReport)
                    viewerUrl = null
                    viewerPhotoId = null
                    viewerRelativeUrl = null
                }
            } else {
                null
            },
        )
    }

    cropSource?.let { src ->
        ImageCropOverlay(
            source = src,
            onCropped = { bytes ->
                onPick(bytes, "image/jpeg")
                cropSource = null
            },
            onDismiss = { cropSource = null },
        )
    }
}

@Composable
private fun PhotoViewerDialog(
    url: String,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onReport: (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color(0xEE000000),
                ),
        ) {
            // Image (tap anywhere on it to dismiss)
            Box(
                modifier =
                Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }

            // Report / Delete actions at the bottom (prominent, thumb-friendly)
            if (onReport != null || onDelete != null) {
                Box(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (onReport != null) {
                            PhotoActionButton(
                                iconRes = com.example.uikit.R.drawable.ic_error,
                                label = stringResource(R.string.report_photo),
                                background = TDTheme.colors.pendingGray,
                                onClick = onReport,
                            )
                        }
                        if (onDelete != null) {
                            PhotoActionButton(
                                iconRes = com.example.uikit.R.drawable.ic_delete,
                                label = stringResource(R.string.delete),
                                background = TDTheme.colors.crossRed,
                                onClick = onDelete,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoActionButton(
    iconRes: Int,
    label: String,
    background: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
        TDText(
            text = label,
            style = TDTheme.typography.subheading2,
            color = Color.White,
        )
    }
}

@Composable
private fun AddPhotoTile(onClick: () -> Unit) {
    Box(
        modifier =
        Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TDTheme.colors.lightPending)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(com.example.uikit.R.drawable.ic_plus),
            contentDescription = stringResource(R.string.add_photo),
            tint = TDTheme.colors.pendingGray,
        )
    }
}

@Composable
private fun PendingPhotoTile(
    bytes: ByteArray,
    onCancel: () -> Unit,
) {
    Box(modifier = Modifier.size(80.dp)) {
        Box(
            modifier =
            Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TDTheme.colors.lightPending),
        ) {
            AsyncImage(
                model = bytes,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(80.dp),
            )
        }
        Box(
            modifier =
            Modifier
                .size(24.dp)
                .offset(x = 4.dp, y = (-4).dp)
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(12.dp))
                .background(TDTheme.colors.crossRed)
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(com.example.uikit.R.drawable.ic_delete),
                contentDescription = stringResource(R.string.delete),
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun PhotoTile(
    url: String,
    onLongPress: () -> Unit,
) {
    Box(
        modifier =
        Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TDTheme.colors.lightPending)
            .pointerInput(url) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { onLongPress() },
                )
            },
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(80.dp),
        )
    }
}

private fun absoluteUrl(relative: String): String {
    val base = BuildConfig.BASE_URL.trimEnd('/')
    val path = relative.trimStart('/')
    return "$base/$path"
}

private fun photoIdFromUrl(url: String): Long? = url.trimEnd('/').substringAfterLast('/').toLongOrNull()
