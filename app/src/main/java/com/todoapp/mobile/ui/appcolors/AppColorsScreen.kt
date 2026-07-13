package com.todoapp.mobile.ui.appcolors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.appcolors.AppColorsContract.UiAction
import com.todoapp.mobile.ui.appcolors.AppColorsContract.UiState
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.modifier.gridBackground
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.gridColors
import com.todoapp.uikit.theme.stripColors
import com.example.uikit.R as UikitR

@Composable
fun AppColorsScreen(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    LazyColumn(
        modifier =
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(PaletteKit.entries) { kit ->
            PaletteKitCard(
                kit = kit,
                isSelected = uiState.selected == kit,
                onClick = { onAction(UiAction.OnSelectPalette(kit)) },
            )
        }
    }
}

@Composable
internal fun PaletteKitCard(
    kit: PaletteKit,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    // Each card previews its OWN kit's body (grid for MONOCHROME, plain fill for ORIGINAL) regardless
    // of the currently active theme.
    val (kitBase, kitLine) = kit.gridColors(TDTheme.isDark)
    val borderColor = if (isSelected) TDTheme.colors.primary else TDTheme.colors.lightGray
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .gridBackground(baseColor = kitBase, lineColor = kitLine)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, shape)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                TDText(
                    text = stringResource(paletteTitleRes(kit)),
                    style = TDTheme.typography.heading6,
                    color = TDTheme.colors.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                TDText(
                    text = stringResource(paletteDescRes(kit)),
                    style = TDTheme.typography.subheading2,
                    color = TDTheme.colors.gray,
                )
            }
            if (isSelected) {
                Spacer(Modifier.width(12.dp))
                Icon(
                    painter = painterResource(UikitR.drawable.ic_check),
                    contentDescription = null,
                    tint = TDTheme.colors.primary,
                )
            }
        }
        // Full-bleed palette strip: touching (no-gap) segments filling the card's rounded bottom.
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
        ) {
            kit.stripColors(TDTheme.isDark).forEach { swatch ->
                Box(
                    modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(swatch),
                )
            }
        }
    }
}

private fun paletteTitleRes(kit: PaletteKit): Int = when (kit) {
    PaletteKit.ORIGINAL -> R.string.palette_original
    PaletteKit.MONOCHROME -> R.string.palette_monochrome
}

private fun paletteDescRes(kit: PaletteKit): Int = when (kit) {
    PaletteKit.ORIGINAL -> R.string.palette_original_desc
    PaletteKit.MONOCHROME -> R.string.palette_monochrome_desc
}

@TDPreview
@Composable
private fun AppColorsOriginalPreview() {
    TDTheme {
        AppColorsScreen(uiState = UiState(selected = PaletteKit.ORIGINAL), onAction = {})
    }
}

@TDPreview
@Composable
private fun AppColorsMonochromePreview() {
    TDTheme {
        AppColorsScreen(uiState = UiState(selected = PaletteKit.MONOCHROME), onAction = {})
    }
}
