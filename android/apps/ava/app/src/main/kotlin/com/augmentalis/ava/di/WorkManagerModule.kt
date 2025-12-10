// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/di/WorkManagerModule.kt
// created: 2025-12-04
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// AVA AI - WorkManager Module for Hilt DI
// ADR-013: Self-Learning NLU with LLM-as-Teacher Architecture

package com.augmentalis.ava.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for WorkManager dependencies.
 *
 * Provides WorkManager instance for background task scheduling.
 * Used by EmbeddingComputeWorker for deferred embedding computation.
 *
 * @see ADR-013: Self-Learning NLU with LLM-as-Teacher Architecture
 * @author Manoj Jhawar
 * @since 1.0.0-alpha01
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    /**
     * Provides WorkManager singleton.
     *
     * WorkManager handles background tasks with:
     * - Battery-aware scheduling (deferred when low)
     * - Constraint-based execution
     * - Automatic retry with exponential backoff
     */
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }
}
