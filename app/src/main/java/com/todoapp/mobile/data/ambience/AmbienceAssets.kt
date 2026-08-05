package com.todoapp.mobile.data.ambience

import androidx.annotation.RawRes
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.ambience.PomodoroAmbience

/**
 * Maps an ambience to its bundled loop. Lives in data rather than domain because `@RawRes` is an
 * Android type; the loops themselves come from `tools/prep_ambience.sh`, which seam-matches each
 * clip and levels all of them to -23 LUFS so switching soundscapes never means re-reaching for
 * the volume slider.
 */
object AmbienceAssets {
    @RawRes
    fun rawResFor(ambience: PomodoroAmbience): Int? = when (ambience) {
        PomodoroAmbience.None -> null
        PomodoroAmbience.Fireplace -> R.raw.ambience_fireplace
        PomodoroAmbience.Rain -> R.raw.ambience_rain
        PomodoroAmbience.Handpan -> R.raw.ambience_handpan
    }
}
