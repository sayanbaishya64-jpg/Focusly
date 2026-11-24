package com.focusly.app.navigation

sealed class FocuslyRoute(val route: String) {
    data object Splash : FocuslyRoute("splash")

    data object Welcome : FocuslyRoute("onboarding/welcome")
    data object StudentCategory : FocuslyRoute("onboarding/student_category")
    data object StudyGoals : FocuslyRoute("onboarding/study_goals")
    data object DailyTarget : FocuslyRoute("onboarding/daily_target")
    data object LanguageSelection : FocuslyRoute("onboarding/language")
    data object DistractionPreferences : FocuslyRoute("onboarding/distraction_preferences")

    data object Home : FocuslyRoute("main/home")
    data object Focus : FocuslyRoute("main/focus")
    data object Study : FocuslyRoute("main/study")
    data object Progress : FocuslyRoute("main/progress")
    data object Profile : FocuslyRoute("main/profile")
    data object Rewards : FocuslyRoute("main/rewards")
    data object DistractionBlocker : FocuslyRoute("settings/distraction_blocker")
}
