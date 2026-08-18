// Detekt false-positives the trailing Icon() block as unreachable due to the `?: return@NavigationBarItem`
// guard above it — the early-return only fires when icon is null.
@file:Suppress("UnreachableCode")

package com.todoapp.mobile.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.todoapp.mobile.LocalNavController
import com.todoapp.mobile.R
import com.todoapp.uikit.image.rememberKitArtPainter
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.modifier.crtScreen
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDTheme

fun shouldShowNav(currentDestination: NavDestination?): Boolean {
    val topLevelRoutes = AppDestination.bottomBarItems.map { it.route }.toSet()
    return currentDestination?.hierarchy?.any { dest ->
        dest.route?.substringBefore("?")?.substringBefore("/") in topLevelRoutes
    } == true
}

@Composable
fun TDBottomBar() {
    val navController = LocalNavController.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    if (!shouldShowNav(currentDestination)) return

    NavigationBar(
        containerColor = TDTheme.colors.background,
    ) {
        AppDestination.bottomBarItems.forEach { screen ->
            val screenRoute = screen.route
            val selected =
                currentDestination?.hierarchy?.any { dest ->
                    dest.route?.substringBefore("?")?.substringBefore("/") == screenRoute
                } == true
            val isChat = screen is AppDestination.Chat

            NavigationBarItem(
                selected = selected,
                colors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = TDTheme.colors.pendingGray,
                    selectedTextColor = TDTheme.colors.pendingGray,
                    unselectedIconColor = TDTheme.colors.gray,
                    unselectedTextColor = TDTheme.colors.gray,
                    indicatorColor =
                    if (isChat) Color.Transparent
                    else TDTheme.colors.primary.copy(alpha = 0.12f),
                ),
                onClick = {
                    if (selected) return@NavigationBarItem
                    navController.navigate(screenRoute) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    if (isChat) {
                        val chatImage =
                            if (selected) R.drawable.img_donebot_bottombar_2
                            else R.drawable.img_splash
                        Box(
                            modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TDTheme.colors.pendingGray)
                                .crtScreen(minSide = 40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = rememberKitArtPainter(painterResource(id = chatImage), 40.dp),
                                contentDescription = stringResource(screen.title),
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    } else {
                        // Under a flat tint the filled selected calendar art collapses into an
                        // unreadable blob (its detail is white-on-fill), so every tinting kit uses the
                        // clean outline instead. PIXEL is exempt: its own art is already a flat
                        // silhouette.
                        val useCalendarOutline = when (TDTheme.palette) {
                            PaletteKit.ORIGINAL, PaletteKit.PIXEL -> false
                            PaletteKit.MONOCHROME, PaletteKit.TERMINAL ->
                                selected && screen is AppDestination.Calendar
                        }
                        val iconId = (if (selected && !useCalendarOutline) screen.selectedIcon else screen.icon)
                            ?: return@NavigationBarItem
                        Icon(
                            painter = tdPainter(id = iconId),
                            contentDescription = stringResource(screen.title),
                            // ORIGINAL keeps the drawable's own art; the other kits tint the
                            // silhouette with the accent when selected, gray otherwise. That art
                            // hardcodes a slate blue, which would sit untouched on TERMINAL's CRT
                            // ground, so this kit has to tint too.
                            tint = when (TDTheme.palette) {
                                PaletteKit.ORIGINAL -> Color.Unspecified
                                PaletteKit.MONOCHROME, PaletteKit.PIXEL, PaletteKit.TERMINAL ->
                                    if (selected) TDTheme.colors.primary else TDTheme.colors.gray
                            },
                        )
                    }
                },
                label = {
                    val labelRes = if (isChat) R.string.bottombar_chat_tab_label else screen.title
                    // Explicit style: NavigationBarItem wraps its label in ProvideTextStyle with
                    // MaterialTheme.typography.labelMedium, which overrides the LocalTextStyle the
                    // active kit provides — a bare Text() here renders in Roboto, not the kit's face.
                    Text(
                        text = stringResource(id = labelRes),
                        style = TDTheme.typography.subheading2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                alwaysShowLabel = false,
            )
        }
    }
}
