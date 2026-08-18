package com.todoapp.uikit.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDTheme

/**
 * Colour overrides for the celebration animation, or `null` to play it as authored.
 *
 * The confetti's colours live inside its JSON, which puts it out of reach of every token in the app.
 * That is fine for the three chromatic kits — a burst of colour is the point — but the Terminal kit
 * has one phosphor on screen and a multicoloured shower reads as a different app briefly taking over.
 * There, every layer is driven to the kit's own accent instead.
 *
 * `**` matches every keypath in the composition, so this survives the animation being re-authored.
 */
@Composable
fun tdConfettiProperties(): LottieDynamicProperties? {
    if (TDTheme.palette != PaletteKit.TERMINAL) {
        return null
    }
    val phosphor = TDTheme.colors.primary.toArgb()
    return rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = phosphor,
            keyPath = arrayOf("**"),
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = phosphor,
            keyPath = arrayOf("**"),
        ),
    )
}
