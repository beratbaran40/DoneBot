package com.todoapp.mobile.ui.settings

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.todoapp.mobile.ui.settings.SettingsContract.UiAction
import com.todoapp.mobile.ui.settings.SettingsContract.UiState

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun SettingsScreen(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
    onCheckPermissions: () -> Unit,
    onDismissPermission: (PermissionType) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            onCheckPermissions()
        }
    }

    SettingsContent(
        uiState = uiState,
        onAction = onAction,
        onCheckPermissions = onCheckPermissions,
        onDismissPermission = onDismissPermission,
    )
}
