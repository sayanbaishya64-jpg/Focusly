import type {
  AnalyticsPeriod,
  AppLanguage,
  FocusSessionRecord,
  ProgressAnalyticsSummary,
  StudyScheduleFilter,
  StudyTaskItem,
  SubjectItem,
  SubjectProgressSummary,
  TaskPriority,
  WeeklyDayBreakdown,
} from '../types/focusly';
import { getLocalDateString, shiftLocalDateString } from './timerEngine';

const GENERAL_STUDY_COLOR = '#22D3EE';

const PRIORITY_RANK: Record<TaskPriority, number> = {
  HIGH: 0,
  MEDIUM: 1,
  LOW: 2,
};

export function findOverdueUnfinishedTasks(
  tasks: StudyTaskItem[],
  todayDate: string = getLocalDateString(),
): StudyTaskItem[] {
  return tasks.filter(
    (t) =>
      t.status !== 'COMPLETED' &&
      Boolean(t.scheduledDate) &&
      (t.scheduledDate as string) < todayDate,
  );
}

export function filterAndSortTasks(
  tasks: StudyTaskItem[],
  filter: StudyScheduleFilter,
  subjectFilterId: string | null = null,
  todayDate: string = getLocalDateString(),
): StudyTaskItem[] {
  const bySubject = subjectFilterId
    ? tasks.filter((t) => t.subjectId === subjectFilterId)
    : tasks;

  const bySchedule = bySubject.filter((t) => {
    if (filter === 'TODAY') {
      return t.status !== 'COMPLETED' && (!t.scheduledDate || t.scheduledDate === todayDate);
    }
    if (filter === 'UPCOMING') {
      return t.status !== 'COMPLETED' && Boolean(t.scheduledDate) && (t.scheduledDate as string) > todayDate;
    }
    if (filter === 'ALL') {
      return t.status !== 'COMPLETED';
    }
    return t.status === 'COMPLETED';
  });

  return [...bySchedule].sort((a, b) => {
    if (a.status !== b.status && (a.status === 'COMPLETED' || b.status === 'COMPLETED')) {
      return a.status === 'COMPLETED' ? 1 : -1;
    }
    const prioDiff = PRIORITY_RANK[a.priority] - PRIORITY_RANK[b.priority];
    if (prioDiff !== 0) return prioDiff;

    const aDate = a.scheduledDate ?? '9999-99-99';
    const bDate = b.scheduledDate ?? '9999-99-99';
    if (aDate !== bDate) return aDate.localeCompare(bDate);

    return b.updatedAt - a.updatedAt;
  });
}

function deduplicateSessions(sessions: FocusSessionRecord[]): FocusSessionRecord[] {
  const seen = new Set<string>();
  const out: FocusSessionRecord[] = [];
  for (const s of sessions) {
    if (!seen.has(s.id)) {
      seen.add(s.id);
      out.push(s);
    }
  }
  return out;
}

export function calculateProgressAnalytics(
  sessions: FocusSessionRecord[],
  subjects: SubjectItem[],
  period: AnalyticsPeriod,
  endDate: string = getLocalDateString(),
  language: AppLanguage = 'en',
  generalStudyLabel: string = 'General Study',
): ProgressAnalyticsSummary {
  const daysCount = period === 'WEEK' ? 7 : 30;
  const dates: string[] = [];
  for (let i = daysCount - 1; i >= 0; i -= 1) {
    dates.push(shiftLocalDateString(endDate, -i));
  }
  const dateSet = new Set(dates);

  const uniqueFocusInRange = deduplicateSessions(sessions).filter(
    (s) => s.sessionType === 'FOCUS' && dateSet.has(s.localDate),
  );

  const completed = uniqueFocusInRange.filter((s) => s.sessionStatus === 'COMPLETED');
  const earlyEnded = uniqueFocusInRange.filter((s) => s.sessionStatus === 'ENDED_EARLY');
  const totalAttempts = completed.length + earlyEnded.length;

  const completionRatePercent =
    totalAttempts > 0 ? Math.round((completed.length / totalAttempts) * 1000) / 10 : 0;

  const totalCompletedSeconds = completed.reduce((sum, s) => {
    const valid = Math.min(Math.max(0, s.actualDurationSeconds), Math.max(0, s.plannedDurationSeconds));
    return sum + valid;
  }, 0);
  const totalFocusMinutes = Math.floor(totalCompletedSeconds / 60);
  const averageSessionMinutes =
    completed.length > 0 ? Math.floor(totalFocusMinutes / completed.length) : 0;

  const locale = language === 'bn' ? 'bn-BD' : 'en-US';
  const dailyBreakdown: WeeklyDayBreakdown[] = dates.map((dateStr) => {
    const [y, m, d] = dateStr.split('-').map(Number);
    const dt = new Date(y, m - 1, d);
    const label =
      period === 'WEEK'
        ? dt.toLocaleDateString(locale, { weekday: 'short' })
        : String(dt.getDate());
    const daySessions = completed.filter((s) => s.localDate === dateStr);
    const daySeconds = daySessions.reduce(
      (sum, s) =>
        sum + Math.min(Math.max(0, s.actualDurationSeconds), Math.max(0, s.plannedDurationSeconds)),
      0,
    );
    return {
      date: dateStr,
      dayShortLabel: label,
      focusMinutes: Math.floor(daySeconds / 60),
      completedSessions: daySessions.length,
    };
  });

  const activeDaysCount = dailyBreakdown.filter((d) => d.focusMinutes > 0).length;

  const subjectMap = new Map(subjects.map((s) => [s.id, s]));
  const grouped = new Map<string | null, FocusSessionRecord[]>();
  for (const s of completed) {
    const key = s.subjectId && subjectMap.has(s.subjectId) ? s.subjectId : null;
    const list = grouped.get(key) ?? [];
    list.push(s);
    grouped.set(key, list);
  }

  const subjectBreakdown: SubjectProgressSummary[] = Array.from(grouped.entries())
    .map(([subjId, list]) => {
      const subj = subjId ? subjectMap.get(subjId) : undefined;
      const secs = list.reduce(
        (sum, s) =>
          sum + Math.min(Math.max(0, s.actualDurationSeconds), Math.max(0, s.plannedDurationSeconds)),
        0,
      );
      const mins = Math.floor(secs / 60);
      const sharePercentage =
        totalFocusMinutes > 0 ? Math.round((mins / totalFocusMinutes) * 1000) / 10 : 0;
      return {
        subjectId: subjId,
        subjectName: subj?.subjectName ?? generalStudyLabel,
        subjectColor: subj?.subjectColor ?? GENERAL_STUDY_COLOR,
        focusMinutes: mins,
        completedSessions: list.length,
        sharePercentage,
      };
    })
    .sort((a, b) => b.focusMinutes - a.focusMinutes || b.completedSessions - a.completedSessions);

  return {
    period,
    totalFocusMinutes,
    completedSessions: completed.length,
    earlyEndedSessions: earlyEnded.length,
    completionRatePercent,
    averageSessionMinutes,
    activeDaysCount,
    dailyBreakdown,
    subjectBreakdown,
  };
}
