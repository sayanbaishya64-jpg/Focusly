package com.focusly.app.domain.usecase

import com.focusly.app.core.model.AppLanguage
import com.focusly.app.core.model.OnboardingPreferences
import com.focusly.app.core.model.StudentCategory
import com.focusly.app.core.model.StudyGoal
import com.focusly.app.domain.model.FocusSession
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import com.focusly.app.domain.model.StudyTask
import com.focusly.app.domain.model.TaskPriority
import com.focusly.app.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DailyMissionsAndBackupTest {

    @Test
    fun `daily missions evaluate session, completed task, and daily goal accurately`() {
        val today = LocalDate.of(2026, 11, 24)
        val todayNoonMs = today.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val sessions = listOf(
            FocusSession(
                id = "s1",
                plannedDurationSeconds = 60 * 60L,
                actualDurationSeconds = 60 * 60L,
                startTimestamp = todayNoonMs - 3600_000L,
                completionTimestamp = todayNoonMs,
                pausedDurationSeconds = 0L,
                status = SessionStatus.COMPLETED,
                type = SessionType.FOCUS,
                localDate = today.toString(),
                subjectId = null,
                taskId = null,
                createdAt = todayNoonMs
            )
        )
        val tasks = listOf(
            StudyTask(
                id = "t1",
                title = "Revise Calculus",
                description = "",
                subjectId = null,
                subjectName = null,
                plannedDurationMinutes = 30,
                priority = TaskPriority.HIGH,
                status = TaskStatus.COMPLETED,
                scheduledDate = today.toString(),
                createdAt = todayNoonMs,
                updatedAt = todayNoonMs
            )
        )

        val missions = DailyMissionsEngine.evaluateDailyMissions(
            sessions = sessions,
            tasks = tasks,
            dailyTargetMinutes = 60,
            todayDate = today,
            claimedMissionIds = setOf(DailyMissionsEngine.MISSION_FIRST_SESSION)
        )

        val first = missions.first { it.id == DailyMissionsEngine.MISSION_FIRST_SESSION }
        val taskMission = missions.first { it.id == DailyMissionsEngine.MISSION_DAILY_TASK }
        val goalMission = missions.first { it.id == DailyMissionsEngine.MISSION_DAILY_GOAL }

        assertTrue(first.isCompleted)
        assertTrue(first.isClaimed)
        assertTrue(taskMission.isCompleted)
        assertFalse(taskMission.isClaimed)
        assertTrue(goalMission.isCompleted)
        assertEquals(100, goalMission.currentProgress)
    }

    @Test
    fun `auto schedule selects in-progress or highest priority today or overdue task`() {
        val today = LocalDate.of(2026, 11, 24)
        val tasks = listOf(
            StudyTask("low", "Low future", "", null, null, 20, TaskPriority.LOW, TaskStatus.PENDING, "2026-11-26", 1L, 1L),
            StudyTask("med", "Medium today", "", null, null, 25, TaskPriority.MEDIUM, TaskStatus.PENDING, "2026-11-24", 2L, 2L),
            StudyTask("high", "High overdue", "", null, null, 45, TaskPriority.HIGH, TaskStatus.PENDING, "2026-11-23", 3L, 3L)
        )

        val picked = DailyMissionsEngine.selectAutoScheduleTask(tasks, today)
        assertNotNull(picked)
        assertEquals("high", picked?.id)
    }

    @Test
    fun `backup codec round-trips payload and rejects unsupported schema version`() {
        val payload = FocuslyBackupPayload(
            schemaVersion = 1,
            exportedAt = 1_700_000_000_000L,
            preferences = OnboardingPreferences(
                onboardingCompleted = true,
                studentCategory = StudentCategory.COMPETITIVE_ASPIRANT,
                studyGoals = setOf(StudyGoal.BUILD_CONSISTENCY),
                dailyFocusTargetMinutes = 90,
                language = AppLanguage.BENGALI,
                selectedDistractionApps = setOf("com.instagram.android"),
                distractionPreferencesSkipped = false
            ),
            studentName = "Aarav",
            subjects = listOf(BackupSubjectPayload("sub1", "Physics", 0xFF3B82F6L, 1L, 1L)),
            tasks = listOf(BackupTaskPayload("t1", "Optics", "", "sub1", 45, "HIGH", "PENDING", "2026-11-24", 1L, 1L)),
            sessions = listOf(
                BackupSessionPayload("s1", 1500L, 1500L, 100L, 1600L, 0L, "COMPLETED", "FOCUS", "2026-11-24", "sub1", "t1", 100L)
            ),
            rewardTransactions = emptyList(),
            unlockedAchievements = emptyList(),
            unlockedGardenItems = emptyList()
        )

        val encoded = FocuslyBackupCodec.encode(payload)
        val decoded = FocuslyBackupCodec.decode(encoded)
        assertTrue(decoded.isSuccess)
        val restored = decoded.getOrThrow()
        assertEquals("Aarav", restored.studentName)
        assertEquals(90, restored.preferences.dailyFocusTargetMinutes)
        assertEquals(1, restored.sessions.size)

        val invalid = FocuslyBackupCodec.decode("""{"schemaVersion":99,"preferences":{}}""")
        assertTrue(invalid.isFailure)
    }
}
