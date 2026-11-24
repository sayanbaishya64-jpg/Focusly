package com.focusly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusly.app.data.local.entity.FocusRecoveryStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusRecoveryDao {

    @Query("SELECT * FROM focus_recovery_state WHERE id = :id LIMIT 1")
    fun observeRecoveryState(id: String = FocusRecoveryStateEntity.SINGLETON_ID): Flow<FocusRecoveryStateEntity?>

    @Query("SELECT * FROM focus_recovery_state WHERE id = :id LIMIT 1")
    suspend fun getRecoveryState(id: String = FocusRecoveryStateEntity.SINGLETON_ID): FocusRecoveryStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FocusRecoveryStateEntity)

    @Query("UPDATE focus_recovery_state SET isActive = 0, resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun clearActive(resolvedAt: Long, id: String = FocusRecoveryStateEntity.SINGLETON_ID)

    @Query("DELETE FROM focus_recovery_state")
    suspend fun deleteAll()
}
