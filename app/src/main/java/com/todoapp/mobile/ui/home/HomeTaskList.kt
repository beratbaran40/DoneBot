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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.todoapp.mobile.domain.model.Subtask
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.ui.common.components.SubtaskChecklist
import com.todoapp.mobile.ui.common.components.categoryIconFor
import com.todoapp.mobile.ui.common.components.taskChipLabel
import com.todoapp.mobile.ui.common.rememberOpenLocation
import com.todoapp.uikit.components.TDTaskCardWithCheckbox
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.rememberKitArtPainter
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.modifier.crtScreen
import com.todoapp.uikit.modifier.tdDropShadow
import com.todoapp.uikit.theme.PaletteKit
import com.todoapp.uikit.theme.TDTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTaskList(
    tasks: List<Task>,
    isSignedIn: Boolean = false,
    lazyListState: LazyListState,
    reorderableLazyListState: ReorderableLazyListState,
    hapticFeedback: HapticFeedback,
    onTaskCheck: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onTaskLongPress: (Task) -> Unit,
    onToggleTaskSecret: (Task) -> Unit,
    onMoveTask: (Int, Int) -> Unit,
    onReorderFinished: () -> Unit,
    expandedStagedTaskId: Long? = null,
    expandedSubtasks: List<Subtask> = emptyList(),
    onStagedExpandToggle: (Long) -> Unit = {},
    onSubtaskToggle: (Long, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    emptyTitleRes: Int = com.todoapp.mobile.R.string.no_tasks_today,
    emptyDescriptionRes: Int = com.todoapp.mobile.R.string.no_tasks_today_description,
    emptyImageRes: Int? = null,
    headerContent: LazyListScope.() -> Unit = {},
) {
    val isAnyDragging = reorderableLazyListState.isAnyItemDragging
    val today = remember { java.time.LocalDate.now() }
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
                        painter = rememberKitArtPainter(painterResource(emptyImageRes ?: defaultIdleImage), 180.dp),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(180.dp)
                            .tdDropShadow(
                                elevation = 8.dp,
                                shape = TDTheme.shapes.circle,
                                ambientColor = TDTheme.colors.purple.copy(alpha = 0.3f),
                            )
                            .clip(TDTheme.shapes.circle)
                            .crtScreen(minSide = 180.dp, bezel = false)
                            .border(
                                width = 2.dp,
                                color = TDTheme.colors.lightPurple.copy(alpha = 0.6f),
                                shape = TDTheme.shapes.circle,
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
                        // Two constraints pin this line, and only the type size satisfies both.
                        //
                        // Vertically: at heading6 these strings wrapped to two lines in every language
                        // and face, and the second line landed under the bottom bar.
                        //
                        // Horizontally: the add-task FAB's left edge sits 339dp in, and the screen
                        // centre is 213dp, so any centred line wider than ~251dp runs under it. That
                        // is narrower than heading6 or the body size can make these strings, in the
                        // mono face especially — widening the column to force one line just traded a
                        // clipped tail for a tail behind the FAB.
                        //
                        // subheading1 clears both at once, and fixes a hierarchy that was never right:
                        // a description two points under its own title reads as a second title.
                        text = stringResource(emptyDescriptionRes),
                        modifier = Modifier.padding(horizontal = 80.dp),
                        style = TDTheme.typography.subheading1,
                        color = TDTheme.colors.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            itemsIndexed(
                items = tasks,
                key = { _, task -> task.id },
                contentType = { _, task -> task.photoUrls.isNotEmpty() },
            ) { index, task ->
                val isOverdue = !task.isCompleted &&
                    task.recurrence == Recurrence.NONE &&
                    task.date.isBefore(today)
                val displayTitle =
                    remember(task.title, task.isSecret) {
                        if (task.isSecret) task.title.maskTitle() else task.title
                    }
                val displayDescription =
                    remember(task.description, task.isSecret) {
                        if (task.isSecret) task.description?.maskDescription() else task.description
                    }
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
                                val photoUrl =
                                    remember(firstPhoto) {
                                        val base = com.todoapp.mobile.BuildConfig.BASE_URL.trimEnd('/')
                                        "$base/${firstPhoto.trimStart('/')}"
                                    }
                                Column(
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(TDTheme.shapes.medium)
                                        .background(TDTheme.colors.lightPending),
                                ) {
                                    SecretOrNormalPhotoBanner(
                                        url = photoUrl,
                                        isSecret = task.isSecret,
                                    )
                                    val openLocation = rememberOpenLocation(
                                        task.locationName,
                                        task.locationAddress,
                                        task.locationLat,
                                        task.locationLng,
                                    )
                                    TDTaskCardWithCheckbox(
                                        taskText = displayTitle,
                                        taskDescription = displayDescription,
                                        isChecked = task.isCompleted,
                                        onCheckBoxClick = { onTaskCheck(task) },
                                        isPendingSync = task.isPendingSync && isSignedIn,
                                        // Every kit but ORIGINAL marks one-time tasks (no recurrence,
                                        // no subtasks) with an accent left stripe — a sprite edge in
                                        // the 8-bit kit, a gutter marker in the terminal one.
                                        categoryStripeColor = when (TDTheme.palette) {
                                            PaletteKit.ORIGINAL -> null
                                            PaletteKit.MONOCHROME,
                                            PaletteKit.PIXEL,
                                            PaletteKit.TERMINAL,
                                            ->
                                                TDTheme.colors.purple.takeIf {
                                                    task.recurrence == Recurrence.NONE &&
                                                        task.subtaskTotal == 0
                                                }
                                        },
                                        isDragging = isDragging,
                                        isAnyDragging = isAnyDragging,
                                        shape =
                                        RoundedCornerShape(
                                            topStart = 0.dp,
                                            topEnd = 0.dp,
                                            bottomStart = 12.dp,
                                            bottomEnd = 12.dp,
                                        ),
                                        categoryLabel = taskChipLabel(task),
                                        categoryIcon = categoryIconFor(task.category),
                                        locationLabel = task.locationName,
                                        onLocationClick = openLocation,
                                        isOverdue = isOverdue,
                                        overdueLabel = overdueLabel,
                                        subtaskTotal = task.subtaskTotal,
                                        subtaskDone = task.subtaskDone,
                                        subtaskExpanded = task.id == expandedStagedTaskId,
                                        onSubtaskExpandToggle = { onStagedExpandToggle(task.id) },
                                        subtaskContent = {
                                            SubtaskChecklist(
                                                subtasks = expandedSubtasks,
                                                onToggle = onSubtaskToggle,
                                                masked = task.isSecret,
                                            )
                                        },
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
                                    taskText = displayTitle,
                                    taskDescription = displayDescription,
                                    isChecked = task.isCompleted,
                                    onCheckBoxClick = { onTaskCheck(task) },
                                    isPendingSync = task.isPendingSync && isSignedIn,
                                    isDragging = isDragging,
                                    isAnyDragging = isAnyDragging,
                                    categoryLabel = taskChipLabel(task),
                                    categoryIcon = categoryIconFor(task.category),
                                    locationLabel = task.locationName,
                                    onLocationClick = openLocation,
                                    isOverdue = isOverdue,
                                    overdueLabel = overdueLabel,
                                    subtaskTotal = task.subtaskTotal,
                                    subtaskDone = task.subtaskDone,
                                    subtaskExpanded = task.id == expandedStagedTaskId,
                                    onSubtaskExpandToggle = { onStagedExpandToggle(task.id) },
                                    subtaskContent = {
                                        SubtaskChecklist(
                                            subtasks = expandedSubtasks,
                                            onToggle = onSubtaskToggle,
                                            masked = task.isSecret,
                                        )
                                    },
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
            .background(color, TDTheme.shapes.medium)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment,
    ) {
        when (direction) {
            SwipeToDismissBoxValue.EndToStart ->
                Icon(
                    painter = tdPainter(R.drawable.ic_delete),
                    contentDescription = null,
                    tint = Color.White,
                )

            SwipeToDismissBoxValue.StartToEnd ->
                Icon(
                    painter = tdPainter(com.todoapp.mobile.R.drawable.ic_secret_mode),
                    contentDescription = null,
                    tint = Color.White,
                )

            else -> {}
        }
    }
}
