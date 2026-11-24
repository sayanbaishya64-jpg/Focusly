import type {
  DailyMissionItem,
  FocusSessionRecord,
  StudyTaskItem,
  TaskPriority,
} from '../types/focusly';
import { calculateDailyStats, getLocalDateString } from './timerEngine';

export interface DailyMissionDefinition {
  id: string;
  title: string;
  description: string;
  rewardCoins: number;
  targetProgress: number;
}

export const MISSION_FIRST_SESSION = 'mission_first_session';
export const MISSION_DAILY_TASK = 'mission_daily_task';
export const MISSION_DAILY_GOAL = 'mission_daily_goal';

export const MISSIONS_CATALOG: DailyMissionDefinition[] = [
  {
    id: MISSION_FIRST_SESSION,
    title: 'First Focus Block',
    description: 'Complete 1 focus session today',
    rewardCoins: 5,
    targetProgress: 1,
  },
  {
    id: MISSION_DAILY_TASK,
    title: 'Finish a Study Task',
    description: 'Mark 1 planned study task completed today',
    rewardCoins: 5,
    targetProgress: 1,
  },
  {
    id: MISSION_DAILY_GOAL,
    title: 'Hit Daily Target',
    description: "Reach 100% of today's focus minute goal",
    rewardCoins: 10,
    targetProgress: 100,
  },
];

export function evaluateDailyMissions(
  sessions: FocusSessionRecord[],
  tasks: StudyTaskItem[],
  dailyTargetMinutes: number,
  todayDate: string,
  claimedMissionIds: Set<string>,
): DailyMissionItem[] {
  const dailyStats = calculateDailyStats(sessions, dailyTargetMinutes, todayDate);

  const seenSessionIds = new Set<string>();
  let completedSessionsToday = 0;
  for (const s of sessions) {
    if (seenSessionIds.has(s.id)) continue;
    seenSessionIds.add(s.id);
    if (
      s.sessionType === 'FOCUS' &&
      s.sessionStatus === 'COMPLETED' &&
      s.localDate === todayDate
    ) {
      completedSessionsToday += 1;
    }
  }

  let completedTasksToday = 0;
  for (const task of tasks) {
    if (
      task.status === 'COMPLETED' &&
      getLocalDateString(task.updatedAt) === todayDate
    ) {
      completedTasksToday += 1;
    }
  }

  const goalProgressPct = Math.min(100, Math.max(0, Math.floor(dailyStats.completionPercentage)));

  return MISSIONS_CATALOG.map((def) => {
    let currentProgress = 0;
    let isCompleted = false;

    if (def.id === MISSION_FIRST_SESSION) {
      currentProgress = Math.min(def.targetProgress, completedSessionsToday);
      isCompleted = currentProgress >= def.targetProgress;
    } else if (def.id === MISSION_DAILY_TASK) {
      currentProgress = Math.min(def.targetProgress, completedTasksToday);
      isCompleted = currentProgress >= def.targetProgress;
    } else if (def.id === MISSION_DAILY_GOAL) {
      currentProgress = goalProgressPct;
      isCompleted = dailyStats.completedFocusMinutes >= dailyStats.targetMinutes;
    }

    return {
      id: def.id,
      title: def.title,
      description: def.description,
      rewardCoins: def.rewardCoins,
      currentProgress,
      targetProgress: def.targetProgress,
      isCompleted,
      isClaimed: claimedMissionIds.has(def.id),
    };
  });
}

function priorityRank(priority: TaskPriority): number {
  switch (priority) {
    case 'HIGH':
      return 0;
    case 'MEDIUM':
      return 1;
    case 'LOW':
    default:
      return 2;
  }
}

export function selectAutoScheduleTask(
  tasks: StudyTaskItem[],
  todayDate: string,
): StudyTaskItem | null {
  const activeTasks = tasks.filter((t) => t.status !== 'COMPLETED');
  if (activeTasks.length === 0) return null;

  const todayOrOverdue = activeTasks.filter(
    (t) => t.scheduledDate != null && t.scheduledDate <= todayDate,
  );
  const pool = todayOrOverdue.length > 0 ? todayOrOverdue : activeTasks;

  const sorted = [...pool].sort((a, b) => {
    const inProgressDiff =
      (a.status === 'IN_PROGRESS' ? 0 : 1) - (b.status === 'IN_PROGRESS' ? 0 : 1);
    if (inProgressDiff !== 0) return inProgressDiff;

    const prioDiff = priorityRank(a.priority) - priorityRank(b.priority);
    if (prioDiff !== 0) return prioDiff;

    const dateA = a.scheduledDate ?? '9999-12-31';
    const dateB = b.scheduledDate ?? '9999-12-31';
    if (dateA !== dateB) return dateA.localeCompare(dateB);

    return b.updatedAt - a.updatedAt;
  });

  return sorted[0] ?? null;
}
