package com.todoapp.mobile.domain.update

/**
 * What Play has to say about the installed build.
 *
 * The distinction that matters is [NotAvailable] versus [Unknown]. Collapsing both to "no" — which a
 * Boolean forces you to do — makes a retry policy impossible to write: either you never ask again and
 * a user who launched on a dead network is never told about an update, or you ask on every foreground
 * forever including the overwhelmingly common case where Play already gave a definitive answer.
 */
sealed interface AppUpdateStatus {
    /** Play has a higher versionCode for this user, on their track. */
    data object Available : AppUpdateStatus

    /** Play answered, and there is nothing newer. A definitive no — do not ask again this launch. */
    data object NotAvailable : AppUpdateStatus

    /** An update this app started is already downloading. Play requires the flow to be re-entered. */
    data object InProgress : AppUpdateStatus

    /**
     * The question could not be asked or answered: a build Play did not install (every debug build,
     * every sideload), a device without Play, no network, or a check that ran out of time. Worth
     * asking again the next time the app comes forward.
     */
    data object Unknown : AppUpdateStatus
}

/**
 * Whether a newer build of the app is available to install.
 *
 * Only the question lives behind this interface, not the answer's delivery — walking the user through
 * an update needs an `ActivityResultLauncher` and belongs to [AppUpdateFlowStarter].
 */
interface AppUpdateChecker {
    /** Never throws for an unavailable Play: every failure resolves to [AppUpdateStatus.Unknown]. */
    suspend fun check(): AppUpdateStatus
}
