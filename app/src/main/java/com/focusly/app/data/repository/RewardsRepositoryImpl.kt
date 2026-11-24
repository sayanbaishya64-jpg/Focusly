package com.focusly.app.data.repository

import androidx.room.withTransaction
import com.focusly.app.data.local.dao.FocusSessionDao
import com.focusly.app.data.local.dao.RewardDao
import com.focusly.app.data.local.database.FocuslyDatabase
import com.focusly.app.data.local.entity.RewardTransactionEntity
import com.focusly.app.data.local.entity.UnlockedRewardItemEntity
import com.focusly.app.domain.model.AchievementItem
import com.focusly.app.domain.model.FocusSession
import com.focusly.app.domain.model.GardenStoreItem
import com.focusly.app.domain.model.RewardPreview
import com.focusly.app.domain.model.RewardPurchaseResult
import com.focusly.app.domain.model.RewardTransaction
import com.focusly.app.domain.model.RewardTransactionType
import com.focusly.app.domain.model.RewardsDashboardState
import com.focusly.app.domain.model.SessionStatus
import com.focusly.app.domain.model.SessionType
import com.focusly.app.domain.repository.RewardPreviewProvider
import com.focusly.app.domain.repository.RewardsRepository
import com.focusly.app.domain.usecase.RewardEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardsRepositoryImpl @Inject constructor(
    private val database: FocuslyDatabase,
    private val rewardDao: RewardDao,
    private val focusSessionDao: FocusSessionDao
) : RewardsRepository, RewardPreviewProvider {

    override fun observeRewardPreview(): Flow<RewardPreview> {
        return combine(
            rewardDao.observeCoinBalance(),
            rewardDao.observeUnlockedAchievementCount()
        ) { coins, unlockedCount ->
            RewardPreview(
                focusCoins = coins.coerceAtLeast(0),
                unlockedAchievements = unlockedCount
            )
        }
    }

    override fun observeRewardsDashboard(): Flow<RewardsDashboardState> {
        return combine(
            rewardDao.observeCoinBalance(),
            rewardDao.observeTotalEarnedCoins(),
            rewardDao.observeAchievementUnlocks(),
            rewardDao.observeUnlockedItems(),
            rewardDao.observeRecentTransactions(20),
            focusSessionDao.observeCompletedFocusSessions()
        ) { values ->
            val balance = (values[0] as Int).coerceAtLeast(0)
            val totalEarned = (values[1] as Int).coerceAtLeast(0)
            @Suppress("UNCHECKED_CAST")
            val unlockedAchievements = values[2] as List<com.focusly.app.data.local.entity.AchievementUnlockEntity>
            @Suppress("UNCHECKED_CAST")
            val unlockedItems = values[3] as List<UnlockedRewardItemEntity>
            @Suppress("UNCHECKED_CAST")
            val transactions = values[4] as List<RewardTransactionEntity>
            @Suppress("UNCHECKED_CAST")
            val sessionEntities = values[5] as List<com.focusly.app.data.local.entity.FocusSessionEntity>

            val unlockMap = unlockedAchievements.associateBy { it.achievementId }
            val unlockedItemMap = unlockedItems.associateBy { it.itemId }

            val domainSessions = sessionEntities.map {
                FocusSession(
                    id = it.id,
                    plannedDurationSeconds = it.plannedDurationSeconds,
                    actualDurationSeconds = it.actualDurationSeconds,
                    startTimestamp = it.startTimestamp,
                    completionTimestamp = it.completionTimestamp,
                    pausedDurationSeconds = it.pausedDurationSeconds,
                    status = SessionStatus.COMPLETED,
                    type = SessionType.FOCUS,
                    localDate = it.localDate,
                    subjectId = it.subjectId,
                    taskId = it.taskId,
                    createdAt = it.createdAt
                )
            }

            val completedCount = domainSessions.distinctBy { it.id }.size
            val (currentStreak, _) = RewardEngine.computeStreaks(domainSessions, LocalDate.now())
            val growthStage = RewardEngine.computeGrowthStage(completedCount)

            val achievementItems = RewardEngine.ACHIEVEMENTS_CATALOG.map { def ->
                val unlocked = unlockMap[def.id]
                AchievementItem(
                    id = def.id,
                    title = def.title,
                    description = def.description,
                    coinBonus = def.coinBonus,
                    isUnlocked = unlocked != null,
                    unlockedAt = unlocked?.unlockedAt
                )
            }

            val hasAnyFeatured = unlockedItems.any { it.isFeatured }
            val storeItems = RewardEngine.GARDEN_STORE_CATALOG.map { def ->
                val owned = unlockedItemMap[def.id]
                val isStarterDefault = def.costCoins == 0
                GardenStoreItem(
                    id = def.id,
                    title = def.title,
                    description = def.description,
                    costCoins = def.costCoins,
                    accentHex = def.accentHex,
                    isUnlocked = owned != null || isStarterDefault,
                    isFeatured = owned?.isFeatured == true || (!hasAnyFeatured && def.id == "sunlit_sprout"),
                    growthStage = growthStage
                )
            }

            val recentTx = transactions.map { tx ->
                RewardTransaction(
                    id = tx.id,
                    type = runCatching { RewardTransactionType.valueOf(tx.type) }
                        .getOrDefault(RewardTransactionType.EARN_SESSION),
                    amount = tx.amount,
                    title = tx.title,
                    referenceId = tx.referenceId,
                    localDate = tx.localDate,
                    createdAt = tx.createdAt
                )
            }

            RewardsDashboardState(
                coinBalance = balance,
                totalEarnedCoins = totalEarned,
                unlockedAchievementsCount = unlockMap.size,
                totalAchievementsCount = RewardEngine.ACHIEVEMENTS_CATALOG.size,
                completedFocusSessionsCount = completedCount,
                currentStreakDays = currentStreak,
                achievements = achievementItems,
                storeItems = storeItems,
                recentTransactions = recentTx
            )
        }
    }

    override fun observeClaimedMissionIdsForDate(localDate: String): Flow<Set<String>> {
        return rewardDao.observeClaimedMissionIdsForDate(localDate).map { it.toSet() }
    }

    override suspend fun claimDailyMission(missionId: String, localDate: String): Boolean {
        val definition = com.focusly.app.domain.usecase.DailyMissionsEngine.MISSIONS_CATALOG
            .firstOrNull { it.id == missionId } ?: return false

        return database.withTransaction {
            val txId = "claim_mission_${localDate}_${definition.id}"
            val inserted = rewardDao.insertTransactionIfAbsent(
                RewardTransactionEntity(
                    id = txId,
                    type = RewardTransactionType.EARN_MISSION.name,
                    amount = definition.rewardCoins,
                    title = "Daily Mission: ${definition.title}",
                    referenceId = definition.id,
                    localDate = localDate,
                    createdAt = System.currentTimeMillis()
                )
            )
            inserted != -1L
        }
    }

    override suspend fun purchaseStoreItem(itemId: String, localDate: String): RewardPurchaseResult {
        val definition = RewardEngine.GARDEN_STORE_CATALOG.firstOrNull { it.id == itemId }
            ?: return RewardPurchaseResult.NotFound

        return database.withTransaction {
            val existing = rewardDao.getUnlockedItemSync(itemId)
            if (existing != null || definition.costCoins == 0) {
                return@withTransaction RewardPurchaseResult.AlreadyUnlocked
            }
            val currentBalance = rewardDao.getCoinBalanceSync()
            if (currentBalance < definition.costCoins) {
                return@withTransaction RewardPurchaseResult.InsufficientCoins
            }

            val now = System.currentTimeMillis()
            if (definition.costCoins > 0) {
                rewardDao.insertTransactionIfAbsent(
                    RewardTransactionEntity(
                        id = "spend_store_${definition.id}",
                        type = RewardTransactionType.SPEND_STORE.name,
                        amount = -definition.costCoins,
                        title = "Unlocked ${definition.title}",
                        referenceId = definition.id,
                        localDate = localDate,
                        createdAt = now
                    )
                )
            }

            rewardDao.clearAllFeaturedItems()
            rewardDao.insertUnlockedItemIfAbsent(
                UnlockedRewardItemEntity(
                    itemId = definition.id,
                    purchasedAt = now,
                    isFeatured = true
                )
            )
            RewardPurchaseResult.Success
        }
    }

    override suspend fun setFeaturedGardenItem(itemId: String) {
        val definition = RewardEngine.GARDEN_STORE_CATALOG.firstOrNull { it.id == itemId } ?: return
        database.withTransaction {
            val existing = rewardDao.getUnlockedItemSync(itemId)
            if (existing == null && definition.costCoins > 0) {
                return@withTransaction
            }
            rewardDao.clearAllFeaturedItems()
            if (existing == null) {
                rewardDao.insertUnlockedItemIfAbsent(
                    UnlockedRewardItemEntity(
                        itemId = definition.id,
                        purchasedAt = System.currentTimeMillis(),
                        isFeatured = true
                    )
                )
            } else {
                rewardDao.markFeaturedItem(existing.itemId)
            }
        }
    }
}
