package com.augmentalis.chat.event

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing WakeWordEventBus.
 *
 * This module provides a singleton instance of WakeWordEventBus
 * for dependency injection throughout the Android app.
 *
 * @author Manoj Jhawar
 * @since 2025-12-17
 */
@Module
@InstallIn(SingletonComponent::class)
object WakeWordEventBusModule {

    @Provides
    @Singleton
    fun provideWakeWordEventBus(): WakeWordEventBus {
        return WakeWordEventBus()
    }
}
