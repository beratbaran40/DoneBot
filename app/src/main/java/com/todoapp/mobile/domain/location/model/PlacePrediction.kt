package com.todoapp.mobile.domain.location.model

/**
 * A single autocomplete suggestion returned while the user types a location query.
 *
 * @param placeId opaque Google place id, later exchanged for full [PickedPlace] details.
 * @param primaryText bold main label (e.g. "Acıbadem Hastanesi").
 * @param secondaryText fuller context line (e.g. "Bağdat Cd., Kadıköy/İstanbul"); may be blank.
 */
data class PlacePrediction(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
)
