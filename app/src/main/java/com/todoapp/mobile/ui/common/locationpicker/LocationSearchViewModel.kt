package com.todoapp.mobile.ui.common.locationpicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoapp.mobile.domain.usecase.location.GetPlaceDetailsUseCase
import com.todoapp.mobile.domain.usecase.location.GetPlacePredictionsUseCase
import com.todoapp.mobile.ui.common.locationpicker.LocationSearchContract.UiState.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationSearchViewModel @Inject constructor(
    private val getPredictions: GetPlacePredictionsUseCase,
    private val getDetails: GetPlaceDetailsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationSearchContract.UiState())
    val uiState: StateFlow<LocationSearchContract.UiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<LocationSearchContract.UiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<LocationSearchContract.UiEffect> = _uiEffect.receiveAsFlow()

    // The debounced source of truth for what we actually query, kept separate from the UiState
    // query so the text field stays perfectly responsive while network calls are rate-limited.
    private val queryFlow = MutableStateFlow("")

    init {
        observePredictions()
    }

    fun onAction(action: LocationSearchContract.UiAction) {
        when (action) {
            is LocationSearchContract.UiAction.QueryChanged -> onQueryChanged(action.query)
            is LocationSearchContract.UiAction.PredictionClicked -> onPredictionClicked(action.placeId)
            LocationSearchContract.UiAction.ClearQuery -> onQueryChanged("")
            LocationSearchContract.UiAction.Retry -> onRetry()
            LocationSearchContract.UiAction.Dismiss -> _uiEffect.trySend(LocationSearchContract.UiEffect.Dismiss)
        }
    }

    /** Called when the picker (re)opens so a reused ViewModel never shows stale results. */
    fun reset() {
        queryFlow.value = ""
        _uiState.value = LocationSearchContract.UiState()
    }

    private fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(
            query = query,
            status = if (query.isBlank()) Status.Idle else _uiState.value.status,
        )
        queryFlow.value = query
    }

    @OptIn(FlowPreview::class)
    private fun observePredictions() {
        viewModelScope.launch {
            queryFlow
                .debounce(DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { query -> search(query) }
        }
    }

    private suspend fun search(query: String) {
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(status = Status.Idle, results = emptyList())
            return
        }
        _uiState.value = _uiState.value.copy(status = Status.Loading)
        runCatching { getPredictions(query) }
            .onSuccess { predictions ->
                if (query != queryFlow.value) return@onSuccess // a newer query already superseded this
                _uiState.value = _uiState.value.copy(
                    results = predictions,
                    status = if (predictions.isEmpty()) Status.Empty else Status.Success,
                )
            }
            .onFailure { error ->
                if (error is CancellationException) throw error
                if (query != queryFlow.value) return@onFailure
                _uiState.value = _uiState.value.copy(status = Status.Error)
            }
    }

    private fun onPredictionClicked(placeId: String) {
        viewModelScope.launch {
            runCatching { getDetails(placeId) }
                .onSuccess { place ->
                    place ?: return@onSuccess
                    _uiEffect.trySend(
                        LocationSearchContract.UiEffect.PlacePicked(
                            name = place.name,
                            address = place.address,
                            lat = place.lat,
                            lng = place.lng,
                        ),
                    )
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.value = _uiState.value.copy(status = Status.Error)
                }
        }
    }

    private fun onRetry() {
        viewModelScope.launch { search(queryFlow.value) }
    }

    private companion object {
        const val DEBOUNCE_MS = 280L
    }
}
