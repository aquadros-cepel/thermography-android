package com.tech.thermography.android.di

import android.content.Context
import com.tech.thermography.android.flir.FlirAceCameraService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(ViewModelComponent::class)
object FlirModule {

    @Provides
    fun provideFlirAceCameraService(
        @ApplicationContext ctx: Context
    ): FlirAceCameraService {
        return FlirAceCameraService(ctx)
    }
}
