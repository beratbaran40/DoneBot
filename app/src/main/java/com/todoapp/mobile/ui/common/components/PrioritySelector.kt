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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreview
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
    val (bg, fg) = when (value?.uppercase()) {
        "HIGH" -> TDTheme.colors.lightRed to TDTheme.colors.crossRed
        "MEDIUM" -> TDTheme.colors.lightOrange to TDTheme.colors.orange
        "LOW" -> TDTheme.colors.lightPending to TDTheme.colors.darkPending
        else -> TDTheme.colors.lightPending to TDTheme.colors.pendingGray
    }
    val containerBg = if (isSelected) bg else bg.copy(alpha = 0.35f)
    val contentColor = if (isSelected) fg else fg.copy(alpha = 0.6f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerBg)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, TDTheme.colors.pendingGray, RoundedCornerShape(8.dp))
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
