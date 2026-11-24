package com.focusly.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: String = LOCAL_PROFILE_ID,
    val studentName: String,
    val updatedAt: Long
) {
    companion object {
        const val LOCAL_PROFILE_ID = "local_student_profile"
    }
}
