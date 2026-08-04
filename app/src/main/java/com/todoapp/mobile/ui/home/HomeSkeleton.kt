package com.todoapp.mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.components.TDSkeletonBox
import com.todoapp.uikit.components.TDSkeletonText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner

@Composable
internal fun HomeSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        HomeSkeletonGreetingRow()
        HomeSkeletonCalendarStrip()
        Spacer(Modifier.height(4.dp))
        HomeSkeletonStatsRow()
        Spacer(Modifier.height(4.dp))
        HomeSkeletonTabRow()
        Spacer(Modifier.height(4.dp))
        repeat(HOME_SKELETON_TASK_COUNT) { index ->
            HomeSkeletonTaskCard(variantIndex = index)
        }
    }
}

private const val HOME_SKELETON_TASK_COUNT = 4

@Composable
private fun HomeSkeletonGreetingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDSkeletonBox(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
        )
        Spacer(Modifier.width(8.dp))
        TDSkeletonText(width = 220.dp, height = 18.dp)
    }
}

@Composable
private fun HomeSkeletonCalendarStrip() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TDSkeletonText(width = 140.dp, height = 20.dp)
            Spacer(Modifier.weight(1f))
            TDSkeletonBox(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
            )
            Spacer(Modifier.width(8.dp))
            TDSkeletonBox(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(HOME_SKELETON_DATE_PILL_COUNT) {
                TDSkeletonBox(
                    modifier = Modifier.size(width = 44.dp, height = 56.dp),
                    shape = tdCorner(14.dp),
                )
            }
        }
    }
}

private const val HOME_SKELETON_DATE_PILL_COUNT = 8

@Composable
private fun HomeSkeletonStatsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TDSkeletonBox(
            modifier = Modifier
                .weight(1f)
                .height(96.dp),
            shape = TDTheme.shapes.xLarge,
        )
        TDSkeletonBox(
            modifier = Modifier
                .weight(1f)
                .height(96.dp),
            shape = TDTheme.shapes.xLarge,
        )
    }
}

@Composable
private fun HomeSkeletonTabRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TDSkeletonBox(
            modifier = Modifier
                .weight(1f)
                .height(36.dp),
            shape = tdCorner(10.dp),
        )
        TDSkeletonBox(
            modifier = Modifier
                .weight(1f)
                .height(36.dp),
            shape = tdCorner(10.dp),
        )
    }
}

private val taskTitleWidths = listOf(200.dp, 240.dp, 180.dp, 220.dp)
private val taskSubtitleWidths = listOf(120.dp, 160.dp, 140.dp, 100.dp)

@Composable
private fun HomeSkeletonTaskCard(variantIndex: Int) {
    val titleWidth: Dp = taskTitleWidths[variantIndex % taskTitleWidths.size]
    val subtitleWidth: Dp = taskSubtitleWidths[variantIndex % taskSubtitleWidths.size]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TDTheme.shapes.medium)
            .background(TDTheme.colors.lightPending.copy(alpha = 0.4f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDSkeletonBox(
            modifier = Modifier.size(28.dp),
            shape = TDTheme.shapes.small,
        )
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TDSkeletonText(width = titleWidth, height = 16.dp)
            TDSkeletonText(width = subtitleWidth, height = 12.dp)
        }
    }
}

@TDPreview
@Composable
private fun HomeSkeletonPreview() {
    TDTheme {
        HomeSkeleton()
    }
}
