package com.todoapp.mobile.ui.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.TaskType
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR

/**
 * Compact type pill (icon + short label on an accent fill) for overlaying on a task photo banner.
 * Mirrors the per-type icon/accent/label used by the detail screen's type header.
 */
@Composable
fun TaskTypeBadge(
    type: TaskType,
    modifier: Modifier = Modifier,
) {
    val accent = taskTypeAccent(type)
    val icon: Painter
    val labelRes: Int
    when (type) {
        TaskType.ONE_TIME -> {
            icon = tdPainter(UiKitR.drawable.ic_edit_task)
            labelRes = R.string.type_one_time_title
        }
        TaskType.ROUTINE -> {
            icon = tdPainter(R.drawable.ic_calendar)
            labelRes = R.string.type_routine_title
        }
        TaskType.STAGED -> {
            icon = tdPainter(R.drawable.ic_staged)
            labelRes = R.string.type_staged_title
        }
        TaskType.CUSTOM -> {
            icon = tdPainter(R.drawable.ic_custom)
            labelRes = R.string.type_custom_title
        }
    }
    Row(
        modifier = modifier
            .clip(TDTheme.shapes.small)
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

/**
 * Palette-aware accent for a task type. In MONOCHROME the characteristic blue (`purple`) marks
 * both ONE_TIME and ROUTINE (they stay distinct through their icons), STAGED keeps green. The
 * chromatic kits keep the classic per-type colors (ONE_TIME blue `darkPending`, ROUTINE `purple`,
 * STAGED green) — 8-bit wants more hue separation, not less.
 */
@Composable
fun taskTypeAccent(type: TaskType): Color = when (TDTheme.palette) {
    PaletteKit.MONOCHROME -> when (type) {
        TaskType.ONE_TIME -> TDTheme.colors.purple
        TaskType.ROUTINE -> TDTheme.colors.purple
        TaskType.STAGED -> TDTheme.colors.mediumGreen
        TaskType.CUSTOM -> TDTheme.colors.darkPending
    }
    PaletteKit.ORIGINAL, PaletteKit.PIXEL -> when (type) {
        TaskType.ONE_TIME -> TDTheme.colors.darkPending
        TaskType.ROUTINE -> TDTheme.colors.purple
        TaskType.STAGED -> TDTheme.colors.mediumGreen
        // A custom task combines the others, so it gets its own ink rather than borrowing one of
        // theirs — reusing ROUTINE's purple would make a routine+staged task read as a plain routine.
        TaskType.CUSTOM -> TDTheme.colors.darkPurple
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
            TaskTypeBadge(TaskType.ONE_TIME)
            TaskTypeBadge(TaskType.ROUTINE)
            TaskTypeBadge(TaskType.STAGED)
            TaskTypeBadge(TaskType.CUSTOM)
        }
    }
}
