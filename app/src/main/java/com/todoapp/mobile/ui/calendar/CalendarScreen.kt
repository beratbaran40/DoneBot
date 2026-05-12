package com.todoapp.mobile.ui.calendar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.uikit.R
import com.todoapp.mobile.common.DeadlineStatus
import com.todoapp.mobile.common.rememberDeadlineDisplay
import com.todoapp.mobile.ui.calendar.CalendarContract.GroupTaskCalendarItem
import com.todoapp.mobile.ui.calendar.CalendarContract.PersonalTaskCalendarItem
import com.todoapp.mobile.ui.calendar.CalendarContract.UiAction
import com.todoapp.mobile.ui.calendar.CalendarContract.UiEffect
import com.todoapp.mobile.ui.calendar.CalendarContract.UiState
import com.todoapp.mobile.ui.common.components.OverdueBanner
import com.todoapp.mobile.ui.home.AddTaskSheet
import com.todoapp.mobile.ui.home.HomeFabMenu
import com.todoapp.mobile.ui.home.TaskFormUiAction
import com.todoapp.mobile.ui.security.biometric.BiometricAuthenticator
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDDatePicker
import com.todoapp.uikit.components.TDFullscreenImageViewer
import com.todoapp.uikit.components.TDGroupTaskCard
import com.todoapp.uikit.components.TDLoadingBar
import com.todoapp.uikit.components.TDScreenWithSheet
import com.todoapp.uikit.components.TDStatusChipTone
import com.todoapp.uikit.components.TDTaskCard
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.previews.TDPreviewWide
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Composable
fun CalendarScreen(
    uiState: UiState,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    val context = LocalContext.current

    uiEffect.collectWithLifecycle {
        when (it) {
            is UiEffect.ShowBiometricAuthenticator -> {
                val activity = context as? FragmentActivity ?: return@collectWithLifecycle
                val isAuthenticated = BiometricAuthenticator.authenticate(activity)
                if (isAuthenticated) onAction(UiAction.OnSuccessfulBiometricAuthenticationHandle)
            }
        }
    }

    when (uiState) {
        is UiState.Loading -> CalendarLoadingContent()
        is UiState.Error -> CalendarErrorContent(message = uiState.message, onAction = onAction)
        is UiState.Success -> CalendarSuccessContent(uiState = uiState, onAction = onAction)
    }
}

@Composable
private fun CalendarLoadingContent() {
    TDLoadingBar()
}

@Composable
private fun CalendarErrorContent(
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
        Spacer(Modifier.height(24.dp))
        TDButton(
            text = stringResource(com.todoapp.mobile.R.string.retry),
            onClick = { onAction(UiAction.OnRetry) },
            size = TDButtonSize.SMALL,
        )
    }
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun CalendarSuccessContent(
    uiState: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    TDScreenWithSheet(
        isSheetOpen = uiState.isSheetOpen,
        sheetContent = {
            AddTaskSheet(
                formState = uiState.taskFormState,
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
                        else -> Unit
                    }
                },
            )
        },
        onDismissSheet = { onAction(UiAction.OnDismissBottomSheet) },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier =
                Modifier
                    .fillMaxSize()
                    .background(color = TDTheme.colors.background),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (uiState.hasOverdueBeforeDisplayedMonth && uiState.overdueCount > 0) {
                    item {
                        OverdueBanner(
                            count = uiState.overdueCount,
                            onView = { onAction(UiAction.OnJumpToEarliestOverdue) },
                        )
                    }
                }
                item {
                    TDDatePicker(
                        selectedDate = uiState.selectedDate,
                        selectedMonth = uiState.selectedMonth,
                        onMonthForward = { onAction(UiAction.OnMonthForward) },
                        onMonthBack = { onAction(UiAction.OnMonthBack) },
                        taskDates = uiState.taskDatesInMonth,
                        overdueDates = uiState.overdueDates,
                        hasOverdueBeforeDisplayedMonth = uiState.hasOverdueBeforeDisplayedMonth,
                        onDaySelect = { onAction(UiAction.OnDateSelect(it)) },
                        onDayDeselect = { onAction(UiAction.OnDateDeselect) },
                    )
                }
                val (recurringItems, oneOffPersonalItems) = uiState.personalTaskItems
                    .partition { it.isRecurringInstance }
                val hasOneOffPersonal = oneOffPersonalItems.isNotEmpty()
                val hasRecurring = recurringItems.isNotEmpty()
                val hasGroup = uiState.groupTaskItems.isNotEmpty()
                val noResults = !hasOneOffPersonal && !hasRecurring && !hasGroup
                if (uiState.selectedDate != null && noResults) {
                    item { CalendarEmptyState() }
                } else {
                    if (hasOneOffPersonal) {
                        item(key = "header-personal") {
                            SectionHeader(
                                text = stringResource(com.todoapp.mobile.R.string.calendar_section_my_tasks),
                            )
                        }
                        items(
                            items = oneOffPersonalItems,
                            key = { "personal-${it.taskId}" },
                        ) { personalItem ->
                            PersonalTaskEntry(
                                item = personalItem,
                                onClick = { onAction(UiAction.OnTaskClick(it)) },
                                onPhotoClick = { onAction(UiAction.OnGroupTaskPhotoOpen(it)) },
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                        }
                    }
                    if (hasRecurring) {
                        item(key = "header-recurring") {
                            SectionHeader(
                                text = stringResource(com.todoapp.mobile.R.string.calendar_section_recurring_tasks),
                            )
                        }
                        items(
                            items = recurringItems,
                            key = { "recurring-${it.taskId}" },
                        ) { recurringItem ->
                            PersonalTaskEntry(
                                item = recurringItem,
                                onClick = { onAction(UiAction.OnTaskClick(it)) },
                                onPhotoClick = { onAction(UiAction.OnGroupTaskPhotoOpen(it)) },
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                        }
                    }
                    if (hasGroup) {
                        item(key = "header-group") {
                            SectionHeader(
                                text = stringResource(com.todoapp.mobile.R.string.calendar_section_group_deadlines),
                            )
                        }
                        items(
                            items = uiState.groupTaskItems,
                            key = { "group-${it.taskId}" },
                        ) { groupItem ->
                            GroupTaskEntry(
                                item = groupItem,
                                onPhotoClick = { onAction(UiAction.OnGroupTaskPhotoOpen(it)) },
                                onCardClick = { groupId, taskId ->
                                    onAction(UiAction.OnGroupTaskClick(groupId, taskId))
                                },
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                        }
                    }
                }
            }
            HomeFabMenu(
                onAddTask = { onAction(UiAction.OnShowBottomSheet) },
                onPomodoro = { onAction(UiAction.OnPomodoroTap) },
                onJournal = { onAction(UiAction.OnJournalTap) },
            )
            val viewerUrl = uiState.viewerPhotoUrl
            if (!viewerUrl.isNullOrBlank()) {
                TDFullscreenImageViewer(
                    model = viewerUrl,
                    onDismiss = { onAction(UiAction.OnGroupTaskPhotoDismiss) },
                )
            }
        }
    }
}

@Composable
private fun GroupTaskEntry(
    item: GroupTaskCalendarItem,
    onPhotoClick: (String) -> Unit,
    onCardClick: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val deadline = rememberDeadlineDisplay(
        dueAtEpochMs = item.dueAtEpochMs,
        isCompleted = item.isCompleted,
    )
    val unassignedLabel = stringResource(com.todoapp.mobile.R.string.deadline_unassigned)
    TDGroupTaskCard(
        modifier = modifier,
        title = item.title,
        priority = item.priority,
        deadlinePrimary = deadline.primary,
        deadlineSecondary = deadline.secondary,
        deadlineColor = deadlineColorFor(deadline.status),
        statusTone = statusToneFor(deadline.status),
        statusLabel = stringResource(statusLabelResFor(deadline.status)),
        assigneeName = item.assigneeName,
        assigneeAvatarUrl = item.assigneeAvatarUrl,
        assigneeInitials = item.assigneeInitials,
        unassignedLabel = unassignedLabel,
        photoUrl = item.photoUrl,
        isCompleted = item.isCompleted,
        onClick = item.groupId?.let { gid -> { onCardClick(gid, item.taskId) } },
        onPhotoClick = item.photoUrl?.let { url -> { onPhotoClick(url) } },
    )
}

@Composable
private fun PersonalTaskEntry(
    item: PersonalTaskCalendarItem,
    onClick: (Long) -> Unit,
    onPhotoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val deadline = rememberDeadlineDisplay(
        dueAtEpochMs = item.dueAtEpochMs,
        isCompleted = item.isCompleted,
        isRecurringInstance = item.isRecurringInstance,
    )
    val openLocation = com.todoapp.mobile.ui.common.rememberOpenLocation(
        item.locationName, item.locationAddress, item.locationLat, item.locationLng,
    )
    TDTaskCard(
        modifier = modifier,
        taskTitle = item.title,
        description = item.description,
        deadlinePrimary = deadline.primary,
        deadlineSecondary = deadline.secondary,
        deadlineColor = deadlineColorFor(deadline.status),
        statusTone = statusToneFor(deadline.status),
        statusLabel = stringResource(statusLabelResFor(deadline.status)),
        isCompleted = item.isCompleted,
        photoUrl = item.photoUrl,
        onClick = { onClick(item.taskId) },
        onPhotoClick = item.photoUrl?.let { url -> { onPhotoClick(url) } },
        locationLabel = item.locationName,
        onLocationClick = openLocation,
    )
}

@Composable
private fun SectionHeader(text: String) {
    TDText(
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
        text = text,
        style = TDTheme.typography.heading5,
        color = TDTheme.colors.onBackground,
    )
}

@Composable
private fun deadlineColorFor(status: DeadlineStatus) = when (status) {
    DeadlineStatus.Future -> TDTheme.colors.onBackground
    DeadlineStatus.Done -> TDTheme.colors.darkGreen
    DeadlineStatus.StillPending -> TDTheme.colors.crossRed
}

private fun statusToneFor(status: DeadlineStatus): TDStatusChipTone = when (status) {
    DeadlineStatus.Future -> TDStatusChipTone.Neutral
    DeadlineStatus.Done -> TDStatusChipTone.Success
    DeadlineStatus.StillPending -> TDStatusChipTone.Danger
}

private fun statusLabelResFor(status: DeadlineStatus): Int = when (status) {
    DeadlineStatus.Future -> com.todoapp.mobile.R.string.status_pending
    DeadlineStatus.Done -> com.todoapp.mobile.R.string.status_done
    DeadlineStatus.StillPending -> com.todoapp.mobile.R.string.status_overdue
}

@Composable
private fun CalendarEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(
                if (TDTheme.isDark) {
                    com.todoapp.mobile.R.drawable.ic_idle_robot_dark
                } else {
                    com.todoapp.mobile.R.drawable.ic_idle_robot_light
                },
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(160.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = TDTheme.colors.pendingGray.copy(alpha = 0.3f),
                    spotColor = TDTheme.colors.pendingGray.copy(alpha = 0.3f),
                )
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = TDTheme.colors.darkPending.copy(alpha = 0.6f),
                    shape = CircleShape,
                ),
        )
        Spacer(Modifier.height(12.dp))
        TDText(
            text = stringResource(com.todoapp.mobile.R.string.calendar_no_tasks_for_day),
            style = TDTheme.typography.heading3,
            color = TDTheme.colors.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        TDText(
            text = stringResource(com.todoapp.mobile.R.string.calendar_no_tasks_for_day_description),
            modifier = Modifier.padding(horizontal = 48.dp),
            style = TDTheme.typography.heading6,
            color = TDTheme.colors.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
    }
}

@TDPreview
@Composable
private fun CalendarLoadingPreview() {
    TDTheme {
        CalendarLoadingContent()
    }
}

@TDPreview
@Composable
private fun CalendarErrorPreview() {
    TDTheme {
        CalendarErrorContent(
            message = "Something went wrong",
            onAction = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun CalendarSuccessPreview() {
    TDTheme {
        CalendarSuccessContent(
            uiState =
            UiState.Success(
                selectedDate = LocalDate.of(2025, 1, 12),
                personalTaskItems =
                listOf(
                    PersonalTaskCalendarItem(
                        taskId = 1L,
                        title = "Read Book",
                        description = "Chapter 5 of Clean Code",
                        dueAtEpochMs = System.currentTimeMillis() + 3_600_000L,
                        isCompleted = false,
                        photoUrl = null,
                    ),
                    PersonalTaskCalendarItem(
                        taskId = 2L,
                        title = "Gym",
                        description = null,
                        dueAtEpochMs = System.currentTimeMillis() - 3_600_000L,
                        isCompleted = true,
                        photoUrl = null,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun CalendarSuccessEmptyPreview() {
    TDTheme {
        CalendarSuccessContent(
            uiState = UiState.Success(
                selectedDate = LocalDate.of(2025, 1, 12),
                personalTaskItems = emptyList(),
                groupTaskItems = emptyList(),
            ),
            onAction = {},
        )
    }
}

@TDPreviewWide
@Composable
private fun CalendarSuccessWithRecurringPreview() {
    TDTheme {
        CalendarSuccessContent(
            uiState = UiState.Success(
                selectedDate = LocalDate.of(2025, 1, 12),
                personalTaskItems = listOf(
                    PersonalTaskCalendarItem(
                        taskId = 100L,
                        title = "Daily medicine",
                        description = "8am — vitamin D",
                        dueAtEpochMs = System.currentTimeMillis() + 86_400_000L * 3,
                        isCompleted = false,
                        photoUrl = null,
                        isRecurringInstance = true,
                    ),
                    PersonalTaskCalendarItem(
                        taskId = 101L,
                        title = "Weekly review",
                        description = null,
                        dueAtEpochMs = System.currentTimeMillis() + 86_400_000L * 4,
                        isCompleted = false,
                        photoUrl = null,
                        isRecurringInstance = true,
                    ),
                    PersonalTaskCalendarItem(
                        taskId = 200L,
                        title = "Submit report",
                        description = "Quarterly status",
                        dueAtEpochMs = System.currentTimeMillis() + 7_200_000L,
                        isCompleted = false,
                        photoUrl = null,
                        isRecurringInstance = false,
                    ),
                ),
                groupTaskItems = emptyList(),
            ),
            onAction = {},
        )
    }
}
