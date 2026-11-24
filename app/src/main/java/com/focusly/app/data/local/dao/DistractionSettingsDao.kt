package com.focusly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusly.app.data.local.entity.DistractionSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DistractionSettingsDao {

    @Query("SELECT * FROM distraction_settings WHERE id = :id LIMIT 1")
    fun observeSettings(id: String = DistractionSettingsEntity.SINGLETON_ID): Flow<DistractionSettingsEntity?>

    @Query("SELECT * FROM distraction_settings WHERE id = :id LIMIT 1")
    suspend fun getSettings(id: String = DistractionSettingsEntity.SINGLETON_ID): DistractionSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DistractionSettingsEntity)

    @Query("DELETE FROM distraction_settings")
    suspend fun deleteAll()
}
