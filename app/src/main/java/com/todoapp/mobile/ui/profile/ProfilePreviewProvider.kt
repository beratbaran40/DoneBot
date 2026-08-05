package com.todoapp.mobile.ui.profile

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

class ProfilePreviewProvider : PreviewParameterProvider<ProfileContract.UiState> {
    override val values: Sequence<ProfileContract.UiState>
        get() =
            sequenceOf(
                ProfileContract.UiState(isLoading = true),
                ProfileContract.UiState(
                    isLoading = false,
                    userId = 1L,
                    email = "berat.baran@example.com",
                    displayName = "Berat Baran",
                    editedDisplayName = "Berat Baran",
                ),
                ProfileContract.UiState(
                    isLoading = false,
                    userId = 1L,
                    email = "berat.baran@example.com",
                    displayName = "Berat Baran",
                    editedDisplayName = "Berat B.",
                ),
                ProfileContract.UiState(
                    isLoading = false,
                    userId = 1L,
                    email = "berat.baran@example.com",
                    displayName = "Berat Baran",
                    editedDisplayName = "Berat B.",
                    isSaving = true,
                ),
                ProfileContract.UiState(
                    isLoading = false,
                    userId = 1L,
                    email = "berat.baran@example.com",
                    displayName = "Berat Baran",
                    editedDisplayName = "Berat Baran",
                    isUploading = true,
                ),
                ProfileContract.UiState(
                    isLoading = false,
                    userId = 1L,
                    email = "berat.baran@example.com",
                    displayName = "Berat Baran",
                    editedDisplayName = "",
                    errorMessage = "Network error",
                ),
                // Partial health — an odd half-heart count, so the badge renders the "½".
                ProfileContract.UiState(
                    isLoading = false,
                    userId = 1L,
                    email = "berat.baran@example.com",
                    displayName = "Berat Baran",
                    editedDisplayName = "Berat Baran",
                    healthHalfHearts = 13,
                ),
                // Depleted health — the badge must still read cleanly at "0".
                ProfileContract.UiState(
                    isLoading = false,
                    userId = 1L,
                    email = "berat.baran@example.com",
                    displayName = "Berat Baran",
                    editedDisplayName = "Berat Baran",
                    healthHalfHearts = 0,
                ),
            )
}
