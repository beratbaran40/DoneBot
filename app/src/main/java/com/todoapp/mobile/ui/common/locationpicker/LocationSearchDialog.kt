package com.todoapp.mobile.ui.common.locationpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.common.locationpicker.LocationSearchContract.UiState.Status
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.theme.TDTheme

/**
 * Full-screen, on-brand replacement for Google's stock Places Autocomplete Activity. Hosts a
 * search field + live prediction list and returns the chosen place through [onPicked] — the same
 * (name, address, lat, lng) contract the launcher always exposed.
 */
@Composable
fun LocationSearchDialog(
    onPicked: (name: String, address: String, lat: Double?, lng: Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    val viewModel: LocationSearchViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.reset() }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is LocationSearchContract.UiEffect.PlacePicked ->
                    onPicked(effect.name, effect.address, effect.lat, effect.lng)

                LocationSearchContract.UiEffect.Dismiss -> onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LocationSearchContent(state = state, onAction = viewModel::onAction)
    }
}

@Composable
internal fun LocationSearchContent(
    state: LocationSearchContract.UiState,
    onAction: (LocationSearchContract.UiAction) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = TDTheme.colors.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            LocationSearchBar(
                query = state.query,
                onQueryChange = { onAction(LocationSearchContract.UiAction.QueryChanged(it)) },
                onBack = { onAction(LocationSearchContract.UiAction.Dismiss) },
                onClear = { onAction(LocationSearchContract.UiAction.ClearQuery) },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (state.status) {
                    Status.Idle -> LocationSearchIdle()
                    Status.Loading -> LocationSearchLoading()
                    Status.Success -> LocationSearchResults(
                        results = state.results,
                        onClick = { onAction(LocationSearchContract.UiAction.PredictionClicked(it)) },
                    )

                    Status.Empty -> LocationSearchEmpty()
                    Status.Error -> LocationSearchError(
                        onRetry = { onAction(LocationSearchContract.UiAction.Retry) },
                    )
                }
            }
            PoweredByGoogleFooter()
        }
    }
}

@Composable
private fun LocationSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val isInspection = LocalInspectionMode.current
    LaunchedEffect(Unit) {
        if (!isInspection) focusRequester.requestFocus()
    }

    // Local TextFieldValue keeps the caret at the end when the ViewModel echoes the query back,
    // mirroring TDCompactOutlinedTextField — avoids caret jumps on fast typing.
    var fieldValue by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    LaunchedEffect(query) {
        if (query != fieldValue.text) {
            fieldValue = fieldValue.copy(text = query, selection = TextRange(query.length))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(com.example.uikit.R.drawable.ic_arrow_back),
            contentDescription = stringResource(R.string.location_search_back),
            tint = TDTheme.colors.onBackground,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .padding(8.dp)
                .size(24.dp),
        )
        Spacer(Modifier.width(4.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(TDTheme.colors.lightPending)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(com.example.uikit.R.drawable.ic_search),
                contentDescription = null,
                tint = TDTheme.colors.darkPending,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    TDText(
                        text = stringResource(R.string.location_search_hint),
                        style = TDTheme.typography.regularTextStyle,
                        color = TDTheme.colors.gray,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = fieldValue,
                    onValueChange = {
                        fieldValue = it
                        if (it.text != query) onQueryChange(it.text)
                    },
                    singleLine = true,
                    textStyle = TDTheme.typography.regularTextStyle.copy(color = TDTheme.colors.onBackground),
                    cursorBrush = SolidColor(TDTheme.colors.purple),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
            if (query.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(com.example.uikit.R.drawable.ic_close),
                    contentDescription = stringResource(R.string.location_search_clear_query),
                    tint = TDTheme.colors.gray,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onClear)
                        .size(20.dp),
                )
            }
        }
    }
}
