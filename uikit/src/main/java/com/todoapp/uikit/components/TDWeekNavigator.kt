package com.todoapp.uikit.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreviewNarrow
import com.todoapp.uikit.previews.TDPreviewWide
import com.todoapp.uikit.theme.TDTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TDWeekNavigator(
    modifier: Modifier = Modifier,
    selectedDate: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    val weekStart = selectedDate.with(DayOfWeek.MONDAY)
    val weekEnd = weekStart.plusDays(6)
    val isCurrentWeek = LocalDate.now().with(DayOfWeek.MONDAY) == weekStart

    // Safely obtain the locale from configuration to avoid exceptions in Preview environments
    val configuration = LocalConfiguration.current
    val locale =
        if (configuration.locales.isEmpty) {
            Locale.getDefault()
        } else {
            configuration.locales[0]
        }

    val label =
        if (weekStart.year == weekEnd.year) {
            val formatter = DateTimeFormatter.ofPattern("MMM d", locale)
            "${formatter.format(weekStart)} – ${formatter.format(weekEnd)}"
        } else {
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", locale)
            "${formatter.format(weekStart)} – ${formatter.format(weekEnd)}"
        }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousWeek) {
            Icon(
                painter = tdPainter(R.drawable.ic_arrow_back),
                contentDescription = "Previous week",
                tint = TDTheme.colors.onBackground,
            )
        }

        TDText(
            // The cross-year branch above names the year twice ("Dec 29, 2025 – Jan 4, 2026"), roughly
            // double the width of the ordinary label, against two 48dp icon buttons. Unbounded it
            // wrapped the row onto a second line; the weight also takes over the centring SpaceBetween
            // stops doing once a child claims the free space.
            modifier = Modifier.weight(1f),
            text = label,
            style = TDTheme.typography.regularTextStyle,
            color = TDTheme.colors.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        IconButton(
            onClick = onNextWeek,
            enabled = !isCurrentWeek,
        ) {
            Icon(
                painter = tdPainter(R.drawable.ic_arrow_forward),
                contentDescription = "Next week",
                tint = TDTheme.colors.onBackground,
                modifier = Modifier.alpha(if (isCurrentWeek) 0.3f else 1f),
            )
        }
    }
}

/**
 * The modifier the real call site passes (`FilteredTasksScreen`). Without it the Row is wrap-content,
 * `SpaceBetween` never bites, and the previews cannot reproduce the overflow they exist to catch.
 */
private val PREVIEW_MODIFIER = Modifier.fillMaxWidth().padding(horizontal = 16.dp)

@RequiresApi(Build.VERSION_CODES.O)
@TDPreviewWide
@Composable
private fun TdWeekNavigatorCurrentPreview() {
    TDTheme {
        TDWeekNavigator(
            modifier = PREVIEW_MODIFIER,
            selectedDate = LocalDate.now(),
            onPreviousWeek = {},
            onNextWeek = {},
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@TDPreviewWide
@Composable
private fun TdWeekNavigatorPastWeekPreview() {
    TDTheme {
        TDWeekNavigator(
            modifier = PREVIEW_MODIFIER,
            selectedDate = LocalDate.now().minusWeeks(3),
            onPreviousWeek = {},
            onNextWeek = {},
        )
    }
}

/** The widest label this component can produce: a week straddling New Year names the year twice. */
@RequiresApi(Build.VERSION_CODES.O)
@TDPreviewNarrow
@Composable
private fun TdWeekNavigatorAcrossYearsPreview() {
    TDTheme {
        TDWeekNavigator(
            modifier = PREVIEW_MODIFIER,
            selectedDate = LocalDate.of(2025, 12, 31),
            onPreviousWeek = {},
            onNextWeek = {},
        )
    }
}
