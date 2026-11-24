package com.focusly.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_tasks",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["status"]),
        Index(value = ["scheduledDate"])
    ]
)
data class StudyTaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val subjectId: String?,
    val plannedDurationMinutes: Int,
    val priority: String,
    val status: String,
    val scheduledDate: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
