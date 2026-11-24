package com.focusly.app.data.repository

import com.focusly.app.data.local.dao.StudyTaskDao
import com.focusly.app.data.local.dao.SubjectDao
import com.focusly.app.data.local.dao.UserProfileDao
import com.focusly.app.data.local.entity.StudyTaskEntity
import com.focusly.app.data.local.entity.SubjectEntity
import com.focusly.app.data.local.entity.UserProfileEntity
import com.focusly.app.domain.model.StudyTask
import com.focusly.app.domain.model.Subject
import com.focusly.app.domain.model.TaskPriority
import com.focusly.app.domain.model.TaskStatus
import com.focusly.app.domain.repository.StudyTaskRepository
import com.focusly.app.domain.repository.SubjectRepository
import com.focusly.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubjectRepositoryImpl @Inject constructor(
    private val subjectDao: SubjectDao
) : SubjectRepository {

    override fun observeAllSubjects(): Flow<List<Subject>> {
        return subjectDao.observeAllSubjects().map { list ->
            list.map {
                Subject(
                    id = it.id,
                    name = it.subjectName,
                    colorHex = it.subjectColor,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
        }
    }

    override suspend fun upsertSubject(name: String, colorHex: Long): Subject {
        val now = System.currentTimeMillis()
        val trimmed = name.trim().ifEmpty { "General Study" }
        val existing = subjectDao.getByNameIgnoreCase(trimmed)
        if (existing != null) {
            subjectDao.updateSubject(
                id = existing.id,
                name = trimmed,
                colorHex = colorHex,
                updatedAt = now
            )
            return Subject(
                id = existing.id,
                name = trimmed,
                colorHex = colorHex,
                createdAt = existing.createdAt,
                updatedAt = now
            )
        }
        val entity = SubjectEntity(
            id = UUID.randomUUID().toString(),
            subjectName = trimmed,
            subjectColor = colorHex,
            createdAt = now,
            updatedAt = now
        )
        subjectDao.upsert(entity)
        return Subject(
            id = entity.id,
            name = entity.subjectName,
            colorHex = entity.subjectColor,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override suspend fun updateSubject(subjectId: String, name: String, colorHex: Long) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        subjectDao.updateSubject(
            id = subjectId,
            name = trimmed,
            colorHex = colorHex,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun deleteSubject(subjectId: String) {
        subjectDao.deleteById(subjectId)
    }
}

@Singleton
class StudyTaskRepositoryImpl @Inject constructor(
    private val studyTaskDao: StudyTaskDao,
    private val subjectDao: SubjectDao
) : StudyTaskRepository {

    override fun observeAllTasks(): Flow<List<StudyTask>> {
        return combine(
            studyTaskDao.observeAllTasks(),
            subjectDao.observeAllSubjects()
        ) { tasks, subjects ->
            val subjectMap = subjects.associateBy { it.id }
            tasks.map { it.toDomain(subjectMap[it.subjectId]?.subjectName) }
        }
    }

    override fun observeCurrentActiveTask(): Flow<StudyTask?> {
        return combine(
            studyTaskDao.observeCurrentActiveTask(),
            subjectDao.observeAllSubjects()
        ) { task, subjects ->
            val subjectMap = subjects.associateBy { it.id }
            task?.toDomain(subjectMap[task.subjectId]?.subjectName)
        }
    }

    override suspend fun createTask(
        title: String,
        description: String,
        subjectId: String?,
        plannedDurationMinutes: Int,
        priority: TaskPriority,
        scheduledDate: String?
    ): StudyTask {
        val now = System.currentTimeMillis()
        val entity = StudyTaskEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            description = description.trim(),
            subjectId = subjectId,
            plannedDurationMinutes = plannedDurationMinutes.coerceIn(5, 480),
            priority = priority.name,
            status = TaskStatus.PENDING.name,
            scheduledDate = scheduledDate,
            createdAt = now,
            updatedAt = now
        )
        studyTaskDao.upsert(entity)
        val subjectName = subjectId?.let { subjectDao.getById(it)?.subjectName }
        return entity.toDomain(subjectName)
    }

    override suspend fun updateTask(
        taskId: String,
        title: String,
        description: String,
        subjectId: String?,
        plannedDurationMinutes: Int,
        priority: TaskPriority,
        scheduledDate: String?
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        studyTaskDao.updateTaskDetails(
            id = taskId,
            title = trimmed,
            description = description.trim(),
            subjectId = subjectId,
            plannedDurationMinutes = plannedDurationMinutes.coerceIn(5, 480),
            priority = priority.name,
            scheduledDate = scheduledDate,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus) {
        studyTaskDao.updateStatus(
            id = taskId,
            status = status.name,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun rescheduleTask(taskId: String, scheduledDate: String?) {
        studyTaskDao.rescheduleTask(
            id = taskId,
            scheduledDate = scheduledDate,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun rescheduleUnfinishedTasksToDate(beforeDate: String, targetDate: String): Int {
        return studyTaskDao.rescheduleUnfinishedBeforeDate(
            beforeDate = beforeDate,
            targetDate = targetDate,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun deleteTask(taskId: String) {
        studyTaskDao.deleteById(taskId)
    }

    private fun StudyTaskEntity.toDomain(subjectName: String?): StudyTask {
        return StudyTask(
            id = id,
            title = title,
            description = description,
            subjectId = subjectId,
            subjectName = subjectName,
            plannedDurationMinutes = plannedDurationMinutes,
            priority = runCatching { TaskPriority.valueOf(priority) }.getOrDefault(TaskPriority.MEDIUM),
            status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.PENDING),
            scheduledDate = scheduledDate,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val userProfileDao: UserProfileDao
) : UserProfileRepository {

    override fun observeStudentName(): Flow<String> {
        return userProfileDao.observeProfile().map { it?.studentName.orEmpty() }
    }

    override suspend fun updateStudentName(name: String) {
        userProfileDao.upsert(
            UserProfileEntity(
                id = UserProfileEntity.LOCAL_PROFILE_ID,
                studentName = name.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
