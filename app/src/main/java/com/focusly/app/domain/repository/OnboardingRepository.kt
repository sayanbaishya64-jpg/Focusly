package com.focusly.app.domain.repository

import com.focusly.app.core.model.AppLanguage
import com.focusly.app.core.model.OnboardingPreferences
import com.focusly.app.core.model.StudentCategory
import com.focusly.app.core.model.StudyGoal
import kotlinx.coroutines.flow.Flow

interface OnboardingRepository {
    val preferencesFlow: Flow<OnboardingPreferences>

    suspend fun saveStudentCategory(category: StudentCategory)
    suspend fun saveStudyGoals(goals: Set<StudyGoal>)
    suspend fun saveDailyFocusTarget(minutes: Int)
    suspend fun saveLanguage(language: AppLanguage)
    suspend fun saveDistractionPreferences(apps: Set<String>, skipped: Boolean)
    suspend fun completeOnboarding()
    suspend fun resetOnboarding()
}
