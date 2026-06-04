package com.todoapp.uikit.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Color tokens for the skeuomorphic Polaroid OneStep camera rendering used by the Journal
 * capture screen.
 *
 * These describe a **physical object** (a vintage Polaroid camera), so — unlike the rest of
 * [TDColor] — they are intentionally **identical in light and dark mode**. A single
 * [polaroidColors] factory feeds both schemes.
 *
 * The source artwork used ~40 distinct inline `Color(0xFF…)` literals; near-identical
 * near-black gradient stops (Δ ≲ 0x09, imperceptible) have been consolidated onto the nearest
 * token here while every gradient keeps its light→dark contrast. Pure [Color.Black] /
 * [Color.White] / [Color.Transparent] and their alpha variants stay inline in the drawing code
 * (true light/shadow, not palette colors).
 */
@Immutable
data class PolaroidColors(
    // Surface behind the camera body
    val appBackground: Color,
    // Cream plastic body
    val bodyCream: Color,
    val bodyCreamShadow: Color,
    val bodyCreamLip: Color,
    val bodyCreamEdge: Color,
    // Black chassis / tray
    val chassisLight: Color,
    val chassisDark: Color,
    val nearBlack: Color,
    val panelRecess: Color,
    val panelSeam: Color,
    val trayDeep: Color,
    val slotLip: Color,
    // Lens assembly
    val lensRimInner: Color,
    val glassLight: Color,
    // Flash unit
    val housingLight: Color,
    val housingDark: Color,
    val housingRecess: Color,
    val flashBulb: Color,
    // Red shutter button + cream collar
    val shutterRed: Color,
    val shutterRedHighlight: Color,
    val shutterCollarLight: Color,
    val shutterCollarDark: Color,
    // Branding sticker / chassis text
    val stickerBeige: Color,
    val stickerRule: Color,
    val brandingRed: Color,
    val textColor: Color,
    // Ejected print + developing chemistry
    val printPaper: Color,
    val developGrey: Color,
    val developTeal: Color,
    // Live-preview placeholder backdrop (IDE preview only)
    val previewBackdrop: Color,
    // Iconic Polaroid rainbow stripe (top → bottom)
    val rainbow: List<Color>,
)

internal fun polaroidColors(): PolaroidColors = PolaroidColors(
    appBackground = Color(0x006B705C),
    bodyCream = Color(0xFFEBE9DE),
    bodyCreamShadow = Color(0xFFBCBAAB),
    bodyCreamLip = Color(0xFFE5E3D5),
    bodyCreamEdge = Color(0xFFE0DDD0),
    chassisLight = Color(0xFF2E2E2E),
    chassisDark = Color(0xFF141414),
    nearBlack = Color(0xFF0A0A0A),
    panelRecess = Color(0xFF1F1F1F),
    panelSeam = Color(0xFF111111),
    trayDeep = Color(0xFF050505),
    slotLip = Color(0xFF6A6A6A),
    lensRimInner = Color(0xFFC7C5B5),
    glassLight = Color(0xFF13181A),
    housingLight = Color(0xFF383838),
    housingDark = Color(0xFF121212),
    housingRecess = Color(0xFF222222),
    flashBulb = Color(0xFFE5E5E5),
    shutterRed = Color(0xFFDF1A00),
    shutterRedHighlight = Color(0xFFFF4D33),
    shutterCollarLight = Color(0xFFE2DFCD),
    shutterCollarDark = Color(0xFFA5A394),
    stickerBeige = Color(0xFFEBE6C8),
    stickerRule = Color(0xFFD4D0B3),
    brandingRed = Color(0xFFD11A00),
    textColor = Color(0xFFAFAFAF),
    printPaper = Color(0xFFF4F4F0),
    developGrey = Color(0xFF536C7A),
    developTeal = Color(0xFF1A2B2D),
    previewBackdrop = Color(0xFF222222),
    rainbow = listOf(
        Color(0xFFB30033),
        Color(0xFFE65C00),
        Color(0xFFFFC000),
        Color(0xFF00A633),
        Color(0xFF0073B3),
    ),
)
