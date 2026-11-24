package com.focusly.app.domain.usecase

import com.focusly.app.domain.model.ActiveTimerSnapshot
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.TimerProgress
import kotlin.math.max
import kotlin.math.min

object TimerCalculator {

    fun calculateProgress(
        snapshot: ActiveTimerSnapshot,
        nowMs: Long
    ): TimerProgress {
        val planned = max(1L, snapshot.plannedDurationSeconds)
        val effectiveNowMs = when (snapshot.status) {
            SessionStatus.PAUSED -> snapshot.pauseStartedTimestampMs ?: nowMs
            else -> nowMs
        }
        val rawElapsedMs = max(
            0L,
            (effectiveNowMs - snapshot.startTimestampMs) - snapshot.accumulatedPausedMs
        )
        val rawElapsedSeconds = rawElapsedMs / 1000L
        val clampedElapsedSeconds = min(planned, rawElapsedSeconds)
        val remainingSeconds = max(0L, planned - clampedElapsedSeconds)
        val fraction = (clampedElapsedSeconds.toFloat() / planned.toFloat()).coerceIn(0f, 1f)

        return TimerProgress(
            elapsedSeconds = clampedElapsedSeconds,
            remainingSeconds = remainingSeconds,
            progressFraction = fraction,
            isCompleted = snapshot.status == SessionStatus.RUNNING && remainingSeconds == 0L
        )
    }

    fun formatDuration(totalSeconds: Long): String {
        val safe = max(0L, totalSeconds)
        val hours = safe / 3600L
        val minutes = (safe % 3600L) / 60L
        val seconds = safe % 60L
        return if (hours > 0L) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }
}

object SessionStateMachine {

    fun canTransition(from: SessionStatus, to: SessionStatus): Boolean {
        return when (from) {
            SessionStatus.IDLE -> to == SessionStatus.RUNNING
            SessionStatus.RUNNING -> to == SessionStatus.PAUSED ||
                to == SessionStatus.COMPLETED ||
                to == SessionStatus.ENDED_EARLY
            SessionStatus.PAUSED -> to == SessionStatus.RUNNING ||
                to == SessionStatus.ENDED_EARLY ||
                to == SessionStatus.COMPLETED
            SessionStatus.COMPLETED,
            SessionStatus.ENDED_EARLY -> to == SessionStatus.IDLE || to == SessionStatus.RUNNING
        }
    }

    fun pause(snapshot: ActiveTimerSnapshot, nowMs: Long): ActiveTimerSnapshot {
        check(canTransition(snapshot.status, SessionStatus.PAUSED)) {
            "Invalid state transition from ${snapshot.status} to PAUSED"
        }
        return snapshot.copy(
            status = SessionStatus.PAUSED,
            pauseStartedTimestampMs = nowMs
        )
    }

    fun resume(snapshot: ActiveTimerSnapshot, nowMs: Long): ActiveTimerSnapshot {
        check(canTransition(snapshot.status, SessionStatus.RUNNING)) {
            "Invalid state transition from ${snapshot.status} to RUNNING"
        }
        val pauseDuration = snapshot.pauseStartedTimestampMs?.let { max(0L, nowMs - it) } ?: 0L
        return snapshot.copy(
            status = SessionStatus.RUNNING,
            pauseStartedTimestampMs = null,
            accumulatedPausedMs = snapshot.accumulatedPausedMs + pauseDuration
        )
    }
}
