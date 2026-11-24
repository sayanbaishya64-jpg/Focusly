package com.focusly.app.data.repository

import com.focusly.app.core.model.AppLanguage
import com.focusly.app.core.model.OnboardingPreferences
import com.focusly.app.core.model.StudentCategory
import com.focusly.app.core.model.StudyGoal
import com.focusly.app.data.local.dao.DailyGoalDao
import com.focusly.app.data.local.dao.FocusSessionDao
import com.focusly.app.data.local.dao.RewardDao
import com.focusly.app.data.local.dao.StudyTaskDao
import com.focusly.app.data.local.dao.SubjectDao
import com.focusly.app.data.local.dao.UserProfileDao
import com.focusly.app.data.preferences.UserPreferencesDataStore
import com.focusly.app.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val dataStore: UserPreferencesDataStore,
    private val focusSessionDao: FocusSessionDao,
    private val studyTaskDao: StudyTaskDao,
    private val subjectDao: SubjectDao,
    private val userProfileDao: UserProfileDao,
    private val dailyGoalDao: DailyGoalDao,
    private val rewardDao: RewardDao
) : OnboardingRepository {

    override val preferencesFlow: Flow<OnboardingPreferences> = dataStore.preferencesFlow

    override suspend fun saveStudentCategory(category: StudentCategory) {
        dataStore.setStudentCategory(category)
    }

    override suspend fun saveStudyGoals(goals: Set<StudyGoal>) {
        dataStore.setStudyGoals(goals)
    }

    override suspend fun saveDailyFocusTarget(minutes: Int) {
        dataStore.setDailyFocusTarget(minutes)
    }

    override suspend fun saveLanguage(language: AppLanguage) {
        dataStore.setLanguage(language)
    }

    override suspend fun saveDistractionPreferences(apps: Set<String>, skipped: Boolean) {
        dataStore.setDistractionPreferences(apps, skipped)
    }

    override suspend fun completeOnboarding() {
        dataStore.setOnboardingCompleted(true)
    }

    override suspend fun resetOnboarding() {
        dataStore.clearAll()
        focusSessionDao.deleteAll()
        studyTaskDao.deleteAll()
        subjectDao.deleteAll()
        userProfileDao.deleteAll()
        dailyGoalDao.deleteAll()
        rewardDao.deleteAllTransactions()
        rewardDao.deleteAllAchievements()
        rewardDao.deleteAllUnlockedItems()
    }
}
