package com.todoapp.mobile.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.common.maskTitle
import com.todoapp.mobile.domain.model.Subtask
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner
import com.example.uikit.R as UiKitR

/**
 * A staged task's steps as a tappable checklist (no header — the caller shows the X/Y label). Shared by
 * the task detail screen and the Home inline expansion.
 */
@Composable
fun SubtaskChecklist(
    subtasks: List<Subtask>,
    onToggle: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    masked: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        subtasks.forEach { subtask ->
            val checked = subtask.isCompleted
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(tdCorner(10.dp))
                    .background(TDTheme.colors.lightPending)
                    .clickable { onToggle(subtask.id, !checked) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(tdCorner(6.dp))
                        .background(
                            if (checked) TDTheme.colors.mediumGreen else TDTheme.colors.pendingGray.copy(alpha = 0.08f),
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (checked) {
                                TDTheme.colors.mediumGreen
                            } else {
                                TDTheme.colors.pendingGray.copy(alpha = 0.5f)
                            },
                            shape = tdCorner(6.dp),
                        ),
                ) {
                    if (checked) {
                        Icon(
                            painter = tdPainter(UiKitR.drawable.ic_check_svg),
                            contentDescription = null,
                            tint = TDTheme.colors.white,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                TDText(
                    text = if (masked) subtask.title.maskTitle() else subtask.title,
                    style = TDTheme.typography.regularTextStyle,
                    color = TDTheme.colors.onBackground.copy(alpha = if (checked) 0.5f else 1f),
                )
            }
        }
    }
}

@TDPreview
@Composable
private fun SubtaskChecklistPreview() {
    val steps = listOf(
        Subtask(id = 1, parentTaskId = 1, title = "Buy the groceries", isCompleted = true, orderIndex = 0),
        Subtask(id = 2, parentTaskId = 1, title = "Cook dinner", isCompleted = false, orderIndex = 1),
        Subtask(id = 3, parentTaskId = 1, title = "Wash the dishes", isCompleted = false, orderIndex = 2),
    )
    TDTheme {
        Column(
            modifier = Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Partial progress
            SubtaskChecklist(subtasks = steps, onToggle = { _, _ -> })
            // All done
            SubtaskChecklist(subtasks = steps.map { it.copy(isCompleted = true) }, onToggle = { _, _ -> })
            // Masked (secret mode)
            SubtaskChecklist(subtasks = steps, onToggle = { _, _ -> }, masked = true)
        }
    }
}
