package com.todoapp.mobile.ui.journal.camera

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Resolved pixel geometry of the hand-drawn Polaroid body for a box of [boxSize].
 *
 * Three call sites need the exact same numbers: the canvas that draws the body, the pointer input
 * that hit-tests the red shutter, and the screen layout that anchors the ejecting print to the film
 * slot. Deriving them once here keeps them from drifting apart.
 *
 * Two coordinate spaces are exposed. Everything is computed in **body space** (origin at the
 * top-left of the drawn silhouette, which is what the draw phase sees after it translates by the
 * canvas padding); properties suffixed `InBox` are the same values in **box space** (origin at the
 * top-left of the composable), which is what hit-testing and layout see.
 */
internal class PolaroidBodyMetrics(boxSize: Size, density: Density) {

    /** Canvas padding — the silhouette is inset so its drop shadow has room to fall outside it. */
    val originX: Float = with(density) { PADDING_X.toPx() }
    val originY: Float = with(density) { PADDING_TOP.toPx() }

    /** Drawable extent of the body inside the padding. */
    val w: Float = boxSize.width - originX * 2f
    val h: Float = boxSize.height - (originY + with(density) { PADDING_BOTTOM.toPx() })

    // ── Vertical bands, top to bottom: cream top body, ledge, lip, black chassis tray ──
    val trayH: Float = h * TRAY_FRACTION
    private val lipH: Float = h * LIP_FRACTION
    private val ledgeH: Float = h * LEDGE_FRACTION
    val topBodyH: Float = h - trayH - ledgeH - lipH

    val ledgeTopY: Float = topBodyH
    val lipTopY: Float = ledgeTopY + ledgeH
    val trayTopY: Float = lipTopY + lipH
    val bottomY: Float = h

    // ── Horizontal extents: the body tapers towards the top ──
    val topBodyBottomW: Float = w * 0.94f
    val topBodyTopW: Float = w * 0.88f

    /** Dark hood above the top edge — the housing the film slot is cut into. */
    val hoodW: Float = topBodyTopW * HOOD_WIDTH
    val hoodH: Float = with(density) { HOOD_HEIGHT.toPx() }
    val hoodLeft: Float = (w - hoodW) / 2f

    /** Band across the top of the cream body reserved for the film ejection slot. */
    val slotBandH: Float = topBodyH * SLOT_BAND_FRACTION

    /**
     * Content area below the slot band. Component fractions are applied to this rather than to the
     * full [topBodyH], so making room for the slot only compresses the original composition instead
     * of forcing every component to be re-tuned by hand.
     */
    private val contentTop: Float = slotBandH
    private val contentH: Float = topBodyH - slotBandH

    /** The film slot opening, in body space. The print ejects upwards out of this rect. */
    val slotMouth: Rect = run {
        val top = slotBandH * SLOT_MOUTH_TOP
        val height = slotBandH * SLOT_MOUTH_HEIGHT
        // The body tapers, so take its width at the slot's own height rather than at the very top.
        val taper = top / topBodyH
        val bodyWidthHere = topBodyTopW + (topBodyBottomW - topBodyTopW) * taper
        val width = bodyWidthHere * SLOT_MOUTH_WIDTH
        Rect(Offset((w - width) / 2f, top), Size(width, height))
    }

    // ── Components, positioned inside the content area ──
    val lensCenter: Offset = Offset(w / 2f, contentTop + contentH * LENS_CY)
    val lensSize: Float = topBodyBottomW * 0.35f
    val stripeStartY: Float = lensCenter.y + lensSize * 0.35f

    val shutterCenter: Offset = Offset(w * SHUTTER_CX, contentTop + contentH * SHUTTER_CY)
    val shutterRadius: Float = w * SHUTTER_R

    val flashCenter: Offset = Offset(w * FLASH_CX, contentTop + contentH * FLASH_CY)
    val flashSize: Float = w * 0.14f

    val dialCenter: Offset = Offset(w * DIAL_CX, contentTop + contentH * DIAL_CY)
    val dialRadius: Float = w * 0.05f

    val brandingRect: Rect = Rect(
        Offset(w * 0.11f, contentTop + contentH * BRANDING_CY),
        Size(w * 0.16f, w * 0.16f),
    )

    /** The slot opening in box space — what the layout needs to anchor the print. */
    val slotMouthInBox: Rect get() = slotMouth.translate(originX, originY)

    /** Centre of the red shutter in box space, with the generous radius used for hit-testing. */
    val shutterTouchCenterInBox: Offset get() = shutterCenter + Offset(originX, originY)
    val shutterTouchRadius: Float get() = shutterRadius * SHUTTER_TOUCH_SCALE

    internal companion object {
        /** Width / height of the box the body is drawn into. */
        const val BODY_ASPECT = 1.15f

        val PADDING_X = 16.dp
        val PADDING_TOP = 16.dp
        val PADDING_BOTTOM = 32.dp

        private const val TRAY_FRACTION = 0.28f
        private const val LIP_FRACTION = 0.048f
        private const val LEDGE_FRACTION = 0.065f

        private const val SLOT_BAND_FRACTION = 0.12f
        private const val SLOT_MOUTH_TOP = 0.34f
        private const val SLOT_MOUTH_HEIGHT = 0.44f
        private const val SLOT_MOUTH_WIDTH = 0.84f

        private const val HOOD_WIDTH = 0.90f

        /** Must stay under [PADDING_TOP] — the hood sits above the body's top edge. */
        private val HOOD_HEIGHT = 14.dp

        private const val LENS_CY = 0.48f
        private const val SHUTTER_CX = 0.20f
        private const val SHUTTER_CY = 0.68f
        private const val SHUTTER_R = 0.065f
        private const val SHUTTER_TOUCH_SCALE = 1.35f
        private const val FLASH_CX = 0.79f
        private const val FLASH_CY = 0.22f
        private const val DIAL_CX = 0.77f
        private const val DIAL_CY = 0.65f
        private const val BRANDING_CY = 0.10f
    }
}
