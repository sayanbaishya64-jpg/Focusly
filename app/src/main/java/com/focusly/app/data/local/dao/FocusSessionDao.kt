package com.focusly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusly.app.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(session: FocusSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE localDate = :localDate ORDER BY startTimestamp DESC")
    fun observeSessionsForDate(localDate: String): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE localDate BETWEEN :startDate AND :endDate ORDER BY startTimestamp DESC")
    fun observeSessionsInRange(startDate: String, endDate: String): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startTimestamp DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE sessionType = 'FOCUS' ORDER BY startTimestamp DESC LIMIT :limit")
    fun observeFocusSessionsHistory(limit: Int): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE sessionType = 'FOCUS' AND sessionStatus = 'COMPLETED' ORDER BY startTimestamp DESC")
    fun observeCompletedFocusSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE sessionType = 'FOCUS' AND sessionStatus = 'COMPLETED' ORDER BY startTimestamp DESC")
    suspend fun getCompletedFocusSessionsSync(): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions WHERE sessionStatus IN ('RUNNING', 'PAUSED') LIMIT 1")
    suspend fun getUnfinishedSession(): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions ORDER BY startTimestamp DESC")
    suspend fun getAllSessionsSync(): List<FocusSessionEntity>

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()
}
