package com.focusly.app.core.model

enum class StudentCategory(val key: String) {
    SCHOOL_STUDENT("school_student"),
    COLLEGE_STUDENT("college_student"),
    COMPETITIVE_ASPIRANT("competitive_aspirant"),
    SELF_LEARNER("self_learner");

    companion object {
        fun fromKey(key: String?): StudentCategory {
            return entries.firstOrNull { it.key == key } ?: COLLEGE_STUDENT
        }
    }
}

enum class StudyGoal(val key: String) {
    IMPROVE_CONCENTRATION("improve_concentration"),
    REDUCE_DISTRACTIONS("reduce_distractions"),
    BUILD_CONSISTENCY("build_consistency"),
    COMPLETE_DAILY_GOALS("complete_daily_goals");

    companion object {
        fun fromKeys(keys: Set<String>): Set<StudyGoal> {
            val mapped = keys.mapNotNull { key -> entries.firstOrNull { it.key == key } }.toSet()
            return mapped.ifEmpty { setOf(IMPROVE_CONCENTRATION) }
        }
    }
}

enum class AppLanguage(val languageTag: String) {
    ENGLISH("en"),
    BENGALI("bn");

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            return entries.firstOrNull { it.languageTag == tag } ?: ENGLISH
        }
    }
}

data class OnboardingPreferences(
    val onboardingCompleted: Boolean = false,
    val studentCategory: StudentCategory = StudentCategory.COLLEGE_STUDENT,
    val studyGoals: Set<StudyGoal> = setOf(StudyGoal.IMPROVE_CONCENTRATION),
    val dailyFocusTargetMinutes: Int = 60,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val selectedDistractionApps: Set<String> = emptySet(),
    val distractionPreferencesSkipped: Boolean = false
)
