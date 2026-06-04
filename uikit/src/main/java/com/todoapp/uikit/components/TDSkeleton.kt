package com.todoapp.uikit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

private const val SHIMMER_DURATION_MS = 1200

/**
 * Animated shimmer brush that sweeps a light highlight across the placeholder. Use as the
 * `background(brush = ...)` of a Box to make any rectangular block read as a loading skeleton.
 */
@Composable
private fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "td-skeleton-shimmer")
    val base = TDTheme.colors.lightPending
    val highlight = TDTheme.colors.lightGray.copy(alpha = 0.45f)
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "td-skeleton-translate",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translate - 500f, 0f),
        end = Offset(translate, 0f),
    )
}

/**
 * Rectangular skeleton block. Pass a fixed `width` + `height` for text-like placeholders or
 * `Modifier.fillMaxWidth().height(...)` for full-width blocks.
 */
@Composable
fun TDSkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(rememberShimmerBrush()),
    )
}

/**
 * A single-line text skeleton. Defaults to 14dp tall (matches `regularTextStyle`) but caller
 * should set `width` to match the expected text length so the layout doesn't jump on settle.
 */
@Composable
fun TDSkeletonText(
    modifier: Modifier = Modifier,
    width: Dp = 120.dp,
    height: Dp = 14.dp,
) {
    TDSkeletonBox(
        modifier = modifier
            .width(width)
            .height(height),
        shape = RoundedCornerShape(4.dp),
    )
}

/**
 * Task-card shaped skeleton: avatar circle + two text lines. Drop several inside a column to
 * stand in for a task list while data loads. Width is `fillMaxWidth` by default.
 */
@Composable
fun TDSkeletonCard(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TDTheme.colors.lightPending.copy(alpha = 0.4f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDSkeletonBox(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TDSkeletonText(width = 180.dp, height = 14.dp)
            TDSkeletonText(width = 100.dp, height = 10.dp)
        }
    }
}

@TDPreview
@Composable
private fun TdSkeletonBoxPreview() {
    TDTheme {
        TDSkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(120.dp),
            shape = RoundedCornerShape(16.dp),
        )
    }
}

@TDPreview
@Composable
private fun TdSkeletonTextStackPreview() {
    TDTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TDSkeletonText(width = 220.dp, height = 18.dp)
            TDSkeletonText(width = 140.dp, height = 14.dp)
            TDSkeletonText(width = 200.dp, height = 14.dp)
            TDSkeletonText(width = 80.dp, height = 12.dp)
        }
    }
}

@TDPreview
@Composable
private fun TdSkeletonCardListPreview() {
    TDTheme {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = List(4) { it }) {
                TDSkeletonCard()
            }
        }
    }
}
