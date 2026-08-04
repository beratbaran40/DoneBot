package com.todoapp.uikit.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import kotlin.math.round
import kotlin.math.roundToInt

/** Whole cells a stepped kit's progress fill snaps to — 20 reads as chunky without being coarse. */
private const val PIXEL_SEGMENTS = 20f

@Composable
fun TDGeneralProgressBar(
    progress: Float,
    completedCount: Int? = null,
    totalCount: Int? = null,
) {
    val height = 32.dp
    val barColor = TDTheme.colors.pendingGray
    val progressColor = TDTheme.colors.mediumGreen
    val p = progress.coerceIn(0f, 1f)
    val stepped = TDTheme.motion.stepped
    // A pixel machine has no partial cells: quantise the fill to whole blocks so the bar advances in
    // discrete jumps, and drive it with the kit's stepped easing so the travel matches the form.
    val target = if (stepped) (round(p * PIXEL_SEGMENTS) / PIXEL_SEGMENTS) else p
    val animatedP by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(easing = TDTheme.motion.standardEasing),
        label = "progressBar",
    )

    // The capsule is a soft-kit affordance; a stepped kit fills the whole rectangle instead.
    val shape = if (stepped) TDTheme.shapes.none else RoundedCornerShape(percent = 50)
    Column {
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(barColor)
                .height(height),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(animatedP)
                    .clip(shape)
                    .background(progressColor),
            )
            TDText(
                text = "${(p * 100).roundToInt()}%",
                style = TDTheme.typography.regularTextStyle,
                color = Color.White,
            )
        }

        if (completedCount != null && totalCount != null) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TDText(
                    text = stringResource(com.example.uikit.R.string.general_progress_tasks_done, completedCount),
                    style = TDTheme.typography.regularTextStyle,
                    color = TDTheme.colors.onBackground,
                )
                TDText(
                    text = stringResource(com.example.uikit.R.string.general_progress_total, totalCount),
                    style = TDTheme.typography.regularTextStyle,
                    color = TDTheme.colors.onBackground,
                )
            }
        }
    }
}

@TDPreview
@Composable
fun TDGeneralProgressBarPreviewLight() {
    TDTheme {
        Column(
            modifier =
            Modifier
                .fillMaxWidth(),
        ) {
            TDGeneralProgressBar(
                progress = 0.7f,
            )
        }
    }
}

@TDPreview
@Composable
private fun TDGeneralProgressBarZeroPreview() {
    TDTheme {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            TDGeneralProgressBar(progress = 0f)
        }
    }
}

@TDPreview
@Composable
private fun TDGeneralProgressBarHalfPreview() {
    TDTheme {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            TDGeneralProgressBar(progress = 0.5f)
        }
    }
}

@TDPreview
@Composable
private fun TDGeneralProgressBarFullPreview() {
    TDTheme {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            TDGeneralProgressBar(progress = 1f)
        }
    }
}

@TDPreview
@Composable
private fun TDGeneralProgressBarWithCountsPreview() {
    TDTheme {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            TDGeneralProgressBar(
                progress = 0.6f,
                completedCount = 6,
                totalCount = 10,
            )
        }
    }
}
