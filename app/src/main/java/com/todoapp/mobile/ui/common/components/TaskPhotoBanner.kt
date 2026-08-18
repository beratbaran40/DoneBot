// Detekt mis-flags trailing code after `?: return@rememberLauncherForActivityResult` as unreachable.
@file:Suppress("UnreachableCode")

package com.todoapp.mobile.ui.common.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.TaskType
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR

private val BANNER_HEIGHT = 180.dp

/**
 * Hero photo banner for a task detail screen. Tap the image to "arm" it (dark scrim + center edit
 * icon); tap the scrim to disarm. The edit icon opens a bottom sheet with Edit (re-crop the current
 * image), Replace (pick a new one → crop) and Remove. Edit/Replace both emit [onCropped] with the
 * cropped JPEG bytes; [onRemove] removes the cover. A [badge] (task type / priority) is overlaid in
 * the bottom-end corner.
 *
 * @param displayModel absolute photo URL ([String]) or pending [ByteArray] to render; "Edit"
 *  re-crops exactly this, so a just-cropped in-memory image can be re-cropped again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskPhotoBannerEditable(
    displayModel: Any?,
    onCropped: (ByteArray) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    badge: @Composable () -> Unit = {},
) {
    var armed by remember { mutableStateOf(false) }
    var sheetOpen by remember { mutableStateOf(false) }
    var cropSource by remember { mutableStateOf<Any?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val picker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            cropSource = uri.toString()
        }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BANNER_HEIGHT),
    ) {
        AsyncImage(
            model = displayModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clickable { armed = true },
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
        ) {
            badge()
        }

        AnimatedVisibility(
            visible = armed,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { armed = false },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { sheetOpen = true },
                ) {
                    Icon(
                        painter = tdPainter(UiKitR.drawable.ic_edit_task),
                        contentDescription = stringResource(R.string.task_banner_edit_cd),
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            containerColor = TDTheme.colors.surface,
        ) {
            if (displayModel != null) {
                BannerActionRow(
                    iconRes = UiKitR.drawable.ic_edit_task,
                    label = stringResource(R.string.task_banner_edit),
                    tint = TDTheme.colors.onBackground,
                    onClick = {
                        sheetOpen = false
                        cropSource = displayModel
                    },
                )
            }
            BannerActionRow(
                iconRes = UiKitR.drawable.ic_plus,
                label = stringResource(R.string.task_banner_replace),
                tint = TDTheme.colors.onBackground,
                onClick = {
                    sheetOpen = false
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
            BannerActionRow(
                iconRes = UiKitR.drawable.ic_delete,
                label = stringResource(R.string.task_banner_remove),
                tint = TDTheme.colors.crossRed,
                onClick = {
                    sheetOpen = false
                    armed = false
                    onRemove()
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    cropSource?.let { src ->
        ImageCropOverlay(
            source = src,
            onCropped = { bytes ->
                cropSource = null
                armed = false
                onCropped(bytes)
            },
            onDismiss = { cropSource = null },
        )
    }
}

@Composable
private fun BannerActionRow(
    iconRes: Int,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = tdPainter(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        TDText(
            text = label,
            style = TDTheme.typography.subheading2,
            color = tint,
        )
    }
}

@TDPreview
@Composable
private fun TaskPhotoBannerEditablePreview() {
    TDTheme {
        Box(modifier = Modifier.background(TDTheme.colors.lightPending)) {
            TaskPhotoBannerEditable(
                displayModel = "https://example.com/photo.jpg",
                onCropped = {},
                onRemove = {},
                badge = { TaskTypeBadge(TaskType.STAGED) },
            )
        }
    }
}

@TDPreview
@Composable
private fun TaskPhotoBannerNoBadgePreview() {
    TDTheme {
        Box(modifier = Modifier.background(TDTheme.colors.lightPending)) {
            TaskPhotoBannerEditable(
                displayModel = "https://example.com/cover.jpg",
                onCropped = {},
                onRemove = {},
            )
        }
    }
}
