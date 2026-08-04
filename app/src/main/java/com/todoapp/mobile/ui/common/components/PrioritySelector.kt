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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDTheme

/** Priority picker for group tasks (None / Low / Medium / High). `null` = no priority. */
@Composable
fun PrioritySelector(
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options: List<Pair<String?, String>> = listOf(
        null to stringResource(R.string.priority_none),
        "LOW" to "LOW",
        "MEDIUM" to "MED",
        "HIGH" to "HIGH",
    )
    Column(modifier = modifier.fillMaxWidth()) {
        TDText(
            text = stringResource(R.string.priority),
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                PriorityChip(
                    value = value,
                    label = label,
                    isSelected = selected == value,
                    onClick = { onSelect(value) },
                )
            }
        }
    }
}

@Composable
private fun PriorityChip(
    value: String?,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    // The chromatic kits show the classic red/orange/blue ramp — an NES palette is small but
    // maximally saturated, so PIXEL belongs here. MONOCHROME alone keeps HIGH red and encodes
    // MEDIUM/LOW/None by gray-ink intensity (the text labels disambiguate either way).
    val (bg, fg) = when (TDTheme.palette) {
        PaletteKit.ORIGINAL, PaletteKit.PIXEL -> when (value?.uppercase()) {
            "HIGH" -> TDTheme.colors.lightRed to TDTheme.colors.crossRed
            "MEDIUM" -> TDTheme.colors.lightOrange to TDTheme.colors.orange
            "LOW" -> TDTheme.colors.lightPending to TDTheme.colors.darkPending
            else -> TDTheme.colors.lightPending to TDTheme.colors.pendingGray
        }
        PaletteKit.MONOCHROME -> when (value?.uppercase()) {
            "HIGH" -> TDTheme.colors.lightRed to TDTheme.colors.crossRed
            "MEDIUM" -> TDTheme.colors.lightPending to TDTheme.colors.darkPending
            "LOW" -> TDTheme.colors.lightPending to TDTheme.colors.gray
            else -> TDTheme.colors.lightPending to TDTheme.colors.mediumPending
        }
    }
    val containerBg = if (isSelected) bg else bg.copy(alpha = 0.35f)
    val contentColor = if (isSelected) fg else fg.copy(alpha = 0.6f)
    Box(
        modifier = Modifier
            .clip(TDTheme.shapes.small)
            .background(containerBg)
            .then(
                if (isSelected) {
                    Modifier.border(TDTheme.style.borderWidth, TDTheme.colors.pendingGray, TDTheme.shapes.small)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        TDText(
            text = label,
            style = TDTheme.typography.subheading1,
            color = contentColor,
        )
    }
}

@TDPreview
@Composable
private fun PrioritySelectorPreview() {
    TDTheme {
        Column(
            modifier = Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PrioritySelector(selected = null, onSelect = {})
            PrioritySelector(selected = "LOW", onSelect = {})
            PrioritySelector(selected = "MEDIUM", onSelect = {})
            PrioritySelector(selected = "HIGH", onSelect = {})
        }
    }
}
