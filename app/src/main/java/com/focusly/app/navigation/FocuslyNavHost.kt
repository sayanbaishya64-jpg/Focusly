package com.focusly.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focusly.app.core.designsystem.theme.FocuslyColors
import com.focusly.app.feature.blocker.DistractionBlockerScreen
import com.focusly.app.feature.blocker.DistractionBlockerViewModel
import com.focusly.app.feature.focus.FocusScreen
import com.focusly.app.feature.focus.FocusViewModel
import com.focusly.app.feature.focus.components.FocusSessionRecoveryDialog
import com.focusly.app.feature.home.HomeScreen
import com.focusly.app.feature.home.HomeViewModel
import com.focusly.app.feature.onboarding.DailyTargetScreen
import com.focusly.app.feature.onboarding.LanguageSelectionScreen
import com.focusly.app.feature.onboarding.OnboardingViewModel
import com.focusly.app.feature.onboarding.PermissionExplanationScreen
import com.focusly.app.feature.onboarding.StudentCategoryScreen
import com.focusly.app.feature.onboarding.StudyGoalsScreen
import com.focusly.app.feature.onboarding.WelcomeScreen
import com.focusly.app.feature.progress.ProgressScreen
import com.focusly.app.feature.progress.ProgressViewModel
import com.focusly.app.feature.rewards.RewardsScreen
import com.focusly.app.feature.rewards.RewardsViewModel
import com.focusly.app.feature.shell.ProfileBackupViewModel
import com.focusly.app.feature.shell.ProfileShellScreen
import com.focusly.app.feature.splash.SplashScreen
import com.focusly.app.feature.study.StudyPlannerScreen
import com.focusly.app.feature.study.StudyPlannerViewModel

@Composable
fun FocuslyNavHost(
    onboardingCompleted: Boolean,
    studentName: String,
    onSaveStudentName: (String) -> Unit,
    onLanguageChanged: (String) -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val bottomTabRoutes = BottomNavTab.entries.map { it.route }.toSet()
    val showBottomBar = currentRoute in bottomTabRoutes

    val focusViewModel: FocusViewModel = hiltViewModel()
    val focusState by focusViewModel.uiState.collectAsStateWithLifecycle()

    val navigateToTab: (String) -> Unit = { targetRoute ->
        navController.navigate(targetRoute) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = FocuslyColors.PrimaryBackground,
        bottomBar = {
            if (showBottomBar) {
                FocuslyBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = navigateToTab
                )
            }
        }
    ) { innerPadding ->
        val onboardingViewModel: OnboardingViewModel = hiltViewModel()
        val onboardingState by onboardingViewModel.uiState.collectAsStateWithLifecycle()

        NavHost(
            navController = navController,
            startDestination = FocuslyRoute.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(FocuslyRoute.Splash.route) {
                SplashScreen(
                    onFinished = {
                        val nextRoute = if (onboardingCompleted) {
                            FocuslyRoute.Home.route
                        } else {
                            FocuslyRoute.Welcome.route
                        }
                        navController.navigate(nextRoute) {
                            popUpTo(FocuslyRoute.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(FocuslyRoute.Welcome.route) {
                WelcomeScreen(
                    onGetStarted = {
                        navController.navigate(FocuslyRoute.StudentCategory.route)
                    }
                )
            }

            composable(FocuslyRoute.StudentCategory.route) {
                StudentCategoryScreen(
                    selectedCategory = onboardingState.studentCategory,
                    onSelectCategory = onboardingViewModel::selectCategory,
                    onBack = { navController.popBackStack() },
                    onNext = {
                        onboardingViewModel.persistCategory()
                        navController.navigate(FocuslyRoute.StudyGoals.route)
                    }
                )
            }

            composable(FocuslyRoute.StudyGoals.route) {
                StudyGoalsScreen(
                    selectedGoals = onboardingState.studyGoals,
                    onToggleGoal = onboardingViewModel::toggleGoal,
                    onBack = { navController.popBackStack() },
                    onNext = {
                        onboardingViewModel.persistGoals()
                        navController.navigate(FocuslyRoute.DailyTarget.route)
                    }
                )
            }

            composable(FocuslyRoute.DailyTarget.route) {
                DailyTargetScreen(
                    selectedMinutes = onboardingState.dailyFocusTargetMinutes,
                    customMinutesInput = onboardingState.customMinutesInput,
                    isCustomSelected = onboardingState.isCustomTargetSelected,
                    onSelectPreset = onboardingViewModel::selectTargetPreset,
                    onCustomInputChange = onboardingViewModel::updateCustomMinutes,
                    onBack = { navController.popBackStack() },
                    onNext = {
                        onboardingViewModel.persistDailyTarget()
                        navController.navigate(FocuslyRoute.LanguageSelection.route)
                    }
                )
            }

            composable(FocuslyRoute.LanguageSelection.route) {
                LanguageSelectionScreen(
                    selectedLanguage = onboardingState.language,
                    onSelectLanguage = { lang ->
                        onboardingViewModel.selectLanguage(lang)
                        onLanguageChanged(lang.languageTag)
                    },
                    onBack = { navController.popBackStack() },
                    onNext = {
                        onboardingViewModel.persistLanguage()
                        navController.navigate(FocuslyRoute.DistractionPreferences.route)
                    }
                )
            }

            composable(FocuslyRoute.DistractionPreferences.route) {
                PermissionExplanationScreen(
                    selectedApps = onboardingState.selectedDistractionApps,
                    onToggleApp = onboardingViewModel::toggleDistractionApp,
                    onBack = { navController.popBackStack() },
                    onSkip = {
                        onboardingViewModel.completeOnboarding(skippedDistractions = true) {
                            navController.navigate(FocuslyRoute.Home.route) {
                                popUpTo(FocuslyRoute.Welcome.route) { inclusive = true }
                            }
                        }
                    },
                    onFinish = {
                        onboardingViewModel.completeOnboarding(skippedDistractions = false) {
                            navController.navigate(FocuslyRoute.Home.route) {
                                popUpTo(FocuslyRoute.Welcome.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(FocuslyRoute.Home.route) {
                val homeViewModel: HomeViewModel = hiltViewModel()
                val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()

                HomeScreen(
                    state = homeState,
                    onStartFocusClick = { navigateToTab(FocuslyRoute.Focus.route) },
                    onStartRecommendedFocus = { recommendedMinutes ->
                        focusViewModel.startSessionWithMinutes(
                            minutes = recommendedMinutes,
                            fromRecommendationId = homeState.recommendation.id
                        )
                        navigateToTab(FocuslyRoute.Focus.route)
                    },
                    onIgnoreRecommendation = homeViewModel::ignoreRecommendation,
                    onRestartShorterFromRecovery = { shorterMinutes ->
                        focusViewModel.restartShorterFromRecovery(shorterMinutes)
                        navigateToTab(FocuslyRoute.Focus.route)
                    },
                    onTakeRecoveryBreak = {
                        homeViewModel.dismissRecovery()
                        focusViewModel.resetCompletionScreen(switchToBreak = true)
                        navigateToTab(FocuslyRoute.Focus.route)
                    },
                    onDismissRecovery = homeViewModel::dismissRecovery,
                    onOpenBlockerSettings = {
                        navController.navigate(FocuslyRoute.DistractionBlocker.route)
                    },
                    onOpenRewards = {
                        navController.navigate(FocuslyRoute.Rewards.route)
                    },
                    onClaimMission = homeViewModel::claimDailyMission,
                    onNavigateToSettings = { navigateToTab(FocuslyRoute.Profile.route) },
                    onCreateQuickTask = homeViewModel::createQuickTask
                )
            }

            composable(FocuslyRoute.Focus.route) {
                FocusScreen(
                    state = focusState,
                    onSelectSessionType = focusViewModel::selectSessionType,
                    onSelectFocusMode = focusViewModel::selectFocusMode,
                    onSelectPreset = focusViewModel::selectPresetDuration,
                    onCustomDurationChange = focusViewModel::updateCustomDuration,
                    onSelectSubject = focusViewModel::selectSubject,
                    onSelectTask = focusViewModel::selectTask,
                    onApplyRecommendedDuration = focusViewModel::applyRecommendationDuration,
                    onIgnoreRecommendation = focusViewModel::ignoreRecommendation,
                    onRestartShorterFromRecovery = focusViewModel::restartShorterFromRecovery,
                    onDismissRecovery = focusViewModel::dismissRecoveryCard,
                    onOpenBlockerSettings = {
                        navController.navigate(FocuslyRoute.DistractionBlocker.route)
                    },
                    onStart = focusViewModel::startSession,
                    onPause = focusViewModel::pauseSession,
                    onResume = focusViewModel::resumeSession,
                    onEndEarly = focusViewModel::endSessionEarly,
                    onStartAnother = { focusViewModel.resetCompletionScreen(switchToBreak = false) },
                    onTakeBreak = { focusViewModel.resetCompletionScreen(switchToBreak = true) },
                    onReturnHome = {
                        focusViewModel.resetCompletionScreen(switchToBreak = false)
                        navigateToTab(FocuslyRoute.Home.route)
                    }
                )
            }

            composable(FocuslyRoute.Study.route) {
                val studyViewModel: StudyPlannerViewModel = hiltViewModel()
                val studyState by studyViewModel.uiState.collectAsStateWithLifecycle()

                StudyPlannerScreen(
                    state = studyState,
                    onSelectScheduleFilter = studyViewModel::selectScheduleFilter,
                    onSelectSubjectFilter = studyViewModel::selectSubjectFilter,
                    onCreateSubject = studyViewModel::createSubject,
                    onDeleteSubject = studyViewModel::deleteSubject,
                    onSaveTask = studyViewModel::saveTask,
                    onToggleTaskCompleted = studyViewModel::toggleTaskCompleted,
                    onRescheduleTask = studyViewModel::rescheduleTaskToDate,
                    onRescheduleAllOverdue = studyViewModel::rescheduleAllOverdueToToday,
                    onDeleteTask = studyViewModel::deleteTask,
                    onStartFocusForTask = { task ->
                        focusViewModel.prepareFocusForTask(task)
                        navigateToTab(FocuslyRoute.Focus.route)
                    }
                )
            }

            composable(FocuslyRoute.Progress.route) {
                val progressViewModel: ProgressViewModel = hiltViewModel()
                val progressState by progressViewModel.uiState.collectAsStateWithLifecycle()

                ProgressScreen(
                    state = progressState,
                    onSelectPeriod = progressViewModel::selectPeriod,
                    onStartRecommendedFocus = { recommendedMinutes ->
                        focusViewModel.startSessionWithMinutes(
                            minutes = recommendedMinutes,
                            fromRecommendationId = progressState.recommendation.id
                        )
                        navigateToTab(FocuslyRoute.Focus.route)
                    },
                    onIgnoreRecommendation = progressViewModel::ignoreRecommendation
                )
            }

            composable(FocuslyRoute.Profile.route) {
                val backupViewModel: ProfileBackupViewModel = hiltViewModel()
                val backupStatus by backupViewModel.status.collectAsStateWithLifecycle()

                ProfileShellScreen(
                    preferences = onboardingState.toPreferences(),
                    studentName = studentName,
                    backupStatus = backupStatus,
                    onSaveStudentName = onSaveStudentName,
                    onOpenBlockerSettings = {
                        navController.navigate(FocuslyRoute.DistractionBlocker.route)
                    },
                    onOpenRewards = {
                        navController.navigate(FocuslyRoute.Rewards.route)
                    },
                    onExportBackupUri = backupViewModel::exportBackupToUri,
                    onImportBackupUri = { uri ->
                        backupViewModel.importBackupFromUri(uri) {
                            onboardingViewModel.refreshFromPreferences()
                        }
                    },
                    onLanguageChange = { lang ->
                        onboardingViewModel.selectLanguage(lang)
                        onboardingViewModel.persistLanguage()
                        onLanguageChanged(lang.languageTag)
                    },
                    onResetOnboarding = {
                        onboardingViewModel.resetAllLocalData {
                            navController.navigate(FocuslyRoute.Welcome.route) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(FocuslyRoute.DistractionBlocker.route) {
                val blockerViewModel: DistractionBlockerViewModel = hiltViewModel()
                val blockerState by blockerViewModel.uiState.collectAsStateWithLifecycle()
                val testMessage by blockerViewModel.testStatusSummary.collectAsStateWithLifecycle()

                DistractionBlockerScreen(
                    state = blockerState,
                    testStatusMessage = testMessage,
                    onBack = { navController.popBackStack() },
                    onToggleEnabled = blockerViewModel::setEnabled,
                    onToggleApp = blockerViewModel::toggleApp,
                    onRefreshStatus = blockerViewModel::refreshStatus,
                    onOpenUsageAccessSettings = blockerViewModel::openUsageAccessSettings,
                    onOpenAppSettings = blockerViewModel::openAppSettingsForRevoke
                )
            }

            composable(FocuslyRoute.Rewards.route) {
                val rewardsViewModel: RewardsViewModel = hiltViewModel()
                val rewardsState by rewardsViewModel.uiState.collectAsStateWithLifecycle()
                val purchaseMessage by rewardsViewModel.purchaseMessage.collectAsStateWithLifecycle()

                RewardsScreen(
                    state = rewardsState,
                    purchaseMessage = purchaseMessage,
                    onBack = { navController.popBackStack() },
                    onPurchaseItem = rewardsViewModel::purchaseItem,
                    onSetFeaturedItem = rewardsViewModel::setFeaturedItem,
                    onDismissPurchaseMessage = rewardsViewModel::clearPurchaseMessage
                )
            }
        }
    }

    val pendingRecovery = focusState.pendingRecoverySnapshot
    if (pendingRecovery != null && onboardingCompleted && currentRoute != FocuslyRoute.Splash.route) {
        FocusSessionRecoveryDialog(
            snapshot = pendingRecovery,
            onResumeSession = {
                focusViewModel.dismissRecoveryAndResume()
                navigateToTab(FocuslyRoute.Focus.route)
            },
            onRestartShorter = { shorterMinutes ->
                focusViewModel.restartShorterFromRecovery(shorterMinutes)
                navigateToTab(FocuslyRoute.Focus.route)
            },
            onEndSession = {
                focusViewModel.endSessionEarly()
            }
        )
    }
}
