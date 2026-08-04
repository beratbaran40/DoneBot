package com.todoapp.uikit.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoapp.uikit.modifier.GridStyle
import kotlin.math.floor

/**
 * The non-colour half of a [PaletteKit]: corner language, typeface, border weight, elevation
 * treatment, background grid and motion. Provided once by [TDTheme]; read through `TDTheme.style`,
 * `TDTheme.shapes` and `TDTheme.motion`.
 *
 * A single local (rather than separate `LocalShapes` / `LocalMotion` / …) is deliberate: the only
 * thing that ever changes these tokens is a palette switch — a rare, user-initiated event already
 * wrapped in a full-screen curtain wipe — so `staticCompositionLocalOf`'s coarse subtree
 * invalidation is exactly the behaviour we want, and adding a kit stays a one-line change.
 */
internal val LocalStyle = staticCompositionLocalOf { defaultStyle() }

/** How corners are cut. Also drives the [tdCorner] escape hatch. */
enum class TDCornerStyle { ROUNDED, PIXEL }

/** How elevation is rendered: [SOFT] is the blurred neumorphic shadow, [HARD] a zero-blur block. */
enum class TDElevationStyle { SOFT, HARD }

/**
 * Semantic corner slots. Each maps to the literal radius that dominates that role today, so
 * substituting `RoundedCornerShape(12.dp)` → `TDTheme.shapes.medium` is a no-op for the rounded kits.
 *
 * [pill] and [circle] are separate even though both are `CircleShape` in the rounded kits: a pixel
 * kit wants a stadium for capsules but a stepped octagon for true circles, and keeping them apart
 * means a wrong pick during migration is invisible until a pixel kit exists.
 */
@Immutable
data class TDShapes(
    val none: Shape,
    val tiny: Shape,
    val small: Shape,
    val medium: Shape,
    val large: Shape,
    val xLarge: Shape,
    val pill: Shape,
    val circle: Shape,
    val cornerStyle: TDCornerStyle,
)

/**
 * Motion language. [revealEasing] is kept separate from [standardEasing] because the theme-change
 * curtain wipe is linear in every kit today and must stay that way for the rounded kits.
 */
@Immutable
data class TDMotion(
    val standardEasing: Easing,
    val emphasizedEasing: Easing,
    val revealEasing: Easing,
    /** True when animations should tick in discrete frames rather than glide. */
    val stepped: Boolean,
)

@Immutable
data class TDStyle(
    val shapes: TDShapes,
    val motion: TDMotion,
    /** Family behind every `TDTheme.typography.*` style. */
    val fontFamily: FontFamily,
    /** Family for display-scale numerals (the 96sp `pomodoro` hero). */
    val displayFontFamily: FontFamily,
    /**
     * Pushed into M3's `LocalTextStyle` so the handful of bare `Text(...)` / `TextField` sites that
     * never set a style still pick up the kit's face. `null` leaves `LocalTextStyle` untouched,
     * which is exactly today's behaviour.
     */
    val fallbackFontFamily: FontFamily?,
    /** Floor applied to the small end of the type ramp only. `0.sp` is the identity. */
    val minFontSize: TextUnit,
    val borderWidth: Dp,
    val elevationStyle: TDElevationStyle,
    /** Hard drop-shadow offset; used only when [elevationStyle] is [TDElevationStyle.HARD]. */
    val hardShadowOffset: Dp,
    val gridStyle: GridStyle,
    val gridSpacing: Dp,
    val gridLineWidth: Dp,
)

/**
 * Quantises progress into [steps] discrete jumps so an animation ticks like a sprite sheet instead
 * of gliding. Six to eight steps reads as 8-bit without feeling broken; one step is a hard cut.
 * `floor` gives a "hold, then snap" feel — the final step lands only at the very end of the run.
 */
@Immutable
data class SteppedEasing(
    private val steps: Int,
    private val base: Easing = LinearEasing,
) : Easing {
    override fun transform(fraction: Float): Float {
        val stepCount = steps.coerceAtLeast(1)
        return (floor(base.transform(fraction) * stepCount) / stepCount).coerceIn(0f, 1f)
    }
}

private fun roundedShapes(): TDShapes = TDShapes(
    none = RectangleShape,
    tiny = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    xLarge = RoundedCornerShape(20.dp),
    pill = CircleShape,
    circle = CircleShape,
    cornerStyle = TDCornerStyle.ROUNDED,
)

private fun softMotion(): TDMotion = TDMotion(
    standardEasing = FastOutSlowInEasing,
    emphasizedEasing = FastOutSlowInEasing,
    revealEasing = LinearEasing,
    stepped = false,
)

/** Tokens for [PaletteKit.ORIGINAL] — and the baseline every other kit is a delta against. */
internal fun defaultStyle(): TDStyle = TDStyle(
    shapes = roundedShapes(),
    motion = softMotion(),
    fontFamily = Poppins,
    displayFontFamily = Poppins,
    fallbackFontFamily = null,
    minFontSize = 0.sp,
    borderWidth = 1.dp,
    elevationStyle = TDElevationStyle.SOFT,
    hardShadowOffset = 0.dp,
    gridStyle = GridStyle.Lines,
    gridSpacing = 24.dp,
    gridLineWidth = 1.dp,
)

/**
 * Tokens for [PaletteKit.MONOCHROME]. That kit differs from ORIGINAL only in colour — its grid comes
 * from a non-transparent `gridLine`, not from different geometry — so it shares the default tokens.
 */
internal fun monochromeStyle(): TDStyle = defaultStyle()

private fun pixelShapes(): TDShapes = TDShapes(
    none = RectangleShape,
    tiny = PixelCornerShape(unit = 2.dp, steps = 1),
    small = PixelCornerShape(unit = 2.dp, steps = 1),
    medium = PixelCornerShape(unit = 3.dp, steps = 2),
    large = PixelCornerShape(unit = 3.dp, steps = 2),
    xLarge = PixelCornerShape(unit = 4.dp, steps = 2),
    // A capsule keeps a longer stair; a true circle needs enough steps to read as a stepped disc.
    pill = PixelCornerShape(unit = 4.dp, steps = 3),
    circle = PixelCornerShape(unit = 4.dp, steps = 3),
    cornerStyle = TDCornerStyle.PIXEL,
)

/**
 * Tokens for [PaletteKit.PIXEL] — square-ish stair corners, a pixel face, chunky borders, zero-blur
 * hard shadows, a tight scanline grid and stepped motion.
 *
 * `minFontSize = 12.sp` lifts only `subheading2` (10sp, the app's most-used style); a global scale
 * would push the 96sp pomodoro hero to 110sp and break the timer layout.
 */
internal fun pixelStyle(): TDStyle = TDStyle(
    shapes = pixelShapes(),
    motion = TDMotion(
        standardEasing = SteppedEasing(steps = 6),
        emphasizedEasing = SteppedEasing(steps = 4),
        revealEasing = SteppedEasing(steps = 8),
        stepped = true,
    ),
    fontFamily = PixelifySans,
    displayFontFamily = PixelifySans,
    fallbackFontFamily = PixelifySans,
    minFontSize = 12.sp,
    borderWidth = 2.dp,
    elevationStyle = TDElevationStyle.HARD,
    hardShadowOffset = 4.dp,
    // Ordered dither, not graph paper: the lattice reads as MONOCHROME's cousin, while a 2dp
    // checkerboard is the texture 8-bit hardware actually used in place of gradients. `gridLineWidth`
    // is unused by the dither path (the cell size comes from `gridSpacing`).
    gridStyle = GridStyle.Dither,
    gridSpacing = 2.dp,
    gridLineWidth = 1.dp,
)

private val PIXEL_UNIT_LARGE = 4.dp
private val PIXEL_UNIT_MEDIUM = 3.dp
private val PIXEL_UNIT_SMALL = 2.dp
private val PIXEL_LARGE_UNIT_MIN = 16.dp
private val PIXEL_TWO_STEP_MIN = 10.dp

/**
 * Border colour for a card-like surface. Kits with soft elevation keep the hairline tint the call
 * site already used — so ORIGINAL and MONOCHROME are untouched — while a hard-elevation kit swaps in
 * flat ink, because a 2dp pale-tint border reads as a smudge next to a zero-blur shadow.
 */
@Composable
@ReadOnlyComposable
fun tdOutlineColor(soft: Color): Color = when (LocalStyle.current.elevationStyle) {
    TDElevationStyle.SOFT -> soft
    TDElevationStyle.HARD -> TDTheme.colors.onBackground
}

/**
 * Palette-aware corner for radii that have no canonical [TDShapes] slot (6, 10, 14, 22 dp, …).
 *
 * For every rounded kit this returns **exactly** `RoundedCornerShape(radius)`, so mechanically
 * substituting `RoundedCornerShape(x.dp)` → `tdCorner(x.dp)` is a provable no-op. Prefer the
 * semantic slots (`TDTheme.shapes.medium`, …) in new code and when retrofitting a canonical radius;
 * never map a literal onto a slot whose canonical dp it does not equal.
 */
@Composable
@ReadOnlyComposable
fun tdCorner(radius: Dp): Shape = when (LocalStyle.current.shapes.cornerStyle) {
    TDCornerStyle.ROUNDED -> RoundedCornerShape(radius)
    TDCornerStyle.PIXEL ->
        PixelCornerShape(
            unit =
            when {
                radius >= PIXEL_LARGE_UNIT_MIN -> PIXEL_UNIT_LARGE
                radius >= PIXEL_TWO_STEP_MIN -> PIXEL_UNIT_MEDIUM
                else -> PIXEL_UNIT_SMALL
            },
            steps = if (radius >= PIXEL_TWO_STEP_MIN) 2 else 1,
        )
}
