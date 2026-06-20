package com.todoapp.mobile.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import com.todoapp.mobile.R.string
import com.todoapp.mobile.common.needsCameraPermission
import com.todoapp.mobile.common.openAppDetailsSettings
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreviewDialog
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR

/**
 * Encapsulates the full CAMERA permission state machine and returns a `request()` callback that
 * call sites invoke from a click/toggle handler. Renders its own rationale / settings dialogs.
 *
 * Handles the three OS states a camera request can be in:
 * - **granted** → [onGranted];
 * - **can ask with rationale** (`shouldShowRequestPermissionRationale == true`) → show a rationale
 *   dialog, then launch the system dialog;
 * - **blocked** → launch the system dialog, and if the OS refuses to show it (the `USER_FIXED` /
 *   permanently-denied state — which returns denied with no dialog and can survive an uninstall on
 *   some ROMs) route the user to the app's Settings page instead of dead-ending on a toast.
 *
 * @param onGranted invoked when the permission is already granted or becomes granted.
 */
@Composable
fun rememberCameraPermissionRequest(onGranted: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val activity = context as? Activity
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onGranted()
        } else {
            onCameraPermissionDenied(context, activity) { showSettingsDialog = true }
        }
    }

    if (showRationaleDialog) {
        CameraPermissionDialog(
            body = stringResource(string.camera_permission_rationale_body),
            confirmText = stringResource(UiKitR.string.grant_permission),
            onConfirm = {
                showRationaleDialog = false
                launcher.launch(Manifest.permission.CAMERA)
            },
            onDismiss = { showRationaleDialog = false },
        )
    }
    if (showSettingsDialog) {
        CameraPermissionDialog(
            body = stringResource(string.camera_permission_settings_body),
            confirmText = stringResource(string.open_settings),
            onConfirm = {
                showSettingsDialog = false
                context.openAppDetailsSettings()
            },
            onDismiss = { showSettingsDialog = false },
        )
    }

    return {
        when {
            !context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ->
                Toast.makeText(
                    context,
                    context.getString(string.polaroid_camera_unavailable),
                    Toast.LENGTH_SHORT,
                ).show()

            !context.needsCameraPermission() -> onGranted()

            activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) ->
                showRationaleDialog = true

            else -> launcher.launch(Manifest.permission.CAMERA)
        }
    }
}

/**
 * Handles a denied camera request. If the system would still show its dialog the user just gets a
 * toast; if it refuses to prompt (permanently denied / a flag the uninstall never cleared) we route
 * them to Settings via [onOpenSettings] instead of dead-ending on a toast.
 */
private fun onCameraPermissionDenied(
    context: Context,
    activity: Activity?,
    onOpenSettings: () -> Unit,
) {
    val canAskAgain = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
    } ?: false
    if (canAskAgain) {
        Toast.makeText(
            context,
            context.getString(string.polaroid_permission_denied),
            Toast.LENGTH_SHORT,
        ).show()
    } else {
        onOpenSettings()
    }
}

@Composable
private fun CameraPermissionDialog(
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TDTheme.colors.surface,
        title = {
            TDText(
                text = stringResource(string.camera_permission_required_title),
                style = TDTheme.typography.heading3,
                color = TDTheme.colors.onBackground,
            )
        },
        text = {
            TDText(
                text = body,
                style = TDTheme.typography.regularTextStyle,
                color = TDTheme.colors.onBackground,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                TDText(
                    text = confirmText,
                    style = TDTheme.typography.subheading2,
                    color = TDTheme.colors.purple,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                TDText(
                    text = stringResource(string.cancel),
                    style = TDTheme.typography.subheading2,
                    color = TDTheme.colors.gray,
                )
            }
        },
    )
}

@TDPreviewDialog
@Composable
private fun CameraPermissionDialogPreview() {
    TDTheme {
        CameraPermissionDialog(
            body = stringResource(string.camera_permission_rationale_body),
            confirmText = stringResource(UiKitR.string.grant_permission),
            onConfirm = {},
            onDismiss = {},
        )
    }
}
