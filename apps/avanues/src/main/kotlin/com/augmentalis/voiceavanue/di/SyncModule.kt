/**
 * SyncModule.kt - Hilt DI module for VOS sync dependencies
 *
 * Provides VosSftpClient, VosSyncManager, and IVosFileRegistryRepository
 * as singletons for the VOS SFTP sync system.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-11
 */
package com.augmentalis.voiceavanue.di

import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.repositories.IPhraseSuggestionRepository
import com.augmentalis.database.repositories.IVosFileRegistryRepository
import com.augmentalis.voiceoscore.vos.sync.VosSftpClient
import com.augmentalis.voiceoscore.vos.sync.VosSyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideVosSftpClient(): VosSftpClient = VosSftpClient()

    @Provides
    @Singleton
    fun provideVosFileRegistry(
        databaseManager: VoiceOSDatabaseManager
    ): IVosFileRegistryRepository = databaseManager.vosFileRegistry

    @Provides
    @Singleton
    fun providePhraseSuggestionRepository(
        databaseManager: VoiceOSDatabaseManager
    ): IPhraseSuggestionRepository = databaseManager.phraseSuggestions

    @Provides
    @Singleton
    fun provideVosSyncManager(
        sftpClient: VosSftpClient,
        registry: IVosFileRegistryRepository
    ): VosSyncManager = VosSyncManager(
        sftpClient = sftpClient,
        registry = registry,
        importer = null // Late-bound: VoiceAvanueAccessibilityService.onServiceReady() calls setImporter() after DB init
    )
}
