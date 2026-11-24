package com.focusly.app.feature.focus

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusly.app.domain.model.ActiveTimerSnapshot
import com.focusly.app.domain.model.DailyStats
import com.focusly.app.domain.model.DistractionBlockerState
import com.focusly.app.domain.model.FocusMode
import com.focusly.app.domain.model.FocusRecommendation
import com.focusly.app.domain.model.FocusRecoveryState
import com.focusly.app.domain.model.FocusSession
import com.focusly.app.domain.model.RecommendationConfidence
import com.focusly.app.domain.model.RecommendationReasonType
import com.focusly.app.domain.model.RecommendationStatus
import com.focusly.app.domain.model.RecoveryTriggerReason
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import com.focusly.app.domain.model.StudyTask
import com.focusly.app.domain.model.Subject
import com.focusly.app.domain.repository.DistractionBlockerRepository
import com.focusly.app.domain.repository.FocusRecoveryRepository
import com.focusly.app.domain.repository.FocusSessionRepository
import com.focusly.app.domain.repository.OnboardingRepository
import com.focusly.app.domain.repository.RecommendationRepository
import com.focusly.app.domain.repository.StudyTaskRepository
import com.focusly.app.domain.repository.SubjectRepository
import com.focusly.app.domain.usecase.DailyMissionsEngine
import com.focusly.app.domain.usecase.DashboardStatsCalculator
import com.focusly.app.domain.usecase.SessionStateMachine
import com.focusly.app.domain.usecase.TimerCalculator
import com.focusly.app.services.timer.FocusTimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class FocusUiState(
    val sessionType: SessionType = SessionType.FOCUS,
    val selectedDurationMinutes: Int = 25,
    val customDurationInput: String = "",
    val isCustomDuration: Boolean = false,
    val selectedSubjectId: String? = null,
    val selectedTaskId: String? = null,
    val subjects: List<Subject> = emptyList(),
    val tasks: List<StudyTask> = emptyList(),
    val activeSnapshot: ActiveTimerSnapshot? = null,
    val remainingSeconds: Long = 25 * 60L,
    val progressFraction: Float = 0f,
    val status: SessionStatus = SessionStatus.IDLE,
    val lastCompletedSession: FocusSession? = null,
    val recentSessions: List<FocusSession> = emptyList(),
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
    val pendingRecoverySnapshot: ActiveTimerSnapshot? = null,
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
    val focusMode: FocusMode = FocusMode.MANUAL
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val focusSessionRepository: FocusSessionRepository,
    private val onboardingRepository: OnboardingRepository,
    private val subjectRepository: SubjectRepository,
    private val studyTaskRepository: StudyTaskRepository,
    private val recommendationRepository: RecommendationRepository,
    private val distractionBlockerRepository: DistractionBlockerRepository,
    private val focusRecoveryRepository: FocusRecoveryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var recoveryChecked = false

    init {
        viewModelScope.launch {
            distractionBlockerRepository.syncFromOnboardingIfNeeded()
        }
        observeState()
        startUiClockLoop()
    }

    private fun observeState() {
        val todayStr = LocalDate.now().toString()
        viewModelScope.launch {
            combine(
                focusSessionRepository.activeTimerFlow,
                subjectRepository.observeAllSubjects(),
                studyTaskRepository.observeAllTasks(),
                focusSessionRepository.observeSessionsForDate(todayStr),
                focusSessionRepository.observeRecentSessions(15),
                onboardingRepository.preferencesFlow,
                recommendationRepository.observeLatestRecommendation(),
                distractionBlockerRepository.observeBlockerState(),
                focusRecoveryRepository.observeRecoveryState()
            ) { values ->
                val active = values[0] as ActiveTimerSnapshot?
                @Suppress("UNCHECKED_CAST")
                val subjects = values[1] as List<Subject>
                @Suppress("UNCHECKED_CAST")
                val tasks = values[2] as List<StudyTask>
                @Suppress("UNCHECKED_CAST")
                val todaySessions = values[3] as List<FocusSession>
                @Suppress("UNCHECKED_CAST")
                val recent = values[4] as List<FocusSession>
                val prefs = values[5] as com.focusly.app.core.model.OnboardingPreferences
                val recommendation = values[6] as FocusRecommendation
                val blockerState = values[7] as DistractionBlockerState
                val recoveryState = values[8] as FocusRecoveryState?

                val dailyStats = DashboardStatsCalculator.calculateDailyStats(
                    sessions = todaySessions,
                    targetMinutes = prefs.dailyFocusTargetMinutes,
                    targetDate = LocalDate.now()
                )

                val current = _uiState.value
                if (!recoveryChecked) {
                    recoveryChecked = true
                    if (active != null && active.isActive) {
                        val checkProgress = TimerCalculator.calculateProgress(active, System.currentTimeMillis())
                        if (checkProgress.isCompleted) {
                            completeSessionInternal(active, SessionStatus.COMPLETED, active.plannedDurationSeconds)
                        } else {
                            _uiState.update { it.copy(pendingRecoverySnapshot = active) }
                        }
                    }
                }

                val nextStatus = active?.status ?: if (current.lastCompletedSession != null) {
                    SessionStatus.COMPLETED
                } else {
                    SessionStatus.IDLE
                }

                val progress = if (active != null) {
                    TimerCalculator.calculateProgress(active, System.currentTimeMillis())
                } else {
                    null
                }

                current.copy(
                    subjects = subjects,
                    tasks = tasks,
                    activeSnapshot = active,
                    status = nextStatus,
                    sessionType = active?.sessionType ?: current.sessionType,
                    remainingSeconds = progress?.remainingSeconds
                        ?: (current.selectedDurationMinutes * 60L),
                    progressFraction = progress?.progressFraction ?: 0f,
                    recentSessions = recent,
                    dailyStats = dailyStats,
                    recommendation = recommendation,
                    blockerState = blockerState,
                    recoveryState = recoveryState
                )
            }.collect { nextState ->
                _uiState.value = nextState
            }
        }
    }

    private fun startUiClockLoop() {
        viewModelScope.launch {
            while (isActive) {
                val active = _uiState.value.activeSnapshot
                if (active != null && active.status == SessionStatus.RUNNING) {
                    val nowMs = System.currentTimeMillis()
                    val progress = TimerCalculator.calculateProgress(active, nowMs)
                    if (progress.isCompleted) {
                        completeSessionInternal(active, SessionStatus.COMPLETED, active.plannedDurationSeconds)
                    } else {
                        _uiState.update {
                            it.copy(
                                remainingSeconds = progress.remainingSeconds,
                                progressFraction = progress.progressFraction
                            )
                        }
                    }
                }
                delay(500L)
            }
        }
    }

    fun selectSessionType(type: SessionType) {
        if (_uiState.value.activeSnapshot?.isActive == true) return
        val defaultMinutes = if (type == SessionType.FOCUS) 25 else 5
        _uiState.update {
            it.copy(
                sessionType = type,
                selectedDurationMinutes = defaultMinutes,
                isCustomDuration = false,
                remainingSeconds = defaultMinutes * 60L,
                progressFraction = 0f,
                lastCompletedSession = null,
                status = SessionStatus.IDLE
            )
        }
    }

    fun selectFocusMode(mode: FocusMode) {
        val current = _uiState.value
        if (current.activeSnapshot?.isActive == true) return
        val presets = listOf(15, 25, 45, 60, 90)
        when (mode) {
            FocusMode.MANUAL -> {
                _uiState.update {
                    it.copy(
                        focusMode = FocusMode.MANUAL,
                        sessionType = SessionType.FOCUS,
                        lastCompletedSession = null,
                        status = SessionStatus.IDLE
                    )
                }
            }
            FocusMode.AUTO_SCHEDULE -> {
                val picked = DailyMissionsEngine.selectAutoScheduleTask(
                    tasks = current.tasks,
                    todayDate = LocalDate.now()
                )
                val targetMins = picked?.plannedDurationMinutes?.coerceIn(5, 240)
                    ?: current.selectedDurationMinutes
                _uiState.update {
                    it.copy(
                        focusMode = FocusMode.AUTO_SCHEDULE,
                        sessionType = SessionType.FOCUS,
                        selectedTaskId = picked?.id,
                        selectedSubjectId = picked?.subjectId,
                        selectedDurationMinutes = targetMins,
                        isCustomDuration = targetMins !in presets,
                        customDurationInput = if (targetMins !in presets) targetMins.toString() else "",
                        remainingSeconds = targetMins * 60L,
                        progressFraction = 0f,
                        lastCompletedSession = null,
                        status = SessionStatus.IDLE
                    )
                }
            }
            FocusMode.SMART -> {
                val smartMins = if (current.recommendation.hasEnoughData) {
                    current.recommendation.recommendedMinutes.coerceIn(5, 240)
                } else {
                    25
                }
                _uiState.update {
                    it.copy(
                        focusMode = FocusMode.SMART,
                        sessionType = SessionType.FOCUS,
                        selectedDurationMinutes = smartMins,
                        isCustomDuration = smartMins !in presets,
                        customDurationInput = if (smartMins !in presets) smartMins.toString() else "",
                        remainingSeconds = smartMins * 60L,
                        progressFraction = 0f,
                        lastCompletedSession = null,
                        status = SessionStatus.IDLE
                    )
                }
            }
        }
    }

    fun selectPresetDuration(minutes: Int) {
        if (_uiState.value.activeSnapshot?.isActive == true) return
        _uiState.update {
            it.copy(
                selectedDurationMinutes = minutes,
                isCustomDuration = false,
                remainingSeconds = minutes * 60L,
                progressFraction = 0f
            )
        }
    }

    fun updateCustomDuration(input: String) {
        if (_uiState.value.activeSnapshot?.isActive == true) return
        val digits = input.filter(Char::isDigit).take(3)
        val parsed = digits.toIntOrNull()
        val maxLimit = if (_uiState.value.sessionType == SessionType.FOCUS) 240 else 60
        _uiState.update { state ->
            val validMinutes = if (parsed != null && parsed in 1..maxLimit) parsed else state.selectedDurationMinutes
            state.copy(
                isCustomDuration = true,
                customDurationInput = digits,
                selectedDurationMinutes = validMinutes,
                remainingSeconds = validMinutes * 60L
            )
        }
    }

    fun selectSubject(subjectId: String?) {
        _uiState.update { it.copy(selectedSubjectId = subjectId) }
    }

    fun selectTask(taskId: String?) {
        _uiState.update { state ->
            val task = state.tasks.firstOrNull { it.id == taskId }
            state.copy(
                selectedTaskId = taskId,
                selectedSubjectId = task?.subjectId ?: state.selectedSubjectId
            )
        }
    }

    fun prepareFocusForTask(task: StudyTask) {
        if (_uiState.value.activeSnapshot?.isActive == true) return
        val clamped = task.plannedDurationMinutes.coerceIn(5, 240)
        val presets = listOf(15, 25, 45, 60, 90)
        _uiState.update {
            it.copy(
                sessionType = SessionType.FOCUS,
                selectedSubjectId = task.subjectId,
                selectedTaskId = task.id,
                selectedDurationMinutes = clamped,
                isCustomDuration = clamped !in presets,
                customDurationInput = if (clamped !in presets) clamped.toString() else "",
                remainingSeconds = clamped * 60L,
                progressFraction = 0f,
                lastCompletedSession = null,
                status = SessionStatus.IDLE
            )
        }
    }

    fun applyRecommendationDuration(minutes: Int) {
        if (_uiState.value.activeSnapshot?.isActive == true) return
        val recId = _uiState.value.recommendation.id
        val clamped = minutes.coerceIn(5, 240)
        _uiState.update {
            it.copy(
                sessionType = SessionType.FOCUS,
                selectedDurationMinutes = clamped,
                isCustomDuration = clamped !in listOf(15, 25, 45, 60, 90),
                customDurationInput = if (clamped !in listOf(15, 25, 45, 60, 90)) clamped.toString() else "",
                remainingSeconds = clamped * 60L,
                progressFraction = 0f,
                lastCompletedSession = null,
                status = SessionStatus.IDLE
            )
        }
        viewModelScope.launch {
            recommendationRepository.markRecommendationAccepted(recId)
        }
    }

    fun ignoreRecommendation(recommendationId: String) {
        viewModelScope.launch {
            recommendationRepository.markRecommendationIgnored(recommendationId)
        }
    }

    fun startSessionWithMinutes(minutes: Int, fromRecommendationId: String? = null) {
        if (_uiState.value.activeSnapshot?.isActive == true) return
        val clamped = minutes.coerceIn(5, 240)
        _uiState.update {
            it.copy(
                sessionType = SessionType.FOCUS,
                selectedDurationMinutes = clamped,
                isCustomDuration = clamped !in listOf(15, 25, 45, 60, 90),
                customDurationInput = if (clamped !in listOf(15, 25, 45, 60, 90)) clamped.toString() else "",
                remainingSeconds = clamped * 60L,
                progressFraction = 0f,
                lastCompletedSession = null,
                status = SessionStatus.IDLE
            )
        }
        viewModelScope.launch {
            if (fromRecommendationId != null) {
                recommendationRepository.markRecommendationAccepted(fromRecommendationId)
            }
            focusRecoveryRepository.resolveRecovery()
            startSessionInternal(SessionType.FOCUS, clamped)
        }
    }

    fun restartShorterFromRecovery(minutes: Int) {
        val active = _uiState.value.activeSnapshot
        viewModelScope.launch {
            if (active != null && active.isActive) {
                val nowMs = System.currentTimeMillis()
                val progress = TimerCalculator.calculateProgress(active, nowMs)
                completeSessionInternal(active, SessionStatus.ENDED_EARLY, progress.elapsedSeconds)
                FocusTimerService.stop(appContext)
            }
            _uiState.update { it.copy(pendingRecoverySnapshot = null) }
            focusRecoveryRepository.resolveRecovery()
            startSessionInternal(SessionType.FOCUS, minutes.coerceIn(5, 60))
        }
    }

    fun dismissRecoveryCard() {
        viewModelScope.launch {
            focusRecoveryRepository.resolveRecovery()
        }
    }

    fun startSession() {
        if (_uiState.value.activeSnapshot?.isActive == true) return
        val state = _uiState.value
        viewModelScope.launch {
            focusRecoveryRepository.resolveRecovery()
            startSessionInternal(state.sessionType, state.selectedDurationMinutes)
        }
    }

    private suspend fun startSessionInternal(type: SessionType, durationMinutes: Int) {
        val state = _uiState.value
        val nowMs = System.currentTimeMillis()
        val snapshot = ActiveTimerSnapshot(
            sessionId = UUID.randomUUID().toString(),
            sessionType = type,
            plannedDurationSeconds = durationMinutes * 60L,
            startTimestampMs = nowMs,
            pauseStartedTimestampMs = null,
            accumulatedPausedMs = 0L,
            status = SessionStatus.RUNNING,
            subjectId = if (type == SessionType.FOCUS) state.selectedSubjectId else null,
            taskId = if (type == SessionType.FOCUS) state.selectedTaskId else null
        )
        _uiState.update {
            it.copy(
                sessionType = type,
                selectedDurationMinutes = durationMinutes,
                lastCompletedSession = null,
                pendingRecoverySnapshot = null
            )
        }
        focusSessionRepository.saveActiveTimerSnapshot(snapshot)
        FocusTimerService.startOrSync(appContext)
    }

    fun pauseSession() {
        val active = _uiState.value.activeSnapshot ?: return
        if (!SessionStateMachine.canTransition(active.status, SessionStatus.PAUSED)) return
        val paused = SessionStateMachine.pause(active, System.currentTimeMillis())
        viewModelScope.launch {
            focusSessionRepository.saveActiveTimerSnapshot(paused)
            FocusTimerService.startOrSync(appContext)
        }
    }

    fun resumeSession() {
        val active = _uiState.value.activeSnapshot ?: return
        if (!SessionStateMachine.canTransition(active.status, SessionStatus.RUNNING)) return
        val resumed = SessionStateMachine.resume(active, System.currentTimeMillis())
        viewModelScope.launch {
            _uiState.update { it.copy(pendingRecoverySnapshot = null) }
            focusSessionRepository.saveActiveTimerSnapshot(resumed)
            FocusTimerService.startOrSync(appContext)
        }
    }

    fun endSessionEarly() {
        val active = _uiState.value.activeSnapshot ?: return
        val nowMs = System.currentTimeMillis()
        val progress = TimerCalculator.calculateProgress(active, nowMs)
        viewModelScope.launch {
            completeSessionInternal(active, SessionStatus.ENDED_EARLY, progress.elapsedSeconds)
            _uiState.update { it.copy(pendingRecoverySnapshot = null) }
            FocusTimerService.stop(appContext)
        }
    }

    fun dismissRecoveryAndResume() {
        val pending = _uiState.value.pendingRecoverySnapshot ?: return
        _uiState.update { it.copy(pendingRecoverySnapshot = null) }
        if (pending.status == SessionStatus.PAUSED) {
            resumeSession()
        } else {
            FocusTimerService.startOrSync(appContext)
        }
    }

    fun resetCompletionScreen(switchToBreak: Boolean = false) {
        val nextType = if (switchToBreak) SessionType.BREAK else SessionType.FOCUS
        val defaultMinutes = if (nextType == SessionType.BREAK) 5 else 25
        _uiState.update {
            it.copy(
                sessionType = nextType,
                selectedDurationMinutes = defaultMinutes,
                isCustomDuration = false,
                remainingSeconds = defaultMinutes * 60L,
                progressFraction = 0f,
                lastCompletedSession = null,
                status = SessionStatus.IDLE
            )
        }
    }

    private suspend fun completeSessionInternal(
        snapshot: ActiveTimerSnapshot,
        finalStatus: SessionStatus,
        actualSeconds: Long
    ) {
        val nowMs = System.currentTimeMillis()
        val dailyTarget = onboardingRepository.preferencesFlow.first().dailyFocusTargetMinutes
        val recorded = focusSessionRepository.recordSessionCompletion(
            snapshot = snapshot,
            finalStatus = finalStatus,
            actualDurationSeconds = actualSeconds,
            completionTimestampMs = nowMs,
            localDate = DashboardStatsCalculator.localDateFromTimestamp(nowMs),
            dailyTargetMinutes = dailyTarget
        )

        if (finalStatus == SessionStatus.COMPLETED) {
            focusRecoveryRepository.resolveRecovery()
        } else if (finalStatus == SessionStatus.ENDED_EARLY && snapshot.sessionType == SessionType.FOCUS) {
            focusRecoveryRepository.createOrUpdateRecovery(
                lastSessionId = snapshot.sessionId,
                triggerReason = RecoveryTriggerReason.ENDED_EARLY,
                plannedDurationMinutes = (snapshot.plannedDurationSeconds / 60L).toInt().coerceAtLeast(1),
                actualDurationMinutes = (actualSeconds / 60L).toInt(),
                subjectId = snapshot.subjectId,
                taskId = snapshot.taskId
            )
        }

        _uiState.update {
            it.copy(
                activeSnapshot = null,
                lastCompletedSession = if (finalStatus == SessionStatus.COMPLETED) recorded else null,
                status = if (finalStatus == SessionStatus.COMPLETED) SessionStatus.COMPLETED else SessionStatus.IDLE,
                remainingSeconds = it.selectedDurationMinutes * 60L,
                progressFraction = 0f
            )
        }
    }
}
