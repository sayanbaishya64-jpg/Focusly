package com.focusly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusly.app.data.local.entity.AchievementUnlockEntity
import com.focusly.app.data.local.entity.RewardTransactionEntity
import com.focusly.app.data.local.entity.UnlockedRewardItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionIfAbsent(transaction: RewardTransactionEntity): Long

    @Query("SELECT * FROM reward_transactions ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentTransactions(limit: Int): Flow<List<RewardTransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM reward_transactions")
    fun observeCoinBalance(): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM reward_transactions")
    suspend fun getCoinBalanceSync(): Int

    @Query("SELECT COALESCE(SUM(amount), 0) FROM reward_transactions WHERE amount > 0")
    fun observeTotalEarnedCoins(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievementUnlockIfAbsent(unlock: AchievementUnlockEntity): Long

    @Query("SELECT * FROM achievement_unlocks ORDER BY unlockedAt DESC")
    fun observeAchievementUnlocks(): Flow<List<AchievementUnlockEntity>>

    @Query("SELECT achievementId FROM achievement_unlocks")
    suspend fun getUnlockedAchievementIdsSync(): List<String>

    @Query("SELECT COUNT(*) FROM achievement_unlocks")
    fun observeUnlockedAchievementCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlockedItemIfAbsent(item: UnlockedRewardItemEntity): Long

    @Query("SELECT * FROM unlocked_reward_items ORDER BY purchasedAt ASC")
    fun observeUnlockedItems(): Flow<List<UnlockedRewardItemEntity>>

    @Query("SELECT * FROM unlocked_reward_items WHERE itemId = :itemId LIMIT 1")
    suspend fun getUnlockedItemSync(itemId: String): UnlockedRewardItemEntity?

    @Query("UPDATE unlocked_reward_items SET isFeatured = 0")
    suspend fun clearAllFeaturedItems()

    @Query("UPDATE unlocked_reward_items SET isFeatured = 1 WHERE itemId = :itemId")
    suspend fun markFeaturedItem(itemId: String)

    @Query("SELECT referenceId FROM reward_transactions WHERE type = 'EARN_MISSION' AND localDate = :localDate AND referenceId IS NOT NULL")
    fun observeClaimedMissionIdsForDate(localDate: String): Flow<List<String>>

    @Query("SELECT * FROM reward_transactions ORDER BY createdAt DESC")
    suspend fun getAllTransactionsSync(): List<RewardTransactionEntity>

    @Query("SELECT * FROM achievement_unlocks ORDER BY unlockedAt DESC")
    suspend fun getAllAchievementsSync(): List<AchievementUnlockEntity>

    @Query("SELECT * FROM unlocked_reward_items ORDER BY purchasedAt ASC")
    suspend fun getAllUnlockedItemsSync(): List<UnlockedRewardItemEntity>

    @Query("DELETE FROM reward_transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM achievement_unlocks")
    suspend fun deleteAllAchievements()

    @Query("DELETE FROM unlocked_reward_items")
    suspend fun deleteAllUnlockedItems()
}
