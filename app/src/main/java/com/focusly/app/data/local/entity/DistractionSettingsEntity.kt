package com.focusly.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "distraction_settings")
data class DistractionSettingsEntity(
    @PrimaryKey
    val id: String = SINGLETON_ID,
    val isEnabled: Boolean,
    val selectedPackageNamesCsv: String,
    val lastPermissionStatus: String,
    val lastCheckedAt: Long?,
    val updatedAt: Long
) {
    companion object {
        const val SINGLETON_ID = "default_distraction_settings"
    }
}
