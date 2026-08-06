package com.todoapp.mobile.ui.home

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.todoapp.mobile.data.perf.StartupColdStartTrace
import com.todoapp.mobile.ui.home.HomeContract.UiAction
import com.todoapp.mobile.ui.home.HomeContract.UiEffect
import com.todoapp.mobile.ui.home.HomeContract.UiState
import com.todoapp.mobile.ui.security.biometric.BiometricAuthenticator
import com.todoapp.uikit.components.TDErrorState
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow

@Composable
fun HomeScreen(
    uiState: UiState,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        onAction(UiAction.RefreshPermissions)
    }

    // §3.10 Stop the cold-start → first-content trace once Home first reaches Success
    // (idempotent; no-op when perf collection is off).
    LaunchedEffect(uiState is UiState.Success) {
        if (uiState is UiState.Success) StartupColdStartTrace.stop()
    }

    uiEffect.collectWithLifecycle {
        when (it) {
            is UiEffect.ShowToast -> {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }

            is UiEffect.ShowBiometricAuthenticator -> {
                handleBiometricAuthentication(context) {
                    onAction(UiAction.OnSuccessfulBiometricAuthenticationHandle)
                }
            }

            is UiEffect.ShowBiometricForSecretToggle -> {
                val task = it.task
                handleBiometricAuthentication(context) {
                    onAction(UiAction.OnBiometricSuccessForSecretToggle(task))
                }
            }

            is UiEffect.ShowError -> {
                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    when (uiState) {
        is UiState.Loading -> HomeSkeleton()
        is UiState.Error -> TDErrorState(
            modifier = Modifier,
            message = uiState.message,
            actionText = stringResource(com.todoapp.mobile.R.string.retry),
            onActionClick = { onAction(UiAction.OnRetry) },
        )
        is UiState.Success -> HomeSuccessContent(uiState = uiState, onAction = onAction)
    }
}

@Composable
private fun HomeSuccessContent(
    uiState: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    HomeContent(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        uiState = uiState,
        onAction = onAction,
    )
}

private suspend fun handleBiometricAuthentication(
    context: Context,
    onSuccess: () -> Unit,
) {
    val activity = context as? FragmentActivity ?: return
    val isAuthenticated = BiometricAuthenticator.authenticate(activity)
    if (isAuthenticated) onSuccess()
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeErrorPreview() {
    TDTheme {
        TDErrorState(
            modifier = Modifier.background(TDTheme.colors.background),
            message = "Something went wrong",
            actionText = "Retry",
            onActionClick = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@com.todoapp.uikit.previews.TDPreviewDevices
@Composable
private fun HomeSuccessWithTasksPreview() {
    TDTheme {
        HomeSuccessContent(
            uiState = HomePreviewData.successState(
                tasks = HomePreviewData.sampleTasks,
                completedTaskCountThisWeek = 5,
                pendingTaskCountThisWeek = 12,
            ),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeSuccessEmptyPreview() {
    TDTheme {
        HomeSuccessContent(
            uiState = HomePreviewData.successState(
                tasks = emptyList(),
                completedTaskCountThisWeek = 0,
                pendingTaskCountThisWeek = 0,
            ),
            onAction = {},
        )
    }
}
