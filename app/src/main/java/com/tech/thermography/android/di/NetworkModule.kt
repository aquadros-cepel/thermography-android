package com.tech.thermography.android.di

import com.tech.thermography.android.data.remote.auth.AuthApi
import com.tech.thermography.android.data.remote.auth.AuthInterceptor
import com.tech.thermography.android.data.remote.sync.SyncApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    fun provideOkHttp(interceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

    @Provides
    fun provideRetrofit(client: OkHttpClient): Retrofit =        
        Retrofit.Builder()
            .baseUrl("http://192.168.15.172:8080/api/")
//            .baseUrl("http://35.247.197.28:8080/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

    @Provides
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    fun provideSyncApi(retrofit: Retrofit): SyncApi =
        retrofit.create(SyncApi::class.java)
}
