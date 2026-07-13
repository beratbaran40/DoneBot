package com.todoapp.mobile.ui.blockedusers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.blockedusers.BlockedUsersContract.BlockedUserUiItem
import com.todoapp.mobile.ui.blockedusers.BlockedUsersContract.UiAction
import com.todoapp.mobile.ui.blockedusers.BlockedUsersContract.UiState
import com.todoapp.mobile.ui.groups.groupdetail.MemberAvatar
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

@Composable
fun BlockedUsersScreen(viewModel: BlockedUsersViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BlockedUsersContent(uiState = uiState, onAction = viewModel::onAction)
}

@Composable
private fun BlockedUsersContent(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    Box(
        modifier =
        Modifier
            .fillMaxSize(),
    ) {
        when (uiState) {
            is UiState.Loading ->
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = TDTheme.colors.pendingGray,
                )

            is UiState.Success ->
                if (uiState.users.isEmpty()) {
                    TDText(
                        text = stringResource(R.string.blocked_users_empty),
                        style = TDTheme.typography.subheading1,
                        color = TDTheme.colors.gray,
                        textAlign = TextAlign.Center,
                        modifier =
                        Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp),
                    )
                } else {
                    LazyColumn(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        items(uiState.users, key = { it.userId }) { user ->
                            BlockedUserRow(
                                user = user,
                                onUnblock = { onAction(UiAction.OnUnblock(user.userId)) },
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun BlockedUserRow(
    user: BlockedUserUiItem,
    onUnblock: () -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MemberAvatar(
            initials = user.initials.ifBlank { "?" },
            avatarUrl = null,
        )
        Spacer(modifier = Modifier.width(12.dp))
        TDText(
            text = user.displayName.ifBlank { stringResource(R.string.blocked_user_fallback) },
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        TDButton(
            text = stringResource(R.string.unblock),
            type = TDButtonType.SECONDARY,
            size = TDButtonSize.SMALL,
            onClick = onUnblock,
        )
    }
}

@TDPreview
@Composable
private fun BlockedUsersLoadingPreview() {
    TDTheme { BlockedUsersContent(uiState = UiState.Loading, onAction = {}) }
}

@TDPreview
@Composable
private fun BlockedUsersEmptyPreview() {
    TDTheme { BlockedUsersContent(uiState = UiState.Success(emptyList()), onAction = {}) }
}

@TDPreview
@Composable
private fun BlockedUsersSuccessPreview() {
    TDTheme {
        BlockedUsersContent(
            uiState =
            UiState.Success(
                listOf(
                    BlockedUserUiItem(1L, "Jane Smith", "JS"),
                    BlockedUserUiItem(2L, "Bob Lee", "BL"),
                ),
            ),
            onAction = {},
        )
    }
}
