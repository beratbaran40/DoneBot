package com.todoapp.mobile.domain.update

/**
 * Whether a newer build of the app is available to install.
 *
 * Only the question lives behind this interface, not the answer's delivery: actually walking the user
 * through an update is an Activity-scoped affair that belongs to the UI layer. Keeping the check
 * separate is what makes "should the update dialog appear?" a thing that can be tested.
 */
interface AppUpdateChecker {
    /**
     * False — never an error — when there is nothing to install, when the check cannot run at all
     * (sideloaded builds, no Play services, no network), or when it is simply taking too long.
     * A version nudge is not worth a failure state on screen.
     */
    suspend fun isUpdateAvailable(): Boolean
}
