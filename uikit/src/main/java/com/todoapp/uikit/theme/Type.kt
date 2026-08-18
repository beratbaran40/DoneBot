package com.todoapp.uikit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.uikit.R

internal val LocalTypography = staticCompositionLocalOf { TDTypography() }

val Poppins =
    FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_medium, FontWeight.Medium),
        Font(R.font.poppins_semi_bold, FontWeight.SemiBold),
        Font(R.font.poppins_bold, FontWeight.Bold),
    )

/**
 * The 8-Bit kit's face (Pixelify Sans, SIL OFL 1.1) — a pixel typeface drawn for UI sizes rather
 * than a strict 8x8 arcade grid, which is what keeps this app's dense 12sp rows legible.
 *
 * Only the two authored masters ship. Interpolating a pixel outline lands its stems off the pixel
 * grid and turns them to mush, so `W500` resolves to Regular and `W600`/`W800` to Bold via Compose's
 * nearest-weight matching — a deliberate two-tier hierarchy, which is period-correct anyway.
 */
val PixelifySans =
    FontFamily(
        Font(R.font.pixelify_sans_regular, FontWeight.Normal),
        Font(R.font.pixelify_sans_bold, FontWeight.Bold),
    )

/**
 * The Terminal kit's face (JetBrains Mono, SIL OFL 1.1) — a monospace drawn for code at UI sizes,
 * which is what lets this app's dense 10-14sp rows survive a face whose advances run wider than
 * Poppins'.
 *
 * All four masters the type ramp asks for ship, subset to latin + latin-ext by `tools/prep_fonts.sh`
 * (the app's only locales are en and tr). The ligature-free "NL" masters are used deliberately:
 * JetBrains Mono's coding ligatures would rewrite a task titled "A -> B" into an arrow glyph, and a
 * substitution is something `tools/textfit.py` cannot model.
 */
val JetBrainsMono =
    FontFamily(
        Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
        Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
        Font(R.font.jetbrains_mono_semi_bold, FontWeight.SemiBold),
        Font(R.font.jetbrains_mono_extra_bold, FontWeight.ExtraBold),
    )

/**
 * Raises only the small end of the type ramp. Pixel typefaces have a fixed one-pixel stem; below
 * roughly 12sp the stems land between device pixels and the glyphs turn to mush. Styles at 14sp and
 * above are left alone so nothing laid out against a heading size moves. `0.sp` is the identity.
 */
private fun TextUnit.atLeast(min: TextUnit): TextUnit = if (value < min.value) min else this

/**
 * The app's text styles. The colour of each style already follows the active kit (every getter reads
 * `TDTheme.colors`); [fontFamily], [displayFontFamily] and [minFontSize] let the *face* follow it
 * too. All three default to today's values, so `TDTypography()` is unchanged.
 */
@Immutable
class TDTypography(
    private val fontFamily: FontFamily = Poppins,
    private val displayFontFamily: FontFamily = fontFamily,
    private val minFontSize: TextUnit = 0.sp,
    private val fontScale: Float = 1f,
) {
    /** The kit's size multiplier alone. Identity at `1f`, down to the same instance. */
    private fun TextUnit.kitScaled(): TextUnit = if (fontScale == 1f) this else (value * fontScale).sp

    /**
     * The kit's full size transform: **scale first, floor second**. The floor is an absolute
     * legibility minimum in real sp, so flooring first would just let the scale push the value back
     * under it. `fontScale = 1f` with `minFontSize = 0.sp` returns the receiver untouched, which is
     * why applying this to every style leaves the existing kits byte-identical.
     */
    private fun TextUnit.kitSize(): TextUnit = kitScaled().atLeast(minFontSize)

    val pomodoro: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = displayFontFamily,
                fontSize = 96.sp.kitSize(),
                fontWeight = FontWeight.W800,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.pendingGray,
            )
    val heading1: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = 24.sp.kitSize(),
                fontWeight = FontWeight.W600,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val heading2: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = 22.sp.kitSize(),
                fontWeight = FontWeight.W600,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val heading3: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = 18.sp.kitSize(),
                fontWeight = FontWeight.W600,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val heading4: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = 18.sp.kitSize(),
                fontWeight = FontWeight.W500,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val heading5: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = 16.sp.kitSize(),
                fontWeight = FontWeight.W600,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val heading6: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = 16.sp.kitSize(),
                fontWeight = FontWeight.W500,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val heading7: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = 14.sp.kitSize(),
                fontWeight = FontWeight.W500,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val dayOfTheCalendar: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = 12.sp.kitSize(),
                fontWeight = FontWeight.W600,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )

    val regularTextStyle: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = 14.sp.kitSize(),
                fontWeight = FontWeight.W500,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val subheading1: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = 12.sp.kitSize(),
                fontWeight = FontWeight.W400,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val subheading2: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = 10.sp.kitSize(),
                fontWeight = FontWeight.W400,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
            )
    val subheading3: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = 14.sp.kitSize(),
                fontWeight = FontWeight.W500,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
    val subheading4: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = 12.sp.kitSize(),
                fontWeight = FontWeight.W500,
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.brown,
            )

    /**
     * Body text used for the journal entry editor. The relaxed 28sp lineHeight gives the writing
     * surface generous, handwriting-style breathing room between lines.
     */
    val journalHandwritingStyle: TextStyle
        @Composable
        get() =
            TextStyle(
                fontFamily = fontFamily,
                fontSize = 16.sp.kitSize(),
                fontWeight = FontWeight.Normal,
                lineHeight = 28.sp.kitScaled(),
                lineHeightStyle =
                LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
                color = TDTheme.colors.onBackground,
            )
}
