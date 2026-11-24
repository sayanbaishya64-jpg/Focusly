package com.focusly.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_recommendations")
data class FocusRecommendationEntity(
    @PrimaryKey
    val id: String,
    val recommendedMinutes: Int,
    val reasonType: String,
    val reasonText: String,
    val confidence: String,
    val subjectId: String?,
    val preferredPeriod: String?,
    val basedOnSessionCount: Int,
    val status: String,
    val generatedAt: Long,
    val updatedAt: Long
)
