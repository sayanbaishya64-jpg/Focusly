package com.focusly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusly.app.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subject: SubjectEntity)

    @Query("SELECT * FROM subjects ORDER BY subjectName ASC")
    fun observeAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE LOWER(subjectName) = LOWER(:name) LIMIT 1")
    suspend fun getByNameIgnoreCase(name: String): SubjectEntity?

    @Query("UPDATE subjects SET subjectName = :name, subjectColor = :colorHex, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSubject(id: String, name: String, colorHex: Long, updatedAt: Long)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM subjects ORDER BY subjectName ASC")
    suspend fun getAllSubjectsSync(): List<SubjectEntity>

    @Query("DELETE FROM subjects")
    suspend fun deleteAll()
}
