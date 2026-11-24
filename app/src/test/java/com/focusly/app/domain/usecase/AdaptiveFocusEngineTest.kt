package com.focusly.app.domain.usecase

import com.focusly.app.domain.model.FocusSession
import com.focusly.app.domain.model.RecommendationConfidence
import com.focusly.app.domain.model.RecommendationReasonType
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveFocusEngineTest {

    private fun focusSession(
        id: String,
        plannedMinutes: Int,
        actualMinutes: Int,
        status: SessionStatus,
        startTimestamp: Long,
        subjectId: String? = null,
        type: SessionType = SessionType.FOCUS
    ) = FocusSession(
        id = id,
        plannedDurationSeconds = plannedMinutes * 60L,
        actualDurationSeconds = actualMinutes * 60L,
        startTimestamp = startTimestamp,
        completionTimestamp = startTimestamp + actualMinutes * 60_000L,
        pausedDurationSeconds = 0L,
        status = status,
        type = type,
        localDate = "2026-11-24",
        subjectId = subjectId,
        taskId = null,
        createdAt = startTimestamp
    )

    @Test
    fun `no history or insufficient history returns safe baseline without unsupported claim`() {
        val emptyRec = AdaptiveFocusEngine.generateRecommendation(
            sessions = emptyList(),
            dailyTargetMinutes = 60
        )
        assertFalse(emptyRec.hasEnoughData)
        assertEquals(RecommendationConfidence.INSUFFICIENT_DATA, emptyRec.confidence)
        assertEquals(RecommendationReasonType.INSUFFICIENT_DATA, emptyRec.reasonType)

        val twoSessions = listOf(
            focusSession("s1", 25, 25, SessionStatus.COMPLETED, 1_700_000_000_000L),
            focusSession("s2", 25, 25, SessionStatus.COMPLETED, 1_700_010_000_000L),
            // Break session should not count toward minimum 3 focus sessions
            focusSession("b1", 5, 5, SessionStatus.COMPLETED, 1_700_020_000_000L, type = SessionType.BREAK)
        )
        val underMinRec = AdaptiveFocusEngine.generateRecommendation(
            sessions = twoSessions,
            dailyTargetMinutes = 120
        )
        assertFalse(underMinRec.hasEnoughData)
        assertEquals(2, underMinRec.basedOnSessionCount)
    }

    @Test
    fun `completing 25m while ending 60m early recommends 25m`() {
        val base = 1_700_000_000_000L
        val sessions = listOf(
            focusSession("1", 25, 25, SessionStatus.COMPLETED, base + 50_000L),
            focusSession("2", 25, 25, SessionStatus.COMPLETED, base + 40_000L),
            focusSession("3", 60, 22, SessionStatus.ENDED_EARLY, base + 30_000L),
            focusSession("4", 25, 25, SessionStatus.COMPLETED, base + 20_000L),
            focusSession("5", 60, 18, SessionStatus.ENDED_EARLY, base + 10_000L)
        )

        val rec = AdaptiveFocusEngine.generateRecommendation(sessions, dailyTargetMinutes = 120)
        assertTrue(rec.hasEnoughData)
        assertEquals(25, rec.recommendedMinutes)
        assertEquals(RecommendationReasonType.AVOID_LONG_DROPOFF, rec.reasonType)
        assertTrue(rec.reasonText.contains("25-minute"))
    }

    @Test
    fun `consistently completing 45m sessions proposes 50m step up`() {
        val base = 1_700_000_000_000L
        val sessions = listOf(
            focusSession("1", 45, 45, SessionStatus.COMPLETED, base + 40_000L),
            focusSession("2", 45, 45, SessionStatus.COMPLETED, base + 30_000L),
            focusSession("3", 45, 45, SessionStatus.COMPLETED, base + 20_000L),
            focusSession("4", 45, 45, SessionStatus.COMPLETED, base + 10_000L)
        )

        val rec = AdaptiveFocusEngine.generateRecommendation(sessions, dailyTargetMinutes = 120)
        assertTrue(rec.hasEnoughData)
        assertEquals(50, rec.recommendedMinutes)
        assertEquals(RecommendationReasonType.CONSISTENT_STEP_UP, rec.reasonType)
        assertTrue(rec.reasonText.contains("45-minute"))
        assertTrue(rec.reasonText.contains("50 minutes"))
    }

    @Test
    fun `recent consecutive early ends triggers momentum recovery suggestion`() {
        val base = 1_700_000_000_000L
        val sessions = listOf(
            focusSession("1", 45, 10, SessionStatus.ENDED_EARLY, base + 40_000L),
            focusSession("2", 45, 12, SessionStatus.ENDED_EARLY, base + 30_000L),
            focusSession("3", 25, 25, SessionStatus.COMPLETED, base + 20_000L),
            focusSession("4", 25, 25, SessionStatus.COMPLETED, base + 10_000L)
        )

        val rec = AdaptiveFocusEngine.generateRecommendation(sessions, dailyTargetMinutes = 90)
        assertTrue(rec.hasEnoughData)
        assertTrue(rec.recommendedMinutes in 10..20)
        assertEquals(RecommendationReasonType.RECENT_STRUGGLE, rec.reasonType)
        assertEquals("Start with a shorter session and build your momentum.", rec.reasonText)
    }

    @Test
    fun `duplicate session ids are deduplicated before evaluating rules`() {
        val base = 1_700_000_000_000L
        val dup = focusSession("dup-1", 25, 25, SessionStatus.COMPLETED, base)
        val rec = AdaptiveFocusEngine.generateRecommendation(
            sessions = listOf(dup, dup, dup),
            dailyTargetMinutes = 60
        )
        assertFalse(rec.hasEnoughData)
        assertEquals(1, rec.basedOnSessionCount)
    }
}
