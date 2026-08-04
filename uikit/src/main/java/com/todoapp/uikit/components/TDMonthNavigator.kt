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
import androidx.compose.ui.res.stringResource
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TDMonthNavigator(
    month: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val locale = if (configuration.locales.isEmpty) Locale.getDefault() else configuration.locales[0]

    val isCurrentMonth = month == YearMonth.now()
    val label = "${month.month.getDisplayName(TextStyle.FULL, locale)} ${month.year}"

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                painter = tdPainter(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.cd_previous_month),
                tint = TDTheme.colors.onBackground,
            )
        }

        TDText(
            text = label,
            style = TDTheme.typography.heading5,
            color = TDTheme.colors.onBackground,
        )

        IconButton(
            onClick = onNextMonth,
            enabled = !isCurrentMonth,
        ) {
            Icon(
                painter = tdPainter(R.drawable.ic_arrow_forward),
                contentDescription = stringResource(R.string.cd_next_month),
                tint = TDTheme.colors.onBackground,
                modifier = Modifier.alpha(if (isCurrentMonth) 0.3f else 1f),
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@TDPreview
@Composable
private fun TDMonthNavigatorCurrentPreview() {
    TDTheme {
        TDMonthNavigator(
            month = YearMonth.now(),
            onPreviousMonth = {},
            onNextMonth = {},
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@TDPreview
@Composable
private fun TDMonthNavigatorPastPreview() {
    TDTheme {
        TDMonthNavigator(
            month = YearMonth.now().minusMonths(3),
            onPreviousMonth = {},
            onNextMonth = {},
        )
    }
}
