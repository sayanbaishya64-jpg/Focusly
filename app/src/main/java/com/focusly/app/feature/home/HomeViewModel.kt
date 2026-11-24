package com.focusly.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusly.app.domain.model.ActiveTimerSnapshot
import com.focusly.app.domain.model.DailyMissionItem
import com.focusly.app.domain.model.DailyStats
import com.focusly.app.domain.model.DistractionBlockerState
import com.focusly.app.domain.model.FocusRecommendation
import com.focusly.app.domain.model.FocusRecoveryState
import com.focusly.app.domain.model.RecommendationConfidence
import com.focusly.app.domain.model.RecommendationReasonType
import com.focusly.app.domain.model.RecommendationStatus
import com.focusly.app.domain.model.RewardPreview
import com.focusly.app.domain.model.StudyTask
import com.focusly.app.domain.model.Subject
import com.focusly.app.domain.model.TaskPriority
import com.focusly.app.domain.model.WeeklyStats
import com.focusly.app.domain.repository.DistractionBlockerRepository
import com.focusly.app.domain.repository.FocusRecoveryRepository
import com.focusly.app.domain.repository.FocusSessionRepository
import com.focusly.app.domain.repository.OnboardingRepository
import com.focusly.app.domain.repository.RecommendationRepository
import com.focusly.app.domain.repository.RewardPreviewProvider
import com.focusly.app.domain.repository.RewardsRepository
import com.focusly.app.domain.repository.StudyTaskRepository
import com.focusly.app.domain.repository.SubjectRepository
import com.focusly.app.domain.repository.UserProfileRepository
import com.focusly.app.domain.usecase.DailyMissionsEngine
import com.focusly.app.domain.usecase.DashboardStatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val studentName: String = "",
    val dailyStats: DailyStats = DailyStats(
        date = LocalDate.now().toString(),
        targetMinutes = 60,
        completedFocusMinutes = 0,
        completedFocusSeconds = 0,
        remainingMinutes = 60,
        completionPercentage = 0f,
        completedSessions = 0,
        earlyEndedSessions = 0
    ),
    val weeklyStats: WeeklyStats = WeeklyStats(0, 0, 0, emptyList()),
    val currentTask: StudyTask? = null,
    val subjects: List<Subject> = emptyList(),
    val rewardPreview: RewardPreview = RewardPreview(),
    val activeTimer: ActiveTimerSnapshot? = null,
    val recommendation: FocusRecommendation = FocusRecommendation(
        id = "rec-init",
        recommendedMinutes = 25,
        reasonType = RecommendationReasonType.INSUFFICIENT_DATA,
        reasonText = "",
        confidence = RecommendationConfidence.INSUFFICIENT_DATA,
        hasEnoughData = false,
        basedOnSessionCount = 0,
        status = RecommendationStatus.ACTIVE
    ),
    val blockerState: DistractionBlockerState = DistractionBlockerState(),
    val recoveryState: FocusRecoveryState? = null,
    val dailyMissions: List<DailyMissionItem> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    onboardingRepository: OnboardingRepository,
    focusSessionRepository: FocusSessionRepository,
    private val studyTaskRepository: StudyTaskRepository,
    private val subjectRepository: SubjectRepository,
    userProfileRepository: UserProfileRepository,
    rewardPreviewProvider: RewardPreviewProvider,
    private val rewardsRepository: RewardsRepository,
    private val recommendationRepository: RecommendationRepository,
    private val distractionBlockerRepository: DistractionBlockerRepository,
    private val focusRecoveryRepository: FocusRecoveryRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val weekStart = today.minusDays(6).toString()
    private val todayStr = today.toString()

    private val errorState = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            distractionBlockerRepository.syncFromOnboardingIfNeeded()
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        onboardingRepository.preferencesFlow,
        focusSessionRepository.observeSessionsInRange(weekStart, todayStr),
        studyTaskRepository.observeCurrentActiveTask(),
        subjectRepository.observeAllSubjects(),
        userProfileRepository.observeStudentName(),
        rewardPreviewProvider.observeRewardPreview(),
        focusSessionRepository.activeTimerFlow,
        recommendationRepository.observeLatestRecommendation(),
        distractionBlockerRepository.observeBlockerState(),
        focusRecoveryRepository.observeRecoveryState(),
        studyTaskRepository.observeAllTasks(),
        rewardsRepository.observeClaimedMissionIdsForDate(todayStr),
        errorState
    ) { values ->
        val prefs = values[0] as com.focusly.app.core.model.OnboardingPreferences
        @Suppress("UNCHECKED_CAST")
        val sessions = values[1] as List<com.focusly.app.domain.model.FocusSession>
        val task = values[2] as StudyTask?
        @Suppress("UNCHECKED_CAST")
        val subjects = values[3] as List<Subject>
        val studentName = values[4] as String
        val rewardPreview = values[5] as RewardPreview
        val activeTimer = values[6] as ActiveTimerSnapshot?
        val recommendation = values[7] as FocusRecommendation
        val blockerState = values[8] as DistractionBlockerState
        val recoveryState = values[9] as FocusRecoveryState?
        @Suppress("UNCHECKED_CAST")
        val allTasks = values[10] as List<StudyTask>
        @Suppress("UNCHECKED_CAST")
        val claimedMissionIds = values[11] as Set<String>
        val error = values[12] as String?

        val nowDate = LocalDate.now()
        val dailyStats = DashboardStatsCalculator.calculateDailyStats(
            sessions = sessions,
            targetMinutes = prefs.dailyFocusTargetMinutes,
            targetDate = nowDate
        )
        val weeklyStats = DashboardStatsCalculator.calculateWeeklyStats(
            sessions = sessions,
            endDate = nowDate
        )
        val dailyMissions = DailyMissionsEngine.evaluateDailyMissions(
            sessions = sessions,
            tasks = allTasks,
            dailyTargetMinutes = prefs.dailyFocusTargetMinutes,
            todayDate = nowDate,
            claimedMissionIds = claimedMissionIds
        )

        HomeUiState(
            isLoading = false,
            studentName = studentName,
            dailyStats = dailyStats,
            weeklyStats = weeklyStats,
            currentTask = task,
            subjects = subjects,
            rewardPreview = rewardPreview,
            activeTimer = activeTimer,
            recommendation = recommendation,
            blockerState = blockerState,
            recoveryState = recoveryState,
            dailyMissions = dailyMissions,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun createQuickTask(title: String, subjectName: String, durationMinutes: Int) {
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val subjectId = if (subjectName.isNotBlank()) {
                    subjectRepository.upsertSubject(subjectName.trim(), 0xFF3B82F6).id
                } else {
                    null
                }
                studyTaskRepository.createTask(
                    title = title,
                    description = "",
                    subjectId = subjectId,
                    plannedDurationMinutes = durationMinutes,
                    priority = TaskPriority.MEDIUM
                )
            }.onFailure { err ->
                errorState.value = err.message ?: "Unable to save study task"
            }
        }
    }

    fun acceptRecommendation(recommendationId: String) {
        viewModelScope.launch {
            recommendationRepository.markRecommendationAccepted(recommendationId)
        }
    }

    fun ignoreRecommendation(recommendationId: String) {
        viewModelScope.launch {
            recommendationRepository.markRecommendationIgnored(recommendationId)
        }
    }

    fun dismissRecovery() {
        viewModelScope.launch {
            focusRecoveryRepository.resolveRecovery()
        }
    }

    fun claimDailyMission(missionId: String) {
        viewModelScope.launch {
            rewardsRepository.claimDailyMission(missionId, LocalDate.now().toString())
        }
    }

    fun clearError() {
        errorState.value = null
    }
}
