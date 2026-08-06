package com.todoapp.uikit.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.previews.TDPreviewNarrow
import com.todoapp.uikit.theme.TDTheme

/**
 * The platform minimum touch target, and what M3 expands any smaller control to anyway. Public so a
 * row that mixes these with a labelled [TDButton] can pin the labelled one to the same height.
 */
val TDIconButtonSize: Dp = 48.dp

/** Matches [TDButton]'s leading-icon size, so a labelled and an icon-only button read at one scale. */
private val TD_ICON_BUTTON_GLYPH_SIZE: Dp = 24.dp

/**
 * A square, icon-only button: the same fills, borders and disabled alphas as [TDButton], with the
 * label removed and the box locked to a touch-target-sized square.
 *
 * It exists because [TDButton] requires a `text` and applies a 140dp minimum width, so squeezing an
 * icon-only control out of it takes a fake empty label and a fight with the size modifier. Where three
 * labelled buttons will not fit a narrow row — the date picker's footer is the first such place — the
 * two most recognisable ones lose their labels instead of wrapping.
 *
 * [contentDescription] is required and non-null on purpose. An icon-only control has no label, so an
 * omitted description is not a style nit: it is a button TalkBack cannot name. Pass the same string
 * the labelled equivalent would have used.
 */
@Composable
fun TDIconButton(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnable: Boolean = true,
    type: TDButtonType = TDButtonType.PRIMARY,
    size: Dp = TDIconButtonSize,
    iconSize: Dp = TD_ICON_BUTTON_GLYPH_SIZE,
) {
    val sizeModifier = modifier.size(size)
    // Zero padding: the box is already the touch target, and M3's default content padding would push
    // the glyph off-centre inside it.
    val noPadding = PaddingValues(0.dp)

    when (type) {
        TDButtonType.PRIMARY -> {
            val fill = tdPrimaryFill()
            val content = tdPrimaryContent()
            Button(
                modifier = sizeModifier,
                onClick = onClick,
                enabled = isEnable,
                contentPadding = noPadding,
                shape = TDTheme.shapes.medium,
                colors = ButtonColors(
                    containerColor = fill,
                    contentColor = content,
                    disabledContainerColor = fill.copy(alpha = 0.4f),
                    disabledContentColor = content.copy(alpha = 0.5f),
                ),
            ) {
                Icon(
                    modifier = Modifier.size(iconSize),
                    painter = icon,
                    contentDescription = contentDescription,
                    tint = content,
                )
            }
        }

        TDButtonType.CANCEL -> {
            Button(
                modifier = sizeModifier,
                onClick = onClick,
                enabled = isEnable,
                contentPadding = noPadding,
                shape = TDTheme.shapes.medium,
                colors = ButtonColors(
                    containerColor = TDTheme.colors.crossRed,
                    contentColor = TDTheme.colors.white,
                    disabledContainerColor = TDTheme.colors.red.copy(alpha = 0.5f),
                    disabledContentColor = TDTheme.colors.white.copy(alpha = 0.5f),
                ),
            ) {
                Icon(
                    modifier = Modifier.size(iconSize),
                    painter = icon,
                    contentDescription = contentDescription,
                    tint = TDTheme.colors.white,
                )
            }
        }

        // 1.5dp rather than TDTheme.style.borderWidth, matching TDButton.OUTLINE: these sit side by
        // side with labelled buttons, so agreeing with them beats agreeing with the kit token.
        TDButtonType.OUTLINE, TDButtonType.SECONDARY, TDButtonType.PENDING -> {
            val accent = when (type) {
                TDButtonType.SECONDARY -> TDTheme.colors.crossRed
                TDButtonType.PENDING -> TDTheme.colors.orange
                else -> TDTheme.colors.pendingGray
            }
            OutlinedButton(
                modifier = sizeModifier,
                onClick = onClick,
                enabled = isEnable,
                contentPadding = noPadding,
                shape = TDTheme.shapes.medium,
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (isEnable) accent else accent.copy(alpha = 0.3f),
                ),
            ) {
                Icon(
                    modifier = Modifier.size(iconSize),
                    painter = icon,
                    contentDescription = contentDescription,
                    tint = if (isEnable) accent else accent.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@TDPreview
@Composable
private fun TdIconButtonTypesPreview() {
    TDTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TDIconButton(
                icon = tdPainter(R.drawable.ic_check),
                contentDescription = "OK",
                onClick = {},
            )
            TDIconButton(
                icon = tdPainter(R.drawable.ic_close),
                contentDescription = "Cancel",
                type = TDButtonType.OUTLINE,
                onClick = {},
            )
            TDIconButton(
                icon = tdPainter(R.drawable.ic_close),
                contentDescription = "Delete",
                type = TDButtonType.CANCEL,
                onClick = {},
            )
        }
    }
}

@TDPreview
@Composable
private fun TdIconButtonDisabledPreview() {
    TDTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TDIconButton(
                icon = tdPainter(R.drawable.ic_check),
                contentDescription = "OK",
                isEnable = false,
                onClick = {},
            )
            TDIconButton(
                icon = tdPainter(R.drawable.ic_close),
                contentDescription = "Cancel",
                type = TDButtonType.OUTLINE,
                isEnable = false,
                onClick = {},
            )
        }
    }
}

/**
 * The arrangement these were built for: a labelled button taking the free width with the two squares
 * pinned beside it. If the label wraps here it will wrap in the date picker too.
 */
@TDPreviewNarrow
@Composable
private fun TdIconButtonBesideLabelPreview() {
    TDTheme {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TDButton(
                modifier = Modifier.weight(1f).height(TDIconButtonSize),
                text = "Select today",
                type = TDButtonType.OUTLINE,
                size = TDButtonSize.SMALL,
                maxLines = 1,
                fullWidth = true,
                onClick = {},
            )
            TDIconButton(
                icon = tdPainter(R.drawable.ic_close),
                contentDescription = "Cancel",
                type = TDButtonType.OUTLINE,
                onClick = {},
            )
            TDIconButton(
                icon = tdPainter(R.drawable.ic_check),
                contentDescription = "OK",
                onClick = {},
            )
        }
    }
}
