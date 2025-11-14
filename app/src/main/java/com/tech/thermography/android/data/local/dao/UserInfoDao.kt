package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.UserInfoEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface UserInfoDao {
    @Query("SELECT * FROM user_info ORDER BY position")
    fun getAllUserInfos(): Flow<List<UserInfoEntity>>

    @Query("SELECT * FROM user_info WHERE id = :id")
    suspend fun getUserInfoById(id: UUID): UserInfoEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUserInfo(userInfo: UserInfoEntity)
    
    @Update
    suspend fun updateUserInfo(userInfo: UserInfoEntity)
    
    @Delete
    suspend fun deleteUserInfo(userInfo: UserInfoEntity)
}
