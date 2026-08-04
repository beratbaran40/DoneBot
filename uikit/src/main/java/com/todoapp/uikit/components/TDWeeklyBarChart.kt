package com.todoapp.uikit.components

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreviewWide
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner
import kotlin.math.max

/**
 * Bar cap. Rounded kits keep the soft top the chart was drawn with; a stepped kit gets a flat
 * block, because a curved cap cannot be expressed on a whole-pixel grid.
 */
@Composable
private fun barShape(): Shape = if (TDTheme.motion.stepped) RectangleShape else RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)

@Composable
fun TDWeeklyBarChart(
    modifier: Modifier,
    title: String,
    days: List<String>? = null,
    values: List<Int>,
    pendingValues: List<Int> = emptyList(),
    pendingBarColor: Color = TDTheme.colors.pendingGray,
    yLabelColor: Color = TDTheme.colors.onBackground,
    height: Dp,
    autoScaleHeightToMaxY: Boolean = true,
    minGridStep: Dp = 24.dp,
    scrollableHeight: Dp? = null,
    onExpandClick: (() -> Unit)? = null,
) {
    val resolvedDays =
        days ?: listOf(
            stringResource(R.string.bar_chart_days_mon),
            stringResource(R.string.bar_chart_days_tue),
            stringResource(R.string.bar_chart_days_wed),
            stringResource(R.string.bar_chart_days_thu),
            stringResource(R.string.bar_chart_days_fri),
            stringResource(R.string.bar_chart_days_sat),
            stringResource(R.string.bar_chart_days_sun),
        )

    if (values.size != resolvedDays.size) return

    val hasPending = pendingValues.size == resolvedDays.size

    val barWidth = 24.dp
    val barColor = TDTheme.colors.mediumGreen
    val gridColor = TDTheme.colors.gray
    val xAxisHeight = 36.dp
    val yLabelWidth = 24.dp
    val leftGapToPlot = 2.dp

    val density = LocalDensity.current

    val maxY =
        if (hasPending) {
            values.zip(pendingValues) { c, p -> c + p }.maxOrNull() ?: 0
        } else {
            values.maxOrNull() ?: 0
        }
    val safeMaxY = max(1, maxY)

    val requiredPlotHeight = (safeMaxY * minGridStep.value).dp
    val plotHeight = if (autoScaleHeightToMaxY && requiredPlotHeight > height) requiredPlotHeight else height

    val labelTextSizePx = with(density) { 12.sp.toPx() }
    val labelXOffsetPx = with(density) { 10.dp.toPx() }
    val yLabelPaint =
        remember(yLabelColor, labelTextSizePx) {
            Paint().apply {
                isAntiAlias = true
                textSize = labelTextSizePx
                textAlign = Paint.Align.RIGHT
                color = yLabelColor.toArgb()
            }
        }

    val dashedLines = remember { PathEffect.dashPathEffect(floatArrayOf(48f, 4f), 0f) }

    TDWeeklyBarChartContent(
        modifier = modifier,
        title = title,
        days = resolvedDays,
        values = values,
        pendingValues = if (hasPending) pendingValues else null,
        safeMaxY = safeMaxY,
        plotHeight = plotHeight,
        barWidth = barWidth,
        barColor = barColor,
        pendingBarColor = pendingBarColor,
        gridColor = gridColor,
        xAxisHeight = xAxisHeight,
        yLabelWidth = yLabelWidth,
        leftGapToPlot = leftGapToPlot,
        density = density,
        yLabelPaint = yLabelPaint,
        labelXOffsetPx = labelXOffsetPx,
        dashedLines = dashedLines,
        scrollableHeight = scrollableHeight,
        onExpandClick = onExpandClick,
    )
}

@Composable
private fun TDWeeklyBarChartContent(
    modifier: Modifier,
    title: String,
    days: List<String>,
    values: List<Int>,
    pendingValues: List<Int>?,
    safeMaxY: Int,
    plotHeight: Dp,
    barWidth: Dp,
    barColor: Color,
    pendingBarColor: Color,
    gridColor: Color,
    xAxisHeight: Dp,
    yLabelWidth: Dp,
    leftGapToPlot: Dp,
    density: Density,
    yLabelPaint: Paint,
    labelXOffsetPx: Float,
    dashedLines: PathEffect,
    scrollableHeight: Dp?,
    onExpandClick: (() -> Unit)?,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TDText(
                text = title,
                style = TDTheme.typography.heading4,
                color = TDTheme.colors.onBackground,
            )
            if (onExpandClick != null) {
                IconButton(
                    onClick = onExpandClick,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        painter = tdPainter(R.drawable.ic_fullscreen),
                        contentDescription = stringResource(R.string.cd_expand_chart),
                        tint = TDTheme.colors.onBackground,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        val chartPlot: @Composable () -> Unit = {
            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(plotHeight + xAxisHeight)
                    .padding(horizontal = 16.dp),
            ) {
                Canvas(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                ) {
                    val plotLeft = with(density) { (yLabelWidth + leftGapToPlot).toPx() }
                    val plotRight = size.width
                    val plotTop = 0f
                    val plotBottom = with(density) { plotHeight.toPx() }
                    val stepPx = (plotBottom - plotTop) / safeMaxY.toFloat()

                    for (i in 0..safeMaxY) {
                        val y = plotBottom - i * stepPx

                        drawLine(
                            color = gridColor,
                            start = Offset(plotLeft, y),
                            end = Offset(plotRight, y),
                            strokeWidth = with(density) { 1.dp.toPx() },
                            pathEffect = dashedLines,
                        )

                        val label = if (i == 0) "0" else i.toString().padStart(2, '0')
                        drawContext.canvas.nativeCanvas.apply {
                            drawText(
                                label,
                                plotLeft - labelXOffsetPx,
                                y + yLabelPaint.textSize / 3f,
                                yLabelPaint,
                            )
                        }
                    }
                }

                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = yLabelWidth + leftGapToPlot)
                        .height(plotHeight + xAxisHeight),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    values.forEachIndexed { index, v ->
                        val pendingV = pendingValues?.getOrNull(index) ?: 0
                        val total = (v + pendingV).coerceIn(0, safeMaxY)
                        val totalTarget = total / safeMaxY.toFloat()
                        val completedTarget = v.coerceIn(0, total) / safeMaxY.toFloat()

                        val totalAnim by animateFloatAsState(
                            targetValue = totalTarget,
                            animationSpec = tween(durationMillis = 650),
                        )

                        val completedAnim by animateFloatAsState(
                            targetValue = completedTarget,
                            animationSpec = tween(durationMillis = 650),
                        )

                        Column(
                            modifier = Modifier.height(plotHeight + xAxisHeight),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Box(
                                modifier =
                                Modifier
                                    .height(plotHeight)
                                    .width(barWidth),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                if (pendingValues != null) {
                                    Box(
                                        modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(totalAnim)
                                            .clip(barShape())
                                            .background(pendingBarColor),
                                    )
                                }
                                Box(
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(if (pendingValues != null) completedAnim else totalAnim)
                                        .clip(tdCorner(2.dp))
                                        .background(barColor),
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            TDText(
                                text = days[index],
                                style = TDTheme.typography.regularTextStyle,
                                color = TDTheme.colors.onBackground,
                            )
                        }
                    }
                }
            }
        }

        if (scrollableHeight != null) {
            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(scrollableHeight),
            ) {
                Column(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    chartPlot()
                }
            }
        } else {
            chartPlot()
        }
    }
}

@TDPreviewWide
@Composable
fun TDWeeklyBarChartPreview() {
    TDTheme {
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TDWeeklyBarChart(
                modifier = Modifier,
                title = "Task",
                values = listOf(9, 4, 5, 2, 2, 3, 4),
                pendingValues = listOf(3, 1, 2, 4, 0, 1, 2),
                height = 220.dp,
                scrollableHeight = 180.dp,
                onExpandClick = {},
            )
        }
    }
}

@TDPreviewWide
@Composable
private fun TDWeeklyBarChartCompletedOnlyPreview() {
    TDTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TDWeeklyBarChart(
                modifier = Modifier,
                title = "Completed Tasks",
                values = listOf(5, 3, 7, 2, 6, 4, 8),
                height = 200.dp,
            )
        }
    }
}

@TDPreviewWide
@Composable
private fun TDWeeklyBarChartEmptyPreview() {
    TDTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TDWeeklyBarChart(
                modifier = Modifier,
                title = "This Week",
                values = listOf(0, 0, 0, 0, 0, 0, 0),
                height = 180.dp,
            )
        }
    }
}

@TDPreviewWide
@Composable
private fun TDWeeklyBarChartWithExpandPreview() {
    TDTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TDWeeklyBarChart(
                modifier = Modifier,
                title = "Tasks",
                values = listOf(2, 4, 1, 3, 5, 2, 1),
                pendingValues = listOf(1, 1, 2, 1, 0, 1, 2),
                height = 200.dp,
                onExpandClick = {},
            )
        }
    }
}
