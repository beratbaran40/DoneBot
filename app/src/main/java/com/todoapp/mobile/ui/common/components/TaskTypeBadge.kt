package com.todoapp.mobile.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.common.taskform.TaskFormType
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR

/**
 * Compact type pill (icon + short label on an accent fill) for overlaying on a task photo banner.
 * Mirrors the per-type icon/accent/label used by the detail screen's type header.
 */
@Composable
fun TaskTypeBadge(
    type: TaskFormType,
    modifier: Modifier = Modifier,
) {
    val icon: Painter
    val accent: Color
    val labelRes: Int
    when (type) {
        TaskFormType.ONE_TIME -> {
            icon = painterResource(UiKitR.drawable.ic_edit_task)
            accent = TDTheme.colors.darkPending
            labelRes = R.string.type_one_time_title
        }
        TaskFormType.ROUTINE -> {
            icon = painterResource(R.drawable.ic_calendar)
            accent = TDTheme.colors.purple
            labelRes = R.string.type_routine_title
        }
        TaskFormType.STAGED -> {
            icon = painterResource(R.drawable.ic_staged)
            accent = TDTheme.colors.mediumGreen
            labelRes = R.string.type_staged_title
        }
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = TDTheme.colors.white,
            modifier = Modifier.size(14.dp),
        )
        TDText(
            text = stringResource(labelRes),
            style = TDTheme.typography.subheading1.copy(fontWeight = FontWeight.SemiBold),
            color = TDTheme.colors.white,
        )
    }
}

@TDPreview
@Composable
private fun TaskTypeBadgePreview() {
    TDTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TaskTypeBadge(TaskFormType.ONE_TIME)
            TaskTypeBadge(TaskFormType.ROUTINE)
            TaskTypeBadge(TaskFormType.STAGED)
        }
    }
}
