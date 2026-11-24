import { useState } from 'react';
import {
  CheckCircle2,
  Pause,
  Play,
  RefreshCw,
  Shield,
  Sparkles,
  Square,
  Wind,
} from 'lucide-react';
import type { FocuslyStrings } from '../i18n/translations';
import type {
  ActiveTimerSnapshot,
  DailyStats,
  DistractionBlockerSettings,
  FocusMode,
  FocusRecommendation,
  FocusRecoveryState,
  FocusSessionRecord,
  SessionType,
  StudyTaskItem,
  SubjectItem,
  TimerProgress,
} from '../types/focusly';
import { resolveDistractionBlockerMode } from '../utils/adaptiveFocusEngine';
import { calculateSessionCoins } from '../utils/rewardEngine';
import { formatDuration } from '../utils/timerEngine';

interface FocusTimerScreenProps {
  strings: FocuslyStrings;
  activeTimer: ActiveTimerSnapshot | null;
  timerProgress: TimerProgress | null;
  sessionType: SessionType;
  selectedDurationMinutes: number;
  selectedSubjectId: string | null;
  selectedTaskId: string | null;
  subjects: SubjectItem[];
  tasks: StudyTaskItem[];
  recentSessions: FocusSessionRecord[];
  lastCompletedSession: FocusSessionRecord | null;
  dailyStats: DailyStats;
  recommendation: FocusRecommendation;
  blockerSettings: DistractionBlockerSettings;
  recoveryState: FocusRecoveryState | null;
  focusMode: FocusMode;
  onSelectSessionType: (type: SessionType) => void;
  onSelectFocusMode: (mode: FocusMode) => void;
  onSelectDurationMinutes: (minutes: number) => void;
  onSelectSubject: (subjectId: string | null) => void;
  onSelectTask: (taskId: string | null) => void;
  onApplyRecommendation: (minutes: number, recId: string) => void;
  onIgnoreRecommendation: (recId: string) => void;
  onRestartShorterFromRecovery: (minutes: number) => void;
  onDismissRecovery: () => void;
  onOpenBreathingReset: () => void;
  onOpenBlockerSettings: () => void;
  onStart: () => void;
  onPause: () => void;
  onResume: () => void;
  onEndEarly: () => void;
  onStartAnother: () => void;
  onTakeBreak: () => void;
  onReturnHome: () => void;
}

const FOCUS_PRESETS = [15, 25, 45, 60, 90];
const BREAK_PRESETS = [5, 10, 15];

export function FocusTimerScreen({
  strings,
  activeTimer,
  timerProgress,
  sessionType,
  selectedDurationMinutes,
  selectedSubjectId,
  selectedTaskId,
  subjects,
  tasks,
  recentSessions,
  lastCompletedSession,
  dailyStats,
  recommendation,
  blockerSettings,
  recoveryState,
  focusMode,
  onSelectSessionType,
  onSelectFocusMode,
  onSelectDurationMinutes,
  onSelectSubject,
  onSelectTask,
  onApplyRecommendation,
  onIgnoreRecommendation,
  onRestartShorterFromRecovery,
  onDismissRecovery,
  onOpenBreathingReset,
  onOpenBlockerSettings,
  onStart,
  onPause,
  onResume,
  onEndEarly,
  onStartAnother,
  onTakeBreak,
  onReturnHome,
}: FocusTimerScreenProps) {
  const [isCustom, setIsCustom] = useState(false);
  const [customInput, setCustomInput] = useState('');
  const [showEndConfirm, setShowEndConfirm] = useState(false);

  const blockerMode = resolveDistractionBlockerMode(blockerSettings);
  const blockerModeLabel =
    blockerMode === 'ACTIVE_DETECTION'
      ? strings.phase3.blockerStatusActive
      : blockerMode === 'CHECKLIST_ONLY'
        ? strings.phase3.blockerStatusChecklist
        : strings.phase3.blockerStatusDisabled;

  // Show completion screen if a session just completed and no new timer is active
  if (lastCompletedSession && !activeTimer) {
    const isBreak = lastCompletedSession.sessionType === 'BREAK';
    const subj = subjects.find((s) => s.id === lastCompletedSession.subjectId)?.subjectName;
    const task = tasks.find((t) => t.id === lastCompletedSession.taskId)?.title;

    return (
      <div className="max-w-xl mx-auto py-4 space-y-6 text-center">
        <div className="flex justify-center">
          <div className="w-16 h-16 rounded-2xl bg-[#22C55E]/15 border border-[#22C55E]/40 flex items-center justify-center">
            <CheckCircle2 className="w-9 h-9 text-[#22C55E]" />
          </div>
        </div>

        <div className="space-y-2">
          <h1 className="text-2xl sm:text-3xl font-bold text-white">
            {isBreak ? strings.timer.breakCompletedTitle : strings.timer.completedTitle}
          </h1>
          <p className="text-base text-[#A7B4C8]">{strings.timer.completedMessage}</p>
        </div>

        <div className="rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-3 text-left text-sm">
          <div className="flex justify-between gap-4 py-1.5 border-b border-[#233554]">
            <span className="text-[#A7B4C8]">{strings.timer.actualDuration}</span>
            <span className="font-semibold text-white font-mono-tabular">
              {formatDuration(lastCompletedSession.actualDurationSeconds)}
            </span>
          </div>
          <div className="flex justify-between gap-4 py-1.5 border-b border-[#233554]">
            <span className="text-[#A7B4C8]">{strings.timer.plannedDuration}</span>
            <span className="font-semibold text-white font-mono-tabular">
              {formatDuration(lastCompletedSession.plannedDurationSeconds)}
            </span>
          </div>
          {subj && (
            <div className="flex justify-between gap-4 py-1.5 border-b border-[#233554]">
              <span className="text-[#A7B4C8]">{strings.timer.subjectPickerLabel}</span>
              <span className="font-semibold text-white">{subj}</span>
            </div>
          )}
          {task && (
            <div className="flex justify-between gap-4 py-1.5 border-b border-[#233554]">
              <span className="text-[#A7B4C8]">{strings.timer.taskPickerLabel}</span>
              <span className="font-semibold text-white">{task}</span>
            </div>
          )}
          <div className="flex justify-between gap-4 pt-1.5">
            <span className="text-[#A7B4C8]">{strings.timer.dailyProgressUpdate}</span>
            <span className="font-semibold text-[#22D3EE] font-mono-tabular">
              {dailyStats.completedFocusMinutes} / {dailyStats.targetMinutes}{' '}
              {strings.home.minutesUnit} ({dailyStats.completionPercentage}%)
            </span>
          </div>
          {(() => {
            const earnedCoins = calculateSessionCoins(
              lastCompletedSession.plannedDurationSeconds,
              lastCompletedSession.actualDurationSeconds,
              lastCompletedSession.sessionType,
              lastCompletedSession.sessionStatus,
            );
            if (earnedCoins <= 0) return null;
            return (
              <div className="flex justify-between gap-4 pt-2 border-t border-[#233554]">
                <span className="text-[#A7B4C8]">{strings.phase5.sessionCoinsEarned}</span>
                <span className="font-bold text-[#F59E0B] font-mono-tabular">+{earnedCoins}</span>
              </div>
            );
          })()}
        </div>

        <div className="space-y-3">
          <button
            type="button"
            onClick={onStartAnother}
            className="w-full min-h-[52px] px-6 py-3 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white font-semibold text-base transition-colors cursor-pointer"
          >
            {strings.timer.startAnother}
          </button>
          {!isBreak && (
            <button
              type="button"
              onClick={onTakeBreak}
              className="w-full min-h-[48px] px-6 py-3 rounded-xl bg-[#16243D] border border-[#233554] hover:border-[#3B82F6] text-white font-semibold text-sm transition-colors cursor-pointer"
            >
              {strings.timer.takeBreak}
            </button>
          )}
          <button
            type="button"
            onClick={onReturnHome}
            className="w-full min-h-[44px] px-6 py-2.5 text-sm font-medium text-[#A7B4C8] hover:text-white transition-colors cursor-pointer"
          >
            {strings.timer.returnHome}
          </button>
        </div>
      </div>
    );
  }

  const isRunningOrPaused =
    activeTimer !== null &&
    (activeTimer.status === 'RUNNING' || activeTimer.status === 'PAUSED');

  const currentType = activeTimer?.sessionType ?? sessionType;
  const presets = currentType === 'FOCUS' ? FOCUS_PRESETS : BREAK_PRESETS;
  const maxCustom = currentType === 'FOCUS' ? 240 : 60;

  const remainingSeconds =
    timerProgress?.remainingSeconds ?? selectedDurationMinutes * 60;
  const progressFraction = timerProgress?.progressFraction ?? 0;

  const radius = 104;
  const circumference = 2 * Math.PI * radius;
  const remainingFraction = Math.max(0, Math.min(1, 1 - progressFraction));
  const strokeDashoffset = circumference * (1 - remainingFraction);

  const ringColor =
    currentType === 'BREAK'
      ? '#22C55E'
      : activeTimer?.status === 'PAUSED'
        ? '#F59E0B'
        : '#3B82F6';

  const activeSubject = subjects.find(
    (s) => s.id === (activeTimer?.subjectId ?? selectedSubjectId),
  );
  const activeTask = tasks.find(
    (t) => t.id === (activeTimer?.taskId ?? selectedTaskId),
  );

  const handleCustomChange = (val: string) => {
    const digits = val.replace(/\D/g, '').slice(0, 3);
    setIsCustom(true);
    setCustomInput(digits);
    const num = Number.parseInt(digits, 10);
    if (!Number.isNaN(num) && num >= 1 && num <= maxCustom) {
      onSelectDurationMinutes(num);
    }
  };

  return (
    <div className="space-y-8">
      {/* Top Mode Switcher */}
      <div className="max-w-xl mx-auto space-y-6">
        <div className="grid grid-cols-2 gap-2 p-1.5 rounded-xl bg-[#101B32] border border-[#233554]">
          {(['FOCUS', 'BREAK'] as SessionType[]).map((type) => {
            const selected = currentType === type;
            return (
              <button
                key={type}
                type="button"
                disabled={isRunningOrPaused}
                onClick={() => {
                  setIsCustom(false);
                  onSelectSessionType(type);
                }}
                className={`min-h-[44px] rounded-lg text-sm font-semibold transition-colors duration-150 cursor-pointer disabled:cursor-not-allowed ${
                  selected
                    ? 'bg-[#3B82F6] text-white'
                    : 'text-[#A7B4C8] hover:text-white'
                }`}
              >
                {type === 'FOCUS' ? strings.timer.focusMode : strings.timer.breakMode}
              </button>
            );
          })}
        </div>

        {/* Phase 6: Focus Mode Selector (Manual / Auto-Schedule / Smart) */}
        {!isRunningOrPaused && currentType === 'FOCUS' && (
          <div className="space-y-2">
            <div className="grid grid-cols-3 gap-2 p-1 rounded-xl bg-[#101B32] border border-[#233554]">
              {(
                [
                  { id: 'MANUAL', label: strings.phase6.focusModeManual },
                  { id: 'AUTO_SCHEDULE', label: strings.phase6.focusModeAuto },
                  { id: 'SMART', label: strings.phase6.focusModeSmart },
                ] as { id: FocusMode; label: string }[]
              ).map((item) => {
                const selected = focusMode === item.id;
                return (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => {
                      setIsCustom(false);
                      onSelectFocusMode(item.id);
                    }}
                    className={`min-h-[40px] px-2 rounded-lg text-xs sm:text-sm font-semibold transition-colors whitespace-nowrap cursor-pointer ${
                      selected
                        ? 'bg-[#16243D] text-[#22D3EE] border border-[#3B82F6]/50'
                        : 'text-[#A7B4C8] hover:text-white'
                    }`}
                  >
                    {item.label}
                  </button>
                );
              })}
            </div>
            {focusMode === 'AUTO_SCHEDULE' && (
              <p className="text-xs text-[#22D3EE] text-center">
                {activeTask
                  ? strings.phase6.focusModeAutoHint(
                      activeTask.title,
                      activeTask.plannedDurationMinutes,
                    )
                  : strings.phase6.focusModeAutoEmpty}
              </p>
            )}
            {focusMode === 'SMART' && (
              <p className="text-xs text-[#22D3EE] text-center">
                {strings.phase6.focusModeSmartHint(selectedDurationMinutes)}
              </p>
            )}
          </div>
        )}

        {/* Phase 3: Supportive Recovery Card when IDLE after early-ended focus session */}
        {!isRunningOrPaused && recoveryState && recoveryState.isActive && (
          <div className="rounded-2xl bg-[#16243D] border border-[#233554] p-5 space-y-3">
            <div className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <RefreshCw className="w-4 h-4 text-[#22D3EE]" />
                <span className="text-sm font-semibold text-white">
                  {strings.phase3.recoveryTitle}
                </span>
              </div>
              <button
                type="button"
                onClick={onDismissRecovery}
                className="text-xs font-medium text-[#A7B4C8] hover:text-white cursor-pointer"
              >
                {strings.phase3.recoveryDismissBtn}
              </button>
            </div>
            <p className="text-xs sm:text-sm text-[#A7B4C8]">
              {strings.phase3.recoveryFreshStart}
            </p>
            <div className="flex flex-wrap gap-2.5">
              <button
                type="button"
                onClick={() => onRestartShorterFromRecovery(recoveryState.suggestedRetryMinutes)}
                className="min-h-[40px] px-4 py-2 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white text-xs sm:text-sm font-semibold cursor-pointer"
              >
                {strings.phase3.recoveryShorterBtn(recoveryState.suggestedRetryMinutes)}
              </button>
              <button
                type="button"
                onClick={onTakeBreak}
                className="min-h-[40px] px-3.5 py-2 rounded-xl bg-[#101B32] border border-[#233554] text-white text-xs sm:text-sm font-medium cursor-pointer"
              >
                {strings.timer.takeBreak}
              </button>
              <button
                type="button"
                onClick={onOpenBreathingReset}
                className="min-h-[40px] px-3.5 py-2 rounded-xl bg-[#101B32] border border-[#233554] hover:border-[#22D3EE] text-[#22D3EE] text-xs sm:text-sm font-medium inline-flex items-center gap-1.5 whitespace-nowrap cursor-pointer"
              >
                <Wind className="w-3.5 h-3.5 shrink-0" />
                <span>{strings.phase6.breathingActionBtn}</span>
              </button>
            </div>
          </div>
        )}

        {/* Phase 3: Adaptive Focus Recommendation Banner */}
        {!isRunningOrPaused &&
          currentType === 'FOCUS' &&
          recommendation.status !== 'IGNORED' && (
            <div className="rounded-2xl bg-[#16243D] border border-[#233554] p-5 space-y-3">
              <div className="flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-[#22D3EE] shrink-0" />
                <span className="text-sm font-semibold text-white">
                  {recommendation.hasEnoughData
                    ? strings.phase3.adaptiveTitle
                    : strings.phase3.adaptiveInsufficientTitle}
                </span>
              </div>
              <p className="text-xs sm:text-sm text-[#A7B4C8]">
                {recommendation.hasEnoughData
                  ? recommendation.reasonText
                  : strings.phase3.adaptiveInsufficientDesc}
              </p>
              {recommendation.hasEnoughData && (
                <div className="flex flex-wrap items-center gap-3 pt-0.5">
                  <button
                    type="button"
                    onClick={() => {
                      setIsCustom(false);
                      onApplyRecommendation(recommendation.recommendedMinutes, recommendation.id);
                    }}
                    className="min-h-[40px] px-4 py-2 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white text-xs sm:text-sm font-semibold cursor-pointer"
                  >
                    {strings.phase3.adaptiveUseDuration(recommendation.recommendedMinutes)}
                  </button>
                  <button
                    type="button"
                    onClick={() => onIgnoreRecommendation(recommendation.id)}
                    className="min-h-[40px] px-3 py-2 text-xs sm:text-sm text-[#A7B4C8] hover:text-white cursor-pointer"
                  >
                    {strings.phase3.adaptiveIgnore}
                  </button>
                </div>
              )}
            </div>
          )}

        {/* Active Subject & Task banner */}
        {currentType === 'FOCUS' && (activeSubject || activeTask) && (
          <div className="text-center text-sm font-medium text-[#22D3EE]">
            {[activeSubject?.subjectName, activeTask?.title].filter(Boolean).join(' · ')}
          </div>
        )}

        {/* Circular Timer Display */}
        <div className="flex flex-col items-center justify-center py-2">
          <div className="relative w-64 h-64 flex items-center justify-center">
            <svg className="w-full h-full -rotate-90" viewBox="0 0 240 240">
              <circle
                cx="120"
                cy="120"
                r={radius}
                fill="none"
                stroke="#16243D"
                strokeWidth="12"
              />
              <circle
                cx="120"
                cy="120"
                r={radius}
                fill="none"
                stroke={ringColor}
                strokeWidth="12"
                strokeLinecap="round"
                strokeDasharray={circumference}
                strokeDashoffset={strokeDashoffset}
                className="transition-all duration-200"
              />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <span className="text-xs font-semibold tracking-wider text-[#22D3EE]">
                {currentType === 'BREAK'
                  ? strings.timer.breakMode.toUpperCase()
                  : strings.timer.focusMode.toUpperCase()}
              </span>
              <span className="text-5xl font-bold text-white font-mono-tabular mt-1">
                {formatDuration(remainingSeconds)}
              </span>
              <span className="text-xs text-[#A7B4C8] mt-1.5">
                {activeTimer?.status === 'PAUSED'
                  ? strings.timer.statusPaused
                  : activeTimer?.status === 'RUNNING'
                    ? strings.timer.statusActive
                    : strings.timer.statusReady}
              </span>
            </div>
          </div>
        </div>

        {/* Presets & Subject/Task Selectors (visible when IDLE) */}
        {!isRunningOrPaused && (
          <div className="space-y-5">
            <div className="flex flex-wrap items-center justify-center gap-2">
              {presets.map((mins) => {
                const selected = !isCustom && selectedDurationMinutes === mins;
                return (
                  <button
                    key={mins}
                    type="button"
                    onClick={() => {
                      setIsCustom(false);
                      onSelectDurationMinutes(mins);
                    }}
                    className={`min-h-[44px] px-4 py-2 rounded-xl border text-sm font-semibold font-mono-tabular transition-colors cursor-pointer ${
                      selected
                        ? 'bg-[#3B82F6] border-[#3B82F6] text-white'
                        : 'bg-[#16243D] border-[#233554] text-[#A7B4C8] hover:text-white'
                    }`}
                  >
                    {mins}m
                  </button>
                );
              })}
              <button
                type="button"
                onClick={() => {
                  setIsCustom(true);
                  if (!customInput) setCustomInput('35');
                }}
                className={`min-h-[44px] px-4 py-2 rounded-xl border text-sm font-semibold transition-colors cursor-pointer ${
                  isCustom
                    ? 'bg-[#3B82F6] border-[#3B82F6] text-white'
                    : 'bg-[#16243D] border-[#233554] text-[#A7B4C8] hover:text-white'
                }`}
              >
                {strings.timer.custom}
              </button>
            </div>

            {isCustom && (
              <div className="space-y-1.5">
                <label className="block text-xs text-[#A7B4C8]">
                  {strings.timer.customMinutesHint} (1–{maxCustom})
                </label>
                <input
                  type="text"
                  inputMode="numeric"
                  value={customInput}
                  onChange={(e) => handleCustomChange(e.target.value)}
                  placeholder="35"
                  className="w-full min-h-[48px] px-4 rounded-xl bg-[#16243D] border border-[#233554] focus:border-[#3B82F6] focus:outline-none text-white text-sm font-mono-tabular"
                />
              </div>
            )}

            {currentType === 'FOCUS' && (subjects.length > 0 || tasks.length > 0) && (
              <div className="rounded-2xl bg-[#16243D] border border-[#233554] p-4 space-y-4">
                {subjects.length > 0 && (
                  <div className="space-y-2">
                    <div className="text-xs font-medium text-[#A7B4C8]">
                      {strings.timer.subjectPickerLabel}
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <button
                        type="button"
                        onClick={() => onSelectSubject(null)}
                        className={`min-h-[38px] px-3 py-1.5 rounded-lg text-xs font-medium border cursor-pointer ${
                          selectedSubjectId === null
                            ? 'bg-[#3B82F6]/20 border-[#3B82F6] text-white'
                            : 'bg-[#101B32] border-[#233554] text-[#A7B4C8]'
                        }`}
                      >
                        {strings.timer.noneOption}
                      </button>
                      {subjects.map((subj) => (
                        <button
                          key={subj.id}
                          type="button"
                          onClick={() => onSelectSubject(subj.id)}
                          className={`min-h-[38px] px-3 py-1.5 rounded-lg text-xs font-medium border cursor-pointer ${
                            selectedSubjectId === subj.id
                              ? 'bg-[#3B82F6]/20 border-[#3B82F6] text-white'
                              : 'bg-[#101B32] border-[#233554] text-[#A7B4C8]'
                          }`}
                        >
                          {subj.subjectName}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {tasks.length > 0 && (
                  <div className="space-y-2">
                    <div className="text-xs font-medium text-[#A7B4C8]">
                      {strings.timer.taskPickerLabel}
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <button
                        type="button"
                        onClick={() => onSelectTask(null)}
                        className={`min-h-[38px] px-3 py-1.5 rounded-lg text-xs font-medium border cursor-pointer ${
                          selectedTaskId === null
                            ? 'bg-[#3B82F6]/20 border-[#3B82F6] text-white'
                            : 'bg-[#101B32] border-[#233554] text-[#A7B4C8]'
                        }`}
                      >
                        {strings.timer.noneOption}
                      </button>
                      {tasks.map((task) => (
                        <button
                          key={task.id}
                          type="button"
                          onClick={() => onSelectTask(task.id)}
                          className={`min-h-[38px] px-3 py-1.5 rounded-lg text-xs font-medium border cursor-pointer ${
                            selectedTaskId === task.id
                              ? 'bg-[#3B82F6]/20 border-[#3B82F6] text-white'
                              : 'bg-[#101B32] border-[#233554] text-[#A7B4C8]'
                          }`}
                        >
                          {task.title}
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* Controls */}
        <div className="space-y-3">
          {!isRunningOrPaused ? (
            <>
              <button
                type="button"
                onClick={onStart}
                className="w-full min-h-[54px] px-6 py-3.5 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white font-semibold text-base inline-flex items-center justify-center gap-2.5 transition-colors cursor-pointer"
              >
                <Play className="w-5 h-5 fill-current" />
                <span>
                  {currentType === 'BREAK' ? strings.timer.startBreak : strings.timer.startFocus}
                </span>
              </button>
              <button
                type="button"
                onClick={onOpenBreathingReset}
                className="w-full min-h-[44px] px-5 py-2.5 rounded-xl bg-[#101B32] border border-[#233554] hover:border-[#22D3EE] text-[#22D3EE] font-semibold text-sm inline-flex items-center justify-center gap-2 transition-colors whitespace-nowrap cursor-pointer"
              >
                <Wind className="w-4 h-4 shrink-0" />
                <span>{strings.phase6.breathingActionBtn}</span>
              </button>
            </>
          ) : (
            <div className="grid grid-cols-2 gap-3">
              {activeTimer?.status === 'RUNNING' ? (
                <button
                  type="button"
                  onClick={onPause}
                  className="min-h-[52px] px-5 py-3 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white font-semibold text-sm inline-flex items-center justify-center gap-2 cursor-pointer"
                >
                  <Pause className="w-4 h-4" />
                  <span>{strings.timer.pause}</span>
                </button>
              ) : (
                <button
                  type="button"
                  onClick={onResume}
                  className="min-h-[52px] px-5 py-3 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white font-semibold text-sm inline-flex items-center justify-center gap-2 cursor-pointer"
                >
                  <Play className="w-4 h-4 fill-current" />
                  <span>{strings.timer.resume}</span>
                </button>
              )}

              <button
                type="button"
                onClick={() => setShowEndConfirm(true)}
                className="min-h-[52px] px-5 py-3 rounded-xl bg-[#16243D] border border-[#233554] hover:border-[#F59E0B] text-white font-semibold text-sm inline-flex items-center justify-center gap-2 cursor-pointer"
              >
                <Square className="w-4 h-4" />
                <span>{strings.timer.endSession}</span>
              </button>
            </div>
          )}
        </div>

        {/* Live Distraction Blocker Status */}
        <div className="rounded-2xl bg-[#101B32] border border-[#233554] p-4 flex items-center justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <Shield
              className={`w-5 h-5 shrink-0 ${
                blockerSettings.isEnabled ? 'text-[#22C55E]' : 'text-[#A7B4C8]'
              }`}
            />
            <div className="min-w-0">
              <div className="text-sm font-semibold text-white truncate">
                {strings.phase3.blockerTitle}
              </div>
              <div className="text-xs text-[#A7B4C8] truncate">
                {blockerModeLabel} ·{' '}
                {strings.phase3.blockerAppsUnit(blockerSettings.selectedPackageNames.length)}
              </div>
            </div>
          </div>
          <button
            type="button"
            onClick={onOpenBlockerSettings}
            className="min-h-[40px] px-3.5 py-1.5 rounded-xl bg-[#16243D] border border-[#233554] hover:border-[#3B82F6] text-xs font-semibold text-[#22D3EE] whitespace-nowrap cursor-pointer"
          >
            {strings.phase3.blockerManage}
          </button>
        </div>
      </div>

      {/* Recent Session History */}
      <section className="rounded-2xl bg-[#16243D] border border-[#233554] p-6 space-y-4">
        <h2 className="text-base font-semibold text-white">{strings.timer.historyTitle}</h2>
        {recentSessions.length === 0 ? (
          <p className="text-sm text-[#A7B4C8]">{strings.timer.historyEmpty}</p>
        ) : (
          <div className="divide-y divide-[#233554]">
            {recentSessions.slice(0, 8).map((s) => {
              const subj = subjects.find((x) => x.id === s.subjectId)?.subjectName;
              return (
                <div
                  key={s.id}
                  className="py-3 flex items-center justify-between gap-4 text-sm"
                >
                  <div className="min-w-0">
                    <div className="font-medium text-white truncate">
                      {s.sessionType === 'BREAK'
                        ? strings.timer.breakMode
                        : subj || strings.timer.focusMode}
                    </div>
                    <div className="text-xs text-[#A7B4C8] font-mono-tabular">
                      {s.localDate} ·{' '}
                      {s.sessionStatus === 'COMPLETED'
                        ? strings.timer.statusCompleted
                        : strings.timer.statusEndedEarly}
                    </div>
                  </div>
                  <div className="text-sm font-semibold text-[#22D3EE] font-mono-tabular shrink-0">
                    {formatDuration(s.actualDurationSeconds)}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* End Session Confirmation Modal */}
      {showEndConfirm && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-5"
        >
          <div className="w-full max-w-md rounded-2xl bg-[#101B32] border border-[#233554] p-6 space-y-4">
            <h2 className="text-lg font-bold text-white">{strings.timer.endConfirmTitle}</h2>
            <p className="text-sm text-[#A7B4C8] leading-relaxed">
              {strings.timer.endConfirmBody}
            </p>
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setShowEndConfirm(false)}
                className="min-h-[44px] px-4 py-2 rounded-xl border border-[#233554] text-sm font-medium text-[#A7B4C8] hover:text-white cursor-pointer"
              >
                {strings.profile.cancelLabel}
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowEndConfirm(false);
                  onEndEarly();
                }}
                className="min-h-[44px] px-4 py-2 rounded-xl bg-[#F59E0B] text-[#080F1F] text-sm font-semibold cursor-pointer"
              >
                {strings.timer.endSession}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
