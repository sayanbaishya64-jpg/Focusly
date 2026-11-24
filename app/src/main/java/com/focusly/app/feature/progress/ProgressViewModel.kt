package com.focusly.app.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusly.app.domain.model.AnalyticsPeriod
import com.focusly.app.domain.model.DailyStats
import com.focusly.app.domain.model.FocusRecommendation
import com.focusly.app.domain.model.ProgressAnalyticsSummary
import com.focusly.app.domain.model.RecommendationConfidence
import com.focusly.app.domain.model.RecommendationReasonType
import com.focusly.app.domain.model.RecommendationStatus
import com.focusly.app.domain.model.StudyTask
import com.focusly.app.domain.model.Subject
import com.focusly.app.domain.model.TaskStatus
import com.focusly.app.domain.repository.FocusSessionRepository
import com.focusly.app.domain.repository.OnboardingRepository
import com.focusly.app.domain.repository.RecommendationRepository
import com.focusly.app.domain.repository.StudyTaskRepository
import com.focusly.app.domain.repository.SubjectRepository
import com.focusly.app.domain.usecase.DashboardStatsCalculator
import com.focusly.app.domain.usecase.StudyAnalyticsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ProgressUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.WEEK,
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
    val summary: ProgressAnalyticsSummary = ProgressAnalyticsSummary(
        period = AnalyticsPeriod.WEEK,
        totalFocusMinutes = 0,
        completedSessions = 0,
        earlyEndedSessions = 0,
        completionRatePercent = 0f,
        averageSessionMinutes = 0,
        activeDaysCount = 0,
        dailyBreakdown = emptyList(),
        subjectBreakdown = emptyList()
    ),
    val completedTasksCount: Int = 0,
    val totalTasksCount: Int = 0,
    val recommendation: FocusRecommendation = FocusRecommendation(
        id = "rec-init",
        recommendedMinutes = 25,
        reasonType = RecommendationReasonType.INSUFFICIENT_DATA,
        reasonText = "",
        confidence = RecommendationConfidence.INSUFFICIENT_DATA,
        hasEnoughData = false,
        basedOnSessionCount = 0,
        status = RecommendationStatus.ACTIVE
    )
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    onboardingRepository: OnboardingRepository,
    focusSessionRepository: FocusSessionRepository,
    subjectRepository: SubjectRepository,
    studyTaskRepository: StudyTaskRepository,
    private val recommendationRepository: RecommendationRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val monthStart = today.minusDays(29).toString()
    private val todayStr = today.toString()

    private val periodState = MutableStateFlow(AnalyticsPeriod.WEEK)

    val uiState: StateFlow<ProgressUiState> = combine(
        onboardingRepository.preferencesFlow,
        focusSessionRepository.observeSessionsInRange(monthStart, todayStr),
        subjectRepository.observeAllSubjects(),
        studyTaskRepository.observeAllTasks(),
        recommendationRepository.observeLatestRecommendation(),
        periodState
    ) { values ->
        val prefs = values[0] as com.focusly.app.core.model.OnboardingPreferences
        @Suppress("UNCHECKED_CAST")
        val sessions = values[1] as List<com.focusly.app.domain.model.FocusSession>
        @Suppress("UNCHECKED_CAST")
        val subjects = values[2] as List<Subject>
        @Suppress("UNCHECKED_CAST")
        val tasks = values[3] as List<StudyTask>
        val recommendation = values[4] as FocusRecommendation
        val period = values[5] as AnalyticsPeriod

        val nowDate = LocalDate.now()
        val daily = DashboardStatsCalculator.calculateDailyStats(
            sessions = sessions,
            targetMinutes = prefs.dailyFocusTargetMinutes,
            targetDate = nowDate
        )
        val analytics = StudyAnalyticsCalculator.calculateProgressAnalytics(
            sessions = sessions,
            subjects = subjects,
            period = period,
            endDate = nowDate
        )

        ProgressUiState(
            isLoading = false,
            selectedPeriod = period,
            dailyStats = daily,
            summary = analytics,
            completedTasksCount = tasks.count { it.status == TaskStatus.COMPLETED },
            totalTasksCount = tasks.size,
            recommendation = recommendation
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgressUiState()
    )

    fun selectPeriod(period: AnalyticsPeriod) {
        periodState.value = period
    }

    fun ignoreRecommendation(recommendationId: String) {
        viewModelScope.launch {
            recommendationRepository.markRecommendationIgnored(recommendationId)
        }
    }
}
