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
) : SyncableRepository {
    private val companyDao = db.companyDao()

    fun getAllCompanies(): Flow<List<CompanyEntity>> = companyDao.getAllCompanies()

    suspend fun getCompanyById(id: UUID): CompanyEntity? = companyDao.getCompanyById(id)

    suspend fun insertCompany(company: CompanyEntity) = companyDao.insertCompany(company)

    suspend fun updateCompany(company: CompanyEntity) = companyDao.updateCompany(company)

    suspend fun deleteCompany(company: CompanyEntity) = companyDao.deleteCompany(company)

    override suspend fun syncEntities() {
        val remoteCompanies = syncApi.getAllCompanies()
        val entities = remoteCompanies.map { dto -> CompanyMapper.dtoToEntity(dto) }

        db.runInTransaction {
            runBlocking {
                entities.forEach { companyDao.insertCompany(it) }
            }
        }
    }
}
