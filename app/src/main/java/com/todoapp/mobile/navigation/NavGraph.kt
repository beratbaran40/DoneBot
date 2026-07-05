package com.todoapp.mobile.navigation

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.todoapp.mobile.LocalNavController
import com.todoapp.mobile.LocalWindowSizeClass
import com.todoapp.mobile.MainActivity
import com.todoapp.mobile.MainViewModel
import com.todoapp.mobile.ui.activity.ActivityScreen
import com.todoapp.mobile.ui.activity.ActivityViewModel
import com.todoapp.mobile.ui.addpomodorotimer.AddPomodoroTimerScreen
import com.todoapp.mobile.ui.addpomodorotimer.AddPomodoroTimerViewModel
import com.todoapp.mobile.ui.auth.AuthFormMaxWidth
import com.todoapp.mobile.ui.banner.BannerOverlay
import com.todoapp.mobile.ui.banner.BannerViewModel
import com.todoapp.mobile.ui.calendar.CalendarScreen
import com.todoapp.mobile.ui.calendar.CalendarViewModel
import com.todoapp.mobile.ui.changepassword.ChangePasswordScreen
import com.todoapp.mobile.ui.changepassword.ChangePasswordViewModel
import com.todoapp.mobile.ui.chat.ChatScreen
import com.todoapp.mobile.ui.chat.ChatViewModel
import com.todoapp.mobile.ui.common.ResponsiveContainer
import com.todoapp.mobile.ui.common.ScreenInfoDialog
import com.todoapp.mobile.ui.creationhub.CreationHubScreen
import com.todoapp.mobile.ui.creationhub.CreationHubViewModel
import com.todoapp.mobile.ui.details.DetailsScreen
import com.todoapp.mobile.ui.details.DetailsViewModel
import com.todoapp.mobile.ui.filteredtasks.FilteredTasksScreen
import com.todoapp.mobile.ui.filteredtasks.FilteredTasksViewModel
import com.todoapp.mobile.ui.forgotpassword.ForgotPasswordScreen
import com.todoapp.mobile.ui.forgotpassword.ForgotPasswordViewModel
import com.todoapp.mobile.ui.groups.GroupScreen
import com.todoapp.mobile.ui.groups.GroupsTwoPane
import com.todoapp.mobile.ui.groups.GroupsViewModel
import com.todoapp.mobile.ui.groups.createnewgroup.CreateNewGroupScreen
import com.todoapp.mobile.ui.groups.createnewgroup.CreateNewGroupViewModel
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailScreen
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailViewModel
import com.todoapp.mobile.ui.groups.groupsettings.GroupSettingsScreen
import com.todoapp.mobile.ui.groups.groupsettings.GroupSettingsViewModel
import com.todoapp.mobile.ui.groups.grouptaskdetail.GroupTaskDetailScreen
import com.todoapp.mobile.ui.groups.invitemember.InviteMemberScreen
import com.todoapp.mobile.ui.groups.invitemember.InviteMemberViewModel
import com.todoapp.mobile.ui.groups.managemembers.ManageMembersScreen
import com.todoapp.mobile.ui.groups.managemembers.ManageMembersViewModel
import com.todoapp.mobile.ui.groups.memberprofile.MemberProfileScreen
import com.todoapp.mobile.ui.groups.memberprofile.MemberProfileViewModel
import com.todoapp.mobile.ui.groups.transferownership.TransferOwnershipScreen
import com.todoapp.mobile.ui.groups.transferownership.TransferOwnershipViewModel
import com.todoapp.mobile.ui.home.HomeScreen
import com.todoapp.mobile.ui.home.HomeViewModel
import com.todoapp.mobile.ui.journal.JournalScreen
import com.todoapp.mobile.ui.journal.JournalViewModel
import com.todoapp.mobile.ui.journal.camera.POLAROID_PHOTO_RESULT_KEY
import com.todoapp.mobile.ui.journal.camera.PolaroidCameraScreen
import com.todoapp.mobile.ui.journal.camera.PolaroidCameraViewModel
import com.todoapp.mobile.ui.journal.entry.JournalEntryContract
import com.todoapp.mobile.ui.journal.entry.JournalEntryScreen
import com.todoapp.mobile.ui.journal.entry.JournalEntryViewModel
import com.todoapp.mobile.ui.login.LoginScreen
import com.todoapp.mobile.ui.login.LoginViewModel
import com.todoapp.mobile.ui.onboarding.OnboardingScreen
import com.todoapp.mobile.ui.onboarding.OnboardingViewModel
import com.todoapp.mobile.ui.planyourday.PlanYourDayScreen
import com.todoapp.mobile.ui.planyourday.PlanYourDayViewModel
import com.todoapp.mobile.ui.pomodoro.PomodoroScreen
import com.todoapp.mobile.ui.pomodoro.PomodoroViewModel
import com.todoapp.mobile.ui.pomodorolaunch.PomodoroLaunchScreen
import com.todoapp.mobile.ui.pomodorolaunch.PomodoroLaunchViewModel
import com.todoapp.mobile.ui.pomodorosummary.PomodoroSummaryScreen
import com.todoapp.mobile.ui.pomodorosummary.PomodoroSummaryViewModel
import com.todoapp.mobile.ui.profile.avatarcrop.AVATAR_CROP_RESULT_KEY
import com.todoapp.mobile.ui.profile.avatarcrop.AvatarCropScreen
import com.todoapp.mobile.ui.profile.avatarcrop.AvatarCropViewModel
import com.todoapp.mobile.ui.register.RegisterScreen
import com.todoapp.mobile.ui.register.RegisterViewModel
import com.todoapp.mobile.ui.resetpassword.ResetPasswordScreen
import com.todoapp.mobile.ui.resetpassword.ResetPasswordViewModel
import com.todoapp.mobile.ui.search.SearchScreen
import com.todoapp.mobile.ui.search.SearchViewModel
import com.todoapp.mobile.ui.settings.SecretModeSettingsScreen
import com.todoapp.mobile.ui.settings.SettingsContract
import com.todoapp.mobile.ui.settings.SettingsScreen
import com.todoapp.mobile.ui.settings.SettingsViewModel
import com.todoapp.mobile.ui.settings.rememberDataExportSaver
import com.todoapp.mobile.ui.splash.TDSplashScreen
import com.todoapp.mobile.ui.topbar.ShowTopBar
import com.todoapp.mobile.ui.topbar.TopBarViewModel
import com.todoapp.mobile.ui.webview.WebViewScreen
import com.todoapp.mobile.ui.webview.WebViewViewModel
import com.todoapp.uikit.extensions.collectWithLifecycle
import com.todoapp.uikit.theme.TDTheme
import kotlinx.coroutines.flow.Flow

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: Screen,
    topBarViewModel: TopBarViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 6 } },
        exitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 6 } },
        popEnterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 6 } },
        popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { it / 6 } },
    ) {
        composable<Screen.Onboarding> {
            val viewModel: OnboardingViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val navEffect = viewModel.navEffect
            OnboardingScreen(
                uiState = uiState,
                onAction = viewModel::onAction,
            )
            NavigationEffectController(navEffect)
        }
        composable<Screen.Home> {
            val viewModel: HomeViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val uiEffect = viewModel.uiEffect
            val navEffect = viewModel.navEffect
            ResponsiveContainer {
                HomeScreen(
                    uiState = uiState,
                    uiEffect = uiEffect,
                    onAction = viewModel::onAction,
                )
            }
            NavigationEffectController(navEffect)
        }
        composable<Screen.CreationHub> {
            val viewModel: CreationHubViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                CreationHubScreen(
                    state = state,
                    effect = viewModel.effect,
                    onAction = viewModel::onAction,
                )
            }
        }
        composable<Screen.Calendar> {
            val viewModel: CalendarViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            CalendarScreen(
                uiState = uiState,
                uiEffect = viewModel.effect,
                onAction = viewModel::onAction,
            )
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.calendar_info_title,
                descriptionRes = com.todoapp.mobile.R.string.calendar_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.calendar_info_bullet_1,
                    com.todoapp.mobile.R.string.calendar_info_bullet_2,
                    com.todoapp.mobile.R.string.calendar_info_bullet_3,
                ),
            )
        }
        composable<Screen.Activity> {
            val viewModel: ActivityViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                ActivityScreen(
                    uiState = uiState,
                    onAction = viewModel::onAction,
                )
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.activity_info_title,
                descriptionRes = com.todoapp.mobile.R.string.activity_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.activity_info_bullet_1,
                    com.todoapp.mobile.R.string.activity_info_bullet_2,
                    com.todoapp.mobile.R.string.activity_info_bullet_3,
                ),
            )
        }

        composable<Screen.Chat> {
            val viewModel: ChatViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                ChatScreen(
                    uiState = uiState,
                    uiEffect = viewModel.uiEffect,
                    onAction = viewModel::onAction,
                )
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.chat_info_title,
                descriptionRes = com.todoapp.mobile.R.string.chat_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.chat_info_bullet_1,
                    com.todoapp.mobile.R.string.chat_info_bullet_2,
                    com.todoapp.mobile.R.string.chat_info_bullet_3,
                    com.todoapp.mobile.R.string.chat_info_bullet_4,
                ),
            )
        }

        composable<Screen.Settings> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val saveExport = rememberDataExportSaver()
            NavigationEffectController(viewModel.navEffect)
            viewModel.uiEffect.collectWithLifecycle { effect ->
                when (effect) {
                    is SettingsContract.UiEffect.ShowToast ->
                        android.widget.Toast.makeText(context, effect.message, android.widget.Toast.LENGTH_SHORT).show()
                    is SettingsContract.UiEffect.SaveDataExport -> saveExport(effect.json)
                    SettingsContract.UiEffect.RecreateActivity -> {
                        val activity = context as? Activity ?: return@collectWithLifecycle
                        if (activity is MainActivity) MainActivity.suppressNextTransition.set(true)
                        activity.recreate()
                    }
                    is SettingsContract.UiEffect.ApplyLocale -> {
                        val locales =
                            androidx.core.os.LocaleListCompat
                                .forLanguageTags(effect.tag)
                        if (Build.VERSION.SDK_INT >= 33) {
                            // Android 13+: platform LocaleManager applies process-wide without full recreate.
                            val lm = context.getSystemService(android.app.LocaleManager::class.java)
                            lm?.applicationLocales = android.os.LocaleList.forLanguageTags(effect.tag)
                            androidx.appcompat.app.AppCompatDelegate
                                .setApplicationLocales(locales)
                        } else {
                            // Pre-33: AppCompat routes through the delegate and auto-recreates.
                            androidx.appcompat.app.AppCompatDelegate
                                .setApplicationLocales(locales)
                        }
                    }
                }
            }
            ResponsiveContainer {
                SettingsScreen(
                    uiState = uiState,
                    onAction = viewModel::onAction,
                    onCheckPermissions = { viewModel.checkPermission(context) },
                    onDismissPermission = viewModel::dismissPermission,
                )
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.settings_info_title,
                descriptionRes = com.todoapp.mobile.R.string.settings_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.settings_info_bullet_1,
                    com.todoapp.mobile.R.string.settings_info_bullet_2,
                    com.todoapp.mobile.R.string.settings_info_bullet_3,
                ),
            )
        }

        composable<Screen.SecretMode> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                SecretModeSettingsScreen(
                    uiState = uiState,
                    onAction = viewModel::onAction,
                    uiEffect = viewModel.uiEffect,
                )
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.secret_mode_info_title,
                descriptionRes = com.todoapp.mobile.R.string.secret_mode_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.secret_mode_info_bullet_1,
                    com.todoapp.mobile.R.string.secret_mode_info_bullet_2,
                    com.todoapp.mobile.R.string.secret_mode_info_bullet_3,
                ),
            )
        }

        composable<Screen.PlanYourDay> {
            val viewModel: PlanYourDayViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ResponsiveContainer {
                PlanYourDayScreen(
                    uiState = uiState,
                    uiEffect = viewModel.uiEffect,
                    onAction = viewModel::onAction,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.plan_your_day_info_title,
                descriptionRes = com.todoapp.mobile.R.string.plan_your_day_info_description,
            )
        }

        composable<Screen.PomodoroLaunch> {
            val viewModel: PomodoroLaunchViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                PomodoroLaunchScreen(
                    uiState = uiState,
                    onAction = viewModel::onAction,
                )
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.pomodoro_info_title,
                descriptionRes = com.todoapp.mobile.R.string.pomodoro_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.pomodoro_info_bullet_1,
                    com.todoapp.mobile.R.string.pomodoro_info_bullet_2,
                    com.todoapp.mobile.R.string.pomodoro_info_bullet_3,
                ),
            )
        }

        composable<Screen.AddPomodoroTimer> {
            val viewModel: AddPomodoroTimerViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                AddPomodoroTimerScreen(
                    uiState,
                    viewModel::onAction,
                )
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.add_timer_info_title,
                descriptionRes = com.todoapp.mobile.R.string.add_timer_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.add_timer_info_bullet_1,
                    com.todoapp.mobile.R.string.add_timer_info_bullet_2,
                    com.todoapp.mobile.R.string.add_timer_info_bullet_3,
                ),
            )
        }
        composable<Screen.Pomodoro> {
            val viewModel: PomodoroViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val uiEffect = viewModel.uiEffect
            NavigationEffectController(viewModel.navEffect)
            PomodoroScreen(
                uiState,
                uiEffect,
                viewModel::onAction,
            )
        }

        composable<Screen.Task> {
            val viewModel: DetailsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val uiEffect = viewModel.uiEffect
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                DetailsScreen(
                    uiState,
                    uiEffect,
                    viewModel::onAction,
                )
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.task_detail_info_title,
                descriptionRes = com.todoapp.mobile.R.string.task_detail_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.task_detail_info_bullet_1,
                    com.todoapp.mobile.R.string.task_detail_info_bullet_2,
                    com.todoapp.mobile.R.string.task_detail_info_bullet_3,
                ),
            )
        }
        composable<Screen.Register> {
            val viewModel: RegisterViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val uiEffect = viewModel.uiEffect
            NavigationEffectController(viewModel.navEffect)
            RegisterScreen(
                uiState = uiState,
                onAction = viewModel::onAction,
                uiEffect = uiEffect,
            )
        }

        composable<Screen.Login> {
            val viewModel: LoginViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val uiEffect = viewModel.uiEffect
            NavigationEffectController(viewModel.navEffect)
            LoginScreen(
                uiState = uiState,
                onAction = viewModel::onAction,
                uiEffect = uiEffect,
            )
        }

        composable<Screen.WebView> {
            val viewModel: WebViewViewModel = hiltViewModel()
            val uiEffect = viewModel.uiEffect
            NavigationEffectController(viewModel.navEffect)
            WebViewScreen(
                onAction = viewModel::onAction,
                uiEffect = uiEffect,
            )
        }

        composable<Screen.ForgotPassword> {
            val viewModel: ForgotPasswordViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ForgotPasswordScreen(
                uiState = uiState,
                uiEffect = viewModel.effect,
                onAction = viewModel::onAction,
            )
        }

        composable<Screen.PomodoroSummary> {
            val viewModel: PomodoroSummaryViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                PomodoroSummaryScreen(uiState = uiState, onAction = viewModel::onAction)
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.pomodoro_summary_info_title,
                descriptionRes = com.todoapp.mobile.R.string.pomodoro_summary_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.pomodoro_summary_info_bullet_1,
                    com.todoapp.mobile.R.string.pomodoro_summary_info_bullet_2,
                ),
            )
        }

        composable<Screen.CreateNewGroup> {
            val viewModel: CreateNewGroupViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                CreateNewGroupScreen(uiState, viewModel::onAction)
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.create_group_info_title,
                descriptionRes = com.todoapp.mobile.R.string.create_group_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.create_group_info_bullet_1,
                    com.todoapp.mobile.R.string.create_group_info_bullet_2,
                    com.todoapp.mobile.R.string.create_group_info_bullet_3,
                ),
            )
        }

        composable<Screen.Groups> {
            val viewModel: GroupsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val context = androidx.compose.ui.platform.LocalContext.current
            NavigationEffectController(viewModel.navEffect)
            androidx.compose.runtime.LaunchedEffect(viewModel) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        is com.todoapp.mobile.ui.groups.GroupsContract.UiEffect.ShowToast ->
                            android.widget.Toast.makeText(context, effect.message, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
            if (LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Expanded) {
                GroupsTwoPane(uiState = uiState, onAction = viewModel::onAction)
            } else {
                ResponsiveContainer {
                    GroupScreen(uiState, viewModel::onAction)
                }
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.groups_info_title,
                descriptionRes = com.todoapp.mobile.R.string.groups_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.groups_info_bullet_1,
                    com.todoapp.mobile.R.string.groups_info_bullet_2,
                    com.todoapp.mobile.R.string.groups_info_bullet_3,
                ),
            )
        }
        composable<Screen.FilteredTasks> {
            val viewModel: FilteredTasksViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                FilteredTasksScreen(
                    uiState = uiState,
                    uiEffect = viewModel.uiEffect,
                    onAction = viewModel::onAction,
                )
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.filtered_tasks_info_title,
                descriptionRes = com.todoapp.mobile.R.string.filtered_tasks_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.filtered_tasks_info_bullet_1,
                    com.todoapp.mobile.R.string.filtered_tasks_info_bullet_2,
                ),
            )
        }

        composable<Screen.Search>(
            enterTransition = { slideInVertically { -it } + fadeIn() },
            exitTransition = { slideOutVertically { -it } + fadeOut() },
            popEnterTransition = { slideInVertically { -it } + fadeIn() },
            popExitTransition = { slideOutVertically { -it } + fadeOut() },
        ) {
            val viewModel: SearchViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val query by viewModel.query.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                SearchScreen(
                    uiState = uiState,
                    query = query,
                    uiEffect = viewModel.uiEffect,
                    onAction = viewModel::onAction,
                )
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.search_info_title,
                descriptionRes = com.todoapp.mobile.R.string.search_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.search_info_bullet_1,
                    com.todoapp.mobile.R.string.search_info_bullet_2,
                    com.todoapp.mobile.R.string.search_info_bullet_3,
                ),
            )
        }
        composable<Screen.Profile> {
            val viewModel: com.todoapp.mobile.ui.profile.ProfileViewModel = hiltViewModel()
            NavigationEffectController(viewModel.navEffect)
            // Cropped avatar path is handed back via savedStateHandle by the crop screen.
            LaunchedEffect(it) {
                it.savedStateHandle.getStateFlow<String?>(AVATAR_CROP_RESULT_KEY, null)
                    .collect { path ->
                        if (path != null) {
                            viewModel.onAction(
                                com.todoapp.mobile.ui.profile.ProfileContract.UiAction.OnAvatarCropped(path),
                            )
                            it.savedStateHandle[AVATAR_CROP_RESULT_KEY] = null
                        }
                    }
            }
            ResponsiveContainer {
                com.todoapp.mobile.ui.profile
                    .ProfileScreen(viewModel = viewModel)
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.profile_info_title,
                descriptionRes = com.todoapp.mobile.R.string.profile_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.profile_info_bullet_1,
                    com.todoapp.mobile.R.string.profile_info_bullet_2,
                ),
            )
        }

        composable<Screen.Notifications> {
            val viewModel: com.todoapp.mobile.ui.notifications.NotificationsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                com.todoapp.mobile.ui.notifications.NotificationsScreen(
                    uiState = uiState,
                    uiEffect = viewModel.effect,
                    onAction = viewModel::onAction,
                )
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.notifications_info_title,
                descriptionRes = com.todoapp.mobile.R.string.notifications_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.notifications_info_bullet_1,
                    com.todoapp.mobile.R.string.notifications_info_bullet_2,
                ),
            )
        }

        composable<Screen.Invitations> {
            val viewModel: com.todoapp.mobile.ui.invitations.InvitationsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                com.todoapp.mobile.ui.invitations.InvitationsScreen(
                    uiState = uiState,
                    uiEffect = viewModel.effect,
                    onAction = viewModel::onAction,
                )
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.invitations_info_title,
                descriptionRes = com.todoapp.mobile.R.string.invitations_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.invitations_info_bullet_1,
                    com.todoapp.mobile.R.string.invitations_info_bullet_2,
                ),
            )
        }

        composable<Screen.AlarmSounds> {
            val viewModel: com.todoapp.mobile.ui.alarmsounds.AlarmSoundsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                com.todoapp.mobile.ui.alarmsounds.AlarmSoundsScreen(
                    uiState = uiState,
                    onAction = viewModel::onAction,
                )
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.alarm_sounds_info_title,
                descriptionRes = com.todoapp.mobile.R.string.alarm_sounds_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.alarm_sounds_info_bullet_1,
                    com.todoapp.mobile.R.string.alarm_sounds_info_bullet_2,
                ),
            )
        }

        composable<Screen.Licenses> {
            ResponsiveContainer {
                com.todoapp.mobile.ui.licenses.LicensesScreen()
            }
        }
        composable<Screen.BlockedUsers> {
            ResponsiveContainer {
                com.todoapp.mobile.ui.blockedusers.BlockedUsersScreen()
            }
        }

        composable<Screen.ChangePassword> {
            val viewModel: ChangePasswordViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer(maxWidth = AuthFormMaxWidth) {
                ChangePasswordScreen(
                    uiState = uiState,
                    uiEffect = viewModel.effect,
                    onAction = viewModel::onAction,
                )
            }
        }

        composable<Screen.ResetPassword> {
            val viewModel: ResetPasswordViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer(maxWidth = AuthFormMaxWidth) {
                ResetPasswordScreen(
                    uiState = uiState,
                    uiEffect = viewModel.effect,
                    onAction = viewModel::onAction,
                )
            }
        }

        composable<Screen.GroupDetail> {
            val viewModel: GroupDetailViewModel = hiltViewModel()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                GroupDetailScreen(viewModel = viewModel)
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.group_detail_info_title,
                descriptionRes = com.todoapp.mobile.R.string.group_detail_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.group_detail_info_bullet_1,
                    com.todoapp.mobile.R.string.group_detail_info_bullet_2,
                    com.todoapp.mobile.R.string.group_detail_info_bullet_3,
                ),
            )
        }

        composable<Screen.GroupSettings> {
            val viewModel: GroupSettingsViewModel = hiltViewModel()
            NavigationEffectController(viewModel.navEffect)
            // Cropped group-avatar path is handed back via savedStateHandle by the crop screen.
            LaunchedEffect(it) {
                it.savedStateHandle.getStateFlow<String?>(AVATAR_CROP_RESULT_KEY, null)
                    .collect { path ->
                        if (path != null) {
                            viewModel.onAction(
                                com.todoapp.mobile.ui.groups.groupsettings
                                    .GroupSettingsContract.UiAction.OnAvatarCropped(path),
                            )
                            it.savedStateHandle[AVATAR_CROP_RESULT_KEY] = null
                        }
                    }
            }
            ResponsiveContainer {
                GroupSettingsScreen(viewModel = viewModel)
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.group_settings_info_title,
                descriptionRes = com.todoapp.mobile.R.string.group_settings_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.group_settings_info_bullet_1,
                    com.todoapp.mobile.R.string.group_settings_info_bullet_2,
                    com.todoapp.mobile.R.string.group_settings_info_bullet_3,
                ),
            )
        }

        composable<Screen.InviteMember> {
            val viewModel: InviteMemberViewModel = hiltViewModel()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                InviteMemberScreen(viewModel = viewModel)
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.invite_member_info_title,
                descriptionRes = com.todoapp.mobile.R.string.invite_member_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.invite_member_info_bullet_1,
                    com.todoapp.mobile.R.string.invite_member_info_bullet_2,
                ),
            )
        }

        composable<Screen.ManageMembers> {
            val viewModel: ManageMembersViewModel = hiltViewModel()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                ManageMembersScreen(viewModel = viewModel)
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.manage_members_info_title,
                descriptionRes = com.todoapp.mobile.R.string.manage_members_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.manage_members_info_bullet_1,
                    com.todoapp.mobile.R.string.manage_members_info_bullet_2,
                ),
            )
        }

        composable<Screen.MemberProfile> {
            val viewModel: MemberProfileViewModel = hiltViewModel()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                MemberProfileScreen(viewModel = viewModel)
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.member_profile_info_title,
                descriptionRes = com.todoapp.mobile.R.string.member_profile_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.member_profile_info_bullet_1,
                    com.todoapp.mobile.R.string.member_profile_info_bullet_2,
                ),
            )
        }

        composable<Screen.GroupTaskDetail> {
            ResponsiveContainer {
                GroupTaskDetailScreen()
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.group_task_detail_info_title,
                descriptionRes = com.todoapp.mobile.R.string.group_task_detail_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.group_task_detail_info_bullet_1,
                    com.todoapp.mobile.R.string.group_task_detail_info_bullet_2,
                ),
            )
        }

        composable<Screen.TransferOwnership> {
            val viewModel: TransferOwnershipViewModel = hiltViewModel()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                TransferOwnershipScreen(viewModel = viewModel)
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.transfer_ownership_info_title,
                descriptionRes = com.todoapp.mobile.R.string.transfer_ownership_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.transfer_ownership_info_bullet_1,
                    com.todoapp.mobile.R.string.transfer_ownership_info_bullet_2,
                ),
            )
        }

        composable<Screen.Journal> {
            val viewModel: JournalViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            ResponsiveContainer {
                JournalScreen(
                    uiState = uiState,
                    uiEffect = viewModel.uiEffect,
                    onAction = viewModel::onAction,
                )
            }
            ScreenInfoDialog(
                infoClicks = topBarViewModel.infoClicks,
                titleRes = com.todoapp.mobile.R.string.journal_info_title,
                descriptionRes = com.todoapp.mobile.R.string.journal_info_description,
                bulletPointRes = listOf(
                    com.todoapp.mobile.R.string.journal_info_bullet_1,
                    com.todoapp.mobile.R.string.journal_info_bullet_2,
                    com.todoapp.mobile.R.string.journal_info_bullet_3,
                    com.todoapp.mobile.R.string.journal_info_bullet_4,
                ),
            )
        }

        composable<Screen.JournalEntry> {
            val viewModel: JournalEntryViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            NavigationEffectController(viewModel.navEffect)
            // Photo captured by the Polaroid camera is handed back via savedStateHandle.
            LaunchedEffect(it) {
                it.savedStateHandle.getStateFlow<String?>(POLAROID_PHOTO_RESULT_KEY, null)
                    .collect { path ->
                        if (path != null) {
                            viewModel.onAction(JournalEntryContract.UiAction.OnPhotoCapturedFromCamera(path))
                            it.savedStateHandle[POLAROID_PHOTO_RESULT_KEY] = null
                        }
                    }
            }
            JournalEntryScreen(
                uiState = uiState,
                uiEffect = viewModel.uiEffect,
                onAction = viewModel::onAction,
            )
            // Info dialog rendered inside JournalEntryScreen — topbar is hidden for this route.
        }

        composable<Screen.PolaroidCamera> {
            val viewModel: PolaroidCameraViewModel = hiltViewModel()
            PolaroidCameraScreen(
                uiEffect = viewModel.uiEffect,
                onAction = viewModel::onAction,
                onPhotoSaved = { path ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle?.set(POLAROID_PHOTO_RESULT_KEY, path)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable<Screen.AvatarCrop> {
            val viewModel: AvatarCropViewModel = hiltViewModel()
            AvatarCropScreen(
                source = it.toRoute<Screen.AvatarCrop>().source,
                uiEffect = viewModel.uiEffect,
                onAction = viewModel::onAction,
                onCropped = { path ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle?.set(AVATAR_CROP_RESULT_KEY, path)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun ToDoApp() {
    val bannerViewModel: BannerViewModel = hiltViewModel()
    val bannerState by bannerViewModel.uiState.collectAsStateWithLifecycle()
    val topBarViewModel: TopBarViewModel = hiltViewModel()
    val topBarState by topBarViewModel.uiState.collectAsStateWithLifecycle()
    val mainViewModel: MainViewModel = hiltViewModel()
    val isLoggedIn = mainViewModel.isLoggedIn
    var splashDone by rememberSaveable { mutableStateOf(false) }

    if (isLoggedIn == null || !splashDone) {
        TDSplashScreen(onAnimationComplete = { splashDone = true })
        return
    }

    val startDestination = remember { if (isLoggedIn) Screen.Home else Screen.Onboarding }
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val isCompactWidth = LocalWindowSizeClass.current.widthSizeClass == WindowWidthSizeClass.Compact
    // Bottom bar only for phones held in portrait; tablets (both orientations) and landscape get the rail.
    val useBottomBar = isPortrait && isCompactWidth

    Scaffold(
        modifier =
        Modifier
            .fillMaxSize()
            .imePadding(),
        // Material3 Scaffold's Surface paints containerColor over any background modifier; without this
        // it falls back to the default (light) colorScheme and leaks through the side gutters that the
        // centred ResponsiveContainer leaves on tablets / landscape. Set it to the themed background.
        containerColor = TDTheme.colors.background,
        bottomBar = { if (useBottomBar) TDBottomBar() },
        topBar = {
            Column {
                BannerOverlay(
                    bannerState,
                    bannerViewModel::onAction,
                    bannerViewModel.uiEffect,
                )
                NavigationEffectController(bannerViewModel.navEffect)
                ShowTopBar(bannerState.isBannerActivated, topBarViewModel::onAction, topBarState)
                NavigationEffectController(topBarViewModel.navEffect)
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Row(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!useBottomBar) TDNavigationRail()
            NavGraph(
                navController = LocalNavController.current,
                modifier =
                Modifier
                    .fillMaxSize()
                    .weight(1f),
                startDestination = startDestination,
                topBarViewModel = topBarViewModel,
            )
        }
    }
}

sealed interface NavigationEffect {
    data class Navigate(
        val route: Screen,
        val popUpTo: Screen? = null,
        val isInclusive: Boolean = false,
        val launchSingleTop: Boolean = false,
        val saveState: Boolean = false,
        val restoreState: Boolean = false,
    ) : NavigationEffect

    data class NavigateClearingBackstack(
        val route: Screen,
    ) : NavigationEffect

    data object Back : NavigationEffect
}

@Composable
fun NavigationEffectController(navEffect: Flow<NavigationEffect>) {
    val navController = LocalNavController.current
    navEffect.collectWithLifecycle { effect ->
        when (effect) {
            is NavigationEffect.Navigate -> {
                navController.navigate(effect.route) {
                    effect.popUpTo?.let {
                        popUpTo(it) {
                            inclusive = effect.isInclusive
                            saveState = effect.saveState
                        }
                    }
                    launchSingleTop = effect.launchSingleTop
                    restoreState = effect.restoreState
                }
            }

            is NavigationEffect.Back -> {
                navController.popBackStack()
            }

            is NavigationEffect.NavigateClearingBackstack -> {
                navController.navigate(effect.route) {
                    popUpTo(0)
                }
            }
        }
    }
}
