package com.todoapp.mobile.domain.model

/**
 * The shape a task was created as — the card the user tapped in the Creation Hub.
 *
 * This is a *declaration*, not a summary: it says what the user set out to make, and it does not
 * move when the row's fields later change. Deriving it instead (see `derivedTaskType`) is what made
 * "Custom, running between two dates" report itself as a Routine — the two are indistinguishable in
 * the data, because a scheduled end sits beside FREQ in the same rule rather than being a different
 * shape.
 *
 * A null declaration is normal and permanent for rows that predate the column and for rows that
 * arrive from the server, so every reader falls back to the derivation.
 */
enum class TaskType {
    ONE_TIME,
    ROUTINE,
    STAGED,
    CUSTOM,
    ;

    companion object {
        /**
         * Null — not a default value — for an absent or unrecognised name.
         *
         * Deliberately unlike [TaskCategory.fromStorage] and [Recurrence.fromStorage], which fall back
         * to a real value. Here "no declaration" is a meaningful state that the caller resolves by
         * deriving, and picking any of the four instead would relabel every pre-column task on its
         * first read.
         */
        fun fromStorage(value: String?): TaskType? = value?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}
