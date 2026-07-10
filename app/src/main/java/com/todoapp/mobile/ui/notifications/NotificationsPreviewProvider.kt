package com.todoapp.mobile.ui.notifications

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.todoapp.mobile.domain.model.Notification
import com.todoapp.mobile.domain.model.NotificationType
import java.time.LocalDate
import java.time.ZoneId

class NotificationsPreviewProvider : PreviewParameterProvider<NotificationsContract.UiState> {
    override val values: Sequence<NotificationsContract.UiState>
        get() {
            val now = System.currentTimeMillis()
            val sample =
                listOf(
                    Notification(
                        id = 1,
                        type = NotificationType.TASK_DUE_SOON,
                        title = "Due soon",
                        body = "Pay electricity bill is due in 1 hour",
                        payload = mapOf("taskTitle" to "Pay electricity bill", "groupName" to "Smith Family"),
                        isRead = false,
                        createdAt = now - HOUR_MS * 2,
                    ),
                    Notification(
                        id = 2,
                        type = NotificationType.INVITATION_DECLINED,
                        title = "Invitation declined",
                        body = "Mehmet declined your invitation to Weekend Crew",
                        payload = mapOf("declinerName" to "Mehmet", "groupName" to "Weekend Crew"),
                        isRead = false,
                        createdAt = now - HOUR_MS * 5,
                    ),
                    Notification(
                        id = 3,
                        type = NotificationType.TASK_COMPLETED,
                        title = "Task completed",
                        body = "Ayse completed Submit weekly report",
                        payload = mapOf("actorName" to "Ayse", "taskTitle" to "Submit weekly report"),
                        isRead = true,
                        createdAt = now - HOUR_MS * 8,
                    ),
                    Notification(
                        id = 4,
                        type = NotificationType.INVITATION_RECEIVED,
                        title = "New invitation",
                        body = "Berat invited you to Smith Family",
                        payload =
                        mapOf(
                            "invitationId" to "42",
                            "inviterName" to "Berat",
                            "groupName" to "Smith Family",
                            "groupDescription" to "Daily chores and groceries",
                            "memberCount" to "5",
                        ),
                        isRead = false,
                        createdAt = daysAgoAt(daysAgo = 1, hour = 18, minute = 45),
                    ),
                    Notification(
                        id = 5,
                        type = NotificationType.TASK_ASSIGNED,
                        title = "New task",
                        body = "Buy groceries assigned to you",
                        payload = mapOf("taskTitle" to "Buy groceries"),
                        isRead = true,
                        createdAt = daysAgoAt(daysAgo = 3, hour = 12, minute = 0),
                    ),
                    Notification(
                        id = 6,
                        type = NotificationType.UNKNOWN,
                        title = "DoneBot update",
                        body = "Something new is waiting in the app",
                        payload = emptyMap(),
                        isRead = true,
                        createdAt = daysAgoAt(daysAgo = 6, hour = 9, minute = 30),
                    ),
                )

            return sequenceOf(
                NotificationsContract.UiState.Loading,
                NotificationsContract.UiState.Error("Could not load notifications"),
                NotificationsContract.UiState.Success(items = emptyList()),
                NotificationsContract.UiState.Success(items = sample),
                NotificationsContract.UiState.Success(items = sample, isRefreshing = true),
                NotificationsContract.UiState.Success(items = sample.map { it.copy(isRead = true) }),
            )
        }
}

private const val HOUR_MS = 3_600_000L

/** Pins a sample to a concrete local date so the Yesterday/Earlier buckets stay stable in previews. */
private fun daysAgoAt(daysAgo: Long, hour: Int, minute: Int): Long = LocalDate.now()
    .minusDays(daysAgo)
    .atTime(hour, minute)
    .atZone(ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()
