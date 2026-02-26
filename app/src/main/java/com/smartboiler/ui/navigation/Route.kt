package com.smartboiler.ui.navigation

/**
 * All navigation routes in the app, grouped logically.
 */
sealed class Route(val route: String) {
    // Onboarding
    data object Welcome : Route("onboarding/welcome")
    data object BoilerSetup : Route("onboarding/boiler_setup")
    data object LocationSetup : Route("onboarding/location")
    data object BaselineSetup : Route("onboarding/baselines")

    // Main
    data object Home : Route("home")
    data object PlanShower : Route("plan_shower")
    data object ScheduleShower : Route("schedule_shower")
    data object RecurringSchedules : Route("recurring_schedules")
    data object History : Route("history")
    data object Settings : Route("settings")
    data object DeviceSetup : Route("device_setup")
    data object Feedback : Route("feedback/{scheduleId}") {
        fun withScheduleId(id: Long) = "feedback/$id"
    }
}
