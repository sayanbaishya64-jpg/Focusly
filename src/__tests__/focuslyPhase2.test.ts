import assert from 'node:assert/strict';
import test from 'node:test';
import { FocusLocalRepository, type KeyValueStorage } from '../data/focusStore';
import type { ActiveTimerSnapshot, FocusSessionRecord } from '../types/focusly';
import {
  calculateDailyStats,
  calculateTimerProgress,
  calculateWeeklyStats,
  getLocalDateString,
  SessionStateMachine,
} from '../utils/timerEngine';

class MemoryStorage implements KeyValueStorage {
  private map = new Map<string, string>();
  getItem(key: string): string | null {
    return this.map.get(key) ?? null;
  }
  setItem(key: string, value: string): void {
    this.map.set(key, value);
  }
  removeItem(key: string): void {
    this.map.delete(key);
  }
}

test('1. 25-minute session elapsed time stays accurate after screen lock / backgrounding', () => {
  const startMs = 1_700_000_000_000;
  const snapshot: ActiveTimerSnapshot = {
    sessionId: 'sess-25m',
    sessionType: 'FOCUS',
    plannedDurationSeconds: 25 * 60,
    startTimestampMs: startMs,
    pauseStartedTimestampMs: null,
    accumulatedPausedMs: 0,
    status: 'RUNNING',
    subjectId: null,
    taskId: null,
  };

  // Simulate returning 12 minutes later without continuous counter ticks
  const progress = calculateTimerProgress(snapshot, startMs + 12 * 60_000);
  assert.equal(progress.elapsedSeconds, 720);
  assert.equal(progress.remainingSeconds, 780);
  assert.equal(progress.isCompleted, false);
});

test('2 & 3. Pause and resume exclude paused duration from focused time', () => {
  const startMs = 1_700_000_000_000;
  const running: ActiveTimerSnapshot = {
    sessionId: 'sess-pause',
    sessionType: 'FOCUS',
    plannedDurationSeconds: 1500,
    startTimestampMs: startMs,
    pauseStartedTimestampMs: null,
    accumulatedPausedMs: 0,
    status: 'RUNNING',
    subjectId: null,
    taskId: null,
  };

  const paused = SessionStateMachine.pause(running, startMs + 300_000);
  assert.equal(paused.status, 'PAUSED');

  // Time advances 10 minutes while paused; elapsed must freeze at 5 minutes (300s)
  const pausedProgress = calculateTimerProgress(paused, startMs + 900_000);
  assert.equal(pausedProgress.elapsedSeconds, 300);
  assert.equal(pausedProgress.remainingSeconds, 1200);

  const resumed = SessionStateMachine.resume(paused, startMs + 900_000);
  assert.equal(resumed.status, 'RUNNING');
  assert.equal(resumed.accumulatedPausedMs, 600_000);

  const resumedProgress = calculateTimerProgress(resumed, startMs + 1_200_000);
  assert.equal(resumedProgress.elapsedSeconds, 600);
  assert.equal(resumedProgress.remainingSeconds, 900);
});

test('4. Session state machine validates transitions and rejects invalid transitions', () => {
  assert.equal(SessionStateMachine.canTransition('IDLE', 'RUNNING'), true);
  assert.equal(SessionStateMachine.canTransition('IDLE', 'PAUSED'), false);
  assert.equal(SessionStateMachine.canTransition('RUNNING', 'PAUSED'), true);
  assert.equal(SessionStateMachine.canTransition('PAUSED', 'RUNNING'), true);
  assert.equal(SessionStateMachine.canTransition('PAUSED', 'PAUSED'), false);
  assert.equal(SessionStateMachine.canTransition('RUNNING', 'ENDED_EARLY'), true);
  assert.throws(() =>
    SessionStateMachine.pause(
      {
        sessionId: 'invalid',
        sessionType: 'FOCUS',
        plannedDurationSeconds: 900,
        startTimestampMs: 0,
        pauseStartedTimestampMs: null,
        accumulatedPausedMs: 0,
        status: 'IDLE',
        subjectId: null,
        taskId: null,
      },
      1000,
    ),
  );
});

test('5. Unfinished session persists and recovers after app restart', () => {
  const storage = new MemoryStorage();
  const repo1 = new FocusLocalRepository(storage);
  const startMs = 1_700_000_000_000;

  repo1.saveActiveTimer({
    sessionId: 'recover-1',
    sessionType: 'FOCUS',
    plannedDurationSeconds: 1500,
    startTimestampMs: startMs,
    pauseStartedTimestampMs: null,
    accumulatedPausedMs: 0,
    status: 'RUNNING',
    subjectId: null,
    taskId: null,
  });

  // Simulate fresh repository instance after app restart
  const repo2 = new FocusLocalRepository(storage);
  const recovered = repo2.load().activeTimer;
  assert.ok(recovered !== null);
  assert.equal(recovered.sessionId, 'recover-1');

  const progressAfterRestart = calculateTimerProgress(recovered, startMs + 600_000);
  assert.equal(progressAfterRestart.elapsedSeconds, 600);
  assert.equal(progressAfterRestart.remainingSeconds, 900);
});

test('6 & 7 & 14. Completed vs early-ended recording and duplicate session prevention', () => {
  const storage = new MemoryStorage();
  const repo = new FocusLocalRepository(storage);
  const startMs = 1_700_000_000_000;

  const snap: ActiveTimerSnapshot = {
    sessionId: 'dup-check-1',
    sessionType: 'FOCUS',
    plannedDurationSeconds: 1500,
    startTimestampMs: startMs,
    pauseStartedTimestampMs: null,
    accumulatedPausedMs: 0,
    status: 'RUNNING',
    subjectId: null,
    taskId: null,
  };

  repo.recordSessionCompletion(snap, 'COMPLETED', 1500, startMs + 1_500_000);
  // Attempting to record the same session ID again must not duplicate it
  repo.recordSessionCompletion(snap, 'COMPLETED', 1500, startMs + 1_505_000);

  const earlySnap: ActiveTimerSnapshot = {
    ...snap,
    sessionId: 'early-1',
  };
  repo.recordSessionCompletion(earlySnap, 'ENDED_EARLY', 420, startMs + 420_000);

  const db = repo.load();
  assert.equal(db.sessions.length, 2);
  assert.equal(db.activeTimer, null);

  const today = getLocalDateString(startMs + 1_500_000);
  const daily = calculateDailyStats(db.sessions, 120, today);
  assert.equal(daily.completedFocusMinutes, 25);
  assert.equal(daily.completedSessions, 1);
  assert.equal(daily.earlyEndedSessions, 1);
});

test('11. Break time is excluded from focus daily and weekly statistics', () => {
  const today = '2026-11-24';
  const sessions: FocusSessionRecord[] = [
    {
      id: 'f1',
      plannedDurationSeconds: 2700,
      actualDurationSeconds: 2700,
      startTimestamp: 1,
      completionTimestamp: 2,
      pausedDurationSeconds: 0,
      sessionStatus: 'COMPLETED',
      sessionType: 'FOCUS',
      localDate: today,
      subjectId: null,
      taskId: null,
      createdAt: 1,
    },
    {
      id: 'b1',
      plannedDurationSeconds: 900,
      actualDurationSeconds: 900,
      startTimestamp: 3,
      completionTimestamp: 4,
      pausedDurationSeconds: 0,
      sessionStatus: 'COMPLETED',
      sessionType: 'BREAK',
      localDate: today,
      subjectId: null,
      taskId: null,
      createdAt: 3,
    },
  ];

  const daily = calculateDailyStats(sessions, 120, today);
  assert.equal(daily.completedFocusMinutes, 45);
  assert.equal(daily.remainingMinutes, 75);
  assert.equal(daily.completionPercentage, 37.5);

  const weekly = calculateWeeklyStats(sessions, today, 'en');
  assert.equal(weekly.totalWeeklyFocusMinutes, 45);
  assert.equal(weekly.completedSessions, 1);
});

test('12 & 13. Daily progress resets on next local calendar day and weekly stats aggregate 7 days', () => {
  const day1 = '2026-11-23';
  const day2 = '2026-11-24';
  const outOfRange = '2026-11-10';

  const sessions: FocusSessionRecord[] = [
    {
      id: 's-old',
      plannedDurationSeconds: 3600,
      actualDurationSeconds: 3600,
      startTimestamp: 1,
      completionTimestamp: 2,
      pausedDurationSeconds: 0,
      sessionStatus: 'COMPLETED',
      sessionType: 'FOCUS',
      localDate: outOfRange,
      subjectId: null,
      taskId: null,
      createdAt: 1,
    },
    {
      id: 's-day1',
      plannedDurationSeconds: 1800,
      actualDurationSeconds: 1800,
      startTimestamp: 3,
      completionTimestamp: 4,
      pausedDurationSeconds: 0,
      sessionStatus: 'COMPLETED',
      sessionType: 'FOCUS',
      localDate: day1,
      subjectId: null,
      taskId: null,
      createdAt: 3,
    },
  ];

  const day2Daily = calculateDailyStats(sessions, 60, day2);
  assert.equal(day2Daily.completedFocusMinutes, 0);
  assert.equal(day2Daily.remainingMinutes, 60);

  const weekly = calculateWeeklyStats(sessions, day2, 'en');
  assert.equal(weekly.totalWeeklyFocusMinutes, 30);
  assert.equal(weekly.completedSessions, 1);
  assert.equal(weekly.averageSessionMinutes, 30);
  assert.equal(weekly.dailyBreakdown.length, 7);
});

test('15. Repository creates subjects & tasks offline and links session completion', () => {
  const storage = new MemoryStorage();
  const repo = new FocusLocalRepository(storage);
  const { createdTask } = repo.createQuickTask('Solve Calculus Integrals', 'Mathematics', 45);

  assert.equal(createdTask.title, 'Solve Calculus Integrals');
  assert.equal(createdTask.status, 'PENDING');

  const nowMs = Date.now();
  repo.recordSessionCompletion(
    {
      sessionId: 'linked-1',
      sessionType: 'FOCUS',
      plannedDurationSeconds: 2700,
      startTimestampMs: nowMs - 2_700_000,
      pauseStartedTimestampMs: null,
      accumulatedPausedMs: 0,
      status: 'RUNNING',
      subjectId: createdTask.subjectId,
      taskId: createdTask.id,
    },
    'COMPLETED',
    2700,
    nowMs,
  );

  const reloaded = new FocusLocalRepository(storage).load();
  assert.equal(reloaded.subjects.length, 1);
  assert.equal(reloaded.subjects[0].subjectName, 'Mathematics');
  assert.equal(reloaded.tasks[0].status, 'IN_PROGRESS');
});
