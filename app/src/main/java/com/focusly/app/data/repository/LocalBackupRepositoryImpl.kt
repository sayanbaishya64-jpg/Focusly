package com.focusly.app.data.repository

import androidx.room.withTransaction
import com.focusly.app.data.local.dao.FocusSessionDao
import com.focusly.app.data.local.dao.RewardDao
import com.focusly.app.data.local.dao.StudyTaskDao
import com.focusly.app.data.local.dao.SubjectDao
import com.focusly.app.data.local.dao.UserProfileDao
import com.focusly.app.data.local.database.FocuslyDatabase
import com.focusly.app.data.local.entity.AchievementUnlockEntity
import com.focusly.app.data.local.entity.FocusSessionEntity
import com.focusly.app.data.local.entity.RewardTransactionEntity
import com.focusly.app.data.local.entity.StudyTaskEntity
import com.focusly.app.data.local.entity.SubjectEntity
import com.focusly.app.data.local.entity.UnlockedRewardItemEntity
import com.focusly.app.data.local.entity.UserProfileEntity
import com.focusly.app.data.preferences.UserPreferencesDataStore
import com.focusly.app.domain.model.BackupImportResult
import com.focusly.app.domain.repository.LocalBackupRepository
import com.focusly.app.domain.usecase.BackupAchievementPayload
import com.focusly.app.domain.usecase.BackupGardenItemPayload
import com.focusly.app.domain.usecase.BackupSessionPayload
import com.focusly.app.domain.usecase.BackupSubjectPayload
import com.focusly.app.domain.usecase.BackupTaskPayload
import com.focusly.app.domain.usecase.BackupTransactionPayload
import com.focusly.app.domain.usecase.FocuslyBackupCodec
import com.focusly.app.domain.usecase.FocuslyBackupPayload
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalBackupRepositoryImpl @Inject constructor(
    private val database: FocuslyDatabase,
    private val dataStore: UserPreferencesDataStore,
    private val userProfileDao: UserProfileDao,
    private val subjectDao: SubjectDao,
    private val studyTaskDao: StudyTaskDao,
    private val focusSessionDao: FocusSessionDao,
    private val rewardDao: RewardDao
) : LocalBackupRepository {

    override suspend fun exportBackupJson(): String {
        val prefs = dataStore.preferencesFlow.first()
        val profile = userProfileDao.getProfileSync()
        val subjects = subjectDao.getAllSubjectsSync().map {
            BackupSubjectPayload(
                id = it.id,
                subjectName = it.subjectName,
                subjectColor = it.subjectColor,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
        val tasks = studyTaskDao.getAllTasksSync().map {
            BackupTaskPayload(
                id = it.id,
                title = it.title,
                description = it.description,
                subjectId = it.subjectId,
                plannedDurationMinutes = it.plannedDurationMinutes,
                priority = it.priority,
                status = it.status,
                scheduledDate = it.scheduledDate,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
        val sessions = focusSessionDao.getAllSessionsSync().map {
            BackupSessionPayload(
                id = it.id,
                plannedDurationSeconds = it.plannedDurationSeconds,
                actualDurationSeconds = it.actualDurationSeconds,
                startTimestamp = it.startTimestamp,
                completionTimestamp = it.completionTimestamp,
                pausedDurationSeconds = it.pausedDurationSeconds,
                sessionStatus = it.sessionStatus,
                sessionType = it.sessionType,
                localDate = it.localDate,
                subjectId = it.subjectId,
                taskId = it.taskId,
                createdAt = it.createdAt
            )
        }
        val transactions = rewardDao.getAllTransactionsSync().map {
            BackupTransactionPayload(
                id = it.id,
                type = it.type,
                amount = it.amount,
                title = it.title,
                referenceId = it.referenceId,
                localDate = it.localDate,
                createdAt = it.createdAt
            )
        }
        val achievements = rewardDao.getAllAchievementsSync().map {
            BackupAchievementPayload(
                achievementId = it.achievementId,
                unlockedAt = it.unlockedAt
            )
        }
        val gardenItems = rewardDao.getAllUnlockedItemsSync().map {
            BackupGardenItemPayload(
                itemId = it.itemId,
                purchasedAt = it.purchasedAt,
                isFeatured = it.isFeatured
            )
        }

        return FocuslyBackupCodec.encode(
            FocuslyBackupPayload(
                exportedAt = System.currentTimeMillis(),
                preferences = prefs,
                studentName = profile?.studentName.orEmpty(),
                subjects = subjects,
                tasks = tasks,
                sessions = sessions,
                rewardTransactions = transactions,
                unlockedAchievements = achievements,
                unlockedGardenItems = gardenItems
            )
        )
    }

    override suspend fun importBackupJson(json: String): BackupImportResult {
        val decoded = FocuslyBackupCodec.decode(json)
        val payload = decoded.getOrElse { err ->
            return BackupImportResult.InvalidFormat(err.message ?: "Invalid backup JSON")
        }

        val subjectIds = payload.subjects.mapTo(HashSet()) { it.id }
        val taskIds = payload.tasks.mapTo(HashSet()) { it.id }

        database.withTransaction {
            focusSessionDao.deleteAll()
            studyTaskDao.deleteAll()
            subjectDao.deleteAll()
            rewardDao.deleteAllTransactions()
            rewardDao.deleteAllAchievements()
            rewardDao.deleteAllUnlockedItems()

            payload.subjects.forEach { s ->
                subjectDao.upsert(
                    SubjectEntity(
                        id = s.id,
                        subjectName = s.subjectName,
                        subjectColor = s.subjectColor,
                        createdAt = s.createdAt,
                        updatedAt = s.updatedAt
                    )
                )
            }
            payload.tasks.forEach { t ->
                studyTaskDao.upsert(
                    StudyTaskEntity(
                        id = t.id,
                        title = t.title,
                        description = t.description,
                        subjectId = t.subjectId?.takeIf { it in subjectIds },
                        plannedDurationMinutes = t.plannedDurationMinutes,
                        priority = t.priority,
                        status = t.status,
                        scheduledDate = t.scheduledDate,
                        createdAt = t.createdAt,
                        updatedAt = t.updatedAt
                    )
                )
            }
            payload.sessions.forEach { sess ->
                focusSessionDao.upsert(
                    FocusSessionEntity(
                        id = sess.id,
                        plannedDurationSeconds = sess.plannedDurationSeconds,
                        actualDurationSeconds = sess.actualDurationSeconds,
                        startTimestamp = sess.startTimestamp,
                        completionTimestamp = sess.completionTimestamp,
                        pausedDurationSeconds = sess.pausedDurationSeconds,
                        sessionStatus = sess.sessionStatus,
                        sessionType = sess.sessionType,
                        localDate = sess.localDate,
                        subjectId = sess.subjectId?.takeIf { it in subjectIds },
                        taskId = sess.taskId?.takeIf { it in taskIds },
                        createdAt = sess.createdAt
                    )
                )
            }
            userProfileDao.upsert(
                UserProfileEntity(
                    id = UserProfileEntity.LOCAL_PROFILE_ID,
                    studentName = payload.studentName.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            payload.rewardTransactions.forEach { tx ->
                rewardDao.insertTransactionIfAbsent(
                    RewardTransactionEntity(
                        id = tx.id,
                        type = tx.type,
                        amount = tx.amount,
                        title = tx.title,
                        referenceId = tx.referenceId,
                        localDate = tx.localDate,
                        createdAt = tx.createdAt
                    )
                )
            }
            payload.unlockedAchievements.forEach { a ->
                rewardDao.insertAchievementUnlockIfAbsent(
                    AchievementUnlockEntity(
                        achievementId = a.achievementId,
                        unlockedAt = a.unlockedAt
                    )
                )
            }
            payload.unlockedGardenItems.forEach { g ->
                rewardDao.insertUnlockedItemIfAbsent(
                    UnlockedRewardItemEntity(
                        itemId = g.itemId,
                        purchasedAt = g.purchasedAt,
                        isFeatured = g.isFeatured
                    )
                )
            }
        }

        dataStore.setStudentCategory(payload.preferences.studentCategory)
        dataStore.setStudyGoals(payload.preferences.studyGoals)
        dataStore.setDailyFocusTarget(payload.preferences.dailyFocusTargetMinutes)
        dataStore.setLanguage(payload.preferences.language)
        dataStore.setDistractionPreferences(
            apps = payload.preferences.selectedDistractionApps,
            skipped = payload.preferences.distractionPreferencesSkipped
        )
        dataStore.setOnboardingCompleted(payload.preferences.onboardingCompleted)

        return BackupImportResult.Success(
            sessionsCount = payload.sessions.size,
            tasksCount = payload.tasks.size,
            subjectsCount = payload.subjects.size
        )
    }
}
