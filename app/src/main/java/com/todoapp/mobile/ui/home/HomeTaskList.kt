package com.todoapp.mobile.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.mobile.common.maskDescription
import com.todoapp.mobile.common.maskTitle
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.ui.common.rememberOpenLocation
import com.todoapp.uikit.components.TDTaskCardWithCheckbox
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTaskList(
    tasks: List<Task>,
    lazyListState: LazyListState,
    reorderableLazyListState: ReorderableLazyListState,
    hapticFeedback: HapticFeedback,
    onTaskCheck: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onTaskLongPress: (Task) -> Unit,
    onToggleTaskSecret: (Task) -> Unit,
    onMoveTask: (Int, Int) -> Unit,
    onReorderFinished: () -> Unit,
    modifier: Modifier = Modifier,
    emptyTitleRes: Int = com.todoapp.mobile.R.string.no_tasks_today,
    emptyDescriptionRes: Int = com.todoapp.mobile.R.string.no_tasks_today_description,
    emptyImageRes: Int? = null,
    headerContent: LazyListScope.() -> Unit = {},
) {
    val isAnyDragging = reorderableLazyListState.isAnyItemDragging
    val today = java.time.LocalDate.now()
    val overdueLabel = stringResource(com.todoapp.mobile.R.string.status_overdue)
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        headerContent()
        if (tasks.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val defaultIdleImage = if (TDTheme.isDark) {
                        com.todoapp.mobile.R.drawable.ic_idle_robot_dark
                    } else {
                        com.todoapp.mobile.R.drawable.ic_idle_robot_light
                    }
                    Image(
                        painter = painterResource(emptyImageRes ?: defaultIdleImage),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(180.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                ambientColor = TDTheme.colors.purple.copy(alpha = 0.3f),
                                spotColor = TDTheme.colors.purple.copy(alpha = 0.3f),
                            )
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = TDTheme.colors.lightPurple.copy(alpha = 0.6f),
                                shape = CircleShape,
                            ),
                    )
                    Spacer(Modifier.height(12.dp))
                    TDText(
                        text = stringResource(emptyTitleRes),
                        style = TDTheme.typography.heading3,
                        color = TDTheme.colors.onBackground,
                    )
                    Spacer(Modifier.height(8.dp))
                    TDText(
                        text = stringResource(emptyDescriptionRes),
                        modifier = Modifier.padding(horizontal = 80.dp),
                        style = TDTheme.typography.heading6,
                        color = TDTheme.colors.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            itemsIndexed(
                items = tasks,
                key = { _, task -> task.id },
            ) { index, task ->
                val isOverdue = !task.isCompleted &&
                    task.recurrence == Recurrence.NONE &&
                    task.date.isBefore(today)
                ReorderableItem(
                    state = reorderableLazyListState,
                    key = task.id,
                ) { isDragging ->
                    val dismissState = rememberSwipeToDismissBoxState()

                    LaunchedEffect(dismissState.currentValue) {
                        when (dismissState.currentValue) {
                            SwipeToDismissBoxValue.EndToStart -> {
                                onTaskLongPress(task)
                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                            }

                            SwipeToDismissBoxValue.StartToEnd -> {
                                onToggleTaskSecret(task)
                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                            }

                            SwipeToDismissBoxValue.Settled -> {}
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            HomeSwipeDismissBackground(direction = dismissState.dismissDirection)
                        },
                    ) {
                        Card(
                            modifier =
                            Modifier
                                .longPressDraggableHandle(
                                    onDragStarted = {
                                        hapticFeedback.performHapticFeedback(
                                            HapticFeedbackType.GestureThresholdActivate,
                                        )
                                    },
                                    onDragStopped = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                        onReorderFinished()
                                    },
                                )
                                .clickable { onTaskClick(task) }
                                .semantics {
                                    customActions =
                                        listOf(
                                            CustomAccessibilityAction(
                                                label = "Move Up",
                                                action = {
                                                    if (index > 0) {
                                                        onMoveTask(index, index - 1)
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                },
                                            ),
                                            CustomAccessibilityAction(
                                                label = "Move Down",
                                                action = {
                                                    if (index < tasks.lastIndex) {
                                                        onMoveTask(index, index + 1)
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                },
                                            ),
                                        )
                                },
                        ) {
                            val firstPhoto = task.photoUrls.firstOrNull()
                            if (firstPhoto != null) {
                                Column(
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(
                                            androidx.compose.foundation.shape
                                                .RoundedCornerShape(12.dp),
                                        )
                                        .background(TDTheme.colors.lightPending),
                                ) {
                                    SecretOrNormalPhotoBanner(
                                        url =
                                        run {
                                            val base =
                                                com.todoapp.mobile.BuildConfig.BASE_URL
                                                    .trimEnd('/')
                                            "$base/${firstPhoto.trimStart('/')}"
                                        },
                                        isSecret = task.isSecret,
                                    )
                                    val openLocation = rememberOpenLocation(
                                        task.locationName,
                                        task.locationAddress,
                                        task.locationLat,
                                        task.locationLng,
                                    )
                                    TDTaskCardWithCheckbox(
                                        taskText = if (task.isSecret) task.title.maskTitle() else task.title,
                                        taskDescription = if (task.isSecret) task.description?.maskDescription() else task.description,
                                        isChecked = task.isCompleted,
                                        onCheckBoxClick = { onTaskCheck(task) },
                                        isDragging = isDragging,
                                        isAnyDragging = isAnyDragging,
                                        shape =
                                        RoundedCornerShape(
                                            topStart = 0.dp,
                                            topEnd = 0.dp,
                                            bottomStart = 12.dp,
                                            bottomEnd = 12.dp,
                                        ),
                                        categoryLabel = categoryLabelFor(task),
                                        categoryIcon = categoryIconFor(task.category),
                                        locationLabel = task.locationName,
                                        onLocationClick = openLocation,
                                        isOverdue = isOverdue,
                                        overdueLabel = overdueLabel,
                                    )
                                }
                            } else {
                                val openLocation = rememberOpenLocation(
                                    task.locationName,
                                    task.locationAddress,
                                    task.locationLat,
                                    task.locationLng,
                                )
                                TDTaskCardWithCheckbox(
                                    taskText = if (task.isSecret) task.title.maskTitle() else task.title,
                                    taskDescription = if (task.isSecret) task.description?.maskDescription() else task.description,
                                    isChecked = task.isCompleted,
                                    onCheckBoxClick = { onTaskCheck(task) },
                                    isDragging = isDragging,
                                    isAnyDragging = isAnyDragging,
                                    categoryLabel = categoryLabelFor(task),
                                    categoryIcon = categoryIconFor(task.category),
                                    locationLabel = task.locationName,
                                    onLocationClick = openLocation,
                                    isOverdue = isOverdue,
                                    overdueLabel = overdueLabel,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeSwipeDismissBackground(direction: SwipeToDismissBoxValue) {
    val color by animateColorAsState(
        targetValue =
        when (direction) {
            SwipeToDismissBoxValue.EndToStart -> TDTheme.colors.crossRed
            SwipeToDismissBoxValue.StartToEnd -> TDTheme.colors.pendingGray
            else -> Color.Transparent
        },
        label = "swipe_bg_color",
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

@Composable
private fun categoryLabelFor(task: Task): String? {
    val categoryText = categoryDisplayText(task)
    val recurrenceText = recurrenceDisplayText(task.recurrence)
    return when {
        categoryText != null && recurrenceText != null -> "$categoryText · $recurrenceText"
        categoryText != null -> categoryText
        recurrenceText != null -> recurrenceText
        else -> null
    }
}

@Composable
private fun categoryDisplayText(task: Task): String? {
    val category = task.category
    if (category == com.todoapp.mobile.domain.model.TaskCategory.PERSONAL) return null
    if (category == com.todoapp.mobile.domain.model.TaskCategory.OTHER) {
        return task.customCategoryName?.takeIf { it.isNotBlank() }
            ?: stringResource(com.todoapp.mobile.R.string.category_other)
    }
    val res = when (category) {
        com.todoapp.mobile.domain.model.TaskCategory.SHOPPING -> com.todoapp.mobile.R.string.category_shopping
        com.todoapp.mobile.domain.model.TaskCategory.MEDICINE -> com.todoapp.mobile.R.string.category_medicine
        com.todoapp.mobile.domain.model.TaskCategory.HEALTH -> com.todoapp.mobile.R.string.category_health
        com.todoapp.mobile.domain.model.TaskCategory.WORK -> com.todoapp.mobile.R.string.category_work
        com.todoapp.mobile.domain.model.TaskCategory.STUDY -> com.todoapp.mobile.R.string.category_study
        com.todoapp.mobile.domain.model.TaskCategory.BIRTHDAY -> com.todoapp.mobile.R.string.category_birthday
        com.todoapp.mobile.domain.model.TaskCategory.PERSONAL,
        com.todoapp.mobile.domain.model.TaskCategory.OTHER,
        -> return null
    }
    return stringResource(res)
}

@androidx.annotation.DrawableRes
private fun categoryIconFor(category: com.todoapp.mobile.domain.model.TaskCategory): Int? = when (category) {
    com.todoapp.mobile.domain.model.TaskCategory.SHOPPING -> R.drawable.ic_shopping_label
    com.todoapp.mobile.domain.model.TaskCategory.MEDICINE -> R.drawable.ic_medication_label
    com.todoapp.mobile.domain.model.TaskCategory.HEALTH -> R.drawable.ic_health_label
    com.todoapp.mobile.domain.model.TaskCategory.WORK -> R.drawable.ic_work_label
    com.todoapp.mobile.domain.model.TaskCategory.STUDY -> R.drawable.ic_study_label
    com.todoapp.mobile.domain.model.TaskCategory.BIRTHDAY -> R.drawable.ic_birthday_label
    com.todoapp.mobile.domain.model.TaskCategory.PERSONAL,
    com.todoapp.mobile.domain.model.TaskCategory.OTHER,
    -> null
}

@Composable
private fun recurrenceDisplayText(recurrence: Recurrence): String? = when (recurrence) {
    Recurrence.NONE -> null
    Recurrence.DAILY -> stringResource(com.todoapp.mobile.R.string.recurrence_daily)
    Recurrence.WEEKLY -> stringResource(com.todoapp.mobile.R.string.recurrence_weekly)
    Recurrence.MONTHLY -> stringResource(com.todoapp.mobile.R.string.recurrence_monthly)
    Recurrence.YEARLY -> stringResource(com.todoapp.mobile.R.string.recurrence_yearly)
}
