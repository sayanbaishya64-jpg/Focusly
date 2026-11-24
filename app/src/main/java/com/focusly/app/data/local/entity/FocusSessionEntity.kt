package com.focusly.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = StudyTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["taskId"]),
        Index(value = ["localDate"]),
        Index(value = ["sessionStatus"])
    ]
)
data class FocusSessionEntity(
    @PrimaryKey
    val id: String,
    val plannedDurationSeconds: Long,
    val actualDurationSeconds: Long,
    val startTimestamp: Long,
    val completionTimestamp: Long?,
    val pausedDurationSeconds: Long,
    val sessionStatus: String,
    val sessionType: String,
    val localDate: String,
    val subjectId: String?,
    val taskId: String?,
    val createdAt: Long
)
