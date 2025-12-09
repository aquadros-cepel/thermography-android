package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.BusinessUnitEntity
import com.tech.thermography.android.data.remote.mapper.BusinessUnitMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessUnitRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : SyncableRepository {
    private val businessUnitDao = db.businessUnitDao()

    fun getAllBusinessUnits(): Flow<List<BusinessUnitEntity>> = businessUnitDao.getAllBusinessUnits()

    suspend fun getBusinessUnitById(id: UUID): BusinessUnitEntity? = businessUnitDao.getBusinessUnitById(id)

    suspend fun insertBusinessUnit(businessUnit: BusinessUnitEntity) = businessUnitDao.insertBusinessUnit(businessUnit)

    suspend fun deleteBusinessUnit(businessUnit: BusinessUnitEntity) = businessUnitDao.deleteBusinessUnit(businessUnit)

    override suspend fun syncEntities() {
        val remoteBusinessUnits = syncApi.getAllBusinessUnits()
        val entities = remoteBusinessUnits.map { dto -> BusinessUnitMapper.dtoToEntity(dto) }

        db.runInTransaction {
            runBlocking {
                entities.forEach { businessUnitDao.insertBusinessUnit(it) }
            }
        }
    }
}
