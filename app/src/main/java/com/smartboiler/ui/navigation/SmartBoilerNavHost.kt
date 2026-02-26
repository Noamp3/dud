package com.smartboiler.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartboiler.ui.feedback.FeedbackScreen
import com.smartboiler.ui.feedback.FeedbackViewModel
import com.smartboiler.ui.device.DeviceSetupScreen
import com.smartboiler.ui.device.DeviceSetupViewModel
import com.smartboiler.ui.history.HistoryScreen
import com.smartboiler.ui.history.HistoryViewModel
import com.smartboiler.ui.home.HomeScreen
import com.smartboiler.ui.home.HomeViewModel
import com.smartboiler.ui.onboarding.BaselineSetupScreen
import com.smartboiler.ui.onboarding.BoilerSetupScreen
import com.smartboiler.ui.onboarding.LocationSetupScreen
import com.smartboiler.ui.onboarding.OnboardingViewModel
import com.smartboiler.ui.onboarding.WelcomeScreen
import com.smartboiler.ui.plan.PlanScreen
import com.smartboiler.ui.plan.PlanViewModel
import com.smartboiler.ui.recurring.RecurringSchedulesScreen
import com.smartboiler.ui.recurring.RecurringSchedulesViewModel
import com.smartboiler.ui.schedule.ScheduleShowerScreen
import com.smartboiler.ui.schedule.ScheduleViewModel
import com.smartboiler.ui.settings.SettingsScreen
import com.smartboiler.ui.settings.SettingsViewModel
import java.time.LocalDate

private const val PREFILL_PEOPLE = "prefill_people"
private const val PREFILL_DATE = "prefill_date"
private const val PREFILL_HOUR = "prefill_hour"
private const val PREFILL_MINUTE = "prefill_minute"

@Composable
fun SmartBoilerNavHost() {
    val navController = rememberNavController()

    // Share the VM across onboarding screens (scoped to the activity/navGraph)
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingState = onboardingViewModel.uiState

    // Determine start destination based on onboarding completion
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val isComplete = onboardingViewModel.checkOnboardingStatus()
        startDestination = if (isComplete) Route.Home.route else Route.Welcome.route
    }

    // Watch for onboarding completion → navigate to Home
    LaunchedEffect(onboardingState.onboardingComplete) {
        if (onboardingState.onboardingComplete) {
            navController.navigate(Route.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Show loading while determining start destination
    if (startDestination == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = startDestination!!,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
    ) {
        // --- Onboarding ---
        composable(Route.Welcome.route) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Route.BoilerSetup.route) },
            )
        }

        composable(Route.BoilerSetup.route) {
            BoilerSetupScreen(
                uiState = onboardingState,
                onCapacityChange = onboardingViewModel::updateCapacity,
                onPowerChange = onboardingViewModel::updateHeatingPower,
                onTempChange = onboardingViewModel::updateDesiredTemp,
                onNext = { navController.navigate(Route.LocationSetup.route) },
            )
        }

        composable(Route.LocationSetup.route) {
            LocationSetupScreen(
                uiState = onboardingState,
                onLocationDetected = onboardingViewModel::updateLocation,
                onCityManualUpdate = onboardingViewModel::updateCityManually,
                onLocationLoading = onboardingViewModel::setLocationLoading,
                onLocationError = onboardingViewModel::setLocationError,
                onNext = { navController.navigate(Route.BaselineSetup.route) },
            )
        }

        composable(Route.BaselineSetup.route) {
            BaselineSetupScreen(
                uiState = onboardingState,
                onSunnyChange = onboardingViewModel::updateSunnyMinutes,
                onPartlyCloudyChange = onboardingViewModel::updatePartlyCloudyMinutes,
                onCloudyChange = onboardingViewModel::updateCloudyMinutes,
                onFinish = onboardingViewModel::completeOnboarding,
            )
        }

        // --- Main App ---
        composable(Route.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                uiState = homeViewModel.uiState,
                onScheduleShower = { navController.navigate(Route.ScheduleShower.route) },
                onPlan = { navController.navigate(Route.PlanShower.route) },
                onShowerNow = homeViewModel::startOnePersonShowerNow,
                onSettings = { navController.navigate(Route.Settings.route) },
                onHistory = { navController.navigate(Route.History.route) },
                onFeedback = { scheduleId ->
                    navController.navigate(Route.Feedback.withScheduleId(scheduleId))
                },
            )
        }

        composable(Route.ScheduleShower.route) {
            val scheduleViewModel: ScheduleViewModel = hiltViewModel()

            val prefillPeople = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<Int>(PREFILL_PEOPLE)
            val prefillDate = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>(PREFILL_DATE)
            val prefillHour = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<Int>(PREFILL_HOUR)
            val prefillMinute = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<Int>(PREFILL_MINUTE)

            LaunchedEffect(prefillPeople, prefillDate, prefillHour, prefillMinute) {
                if (prefillPeople != null &&
                    prefillDate != null &&
                    prefillHour != null &&
                    prefillMinute != null
                ) {
                    runCatching { LocalDate.parse(prefillDate) }
                        .onSuccess { parsedDate ->
                            scheduleViewModel.applyPrefill(
                                peopleCount = prefillPeople,
                                date = parsedDate,
                                hour = prefillHour,
                                minute = prefillMinute,
                            )
                        }

                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.remove<Int>(PREFILL_PEOPLE)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.remove<String>(PREFILL_DATE)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.remove<Int>(PREFILL_HOUR)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.remove<Int>(PREFILL_MINUTE)
                }
            }

            ScheduleShowerScreen(
                uiState = scheduleViewModel.uiState,
                onPeopleCountChange = scheduleViewModel::updatePeopleCount,
                onDateChange = scheduleViewModel::updateDate,
                onRecurringChange = scheduleViewModel::updateRecurring,
                onRecurringDayChange = scheduleViewModel::updateRecurringDay,
                onTimeChange = scheduleViewModel::updateTime,
                onConfirm = scheduleViewModel::confirmSchedule,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Route.PlanShower.route) {
            val planViewModel: PlanViewModel = hiltViewModel()
            PlanScreen(
                uiState = planViewModel.uiState,
                onDesiredTempChange = planViewModel::updateDesiredTemp,
                onShowersCountChange = planViewModel::updateShowersCount,
                onDateChange = planViewModel::updateDate,
                onTimeChange = planViewModel::updateTime,
                onUsePlan = {
                    val planState = planViewModel.uiState
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        PREFILL_PEOPLE,
                        planState.showersCount,
                    )
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        PREFILL_DATE,
                        planState.selectedDate.toString(),
                    )
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        PREFILL_HOUR,
                        planState.selectedHour,
                    )
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        PREFILL_MINUTE,
                        planState.selectedMinute,
                    )
                    navController.navigate(Route.ScheduleShower.route)
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Route.Feedback.route,
            arguments = listOf(navArgument("scheduleId") { type = NavType.LongType }),
        ) {
            val feedbackViewModel: FeedbackViewModel = hiltViewModel()
            FeedbackScreen(
                uiState = feedbackViewModel.uiState,
                onSubmit = feedbackViewModel::submitFeedback,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Route.History.route) {
            val historyViewModel: HistoryViewModel = hiltViewModel()
            HistoryScreen(
                uiState = historyViewModel.uiState,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Route.Settings.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                uiState = settingsViewModel.uiState,
                onUpdateBoilerSize = settingsViewModel::updateBoilerSize,
                onUpdateHeatingPower = settingsViewModel::updateHeatingPower,
                onUpdateTargetTemp = settingsViewModel::updateTargetTemp,
                onSave = settingsViewModel::save,
                onManageRecurring = { navController.navigate(Route.RecurringSchedules.route) },
                onDeviceSetup = { navController.navigate(Route.DeviceSetup.route) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Route.RecurringSchedules.route) {
            val recurringViewModel: RecurringSchedulesViewModel = hiltViewModel()
            RecurringSchedulesScreen(
                uiState = recurringViewModel.uiState,
                onToggleEnabled = recurringViewModel::setEnabled,
                onDelete = recurringViewModel::delete,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Route.DeviceSetup.route) {
            val deviceViewModel: DeviceSetupViewModel = hiltViewModel()
            DeviceSetupScreen(
                uiState = deviceViewModel.uiState,
                onTokenChange = deviceViewModel::updateToken,
                onSaveToken = deviceViewModel::saveToken,
                onDiscover = deviceViewModel::discoverDevices,
                onSelect = deviceViewModel::selectDevice,
                onTestToggle = deviceViewModel::testToggle,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
