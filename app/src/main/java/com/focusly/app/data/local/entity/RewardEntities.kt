package com.focusly.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reward_transactions",
    indices = [
        Index(value = ["localDate"]),
        Index(value = ["createdAt"])
    ]
)
data class RewardTransactionEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val amount: Int,
    val title: String,
    val referenceId: String? = null,
    val localDate: String,
    val createdAt: Long
)

@Entity(tableName = "achievement_unlocks")
data class AchievementUnlockEntity(
    @PrimaryKey
    val achievementId: String,
    val unlockedAt: Long
)

@Entity(tableName = "unlocked_reward_items")
data class UnlockedRewardItemEntity(
    @PrimaryKey
    val itemId: String,
    val purchasedAt: Long,
    val isFeatured: Boolean = false
)
