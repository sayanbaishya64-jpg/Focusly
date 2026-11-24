import type {
  DistractionBlockerMode,
  DistractionBlockerSettings,
  FocusRecommendation,
  FocusSessionRecord,
} from '../types/focusly';

export const MIN_SESSIONS_FOR_RECOMMENDATION = 3;

export function roundToNearestFive(minutes: number): number {
  return Math.min(120, Math.max(5, Math.round(minutes / 5) * 5));
}

export function detectPreferredStudyPeriod(
  sessions: FocusSessionRecord[],
): 'morning' | 'afternoon' | 'evening' | null {
  const completed = sessions.filter((s) => s.sessionStatus === 'COMPLETED');
  if (completed.length < 3) return null;
  const counts: Record<'morning' | 'afternoon' | 'evening', number> = {
    morning: 0,
    afternoon: 0,
    evening: 0,
  };
  for (const s of completed) {
    const hour = new Date(s.startTimestamp).getHours();
    if (hour < 12) counts.morning += 1;
    else if (hour < 17) counts.afternoon += 1;
    else counts.evening += 1;
  }
  const sorted = Object.entries(counts).sort((a, b) => b[1] - a[1]);
  return sorted[0][0] as 'morning' | 'afternoon' | 'evening';
}

export function generateAdaptiveRecommendation(
  sessions: FocusSessionRecord[],
  dailyTargetMinutes: number,
  preferredSubjectId: string | null = null,
  nowMs: number = Date.now(),
): FocusRecommendation {
  const seen = new Set<string>();
  const uniqueFocusSessions = sessions
    .filter((s) => {
      if (seen.has(s.id)) return false;
      seen.add(s.id);
      return (
        s.sessionType === 'FOCUS' &&
        (s.sessionStatus === 'COMPLETED' || s.sessionStatus === 'ENDED_EARLY')
      );
    })
    .sort((a, b) => b.startTimestamp - a.startTimestamp);

  const count = uniqueFocusSessions.length;
  const safeTarget = Math.min(480, Math.max(10, dailyTargetMinutes));

  if (count < MIN_SESSIONS_FOR_RECOMMENDATION) {
    const starterMinutes = Math.min(25, Math.max(15, safeTarget));
    return {
      id: `rec-insufficient-${count}`,
      recommendedMinutes: starterMinutes,
      reasonType: 'INSUFFICIENT_DATA',
      reasonText:
        'Complete at least 3 focus sessions so FOCUSLY can suggest realistic durations from your local study history.',
      confidence: 'INSUFFICIENT_DATA',
      hasEnoughData: false,
      basedOnSessionCount: count,
      preferredPeriod: null,
      subjectId: preferredSubjectId,
      status: 'ACTIVE',
      generatedAt: nowMs,
    };
  }

  const preferredPeriod = detectPreferredStudyPeriod(uniqueFocusSessions);
  const subjectSessions = preferredSubjectId
    ? uniqueFocusSessions.filter((s) => s.subjectId === preferredSubjectId)
    : [];
  const activeHistory = subjectSessions.length >= 3 ? subjectSessions : uniqueFocusSessions;
  const scopedSubjectId = subjectSessions.length >= 3 ? preferredSubjectId : null;

  const recentThree = activeHistory.slice(0, 3);
  const recentEarlyCount = recentThree.filter((s) => s.sessionStatus === 'ENDED_EARLY').length;
  const recentFive = activeHistory.slice(0, 5);
  const recentCompletionRate =
    recentFive.filter((s) => s.sessionStatus === 'COMPLETED').length / recentFive.length;

  // Rule 1: Recent struggle / consecutive early endings
  if (recentEarlyCount >= 2 || recentCompletionRate < 0.45) {
    const completedActualMins = activeHistory
      .filter((s) => s.sessionStatus === 'COMPLETED')
      .map((s) => Math.max(5, Math.floor(s.actualDurationSeconds / 60)));
    const fallbackMins =
      completedActualMins.length > 0
        ? Math.min(20, Math.max(10, roundToNearestFive(Math.min(...completedActualMins))))
        : 15;
    return {
      id: `rec-struggle-${activeHistory.length}`,
      recommendedMinutes: fallbackMins,
      reasonType: 'RECENT_STRUGGLE',
      reasonText: 'Start with a shorter session and build your momentum.',
      confidence: 'HIGH',
      hasEnoughData: true,
      basedOnSessionCount: count,
      preferredPeriod,
      subjectId: scopedSubjectId,
      status: 'ACTIVE',
      generatedAt: nowMs,
    };
  }

  // Rule 2: Regularly completes shorter sessions (15-35m) while ending long sessions (45m+) early
  const shortMediumSessions = activeHistory.filter((s) => {
    const plannedMin = Math.floor(s.plannedDurationSeconds / 60);
    return plannedMin >= 15 && plannedMin <= 35;
  });
  const longSessions = activeHistory.filter(
    (s) => Math.floor(s.plannedDurationSeconds / 60) >= 45,
  );

  if (shortMediumSessions.length >= 2 && longSessions.length >= 2) {
    const shortCompletionRate =
      shortMediumSessions.filter((s) => s.sessionStatus === 'COMPLETED').length /
      shortMediumSessions.length;
    const longEarlyRate =
      longSessions.filter((s) => s.sessionStatus === 'ENDED_EARLY').length / longSessions.length;

    if (shortCompletionRate >= 0.7 && longEarlyRate >= 0.5) {
      const counts = new Map<number, number>();
      for (const s of shortMediumSessions.filter((x) => x.sessionStatus === 'COMPLETED')) {
        const bucket = roundToNearestFive(Math.floor(s.plannedDurationSeconds / 60));
        counts.set(bucket, (counts.get(bucket) ?? 0) + 1);
      }
      const bestShort =
        Array.from(counts.entries()).sort((a, b) => b[1] - a[1])[0]?.[0] ?? 25;
      const clampedShort = Math.min(35, Math.max(15, bestShort));
      return {
        id: `rec-dropoff-${clampedShort}`,
        recommendedMinutes: clampedShort,
        reasonType: 'AVOID_LONG_DROPOFF',
        reasonText: `Try a ${clampedShort}-minute focus session today.`,
        confidence: 'HIGH',
        hasEnoughData: true,
        basedOnSessionCount: count,
        preferredPeriod,
        subjectId: scopedSubjectId,
        status: 'ACTIVE',
        generatedAt: nowMs,
      };
    }
  }

  // Rule 3: Consistent mastery on current planned duration -> gentle +5m suggestion (requires approval)
  const buckets = new Map<number, FocusSessionRecord[]>();
  for (const s of activeHistory) {
    const bucket = roundToNearestFive(Math.max(10, Math.floor(s.plannedDurationSeconds / 60)));
    const arr = buckets.get(bucket) ?? [];
    arr.push(s);
    buckets.set(bucket, arr);
  }
  const dominant = Array.from(buckets.entries())
    .filter(([, arr]) => arr.length >= 3)
    .sort((a, b) => b[1].length - a[1].length)[0];

  if (dominant) {
    const [baseMinutes, bucketSessions] = dominant;
    const bucketRate =
      bucketSessions.filter((s) => s.sessionStatus === 'COMPLETED').length / bucketSessions.length;
    if (bucketRate >= 0.85 && baseMinutes < 90) {
      const steppedMinutes = Math.min(90, baseMinutes + 5);
      return {
        id: `rec-stepup-${baseMinutes}-${steppedMinutes}`,
        recommendedMinutes: steppedMinutes,
        reasonType: 'CONSISTENT_STEP_UP',
        reasonText: `You're regularly completing ${baseMinutes}-minute sessions. Would you like to try ${steppedMinutes} minutes?`,
        confidence: 'HIGH',
        hasEnoughData: true,
        basedOnSessionCount: count,
        preferredPeriod,
        subjectId: scopedSubjectId,
        status: 'ACTIVE',
        generatedAt: nowMs,
      };
    }
  }

  // Rule 4: Steady rhythm
  const completedSessions = activeHistory.filter((s) => s.sessionStatus === 'COMPLETED');
  let bestCompleted = 25;
  if (completedSessions.length > 0) {
    const counts = new Map<number, number>();
    for (const s of completedSessions) {
      const bucket = roundToNearestFive(Math.max(15, Math.floor(s.plannedDurationSeconds / 60)));
      counts.set(bucket, (counts.get(bucket) ?? 0) + 1);
    }
    bestCompleted = Array.from(counts.entries()).sort((a, b) => b[1] - a[1])[0]?.[0] ?? 25;
  }
  bestCompleted = Math.min(90, Math.max(15, bestCompleted));

  const periodSuffix = preferredPeriod
    ? ` Your strongest completion streak is in the ${preferredPeriod}.`
    : '';

  return {
    id: `rec-steady-${bestCompleted}`,
    recommendedMinutes: bestCompleted,
    reasonType: 'STEADY_RHYTHM',
    reasonText: `Try a ${bestCompleted}-minute focus session today based on your recent completed blocks.${periodSuffix}`,
    confidence: count >= 6 ? 'HIGH' : 'MEDIUM',
    hasEnoughData: true,
    basedOnSessionCount: count,
    preferredPeriod,
    subjectId: scopedSubjectId,
    status: 'ACTIVE',
    generatedAt: nowMs,
  };
}

export function suggestRecoveryRetryMinutes(
  plannedMinutes: number,
  actualMinutes: number,
): number {
  if (plannedMinutes >= 45 && actualMinutes < 20) return 15;
  if (plannedMinutes >= 25 && actualMinutes < 15) return 10;
  const half = Math.round(Math.round(plannedMinutes / 2) / 5) * 5;
  return Math.min(25, Math.max(5, half));
}

export function resolveDistractionBlockerMode(
  settings: DistractionBlockerSettings,
): DistractionBlockerMode {
  if (!settings.isEnabled) return 'DISABLED';
  if (settings.permissionStatus === 'GRANTED') return 'ACTIVE_DETECTION';
  return 'CHECKLIST_ONLY';
}
