package com.focusly.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focusly.app.core.model.AppLanguage
import com.focusly.app.core.model.OnboardingPreferences
import com.focusly.app.core.model.StudentCategory
import com.focusly.app.core.model.StudyGoal
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.focuslyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "focusly_preferences"
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val STUDENT_CATEGORY = stringPreferencesKey("student_category")
        val STUDY_GOALS = stringSetPreferencesKey("study_goals")
        val DAILY_FOCUS_TARGET_MINUTES = intPreferencesKey("daily_focus_target_minutes")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val DISTRACTION_APPS = stringSetPreferencesKey("distraction_apps")
        val DISTRACTION_SKIPPED = booleanPreferencesKey("distraction_skipped")
    }

    val preferencesFlow: Flow<OnboardingPreferences> = context.focuslyDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            OnboardingPreferences(
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                studentCategory = StudentCategory.fromKey(prefs[Keys.STUDENT_CATEGORY]),
                studyGoals = StudyGoal.fromKeys(prefs[Keys.STUDY_GOALS] ?: emptySet()),
                dailyFocusTargetMinutes = prefs[Keys.DAILY_FOCUS_TARGET_MINUTES] ?: 60,
                language = AppLanguage.fromTag(prefs[Keys.APP_LANGUAGE]),
                selectedDistractionApps = prefs[Keys.DISTRACTION_APPS] ?: emptySet(),
                distractionPreferencesSkipped = prefs[Keys.DISTRACTION_SKIPPED] ?: false
            )
        }

    suspend fun setStudentCategory(category: StudentCategory) {
        context.focuslyDataStore.edit { prefs ->
            prefs[Keys.STUDENT_CATEGORY] = category.key
        }
    }

    suspend fun setStudyGoals(goals: Set<StudyGoal>) {
        context.focuslyDataStore.edit { prefs ->
            prefs[Keys.STUDY_GOALS] = goals.map { it.key }.toSet()
        }
    }

    suspend fun setDailyFocusTarget(minutes: Int) {
        val clamped = minutes.coerceIn(10, 480)
        context.focuslyDataStore.edit { prefs ->
            prefs[Keys.DAILY_FOCUS_TARGET_MINUTES] = clamped
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.focuslyDataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = language.languageTag
        }
    }

    suspend fun setDistractionPreferences(apps: Set<String>, skipped: Boolean) {
        context.focuslyDataStore.edit { prefs ->
            prefs[Keys.DISTRACTION_APPS] = apps
            prefs[Keys.DISTRACTION_SKIPPED] = skipped
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.focuslyDataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun clearAll() {
        context.focuslyDataStore.edit { it.clear() }
    }
}
