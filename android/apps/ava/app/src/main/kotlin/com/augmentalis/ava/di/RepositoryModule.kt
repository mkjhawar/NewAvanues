// filename: apps/ava-app-android/src/main/kotlin/com/augmentalis/ava/di/RepositoryModule.kt
// created: 2025-11-13
// updated: 2025-12-01 - Migrated from Room DAOs to SQLDelight Queries
// © Augmentalis Inc, Intelligent Devices LLC
// AVA AI - Repository Dependency Injection Module

package com.augmentalis.ava.di

import com.augmentalis.ava.core.data.db.ConversationQueries
import com.augmentalis.ava.core.data.db.DecisionQueries
import com.augmentalis.ava.core.data.db.IntentCategoryQueries
import com.augmentalis.ava.core.data.db.LearningQueries
import com.augmentalis.ava.core.data.db.MemoryQueries
import com.augmentalis.ava.core.data.db.MessageQueries
import com.augmentalis.ava.core.data.db.TrainExampleQueries
import com.augmentalis.ava.core.data.db.AppPreferencesQueries
import com.augmentalis.ava.core.data.repository.*
import com.augmentalis.ava.core.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository dependencies
 *
 * Provides repository implementations for domain layer interfaces.
 * Binds concrete implementations to their interfaces for dependency injection.
 *
 * ## Architecture
 * - Follows Repository pattern (domain interfaces, data implementations)
 * - Each repository is provided as singleton to maintain data consistency
 * - SQLDelight Queries are injected from DatabaseModule
 *
 * ## SQLDelight Migration (2025-12-01)
 * - Room DAOs replaced with SQLDelight Queries
 * - KMP-ready architecture
 *
 * ## Usage
 * ViewModels and other components can now inject repositories:
 * ```kotlin
 * @HiltViewModel
 * class ChatViewModel @Inject constructor(
 *     private val conversationRepository: ConversationRepository,
 *     private val messageRepository: MessageRepository
 * ) : ViewModel()
 * ```
 *
 * @author Manoj Jhawar
 * @since 1.0.0-alpha01
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Provides ConversationRepository implementation
     */
    @Provides
    @Singleton
    fun provideConversationRepository(
        conversationQueries: ConversationQueries
    ): ConversationRepository {
        return ConversationRepositoryImpl(conversationQueries)
    }

    /**
     * Provides MessageRepository implementation
     *
     * Note: MessageRepositoryImpl requires conversationQueries for updating
     * denormalized message counts when messages are added/deleted.
     */
    @Provides
    @Singleton
    fun provideMessageRepository(
        messageQueries: MessageQueries,
        conversationQueries: ConversationQueries
    ): MessageRepository {
        return MessageRepositoryImpl(messageQueries, conversationQueries)
    }

    /**
     * Provides TrainExampleRepository implementation
     */
    @Provides
    @Singleton
    fun provideTrainExampleRepository(
        trainExampleQueries: TrainExampleQueries
    ): TrainExampleRepository {
        return TrainExampleRepositoryImpl(trainExampleQueries)
    }

    /**
     * Provides MemoryRepository implementation
     */
    @Provides
    @Singleton
    fun provideMemoryRepository(
        memoryQueries: MemoryQueries
    ): MemoryRepository {
        return MemoryRepositoryImpl(memoryQueries)
    }

    /**
     * Provides DecisionRepository implementation
     */
    @Provides
    @Singleton
    fun provideDecisionRepository(
        decisionQueries: DecisionQueries
    ): DecisionRepository {
        return DecisionRepositoryImpl(decisionQueries)
    }

    /**
     * Provides LearningRepository implementation
     */
    @Provides
    @Singleton
    fun provideLearningRepository(
        learningQueries: LearningQueries
    ): LearningRepository {
        return LearningRepositoryImpl(learningQueries)
    }

    /**
     * Provides AppPreferencesRepository implementation
     * Part of Intelligent Resolution System (Chapter 71)
     */
    @Provides
    @Singleton
    fun provideAppPreferencesRepository(
        appPreferencesQueries: AppPreferencesQueries
    ): AppPreferencesRepository {
        return AppPreferencesRepositoryImpl(appPreferencesQueries)
    }

    /**
     * Provides IntentCategoryRepository implementation
     * Phase 2: Database-driven category lookup for IntentRouter
     */
    @Provides
    @Singleton
    fun provideIntentCategoryRepository(
        intentCategoryQueries: IntentCategoryQueries
    ): com.augmentalis.ava.core.data.repository.IntentCategoryRepository {
        return com.augmentalis.ava.core.data.repository.IntentCategoryRepositoryImpl(intentCategoryQueries)
    }
}
