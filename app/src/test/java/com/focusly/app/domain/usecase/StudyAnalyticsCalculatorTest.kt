package com.focusly.app.domain.usecase

import com.focusly.app.data.local.database.FocuslyDatabase
import com.focusly.app.domain.model.AnalyticsPeriod
import com.focusly.app.domain.model.FocusSession
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import com.focusly.app.domain.model.StudyScheduleFilter
import com.focusly.app.domain.model.StudyTask
import com.focusly.app.domain.model.Subject
import com.focusly.app.domain.model.TaskPriority
import com.focusly.app.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StudyAnalyticsCalculatorTest {

    private fun buildTask(
        id: String,
        priority: TaskPriority = TaskPriority.MEDIUM,
        status: TaskStatus = TaskStatus.PENDING,
        scheduledDate: String? = "2026-11-24",
        subjectId: String? = null
    ) = StudyTask(
        id = id,
        title = "Task $id",
        description = "",
        subjectId = subjectId,
        subjectName = null,
        plannedDurationMinutes = 30,
        priority = priority,
        status = status,
        scheduledDate = scheduledDate,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private fun buildSession(
        id: String,
        localDate: String,
        plannedMin: Int,
        actualMin: Int,
        status: SessionStatus,
        subjectId: String? = null,
        type: SessionType = SessionType.FOCUS
    ) = FocusSession(
        id = id,
        plannedDurationSeconds = plannedMin * 60L,
        actualDurationSeconds = actualMin * 60L,
        startTimestamp = 1_700_000_000_000L,
        completionTimestamp = 1_700_001_500_000L,
        pausedDurationSeconds = 0L,
        status = status,
        type = type,
        localDate = localDate,
        subjectId = subjectId,
        taskId = null,
        createdAt = 1_700_000_000_000L
    )

    @Test
    fun overdueDetection_identifiesOnlyUnfinishedTasksBeforeToday() {
        val today = "2026-11-24"
        val tasks = listOf(
            buildTask("overdue-pending", status = TaskStatus.PENDING, scheduledDate = "2026-11-22"),
            buildTask("overdue-completed", status = TaskStatus.COMPLETED, scheduledDate = "2026-11-22"),
            buildTask("today-task", status = TaskStatus.PENDING, scheduledDate = "2026-11-24"),
            buildTask("unscheduled-task", status = TaskStatus.PENDING, scheduledDate = null)
        )

        val overdue = StudyAnalyticsCalculator.findOverdueUnfinishedTasks(tasks, today)
        assertEquals(1, overdue.size)
        assertEquals("overdue-pending", overdue.first().id)
    }

    @Test
    fun scheduleFiltering_ordersByPriorityHighFirst() {
        val today = "2026-11-24"
        val tasks = listOf(
            buildTask("low", priority = TaskPriority.LOW, scheduledDate = today),
            buildTask("high", priority = TaskPriority.HIGH, scheduledDate = today),
            buildTask("upcoming", priority = TaskPriority.HIGH, scheduledDate = "2026-11-25")
        )

        val todayList = StudyAnalyticsCalculator.filterAndSortTasks(tasks, StudyScheduleFilter.TODAY, null, today)
        assertEquals(listOf("high", "low"), todayList.map { it.id })

        val upcomingList = StudyAnalyticsCalculator.filterAndSortTasks(tasks, StudyScheduleFilter.UPCOMING, null, today)
        assertEquals(listOf("upcoming"), upcomingList.map { it.id })
    }

    @Test
    fun progressAnalytics_computesCompletionRateAndSubjectBreakdown() {
        val endDate = LocalDate.of(2026, 11, 24)
        val math = Subject("s-math", "Mathematics", 0xFF3B82F6L, 1L, 1L)
        val sessions = listOf(
            buildSession("1", "2026-11-24", 45, 45, SessionStatus.COMPLETED, subjectId = "s-math"),
            buildSession("2", "2026-11-23", 25, 25, SessionStatus.COMPLETED, subjectId = null),
            buildSession("3", "2026-11-23", 60, 15, SessionStatus.ENDED_EARLY, subjectId = "s-math"),
            buildSession("break1", "2026-11-24", 5, 5, SessionStatus.COMPLETED, type = SessionType.BREAK),
            // Duplicate session id should be deduplicated
            buildSession("1", "2026-11-24", 45, 45, SessionStatus.COMPLETED, subjectId = "s-math")
        )

        val summary = StudyAnalyticsCalculator.calculateProgressAnalytics(
            sessions = sessions,
            subjects = listOf(math),
            period = AnalyticsPeriod.WEEK,
            endDate = endDate,
            generalStudyLabel = "General Study"
        )

        assertEquals(70, summary.totalFocusMinutes)
        assertEquals(2, summary.completedSessions)
        assertEquals(1, summary.earlyEndedSessions)
        assertEquals(35, summary.averageSessionMinutes)
        assertEquals(2, summary.activeDaysCount)
        assertEquals(66.7f, summary.completionRatePercent, 0.1f)
        assertEquals("Mathematics", summary.subjectBreakdown[0].subjectName)
        assertEquals(45, summary.subjectBreakdown[0].focusMinutes)
        assertEquals("General Study", summary.subjectBreakdown[1].subjectName)
    }

    @Test
    fun databaseMigration2To3_isRegistered() {
        assertEquals(2, FocuslyDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, FocuslyDatabase.MIGRATION_2_3.endVersion)
        assertTrue(FocuslyDatabase.ALL_MIGRATIONS.any { it.startVersion == 2 && it.endVersion == 3 })
    }
}
