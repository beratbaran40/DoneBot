package com.todoapp.mobile.ui.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat

/**
 * Whether a continuously-running animation should actually run.
 *
 * Four signals, all of which mean "don't": the app's own Reduce motion toggle
 * ([LocalReduceMotion]), the system-wide animation-scale setting, battery saver, and IDE
 * preview rendering. A screen-filling loop is exactly the thing each of them exists to stop, so
 * they are answered in one place rather than checked piecemeal at each call site.
 *
 * Battery saver is observed live — a user who flips it mid-session should see the animation
 * settle immediately, not on the next navigation.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    if (LocalInspectionMode.current) return false

    val reduceMotion = LocalReduceMotion.current
    val context = LocalContext.current

    // Settings → Developer options → Animator duration scale = off. Read once: changing it
    // restarts activities anyway.
    val systemAnimationsOff =
        remember(context) {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }

    val powerManager = remember(context) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    var powerSaveMode by remember(powerManager) { mutableStateOf(powerManager?.isPowerSaveMode == true) }

    DisposableEffect(context, powerManager) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    receiverContext: Context?,
                    intent: Intent?,
                ) {
                    powerSaveMode = powerManager?.isPowerSaveMode == true
                }
            }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    return !reduceMotion && !systemAnimationsOff && !powerSaveMode
}
