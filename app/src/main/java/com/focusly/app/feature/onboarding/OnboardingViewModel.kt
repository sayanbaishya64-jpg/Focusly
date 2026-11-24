package com.focusly.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusly.app.core.model.AppLanguage
import com.focusly.app.core.model.OnboardingPreferences
import com.focusly.app.core.model.StudentCategory
import com.focusly.app.core.model.StudyGoal
import com.focusly.app.domain.repository.OnboardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val studentCategory: StudentCategory = StudentCategory.COLLEGE_STUDENT,
    val studyGoals: Set<StudyGoal> = setOf(StudyGoal.IMPROVE_CONCENTRATION),
    val dailyFocusTargetMinutes: Int = 60,
    val customMinutesInput: String = "",
    val isCustomTargetSelected: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val selectedDistractionApps: Set<String> = emptySet(),
    val onboardingCompleted: Boolean = false
) {
    fun toPreferences(): OnboardingPreferences = OnboardingPreferences(
        onboardingCompleted = onboardingCompleted,
        studentCategory = studentCategory,
        studyGoals = studyGoals,
        dailyFocusTargetMinutes = dailyFocusTargetMinutes,
        language = language,
        selectedDistractionApps = selectedDistractionApps,
        distractionPreferencesSkipped = selectedDistractionApps.isEmpty()
    )
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = onboardingRepository.preferencesFlow.first()
            _uiState.update {
                it.copy(
                    studentCategory = saved.studentCategory,
                    studyGoals = saved.studyGoals,
                    dailyFocusTargetMinutes = saved.dailyFocusTargetMinutes,
                    language = saved.language,
                    selectedDistractionApps = saved.selectedDistractionApps,
                    onboardingCompleted = saved.onboardingCompleted
                )
            }
        }
    }

    fun selectCategory(category: StudentCategory) {
        _uiState.update { it.copy(studentCategory = category) }
    }

    fun persistCategory() {
        val category = _uiState.value.studentCategory
        viewModelScope.launch { onboardingRepository.saveStudentCategory(category) }
    }

    fun toggleGoal(goal: StudyGoal) {
        _uiState.update { state ->
            val updated = state.studyGoals.toMutableSet()
            if (goal in updated && updated.size > 1) {
                updated.remove(goal)
            } else {
                updated.add(goal)
            }
            state.copy(studyGoals = updated)
        }
    }

    fun persistGoals() {
        val goals = _uiState.value.studyGoals
        viewModelScope.launch { onboardingRepository.saveStudyGoals(goals) }
    }

    fun selectTargetPreset(minutes: Int) {
        _uiState.update {
            it.copy(
                dailyFocusTargetMinutes = minutes,
                isCustomTargetSelected = false
            )
        }
    }

    fun updateCustomMinutes(input: String) {
        val digits = input.filter { it.isDigit() }.take(3)
        val parsed = digits.toIntOrNull()
        _uiState.update {
            it.copy(
                isCustomTargetSelected = true,
                customMinutesInput = digits,
                dailyFocusTargetMinutes = if (parsed != null && parsed in 10..480) parsed else it.dailyFocusTargetMinutes
            )
        }
    }

    fun persistDailyTarget() {
        val minutes = _uiState.value.dailyFocusTargetMinutes.coerceIn(10, 480)
        viewModelScope.launch { onboardingRepository.saveDailyFocusTarget(minutes) }
    }

    fun selectLanguage(language: AppLanguage) {
        _uiState.update { it.copy(language = language) }
    }

    fun persistLanguage() {
        val lang = _uiState.value.language
        viewModelScope.launch { onboardingRepository.saveLanguage(lang) }
    }

    fun toggleDistractionApp(packageName: String) {
        _uiState.update { state ->
            val updated = state.selectedDistractionApps.toMutableSet()
            if (packageName in updated) updated.remove(packageName) else updated.add(packageName)
            state.copy(selectedDistractionApps = updated)
        }
    }

    fun completeOnboarding(skippedDistractions: Boolean, onDone: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            onboardingRepository.saveStudentCategory(state.studentCategory)
            onboardingRepository.saveStudyGoals(state.studyGoals)
            onboardingRepository.saveDailyFocusTarget(state.dailyFocusTargetMinutes)
            onboardingRepository.saveLanguage(state.language)
            onboardingRepository.saveDistractionPreferences(
                apps = if (skippedDistractions) emptySet() else state.selectedDistractionApps,
                skipped = skippedDistractions
            )
            onboardingRepository.completeOnboarding()
            _uiState.update { it.copy(onboardingCompleted = true) }
            onDone()
        }
    }

    fun refreshFromPreferences() {
        viewModelScope.launch {
            val saved = onboardingRepository.preferencesFlow.first()
            _uiState.update {
                it.copy(
                    studentCategory = saved.studentCategory,
                    studyGoals = saved.studyGoals,
                    dailyFocusTargetMinutes = saved.dailyFocusTargetMinutes,
                    language = saved.language,
                    selectedDistractionApps = saved.selectedDistractionApps,
                    onboardingCompleted = saved.onboardingCompleted
                )
            }
        }
    }

    fun resetAllLocalData(onReset: () -> Unit) {
        viewModelScope.launch {
            onboardingRepository.resetOnboarding()
            _uiState.value = OnboardingUiState()
            onReset()
        }
    }
}
