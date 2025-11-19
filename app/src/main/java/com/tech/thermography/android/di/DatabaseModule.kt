package com.tech.thermography.android.di

import android.content.Context
import androidx.room.Room
import com.tech.thermography.android.data.local.AppDatabase
import com.tech.thermography.android.data.local.dao.PlantDao
import com.tech.thermography.android.data.local.repository.PlantRepository

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "thermography-db"
        ).build()
    }

    @Provides
    fun providePlantDao(db: AppDatabase) = db.plantDao()

    @Provides
    @Singleton
    fun providePlantRepository(
        plantDao: PlantDao
    ): PlantRepository {
        return PlantRepository(plantDao)
    }
}
