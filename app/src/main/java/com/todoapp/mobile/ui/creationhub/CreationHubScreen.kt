package com.todoapp.mobile.ui.creationhub

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.TaskType
import com.todoapp.mobile.ui.common.components.taskTypeAccent
import com.todoapp.mobile.ui.creationhub.CreationHubContract.GroupOption
import com.todoapp.mobile.ui.creationhub.CreationHubContract.Step
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskScope
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiAction
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiEffect
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiState
import com.todoapp.uikit.components.TDFeatureCard
import com.todoapp.uikit.components.TDFeatureExplainer
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.previews.TDPreviewNarrow
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import com.example.uikit.R as UiKitR

@Composable
fun CreationHubScreen(
    state: UiState,
    effect: Flow<UiEffect>,
    onAction: (UiAction) -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        effect.collect { e ->
            when (e) {
                is UiEffect.ShowToast ->
                    Toast.makeText(context, context.getString(e.messageRes), Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = state.step != Step.HUB_ROOT) { onAction(UiAction.OnBack) }

    var showInfo by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        CreationHubHeader(
            step = state.step,
            taskType = state.taskType,
            onBack = { onAction(UiAction.OnBack) },
            onInfo = { showInfo = true },
        )
        AnimatedContent(
            targetState = state.step,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
            label = "creationHubStep",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { step ->
            when (step) {
                Step.HUB_ROOT -> CreationHubGrid(onAction = onAction)
                Step.TASK_SCOPE ->
                    CreationHubScopeStep(
                        hasGroups = state.adminGroups.isNotEmpty(),
                        selected = state.scope,
                        onAction = onAction,
                    )
                Step.TASK_TYPE -> CreationHubTypeStep(onAction = onAction)
                // One form for both scopes now — the group-only sections live inside it.
                Step.TASK_CORE -> CreationHubCoreStep(state = state, onAction = onAction)
            }
        }
        if (showInfo) {
            CreationHubInfoDialog(step = state.step, onDismiss = { showInfo = false })
        }
    }
}

@Composable
private fun CreationHubHeader(
    step: Step,
    taskType: TaskType?,
    onBack: () -> Unit,
    onInfo: () -> Unit,
) {
    if (step == Step.TASK_CORE && taskType != null) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onBack)
            }
            TypeHeaderBlock(taskType)
        }
        return
    }
    // The scope step asks its own question in bold inside the step body, so the bar carries only the
    // controls — two headings stacked would read as a title plus a subtitle rather than one question.
    val titleRes = when (step) {
        Step.HUB_ROOT -> R.string.creation_hub_prompt
        Step.TASK_SCOPE -> null
        else -> R.string.creation_type_title
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onBack)
        Spacer(Modifier.width(8.dp))
        titleRes?.let {
            TDText(
                text = stringResource(it),
                style = TDTheme.typography.heading3.copy(fontWeight = FontWeight.SemiBold),
                color = TDTheme.colors.onBackground,
            )
        }
        Spacer(Modifier.weight(1f))
        InfoButton(onInfo)
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
        Icon(
            painter = tdPainter(UiKitR.drawable.ic_arrow_back),
            contentDescription = stringResource(R.string.creation_back_cd),
            tint = TDTheme.colors.onBackground,
        )
    }
}

@Composable
private fun InfoButton(onInfo: () -> Unit) {
    IconButton(onClick = onInfo, modifier = Modifier.size(40.dp)) {
        Icon(
            painter = tdPainter(UiKitR.drawable.ic_info),
            contentDescription = stringResource(R.string.creation_info_cd),
            tint = TDTheme.colors.onBackground,
        )
    }
}

@Composable
private fun CreationHubInfoDialog(step: Step, onDismiss: () -> Unit) {
    val titleRes: Int
    val descRes: Int
    val bullets: List<Int>
    if (step == Step.TASK_TYPE) {
        titleRes = R.string.creation_type_info_title
        descRes = R.string.creation_type_info_description
        bullets = listOf(
            R.string.creation_type_info_bullet_1,
            R.string.creation_type_info_bullet_2,
            R.string.creation_type_info_bullet_3,
            R.string.creation_type_info_bullet_4,
        )
    } else {
        titleRes = R.string.creation_hub_info_title
        descRes = R.string.creation_hub_info_description
        bullets = listOf(
            R.string.creation_hub_info_bullet_1,
            R.string.creation_hub_info_bullet_2,
            R.string.creation_hub_info_bullet_3,
        )
    }
    TDFeatureExplainer(
        title = stringResource(titleRes),
        description = stringResource(descRes),
        bulletPoints = bullets.map { stringResource(it) },
        buttonText = stringResource(R.string.got_it),
        onDismiss = onDismiss,
    )
}

@Composable
private fun TypeHeaderBlock(taskType: TaskType) {
    val icon: Painter
    val accent: Color
    val nameRes: Int
    val subtitleRes: Int
    when (taskType) {
        TaskType.ONE_TIME -> {
            icon = tdPainter(UiKitR.drawable.ic_edit_task)
            accent = taskTypeAccent(TaskType.ONE_TIME)
            nameRes = R.string.type_one_time_title
            subtitleRes = R.string.type_one_time_subtitle
        }
        TaskType.ROUTINE -> {
            icon = tdPainter(R.drawable.ic_calendar)
            accent = taskTypeAccent(TaskType.ROUTINE)
            nameRes = R.string.type_routine_title
            subtitleRes = R.string.type_routine_subtitle
        }
        TaskType.STAGED -> {
            icon = tdPainter(R.drawable.ic_staged)
            accent = taskTypeAccent(TaskType.STAGED)
            nameRes = R.string.type_staged_title
            subtitleRes = R.string.type_staged_subtitle
        }
        TaskType.CUSTOM -> {
            icon = tdPainter(R.drawable.ic_custom)
            accent = taskTypeAccent(TaskType.CUSTOM)
            nameRes = R.string.type_custom_title
            subtitleRes = R.string.type_custom_subtitle
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(accent),
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = TDTheme.colors.white,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        TDText(
            text = stringResource(nameRes),
            style = TDTheme.typography.heading4.copy(fontWeight = FontWeight.SemiBold),
            color = TDTheme.colors.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        TDText(
            text = stringResource(subtitleRes),
            style = TDTheme.typography.subheading1.copy(textAlign = TextAlign.Center),
            color = TDTheme.colors.onBackground.copy(alpha = 0.6f),
        )
    }
}

private data class HubFeature(
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: Painter,
    val cardColor: Color,
    val accentColor: Color,
    val action: UiAction,
)

/**
 * Everything the hub can create, all four on one screen: two rows of two, a quarter of the body each.
 *
 * This replaced a `HorizontalPager` carousel with page dots. Four is few enough to show at once, and
 * paging hid three of them behind a swipe and cost an extra tap — a tap on an off-centre page only
 * scrolled to it, it did not open it.
 */
@Composable
private fun CreationHubGrid(onAction: (UiAction) -> Unit) {
    val features = listOf(
        HubFeature(
            R.string.create_task_card_title,
            R.string.create_task_card_subtitle,
            tdPainter(UiKitR.drawable.ic_edit_task),
            TDTheme.colors.lightPending,
            taskTypeAccent(TaskType.ONE_TIME),
            UiAction.OnCreateTaskCardTap,
        ),
        HubFeature(
            R.string.journal_card_title,
            R.string.journal_card_subtitle,
            tdPainter(UiKitR.drawable.ic_journal),
            TDTheme.colors.lightGreen,
            TDTheme.colors.darkGreen,
            UiAction.OnJournalCardTap,
        ),
        HubFeature(
            R.string.pomodoro_card_title,
            R.string.pomodoro_card_subtitle,
            tdPainter(UiKitR.drawable.ic_pomodoro),
            TDTheme.colors.warmContainer,
            TDTheme.colors.orange,
            UiAction.OnPomodoroCardTap,
        ),
        HubFeature(
            R.string.group_card_title,
            R.string.group_card_subtitle,
            tdPainter(R.drawable.ic_groups),
            TDTheme.colors.purpleContainer,
            TDTheme.colors.darkPurple,
            UiAction.OnGroupCardTap,
        ),
    )
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Half of what is left once the gap and the bottom margin are taken out: when the copy fits —
        // every phone held in portrait — the two rows ARE the two halves and there is nothing to
        // scroll. A short landscape viewport or a large system font pushes a row past this floor, and
        // then the column scrolls rather than clipping a description off the bottom of a card.
        val boundedHeight = constraints.hasBoundedHeight
        val rowFloor =
            if (boundedHeight) {
                ((maxHeight - CREATION_GRID_GAP - CREATION_GRID_BOTTOM) / 2).coerceAtLeast(0.dp)
            } else {
                0.dp
            }
        // A cell has to be roomy both ways to earn the big card: a tablet held in landscape is wide
        // enough and nowhere near tall enough, and a 76dp medallion there just forces a scroll.
        val compactCells =
            (maxWidth - CREATION_GRID_GAP) / 2 < ROOMY_CELL_MIN_WIDTH ||
                (boundedHeight && rowFloor < ROOMY_CELL_MIN_HEIGHT)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = CREATION_GRID_BOTTOM),
            verticalArrangement = Arrangement.spacedBy(CREATION_GRID_GAP),
        ) {
            features.chunked(CREATION_GRID_COLUMNS).forEach { row ->
                CreationHubGridRow(
                    features = row,
                    minHeight = rowFloor,
                    compact = compactCells,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun CreationHubGridRow(
    features: List<HubFeature>,
    minHeight: Dp,
    compact: Boolean,
    onAction: (UiAction) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            // IntrinsicSize.Max + fillMaxHeight keeps the pair the same height when one card's
            // description wraps onto more lines than its neighbour's.
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(CREATION_GRID_GAP),
    ) {
        features.forEach { feature ->
            TDFeatureCard(
                title = stringResource(feature.titleRes),
                subtitle = stringResource(feature.subtitleRes),
                icon = feature.icon,
                cardColor = feature.cardColor,
                accentColor = feature.accentColor,
                compact = compact,
                onClick = { onAction(feature.action) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

private const val CREATION_GRID_COLUMNS = 2

/** Gap between the cards, both ways. `tools/textfit.py` reads this line. */
private val CREATION_GRID_GAP = 12.dp

/** Bottom margin under the grid, matching the one the task form leaves. */
private val CREATION_GRID_BOTTOM = 24.dp

/** Below either of these a cell is a phone quarter and the card packs itself; above both, a tablet's. */
private val ROOMY_CELL_MIN_WIDTH = 240.dp
private val ROOMY_CELL_MIN_HEIGHT = 240.dp

@TDPreview
@Composable
private fun CreationHubGridPreview() {
    TDTheme {
        Box(modifier = Modifier.height(720.dp)) {
            CreationHubScreen(
                state = UiState(step = Step.HUB_ROOT),
                effect = emptyFlow(),
                onAction = {},
            )
        }
    }
}

@TDPreviewNarrow
@Composable
private fun CreationHubGridNarrowPreview() {
    TDTheme {
        Box(modifier = Modifier.height(640.dp)) {
            CreationHubScreen(
                state = UiState(step = Step.HUB_ROOT),
                effect = emptyFlow(),
                onAction = {},
            )
        }
    }
}

@TDPreview
@Composable
private fun CreationHubScopePreview() {
    TDTheme {
        CreationHubScreen(
            state = UiState(
                step = Step.TASK_SCOPE,
                adminGroups = listOf(GroupOption(localId = 1, remoteId = 10, name = "Ev")),
            ),
            effect = emptyFlow(),
            onAction = {},
        )
    }
}

@TDPreview
@Composable
private fun CreationHubScopeNoGroupsPreview() {
    TDTheme {
        CreationHubScreen(
            state = UiState(step = Step.TASK_SCOPE),
            effect = emptyFlow(),
            onAction = {},
        )
    }
}

@TDPreview
@Composable
private fun CreationHubTypePreview() {
    TDTheme {
        CreationHubScreen(
            state = UiState(step = Step.TASK_TYPE),
            effect = emptyFlow(),
            onAction = {},
        )
    }
}

@TDPreview
@Composable
private fun CreationHubGroupCustomCorePreview() {
    TDTheme {
        CreationHubScreen(
            state = UiState(
                step = Step.TASK_CORE,
                scope = TaskScope.GROUP,
                taskType = TaskType.CUSTOM,
                title = "Çöpü çıkar",
                recurrence = Recurrence.DAILY,
                adminGroups = listOf(GroupOption(localId = 1, remoteId = 10, name = "Ev")),
                selectedGroupLocalId = 1,
                selectedGroupRemoteId = 10,
            ),
            effect = emptyFlow(),
            onAction = {},
        )
    }
}

@TDPreview
@Composable
private fun CreationHubStagedCorePreview() {
    TDTheme {
        CreationHubScreen(
            state = UiState(
                step = Step.TASK_CORE,
                taskType = TaskType.STAGED,
                title = "Tez bölümünü bitir",
                subtaskDrafts = listOf("Giriş", "Yöntem", ""),
            ),
            effect = emptyFlow(),
            onAction = {},
        )
    }
}
