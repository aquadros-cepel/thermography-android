package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.ROIEntity
import com.tech.thermography.android.data.remote.mapper.ROIMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ROIRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<ROIEntity>() {
    private val roiDao = db.roiDao()

    fun getAllROIs(): Flow<List<ROIEntity>> = roiDao.getAllROIs()

    suspend fun getROIById(id: UUID): ROIEntity? = roiDao.getROIById(id)

    suspend fun insertROI(roi: ROIEntity) = roiDao.insertROI(roi)

    suspend fun updateROI(roi: ROIEntity) = roiDao.updateROI(roi)

    suspend fun deleteROI(roi: ROIEntity) = roiDao.deleteROI(roi)

    override suspend fun syncEntities() {
        val remoteEntities = syncApi.getAllROIs()
        val entities = remoteEntities.map { dto -> ROIMapper.dtoToEntity(dto) }
        setCache(entities)
    }

    override suspend fun insertCached() {
        roiDao.insertROIs(cache)
    }
}
