package com.todoapp.mobile.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.mobile.common.move
import com.todoapp.mobile.ui.common.components.OverdueBanner
import com.todoapp.mobile.ui.home.HomeContract.UiAction
import com.todoapp.mobile.ui.home.HomeContract.UiState
import com.todoapp.uikit.components.TDMonthlyDatePicker
import com.todoapp.uikit.components.TDStatisticCard
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.components.TDUndoSnackbar
import com.todoapp.uikit.extensions.ObscuredTouchGuard
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDate
import java.time.YearMonth

@Suppress("CyclomaticComplexMethod", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    modifier: Modifier,
    uiState: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val dateStripListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isTodayVisible by remember(uiState.displayedMonth) {
        derivedStateOf {
            val today = LocalDate.now()
            if (uiState.displayedMonth != YearMonth.from(today)) {
                false
            } else {
                val todayIndex = today.dayOfMonth - 1
                dateStripListState.layoutInfo.visibleItemsInfo.any { it.index == todayIndex }
            }
        }
    }

    var localTasks by remember { mutableStateOf(uiState.tasks) }
    var dragOriginalIndex by remember { mutableIntStateOf(-1) }
    var dragFinalIndex by remember { mutableIntStateOf(-1) }

    val reorderableLazyListState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            val fromIndex = localTasks.indexOfFirst { it.id == from.key }
            val toIndex = localTasks.indexOfFirst { it.id == to.key }
            if (fromIndex < 0 || toIndex < 0) return@rememberReorderableLazyListState
            if (dragOriginalIndex == -1) dragOriginalIndex = fromIndex
            dragFinalIndex = toIndex
            val list = localTasks.toMutableList()
            list.move(fromIndex, toIndex)
            localTasks = list
            hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }

    // Only re-sync localTasks from upstream when no drag is in flight. Adopt the new list when
    // the IDs differ (real insert/delete from server) OR when same IDs but content differs
    // (completion flip etc.). The naive overwrite during a gesture's transition window
    // crashes the reorderable library because its internal key map references items the
    // overwrite removed.
    LaunchedEffect(uiState.tasks) {
        if (reorderableLazyListState.isAnyItemDragging) return@LaunchedEffect
        val sameIds = uiState.tasks.size == localTasks.size &&
            uiState.tasks.zip(localTasks).all { (a, b) -> a.id == b.id }
        if (!sameIds || uiState.tasks != localTasks) {
            localTasks = uiState.tasks
        }
    }
    var showHint by remember { mutableStateOf(false) }
    var currentHintRes by remember { mutableIntStateOf(com.todoapp.mobile.R.string.hint_swipe_left_delete) }
    val hints =
        remember {
            listOf(
                com.todoapp.mobile.R.string.hint_swipe_left_delete,
                com.todoapp.mobile.R.string.hint_swipe_right_secret,
                com.todoapp.mobile.R.string.hint_long_press_reorder,
                com.todoapp.mobile.R.string.hint_add_task,
                com.todoapp.mobile.R.string.hint_secret_on_create,
                com.todoapp.mobile.R.string.hint_calendar_range,
                com.todoapp.mobile.R.string.hint_activity_stats,
                com.todoapp.mobile.R.string.hint_task_detail,
                com.todoapp.mobile.R.string.hint_daily_plan,
                com.todoapp.mobile.R.string.hint_groups,
                com.todoapp.mobile.R.string.hint_theme,
                com.todoapp.mobile.R.string.hint_pomodoro,
                com.todoapp.mobile.R.string.hint_weekly_picker,
                com.todoapp.mobile.R.string.hint_weekly_stats,
                com.todoapp.mobile.R.string.hint_group_reorder,
                com.todoapp.mobile.R.string.hint_task_photos,
                com.todoapp.mobile.R.string.hint_group_task_flip,
                com.todoapp.mobile.R.string.hint_group_task_assign_swipe,
                com.todoapp.mobile.R.string.hint_group_task_priority,
                com.todoapp.mobile.R.string.hint_profile_avatar,
                com.todoapp.mobile.R.string.hint_group_avatar,
                com.todoapp.mobile.R.string.hint_pomodoro_banner,
                com.todoapp.mobile.R.string.hint_transfer_ownership,
                com.todoapp.mobile.R.string.hint_search_across_groups,
                com.todoapp.mobile.R.string.hint_group_task_edit,
                com.todoapp.mobile.R.string.hint_undo_delete,
                com.todoapp.mobile.R.string.hint_language_switch,
            )
        }
    val pullState = rememberPullToRefreshState()
    LaunchedEffect(showHint) {
        if (showHint) {
            delay(4000L)
            showHint = false
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = {
            currentHintRes = hints.random()
            showHint = true
            onAction(UiAction.OnPullToRefresh)
        },
        state = pullState,
        indicator = {
            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (pullState.distanceFraction > 0f) {
                    Row(
                        modifier =
                        Modifier
                            .background(
                                color =
                                TDTheme.colors.pendingGray.copy(
                                    alpha = (pullState.distanceFraction * 1.5f).coerceIn(0.4f, 1f),
                                ),
                                shape = RoundedCornerShape(50.dp),
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        TDText(
                            text =
                            stringResource(
                                if (pullState.distanceFraction >= 1f) {
                                    com.todoapp.mobile.R.string.hint_release_indicator
                                } else {
                                    com.todoapp.mobile.R.string.hint_pull_indicator
                                },
                            ),
                            style = TDTheme.typography.subheading4,
                            color = Color.White,
                        )
                    }
                }
            }
        },
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            val emptyTitleRes = when (uiState.selectedFilter) {
                HomeContract.HomeFilter.TODAY -> com.todoapp.mobile.R.string.no_tasks_today
                HomeContract.HomeFilter.DAILY -> com.todoapp.mobile.R.string.no_recurring_daily_title
                HomeContract.HomeFilter.WEEKLY -> com.todoapp.mobile.R.string.no_recurring_weekly_title
                HomeContract.HomeFilter.MONTHLY -> com.todoapp.mobile.R.string.no_recurring_monthly_title
                HomeContract.HomeFilter.YEARLY -> com.todoapp.mobile.R.string.no_recurring_yearly_title
            }
            val emptyDescriptionRes = if (uiState.selectedFilter == HomeContract.HomeFilter.TODAY) {
                com.todoapp.mobile.R.string.no_tasks_today_description
            } else {
                com.todoapp.mobile.R.string.no_recurring_description
            }
            val isDark = TDTheme.isDark
            val emptyImageRes: Int? = when (uiState.selectedFilter) {
                HomeContract.HomeFilter.TODAY -> null
                HomeContract.HomeFilter.DAILY ->
                    if (isDark) R.drawable.img_donebot_recurring_daily_night
                    else R.drawable.img_donebot_recurring_daily_day

                HomeContract.HomeFilter.WEEKLY ->
                    if (isDark) R.drawable.img_donebot_recurring_weekly_night
                    else R.drawable.img_donebot_recurring_weekly_day

                HomeContract.HomeFilter.MONTHLY ->
                    if (isDark) R.drawable.img_donebot_recurring_monthly_night
                    else R.drawable.img_donebot_recurring_monthly_day

                HomeContract.HomeFilter.YEARLY ->
                    if (isDark) R.drawable.img_donebot_recurring_yearly_night
                    else R.drawable.img_donebot_recurring_yearly_day
            }
            val visibleTasks =
                remember(localTasks, uiState.pendingDeleteTask?.id) {
                    localTasks.filter { it.id != uiState.pendingDeleteTask?.id }
                }
            HomeTaskList(
                tasks = visibleTasks,
                isSignedIn = uiState.isSignedIn,
                lazyListState = lazyListState,
                reorderableLazyListState = reorderableLazyListState,
                hapticFeedback = hapticFeedback,
                onTaskCheck = { onAction(UiAction.OnTaskCheck(it)) },
                onTaskClick = { onAction(UiAction.OnTaskClick(it)) },
                onTaskLongPress = { onAction(UiAction.OnTaskLongPress(it)) },
                onToggleTaskSecret = { onAction(UiAction.OnToggleTaskSecret(it)) },
                onMoveTask = { from, to -> onAction(UiAction.OnMoveTask(from, to)) },
                onReorderFinished = {
                    val orig = dragOriginalIndex
                    val final = dragFinalIndex
                    dragOriginalIndex = -1
                    dragFinalIndex = -1
                    if (orig != -1 && final != -1 && orig != final) {
                        onAction(UiAction.OnMoveTask(orig, final))
                    }
                },
                modifier = Modifier.fillMaxSize(),
                expandedStagedTaskId = uiState.expandedStagedTaskId,
                expandedSubtasks = uiState.expandedSubtasks,
                onStagedExpandToggle = { onAction(UiAction.OnStagedExpandToggle(it)) },
                onSubtaskToggle = { id, done -> onAction(UiAction.OnSubtaskToggle(id, done)) },
                emptyTitleRes = emptyTitleRes,
                emptyDescriptionRes = emptyDescriptionRes,
                emptyImageRes = emptyImageRes,
                headerContent = {
                    item {
                        HomeGreetingRow(
                            displayName = uiState.displayName,
                            dayMode = uiState.dayMode,
                            currentTimeFormatted = uiState.currentTimeFormatted,
                        )
                    }
                    val totalTasksToday = uiState.tasks.size
                    val isMorningMode =
                        uiState.dayMode == com.todoapp.mobile.domain.model.DayMode.MORNING
                    val isEndOfDayMoment = uiState.isEndOfDayMoment
                    val showSuggest =
                        uiState.selectedFilter == HomeContract.HomeFilter.TODAY &&
                            !uiState.isSuggestCardDismissedToday &&
                            (isMorningMode || (isEndOfDayMoment && totalTasksToday > 0))
                    if (showSuggest) {
                        item {
                            HomeSuggestCard(
                                dayMode = uiState.dayMode,
                                yesterdayCompleted = uiState.yesterdayCompletedCount,
                                pendingCount = uiState.tasks.count { !it.isCompleted },
                                completedCount = uiState.tasks.count { it.isCompleted },
                                onPrimary = { onAction(UiAction.OnSuggestCardPrimaryAction) },
                                onSecondary = { onAction(UiAction.OnSuggestCardSecondaryAction) },
                                onDismiss = { onAction(UiAction.OnSuggestCardDismiss) },
                            )
                        }
                        item { Spacer(Modifier.height(12.dp)) }
                    }
                    if (uiState.pendingPermissions.isNotEmpty()) {
                        item {
                            HomePermissionPrompts(
                                permissions = uiState.pendingPermissions,
                                onAction = onAction,
                            )
                        }
                    }
                    item { HomeHintCard(showHint = showHint, hintRes = currentHintRes) }
                    item {
                        TDMonthlyDatePicker(
                            modifier = Modifier.fillMaxWidth(),
                            displayedMonth = uiState.displayedMonth,
                            selectedDate = uiState.selectedDate,
                            taskDates = uiState.taskDatesInMonth,
                            overdueDates = uiState.overdueDates,
                            hasOverdueBeforeDisplayedMonth = uiState.hasOverdueBeforeDisplayedMonth,
                            listState = dateStripListState,
                            onDateSelect = { onAction(UiAction.OnDateSelect(it)) },
                            onPreviousMonth = { onAction(UiAction.OnPreviousMonth) },
                            onNextMonth = { onAction(UiAction.OnNextMonth) },
                        )
                    }
                    item {
                        AnimatedVisibility(
                            visible = !isTodayVisible,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            HomeBackToTodayPill(
                                onClick = {
                                    val today = LocalDate.now()
                                    val alreadyOnToday = uiState.selectedDate == today &&
                                        uiState.displayedMonth == YearMonth.from(today)
                                    onAction(UiAction.OnDateSelect(today))
                                    if (alreadyOnToday) {
                                        coroutineScope.launch {
                                            dateStripListState.animateScrollToItem(
                                                maxOf(0, today.dayOfMonth - 4),
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                    if (uiState.hasOverdueBeforeDisplayedMonth && uiState.overdueCount > 0) {
                        item {
                            OverdueBanner(
                                count = uiState.overdueCount,
                                onView = { onAction(UiAction.OnJumpToEarliestOverdue) },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                    item {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TDStatisticCard(
                                text = stringResource(com.todoapp.mobile.R.string.task_complete),
                                taskAmount = uiState.completedTaskCountThisWeek,
                                modifier = Modifier.weight(1f),
                                isCompleted = true,
                                onClick = { onAction(UiAction.OnCompletedStatCardTap) },
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            TDStatisticCard(
                                text = stringResource(com.todoapp.mobile.R.string.task_pending),
                                taskAmount = uiState.pendingTaskCountThisWeek,
                                modifier = Modifier.weight(1f),
                                isCompleted = false,
                                onClick = { onAction(UiAction.OnPendingStatCardTap) },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                    item {
                        HomeSectionTabRow(
                            isRecurring = uiState.selectedFilter != HomeContract.HomeFilter.TODAY,
                            selectedDate = uiState.selectedDate,
                            onSelectToday = {
                                onAction(UiAction.OnFilterChange(HomeContract.HomeFilter.TODAY))
                            },
                            onSelectRecurring = {
                                onAction(UiAction.OnFilterChange(uiState.lastRecurringFilter))
                            },
                        )
                    }
                    item {
                        AnimatedVisibility(
                            visible = uiState.selectedFilter != HomeContract.HomeFilter.TODAY,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Column {
                                Spacer(Modifier.height(12.dp))
                                HomeRecurringChipRow(
                                    selected = uiState.selectedFilter,
                                    onSelected = { onAction(UiAction.OnFilterChange(it)) },
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                },
            )
            HomeFabMenu(
                onCreate = { onAction(UiAction.OnCreateHubTap) },
            )
            Box(
                modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                AnimatedVisibility(
                    visible = uiState.pendingDeleteTask != null,
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
        if (uiState.isDeleteDialogOpen) {
            AlertDialog(
                onDismissRequest = { onAction(UiAction.OnDeleteDialogDismiss) },
                title = { Text(stringResource(com.todoapp.mobile.R.string.delete_task_title)) },
                titleContentColor = TDTheme.colors.onBackground,
                containerColor = TDTheme.colors.background,
                textContentColor = TDTheme.colors.gray,
                text = {
                    ObscuredTouchGuard()
                    Text(stringResource(com.todoapp.mobile.R.string.delete_task_message))
                },
                confirmButton = {
                    TextButton(onClick = { onAction(UiAction.OnDeleteDialogConfirm) }) {
                        Text(
                            text = stringResource(com.todoapp.mobile.R.string.delete),
                            color = TDTheme.colors.crossRed,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(UiAction.OnDeleteDialogDismiss) }) {
                        Text(
                            text = stringResource(com.todoapp.mobile.R.string.cancel),
                            color = TDTheme.colors.gray,
                        )
                    }
                },
            )
        }
        if (uiState.isFinishRoutineDialogOpen) {
            AlertDialog(
                onDismissRequest = { onAction(UiAction.OnFinishRoutineDialogDismiss) },
                title = { Text(stringResource(com.todoapp.mobile.R.string.finish_routine_dialog_title)) },
                titleContentColor = TDTheme.colors.onBackground,
                containerColor = TDTheme.colors.background,
                textContentColor = TDTheme.colors.gray,
                text = { Text(stringResource(com.todoapp.mobile.R.string.finish_routine_dialog_message)) },
                confirmButton = {
                    TextButton(onClick = { onAction(UiAction.OnFinishRoutineDialogConfirm) }) {
                        Text(
                            text = stringResource(com.todoapp.mobile.R.string.finish_routine_confirm),
                            color = TDTheme.colors.darkPending,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(UiAction.OnFinishRoutineDialogDismiss) }) {
                        Text(
                            text = stringResource(com.todoapp.mobile.R.string.cancel),
                            color = TDTheme.colors.gray,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun HomeGreetingRow(
    displayName: String,
    dayMode: com.todoapp.mobile.domain.model.DayMode,
    currentTimeFormatted: String,
) {
    val name = displayName.trim().substringBefore(' ').takeIf { it.isNotBlank() }
    val greetingText = when (dayMode) {
        com.todoapp.mobile.domain.model.DayMode.MORNING ->
            if (name != null) {
                stringResource(com.todoapp.mobile.R.string.home_greeting_morning_format, name)
            } else {
                stringResource(com.todoapp.mobile.R.string.home_greeting_morning_no_name)
            }

        com.todoapp.mobile.domain.model.DayMode.MIDDAY ->
            if (name != null) {
                stringResource(com.todoapp.mobile.R.string.home_greeting_midday_format, name)
            } else {
                stringResource(com.todoapp.mobile.R.string.home_greeting_midday_no_name)
            }

        com.todoapp.mobile.domain.model.DayMode.EVENING ->
            if (name != null) {
                stringResource(com.todoapp.mobile.R.string.home_greeting_evening_format, name)
            } else {
                stringResource(com.todoapp.mobile.R.string.home_greeting_evening_no_name)
            }

        com.todoapp.mobile.domain.model.DayMode.NIGHT ->
            if (name != null) {
                stringResource(com.todoapp.mobile.R.string.home_greeting_night_format, name)
            } else {
                stringResource(com.todoapp.mobile.R.string.home_greeting_night_no_name)
            }
    }
    val (iconRes, iconCdRes, iconTint) = when (dayMode) {
        com.todoapp.mobile.domain.model.DayMode.MORNING -> Triple(
            R.drawable.ic_sun,
            com.todoapp.mobile.R.string.home_greeting_icon_morning_cd,
            TDTheme.colors.orange,
        )

        com.todoapp.mobile.domain.model.DayMode.MIDDAY -> Triple(
            R.drawable.ic_sun,
            com.todoapp.mobile.R.string.home_greeting_icon_midday_cd,
            TDTheme.colors.orange,
        )

        com.todoapp.mobile.domain.model.DayMode.EVENING -> Triple(
            R.drawable.ic_moon,
            com.todoapp.mobile.R.string.home_greeting_icon_evening_cd,
            TDTheme.colors.orange,
        )

        com.todoapp.mobile.domain.model.DayMode.NIGHT -> Triple(
            R.drawable.ic_moon,
            com.todoapp.mobile.R.string.home_greeting_icon_night_cd,
            TDTheme.colors.orange,
        )
    }
    val iconCd = stringResource(iconCdRes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$iconCd, $greetingText, $currentTimeFormatted"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(8.dp))
        TDText(
            text = greetingText,
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.onBackground.copy(alpha = 0.85f),
        )
        if (currentTimeFormatted.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            TDText(
                text = "· $currentTimeFormatted",
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.onBackground.copy(alpha = 0.55f),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun HomeSuggestCard(
    dayMode: com.todoapp.mobile.domain.model.DayMode,
    yesterdayCompleted: Int,
    pendingCount: Int,
    completedCount: Int,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isMorning = dayMode == com.todoapp.mobile.domain.model.DayMode.MORNING
    val isEveningAllDone = !isMorning && pendingCount == 0 && completedCount > 0
    val avatarRes = if (isMorning || isEveningAllDone) {
        if (TDTheme.isDark) R.drawable.img_donebot_plan_your_day_light else R.drawable.img_donebot_plan_your_day_dark
    } else {
        if (TDTheme.isDark) R.drawable.img_donebot_alarm_reminder_light else R.drawable.img_donebot_alarm_reminder_dark
    }
    val title = stringResource(
        when {
            isMorning -> com.todoapp.mobile.R.string.donebot_suggest_morning_title
            isEveningAllDone -> com.todoapp.mobile.R.string.donebot_suggest_evening_all_done_title
            else -> com.todoapp.mobile.R.string.donebot_suggest_evening_title
        },
    )
    val body = when {
        isMorning ->
            if (yesterdayCompleted == 0) {
                stringResource(com.todoapp.mobile.R.string.donebot_suggest_morning_body_zero)
            } else {
                stringResource(com.todoapp.mobile.R.string.donebot_suggest_morning_body_format, yesterdayCompleted)
            }

        isEveningAllDone ->
            stringResource(com.todoapp.mobile.R.string.donebot_suggest_evening_all_done_body_format, completedCount)

        else -> {
            val total = pendingCount + completedCount
            if (completedCount == 0) {
                stringResource(
                    com.todoapp.mobile.R.string.donebot_suggest_evening_body_none_done_format,
                    pendingCount,
                )
            } else {
                stringResource(
                    com.todoapp.mobile.R.string.donebot_suggest_evening_body_format,
                    completedCount,
                    total,
                    pendingCount,
                )
            }
        }
    }
    val primaryCta = stringResource(
        when {
            isMorning -> com.todoapp.mobile.R.string.donebot_suggest_morning_cta
            isEveningAllDone -> com.todoapp.mobile.R.string.donebot_suggest_evening_all_done_cta
            else -> com.todoapp.mobile.R.string.donebot_suggest_evening_cta_primary
        },
    )
    val secondaryCta =
        if (isMorning || isEveningAllDone) {
            null
        } else {
            stringResource(com.todoapp.mobile.R.string.donebot_suggest_evening_cta_secondary)
        }
    val onSecondaryNullable: (() -> Unit)? =
        if (isMorning || isEveningAllDone) null else onSecondary
    val onPrimaryAdjusted: () -> Unit = if (isEveningAllDone) onDismiss else onPrimary
    com.todoapp.uikit.components.TDDoneBotSuggestCard(
        avatarRes = avatarRes,
        title = title,
        body = body,
        primaryCtaText = primaryCta,
        onPrimary = onPrimaryAdjusted,
        onDismiss = onDismiss,
        dismissContentDescription = stringResource(com.todoapp.mobile.R.string.donebot_suggest_dismiss_cd),
        secondaryCtaText = secondaryCta,
        onSecondary = onSecondaryNullable,
    )
}

@Composable
private fun HomeHintCard(
    showHint: Boolean,
    hintRes: Int,
) {
    AnimatedVisibility(
        visible = showHint,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .background(TDTheme.colors.infoCardBgColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = TDTheme.colors.pendingGray,
                modifier =
                Modifier
                    .size(20.dp)
                    .padding(top = 2.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                TDText(
                    text = stringResource(com.todoapp.mobile.R.string.hint_tip_label),
                    style = TDTheme.typography.subheading4,
                    color = TDTheme.colors.pendingGray,
                )
                Spacer(modifier = Modifier.height(4.dp))
                TDText(
                    text = stringResource(hintRes),
                    style = TDTheme.typography.subheading4,
                    color = TDTheme.colors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun HomeBackToTodayPill(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = TDTheme.colors.bgColorPurple,
                    shape = RoundedCornerShape(20.dp),
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(com.todoapp.mobile.R.drawable.ic_calendar),
                contentDescription = null,
                tint = TDTheme.colors.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            TDText(
                text = stringResource(com.todoapp.mobile.R.string.home_back_to_today),
                style = TDTheme.typography.subheading4,
                color = TDTheme.colors.primary,
            )
        }
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentPreview() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = HomePreviewData.sampleTasks,
                completedTaskCountThisWeek = 5,
                pendingTaskCountThisWeek = 8,
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentPreview_no_task() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = emptyList(),
                completedTaskCountThisWeek = 0,
                pendingTaskCountThisWeek = 0,
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentPreview_no_task_dark() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = emptyList(),
                completedTaskCountThisWeek = 0,
                pendingTaskCountThisWeek = 0,
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentPreview_Dark() {
    TDTheme(darkTheme = true) {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = HomePreviewData.sampleTasks,
                completedTaskCountThisWeek = 5,
                pendingTaskCountThisWeek = 8,
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentSheetOpenPreview() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = HomePreviewData.sampleTasks,
                completedTaskCountThisWeek = 5,
                pendingTaskCountThisWeek = 8,
                isSheetOpen = true,
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentDeleteDialogPreview() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = HomePreviewData.sampleTasks,
                completedTaskCountThisWeek = 5,
                pendingTaskCountThisWeek = 8,
                isDeleteDialogOpen = true,
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentSecretModeOffPreview() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = HomePreviewData.sampleTasks,
                completedTaskCountThisWeek = 5,
                pendingTaskCountThisWeek = 8,
                isSecretModeEnabled = false,
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentValidationErrorPreview() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = HomePreviewData.sampleTasks,
                completedTaskCountThisWeek = 5,
                pendingTaskCountThisWeek = 8,
                isSheetOpen = true,
                titleErrorRes = com.todoapp.mobile.R.string.error_task_title_required,
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentMorningSuggestPreview() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = HomePreviewData.sampleTasks,
                completedTaskCountThisWeek = 5,
                pendingTaskCountThisWeek = 8,
                displayName = "Berat",
                dayMode = com.todoapp.mobile.domain.model.DayMode.MORNING,
                yesterdayCompletedCount = 4,
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentMorningSuggestZeroPreview() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = HomePreviewData.sampleTasks,
                completedTaskCountThisWeek = 5,
                pendingTaskCountThisWeek = 8,
                displayName = "Berat",
                dayMode = com.todoapp.mobile.domain.model.DayMode.MORNING,
                yesterdayCompletedCount = 0,
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentEveningSuggestPreview() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = HomePreviewData.sampleTasks,
                completedTaskCountThisWeek = 5,
                pendingTaskCountThisWeek = 8,
                displayName = "Berat",
                dayMode = com.todoapp.mobile.domain.model.DayMode.EVENING,
                isEndOfDayMoment = true,
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentEveningSuggestNoneDonePreview() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = HomePreviewData.sampleTasks.map { it.copy(isCompleted = false) },
                completedTaskCountThisWeek = 0,
                pendingTaskCountThisWeek = 8,
                displayName = "Berat",
                dayMode = com.todoapp.mobile.domain.model.DayMode.EVENING,
                isEndOfDayMoment = true,
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentNightSuggestPreview() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = HomePreviewData.sampleTasks,
                completedTaskCountThisWeek = 5,
                pendingTaskCountThisWeek = 8,
                displayName = "Berat",
                dayMode = com.todoapp.mobile.domain.model.DayMode.NIGHT,
                currentTimeFormatted = "23:15",
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentEveningAllDonePreview() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = HomePreviewData.sampleTasks.map { it.copy(isCompleted = true) },
                completedTaskCountThisWeek = 8,
                pendingTaskCountThisWeek = 0,
                displayName = "Berat",
                dayMode = com.todoapp.mobile.domain.model.DayMode.EVENING,
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentRecurringPreview() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = HomePreviewData.sampleRoutines,
                completedTaskCountThisWeek = 5,
                pendingTaskCountThisWeek = 8,
                selectedFilter = HomeContract.HomeFilter.DAILY,
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentFinishRoutineDialogPreview() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = HomePreviewData.sampleRoutines,
                completedTaskCountThisWeek = 3,
                pendingTaskCountThisWeek = 4,
                selectedFilter = HomeContract.HomeFilter.DAILY,
                isFinishRoutineDialogOpen = true,
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentRecurringNoTasksPreview() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = emptyList(),
                completedTaskCountThisWeek = 0,
                pendingTaskCountThisWeek = 0,
                selectedFilter = HomeContract.HomeFilter.WEEKLY,
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun HomeContentBackToTodayVisiblePreview() {
    TDTheme {
        HomeContent(
            uiState =
            HomePreviewData.successState(
                tasks = HomePreviewData.sampleTasks,
                completedTaskCountThisWeek = 5,
                pendingTaskCountThisWeek = 8,
                displayedMonth = YearMonth.now().plusMonths(1),
                selectedDate = YearMonth.now().plusMonths(1).atDay(1),
            ),
            onAction = {},
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}
