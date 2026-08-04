package com.todoapp.mobile.ui.filteredtasks

import androidx.compose.foundation.background
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

@Composable
internal fun FilteredTasksSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        FilteredTasksSkeletonWeekNavigator()
        Spacer(Modifier.height(12.dp))
        FilteredTasksSkeletonTabRow()
        Spacer(Modifier.height(8.dp))
        FilteredTasksSkeletonSortButton()
        Spacer(Modifier.height(16.dp))
        repeat(FILTERED_TASKS_SKELETON_COUNT) { index ->
            FilteredTasksSkeletonRow(variantIndex = index)
            Spacer(Modifier.height(12.dp))
        }
    }
}

private const val FILTERED_TASKS_SKELETON_COUNT = 4

@Composable
private fun FilteredTasksSkeletonWeekNavigator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDSkeletonBox(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
        )
        Spacer(Modifier.weight(1f))
        TDSkeletonText(width = 160.dp, height = 16.dp)
        Spacer(Modifier.weight(1f))
        TDSkeletonBox(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
        )
    }
}

@Composable
private fun FilteredTasksSkeletonTabRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TDSkeletonBox(
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            shape = TDTheme.shapes.medium,
        )
        TDSkeletonBox(
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            shape = TDTheme.shapes.medium,
        )
    }
}

@Composable
private fun FilteredTasksSkeletonSortButton() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        TDSkeletonBox(
            modifier = Modifier.size(width = 110.dp, height = 32.dp),
            shape = TDTheme.shapes.large,
        )
    }
}

private val filteredTitleWidths = listOf(220.dp, 180.dp, 240.dp, 200.dp)
private val filteredSubtitleWidths = listOf(140.dp, 100.dp, 160.dp, 120.dp)

@Composable
private fun FilteredTasksSkeletonRow(variantIndex: Int) {
    val titleWidth: Dp = filteredTitleWidths[variantIndex % filteredTitleWidths.size]
    val subtitleWidth: Dp = filteredSubtitleWidths[variantIndex % filteredSubtitleWidths.size]
    Column {
        TDSkeletonBox(
            modifier = Modifier.size(width = 96.dp, height = 20.dp),
            shape = TDTheme.shapes.small,
        )
        Spacer(Modifier.height(6.dp))
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
}

@TDPreview
@Composable
private fun FilteredTasksSkeletonPreview() {
    TDTheme {
        FilteredTasksSkeleton()
    }
}
