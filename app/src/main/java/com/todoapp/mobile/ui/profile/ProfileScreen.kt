// Detekt mis-flags trailing code after `?: return@rememberLauncherForActivityResult` as unreachable.
@file:Suppress("UnreachableCode")

package com.todoapp.mobile.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.profile.ProfileContract.UiAction
import com.todoapp.mobile.ui.profile.ProfileContract.UiState
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    viewModel.uiEffect.collectWithLifecycle { effect ->
        when (effect) {
            is ProfileContract.UiEffect.ShowToast ->
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
    }

    val picker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            // Route the picked image through the crop screen; upload happens once it returns.
            viewModel.onAction(UiAction.OnAvatarPicked(uri.toString()))
        }

    ProfileContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onPickAvatar = {
            picker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
    )
}

@Composable
private fun ProfileContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
    onPickAvatar: () -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .background(TDTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(color = TDTheme.colors.pendingGray)
            return@Column
        }

        AvatarDisplay(
            url = uiState.avatarUrl?.let { absoluteAvatarUrl(it, uiState.avatarVersion) },
            initials = initialsFrom(uiState.displayName),
            isUploading = uiState.isUploading,
            onClick = onPickAvatar,
        )

        Spacer(modifier = Modifier.height(8.dp))

        TDText(
            text = stringResource(R.string.change_photo),
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.pendingGray,
            modifier = Modifier.clickable(onClick = onPickAvatar),
        )

        Spacer(modifier = Modifier.height(24.dp))

        TDText(
            text = stringResource(R.string.full_name),
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.onBackground,
            modifier = Modifier.align(Alignment.Start),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.editedDisplayName,
            onValueChange = { onAction(UiAction.OnDisplayNameChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TDTheme.colors.pendingGray,
                unfocusedBorderColor = TDTheme.colors.lightGray,
                focusedTextColor = TDTheme.colors.onBackground,
                unfocusedTextColor = TDTheme.colors.onBackground,
                cursorColor = TDTheme.colors.pendingGray,
            ),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        TDText(
            text = stringResource(R.string.email),
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.onBackground,
            modifier = Modifier.align(Alignment.Start),
        )
        Spacer(modifier = Modifier.height(8.dp))
        TDText(
            text = uiState.email,
            style = TDTheme.typography.regularTextStyle,
            color = TDTheme.colors.gray,
            modifier =
            Modifier
                .align(Alignment.Start)
                .padding(vertical = 12.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TDTheme.colors.lightPending)
                .clickable { onAction(UiAction.OnChangePasswordTap) }
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Column {
                TDText(
                    text = stringResource(R.string.change_password),
                    style = TDTheme.typography.subheading2,
                    color = TDTheme.colors.onBackground,
                )
                Spacer(modifier = Modifier.height(4.dp))
                TDText(
                    text = stringResource(R.string.change_password_hint),
                    style = TDTheme.typography.regularTextStyle,
                    color = TDTheme.colors.gray,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TDButton(
            text = stringResource(R.string.save_changes),
            isEnable =
            !uiState.isSaving &&
                uiState.editedDisplayName.isNotBlank() &&
                uiState.editedDisplayName.trim() != uiState.displayName,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onAction(UiAction.OnSaveName) },
        )
    }
}

@Composable
private fun AvatarDisplay(
    url: String?,
    initials: String,
    isUploading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
        Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(TDTheme.colors.lightPending)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            TDText(
                text = initials,
                style = TDTheme.typography.heading4,
                color = TDTheme.colors.pendingGray,
            )
        }
        if (isUploading) {
            Box(
                modifier =
                Modifier
                    .fillMaxSize()
                    .background(TDTheme.colors.background.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = TDTheme.colors.pendingGray)
            }
        }
    }
}

private fun absoluteAvatarUrl(
    path: String,
    version: Long,
): String {
    val base = BuildConfig.BASE_URL.trimEnd('/')
    val relative = path.trimStart('/')
    return "$base/$relative?v=$version"
}

private fun initialsFrom(name: String): String = name
    .split(" ")
    .mapNotNull { it.firstOrNull()?.toString() }
    .take(2)
    .joinToString("")
    .uppercase()

@TDPreview
@Composable
private fun ProfileScreenPreview(
    @PreviewParameter(ProfilePreviewProvider::class) state: UiState,
) {
    TDTheme {
        ProfileContent(
            uiState = state,
            onAction = {},
            onPickAvatar = {},
        )
    }
}
