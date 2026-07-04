package com.todoapp.mobile

import android.os.StrictMode

/**
 * Debug-only StrictMode setup (§3.5). Surfaces accidental main-thread disk/network and common VM
 * leaks (unclosed Closeables/cursors, leaked Activities/registrations) to logcat during development.
 * Always [StrictMode.ThreadPolicy.Builder.penaltyLog] — never penaltyDeath — so violations are
 * visible without crashing the app; the caller gates this on [BuildConfig.DEBUG] so release pays
 * nothing.
 */
fun configureStrictMode() {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build(),
    )
    StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
            .detectLeakedClosableObjects()
            .detectLeakedSqlLiteObjects()
            .detectActivityLeaks()
            .detectLeakedRegistrationObjects()
            .penaltyLog()
            .build(),
    )
}
