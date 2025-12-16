/**
 * CleanupModule.kt - Hilt dependency injection module for cleanup functionality
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-15
 *
 * P2 Task 2.2: Cleanup preview infrastructure with Hilt DI
 */

package com.augmentalis.voiceoscore.cleanup.di

import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.voiceoscore.cleanup.CleanupManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * Hilt module providing cleanup-related dependencies
 */
@Module
@InstallIn(ViewModelComponent::class)
object CleanupModule {

    /**
     * Provide CleanupManager instance
     *
     * @param commandRepo Repository for generated command operations
     * @return Configured CleanupManager instance
     */
    @Provides
    @ViewModelScoped
    fun provideCleanupManager(
        commandRepo: IGeneratedCommandRepository
    ): CleanupManager {
        return CleanupManager(commandRepo)
    }
}
