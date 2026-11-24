import { useState } from 'react';
import {
  Bell,
  BookOpen,
  CheckCircle2,
  Coins,
  Flag,
  Play,
  Plus,
  RefreshCw,
  Settings,
  Shield,
  Sparkles,
  Trophy,
  Wind,
} from 'lucide-react';
import { FocuslyBrandMark } from './FocuslyBrandMark';
import type { FocuslyStrings } from '../i18n/translations';
import type {
  ActiveTimerSnapshot,
  DailyMissionItem,
  DailyStats,
  DistractionBlockerSettings,
  FocusRecommendation,
  FocusRecoveryState,
  RewardPreview,
  StudyTaskItem,
  SubjectItem,
  WeeklyStats,
} from '../types/focusly';
import { resolveDistractionBlockerMode } from '../utils/adaptiveFocusEngine';

interface HomeDashboardProps {
  strings: FocuslyStrings;
  studentName: string;
  categoryLabel: string;
  dailyStats: DailyStats;
  weeklyStats: WeeklyStats;
  currentTask: StudyTaskItem | null;
  subjects: SubjectItem[];
  activeTimer: ActiveTimerSnapshot | null;
  recommendation: FocusRecommendation;
  blockerSettings: DistractionBlockerSettings;
  recoveryState: FocusRecoveryState | null;
  rewardPreview: RewardPreview;
  dailyMissions: DailyMissionItem[];
  onStartFocus: () => void;
  onStartRecommendedFocus: (minutes: number, recId: string) => void;
  onIgnoreRecommendation: (recId: string) => void;
  onRestartShorterFromRecovery: (minutes: number) => void;
  onTakeRecoveryBreak: () => void;
  onDismissRecovery: () => void;
  onOpenBreathingReset: () => void;
  onOpenBlockerSettings: () => void;
  onOpenRewards: () => void;
  onClaimMission: (missionId: string) => void;
  onOpenSettings: () => void;
  onCreateQuickTask: (title: string, subjectName: string, plannedMinutes: number) => void;
}

function getGreeting(strings: FocuslyStrings): string {
  const hour = new Date().getHours();
  if (hour < 12) return strings.greetings.morning;
  if (hour < 17) return strings.greetings.afternoon;
  return strings.greetings.evening;
}

export function HomeDashboard({
  strings,
  studentName,
  categoryLabel,
  dailyStats,
  weeklyStats,
  currentTask,
  subjects,
  activeTimer,
  recommendation,
  blockerSettings,
  recoveryState,
  rewardPreview,
  dailyMissions,
  onStartFocus,
  onStartRecommendedFocus,
  onIgnoreRecommendation,
  onRestartShorterFromRecovery,
  onTakeRecoveryBreak,
  onDismissRecovery,
  onOpenBreathingReset,
  onOpenBlockerSettings,
  onOpenRewards,
  onClaimMission,
  onOpenSettings,
  onCreateQuickTask,
}: HomeDashboardProps) {
  const [showAddTaskModal, setShowAddTaskModal] = useState(false);
  const [showNotifModal, setShowNotifModal] = useState(false);

  const [taskTitle, setTaskTitle] = useState('');
  const [taskSubject, setTaskSubject] = useState('');
  const [taskMinutes, setTaskMinutes] = useState('25');

  const greetingText = studentName.trim()
    ? `${getGreeting(strings)}, ${studentName.trim()}`
    : getGreeting(strings);

  const radius = 50;
  const circumference = 2 * Math.PI * radius;
  const clampedPct = Math.min(100, Math.max(0, dailyStats.completionPercentage));
  const strokeOffset = circumference - (clampedPct / 100) * circumference;

  const taskSubjectName = currentTask?.subjectId
    ? subjects.find((s) => s.id === currentTask.subjectId)?.subjectName ?? null
    : null;

  const maxWeeklyDayMinutes = Math.max(
    1,
    ...weeklyStats.dailyBreakdown.map((d) => d.focusMinutes),
  );

  const blockerMode = resolveDistractionBlockerMode(blockerSettings);
  const blockerStatusLabel =
    blockerMode === 'ACTIVE_DETECTION'
      ? strings.phase3.blockerStatusActive
      : blockerMode === 'CHECKLIST_ONLY'
        ? strings.phase3.blockerStatusChecklist
        : strings.phase3.blockerStatusDisabled;

  const handleSaveTask = (e: React.FormEvent) => {
    e.preventDefault();
    if (!taskTitle.trim()) return;
    const mins = Number.parseInt(taskMinutes, 10);
    onCreateQuickTask(
      taskTitle.trim(),
      taskSubject.trim(),
      Number.isNaN(mins) ? 25 : Math.min(480, Math.max(5, mins)),
    );
    setTaskTitle('');
    setTaskSubject('');
    setTaskMinutes('25');
    setShowAddTaskModal(false);
  };

  return (
    <div className="space-y-6">
      {/* A. Header */}
      <header className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3.5 min-w-0">
          <div className="md:hidden">
            <FocuslyBrandMark size={42} />
          </div>
          <div className="min-w-0">
            <p className="text-xs sm:text-sm font-medium text-[#22D3EE] truncate">
              {categoryLabel}
            </p>
            <h1 className="text-2xl sm:text-3xl font-bold text-white truncate">
              {greetingText}
            </h1>
          </div>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <button
            type="button"
            onClick={() => setShowNotifModal(true)}
            aria-label={strings.dashboard.notificationsTitle}
            className="min-h-[44px] min-w-[44px] rounded-xl bg-[#101B32] border border-[#233554] hover:border-[#3B82F6]/60 flex items-center justify-center text-[#A7B4C8] hover:text-white transition-colors duration-150 cursor-pointer"
          >
            <Bell className="w-5 h-5" />
          </button>
          <button
            type="button"
            onClick={onOpenSettings}
            aria-label={strings.nav.profile}
            className="min-h-[44px] min-w-[44px] rounded-xl bg-[#101B32] border border-[#233554] hover:border-[#3B82F6]/60 flex items-center justify-center text-[#A7B4C8] hover:text-white transition-colors duration-150 cursor-pointer"
          >
            <Settings className="w-5 h-5" />
          </button>
        </div>
      </header>

      {/* B. Daily Focus Progress Card + C. Start Focus Card */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
        <section className="lg:col-span-7 rounded-2xl bg-[#16243D] border border-[#233554] p-6 flex flex-col sm:flex-row items-center gap-6">
          <div className="relative w-36 h-36 shrink-0 flex items-center justify-center">
            <svg className="w-full h-full -rotate-90" viewBox="0 0 120 120">
              <circle
                cx="60"
                cy="60"
                r={radius}
                fill="none"
                stroke="#101B32"
                strokeWidth="10"
              />
              <circle
                cx="60"
                cy="60"
                r={radius}
                fill="none"
                stroke={clampedPct >= 100 ? '#22C55E' : '#3B82F6'}
                strokeWidth="10"
                strokeLinecap="round"
                strokeDasharray={circumference}
                strokeDashoffset={strokeOffset}
                className="transition-all duration-300"
              />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <span className="text-2xl font-bold text-white font-mono-tabular">
                {dailyStats.completionPercentage}%
              </span>
              <span className="text-xs text-[#A7B4C8]">
                {strings.dashboard.progressLabel}
              </span>
            </div>
          </div>

          <div className="flex-1 w-full space-y-3">
            <div>
              <h2 className="text-base font-semibold text-white">
                {strings.home.todayGoal}
              </h2>
              <p className="text-sm text-[#22D3EE] font-mono-tabular mt-0.5">
                {dailyStats.targetMinutes} {strings.home.minutesUnit}
              </p>
            </div>

            <div className="grid grid-cols-2 gap-3 pt-1">
              <div className="rounded-xl bg-[#101B32] border border-[#233554] px-3.5 py-2.5">
                <div className="text-xs text-[#A7B4C8]">
                  {strings.dashboard.completedLabel}
                </div>
                <div className="text-lg font-bold text-white font-mono-tabular mt-0.5">
                  {dailyStats.completedFocusMinutes} {strings.home.minutesUnit}
                </div>
              </div>
              <div className="rounded-xl bg-[#101B32] border border-[#233554] px-3.5 py-2.5">
                <div className="text-xs text-[#A7B4C8]">
                  {strings.dashboard.remainingLabel}
                </div>
                <div className="text-lg font-bold text-white font-mono-tabular mt-0.5">
                  {dailyStats.remainingMinutes} {strings.home.minutesUnit}
                </div>
              </div>
            </div>

            {dailyStats.completedSessions === 0 && (
              <p className="text-xs text-[#A7B4C8]">
                {strings.dashboard.noSessionsToday}
              </p>
            )}
          </div>
        </section>

        {/* C. Prominent Start Focus Card */}
        <section className="lg:col-span-5 rounded-2xl bg-[#16243D] border border-[#233554] p-6 flex flex-col justify-between gap-5">
          <div className="space-y-1.5">
            <h2 className="text-lg font-semibold text-white">
              {activeTimer && (activeTimer.status === 'RUNNING' || activeTimer.status === 'PAUSED')
                ? strings.dashboard.resumeFocus
                : strings.dashboard.startFocus}
            </h2>
            <p className="text-sm text-[#A7B4C8]">
              {currentTask
                ? `${taskSubjectName ? `${taskSubjectName} · ` : ''}${currentTask.title}`
                : strings.dashboard.noTaskSelected}
            </p>
          </div>

          <button
            type="button"
            onClick={onStartFocus}
            className="w-full min-h-[56px] px-6 py-3.5 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] active:scale-[0.99] text-white font-semibold text-base inline-flex items-center justify-center gap-2.5 transition-all duration-150 whitespace-nowrap cursor-pointer"
          >
            <Play className="w-5 h-5 fill-current shrink-0" />
            <span>
              {activeTimer && (activeTimer.status === 'RUNNING' || activeTimer.status === 'PAUSED')
                ? strings.dashboard.resumeFocus
                : strings.dashboard.startFocus}
            </span>
          </button>
        </section>
      </div>

      {/* Phase 3: Focus Recovery Card (when eligible after early-ended focus session) */}
      {recoveryState && recoveryState.isActive && !activeTimer && (
        <section className="rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-4">
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-center gap-2.5">
              <RefreshCw className="w-5 h-5 text-[#22D3EE] shrink-0" />
              <h2 className="text-base font-semibold text-white">
                {strings.phase3.recoveryTitle}
              </h2>
            </div>
            <button
              type="button"
              onClick={onDismissRecovery}
              className="min-h-[36px] px-3 text-xs font-medium text-[#A7B4C8] hover:text-white cursor-pointer"
            >
              {strings.phase3.recoveryDismissBtn}
            </button>
          </div>
          <p className="text-sm text-[#A7B4C8]">{strings.phase3.recoveryFreshStart}</p>
          <p className="text-sm text-[#22D3EE]">
            {strings.phase3.recoveryShorterPrompt(recoveryState.suggestedRetryMinutes)}
          </p>
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => onRestartShorterFromRecovery(recoveryState.suggestedRetryMinutes)}
              className="min-h-[44px] px-5 py-2 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white text-sm font-semibold whitespace-nowrap cursor-pointer"
            >
              {strings.phase3.recoveryShorterBtn(recoveryState.suggestedRetryMinutes)}
            </button>
            <button
              type="button"
              onClick={onTakeRecoveryBreak}
              className="min-h-[44px] px-4 py-2 rounded-xl bg-[#101B32] border border-[#233554] hover:border-[#3B82F6] text-white text-sm font-semibold whitespace-nowrap cursor-pointer"
            >
              {strings.timer.takeBreak}
            </button>
            <button
              type="button"
              onClick={onOpenBreathingReset}
              className="min-h-[44px] px-4 py-2 rounded-xl bg-[#101B32] border border-[#233554] hover:border-[#22D3EE] text-[#22D3EE] text-sm font-semibold inline-flex items-center gap-2 whitespace-nowrap cursor-pointer"
            >
              <Wind className="w-4 h-4 shrink-0" />
              <span>{strings.phase6.breathingActionBtn}</span>
            </button>
          </div>
        </section>
      )}

      {/* Phase 3: Adaptive Focus Recommendation + Distraction Blocker Status */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
        {recommendation.status !== 'IGNORED' && (
          <section className="lg:col-span-7 rounded-2xl bg-[#16243D] border border-[#233554] p-6 flex flex-col justify-between space-y-4">
            <div className="space-y-2">
              <div className="flex items-center gap-2.5">
                <Sparkles className="w-5 h-5 text-[#22D3EE] shrink-0" />
                <h2 className="text-base font-semibold text-white">
                  {recommendation.hasEnoughData
                    ? strings.phase3.adaptiveTitle
                    : strings.phase3.adaptiveInsufficientTitle}
                </h2>
              </div>
              <p className="text-sm text-[#A7B4C8] leading-relaxed">
                {recommendation.hasEnoughData
                  ? recommendation.reasonText
                  : strings.phase3.adaptiveInsufficientDesc}
              </p>
              {recommendation.hasEnoughData && (
                <p className="text-xs text-[#22D3EE]">
                  {strings.phase3.adaptiveRuleMeta(recommendation.basedOnSessionCount)}
                </p>
              )}
            </div>

            {recommendation.hasEnoughData && (
              <div className="flex flex-wrap items-center gap-3 pt-1">
                <button
                  type="button"
                  onClick={() =>
                    onStartRecommendedFocus(recommendation.recommendedMinutes, recommendation.id)
                  }
                  className="min-h-[44px] px-5 py-2.5 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white text-sm font-semibold whitespace-nowrap cursor-pointer"
                >
                  {strings.phase3.adaptiveStartRecommended(recommendation.recommendedMinutes)}
                </button>
                <button
                  type="button"
                  onClick={() => onIgnoreRecommendation(recommendation.id)}
                  className="min-h-[44px] px-4 py-2 text-sm font-medium text-[#A7B4C8] hover:text-white cursor-pointer"
                >
                  {strings.phase3.adaptiveIgnore}
                </button>
              </div>
            )}
          </section>
        )}

        <section
          className={`${
            recommendation.status !== 'IGNORED' ? 'lg:col-span-5' : 'lg:col-span-12'
          } rounded-2xl bg-[#16243D] border border-[#233554] p-6 flex flex-col justify-between space-y-4`}
        >
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-start gap-3">
              <Shield
                className={`w-5 h-5 shrink-0 mt-0.5 ${
                  blockerSettings.isEnabled ? 'text-[#22C55E]' : 'text-[#A7B4C8]'
                }`}
              />
              <div>
                <h2 className="text-base font-semibold text-white">
                  {strings.phase3.blockerTitle}
                </h2>
                <p className="text-sm text-[#A7B4C8] mt-1">
                  {blockerStatusLabel} ·{' '}
                  {strings.phase3.blockerAppsUnit(blockerSettings.selectedPackageNames.length)}
                </p>
              </div>
            </div>
          </div>
          <button
            type="button"
            onClick={onOpenBlockerSettings}
            className="w-full min-h-[44px] px-4 py-2 rounded-xl bg-[#101B32] border border-[#233554] hover:border-[#3B82F6] text-sm font-semibold text-[#22D3EE] transition-colors cursor-pointer"
          >
            {strings.phase3.blockerConfigure}
          </button>
        </section>
      </div>

      {/* D. Current Study Task + E. Reward Preview */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch">
        <section className="lg:col-span-7 rounded-2xl bg-[#16243D] border border-[#233554] p-6 flex flex-col justify-between space-y-4">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2.5">
              <BookOpen className="w-5 h-5 text-[#22D3EE]" />
              <h2 className="text-base font-semibold text-white">
                {strings.dashboard.currentTask}
              </h2>
            </div>
            <button
              type="button"
              onClick={() => setShowAddTaskModal(true)}
              className="min-h-[40px] px-3.5 py-1.5 rounded-xl bg-[#101B32] border border-[#233554] hover:border-[#3B82F6] text-xs sm:text-sm font-semibold text-[#22D3EE] inline-flex items-center gap-1.5 transition-colors duration-150 whitespace-nowrap cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              <span>{strings.dashboard.addTask}</span>
            </button>
          </div>

          {currentTask ? (
            <div className="rounded-xl bg-[#101B32] border border-[#233554] p-4 space-y-1.5">
              {taskSubjectName && (
                <div className="text-xs font-medium text-[#22D3EE]">{taskSubjectName}</div>
              )}
              <div className="text-base font-semibold text-white">{currentTask.title}</div>
              <div className="text-xs text-[#A7B4C8] font-mono-tabular">
                {currentTask.plannedDurationMinutes} {strings.home.minutesUnit} ·{' '}
                {currentTask.status.replace('_', ' ')}
              </div>
            </div>
          ) : (
            <div className="rounded-xl bg-[#101B32] border border-[#233554] p-5 text-sm text-[#A7B4C8]">
              {strings.dashboard.noTaskSelected}
            </div>
          )}
        </section>

        {/* E. Reward Preview */}
        <section className="lg:col-span-5 rounded-2xl bg-[#16243D] border border-[#233554] p-6 flex flex-col justify-between space-y-4">
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-base font-semibold text-white">
              {strings.dashboard.rewardsPreview}
            </h2>
            <button
              type="button"
              onClick={onOpenRewards}
              className="min-h-[40px] px-3 py-1.5 text-xs sm:text-sm font-semibold text-[#22D3EE] hover:text-white transition-colors whitespace-nowrap cursor-pointer"
            >
              {strings.dashboard.viewRewards}
            </button>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-xl bg-[#101B32] border border-[#233554] p-3.5 space-y-1">
              <div className="flex items-center gap-2 text-xs text-[#A7B4C8]">
                <Coins className="w-4 h-4 text-[#F59E0B]" />
                <span>{strings.dashboard.focusCoins}</span>
              </div>
              <div className="text-2xl font-bold text-white font-mono-tabular">
                {rewardPreview.focusCoins}
              </div>
            </div>
            <div className="rounded-xl bg-[#101B32] border border-[#233554] p-3.5 space-y-1">
              <div className="flex items-center gap-2 text-xs text-[#A7B4C8]">
                <Trophy className="w-4 h-4 text-[#22D3EE]" />
                <span>{strings.dashboard.achievements}</span>
              </div>
              <div className="text-2xl font-bold text-white font-mono-tabular">
                {rewardPreview.unlockedAchievements}
              </div>
            </div>
          </div>
        </section>
      </div>

      {/* Phase 6: Daily Study Missions Card */}
      {dailyMissions.length > 0 && (
        <section className="rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-4">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2.5">
              <Flag className="w-5 h-5 text-[#22D3EE] shrink-0" />
              <h2 className="text-base font-semibold text-white">
                {strings.phase6.missionsTitle}
              </h2>
            </div>
            <span className="text-xs font-semibold text-[#22D3EE] font-mono-tabular">
              {strings.phase6.missionsCompletedSummary(
                dailyMissions.filter((m) => m.isCompleted).length,
                dailyMissions.length,
              )}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            {dailyMissions.map((mission) => (
              <div
                key={mission.id}
                className="rounded-xl bg-[#101B32] border border-[#233554] p-4 flex flex-col justify-between gap-3"
              >
                <div className="space-y-1">
                  <div className="flex items-center justify-between gap-2">
                    <h3 className="text-sm font-semibold text-white">{mission.title}</h3>
                    <span className="text-xs font-bold text-[#F59E0B] font-mono-tabular shrink-0">
                      +{mission.rewardCoins}c
                    </span>
                  </div>
                  <p className="text-xs text-[#A7B4C8] leading-relaxed">{mission.description}</p>
                </div>

                <div className="flex items-center justify-between gap-2 pt-1">
                  <span className="text-xs text-[#A7B4C8] font-mono-tabular">
                    {strings.phase6.missionsProgressBadge(
                      mission.currentProgress,
                      mission.targetProgress,
                    )}
                  </span>

                  {mission.isClaimed ? (
                    <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#22C55E] whitespace-nowrap">
                      <CheckCircle2 className="w-4 h-4 shrink-0" />
                      <span>{strings.phase6.missionsClaimedBadge}</span>
                    </span>
                  ) : mission.isCompleted ? (
                    <button
                      type="button"
                      onClick={() => onClaimMission(mission.id)}
                      className="min-h-[36px] px-3.5 py-1.5 rounded-lg bg-[#3B82F6] hover:bg-[#2563EB] text-white text-xs font-semibold whitespace-nowrap cursor-pointer"
                    >
                      {strings.phase6.missionsClaimBtn(mission.rewardCoins)}
                    </button>
                  ) : null}
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* F. Weekly Summary */}
      <section className="rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <h2 className="text-lg font-semibold text-white">
            {strings.dashboard.weeklySummary}
          </h2>
          <div className="flex items-center gap-5 text-sm">
            <div>
              <span className="text-[#A7B4C8]">{strings.dashboard.weeklyMinutes}: </span>
              <span className="font-bold text-white font-mono-tabular">
                {weeklyStats.totalWeeklyFocusMinutes} {strings.home.minutesUnit}
              </span>
            </div>
            <div>
              <span className="text-[#A7B4C8]">{strings.dashboard.completedSessions}: </span>
              <span className="font-bold text-[#22D3EE] font-mono-tabular">
                {weeklyStats.completedSessions}
              </span>
            </div>
          </div>
        </div>

        {weeklyStats.totalWeeklyFocusMinutes === 0 && weeklyStats.completedSessions === 0 ? (
          <p className="text-sm text-[#A7B4C8]">{strings.dashboard.noWeeklyActivity}</p>
        ) : (
          <div className="grid grid-cols-7 gap-2 sm:gap-4 items-end pt-2">
            {weeklyStats.dailyBreakdown.map((day) => {
              const ratio = day.focusMinutes / maxWeeklyDayMinutes;
              const barHeightPx = Math.round(12 + ratio * 72);
              return (
                <div
                  key={day.date}
                  className="flex flex-col items-center gap-2"
                >
                  <span className="text-[11px] text-[#A7B4C8] font-mono-tabular">
                    {day.focusMinutes > 0 ? `${day.focusMinutes}m` : '0'}
                  </span>
                  <div className="w-full max-w-[28px] h-[84px] flex items-end justify-center">
                    <div
                      className={`w-full rounded-md transition-all duration-200 ${
                        day.focusMinutes > 0 ? 'bg-[#3B82F6]' : 'bg-[#101B32]'
                      }`}
                      style={{ height: `${barHeightPx}px` }}
                    />
                  </div>
                  <span className="text-xs font-medium text-[#A7B4C8]">
                    {day.dayShortLabel}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* Add Task Modal */}
      {showAddTaskModal && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-5"
        >
          <form
            onSubmit={handleSaveTask}
            className="w-full max-w-md rounded-2xl bg-[#101B32] border border-[#233554] p-6 space-y-4"
          >
            <h2 className="text-lg font-bold text-white">{strings.dashboard.addTask}</h2>

            <div className="space-y-1.5">
              <label className="block text-xs text-[#A7B4C8]">
                {strings.dashboard.taskTitleLabel}
              </label>
              <input
                type="text"
                required
                value={taskTitle}
                onChange={(e) => setTaskTitle(e.target.value)}
                className="w-full min-h-[46px] px-3.5 rounded-xl bg-[#16243D] border border-[#233554] focus:border-[#3B82F6] focus:outline-none text-white text-sm"
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs text-[#A7B4C8]">
                {strings.dashboard.subjectLabel}
              </label>
              <input
                type="text"
                value={taskSubject}
                onChange={(e) => setTaskSubject(e.target.value)}
                className="w-full min-h-[46px] px-3.5 rounded-xl bg-[#16243D] border border-[#233554] focus:border-[#3B82F6] focus:outline-none text-white text-sm"
              />
            </div>

            <div className="space-y-1.5">
              <label className="block text-xs text-[#A7B4C8]">
                {strings.dashboard.plannedMinutesLabel}
              </label>
              <input
                type="text"
                inputMode="numeric"
                value={taskMinutes}
                onChange={(e) => setTaskMinutes(e.target.value.replace(/\D/g, '').slice(0, 3))}
                className="w-full min-h-[46px] px-3.5 rounded-xl bg-[#16243D] border border-[#233554] focus:border-[#3B82F6] focus:outline-none text-white text-sm font-mono-tabular"
              />
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setShowAddTaskModal(false)}
                className="min-h-[44px] px-4 py-2 rounded-xl border border-[#233554] text-sm font-medium text-[#A7B4C8] hover:text-white cursor-pointer"
              >
                {strings.profile.cancelLabel}
              </button>
              <button
                type="submit"
                disabled={!taskTitle.trim()}
                className="min-h-[44px] px-5 py-2 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] disabled:opacity-50 text-white text-sm font-semibold cursor-pointer"
              >
                {strings.dashboard.saveTask}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Notifications Info Modal */}
      {showNotifModal && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-5"
        >
          <div className="w-full max-w-md rounded-2xl bg-[#101B32] border border-[#233554] p-6 space-y-4">
            <h2 className="text-lg font-bold text-white">
              {strings.dashboard.notificationsTitle}
            </h2>
            <p className="text-sm text-[#A7B4C8] leading-relaxed">
              {strings.dashboard.notificationsBody}
            </p>
            <div className="flex justify-end">
              <button
                type="button"
                onClick={() => setShowNotifModal(false)}
                className="min-h-[44px] px-5 py-2 rounded-xl bg-[#3B82F6] text-white text-sm font-semibold cursor-pointer"
              >
                {strings.dashboard.okLabel}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
