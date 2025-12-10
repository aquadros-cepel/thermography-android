package com.tech.thermography.android.data.local.repository

import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.entity.CompanyEntity
import com.tech.thermography.android.data.remote.mapper.CompanyMapper
import com.tech.thermography.android.data.remote.sync.SyncApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanyRepository @Inject constructor(
    private val db: AppDatabase,
    private val syncApi: SyncApi
) : AbstractSyncRepository<CompanyEntity>() {
    private val companyDao = db.companyDao()

    fun getAllCompanies(): Flow<List<CompanyEntity>> = companyDao.getAllCompanies()

    suspend fun getCompanyById(id: UUID): CompanyEntity? = companyDao.getCompanyById(id)

    suspend fun insertCompany(company: CompanyEntity) = companyDao.insertCompany(company)

    suspend fun updateCompany(company: CompanyEntity) = companyDao.updateCompany(company)

    suspend fun deleteCompany(company: CompanyEntity) = companyDao.deleteCompany(company)

    override suspend fun syncEntities() {
        // 1. Buscar tudo do backend
        val remoteCompanies = syncApi.getAllCompanies()

        // 2. Fazer o mapeamento para entidades Room e guardar no cache local
        val entities = remoteCompanies.map { dto -> CompanyMapper.dtoToEntity(dto) }
        setCache(entities)
    }

    override suspend fun insertCached() {
        companyDao.insertCompanies(cache)
        clearCache()
    }
}
