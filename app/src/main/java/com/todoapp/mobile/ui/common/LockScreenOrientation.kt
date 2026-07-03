package com.todoapp.mobile.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Forces the hosting Activity into [orientation] while this composable is in the composition and
 * restores the previously requested orientation on dispose.
 *
 * Use for screens whose layout is intentionally single-orientation — e.g. the skeuomorphic Polaroid
 * camera, whose vertical print-eject animation, 1:1 viewfinder and stacked camera body only make
 * sense in portrait (in landscape the ejected print blows up to 75% of the wide screen).
 *
 * [MainActivity] handles orientation config changes itself, so toggling `requestedOrientation` here
 * re-lays out the window without recreating the Activity.
 */
@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    DisposableEffect(orientation) {
        val activity = context.findActivity()
        val original = activity?.requestedOrientation
        activity?.requestedOrientation = orientation
        onDispose { original?.let { activity.requestedOrientation = it } }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
