// filename: android/ava/src/main/kotlin/com/augmentalis/ava/di/ResolutionModule.kt
// created: 2025-12-04
// © Augmentalis Inc, Intelligent Devices LLC
// AVA AI - Resolution Dependency Injection Module

package com.augmentalis.ava.di

import android.content.Context
import com.augmentalis.ava.core.domain.repository.AppPreferencesRepository
import com.augmentalis.ava.core.domain.resolution.AppResolverService
import com.augmentalis.ava.core.domain.resolution.PreferencePromptManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Intelligent Resolution System dependencies.
 *
 * Part of Intelligent Resolution System (Chapter 71).
 *
 * Provides:
 * - AppResolverService - Resolves best app for capabilities (email, SMS, etc.)
 * - PreferencePromptManager - Coordinates app selection UI prompts
 *
 * ## Architecture
 * - AppResolverService depends on AppPreferencesRepository for persistence
 * - PreferencePromptManager depends on AppResolverService for saving preferences
 * - Both are singletons to maintain consistent state across the app
 *
 * ## Usage
 * ViewModels and ActionHandlers can inject these services:
 * ```kotlin
 * @HiltViewModel
 * class ChatViewModel @Inject constructor(
 *     private val appResolverService: AppResolverService,
 *     private val preferencePromptManager: PreferencePromptManager
 * ) : ViewModel()
 * ```
 *
 * @author Manoj Jhawar
 * @since 1.0.0-alpha01
 */
@Module
@InstallIn(SingletonComponent::class)
object ResolutionModule {

    /**
     * Provides AppResolverService singleton.
     *
     * Core service for resolving the best app for any capability.
     * Scans installed apps, checks saved preferences, and handles auto-selection.
     */
    @Provides
    @Singleton
    fun provideAppResolverService(
        @ApplicationContext context: Context,
        appPreferencesRepository: AppPreferencesRepository
    ): AppResolverService {
        return AppResolverService(
            context = context,
            preferencesRepository = appPreferencesRepository
        )
    }

    /**
     * Provides PreferencePromptManager singleton.
     *
     * Manages app preference prompts in the UI.
     * Coordinates between AppResolverService and UI components.
     */
    @Provides
    @Singleton
    fun providePreferencePromptManager(
        appResolverService: AppResolverService
    ): PreferencePromptManager {
        return PreferencePromptManager(appResolverService)
    }
}
