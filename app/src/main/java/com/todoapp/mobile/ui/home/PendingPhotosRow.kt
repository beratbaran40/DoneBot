// Detekt mis-flags trailing code after `?: return@rememberLauncherForActivityResult` as unreachable.
@file:Suppress("UnreachableCode")

package com.todoapp.mobile.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.common.components.ImageCropOverlay
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.theme.TDTheme

@Composable
fun PendingPhotosRow(
    pending: List<PendingPhoto>,
    onPick: (ByteArray, String) -> Unit,
    onRemoveAt: (Int) -> Unit,
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

    Column(Modifier.fillMaxWidth()) {
        TDText(
            text = stringResource(R.string.photos),
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Box(
                    modifier =
                    Modifier
                        .size(72.dp)
                        .clip(TDTheme.shapes.medium)
                        .background(TDTheme.colors.lightPending)
                        .clickable {
                            picker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = tdPainter(com.example.uikit.R.drawable.ic_plus),
                        contentDescription = stringResource(R.string.add_photo),
                        tint = TDTheme.colors.pendingGray,
                    )
                }
            }
            itemsIndexed(pending) { index, photo ->
                // Coil decodes from the byte array off the main thread and manages
                // recycling itself, so we no longer race a manual `bitmap.recycle()`
                // against in-flight draw frames (the previous bug:
                // `Canvas: trying to use a recycled bitmap`).
                Box(
                    modifier =
                    Modifier
                        .size(72.dp)
                        .clip(TDTheme.shapes.medium)
                        .background(TDTheme.colors.lightPending)
                        .clickable { onRemoveAt(index) },
                ) {
                    AsyncImage(
                        model = photo.bytes,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp),
                    )
                }
            }
        }
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
