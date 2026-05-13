package com.todoapp.mobile.ui.filteredtasks

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.uikit.R
import com.todoapp.mobile.common.maskDescription
import com.todoapp.mobile.common.maskTitle
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.ui.filteredtasks.FilteredTasksContract.SortOrder
import com.todoapp.mobile.ui.filteredtasks.FilteredTasksContract.TaskTab
import com.todoapp.mobile.ui.filteredtasks.FilteredTasksContract.UiAction
import com.todoapp.mobile.ui.filteredtasks.FilteredTasksContract.UiEffect
import com.todoapp.mobile.ui.filteredtasks.FilteredTasksContract.UiState
import com.todoapp.mobile.ui.security.biometric.BiometricAuthenticator
import com.todoapp.uikit.components.TDLoadingBar
import com.todoapp.uikit.components.TDTaskCardWithCheckbox
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.components.TDUndoSnackbar
import com.todoapp.uikit.components.TDWeekNavigator
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun FilteredTasksScreen(
    uiState: UiState,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    val context = LocalContext.current

    uiEffect.collectWithLifecycle {
        when (it) {
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
        }
    }

    when (uiState) {
        is UiState.Loading -> TDLoadingBar()
        is UiState.Error -> FilteredTasksErrorContent(uiState.message, onAction)
        is UiState.Success -> FilteredTasksSuccessContent(uiState, onAction)
    }
}

@Composable
private fun FilteredTasksErrorContent(
    message: String,
    onAction: (UiAction) -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .background(TDTheme.colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_error),
            contentDescription = null,
            tint = TDTheme.colors.crossRed,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        TDText(
            text = message,
            style = TDTheme.typography.heading3,
            color = TDTheme.colors.onBackground,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilteredTasksSuccessContent(
    uiState: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    val visibleTasks =
        remember(uiState.tasks, uiState.pendingDeleteTask) {
            uiState.tasks.filter { it.id != uiState.pendingDeleteTask?.id }
        }

    if (uiState.isDeleteDialogOpen) {
        AlertDialog(
            onDismissRequest = { onAction(UiAction.OnDeleteDialogDismiss) },
            title = { Text(stringResource(com.todoapp.mobile.R.string.delete_task_title)) },
            titleContentColor = TDTheme.colors.onBackground,
            containerColor = TDTheme.colors.background,
            textContentColor = TDTheme.colors.onBackground,
            text = { Text(stringResource(com.todoapp.mobile.R.string.delete_task_message)) },
            confirmButton = {
                TextButton(onClick = { onAction(UiAction.OnDeleteDialogConfirm) }) {
                    Text(stringResource(com.todoapp.mobile.R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(UiAction.OnDeleteDialogDismiss) }) {
                    Text(stringResource(com.todoapp.mobile.R.string.cancel))
                }
            },
        )
    }

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .background(TDTheme.colors.background),
    ) {
        // Week navigator
        TDWeekNavigator(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            selectedDate = uiState.selectedWeekDate,
            onPreviousWeek = { onAction(UiAction.OnWeekSelect(uiState.selectedWeekDate.minusWeeks(1))) },
            onNextWeek = { onAction(UiAction.OnWeekSelect(uiState.selectedWeekDate.plusWeeks(1))) },
        )

        // Tab toggle
        FilteredTasksTabRow(
            selectedTab = uiState.selectedTab,
            onTabSelect = { onAction(UiAction.OnTabSelect(it)) },
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // Sort order button — right-aligned below the tab row
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                onClick = { onAction(UiAction.OnToggleSortOrder) },
                shape = RoundedCornerShape(8.dp),
                color = TDTheme.colors.onBackground.copy(alpha = 0.08f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        modifier = Modifier.size(12.dp),
                        painter = painterResource(com.todoapp.mobile.R.drawable.ic_calendar),
                        contentDescription = null,
                        tint = TDTheme.colors.onBackground,
                    )
                    TDText(
                        text =
                        if (uiState.sortOrder == SortOrder.ASC) {
                            stringResource(com.todoapp.mobile.R.string.sort_order_oldest)
                        } else {
                            stringResource(com.todoapp.mobile.R.string.sort_order_newest)
                        },
                        style = TDTheme.typography.subheading4,
                        color = TDTheme.colors.onBackground,
                    )
                    Icon(
                        modifier = Modifier.size(12.dp),
                        painter =
                        painterResource(
                            if (uiState.sortOrder == SortOrder.ASC) {
                                R.drawable.ic_arrow_up
                            } else {
                                R.drawable.ic_arrow_down
                            },
                        ),
                        contentDescription = null,
                        tint = TDTheme.colors.onBackground,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Task list
        Box(modifier = Modifier.weight(1f)) {
            if (visibleTasks.isEmpty()) {
                Column(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    TDText(
                        text = stringResource(com.todoapp.mobile.R.string.filtered_tasks_no_tasks),
                        style = TDTheme.typography.heading3,
                        color = TDTheme.colors.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    TDText(
                        text = stringResource(com.todoapp.mobile.R.string.filtered_tasks_no_tasks_description),
                        style = TDTheme.typography.heading6,
                        color = TDTheme.colors.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { Spacer(Modifier.height(0.dp)) }
                    itemsIndexed(
                        items = visibleTasks,
                        key = { _, task -> task.id },
                    ) { _, task ->
                        val dismissState = rememberSwipeToDismissBoxState()

                        LaunchedEffect(dismissState.currentValue) {
                            when (dismissState.currentValue) {
                                SwipeToDismissBoxValue.EndToStart -> {
                                    onAction(UiAction.OnTaskLongPress(task))
                                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                }

                                SwipeToDismissBoxValue.StartToEnd -> {
                                    onAction(UiAction.OnToggleTaskSecret(task))
                                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                }

                                SwipeToDismissBoxValue.Settled -> {}
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                FilteredSwipeDismissBackground(direction = dismissState.dismissDirection)
                            },
                        ) {
                            Column {
                                Row(
                                    modifier =
                                    Modifier
                                        .padding(start = 4.dp, bottom = 4.dp)
                                        .background(
                                            color = TDTheme.colors.onBackground.copy(alpha = 0.06f),
                                            shape = RoundedCornerShape(6.dp),
                                        ).padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        modifier = Modifier.size(10.dp),
                                        painter = painterResource(com.todoapp.mobile.R.drawable.ic_calendar),
                                        contentDescription = null,
                                        tint = TDTheme.colors.onBackground.copy(alpha = 0.5f),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    TDText(
                                        text = task.date.format(DATE_FORMATTER),
                                        style = TDTheme.typography.subheading2,
                                        color = TDTheme.colors.onBackground.copy(alpha = 0.5f),
                                    )
                                }
                                val openLocation = com.todoapp.mobile.ui.common.rememberOpenLocation(
                                    task.locationName, task.locationAddress, task.locationLat, task.locationLng,
                                )
                                Card(onClick = { onAction(UiAction.OnTaskClick(task)) }) {
                                    TDTaskCardWithCheckbox(
                                        taskText = if (task.isSecret) task.title.maskTitle() else task.title,
                                        taskDescription = if (task.isSecret) task.description?.maskDescription() else task.description,
                                        isChecked = task.isCompleted,
                                        onCheckBoxClick = { onAction(UiAction.OnTaskCheck(task)) },
                                        locationLabel = task.locationName,
                                        onLocationClick = openLocation,
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(0.dp)) }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.pendingDeleteTask != null,
            modifier = Modifier.fillMaxWidth(),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            TDUndoSnackbar(
                message = stringResource(com.todoapp.mobile.R.string.task_deleted),
                onUndo = { onAction(UiAction.OnUndoDelete) },
            )
        }
    }
}

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d")

@Composable
private fun FilteredTasksTabRow(
    selectedTab: TaskTab,
    onTabSelect: (TaskTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
        modifier
            .background(TDTheme.colors.onBackground.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilteredTasksTab(
            label = stringResource(com.todoapp.mobile.R.string.filtered_tasks_tab_done),
            isSelected = selectedTab == TaskTab.DONE,
            onClick = { onTabSelect(TaskTab.DONE) },
            modifier = Modifier.weight(1f),
        )
        FilteredTasksTab(
            label = stringResource(com.todoapp.mobile.R.string.filtered_tasks_tab_pending),
            isSelected = selectedTab == TaskTab.PENDING,
            onClick = { onTabSelect(TaskTab.PENDING) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FilteredTasksTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) TDTheme.colors.background else Color.Transparent,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
    ) {
        TDText(
            text = label,
            style = TDTheme.typography.subheading4,
            color = if (isSelected) TDTheme.colors.pendingGray else TDTheme.colors.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilteredSwipeDismissBackground(direction: SwipeToDismissBoxValue) {
    val color by animateColorAsState(
        targetValue =
        when (direction) {
            SwipeToDismissBoxValue.EndToStart -> TDTheme.colors.crossRed
            SwipeToDismissBoxValue.StartToEnd -> TDTheme.colors.pendingGray
            else -> Color.Transparent
        },
        label = "swipe_bg",
    )
    val alignment =
        when (direction) {
            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(color, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = alignment,
    ) {
        when (direction) {
            SwipeToDismissBoxValue.EndToStart ->
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    tint = Color.White,
                )
            SwipeToDismissBoxValue.StartToEnd ->
                Icon(
                    painter = painterResource(com.todoapp.mobile.R.drawable.ic_secret_mode),
                    contentDescription = null,
                    tint = Color.White,
                )
            else -> {}
        }
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
private fun FilteredTasksScreenLoadingPreview() {
    TDTheme {
        FilteredTasksScreen(
            uiState = UiState.Loading,
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun FilteredTasksScreenErrorPreview() {
    TDTheme {
        FilteredTasksScreen(
            uiState = UiState.Error("An unexpected error occurred"),
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun FilteredTasksScreenEmptyPreview() {
    TDTheme {
        FilteredTasksScreen(
            uiState =
            UiState.Success(
                tasks = emptyList(),
                selectedTab = TaskTab.PENDING,
                selectedWeekDate = LocalDate.now(),
            ),
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun FilteredTasksScreenSuccessPreview() {
    TDTheme {
        FilteredTasksScreen(
            uiState =
            UiState.Success(
                tasks = dummyTasks,
                selectedTab = TaskTab.PENDING,
                selectedWeekDate = LocalDate.now(),
            ),
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun FilteredTasksScreenDeleteDialogPreview() {
    TDTheme {
        FilteredTasksScreen(
            uiState =
            UiState.Success(
                tasks = dummyTasks,
                selectedTab = TaskTab.PENDING,
                selectedWeekDate = LocalDate.now(),
                isDeleteDialogOpen = true,
            ),
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun FilteredTasksScreenUndoPreview() {
    TDTheme {
        FilteredTasksScreen(
            uiState =
            UiState.Success(
                tasks = dummyTasks,
                selectedTab = TaskTab.PENDING,
                selectedWeekDate = LocalDate.now(),
                pendingDeleteTask = dummyTasks[0],
            ),
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}

private val dummyTasks =
    listOf(
        Task(
            id = 1,
            title = "Buy groceries",
            description = "Milk, eggs, bread",
            date = LocalDate.now(),
            timeStart = LocalTime.of(10, 0),
            timeEnd = LocalTime.of(11, 0),
            isCompleted = false,
            isSecret = false,
        ),
        Task(
            id = 2,
            title = "Gym session",
            description = "Leg day",
            date = LocalDate.now(),
            timeStart = LocalTime.of(18, 0),
            timeEnd = LocalTime.of(19, 30),
            isCompleted = true,
            isSecret = false,
        ),
        Task(
            id = 3,
            title = "Secret meeting",
            description = "Classified",
            date = LocalDate.now().plusDays(1),
            timeStart = LocalTime.of(14, 0),
            timeEnd = LocalTime.of(15, 0),
            isCompleted = false,
            isSecret = true,
        ),
    )
