package com.todoapp.mobile.ui.groups.invitemember

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.uikit.R
import com.todoapp.mobile.ui.groups.invitemember.InviteMemberContract.UiAction
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDInfoCard
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.theme.TDTheme

@Composable
fun InviteMemberScreen(viewModel: InviteMemberViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    viewModel.uiEffect.collectWithLifecycle { effect ->
        when (effect) {
            is InviteMemberContract.UiEffect.ShowToast -> {
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    InviteMemberContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun InviteMemberContent(
    uiState: InviteMemberContract.UiState,
    onAction: (UiAction) -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .background(TDTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            painter = painterResource(R.drawable.ic_members),
            contentDescription = null,
            tint = TDTheme.colors.pendingGray,
            modifier = Modifier.size(100.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        TDText(
            text = stringResource(com.todoapp.mobile.R.string.grow_your_family_group),
            style = TDTheme.typography.heading3,
            color = TDTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TDText(
            text = stringResource(com.todoapp.mobile.R.string.invite_subtitle),
            style = TDTheme.typography.subheading3,
            color = TDTheme.colors.gray,
        )

        Spacer(modifier = Modifier.height(32.dp))

        TDText(
            text = stringResource(com.todoapp.mobile.R.string.email_address),
            style = TDTheme.typography.subheading2,
            color = TDTheme.colors.onBackground,
            modifier = Modifier.align(Alignment.Start),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.email,
            onValueChange = { onAction(UiAction.OnEmailChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                TDText(
                    text = stringResource(com.todoapp.mobile.R.string.invite_member_email_hint),
                    color = TDTheme.colors.gray,
                    style = TDTheme.typography.subheading3,
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_mail),
                    contentDescription = null,
                    tint = TDTheme.colors.gray,
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (uiState.emailError != null) TDTheme.colors.crossRed else TDTheme.colors.pendingGray,
                unfocusedBorderColor = if (uiState.emailError != null) TDTheme.colors.crossRed else TDTheme.colors.lightGray,
                focusedTextColor = TDTheme.colors.onBackground,
                unfocusedTextColor = TDTheme.colors.onBackground,
                cursorColor = TDTheme.colors.pendingGray,
            ),
            singleLine = true,
            isError = uiState.emailError != null,
            supportingText =
            uiState.emailError?.let { error ->
                {
                    TDText(
                        text = error,
                        style = TDTheme.typography.subheading1,
                        color = TDTheme.colors.crossRed,
                    )
                }
            },
        )

        Spacer(modifier = Modifier.height(24.dp))

        TDButton(
            text = stringResource(com.todoapp.mobile.R.string.send_invite),
            type = TDButtonType.PRIMARY,
            fullWidth = true,
            isEnable = !uiState.isLoading,
            onClick = { onAction(UiAction.OnSendInviteTap) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        TDInfoCard(
            text = stringResource(com.todoapp.mobile.R.string.invite_info),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
            Modifier
                .clickable { onAction(UiAction.OnShareLinkTap) }
                .padding(8.dp),
        ) {
            TDText(
                text = stringResource(com.todoapp.mobile.R.string.or_share_invite_link),
                style = TDTheme.typography.subheading3,
                color = TDTheme.colors.pendingGray,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = TDTheme.colors.pendingGray,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun InviteMemberPreview() {
    TDTheme {
        InviteMemberContent(
            uiState =
            InviteMemberContract.UiState(
                email = "user@example.com",
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun InviteMemberDarkPreview() {
    TDTheme {
        InviteMemberContent(
            uiState =
            InviteMemberContract.UiState(
                email = "user@example.com",
                emailError = "Invalid email address",
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun InviteMemberEmptyPreview() {
    TDTheme {
        InviteMemberContent(uiState = InviteMemberContract.UiState(), onAction = {})
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun InviteMemberSubmittingPreview() {
    TDTheme {
        InviteMemberContent(
            uiState =
            InviteMemberContract.UiState(
                email = "user@example.com",
                isLoading = true,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun InviteMemberSentPreview() {
    TDTheme {
        InviteMemberContent(
            uiState =
            InviteMemberContract.UiState(
                email = "user@example.com",
                isSent = true,
            ),
            onAction = {},
        )
    }
}
