package com.focusly.app.domain.repository

import com.focusly.app.domain.model.DistractionBlockerState
import com.focusly.app.domain.model.FocusRecommendation
import com.focusly.app.domain.model.FocusRecoveryState
import com.focusly.app.domain.model.RecoveryTriggerReason
import kotlinx.coroutines.flow.Flow

interface RecommendationRepository {
    fun observeLatestRecommendation(preferredSubjectId: String? = null): Flow<FocusRecommendation>
    suspend fun refreshRecommendation(preferredSubjectId: String? = null): FocusRecommendation
    suspend fun markRecommendationAccepted(recommendationId: String)
    suspend fun markRecommendationIgnored(recommendationId: String)
}

interface DistractionBlockerRepository {
    fun observeBlockerState(): Flow<DistractionBlockerState>
    suspend fun syncFromOnboardingIfNeeded()
    suspend fun setEnabled(enabled: Boolean)
    suspend fun toggleAppSelection(packageName: String)
    suspend fun refreshPermissionAndApps(): DistractionBlockerState
    suspend fun recordForegroundCheck(detectedDistractionPackage: String?)
}

interface FocusRecoveryRepository {
    fun observeRecoveryState(): Flow<FocusRecoveryState?>
    suspend fun createOrUpdateRecovery(
        lastSessionId: String?,
        triggerReason: RecoveryTriggerReason,
        plannedDurationMinutes: Int,
        actualDurationMinutes: Int,
        subjectId: String?,
        taskId: String?
    ): FocusRecoveryState

    suspend fun resolveRecovery()
}
