package com.todoapp.mobile.domain.usecase.location

import com.todoapp.mobile.domain.location.PlaceSearchRepository
import com.todoapp.mobile.domain.location.model.PickedPlace
import javax.inject.Inject

class GetPlaceDetailsUseCase @Inject constructor(
    private val repository: PlaceSearchRepository,
) {
    suspend operator fun invoke(placeId: String): PickedPlace? = repository.details(placeId)
}
