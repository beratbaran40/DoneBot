package com.todoapp.mobile.domain.location

import com.todoapp.mobile.domain.location.model.PickedPlace
import com.todoapp.mobile.domain.location.model.PlacePrediction

/**
 * Place autocomplete + details, backing the custom location picker. Implementations own the
 * Google autocomplete session-token lifecycle internally (one session per search → details
 * round-trip), so callers only pass a query / place id and stay free of Maps SDK types.
 */
interface PlaceSearchRepository {
    /** Autocomplete predictions for [query]; returns empty for too-short/blank queries. */
    suspend fun predictions(query: String): List<PlacePrediction>

    /** Resolves [placeId] to full details and closes the current autocomplete session. */
    suspend fun details(placeId: String): PickedPlace?
}
