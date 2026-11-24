package com.focusly.app.domain.usecase

import com.focusly.app.domain.model.FocusSession
import com.focusly.app.domain.model.GardenGrowthStage
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val coinBonus: Int
)

data class StoreCatalogDefinition(
    val id: String,
    val title: String,
    val description: String,
    val costCoins: Int,
    val accentHex: Long
)

object RewardEngine {

    const val DAILY_TARGET_BONUS_COINS = 10

    val ACHIEVEMENTS_CATALOG: List<AchievementDefinition> = listOf(
        AchievementDefinition(
            id = "first_focus",
            title = "First Focus",
            description = "Complete your first focus session",
            coinBonus = 15
        ),
        AchievementDefinition(
            id = "sixty_minutes",
            title = "First Hour",
            description = "Accumulate 60 completed focus minutes",
            coinBonus = 20
        ),
        AchievementDefinition(
            id = "five_sessions",
            title = "Momentum Builder",
            description = "Complete 5 focus sessions",
            coinBonus = 25
        ),
        AchievementDefinition(
            id = "daily_target",
            title = "Daily Target Met",
            description = "Reach 100% of your daily focus goal",
            coinBonus = 25
        ),
        AchievementDefinition(
            id = "three_day_streak",
            title = "3-Day Streak",
            description = "Complete focus sessions on 3 consecutive days",
            coinBonus = 40
        ),
        AchievementDefinition(
            id = "ten_sessions",
            title = "Steady Scholar",
            description = "Complete 10 focus sessions",
            coinBonus = 50
        )
    )

    val GARDEN_STORE_CATALOG: List<StoreCatalogDefinition> = listOf(
        StoreCatalogDefinition(
            id = "sunlit_sprout",
            title = "Sunlit Sprout",
            description = "A starter desk seedling that grows with every completed session.",
            costCoins = 0,
            accentHex = 0xFF22C55EL
        ),
        StoreCatalogDefinition(
            id = "electric_lotus",
            title = "Electric Lotus",
            description = "Calm deep-navy lotus symbolizing steady concentration.",
            costCoins = 25,
            accentHex = 0xFF3B82F6L
        ),
        StoreCatalogDefinition(
            id = "cedar_fern",
            title = "Calm Cedar Fern",
            description = "Layered evergreen fronds built for long revision blocks.",
            costCoins = 40,
            accentHex = 0xFF22D3EEL
        ),
        StoreCatalogDefinition(
            id = "lantern_moss",
            title = "Desk Lantern Moss",
            description = "Warm amber study companion for evening problem solving.",
            costCoins = 65,
            accentHex = 0xFFF59E0BL
        ),
        StoreCatalogDefinition(
            id = "night_bonsai",
            title = "Night-Sky Bonsai",
            description = "Patient sculpted bonsai earned through disciplined study habits.",
            costCoins = 95,
            accentHex = 0xFF8B5CF6L
        ),
        StoreCatalogDefinition(
            id = "scholars_ginkgo",
            title = "Scholar's Ginkgo",
            description = "Golden canopy celebrating lasting academic consistency.",
            costCoins = 130,
            accentHex = 0xFFEAB308L
        )
    )

    fun calculateSessionCoins(
        plannedDurationSeconds: Long,
        actualDurationSeconds: Long,
        sessionType: SessionType,
        sessionStatus: SessionStatus
    ): Int {
        if (sessionType != SessionType.FOCUS || sessionStatus != SessionStatus.COMPLETED) {
            return 0
        }
        val validSeconds = min(max(0L, actualDurationSeconds), max(0L, plannedDurationSeconds))
        val minutes = (validSeconds / 60L).toInt()
        if (minutes <= 0) return 0
        return max(5, minutes)
    }

    fun computeGrowthStage(completedFocusSessionsCount: Int): GardenGrowthStage {
        return when {
            completedFocusSessionsCount >= 5 -> GardenGrowthStage.BLOOMING
            completedFocusSessionsCount >= 2 -> GardenGrowthStage.SPROUTING
            else -> GardenGrowthStage.SEEDLING
        }
    }

    fun computeStreaks(
        sessions: List<FocusSession>,
        todayDate: LocalDate = LocalDate.now()
    ): Pair<Int, Int> {
        val completedDates = sessions
            .distinctBy { it.id }
            .filter { it.type == SessionType.FOCUS && it.status == SessionStatus.COMPLETED }
            .mapNotNull { runCatching { LocalDate.parse(it.localDate) }.getOrNull() }
            .toSortedSet()

        if (completedDates.isEmpty()) return 0 to 0

        // Longest streak across all recorded local dates
        var longest = 0
        var run = 0
        var prevDate: LocalDate? = null
        for (date in completedDates) {
            run = if (prevDate != null && prevDate.plusDays(1) == date) {
                run + 1
            } else {
                1
            }
            if (run > longest) longest = run
            prevDate = date
        }

        // Current active streak (includes today, or yesterday if today not yet completed)
        var current = 0
        var cursor = when {
            completedDates.contains(todayDate) -> todayDate
            completedDates.contains(todayDate.minusDays(1)) -> todayDate.minusDays(1)
            else -> null
        }
        while (cursor != null && completedDates.contains(cursor)) {
            current += 1
            cursor = cursor.minusDays(1)
        }

        return current to longest
    }

    fun evaluateNewlyUnlockedAchievements(
        sessions: List<FocusSession>,
        dailyTargetMinutes: Int,
        todayDate: LocalDate,
        alreadyUnlockedIds: Set<String>
    ): List<AchievementDefinition> {
        val completedFocus = sessions
            .distinctBy { it.id }
            .filter { it.type == SessionType.FOCUS && it.status == SessionStatus.COMPLETED }

        val completedCount = completedFocus.size
        val totalMinutes = completedFocus.sumOf { s ->
            min(max(0L, s.actualDurationSeconds), max(0L, s.plannedDurationSeconds)) / 60L
        }.toInt()
        val dailyStats = DashboardStatsCalculator.calculateDailyStats(
            sessions = completedFocus,
            targetMinutes = dailyTargetMinutes,
            targetDate = todayDate
        )
        val (_, longestStreak) = computeStreaks(completedFocus, todayDate)

        val candidates = mutableListOf<String>()
        if (completedCount >= 1) candidates += "first_focus"
        if (totalMinutes >= 60) candidates += "sixty_minutes"
        if (completedCount >= 5) candidates += "five_sessions"
        if (dailyStats.completedFocusMinutes >= dailyStats.targetMinutes) candidates += "daily_target"
        if (longestStreak >= 3) candidates += "three_day_streak"
        if (completedCount >= 10) candidates += "ten_sessions"

        return ACHIEVEMENTS_CATALOG.filter { def ->
            def.id in candidates && def.id !in alreadyUnlockedIds
        }
    }
}
