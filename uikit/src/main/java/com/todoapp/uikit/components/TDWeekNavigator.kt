package com.todoapp.uikit.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import com.example.uikit.R
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
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Previous week",
                tint = TDTheme.colors.onBackground,
            )
        }

        TDText(
            text = label,
            style = TDTheme.typography.regularTextStyle,
            color = TDTheme.colors.onBackground,
        )

        IconButton(
            onClick = onNextWeek,
            enabled = !isCurrentWeek,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = "Next week",
                tint = TDTheme.colors.onBackground,
                modifier = Modifier.alpha(if (isCurrentWeek) 0.3f else 1f),
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@TDPreviewWide
@Composable
private fun TdWeekNavigatorCurrentPreview() {
    TDTheme {
        TDWeekNavigator(
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
            selectedDate = LocalDate.now().minusWeeks(3),
            onPreviousWeek = {},
            onNextWeek = {},
        )
    }
}
