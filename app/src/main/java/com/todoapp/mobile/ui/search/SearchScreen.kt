package com.todoapp.mobile.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.todoapp.mobile.R
import com.todoapp.mobile.common.maskDescription
import com.todoapp.mobile.common.maskTitle
import com.todoapp.mobile.domain.model.Task
import com.todoapp.mobile.ui.common.components.categoryIconFor
import com.todoapp.mobile.ui.common.components.taskChipLabel
import com.todoapp.mobile.ui.search.SearchContract.SearchResultItem
import com.todoapp.mobile.ui.search.SearchContract.UiAction
import com.todoapp.mobile.ui.search.SearchContract.UiEffect
import com.todoapp.mobile.ui.search.SearchContract.UiState
import com.todoapp.mobile.ui.security.biometric.BiometricAuthenticator
import com.todoapp.uikit.components.TDEmptyState
import com.todoapp.uikit.components.TDTaskCardWithCheckbox
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.components.TDTextField
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdCorner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.example.uikit.R as UikitR

@Composable
fun SearchScreen(
    uiState: UiState,
    query: String,
    uiEffect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    val context = LocalContext.current
    uiEffect.collectWithLifecycle { effect ->
        when (effect) {
            is UiEffect.ShowBiometricAuthenticator -> {
                val activity = context as? FragmentActivity ?: return@collectWithLifecycle
                val authenticated = BiometricAuthenticator.authenticate(activity)
                if (authenticated) onAction(UiAction.OnBiometricSuccess)
            }
        }
    }

    var textFieldVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        textFieldVisible = true
        focusRequester.requestFocus()
    }

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        AnimatedVisibility(
            visible = textFieldVisible,
            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
            exit = shrinkHorizontally() + fadeOut(),
        ) {
            TDTextField(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                value = query,
                onValueChange = { new -> onAction(UiAction.OnQueryChange(new)) },
                label = stringResource(R.string.search),
                leadingIcon = {
                    Icon(
                        painter = tdPainter(UikitR.drawable.ic_search),
                        contentDescription = null,
                        tint = TDTheme.colors.gray,
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        IconButton(onClick = { onAction(UiAction.OnQueryChange("")) }) {
                            Icon(
                                painter = tdPainter(UikitR.drawable.ic_close),
                                contentDescription = stringResource(R.string.cd_clear_search),
                                tint = TDTheme.colors.gray,
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                singleLine = true,
            )
        }

        Spacer(Modifier.height(12.dp))

        when (uiState) {
            is UiState.Idle -> SearchHint()
            is UiState.Loading -> SearchSkeleton()
            is UiState.Error -> SearchError(uiState.message)
            is UiState.Success -> SearchResults(uiState, onAction)
        }
    }
}

@Composable
private fun SearchResults(
    uiState: UiState.Success,
    onAction: (UiAction) -> Unit,
) {
    if (uiState.isFilterDialogOpen) {
        SearchFiltersDialog(
            initialFilters = uiState.filters,
            onApply = { onAction(UiAction.OnApplyFilters(it)) },
            onDismiss = { onAction(UiAction.OnDismissFilterDialog) },
            onClearAll = { onAction(UiAction.OnClearFilters) },
        )
    }
    Column {
        SearchFilterTrigger(
            activeCount = uiState.filters.activeCount,
            onClick = { onAction(UiAction.OnOpenFilterDialog) },
        )
        Spacer(Modifier.height(8.dp))
        if (uiState.results.isEmpty()) {
            SearchNoResults(uiState.query)
        } else {
            val hasPersonalTasks = uiState.results.any { it is SearchResultItem.PersonalTask }
            val hasGroupItems = uiState.results.any { it is SearchResultItem.GroupHeader }
            val showSections = hasPersonalTasks && hasGroupItems
            val firstPersonalTaskId =
                uiState.results
                    .filterIsInstance<SearchResultItem.PersonalTask>()
                    .firstOrNull()
                    ?.task
                    ?.id
            val firstGroupId =
                uiState.results
                    .filterIsInstance<SearchResultItem.GroupHeader>()
                    .firstOrNull()
                    ?.group
                    ?.id
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    items = uiState.results,
                    key = { item ->
                        when (item) {
                            is SearchResultItem.PersonalTask -> "personal_${item.task.id}"
                            is SearchResultItem.GroupHeader -> "group_header_${item.group.id}"
                            is SearchResultItem.GroupTaskResult -> "group_task_${item.group.id}_${item.groupTask.id}"
                        }
                    },
                ) { item ->
                    when (item) {
                        is SearchResultItem.PersonalTask -> {
                            if (showSections && item.task.id == firstPersonalTaskId) {
                                SearchSectionHeader(title = stringResource(R.string.search_section_tasks))
                            }
                            SearchTaskItem(task = item.task, onAction = onAction)
                        }
                        is SearchResultItem.GroupHeader -> {
                            if (item.group.id == firstGroupId) {
                                SearchSectionHeader(title = stringResource(R.string.search_section_groups))
                            }
                            SearchGroupHeaderItem(group = item.group, onAction = onAction)
                        }
                        is SearchResultItem.GroupTaskResult -> {
                            SearchGroupTaskItem(
                                group = item.group,
                                groupTask = item.groupTask,
                                onAction = onAction,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFilterTrigger(
    activeCount: Int,
    onClick: () -> Unit,
) {
    val hasActive = activeCount > 0
    val bgColor = if (hasActive) TDTheme.colors.purple else TDTheme.colors.lightPending
    val textColor = if (hasActive) TDTheme.colors.background else TDTheme.colors.darkPending
    val label =
        if (hasActive) {
            stringResource(R.string.search_filter_chip_label_with_count, activeCount)
        } else {
            stringResource(R.string.search_filter_chip_label)
        }
    Row(
        modifier =
        Modifier
            .background(color = bgColor, shape = TDTheme.shapes.xLarge)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = tdPainter(UikitR.drawable.ic_filter),
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        TDText(
            text = label,
            style = TDTheme.typography.subheading1,
            color = textColor,
        )
    }
}

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
private fun SearchTaskItem(
    task: Task,
    onAction: (UiAction) -> Unit,
) {
    val openLocation = com.todoapp.mobile.ui.common.rememberOpenLocation(
        task.locationName, task.locationAddress, task.locationLat, task.locationLng,
    )
    Column(modifier = Modifier.clickable { onAction(UiAction.OnTaskClick(task)) }) {
        TDTaskCardWithCheckbox(
            taskText = if (task.isSecret) task.title.maskTitle() else task.title,
            taskDescription = if (task.isSecret) task.description?.maskDescription() else task.description,
            isChecked = task.isCompleted,
            onCheckBoxClick = { onAction(UiAction.OnTaskCheck(task)) },
            categoryLabel = taskChipLabel(task),
            categoryIcon = categoryIconFor(task.category),
            locationLabel = task.locationName,
            onLocationClick = openLocation,
            subtaskTotal = task.subtaskTotal,
            subtaskDone = task.subtaskDone,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier =
            Modifier
                .padding(horizontal = 4.dp)
                .background(
                    color = TDTheme.colors.onBackground.copy(alpha = 0.06f),
                    shape = tdCorner(6.dp),
                ).padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(10.dp),
                painter = tdPainter(R.drawable.ic_calendar),
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
    }
}

@Composable
private fun SearchHint() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        TDText(
            text = stringResource(R.string.search_hint_with_groups),
            style = TDTheme.typography.heading6,
            color = TDTheme.colors.onBackground.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun SearchNoResults(query: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        TDEmptyState(
            title = stringResource(R.string.search_no_results, query),
        )
    }
}

@Composable
private fun SearchError(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        TDText(
            text = message,
            style = TDTheme.typography.heading4,
            color = TDTheme.colors.crossRed,
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun SearchScreenIdlePreview() {
    TDTheme {
        SearchScreen(
            uiState = UiState.Idle,
            query = "",
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun SearchScreenLoadingPreview() {
    TDTheme {
        SearchScreen(
            uiState = UiState.Loading,
            query = "Buy",
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun SearchScreenSuccessPreview() {
    TDTheme {
        SearchScreen(
            uiState =
            UiState.Success(
                query = "Buy",
                results =
                listOf(
                    SearchResultItem.PersonalTask(
                        Task(
                            id = 1,
                            title = "Buy milk",
                            description = "Organic",
                            date = LocalDate.now(),
                            isCompleted = false,
                            timeStart = LocalTime.of(9, 0),
                            timeEnd = LocalTime.of(10, 0),
                            isSecret = false,
                        ),
                    ),
                ),
                filters = SearchContract.SearchFilters(),
            ),
            query = "Buy",
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun SearchScreenSuccessDarkPreview() {
    TDTheme {
        SearchScreen(
            uiState =
            UiState.Success(
                query = "Buy",
                results =
                listOf(
                    SearchResultItem.PersonalTask(
                        Task(
                            id = 1,
                            title = "Buy milk",
                            description = "Organic",
                            date = LocalDate.now(),
                            isCompleted = false,
                            timeStart = LocalTime.of(9, 0),
                            timeEnd = LocalTime.of(10, 0),
                            isSecret = false,
                        ),
                    ),
                ),
                filters = SearchContract.SearchFilters(),
            ),
            query = "Buy",
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun SearchScreenEmptyResultsPreview() {
    TDTheme {
        SearchScreen(
            uiState =
            UiState.Success(
                query = "xyzzy",
                results = emptyList(),
                filters = SearchContract.SearchFilters(),
            ),
            query = "xyzzy",
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}

@com.todoapp.uikit.previews.TDPreview
@Composable
private fun SearchScreenErrorPreview() {
    TDTheme {
        SearchScreen(
            uiState = UiState.Error("Something went wrong while searching"),
            query = "Buy",
            uiEffect = emptyFlow(),
            onAction = {},
        )
    }
}
