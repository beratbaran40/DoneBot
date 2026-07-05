package com.todoapp.mobile.ui.common

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Adds [WindowManager.LayoutParams.FLAG_SECURE] to the host window while this composable is in the
 * composition and clears it on dispose. FLAG_SECURE blocks screenshots, screen recording and the
 * Recents (recent-apps) preview for the current window.
 *
 * Call this at the top of a sensitive screen (auth / password / journal / secret mode). The flag is
 * set per-screen and restored on exit because [com.todoapp.uikit.theme.TDTheme] manages the window
 * globally (edge-to-edge); leaving the flag set would leak it onto the next, non-sensitive screen.
 *
 * Single-Activity app (MainActivity is the only FragmentActivity host), so resolving the Activity
 * from the composition's view is reliable — mirrors the per-screen window handling in
 * `JournalEntryScreen` and `LockScreenOrientation`.
 */
@Composable
fun SecureScreenEffect() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}
