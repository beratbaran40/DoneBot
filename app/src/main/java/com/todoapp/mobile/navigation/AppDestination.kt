package com.todoapp.mobile.navigation

import com.todoapp.mobile.R
import kotlin.reflect.KClass

/**
 * Resolves the route key for a [Screen] subtype. Reflection's [KClass.qualifiedName] returns
 * `null` only for anonymous / local classes (never the case for serializable navigation
 * destinations); fail loudly if a misconfigured target sneaks in.
 */
private fun KClass<out Screen>.requiredRoute(): String = requireNotNull(qualifiedName) {
    "$simpleName must have a qualifiedName to be used as a navigation route"
}

sealed class AppDestination(
    val title: Int,
    val route: String,
    val icon: Int?,
    val selectedIcon: Int?,
    val hasInfoDialog: Boolean = false,
) {
    data object Home : AppDestination(
        title = R.string.navbar_home_screen_page_name,
        route = Screen.Home::class.requiredRoute(),
        icon = R.drawable.ic_home,
        selectedIcon = R.drawable.ic_selected_home,
    )

    data object Calendar : AppDestination(
        title = R.string.navbar_calendar_screen_page_name,
        route = Screen.Calendar::class.requiredRoute(),
        icon = R.drawable.ic_calendar,
        selectedIcon = R.drawable.ic_selected_calendar,
        hasInfoDialog = true,
    )

    data object Activity : AppDestination(
        title = R.string.navbar_statistic_screen_page_name,
        route = Screen.Activity::class.requiredRoute(),
        icon = R.drawable.ic_statistic,
        selectedIcon = R.drawable.ic_selected_statistic,
        hasInfoDialog = true,
    )

    data object Chat : AppDestination(
        title = R.string.navbar_chat_screen_page_name,
        route = Screen.Chat::class.requiredRoute(),
        icon = R.drawable.ic_chat,
        selectedIcon = R.drawable.ic_selected_chat,
        hasInfoDialog = true,
    )

    data object Settings : AppDestination(
        title = R.string.settings,
        route = Screen.Settings::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object PomodoroAddTimer : AppDestination(
        title = R.string.add_timer,
        route = Screen.AddPomodoroTimer::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object Task : AppDestination(
        title = R.string.task_details,
        route = Screen.Task::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object SecretMode : AppDestination(
        title = R.string.secret_mode_settings,
        route = Screen.SecretMode::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object PlanYourDay : AppDestination(
        title = R.string.plan_your_day,
        route = Screen.PlanYourDay::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object Groups : AppDestination(
        title = R.string.groups,
        route = Screen.Groups::class.requiredRoute(),
        icon = R.drawable.ic_groups,
        selectedIcon = R.drawable.ic_selected_groups,
        hasInfoDialog = true,
    )

    data object CreateNewGroup : AppDestination(
        title = R.string.new_group,
        route = Screen.CreateNewGroup::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object FilteredTasks : AppDestination(
        title = R.string.filtered_tasks_title,
        route = Screen.FilteredTasks::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object Search : AppDestination(
        title = R.string.search,
        route = Screen.Search::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object PomodoroLaunch : AppDestination(
        title = R.string.add_timer,
        route = Screen.PomodoroLaunch::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object PomodoroSummary : AppDestination(
        title = R.string.pomodoro_summary,
        route = Screen.PomodoroSummary::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object GroupDetail : AppDestination(
        title = R.string.group_detail,
        route = Screen.GroupDetail::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object GroupSettings : AppDestination(
        title = R.string.group_settings,
        route = Screen.GroupSettings::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object InviteMember : AppDestination(
        title = R.string.invite_member,
        route = Screen.InviteMember::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object ManageMembers : AppDestination(
        title = R.string.manage_members,
        route = Screen.ManageMembers::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object GroupTaskDetail : AppDestination(
        title = R.string.group_task_detail,
        route = Screen.GroupTaskDetail::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object MemberProfile : AppDestination(
        title = R.string.member_profile,
        route = Screen.MemberProfile::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object TransferOwnership : AppDestination(
        title = R.string.transfer_ownership,
        route = Screen.TransferOwnership::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object Profile : AppDestination(
        title = R.string.profile,
        route = Screen.Profile::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object ChangePassword : AppDestination(
        title = R.string.change_password,
        route = Screen.ChangePassword::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
    )

    data object ResetPassword : AppDestination(
        title = R.string.reset_password,
        route = Screen.ResetPassword::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
    )

    data object Notifications : AppDestination(
        title = R.string.notifications_title,
        route = Screen.Notifications::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object Invitations : AppDestination(
        title = R.string.invitations_title,
        route = Screen.Invitations::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object AlarmSounds : AppDestination(
        title = R.string.alarm_sounds,
        route = Screen.AlarmSounds::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object Journal : AppDestination(
        title = R.string.nav_journal_title,
        route = Screen.Journal::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object JournalEntry : AppDestination(
        title = R.string.journal_entry_screen_title,
        route = Screen.JournalEntry::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
        hasInfoDialog = true,
    )

    data object PolaroidCamera : AppDestination(
        title = R.string.polaroid_camera_screen_title,
        route = Screen.PolaroidCamera::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
    )

    data object AvatarCrop : AppDestination(
        title = R.string.avatar_crop_screen_title,
        route = Screen.AvatarCrop::class.requiredRoute(),
        icon = null,
        selectedIcon = null,
    )

    companion object {
        val bottomBarItems = listOf(Home, Groups, Chat, Calendar, Activity)
        val topBarItems =
            listOf(
                Home,
                Calendar,
                Activity,
                Chat,
                PomodoroAddTimer,
                Settings,
                Task,
                SecretMode,
                PlanYourDay,
                Groups,
                CreateNewGroup,
                FilteredTasks,
                Search,
                PomodoroLaunch,
                PomodoroSummary,
                GroupDetail,
                GroupSettings,
                InviteMember,
                ManageMembers,
                GroupTaskDetail,
                MemberProfile,
                TransferOwnership,
                Profile,
                ChangePassword,
                ResetPassword,
                Notifications,
                Invitations,
                AlarmSounds,
                Journal,
                // JournalEntry, PolaroidCamera and AvatarCrop intentionally NOT listed — they hide
                // the topbar so they can render their own floating chrome over a full-bleed surface
                // (paper background / skeuomorphic camera body / crop surface). Keeping the
                // AppDestination objects in case future surfaces need their title resources.
            )
    }
}

fun bottomBarAppDestinationFromRoute(route: String?) = AppDestination.bottomBarItems.firstOrNull { it.route == route }

fun appDestinationFromRoute(route: String?) = AppDestination.topBarItems.firstOrNull { it.route == route }
