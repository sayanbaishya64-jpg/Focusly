import type { AppLanguage, StudentCategory, StudyGoal } from '../types/focusly';

export interface FocuslyStrings {
  appName: string;
  tagline: string;
  getStarted: string;
  continueLabel: string;
  skipLabel: string;
  backLabel: string;
  stepOf: (step: number, total: number) => string;

  welcomeTitle: string;
  welcomeSubtitle: string;
  welcomeHighlights: string[];

  categoryTitle: string;
  categorySubtitle: string;
  categories: Record<StudentCategory, { title: string; desc: string }>;

  goalsTitle: string;
  goalsSubtitle: string;
  goals: Record<StudyGoal, string>;

  targetTitle: string;
  targetSubtitle: string;
  minutesLabel: (m: number) => string;
  customDuration: string;
  customDurationHint: string;
  customDurationError: string;

  languageTitle: string;
  languageSubtitle: string;
  englishLabel: string;
  englishDesc: string;
  bengaliLabel: string;
  bengaliDesc: string;

  distractionTitle: string;
  distractionSubtitle: string;
  usagePermissionTitle: string;
  usagePermissionDesc: string;
  notifPermissionTitle: string;
  notifPermissionDesc: string;
  optionalAppsHeading: string;
  finishSetup: string;

  nav: {
    home: string;
    focus: string;
    study: string;
    progress: string;
    profile: string;
  };

  greetings: {
    morning: string;
    afternoon: string;
    evening: string;
  };

  home: {
    todayGoal: string;
    minutesUnit: string;
    startFocusCta: string;
    configuredProfile: string;
    selectedGoalsLabel: string;
    distractionAppsLabel: string;
    noneSelected: string;
    phase1NoteTitle: string;
    phase1NoteBody: string;
    upcomingTitle: string;
    upcomingItems: { phase: string; title: string; desc: string }[];
  };

  shells: {
    focusTitle: string;
    focusDesc: string;
    studyTitle: string;
    studyDesc: string;
    progressTitle: string;
    progressDesc: string;
    plannedCapabilities: string;
    focusCapabilities: string[];
    studyCapabilities: string[];
    progressCapabilities: string[];
  };

  profile: {
    title: string;
    subtitle: string;
    savedPreferences: string;
    categoryLabel: string;
    dailyTargetLabel: string;
    goalsLabel: string;
    distractionLabel: string;
    languageSectionTitle: string;
    privacySectionTitle: string;
    privacyBody: string;
    resetButton: string;
    resetConfirmTitle: string;
    resetConfirmBody: string;
    cancelLabel: string;
    confirmResetLabel: string;
    studentNameLabel: string;
    studentNamePlaceholder: string;
    saveNameLabel: string;
  };

  dashboard: {
    completedLabel: string;
    remainingLabel: string;
    progressLabel: string;
    startFocus: string;
    resumeFocus: string;
    currentTask: string;
    noTaskSelected: string;
    addTask: string;
    taskTitleLabel: string;
    subjectLabel: string;
    plannedMinutesLabel: string;
    saveTask: string;
    rewardsPreview: string;
    focusCoins: string;
    achievements: string;
    viewRewards: string;
    rewardsPhaseNote: string;
    weeklySummary: string;
    weeklyMinutes: string;
    completedSessions: string;
    avgSession: string;
    noSessionsToday: string;
    noWeeklyActivity: string;
    notificationsTitle: string;
    notificationsBody: string;
    okLabel: string;
  };

  timer: {
    focusMode: string;
    breakMode: string;
    custom: string;
    customMinutesHint: string;
    subjectPickerLabel: string;
    taskPickerLabel: string;
    noneOption: string;
    startFocus: string;
    startBreak: string;
    pause: string;
    resume: string;
    endSession: string;
    endConfirmTitle: string;
    endConfirmBody: string;
    blockerPreviewNotice: string;
    completedTitle: string;
    breakCompletedTitle: string;
    completedMessage: string;
    actualDuration: string;
    plannedDuration: string;
    dailyProgressUpdate: string;
    startAnother: string;
    takeBreak: string;
    returnHome: string;
    historyTitle: string;
    historyEmpty: string;
    statusCompleted: string;
    statusEndedEarly: string;
    recoveryTitle: string;
    recoveryBody: string;
    recoveryResume: string;
    recoveryEnd: string;
    statusReady: string;
    statusActive: string;
    statusPaused: string;
  };

  phase3: {
    adaptiveTitle: string;
    adaptiveInsufficientTitle: string;
    adaptiveInsufficientDesc: string;
    adaptiveStartRecommended: (m: number) => string;
    adaptiveUseDuration: (m: number) => string;
    adaptiveIgnore: string;
    adaptiveRuleMeta: (count: number) => string;

    blockerTitle: string;
    blockerSubtitle: string;
    blockerConfigure: string;
    blockerManage: string;
    blockerBack: string;
    blockerEnableLabel: string;
    blockerStatusActive: string;
    blockerStatusChecklist: string;
    blockerStatusDisabled: string;
    blockerPermGranted: string;
    blockerPermDenied: string;
    blockerSimGrant: string;
    blockerSimRevoke: string;
    blockerTestStatus: string;
    blockerPrivacyNote: string;
    blockerSelectedApps: string;
    blockerAppsUnit: (count: number) => string;
    blockerWebBoundaryNote: string;

    recoveryTitle: string;
    recoveryFreshStart: string;
    recoveryShorterPrompt: (m: number) => string;
    recoveryShorterBtn: (m: number) => string;
    recoveryDismissBtn: string;
  };

  phase4: {
    studyTitle: string;
    studySubtitle: string;
    addTask: string;
    editTask: string;
    addSubject: string;
    manageSubjects: string;
    subjectNameHint: string;
    noSubjects: string;
    filterToday: string;
    filterUpcoming: string;
    filterAll: string;
    filterCompleted: string;
    overdueTitle: (count: number) => string;
    overdueDesc: string;
    moveToToday: string;
    emptyTasksTitle: string;
    emptyTasksDesc: string;
    priorityLabel: string;
    priorityLow: string;
    priorityMedium: string;
    priorityHigh: string;
    scheduleLabel: string;
    scheduleToday: string;
    scheduleTomorrow: string;
    scheduleUnscheduled: string;
    focusNow: string;
    taskNotesLabel: string;
    generalStudy: string;
    closeLabel: string;
    deleteLabel: string;

    progressTitle: string;
    progressSubtitle: string;
    periodWeek: string;
    periodMonth: string;
    totalFocus: string;
    completionRate: string;
    avgSession: string;
    tasksDone: string;
    activityTitle: string;
    activeDays: (active: number, total: number) => string;
    noActivity: string;
    subjectBreakdownTitle: string;
    noSubjectData: string;
  };

  phase5: {
    rewardsTitle: string;
    rewardsSubtitle: string;
    lifetimeEarned: (coins: number) => string;
    streakLabel: string;
    sessionsCompleted: (count: number) => string;
    gardenTitle: string;
    stageSeedling: string;
    stageSprouting: string;
    stageBlooming: string;
    gardenHintSeedling: string;
    gardenHintSprouting: string;
    gardenHintBlooming: string;
    storeTitle: string;
    featuredBadge: string;
    setFeatured: string;
    ledgerTitle: string;
    ledgerEmpty: string;
    sessionCoinsEarned: string;
    purchaseSuccess: string;
    purchaseInsufficient: string;
    purchaseAlready: string;
    purchaseNotFound: string;
  };

  phase6: {
    missionsTitle: string;
    missionsCompletedSummary: (done: number, total: number) => string;
    missionsClaimBtn: (coins: number) => string;
    missionsClaimedBadge: string;
    missionsProgressBadge: (current: number, target: number) => string;

    focusModeManual: string;
    focusModeAuto: string;
    focusModeSmart: string;
    focusModeAutoHint: (title: string, mins: number) => string;
    focusModeAutoEmpty: string;
    focusModeSmartHint: (mins: number) => string;

    breathingActionBtn: string;
    breathingTitle: string;
    breathingSubtitle: string;
    breathingInhale: string;
    breathingHold: string;
    breathingExhale: string;
    breathingRest: string;
    breathingComplete: string;
    breathingRemainingSeconds: (secs: number) => string;
    breathingDoneHint: string;

    backupTitle: string;
    backupSubtitle: string;
    backupExportBtn: string;
    backupImportBtn: string;
    backupExportSuccess: string;
    backupImportSuccess: (sessions: number, tasks: number, subjects: number) => string;
    backupImportError: (reason: string) => string;
  };
}

export const STRINGS: Record<AppLanguage, FocuslyStrings> = {
  en: {
    appName: 'FOCUSLY',
    tagline: 'Take Control. Stay Focused. Achieve More.',
    getStarted: 'Get Started',
    continueLabel: 'Continue',
    skipLabel: 'Skip for now',
    backLabel: 'Back',
    stepOf: (step, total) => `Step ${step} of ${total}`,

    welcomeTitle: 'Build calm, consistent study habits.',
    welcomeSubtitle:
      'Designed for students and self-learners to cut through digital noise, protect deep concentration, and track genuine study progress completely offline.',
    welcomeHighlights: [
      'Offline-first local privacy with zero mandatory sign-up',
      'Smart study rhythm tailored to your daily focus target',
      'English and Bengali support built directly into every screen',
    ],

    categoryTitle: 'How do you study?',
    categorySubtitle:
      'Choose your current academic stage so FOCUSLY can shape your study workspace.',
    categories: {
      school_student: {
        title: 'School Student',
        desc: 'Daily schoolwork, homework routines, and exam preparation',
      },
      college_student: {
        title: 'College Student',
        desc: 'Lectures, assignments, lab sessions, and semester projects',
      },
      competitive_aspirant: {
        title: 'Competitive Exam Aspirant',
        desc: 'Deep revision blocks, mock tests, and sustained discipline',
      },
      self_learner: {
        title: 'Self Learner',
        desc: 'Independent courses, coding, reading, and skill mastery',
      },
    },

    goalsTitle: 'What matters most right now?',
    goalsSubtitle: 'Select one or more goals to shape your daily study rhythm.',
    goals: {
      improve_concentration: 'Improve concentration',
      reduce_distractions: 'Reduce distractions',
      build_consistency: 'Build consistency',
      complete_daily_goals: 'Complete daily goals',
    },

    targetTitle: 'Set your daily focus target',
    targetSubtitle:
      'Pick a realistic daily commitment. You can adjust this anytime from Profile.',
    minutesLabel: (m) => `${m} minutes`,
    customDuration: 'Custom duration',
    customDurationHint: 'Enter minutes (10 – 480)',
    customDurationError: 'Please enter a duration between 10 and 480 minutes.',

    languageTitle: 'Choose your language',
    languageSubtitle:
      'Switch cleanly between English and Bengali anytime without losing local data.',
    englishLabel: 'English',
    englishDesc: 'Default interface & study terminology',
    bengaliLabel: 'বাংলা (Bengali)',
    bengaliDesc: 'সম্পূর্ণ বাংলা ইন্টারফেস ও নির্দেশনা',

    distractionTitle: 'Distraction preferences',
    distractionSubtitle:
      'Optionally select apps that pull your attention away. FOCUSLY never uploads usage data and never blocks emergency or essential system tools.',
    usagePermissionTitle: 'Usage Access (Optional)',
    usagePermissionDesc:
      'Used strictly on-device during active focus sessions to detect when a selected distracting app opens. Only requested if you enable alerts in Phase 4.',
    notifPermissionTitle: 'Focus Notifications',
    notifPermissionDesc:
      'Displays your active focus timer in the notification shade and alerts you gently when a session finishes.',
    optionalAppsHeading: 'Optional distraction list',
    finishSetup: 'Finish Setup',

    nav: {
      home: 'Home',
      focus: 'Focus',
      study: 'Study',
      progress: 'Progress',
      profile: 'Profile',
    },

    greetings: {
      morning: 'Good morning',
      afternoon: 'Good afternoon',
      evening: 'Good evening',
    },

    home: {
      todayGoal: "Today's Focus Goal",
      minutesUnit: 'min',
      startFocusCta: 'Open Focus Tab',
      configuredProfile: 'Configured Study Profile',
      selectedGoalsLabel: 'Study Goals',
      distractionAppsLabel: 'Selected Distractions',
      noneSelected: 'None selected (skipped)',
      phase1NoteTitle: 'Phase 1 Foundation Active',
      phase1NoteBody:
        'Design system, 5-tab navigation, and 6-screen onboarding with local persistence are complete. Timer, Room database, Study Planner, and Rewards activate in Phases 2–6.',
      upcomingTitle: 'Development Roadmap',
      upcomingItems: [
        {
          phase: 'Phase 2',
          title: 'Smart Focus Timer & Room Database',
          desc: 'Elapsed-time foreground timer, 5 presets + custom, Manual/Automatic/Smart modes, and full Room entities.',
        },
        {
          phase: 'Phase 3',
          title: 'Smart Study Planner',
          desc: 'Subjects, priorities, target durations, daily schedules, and task rescheduling.',
        },
        {
          phase: 'Phase 4',
          title: 'Transparent Distraction Manager',
          desc: 'Launchable app picker, permission-safe alerts, and emergency exit controls.',
        },
      ],
    },

    shells: {
      focusTitle: 'Focus Workspace',
      focusDesc:
        'The Smart Focus Timer and Foreground Service persistence will be implemented in Phase 2.',
      studyTitle: 'Study Planner',
      studyDesc:
        'Subject management, study tasks, priorities, and daily scheduling will be implemented in Phase 3.',
      progressTitle: 'Progress & Analytics',
      progressDesc:
        'Room-backed analytics, Adaptive Focus Engine insights, and completion trends will be implemented in Phase 6.',
      plannedCapabilities: 'Planned Capabilities',
      focusCapabilities: [
        'Presets: 15, 25, 45, 60, and 90 minutes plus custom durations',
        'Manual Focus, Automatic Focus (schedule-driven), and Smart Focus modes',
        'Clock-based elapsed time recovery across screen locks and backgrounding',
        'Focus Recovery module with 5-minute restart and breathing activity',
      ],
      studyCapabilities: [
        'Create and manage academic subjects',
        'Add study tasks with priority levels and target durations',
        'Daily schedules with completion tracking and unfinished task rescheduling',
      ],
      progressCapabilities: [
        'Daily, weekly, and monthly focus time calculated from Room sessions',
        'Session completion rate, average duration, and subject-wise progress',
        'Local Adaptive Focus Engine (FAFE) session recommendations',
      ],
    },

    profile: {
      title: 'Profile & Settings',
      subtitle: 'Manage your local study preferences and language.',
      savedPreferences: 'Saved Onboarding Preferences',
      categoryLabel: 'Student Category',
      dailyTargetLabel: 'Daily Focus Target',
      goalsLabel: 'Active Study Goals',
      distractionLabel: 'Selected Distraction Apps',
      languageSectionTitle: 'App Language',
      privacySectionTitle: 'Offline-First Privacy',
      privacyBody:
        'All onboarding choices and preferences are stored locally on your device. No account registration or remote server upload is required.',
      resetButton: 'Reset Local Data & Re-run Onboarding',
      resetConfirmTitle: 'Reset local onboarding data?',
      resetConfirmBody:
        'This clears your saved category, goals, daily target, and distraction preferences from local storage and returns to the Welcome screen.',
      cancelLabel: 'Cancel',
      confirmResetLabel: 'Reset Data',
      studentNameLabel: 'Student Name (Optional)',
      studentNamePlaceholder: 'Enter your name for greetings',
      saveNameLabel: 'Save Name',
    },

    dashboard: {
      completedLabel: 'Completed',
      remainingLabel: 'Remaining',
      progressLabel: 'Progress',
      startFocus: 'Start Focus',
      resumeFocus: 'Resume Active Session',
      currentTask: 'Current Study Task',
      noTaskSelected: 'No study task selected.',
      addTask: 'Add Task',
      taskTitleLabel: 'Task title',
      subjectLabel: 'Subject (optional)',
      plannedMinutesLabel: 'Planned minutes',
      saveTask: 'Save Task',
      rewardsPreview: 'Rewards Preview',
      focusCoins: 'Focus Coins',
      achievements: 'Achievements',
      viewRewards: 'View Rewards',
      rewardsPhaseNote:
        'Focus Coins, Achievement Badges, Virtual Garden, and the Reward Store will activate in Phase 5 using genuinely completed sessions.',
      weeklySummary: 'Weekly Summary',
      weeklyMinutes: 'Weekly Focus',
      completedSessions: 'Completed Sessions',
      avgSession: 'Avg Session',
      noSessionsToday: 'No completed focus sessions today. Start a focus block to track progress.',
      noWeeklyActivity: 'No weekly activity recorded yet.',
      notificationsTitle: 'Focus Notifications',
      notificationsBody:
        'On Android, active focus and break sessions run in a Foreground Service with live notification controls for Pause, Resume, and End.',
      okLabel: 'OK',
    },

    timer: {
      focusMode: 'Focus',
      breakMode: 'Break',
      custom: 'Custom',
      customMinutesHint: 'Enter duration in minutes',
      subjectPickerLabel: 'Subject',
      taskPickerLabel: 'Study Task',
      noneOption: 'None',
      startFocus: 'Start Focus',
      startBreak: 'Start Break',
      pause: 'Pause',
      resume: 'Resume',
      endSession: 'End Session',
      endConfirmTitle: 'End focus session early?',
      endConfirmBody:
        'Actual focused time will be recorded separately. Only full sessions count toward completed-session analytics.',
      blockerPreviewNotice:
        'Web Preview Note: Browser previews cannot monitor or block Android apps. Native Android app-blocking preferences activate in Phase 4.',
      completedTitle: 'Focus Session Completed!',
      breakCompletedTitle: 'Break Completed!',
      completedMessage: 'Consistent blocks like this build lasting concentration.',
      actualDuration: 'Actual Focused Duration',
      plannedDuration: 'Planned Duration',
      dailyProgressUpdate: 'Today’s Progress',
      startAnother: 'Start Another Session',
      takeBreak: 'Take a Break',
      returnHome: 'Return Home',
      historyTitle: 'Recent Session History',
      historyEmpty: 'No sessions recorded yet.',
      statusCompleted: 'Completed',
      statusEndedEarly: 'Ended Early',
      recoveryTitle: 'An unfinished focus session was found.',
      recoveryBody:
        'Resume where you left off using accurate elapsed time, or end the session now.',
      recoveryResume: 'Resume Session',
      recoveryEnd: 'End Session',
      statusReady: 'Ready',
      statusActive: 'Active',
      statusPaused: 'Paused',
    },

    phase3: {
      adaptiveTitle: 'Recommended Focus',
      adaptiveInsufficientTitle: 'Building your focus baseline',
      adaptiveInsufficientDesc:
        'Complete at least 3 focus sessions so FOCUSLY can suggest realistic durations from your local study history.',
      adaptiveStartRecommended: (m) => `Start Recommended (${m} min)`,
      adaptiveUseDuration: (m) => `Use ${m} min`,
      adaptiveIgnore: 'Ignore',
      adaptiveRuleMeta: (count) => `Based on ${count} local focus sessions · Rule-based`,

      blockerTitle: 'Distraction Blocker',
      blockerSubtitle: 'Transparent, permission-based reminders during active focus blocks.',
      blockerConfigure: 'Configure',
      blockerManage: 'Manage',
      blockerBack: 'Back',
      blockerEnableLabel: 'Enable Distraction Awareness',
      blockerStatusActive: 'Active during focus',
      blockerStatusChecklist: 'Checklist mode (No Usage Access)',
      blockerStatusDisabled: 'Disabled',
      blockerPermGranted: 'Usage Access Granted',
      blockerPermDenied: 'Usage Access Not Granted',
      blockerSimGrant: 'Simulate Grant Usage Access',
      blockerSimRevoke: 'Revoke / Reset Usage Access',
      blockerTestStatus: 'Test Blocker Status',
      blockerPrivacyNote:
        'On Android, FOCUSLY checks foreground apps strictly on-device during active focus sessions. No usage data is ever uploaded, and emergency or system settings are never blocked.',
      blockerSelectedApps: 'Managed Distraction Apps',
      blockerAppsUnit: (count) => `${count} apps`,
      blockerWebBoundaryNote:
        'Web Preview: Simulates permission states and selected apps locally. Native Android uses AppOpsManager, PackageManager <queries>, and UsageStatsManager.',

      recoveryTitle: 'Ready to continue?',
      recoveryFreshStart: 'A fresh start is always possible. Your progress is still valuable.',
      recoveryShorterPrompt: (m) => `Would you like to try a shorter ${m}-minute session?`,
      recoveryShorterBtn: (m) => `Restart Shorter (${m} min)`,
      recoveryDismissBtn: 'Dismiss',
    },

    phase4: {
      studyTitle: 'Study Planner',
      studySubtitle: 'Organize subjects, priorities, and daily focus blocks completely offline.',
      addTask: 'Add Task',
      editTask: 'Edit Task',
      addSubject: 'Add Subject',
      manageSubjects: 'Subjects',
      subjectNameHint: 'Subject name',
      noSubjects: 'No subjects yet. Add subjects like Mathematics, Physics, or Revision.',
      filterToday: 'Today',
      filterUpcoming: 'Upcoming',
      filterAll: 'All',
      filterCompleted: 'Completed',
      overdueTitle: (count) => `${count} unfinished tasks from earlier days`,
      overdueDesc: 'Move remaining tasks to today so nothing slips through the cracks.',
      moveToToday: 'Move to Today',
      emptyTasksTitle: 'No study tasks in this view',
      emptyTasksDesc: 'Add a focused task with subject, priority, and target minutes to start strong.',
      priorityLabel: 'Priority',
      priorityLow: 'Low',
      priorityMedium: 'Medium',
      priorityHigh: 'High',
      scheduleLabel: 'Schedule',
      scheduleToday: 'Today',
      scheduleTomorrow: 'Tomorrow',
      scheduleUnscheduled: 'Unscheduled',
      focusNow: 'Focus',
      taskNotesLabel: 'Notes (optional)',
      generalStudy: 'General Study',
      closeLabel: 'Close',
      deleteLabel: 'Delete',

      progressTitle: 'Progress & Analytics',
      progressSubtitle: 'Measured strictly from your locally recorded focus sessions.',
      periodWeek: '7 Days',
      periodMonth: '30 Days',
      totalFocus: 'Total Focus',
      completionRate: 'Completion Rate',
      avgSession: 'Avg Session',
      tasksDone: 'Tasks Done',
      activityTitle: 'Focus Activity',
      activeDays: (active, total) => `${active} / ${total} active days`,
      noActivity: 'Complete a focus session to see daily progress bars.',
      subjectBreakdownTitle: 'Subject Breakdown',
      noSubjectData: 'No completed subject focus sessions in this range yet.',
    },

    phase5: {
      rewardsTitle: 'Rewards & Garden',
      rewardsSubtitle: 'Earned exclusively from genuinely completed focus sessions.',
      lifetimeEarned: (coins) => `${coins} lifetime earned`,
      streakLabel: 'Study Streak',
      sessionsCompleted: (count) => `${count} focus sessions`,
      gardenTitle: 'Virtual Garden',
      stageSeedling: 'Seedling',
      stageSprouting: 'Sprouting',
      stageBlooming: 'Blooming',
      gardenHintSeedling: 'Complete 2 focus sessions to reach Sprouting stage.',
      gardenHintSprouting: 'Complete 5 focus sessions to reach full Bloom.',
      gardenHintBlooming: 'Your study companion is in full bloom.',
      storeTitle: 'Garden Companion Store',
      featuredBadge: 'Featured',
      setFeatured: 'Set Active',
      ledgerTitle: 'Recent Coin Activity',
      ledgerEmpty: 'Complete a focus session to earn your first Focus Coins.',
      sessionCoinsEarned: 'Focus Coins Earned',
      purchaseSuccess: 'Unlocked new garden companion.',
      purchaseInsufficient: 'Not enough Focus Coins yet. Complete more sessions to earn coins.',
      purchaseAlready: 'This companion is already unlocked.',
      purchaseNotFound: 'Reward item unavailable.',
    },

    phase6: {
      missionsTitle: 'Daily Study Missions',
      missionsCompletedSummary: (done, total) => `${done} / ${total} completed`,
      missionsClaimBtn: (coins) => `Claim +${coins}c`,
      missionsClaimedBadge: 'Claimed',
      missionsProgressBadge: (current, target) => `${current} / ${target}`,

      focusModeManual: 'Manual',
      focusModeAuto: 'Auto-Schedule',
      focusModeSmart: 'Smart',
      focusModeAutoHint: (title, mins) => `Selected next planned task: ${title} (${mins} min)`,
      focusModeAutoEmpty: 'No pending study tasks found. Add a task in Study Planner.',
      focusModeSmartHint: (mins) => `Using Adaptive Focus duration (${mins} min) from your history.`,

      breathingActionBtn: '1-Min Calm Reset',
      breathingTitle: '1-Minute Box Breathing',
      breathingSubtitle:
        'Follow a steady 4-4-4-4 rhythm to settle your attention before studying.',
      breathingInhale: 'Inhale',
      breathingHold: 'Hold',
      breathingExhale: 'Exhale',
      breathingRest: 'Rest',
      breathingComplete: 'Reset Complete',
      breathingRemainingSeconds: (secs) => `${secs}s remaining`,
      breathingDoneHint: 'Mind settled. Ready when you are.',

      backupTitle: 'Local JSON Backup & Restore',
      backupSubtitle:
        'Export your study sessions, tasks, subjects, rewards, and preferences to a local JSON file, or restore from an existing backup.',
      backupExportBtn: 'Export Backup JSON',
      backupImportBtn: 'Restore from JSON',
      backupExportSuccess: 'Backup JSON file downloaded.',
      backupImportSuccess: (sessions, tasks, subjects) =>
        `Restored ${sessions} sessions, ${tasks} tasks, and ${subjects} subjects.`,
      backupImportError: (reason) => `Restore failed: ${reason}`,
    },
  },

  bn: {
    appName: 'FOCUSLY',
    tagline: 'নিয়ন্ত্রণ নিন। মনোযোগ ধরে রাখুন। আরও অর্জন করুন।',
    getStarted: 'শুরু করুন',
    continueLabel: 'পরবর্তী',
    skipLabel: 'আপাতত এড়িয়ে যান',
    backLabel: 'পেছনে',
    stepOf: (step, total) => `ধাপ ${step} / ${total}`,

    welcomeTitle: 'সুস্থ ও নিয়মিত পড়াশোনার অভ্যাস গড়ুন।',
    welcomeSubtitle:
      'শিক্ষার্থী ও স্ব-শিক্ষার্থীদের জন্য তৈরি—ডিজিটাল বিভ্রান্তি কমিয়ে গভীর মনোযোগ ধরে রাখুন এবং সম্পূর্ণ অফলাইনে আপনার অগ্রগতি অনুসরণ করুন।',
    welcomeHighlights: [
      'কোনো বাধ্যতামূলক অ্যাকাউন্ট ছাড়াই সম্পূর্ণ অফলাইন গোপনীয়তা',
      'আপনার দৈনিক ফোকাস লক্ষ্য অনুযায়ী সাজানো অধ্যয়নের পরিবেশ',
      'প্রতিটি স্ক্রিনে ইংরেজি ও বাংলা ভাষার পূর্ণ সমর্থন',
    ],

    categoryTitle: 'আপনি কীভাবে পড়াশোনা করেন?',
    categorySubtitle:
      'আপনার বর্তমান শিক্ষাস্তর নির্বাচন করুন যাতে FOCUSLY উপযুক্ত কর্মক্ষেত্র তৈরি করতে পারে।',
    categories: {
      school_student: {
        title: 'স্কুল শিক্ষার্থী',
        desc: 'দৈনন্দিন স্কুলের পড়া, বাড়ির কাজ এবং পরীক্ষার প্রস্তুতি',
      },
      college_student: {
        title: 'কলেজ / বিশ্ববিদ্যালয় শিক্ষার্থী',
        desc: 'লেকচার, অ্যাসাইনমেন্ট, ল্যাব এবং সেমিস্টার প্রজেক্ট',
      },
      competitive_aspirant: {
        title: 'প্রতিযোগিতামূলক পরীক্ষার প্রার্থী',
        desc: 'গভীর রিভিশন সেশন, মক টেস্ট এবং নিয়মিত শৃঙ্খলা',
      },
      self_learner: {
        title: 'স্ব-শিক্ষার্থী',
        desc: 'অনলাইন কোর্স, কোডিং, বই পড়া এবং দক্ষতা উন্নয়ন',
      },
    },

    goalsTitle: 'এই মুহূর্তে আপনার প্রধান লক্ষ্য কী?',
    goalsSubtitle: 'দৈনন্দিন অধ্যয়নের ছন্দ ঠিক করতে এক বা একাধিক লক্ষ্য বেছে নিন।',
    goals: {
      improve_concentration: 'মনোযোগ বৃদ্ধি করা',
      reduce_distractions: 'বিভ্রান্তি কমানো',
      build_consistency: 'ধারাবাহিকতা তৈরি করা',
      complete_daily_goals: 'দৈনিক লক্ষ্য পূরণ করা',
    },

    targetTitle: 'দৈনিক ফোকাস লক্ষ্য নির্ধারণ করুন',
    targetSubtitle:
      'বাস্তবসম্মত দৈনিক সময় বেছে নিন। প্রোফাইল থেকে যেকোনো সময় পরিবর্তন করা যাবে।',
    minutesLabel: (m) => `${m} মিনিট`,
    customDuration: 'কাস্টম সময়',
    customDurationHint: 'মিনিট লিখুন (১০ – ৪৮০)',
    customDurationError: 'দয়া করে ১০ থেকে ৪৮০ মিনিটের মধ্যে একটি মান দিন।',

    languageTitle: 'আপনার ভাষা নির্বাচন করুন',
    languageSubtitle:
      'যেকোনো সময় কোনো ডেটা না হারিয়ে ইংরেজি ও বাংলার মধ্যে পরিবর্তন করুন।',
    englishLabel: 'English',
    englishDesc: 'ডিফল্ট ইংরেজি ইন্টারফেস',
    bengaliLabel: 'বাংলা (Bengali)',
    bengaliDesc: 'সম্পূর্ণ বাংলা ইন্টারফেস ও নির্দেশনা',

    distractionTitle: 'বিভ্রান্তি নিয়ন্ত্রণ পছন্দ',
    distractionSubtitle:
      'ঐচ্ছিকভাবে এমন অ্যাপ নির্বাচন করুন যা আপনার মনোযোগ নষ্ট করে। FOCUSLY কোনো ডেটা সার্ভারে পাঠায় না এবং জরুরি সেবা ব্লক করে না।',
    usagePermissionTitle: 'অ্যাপ ব্যবহারের অনুমতি (ঐচ্ছিক)',
    usagePermissionDesc:
      'সক্রিয় ফোকাস সেশনের সময় নির্বাচিত অ্যাপ খুললে কেবল ডিভাইসের ভেতরেই সতর্কতা দেখাতে ব্যবহৃত হয়।',
    notifPermissionTitle: 'ফোকাস নোটিফিকেশন',
    notifPermissionDesc: 'সক্রিয় টাইমার প্রদর্শন করে এবং সেশন শেষ হলে জানিয়ে দেয়।',
    optionalAppsHeading: 'ঐচ্ছিক বিভ্রান্তির তালিকা',
    finishSetup: 'সেটআপ সম্পন্ন করুন',

    nav: {
      home: 'হোম',
      focus: 'ফোকাস',
      study: 'পড়াশোনা',
      progress: 'অগ্রগতি',
      profile: 'প্রোফাইল',
    },

    greetings: {
      morning: 'সুপ্রভাত',
      afternoon: 'শুভ অপরাহ্ন',
      evening: 'শুভ সন্ধ্যা',
    },

    home: {
      todayGoal: 'আজকের ফোকাস লক্ষ্য',
      minutesUnit: 'মিনিট',
      startFocusCta: 'ফোকাস ট্যাব খুলুন',
      configuredProfile: 'সংরক্ষিত অধ্যয়ন প্রোফাইল',
      selectedGoalsLabel: 'অধ্যয়নের লক্ষ্য',
      distractionAppsLabel: 'নির্বাচিত অ্যাপ',
      noneSelected: 'কোনোটি নির্বাচিত নয় (এড়িয়ে যাওয়া হয়েছে)',
      phase1NoteTitle: 'ফেজ ১ ভিত্তি সক্রিয়',
      phase1NoteBody:
        'ডিজাইন সিস্টেম, ৫-ট্যাব নেভিগেশন এবং ৬-ধাপের অনবোর্ডিং স্থানীয় সংরক্ষণসহ সম্পন্ন হয়েছে। টাইমার, রুম ডাটাবেস ও স্টাডি প্ল্যানার পরবর্তী ধাপে যুক্ত হবে।',
      upcomingTitle: 'উন্নয়ন রোডম্যাপ',
      upcomingItems: [
        {
          phase: 'ফেজ ২',
          title: 'স্মার্ট ফোকাস টাইমার ও রুম ডাটাবেস',
          desc: 'সঠিক সময় গণনা, ৫টি প্রিসেট + কাস্টম, এবং অফলাইন ডাটাবেস।',
        },
        {
          phase: 'ফেজ ৩',
          title: 'স্মার্ট স্টাডি প্ল্যানার',
          desc: 'বিষয় পরিচালনা, কাজের অগ্রাধিকার এবং দৈনিক সময়সূচি।',
        },
        {
          phase: 'ফেজ ৪',
          title: 'স্বচ্ছ বিভ্রান্তি নিয়ন্ত্রণ',
          desc: 'অ্যাপ নির্বাচন, অনুমতি-সচেতন সতর্কতা এবং জরুরি প্রস্থান।',
        },
      ],
    },

    shells: {
      focusTitle: 'ফোকাস কর্মক্ষেত্র',
      focusDesc: 'স্মার্ট ফোকাস টাইমার এবং ফোরগ্রাউন্ড সার্ভিস ফেজ ২-এ যুক্ত হবে।',
      studyTitle: 'স্টাডি প্ল্যানার',
      studyDesc: 'বিষয় ব্যবস্থাপনা, অধ্যয়নের কাজ এবং দৈনিক সময়সূচি ফেজ ৩-এ যুক্ত হবে।',
      progressTitle: 'অগ্রগতি ও বিশ্লেষণ',
      progressDesc: 'রুম ডাটাবেস বিশ্লেষণ এবং অ্যাডাপ্টিভ ফোকাস ইঞ্জিন ফেজ ৬-এ যুক্ত হবে।',
      plannedCapabilities: 'পরিকল্পিত বৈশিষ্ট্যসমূহ',
      focusCapabilities: [
        '১৫, ২৫, ৪৫, ৬০ ও ৯০ মিনিটের প্রিসেট এবং কাস্টম সময়',
        'ম্যানুয়াল, অটোমেটিক এবং স্মার্ট ফোকাস মোড',
        'স্ক্রিন লক ও ব্যাকগ্রাউন্ডে গেলেও নির্ভুল সময় পুনরুদ্ধার',
        '৫ মিনিটের দ্রুত পুনরায় শুরু এবং শ্বাস-প্রশ্বাস অনুশীলন',
      ],
      studyCapabilities: [
        'বিষয় তৈরি ও পরিচালনা করা',
        'কাজের অগ্রাধিকার ও লক্ষ্য সময় নির্ধারণ',
        'অসম্পূর্ণ কাজ পুনরায় নির্ধারণের সুবিধা',
      ],
      progressCapabilities: [
        'দৈনিক, সাপ্তাহিক ও মাসিক ফোকাস সময় হিসাব',
        'সেশন সম্পন্ন করার হার এবং বিষয়ভিত্তিক অগ্রগতি',
        'লোকাল অ্যাডাপ্টিভ ফোকাস ইঞ্জিন (FAFE) পরামর্শ',
      ],
    },

    profile: {
      title: 'প্রোফাইল ও সেটিংস',
      subtitle: 'আপনার স্থানীয় অধ্যয়ন পছন্দ ও ভাষা পরিচালনা করুন।',
      savedPreferences: 'সংরক্ষিত অনবোর্ডিং পছন্দ',
      categoryLabel: 'শিক্ষার্থীর ধরন',
      dailyTargetLabel: 'দৈনিক ফোকাস লক্ষ্য',
      goalsLabel: 'সক্রিয় অধ্যয়নের লক্ষ্য',
      distractionLabel: 'নির্বাচিত বিভ্রান্তিকর অ্যাপ',
      languageSectionTitle: 'অ্যাপের ভাষা',
      privacySectionTitle: 'অফলাইন-প্রথম গোপনীয়তা',
      privacyBody:
        'আপনার সমস্ত পছন্দ কেবল আপনার ডিভাইসেই সংরক্ষিত থাকে। কোনো নিবন্ধন বা সার্ভারে ডেটা পাঠানোর প্রয়োজন নেই।',
      resetButton: 'স্থানীয় ডেটা মুছুন এবং অনবোর্ডিং পুনরায় চালান',
      resetConfirmTitle: 'স্থানীয় ডেটা মুছে ফেলবেন?',
      resetConfirmBody:
        'এটি আপনার সংরক্ষিত ধরন, লক্ষ্য, দৈনিক সময় ও অ্যাপ পছন্দ মুছে ফেলবে এবং স্বাগতম স্ক্রিনে ফিরে যাবে।',
      cancelLabel: 'বাতিল',
      confirmResetLabel: 'ডেটা মুছুন',
      studentNameLabel: 'শিক্ষার্থীর নাম (ঐচ্ছিক)',
      studentNamePlaceholder: 'আপনার নাম লিখুন',
      saveNameLabel: 'নাম সংরক্ষণ করুন',
    },

    dashboard: {
      completedLabel: 'সম্পন্ন',
      remainingLabel: 'বাকি',
      progressLabel: 'অগ্রগতি',
      startFocus: 'ফোকাস শুরু করুন',
      resumeFocus: 'চলমান সেশন চালিয়ে যান',
      currentTask: 'বর্তমান অধ্যয়নের কাজ',
      noTaskSelected: 'কোনো অধ্যয়নের কাজ নির্বাচিত নেই।',
      addTask: 'কাজ যোগ করুন',
      taskTitleLabel: 'কাজের শিরোনাম',
      subjectLabel: 'বিষয় (ঐচ্ছিক)',
      plannedMinutesLabel: 'পরিকল্পিত মিনিট',
      saveTask: 'সংরক্ষণ করুন',
      rewardsPreview: 'পুরস্কার প্রিভিউ',
      focusCoins: 'ফোকাস কয়েন',
      achievements: 'অর্জনসমূহ',
      viewRewards: 'পুরস্কার দেখুন',
      rewardsPhaseNote:
        'ফোকাস কয়েন, ব্যাজ, ভার্চুয়াল গার্ডেন এবং রিওয়ার্ড স্টোর ফেজ ৫-এ সক্রিয় হবে।',
      weeklySummary: 'সাপ্তাহিক সারসংক্ষেপ',
      weeklyMinutes: 'সাপ্তাহিক ফোকাস',
      completedSessions: 'সম্পন্ন সেশন',
      avgSession: 'গড় সেশন',
      noSessionsToday: 'আজ এখনো কোনো ফোকাস সেশন সম্পন্ন হয়নি। অগ্রগতি দেখতে সেশন শুরু করুন।',
      noWeeklyActivity: 'এখনো কোনো সাপ্তাহিক কার্যকলাপ নেই।',
      notificationsTitle: 'ফোকাস নোটিফিকেশন',
      notificationsBody:
        'অ্যান্ড্রয়েডে সক্রিয় ফোকাস ও বিরতির সেশন ফোরগ্রাউন্ড সার্ভিসে চলে এবং নোটিফিকেশনে বিরতি ও শেষ করার নিয়ন্ত্রণ দেখায়।',
      okLabel: 'ঠিক আছে',
    },

    timer: {
      focusMode: 'ফোকাস',
      breakMode: 'বিরতি',
      custom: 'কাস্টম',
      customMinutesHint: 'মিনিটে সময় লিখুন',
      subjectPickerLabel: 'বিষয়',
      taskPickerLabel: 'অধ্যয়নের কাজ',
      noneOption: 'কোনোটি নয়',
      startFocus: 'ফোকাস শুরু করুন',
      startBreak: 'বিরতি শুরু করুন',
      pause: 'বিরতি নিন',
      resume: 'পুনরায় শুরু করুন',
      endSession: 'সেশন শেষ করুন',
      endConfirmTitle: 'ফোকাস সেশন আগেই শেষ করবেন?',
      endConfirmBody:
        'প্রকৃত ফোকাস সময় আলাদাভাবে সংরক্ষিত হবে। কেবল পূর্ণ সেশনগুলো সম্পন্ন সেশন হিসেবে গণ্য হবে।',
      blockerPreviewNotice:
        'ওয়েব প্রিভিউ তথ্য: ব্রাউজার প্রিভিউ অ্যান্ড্রয়েড অ্যাপ ব্লক করতে পারে না। নেটিভ অ্যাপ নিয়ন্ত্রণ ফেজ ৪-এ যুক্ত হবে।',
      completedTitle: 'ফোকাস সেশন সম্পন্ন হয়েছে!',
      breakCompletedTitle: 'বিরতি সম্পন্ন হয়েছে!',
      completedMessage: 'নিয়মিত এমন সেশন আপনার মনোযোগকে আরও দৃঢ় করে তোলে।',
      actualDuration: 'প্রকৃত ফোকাস সময়',
      plannedDuration: 'পরিকল্পিত সময়',
      dailyProgressUpdate: 'আজকের অগ্রগতি',
      startAnother: 'আরেকটি সেশন শুরু করুন',
      takeBreak: 'বিরতি নিন',
      returnHome: 'হোমে ফিরুন',
      historyTitle: 'সাম্প্রতিক সেশনের ইতিহাস',
      historyEmpty: 'এখনো কোনো সেশন সংরক্ষিত হয়নি।',
      statusCompleted: 'সম্পন্ন',
      statusEndedEarly: 'আগেই শেষ',
      recoveryTitle: 'একটি অসমাপ্ত ফোকাস সেশন পাওয়া গেছে।',
      recoveryBody:
        'সঠিক অতিবাহিত সময়সহ সেশনটি পুনরায় শুরু করুন অথবা এখনই শেষ করুন।',
      recoveryResume: 'সেশন চালিয়ে যান',
      recoveryEnd: 'সেশন শেষ করুন',
      statusReady: 'প্রস্তুত',
      statusActive: 'সক্রিয়',
      statusPaused: 'বিরতিতে',
    },

    phase3: {
      adaptiveTitle: 'প্রস্তাবিত ফোকাস সময়',
      adaptiveInsufficientTitle: 'আপনার অধ্যয়নের ছন্দ তৈরি হচ্ছে',
      adaptiveInsufficientDesc:
        'আপনার স্থানীয় ইতিহাস থেকে বাস্তবসম্মত পরামর্শ পেতে কমপক্ষে ৩টি ফোকাস সেশন সম্পন্ন করুন।',
      adaptiveStartRecommended: (m) => `প্রস্তাবিত শুরু করুন (${m} মি.)`,
      adaptiveUseDuration: (m) => `${m} মিনিট ব্যবহার করুন`,
      adaptiveIgnore: 'উপেক্ষা করুন',
      adaptiveRuleMeta: (count) => `${count}টি স্থানীয় সেশনের ভিত্তিতে · নিয়ম-ভিত্তিক`,

      blockerTitle: 'বিভ্রান্তি নিয়ন্ত্রণ',
      blockerSubtitle: 'সক্রিয় ফোকাস সেশনের সময় স্বচ্ছ ও অনুমতি-নির্ভর সতর্কতা।',
      blockerConfigure: 'সেটিংস',
      blockerManage: 'পরিচালনা',
      blockerBack: 'পেছনে',
      blockerEnableLabel: 'বিভ্রান্তি সতর্কতা সক্রিয় করুন',
      blockerStatusActive: 'ফোকাস চলাকালীন সক্রিয়',
      blockerStatusChecklist: 'চেকলিস্ট মোড (Usage Access ছাড়া)',
      blockerStatusDisabled: 'নিষ্ক্রিয়',
      blockerPermGranted: 'Usage Access অনুমতি দেওয়া আছে',
      blockerPermDenied: 'Usage Access অনুমতি দেওয়া নেই',
      blockerSimGrant: 'অনুমতি পরীক্ষা সিমুলেট করুন',
      blockerSimRevoke: 'অনুমতি প্রত্যাহার / রিসেট করুন',
      blockerTestStatus: 'স্ট্যাটাস পরীক্ষা করুন',
      blockerPrivacyNote:
        'অ্যান্ড্রয়েডে FOCUSLY কেবল সক্রিয় ফোকাস সেশনের সময় ডিভাইসের ভেতরেই ফোরগ্রাউন্ড অ্যাপ পরীক্ষা করে। কোনো ডেটা আপলোড হয় না এবং জরুরি সেবা ব্লক করা হয় না।',
      blockerSelectedApps: 'নির্বাচিত বিভ্রান্তিকর অ্যাপ',
      blockerAppsUnit: (count) => `${count}টি অ্যাপ`,
      blockerWebBoundaryNote:
        'ওয়েব প্রিভিউ: অনুমতি ও অ্যাপ তালিকা স্থানীয়ভাবে সিমুলেট করে। নেটিভ অ্যান্ড্রয়েডে AppOpsManager ও UsageStatsManager ব্যবহৃত হয়।',

      recoveryTitle: 'পুনরায় শুরু করতে প্রস্তুত?',
      recoveryFreshStart: 'নতুন করে শুরু করা সবসময়ই সম্ভব। আপনার প্রতিটি প্রচেষ্টাই মূল্যবান।',
      recoveryShorterPrompt: (m) => `আপনি কি ছোট ${m} মিনিটের সেশন চেষ্টা করতে চান?`,
      recoveryShorterBtn: (m) => `ছোট সেশন শুরু করুন (${m} মি.)`,
      recoveryDismissBtn: 'বাদ দিন',
    },

    phase4: {
      studyTitle: 'স্টাডি প্ল্যানার',
      studySubtitle: 'অফলাইনেই বিষয়, কাজের অগ্রাধিকার ও দৈনিক অধ্যয়ন সূচি সাজান।',
      addTask: 'কাজ যোগ করুন',
      editTask: 'কাজ সম্পাদনা',
      addSubject: 'বিষয় যোগ করুন',
      manageSubjects: 'বিষয়সমূহ',
      subjectNameHint: 'বিষয়ের নাম',
      noSubjects: 'এখনো কোনো বিষয় নেই। গণিত, পদার্থবিজ্ঞান বা রিভিশন যোগ করুন।',
      filterToday: 'আজ',
      filterUpcoming: 'আসন্ন',
      filterAll: 'সব',
      filterCompleted: 'সম্পন্ন',
      overdueTitle: (count) => `আগের দিনের ${count}টি অসমাপ্ত কাজ রয়েছে`,
      overdueDesc: 'কোনো কাজ বাদ না পড়তে এগুলো আজকের সূচিতে নিয়ে আসুন।',
      moveToToday: 'আজে সরান',
      emptyTasksTitle: 'এই তালিকায় কোনো কাজ নেই',
      emptyTasksDesc: 'বিষয়, অগ্রাধিকার ও লক্ষ্য সময়সহ একটি অধ্যয়নের কাজ যোগ করুন।',
      priorityLabel: 'অগ্রাধিকার',
      priorityLow: 'কম',
      priorityMedium: 'মাঝারি',
      priorityHigh: 'উচ্চ',
      scheduleLabel: 'সময়সূচি',
      scheduleToday: 'আজ',
      scheduleTomorrow: 'আগামীকাল',
      scheduleUnscheduled: 'অনির্ধারিত',
      focusNow: 'ফোকাস',
      taskNotesLabel: 'নোট (ঐচ্ছিক)',
      generalStudy: 'সাধারণ অধ্যয়ন',
      closeLabel: 'বন্ধ',
      deleteLabel: 'মুছুন',

      progressTitle: 'অগ্রগতি ও বিশ্লেষণ',
      progressSubtitle: 'সম্পূর্ণভাবে আপনার স্থানীয় ফোকাস সেশন থেকে গণনাকৃত।',
      periodWeek: '৭ দিন',
      periodMonth: '৩০ দিন',
      totalFocus: 'মোট ফোকাস',
      completionRate: 'সম্পন্নের হার',
      avgSession: 'গড় সেশন',
      tasksDone: 'সম্পন্ন কাজ',
      activityTitle: 'ফোকাস কার্যকলাপ',
      activeDays: (active, total) => `${total} দিনের মধ্যে ${active} দিন সক্রিয়`,
      noActivity: 'দৈনিক অগ্রগতি দেখতে একটি ফোকাস সেশন সম্পন্ন করুন।',
      subjectBreakdownTitle: 'বিষয়ভিত্তিক বিশ্লেষণ',
      noSubjectData: 'এই সময়কালে এখনো কোনো বিষয়ভিত্তিক সম্পন্ন সেশন নেই।',
    },

    phase5: {
      rewardsTitle: 'পুরস্কার ও গার্ডেন',
      rewardsSubtitle: 'কেবল সত্যিকারের সম্পন্ন ফোকাস সেশন থেকেই অর্জিত।',
      lifetimeEarned: (coins) => `মোট অর্জিত ${coins}`,
      streakLabel: 'অধ্যয়নের ধারা',
      sessionsCompleted: (count) => `${count}টি সম্পন্ন সেশন`,
      gardenTitle: 'ভার্চুয়াল গার্ডেন',
      stageSeedling: 'অঙ্কুর',
      stageSprouting: 'চারা',
      stageBlooming: 'প্রস্ফুটিত',
      gardenHintSeedling: 'চারা ধাপে যেতে ২টি ফোকাস সেশন সম্পন্ন করুন।',
      gardenHintSprouting: 'পূর্ণ প্রস্ফুটিত হতে ৫টি ফোকাস সেশন সম্পন্ন করুন।',
      gardenHintBlooming: 'আপনার অধ্যয়ন সাথী সম্পূর্ণ প্রস্ফুটিত হয়েছে।',
      storeTitle: 'গার্ডেন সঙ্গী স্টোর',
      featuredBadge: 'সক্রিয়',
      setFeatured: 'সক্রিয় করুন',
      ledgerTitle: 'সাম্প্রতিক কয়েন বিবরণ',
      ledgerEmpty: 'প্রথম ফোকাস কয়েন অর্জন করতে একটি সেশন সম্পন্ন করুন।',
      sessionCoinsEarned: 'অর্জিত ফোকাস কয়েন',
      purchaseSuccess: 'নতুন গার্ডেন সঙ্গী আনলক হয়েছে।',
      purchaseInsufficient: 'পর্যাপ্ত ফোকাস কয়েন নেই। আরও সেশন সম্পন্ন করুন।',
      purchaseAlready: 'এই সঙ্গী ইতোমধ্যেই আনলক করা আছে।',
      purchaseNotFound: 'পুরস্কারটি পাওয়া যায়নি।',
    },

    phase6: {
      missionsTitle: 'দৈনিক অধ্যয়ন মিশন',
      missionsCompletedSummary: (done, total) => `${done} / ${total} সম্পন্ন`,
      missionsClaimBtn: (coins) => `+${coins} কয়েন নিন`,
      missionsClaimedBadge: 'সংগৃহীত',
      missionsProgressBadge: (current, target) => `${current} / ${target}`,

      focusModeManual: 'ম্যানুয়াল',
      focusModeAuto: 'অটো-সূচি',
      focusModeSmart: 'স্মার্ট',
      focusModeAutoHint: (title, mins) => `পরবর্তী পরিকল্পিত কাজ নির্বাচিত: ${title} (${mins} মি.)`,
      focusModeAutoEmpty: 'কোনো অসমাপ্ত কাজ পাওয়া যায়নি। স্টাডি প্ল্যানারে কাজ যোগ করুন।',
      focusModeSmartHint: (mins) =>
        `আপনার ইতিহাস অনুযায়ী অ্যাডাপ্টিভ সময় (${mins} মি.) ব্যবহৃত হচ্ছে।`,

      breathingActionBtn: '১-মিনিট শান্ত শ্বাস-প্রশ্বাস',
      breathingTitle: '১-মিনিট বক্স শ্বাস-প্রশ্বাস',
      breathingSubtitle: 'পড়াশোনার আগে মন স্থির করতে ৪-৪-৪-৪ ছন্দ অনুসরণ করুন।',
      breathingInhale: 'শ্বাস নিন',
      breathingHold: 'ধরে রাখুন',
      breathingExhale: 'শ্বাস ছাড়ুন',
      breathingRest: 'বিশ্রাম',
      breathingComplete: 'অনুশীলন সম্পন্ন',
      breathingRemainingSeconds: (secs) => `${secs} সে. বাকি`,
      breathingDoneHint: 'মন শান্ত হয়েছে। এখন শুরু করতে প্রস্তুত।',

      backupTitle: 'লোকাল JSON ব্যাকআপ ও পুনরুদ্ধার',
      backupSubtitle:
        'আপনার সেশন, কাজ, বিষয়, পুরস্কার ও পছন্দসমূহ একটি JSON ফাইলে সংরক্ষণ করুন বা আগের ব্যাকআপ থেকে পুনরুদ্ধার করুন।',
      backupExportBtn: 'JSON ব্যাকআপ এক্সপোর্ট',
      backupImportBtn: 'JSON থেকে পুনরুদ্ধার',
      backupExportSuccess: 'ব্যাকআপ JSON ফাইল ডাউনলোড হয়েছে।',
      backupImportSuccess: (sessions, tasks, subjects) =>
        `${sessions}টি সেশন, ${tasks}টি কাজ এবং ${subjects}টি বিষয় পুনরুদ্ধার হয়েছে।`,
      backupImportError: (reason) => `পুনরুদ্ধার ব্যর্থ হয়েছে: ${reason}`,
    },
  },
};
