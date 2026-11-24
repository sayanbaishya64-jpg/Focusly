package com.focusly.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_recovery_state")
data class FocusRecoveryStateEntity(
    @PrimaryKey
    val id: String = SINGLETON_ID,
    val lastSessionId: String?,
    val triggerReason: String,
    val plannedDurationMinutes: Int,
    val actualDurationMinutes: Int,
    val suggestedRetryMinutes: Int,
    val subjectId: String?,
    val taskId: String?,
    val isActive: Boolean,
    val createdAt: Long,
    val resolvedAt: Long?
) {
    companion object {
        const val SINGLETON_ID = "active_recovery_state"
    }
}
