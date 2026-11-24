package com.focusly.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_goals",
    indices = [Index(value = ["goalDate"], unique = true)]
)
data class DailyGoalEntity(
    @PrimaryKey
    val id: String,
    val targetMinutes: Int,
    val goalDate: String,
    val createdAt: Long
)
