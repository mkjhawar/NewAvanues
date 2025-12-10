// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/di/DataStoreModule.kt
// created: 2025-11-15
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Extension property for production DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ava_preferences")

/**
 * Hilt module for DataStore dependency
 *
 * This module is kept separate from AppModule to allow easy replacement in tests.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    /**
     * Provides DataStore singleton
     *
     * DataStore manages persistent key-value storage for user preferences.
     */
    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.dataStore
    }
}
