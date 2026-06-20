package com.todoapp.mobile.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.todoapp.mobile.R
import com.todoapp.uikit.components.TDPermissionPromptCard
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
internal fun NotificationPermissionPrompt(
    onGranted: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val permission = Manifest.permission.POST_NOTIFICATIONS
    val shouldOpenSettings = remember { mutableStateOf(false) }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                shouldOpenSettings.value = false
                onGranted()
            } else {
                val activity = context as? Activity
                val shouldShowRationale =
                    activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
                    } ?: true
                if (!shouldShowRationale) {
                    shouldOpenSettings.value = true
                }
            }
        }

    TDPermissionPromptCard(
        iconRes = com.example.uikit.R.drawable.ic_notification,
        title = stringResource(R.string.home_permission_notification_title),
        description = stringResource(R.string.home_permission_notification_body),
        ctaText = stringResource(com.example.uikit.R.string.grant_permission),
        onCtaClick = {
            if (shouldOpenSettings.value) {
                openAppNotificationSettings(context)
            } else {
                launcher.launch(permission)
            }
        },
        onDismiss = onDismiss,
    )
}

@Composable
internal fun OverlayPermissionPrompt(
    onGranted: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) {
            if (Settings.canDrawOverlays(context)) onGranted()
        }

    TDPermissionPromptCard(
        iconRes = com.example.uikit.R.drawable.ic_fullscreen,
        title = stringResource(R.string.home_permission_overlay_title),
        description = stringResource(R.string.home_permission_overlay_body),
        ctaText = stringResource(com.example.uikit.R.string.grant_permission),
        onCtaClick = {
            val intent =
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri(),
                )
            launcher.launch(intent)
        },
        onDismiss = onDismiss,
    )
}

@RequiresApi(Build.VERSION_CODES.O)
private fun openAppNotificationSettings(context: Context) {
    val intent =
        Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    context.startActivity(intent)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@TDPreview
@Composable
private fun NotificationPermissionPromptPreview() {
    TDTheme {
        NotificationPermissionPrompt(onGranted = {}, onDismiss = {})
    }
}

@TDPreview
@Composable
private fun OverlayPermissionPromptPreview() {
    TDTheme {
        OverlayPermissionPrompt(onGranted = {}, onDismiss = {})
    }
}
