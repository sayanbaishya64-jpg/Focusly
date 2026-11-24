package com.focusly.app.domain.model

enum class RecommendationConfidence {
    INSUFFICIENT_DATA,
    LOW,
    MEDIUM,
    HIGH
}

enum class RecommendationReasonType {
    INSUFFICIENT_DATA,
    AVOID_LONG_DROPOFF,
    CONSISTENT_STEP_UP,
    RECENT_STRUGGLE,
    STEADY_RHYTHM
}

enum class RecommendationStatus {
    ACTIVE,
    ACCEPTED,
    IGNORED
}

data class FocusRecommendation(
    val id: String,
    val recommendedMinutes: Int,
    val reasonType: RecommendationReasonType,
    val reasonText: String,
    val confidence: RecommendationConfidence,
    val hasEnoughData: Boolean,
    val basedOnSessionCount: Int,
    val preferredPeriod: String? = null,
    val subjectId: String? = null,
    val status: RecommendationStatus = RecommendationStatus.ACTIVE,
    val generatedAt: Long = 0L
)

enum class DistractionPermissionStatus {
    GRANTED,
    DENIED
}

enum class DistractionBlockerMode {
    ACTIVE_DETECTION,
    CHECKLIST_ONLY,
    DISABLED
}

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isSuggestedDefault: Boolean = false
)

data class DistractionBlockerState(
    val isEnabled: Boolean = false,
    val selectedPackageNames: Set<String> = emptySet(),
    val permissionStatus: DistractionPermissionStatus = DistractionPermissionStatus.DENIED,
    val effectiveMode: DistractionBlockerMode = DistractionBlockerMode.DISABLED,
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val lastCheckedAt: Long? = null,
    val lastDetectedDistractionPackage: String? = null
)

enum class RecoveryTriggerReason {
    INTERRUPTED_IN_BACKGROUND,
    ENDED_EARLY,
    PAUSED_LONG
}

data class FocusRecoveryState(
    val lastSessionId: String?,
    val triggerReason: RecoveryTriggerReason,
    val plannedDurationMinutes: Int,
    val actualDurationMinutes: Int,
    val suggestedRetryMinutes: Int,
    val subjectId: String? = null,
    val taskId: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = 0L
)
