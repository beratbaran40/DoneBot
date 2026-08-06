package com.todoapp.mobile.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.ui.permissions.NotificationPermissionPrompt
import com.todoapp.mobile.ui.permissions.OverlayPermissionPrompt
import com.todoapp.uikit.theme.TDTheme

@Composable
internal fun SettingsPermissionPager(
    permissions: List<PermissionType>,
    onDismiss: (PermissionType) -> Unit,
) {
    if (permissions.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { permissions.size })

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { index ->
            when (permissions[index]) {
                // POST_NOTIFICATIONS only exists on 33+; the ViewModel never adds this entry below
                // that, but the guard is what lets the whole Settings tree drop its @RequiresApi
                // chain — and that chain was hiding a real crash (see SettingsNotificationsSection).
                PermissionType.NOTIFICATION ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        NotificationPermissionPrompt(
                            onGranted = { onDismiss(PermissionType.NOTIFICATION) },
                            onDismiss = { onDismiss(PermissionType.NOTIFICATION) },
                        )
                    }

                PermissionType.OVERLAY ->
                    OverlayPermissionPrompt(
                        onGranted = { onDismiss(PermissionType.OVERLAY) },
                        onDismiss = { onDismiss(PermissionType.OVERLAY) },
                    )
            }
        }

        if (permissions.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                repeat(permissions.size) { index ->
                    Box(
                        modifier =
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                            .background(
                                color =
                                if (pagerState.currentPage == index) {
                                    TDTheme.colors.pendingGray
                                } else {
                                    TDTheme.colors.onBackground.copy(alpha = 0.3f)
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}
