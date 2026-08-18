package com.todoapp.uikit.components

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.rememberKitArtPainter
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.modifier.crtScreen
import com.todoapp.uikit.previews.TDPreviewDialog
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun TDOverlayDailyPlanNotificationCard(
    isVisible: Boolean,
    onOpenApp: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoDismissMillis: Int = 10_000,
    onDrag: (deltaX: Float, deltaY: Float) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {},
) {
    if (!isVisible) return

    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val startTime = SystemClock.elapsedRealtime()
        while (isActive) {
            delay(16L)
            val elapsed = SystemClock.elapsedRealtime() - startTime
            progress = (elapsed.toFloat() / autoDismissMillis.toFloat()).coerceIn(0f, 1f)
            if (progress >= 1f) {
                onDismiss()
                break
            }
        }
    }

    val overlayTextColor = TDTheme.colors.onBackground
    val overlaySurfaceColor = TDTheme.colors.background

    Surface(
        modifier =
        modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onDragEnd() },
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }.padding(horizontal = 16.dp, vertical = 16.dp)
            .widthIn(max = 520.dp)
            .clickable { onOpenApp() },
        shape = tdCorner(22.dp),
        tonalElevation = 6.dp,
        color = overlaySurfaceColor,
        contentColor = overlayTextColor,
    ) {
        Column {
            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 22.dp))
                    .background(TDTheme.colors.pendingGray.copy(alpha = 0.2f)),
            ) {
                Box(
                    modifier =
                    Modifier
                        .fillMaxWidth(fraction = progress)
                        .height(3.dp)
                        .background(TDTheme.colors.crossRed),
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                    Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(TDTheme.colors.background.copy(alpha = 0.14f))
                        .crtScreen(diameter = 70.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = rememberKitArtPainter(
                            tdPainter(
                                if (TDTheme.isDark) {
                                    R.drawable.img_donebot_overlay_notification_light
                                } else {
                                    R.drawable.img_donebot_overlay_notification_dark
                                },
                            ),
                            70.dp,
                        ),
                        contentDescription = "Logo",
                        modifier = Modifier.size(70.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.daily_plan_notification_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = overlayTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.daily_plan_notification_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = overlayTextColor.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Box(
                    modifier =
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = tdPainter(id = R.drawable.ic_close),
                        contentDescription = "Close",
                        tint = overlayTextColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@TDPreviewDialog
@Composable
fun TDOverlayDailyPlanNotificationCardPreview() {
    TDTheme {
        TDOverlayDailyPlanNotificationCard(
            isVisible = true,
            onOpenApp = {},
            onDismiss = {},
        )
    }
}
