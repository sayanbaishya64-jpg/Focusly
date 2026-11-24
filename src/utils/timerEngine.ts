import type {
  ActiveTimerSnapshot,
  AppLanguage,
  DailyStats,
  FocusSessionRecord,
  SessionStatus,
  TimerProgress,
  WeeklyDayBreakdown,
  WeeklyStats,
} from '../types/focusly';

export function calculateTimerProgress(
  snapshot: ActiveTimerSnapshot,
  nowMs: number,
): TimerProgress {
  const planned = Math.max(1, snapshot.plannedDurationSeconds);
  const effectiveNow =
    snapshot.status === 'PAUSED' && snapshot.pauseStartedTimestampMs !== null
      ? snapshot.pauseStartedTimestampMs
      : nowMs;

  const rawElapsedMs = Math.max(
    0,
    effectiveNow - snapshot.startTimestampMs - snapshot.accumulatedPausedMs,
  );
  const elapsedSeconds = Math.min(planned, Math.floor(rawElapsedMs / 1000));
  const remainingSeconds = Math.max(0, planned - elapsedSeconds);
  const progressFraction = Math.min(1, Math.max(0, elapsedSeconds / planned));

  return {
    elapsedSeconds,
    remainingSeconds,
    progressFraction,
    isCompleted: snapshot.status === 'RUNNING' && remainingSeconds === 0,
  };
}

export function formatDuration(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds));
  const hours = Math.floor(safe / 3600);
  const minutes = Math.floor((safe % 3600) / 60);
  const seconds = safe % 60;
  const pad = (n: number) => String(n).padStart(2, '0');
  if (hours > 0) {
    return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
  }
  return `${pad(minutes)}:${pad(seconds)}`;
}

export const SessionStateMachine = {
  canTransition(from: SessionStatus, to: SessionStatus): boolean {
    switch (from) {
      case 'IDLE':
        return to === 'RUNNING';
      case 'RUNNING':
        return to === 'PAUSED' || to === 'COMPLETED' || to === 'ENDED_EARLY';
      case 'PAUSED':
        return to === 'RUNNING' || to === 'ENDED_EARLY' || to === 'COMPLETED';
      case 'COMPLETED':
      case 'ENDED_EARLY':
        return to === 'IDLE' || to === 'RUNNING';
    }
  },

  pause(snapshot: ActiveTimerSnapshot, nowMs: number): ActiveTimerSnapshot {
    if (!this.canTransition(snapshot.status, 'PAUSED')) {
      throw new Error(`Invalid state transition from ${snapshot.status} to PAUSED`);
    }
    return {
      ...snapshot,
      status: 'PAUSED',
      pauseStartedTimestampMs: nowMs,
    };
  },

  resume(snapshot: ActiveTimerSnapshot, nowMs: number): ActiveTimerSnapshot {
    if (!this.canTransition(snapshot.status, 'RUNNING')) {
      throw new Error(`Invalid state transition from ${snapshot.status} to RUNNING`);
    }
    const pauseDelta =
      snapshot.pauseStartedTimestampMs !== null
        ? Math.max(0, nowMs - snapshot.pauseStartedTimestampMs)
        : 0;
    return {
      ...snapshot,
      status: 'RUNNING',
      pauseStartedTimestampMs: null,
      accumulatedPausedMs: snapshot.accumulatedPausedMs + pauseDelta,
    };
  },
};

export function getLocalDateString(timestampMs: number = Date.now(), timeZoneOffsetMinutes?: number): string {
  if (typeof timeZoneOffsetMinutes === 'number') {
    const shifted = new Date(timestampMs + timeZoneOffsetMinutes * 60_000);
    const y = shifted.getUTCFullYear();
    const m = String(shifted.getUTCMonth() + 1).padStart(2, '0');
    const d = String(shifted.getUTCDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
  const date = new Date(timestampMs);
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function shiftLocalDateString(dateStr: string, deltaDays: number): string {
  const [y, m, d] = dateStr.split('-').map(Number);
  const dt = new Date(y, m - 1, d + deltaDays);
  const yy = dt.getFullYear();
  const mm = String(dt.getMonth() + 1).padStart(2, '0');
  const dd = String(dt.getDate()).padStart(2, '0');
  return `${yy}-${mm}-${dd}`;
}

function deduplicateSessions(sessions: FocusSessionRecord[]): FocusSessionRecord[] {
  const seen = new Set<string>();
  const result: FocusSessionRecord[] = [];
  for (const s of sessions) {
    if (!seen.has(s.id)) {
      seen.add(s.id);
      result.push(s);
    }
  }
  return result;
}

export function calculateDailyStats(
  sessions: FocusSessionRecord[],
  targetMinutes: number,
  targetDate: string = getLocalDateString(),
): DailyStats {
  const safeTarget = Math.max(1, targetMinutes);
  const uniqueFocusDay = deduplicateSessions(sessions).filter(
    (s) => s.sessionType === 'FOCUS' && s.localDate === targetDate,
  );

  const completed = uniqueFocusDay.filter((s) => s.sessionStatus === 'COMPLETED');
  const earlyEnded = uniqueFocusDay.filter((s) => s.sessionStatus === 'ENDED_EARLY');

  const completedFocusSeconds = completed.reduce((acc, s) => {
    const validSeconds = Math.min(
      Math.max(0, s.actualDurationSeconds),
      Math.max(0, s.plannedDurationSeconds),
    );
    return acc + validSeconds;
  }, 0);

  const completedFocusMinutes = Math.floor(completedFocusSeconds / 60);
  const remainingMinutes = Math.max(0, safeTarget - completedFocusMinutes);
  const rawPct = (completedFocusSeconds / (safeTarget * 60)) * 100;
  const completionPercentage = Math.min(100, Math.max(0, Math.round(rawPct * 10) / 10));

  return {
    date: targetDate,
    targetMinutes: safeTarget,
    completedFocusMinutes,
    completedFocusSeconds,
    remainingMinutes,
    completionPercentage,
    completedSessions: completed.length,
    earlyEndedSessions: earlyEnded.length,
  };
}

export function calculateWeeklyStats(
  sessions: FocusSessionRecord[],
  endDate: string = getLocalDateString(),
  language: AppLanguage = 'en',
): WeeklyStats {
  const uniqueCompletedFocus = deduplicateSessions(sessions).filter(
    (s) => s.sessionType === 'FOCUS' && s.sessionStatus === 'COMPLETED',
  );

  const dates: string[] = [];
  for (let i = 6; i >= 0; i -= 1) {
    dates.push(shiftLocalDateString(endDate, -i));
  }
  const dateSet = new Set(dates);

  const sessionsInRange = uniqueCompletedFocus.filter((s) => dateSet.has(s.localDate));
  const locale = language === 'bn' ? 'bn-BD' : 'en-US';

  const dailyBreakdown: WeeklyDayBreakdown[] = dates.map((dateStr) => {
    const [y, m, d] = dateStr.split('-').map(Number);
    const localDateObj = new Date(y, m - 1, d);
    const dayShortLabel = localDateObj.toLocaleDateString(locale, { weekday: 'short' });
    const daySessions = sessionsInRange.filter((s) => s.localDate === dateStr);
    const daySeconds = daySessions.reduce(
      (sum, s) =>
        sum + Math.min(Math.max(0, s.actualDurationSeconds), Math.max(0, s.plannedDurationSeconds)),
      0,
    );
    return {
      date: dateStr,
      dayShortLabel,
      focusMinutes: Math.floor(daySeconds / 60),
      completedSessions: daySessions.length,
    };
  });

  const totalSeconds = sessionsInRange.reduce(
    (sum, s) =>
      sum + Math.min(Math.max(0, s.actualDurationSeconds), Math.max(0, s.plannedDurationSeconds)),
    0,
  );
  const totalWeeklyFocusMinutes = Math.floor(totalSeconds / 60);
  const completedSessions = sessionsInRange.length;
  const averageSessionMinutes =
    completedSessions > 0 ? Math.floor(totalWeeklyFocusMinutes / completedSessions) : 0;

  return {
    totalWeeklyFocusMinutes,
    completedSessions,
    averageSessionMinutes,
    dailyBreakdown,
  };
}
