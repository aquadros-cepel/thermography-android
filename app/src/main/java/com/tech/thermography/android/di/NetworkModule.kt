package com.tech.thermography.android.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tech.thermography.android.data.remote.adapter.InstantAdapter
import com.tech.thermography.android.data.remote.adapter.LocalDateAdapter
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
import java.time.Instant
import java.time.LocalDate
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson =
        GsonBuilder()
            .registerTypeAdapter(Instant::class.java, InstantAdapter())
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
            .create()

    @Provides
    fun provideOkHttp(interceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

    @Provides
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit =        
        Retrofit.Builder()
            .baseUrl("http://34.95.132.119:8081/api/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()

    @Provides
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    fun provideSyncApi(retrofit: Retrofit): SyncApi =
        retrofit.create(SyncApi::class.java)
}
