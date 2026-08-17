package com.todoapp.mobile

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.todoapp.mobile.MainContract.UiAction.OnDialogOkTap
import com.todoapp.mobile.navigation.NavigationEffectController
import com.todoapp.mobile.navigation.RouteArgs
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.navigation.ThemedApp
import com.todoapp.mobile.ui.common.AppPixelIcons
import com.todoapp.mobile.ui.common.LocalReduceMotion
import com.todoapp.uikit.components.LocalTDTextOverflowReporter
import com.todoapp.uikit.components.TDTextOverflowReporter
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.image.LocalPixelIconMap
import com.todoapp.uikit.image.UikitPixelIcons
import timber.log.Timber

@Composable
fun MainContent() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    val reduceMotion by mainViewModel.reduceMotion.collectAsStateWithLifecycle(initialValue = false)
    // Merged icon map: `:uikit` and `:app` own separate R classes, so uikit's default local only
    // resolves uikit drawables. The overlay root keeps that default on purpose — it renders uikit
    // cards only.
    val pixelIcons = remember { UikitPixelIcons + AppPixelIcons }
    CompositionLocalProvider(
        LocalNavController provides navController,
        LocalReduceMotion provides reduceMotion,
        LocalPixelIconMap provides pixelIcons,
        LocalTDTextOverflowReporter provides rememberTextFitReporter(),
    ) {
        var dialogMessage by rememberSaveable { mutableStateOf<String?>(null) }

        mainViewModel.uiEffect.collectWithLifecycle { effect ->
            when (effect) {
                is MainContract.UiEffect.ShowDialog -> {
                    dialogMessage = effect.message
                }
            }
        }

        NavigationEffectController(mainViewModel.navEffect)

        val backStackEntry by navController.currentBackStackEntryAsState()
        val pendingDeepLink by mainViewModel.pendingDeepLink.collectAsStateWithLifecycle()
        val isLoggedIn = mainViewModel.isLoggedIn
        LaunchedEffect(pendingDeepLink, isLoggedIn, backStackEntry) {
            val link = pendingDeepLink ?: return@LaunchedEffect
            // The NavHost graph isn't set until the splash ends (DoneBotApp shows a splash while
            // isLoggedIn == null || !splashDone). On a cold reminder tap isLoggedIn can flip to true
            // before that; navigating now would fail and consumePendingDeepLink() would drop the
            // link. Wait until the start destination is on the back stack.
            if (backStackEntry == null) return@LaunchedEffect
            if (link is MainViewModel.DeepLink.ResetPassword) {
                navController.navigate(Screen.ResetPassword(token = link.token))
                mainViewModel.consumePendingDeepLink()
                return@LaunchedEffect
            }
            if (isLoggedIn != true) return@LaunchedEffect
            val target =
                when (link) {
                    is MainViewModel.DeepLink.Group -> Screen.GroupDetail(groupId = link.groupId, groupName = "")
                    is MainViewModel.DeepLink.GroupTask -> Screen.GroupTaskDetail(groupId = link.groupId, taskId = link.taskId)
                    is MainViewModel.DeepLink.Task -> Screen.Task(taskId = link.taskId)
                    is MainViewModel.DeepLink.Invitations -> Screen.Invitations
                    is MainViewModel.DeepLink.NotificationsInbox -> Screen.Notifications
                    is MainViewModel.DeepLink.ResetPassword -> return@LaunchedEffect
                }
            navController.navigate(target)
            mainViewModel.consumePendingDeepLink()
        }

        LaunchedEffect(backStackEntry) {
            val entry = backStackEntry ?: run {
                mainViewModel.updateCurrentRoute(route = null, args = null)
                return@LaunchedEffect
            }
            val rawRoute = entry.destination.route
            val route = rawRoute?.substringBefore("/")?.substringBefore("?")
            val args = when (route) {
                Screen.GroupTaskDetail::class.qualifiedName ->
                    runCatching { entry.toRoute<Screen.GroupTaskDetail>() }
                        .getOrNull()
                        ?.let { RouteArgs.GroupTaskDetail(it.groupId, it.taskId) }
                Screen.GroupDetail::class.qualifiedName ->
                    runCatching { entry.toRoute<Screen.GroupDetail>() }
                        .getOrNull()
                        ?.let { RouteArgs.GroupDetail(it.groupId) }
                else -> null
            }
            mainViewModel.updateCurrentRoute(route = route, args = args)
        }

        MainDialog(
            message = dialogMessage,
            onOk = {
                dialogMessage = null
                mainViewModel.onAction(OnDialogOkTap)
            },
        )
        ThemedApp()
    }
}

/**
 * The debug text-fit probe. Returns `null` in release, which is what keeps `TDText` on Compose's
 * String fast path — see `LocalTDTextOverflowReporter`. `BuildConfig.DEBUG` is a compile-time
 * constant, so R8 removes this whole branch from the shipping build.
 *
 * Reports are de-duplicated for the life of the process: an overflowing label re-lays-out on every
 * scroll frame, and without this a single walkthrough buries the findings in thousands of repeats.
 * Read it back with `adb logcat -d -s TDTextFit:W`.
 */
@Composable
private fun rememberTextFitReporter(): TDTextOverflowReporter? = remember {
    if (!BuildConfig.DEBUG) {
        null
    } else {
        val seen = mutableSetOf<String>()
        TDTextOverflowReporter { report ->
            val key = "${report.slot}|${report.text}|${report.widthPx}"
            if (seen.add(key)) {
                val kind = when {
                    report.midWordBreak -> "MIDWORD"
                    report.clipped -> "CLIPPED"
                    else -> "CRAMPED"
                }
                Timber.tag("TDTextFit").w(
                    "%s | %s | lines=%d maxLines=%s | %.0fsp in %dpx | \"%s\"",
                    kind,
                    report.slot ?: "?",
                    report.lineCount,
                    if (report.maxLines == Int.MAX_VALUE) "∞" else report.maxLines.toString(),
                    report.fontSizeSp,
                    report.widthPx,
                    report.text,
                )
            }
        }
    }
}

@Composable
private fun MainDialog(
    message: String?,
    onOk: () -> Unit,
) {
    if (message == null) return

    AlertDialog(
        onDismissRequest = onOk,
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onOk) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}
