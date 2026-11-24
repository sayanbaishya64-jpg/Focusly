export type StudentCategory =
  | 'school_student'
  | 'college_student'
  | 'competitive_aspirant'
  | 'self_learner';

export type StudyGoal =
  | 'improve_concentration'
  | 'reduce_distractions'
  | 'build_consistency'
  | 'complete_daily_goals';

export type AppLanguage = 'en' | 'bn';

export type BottomNavTab = 'home' | 'focus' | 'study' | 'progress' | 'profile';

export interface OnboardingPreferences {
  onboardingCompleted: boolean;
  studentCategory: StudentCategory;
  studyGoals: StudyGoal[];
  dailyFocusTargetMinutes: number;
  language: AppLanguage;
  selectedDistractionApps: string[];
  distractionPreferencesSkipped: boolean;
}

export const DEFAULT_PREFERENCES: OnboardingPreferences = {
  onboardingCompleted: false,
  studentCategory: 'college_student',
  studyGoals: ['improve_concentration'],
  dailyFocusTargetMinutes: 60,
  language: 'en',
  selectedDistractionApps: [],
  distractionPreferencesSkipped: false,
};

export interface DistractionCandidate {
  packageName: string;
  label: string;
  category: string;
}

export const DISTRACTION_CANDIDATES: DistractionCandidate[] = [
  { packageName: 'com.instagram.android', label: 'Instagram', category: 'Social Media' },
  { packageName: 'com.google.android.youtube', label: 'YouTube', category: 'Video Streaming' },
  { packageName: 'com.zhiliaoapp.musically', label: 'Short Video Apps', category: 'Short Reels' },
  { packageName: 'com.whatsapp', label: 'WhatsApp', category: 'Messaging' },
  { packageName: 'com.reddit.frontpage', label: 'Reddit', category: 'Forums & Feeds' },
];

export type SessionStatus = 'IDLE' | 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'ENDED_EARLY';
export type SessionType = 'FOCUS' | 'BREAK';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';

export interface SubjectItem {
  id: string;
  subjectName: string;
  subjectColor: string;
  createdAt: number;
  updatedAt: number;
}

export interface StudyTaskItem {
  id: string;
  title: string;
  description: string;
  subjectId: string | null;
  plannedDurationMinutes: number;
  priority: TaskPriority;
  status: TaskStatus;
  scheduledDate?: string | null;
  createdAt: number;
  updatedAt: number;
}

export interface FocusSessionRecord {
  id: string;
  plannedDurationSeconds: number;
  actualDurationSeconds: number;
  startTimestamp: number;
  completionTimestamp: number | null;
  pausedDurationSeconds: number;
  sessionStatus: SessionStatus;
  sessionType: SessionType;
  localDate: string;
  subjectId: string | null;
  taskId: string | null;
  createdAt: number;
}

export interface ActiveTimerSnapshot {
  sessionId: string;
  sessionType: SessionType;
  plannedDurationSeconds: number;
  startTimestampMs: number;
  pauseStartedTimestampMs: number | null;
  accumulatedPausedMs: number;
  status: SessionStatus;
  subjectId: string | null;
  taskId: string | null;
}

export interface TimerProgress {
  elapsedSeconds: number;
  remainingSeconds: number;
  progressFraction: number;
  isCompleted: boolean;
}

export interface DailyStats {
  date: string;
  targetMinutes: number;
  completedFocusMinutes: number;
  completedFocusSeconds: number;
  remainingMinutes: number;
  completionPercentage: number;
  completedSessions: number;
  earlyEndedSessions: number;
}

export interface WeeklyDayBreakdown {
  date: string;
  dayShortLabel: string;
  focusMinutes: number;
  completedSessions: number;
}

export interface WeeklyStats {
  totalWeeklyFocusMinutes: number;
  completedSessions: number;
  averageSessionMinutes: number;
  dailyBreakdown: WeeklyDayBreakdown[];
}

export type StudyScheduleFilter = 'TODAY' | 'UPCOMING' | 'ALL' | 'COMPLETED';
export type AnalyticsPeriod = 'WEEK' | 'MONTH';

export interface SubjectProgressSummary {
  subjectId: string | null;
  subjectName: string;
  subjectColor: string;
  focusMinutes: number;
  completedSessions: number;
  sharePercentage: number;
}

export interface ProgressAnalyticsSummary {
  period: AnalyticsPeriod;
  totalFocusMinutes: number;
  completedSessions: number;
  earlyEndedSessions: number;
  completionRatePercent: number;
  averageSessionMinutes: number;
  activeDaysCount: number;
  dailyBreakdown: WeeklyDayBreakdown[];
  subjectBreakdown: SubjectProgressSummary[];
}

export type RecommendationConfidence =
  | 'INSUFFICIENT_DATA'
  | 'LOW'
  | 'MEDIUM'
  | 'HIGH';

export type RecommendationReasonType =
  | 'INSUFFICIENT_DATA'
  | 'AVOID_LONG_DROPOFF'
  | 'CONSISTENT_STEP_UP'
  | 'RECENT_STRUGGLE'
  | 'STEADY_RHYTHM';

export type RecommendationStatus = 'ACTIVE' | 'ACCEPTED' | 'IGNORED';

export interface FocusRecommendation {
  id: string;
  recommendedMinutes: number;
  reasonType: RecommendationReasonType;
  reasonText: string;
  confidence: RecommendationConfidence;
  hasEnoughData: boolean;
  basedOnSessionCount: number;
  preferredPeriod: 'morning' | 'afternoon' | 'evening' | null;
  subjectId: string | null;
  status: RecommendationStatus;
  generatedAt: number;
}

export type DistractionPermissionStatus = 'GRANTED' | 'DENIED';
export type DistractionBlockerMode = 'ACTIVE_DETECTION' | 'CHECKLIST_ONLY' | 'DISABLED';

export interface DistractionBlockerSettings {
  isEnabled: boolean;
  selectedPackageNames: string[];
  permissionStatus: DistractionPermissionStatus;
  lastCheckedAt: number | null;
  initializedFromOnboarding: boolean;
}

export type RecoveryTriggerReason =
  | 'INTERRUPTED_IN_BACKGROUND'
  | 'ENDED_EARLY'
  | 'PAUSED_LONG';

export interface FocusRecoveryState {
  lastSessionId: string | null;
  triggerReason: RecoveryTriggerReason;
  plannedDurationMinutes: number;
  actualDurationMinutes: number;
  suggestedRetryMinutes: number;
  subjectId: string | null;
  taskId: string | null;
  isActive: boolean;
  createdAt: number;
}

export type RewardTransactionType =
  | 'EARN_SESSION'
  | 'EARN_DAILY_GOAL'
  | 'EARN_ACHIEVEMENT'
  | 'EARN_MISSION'
  | 'SPEND_STORE';

export interface RewardTransactionRecord {
  id: string;
  type: RewardTransactionType;
  amount: number;
  title: string;
  referenceId: string | null;
  localDate: string;
  createdAt: number;
}

export interface AchievementUnlockRecord {
  achievementId: string;
  unlockedAt: number;
}

export interface UnlockedGardenItemRecord {
  itemId: string;
  purchasedAt: number;
  isFeatured: boolean;
}

export interface AchievementItem {
  id: string;
  title: string;
  description: string;
  coinBonus: number;
  isUnlocked: boolean;
  unlockedAt: number | null;
}

export type GardenGrowthStage = 'SEEDLING' | 'SPROUTING' | 'BLOOMING';

export interface GardenStoreItem {
  id: string;
  title: string;
  description: string;
  costCoins: number;
  accentColor: string;
  isUnlocked: boolean;
  isFeatured: boolean;
  growthStage: GardenGrowthStage;
}

export interface RewardPreview {
  focusCoins: number;
  unlockedAchievements: number;
}

export interface RewardsDashboardState {
  coinBalance: number;
  totalEarnedCoins: number;
  unlockedAchievementsCount: number;
  totalAchievementsCount: number;
  completedFocusSessionsCount: number;
  currentStreakDays: number;
  achievements: AchievementItem[];
  storeItems: GardenStoreItem[];
  recentTransactions: RewardTransactionRecord[];
}

export type RewardPurchaseResult =
  | 'SUCCESS'
  | 'INSUFFICIENT_COINS'
  | 'ALREADY_UNLOCKED'
  | 'NOT_FOUND';

export type FocusMode = 'MANUAL' | 'AUTO_SCHEDULE' | 'SMART';

export interface DailyMissionItem {
  id: string;
  title: string;
  description: string;
  rewardCoins: number;
  currentProgress: number;
  targetProgress: number;
  isCompleted: boolean;
  isClaimed: boolean;
}

export interface FocuslyBackupPayload {
  schemaVersion: number;
  exportedAt: number;
  preferences: OnboardingPreferences;
  studentName: string;
  subjects: SubjectItem[];
  tasks: StudyTaskItem[];
  sessions: FocusSessionRecord[];
  rewardTransactions: RewardTransactionRecord[];
  unlockedAchievements: AchievementUnlockRecord[];
  unlockedGardenItems: UnlockedGardenItemRecord[];
}

export type BackupImportResult =
  | {
      ok: true;
      preferences: OnboardingPreferences;
      sessionsCount: number;
      tasksCount: number;
      subjectsCount: number;
    }
  | {
      ok: false;
      error: string;
    };

const STORAGE_KEY = 'focusly_phase1_preferences_v1';

export function loadPreferences(): OnboardingPreferences {
  if (typeof window === 'undefined') return DEFAULT_PREFERENCES;
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return DEFAULT_PREFERENCES;
    const parsed = JSON.parse(raw) as Partial<OnboardingPreferences>;
    return {
      onboardingCompleted: Boolean(parsed.onboardingCompleted),
      studentCategory: parsed.studentCategory ?? 'college_student',
      studyGoals:
        Array.isArray(parsed.studyGoals) && parsed.studyGoals.length > 0
          ? parsed.studyGoals
          : ['improve_concentration'],
      dailyFocusTargetMinutes:
        typeof parsed.dailyFocusTargetMinutes === 'number' &&
        parsed.dailyFocusTargetMinutes >= 10 &&
        parsed.dailyFocusTargetMinutes <= 480
          ? parsed.dailyFocusTargetMinutes
          : 60,
      language: parsed.language === 'bn' ? 'bn' : 'en',
      selectedDistractionApps: Array.isArray(parsed.selectedDistractionApps)
        ? parsed.selectedDistractionApps
        : [],
      distractionPreferencesSkipped: Boolean(parsed.distractionPreferencesSkipped),
    };
  } catch {
    return DEFAULT_PREFERENCES;
  }
}

export function savePreferences(prefs: OnboardingPreferences): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(prefs));
  } catch {
    // Ignore storage errors
  }
}

export function clearPreferences(): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Ignore storage errors
  }
}
