package com.todoapp.mobile.ui.update

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.common.LocalReduceMotion
import com.todoapp.uikit.components.TDUpdateAvailableDialog
import timber.log.Timber

/**
 * Global host for the update prompt. Mounted once at the app's content root, past the splash gate and
 * inside `TDTheme`, so it draws over whatever screen the user happens to be on — a Compose `Dialog`
 * gets its own window, which puts it above the bottom bar and the navigation rail too.
 */
@Composable
fun AppUpdateDialogHost() {
    val viewModel: AppUpdateViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The result is genuinely uninteresting: an IMMEDIATE update that succeeds restarts the app, and
    // one the user backs out of leaves them where they were, which is the same place "Not now" does.
    val updateLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }

    // Play requires an app that started an immediate update to re-enter the flow when it comes back —
    // the update does not resume on its own, and until it does the user is stuck in a half-updated
    // install. This doubles as the retry for a check that could not reach Play at cold start.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(AppUpdateContract.UiAction.OnAppResumed)
    }

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                AppUpdateContract.UiEffect.LaunchUpdateFlow ->
                    if (!viewModel.startUpdateFlow(updateLauncher)) openPlayStoreListing(context)

                // No store-listing fallback here: if Play cannot resume the update it started itself,
                // a listing page accomplishes nothing.
                AppUpdateContract.UiEffect.ResumeUpdateFlow -> viewModel.resumeUpdateFlow(updateLauncher)
            }
        }
    }

    if (!state.isDialogVisible) return

    TDUpdateAvailableDialog(
        speechBubbleText = stringResource(R.string.update_available_bubble),
        detailText = stringResource(R.string.update_available_detail),
        updateButtonText = stringResource(R.string.update_available_action),
        laterButtonText = stringResource(R.string.update_available_later),
        avatarRes = R.drawable.img_splash,
        onUpdate = { viewModel.onAction(AppUpdateContract.UiAction.OnUpdateClick) },
        onDismiss = { viewModel.onAction(AppUpdateContract.UiAction.OnDismiss) },
        reduceMotion = LocalReduceMotion.current,
    )
}

/**
 * Debug builds carry an `applicationIdSuffix`, so this link 404s there — which is moot, since Play
 * reports no update for a build it did not install and the dialog never opens.
 */
private fun openPlayStoreListing(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, "$PLAY_LISTING_URL${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure { Timber.tag(TAG).w(it, "could not open the store listing") }
}

private const val TAG: String = "AppUpdate"
private const val PLAY_LISTING_URL: String = "https://play.google.com/store/apps/details?id="
