// filename: apps/ava-standalone/src/androidTest/kotlin/com/augmentalis/ava/di/TestDataStoreModule.kt
// created: 2025-11-15
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.io.File
import javax.inject.Singleton

/**
 * Test module that replaces production DataStore with test-specific instance
 *
 * Problem: Production DataStore uses a fixed file path, causing "multiple DataStores
 * active for the same file" error when multiple tests run.
 *
 * Solution: Each test gets a unique DataStore file to prevent conflicts.
 *
 * Usage: Automatically used in all instrumentation tests via @TestInstallIn
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataStoreModule::class]
)
object TestDataStoreModule {

    @Provides
    @Singleton
    fun provideTestDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        // Use unique file for each test run to prevent conflicts
        val testFile = File(
            context.filesDir,
            "test_datastore_${System.currentTimeMillis()}.preferences_pb"
        )

        return PreferenceDataStoreFactory.create(
            produceFile = { testFile }
        )
    }
}
