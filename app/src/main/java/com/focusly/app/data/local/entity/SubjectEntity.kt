package com.focusly.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subjects",
    indices = [Index(value = ["subjectName"], unique = true)]
)
data class SubjectEntity(
    @PrimaryKey
    val id: String,
    val subjectName: String,
    val subjectColor: Long,
    val createdAt: Long,
    val updatedAt: Long
)
