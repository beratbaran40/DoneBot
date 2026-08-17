package com.todoapp.uikit.components

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How much room a piece of text is entitled to.
 *
 * The app's policy is "grow, don't cut": a label that needs two lines gets two lines, and the
 * container grows. That makes a second line NORMAL for most text, which is exactly why a raw
 * `lineCount > 1` signal is useless here — 429 of the ~480 [TDText] call sites are free-flowing copy
 * where wrapping is the correct outcome.
 *
 * So fitting is declared, not inferred. Only the slots that genuinely cannot afford a second line
 * opt into [BOUNDED], and only those are watched.
 */
enum class TDFitPolicy {
    /** Free-flowing copy. Wrapping is correct; only a hard clip is ever a defect. */
    FREE,

    /**
     * A slot with a fixed height or a fixed number of siblings — nav labels, app-bar titles, the
     * text column of an N-up card. Here a second line means the layout ran out of horizontal room,
     * which is a defect even though nothing was truncated.
     */
    BOUNDED,
}

/**
 * One text that did not fit. [slot] names the component so the log is greppable; when it is null,
 * [text] is the locator — search it in `strings.xml`, then search the key in Kotlin.
 */
data class TDTextOverflowReport(
    val text: String,
    val slot: String?,
    val widthPx: Int,
    val fontSizeSp: Float,
    val lineCount: Int,
    val maxLines: Int,
    /** True when glyphs were actually cut off — a `maxLines` cap, or a fixed-height container. */
    val clipped: Boolean,
    /**
     * True when a line ended mid-word, i.e. the slot is narrower than a single word. This is the
     * narrow-column failure, and wrapping cannot fix it — the slot has to get wider or the word
     * shorter.
     */
    val midWordBreak: Boolean,
)

/** Receives a [TDTextOverflowReport] for every [TDText] that overruns its slot. */
fun interface TDTextOverflowReporter {
    fun onOverflow(report: TDTextOverflowReport)
}

/**
 * Debug-only text-fit probe. `null` — the default, and what release builds keep — is load-bearing:
 * [TDText] passes `onTextLayout = null` when there is no reporter, which keeps it on Compose's
 * String fast path. A non-null callback moves all ~480 call sites onto the AnnotatedString path,
 * so this must never be provided in a release build.
 *
 * `staticCompositionLocalOf` because reading it must not open a recomposition scope: the cost when
 * the probe is off is one lookup, not a subscription. Provided from `:app`'s `MainContent`, which is
 * where `BuildConfig.DEBUG` and Timber live — `:uikit` has neither.
 */
val LocalTDTextOverflowReporter = staticCompositionLocalOf<TDTextOverflowReporter?> { null }
