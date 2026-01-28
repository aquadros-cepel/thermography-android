package com.tech.thermography.android.di

import android.content.Context
import androidx.room.Room
import com.tech.thermography.android.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "thermography-db"
        )
        .fallbackToDestructiveMigrationFrom(2, 3, 4)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun providePlantDao(db: AppDatabase) = db.plantDao()
}
