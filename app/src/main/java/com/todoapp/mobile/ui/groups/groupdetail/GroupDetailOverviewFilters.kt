package com.todoapp.mobile.ui.groups.groupdetail

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.GroupTaskTimeFilter
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.TaskFilter
import com.todoapp.uikit.components.TDSegmentedControl
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.previews.TDPreviewDevices
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR

/**
 * The Overview tab's single filter row: assignment scope as a two-segment control (the frequent
 * toggle stays one tap) and the time range as a compact menu chip showing the current value
 * (four options behind one anchor). Replaced the two stacked chip rows that each carried their
 * own "All" chip.
 */
@Composable
internal fun GroupDetailOverviewFilterRow(
    taskFilter: TaskFilter,
    timeFilter: GroupTaskTimeFilter,
    onTaskFilterSelected: (TaskFilter) -> Unit,
    onTimeFilterSelected: (GroupTaskTimeFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TDSegmentedControl(
            segments = listOf(stringResource(R.string.all), stringResource(R.string.assigned_to_me)),
            selectedIndex = if (taskFilter == TaskFilter.ALL) 0 else 1,
            onSegmentSelected = { index ->
                onTaskFilterSelected(if (index == 0) TaskFilter.ALL else TaskFilter.ASSIGNED_TO_ME)
            },
            modifier = Modifier.weight(1f),
        )
        GroupTimeMenuChip(
            selected = timeFilter,
            onSelected = onTimeFilterSelected,
            modifier = Modifier.fillMaxHeight(),
        )
    }
}

@Composable
private fun GroupTimeMenuChip(
    selected: GroupTaskTimeFilter,
    onSelected: (GroupTaskTimeFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Transient view state (menu visibility), not app state — same pattern as the overview card flip.
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxHeight(),
            shape = RoundedCornerShape(12.dp),
            color = TDTheme.colors.onBackground.copy(alpha = 0.06f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = TDTheme.colors.onBackground,
                )
                TDText(
                    text = stringResource(selected.labelRes()),
                    style = TDTheme.typography.subheading4,
                    color = TDTheme.colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    painter = painterResource(UiKitR.drawable.ic_arrow_down),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = TDTheme.colors.onBackground,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = TDTheme.colors.surface,
        ) {
            GroupTaskTimeFilter.entries.forEach { option ->
                val isSelected = option == selected
                DropdownMenuItem(
                    text = {
                        TDText(
                            text = stringResource(option.labelRes()),
                            style = TDTheme.typography.subheading4,
                            color = if (isSelected) TDTheme.colors.darkPending else TDTheme.colors.onBackground,
                        )
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                painter = painterResource(UiKitR.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = TDTheme.colors.darkPending,
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}

@StringRes
private fun GroupTaskTimeFilter.labelRes(): Int = when (this) {
    GroupTaskTimeFilter.TODAY -> R.string.group_time_today
    GroupTaskTimeFilter.THIS_WEEK -> R.string.group_time_this_week
    GroupTaskTimeFilter.THIS_MONTH -> R.string.group_time_this_month
    GroupTaskTimeFilter.ALL -> R.string.group_time_all
}

@TDPreviewDevices
@Composable
private fun GroupDetailOverviewFilterRowAllThisWeekPreview() {
    TDTheme {
        GroupDetailOverviewFilterRow(
            taskFilter = TaskFilter.ALL,
            timeFilter = GroupTaskTimeFilter.THIS_WEEK,
            onTaskFilterSelected = {},
            onTimeFilterSelected = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@TDPreview
@Composable
private fun GroupDetailOverviewFilterRowAssignedAllTimePreview() {
    TDTheme {
        GroupDetailOverviewFilterRow(
            taskFilter = TaskFilter.ASSIGNED_TO_ME,
            timeFilter = GroupTaskTimeFilter.ALL,
            onTaskFilterSelected = {},
            onTimeFilterSelected = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
