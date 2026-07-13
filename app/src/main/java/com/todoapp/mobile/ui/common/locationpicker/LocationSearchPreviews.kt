package com.todoapp.mobile.ui.common.locationpicker

import androidx.compose.runtime.Composable
import com.todoapp.mobile.domain.location.model.PlacePrediction
import com.todoapp.mobile.ui.common.locationpicker.LocationSearchContract.UiState
import com.todoapp.mobile.ui.common.locationpicker.LocationSearchContract.UiState.Status
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

private val sampleResults = listOf(
    PlacePrediction("1", "Acıbadem Hastanesi", "Bağdat Cd. No:123, Kadıköy/İstanbul"),
    PlacePrediction("2", "Acıbadem Metro İstasyonu", "Üsküdar/İstanbul"),
    PlacePrediction("3", "Acıbadem Mahallesi", "Kadıköy/İstanbul"),
    PlacePrediction("4", "Acıbadem Spor Kulübü", "Ataşehir/İstanbul"),
)

@TDPreview
@Composable
private fun LocationSearchResultsPreview() {
    TDTheme {
        LocationSearchContent(
            state = UiState(query = "Acıbadem", status = Status.Success, results = sampleResults),
            onAction = {},
        )
    }
}

@TDPreview
@Composable
private fun LocationSearchIdlePreview() {
    TDTheme {
        LocationSearchContent(state = UiState(status = Status.Idle), onAction = {})
    }
}

@TDPreview
@Composable
private fun LocationSearchLoadingPreview() {
    TDTheme {
        LocationSearchContent(state = UiState(query = "Acı", status = Status.Loading), onAction = {})
    }
}

@TDPreview
@Composable
private fun LocationSearchEmptyPreview() {
    TDTheme {
        LocationSearchContent(state = UiState(query = "qwertyz", status = Status.Empty), onAction = {})
    }
}

@TDPreview
@Composable
private fun LocationSearchErrorPreview() {
    TDTheme {
        LocationSearchContent(state = UiState(query = "Acıbadem", status = Status.Error), onAction = {})
    }
}
