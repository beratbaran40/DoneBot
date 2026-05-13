// Detekt mis-flags ShowTopBar()'s entire body as unreachable due to the `appDestinationFromRoute(...) ?: return`
// early-guard at the top — it can't follow the control flow past the elvis operator.
@file:Suppress("UnreachableCode")

package com.todoapp.mobile.ui.topbar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import coil.compose.AsyncImage
import com.example.uikit.R
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.LocalNavController
import com.todoapp.mobile.navigation.AppDestination
import com.todoapp.mobile.navigation.Screen
import com.todoapp.mobile.navigation.appDestinationFromRoute
import com.todoapp.mobile.ui.topbar.TopBarContract.UiAction
import com.todoapp.uikit.theme.TDTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TDTopBar(
    state: TDTopBarState,
    isBannerActivated: Boolean,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = state.title,
                textAlign = TextAlign.Center,
                style = TDTheme.typography.heading3,
                color = TDTheme.colors.onBackground,
            )
        },
        navigationIcon = {
            IconButton(onClick = state.onNavigationClick) {
                Icon(
                    painterResource(state.navigationIcon),
                    tint = TDTheme.colors.onBackground,
                    contentDescription = stringResource(state.navigationContentDescription),
                )
            }
        },
        actions = {
            state.actions.forEach { action ->
                IconButton(onClick = action.onClick) {
                    Box {
                        Icon(
                            painterResource(action.icon),
                            tint = TDTheme.colors.onBackground,
                            contentDescription = stringResource(action.contentDescription),
                        )
                        if (action.unreadBadgeCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(TDTheme.colors.crossRed),
                            )
                        }
                    }
                }
            }
            state.profileChip?.let { chip ->
                AvatarChip(
                    url = chip.avatarUrl,
                    initials = chip.initials,
                    onClick = chip.onClick,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = TDTheme.colors.background),
        windowInsets = if (isBannerActivated) WindowInsets(0, 0, 0, 0) else TopAppBarDefaults.windowInsets,
    )
}

@Composable
fun ShowTopBar(
    isBannerActivated: Boolean,
    onEvent: (UiAction) -> Unit,
    uiState: TopBarContract.UiState,
) {
    val navController = LocalNavController.current
    val route =
        navController
            .currentBackStackEntryAsState()
            .value
            ?.destination
            ?.route

    val normalizedRoute = normalizeRoute(route)
    val destination = appDestinationFromRoute(normalizedRoute) ?: return
    val titleText = stringResource(destination.title)
    val currentEntry = navController.currentBackStackEntryAsState().value
    val infoAction = TDTopBarAction(
        icon = R.drawable.ic_info,
        contentDescription = com.todoapp.mobile.R.string.cd_top_bar_info,
        onClick = { onEvent(UiAction.OnInfoClick) },
    )
    val state =
        when (destination) {
            AppDestination.Home ->
                TDTopBarState(
                    title = titleText,
                    onNavigationClick = { onEvent(UiAction.OnSettingsClick) },
                    navigationIcon = R.drawable.ic_settings,
                    navigationContentDescription = com.todoapp.mobile.R.string.cd_top_bar_settings,
                    actions =
                    buildList {
                        add(
                            TDTopBarAction(
                                icon = R.drawable.ic_search,
                                contentDescription = com.todoapp.mobile.R.string.cd_top_bar_search,
                                onClick = { onEvent(UiAction.OnSearchClick) },
                            ),
                        )
                        add(
                            TDTopBarAction(
                                icon = R.drawable.ic_notification,
                                contentDescription = com.todoapp.mobile.R.string.cd_top_bar_notifications,
                                onClick = { onEvent(UiAction.OnNotificationClick) },
                                unreadBadgeCount = uiState.unreadNotifications,
                            ),
                        )
                        if (destination.hasInfoDialog) add(infoAction)
                    },
                    profileChip =
                    if (uiState.isUserAuthenticated) {
                        TDProfileChip(
                            avatarUrl = absoluteAvatarUrl(uiState.avatarUrl, uiState.avatarVersion),
                            initials = initialsFrom(uiState.displayName),
                            onClick = { onEvent(UiAction.OnProfileClick) },
                        )
                    } else {
                        null
                    },
                )

            AppDestination.GroupDetail -> {
                val groupDetailArgs = runCatching { currentEntry?.toRoute<Screen.GroupDetail>() }.getOrNull()
                TDTopBarState(
                    title = groupDetailArgs?.groupName ?: titleText,
                    onNavigationClick = { onEvent(UiAction.OnBackClick) },
                    navigationIcon = R.drawable.ic_arrow_back,
                    navigationContentDescription = com.todoapp.mobile.R.string.cd_navigate_back,
                    actions =
                    buildList {
                        groupDetailArgs?.let { args ->
                            add(
                                TDTopBarAction(
                                    icon = com.example.uikit.R.drawable.ic_settings,
                                    contentDescription = com.todoapp.mobile.R.string.cd_top_bar_group_settings,
                                    onClick = { onEvent(UiAction.OnGroupSettingsClick(args.groupId)) },
                                ),
                            )
                        }
                        if (destination.hasInfoDialog) add(infoAction)
                    },
                )
            }

            else -> {
                TDTopBarState(
                    title = titleText,
                    onNavigationClick = { onEvent(UiAction.OnBackClick) },
                    navigationIcon = R.drawable.ic_arrow_back,
                    navigationContentDescription = com.todoapp.mobile.R.string.cd_navigate_back,
                    actions = if (destination.hasInfoDialog) listOf(infoAction) else emptyList(),
                )
            }
        }

    TDTopBar(state = state, isBannerActivated)
}

data class TDTopBarState(
    val title: String,
    @DrawableRes val navigationIcon: Int,
    @StringRes val navigationContentDescription: Int,
    val onNavigationClick: () -> Unit = {},
    val actions: List<TDTopBarAction> = emptyList(),
    val profileChip: TDProfileChip? = null,
)

data class TDTopBarAction(
    @DrawableRes val icon: Int,
    @StringRes val contentDescription: Int,
    val onClick: () -> Unit,
    val unreadBadgeCount: Int = 0,
)

data class TDProfileChip(
    val avatarUrl: String?,
    val initials: String,
    val onClick: () -> Unit,
)

@Composable
private fun AvatarChip(
    url: String?,
    initials: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
        Modifier
            .padding(end = 8.dp)
            .size(36.dp)
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
                modifier = Modifier.size(36.dp),
            )
        } else {
            Text(
                text = initials,
                style = TDTheme.typography.subheading2,
                color = TDTheme.colors.pendingGray,
            )
        }
    }
}

private fun absoluteAvatarUrl(
    path: String?,
    version: Long,
): String? {
    if (path.isNullOrBlank()) return null
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

private fun normalizeRoute(route: String?): String? = route?.substringBefore("/")?.substringBefore("?")

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun TDTopBarPreview_Home() {
    TDTheme {
        TDTopBar(
            state =
            TDTopBarState(
                title = "Home",
                navigationIcon = R.drawable.ic_settings,
                navigationContentDescription = com.todoapp.mobile.R.string.cd_top_bar_settings,
                onNavigationClick = {},
                actions =
                listOf(
                    TDTopBarAction(
                        icon = R.drawable.ic_hamburger,
                        contentDescription = com.todoapp.mobile.R.string.cd_top_bar_search,
                        onClick = {},
                    ),
                    TDTopBarAction(
                        icon = R.drawable.ic_notification,
                        contentDescription = com.todoapp.mobile.R.string.cd_top_bar_notifications,
                        onClick = {},
                    ),
                ),
            ),
            isBannerActivated = false,
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun TDTopBarPreview_Calendar() {
    TDTheme {
        TDTopBar(
            state =
            TDTopBarState(
                title = "Calendar",
                navigationIcon = R.drawable.ic_arrow_back,
                navigationContentDescription = com.todoapp.mobile.R.string.cd_navigate_back,
                onNavigationClick = { },
                actions =
                listOf(
                    TDTopBarAction(
                        icon = R.drawable.ic_hamburger,
                        contentDescription = com.todoapp.mobile.R.string.cd_top_bar_search,
                        onClick = {},
                    ),
                ),
            ),
            isBannerActivated = true,
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun TDTopBarPreview_NoActions() {
    TDTheme {
        TDTopBar(
            state =
            TDTopBarState(
                title = "Settings",
                navigationIcon = R.drawable.ic_arrow_back,
                navigationContentDescription = com.todoapp.mobile.R.string.cd_navigate_back,
                onNavigationClick = {},
                actions = emptyList(),
            ),
            isBannerActivated = false,
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun TDTopBarPreview_LongTitle() {
    TDTheme {
        TDTopBar(
            state =
            TDTopBarState(
                title = "Manage Members of Smith Family Group",
                navigationIcon = R.drawable.ic_arrow_back,
                navigationContentDescription = com.todoapp.mobile.R.string.cd_navigate_back,
                onNavigationClick = {},
                actions = emptyList(),
            ),
            isBannerActivated = false,
        )
    }
}
