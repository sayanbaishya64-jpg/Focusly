import type { FocuslyStrings } from '../i18n/translations';
import type { ActiveTimerSnapshot } from '../types/focusly';
import { suggestRecoveryRetryMinutes } from '../utils/adaptiveFocusEngine';
import { calculateTimerProgress, formatDuration } from '../utils/timerEngine';

interface RecoveryModalProps {
  strings: FocuslyStrings;
  snapshot: ActiveTimerSnapshot;
  onResume: () => void;
  onRestartShorter: (minutes: number) => void;
  onEndSession: () => void;
}

export function RecoveryModal({
  strings,
  snapshot,
  onResume,
  onRestartShorter,
  onEndSession,
}: RecoveryModalProps) {
  const progress = calculateTimerProgress(snapshot, Date.now());
  const plannedMin = Math.max(1, Math.floor(snapshot.plannedDurationSeconds / 60));
  const actualMin = Math.floor(progress.elapsedSeconds / 60);
  const suggestedRetryMin = suggestRecoveryRetryMinutes(plannedMin, actualMin);

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-5"
    >
      <div className="w-full max-w-md rounded-2xl bg-[#101B32] border border-[#233554] p-6 space-y-4">
        <h2 className="text-lg font-bold text-white">{strings.phase3.recoveryTitle}</h2>
        <p className="text-sm text-[#A7B4C8] leading-relaxed">
          {strings.phase3.recoveryFreshStart} {strings.timer.recoveryBody}
        </p>
        <div className="rounded-xl bg-[#16243D] border border-[#233554] px-4 py-3 flex items-center justify-between text-sm">
          <span className="text-[#A7B4C8]">
            {snapshot.sessionType === 'BREAK' ? strings.timer.breakMode : strings.timer.focusMode}
          </span>
          <span className="font-bold text-[#22D3EE] font-mono-tabular">
            {formatDuration(progress.remainingSeconds)}
          </span>
        </div>
        <div className="space-y-2.5 pt-1">
          <button
            type="button"
            onClick={onResume}
            className="w-full min-h-[46px] px-5 py-2.5 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white text-sm font-semibold cursor-pointer"
          >
            {strings.timer.recoveryResume}
          </button>
          <button
            type="button"
            onClick={() => onRestartShorter(suggestedRetryMin)}
            className="w-full min-h-[44px] px-4 py-2 rounded-xl bg-[#16243D] border border-[#233554] hover:border-[#3B82F6] text-white text-sm font-medium cursor-pointer"
          >
            {strings.phase3.recoveryShorterBtn(suggestedRetryMin)}
          </button>
          <div className="flex justify-end">
            <button
              type="button"
              onClick={onEndSession}
              className="min-h-[40px] px-3 py-1.5 text-sm font-medium text-[#F59E0B] hover:underline cursor-pointer"
            >
              {strings.timer.recoveryEnd}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
