/**
 * AppModule.kt - Hilt Dependency Injection Module for Application-level dependencies
 * Path: app/src/main/java/com/augmentalis/voiceos/di/AppModule.kt
 *
 * Author: Manoj Jhawar
 * Created: 2025-10-09
 *
 * Provides common application-wide dependencies for Hilt injection:
 * - SharedPreferences for app settings
 * - PackageManager for system capabilities
 * - Resources for accessing app resources
 */

package com.augmentalis.voiceos.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Application-level Hilt module
 *
 * This module provides core Android system dependencies that are needed
 * across the entire application. All dependencies are scoped as Singleton
 * to ensure only one instance exists for the app lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides SharedPreferences for storing app settings
     *
     * @param context Application context
     * @return SharedPreferences instance for "voiceos_prefs"
     */
    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("voiceos_prefs", Context.MODE_PRIVATE)
    }

    /**
     * Provides PackageManager for querying system capabilities
     *
     * @param context Application context
     * @return PackageManager instance
     */
    @Provides
    @Singleton
    fun providePackageManager(
        @ApplicationContext context: Context
    ): PackageManager {
        return context.packageManager
    }

    /**
     * Provides Resources for accessing app resources
     *
     * @param context Application context
     * @return Resources instance
     */
    @Provides
    @Singleton
    fun provideResources(
        @ApplicationContext context: Context
    ): Resources {
        return context.resources
    }
}
