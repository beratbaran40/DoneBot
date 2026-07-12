package com.todoapp.mobile.ui.common.locationpicker

import com.todoapp.mobile.domain.location.model.PlacePrediction

object LocationSearchContract {

    data class UiState(
        val query: String = "",
        val status: Status = Status.Idle,
        val results: List<PlacePrediction> = emptyList(),
    ) {
        /** Idle = empty query prompt · Loading = querying · Success = has results · Empty = none · Error = failed. */
        enum class Status { Idle, Loading, Success, Empty, Error }
    }

    sealed interface UiAction {
        data class QueryChanged(val query: String) : UiAction
        data class PredictionClicked(val placeId: String) : UiAction
        data object ClearQuery : UiAction
        data object Retry : UiAction
        data object Dismiss : UiAction
    }

    sealed interface UiEffect {
        data class PlacePicked(
            val name: String,
            val address: String,
            val lat: Double?,
            val lng: Double?,
        ) : UiEffect

        data object Dismiss : UiEffect
    }
}
