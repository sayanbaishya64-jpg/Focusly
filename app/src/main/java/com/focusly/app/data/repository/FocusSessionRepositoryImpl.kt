package com.focusly.app.data.repository

import androidx.room.withTransaction
import com.focusly.app.data.local.dao.DailyGoalDao
import com.focusly.app.data.local.dao.FocusSessionDao
import com.focusly.app.data.local.dao.RewardDao
import com.focusly.app.data.local.dao.StudyTaskDao
import com.focusly.app.data.local.database.FocuslyDatabase
import com.focusly.app.data.local.entity.AchievementUnlockEntity
import com.focusly.app.data.local.entity.DailyGoalEntity
import com.focusly.app.data.local.entity.FocusSessionEntity
import com.focusly.app.data.local.entity.RewardTransactionEntity
import com.focusly.app.data.preferences.ActiveTimerStateStore
import com.focusly.app.domain.model.ActiveTimerSnapshot
import com.focusly.app.domain.model.FocusSession
import com.focusly.app.domain.model.RewardTransactionType
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import com.focusly.app.domain.model.TaskStatus
import com.focusly.app.domain.repository.FocusSessionRepository
import com.focusly.app.domain.usecase.DashboardStatsCalculator
import com.focusly.app.domain.usecase.RewardEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class FocusSessionRepositoryImpl @Inject constructor(
    private val database: FocuslyDatabase,
    private val focusSessionDao: FocusSessionDao,
    private val dailyGoalDao: DailyGoalDao,
    private val studyTaskDao: StudyTaskDao,
    private val rewardDao: RewardDao,
    private val activeTimerStateStore: ActiveTimerStateStore
) : FocusSessionRepository {

    override val activeTimerFlow: Flow<ActiveTimerSnapshot?> = activeTimerStateStore.activeTimerFlow

    override fun observeSessionsForDate(localDate: String): Flow<List<FocusSession>> {
        return focusSessionDao.observeSessionsForDate(localDate).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeSessionsInRange(startDate: String, endDate: String): Flow<List<FocusSession>> {
        return focusSessionDao.observeSessionsInRange(startDate, endDate).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeRecentSessions(limit: Int): Flow<List<FocusSession>> {
        return focusSessionDao.observeRecentSessions(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getActiveTimerSnapshot(): ActiveTimerSnapshot? {
        return activeTimerStateStore.getSnapshot()
    }

    override suspend fun saveActiveTimerSnapshot(snapshot: ActiveTimerSnapshot) {
        activeTimerStateStore.saveSnapshot(snapshot)
    }

    override suspend fun clearActiveTimerSnapshot() {
        activeTimerStateStore.clear()
    }

    override suspend fun recordSessionCompletion(
        snapshot: ActiveTimerSnapshot,
        finalStatus: SessionStatus,
        actualDurationSeconds: Long,
        completionTimestampMs: Long,
        localDate: String,
        dailyTargetMinutes: Int
    ): FocusSession {
        val clampedDuration = min(
            max(0L, actualDurationSeconds),
            max(1L, snapshot.plannedDurationSeconds)
        )
        val pausedSeconds = max(0L, snapshot.accumulatedPausedMs / 1000L)

        val entity = FocusSessionEntity(
            id = snapshot.sessionId,
            plannedDurationSeconds = snapshot.plannedDurationSeconds,
            actualDurationSeconds = clampedDuration,
            startTimestamp = snapshot.startTimestampMs,
            completionTimestamp = completionTimestampMs,
            pausedDurationSeconds = pausedSeconds,
            sessionStatus = finalStatus.name,
            sessionType = snapshot.sessionType.name,
            localDate = localDate,
            subjectId = snapshot.subjectId,
            taskId = snapshot.taskId,
            createdAt = snapshot.startTimestampMs
        )

        database.withTransaction {
            val existing = focusSessionDao.getById(snapshot.sessionId)
            val isNewInsertion = existing == null
            if (isNewInsertion) {
                focusSessionDao.insertIfAbsent(entity)
            }

            if (dailyGoalDao.getGoalForDate(localDate) == null) {
                dailyGoalDao.upsert(
                    DailyGoalEntity(
                        id = UUID.randomUUID().toString(),
                        targetMinutes = dailyTargetMinutes,
                        goalDate = localDate,
                        createdAt = completionTimestampMs
                    )
                )
            }

            if (finalStatus == SessionStatus.COMPLETED &&
                snapshot.sessionType == SessionType.FOCUS &&
                snapshot.taskId != null
            ) {
                studyTaskDao.markInProgressIfPending(
                    id = snapshot.taskId,
                    updatedAt = completionTimestampMs
                )
            }

            if (isNewInsertion &&
                finalStatus == SessionStatus.COMPLETED &&
                snapshot.sessionType == SessionType.FOCUS
            ) {
                val sessionCoins = RewardEngine.calculateSessionCoins(
                    plannedDurationSeconds = snapshot.plannedDurationSeconds,
                    actualDurationSeconds = clampedDuration,
                    sessionType = snapshot.sessionType,
                    sessionStatus = finalStatus
                )
                if (sessionCoins > 0) {
                    val mins = (clampedDuration / 60L).toInt().coerceAtLeast(1)
                    rewardDao.insertTransactionIfAbsent(
                        RewardTransactionEntity(
                            id = "earn_session_${snapshot.sessionId}",
                            type = RewardTransactionType.EARN_SESSION.name,
                            amount = sessionCoins,
                            title = "${mins}m Focus Session",
                            referenceId = snapshot.sessionId,
                            localDate = localDate,
                            createdAt = completionTimestampMs
                        )
                    )
                }

                val allCompleted = focusSessionDao.getCompletedFocusSessionsSync().map { it.toDomain() }
                val parsedLocalDate = runCatching { LocalDate.parse(localDate) }.getOrDefault(LocalDate.now())
                val dayStats = DashboardStatsCalculator.calculateDailyStats(
                    sessions = allCompleted,
                    targetMinutes = dailyTargetMinutes,
                    targetDate = parsedLocalDate
                )
                if (dayStats.completedFocusMinutes >= dayStats.targetMinutes) {
                    rewardDao.insertTransactionIfAbsent(
                        RewardTransactionEntity(
                            id = "earn_daily_goal_$localDate",
                            type = RewardTransactionType.EARN_DAILY_GOAL.name,
                            amount = RewardEngine.DAILY_TARGET_BONUS_COINS,
                            title = "Daily Focus Goal Bonus",
                            referenceId = localDate,
                            localDate = localDate,
                            createdAt = completionTimestampMs
                        )
                    )
                }

                val unlockedIds = rewardDao.getUnlockedAchievementIdsSync().toSet()
                val newlyUnlocked = RewardEngine.evaluateNewlyUnlockedAchievements(
                    sessions = allCompleted,
                    dailyTargetMinutes = dailyTargetMinutes,
                    todayDate = parsedLocalDate,
                    alreadyUnlockedIds = unlockedIds
                )
                newlyUnlocked.forEach { achievement ->
                    val inserted = rewardDao.insertAchievementUnlockIfAbsent(
                        AchievementUnlockEntity(
                            achievementId = achievement.id,
                            unlockedAt = completionTimestampMs
                        )
                    )
                    if (inserted != -1L && achievement.coinBonus > 0) {
                        rewardDao.insertTransactionIfAbsent(
                            RewardTransactionEntity(
                                id = "earn_achievement_${achievement.id}",
                                type = RewardTransactionType.EARN_ACHIEVEMENT.name,
                                amount = achievement.coinBonus,
                                title = "Achievement: ${achievement.title}",
                                referenceId = achievement.id,
                                localDate = localDate,
                                createdAt = completionTimestampMs
                            )
                        )
                    }
                }
            }
        }

        activeTimerStateStore.clear()
        return (focusSessionDao.getById(snapshot.sessionId) ?: entity).toDomain()
    }

    private fun FocusSessionEntity.toDomain(): FocusSession {
        return FocusSession(
            id = id,
            plannedDurationSeconds = plannedDurationSeconds,
            actualDurationSeconds = actualDurationSeconds,
            startTimestamp = startTimestamp,
            completionTimestamp = completionTimestamp,
            pausedDurationSeconds = pausedDurationSeconds,
            status = runCatching { SessionStatus.valueOf(sessionStatus) }
                .getOrDefault(SessionStatus.ENDED_EARLY),
            type = runCatching { SessionType.valueOf(sessionType) }
                .getOrDefault(SessionType.FOCUS),
            localDate = localDate,
            subjectId = subjectId,
            taskId = taskId,
            createdAt = createdAt
        )
    }
}
