package com.focusly.app.data.repository

import com.focusly.app.data.local.dao.DistractionSettingsDao
import com.focusly.app.data.local.dao.FocusRecommendationDao
import com.focusly.app.data.local.dao.FocusRecoveryDao
import com.focusly.app.data.local.dao.FocusSessionDao
import com.focusly.app.data.local.entity.DistractionSettingsEntity
import com.focusly.app.data.local.entity.FocusRecommendationEntity
import com.focusly.app.data.local.entity.FocusRecoveryStateEntity
import com.focusly.app.data.local.entity.FocusSessionEntity
import com.focusly.app.data.preferences.UserPreferencesDataStore
import com.focusly.app.domain.model.DistractionBlockerMode
import com.focusly.app.domain.model.DistractionBlockerState
import com.focusly.app.domain.model.DistractionPermissionStatus
import com.focusly.app.domain.model.FocusRecommendation
import com.focusly.app.domain.model.FocusRecoveryState
import com.focusly.app.domain.model.FocusSession
import com.focusly.app.domain.model.RecommendationConfidence
import com.focusly.app.domain.model.RecommendationReasonType
import com.focusly.app.domain.model.RecommendationStatus
import com.focusly.app.domain.model.RecoveryTriggerReason
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import com.focusly.app.domain.repository.DistractionBlockerRepository
import com.focusly.app.domain.repository.FocusRecoveryRepository
import com.focusly.app.domain.repository.RecommendationRepository
import com.focusly.app.domain.usecase.AdaptiveFocusEngine
import com.focusly.app.domain.usecase.FocusRecoveryCalculator
import com.focusly.app.services.blocker.DistractionBlockerManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecommendationRepositoryImpl @Inject constructor(
    private val focusSessionDao: FocusSessionDao,
    private val recommendationDao: FocusRecommendationDao,
    private val preferencesDataStore: UserPreferencesDataStore
) : RecommendationRepository {

    override fun observeLatestRecommendation(preferredSubjectId: String?): Flow<FocusRecommendation> {
        return combine(
            focusSessionDao.observeFocusSessionsHistory(40),
            preferencesDataStore.preferencesFlow,
            recommendationDao.observeLatestRecommendation()
        ) { sessionEntities, prefs, savedEntity ->
            val domainSessions = sessionEntities.map { it.toDomain() }
            val fresh = AdaptiveFocusEngine.generateRecommendation(
                sessions = domainSessions,
                dailyTargetMinutes = prefs.dailyFocusTargetMinutes,
                preferredSubjectId = preferredSubjectId
            )

            // Preserve ACCEPTED or IGNORED status if this exact recommendation id was acted on
            if (savedEntity != null && savedEntity.id == fresh.id) {
                val savedStatus = runCatching { RecommendationStatus.valueOf(savedEntity.status) }
                    .getOrDefault(RecommendationStatus.ACTIVE)
                fresh.copy(status = savedStatus)
            } else {
                fresh
            }
        }
    }

    override suspend fun refreshRecommendation(preferredSubjectId: String?): FocusRecommendation {
        val sessions = focusSessionDao.observeFocusSessionsHistory(40).first().map { it.toDomain() }
        val prefs = preferencesDataStore.preferencesFlow.first()
        val nowMs = System.currentTimeMillis()
        val rec = AdaptiveFocusEngine.generateRecommendation(
            sessions = sessions,
            dailyTargetMinutes = prefs.dailyFocusTargetMinutes,
            preferredSubjectId = preferredSubjectId,
            nowMs = nowMs
        )
        val existing = recommendationDao.getLatestRecommendation()
        val statusToSave = if (existing != null && existing.id == rec.id) {
            existing.status
        } else {
            rec.status.name
        }
        recommendationDao.upsert(
            FocusRecommendationEntity(
                id = rec.id,
                recommendedMinutes = rec.recommendedMinutes,
                reasonType = rec.reasonType.name,
                reasonText = rec.reasonText,
                confidence = rec.confidence.name,
                subjectId = rec.subjectId,
                preferredPeriod = rec.preferredPeriod,
                basedOnSessionCount = rec.basedOnSessionCount,
                status = statusToSave,
                generatedAt = nowMs,
                updatedAt = nowMs
            )
        )
        return rec.copy(
            status = runCatching { RecommendationStatus.valueOf(statusToSave) }
                .getOrDefault(RecommendationStatus.ACTIVE)
        )
    }

    override suspend fun markRecommendationAccepted(recommendationId: String) {
        val nowMs = System.currentTimeMillis()
        val current = recommendationDao.getLatestRecommendation()
        if (current != null && current.id == recommendationId) {
            recommendationDao.updateStatus(recommendationId, RecommendationStatus.ACCEPTED.name, nowMs)
        } else {
            val fresh = refreshRecommendation()
            recommendationDao.updateStatus(fresh.id, RecommendationStatus.ACCEPTED.name, nowMs)
        }
    }

    override suspend fun markRecommendationIgnored(recommendationId: String) {
        val nowMs = System.currentTimeMillis()
        val current = recommendationDao.getLatestRecommendation()
        if (current != null && current.id == recommendationId) {
            recommendationDao.updateStatus(recommendationId, RecommendationStatus.IGNORED.name, nowMs)
        } else {
            val fresh = refreshRecommendation()
            recommendationDao.updateStatus(fresh.id, RecommendationStatus.IGNORED.name, nowMs)
        }
    }

    private fun FocusSessionEntity.toDomain(): FocusSession {
        return FocusSession(
            id = id,
            plannedDurationSeconds = plannedDurationSeconds,
            actualDurationSeconds = actualDurationSeconds,
            startTimestamp = startTimestamp,
            completionTimestamp = completionTimestamp,
            pausedDurationSeconds = pausedDurationSeconds,
            status = runCatching { SessionStatus.valueOf(sessionStatus) }
                .getOrDefault(SessionStatus.ENDED_EARLY),
            type = runCatching { SessionType.valueOf(sessionType) }
                .getOrDefault(SessionType.FOCUS),
            localDate = localDate,
            subjectId = subjectId,
            taskId = taskId,
            createdAt = createdAt
        )
    }
}

@Singleton
class DistractionBlockerRepositoryImpl @Inject constructor(
    private val distractionSettingsDao: DistractionSettingsDao,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val blockerManager: DistractionBlockerManager
) : DistractionBlockerRepository {

    private val installedAppsFlow = MutableStateFlow(blockerManager.getLaunchableApps())
    private val permissionFlow = MutableStateFlow(blockerManager.getUsagePermissionStatus())
    private val lastDetectedPkgFlow = MutableStateFlow<String?>(null)

    override fun observeBlockerState(): Flow<DistractionBlockerState> {
        return combine(
            distractionSettingsDao.observeSettings(),
            permissionFlow,
            installedAppsFlow,
            lastDetectedPkgFlow
        ) { entity, permStatus, apps, lastPkg ->
            val selected = entity?.selectedPackageNamesCsv
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: emptySet()

            val enabled = entity?.isEnabled ?: false
            val mode = when {
                !enabled -> DistractionBlockerMode.DISABLED
                permStatus == DistractionPermissionStatus.GRANTED -> DistractionBlockerMode.ACTIVE_DETECTION
                else -> DistractionBlockerMode.CHECKLIST_ONLY
            }

            DistractionBlockerState(
                isEnabled = enabled,
                selectedPackageNames = selected,
                permissionStatus = permStatus,
                effectiveMode = mode,
                installedApps = apps,
                lastCheckedAt = entity?.lastCheckedAt,
                lastDetectedDistractionPackage = lastPkg
            )
        }
    }

    override suspend fun syncFromOnboardingIfNeeded() {
        val existing = distractionSettingsDao.getSettings()
        if (existing != null) return

        val prefs = preferencesDataStore.preferencesFlow.first()
        val permStatus = blockerManager.getUsagePermissionStatus()
        permissionFlow.value = permStatus
        val nowMs = System.currentTimeMillis()
        distractionSettingsDao.upsert(
            DistractionSettingsEntity(
                isEnabled = prefs.selectedDistractionApps.isNotEmpty() && !prefs.distractionPreferencesSkipped,
                selectedPackageNamesCsv = prefs.selectedDistractionApps.joinToString(","),
                lastPermissionStatus = permStatus.name,
                lastCheckedAt = null,
                updatedAt = nowMs
            )
        )
    }

    override suspend fun setEnabled(enabled: Boolean) {
        syncFromOnboardingIfNeeded()
        val current = distractionSettingsDao.getSettings() ?: defaultEntity()
        val permStatus = blockerManager.getUsagePermissionStatus()
        permissionFlow.value = permStatus
        distractionSettingsDao.upsert(
            current.copy(
                isEnabled = enabled,
                lastPermissionStatus = permStatus.name,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun toggleAppSelection(packageName: String) {
        syncFromOnboardingIfNeeded()
        val current = distractionSettingsDao.getSettings() ?: defaultEntity()
        val mutable = current.selectedPackageNamesCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableSet()

        if (!mutable.add(packageName)) {
            mutable.remove(packageName)
        }

        val nowMs = System.currentTimeMillis()
        distractionSettingsDao.upsert(
            current.copy(
                selectedPackageNamesCsv = mutable.joinToString(","),
                updatedAt = nowMs
            )
        )
        preferencesDataStore.setDistractionPreferences(mutable, skipped = mutable.isEmpty())
    }

    override suspend fun refreshPermissionAndApps(): DistractionBlockerState {
        syncFromOnboardingIfNeeded()
        val permStatus = blockerManager.getUsagePermissionStatus()
        val apps = blockerManager.getLaunchableApps()
        permissionFlow.value = permStatus
        installedAppsFlow.value = apps

        val current = distractionSettingsDao.getSettings() ?: defaultEntity()
        distractionSettingsDao.upsert(
            current.copy(
                lastPermissionStatus = permStatus.name,
                updatedAt = System.currentTimeMillis()
            )
        )
        return observeBlockerState().first()
    }

    override suspend fun recordForegroundCheck(detectedDistractionPackage: String?) {
        val current = distractionSettingsDao.getSettings() ?: return
        val nowMs = System.currentTimeMillis()
        lastDetectedPkgFlow.value = detectedDistractionPackage
        distractionSettingsDao.upsert(
            current.copy(
                lastCheckedAt = nowMs,
                updatedAt = nowMs
            )
        )
    }

    private fun defaultEntity(): DistractionSettingsEntity {
        return DistractionSettingsEntity(
            isEnabled = false,
            selectedPackageNamesCsv = "",
            lastPermissionStatus = blockerManager.getUsagePermissionStatus().name,
            lastCheckedAt = null,
            updatedAt = System.currentTimeMillis()
        )
    }
}

@Singleton
class FocusRecoveryRepositoryImpl @Inject constructor(
    private val focusRecoveryDao: FocusRecoveryDao
) : FocusRecoveryRepository {

    override fun observeRecoveryState(): Flow<FocusRecoveryState?> {
        return focusRecoveryDao.observeRecoveryState().map { entity ->
            if (entity == null || !entity.isActive) {
                null
            } else {
                FocusRecoveryState(
                    lastSessionId = entity.lastSessionId,
                    triggerReason = runCatching { RecoveryTriggerReason.valueOf(entity.triggerReason) }
                        .getOrDefault(RecoveryTriggerReason.ENDED_EARLY),
                    plannedDurationMinutes = entity.plannedDurationMinutes,
                    actualDurationMinutes = entity.actualDurationMinutes,
                    suggestedRetryMinutes = entity.suggestedRetryMinutes,
                    subjectId = entity.subjectId,
                    taskId = entity.taskId,
                    isActive = entity.isActive,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    override suspend fun createOrUpdateRecovery(
        lastSessionId: String?,
        triggerReason: RecoveryTriggerReason,
        plannedDurationMinutes: Int,
        actualDurationMinutes: Int,
        subjectId: String?,
        taskId: String?
    ): FocusRecoveryState {
        val nowMs = System.currentTimeMillis()
        val suggested = FocusRecoveryCalculator.suggestRetryMinutes(
            plannedMinutes = plannedDurationMinutes,
            actualMinutes = actualDurationMinutes
        )
        val entity = FocusRecoveryStateEntity(
            lastSessionId = lastSessionId,
            triggerReason = triggerReason.name,
            plannedDurationMinutes = plannedDurationMinutes,
            actualDurationMinutes = actualDurationMinutes,
            suggestedRetryMinutes = suggested,
            subjectId = subjectId,
            taskId = taskId,
            isActive = true,
            createdAt = nowMs,
            resolvedAt = null
        )
        focusRecoveryDao.upsert(entity)
        return FocusRecoveryState(
            lastSessionId = lastSessionId,
            triggerReason = triggerReason,
            plannedDurationMinutes = plannedDurationMinutes,
            actualDurationMinutes = actualDurationMinutes,
            suggestedRetryMinutes = suggested,
            subjectId = subjectId,
            taskId = taskId,
            isActive = true,
            createdAt = nowMs
        )
    }

    override suspend fun resolveRecovery() {
        focusRecoveryDao.clearActive(System.currentTimeMillis())
    }
}
