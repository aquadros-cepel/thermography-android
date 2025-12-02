package com.tech.thermography.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tech.thermography.android.data.local.dao.BusinessUnitDao
import com.tech.thermography.android.data.local.dao.CompanyDao
import com.tech.thermography.android.data.local.dao.PlantDao
import com.tech.thermography.android.data.local.entity.BusinessUnitEntity
import com.tech.thermography.android.data.local.entity.CompanyEntity
import com.tech.thermography.android.data.local.entity.PlantEntity
import com.tech.thermography.android.data.local.util.Converters

@Database(
    entities = [
        CompanyEntity::class,
        BusinessUnitEntity::class,
        PlantEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun companyDao(): CompanyDao
    abstract fun businessUnitDao(): BusinessUnitDao
    abstract fun plantDao(): PlantDao
}
