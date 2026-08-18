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
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.todoapp.mobile.LocalNavController
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.theme.PaletteKit
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
                    selectedIconColor = TDTheme.colors.primary,
                    selectedTextColor = TDTheme.colors.primary,
                    unselectedIconColor = TDTheme.colors.gray,
                    unselectedTextColor = TDTheme.colors.gray,
                    indicatorColor = TDTheme.colors.primary.copy(alpha = 0.12f),
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
                    // Mirrors TDBottomBar — see the rationale there.
                    val useCalendarOutline = when (TDTheme.palette) {
                        PaletteKit.ORIGINAL, PaletteKit.PIXEL -> false
                        PaletteKit.MONOCHROME, PaletteKit.TERMINAL ->
                            selected && screen is AppDestination.Calendar
                    }
                    val iconId = (if (selected && !useCalendarOutline) screen.selectedIcon else screen.icon)
                        ?: return@NavigationRailItem
                    Icon(
                        painter = tdPainter(id = iconId),
                        contentDescription = stringResource(screen.title),
                        tint = when (TDTheme.palette) {
                            PaletteKit.ORIGINAL -> Color.Unspecified
                            PaletteKit.MONOCHROME, PaletteKit.PIXEL, PaletteKit.TERMINAL ->
                                if (selected) TDTheme.colors.primary else TDTheme.colors.gray
                        },
                    )
                },
                // Explicit style — NavigationRailItem's ProvideTextStyle would otherwise override the
                // kit's LocalTextStyle with MaterialTheme's default (Roboto). See TDBottomBar.
                label = {
                    Text(
                        text = stringResource(id = screen.title),
                        style = TDTheme.typography.subheading2,
                    )
                },
                alwaysShowLabel = false,
            )
        }
    }
}
