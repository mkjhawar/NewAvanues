/**
 * AppModule.kt - Hilt dependency injection module
 *
 * Provides app-level dependencies. Core functionality is in VoiceOSCore module.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.di

import android.content.Context
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.rpc.VoiceOSServerConfig
import com.augmentalis.webavanue.BrowserRepository
import com.augmentalis.webavanue.BrowserRepositoryImpl
import com.augmentalis.webavanue.createAndroidDriver
import com.augmentalis.webavanue.data.db.BrowserDatabase
import com.augmentalis.webavanue.rpc.WebAvanueServerConfig
import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    // Database - shared singleton
    @Provides
    @Singleton
    fun provideDatabaseDriverFactory(
        @ApplicationContext context: Context
    ): DatabaseDriverFactory = DatabaseDriverFactory(context)

    @Provides
    @Singleton
    fun provideVoiceOSDatabaseManager(
        driverFactory: DatabaseDriverFactory
    ): VoiceOSDatabaseManager = VoiceOSDatabaseManager.getInstance(driverFactory)

    // RPC Server configs (for inter-module communication)
    @Provides
    @Singleton
    fun provideVoiceOSServerConfig(): VoiceOSServerConfig =
        VoiceOSServerConfig(port = 50051)

    @Provides
    @Singleton
    fun provideWebAvanueServerConfig(): WebAvanueServerConfig =
        WebAvanueServerConfig(port = 50055)

    @Provides
    @Singleton
    fun provideAvanuesSettingsRepository(
        @ApplicationContext context: Context
    ): AvanuesSettingsRepository = AvanuesSettingsRepository(context)

    // Browser (WebAvanue module) - separate database from VoiceOS
    @Provides
    @Singleton
    fun provideBrowserDatabase(
        @ApplicationContext context: Context
    ): BrowserDatabase {
        val driver = createAndroidDriver(context)
        return BrowserDatabase(driver)
    }

    @Provides
    @Singleton
    fun provideBrowserRepository(
        database: BrowserDatabase
    ): BrowserRepository = BrowserRepositoryImpl(database)
}
