package com.focusly.app.domain.usecase

import com.focusly.app.domain.model.FocusSession
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DashboardStatsCalculatorTest {

    private fun buildSession(
        id: String,
        date: LocalDate,
        actualSeconds: Long,
        plannedSeconds: Long = actualSeconds,
        status: SessionStatus = SessionStatus.COMPLETED,
        type: SessionType = SessionType.FOCUS
    ) = FocusSession(
        id = id,
        plannedDurationSeconds = plannedSeconds,
        actualDurationSeconds = actualSeconds,
        startTimestamp = 1_700_000_000_000L,
        completionTimestamp = 1_700_001_500_000L,
        pausedDurationSeconds = 0L,
        status = status,
        type = type,
        localDate = date.toString(),
        subjectId = null,
        taskId = null,
        createdAt = 1_700_000_000_000L
    )

    @Test
    fun `daily stats exclude break sessions early ended sessions and duplicate ids`() {
        val today = LocalDate.of(2026, 11, 24)
        val sessions = listOf(
            buildSession("1", today, 1500L),
            buildSession("1", today, 1500L), // duplicate ID
            buildSession("2", today, 1200L),
            buildSession("3", today, 600L, status = SessionStatus.ENDED_EARLY),
            buildSession("4", today, 300L, type = SessionType.BREAK)
        )

        val stats = DashboardStatsCalculator.calculateDailyStats(
            sessions = sessions,
            targetMinutes = 120,
            targetDate = today
        )

        assertEquals(45, stats.completedFocusMinutes)
        assertEquals(75, stats.remainingMinutes)
        assertEquals(37.5f, stats.completionPercentage, 0.01f)
        assertEquals(2, stats.completedSessions)
        assertEquals(1, stats.earlyEndedSessions)
    }

    @Test
    fun `daily progress resets on next local calendar day`() {
        val day1 = LocalDate.of(2026, 11, 24)
        val day2 = LocalDate.of(2026, 11, 25)
        val sessions = listOf(
            buildSession("1", day1, 2700L)
        )

        val day2Stats = DashboardStatsCalculator.calculateDailyStats(
            sessions = sessions,
            targetMinutes = 60,
            targetDate = day2
        )

        assertEquals(0, day2Stats.completedFocusMinutes)
        assertEquals(60, day2Stats.remainingMinutes)
        assertEquals(0, day2Stats.completedSessions)
    }

    @Test
    fun `weekly stats compute 7 day totals and averages accurately`() {
        val endDay = LocalDate.of(2026, 11, 24)
        val sessions = listOf(
            buildSession("a", endDay, 1500L),
            buildSession("b", endDay.minusDays(1), 2700L),
            buildSession("c", endDay.minusDays(8), 3600L), // outside 7-day window
            buildSession("d", endDay, 600L, type = SessionType.BREAK)
        )

        val weekly = DashboardStatsCalculator.calculateWeeklyStats(sessions, endDate = endDay)
        assertEquals(70, weekly.totalWeeklyFocusMinutes)
        assertEquals(2, weekly.completedSessions)
        assertEquals(35, weekly.averageSessionMinutes)
        assertEquals(7, weekly.dailyBreakdown.size)
    }

    @Test
    fun `local date uses explicit timezone cleanly across midnight`() {
        // 2026-11-24T20:00:00Z = 2026-11-25T02:00:00+06:00 (Dhaka/Kolkata next day)
        val utcEpochMs = 1795550400_000L
        val utcDate = DashboardStatsCalculator.localDateFromTimestamp(utcEpochMs, ZoneId.of("UTC"))
        val dhakaDate = DashboardStatsCalculator.localDateFromTimestamp(utcEpochMs, ZoneId.of("Asia/Dhaka"))
        assertEquals("2026-11-24", utcDate)
        assertEquals("2026-11-25", dhakaDate)
    }
}
