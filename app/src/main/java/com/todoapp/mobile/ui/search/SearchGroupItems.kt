package com.todoapp.mobile.ui.search

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.domain.model.Group
import com.todoapp.mobile.domain.model.GroupTask
import com.todoapp.mobile.ui.common.components.categoryIconFor
import com.todoapp.mobile.ui.common.components.taskChipLabel
import com.todoapp.mobile.ui.search.SearchContract.UiAction
import com.todoapp.uikit.components.TDTaskCardWithCheckbox
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
internal fun SearchSectionHeader(title: String) {
    TDText(
        text = title.uppercase(),
        style = TDTheme.typography.subheading1,
        color = TDTheme.colors.gray,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
internal fun SearchGroupHeaderItem(
    group: Group,
    onAction: (UiAction) -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable { onAction(UiAction.OnGroupClick(group)) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GroupInitialsAvatar(name = group.name, size = 32)
        Spacer(Modifier.width(10.dp))
        TDText(
            text = group.name,
            style = TDTheme.typography.heading7,
            color = TDTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        if (group.role.isNotBlank()) {
            GroupRolePill(role = group.role)
        }
    }
}

@Composable
internal fun SearchGroupTaskItem(
    group: Group,
    groupTask: GroupTask,
    onAction: (UiAction) -> Unit,
) {
    Column(
        modifier =
        Modifier
            .padding(start = 16.dp)
            .clickable { onAction(UiAction.OnGroupTaskClick(group, groupTask)) },
    ) {
        val openLocation = com.todoapp.mobile.ui.common.rememberOpenLocation(
            groupTask.locationName, groupTask.locationAddress, groupTask.locationLat, groupTask.locationLng,
        )
        TDTaskCardWithCheckbox(
            taskText = groupTask.title,
            taskDescription = groupTask.description,
            isChecked = groupTask.isCompleted,
            onCheckBoxClick = {},
            categoryLabel = taskChipLabel(groupTask),
            categoryIcon = categoryIconFor(groupTask.category),
            subtaskTotal = groupTask.subtasks.size,
            subtaskDone = groupTask.subtasks.count { it.isCompleted },
            locationLabel = groupTask.locationName,
            onLocationClick = openLocation,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            groupTask.dueDate?.let { millis ->
                val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                BadgePill {
                    TDText(
                        text = date.format(DATE_FORMATTER),
                        style = TDTheme.typography.subheading2,
                        color = TDTheme.colors.onBackground.copy(alpha = 0.5f),
                    )
                }
            }
            groupTask.priority?.let { priority ->
                PriorityBadge(priority = priority)
            }
            groupTask.assignee?.let { assignee ->
                val initials =
                    assignee.displayName
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercase() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                    Modifier
                        .background(
                            color = TDTheme.colors.onBackground.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(6.dp),
                        ).padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    GroupInitialsAvatar(name = assignee.displayName, size = 16)
                    Spacer(Modifier.width(4.dp))
                    TDText(
                        text = assignee.displayName,
                        style = TDTheme.typography.subheading2,
                        color = TDTheme.colors.onBackground.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgePill(content: @Composable () -> Unit) {
    Row(
        modifier =
        Modifier
            .background(
                color = TDTheme.colors.onBackground.copy(alpha = 0.06f),
                shape = RoundedCornerShape(6.dp),
            ).padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun PriorityBadge(priority: String) {
    val (bgColor, textColor) =
        // ORIGINAL shows the classic red/orange/blue ramp; MONOCHROME keeps HIGH red + gray intensity.
        if (TDTheme.palette == PaletteKit.ORIGINAL) {
            when (priority.uppercase()) {
                "HIGH" -> TDTheme.colors.lightRed to TDTheme.colors.crossRed
                "MEDIUM" -> TDTheme.colors.lightOrange to TDTheme.colors.orange
                "LOW" -> TDTheme.colors.lightPending to TDTheme.colors.darkPending
                else -> TDTheme.colors.onBackground.copy(alpha = 0.06f) to TDTheme.colors.gray
            }
        } else {
            when (priority.uppercase()) {
                "HIGH" -> TDTheme.colors.lightRed to TDTheme.colors.crossRed
                "MEDIUM" -> TDTheme.colors.lightPending to TDTheme.colors.darkPending
                "LOW" -> TDTheme.colors.lightPending to TDTheme.colors.gray
                else -> TDTheme.colors.onBackground.copy(alpha = 0.06f) to TDTheme.colors.gray
            }
        }
    Row(
        modifier =
        Modifier
            .background(color = bgColor, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        TDText(
            text = priority,
            style = TDTheme.typography.subheading2,
            color = textColor,
        )
    }
}

@Composable
private fun GroupRolePill(role: String) {
    val bgColor =
        if (role.uppercase() == "ADMIN") {
            TDTheme.colors.lightOrange
        } else {
            TDTheme.colors.onBackground.copy(alpha = 0.06f)
        }
    val textColor =
        if (role.uppercase() == "ADMIN") {
            TDTheme.colors.orange
        } else {
            TDTheme.colors.gray
        }
    Box(
        modifier =
        Modifier
            .background(color = bgColor, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        TDText(
            text = role.uppercase(),
            style = TDTheme.typography.subheading2,
            color = textColor,
        )
    }
}

@Composable
private fun GroupInitialsAvatar(
    name: String,
    size: Int,
) {
    val initials =
        name
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
    Box(
        modifier =
        Modifier
            .size(size.dp)
            .background(color = TDTheme.colors.pendingGray, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        TDText(
            text = initials,
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.background,
            textAlign = TextAlign.Center,
        )
    }
}
