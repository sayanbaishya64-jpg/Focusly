import { useEffect, useState } from 'react';
import { Wind, X } from 'lucide-react';
import type { FocuslyStrings } from '../i18n/translations';

interface BreathingResetModalProps {
  strings: FocuslyStrings;
  onClose: () => void;
}

const TOTAL_SECONDS = 60;

export function BreathingResetModal({ strings, onClose }: BreathingResetModalProps) {
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const isComplete = elapsedSeconds >= TOTAL_SECONDS;

  useEffect(() => {
    if (isComplete) return;
    const id = window.setInterval(() => {
      setElapsedSeconds((prev) => (prev + 1 >= TOTAL_SECONDS ? TOTAL_SECONDS : prev + 1));
    }, 1000);
    return () => window.clearInterval(id);
  }, [isComplete]);

  const cycleSecond = elapsedSeconds % 16;
  const phaseStep = Math.floor(cycleSecond / 4);
  const phaseRemaining = 4 - (cycleSecond % 4);
  const remainingTotal = Math.max(0, TOTAL_SECONDS - elapsedSeconds);

  const phaseLabel = isComplete
    ? strings.phase6.breathingComplete
    : phaseStep === 0
      ? strings.phase6.breathingInhale
      : phaseStep === 1
        ? strings.phase6.breathingHold
        : phaseStep === 2
          ? strings.phase6.breathingExhale
          : strings.phase6.breathingRest;

  const ringScaleClass = isComplete
    ? 'scale-100'
    : phaseStep === 0 || phaseStep === 1
      ? 'scale-110'
      : 'scale-95';

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={strings.phase6.breathingTitle}
      className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-5"
    >
      <div className="w-full max-w-md rounded-2xl bg-[#101B32] border border-[#233554] p-6 space-y-6 text-center">
        <div className="flex items-start justify-between gap-3 text-left">
          <div className="flex items-center gap-2.5">
            <Wind className="w-5 h-5 text-[#22D3EE] shrink-0" />
            <h2 className="text-lg font-bold text-white">{strings.phase6.breathingTitle}</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label={strings.phase4.closeLabel}
            className="min-h-[44px] min-w-[44px] -mr-2 -mt-2 rounded-xl flex items-center justify-center text-[#A7B4C8] hover:text-white cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <p className="text-sm text-[#A7B4C8] leading-relaxed">
          {strings.phase6.breathingSubtitle}
        </p>

        <div className="flex justify-center py-2">
          <div
            className={`w-40 h-40 rounded-full bg-[#16243D] border-2 border-[#22D3EE] flex flex-col items-center justify-center transition-transform duration-700 ${ringScaleClass}`}
          >
            <span className="text-base font-bold text-[#22D3EE]">{phaseLabel}</span>
            {!isComplete && (
              <span className="text-3xl font-bold text-white font-mono-tabular mt-1">
                {phaseRemaining}s
              </span>
            )}
          </div>
        </div>

        <p className="text-sm font-medium text-[#A7B4C8] font-mono-tabular">
          {isComplete
            ? strings.phase6.breathingDoneHint
            : strings.phase6.breathingRemainingSeconds(remainingTotal)}
        </p>

        <button
          type="button"
          onClick={onClose}
          className="w-full min-h-[44px] px-5 py-2.5 rounded-xl bg-[#3B82F6] hover:bg-[#2563EB] text-white text-sm font-semibold whitespace-nowrap cursor-pointer"
        >
          {strings.phase4.closeLabel}
        </button>
      </div>
    </div>
  );
}
