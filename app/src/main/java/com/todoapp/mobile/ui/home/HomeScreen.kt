package com.todoapp.mobile.ui.home

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.todoapp.mobile.ui.home.HomeContract.UiAction
import com.todoapp.mobile.ui.home.HomeContract.UiEffect
import com.todoapp.mobile.ui.home.HomeContract.UiState
import com.todoapp.mobile.ui.security.biometric.BiometricAuthenticator
import com.todoapp.uikit.components.TDErrorState
import com.todoapp.uikit.components.TDScreenWithSheet
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
            modifier = Modifier.background(TDTheme.colors.background),
            message = uiState.message,
            actionText = stringResource(com.todoapp.mobile.R.string.retry),
            onActionClick = { onAction(UiAction.OnRetry) },
        )
        is UiState.Success -> HomeSuccessContent(uiState = uiState, onAction = onAction)
    }
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun HomeSuccessContent(
    uiState: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    TDScreenWithSheet(
        isSheetOpen = uiState.isSheetOpen,
        sheetContent = {
            AddTaskSheet(
                formState = uiState.taskFormState,
                availableGroups = uiState.availableGroups,
                onAction = { action ->
                    when (action) {
                        is TaskFormUiAction.Dismiss -> onAction(UiAction.OnDismissBottomSheet)
                        is TaskFormUiAction.Create -> onAction(UiAction.OnTaskCreate)
                        is TaskFormUiAction.TitleChange -> onAction(UiAction.OnTaskTitleChange(action.title))
                        is TaskFormUiAction.DateSelect -> onAction(UiAction.OnDialogDateSelect(action.date))
                        is TaskFormUiAction.DateDeselect -> onAction(UiAction.OnDialogDateDeselect)
                        is TaskFormUiAction.TimeStartChange -> onAction(UiAction.OnTaskTimeStartChange(action.time))
                        is TaskFormUiAction.TimeEndChange -> onAction(UiAction.OnTaskTimeEndChange(action.time))
                        is TaskFormUiAction.DescriptionChange ->
                            onAction(
                                UiAction.OnTaskDescriptionChange(action.description),
                            )

                        is TaskFormUiAction.ToggleAdvancedSettings -> onAction(UiAction.OnToggleAdvancedSettings)
                        is TaskFormUiAction.SecretChange -> onAction(UiAction.OnTaskSecretChange(action.isSecret))
                        is TaskFormUiAction.ReminderOffsetChange ->
                            onAction(UiAction.OnReminderOffsetChange(action.minutes))

                        is TaskFormUiAction.GroupSelectionChanged ->
                            onAction(
                                UiAction.OnGroupSelectionChanged(action.groupId),
                            )

                        is TaskFormUiAction.PriorityChange -> Unit
                        is TaskFormUiAction.AssigneeChange -> Unit
                        is TaskFormUiAction.PhotoPicked ->
                            onAction(
                                UiAction.OnPendingPhotoAdd(action.bytes, action.mimeType),
                            )

                        is TaskFormUiAction.PhotoRemoveAt ->
                            onAction(
                                UiAction.OnPendingPhotoRemove(action.index),
                            )

                        is TaskFormUiAction.ExistingPhotoToggleDelete -> Unit
                        is TaskFormUiAction.CategoryChange ->
                            onAction(UiAction.OnCategoryChange(action.category))

                        is TaskFormUiAction.CustomCategoryNameChange ->
                            onAction(UiAction.OnCustomCategoryNameChange(action.name))

                        is TaskFormUiAction.RecurrenceChange ->
                            onAction(UiAction.OnRecurrenceChange(action.recurrence))

                        is TaskFormUiAction.AllDayChange ->
                            onAction(UiAction.OnAllDayChange(action.isAllDay))

                        is TaskFormUiAction.LocationPicked ->
                            onAction(
                                UiAction.OnLocationPicked(
                                    name = action.name,
                                    address = action.address,
                                    lat = action.lat,
                                    lng = action.lng,
                                ),
                            )

                        is TaskFormUiAction.LocationCleared ->
                            onAction(UiAction.OnLocationCleared)
                    }
                },
            )
        },
        onDismissSheet = { onAction(UiAction.OnDismissBottomSheet) },
    ) {
        HomeContent(
            modifier =
            Modifier
                .fillMaxWidth()
                .background(TDTheme.colors.background)
                .padding(horizontal = 16.dp),
            uiState = uiState,
            onAction = onAction,
        )
    }
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
