package com.todoapp.uikit.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

/**
 * Single-select pill chip (reminder offset, recurrence frequency, …). Selected fill/label default to
 * `primary`/`onPrimary`; override via [selectedContainerColor] / [selectedContentColor] — the task
 * form passes `pendingGray` + `white`. Unselected = outlined. Keep the selected fill a MID tone with
 * a high-contrast label — never a light tint (e.g. `lightPurple`), whose label washes out in dark.
 */
@Composable
fun TDChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Painter? = null,
    selectedContainerColor: Color = TDTheme.colors.primary,
    selectedContentColor: Color = TDTheme.colors.onPrimary,
) {
    val contentColor = if (selected) selectedContentColor else TDTheme.colors.onBackground
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) selectedContainerColor else TDTheme.colors.background,
        border = if (selected) null else BorderStroke(1.dp, TDTheme.colors.lightGray),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    painter = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            TDText(
                text = label,
                style = TDTheme.typography.subheading1.copy(fontWeight = FontWeight.Medium),
                color = contentColor,
            )
        }
    }
}

@TDPreview
@Composable
private fun TDChoiceChipRowPreview() {
    TDTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TDChoiceChip(label = "Zamanında", selected = true, onClick = {})
            TDChoiceChip(label = "15 dk", selected = false, onClick = {})
            TDChoiceChip(label = "1 saat", selected = false, onClick = {})
        }
    }
}

@TDPreview
@Composable
private fun TDChoiceChipFrequencyPreview() {
    TDTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TDChoiceChip(label = "Günlük", selected = false, onClick = {})
            TDChoiceChip(label = "Haftalık", selected = true, onClick = {})
            TDChoiceChip(label = "Aylık", selected = false, onClick = {})
        }
    }
}

@TDPreview
@Composable
private fun TDChoiceChipWithIconPreview() {
    TDTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TDChoiceChip(
                label = "Günlük",
                selected = true,
                onClick = {},
                leadingIcon = painterResource(R.drawable.ic_sun),
            )
            TDChoiceChip(
                label = "Yıllık",
                selected = false,
                onClick = {},
                leadingIcon = painterResource(R.drawable.ic_globe),
            )
        }
    }
}

@TDPreview
@Composable
private fun TDChoiceChipPendingPreview() {
    TDTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TDChoiceChip(
                label = "Zamanında",
                selected = true,
                onClick = {},
                selectedContainerColor = TDTheme.colors.pendingGray,
                selectedContentColor = TDTheme.colors.white,
            )
            TDChoiceChip(
                label = "15 dk",
                selected = false,
                onClick = {},
                selectedContainerColor = TDTheme.colors.pendingGray,
                selectedContentColor = TDTheme.colors.white,
            )
        }
    }
}
