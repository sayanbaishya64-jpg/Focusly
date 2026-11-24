package com.focusly.app.di

import com.focusly.app.data.repository.DistractionBlockerRepositoryImpl
import com.focusly.app.data.repository.FocusRecoveryRepositoryImpl
import com.focusly.app.data.repository.FocusSessionRepositoryImpl
import com.focusly.app.data.repository.LocalBackupRepositoryImpl
import com.focusly.app.data.repository.OnboardingRepositoryImpl
import com.focusly.app.data.repository.RecommendationRepositoryImpl
import com.focusly.app.data.repository.RewardsRepositoryImpl
import com.focusly.app.data.repository.StudyTaskRepositoryImpl
import com.focusly.app.data.repository.SubjectRepositoryImpl
import com.focusly.app.data.repository.UserProfileRepositoryImpl
import com.focusly.app.domain.repository.DistractionBlockerRepository
import com.focusly.app.domain.repository.FocusRecoveryRepository
import com.focusly.app.domain.repository.FocusSessionRepository
import com.focusly.app.domain.repository.LocalBackupRepository
import com.focusly.app.domain.repository.OnboardingRepository
import com.focusly.app.domain.repository.RecommendationRepository
import com.focusly.app.domain.repository.RewardPreviewProvider
import com.focusly.app.domain.repository.RewardsRepository
import com.focusly.app.domain.repository.StudyTaskRepository
import com.focusly.app.domain.repository.SubjectRepository
import com.focusly.app.domain.repository.UserProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(
        impl: OnboardingRepositoryImpl
    ): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindFocusSessionRepository(
        impl: FocusSessionRepositoryImpl
    ): FocusSessionRepository

    @Binds
    @Singleton
    abstract fun bindSubjectRepository(
        impl: SubjectRepositoryImpl
    ): SubjectRepository

    @Binds
    @Singleton
    abstract fun bindStudyTaskRepository(
        impl: StudyTaskRepositoryImpl
    ): StudyTaskRepository

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(
        impl: UserProfileRepositoryImpl
    ): UserProfileRepository

    @Binds
    @Singleton
    abstract fun bindRewardPreviewProvider(
        impl: RewardsRepositoryImpl
    ): RewardPreviewProvider

    @Binds
    @Singleton
    abstract fun bindRewardsRepository(
        impl: RewardsRepositoryImpl
    ): RewardsRepository

    @Binds
    @Singleton
    abstract fun bindRecommendationRepository(
        impl: RecommendationRepositoryImpl
    ): RecommendationRepository

    @Binds
    @Singleton
    abstract fun bindDistractionBlockerRepository(
        impl: DistractionBlockerRepositoryImpl
    ): DistractionBlockerRepository

    @Binds
    @Singleton
    abstract fun bindFocusRecoveryRepository(
        impl: FocusRecoveryRepositoryImpl
    ): FocusRecoveryRepository

    @Binds
    @Singleton
    abstract fun bindLocalBackupRepository(
        impl: LocalBackupRepositoryImpl
    ): LocalBackupRepository
}
