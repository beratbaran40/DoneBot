package com.todoapp.mobile.ui.groups

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.uikit.R
import com.todoapp.mobile.navigation.NavigationEffectController
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.ui.groups.GroupsContract.UiAction
import com.todoapp.mobile.ui.groups.GroupsContract.UiState
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailScreen
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailViewModel
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.theme.TDTheme
import kotlinx.serialization.Serializable

/** Local nested-NavHost start route shown until a group is picked. */
@Serializable
private data object DetailPanePlaceholder

private val LIST_PANE_WIDTH = 360.dp

/**
 * Two-pane (list-detail) Groups layout for Expanded width (tablets in landscape).
 *
 * Left: the group list ([GroupScreen]); right: a nested NavHost that shows a placeholder until a
 * group is picked, then [GroupDetailScreen]. A nested NavHost is used (rather than hosting the
 * detail directly) so [GroupDetailViewModel] keeps receiving its `groupId` from the route's
 * SavedStateHandle exactly as it does in single-pane navigation. The detail's own sub-navigations
 * (invite member, settings, member profile, …) flow through the main NavController and open
 * full-screen over the two pane layout.
 */
@Composable
fun GroupsTwoPane(
    uiState: UiState,
    onAction: (UiAction) -> Unit,
) {
    val detailNavController = rememberNavController()
    var selectedGroupId by rememberSaveable { mutableStateOf<Long?>(null) }

    // System back deselects (returns to the placeholder) before leaving the Groups screen.
    BackHandler(enabled = selectedGroupId != null) {
        selectedGroupId = null
        detailNavController.popBackStack(route = DetailPanePlaceholder, inclusive = false)
    }

    Row(
        modifier =
        Modifier
            .fillMaxSize(),
    ) {
        Box(
            modifier =
            Modifier
                .width(LIST_PANE_WIDTH)
                .fillMaxHeight(),
        ) {
            GroupScreen(
                uiState = uiState,
                onAction = onAction,
                onGroupSelect = { groupId, groupName, initialTab ->
                    selectedGroupId = groupId
                    detailNavController.navigate(
                        Screen.GroupDetail(groupId = groupId, groupName = groupName, initialTab = initialTab),
                    ) {
                        popUpTo(DetailPanePlaceholder) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }

        Box(
            modifier =
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(TDTheme.colors.lightGray.copy(alpha = 0.5f)),
        )

        Box(
            modifier =
            Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            NavHost(
                navController = detailNavController,
                startDestination = DetailPanePlaceholder,
            ) {
                composable<DetailPanePlaceholder> {
                    GroupDetailPanePlaceholder()
                }
                composable<Screen.GroupDetail> { entry ->
                    val groupName = entry.toRoute<Screen.GroupDetail>().groupName
                    val viewModel: GroupDetailViewModel = hiltViewModel()
                    NavigationEffectController(viewModel.navEffect)
                    Column(modifier = Modifier.fillMaxSize()) {
                        TDText(
                            text = groupName,
                            style = TDTheme.typography.heading2,
                            color = TDTheme.colors.onBackground,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                        )
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            GroupDetailScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupDetailPanePlaceholder() {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = tdPainter(R.drawable.ic_members),
            contentDescription = null,
            tint = TDTheme.colors.pendingGray,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(12.dp))
        TDText(
            text = stringResource(com.todoapp.mobile.R.string.groups_select_a_group),
            style = TDTheme.typography.heading3,
            color = TDTheme.colors.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        TDText(
            text = stringResource(com.todoapp.mobile.R.string.groups_select_a_group_description),
            modifier = Modifier.padding(horizontal = 48.dp),
            style = TDTheme.typography.heading6,
            color = TDTheme.colors.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
    }
}
