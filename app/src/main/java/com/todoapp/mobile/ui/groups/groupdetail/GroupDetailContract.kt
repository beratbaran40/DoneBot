package com.todoapp.mobile.ui.groups.groupdetail

import androidx.compose.runtime.Immutable
import com.todoapp.mobile.domain.model.Recurrence
import com.todoapp.mobile.domain.model.TaskCategory
import com.todoapp.mobile.ui.home.TaskFormState
import com.todoapp.mobile.ui.home.TaskFormUiAction
import java.time.DayOfWeek

object GroupDetailContract {
    // Tab indices shared by the tab row, the pager, and initialTab route/navigation call sites.
    const val TAB_OVERVIEW = 0
    const val TAB_MEMBERS = 1
    const val TAB_ACTIVITY = 2
    const val TAB_COUNT = 3

    @Immutable
    data class GroupTaskUiItem(
        val id: Long,
        val title: String,
        val description: String?,
        val assigneeId: Long?,
        val assigneeAvatarUrl: String?,
        val assigneeName: String?,
        val assigneeInitials: String?,
        val dueTime: String?,
        val rawDueDate: Long?,
        val priority: String?,
        val isCompleted: Boolean,
        val isAssignedToMe: Boolean,
        val canDelete: Boolean = false,
        val photoUrls: List<String> = emptyList(),
        val locationName: String? = null,
        val locationAddress: String? = null,
        val locationLat: Double? = null,
        val locationLng: Double? = null,
        /**
         * The rule as raw fields rather than a rendered chip: the label needs string resources, and
         * `taskChipLabel` is a @Composable, so it is resolved in the card exactly the way personal
         * surfaces do it. Pre-formatting here would need a second, VM-side copy of that logic.
         */
        val category: TaskCategory = TaskCategory.PERSONAL,
        val customCategoryName: String? = null,
        val recurrence: Recurrence = Recurrence.NONE,
        val recurrenceInterval: Int = 1,
        val recurrenceByDay: Set<DayOfWeek> = emptySet(),
        val subtaskTotal: Int = 0,
        val subtaskDone: Int = 0,
        /**
         * True when this task repeats. Completion then means "this day's occurrence", not the whole
         * task, and the checkbox writes to the per-day table instead of the flat flag.
         */
        val isRecurring: Boolean = false,
    )

    @Immutable
    data class GroupMemberUiItem(
        val userId: Long,
        val displayName: String,
        val email: String,
        val avatarUrl: String?,
        val initials: String,
        val role: String,
        val joinedAt: String,
        val pendingTaskCount: Int,
        val isCurrentUser: Boolean,
    )

    // Outgoing invitation that hasn't been accepted yet — rendered in the Members tab so admins
    // can see who they already invited (tester feedback: "davet edilenler gösterilmiyor").
    @Immutable
    data class PendingInviteUiItem(
        val id: Long,
        val email: String,
        val invitedAt: String,
    )

    @Immutable
    data class GroupActivityUiItem(
        val id: Long,
        val type: String,
        val actorName: String,
        val actorAvatarUrl: String?,
        val actorInitials: String,
        val description: String,
        val relativeTime: String,
        val taskTitle: String?,
    )

    sealed interface UiState {
        data object Loading : UiState

        @Immutable
        data class Success(
            val groupId: Long,
            val groupName: String,
            val description: String,
            val memberCount: Int,
            val completedCount: Int,
            val pendingCount: Int,
            val tasks: List<GroupTaskUiItem>,
            val members: List<GroupMemberUiItem>,
            val pendingInvites: List<PendingInviteUiItem> = emptyList(),
            val activities: List<GroupActivityUiItem>,
            val selectedTab: Int = TAB_OVERVIEW,
            val taskFilter: TaskFilter = TaskFilter.ALL,
            val statusFilter: GroupTaskStatusFilter = GroupTaskStatusFilter.ALL,
            val timeFilter: GroupTaskTimeFilter = GroupTaskTimeFilter.ALL,
            val currentUserRole: String = "",
            val isTaskSheetOpen: Boolean = false,
            val taskFormState: TaskFormState = TaskFormState(),
            val editingTaskId: Long? = null,
            val pendingDeleteTaskId: Long? = null,
            val undoDeleteTaskId: Long? = null,
            val pendingAssignTaskId: Long? = null,
            val isRefreshing: Boolean = false,
            // First-invite dialog (shown once right after group creation). UI-owned fields —
            // they MUST be carried through loadGroupData's previousState preserve-list, or the
            // RESUMED-triggered reload wipes the open dialog and the typed email.
            val isFirstInviteDialogOpen: Boolean = false,
            val firstInviteEmail: String = "",
            val firstInviteErrorRes: Int? = null,
            val isFirstInviteSending: Boolean = false,
        ) : UiState

        data class Error(
            val message: String,
        ) : UiState
    }

    enum class TaskFilter { ALL, ASSIGNED_TO_ME }

    /** Completed/pending scope, driven by tapping the stat cards. */
    enum class GroupTaskStatusFilter { ALL, COMPLETED, PENDING }

    /** Due-date scope for the overview task list. */
    enum class GroupTaskTimeFilter { TODAY, THIS_WEEK, THIS_MONTH, ALL }

    sealed interface UiAction {
        data class OnTabSelected(
            val index: Int,
        ) : UiAction

        data class OnTaskFilterSelected(
            val filter: TaskFilter,
        ) : UiAction

        data class OnStatusFilterSelected(
            val status: GroupTaskStatusFilter,
        ) : UiAction

        data class OnTimeFilterSelected(
            val filter: GroupTaskTimeFilter,
        ) : UiAction

        data class OnTaskChecked(
            val taskId: Long,
            val isChecked: Boolean,
        ) : UiAction

        data object OnNewTaskTap : UiAction

        data object OnDismissGroupTaskSheet : UiAction

        data object OnGroupTaskCreate : UiAction

        data class OnGroupTaskFormAction(
            val action: TaskFormUiAction,
        ) : UiAction

        data object OnInviteTap : UiAction

        data class OnFirstInviteEmailChange(
            val value: String,
        ) : UiAction

        data object OnFirstInviteSend : UiAction

        data object OnFirstInviteDismiss : UiAction

        data class OnRemoveMemberTap(
            val userId: Long,
        ) : UiAction

        data class OnMemberTap(
            val userId: Long,
        ) : UiAction

        data class OnTaskTapped(
            val taskId: Long,
        ) : UiAction

        data class OnTaskLongPress(
            val taskId: Long,
        ) : UiAction

        data class OnDeleteTask(
            val taskId: Long,
        ) : UiAction

        data class OnAssignToMe(
            val taskId: Long,
        ) : UiAction

        data object OnUndoDeleteTask : UiAction

        data object OnScreenResumed : UiAction

        data object OnAssignToMeConfirm : UiAction

        data object OnAssignToMeDismiss : UiAction

        data object OnDeleteTaskConfirm : UiAction

        data object OnDeleteTaskDismiss : UiAction

        data object OnPullToRefresh : UiAction
    }

    sealed interface UiEffect {
        data class ShowToast(
            val message: String,
        ) : UiEffect
    }
}
