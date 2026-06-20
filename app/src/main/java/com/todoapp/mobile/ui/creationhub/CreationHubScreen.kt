package com.todoapp.mobile.ui.creationhub

import android.provider.Settings
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.creationhub.CreationHubContract.Step
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskType
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiAction
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiEffect
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiState
import com.todoapp.uikit.components.TDFeatureCard
import com.todoapp.uikit.components.TDFeatureExplainer
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
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
            .background(TDTheme.colors.background)
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
                Step.HUB_ROOT -> CreationHubCarousel(onAction = onAction)
                Step.TASK_TYPE ->
                    CreationHubTypeStep(showGroupCard = state.adminGroups.isNotEmpty(), onAction = onAction)
                Step.TASK_CORE ->
                    if (state.taskType == TaskType.GROUP) {
                        CreationHubGroupStep(state = state, onAction = onAction)
                    } else {
                        CreationHubCoreStep(state = state, onAction = onAction)
                    }
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
    val titleRes = if (step == Step.HUB_ROOT) R.string.creation_hub_prompt else R.string.creation_type_title
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onBack)
        Spacer(Modifier.width(8.dp))
        TDText(
            text = stringResource(titleRes),
            style = TDTheme.typography.heading3.copy(fontWeight = FontWeight.SemiBold),
            color = TDTheme.colors.onBackground,
        )
        Spacer(Modifier.weight(1f))
        InfoButton(onInfo)
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
        Icon(
            painter = painterResource(UiKitR.drawable.ic_arrow_back),
            contentDescription = stringResource(R.string.creation_back_cd),
            tint = TDTheme.colors.onBackground,
        )
    }
}

@Composable
private fun InfoButton(onInfo: () -> Unit) {
    IconButton(onClick = onInfo, modifier = Modifier.size(40.dp)) {
        Icon(
            painter = painterResource(UiKitR.drawable.ic_info),
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
            icon = painterResource(UiKitR.drawable.ic_edit_task)
            accent = TDTheme.colors.darkPending
            nameRes = R.string.type_one_time_title
            subtitleRes = R.string.type_one_time_subtitle
        }
        TaskType.ROUTINE -> {
            icon = painterResource(R.drawable.ic_calendar)
            accent = TDTheme.colors.purple
            nameRes = R.string.type_routine_title
            subtitleRes = R.string.type_routine_subtitle
        }
        TaskType.STAGED -> {
            icon = painterResource(R.drawable.ic_staged)
            accent = TDTheme.colors.mediumGreen
            nameRes = R.string.type_staged_title
            subtitleRes = R.string.type_staged_subtitle
        }
        TaskType.GROUP -> {
            icon = painterResource(R.drawable.ic_groups)
            accent = TDTheme.colors.darkPurple
            nameRes = R.string.type_group_title
            subtitleRes = R.string.type_group_subtitle
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

@Composable
private fun CreationHubCarousel(onAction: (UiAction) -> Unit) {
    val features = listOf(
        HubFeature(
            R.string.create_task_card_title,
            R.string.create_task_card_subtitle,
            painterResource(UiKitR.drawable.ic_edit_task),
            TDTheme.colors.lightPending,
            TDTheme.colors.darkPending,
            UiAction.OnCreateTaskCardTap,
        ),
        HubFeature(
            R.string.journal_card_title,
            R.string.journal_card_subtitle,
            painterResource(UiKitR.drawable.ic_journal),
            TDTheme.colors.lightGreen,
            TDTheme.colors.darkGreen,
            UiAction.OnJournalCardTap,
        ),
        HubFeature(
            R.string.pomodoro_card_title,
            R.string.pomodoro_card_subtitle,
            painterResource(UiKitR.drawable.ic_pomodoro),
            TDTheme.colors.warmContainer,
            TDTheme.colors.orange,
            UiAction.OnPomodoroCardTap,
        ),
        HubFeature(
            R.string.group_card_title,
            R.string.group_card_subtitle,
            painterResource(R.drawable.ic_groups),
            TDTheme.colors.purpleContainer,
            TDTheme.colors.darkPurple,
            UiAction.OnGroupCardTap,
        ),
    )
    val pagerState = rememberPagerState(pageCount = { features.size })
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberReduceMotion()
    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 44.dp, vertical = 8.dp),
            pageSpacing = 16.dp,
        ) { page ->
            val feature = features[page]
            val rawOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val offset = rawOffset.absoluteValue.coerceIn(0f, 1f)
            TDFeatureCard(
                title = stringResource(feature.titleRes),
                subtitle = stringResource(feature.subtitleRes),
                icon = feature.icon,
                cardColor = feature.cardColor,
                accentColor = feature.accentColor,
                onClick = {
                    if (page == pagerState.currentPage) {
                        onAction(feature.action)
                    } else {
                        scope.launch {
                            if (reduceMotion) pagerState.scrollToPage(page) else pagerState.animateScrollToPage(page)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val scale = if (reduceMotion) 1f else lerp(MIN_SCALE, 1f, 1f - offset)
                        scaleX = scale
                        scaleY = scale
                        alpha = if (reduceMotion) 1f else lerp(MIN_ALPHA, 1f, 1f - offset)
                    },
            )
        }
        Spacer(Modifier.height(20.dp))
        PageDots(count = features.size, current = pagerState.currentPage)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val selected = index == current
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (selected) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (selected) TDTheme.colors.primary else TDTheme.colors.lightGray),
            )
        }
    }
}

@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    // ANIMATOR_DURATION_SCALE == 0 is Android's "remove animations" signal (Settings > Accessibility).
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

private const val MIN_SCALE = 0.86f
private const val MIN_ALPHA = 0.55f

@TDPreview
@Composable
private fun CreationHubCarouselPreview() {
    TDTheme {
        CreationHubScreen(
            state = UiState(step = Step.HUB_ROOT),
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
