package com.todoapp.mobile.theme

import com.todoapp.mobile.R
import com.todoapp.mobile.ui.common.AppPixelIcons
import com.todoapp.uikit.image.UikitPixelIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the 8-Bit icon fallback contract.
 *
 * `tdIconRes` returns the id it was given whenever the map has no entry for it, so **absence from
 * the map is the opt-out mechanism**: a source icon whose 16x16 conversion was unusable keeps its
 * smooth vector in the 8-Bit kit without a single call site changing. That makes the map's contents
 * load-bearing in a way nothing else checks — re-running `tools/genpixelicons.py` after dropping an
 * entry from `REJECTED`, or after deleting a hand grid, would silently put a bad icon back.
 *
 * `tdIconRes` itself is `@Composable`, and this project has no Compose test harness, so these
 * assertions target the map that drives it.
 */
class PixelIconMapTest {
    @Test
    fun `both modules ship a non-empty pixel icon map`() {
        assertTrue("uikit map is empty", UikitPixelIcons.isNotEmpty())
        assertTrue("app map is empty", AppPixelIcons.isNotEmpty())
    }

    @Test
    fun `no entry is an identity mapping and no two sources share a variant`() {
        for ((label, map) in listOf("uikit" to UikitPixelIcons, "app" to AppPixelIcons)) {
            for ((source, pixel) in map) {
                assertTrue("$label: id 0 in the map", source != 0 && pixel != 0)
                assertFalse("$label: $source maps to itself", source == pixel)
            }
            // A duplicate value would mean two sources resolve to one drawable — the generator emits
            // one variant per source, so that can only happen if the map was hand-edited.
            assertEquals("$label: duplicate pixel variants", map.size, map.values.toSet().size)
        }
    }

    @Test
    fun `icons rejected on review keep their smooth vector`() {
        // Auto-conversions that survived the structural gate but were unusable on sight. If one of
        // these reappears, the 8-Bit kit is showing a blocky smear again.
        for (id in listOf(R.drawable.ic_settings_motion, R.drawable.ic_settings_fingerprint)) {
            assertFalse("rejected app icon is mapped again: $id", AppPixelIcons.containsKey(id))
        }
        for (id in listOf(com.example.uikit.R.drawable.ic_globe, com.example.uikit.R.drawable.ic_palette)) {
            assertFalse("rejected uikit icon is mapped again: $id", UikitPixelIcons.containsKey(id))
        }
    }

    @Test
    fun `brand marks are never pixelated`() {
        // Flags and the Google mark carry identity in their exact colours; a blocky approximation
        // reads as a broken logo, so they are permanently in the generator's SKIP list.
        for (id in listOf(R.drawable.ic_turkish_flag, R.drawable.ic_american_flag, R.drawable.ic_google_logo)) {
            assertFalse("brand mark is mapped: $id", AppPixelIcons.containsKey(id))
        }
    }

    @Test
    fun `every bottom-bar tab has a hand-drawn pixel variant`() {
        // The tabs are the most visible pixel art in the app and are hand-drawn in tools/pixelart/.
        // A generator run that lost them is exactly the regression this whole change fixed.
        val tabs = listOf(
            R.drawable.ic_home, R.drawable.ic_selected_home,
            R.drawable.ic_groups, R.drawable.ic_selected_groups,
            R.drawable.ic_chat, R.drawable.ic_selected_chat,
            R.drawable.ic_calendar, R.drawable.ic_selected_calendar,
            R.drawable.ic_statistic, R.drawable.ic_selected_statistic,
        )
        for (id in tabs) {
            assertTrue("tab icon lost its pixel variant: $id", AppPixelIcons.containsKey(id))
        }
    }
}
