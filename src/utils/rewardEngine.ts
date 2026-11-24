import type {
  FocusSessionRecord,
  GardenGrowthStage,
  SessionStatus,
  SessionType,
} from '../types/focusly';
import { calculateDailyStats, getLocalDateString, shiftLocalDateString } from './timerEngine';

export interface AchievementDefinition {
  id: string;
  title: string;
  description: string;
  coinBonus: number;
}

export interface StoreCatalogDefinition {
  id: string;
  title: string;
  description: string;
  costCoins: number;
  accentColor: string;
}

export const DAILY_TARGET_BONUS_COINS = 10;

export const ACHIEVEMENTS_CATALOG: AchievementDefinition[] = [
  {
    id: 'first_focus',
    title: 'First Focus',
    description: 'Complete your first focus session',
    coinBonus: 15,
  },
  {
    id: 'sixty_minutes',
    title: 'First Hour',
    description: 'Accumulate 60 completed focus minutes',
    coinBonus: 20,
  },
  {
    id: 'five_sessions',
    title: 'Momentum Builder',
    description: 'Complete 5 focus sessions',
    coinBonus: 25,
  },
  {
    id: 'daily_target',
    title: 'Daily Target Met',
    description: 'Reach 100% of your daily focus goal',
    coinBonus: 25,
  },
  {
    id: 'three_day_streak',
    title: '3-Day Streak',
    description: 'Complete focus sessions on 3 consecutive days',
    coinBonus: 40,
  },
  {
    id: 'ten_sessions',
    title: 'Steady Scholar',
    description: 'Complete 10 focus sessions',
    coinBonus: 50,
  },
];

export const GARDEN_STORE_CATALOG: StoreCatalogDefinition[] = [
  {
    id: 'sunlit_sprout',
    title: 'Sunlit Sprout',
    description: 'A starter desk seedling that grows with every completed session.',
    costCoins: 0,
    accentColor: '#22C55E',
  },
  {
    id: 'electric_lotus',
    title: 'Electric Lotus',
    description: 'Calm deep-navy lotus symbolizing steady concentration.',
    costCoins: 25,
    accentColor: '#3B82F6',
  },
  {
    id: 'cedar_fern',
    title: 'Calm Cedar Fern',
    description: 'Layered evergreen fronds built for long revision blocks.',
    costCoins: 40,
    accentColor: '#22D3EE',
  },
  {
    id: 'lantern_moss',
    title: 'Desk Lantern Moss',
    description: 'Warm amber study companion for evening problem solving.',
    costCoins: 65,
    accentColor: '#F59E0B',
  },
  {
    id: 'night_bonsai',
    title: 'Night-Sky Bonsai',
    description: 'Patient sculpted bonsai earned through disciplined study habits.',
    costCoins: 95,
    accentColor: '#8B5CF6',
  },
  {
    id: 'scholars_ginkgo',
    title: "Scholar's Ginkgo",
    description: 'Golden canopy celebrating lasting academic consistency.',
    costCoins: 130,
    accentColor: '#EAB308',
  },
];

export function calculateSessionCoins(
  plannedDurationSeconds: number,
  actualDurationSeconds: number,
  sessionType: SessionType,
  sessionStatus: SessionStatus,
): number {
  if (sessionType !== 'FOCUS' || sessionStatus !== 'COMPLETED') {
    return 0;
  }
  const validSeconds = Math.min(
    Math.max(0, actualDurationSeconds),
    Math.max(0, plannedDurationSeconds),
  );
  const minutes = Math.floor(validSeconds / 60);
  if (minutes <= 0) return 0;
  return Math.max(5, minutes);
}

export function computeGrowthStage(completedFocusSessionsCount: number): GardenGrowthStage {
  if (completedFocusSessionsCount >= 5) return 'BLOOMING';
  if (completedFocusSessionsCount >= 2) return 'SPROUTING';
  return 'SEEDLING';
}

function deduplicateCompletedFocus(sessions: FocusSessionRecord[]): FocusSessionRecord[] {
  const seen = new Set<string>();
  const result: FocusSessionRecord[] = [];
  for (const s of sessions) {
    if (!seen.has(s.id) && s.sessionType === 'FOCUS' && s.sessionStatus === 'COMPLETED') {
      seen.add(s.id);
      result.push(s);
    }
  }
  return result;
}

export function computeStreaks(
  sessions: FocusSessionRecord[],
  todayDate: string = getLocalDateString(),
): { currentStreak: number; longestStreak: number } {
  const completedDates = Array.from(
    new Set(
      deduplicateCompletedFocus(sessions)
        .map((s) => s.localDate)
        .filter((d) => /^\d{4}-\d{2}-\d{2}$/.test(d)),
    ),
  ).sort();

  if (completedDates.length === 0) {
    return { currentStreak: 0, longestStreak: 0 };
  }

  let longest = 0;
  let run = 0;
  let prevDate: string | null = null;
  for (const date of completedDates) {
    if (prevDate && shiftLocalDateString(prevDate, 1) === date) {
      run += 1;
    } else {
      run = 1;
    }
    if (run > longest) longest = run;
    prevDate = date;
  }

  const dateSet = new Set(completedDates);
  const yesterday = shiftLocalDateString(todayDate, -1);
  let cursor: string | null = dateSet.has(todayDate)
    ? todayDate
    : dateSet.has(yesterday)
      ? yesterday
      : null;

  let current = 0;
  while (cursor && dateSet.has(cursor)) {
    current += 1;
    cursor = shiftLocalDateString(cursor, -1);
  }

  return { currentStreak: current, longestStreak: longest };
}

export function evaluateNewlyUnlockedAchievements(
  sessions: FocusSessionRecord[],
  dailyTargetMinutes: number,
  todayDate: string,
  alreadyUnlockedIds: Set<string>,
): AchievementDefinition[] {
  const completed = deduplicateCompletedFocus(sessions);
  const completedCount = completed.length;
  const totalMinutes = completed.reduce((acc, s) => {
    const validSecs = Math.min(
      Math.max(0, s.actualDurationSeconds),
      Math.max(0, s.plannedDurationSeconds),
    );
    return acc + Math.floor(validSecs / 60);
  }, 0);

  const dailyStats = calculateDailyStats(completed, dailyTargetMinutes, todayDate);
  const { longestStreak } = computeStreaks(completed, todayDate);

  const candidates = new Set<string>();
  if (completedCount >= 1) candidates.add('first_focus');
  if (totalMinutes >= 60) candidates.add('sixty_minutes');
  if (completedCount >= 5) candidates.add('five_sessions');
  if (dailyStats.completedFocusMinutes >= dailyStats.targetMinutes) candidates.add('daily_target');
  if (longestStreak >= 3) candidates.add('three_day_streak');
  if (completedCount >= 10) candidates.add('ten_sessions');

  return ACHIEVEMENTS_CATALOG.filter(
    (def) => candidates.has(def.id) && !alreadyUnlockedIds.has(def.id),
  );
}
