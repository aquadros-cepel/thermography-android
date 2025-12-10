package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.ThermogramEntity
import com.tech.thermography.android.data.remote.mapper.ThermogramMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermogramRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<ThermogramEntity>() {
    private val thermogramDao = db.thermogramDao()

    fun getAllThermograms(): Flow<List<ThermogramEntity>> = thermogramDao.getAllThermograms()

    suspend fun getThermogramById(id: UUID): ThermogramEntity? = thermogramDao.getThermogramById(id)

    suspend fun insertThermogram(thermogram: ThermogramEntity) = thermogramDao.insertThermogram(thermogram)

    suspend fun updateThermogram(thermogram: ThermogramEntity) = thermogramDao.updateThermogram(thermogram)

    suspend fun deleteThermogram(thermogram: ThermogramEntity) = thermogramDao.deleteThermogram(thermogram)

    override suspend fun syncEntities() {
        val remoteEntities = syncApi.getAllThermograms()
        val entities = remoteEntities.map { dto -> ThermogramMapper.dtoToEntity(dto) }
        setCache(entities)
    }

    override suspend fun insertCached() {
        thermogramDao.insertThermograms(cache)
    }
}
