package com.todoapp.mobile.domain.usecase

import com.todoapp.mobile.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

data class OverdueSummary(
    val dates: Set<LocalDate>,
    val count: Int,
)

class ObserveOverdueSummaryUseCase
@Inject
constructor(
    private val taskRepository: TaskRepository,
) {
    operator fun invoke(today: LocalDate): Flow<OverdueSummary> = taskRepository.observeOverdueTasks(today).map { tasks ->
        OverdueSummary(
            dates = tasks.map { it.date }.toSet(),
            count = tasks.size,
        )
    }
}
