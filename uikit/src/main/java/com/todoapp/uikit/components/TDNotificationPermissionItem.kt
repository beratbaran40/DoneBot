package com.todoapp.uikit.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.uikit.R
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun TDNotificationPermissionItem(onDismiss: () -> Unit = {}) {
    val context = LocalContext.current
    val permission = Manifest.permission.POST_NOTIFICATIONS
    val shouldOpenSettingsOnNextGrantClick = remember { mutableStateOf(false) }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (!isGranted) {
                val activity = context as? Activity
                val shouldShowRationale =
                    activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
                    } ?: true
                if (!shouldShowRationale) {
                    shouldOpenSettingsOnNextGrantClick.value = true
                }
            } else {
                shouldOpenSettingsOnNextGrantClick.value = false
            }
        }

    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = TDTheme.colors.onBackground.copy(alpha = 0.2f),
                shape = TDTheme.shapes.medium,
            ),
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TDText(
                text = stringResource(R.string.notification_permission),
                style = TDTheme.typography.heading4,
                color = TDTheme.colors.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            TDText(
                text = stringResource(R.string.allows_the_app_to_display_notifications),
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            TDButton(
                isEnable = true,
                text = stringResource(R.string.grant_permission),
                onClick = {
                    if (shouldOpenSettingsOnNextGrantClick.value) {
                        openNotificationSettings(context)
                        return@TDButton
                    }
                    launcher.launch(permission)
                },
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Icon(
                painter = tdPainter(R.drawable.ic_close),
                contentDescription = "Close",
                tint = TDTheme.colors.onBackground,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun openNotificationSettings(context: Context) {
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
private fun TDNotificationPermissionItemPreview() {
    TDTheme {
        Box(
            modifier =
            Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
        ) {
            TDNotificationPermissionItem()
        }
    }
}
