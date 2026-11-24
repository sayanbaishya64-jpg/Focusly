package com.focusly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusly.app.data.local.entity.StudyTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: StudyTaskEntity)

    @Query("SELECT * FROM study_tasks ORDER BY updatedAt DESC")
    fun observeAllTasks(): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks WHERE status != 'COMPLETED' ORDER BY updatedAt DESC LIMIT 1")
    fun observeCurrentActiveTask(): Flow<StudyTaskEntity?>

    @Query("SELECT * FROM study_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StudyTaskEntity?

    @Query("UPDATE study_tasks SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)

    @Query("UPDATE study_tasks SET status = 'IN_PROGRESS', updatedAt = :updatedAt WHERE id = :id AND status = 'PENDING'")
    suspend fun markInProgressIfPending(id: String, updatedAt: Long)

    @Query(
        """
        UPDATE study_tasks
        SET title = :title,
            description = :description,
            subjectId = :subjectId,
            plannedDurationMinutes = :plannedDurationMinutes,
            priority = :priority,
            scheduledDate = :scheduledDate,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateTaskDetails(
        id: String,
        title: String,
        description: String,
        subjectId: String?,
        plannedDurationMinutes: Int,
        priority: String,
        scheduledDate: String?,
        updatedAt: Long
    )

    @Query("UPDATE study_tasks SET scheduledDate = :scheduledDate, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rescheduleTask(id: String, scheduledDate: String?, updatedAt: Long)

    @Query(
        """
        UPDATE study_tasks
        SET scheduledDate = :targetDate, updatedAt = :updatedAt
        WHERE status != 'COMPLETED' AND scheduledDate IS NOT NULL AND scheduledDate < :beforeDate
        """
    )
    suspend fun rescheduleUnfinishedBeforeDate(
        beforeDate: String,
        targetDate: String,
        updatedAt: Long
    ): Int

    @Query("DELETE FROM study_tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM study_tasks ORDER BY updatedAt DESC")
    suspend fun getAllTasksSync(): List<StudyTaskEntity>

    @Query("DELETE FROM study_tasks")
    suspend fun deleteAll()
}
