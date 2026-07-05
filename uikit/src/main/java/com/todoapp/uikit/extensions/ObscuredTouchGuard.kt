package com.todoapp.uikit.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Rejects touch events that arrive while the host window is (partially) obscured by another app's
 * overlay — the core defence against tap-jacking. Call this inside a destructive confirmation
 * dialog (or at the top of a destructive full-screen) so a malicious overlay cannot trick the user
 * into confirming an irreversible action (delete account, delete/transfer group, remove member).
 *
 * Sets [android.view.View.setFilterTouchesWhenObscured] on the composition's host view for the
 * lifetime of the composition and restores the previous value on dispose. Inside a Compose
 * `Dialog` / `AlertDialog` the host view is the dialog's own window, so the guard scopes to that
 * dialog; at the root of a full screen it scopes to that screen while it is visible.
 *
 * Trade-off: while a screen-dimmer / blue-light overlay app is active (it flags every touch as
 * obscured), the guarded surface also rejects taps — the same accepted behaviour as banking apps.
 * Only the destructive surface is affected, not the whole app, and never when no overlay is present.
 */
@Composable
fun ObscuredTouchGuard() {
    val view = LocalView.current
    DisposableEffect(view) {
        val previous = view.filterTouchesWhenObscured
        view.filterTouchesWhenObscured = true
        onDispose { view.filterTouchesWhenObscured = previous }
    }
}
