package com.focusly.app.domain.repository

import com.focusly.app.domain.model.ActiveTimerSnapshot
import com.focusly.app.domain.model.BackupImportResult
import com.focusly.app.domain.model.FocusSession
import com.focusly.app.domain.model.RewardPreview
import com.focusly.app.domain.model.RewardPurchaseResult
import com.focusly.app.domain.model.RewardsDashboardState
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.StudyTask
import com.focusly.app.domain.model.Subject
import com.focusly.app.domain.model.TaskPriority
import com.focusly.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

interface FocusSessionRepository {
    val activeTimerFlow: Flow<ActiveTimerSnapshot?>
    fun observeSessionsForDate(localDate: String): Flow<List<FocusSession>>
    fun observeSessionsInRange(startDate: String, endDate: String): Flow<List<FocusSession>>
    fun observeRecentSessions(limit: Int = 20): Flow<List<FocusSession>>

    suspend fun getActiveTimerSnapshot(): ActiveTimerSnapshot?
    suspend fun saveActiveTimerSnapshot(snapshot: ActiveTimerSnapshot)
    suspend fun clearActiveTimerSnapshot()

    suspend fun recordSessionCompletion(
        snapshot: ActiveTimerSnapshot,
        finalStatus: SessionStatus,
        actualDurationSeconds: Long,
        completionTimestampMs: Long,
        localDate: String,
        dailyTargetMinutes: Int
    ): FocusSession
}

interface SubjectRepository {
    fun observeAllSubjects(): Flow<List<Subject>>
    suspend fun upsertSubject(name: String, colorHex: Long): Subject
    suspend fun updateSubject(subjectId: String, name: String, colorHex: Long)
    suspend fun deleteSubject(subjectId: String)
}

interface StudyTaskRepository {
    fun observeAllTasks(): Flow<List<StudyTask>>
    fun observeCurrentActiveTask(): Flow<StudyTask?>
    suspend fun createTask(
        title: String,
        description: String,
        subjectId: String?,
        plannedDurationMinutes: Int,
        priority: TaskPriority = TaskPriority.MEDIUM,
        scheduledDate: String? = null
    ): StudyTask
    suspend fun updateTask(
        taskId: String,
        title: String,
        description: String,
        subjectId: String?,
        plannedDurationMinutes: Int,
        priority: TaskPriority,
        scheduledDate: String?
    )
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus)
    suspend fun rescheduleTask(taskId: String, scheduledDate: String?)
    suspend fun rescheduleUnfinishedTasksToDate(beforeDate: String, targetDate: String): Int
    suspend fun deleteTask(taskId: String)
}

interface UserProfileRepository {
    fun observeStudentName(): Flow<String>
    suspend fun updateStudentName(name: String)
}

interface RewardPreviewProvider {
    fun observeRewardPreview(): Flow<RewardPreview>
}

interface RewardsRepository {
    fun observeRewardsDashboard(): Flow<RewardsDashboardState>
    fun observeClaimedMissionIdsForDate(localDate: String): Flow<Set<String>>
    suspend fun claimDailyMission(missionId: String, localDate: String): Boolean
    suspend fun purchaseStoreItem(itemId: String, localDate: String): RewardPurchaseResult
    suspend fun setFeaturedGardenItem(itemId: String)
}

interface LocalBackupRepository {
    suspend fun exportBackupJson(): String
    suspend fun importBackupJson(json: String): BackupImportResult
}
