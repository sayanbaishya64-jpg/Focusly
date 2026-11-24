package com.focusly.app.domain.usecase

import com.focusly.app.domain.model.ActiveTimerSnapshot
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerCalculatorAndStateMachineTest {

    @Test
    fun `elapsed time remains accurate after backgrounding without counter ticks`() {
        val startMs = 1_700_000_000_000L
        val snapshot = ActiveTimerSnapshot(
            sessionId = "s-1",
            sessionType = SessionType.FOCUS,
            plannedDurationSeconds = 25 * 60L,
            startTimestampMs = startMs,
            pauseStartedTimestampMs = null,
            accumulatedPausedMs = 0L,
            status = SessionStatus.RUNNING,
            subjectId = null,
            taskId = null
        )

        val afterTenMinutes = TimerCalculator.calculateProgress(snapshot, startMs + 600_000L)
        assertEquals(600L, afterTenMinutes.elapsedSeconds)
        assertEquals(900L, afterTenMinutes.remainingSeconds)
        assertEquals(0.4f, afterTenMinutes.progressFraction, 0.001f)
        assertFalse(afterTenMinutes.isCompleted)
    }

    @Test
    fun `pause and resume exclude paused duration from elapsed focus time`() {
        val startMs = 1_700_000_000_000L
        val running = ActiveTimerSnapshot(
            sessionId = "s-2",
            sessionType = SessionType.FOCUS,
            plannedDurationSeconds = 1500L,
            startTimestampMs = startMs,
            pauseStartedTimestampMs = null,
            accumulatedPausedMs = 0L,
            status = SessionStatus.RUNNING,
            subjectId = null,
            taskId = null
        )

        val paused = SessionStateMachine.pause(running, startMs + 300_000L)
        val whilePaused = TimerCalculator.calculateProgress(paused, startMs + 900_000L)
        assertEquals(300L, whilePaused.elapsedSeconds)
        assertEquals(1200L, whilePaused.remainingSeconds)

        val resumed = SessionStateMachine.resume(paused, startMs + 900_000L)
        val afterResume = TimerCalculator.calculateProgress(resumed, startMs + 1_200_000L)
        assertEquals(600L, afterResume.elapsedSeconds)
        assertEquals(900L, afterResume.remainingSeconds)
    }

    @Test
    fun `completion triggers when elapsed reaches planned duration`() {
        val startMs = 1_700_000_000_000L
        val snapshot = ActiveTimerSnapshot(
            sessionId = "s-3",
            sessionType = SessionType.FOCUS,
            plannedDurationSeconds = 900L,
            startTimestampMs = startMs,
            pauseStartedTimestampMs = null,
            accumulatedPausedMs = 0L,
            status = SessionStatus.RUNNING,
            subjectId = null,
            taskId = null
        )

        val completed = TimerCalculator.calculateProgress(snapshot, startMs + 905_000L)
        assertEquals(900L, completed.elapsedSeconds)
        assertEquals(0L, completed.remainingSeconds)
        assertTrue(completed.isCompleted)
    }

    @Test
    fun `state machine rejects invalid transitions`() {
        assertFalse(SessionStateMachine.canTransition(SessionStatus.IDLE, SessionStatus.PAUSED))
        assertFalse(SessionStateMachine.canTransition(SessionStatus.PAUSED, SessionStatus.PAUSED))
        assertTrue(SessionStateMachine.canTransition(SessionStatus.RUNNING, SessionStatus.ENDED_EARLY))
    }
}
