package com.focusly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.focusly.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    fun observeProfile(id: String = UserProfileEntity.LOCAL_PROFILE_ID): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = :id LIMIT 1")
    suspend fun getProfileSync(id: String = UserProfileEntity.LOCAL_PROFILE_ID): UserProfileEntity?

    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
}
