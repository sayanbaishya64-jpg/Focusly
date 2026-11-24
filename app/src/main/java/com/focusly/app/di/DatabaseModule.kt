package com.focusly.app.di

import android.content.Context
import androidx.room.Room
import com.focusly.app.data.local.dao.DailyGoalDao
import com.focusly.app.data.local.dao.DistractionSettingsDao
import com.focusly.app.data.local.dao.FocusRecommendationDao
import com.focusly.app.data.local.dao.FocusRecoveryDao
import com.focusly.app.data.local.dao.FocusSessionDao
import com.focusly.app.data.local.dao.RewardDao
import com.focusly.app.data.local.dao.StudyTaskDao
import com.focusly.app.data.local.dao.SubjectDao
import com.focusly.app.data.local.dao.UserProfileDao
import com.focusly.app.data.local.database.FocuslyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFocuslyDatabase(
        @ApplicationContext context: Context
    ): FocuslyDatabase {
        return Room.databaseBuilder(
            context,
            FocuslyDatabase::class.java,
            FocuslyDatabase.DATABASE_NAME
        )
            .addMigrations(*FocuslyDatabase.ALL_MIGRATIONS)
            .build()
    }

    @Provides
    fun provideFocusSessionDao(database: FocuslyDatabase): FocusSessionDao =
        database.focusSessionDao()

    @Provides
    fun provideSubjectDao(database: FocuslyDatabase): SubjectDao =
        database.subjectDao()

    @Provides
    fun provideStudyTaskDao(database: FocuslyDatabase): StudyTaskDao =
        database.studyTaskDao()

    @Provides
    fun provideDailyGoalDao(database: FocuslyDatabase): DailyGoalDao =
        database.dailyGoalDao()

    @Provides
    fun provideUserProfileDao(database: FocuslyDatabase): UserProfileDao =
        database.userProfileDao()

    @Provides
    fun provideFocusRecommendationDao(database: FocuslyDatabase): FocusRecommendationDao =
        database.focusRecommendationDao()

    @Provides
    fun provideDistractionSettingsDao(database: FocuslyDatabase): DistractionSettingsDao =
        database.distractionSettingsDao()

    @Provides
    fun provideFocusRecoveryDao(database: FocuslyDatabase): FocusRecoveryDao =
        database.focusRecoveryDao()

    @Provides
    fun provideRewardDao(database: FocuslyDatabase): RewardDao =
        database.rewardDao()
}
