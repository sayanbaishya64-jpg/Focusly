package com.focusly.app.domain.usecase

import com.focusly.app.domain.model.AnalyticsPeriod
import com.focusly.app.domain.model.FocusSession
import com.focusly.app.domain.model.ProgressAnalyticsSummary
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import com.focusly.app.domain.model.StudyScheduleFilter
import com.focusly.app.domain.model.StudyTask
import com.focusly.app.domain.model.Subject
import com.focusly.app.domain.model.SubjectProgressSummary
import com.focusly.app.domain.model.TaskPriority
import com.focusly.app.domain.model.TaskStatus
import com.focusly.app.domain.model.WeeklyDayBreakdown
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object StudyAnalyticsCalculator {

    private const val GENERAL_STUDY_COLOR = 0xFF22D3EE

    fun findOverdueUnfinishedTasks(
        tasks: List<StudyTask>,
        todayDate: String = LocalDate.now().toString()
    ): List<StudyTask> {
        return tasks.filter { task ->
            task.status != TaskStatus.COMPLETED &&
                !task.scheduledDate.isNullOrBlank() &&
                task.scheduledDate < todayDate
        }
    }

    fun filterAndSortTasks(
        tasks: List<StudyTask>,
        filter: StudyScheduleFilter,
        subjectFilterId: String? = null,
        todayDate: String = LocalDate.now().toString()
    ): List<StudyTask> {
        val bySubject = if (subjectFilterId != null) {
            tasks.filter { it.subjectId == subjectFilterId }
        } else {
            tasks
        }

        val bySchedule = when (filter) {
            StudyScheduleFilter.TODAY -> bySubject.filter {
                it.status != TaskStatus.COMPLETED &&
                    (it.scheduledDate == todayDate || it.scheduledDate == null)
            }
            StudyScheduleFilter.UPCOMING -> bySubject.filter {
                it.status != TaskStatus.COMPLETED &&
                    it.scheduledDate != null &&
                    it.scheduledDate > todayDate
            }
            StudyScheduleFilter.ALL -> bySubject.filter { it.status != TaskStatus.COMPLETED }
            StudyScheduleFilter.COMPLETED -> bySubject.filter { it.status == TaskStatus.COMPLETED }
        }

        return bySchedule.sortedWith(
            compareBy<StudyTask>(
                { it.status == TaskStatus.COMPLETED },
                { priorityRank(it.priority) },
                { it.scheduledDate ?: "9999-99-99" },
                { -it.updatedAt }
            )
        )
    }

    private fun priorityRank(priority: TaskPriority): Int {
        return when (priority) {
            TaskPriority.HIGH -> 0
            TaskPriority.MEDIUM -> 1
            TaskPriority.LOW -> 2
        }
    }

    fun calculateProgressAnalytics(
        sessions: List<FocusSession>,
        subjects: List<Subject>,
        period: AnalyticsPeriod,
        endDate: LocalDate = LocalDate.now(),
        locale: Locale = Locale.getDefault(),
        generalStudyLabel: String = "General Study"
    ): ProgressAnalyticsSummary {
        val daysCount = period.days
        val days = ((daysCount - 1).toLong() downTo 0L).map { offset -> endDate.minusDays(offset) }
        val dateSet = days.map { it.toString() }.toSet()

        val uniqueFocusInRange = sessions
            .distinctBy { it.id }
            .filter { it.type == SessionType.FOCUS && it.localDate in dateSet }

        val completed = uniqueFocusInRange.filter { it.status == SessionStatus.COMPLETED }
        val earlyEnded = uniqueFocusInRange.filter { it.status == SessionStatus.ENDED_EARLY }
        val totalAttempts = completed.size + earlyEnded.size

        val completionRate = if (totalAttempts > 0) {
            ((completed.size.toDouble() / totalAttempts.toDouble()) * 1000.0).roundToInt() / 10f
        } else {
            0f
        }

        val totalCompletedSeconds = completed.sumOf { s ->
            min(max(0L, s.actualDurationSeconds), max(0L, s.plannedDurationSeconds))
        }
        val totalFocusMinutes = (totalCompletedSeconds / 60L).toInt()
        val averageSessionMinutes = if (completed.isNotEmpty()) {
            totalFocusMinutes / completed.size
        } else {
            0
        }

        val groupedByDate = completed.groupBy { it.localDate }
        val dailyBreakdown = days.map { day ->
            val key = day.toString()
            val daySessions = groupedByDate[key].orEmpty()
            val daySeconds = daySessions.sumOf { s ->
                min(max(0L, s.actualDurationSeconds), max(0L, s.plannedDurationSeconds))
            }
            val label = if (period == AnalyticsPeriod.WEEK) {
                day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            } else {
                day.dayOfMonth.toString()
            }
            WeeklyDayBreakdown(
                date = key,
                dayShortLabel = label,
                focusMinutes = (daySeconds / 60L).toInt(),
                completedSessions = daySessions.size
            )
        }

        val activeDaysCount = dailyBreakdown.count { it.focusMinutes > 0 }

        val subjectLookup = subjects.associateBy { it.id }
        val subjectGroups = completed.groupBy { session ->
            if (session.subjectId != null && subjectLookup.containsKey(session.subjectId)) {
                session.subjectId
            } else {
                null
            }
        }

        val subjectBreakdown = subjectGroups.map { (subjId, groupSessions) ->
            val subj = subjId?.let { subjectLookup[it] }
            val subjSeconds = groupSessions.sumOf { s ->
                min(max(0L, s.actualDurationSeconds), max(0L, s.plannedDurationSeconds))
            }
            val mins = (subjSeconds / 60L).toInt()
            val sharePct = if (totalFocusMinutes > 0) {
                ((mins.toDouble() / totalFocusMinutes.toDouble()) * 1000.0).roundToInt() / 10f
            } else {
                0f
            }
            SubjectProgressSummary(
                subjectId = subjId,
                subjectName = subj?.name ?: generalStudyLabel,
                colorHex = subj?.colorHex ?: GENERAL_STUDY_COLOR,
                focusMinutes = mins,
                completedSessions = groupSessions.size,
                sharePercentage = sharePct
            )
        }.sortedWith(
            compareByDescending<SubjectProgressSummary> { it.focusMinutes }
                .thenByDescending { it.completedSessions }
        )

        return ProgressAnalyticsSummary(
            period = period,
            totalFocusMinutes = totalFocusMinutes,
            completedSessions = completed.size,
            earlyEndedSessions = earlyEnded.size,
            completionRatePercent = completionRate,
            averageSessionMinutes = averageSessionMinutes,
            activeDaysCount = activeDaysCount,
            dailyBreakdown = dailyBreakdown,
            subjectBreakdown = subjectBreakdown
        )
    }
}
