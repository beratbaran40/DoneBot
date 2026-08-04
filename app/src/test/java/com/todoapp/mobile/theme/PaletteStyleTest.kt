package com.todoapp.mobile.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoapp.uikit.modifier.GridStyle
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.PixelCornerShape
import com.todoapp.uikit.theme.PixelifySans
import com.todoapp.uikit.theme.Poppins
import com.todoapp.uikit.theme.SteppedEasing
import com.todoapp.uikit.theme.TDCornerStyle
import com.todoapp.uikit.theme.TDElevationStyle
import com.todoapp.uikit.theme.gridColors
import com.todoapp.uikit.theme.stripColors
import com.todoapp.uikit.theme.style
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the invariant the whole kit migration rests on: the rounded kits' style tokens are exactly
 * the literals that used to be hardcoded at each call site.
 *
 * There is no screenshot harness in this project, so "ORIGINAL and MONOCHROME are visually
 * unchanged" is otherwise guaranteed only by the *shape* of each diff. Substituting
 * `RoundedCornerShape(12.dp)` → `TDTheme.shapes.medium` is safe precisely because of the assertions
 * below; if a slot's canonical value ever drifts, every migrated call site drifts silently with it.
 */
class PaletteStyleTest {
    @Test
    fun `rounded kits keep the literal corner radii the call sites used to hardcode`() {
        for (kit in listOf(PaletteKit.ORIGINAL, PaletteKit.MONOCHROME)) {
            val shapes = kit.style().shapes
            assertEquals(kit.name, TDCornerStyle.ROUNDED, shapes.cornerStyle)
            assertEquals(kit.name, RoundedCornerShape(4.dp), shapes.tiny)
            assertEquals(kit.name, RoundedCornerShape(8.dp), shapes.small)
            assertEquals(kit.name, RoundedCornerShape(12.dp), shapes.medium)
            assertEquals(kit.name, RoundedCornerShape(16.dp), shapes.large)
            assertEquals(kit.name, RoundedCornerShape(20.dp), shapes.xLarge)
            assertSame(kit.name, CircleShape, shapes.pill)
            assertSame(kit.name, CircleShape, shapes.circle)
        }
    }

    @Test
    fun `rounded kits keep Poppins and the untouched type ramp`() {
        for (kit in listOf(PaletteKit.ORIGINAL, PaletteKit.MONOCHROME)) {
            val style = kit.style()
            assertSame(kit.name, Poppins, style.fontFamily)
            assertSame(kit.name, Poppins, style.displayFontFamily)
            // null keeps M3's LocalTextStyle exactly as it is today.
            assertEquals(kit.name, null, style.fallbackFontFamily)
            // 0.sp is the identity for the small-end font-size floor.
            assertEquals(kit.name, 0.sp, style.minFontSize)
        }
    }

    @Test
    fun `rounded kits keep soft elevation, hairline borders and the 24dp grid`() {
        for (kit in listOf(PaletteKit.ORIGINAL, PaletteKit.MONOCHROME)) {
            val style = kit.style()
            assertEquals(kit.name, TDElevationStyle.SOFT, style.elevationStyle)
            assertEquals(kit.name, 0.dp, style.hardShadowOffset)
            assertEquals(kit.name, 1.dp, style.borderWidth)
            assertEquals(kit.name, GridStyle.Lines, style.gridStyle)
            assertEquals(kit.name, 24.dp, style.gridSpacing)
            assertEquals(kit.name, 1.dp, style.gridLineWidth)
        }
    }

    @Test
    fun `rounded kits keep gliding motion and the linear reveal wipe`() {
        for (kit in listOf(PaletteKit.ORIGINAL, PaletteKit.MONOCHROME)) {
            val motion = kit.style().motion
            assertEquals(kit.name, false, motion.stepped)
            // ThemeChangeReveal's curtain is a linear tween today and must stay one for these kits.
            assertSame(kit.name, LinearEasing, motion.revealEasing)
        }
    }

    @Test
    fun `pixel kit actually differs on every axis the rounded kits pin down`() {
        val style = PaletteKit.PIXEL.style()
        assertEquals(TDCornerStyle.PIXEL, style.shapes.cornerStyle)
        assertSame(PixelifySans, style.fontFamily)
        assertSame(PixelifySans, style.displayFontFamily)
        // Non-null so M3's LocalTextStyle picks up the pixel face for bare Text()/TextField sites.
        assertSame(PixelifySans, style.fallbackFontFamily)
        // Lifts 10sp `subheading2` — the app's most-used style — off the sub-pixel-stem range.
        assertEquals(12.sp, style.minFontSize)
        assertEquals(TDElevationStyle.HARD, style.elevationStyle)
        assertTrue("hard elevation needs a visible offset", style.hardShadowOffset > 0.dp)
        assertEquals(2.dp, style.borderWidth)
        assertEquals(true, style.motion.stepped)
        // The real invariant is "never Dots": that path is O((w/s)·(h/s)) and would be ~2550
        // drawCircle calls per invalidation at this kit's spacing. Dither is O(1) — one drawRect
        // with a repeating shader — so it is a safe texture at a 2dp cell.
        assertNotEquals(GridStyle.Dots, style.gridStyle)
        assertEquals(GridStyle.Dither, style.gridStyle)
    }

    @Test
    fun `pixel kit turns the app-wide grid on in both modes`() {
        for (dark in listOf(false, true)) {
            val (base, line) = PaletteKit.PIXEL.gridColors(dark)
            // gridBackground short-circuits to a flat fill at alpha 0 — that is how ORIGINAL opts out.
            assertTrue("dark=$dark grid must be visible", line.alpha > 0f)
            assertTrue("dark=$dark grid must contrast with the base", line != base)
        }
        assertTrue("ORIGINAL must keep opting out", PaletteKit.ORIGINAL.gridColors(false).second.alpha == 0f)
    }

    @Test
    fun `every kit exposes a non-empty settings swatch strip`() {
        for (kit in PaletteKit.entries) {
            for (dark in listOf(false, true)) {
                assertTrue("$kit dark=$dark", kit.stripColors(dark).isNotEmpty())
            }
        }
    }

    @Test
    fun `pixel corner keeps the requested stair when the box has room`() {
        // 2 steps x 4px = 8px per corner, well inside an 80px-tall box.
        assertEquals(4f, PixelCornerShape.blockPx(120f, 80f, requestedPx = 4f, steps = 2), 0f)
        assertEquals(2f, PixelCornerShape.blockPx(48f, 48f, requestedPx = 2f, steps = 1), 0f)
    }

    @Test
    fun `pixel corner clamps a cramped box so the edges never collapse into a diamond`() {
        for ((w, h, steps) in listOf(Triple(10f, 10f, 2), Triple(16f, 24f, 2), Triple(12f, 200f, 3))) {
            val block = PixelCornerShape.blockPx(w, h, requestedPx = 4f, steps = steps)
            val shorter = minOf(w, h)
            val consumed = 2f * steps * block
            assertTrue("w=$w h=$h steps=$steps produced a non-positive block", block > 0f)
            // Two staircases share each edge. If they consume the whole side the shape degenerates
            // into a diamond with zero-length edges; a third of every edge must stay straight.
            assertTrue(
                "w=$w h=$h steps=$steps: stairs ate $consumed of $shorter",
                consumed <= shorter * (2f / 3f) + 1e-4f,
            )
        }
    }

    @Test
    fun `pixel corner falls back to a rectangle when there is nothing to cut`() {
        // Pure branch — no android.graphics.Path involved, so it is meaningful under the unit-test
        // runtime (which has isReturnDefaultValues = true and would no-op every Path call).
        val degenerate = PixelCornerShape(unit = 4.dp, steps = 2)
            .createOutline(Size(0f, 0f), LayoutDirection.Ltr, Density(1f))
        assertTrue("zero-sized bounds must fall back to a rectangle", degenerate is Outline.Rectangle)
    }

    @Test
    fun `stepped easing quantises progress and still reaches both ends`() {
        val easing = SteppedEasing(steps = 4)
        assertEquals(0f, easing.transform(0f), 0f)
        assertEquals(1f, easing.transform(1f), 0f)
        // Anything inside a quarter holds at that quarter's floor — the "hold, then snap" feel.
        assertEquals(0.25f, easing.transform(0.3f), 0f)
        assertEquals(0.25f, easing.transform(0.49f), 0f)
        assertEquals(0.5f, easing.transform(0.5f), 0f)
    }
}
