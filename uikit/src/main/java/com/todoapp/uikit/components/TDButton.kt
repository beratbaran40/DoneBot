@file:Suppress("TooManyFunctions")

package com.todoapp.uikit.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.previews.TDPreviewNoBg
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDTheme

/**
 * Button label style. Deliberately built here rather than reused from `TDTheme.typography` — those
 * styles also carry a `lineHeightStyle` these labels never had, which would nudge the baseline. The
 * size and weight are unchanged; only the family now follows the active kit (it was a private
 * top-level `FontFamily` val, which cannot read composition and so pinned every label to Poppins).
 */
@Composable
private fun buttonTextStyle(size: TDButtonSize): TextStyle {
    val small = size == TDButtonSize.SMALL
    return TextStyle(
        // The kit's scale is applied by hand because this style never passes through TDTypography.
        // It matters most here: a button label is centred, single-line and boxed, which makes it the
        // tightest slot in the app (see tools/textfit.py). The small-end floor is deliberately NOT
        // applied — these sizes are 14sp and up, well clear of any kit's floor.
        fontSize = ((if (small) 14f else 18f) * TDTheme.style.fontScale).sp,
        fontWeight = if (small) FontWeight.Medium else FontWeight.SemiBold,
        fontFamily = TDTheme.style.fontFamily,
    )
}

private val buttonSmallPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
private val buttonMediumPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)

enum class TDButtonType { PRIMARY, SECONDARY, OUTLINE, CANCEL, PENDING }

enum class TDButtonSize { SMALL, MEDIUM }

/**
 * ORIGINAL keeps the classic pendingGray fill. MONOCHROME uses a black fill in light / gray in dark.
 * PIXEL and TERMINAL are a saturated accent block — an 8-bit button is a solid colour field rather
 * than neutral chrome, and a terminal's focused control is inverse video, which comes to the same
 * thing.
 *
 * Shared with [TDIconButton] rather than copied: one exhaustive `when` means a fourth kit is a compile
 * error in one place, not a compile error here and a silently wrong branch there.
 */
@Composable
internal fun tdPrimaryFill(): Color = when (TDTheme.palette) {
    PaletteKit.ORIGINAL -> TDTheme.colors.pendingGray
    PaletteKit.MONOCHROME -> if (TDTheme.isDark) TDTheme.colors.gray else TDTheme.colors.black
    PaletteKit.PIXEL, PaletteKit.TERMINAL -> TDTheme.colors.primary
}

/** The label/icon colour that goes on [tdPrimaryFill]. */
@Composable
internal fun tdPrimaryContent(): Color = when (TDTheme.palette) {
    PaletteKit.ORIGINAL -> TDTheme.colors.white
    PaletteKit.MONOCHROME, PaletteKit.PIXEL, PaletteKit.TERMINAL -> TDTheme.colors.onPrimary
}

@Composable
fun TDButton(
    modifier: Modifier = Modifier,
    text: String,
    isEnable: Boolean = true,
    type: TDButtonType = TDButtonType.PRIMARY,
    size: TDButtonSize = TDButtonSize.MEDIUM,
    icon: Painter? = null,
    fullWidth: Boolean = false,
    /**
     * Unbounded on purpose. The app's policy is "grow, don't cut": the button's height is a
     * `heightIn(min=)`, so a label that needs two lines gets two lines and the button grows with it.
     * Do NOT pass 1 to make something fit — that trades a visible wrap for silent truncation, and
     * the Turkish labels this would bite are the ones users most need to read. If a label genuinely
     * cannot fit, weight the button, widen the slot, or shorten the string.
     */
    maxLines: Int = Int.MAX_VALUE,
    onClick: () -> Unit,
) {
    val textStyle = buttonTextStyle(size)
    val height = if (size == TDButtonSize.SMALL) 40.dp else 60.dp
    val width = if (size == TDButtonSize.SMALL) 140.dp else 200.dp
    val paddingValues = if (size == TDButtonSize.SMALL) buttonSmallPadding else buttonMediumPadding

    // The width floor comes AFTER `modifier`, which looks like it would override the caller but does
    // not: `widthIn` enforces the incoming constraints, so a caller's `.weight(1f)` or `.width(x)`
    // arrives as an exact constraint and the floor is coerced into it. Two unweighted buttons in a
    // Row are likewise coerced rather than overflowing — the second one just comes out narrower.
    // That is why the fix for a cramped button row is `weight`, never a smaller floor here.
    val sizeModifier =
        Modifier
            .heightIn(min = height)
            .then(modifier)
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier.widthIn(min = width))

    when (type) {
        TDButtonType.PRIMARY -> {
            val primaryFill = tdPrimaryFill()
            val primaryContent = tdPrimaryContent()
            Button(
                modifier = sizeModifier,
                onClick = onClick,
                enabled = isEnable,
                contentPadding = paddingValues,
                shape = TDTheme.shapes.medium,
                colors =
                ButtonColors(
                    containerColor = primaryFill,
                    contentColor = primaryContent,
                    disabledContainerColor = primaryFill.copy(alpha = 0.4f),
                    disabledContentColor = primaryContent.copy(alpha = 0.5f),
                ),
            ) {
                icon?.let {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = icon,
                        contentDescription = text,
                        tint = Color.Unspecified,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TDText(
                    text = text,
                    style = textStyle,
                    color = primaryContent,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        TDButtonType.SECONDARY -> {
            OutlinedButton(
                modifier = sizeModifier,
                onClick = onClick,
                enabled = isEnable,
                contentPadding = paddingValues,
                shape = TDTheme.shapes.medium,
                border =
                BorderStroke(
                    2.dp,
                    if (isEnable) TDTheme.colors.crossRed else TDTheme.colors.crossRed.copy(alpha = 0.3f),
                ),
            ) {
                icon?.let {
                    Icon(
                        modifier = Modifier.size(14.dp),
                        painter = icon,
                        contentDescription = text,
                        tint = TDTheme.colors.crossRed,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TDText(
                    text = text,
                    style = textStyle,
                    color = if (isEnable) TDTheme.colors.crossRed else TDTheme.colors.crossRed.copy(alpha = 0.4f),
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        TDButtonType.OUTLINE -> {
            OutlinedButton(
                modifier = sizeModifier,
                onClick = onClick,
                enabled = isEnable,
                contentPadding = paddingValues,
                shape = TDTheme.shapes.medium,
                border =
                BorderStroke(
                    width = 1.5.dp,
                    color =
                    if (isEnable) {
                        TDTheme.colors.pendingGray
                    } else {
                        TDTheme.colors.pendingGray.copy(
                            alpha = 0.3f,
                        )
                    },
                ),
            ) {
                icon?.let {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = icon,
                        contentDescription = text,
                        tint = Color.Unspecified,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TDText(
                    text = text,
                    style = textStyle,
                    color = if (isEnable) TDTheme.colors.pendingGray else TDTheme.colors.pendingGray.copy(alpha = 0.5f),
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        TDButtonType.CANCEL -> {
            Button(
                modifier = sizeModifier,
                onClick = onClick,
                enabled = isEnable,
                contentPadding = paddingValues,
                shape = TDTheme.shapes.medium,
                colors =
                ButtonColors(
                    containerColor = TDTheme.colors.crossRed,
                    contentColor = TDTheme.colors.white,
                    disabledContainerColor = TDTheme.colors.red.copy(alpha = 0.5f),
                    disabledContentColor = TDTheme.colors.white.copy(alpha = 0.5f),
                ),
            ) {
                icon?.let {
                    Icon(
                        modifier = Modifier.size(14.dp),
                        painter = icon,
                        contentDescription = text,
                        tint = TDTheme.colors.white,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TDText(
                    text = text,
                    style = textStyle,
                    color = TDTheme.colors.white,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        TDButtonType.PENDING -> {
            Button(
                modifier = sizeModifier,
                onClick = onClick,
                enabled = isEnable,
                contentPadding = paddingValues,
                shape = TDTheme.shapes.medium,
                colors =
                ButtonColors(
                    containerColor = TDTheme.colors.pendingGray,
                    contentColor = TDTheme.colors.white,
                    disabledContainerColor = TDTheme.colors.pendingGray.copy(alpha = 0.4f),
                    disabledContentColor = TDTheme.colors.white.copy(alpha = 0.5f),
                ),
            ) {
                icon?.let {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = icon,
                        contentDescription = text,
                        tint = TDTheme.colors.white,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TDText(
                    text = text,
                    style = textStyle,
                    color = TDTheme.colors.white,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@TDPreviewNoBg
@Composable
fun TDButtonPreview() {
    Column {
        TDButton(
            text = "Primary Button",
            onClick = {},
            type = TDButtonType.PRIMARY,
            size = TDButtonSize.SMALL,
        )

        Spacer(modifier = Modifier.height(8.dp))

        TDButton(
            text = "Primary Button 2",
            onClick = {},
            type = TDButtonType.PRIMARY,
            size = TDButtonSize.MEDIUM,
            fullWidth = true,
            isEnable = false,
        )

        Spacer(modifier = Modifier.height(8.dp))

        TDButton(
            text = "Secondary Button",
            onClick = {},
            type = TDButtonType.SECONDARY,
            size = TDButtonSize.SMALL,
        )

        Spacer(modifier = Modifier.height(8.dp))

        TDButton(
            text = "Secondary Button2",
            onClick = {},
            type = TDButtonType.SECONDARY,
            size = TDButtonSize.MEDIUM,
            fullWidth = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        TDButton(
            text = "Outline Button",
            onClick = {},
            type = TDButtonType.OUTLINE,
            size = TDButtonSize.SMALL,
        )

        Spacer(modifier = Modifier.height(8.dp))

        TDButton(
            text = "Outline Button2",
            onClick = {},
            type = TDButtonType.OUTLINE,
            size = TDButtonSize.MEDIUM,
            fullWidth = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        TDButton(
            text = "Cancel Button",
            onClick = {},
            type = TDButtonType.CANCEL,
            size = TDButtonSize.SMALL,
        )

        Spacer(modifier = Modifier.height(8.dp))

        TDButton(
            text = "Cancel Button2",
            onClick = {},
            type = TDButtonType.CANCEL,
            size = TDButtonSize.MEDIUM,
            fullWidth = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        TDButton(
            text = "Pending Button",
            onClick = {},
            type = TDButtonType.PENDING,
            size = TDButtonSize.SMALL,
        )

        Spacer(modifier = Modifier.height(8.dp))

        TDButton(
            text = "Pending Button2",
            onClick = {},
            type = TDButtonType.PENDING,
            size = TDButtonSize.MEDIUM,
            fullWidth = true,
        )
    }
}

@TDPreview
@Composable
private fun TDButtonPrimaryPreview() {
    TDTheme {
        TDButton(text = "Save", onClick = {}, type = TDButtonType.PRIMARY)
    }
}

@TDPreview
@Composable
private fun TDButtonSecondaryPreview() {
    TDTheme {
        TDButton(text = "Cancel", onClick = {}, type = TDButtonType.SECONDARY)
    }
}

@TDPreview
@Composable
private fun TDButtonOutlinePreview() {
    TDTheme {
        TDButton(text = "Skip", onClick = {}, type = TDButtonType.OUTLINE)
    }
}

@TDPreview
@Composable
private fun TDButtonCancelPreview() {
    TDTheme {
        TDButton(text = "Delete", onClick = {}, type = TDButtonType.CANCEL)
    }
}

@TDPreview
@Composable
private fun TDButtonPendingPreview() {
    TDTheme {
        TDButton(text = "Saving...", onClick = {}, type = TDButtonType.PENDING)
    }
}

@TDPreview
@Composable
private fun TDButtonDisabledPreview() {
    TDTheme {
        TDButton(text = "Save", onClick = {}, type = TDButtonType.PRIMARY, isEnable = false)
    }
}

@TDPreview
@Composable
private fun TDButtonWithIconPreview() {
    TDTheme {
        TDButton(
            text = "Add",
            onClick = {},
            type = TDButtonType.PRIMARY,
            icon = tdPainter(id = R.drawable.ic_plus),
        )
    }
}

@TDPreview
@Composable
private fun TDButtonFullWidthPreview() {
    TDTheme {
        TDButton(text = "Continue", onClick = {}, type = TDButtonType.PRIMARY, fullWidth = true)
    }
}

@TDPreview
@Composable
private fun TDButtonSmallPreview() {
    TDTheme {
        TDButton(text = "OK", onClick = {}, type = TDButtonType.PRIMARY, size = TDButtonSize.SMALL)
    }
}

// buttonTextStyle applies the kit's fontScale by hand, so this is the one label in the app whose size
// is not covered by TDTypography's own previews.
@TDPreview
@Composable
private fun TDButtonTerminalPreview() {
    TDTheme(palette = PaletteKit.TERMINAL) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TDButton(text = "Continue", onClick = {}, type = TDButtonType.PRIMARY, fullWidth = true)
            TDButton(text = "OK", onClick = {}, type = TDButtonType.PRIMARY, size = TDButtonSize.SMALL)
        }
    }
}
