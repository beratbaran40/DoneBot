package com.todoapp.mobile.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.todoapp.uikit.components.TDSkeletonBox
import com.todoapp.uikit.components.TDSkeletonText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun CalendarSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        CalendarSkeletonDatePicker()
        Spacer(Modifier.height(20.dp))
        TDSkeletonText(width = 140.dp, height = 18.dp)
        Spacer(Modifier.height(12.dp))
        repeat(CALENDAR_SKELETON_TASK_COUNT) {
            CalendarSkeletonFullTaskCard()
            Spacer(Modifier.height(8.dp))
        }
    }
}

private const val CALENDAR_SKELETON_TASK_COUNT = 3
private const val CALENDAR_SKELETON_GRID_ROWS = 6
private const val CALENDAR_SKELETON_GRID_COLS = 7

@Composable
private fun CalendarSkeletonDatePicker() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TDSkeletonText(width = 140.dp, height = 20.dp)
            Spacer(Modifier.weight(1f))
            TDSkeletonBox(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
            )
            Spacer(Modifier.width(12.dp))
            TDSkeletonBox(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            repeat(CALENDAR_SKELETON_GRID_COLS) {
                TDSkeletonText(width = 24.dp, height = 12.dp)
            }
        }
        Spacer(Modifier.height(12.dp))
        repeat(CALENDAR_SKELETON_GRID_ROWS) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(CALENDAR_SKELETON_GRID_COLS) {
                    TDSkeletonBox(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun CalendarSkeletonFullTaskCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TDTheme.colors.lightPending.copy(alpha = 0.4f))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TDSkeletonText(width = 180.dp, height = 16.dp)
            TDSkeletonText(width = 120.dp, height = 12.dp)
            TDSkeletonBox(
                modifier = Modifier.size(width = 80.dp, height = 24.dp),
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}

@TDPreview
@Composable
private fun CalendarSkeletonPreview() {
    TDTheme {
        CalendarSkeleton()
    }
}
