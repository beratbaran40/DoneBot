package com.todoapp.mobile.ui.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.uikit.components.TDCompactOutlinedTextField
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner
import com.example.uikit.R as UiKitR

/**
 * Full staged-step editor for the detail screen: rename inline, toggle completion, add (trailing
 * empty row), and remove — mirrors the Creation Hub step editor but adds per-step completion + X/Y.
 */
@Composable
internal fun DetailsSubtaskEditor(
    drafts: List<SubtaskDraft>,
    onTitleChange: (Int, String) -> Unit,
    onToggle: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = drafts.count { it.title.isNotBlank() }
    val done = drafts.count { it.title.isNotBlank() && it.isCompleted }
    val connectorColor = TDTheme.colors.pendingGray.copy(alpha = 0.5f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TDText(
            text = stringResource(R.string.creation_steps_label) + "  $done/$total",
            style = TDTheme.typography.heading6.copy(fontWeight = FontWeight.SemiBold),
            color = TDTheme.colors.onBackground,
        )
        Column {
            drafts.forEachIndexed { index, draft ->
                if (index > 0) DashedConnector(connectorColor)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (draft.id != null) {
                        StepCheckbox(checked = draft.isCompleted, onClick = { onToggle(index) })
                        Spacer(Modifier.width(8.dp))
                    }
                    TDCompactOutlinedTextField(
                        value = draft.title,
                        placeholder = stringResource(R.string.creation_add_step_placeholder),
                        onValueChange = { onTitleChange(index, it) },
                        modifier = Modifier.weight(1f),
                    )
                    // The trailing empty row is the "add" affordance and has nothing to remove.
                    if (index != drafts.lastIndex) {
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(
                                painter = tdPainter(UiKitR.drawable.ic_delete),
                                contentDescription = stringResource(R.string.creation_remove_step_cd),
                                tint = TDTheme.colors.crossRed,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCheckbox(
    checked: Boolean,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(24.dp)
            .clip(tdCorner(6.dp))
            .background(if (checked) TDTheme.colors.mediumGreen else TDTheme.colors.mediumGreen.copy(alpha = 0.08f))
            .border(
                width = 1.5.dp,
                color = if (checked) TDTheme.colors.mediumGreen else TDTheme.colors.pendingGray.copy(alpha = 0.5f),
                shape = tdCorner(6.dp),
            )
            .clickable(onClick = onClick),
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
}

@Composable
private fun DashedConnector(color: Color) {
    Canvas(
        modifier = Modifier
            .padding(start = 16.dp)
            .width(2.dp)
            .height(14.dp),
    ) {
        drawLine(
            color = color,
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = size.width,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_ON, DASH_OFF)),
        )
    }
}

private const val DASH_ON = 4f
private const val DASH_OFF = 6f

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun DetailsSubtaskEditorPreview() {
    TDTheme {
        DetailsSubtaskEditor(
            drafts = listOf(
                SubtaskDraft(1L, "Giriş", true),
                SubtaskDraft(2L, "Yöntem", false),
                SubtaskDraft(null, "", false),
            ),
            onTitleChange = { _, _ -> },
            onToggle = {},
            onRemove = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
