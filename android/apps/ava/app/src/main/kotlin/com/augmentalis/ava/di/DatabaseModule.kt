// filename: apps/ava-app-android/src/main/kotlin/com/augmentalis/ava/di/DatabaseModule.kt
// created: 2025-11-13
// updated: 2025-11-30 - Migrated from Room to SQLDelight
// © Augmentalis Inc, Intelligent Devices LLC
// AVA AI - Database Dependency Injection Module

package com.augmentalis.ava.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.augmentalis.ava.core.data.db.AVADatabase
import com.augmentalis.ava.core.data.db.ConversationQueries
import com.augmentalis.ava.core.data.db.DecisionQueries
import com.augmentalis.ava.core.data.db.IntentCategoryQueries
import com.augmentalis.ava.core.data.db.LearningQueries
import com.augmentalis.ava.core.data.db.MemoryQueries
import com.augmentalis.ava.core.data.db.MessageQueries
import com.augmentalis.ava.core.data.db.TokenCacheQueries
import com.augmentalis.ava.core.data.db.TrainExampleQueries
import com.augmentalis.ava.core.data.db.AppPreferencesQueries
import com.augmentalis.ava.core.data.db.DatabaseDriverFactory
import com.augmentalis.ava.core.data.db.createDatabase
import com.augmentalis.ava.core.data.repository.TokenCacheRepositoryImpl
import com.augmentalis.ava.core.domain.repository.TokenCacheRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for SQLDelight database dependencies
 *
 * Provides:
 * - SqlDriver singleton instance
 * - AVADatabase singleton (SQLDelight generated)
 * - All *Queries instances for repository injection
 * - TokenCacheRepository for token caching
 *
 * ## SQLDelight Migration (2025-11-30)
 * - Room DAOs replaced with SQLDelight Queries
 * - Binary token cache added for LLM efficiency
 * - KMP-ready architecture
 *
 * @author Manoj Jhawar
 * @since 1.0.0-alpha01
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides SqlDriver singleton
     */
    @Provides
    @Singleton
    fun provideSqlDriver(
        @ApplicationContext context: Context
    ): SqlDriver {
        return DatabaseDriverFactory(context).createDriver()
    }

    /**
     * Provides AVADatabase singleton (SQLDelight)
     */
    @Provides
    @Singleton
    fun provideDatabase(driver: SqlDriver): AVADatabase {
        return driver.createDatabase()
    }

    /**
     * Provides ConversationQueries
     */
    @Provides
    @Singleton
    fun provideConversationQueries(database: AVADatabase): ConversationQueries {
        return database.conversationQueries
    }

    /**
     * Provides MessageQueries
     */
    @Provides
    @Singleton
    fun provideMessageQueries(database: AVADatabase): MessageQueries {
        return database.messageQueries
    }

    /**
     * Provides TrainExampleQueries
     */
    @Provides
    @Singleton
    fun provideTrainExampleQueries(database: AVADatabase): TrainExampleQueries {
        return database.trainExampleQueries
    }

    /**
     * Provides MemoryQueries
     */
    @Provides
    @Singleton
    fun provideMemoryQueries(database: AVADatabase): MemoryQueries {
        return database.memoryQueries
    }

    /**
     * Provides DecisionQueries
     */
    @Provides
    @Singleton
    fun provideDecisionQueries(database: AVADatabase): DecisionQueries {
        return database.decisionQueries
    }

    /**
     * Provides LearningQueries
     */
    @Provides
    @Singleton
    fun provideLearningQueries(database: AVADatabase): LearningQueries {
        return database.learningQueries
    }

    /**
     * Provides TokenCacheQueries
     */
    @Provides
    @Singleton
    fun provideTokenCacheQueries(database: AVADatabase): TokenCacheQueries {
        return database.tokenCacheQueries
    }

    /**
     * Provides TokenCacheRepository
     */
    @Provides
    @Singleton
    fun provideTokenCacheRepository(
        tokenCacheQueries: TokenCacheQueries
    ): TokenCacheRepository {
        return TokenCacheRepositoryImpl(tokenCacheQueries)
    }

    /**
     * Provides AppPreferencesQueries for Intelligent Resolution System
     */
    @Provides
    @Singleton
    fun provideAppPreferencesQueries(database: AVADatabase): AppPreferencesQueries {
        return database.appPreferencesQueries
    }

    /**
     * Provides IntentCategoryQueries for Phase 2 database-driven category lookup
     */
    @Provides
    @Singleton
    fun provideIntentCategoryQueries(database: AVADatabase): IntentCategoryQueries {
        return database.intentCategoryQueries
    }
}
