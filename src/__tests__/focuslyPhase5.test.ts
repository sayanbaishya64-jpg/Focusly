import assert from 'node:assert/strict';
import test from 'node:test';
import { FocusLocalRepository, type KeyValueStorage } from '../data/focusStore.ts';
import type { ActiveTimerSnapshot, FocusSessionRecord } from '../types/focusly.ts';
import {
  calculateSessionCoins,
  computeGrowthStage,
  computeStreaks,
  evaluateNewlyUnlockedAchievements,
} from '../utils/rewardEngine.ts';
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

function makeSnapshot(
  id: string,
  minutes: number,
  type: 'FOCUS' | 'BREAK' = 'FOCUS',
): ActiveTimerSnapshot {
  return {
    sessionId: id,
    sessionType: type,
    plannedDurationSeconds: minutes * 60,
    startTimestampMs: 1_700_000_000_000,
    pauseStartedTimestampMs: null,
    accumulatedPausedMs: 0,
    status: 'RUNNING',
    subjectId: null,
    taskId: null,
  };
}

function makeSessionRecord(
  id: string,
  localDate: string,
  minutes: number,
  status: 'COMPLETED' | 'ENDED_EARLY' = 'COMPLETED',
  type: 'FOCUS' | 'BREAK' = 'FOCUS',
): FocusSessionRecord {
  return {
    id,
    plannedDurationSeconds: minutes * 60,
    actualDurationSeconds: minutes * 60,
    startTimestamp: 1_700_000_000_000,
    completionTimestamp: 1_700_000_000_000 + minutes * 60_000,
    pausedDurationSeconds: 0,
    sessionStatus: status,
    sessionType: type,
    localDate,
    subjectId: null,
    taskId: null,
    createdAt: 1_700_000_000_000,
  };
}

test('RewardEngine: only completed FOCUS sessions earn coins; BREAK and ENDED_EARLY earn 0', () => {
  assert.equal(calculateSessionCoins(1500, 1500, 'FOCUS', 'COMPLETED'), 25);
  assert.equal(calculateSessionCoins(300, 300, 'FOCUS', 'COMPLETED'), 5);
  assert.equal(calculateSessionCoins(1500, 900, 'FOCUS', 'ENDED_EARLY'), 0);
  assert.equal(calculateSessionCoins(600, 600, 'BREAK', 'COMPLETED'), 0);
});

test('RewardEngine: streaks and achievement milestones ignore early-ended and break sessions', () => {
  const sessions: FocusSessionRecord[] = [
    makeSessionRecord('s1', '2026-11-22', 25),
    makeSessionRecord('s2', '2026-11-23', 25),
    makeSessionRecord('s3', '2026-11-24', 25),
    makeSessionRecord('s4', '2026-11-24', 35),
    makeSessionRecord('s5', '2026-11-24', 25, 'ENDED_EARLY'),
    makeSessionRecord('b1', '2026-11-24', 10, 'COMPLETED', 'BREAK'),
  ];

  const { currentStreak, longestStreak } = computeStreaks(sessions, '2026-11-24');
  assert.equal(currentStreak, 3);
  assert.equal(longestStreak, 3);

  const unlocked = evaluateNewlyUnlockedAchievements(
    sessions,
    60,
    '2026-11-24',
    new Set(),
  ).map((a) => a.id);

  assert.ok(unlocked.includes('first_focus'));
  assert.ok(unlocked.includes('sixty_minutes'));
  assert.ok(unlocked.includes('daily_target'));
  assert.ok(unlocked.includes('three_day_streak'));
  assert.ok(!unlocked.includes('five_sessions'));
});

test('RewardEngine: Virtual Garden growth stage advances strictly at 2 and 5 completed sessions', () => {
  assert.equal(computeGrowthStage(0), 'SEEDLING');
  assert.equal(computeGrowthStage(1), 'SEEDLING');
  assert.equal(computeGrowthStage(2), 'SPROUTING');
  assert.equal(computeGrowthStage(4), 'SPROUTING');
  assert.equal(computeGrowthStage(5), 'BLOOMING');
});

test('FocusLocalRepository: atomic rewards, idempotency, daily bonus, and store balance validation', () => {
  const repo = new FocusLocalRepository(new MemoryStorage());
  const completionTs = Date.UTC(2026, 10, 24, 10, 0, 0);
  const localDate = getLocalDateString(completionTs);

  // Break session and early-ended focus session must award 0 coins & 0 achievements
  repo.recordSessionCompletion(makeSnapshot('brk-1', 10, 'BREAK'), 'COMPLETED', 600, completionTs, 60);
  repo.recordSessionCompletion(makeSnapshot('early-1', 25, 'FOCUS'), 'ENDED_EARLY', 600, completionTs + 1000, 60);

  let preview = repo.getRewardPreview();
  assert.equal(preview.focusCoins, 0);
  assert.equal(preview.unlockedAchievements, 0);

  // Store purchase with 0 coins is rejected cleanly
  const failedBuy = repo.purchaseStoreItem('electric_lotus', localDate);
  assert.equal(failedBuy.result, 'INSUFFICIENT_COINS');

  // Complete a 60m focus session against a 60m daily goal:
  // 60 session coins + 10 daily target bonus + 15 (first_focus) + 20 (sixty_minutes) + 25 (daily_target) = 130
  const snap60 = makeSnapshot('focus-60', 60, 'FOCUS');
  repo.recordSessionCompletion(snap60, 'COMPLETED', 3600, completionTs + 2000, 60);

  // Re-recording same session ID must not double-credit coins
  repo.recordSessionCompletion(snap60, 'COMPLETED', 3600, completionTs + 3000, 60);

  preview = repo.getRewardPreview();
  assert.equal(preview.focusCoins, 130);
  assert.equal(preview.unlockedAchievements, 3);

  // Purchase Electric Lotus (25 coins) -> balance becomes 105 and item becomes featured
  const buy = repo.purchaseStoreItem('electric_lotus', localDate);
  assert.equal(buy.result, 'SUCCESS');
  assert.equal(repo.getRewardPreview().focusCoins, 105);

  const dashboard = repo.getRewardsDashboard(localDate);
  assert.equal(dashboard.coinBalance, 105);
  assert.equal(dashboard.totalEarnedCoins, 130);
  assert.equal(dashboard.storeItems.find((i) => i.id === 'electric_lotus')?.isFeatured, true);
  assert.equal(dashboard.storeItems.find((i) => i.id === 'sunlit_sprout')?.isUnlocked, true);

  // Duplicate purchase returns ALREADY_UNLOCKED and leaves balance untouched
  const dupBuy = repo.purchaseStoreItem('electric_lotus', localDate);
  assert.equal(dupBuy.result, 'ALREADY_UNLOCKED');
  assert.equal(repo.getRewardPreview().focusCoins, 105);

  // Switch featured companion back to sunlit_sprout
  repo.setFeaturedGardenItem('sunlit_sprout');
  const updatedDash = repo.getRewardsDashboard(localDate);
  assert.equal(updatedDash.storeItems.find((i) => i.id === 'sunlit_sprout')?.isFeatured, true);
  assert.equal(updatedDash.storeItems.find((i) => i.id === 'electric_lotus')?.isFeatured, false);
});
