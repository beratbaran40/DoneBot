package com.todoapp.mobile.data.location

import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.location.PlaceSearchRepository
import com.todoapp.mobile.domain.location.model.PickedPlace
import com.todoapp.mobile.domain.location.model.PlacePrediction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Places-SDK-backed [PlaceSearchRepository]. Keeps the Maps SDK types confined to the data layer
 * and offloads every network call to [ioDispatcher].
 *
 * Session token: Google bills autocomplete per session, not per keystroke, when a single
 * [AutocompleteSessionToken] threads all prediction requests plus the final `fetchPlace`. We hold
 * one token across a search and drop it after a details fetch so the next search starts fresh.
 */
@Singleton
class PlaceSearchRepositoryImpl @Inject constructor(
    private val placesClient: PlacesClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PlaceSearchRepository {

    private var sessionToken: AutocompleteSessionToken? = null

    private fun currentToken(): AutocompleteSessionToken = sessionToken ?: AutocompleteSessionToken.newInstance().also { sessionToken = it }

    override suspend fun predictions(query: String): List<PlacePrediction> {
        if (query.length < MIN_QUERY_LENGTH) return emptyList()
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(currentToken())
            .setQuery(query)
            .build()
        return withContext(ioDispatcher) {
            placesClient.findAutocompletePredictions(request).await().autocompletePredictions
                .map { prediction ->
                    PlacePrediction(
                        placeId = prediction.placeId,
                        primaryText = prediction.getPrimaryText(null).toString(),
                        secondaryText = prediction.getSecondaryText(null).toString(),
                    )
                }
        }
    }

    override suspend fun details(placeId: String): PickedPlace {
        val request = FetchPlaceRequest.builder(placeId, PLACE_FIELDS)
            .setSessionToken(currentToken())
            .build()
        val place = withContext(ioDispatcher) {
            placesClient.fetchPlace(request).await().place
        }
        sessionToken = null // session consumed → next search opens a fresh (separately billed) one
        return PickedPlace(
            name = place.name.orEmpty(),
            address = place.address.orEmpty(),
            lat = place.latLng?.latitude,
            lng = place.latLng?.longitude,
        )
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 2
        val PLACE_FIELDS = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
    }
}
