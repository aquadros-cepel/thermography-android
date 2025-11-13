package com.tech.thermography.android.data.local.dao

import androidx.room.*
import com.tech.thermography.android.data.local.entity.CompanyEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface CompanyDao {
    @Query("SELECT * FROM company ORDER BY name")
    fun getAllCompanies(): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM company WHERE id = :id")
    suspend fun getCompanyById(id: UUID): CompanyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: CompanyEntity)

    @Delete
    suspend fun deleteCompany(company: CompanyEntity)
}
