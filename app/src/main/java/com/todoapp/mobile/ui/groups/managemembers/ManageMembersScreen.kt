package com.todoapp.mobile.ui.groups.managemembers

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.uikit.R
import com.todoapp.mobile.ui.groups.groupdetail.MemberAvatar
import com.todoapp.mobile.ui.groups.groupdetail.RoleBadge
import com.todoapp.mobile.ui.groups.managemembers.ManageMembersContract.ManageMemberUiItem
import com.todoapp.mobile.ui.groups.managemembers.ManageMembersContract.UiAction
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.theme.TDTheme

@Composable
fun ManageMembersScreen(viewModel: ManageMembersViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onAction(UiAction.OnScreenResumed)
        }
    }

    viewModel.uiEffect.collectWithLifecycle { effect ->
        when (effect) {
            is ManageMembersContract.UiEffect.ShowToast -> {
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    ManageMembersContent(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun ManageMembersContent(
    uiState: ManageMembersContract.UiState,
    onAction: (UiAction) -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .background(TDTheme.colors.background),
    ) {
        when (uiState) {
            is ManageMembersContract.UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TDTheme.colors.pendingGray)
                }
            }

            is ManageMembersContract.UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TDText(text = uiState.message, color = TDTheme.colors.crossRed)
                }
            }

            is ManageMembersContract.UiState.Success -> {
                ManageMembersSuccessContent(uiState = uiState, onAction = onAction)
            }
        }
    }
}

@Composable
private fun ManageMembersSuccessContent(
    uiState: ManageMembersContract.UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    LazyColumn(
        modifier =
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            TDText(
                text = stringResource(com.todoapp.mobile.R.string.current_members),
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.gray,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        items(uiState.members, key = { it.userId }) { member ->
            ManageMemberRow(
                member = member,
                onClick = { onAction(UiAction.OnMemberTap(member.userId)) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            TDText(
                text = stringResource(com.todoapp.mobile.R.string.manage_members_hint),
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.gray,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ManageMemberRow(
    member: ManageMemberUiItem,
    onClick: () -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TDTheme.colors.background)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MemberAvatar(
            initials = member.initials,
            avatarUrl = member.avatarUrl,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TDText(
                    text = member.displayName,
                    style = TDTheme.typography.subheading2,
                    color = TDTheme.colors.onBackground,
                )
                RoleBadge(role = member.role)
            }
            TDText(
                text = member.email,
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.gray,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_forward),
            contentDescription = null,
            tint = TDTheme.colors.gray,
            modifier = Modifier.size(20.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun ManageMembersContentPreview() {
    TDTheme {
        ManageMembersContent(
            uiState =
            ManageMembersContract.UiState.Success(
                members =
                listOf(
                    ManageMemberUiItem(
                        1,
                        "John Doe",
                        "JD",
                        "john@example.com",
                        "ADMIN",
                        role = "ADMIN",
                    ),
                    ManageMemberUiItem(
                        2,
                        "Jane Smith",
                        "JS",
                        "jane@example.com",
                        "MEMBER",
                        role = "MEMBER",
                    ),
                ),
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun ManageMembersContentDarkPreview() {
    TDTheme {
        ManageMembersContent(
            uiState =
            ManageMembersContract.UiState.Success(
                members =
                listOf(
                    ManageMemberUiItem(
                        1,
                        "John Doe",
                        "JD",
                        "john@example.com",
                        "ADMIN",
                        role = "ADMIN",
                    ),
                    ManageMemberUiItem(
                        2,
                        "Jane Smith",
                        "JS",
                        "jane@example.com",
                        "MEMBER",
                        role = "MEMBER",
                    ),
                ),
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun ManageMembersLoadingPreview() {
    TDTheme {
        ManageMembersContent(uiState = ManageMembersContract.UiState.Loading, onAction = {})
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun ManageMembersErrorPreview() {
    TDTheme {
        ManageMembersContent(
            uiState = ManageMembersContract.UiState.Error("Could not load members"),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun ManageMembersEmptyPreview() {
    TDTheme {
        ManageMembersContent(
            uiState = ManageMembersContract.UiState.Success(members = emptyList()),
            onAction = {},
        )
    }
}
