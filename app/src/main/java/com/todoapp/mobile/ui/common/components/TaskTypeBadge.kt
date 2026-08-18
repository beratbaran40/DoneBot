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
import androidx.compose.ui.graphics.luminance
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
    val onAccent = onAccentColor(accent)
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
            tint = onAccent,
            modifier = Modifier.size(14.dp),
        )
        TDText(
            text = stringResource(labelRes),
            style = TDTheme.typography.subheading1.copy(fontWeight = FontWeight.SemiBold),
            color = onAccent,
        )
    }
}

/**
 * Ink for the filled badge: whichever of the kit's two ink tokens actually contrasts with the accent.
 *
 * The badge used to hardcode `colors.white`, which only works while every accent is dark. Half the
 * accents are not: a dark-mode accent is bright by construction, and even ORIGINAL's light-mode
 * ROUTINE blue left white text at 2.7:1. Across the four kits and both modes, 16 of 28 badges were
 * below AA and the worst sat at 1.24:1.
 *
 * Comparing both candidates rather than thresholding on luminance means the choice is exact, and it
 * is a repair rather than a restyle: white keeps winning wherever it already worked, so no badge that
 * reads correctly today changes at all.
 */
@Composable
private fun onAccentColor(accent: Color): Color {
    val white = TDTheme.colors.white
    val black = TDTheme.colors.black
    return if (contrastRatio(white, accent) >= contrastRatio(black, accent)) white else black
}

/** WCAG relative-luminance contrast ratio, 1:1 to 21:1. */
private fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + CONTRAST_OFFSET
    val lb = b.luminance() + CONTRAST_OFFSET
    return if (la > lb) la / lb else lb / la
}

private const val CONTRAST_OFFSET = 0.05f

/**
 * Palette-aware accent for a task type. In MONOCHROME the characteristic blue (`purple`) marks
 * both ONE_TIME and ROUTINE (they stay distinct through their icons), STAGED keeps green. The
 * chromatic kits keep the classic per-type colors (ONE_TIME blue `darkPending`, ROUTINE `purple`,
 * STAGED green) — 8-bit wants more hue separation, not less. TERMINAL gets its own mapping so the
 * badge agrees with the home list's one-time gutter stripe.
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
    // TERMINAL separates all four by hue on a phosphor ground: violet for one-time, which matches the
    // gutter stripe the home list already draws for exactly that case; the cursor green for a routine,
    // because a repeating task is a running job; and cyan for STAGED, which keeps the success hue in
    // every kit.
    PaletteKit.TERMINAL -> when (type) {
        TaskType.ONE_TIME -> TDTheme.colors.purple
        TaskType.ROUTINE -> TDTheme.colors.primary
        TaskType.STAGED -> TDTheme.colors.mediumGreen
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
