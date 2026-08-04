package com.todoapp.mobile.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.uikit.image.rememberPixelPainter
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BADGE_SIZE_DP = 132
private const val LOGO_SIZE_DP = 150
private const val LOGO_SCALE_DURATION_MS = 500
private const val LOGO_FADE_DURATION_MS = 400
private const val CHECK_DRAW_DURATION_MS = 800
private const val RING_DRAW_DURATION_MS = 600
private const val POST_ANIMATION_HOLD_MS = 200L

@Composable
fun TDSplashScreen(onAnimationComplete: () -> Unit = {}) {
    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }
    val ringProgress = remember { Animatable(0f) }

    val checkColor = TDTheme.colors.mediumGreen
    val badgeColor = Color.Transparent

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                logoScale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(LOGO_SCALE_DURATION_MS, easing = EaseOutBack),
                )
            }
            launch {
                logoAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(LOGO_FADE_DURATION_MS),
                )
            }
        }
        checkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(CHECK_DRAW_DURATION_MS, easing = FastOutSlowInEasing),
        )
        ringProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(RING_DRAW_DURATION_MS, easing = FastOutSlowInEasing),
        )
        delay(POST_ANIMATION_HOLD_MS)
        onAnimationComplete()
    }

    val animatedCheckPath = remember { Path() }

    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(TDTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
            Modifier
                .size(BADGE_SIZE_DP.dp)
                .clip(CircleShape)
                .background(badgeColor),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = rememberPixelPainter(painterResource(R.drawable.img_splash), LOGO_SIZE_DP.dp),
                contentDescription = null,
                modifier =
                Modifier
                    .size(LOGO_SIZE_DP.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
            )
            Canvas(modifier = Modifier.size(BADGE_SIZE_DP.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val checkPath =
                    Path().apply {
                        moveTo(center.x + (-22).dp.toPx(), center.y)
                        lineTo(center.x + (-6).dp.toPx(), center.y + 16.dp.toPx())
                        lineTo(center.x + 22.dp.toPx(), center.y + (-14).dp.toPx())
                    }
                val pathMeasure = PathMeasure().apply { setPath(checkPath, false) }

                animatedCheckPath.reset()
                pathMeasure.getSegment(
                    startDistance = 0f,
                    stopDistance = pathMeasure.length * checkProgress.value,
                    destination = animatedCheckPath,
                    startWithMoveTo = true,
                )
                drawPath(
                    path = animatedCheckPath,
                    color = checkColor,
                    style =
                    Stroke(
                        width = 5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                if (ringProgress.value > 0f) {
                    val strokePx = 2.dp.toPx()
                    drawArc(
                        color = checkColor,
                        startAngle = -90f,
                        sweepAngle = 360f * ringProgress.value,
                        useCenter = false,
                        topLeft = Offset(strokePx / 2f, strokePx / 2f),
                        size = Size(size.width - strokePx, size.height - strokePx),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                }
            }
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun TDSplashScreenPreview() {
    TDTheme {
        TDSplashScreen(onAnimationComplete = {})
    }
}
