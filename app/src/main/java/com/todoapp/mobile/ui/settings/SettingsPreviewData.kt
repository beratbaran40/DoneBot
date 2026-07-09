package com.todoapp.mobile.ui.settings

import com.todoapp.mobile.ui.settings.SettingsContract.UiState

internal object SettingsPreviewData {
    fun authenticatedState(
        displayName: String = "Ada Lovelace",
        email: String = "ada@example.com",
        showLogoutDialog: Boolean = false,
        showDeleteAccountDialog: Boolean = false,
        isDeletingAccount: Boolean = false,
    ): UiState = UiState(
        isUserAuthenticated = true,
        displayName = displayName,
        email = email,
        pushNotificationsEnabled = true,
        journalBiometricProtected = true,
        cameraGranted = true,
        showLogoutDialog = showLogoutDialog,
        showDeleteAccountDialog = showDeleteAccountDialog,
        isDeletingAccount = isDeletingAccount,
    )

    fun guestState(): UiState = UiState(isUserAuthenticated = false)

    fun permissionsState(): UiState = authenticatedState().copy(
        visiblePermissions = listOf(PermissionType.OVERLAY, PermissionType.NOTIFICATION),
    )
}
