package com.focusly.app.domain.usecase

import com.focusly.app.domain.model.DailyStats
import com.focusly.app.domain.model.FocusSession
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import com.focusly.app.domain.model.WeeklyDayBreakdown
import com.focusly.app.domain.model.WeeklyStats
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object DashboardStatsCalculator {

    fun localDateFromTimestamp(
        timestampMs: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        return Instant.ofEpochMilli(timestampMs).atZone(zoneId).toLocalDate().toString()
    }

    fun calculateDailyStats(
        sessions: List<FocusSession>,
        targetMinutes: Int,
        targetDate: LocalDate = LocalDate.now()
    ): DailyStats {
        val dateString = targetDate.toString()
        val safeTarget = max(1, targetMinutes)

        val uniqueFocusSessionsForDay = sessions
            .distinctBy { it.id }
            .filter { it.type == SessionType.FOCUS && it.localDate == dateString }

        val completedSessions = uniqueFocusSessionsForDay.filter {
            it.status == SessionStatus.COMPLETED
        }
        val earlyEndedSessions = uniqueFocusSessionsForDay.filter {
            it.status == SessionStatus.ENDED_EARLY
        }

        val completedSeconds = completedSessions.sumOf { session ->
            min(max(0L, session.actualDurationSeconds), max(0L, session.plannedDurationSeconds))
        }
        val completedMinutes = (completedSeconds / 60L).toInt()
        val remainingMinutes = max(0, safeTarget - completedMinutes)
        val completionPct = ((completedSeconds.toDouble() / (safeTarget * 60.0)) * 100.0)
            .toFloat()
            .coerceIn(0f, 100f)

        return DailyStats(
            date = dateString,
            targetMinutes = safeTarget,
            completedFocusMinutes = completedMinutes,
            completedFocusSeconds = completedSeconds,
            remainingMinutes = remainingMinutes,
            completionPercentage = completionPct,
            completedSessions = completedSessions.size,
            earlyEndedSessions = earlyEndedSessions.size
        )
    }

    fun calculateWeeklyStats(
        sessions: List<FocusSession>,
        endDate: LocalDate = LocalDate.now(),
        locale: Locale = Locale.getDefault()
    ): WeeklyStats {
        val uniqueCompletedFocus = sessions
            .distinctBy { it.id }
            .filter { it.type == SessionType.FOCUS && it.status == SessionStatus.COMPLETED }

        val days = (6L downTo 0L).map { offset -> endDate.minusDays(offset) }
        val dateSet = days.map { it.toString() }.toSet()

        val sessionsInWeek = uniqueCompletedFocus.filter { it.localDate in dateSet }
        val groupedByDate = sessionsInWeek.groupBy { it.localDate }

        val breakdown = days.map { day ->
            val key = day.toString()
            val daySessions = groupedByDate[key].orEmpty()
            val daySeconds = daySessions.sumOf { s ->
                min(max(0L, s.actualDurationSeconds), max(0L, s.plannedDurationSeconds))
            }
            WeeklyDayBreakdown(
                date = key,
                dayShortLabel = day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                focusMinutes = (daySeconds / 60L).toInt(),
                completedSessions = daySessions.size
            )
        }

        val totalSeconds = sessionsInWeek.sumOf { s ->
            min(max(0L, s.actualDurationSeconds), max(0L, s.plannedDurationSeconds))
        }
        val totalMinutes = (totalSeconds / 60L).toInt()
        val count = sessionsInWeek.size
        val averageMinutes = if (count > 0) (totalMinutes / count) else 0

        return WeeklyStats(
            totalWeeklyFocusMinutes = totalMinutes,
            completedSessions = count,
            averageSessionMinutes = averageMinutes,
            dailyBreakdown = breakdown
        )
    }
}
