package com.todoapp.mobile.domain.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

/**
 * Hands the user over to Play's update flow.
 *
 * The launcher is a parameter rather than a constructor dependency because it is Activity-scoped and
 * must not outlive the composition that created it — the same shape as
 * [com.todoapp.mobile.domain.security.Authenticator], which takes its `FragmentActivity` per call for
 * exactly this reason. Nothing here stores it.
 */
interface AppUpdateFlowStarter {
    /**
     * @return false when Play declines to run the flow, which is the caller's cue to fall back to the
     *   store listing rather than leave the button doing nothing.
     */
    suspend fun startImmediateUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean

    /**
     * Re-enters an immediate update Play already began. Required by Play: an immediate update whose
     * app was killed mid-download resumes only if the app asks it to, and until then the user is
     * stranded in a half-updated install they cannot resolve from inside the app.
     *
     * @return false when there was nothing in progress.
     */
    suspend fun resumeInProgressUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean
}
