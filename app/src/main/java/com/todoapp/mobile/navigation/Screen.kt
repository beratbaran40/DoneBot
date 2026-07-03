package com.todoapp.mobile.navigation

import kotlinx.serialization.Serializable

interface Screen {
    @Serializable
    data object Onboarding : Screen

    @Serializable
    data object AddPomodoroTimer : Screen

    @Serializable
    data object Pomodoro : Screen

    @Serializable
    data object Home : Screen

    @Serializable
    data object CreationHub : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object SecretMode : Screen

    @Serializable
    data object PlanYourDay : Screen

    @Serializable
    data object Notifications : Screen

    @Serializable
    data object Invitations : Screen

    @Serializable
    data object AlarmSounds : Screen

    @Serializable
    data object Licenses : Screen

    @Serializable
    data object Search : Screen

    @Serializable
    data object Calendar : Screen

    @Serializable
    data object Activity : Screen

    @Serializable
    data object Chat : Screen

    @Serializable
    data object Profile : Screen

    @Serializable
    data class Task(
        val taskId: Long,
    ) : Screen

    @Serializable
    data class Register(
        val redirectAfterRegister: String? = null,
    ) : Screen

    @Serializable
    data class Login(
        val redirectAfterLogin: String? = null,
    ) : Screen

    @Serializable
    data class WebView(
        val url: String,
    ) : Screen

    @Serializable
    data object ForgotPassword : Screen

    @Serializable
    data class ResetPassword(
        val token: String,
    ) : Screen

    @Serializable
    data object ChangePassword : Screen

    @Serializable
    data class PomodoroSummary(
        val focusSessions: Int,
        val totalFocusMinutes: Int,
        val totalBreakMinutes: Int,
    ) : Screen

    @Serializable
    data object PomodoroLaunch : Screen

    @Serializable
    data class Groups(
        val pendingDeleteGroupId: Long = -1L,
    ) : Screen

    @Serializable
    data object CreateNewGroup : Screen

    @Serializable
    data class FilteredTasks(
        val isCompleted: Boolean,
        val weekDateEpochDay: Long,
    ) : Screen

    @Serializable
    data class GroupDetail(
        val groupId: Long,
        val groupName: String,
    ) : Screen

    @Serializable
    data class GroupSettings(
        val groupId: Long,
    ) : Screen

    @Serializable
    data class InviteMember(
        val groupId: Long,
    ) : Screen

    @Serializable
    data class ManageMembers(
        val groupId: Long,
    ) : Screen

    @Serializable
    data class GroupTaskDetail(
        val groupId: Long,
        val taskId: Long,
    ) : Screen

    @Serializable
    data class MemberProfile(
        val groupId: Long,
        val userId: Long,
    ) : Screen

    @Serializable
    data class TransferOwnership(
        val groupId: Long,
    ) : Screen

    @Serializable
    data object Journal : Screen

    @Serializable
    data class JournalEntry(
        val entryId: Long = 0L,
    ) : Screen

    @Serializable
    data object PolaroidCamera : Screen

    @Serializable
    data class AvatarCrop(
        val source: String,
    ) : Screen
}
