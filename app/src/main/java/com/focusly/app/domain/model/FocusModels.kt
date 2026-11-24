package com.focusly.app.domain.model

enum class SessionStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    ENDED_EARLY
}

enum class SessionType {
    FOCUS,
    BREAK
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH
}

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}

data class Subject(
    val id: String,
    val name: String,
    val colorHex: Long,
    val createdAt: Long,
    val updatedAt: Long
)

data class StudyTask(
    val id: String,
    val title: String,
    val description: String,
    val subjectId: String?,
    val subjectName: String?,
    val plannedDurationMinutes: Int,
    val priority: TaskPriority,
    val status: TaskStatus,
    val scheduledDate: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class FocusSession(
    val id: String,
    val plannedDurationSeconds: Long,
    val actualDurationSeconds: Long,
    val startTimestamp: Long,
    val completionTimestamp: Long?,
    val pausedDurationSeconds: Long,
    val status: SessionStatus,
    val type: SessionType,
    val localDate: String,
    val subjectId: String?,
    val taskId: String?,
    val createdAt: Long
)

data class ActiveTimerSnapshot(
    val sessionId: String,
    val sessionType: SessionType,
    val plannedDurationSeconds: Long,
    val startTimestampMs: Long,
    val pauseStartedTimestampMs: Long?,
    val accumulatedPausedMs: Long,
    val status: SessionStatus,
    val subjectId: String?,
    val taskId: String?
) {
    val isActive: Boolean
        get() = status == SessionStatus.RUNNING || status == SessionStatus.PAUSED
}

data class TimerProgress(
    val elapsedSeconds: Long,
    val remainingSeconds: Long,
    val progressFraction: Float,
    val isCompleted: Boolean
)

data class DailyStats(
    val date: String,
    val targetMinutes: Int,
    val completedFocusMinutes: Int,
    val completedFocusSeconds: Long,
    val remainingMinutes: Int,
    val completionPercentage: Float,
    val completedSessions: Int,
    val earlyEndedSessions: Int
)

data class WeeklyDayBreakdown(
    val date: String,
    val dayShortLabel: String,
    val focusMinutes: Int,
    val completedSessions: Int
)

data class WeeklyStats(
    val totalWeeklyFocusMinutes: Int,
    val completedSessions: Int,
    val averageSessionMinutes: Int,
    val dailyBreakdown: List<WeeklyDayBreakdown>
)

enum class StudyScheduleFilter {
    TODAY,
    UPCOMING,
    ALL,
    COMPLETED
}

enum class AnalyticsPeriod(val days: Int) {
    WEEK(7),
    MONTH(30)
}

data class SubjectProgressSummary(
    val subjectId: String?,
    val subjectName: String,
    val colorHex: Long,
    val focusMinutes: Int,
    val completedSessions: Int,
    val sharePercentage: Float
)

data class ProgressAnalyticsSummary(
    val period: AnalyticsPeriod,
    val totalFocusMinutes: Int,
    val completedSessions: Int,
    val earlyEndedSessions: Int,
    val completionRatePercent: Float,
    val averageSessionMinutes: Int,
    val activeDaysCount: Int,
    val dailyBreakdown: List<WeeklyDayBreakdown>,
    val subjectBreakdown: List<SubjectProgressSummary>
)

data class RewardPreview(
    val focusCoins: Int = 0,
    val unlockedAchievements: Int = 0
)

enum class RewardTransactionType {
    EARN_SESSION,
    EARN_DAILY_GOAL,
    EARN_ACHIEVEMENT,
    EARN_MISSION,
    SPEND_STORE
}

data class RewardTransaction(
    val id: String,
    val type: RewardTransactionType,
    val amount: Int,
    val title: String,
    val referenceId: String? = null,
    val localDate: String,
    val createdAt: Long
)

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val coinBonus: Int,
    val isUnlocked: Boolean,
    val unlockedAt: Long? = null
)

enum class GardenGrowthStage {
    SEEDLING,
    SPROUTING,
    BLOOMING
}

data class GardenStoreItem(
    val id: String,
    val title: String,
    val description: String,
    val costCoins: Int,
    val accentHex: Long,
    val isUnlocked: Boolean,
    val isFeatured: Boolean,
    val growthStage: GardenGrowthStage
)

data class RewardsDashboardState(
    val coinBalance: Int = 0,
    val totalEarnedCoins: Int = 0,
    val unlockedAchievementsCount: Int = 0,
    val totalAchievementsCount: Int = 6,
    val completedFocusSessionsCount: Int = 0,
    val currentStreakDays: Int = 0,
    val achievements: List<AchievementItem> = emptyList(),
    val storeItems: List<GardenStoreItem> = emptyList(),
    val recentTransactions: List<RewardTransaction> = emptyList()
)

sealed class RewardPurchaseResult {
    data object Success : RewardPurchaseResult()
    data object InsufficientCoins : RewardPurchaseResult()
    data object AlreadyUnlocked : RewardPurchaseResult()
    data object NotFound : RewardPurchaseResult()
}

enum class FocusMode {
    MANUAL,
    AUTO_SCHEDULE,
    SMART
}

data class DailyMissionItem(
    val id: String,
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val currentProgress: Int,
    val targetProgress: Int,
    val isCompleted: Boolean,
    val isClaimed: Boolean
)

sealed class BackupImportResult {
    data class Success(
        val sessionsCount: Int,
        val tasksCount: Int,
        val subjectsCount: Int
    ) : BackupImportResult()

    data class InvalidFormat(val reason: String) : BackupImportResult()
}
