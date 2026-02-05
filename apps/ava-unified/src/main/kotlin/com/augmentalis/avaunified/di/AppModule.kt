/**
 * AppModule.kt - Hilt dependency injection module
 *
 * Provides dependencies for AVA Unified app.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.avaunified.di

import android.content.Context
import com.augmentalis.voiceoscore.rpc.VoiceOSServerConfig
import com.augmentalis.webavanue.rpc.WebAvanueServerConfig
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

    @Provides
    @Singleton
    fun provideVoiceOSServerConfig(): VoiceOSServerConfig =
        VoiceOSServerConfig(port = 50051)

    @Provides
    @Singleton
    fun provideWebAvanueServerConfig(): WebAvanueServerConfig =
        WebAvanueServerConfig(port = 50055)
}
