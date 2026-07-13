package com.todoapp.mobile.domain.usecase.location

import com.todoapp.mobile.domain.location.PlaceSearchRepository
import com.todoapp.mobile.domain.location.model.PlacePrediction
import javax.inject.Inject

class GetPlacePredictionsUseCase @Inject constructor(
    private val repository: PlaceSearchRepository,
) {
    suspend operator fun invoke(query: String): List<PlacePrediction> = repository.predictions(query)
}
