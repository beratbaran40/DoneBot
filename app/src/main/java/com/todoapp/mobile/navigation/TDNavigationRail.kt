// Detekt false-positives the trailing Icon() block as unreachable due to the `?: return@NavigationRailItem`
// guard above it — the early-return only fires when icon is null.
@file:Suppress("UnreachableCode")

package com.todoapp.mobile.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.todoapp.mobile.LocalNavController
import com.todoapp.uikit.theme.TDTheme

@Composable
fun TDNavigationRail() {
    val navController = LocalNavController.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    if (!shouldShowNav(currentDestination)) return

    NavigationRail(
        containerColor = TDTheme.colors.background,
    ) {
        AppDestination.bottomBarItems.forEach { screen ->
            val screenRoute = screen.route
            val selected =
                currentDestination?.hierarchy?.any { dest ->
                    dest.route?.substringBefore("?")?.substringBefore("/") == screenRoute
                } == true

            NavigationRailItem(
                selected = selected,
                colors =
                NavigationRailItemDefaults.colors(
                    selectedIconColor = TDTheme.colors.pendingGray,
                    selectedTextColor = TDTheme.colors.pendingGray,
                    indicatorColor = TDTheme.colors.pendingGray.copy(alpha = 0.2f),
                ),
                onClick = {
                    if (selected) return@NavigationRailItem
                    navController.navigate(screenRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    val iconId = (if (selected) screen.selectedIcon else screen.icon)
                        ?: return@NavigationRailItem
                    Icon(
                        painter = painterResource(id = iconId),
                        contentDescription = stringResource(screen.title),
                        tint = Color.Unspecified,
                    )
                },
                label = { Text(text = stringResource(id = screen.title)) },
                alwaysShowLabel = false,
            )
        }
    }
}
