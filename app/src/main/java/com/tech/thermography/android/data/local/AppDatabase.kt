package com.tech.thermography.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tech.thermography.android.data.local.dao.*
import com.tech.thermography.android.data.local.entity.*
import com.tech.thermography.android.data.local.util.Converters

@Database(
    entities = [
        CompanyEntity::class,
        PlantEntity::class],

    version = 1,
    exportSchema = false

)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun companyDao(): CompanyDao
    abstract fun plantDao(): PlantDao
}
