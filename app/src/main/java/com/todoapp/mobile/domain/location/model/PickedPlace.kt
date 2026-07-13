package com.todoapp.mobile.domain.location.model

/**
 * Fully resolved place the user selected, ready to attach to a task. Mirrors the
 * (name, address, lat, lng) tuple the location picker has always returned.
 */
data class PickedPlace(
    val name: String,
    val address: String,
    val lat: Double?,
    val lng: Double?,
)
