package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.UserInfoEntity
import com.tech.thermography.android.data.remote.mapper.UserInfoMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserInfoRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<UserInfoEntity>() {
    private val userInfoDao = db.userInfoDao()

    fun getAllUserInfos(): Flow<List<UserInfoEntity>> = userInfoDao.getAllUserInfos()

    suspend fun getUserInfoById(id: UUID): UserInfoEntity? = userInfoDao.getUserInfoById(id)

    suspend fun insertUserInfo(userInfo: UserInfoEntity) = userInfoDao.insertUserInfo(userInfo)

    suspend fun updateUserInfo(userInfo: UserInfoEntity) = userInfoDao.updateUserInfo(userInfo)

    suspend fun deleteUserInfo(userInfo: UserInfoEntity) = userInfoDao.deleteUserInfo(userInfo)

    override suspend fun syncEntities() {
        val remoteEntities = syncApi.getAllUserInfos()
        val entities = remoteEntities.map { dto -> UserInfoMapper.dtoToEntity(dto) }
        setCache(entities)
    }

    override suspend fun insertCached() {
        userInfoDao.insertUserInfos(cache)
    }
}
