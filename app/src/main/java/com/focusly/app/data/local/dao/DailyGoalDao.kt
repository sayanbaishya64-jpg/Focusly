package com.focusly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusly.app.data.local.entity.DailyGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: DailyGoalEntity)

    @Query("SELECT * FROM daily_goals WHERE goalDate = :goalDate LIMIT 1")
    fun observeGoalForDate(goalDate: String): Flow<DailyGoalEntity?>

    @Query("SELECT * FROM daily_goals WHERE goalDate = :goalDate LIMIT 1")
    suspend fun getGoalForDate(goalDate: String): DailyGoalEntity?

    @Query("DELETE FROM daily_goals")
    suspend fun deleteAll()
}
