import type {
  AchievementItem,
  AchievementUnlockRecord,
  ActiveTimerSnapshot,
  BackupImportResult,
  DistractionBlockerSettings,
  DistractionPermissionStatus,
  FocusRecoveryState,
  FocusSessionRecord,
  GardenStoreItem,
  OnboardingPreferences,
  RecommendationStatus,
  RecoveryTriggerReason,
  RewardPreview,
  RewardPurchaseResult,
  RewardTransactionRecord,
  RewardsDashboardState,
  SessionStatus,
  StudyTaskItem,
  SubjectItem,
  TaskPriority,
  TaskStatus,
  UnlockedGardenItemRecord,
} from '../types/focusly';
import { suggestRecoveryRetryMinutes } from '../utils/adaptiveFocusEngine';
import {
  SUPPORTED_BACKUP_SCHEMA_VERSION,
  decodeBackupJson,
  encodeBackupJson,
} from '../utils/backupCodec';
import { MISSIONS_CATALOG } from '../utils/dailyMissions';
import {
  ACHIEVEMENTS_CATALOG,
  DAILY_TARGET_BONUS_COINS,
  GARDEN_STORE_CATALOG,
  calculateSessionCoins,
  computeGrowthStage,
  computeStreaks,
  evaluateNewlyUnlockedAchievements,
} from '../utils/rewardEngine';
import { calculateDailyStats, getLocalDateString } from '../utils/timerEngine';

export interface FocusLocalDatabase {
  studentName: string;
  subjects: SubjectItem[];
  tasks: StudyTaskItem[];
  sessions: FocusSessionRecord[];
  activeTimer: ActiveTimerSnapshot | null;
  recommendationDecisions: Record<string, RecommendationStatus>;
  distractionBlocker: DistractionBlockerSettings;
  recoveryState: FocusRecoveryState | null;
  rewardTransactions: RewardTransactionRecord[];
  unlockedAchievements: AchievementUnlockRecord[];
  unlockedGardenItems: UnlockedGardenItemRecord[];
}

const FOCUS_DB_STORAGE_KEY = 'focusly_phase2_local_db_v1';

export const DEFAULT_BLOCKER_SETTINGS: DistractionBlockerSettings = {
  isEnabled: false,
  selectedPackageNames: [],
  permissionStatus: 'DENIED',
  lastCheckedAt: null,
  initializedFromOnboarding: false,
};

export const EMPTY_LOCAL_DB: FocusLocalDatabase = {
  studentName: '',
  subjects: [],
  tasks: [],
  sessions: [],
  activeTimer: null,
  recommendationDecisions: {},
  distractionBlocker: { ...DEFAULT_BLOCKER_SETTINGS },
  recoveryState: null,
  rewardTransactions: [],
  unlockedAchievements: [],
  unlockedGardenItems: [],
};

export interface KeyValueStorage {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
}

function defaultStorage(): KeyValueStorage | null {
  if (typeof window !== 'undefined' && window.localStorage) {
    return window.localStorage;
  }
  return null;
}

export class FocusLocalRepository {
  private storage: KeyValueStorage | null;

  constructor(storage: KeyValueStorage | null = defaultStorage()) {
    this.storage = storage;
  }

  load(): FocusLocalDatabase {
    if (!this.storage) return { ...EMPTY_LOCAL_DB, distractionBlocker: { ...DEFAULT_BLOCKER_SETTINGS } };
    try {
      const raw = this.storage.getItem(FOCUS_DB_STORAGE_KEY);
      if (!raw) return { ...EMPTY_LOCAL_DB, distractionBlocker: { ...DEFAULT_BLOCKER_SETTINGS } };
      const parsed = JSON.parse(raw) as Partial<FocusLocalDatabase>;
      return {
        studentName: typeof parsed.studentName === 'string' ? parsed.studentName : '',
        subjects: Array.isArray(parsed.subjects) ? parsed.subjects : [],
        tasks: Array.isArray(parsed.tasks) ? parsed.tasks : [],
        sessions: Array.isArray(parsed.sessions) ? parsed.sessions : [],
        activeTimer: parsed.activeTimer ?? null,
        recommendationDecisions:
          parsed.recommendationDecisions && typeof parsed.recommendationDecisions === 'object'
            ? parsed.recommendationDecisions
            : {},
        distractionBlocker: parsed.distractionBlocker
          ? {
              isEnabled: Boolean(parsed.distractionBlocker.isEnabled),
              selectedPackageNames: Array.isArray(parsed.distractionBlocker.selectedPackageNames)
                ? parsed.distractionBlocker.selectedPackageNames
                : [],
              permissionStatus:
                parsed.distractionBlocker.permissionStatus === 'GRANTED' ? 'GRANTED' : 'DENIED',
              lastCheckedAt:
                typeof parsed.distractionBlocker.lastCheckedAt === 'number'
                  ? parsed.distractionBlocker.lastCheckedAt
                  : null,
              initializedFromOnboarding: Boolean(
                parsed.distractionBlocker.initializedFromOnboarding,
              ),
            }
          : { ...DEFAULT_BLOCKER_SETTINGS },
        recoveryState: parsed.recoveryState ?? null,
        rewardTransactions: Array.isArray(parsed.rewardTransactions)
          ? parsed.rewardTransactions
          : [],
        unlockedAchievements: Array.isArray(parsed.unlockedAchievements)
          ? parsed.unlockedAchievements
          : [],
        unlockedGardenItems: Array.isArray(parsed.unlockedGardenItems)
          ? parsed.unlockedGardenItems
          : [],
      };
    } catch {
      return { ...EMPTY_LOCAL_DB, distractionBlocker: { ...DEFAULT_BLOCKER_SETTINGS } };
    }
  }

  private save(db: FocusLocalDatabase): FocusLocalDatabase {
    if (this.storage) {
      try {
        this.storage.setItem(FOCUS_DB_STORAGE_KEY, JSON.stringify(db));
      } catch {
        // Ignore storage errors safely
      }
    }
    return db;
  }

  updateStudentName(name: string): FocusLocalDatabase {
    const db = this.load();
    return this.save({ ...db, studentName: name.trim() });
  }

  upsertSubject(
    subjectName: string,
    subjectColor: string = '#3B82F6',
  ): { db: FocusLocalDatabase; subject: SubjectItem } {
    const db = this.load();
    const now = Date.now();
    const clean = subjectName.trim() || 'General Study';
    const existing = db.subjects.find(
      (s) => s.subjectName.toLowerCase() === clean.toLowerCase(),
    );

    if (existing) {
      const updated: SubjectItem = {
        ...existing,
        subjectName: clean,
        subjectColor,
        updatedAt: now,
      };
      const nextDb = this.save({
        ...db,
        subjects: db.subjects.map((s) => (s.id === existing.id ? updated : s)),
      });
      return { db: nextDb, subject: updated };
    }

    const created: SubjectItem = {
      id: `subj-${now}-${Math.random().toString(36).slice(2, 7)}`,
      subjectName: clean,
      subjectColor,
      createdAt: now,
      updatedAt: now,
    };
    const nextDb = this.save({
      ...db,
      subjects: [...db.subjects, created].sort((a, b) =>
        a.subjectName.localeCompare(b.subjectName),
      ),
    });
    return { db: nextDb, subject: created };
  }

  deleteSubject(subjectId: string): FocusLocalDatabase {
    const db = this.load();
    const now = Date.now();
    return this.save({
      ...db,
      subjects: db.subjects.filter((s) => s.id !== subjectId),
      tasks: db.tasks.map((t) =>
        t.subjectId === subjectId ? { ...t, subjectId: null, updatedAt: now } : t,
      ),
    });
  }

  createQuickTask(
    title: string,
    subjectName: string,
    plannedDurationMinutes: number,
    priority: TaskPriority = 'MEDIUM',
    scheduledDate: string | null = getLocalDateString(),
  ): { db: FocusLocalDatabase; createdTask: StudyTaskItem } {
    const cleanSubject = subjectName.trim();
    let subjectId: string | null = null;
    if (cleanSubject.length > 0) {
      subjectId = this.upsertSubject(cleanSubject, '#3B82F6').subject.id;
    }

    return this.createTask({
      title,
      description: '',
      subjectId,
      plannedDurationMinutes,
      priority,
      scheduledDate,
    });
  }

  createTask(input: {
    title: string;
    description: string;
    subjectId: string | null;
    plannedDurationMinutes: number;
    priority: TaskPriority;
    scheduledDate: string | null;
  }): { db: FocusLocalDatabase; createdTask: StudyTaskItem } {
    const db = this.load();
    const now = Date.now();
    const newTask: StudyTaskItem = {
      id: `task-${now}-${Math.random().toString(36).slice(2, 7)}`,
      title: input.title.trim(),
      description: input.description.trim(),
      subjectId: input.subjectId,
      plannedDurationMinutes: Math.min(480, Math.max(5, input.plannedDurationMinutes)),
      priority: input.priority,
      status: 'PENDING',
      scheduledDate: input.scheduledDate,
      createdAt: now,
      updatedAt: now,
    };
    const nextDb = this.save({
      ...db,
      tasks: [newTask, ...db.tasks],
    });
    return { db: nextDb, createdTask: newTask };
  }

  updateTask(
    taskId: string,
    input: {
      title: string;
      description: string;
      subjectId: string | null;
      plannedDurationMinutes: number;
      priority: TaskPriority;
      scheduledDate: string | null;
    },
  ): FocusLocalDatabase {
    const db = this.load();
    const now = Date.now();
    const trimmed = input.title.trim();
    if (!trimmed) return db;
    const tasks = db.tasks.map((t) =>
      t.id === taskId
        ? {
            ...t,
            title: trimmed,
            description: input.description.trim(),
            subjectId: input.subjectId,
            plannedDurationMinutes: Math.min(480, Math.max(5, input.plannedDurationMinutes)),
            priority: input.priority,
            scheduledDate: input.scheduledDate,
            updatedAt: now,
          }
        : t,
    );
    return this.save({ ...db, tasks });
  }

  updateTaskStatus(taskId: string, status: TaskStatus): FocusLocalDatabase {
    const db = this.load();
    const now = Date.now();
    const tasks = db.tasks.map((t) =>
      t.id === taskId ? { ...t, status, updatedAt: now } : t,
    );
    return this.save({ ...db, tasks });
  }

  rescheduleTask(taskId: string, scheduledDate: string | null): FocusLocalDatabase {
    const db = this.load();
    const now = Date.now();
    const tasks = db.tasks.map((t) =>
      t.id === taskId ? { ...t, scheduledDate, updatedAt: now } : t,
    );
    return this.save({ ...db, tasks });
  }

  rescheduleUnfinishedTasksToToday(
    todayDate: string = getLocalDateString(),
  ): { db: FocusLocalDatabase; movedCount: number } {
    const db = this.load();
    const now = Date.now();
    let movedCount = 0;
    const tasks = db.tasks.map((t) => {
      if (t.status !== 'COMPLETED' && t.scheduledDate && t.scheduledDate < todayDate) {
        movedCount += 1;
        return { ...t, scheduledDate: todayDate, updatedAt: now };
      }
      return t;
    });
    return { db: this.save({ ...db, tasks }), movedCount };
  }

  deleteTask(taskId: string): FocusLocalDatabase {
    const db = this.load();
    return this.save({
      ...db,
      tasks: db.tasks.filter((t) => t.id !== taskId),
    });
  }

  saveActiveTimer(snapshot: ActiveTimerSnapshot): FocusLocalDatabase {
    const db = this.load();
    return this.save({ ...db, activeTimer: snapshot });
  }

  clearActiveTimer(): FocusLocalDatabase {
    const db = this.load();
    return this.save({ ...db, activeTimer: null });
  }

  recordSessionCompletion(
    snapshot: ActiveTimerSnapshot,
    finalStatus: SessionStatus,
    actualDurationSeconds: number,
    completionTimestampMs: number = Date.now(),
    dailyTargetMinutes: number = 60,
  ): { db: FocusLocalDatabase; recordedSession: FocusSessionRecord } {
    const db = this.load();
    const existing = db.sessions.find((s) => s.id === snapshot.sessionId);
    if (existing) {
      const cleared = this.save({ ...db, activeTimer: null });
      return { db: cleared, recordedSession: existing };
    }

    const clampedActual = Math.min(
      Math.max(0, Math.floor(actualDurationSeconds)),
      Math.max(1, snapshot.plannedDurationSeconds),
    );
    const localDate = getLocalDateString(completionTimestampMs);

    const record: FocusSessionRecord = {
      id: snapshot.sessionId,
      plannedDurationSeconds: snapshot.plannedDurationSeconds,
      actualDurationSeconds: clampedActual,
      startTimestamp: snapshot.startTimestampMs,
      completionTimestamp: completionTimestampMs,
      pausedDurationSeconds: Math.floor(Math.max(0, snapshot.accumulatedPausedMs) / 1000),
      sessionStatus: finalStatus,
      sessionType: snapshot.sessionType,
      localDate,
      subjectId: snapshot.subjectId,
      taskId: snapshot.taskId,
      createdAt: snapshot.startTimestampMs,
    };

    const nextSessions = [record, ...db.sessions];
    const updatedTasks =
      finalStatus === 'COMPLETED' && snapshot.sessionType === 'FOCUS' && snapshot.taskId
        ? db.tasks.map((t) =>
            t.id === snapshot.taskId && t.status === 'PENDING'
              ? { ...t, status: 'IN_PROGRESS' as TaskStatus, updatedAt: completionTimestampMs }
              : t,
          )
        : db.tasks;

    let nextRecovery = db.recoveryState;
    if (finalStatus === 'COMPLETED') {
      nextRecovery = null;
    } else if (finalStatus === 'ENDED_EARLY' && snapshot.sessionType === 'FOCUS') {
      const plannedMins = Math.max(1, Math.floor(snapshot.plannedDurationSeconds / 60));
      const actualMins = Math.floor(clampedActual / 60);
      nextRecovery = {
        lastSessionId: snapshot.sessionId,
        triggerReason: 'ENDED_EARLY',
        plannedDurationMinutes: plannedMins,
        actualDurationMinutes: actualMins,
        suggestedRetryMinutes: suggestRecoveryRetryMinutes(plannedMins, actualMins),
        subjectId: snapshot.subjectId,
        taskId: snapshot.taskId,
        isActive: true,
        createdAt: completionTimestampMs,
      };
    }

    const nextTransactions = [...db.rewardTransactions];
    const existingTxIds = new Set(nextTransactions.map((t) => t.id));
    const nextUnlocks = [...db.unlockedAchievements];
    const unlockedIds = new Set(nextUnlocks.map((a) => a.achievementId));

    if (finalStatus === 'COMPLETED' && snapshot.sessionType === 'FOCUS') {
      const sessionCoins = calculateSessionCoins(
        snapshot.plannedDurationSeconds,
        clampedActual,
        snapshot.sessionType,
        finalStatus,
      );
      const sessionTxId = `earn_session_${snapshot.sessionId}`;
      if (sessionCoins > 0 && !existingTxIds.has(sessionTxId)) {
        const mins = Math.max(1, Math.floor(clampedActual / 60));
        existingTxIds.add(sessionTxId);
        nextTransactions.unshift({
          id: sessionTxId,
          type: 'EARN_SESSION',
          amount: sessionCoins,
          title: `${mins}m Focus Session`,
          referenceId: snapshot.sessionId,
          localDate,
          createdAt: completionTimestampMs,
        });
      }

      const dayStats = calculateDailyStats(nextSessions, dailyTargetMinutes, localDate);
      const dailyGoalTxId = `earn_daily_goal_${localDate}`;
      if (
        dayStats.completedFocusMinutes >= dayStats.targetMinutes &&
        !existingTxIds.has(dailyGoalTxId)
      ) {
        existingTxIds.add(dailyGoalTxId);
        nextTransactions.unshift({
          id: dailyGoalTxId,
          type: 'EARN_DAILY_GOAL',
          amount: DAILY_TARGET_BONUS_COINS,
          title: 'Daily Focus Goal Bonus',
          referenceId: localDate,
          localDate,
          createdAt: completionTimestampMs,
        });
      }

      const newlyUnlocked = evaluateNewlyUnlockedAchievements(
        nextSessions,
        dailyTargetMinutes,
        localDate,
        unlockedIds,
      );
      for (const def of newlyUnlocked) {
        unlockedIds.add(def.id);
        nextUnlocks.unshift({
          achievementId: def.id,
          unlockedAt: completionTimestampMs,
        });
        const achTxId = `earn_achievement_${def.id}`;
        if (def.coinBonus > 0 && !existingTxIds.has(achTxId)) {
          existingTxIds.add(achTxId);
          nextTransactions.unshift({
            id: achTxId,
            type: 'EARN_ACHIEVEMENT',
            amount: def.coinBonus,
            title: `Achievement: ${def.title}`,
            referenceId: def.id,
            localDate,
            createdAt: completionTimestampMs,
          });
        }
      }
    }

    const nextDb = this.save({
      ...db,
      sessions: nextSessions,
      tasks: updatedTasks,
      activeTimer: null,
      recoveryState: nextRecovery,
      rewardTransactions: nextTransactions,
      unlockedAchievements: nextUnlocks,
    });
    return { db: nextDb, recordedSession: record };
  }

  getRewardPreview(): RewardPreview {
    const db = this.load();
    const balance = db.rewardTransactions.reduce((sum, tx) => sum + tx.amount, 0);
    return {
      focusCoins: Math.max(0, balance),
      unlockedAchievements: db.unlockedAchievements.length,
    };
  }

  getRewardsDashboard(todayDate: string = getLocalDateString()): RewardsDashboardState {
    const db = this.load();
    const coinBalance = Math.max(
      0,
      db.rewardTransactions.reduce((sum, tx) => sum + tx.amount, 0),
    );
    const totalEarnedCoins = db.rewardTransactions
      .filter((tx) => tx.amount > 0)
      .reduce((sum, tx) => sum + tx.amount, 0);

    const unlockMap = new Map(db.unlockedAchievements.map((u) => [u.achievementId, u]));
    const ownedMap = new Map(db.unlockedGardenItems.map((g) => [g.itemId, g]));

    const completedFocus = db.sessions.filter(
      (s) => s.sessionType === 'FOCUS' && s.sessionStatus === 'COMPLETED',
    );
    const uniqueCompletedIds = new Set(completedFocus.map((s) => s.id));
    const completedFocusSessionsCount = uniqueCompletedIds.size;
    const { currentStreak } = computeStreaks(completedFocus, todayDate);
    const growthStage = computeGrowthStage(completedFocusSessionsCount);

    const achievements: AchievementItem[] = ACHIEVEMENTS_CATALOG.map((def) => {
      const unlock = unlockMap.get(def.id);
      return {
        id: def.id,
        title: def.title,
        description: def.description,
        coinBonus: def.coinBonus,
        isUnlocked: Boolean(unlock),
        unlockedAt: unlock ? unlock.unlockedAt : null,
      };
    });

    const hasAnyFeatured = db.unlockedGardenItems.some((g) => g.isFeatured);
    const storeItems: GardenStoreItem[] = GARDEN_STORE_CATALOG.map((def) => {
      const owned = ownedMap.get(def.id);
      const isStarter = def.costCoins === 0;
      return {
        id: def.id,
        title: def.title,
        description: def.description,
        costCoins: def.costCoins,
        accentColor: def.accentColor,
        isUnlocked: Boolean(owned) || isStarter,
        isFeatured: Boolean(owned?.isFeatured) || (!hasAnyFeatured && def.id === 'sunlit_sprout'),
        growthStage,
      };
    });

    const recentTransactions = [...db.rewardTransactions]
      .sort((a, b) => b.createdAt - a.createdAt)
      .slice(0, 20);

    return {
      coinBalance,
      totalEarnedCoins,
      unlockedAchievementsCount: unlockMap.size,
      totalAchievementsCount: ACHIEVEMENTS_CATALOG.length,
      completedFocusSessionsCount,
      currentStreakDays: currentStreak,
      achievements,
      storeItems,
      recentTransactions,
    };
  }

  purchaseStoreItem(
    itemId: string,
    localDate: string = getLocalDateString(),
  ): { db: FocusLocalDatabase; result: RewardPurchaseResult } {
    const db = this.load();
    const def = GARDEN_STORE_CATALOG.find((item) => item.id === itemId);
    if (!def) return { db, result: 'NOT_FOUND' };

    const alreadyOwned = db.unlockedGardenItems.some((item) => item.itemId === itemId);
    if (alreadyOwned || def.costCoins === 0) {
      return { db, result: 'ALREADY_UNLOCKED' };
    }

    const currentBalance = Math.max(
      0,
      db.rewardTransactions.reduce((sum, tx) => sum + tx.amount, 0),
    );
    if (currentBalance < def.costCoins) {
      return { db, result: 'INSUFFICIENT_COINS' };
    }

    const now = Date.now();
    const nextTransactions: RewardTransactionRecord[] = [
      {
        id: `spend_store_${def.id}`,
        type: 'SPEND_STORE',
        amount: -def.costCoins,
        title: `Unlocked ${def.title}`,
        referenceId: def.id,
        localDate,
        createdAt: now,
      },
      ...db.rewardTransactions,
    ];

    const nextItems: UnlockedGardenItemRecord[] = [
      ...db.unlockedGardenItems.map((g) => ({ ...g, isFeatured: false })),
      {
        itemId: def.id,
        purchasedAt: now,
        isFeatured: true,
      },
    ];

    const nextDb = this.save({
      ...db,
      rewardTransactions: nextTransactions,
      unlockedGardenItems: nextItems,
    });
    return { db: nextDb, result: 'SUCCESS' };
  }

  setFeaturedGardenItem(itemId: string): FocusLocalDatabase {
    const db = this.load();
    const def = GARDEN_STORE_CATALOG.find((item) => item.id === itemId);
    if (!def) return db;

    const existing = db.unlockedGardenItems.find((item) => item.itemId === itemId);
    if (!existing && def.costCoins > 0) return db;

    const cleared = db.unlockedGardenItems.map((item) => ({
      ...item,
      isFeatured: item.itemId === itemId,
    }));
    const nextItems = existing
      ? cleared
      : [
          ...cleared,
          {
            itemId: def.id,
            purchasedAt: Date.now(),
            isFeatured: true,
          },
        ];

    return this.save({
      ...db,
      unlockedGardenItems: nextItems,
    });
  }

  getClaimedMissionIdsForDate(localDate: string = getLocalDateString()): Set<string> {
    const db = this.load();
    const ids = new Set<string>();
    for (const tx of db.rewardTransactions) {
      if (tx.type === 'EARN_MISSION' && tx.localDate === localDate && tx.referenceId) {
        ids.add(tx.referenceId);
      }
    }
    return ids;
  }

  claimDailyMission(
    missionId: string,
    localDate: string = getLocalDateString(),
  ): { db: FocusLocalDatabase; claimed: boolean } {
    const db = this.load();
    const def = MISSIONS_CATALOG.find((m) => m.id === missionId);
    if (!def) return { db, claimed: false };

    const txId = `claim_mission_${localDate}_${def.id}`;
    if (db.rewardTransactions.some((t) => t.id === txId)) {
      return { db, claimed: false };
    }

    const nextTransactions: RewardTransactionRecord[] = [
      {
        id: txId,
        type: 'EARN_MISSION',
        amount: def.rewardCoins,
        title: `Daily Mission: ${def.title}`,
        referenceId: def.id,
        localDate,
        createdAt: Date.now(),
      },
      ...db.rewardTransactions,
    ];
    const nextDb = this.save({
      ...db,
      rewardTransactions: nextTransactions,
    });
    return { db: nextDb, claimed: true };
  }

  exportBackupJson(preferences: OnboardingPreferences): string {
    const db = this.load();
    return encodeBackupJson({
      schemaVersion: SUPPORTED_BACKUP_SCHEMA_VERSION,
      exportedAt: Date.now(),
      preferences,
      studentName: db.studentName,
      subjects: db.subjects,
      tasks: db.tasks,
      sessions: db.sessions,
      rewardTransactions: db.rewardTransactions,
      unlockedAchievements: db.unlockedAchievements,
      unlockedGardenItems: db.unlockedGardenItems,
    });
  }

  importBackupJson(rawJson: string): BackupImportResult {
    const decoded = decodeBackupJson(rawJson);
    if (!decoded.ok) {
      return { ok: false, error: decoded.error };
    }
    const payload = decoded.payload;
    const currentDb = this.load();
    this.save({
      ...EMPTY_LOCAL_DB,
      studentName: payload.studentName.trim(),
      subjects: payload.subjects,
      tasks: payload.tasks,
      sessions: payload.sessions,
      rewardTransactions: payload.rewardTransactions,
      unlockedAchievements: payload.unlockedAchievements,
      unlockedGardenItems: payload.unlockedGardenItems,
      distractionBlocker: {
        ...currentDb.distractionBlocker,
        selectedPackageNames: [...payload.preferences.selectedDistractionApps],
        isEnabled:
          payload.preferences.selectedDistractionApps.length > 0 &&
          !payload.preferences.distractionPreferencesSkipped,
        initializedFromOnboarding: true,
      },
    });
    return {
      ok: true,
      preferences: payload.preferences,
      sessionsCount: payload.sessions.length,
      tasksCount: payload.tasks.length,
      subjectsCount: payload.subjects.length,
    };
  }

  setRecommendationDecision(
    recommendationId: string,
    status: RecommendationStatus,
  ): FocusLocalDatabase {
    const db = this.load();
    return this.save({
      ...db,
      recommendationDecisions: {
        ...db.recommendationDecisions,
        [recommendationId]: status,
      },
    });
  }

  syncBlockerFromOnboarding(
    selectedApps: string[],
    skipped: boolean,
  ): FocusLocalDatabase {
    const db = this.load();
    if (db.distractionBlocker.initializedFromOnboarding) return db;
    return this.save({
      ...db,
      distractionBlocker: {
        ...db.distractionBlocker,
        isEnabled: selectedApps.length > 0 && !skipped,
        selectedPackageNames: [...selectedApps],
        initializedFromOnboarding: true,
      },
    });
  }

  setBlockerEnabled(enabled: boolean): FocusLocalDatabase {
    const db = this.load();
    return this.save({
      ...db,
      distractionBlocker: {
        ...db.distractionBlocker,
        isEnabled: enabled,
        initializedFromOnboarding: true,
      },
    });
  }

  toggleBlockerApp(packageName: string): FocusLocalDatabase {
    const db = this.load();
    const existing = new Set(db.distractionBlocker.selectedPackageNames);
    if (existing.has(packageName)) {
      existing.delete(packageName);
    } else {
      existing.add(packageName);
    }
    return this.save({
      ...db,
      distractionBlocker: {
        ...db.distractionBlocker,
        selectedPackageNames: Array.from(existing),
        initializedFromOnboarding: true,
      },
    });
  }

  setBlockerPermissionStatus(
    permissionStatus: DistractionPermissionStatus,
    checkedAtMs: number = Date.now(),
  ): FocusLocalDatabase {
    const db = this.load();
    return this.save({
      ...db,
      distractionBlocker: {
        ...db.distractionBlocker,
        permissionStatus,
        lastCheckedAt: checkedAtMs,
        initializedFromOnboarding: true,
      },
    });
  }

  createRecoveryState(
    lastSessionId: string | null,
    triggerReason: RecoveryTriggerReason,
    plannedDurationMinutes: number,
    actualDurationMinutes: number,
    subjectId: string | null = null,
    taskId: string | null = null,
    createdAt: number = Date.now(),
  ): FocusLocalDatabase {
    const db = this.load();
    return this.save({
      ...db,
      recoveryState: {
        lastSessionId,
        triggerReason,
        plannedDurationMinutes,
        actualDurationMinutes,
        suggestedRetryMinutes: suggestRecoveryRetryMinutes(
          plannedDurationMinutes,
          actualDurationMinutes,
        ),
        subjectId,
        taskId,
        isActive: true,
        createdAt,
      },
    });
  }

  dismissRecoveryState(): FocusLocalDatabase {
    const db = this.load();
    return this.save({
      ...db,
      recoveryState: null,
    });
  }

  clearAll(): void {
    if (!this.storage) return;
    try {
      this.storage.removeItem(FOCUS_DB_STORAGE_KEY);
    } catch {
      // Ignore errors
    }
  }
}

export const focusLocalRepository = new FocusLocalRepository();
