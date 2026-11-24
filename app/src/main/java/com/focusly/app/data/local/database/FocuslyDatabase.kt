package com.focusly.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.focusly.app.data.local.dao.DailyGoalDao
import com.focusly.app.data.local.dao.DistractionSettingsDao
import com.focusly.app.data.local.dao.FocusRecommendationDao
import com.focusly.app.data.local.dao.FocusRecoveryDao
import com.focusly.app.data.local.dao.FocusSessionDao
import com.focusly.app.data.local.dao.RewardDao
import com.focusly.app.data.local.dao.StudyTaskDao
import com.focusly.app.data.local.dao.SubjectDao
import com.focusly.app.data.local.dao.UserProfileDao
import com.focusly.app.data.local.entity.AchievementUnlockEntity
import com.focusly.app.data.local.entity.DailyGoalEntity
import com.focusly.app.data.local.entity.DistractionSettingsEntity
import com.focusly.app.data.local.entity.FocusRecommendationEntity
import com.focusly.app.data.local.entity.FocusRecoveryStateEntity
import com.focusly.app.data.local.entity.FocusSessionEntity
import com.focusly.app.data.local.entity.RewardTransactionEntity
import com.focusly.app.data.local.entity.StudyTaskEntity
import com.focusly.app.data.local.entity.SubjectEntity
import com.focusly.app.data.local.entity.UnlockedRewardItemEntity
import com.focusly.app.data.local.entity.UserProfileEntity

@Database(
    entities = [
        FocusSessionEntity::class,
        SubjectEntity::class,
        StudyTaskEntity::class,
        DailyGoalEntity::class,
        UserProfileEntity::class,
        FocusRecommendationEntity::class,
        DistractionSettingsEntity::class,
        FocusRecoveryStateEntity::class,
        RewardTransactionEntity::class,
        AchievementUnlockEntity::class,
        UnlockedRewardItemEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class FocuslyDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun subjectDao(): SubjectDao
    abstract fun studyTaskDao(): StudyTaskDao
    abstract fun dailyGoalDao(): DailyGoalDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun focusRecommendationDao(): FocusRecommendationDao
    abstract fun distractionSettingsDao(): DistractionSettingsDao
    abstract fun focusRecoveryDao(): FocusRecoveryDao
    abstract fun rewardDao(): RewardDao

    companion object {
        const val DATABASE_NAME = "focusly_database.db"

        /**
         * Explicit forward migration registry. Never use destructive migrations in production.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_recommendations` (
                        `id` TEXT NOT NULL,
                        `recommendedMinutes` INTEGER NOT NULL,
                        `reasonType` TEXT NOT NULL,
                        `reasonText` TEXT NOT NULL,
                        `confidence` TEXT NOT NULL,
                        `subjectId` TEXT,
                        `preferredPeriod` TEXT,
                        `basedOnSessionCount` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `generatedAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `distraction_settings` (
                        `id` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `selectedPackageNamesCsv` TEXT NOT NULL,
                        `lastPermissionStatus` TEXT NOT NULL,
                        `lastCheckedAt` INTEGER,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_recovery_state` (
                        `id` TEXT NOT NULL,
                        `lastSessionId` TEXT,
                        `triggerReason` TEXT NOT NULL,
                        `plannedDurationMinutes` INTEGER NOT NULL,
                        `actualDurationMinutes` INTEGER NOT NULL,
                        `suggestedRetryMinutes` INTEGER NOT NULL,
                        `subjectId` TEXT,
                        `taskId` TEXT,
                        `isActive` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `resolvedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `study_tasks` ADD COLUMN `scheduledDate` TEXT")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_study_tasks_scheduledDate` ON `study_tasks` (`scheduledDate`)"
                )
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reward_transactions` (
                        `id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `referenceId` TEXT,
                        `localDate` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_reward_transactions_localDate` ON `reward_transactions` (`localDate`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_reward_transactions_createdAt` ON `reward_transactions` (`createdAt`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `achievement_unlocks` (
                        `achievementId` TEXT NOT NULL,
                        `unlockedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`achievementId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `unlocked_reward_items` (
                        `itemId` TEXT NOT NULL,
                        `purchasedAt` INTEGER NOT NULL,
                        `isFeatured` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`itemId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
    }
}
