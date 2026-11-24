import type {
  AchievementUnlockRecord,
  FocusSessionRecord,
  FocuslyBackupPayload,
  OnboardingPreferences,
  RewardTransactionRecord,
  RewardTransactionType,
  SessionStatus,
  SessionType,
  StudentCategory,
  StudyGoal,
  StudyTaskItem,
  SubjectItem,
  TaskPriority,
  TaskStatus,
  UnlockedGardenItemRecord,
} from '../types/focusly';

export const SUPPORTED_BACKUP_SCHEMA_VERSION = 1;

const VALID_CATEGORIES = new Set<StudentCategory>([
  'school_student',
  'college_student',
  'competitive_aspirant',
  'self_learner',
]);

const VALID_GOALS = new Set<StudyGoal>([
  'improve_concentration',
  'reduce_distractions',
  'build_consistency',
  'complete_daily_goals',
]);

const VALID_TASK_PRIORITY = new Set<TaskPriority>(['LOW', 'MEDIUM', 'HIGH']);
const VALID_TASK_STATUS = new Set<TaskStatus>(['PENDING', 'IN_PROGRESS', 'COMPLETED']);
const VALID_SESSION_STATUS = new Set<SessionStatus>([
  'IDLE',
  'RUNNING',
  'PAUSED',
  'COMPLETED',
  'ENDED_EARLY',
]);
const VALID_SESSION_TYPE = new Set<SessionType>(['FOCUS', 'BREAK']);
const VALID_TX_TYPE = new Set<RewardTransactionType>([
  'EARN_SESSION',
  'EARN_DAILY_GOAL',
  'EARN_ACHIEVEMENT',
  'EARN_MISSION',
  'SPEND_STORE',
]);

export function encodeBackupJson(payload: FocuslyBackupPayload): string {
  return JSON.stringify(payload, null, 2);
}

export function decodeBackupJson(
  rawJson: string,
): { ok: true; payload: FocuslyBackupPayload } | { ok: false; error: string } {
  let parsed: unknown;
  try {
    parsed = JSON.parse(rawJson);
  } catch {
    return { ok: false, error: 'Malformed JSON file.' };
  }

  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    return { ok: false, error: 'Backup root must be a JSON object.' };
  }

  const root = parsed as Record<string, unknown>;
  if (root.schemaVersion !== SUPPORTED_BACKUP_SCHEMA_VERSION) {
    return {
      ok: false,
      error: `Unsupported backup schema version: ${String(root.schemaVersion ?? 'missing')}`,
    };
  }

  if (!root.preferences || typeof root.preferences !== 'object' || Array.isArray(root.preferences)) {
    return { ok: false, error: 'Missing preferences object.' };
  }
  const rawPrefs = root.preferences as Record<string, unknown>;

  if (!Array.isArray(root.subjects) || !Array.isArray(root.tasks) || !Array.isArray(root.sessions)) {
    return { ok: false, error: 'Backup must include subjects, tasks, and sessions arrays.' };
  }

  const rawCategory = rawPrefs.studentCategory;
  const studentCategory: StudentCategory =
    typeof rawCategory === 'string' && VALID_CATEGORIES.has(rawCategory as StudentCategory)
      ? (rawCategory as StudentCategory)
      : 'college_student';

  const rawGoals = Array.isArray(rawPrefs.studyGoals)
    ? rawPrefs.studyGoals.filter(
        (g): g is StudyGoal => typeof g === 'string' && VALID_GOALS.has(g as StudyGoal),
      )
    : [];

  const targetNum =
    typeof rawPrefs.dailyFocusTargetMinutes === 'number'
      ? Math.min(480, Math.max(10, Math.round(rawPrefs.dailyFocusTargetMinutes)))
      : 60;

  const preferences: OnboardingPreferences = {
    onboardingCompleted:
      typeof rawPrefs.onboardingCompleted === 'boolean' ? rawPrefs.onboardingCompleted : true,
    studentCategory,
    studyGoals: rawGoals.length > 0 ? rawGoals : ['improve_concentration'],
    dailyFocusTargetMinutes: targetNum,
    language: rawPrefs.language === 'bn' ? 'bn' : 'en',
    selectedDistractionApps: Array.isArray(rawPrefs.selectedDistractionApps)
      ? rawPrefs.selectedDistractionApps.filter((x): x is string => typeof x === 'string')
      : [],
    distractionPreferencesSkipped: Boolean(rawPrefs.distractionPreferencesSkipped),
  };

  const now = Date.now();
  const subjects: SubjectItem[] = [];
  for (const item of root.subjects) {
    if (!item || typeof item !== 'object') {
      return { ok: false, error: 'Invalid subject entry in backup.' };
    }
    const o = item as Record<string, unknown>;
    if (typeof o.id !== 'string' || !o.id || typeof o.subjectName !== 'string' || !o.subjectName) {
      return { ok: false, error: 'Subject entry missing id or subjectName.' };
    }
    subjects.push({
      id: o.id,
      subjectName: o.subjectName,
      subjectColor: typeof o.subjectColor === 'string' ? o.subjectColor : '#3B82F6',
      createdAt: typeof o.createdAt === 'number' ? o.createdAt : now,
      updatedAt: typeof o.updatedAt === 'number' ? o.updatedAt : now,
    });
  }

  const subjectIds = new Set(subjects.map((s) => s.id));
  const tasks: StudyTaskItem[] = [];
  for (const item of root.tasks) {
    if (!item || typeof item !== 'object') {
      return { ok: false, error: 'Invalid task entry in backup.' };
    }
    const o = item as Record<string, unknown>;
    if (typeof o.id !== 'string' || !o.id || typeof o.title !== 'string' || !o.title) {
      return { ok: false, error: 'Task entry missing id or title.' };
    }
    const rawSub = typeof o.subjectId === 'string' && subjectIds.has(o.subjectId) ? o.subjectId : null;
    tasks.push({
      id: o.id,
      title: o.title,
      description: typeof o.description === 'string' ? o.description : '',
      subjectId: rawSub,
      plannedDurationMinutes:
        typeof o.plannedDurationMinutes === 'number'
          ? Math.min(480, Math.max(5, Math.round(o.plannedDurationMinutes)))
          : 25,
      priority:
        typeof o.priority === 'string' && VALID_TASK_PRIORITY.has(o.priority as TaskPriority)
          ? (o.priority as TaskPriority)
          : 'MEDIUM',
      status:
        typeof o.status === 'string' && VALID_TASK_STATUS.has(o.status as TaskStatus)
          ? (o.status as TaskStatus)
          : 'PENDING',
      scheduledDate: typeof o.scheduledDate === 'string' ? o.scheduledDate : null,
      createdAt: typeof o.createdAt === 'number' ? o.createdAt : now,
      updatedAt: typeof o.updatedAt === 'number' ? o.updatedAt : now,
    });
  }

  const taskIds = new Set(tasks.map((t) => t.id));
  const sessions: FocusSessionRecord[] = [];
  for (const item of root.sessions) {
    if (!item || typeof item !== 'object') {
      return { ok: false, error: 'Invalid session entry in backup.' };
    }
    const o = item as Record<string, unknown>;
    if (
      typeof o.id !== 'string' ||
      !o.id ||
      typeof o.plannedDurationSeconds !== 'number' ||
      typeof o.actualDurationSeconds !== 'number' ||
      typeof o.startTimestamp !== 'number' ||
      typeof o.localDate !== 'string' ||
      !o.localDate
    ) {
      return { ok: false, error: 'Session entry missing required fields.' };
    }
    sessions.push({
      id: o.id,
      plannedDurationSeconds: Math.max(1, Math.floor(o.plannedDurationSeconds)),
      actualDurationSeconds: Math.max(0, Math.floor(o.actualDurationSeconds)),
      startTimestamp: o.startTimestamp,
      completionTimestamp: typeof o.completionTimestamp === 'number' ? o.completionTimestamp : null,
      pausedDurationSeconds:
        typeof o.pausedDurationSeconds === 'number' ? Math.max(0, Math.floor(o.pausedDurationSeconds)) : 0,
      sessionStatus:
        typeof o.sessionStatus === 'string' && VALID_SESSION_STATUS.has(o.sessionStatus as SessionStatus)
          ? (o.sessionStatus as SessionStatus)
          : 'COMPLETED',
      sessionType:
        typeof o.sessionType === 'string' && VALID_SESSION_TYPE.has(o.sessionType as SessionType)
          ? (o.sessionType as SessionType)
          : 'FOCUS',
      localDate: o.localDate,
      subjectId: typeof o.subjectId === 'string' && subjectIds.has(o.subjectId) ? o.subjectId : null,
      taskId: typeof o.taskId === 'string' && taskIds.has(o.taskId) ? o.taskId : null,
      createdAt: typeof o.createdAt === 'number' ? o.createdAt : o.startTimestamp,
    });
  }

  const rewardTransactions: RewardTransactionRecord[] = [];
  if (Array.isArray(root.rewardTransactions)) {
    for (const item of root.rewardTransactions) {
      if (!item || typeof item !== 'object') continue;
      const o = item as Record<string, unknown>;
      if (
        typeof o.id === 'string' &&
        typeof o.amount === 'number' &&
        typeof o.title === 'string' &&
        typeof o.localDate === 'string'
      ) {
        rewardTransactions.push({
          id: o.id,
          type:
            typeof o.type === 'string' && VALID_TX_TYPE.has(o.type as RewardTransactionType)
              ? (o.type as RewardTransactionType)
              : 'EARN_SESSION',
          amount: Math.trunc(o.amount),
          title: o.title,
          referenceId: typeof o.referenceId === 'string' ? o.referenceId : null,
          localDate: o.localDate,
          createdAt: typeof o.createdAt === 'number' ? o.createdAt : now,
        });
      }
    }
  }

  const unlockedAchievements: AchievementUnlockRecord[] = [];
  if (Array.isArray(root.unlockedAchievements)) {
    for (const item of root.unlockedAchievements) {
      if (!item || typeof item !== 'object') continue;
      const o = item as Record<string, unknown>;
      if (typeof o.achievementId === 'string') {
        unlockedAchievements.push({
          achievementId: o.achievementId,
          unlockedAt: typeof o.unlockedAt === 'number' ? o.unlockedAt : now,
        });
      }
    }
  }

  const unlockedGardenItems: UnlockedGardenItemRecord[] = [];
  if (Array.isArray(root.unlockedGardenItems)) {
    for (const item of root.unlockedGardenItems) {
      if (!item || typeof item !== 'object') continue;
      const o = item as Record<string, unknown>;
      if (typeof o.itemId === 'string') {
        unlockedGardenItems.push({
          itemId: o.itemId,
          purchasedAt: typeof o.purchasedAt === 'number' ? o.purchasedAt : now,
          isFeatured: Boolean(o.isFeatured),
        });
      }
    }
  }

  return {
    ok: true,
    payload: {
      schemaVersion: SUPPORTED_BACKUP_SCHEMA_VERSION,
      exportedAt: typeof root.exportedAt === 'number' ? root.exportedAt : now,
      preferences,
      studentName: typeof root.studentName === 'string' ? root.studentName : '',
      subjects,
      tasks,
      sessions,
      rewardTransactions,
      unlockedAchievements,
      unlockedGardenItems,
    },
  };
}
