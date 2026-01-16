// filename: Universal/AVA/Features/WakeWord/src/main/java/com/augmentalis/ava/features/wakeword/di/WakeWordModule.kt
// created: 2025-11-22
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.wakeword.di

import android.content.Context
import com.augmentalis.llm.security.ApiKeyManager
import com.augmentalis.wakeword.detector.WakeWordDetector
import com.augmentalis.wakeword.settings.WakeWordSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Dependency Injection Module for Wake Word Feature
 *
 * Provides:
 * - WakeWordDetector (singleton)
 * - WakeWordSettingsRepository (singleton)
 *
 * @author Manoj Jhawar
 */
@Module
@InstallIn(SingletonComponent::class)
object WakeWordModule {

    @Provides
    @Singleton
    fun provideWakeWordDetector(
        @ApplicationContext context: Context,
        apiKeyManager: ApiKeyManager
    ): WakeWordDetector {
        return WakeWordDetector(context, apiKeyManager)
    }

    @Provides
    @Singleton
    fun provideWakeWordSettingsRepository(
        @ApplicationContext context: Context
    ): WakeWordSettingsRepository {
        return WakeWordSettingsRepository(context)
    }
}
