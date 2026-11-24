import { useEffect, useRef, useState } from 'react';
import {
  BarChart3,
  BookOpen,
  Download,
  Home,
  Languages,
  RotateCcw,
  Shield,
  Sprout,
  Timer,
  Upload,
  User,
} from 'lucide-react';
import { BreathingResetModal } from './BreathingResetModal';
import { DistractionBlockerScreen } from './DistractionBlockerScreen';
import { FocuslyBrandMark } from './FocuslyBrandMark';
import { FocusTimerScreen } from './FocusTimerScreen';
import { HomeDashboard } from './HomeDashboard';
import { ProgressAnalyticsScreen } from './ProgressAnalyticsScreen';
import { RecoveryModal } from './RecoveryModal';
import { RewardsScreen } from './RewardsScreen';
import { StudyPlannerScreen } from './StudyPlannerScreen';
import { focusLocalRepository, type FocusLocalDatabase } from '../data/focusStore';
import type { FocuslyStrings } from '../i18n/translations';
import {
  DISTRACTION_CANDIDATES,
  type ActiveTimerSnapshot,
  type AppLanguage,
  type BottomNavTab,
  type FocusMode,
  type FocusSessionRecord,
  type OnboardingPreferences,
  type RewardPurchaseResult,
  type SessionType,
  type TimerProgress,
} from '../types/focusly';
import { generateAdaptiveRecommendation } from '../utils/adaptiveFocusEngine';
import { evaluateDailyMissions, selectAutoScheduleTask } from '../utils/dailyMissions';
import {
  calculateDailyStats,
  calculateTimerProgress,
  calculateWeeklyStats,
  getLocalDateString,
  SessionStateMachine,
} from '../utils/timerEngine';

interface MainShellProps {
  preferences: OnboardingPreferences;
  strings: FocuslyStrings;
  onLanguageChange: (language: AppLanguage) => void;
  onRestorePreferences: (preferences: OnboardingPreferences) => void;
  onResetOnboarding: () => void;
}

export function MainShell({
  preferences,
  strings,
  onLanguageChange,
  onRestorePreferences,
  onResetOnboarding,
}: MainShellProps) {
  const [activeTab, setActiveTab] = useState<BottomNavTab>('home');
  const [showBlockerSettings, setShowBlockerSettings] = useState(false);
  const [showRewardsScreen, setShowRewardsScreen] = useState(false);
  const [showBreathingModal, setShowBreathingModal] = useState(false);
  const [purchaseMessage, setPurchaseMessage] = useState<RewardPurchaseResult | null>(null);
  const [showResetConfirm, setShowResetConfirm] = useState(false);
  const [backupFeedback, setBackupFeedback] = useState<{ ok: boolean; message: string } | null>(
    null,
  );
  const importFileInputRef = useRef<HTMLInputElement | null>(null);

  const [localDb, setLocalDb] = useState<FocusLocalDatabase>(() =>
    focusLocalRepository.syncBlockerFromOnboarding(
      preferences.selectedDistractionApps,
      preferences.distractionPreferencesSkipped,
    ),
  );
  const [sessionType, setSessionType] = useState<SessionType>('FOCUS');
  const [focusMode, setFocusMode] = useState<FocusMode>('MANUAL');
  const [selectedDurationMinutes, setSelectedDurationMinutes] = useState<number>(25);
  const [selectedSubjectId, setSelectedSubjectId] = useState<string | null>(null);
  const [selectedTaskId, setSelectedTaskId] = useState<string | null>(null);
  const [lastCompletedSession, setLastCompletedSession] = useState<FocusSessionRecord | null>(null);
  const [timerProgress, setTimerProgress] = useState<TimerProgress | null>(() => {
    const initialActive = focusLocalRepository.load().activeTimer;
    return initialActive ? calculateTimerProgress(initialActive, Date.now()) : null;
  });

  const [pendingRecovery, setPendingRecovery] = useState<ActiveTimerSnapshot | null>(() => {
    const saved = focusLocalRepository.load().activeTimer;
    if (saved && (saved.status === 'RUNNING' || saved.status === 'PAUSED')) {
      return saved;
    }
    return null;
  });

  const [nameDraft, setNameDraft] = useState<string>(() => focusLocalRepository.load().studentName);

  // Sync unfinished session on initial mount if it already crossed completion while closed
  useEffect(() => {
    const saved = localDb.activeTimer;
    if (!saved || (saved.status !== 'RUNNING' && saved.status !== 'PAUSED')) return;
    const progress = calculateTimerProgress(saved, Date.now());
    if (progress.isCompleted) {
      const { db, recordedSession } = focusLocalRepository.recordSessionCompletion(
        saved,
        'COMPLETED',
        saved.plannedDurationSeconds,
        Date.now(),
        preferences.dailyFocusTargetMinutes,
      );
      setLocalDb(db);
      setTimerProgress(null);
      setPendingRecovery(null);
      setLastCompletedSession(recordedSession);
    }
    // Only check once on mount
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Clock-based tick loop (no counter decrement drift across background/tab throttling)
  useEffect(() => {
    const active = localDb.activeTimer;
    if (!active || (active.status !== 'RUNNING' && active.status !== 'PAUSED')) {
      setTimerProgress(null);
      return;
    }

    const syncTick = () => {
      const nowMs = Date.now();
      const progress = calculateTimerProgress(active, nowMs);
      if (progress.isCompleted) {
        const { db, recordedSession } = focusLocalRepository.recordSessionCompletion(
          active,
          'COMPLETED',
          active.plannedDurationSeconds,
          nowMs,
          preferences.dailyFocusTargetMinutes,
        );
        setLocalDb(db);
        setTimerProgress(null);
        setPendingRecovery(null);
        setLastCompletedSession(recordedSession);
      } else {
        setTimerProgress(progress);
      }
    };

    syncTick();
    if (active.status === 'PAUSED') return;

    const interval = window.setInterval(syncTick, 500);
    const onVisibility = () => {
      if (document.visibilityState === 'visible') syncTick();
    };
    document.addEventListener('visibilitychange', onVisibility);
    return () => {
      window.clearInterval(interval);
      document.removeEventListener('visibilitychange', onVisibility);
    };
  }, [localDb.activeTimer]);

  const todayStr = getLocalDateString();
  const dailyStats = calculateDailyStats(
    localDb.sessions,
    preferences.dailyFocusTargetMinutes,
    todayStr,
  );
  const weeklyStats = calculateWeeklyStats(
    localDb.sessions,
    todayStr,
    preferences.language,
  );

  const currentTask = localDb.tasks.find((t) => t.status !== 'COMPLETED') ?? null;

  const baseRecommendation = generateAdaptiveRecommendation(
    localDb.sessions,
    preferences.dailyFocusTargetMinutes,
    selectedSubjectId,
  );
  const savedRecStatus = localDb.recommendationDecisions[baseRecommendation.id];
  const recommendation = savedRecStatus
    ? { ...baseRecommendation, status: savedRecStatus }
    : baseRecommendation;
  const rewardPreview = focusLocalRepository.getRewardPreview();
  const rewardsDashboard = focusLocalRepository.getRewardsDashboard(todayStr);
  const claimedMissionIds = focusLocalRepository.getClaimedMissionIdsForDate(todayStr);
  const dailyMissions = evaluateDailyMissions(
    localDb.sessions,
    localDb.tasks,
    preferences.dailyFocusTargetMinutes,
    todayStr,
    claimedMissionIds,
  );

  const navItems: { id: BottomNavTab; label: string; icon: typeof Home }[] = [
    { id: 'home', label: strings.nav.home, icon: Home },
    { id: 'focus', label: strings.nav.focus, icon: Timer },
    { id: 'study', label: strings.nav.study, icon: BookOpen },
    { id: 'progress', label: strings.nav.progress, icon: BarChart3 },
    { id: 'profile', label: strings.nav.profile, icon: User },
  ];

  const categoryLabel = strings.categories[preferences.studentCategory].title;
  const goalsSummary = preferences.studyGoals.map((g) => strings.goals[g]).join(' · ');
  const selectedAppNames = localDb.distractionBlocker.selectedPackageNames
    .map((pkg) => DISTRACTION_CANDIDATES.find((c) => c.packageName === pkg)?.label ?? pkg)
    .join(', ');

  const handleCreateQuickTask = (
    title: string,
    subjectName: string,
    plannedMinutes: number,
  ) => {
    const { db } = focusLocalRepository.createQuickTask(title, subjectName, plannedMinutes);
    setLocalDb(db);
  };

  const startSessionWithDuration = (durationMinutes: number, type: SessionType = 'FOCUS') => {
    const nowMs = Date.now();
    const clamped = Math.min(240, Math.max(5, durationMinutes));
    const snapshot: ActiveTimerSnapshot = {
      sessionId: `sess-${nowMs}-${Math.random().toString(36).slice(2, 7)}`,
      sessionType: type,
      plannedDurationSeconds: clamped * 60,
      startTimestampMs: nowMs,
      pauseStartedTimestampMs: null,
      accumulatedPausedMs: 0,
      status: 'RUNNING',
      subjectId: type === 'FOCUS' ? selectedSubjectId : null,
      taskId: type === 'FOCUS' ? selectedTaskId : null,
    };
    setSessionType(type);
    setSelectedDurationMinutes(clamped);
    setLastCompletedSession(null);
    focusLocalRepository.dismissRecoveryState();
    setLocalDb(focusLocalRepository.saveActiveTimer(snapshot));
  };

  const handleStartSession = () => {
    if (localDb.activeTimer) return;
    startSessionWithDuration(selectedDurationMinutes, sessionType);
  };

  const handlePauseSession = () => {
    const active = localDb.activeTimer;
    if (!active || active.status !== 'RUNNING') return;
    const paused = SessionStateMachine.pause(active, Date.now());
    setLocalDb(focusLocalRepository.saveActiveTimer(paused));
  };

  const handleResumeSession = () => {
    const active = localDb.activeTimer;
    if (!active || active.status !== 'PAUSED') return;
    const resumed = SessionStateMachine.resume(active, Date.now());
    setLocalDb(focusLocalRepository.saveActiveTimer(resumed));
  };

  const handleEndEarly = () => {
    const active = localDb.activeTimer;
    if (!active) return;
    const nowMs = Date.now();
    const progress = calculateTimerProgress(active, nowMs);
    const { db } = focusLocalRepository.recordSessionCompletion(
      active,
      'ENDED_EARLY',
      progress.elapsedSeconds,
      nowMs,
    );
    setLocalDb(db);
    setTimerProgress(null);
    setPendingRecovery(null);
  };

  const handleRestartShorterFromRecovery = (minutes: number) => {
    const active = localDb.activeTimer;
    if (active) {
      const nowMs = Date.now();
      const progress = calculateTimerProgress(active, nowMs);
      focusLocalRepository.recordSessionCompletion(
        active,
        'ENDED_EARLY',
        progress.elapsedSeconds,
        nowMs,
      );
      setTimerProgress(null);
      setPendingRecovery(null);
    }
    startSessionWithDuration(minutes, 'FOCUS');
    setShowBlockerSettings(false);
    setActiveTab('focus');
  };

  const handleSelectFocusMode = (mode: FocusMode) => {
    if (localDb.activeTimer) return;
    setFocusMode(mode);
    setSessionType('FOCUS');
    setLastCompletedSession(null);

    if (mode === 'AUTO_SCHEDULE') {
      const picked = selectAutoScheduleTask(localDb.tasks, todayStr);
      if (picked) {
        setSelectedTaskId(picked.id);
        setSelectedSubjectId(picked.subjectId);
        setSelectedDurationMinutes(Math.min(240, Math.max(5, picked.plannedDurationMinutes)));
      }
    } else if (mode === 'SMART') {
      const smartMins = recommendation.hasEnoughData
        ? Math.min(240, Math.max(5, recommendation.recommendedMinutes))
        : 25;
      setSelectedDurationMinutes(smartMins);
    }
  };

  const handleExportBackup = () => {
    const json = focusLocalRepository.exportBackupJson(preferences);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `focusly-backup-${todayStr}.json`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
    setBackupFeedback({ ok: true, message: strings.phase6.backupExportSuccess });
  };

  const handleImportBackupFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      const raw = typeof reader.result === 'string' ? reader.result : '';
      const result = focusLocalRepository.importBackupJson(raw);
      if (result.ok) {
        const refreshed = focusLocalRepository.load();
        setLocalDb(refreshed);
        setNameDraft(refreshed.studentName);
        onRestorePreferences(result.preferences);
        setBackupFeedback({
          ok: true,
          message: strings.phase6.backupImportSuccess(
            result.sessionsCount,
            result.tasksCount,
            result.subjectsCount,
          ),
        });
      } else {
        setBackupFeedback({
          ok: false,
          message: strings.phase6.backupImportError(result.error),
        });
      }
    };
    reader.readAsText(file);
  };

  const handleFullReset = () => {
    focusLocalRepository.clearAll();
    setLocalDb(focusLocalRepository.load());
    setShowResetConfirm(false);
    onResetOnboarding();
  };

  return (
    <div className="min-h-screen w-full bg-[#080F1F] text-white flex flex-col md:flex-row">
      {/* Desktop Navigation Rail */}
      <aside className="hidden md:flex md:w-64 lg:w-72 shrink-0 flex-col justify-between bg-[#101B32] border-r border-[#233554] p-6">
        <div className="space-y-8">
          <div className="flex items-center gap-3">
            <FocuslyBrandMark size={40} />
            <span className="text-lg font-bold tracking-[0.1em] text-white whitespace-nowrap">
              {strings.appName}
            </span>
          </div>

          <nav aria-label="Main navigation" className="space-y-1.5">
            {navItems.map((item) => {
              const Icon = item.icon;
              const selected = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => {
                    setShowBlockerSettings(false);
                    setShowRewardsScreen(false);
                    setActiveTab(item.id);
                  }}
                  className={`w-full min-h-[48px] px-4 py-2.5 rounded-xl flex items-center gap-3 text-sm font-medium transition-colors duration-150 whitespace-nowrap cursor-pointer ${
                    selected && !showBlockerSettings && !showRewardsScreen
                      ? 'bg-[#3B82F6]/15 text-[#3B82F6]'
                      : 'text-[#A7B4C8] hover:text-white hover:bg-[#16243D]'
                  }`}
                >
                  <Icon className="w-5 h-5 shrink-0" />
                  <span>{item.label}</span>
                </button>
              );
            })}
          </nav>
        </div>

        <div className="pt-6 border-t border-[#233554] space-y-1">
          <div className="text-xs font-medium text-[#A7B4C8]">{categoryLabel}</div>
          <div className="text-sm font-semibold text-white font-mono-tabular">
            {dailyStats.completedFocusMinutes} / {preferences.dailyFocusTargetMinutes}{' '}
            {strings.home.minutesUnit}
          </div>
        </div>
      </aside>

      {/* Main Content Workspace */}
      <main className="flex-1 min-w-0 pb-24 md:pb-12">
        <div className="max-w-5xl mx-auto px-5 sm:px-8 py-6 sm:py-10">
          {showBlockerSettings && (
            <DistractionBlockerScreen
              strings={strings}
              settings={localDb.distractionBlocker}
              onBack={() => setShowBlockerSettings(false)}
              onToggleEnabled={(enabled) =>
                setLocalDb(focusLocalRepository.setBlockerEnabled(enabled))
              }
              onToggleApp={(pkg) =>
                setLocalDb(focusLocalRepository.toggleBlockerApp(pkg))
              }
              onSetPermissionStatus={(perm) =>
                setLocalDb(focusLocalRepository.setBlockerPermissionStatus(perm))
              }
            />
          )}

          {showRewardsScreen && !showBlockerSettings && (
            <RewardsScreen
              strings={strings}
              dashboard={rewardsDashboard}
              purchaseMessage={purchaseMessage}
              onBack={() => {
                setPurchaseMessage(null);
                setShowRewardsScreen(false);
              }}
              onPurchaseItem={(itemId) => {
                const { db, result } = focusLocalRepository.purchaseStoreItem(itemId, todayStr);
                setLocalDb(db);
                setPurchaseMessage(result);
              }}
              onSetFeaturedItem={(itemId) => {
                setLocalDb(focusLocalRepository.setFeaturedGardenItem(itemId));
              }}
              onDismissPurchaseMessage={() => setPurchaseMessage(null)}
            />
          )}

          {!showBlockerSettings && !showRewardsScreen && activeTab === 'home' && (
            <HomeDashboard
              strings={strings}
              studentName={localDb.studentName}
              categoryLabel={categoryLabel}
              dailyStats={dailyStats}
              weeklyStats={weeklyStats}
              currentTask={currentTask}
              subjects={localDb.subjects}
              activeTimer={localDb.activeTimer}
              recommendation={recommendation}
              blockerSettings={localDb.distractionBlocker}
              recoveryState={localDb.recoveryState}
              rewardPreview={rewardPreview}
              dailyMissions={dailyMissions}
              onStartFocus={() => setActiveTab('focus')}
              onStartRecommendedFocus={(mins, recId) => {
                focusLocalRepository.setRecommendationDecision(recId, 'ACCEPTED');
                startSessionWithDuration(mins, 'FOCUS');
                setActiveTab('focus');
              }}
              onIgnoreRecommendation={(recId) => {
                setLocalDb(focusLocalRepository.setRecommendationDecision(recId, 'IGNORED'));
              }}
              onRestartShorterFromRecovery={handleRestartShorterFromRecovery}
              onTakeRecoveryBreak={() => {
                setLocalDb(focusLocalRepository.dismissRecoveryState());
                setSessionType('BREAK');
                setSelectedDurationMinutes(5);
                setActiveTab('focus');
              }}
              onDismissRecovery={() =>
                setLocalDb(focusLocalRepository.dismissRecoveryState())
              }
              onOpenBreathingReset={() => setShowBreathingModal(true)}
              onOpenBlockerSettings={() => setShowBlockerSettings(true)}
              onOpenRewards={() => {
                setPurchaseMessage(null);
                setShowRewardsScreen(true);
              }}
              onClaimMission={(missionId) => {
                const { db } = focusLocalRepository.claimDailyMission(missionId, todayStr);
                setLocalDb(db);
              }}
              onOpenSettings={() => setActiveTab('profile')}
              onCreateQuickTask={handleCreateQuickTask}
            />
          )}

          {!showBlockerSettings && !showRewardsScreen && activeTab === 'focus' && (
            <FocusTimerScreen
              strings={strings}
              activeTimer={localDb.activeTimer}
              timerProgress={timerProgress}
              sessionType={sessionType}
              selectedDurationMinutes={selectedDurationMinutes}
              selectedSubjectId={selectedSubjectId}
              selectedTaskId={selectedTaskId}
              subjects={localDb.subjects}
              tasks={localDb.tasks}
              recentSessions={localDb.sessions}
              lastCompletedSession={lastCompletedSession}
              dailyStats={dailyStats}
              recommendation={recommendation}
              blockerSettings={localDb.distractionBlocker}
              recoveryState={localDb.recoveryState}
              focusMode={focusMode}
              onSelectSessionType={(nextType) => {
                setSessionType(nextType);
                setSelectedDurationMinutes(nextType === 'FOCUS' ? 25 : 5);
                setLastCompletedSession(null);
              }}
              onSelectFocusMode={handleSelectFocusMode}
              onSelectDurationMinutes={(mins) => setSelectedDurationMinutes(mins)}
              onSelectSubject={(id) => setSelectedSubjectId(id)}
              onSelectTask={(id) => {
                setSelectedTaskId(id);
                const task = localDb.tasks.find((t) => t.id === id);
                if (task?.subjectId) setSelectedSubjectId(task.subjectId);
              }}
              onApplyRecommendation={(mins, recId) => {
                setSelectedDurationMinutes(mins);
                setLocalDb(focusLocalRepository.setRecommendationDecision(recId, 'ACCEPTED'));
              }}
              onIgnoreRecommendation={(recId) => {
                setLocalDb(focusLocalRepository.setRecommendationDecision(recId, 'IGNORED'));
              }}
              onRestartShorterFromRecovery={handleRestartShorterFromRecovery}
              onDismissRecovery={() =>
                setLocalDb(focusLocalRepository.dismissRecoveryState())
              }
              onOpenBreathingReset={() => setShowBreathingModal(true)}
              onOpenBlockerSettings={() => setShowBlockerSettings(true)}
              onStart={handleStartSession}
              onPause={handlePauseSession}
              onResume={handleResumeSession}
              onEndEarly={handleEndEarly}
              onStartAnother={() => {
                setLastCompletedSession(null);
                setSessionType('FOCUS');
                setSelectedDurationMinutes(25);
              }}
              onTakeBreak={() => {
                setLastCompletedSession(null);
                setSessionType('BREAK');
                setSelectedDurationMinutes(5);
              }}
              onReturnHome={() => {
                setLastCompletedSession(null);
                setActiveTab('home');
              }}
            />
          )}

          {!showBlockerSettings && !showRewardsScreen && activeTab === 'study' && (
            <StudyPlannerScreen
              strings={strings}
              subjects={localDb.subjects}
              tasks={localDb.tasks}
              onCreateSubject={(name, color) => {
                setLocalDb(focusLocalRepository.upsertSubject(name, color).db);
              }}
              onDeleteSubject={(subjectId) => {
                setLocalDb(focusLocalRepository.deleteSubject(subjectId));
              }}
              onSaveTask={(editingTaskId, input) => {
                if (editingTaskId) {
                  setLocalDb(focusLocalRepository.updateTask(editingTaskId, input));
                } else {
                  setLocalDb(focusLocalRepository.createTask(input).db);
                }
              }}
              onToggleTaskStatus={(task) => {
                const nextStatus = task.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED';
                setLocalDb(focusLocalRepository.updateTaskStatus(task.id, nextStatus));
              }}
              onRescheduleTask={(taskId, scheduledDate) => {
                setLocalDb(focusLocalRepository.rescheduleTask(taskId, scheduledDate));
              }}
              onRescheduleAllOverdue={() => {
                setLocalDb(focusLocalRepository.rescheduleUnfinishedTasksToToday(todayStr).db);
              }}
              onDeleteTask={(taskId) => {
                setLocalDb(focusLocalRepository.deleteTask(taskId));
              }}
              onStartFocusForTask={(task) => {
                if (!localDb.activeTimer) {
                  setSessionType('FOCUS');
                  setSelectedDurationMinutes(Math.min(240, Math.max(5, task.plannedDurationMinutes)));
                  setSelectedSubjectId(task.subjectId);
                  setSelectedTaskId(task.id);
                  setLastCompletedSession(null);
                }
                setActiveTab('focus');
              }}
            />
          )}

          {!showBlockerSettings && !showRewardsScreen && activeTab === 'progress' && (
            <ProgressAnalyticsScreen
              strings={strings}
              language={preferences.language}
              sessions={localDb.sessions}
              subjects={localDb.subjects}
              tasks={localDb.tasks}
              recommendation={recommendation}
              onStartRecommendedFocus={(mins, recId) => {
                focusLocalRepository.setRecommendationDecision(recId, 'ACCEPTED');
                startSessionWithDuration(mins, 'FOCUS');
                setActiveTab('focus');
              }}
              onIgnoreRecommendation={(recId) => {
                setLocalDb(focusLocalRepository.setRecommendationDecision(recId, 'IGNORED'));
              }}
            />
          )}

          {!showBlockerSettings && !showRewardsScreen && activeTab === 'profile' && (
            <section className="space-y-6">
              <header className="space-y-1.5">
                <h1 className="text-2xl sm:text-3xl font-bold text-white">
                  {strings.profile.title}
                </h1>
                <p className="text-base text-[#A7B4C8]">{strings.profile.subtitle}</p>
              </header>

              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="space-y-6">
                  <div className="rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-3">
                    <h2 className="text-base font-semibold text-white">
                      {strings.profile.studentNameLabel}
                    </h2>
                    <div className="flex flex-col sm:flex-row gap-3">
                      <input
                        type="text"
                        value={nameDraft}
                        onChange={(e) => setNameDraft(e.target.value)}
                        placeholder={strings.profile.studentNamePlaceholder}
                        className="flex-1 min-h-[46px] px-3.5 rounded-xl bg-[#101B32] border border-[#233554] focus:border-[#3B82F6] focus:outline-none text-white text-sm"
                      />
                      <button
                        type="button"
                        onClick={() => setLocalDb(focusLocalRepository.updateStudentName(nameDraft))}
                        className="min-h-[46px] px-5 py-2.5 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white text-sm font-semibold whitespace-nowrap cursor-pointer"
                      >
                        {strings.profile.saveNameLabel}
                      </button>
                    </div>
                  </div>

                  <div className="rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-4">
                    <h2 className="text-base font-semibold text-white">
                      {strings.profile.savedPreferences}
                    </h2>
                    <div className="space-y-3 text-sm">
                      <div className="flex justify-between gap-4 py-2 border-b border-[#233554]">
                        <span className="text-[#A7B4C8]">{strings.profile.categoryLabel}</span>
                        <span className="font-medium text-white text-right">{categoryLabel}</span>
                      </div>
                      <div className="flex justify-between gap-4 py-2 border-b border-[#233554]">
                        <span className="text-[#A7B4C8]">{strings.profile.dailyTargetLabel}</span>
                        <span className="font-medium text-white font-mono-tabular">
                          {preferences.dailyFocusTargetMinutes} {strings.home.minutesUnit}
                        </span>
                      </div>
                      <div className="flex flex-col gap-1 py-2 border-b border-[#233554]">
                        <span className="text-[#A7B4C8]">{strings.profile.goalsLabel}</span>
                        <span className="font-medium text-white">{goalsSummary}</span>
                      </div>
                      <div className="flex flex-col gap-1 py-2">
                        <span className="text-[#A7B4C8]">{strings.profile.distractionLabel}</span>
                        <span className="font-medium text-white">
                          {selectedAppNames || strings.home.noneSelected}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="space-y-6">
                  <div className="rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-4">
                    <div className="flex items-center gap-2.5">
                      <Languages className="w-5 h-5 text-[#22D3EE]" />
                      <h2 className="text-base font-semibold text-white">
                        {strings.profile.languageSectionTitle}
                      </h2>
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                      {(['en', 'bn'] as AppLanguage[]).map((lang) => {
                        const selected = preferences.language === lang;
                        return (
                          <button
                            key={lang}
                            type="button"
                            onClick={() => onLanguageChange(lang)}
                            className={`min-h-[48px] px-4 py-3 rounded-xl border text-sm font-semibold transition-colors duration-150 whitespace-nowrap cursor-pointer ${
                              selected
                                ? 'bg-[#3B82F6] border-[#3B82F6] text-white'
                                : 'bg-[#101B32] border-[#233554] text-[#A7B4C8] hover:text-white'
                            }`}
                          >
                            {lang === 'en' ? 'English' : 'বাংলা'}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  <div className="rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-3">
                    <div className="flex items-center gap-2.5">
                      <Shield className="w-5 h-5 text-[#22D3EE]" />
                      <h2 className="text-base font-semibold text-white">
                        {strings.phase3.blockerTitle}
                      </h2>
                    </div>
                    <p className="text-sm text-[#A7B4C8] leading-relaxed">
                      {strings.phase3.blockerSubtitle}
                    </p>
                    <button
                      type="button"
                      onClick={() => setShowBlockerSettings(true)}
                      className="min-h-[44px] px-4 py-2 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white text-sm font-semibold cursor-pointer"
                    >
                      {strings.phase3.blockerManage}
                    </button>
                  </div>

                  <div className="rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-3">
                    <div className="flex items-center gap-2.5">
                      <Sprout className="w-5 h-5 text-[#22C55E]" />
                      <h2 className="text-base font-semibold text-white">
                        {strings.phase5.rewardsTitle}
                      </h2>
                    </div>
                    <p className="text-sm text-[#A7B4C8] leading-relaxed">
                      {strings.phase5.rewardsSubtitle}
                    </p>
                    <button
                      type="button"
                      onClick={() => {
                        setPurchaseMessage(null);
                        setShowRewardsScreen(true);
                      }}
                      className="min-h-[44px] px-4 py-2 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white text-sm font-semibold cursor-pointer"
                    >
                      {strings.dashboard.viewRewards}
                    </button>
                  </div>

                  <div className="rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-3">
                    <div className="flex items-center gap-2.5">
                      <Download className="w-5 h-5 text-[#22D3EE]" />
                      <h2 className="text-base font-semibold text-white">
                        {strings.phase6.backupTitle}
                      </h2>
                    </div>
                    <p className="text-sm text-[#A7B4C8] leading-relaxed">
                      {strings.phase6.backupSubtitle}
                    </p>
                    {backupFeedback && (
                      <p
                        className={`text-xs font-medium ${
                          backupFeedback.ok ? 'text-[#22C55E]' : 'text-[#F59E0B]'
                        }`}
                      >
                        {backupFeedback.message}
                      </p>
                    )}
                    <input
                      ref={importFileInputRef}
                      type="file"
                      accept=".json,application/json"
                      onChange={handleImportBackupFile}
                      className="hidden"
                    />
                    <div className="flex flex-wrap gap-3 pt-1">
                      <button
                        type="button"
                        onClick={handleExportBackup}
                        className="min-h-[44px] px-4 py-2 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white text-sm font-semibold inline-flex items-center gap-2 whitespace-nowrap cursor-pointer"
                      >
                        <Download className="w-4 h-4 shrink-0" />
                        <span>{strings.phase6.backupExportBtn}</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => importFileInputRef.current?.click()}
                        className="min-h-[44px] px-4 py-2 rounded-xl bg-[#101B32] border border-[#233554] hover:border-[#3B82F6] text-white text-sm font-semibold inline-flex items-center gap-2 whitespace-nowrap cursor-pointer"
                      >
                        <Upload className="w-4 h-4 shrink-0" />
                        <span>{strings.phase6.backupImportBtn}</span>
                      </button>
                    </div>
                  </div>

                  <div className="rounded-2xl bg-[#101B32] border border-[#233554] p-6 space-y-3">
                    <div className="flex items-center gap-2.5">
                      <Shield className="w-5 h-5 text-[#22C55E]" />
                      <h2 className="text-base font-semibold text-white">
                        {strings.profile.privacySectionTitle}
                      </h2>
                    </div>
                    <p className="text-sm text-[#A7B4C8] leading-relaxed">
                      {strings.profile.privacyBody}
                    </p>
                  </div>
                </div>
              </div>

              <div>
                <button
                  type="button"
                  onClick={() => setShowResetConfirm(true)}
                  className="min-h-[48px] px-5 py-3 rounded-xl border border-[#233554] bg-[#101B32] hover:border-[#F59E0B]/60 text-sm font-medium text-[#A7B4C8] hover:text-white inline-flex items-center gap-2.5 transition-colors duration-150 cursor-pointer"
                >
                  <RotateCcw className="w-4 h-4" />
                  <span>{strings.profile.resetButton}</span>
                </button>
              </div>
            </section>
          )}
        </div>
      </main>

      {/* Mobile Fixed 5-Tab Bottom Bar */}
      <nav
        aria-label="Bottom navigation"
        className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-[#101B32]/95 backdrop-blur-md border-t border-[#233554]"
      >
        <div className="grid grid-cols-5 h-16">
          {navItems.map((item) => {
            const Icon = item.icon;
            const selected = activeTab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => {
                  setShowBlockerSettings(false);
                  setShowRewardsScreen(false);
                  setActiveTab(item.id);
                }}
                className={`min-h-[44px] flex flex-col items-center justify-center gap-1 text-[11px] font-medium transition-colors duration-150 whitespace-nowrap cursor-pointer ${
                  selected && !showBlockerSettings && !showRewardsScreen
                    ? 'text-[#3B82F6]'
                    : 'text-[#A7B4C8]'
                }`}
              >
                <Icon className="w-5 h-5 shrink-0" />
                <span className="truncate max-w-[64px]">{item.label}</span>
              </button>
            );
          })}
        </div>
      </nav>

      {/* Unfinished Session Recovery Dialog */}
      {pendingRecovery && (
        <RecoveryModal
          strings={strings}
          snapshot={pendingRecovery}
          onResume={() => {
            if (pendingRecovery.status === 'PAUSED') {
              handleResumeSession();
            }
            setPendingRecovery(null);
            setActiveTab('focus');
          }}
          onRestartShorter={handleRestartShorterFromRecovery}
          onEndSession={handleEndEarly}
        />
      )}

      {/* Reset Confirmation Modal */}
      {showResetConfirm && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-5"
        >
          <div className="w-full max-w-md rounded-2xl bg-[#101B32] border border-[#233554] p-6 space-y-4">
            <h2 className="text-lg font-bold text-white">
              {strings.profile.resetConfirmTitle}
            </h2>
            <p className="text-sm text-[#A7B4C8] leading-relaxed">
              {strings.profile.resetConfirmBody}
            </p>
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setShowResetConfirm(false)}
                className="min-h-[44px] px-4 py-2 rounded-xl border border-[#233554] text-sm font-medium text-[#A7B4C8] hover:text-white cursor-pointer"
              >
                {strings.profile.cancelLabel}
              </button>
              <button
                type="button"
                onClick={handleFullReset}
                className="min-h-[44px] px-4 py-2 rounded-xl bg-[#F59E0B] text-[#080F1F] text-sm font-semibold cursor-pointer"
              >
                {strings.profile.confirmResetLabel}
              </button>
            </div>
          </div>
        </div>
      )}
      {/* 1-Minute Box Breathing Calm Reset Modal */}
      {showBreathingModal && (
        <BreathingResetModal
          strings={strings}
          onClose={() => setShowBreathingModal(false)}
        />
      )}
    </div>
  );
}
