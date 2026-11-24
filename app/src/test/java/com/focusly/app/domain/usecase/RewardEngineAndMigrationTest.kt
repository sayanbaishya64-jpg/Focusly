package com.focusly.app.domain.usecase

import com.focusly.app.data.local.database.FocuslyDatabase
import com.focusly.app.domain.model.FocusSession
import com.focusly.app.domain.model.GardenGrowthStage
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RewardEngineAndMigrationTest {

    private fun focusSession(
        id: String,
        date: String,
        plannedMinutes: Int = 25,
        actualMinutes: Int = plannedMinutes,
        status: SessionStatus = SessionStatus.COMPLETED,
        type: SessionType = SessionType.FOCUS
    ): FocusSession {
        return FocusSession(
            id = id,
            plannedDurationSeconds = plannedMinutes * 60L,
            actualDurationSeconds = actualMinutes * 60L,
            startTimestamp = 1_000L,
            completionTimestamp = 2_000L,
            pausedDurationSeconds = 0L,
            status = status,
            type = type,
            localDate = date,
            subjectId = null,
            taskId = null,
            createdAt = 1_000L
        )
    }

    @Test
    fun `only genuinely completed focus sessions earn coins`() {
        assertEquals(
            25,
            RewardEngine.calculateSessionCoins(25 * 60L, 25 * 60L, SessionType.FOCUS, SessionStatus.COMPLETED)
        )
        assertEquals(
            5,
            RewardEngine.calculateSessionCoins(5 * 60L, 5 * 60L, SessionType.FOCUS, SessionStatus.COMPLETED)
        )
        assertEquals(
            0,
            RewardEngine.calculateSessionCoins(25 * 60L, 12 * 60L, SessionType.FOCUS, SessionStatus.ENDED_EARLY)
        )
        assertEquals(
            0,
            RewardEngine.calculateSessionCoins(10 * 60L, 10 * 60L, SessionType.BREAK, SessionStatus.COMPLETED)
        )
    }

    @Test
    fun `streaks and achievement milestones unlock strictly from completed focus sessions`() {
        val today = LocalDate.of(2026, 11, 24)
        val sessions = listOf(
            focusSession("s1", "2026-11-22", 25),
            focusSession("s2", "2026-11-23", 25),
            focusSession("s3", "2026-11-24", 25),
            focusSession("s4", "2026-11-24", 35),
            focusSession("s5", "2026-11-24", 25, status = SessionStatus.ENDED_EARLY)
        )

        val (currentStreak, longestStreak) = RewardEngine.computeStreaks(sessions, today)
        assertEquals(3, currentStreak)
        assertEquals(3, longestStreak)

        val unlocked = RewardEngine.evaluateNewlyUnlockedAchievements(
            sessions = sessions,
            dailyTargetMinutes = 60,
            todayDate = today,
            alreadyUnlockedIds = emptySet()
        ).map { it.id }.toSet()

        assertTrue("first_focus" in unlocked)
        assertTrue("sixty_minutes" in unlocked)
        assertTrue("daily_target" in unlocked)
        assertTrue("three_day_streak" in unlocked)
        assertTrue("five_sessions" !in unlocked) // s5 ended early, only 4 completed
    }

    @Test
    fun `virtual garden grows according to completed focus session count`() {
        assertEquals(GardenGrowthStage.SEEDLING, RewardEngine.computeGrowthStage(0))
        assertEquals(GardenGrowthStage.SEEDLING, RewardEngine.computeGrowthStage(1))
        assertEquals(GardenGrowthStage.SPROUTING, RewardEngine.computeGrowthStage(2))
        assertEquals(GardenGrowthStage.SPROUTING, RewardEngine.computeGrowthStage(4))
        assertEquals(GardenGrowthStage.BLOOMING, RewardEngine.computeGrowthStage(5))
    }

    @Test
    fun `database version is bumped to 4 with explicit non-destructive MIGRATION_3_4`() {
        assertEquals(3, FocuslyDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, FocuslyDatabase.MIGRATION_3_4.endVersion)
        assertTrue(FocuslyDatabase.ALL_MIGRATIONS.contains(FocuslyDatabase.MIGRATION_3_4))
    }
}
