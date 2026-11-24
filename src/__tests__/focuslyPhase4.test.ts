import assert from 'node:assert/strict';
import test from 'node:test';
import { FocusLocalRepository, type KeyValueStorage } from '../data/focusStore.ts';
import type { ActiveTimerSnapshot, FocusSessionRecord } from '../types/focusly.ts';
import {
  calculateProgressAnalytics,
  filterAndSortTasks,
  findOverdueUnfinishedTasks,
} from '../utils/studyAnalytics.ts';

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

function buildSession(
  id: string,
  localDate: string,
  plannedMin: number,
  actualMin: number,
  status: 'COMPLETED' | 'ENDED_EARLY',
  subjectId: string | null = null,
  type: 'FOCUS' | 'BREAK' = 'FOCUS',
): FocusSessionRecord {
  return {
    id,
    plannedDurationSeconds: plannedMin * 60,
    actualDurationSeconds: actualMin * 60,
    startTimestamp: 1_700_000_000_000,
    completionTimestamp: 1_700_000_000_000 + actualMin * 60_000,
    pausedDurationSeconds: 0,
    sessionStatus: status,
    sessionType: type,
    localDate,
    subjectId,
    taskId: null,
    createdAt: 1_700_000_000_000,
  };
}

test('Study Planner: subject upsert deduplicates case-insensitively and preserves existing ID', () => {
  const repo = new FocusLocalRepository(new MemoryStorage());
  const first = repo.upsertSubject('Mathematics', '#3B82F6');
  const second = repo.upsertSubject('  mathematics  ', '#22D3EE');

  assert.equal(second.db.subjects.length, 1);
  assert.equal(second.subject.id, first.subject.id);
  assert.equal(second.subject.subjectColor, '#22D3EE');

  // Deleting subject unlinks tasks cleanly rather than deleting tasks
  const createdTask = repo.createTask({
    title: 'Linear Algebra Sheet',
    description: 'Eigenvectors',
    subjectId: first.subject.id,
    plannedDurationMinutes: 45,
    priority: 'HIGH',
    scheduledDate: '2026-11-24',
  });
  assert.equal(createdTask.createdTask.subjectId, first.subject.id);

  const afterDelete = repo.deleteSubject(first.subject.id);
  assert.equal(afterDelete.subjects.length, 0);
  assert.equal(afterDelete.tasks[0].subjectId, null);
});

test('Study Planner: schedule filtering, priority sorting, and overdue rescheduling', () => {
  const repo = new FocusLocalRepository(new MemoryStorage());
  const today = '2026-11-24';

  repo.createTask({
    title: 'Yesterday Overdue',
    description: '',
    subjectId: null,
    plannedDurationMinutes: 30,
    priority: 'LOW',
    scheduledDate: '2026-11-22',
  });
  const completedOverdue = repo.createTask({
    title: 'Yesterday Done',
    description: '',
    subjectId: null,
    plannedDurationMinutes: 30,
    priority: 'HIGH',
    scheduledDate: '2026-11-22',
  }).createdTask;
  repo.updateTaskStatus(completedOverdue.id, 'COMPLETED');

  repo.createTask({
    title: 'Today High Priority',
    description: '',
    subjectId: null,
    plannedDurationMinutes: 45,
    priority: 'HIGH',
    scheduledDate: today,
  });
  repo.createTask({
    title: 'Tomorrow Task',
    description: '',
    subjectId: null,
    plannedDurationMinutes: 25,
    priority: 'MEDIUM',
    scheduledDate: '2026-11-25',
  });

  const dbBefore = repo.load();
  const overdue = findOverdueUnfinishedTasks(dbBefore.tasks, today);
  assert.equal(overdue.length, 1);
  assert.equal(overdue[0].title, 'Yesterday Overdue');

  const { db: dbAfterMove, movedCount } = repo.rescheduleUnfinishedTasksToToday(today);
  assert.equal(movedCount, 1);
  assert.equal(findOverdueUnfinishedTasks(dbAfterMove.tasks, today).length, 0);

  const todayTasks = filterAndSortTasks(dbAfterMove.tasks, 'TODAY', null, today);
  assert.equal(todayTasks.length, 2);
  assert.equal(todayTasks[0].title, 'Today High Priority');
  assert.equal(todayTasks[1].title, 'Yesterday Overdue');

  const upcomingTasks = filterAndSortTasks(dbAfterMove.tasks, 'UPCOMING', null, today);
  assert.equal(upcomingTasks.length, 1);
  assert.equal(upcomingTasks[0].title, 'Tomorrow Task');
});

test('Study Planner: completing a focus session does not overwrite COMPLETED task back to IN_PROGRESS', () => {
  const repo = new FocusLocalRepository(new MemoryStorage());
  const task = repo.createTask({
    title: 'Revision Chapter 4',
    description: '',
    subjectId: null,
    plannedDurationMinutes: 25,
    priority: 'MEDIUM',
    scheduledDate: '2026-11-24',
  }).createdTask;

  repo.updateTaskStatus(task.id, 'COMPLETED');
  const snapshot: ActiveTimerSnapshot = {
    sessionId: 'sess-completed-task',
    sessionType: 'FOCUS',
    plannedDurationSeconds: 1500,
    startTimestampMs: 1_700_000_000_000,
    pauseStartedTimestampMs: null,
    accumulatedPausedMs: 0,
    status: 'RUNNING',
    subjectId: null,
    taskId: task.id,
  };
  repo.saveActiveTimer(snapshot);
  const { db } = repo.recordSessionCompletion(snapshot, 'COMPLETED', 1500, 1_700_001_500_000);
  assert.equal(db.tasks.find((t) => t.id === task.id)?.status, 'COMPLETED');
});

test('Progress Analytics: calculates weekly/monthly totals, completion rate, and subject breakdown accurately', () => {
  const repo = new FocusLocalRepository(new MemoryStorage());
  const math = repo.upsertSubject('Mathematics', '#3B82F6').subject;
  const physics = repo.upsertSubject('Physics', '#22C55E').subject;

  const sessions: FocusSessionRecord[] = [
    buildSession('s1', '2026-11-24', 45, 45, 'COMPLETED', math.id),
    buildSession('s2', '2026-11-23', 30, 30, 'COMPLETED', physics.id),
    buildSession('s3', '2026-11-22', 60, 15, 'ENDED_EARLY', math.id),
    buildSession('s4', '2026-11-10', 25, 25, 'COMPLETED', null),
    buildSession('b1', '2026-11-24', 5, 5, 'COMPLETED', null, 'BREAK'),
    // Duplicate ID must not double-count
    buildSession('s1', '2026-11-24', 45, 45, 'COMPLETED', math.id),
  ];

  const weekSummary = calculateProgressAnalytics(
    sessions,
    [math, physics],
    'WEEK',
    '2026-11-24',
    'en',
    'General Study',
  );
  assert.equal(weekSummary.totalFocusMinutes, 75);
  assert.equal(weekSummary.completedSessions, 2);
  assert.equal(weekSummary.earlyEndedSessions, 1);
  assert.equal(weekSummary.averageSessionMinutes, 37);
  assert.equal(weekSummary.activeDaysCount, 2);
  assert.equal(weekSummary.completionRatePercent, 66.7);
  assert.equal(weekSummary.subjectBreakdown[0].subjectName, 'Mathematics');
  assert.equal(weekSummary.subjectBreakdown[0].focusMinutes, 45);
  assert.equal(weekSummary.subjectBreakdown[0].sharePercentage, 60);

  const monthSummary = calculateProgressAnalytics(
    sessions,
    [math, physics],
    'MONTH',
    '2026-11-24',
    'en',
    'General Study',
  );
  assert.equal(monthSummary.totalFocusMinutes, 100);
  assert.equal(monthSummary.completedSessions, 3);
  assert.equal(monthSummary.completionRatePercent, 75);
  assert.equal(monthSummary.subjectBreakdown.length, 3);
  assert.equal(monthSummary.subjectBreakdown[2].subjectName, 'General Study');
});
