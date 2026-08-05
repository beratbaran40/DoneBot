package com.todoapp.mobile.domain.ambience

/**
 * A background soundscape the user can run under a Pomodoro session, together with the animated
 * scene that accompanies it — the two are one choice, not two.
 *
 * Adding one is four edits: an entry here, an `.ogg` loop prepared by `tools/prep_ambience.sh`,
 * a `when` branch in `AmbienceAssets`/`PomodoroAmbienceScene`, and an EN+TR label. Every `when`
 * over this enum is deliberately exhaustive so a missing branch is a compile error.
 */
enum class PomodoroAmbience(
    /** Persisted in DataStore. Renaming one silently resets that user's choice — don't. */
    val id: String,
) {
    None("none"),
    Fireplace("fireplace"),
    Rain("rain"),
    Handpan("handpan"),
    ;

    companion object {
        fun fromId(id: String?): PomodoroAmbience = entries.firstOrNull { it.id == id } ?: None

        /** Everything the picker offers, [None] first so "off" is the easiest thing to reach. */
        val selectable: List<PomodoroAmbience> = entries.toList()
    }
}
