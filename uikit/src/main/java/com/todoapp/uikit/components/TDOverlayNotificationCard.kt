package com.todoapp.uikit.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uikit.R
import com.todoapp.uikit.image.rememberPixelPainter
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TDOverlayNotificationCard(
    modifier: Modifier = Modifier,
    message: String,
    show: Boolean,
    minutesBefore: Long = 0,
    onDismissClick: () -> Unit = {},
    onOpenClick: () -> Unit = {},
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dragJobRef = remember { arrayOfNulls<kotlinx.coroutines.Job>(1) }

    LaunchedEffect(show) {
        if (show) offsetX.snapTo(0f)
    }

    AnimatedVisibility(
        visible = show,
        enter =
        slideInVertically(
            initialOffsetY = { fullHeight -> -fullHeight },
        ) + fadeIn(),
        exit =
        slideOutVertically(
            targetOffsetY = { fullHeight -> -fullHeight },
        ) + fadeOut(),
        modifier =
        modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
    ) {
        NotificationCardContent(
            message = message,
            onDismissClick = onDismissClick,
            onOpenClick = onOpenClick,
            minutesBefore = minutesBefore,
            modifier =
            Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val threshold = size.width * 0.35f
                                if (abs(offsetX.value) > threshold) {
                                    val target = if (offsetX.value > 0) size.width.toFloat() else -size.width.toFloat()
                                    offsetX.animateTo(target, animationSpec = tween(200))
                                    onDismissClick()
                                } else {
                                    offsetX.animateTo(0f, animationSpec = tween(300))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f, animationSpec = tween(300)) }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        dragJobRef[0]?.cancel()
                        dragJobRef[0] = scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                    }
                },
        )
    }
}

@Composable
private fun NotificationCardContent(
    modifier: Modifier = Modifier,
    message: String,
    minutesBefore: Long = 0,
    onDismissClick: () -> Unit = {},
    onOpenClick: () -> Unit = {},
) {
    Surface(
        onClick = onOpenClick,
        modifier = modifier.fillMaxWidth(),
        shape = tdCorner(28.dp),
        color = TDTheme.colors.background,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, TDTheme.colors.onSurface.copy(alpha = 0.05f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Image(
                        painter = rememberPixelPainter(
                            tdPainter(
                                if (TDTheme.isDark) {
                                    R.drawable.img_donebot_alarm_reminder_light
                                } else {
                                    R.drawable.img_donebot_alarm_reminder_dark
                                },
                            ),
                            100.dp,
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                    )

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TDText(
                                text = stringResource(id = R.string.overlay_notification_reminder),
                                style =
                                TDTheme.typography.subheading1.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.4.sp,
                                ),
                                color = TDTheme.colors.pendingGray,
                            )
                            TDText(
                                text =
                                if (minutesBefore == 0L) {
                                    stringResource(id = R.string.overlay_notification_now)
                                } else {
                                    stringResource(
                                        id = R.string.overlay_notification_minutes_ago,
                                        minutesBefore.toInt(),
                                    )
                                },
                                style = TDTheme.typography.subheading2,
                                color = TDTheme.colors.gray.copy(alpha = 0.8f),
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        TDText(
                            text = message,
                            style = TDTheme.typography.heading3,
                            color = TDTheme.colors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        TDText(
                            text = stringResource(id = R.string.overlay_notification_tap_to_open),
                            style = TDTheme.typography.heading7,
                            color = TDTheme.colors.gray.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(TDTheme.colors.onSurface.copy(alpha = 0.06f)),
            )

            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { onDismissClick() },
                contentAlignment = Alignment.Center,
            ) {
                TDText(
                    text = stringResource(id = R.string.overlay_notification_dismiss),
                    style = TDTheme.typography.heading7,
                    color = TDTheme.colors.crossRed,
                )
            }
        }
    }
}

@TDPreview
@Composable
fun OverlayNotificationCardDarkPreview() {
    TDTheme(darkTheme = true) {
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .background(TDTheme.colors.background),
        ) {
            TDOverlayNotificationCard(
                message = "Go to the gym and do cardio",
                show = true,
            )
        }
    }
}

@TDPreview
@Composable
fun OverlayNotificationCardLightPreview() {
    TDTheme(darkTheme = false) {
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .background(TDTheme.colors.background),
        ) {
            TDOverlayNotificationCard(
                message = "Go to the gym and do cardio",
                show = true,
            )
        }
    }
}

@TDPreview
@Composable
fun NotificationCardContentPreview() {
    TDTheme {
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .background(TDTheme.colors.background)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            NotificationCardContent(message = "Meeting with the team at 2 PM")
        }
    }
}

@TDPreview
@Composable
private fun NotificationCardContent5MinPreview() {
    TDTheme {
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .background(TDTheme.colors.background)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            NotificationCardContent(
                message = "Pick up groceries on the way home",
                minutesBefore = 5,
            )
        }
    }
}

@TDPreview
@Composable
private fun NotificationCardContentLongMessagePreview() {
    TDTheme {
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .background(TDTheme.colors.background)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            NotificationCardContent(
                message = "Submit the quarterly financial review and reconcile the expense reports for the Berlin trip",
                minutesBefore = 15,
            )
        }
    }
}
