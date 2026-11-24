package com.focusly.app.domain.usecase

import com.focusly.app.domain.model.FocusRecommendation
import com.focusly.app.domain.model.FocusSession
import com.focusly.app.domain.model.RecommendationConfidence
import com.focusly.app.domain.model.RecommendationReasonType
import com.focusly.app.domain.model.RecommendationStatus
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Transparent, deterministic, offline rule-based Adaptive Focus Engine (FAFE V1).
 * Uses local focus session history only. Does not claim machine learning.
 */
object AdaptiveFocusEngine {

    const val MIN_SESSIONS_FOR_RECOMMENDATION = 3

    fun generateRecommendation(
        sessions: List<FocusSession>,
        dailyTargetMinutes: Int,
        preferredSubjectId: String? = null,
        nowMs: Long = System.currentTimeMillis()
    ): FocusRecommendation {
        val uniqueFocusSessions = sessions
            .distinctBy { it.id }
            .filter { it.type == SessionType.FOCUS && (it.status == SessionStatus.COMPLETED || it.status == SessionStatus.ENDED_EARLY) }
            .sortedByDescending { it.startTimestamp }

        val count = uniqueFocusSessions.size
        val safeTarget = dailyTargetMinutes.coerceIn(10, 480)

        if (count < MIN_SESSIONS_FOR_RECOMMENDATION) {
            val starterMinutes = safeTarget.coerceIn(15, 25)
            return FocusRecommendation(
                id = "rec-insufficient-$count",
                recommendedMinutes = starterMinutes,
                reasonType = RecommendationReasonType.INSUFFICIENT_DATA,
                reasonText = "Complete at least 3 focus sessions so FOCUSLY can suggest realistic durations from your local study history.",
                confidence = RecommendationConfidence.INSUFFICIENT_DATA,
                hasEnoughData = false,
                basedOnSessionCount = count,
                preferredPeriod = null,
                subjectId = preferredSubjectId,
                status = RecommendationStatus.ACTIVE,
                generatedAt = nowMs
            )
        }

        val preferredPeriod = detectPreferredStudyPeriod(uniqueFocusSessions)

        // Optional subject-specific slice when enough history exists for that subject
        val subjectSessions = if (preferredSubjectId != null) {
            uniqueFocusSessions.filter { it.subjectId == preferredSubjectId }
        } else {
            emptyList()
        }
        val activeHistory = if (subjectSessions.size >= 3) subjectSessions else uniqueFocusSessions
        val scopedSubjectId = if (subjectSessions.size >= 3) preferredSubjectId else null

        val recentThree = activeHistory.take(3)
        val recentEarlyCount = recentThree.count { it.status == SessionStatus.ENDED_EARLY }
        val recentFive = activeHistory.take(5)
        val recentCompletionRate = recentFive.count { it.status == SessionStatus.COMPLETED }.toFloat() / recentFive.size

        // Rule 1: Recent struggle / consecutive interruptions -> gentle shorter block
        if (recentEarlyCount >= 2 || recentCompletionRate < 0.45f) {
            val completedActualMins = activeHistory
                .filter { it.status == SessionStatus.COMPLETED }
                .map { (it.actualDurationSeconds / 60L).toInt().coerceAtLeast(5) }
            val fallbackMins = if (completedActualMins.isNotEmpty()) {
                roundToNearestFive(completedActualMins.min()).coerceIn(10, 20)
            } else {
                15
            }
            return FocusRecommendation(
                id = "rec-struggle-${activeHistory.size}",
                recommendedMinutes = fallbackMins,
                reasonType = RecommendationReasonType.RECENT_STRUGGLE,
                reasonText = "Start with a shorter session and build your momentum.",
                confidence = RecommendationConfidence.HIGH,
                hasEnoughData = true,
                basedOnSessionCount = count,
                preferredPeriod = preferredPeriod,
                subjectId = scopedSubjectId,
                status = RecommendationStatus.ACTIVE,
                generatedAt = nowMs
            )
        }

        // Rule 2: Regularly completes shorter sessions (e.g. 25m) while ending long sessions (45m+) early
        val shortMediumSessions = activeHistory.filter {
            val plannedMin = (it.plannedDurationSeconds / 60L).toInt()
            plannedMin in 15..35
        }
        val longSessions = activeHistory.filter {
            val plannedMin = (it.plannedDurationSeconds / 60L).toInt()
            plannedMin >= 45
        }

        if (shortMediumSessions.size >= 2 && longSessions.size >= 2) {
            val shortCompletionRate = shortMediumSessions.count { it.status == SessionStatus.COMPLETED }.toFloat() / shortMediumSessions.size
            val longEarlyRate = longSessions.count { it.status == SessionStatus.ENDED_EARLY }.toFloat() / longSessions.size
            if (shortCompletionRate >= 0.70f && longEarlyRate >= 0.50f) {
                val bestShort = shortMediumSessions
                    .filter { it.status == SessionStatus.COMPLETED }
                    .map { (it.plannedDurationSeconds / 60L).toInt() }
                    .groupingBy { roundToNearestFive(it) }
                    .eachCount()
                    .maxByOrNull { it.value }?.key ?: 25

                return FocusRecommendation(
                    id = "rec-dropoff-$bestShort",
                    recommendedMinutes = bestShort.coerceIn(15, 35),
                    reasonType = RecommendationReasonType.AVOID_LONG_DROPOFF,
                    reasonText = "Try a $bestShort-minute focus session today.",
                    confidence = RecommendationConfidence.HIGH,
                    hasEnoughData = true,
                    basedOnSessionCount = count,
                    preferredPeriod = preferredPeriod,
                    subjectId = scopedSubjectId,
                    status = RecommendationStatus.ACTIVE,
                    generatedAt = nowMs
                )
            }
        }

        // Rule 3: Consistent mastery on current planned duration -> gentle +5 min suggestion (needs approval)
        val completedSessions = activeHistory.filter { it.status == SessionStatus.COMPLETED }
        val groupedByPlanned = activeHistory.groupBy {
            roundToNearestFive((it.plannedDurationSeconds / 60L).toInt().coerceAtLeast(10))
        }
        val dominantBucket = groupedByPlanned.entries
            .filter { it.value.size >= 3 }
            .maxByOrNull { it.value.size }

        if (dominantBucket != null) {
            val baseMinutes = dominantBucket.key
            val bucketSessions = dominantBucket.value
            val bucketCompletionRate = bucketSessions.count { it.status == SessionStatus.COMPLETED }.toFloat() / bucketSessions.size
            if (bucketCompletionRate >= 0.85f && baseMinutes < 90) {
                val steppedMinutes = (baseMinutes + 5).coerceAtMost(90)
                return FocusRecommendation(
                    id = "rec-stepup-$baseMinutes-$steppedMinutes",
                    recommendedMinutes = steppedMinutes,
                    reasonType = RecommendationReasonType.CONSISTENT_STEP_UP,
                    reasonText = "You're regularly completing $baseMinutes-minute sessions. Would you like to try $steppedMinutes minutes?",
                    confidence = RecommendationConfidence.HIGH,
                    hasEnoughData = true,
                    basedOnSessionCount = count,
                    preferredPeriod = preferredPeriod,
                    subjectId = scopedSubjectId,
                    status = RecommendationStatus.ACTIVE,
                    generatedAt = nowMs
                )
            }
        }

        // Rule 4: Steady rhythm based on completed history + time of day
        val bestCompletedMinutes = if (completedSessions.isNotEmpty()) {
            completedSessions
                .map { roundToNearestFive((it.plannedDurationSeconds / 60L).toInt().coerceAtLeast(15)) }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }?.key ?: 25
        } else {
            25
        }.coerceIn(15, 90)

        val periodSuffix = if (preferredPeriod != null) {
            " Your strongest completion streak is in the $preferredPeriod."
        } else {
            ""
        }

        return FocusRecommendation(
            id = "rec-steady-$bestCompletedMinutes",
            recommendedMinutes = bestCompletedMinutes,
            reasonType = RecommendationReasonType.STEADY_RHYTHM,
            reasonText = "Try a $bestCompletedMinutes-minute focus session today based on your recent completed blocks.$periodSuffix",
            confidence = if (count >= 6) RecommendationConfidence.HIGH else RecommendationConfidence.MEDIUM,
            hasEnoughData = true,
            basedOnSessionCount = count,
            preferredPeriod = preferredPeriod,
            subjectId = scopedSubjectId,
            status = RecommendationStatus.ACTIVE,
            generatedAt = nowMs
        )
    }

    private fun detectPreferredStudyPeriod(sessions: List<FocusSession>): String? {
        val completed = sessions.filter { it.status == SessionStatus.COMPLETED }
        if (completed.size < 3) return null
        val counts = completed.groupingBy { session ->
            val hour = Instant.ofEpochMilli(session.startTimestamp)
                .atZone(ZoneId.systemDefault())
                .hour
            when {
                hour < 12 -> "morning"
                hour < 17 -> "afternoon"
                else -> "evening"
            }
        }.eachCount()
        return counts.maxByOrNull { it.value }?.key
    }

    fun roundToNearestFive(minutes: Int): Int {
        return ((minutes / 5.0).roundToInt() * 5).coerceIn(5, 120)
    }
}

object FocusRecoveryCalculator {
    fun suggestRetryMinutes(plannedMinutes: Int, actualMinutes: Int): Int {
        if (plannedMinutes >= 45 && actualMinutes < 20) return 15
        if (plannedMinutes >= 25 && actualMinutes < 15) return 10
        val half = ((plannedMinutes / 2.0).roundToInt() / 5.0).roundToInt() * 5
        return half.coerceIn(5, 25)
    }
}
