package com.todoapp.uikit.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.previews.TDPreviewWide
import com.todoapp.uikit.theme.TDTheme
import kotlin.math.roundToInt

@Composable
fun TDWeeklyCircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float,
    inProgress: Float = 0f,
    color: Color = TDTheme.colors.mediumGreen,
    inProgressColor: Color = TDTheme.colors.pendingGray,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
    animationSpec: AnimationSpec<Float> = ProgressIndicatorDefaults.ProgressAnimationSpec,
) {
    val targetCompleted = progress.coerceIn(0f, 1f)
    val targetInProgress = inProgress.coerceIn(0f, 1f - targetCompleted)
    val animatedCompleted by animateFloatAsState(
        targetValue = targetCompleted,
        animationSpec = animationSpec,
        label = "completedProgress",
    )
    val animatedInProgress by animateFloatAsState(
        targetValue = targetInProgress,
        animationSpec = animationSpec,
        label = "inProgressArc",
    )

    BoxWithConstraints(
        modifier = modifier,
    ) {
        val indicatorSize = maxWidth * 0.35f

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier,
                contentAlignment = Alignment.Center,
            ) {
                TDText(
                    text = "%${(100 * targetCompleted).roundToInt()}",
                    color = TDTheme.colors.onBackground,
                    style = TDTheme.typography.heading7,
                    textAlign = TextAlign.Center,
                )

                Canvas(modifier = Modifier.size(indicatorSize)) {
                    val stroke = Stroke(width = strokeWidth.toPx(), cap = strokeCap)
                    val diameter = size.minDimension
                    val arcSize = Size(diameter, diameter)

                    val completedSweep = 360f * animatedCompleted
                    val inProgressSweep = 360f * animatedInProgress

                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = completedSweep,
                        useCenter = false,
                        size = arcSize,
                        style = stroke,
                    )

                    drawArc(
                        color = inProgressColor,
                        startAngle = -90f,
                        sweepAngle = -inProgressSweep,
                        useCenter = false,
                        size = arcSize,
                        style = stroke,
                    )
                }
            }

            Spacer(modifier = Modifier.width(36.dp))

            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.Start,
            ) {
                TDText(
                    text = stringResource(id = R.string.activity_screen_weekly_progress_text),
                    color = TDTheme.colors.onBackground,
                    style = TDTheme.typography.heading2,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                LegendColoredBoxItem(
                    color = TDTheme.colors.mediumGreen,
                    text = stringResource(id = R.string.activity_screen_weekly_progress_legend_complete_text),
                )

                Spacer(modifier = Modifier.height(4.dp))

                LegendColoredBoxItem(
                    color = TDTheme.colors.pendingGray,
                    text = stringResource(id = R.string.activity_screen_weekly_progress_legend_in_progress_text),
                )
            }
        }
    }
}

@Composable
fun LegendColoredBoxItem(
    color: Color,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
            Modifier
                .size(12.dp)
                .background(color),
        )

        Spacer(modifier = Modifier.width(8.dp))

        TDText(
            text = text,
            color = TDTheme.colors.onBackground,
            style = TDTheme.typography.heading7,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@TDPreviewWide
@Composable
fun TDWeeklyCircularProgressIndicatorPreview(modifier: Modifier = Modifier) {
    TDTheme {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            TDWeeklyCircularProgressIndicator(
                modifier = modifier.fillMaxWidth(),
                progress = 0.65f,
                inProgress = 0.35f,
                color = TDTheme.colors.mediumGreen,
                inProgressColor = TDTheme.colors.pendingGray,
                strokeWidth = 14.dp,
                strokeCap = Butt,
            )
        }
    }
}

@TDPreviewWide
@Composable
private fun TDWeeklyCircularProgressIndicatorStatesPreview() {
    TDTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Nothing completed yet, some in progress
            TDWeeklyCircularProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = 0f,
                inProgress = 0.4f,
                strokeWidth = 14.dp,
                strokeCap = Butt,
            )
            // Half completed
            TDWeeklyCircularProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = 0.5f,
                inProgress = 0.25f,
                strokeWidth = 14.dp,
                strokeCap = Butt,
            )
            // Fully completed
            TDWeeklyCircularProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = 1f,
                strokeWidth = 14.dp,
                strokeCap = Butt,
            )
        }
    }
}
