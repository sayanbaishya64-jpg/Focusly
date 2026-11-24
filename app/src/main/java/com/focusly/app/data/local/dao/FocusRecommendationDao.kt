package com.focusly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusly.app.data.local.entity.FocusRecommendationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusRecommendationDao {

    @Query("SELECT * FROM focus_recommendations ORDER BY generatedAt DESC LIMIT 1")
    fun observeLatestRecommendation(): Flow<FocusRecommendationEntity?>

    @Query("SELECT * FROM focus_recommendations ORDER BY generatedAt DESC LIMIT 1")
    suspend fun getLatestRecommendation(): FocusRecommendationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FocusRecommendationEntity)

    @Query("UPDATE focus_recommendations SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)

    @Query("DELETE FROM focus_recommendations")
    suspend fun deleteAll()
}
