import assert from 'node:assert/strict';
import test from 'node:test';
import { FocusLocalRepository, type KeyValueStorage } from '../data/focusStore.ts';
import type { ActiveTimerSnapshot, FocusSessionRecord } from '../types/focusly.ts';
import {
  generateAdaptiveRecommendation,
  resolveDistractionBlockerMode,
  suggestRecoveryRetryMinutes,
} from '../utils/adaptiveFocusEngine.ts';
import { calculateDailyStats } from '../utils/timerEngine.ts';

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

function buildFocusRecord(
  id: string,
  plannedMinutes: number,
  actualMinutes: number,
  status: 'COMPLETED' | 'ENDED_EARLY',
  startTimestamp: number,
  type: 'FOCUS' | 'BREAK' = 'FOCUS',
): FocusSessionRecord {
  return {
    id,
    plannedDurationSeconds: plannedMinutes * 60,
    actualDurationSeconds: actualMinutes * 60,
    startTimestamp,
    completionTimestamp: startTimestamp + actualMinutes * 60_000,
    pausedDurationSeconds: 0,
    sessionStatus: status,
    sessionType: type,
    localDate: '2026-11-24',
    subjectId: null,
    taskId: null,
    createdAt: startTimestamp,
  };
}

test('Adaptive Focus Engine: no history & < 3 focus sessions return insufficient-data baseline', () => {
  const emptyRec = generateAdaptiveRecommendation([], 60);
  assert.equal(emptyRec.hasEnoughData, false);
  assert.equal(emptyRec.confidence, 'INSUFFICIENT_DATA');
  assert.equal(emptyRec.reasonType, 'INSUFFICIENT_DATA');

  const fewSessions = [
    buildFocusRecord('f1', 25, 25, 'COMPLETED', 1_700_000_010_000),
    buildFocusRecord('f2', 25, 25, 'COMPLETED', 1_700_000_020_000),
    buildFocusRecord('b1', 5, 5, 'COMPLETED', 1_700_000_030_000, 'BREAK'),
  ];
  const underMinRec = generateAdaptiveRecommendation(fewSessions, 120);
  assert.equal(underMinRec.hasEnoughData, false);
  assert.equal(underMinRec.basedOnSessionCount, 2);
});

test('Adaptive Focus Engine: completing 25m while ending 60m early recommends 25m', () => {
  const base = 1_700_000_000_000;
  const sessions = [
    buildFocusRecord('1', 25, 25, 'COMPLETED', base + 50_000),
    buildFocusRecord('2', 25, 25, 'COMPLETED', base + 40_000),
    buildFocusRecord('3', 60, 20, 'ENDED_EARLY', base + 30_000),
    buildFocusRecord('4', 25, 25, 'COMPLETED', base + 20_000),
    buildFocusRecord('5', 60, 15, 'ENDED_EARLY', base + 10_000),
  ];

  const rec = generateAdaptiveRecommendation(sessions, 120);
  assert.equal(rec.hasEnoughData, true);
  assert.equal(rec.recommendedMinutes, 25);
  assert.equal(rec.reasonType, 'AVOID_LONG_DROPOFF');
  assert.ok(rec.reasonText.includes('25-minute'));
});

test('Adaptive Focus Engine: consistent 45m completion proposes 50m step-up without forcing', () => {
  const base = 1_700_000_000_000;
  const sessions = [
    buildFocusRecord('1', 45, 45, 'COMPLETED', base + 40_000),
    buildFocusRecord('2', 45, 45, 'COMPLETED', base + 30_000),
    buildFocusRecord('3', 45, 45, 'COMPLETED', base + 20_000),
    buildFocusRecord('4', 45, 45, 'COMPLETED', base + 10_000),
  ];

  const rec = generateAdaptiveRecommendation(sessions, 120);
  assert.equal(rec.hasEnoughData, true);
  assert.equal(rec.recommendedMinutes, 50);
  assert.equal(rec.reasonType, 'CONSISTENT_STEP_UP');
  assert.ok(rec.reasonText.includes('45-minute'));
  assert.ok(rec.reasonText.includes('50 minutes'));
});

test('Adaptive Focus Engine: recent consecutive early endings recommend shorter momentum block', () => {
  const base = 1_700_000_000_000;
  const sessions = [
    buildFocusRecord('1', 45, 10, 'ENDED_EARLY', base + 40_000),
    buildFocusRecord('2', 45, 12, 'ENDED_EARLY', base + 30_000),
    buildFocusRecord('3', 25, 25, 'COMPLETED', base + 20_000),
    buildFocusRecord('4', 25, 25, 'COMPLETED', base + 10_000),
  ];

  const rec = generateAdaptiveRecommendation(sessions, 90);
  assert.equal(rec.hasEnoughData, true);
  assert.equal(rec.reasonType, 'RECENT_STRUGGLE');
  assert.ok(rec.recommendedMinutes >= 10 && rec.recommendedMinutes <= 20);
  assert.equal(rec.reasonText, 'Start with a shorter session and build your momentum.');
});

test('Adaptive Focus Engine: duplicate session IDs are deduplicated', () => {
  const dup = buildFocusRecord('dup-1', 25, 25, 'COMPLETED', 1_700_000_000_000);
  const rec = generateAdaptiveRecommendation([dup, dup, dup], 60);
  assert.equal(rec.hasEnoughData, false);
  assert.equal(rec.basedOnSessionCount, 1);
});

test('Distraction Blocker: mode transitions gracefully across permission denied/granted/revoked and disabled', () => {
  const repo = new FocusLocalRepository(new MemoryStorage());

  // Sync from onboarding with selected apps -> enabled, DENIED permission -> CHECKLIST_ONLY
  let db = repo.syncBlockerFromOnboarding(['com.instagram.android'], false);
  assert.equal(db.distractionBlocker.isEnabled, true);
  assert.equal(resolveDistractionBlockerMode(db.distractionBlocker), 'CHECKLIST_ONLY');

  // Grant permission -> ACTIVE_DETECTION
  db = repo.setBlockerPermissionStatus('GRANTED', 1_700_000_000_000);
  assert.equal(resolveDistractionBlockerMode(db.distractionBlocker), 'ACTIVE_DETECTION');

  // Revoke permission -> falls back safely to CHECKLIST_ONLY
  db = repo.setBlockerPermissionStatus('DENIED', 1_700_000_005_000);
  assert.equal(resolveDistractionBlockerMode(db.distractionBlocker), 'CHECKLIST_ONLY');

  // Disable blocker -> DISABLED even if permission is granted
  repo.setBlockerPermissionStatus('GRANTED', 1_700_000_010_000);
  db = repo.setBlockerEnabled(false);
  assert.equal(resolveDistractionBlockerMode(db.distractionBlocker), 'DISABLED');
});

test('Focus Timer operates independently when Distraction Blocker is disabled or permission denied', () => {
  const repo = new FocusLocalRepository(new MemoryStorage());
  repo.syncBlockerFromOnboarding([], true);
  repo.setBlockerPermissionStatus('DENIED');

  const snapshot: ActiveTimerSnapshot = {
    sessionId: 'indep-1',
    sessionType: 'FOCUS',
    plannedDurationSeconds: 1500,
    startTimestampMs: 1_700_000_000_000,
    pauseStartedTimestampMs: null,
    accumulatedPausedMs: 0,
    status: 'RUNNING',
    subjectId: null,
    taskId: null,
  };
  repo.saveActiveTimer(snapshot);
  const { db, recordedSession } = repo.recordSessionCompletion(
    snapshot,
    'COMPLETED',
    1500,
    1_700_001_500_000,
  );
  assert.equal(recordedSession.sessionStatus, 'COMPLETED');
  assert.equal(db.sessions.length, 1);
});

test('Focus Recovery: early-ended session triggers supportive shorter recovery and clears on completion or dismiss', () => {
  assert.equal(suggestRecoveryRetryMinutes(60, 10), 15);
  assert.equal(suggestRecoveryRetryMinutes(25, 5), 10);

  const repo = new FocusLocalRepository(new MemoryStorage());
  const earlySnapshot: ActiveTimerSnapshot = {
    sessionId: 'early-1',
    sessionType: 'FOCUS',
    plannedDurationSeconds: 45 * 60,
    startTimestampMs: 1_700_000_000_000,
    pauseStartedTimestampMs: null,
    accumulatedPausedMs: 0,
    status: 'RUNNING',
    subjectId: null,
    taskId: null,
  };
  repo.saveActiveTimer(earlySnapshot);
  const afterEarly = repo.recordSessionCompletion(
    earlySnapshot,
    'ENDED_EARLY',
    600,
    1_700_000_600_000,
  ).db;

  assert.ok(afterEarly.recoveryState);
  assert.equal(afterEarly.recoveryState?.isActive, true);
  assert.equal(afterEarly.recoveryState?.suggestedRetryMinutes, 15);

  // Completing a shorter recovery session clears recoveryState and preserves earned stats
  const retrySnapshot: ActiveTimerSnapshot = {
    sessionId: 'retry-1',
    sessionType: 'FOCUS',
    plannedDurationSeconds: 15 * 60,
    startTimestampMs: 1_700_001_000_000,
    pauseStartedTimestampMs: null,
    accumulatedPausedMs: 0,
    status: 'RUNNING',
    subjectId: null,
    taskId: null,
  };
  repo.saveActiveTimer(retrySnapshot);
  const afterRetry = repo.recordSessionCompletion(
    retrySnapshot,
    'COMPLETED',
    900,
    1_700_001_900_000,
  ).db;

  assert.equal(afterRetry.recoveryState, null);
  const stats = calculateDailyStats(
    afterRetry.sessions,
    60,
    afterRetry.sessions[0].localDate,
  );
  assert.equal(stats.completedSessions, 1);
  assert.equal(stats.earlyEndedSessions, 1);
  assert.equal(stats.completedFocusMinutes, 15);
});
