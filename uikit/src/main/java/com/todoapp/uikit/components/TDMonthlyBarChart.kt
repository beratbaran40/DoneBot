package com.todoapp.uikit.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/**
 * Bar cap. Rounded kits keep the soft top the chart was drawn with; a stepped kit gets a flat
 * block, because a curved cap cannot be expressed on a whole-pixel grid.
 */
@Composable
private fun barShape(): Shape = if (TDTheme.motion.stepped) RectangleShape else RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)

@Composable
fun TDMonthlyBarChart(
    title: String,
    completedValues: List<Int>,
    pendingValues: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    barWidth: Dp = 24.dp,
    onBarClick: ((Int) -> Unit)? = null,
) {
    require(completedValues.size == pendingValues.size && labels.size == completedValues.size) {
        "completedValues, pendingValues and labels must all have the same size"
    }
    val totals = completedValues.zip(pendingValues) { c, p -> c + p }
    val maxTotal = totals.maxOrNull()?.coerceAtLeast(1) ?: 1

    Column(modifier = modifier.fillMaxWidth()) {
        TDText(
            text = title,
            style = TDTheme.typography.heading5,
            color = TDTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            for (index in completedValues.indices) {
                BarColumn(
                    completed = completedValues[index],
                    pending = pendingValues[index],
                    label = labels[index],
                    maxValue = maxTotal,
                    barWidth = barWidth,
                    onClick = onBarClick?.let { handler -> { handler(index) } },
                )
            }
        }
    }
}

@Composable
private fun BarColumn(
    completed: Int,
    pending: Int,
    label: String,
    maxValue: Int,
    barWidth: Dp,
    onClick: (() -> Unit)? = null,
) {
    val total = completed + pending
    val totalFraction = total.toFloat() / maxValue
    // Animate the bar height: when values change (month switch, drill-in, recurring toggle, task
    // completion) the bar tween-grows/shrinks to its new size instead of snapping. Initial mount
    // also animates from 0 → target so bars rise into place.
    val animatedFraction by animateFloatAsState(
        targetValue = totalFraction,
        animationSpec = tween(durationMillis = BAR_ANIMATION_MS, easing = FastOutSlowInEasing),
        label = "barFraction",
    )

    Column(
        modifier = Modifier
            .widthIn(min = barWidth)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        TDText(
            text = total.toString(),
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        // The bar is height-allocated proportional to its total relative to the largest bar in the
        // chart; inside, the completed portion stacks at the bottom and pending sits above. Each
        // sub-box is emitted only when its count > 0 because Modifier.weight requires a positive
        // value — without this guard, an all-completed or all-pending bar throws IAE.
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(BAR_AREA_HEIGHT * animatedFraction)
                .clip(barShape()),
        ) {
            if (total > 0) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (pending > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(pending.toFloat())
                                .background(TDTheme.colors.pendingGray),
                        )
                    }
                    if (completed > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(completed.toFloat())
                                .background(TDTheme.colors.mediumGreen),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        TDText(
            text = label,
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.pendingGray,
        )
    }
}

private val BAR_AREA_HEIGHT = 110.dp
private const val BAR_ANIMATION_MS = 450

@TDPreview
@Composable
private fun TDMonthlyBarChartPopulatedPreview() {
    TDTheme {
        Box(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            TDMonthlyBarChart(
                title = "Aylık görevler",
                completedValues = listOf(5, 3, 9, 2, 1),
                pendingValues = listOf(2, 4, 1, 5, 0),
                labels = listOf("W1", "W2", "W3", "W4", "W5"),
            )
        }
    }
}

@TDPreview
@Composable
private fun TDMonthlyBarChartEmptyPreview() {
    TDTheme {
        Box(modifier = Modifier.background(TDTheme.colors.background).padding(16.dp)) {
            TDMonthlyBarChart(
                title = "Aylık görevler",
                completedValues = listOf(0, 0, 0, 0, 0),
                pendingValues = listOf(0, 0, 0, 0, 0),
                labels = listOf("W1", "W2", "W3", "W4", "W5"),
            )
        }
    }
}
