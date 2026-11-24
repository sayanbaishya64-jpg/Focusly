import assert from 'node:assert/strict';
import test from 'node:test';
import { FocusLocalRepository, type KeyValueStorage } from '../data/focusStore.ts';
import {
  DEFAULT_PREFERENCES,
  type ActiveTimerSnapshot,
  type OnboardingPreferences,
  type StudyTaskItem,
} from '../types/focusly.ts';
import {
  MISSION_DAILY_GOAL,
  MISSION_DAILY_TASK,
  MISSION_FIRST_SESSION,
  evaluateDailyMissions,
  selectAutoScheduleTask,
} from '../utils/dailyMissions.ts';
import { getLocalDateString } from '../utils/timerEngine.ts';

class MemoryStorage implements KeyValueStorage {
  private store = new Map<string, string>();
  getItem(key: string): string | null {
    return this.store.get(key) ?? null;
  }
  setItem(key: string, value: string): void {
    this.store.set(key, value);
  }
  removeItem(key: string): void {
    this.store.delete(key);
  }
}

test('DailyMissions: evaluates completed session, task, and daily goal; claiming is idempotent', () => {
  const repo = new FocusLocalRepository(new MemoryStorage());
  const completionTs = Date.UTC(2026, 10, 24, 14, 0, 0);
  const todayStr = getLocalDateString(completionTs);

  const { createdTask } = repo.createTask({
    title: 'Solve Organic Chemistry Set',
    description: '',
    subjectId: null,
    plannedDurationMinutes: 45,
    priority: 'HIGH',
    scheduledDate: todayStr,
  });
  repo.updateTaskStatus(createdTask.id, 'COMPLETED');
  // Ensure task updatedAt matches local date of completionTs
  const dbBefore = repo.load();
  const adjustedTasks = dbBefore.tasks.map((t) => ({ ...t, updatedAt: completionTs }));

  const snap: ActiveTimerSnapshot = {
    sessionId: 'sess-p6-1',
    sessionType: 'FOCUS',
    plannedDurationSeconds: 60 * 60,
    startTimestampMs: completionTs - 3600_000,
    pauseStartedTimestampMs: null,
    accumulatedPausedMs: 0,
    status: 'RUNNING',
    subjectId: null,
    taskId: createdTask.id,
  };
  repo.recordSessionCompletion(snap, 'COMPLETED', 3600, completionTs, 60);

  const dbAfterSession = repo.load();
  const missions = evaluateDailyMissions(
    dbAfterSession.sessions,
    adjustedTasks,
    60,
    todayStr,
    repo.getClaimedMissionIdsForDate(todayStr),
  );

  const first = missions.find((m) => m.id === MISSION_FIRST_SESSION);
  const taskMission = missions.find((m) => m.id === MISSION_DAILY_TASK);
  const goalMission = missions.find((m) => m.id === MISSION_DAILY_GOAL);

  assert.equal(first?.isCompleted, true);
  assert.equal(taskMission?.isCompleted, true);
  assert.equal(goalMission?.isCompleted, true);
  assert.equal(goalMission?.currentProgress, 100);

  const initialCoins = repo.getRewardPreview().focusCoins;
  assert.equal(initialCoins, 130);

  const firstClaim = repo.claimDailyMission(MISSION_FIRST_SESSION, todayStr);
  assert.equal(firstClaim.claimed, true);
  assert.equal(repo.getRewardPreview().focusCoins, 135);

  // Duplicate claim on same date must be ignored
  const dupClaim = repo.claimDailyMission(MISSION_FIRST_SESSION, todayStr);
  assert.equal(dupClaim.claimed, false);
  assert.equal(repo.getRewardPreview().focusCoins, 135);
});

test('Auto-Schedule: picks in-progress or highest-priority today/overdue task and ignores completed', () => {
  const today = '2026-11-24';
  const tasks: StudyTaskItem[] = [
    {
      id: 'done-high',
      title: 'Finished',
      description: '',
      subjectId: null,
      plannedDurationMinutes: 30,
      priority: 'HIGH',
      status: 'COMPLETED',
      scheduledDate: today,
      createdAt: 1,
      updatedAt: 1,
    },
    {
      id: 'future-high',
      title: 'Tomorrow High',
      description: '',
      subjectId: null,
      plannedDurationMinutes: 45,
      priority: 'HIGH',
      status: 'PENDING',
      scheduledDate: '2026-11-25',
      createdAt: 2,
      updatedAt: 2,
    },
    {
      id: 'overdue-high',
      title: 'Overdue High',
      description: '',
      subjectId: null,
      plannedDurationMinutes: 50,
      priority: 'HIGH',
      status: 'PENDING',
      scheduledDate: '2026-11-23',
      createdAt: 3,
      updatedAt: 3,
    },
    {
      id: 'today-med',
      title: 'Today Medium',
      description: '',
      subjectId: null,
      plannedDurationMinutes: 25,
      priority: 'MEDIUM',
      status: 'PENDING',
      scheduledDate: today,
      createdAt: 4,
      updatedAt: 4,
    },
  ];

  const picked = selectAutoScheduleTask(tasks, today);
  assert.equal(picked?.id, 'overdue-high');
});

test('Backup Export & Import: round-trips full database and rejects malformed/unsupported JSON without corrupting state', () => {
  const storage = new MemoryStorage();
  const repoA = new FocusLocalRepository(storage);
  repoA.updateStudentName('Nadia');
  const { subject } = repoA.upsertSubject('Physics', '#3B82F6');
  const { createdTask } = repoA.createTask({
    title: 'Electromagnetism Notes',
    description: 'Chapter 4',
    subjectId: subject.id,
    plannedDurationMinutes: 45,
    priority: 'HIGH',
    scheduledDate: '2026-11-24',
  });
  const ts = Date.UTC(2026, 10, 24, 16, 0, 0);
  repoA.recordSessionCompletion(
    {
      sessionId: 'sess-backup-1',
      sessionType: 'FOCUS',
      plannedDurationSeconds: 2700,
      startTimestampMs: ts - 2700_000,
      pauseStartedTimestampMs: null,
      accumulatedPausedMs: 0,
      status: 'RUNNING',
      subjectId: subject.id,
      taskId: createdTask.id,
    },
    'COMPLETED',
    2700,
    ts,
    90,
  );

  const prefs: OnboardingPreferences = {
    ...DEFAULT_PREFERENCES,
    onboardingCompleted: true,
    studentCategory: 'competitive_aspirant',
    dailyFocusTargetMinutes: 90,
    language: 'bn',
    selectedDistractionApps: ['com.instagram.android'],
  };

  const exportedJson = repoA.exportBackupJson(prefs);

  // Restore into a fresh repository instance
  const repoB = new FocusLocalRepository(new MemoryStorage());
  repoB.updateStudentName('ExistingUser');

  // Reject invalid JSON & unsupported schema version without touching ExistingUser
  const badJsonRes = repoB.importBackupJson('{ not valid json');
  assert.equal(badJsonRes.ok, false);
  assert.equal(repoB.load().studentName, 'ExistingUser');

  const wrongSchemaRes = repoB.importBackupJson(
    JSON.stringify({ schemaVersion: 99, preferences: {}, subjects: [], tasks: [], sessions: [] }),
  );
  assert.equal(wrongSchemaRes.ok, false);
  assert.equal(repoB.load().studentName, 'ExistingUser');

  // Restore valid backup
  const okRes = repoB.importBackupJson(exportedJson);
  assert.equal(okRes.ok, true);
  if (okRes.ok) {
    assert.equal(okRes.sessionsCount, 1);
    assert.equal(okRes.tasksCount, 1);
    assert.equal(okRes.subjectsCount, 1);
    assert.equal(okRes.preferences.language, 'bn');
    assert.equal(okRes.preferences.dailyFocusTargetMinutes, 90);
  }
  const restoredDb = repoB.load();
  assert.equal(restoredDb.studentName, 'Nadia');
  assert.equal(restoredDb.sessions.length, 1);
  assert.equal(restoredDb.rewardTransactions.length > 0, true);
});
