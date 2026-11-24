package com.focusly.app.domain.usecase

import com.focusly.app.domain.model.DailyMissionItem
import com.focusly.app.domain.model.FocusSession
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import com.focusly.app.domain.model.StudyTask
import com.focusly.app.domain.model.TaskPriority
import com.focusly.app.domain.model.TaskStatus
import java.time.LocalDate

data class DailyMissionDefinition(
    val id: String,
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val targetProgress: Int
)

object DailyMissionsEngine {

    const val MISSION_FIRST_SESSION = "mission_first_session"
    const val MISSION_DAILY_TASK = "mission_daily_task"
    const val MISSION_DAILY_GOAL = "mission_daily_goal"

    val MISSIONS_CATALOG: List<DailyMissionDefinition> = listOf(
        DailyMissionDefinition(
            id = MISSION_FIRST_SESSION,
            title = "First Focus Block",
            description = "Complete 1 focus session today",
            rewardCoins = 5,
            targetProgress = 1
        ),
        DailyMissionDefinition(
            id = MISSION_DAILY_TASK,
            title = "Finish a Study Task",
            description = "Mark 1 planned study task completed today",
            rewardCoins = 5,
            targetProgress = 1
        ),
        DailyMissionDefinition(
            id = MISSION_DAILY_GOAL,
            title = "Hit Daily Target",
            description = "Reach 100% of today's focus minute goal",
            rewardCoins = 10,
            targetProgress = 100
        )
    )

    fun evaluateDailyMissions(
        sessions: List<FocusSession>,
        tasks: List<StudyTask>,
        dailyTargetMinutes: Int,
        todayDate: LocalDate,
        claimedMissionIds: Set<String>
    ): List<DailyMissionItem> {
        val todayStr = todayDate.toString()
        val dailyStats = DashboardStatsCalculator.calculateDailyStats(
            sessions = sessions,
            targetMinutes = dailyTargetMinutes,
            targetDate = todayDate
        )
        val completedSessionsToday = sessions
            .distinctBy { it.id }
            .count {
                it.type == SessionType.FOCUS &&
                    it.status == SessionStatus.COMPLETED &&
                    it.localDate == todayStr
            }

        val completedTasksToday = tasks.count { task ->
            task.status == TaskStatus.COMPLETED &&
                DashboardStatsCalculator.localDateFromTimestamp(task.updatedAt) == todayStr
        }

        val goalProgressPct = dailyStats.completionPercentage.toInt().coerceIn(0, 100)

        return MISSIONS_CATALOG.map { def ->
            val currentProgress = when (def.id) {
                MISSION_FIRST_SESSION -> completedSessionsToday.coerceAtMost(def.targetProgress)
                MISSION_DAILY_TASK -> completedTasksToday.coerceAtMost(def.targetProgress)
                MISSION_DAILY_GOAL -> goalProgressPct
                else -> 0
            }
            val isCompleted = when (def.id) {
                MISSION_DAILY_GOAL -> dailyStats.completedFocusMinutes >= dailyStats.targetMinutes
                else -> currentProgress >= def.targetProgress
            }
            DailyMissionItem(
                id = def.id,
                title = def.title,
                description = def.description,
                rewardCoins = def.rewardCoins,
                currentProgress = currentProgress,
                targetProgress = def.targetProgress,
                isCompleted = isCompleted,
                isClaimed = def.id in claimedMissionIds
            )
        }
    }

    fun selectAutoScheduleTask(
        tasks: List<StudyTask>,
        todayDate: LocalDate
    ): StudyTask? {
        val todayStr = todayDate.toString()
        val activeTasks = tasks.filter { it.status != TaskStatus.COMPLETED }
        if (activeTasks.isEmpty()) return null

        val priorityRank: (TaskPriority) -> Int = { priority ->
            when (priority) {
                TaskPriority.HIGH -> 0
                TaskPriority.MEDIUM -> 1
                TaskPriority.LOW -> 2
            }
        }

        val todayOrOverdue = activeTasks.filter {
            it.scheduledDate != null && it.scheduledDate <= todayStr
        }
        val pool = if (todayOrOverdue.isNotEmpty()) todayOrOverdue else activeTasks

        return pool.minWithOrNull(
            compareBy<StudyTask>(
                { if (it.status == TaskStatus.IN_PROGRESS) 0 else 1 },
                { priorityRank(it.priority) },
                { it.scheduledDate ?: "9999-12-31" },
                { -it.updatedAt }
            )
        )
    }
}
