package com.todoapp.mobile.ui.creationhub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.creationhub.CreationHubContract.TaskScope
import com.todoapp.mobile.ui.creationhub.CreationHubContract.UiAction
import com.todoapp.uikit.components.TDButton
import com.todoapp.uikit.components.TDButtonSize
import com.todoapp.uikit.components.TDButtonType
import com.todoapp.uikit.components.TDChoiceTile
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR

/**
 * "Is this yours or the group's?" — asked before the shape of the task, because scope changes who a
 * task is for while type only changes how it repeats. Both answers lead to the same form afterwards.
 *
 * With no administered group the Group tile is shown **disabled** rather than hidden: the old flow
 * silently dropped the group card and a user who had never made a group had no way to learn the
 * feature existed.
 */
@Composable
internal fun CreationHubScopeStep(
    hasGroups: Boolean,
    selected: TaskScope?,
    onAction: (UiAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TDText(
            text = stringResource(R.string.creation_scope_question),
            style = TDTheme.typography.heading3.copy(fontWeight = FontWeight.Bold),
            color = TDTheme.colors.onBackground,
            modifier = Modifier.fillMaxWidth(),
            isHeading = true,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TDChoiceTile(
                label = stringResource(R.string.creation_scope_personal),
                icon = painterResource(UiKitR.drawable.ic_personal_label),
                accentColor = TDTheme.colors.primary,
                selected = selected == TaskScope.PERSONAL,
                onClick = { onAction(UiAction.OnScopeSelect(TaskScope.PERSONAL)) },
                modifier = Modifier.weight(1f),
            )
            TDChoiceTile(
                label = stringResource(R.string.creation_scope_group),
                icon = painterResource(R.drawable.ic_groups),
                accentColor = TDTheme.colors.darkPurple,
                selected = selected == TaskScope.GROUP,
                enabled = hasGroups,
                onClick = { onAction(UiAction.OnScopeSelect(TaskScope.GROUP)) },
                modifier = Modifier.weight(1f),
            )
        }
        if (!hasGroups) {
            Spacer(Modifier.height(16.dp))
            TDText(
                text = stringResource(R.string.creation_scope_no_group_hint),
                style = TDTheme.typography.subheading1.copy(textAlign = TextAlign.Center),
                color = TDTheme.colors.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TDButton(
                    text = stringResource(R.string.creation_scope_create_group),
                    type = TDButtonType.OUTLINE,
                    size = TDButtonSize.SMALL,
                    // Reuses the hub's own "create a group" route rather than minting a second one.
                    onClick = { onAction(UiAction.OnGroupCardTap) },
                )
            }
        }
    }
}

@TDPreview
@Composable
private fun CreationHubScopeStepPreview() {
    TDTheme {
        CreationHubScopeStep(hasGroups = true, selected = null, onAction = {})
    }
}

@TDPreview
@Composable
private fun CreationHubScopeStepSelectedPreview() {
    TDTheme {
        CreationHubScopeStep(hasGroups = true, selected = TaskScope.GROUP, onAction = {})
    }
}

@TDPreview
@Composable
private fun CreationHubScopeStepNoGroupsPreview() {
    TDTheme {
        CreationHubScopeStep(hasGroups = false, selected = null, onAction = {})
    }
}
