package com.smartboiler.di

import android.content.Context
import androidx.room.Room
import com.smartboiler.data.local.SmartBoilerDatabase
import com.smartboiler.data.local.dao.BoilerDao
import com.smartboiler.data.remote.SmartThingsApiService
import com.smartboiler.data.remote.WeatherApiService
import com.smartboiler.data.device.SmartThingsController
import com.smartboiler.data.repository.LocalBoilerRepository
import com.smartboiler.data.repository.RemoteWeatherRepository
import com.smartboiler.domain.device.SmartSwitchController
import com.smartboiler.domain.repository.BoilerRepository
import com.smartboiler.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartBoilerDatabase =
        Room.databaseBuilder(
            context,
            SmartBoilerDatabase::class.java,
            "smart_boiler.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideBoilerDao(db: SmartBoilerDatabase): BoilerDao = db.boilerDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideWeatherApiService(retrofit: Retrofit): WeatherApiService =
        retrofit.create(WeatherApiService::class.java)

    @Provides
    @Singleton
    fun provideSmartThingsApiService(okHttpClient: OkHttpClient): SmartThingsApiService =
        Retrofit.Builder()
            .baseUrl("https://api.smartthings.com/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SmartThingsApiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBoilerRepository(impl: LocalBoilerRepository): BoilerRepository

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(impl: RemoteWeatherRepository): WeatherRepository

    @Binds
    @Singleton
    abstract fun bindSmartSwitchController(impl: SmartThingsController): SmartSwitchController
}
