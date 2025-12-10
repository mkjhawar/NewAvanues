// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/di/AppModule.kt
// created: 2025-11-13
// © Augmentalis Inc, Intelligent Devices LLC
// AVA AI - Application Module for Dependency Injection

package com.augmentalis.ava.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.WorkManager
import com.augmentalis.ava.core.data.prefs.ChatPreferences
import com.augmentalis.ava.core.domain.resolution.AppResolverService
import com.augmentalis.ava.core.domain.resolution.PreferencePromptManager
import com.augmentalis.ava.features.actions.ActionsManager
import com.augmentalis.ava.features.llm.inference.InferenceManager
import com.augmentalis.ava.features.nlu.IntentClassifier
import com.augmentalis.ava.features.nlu.ModelManager
import com.augmentalis.ava.features.nlu.NLUSelfLearner
import com.augmentalis.ava.features.nlu.learning.IntentLearningManager
import com.augmentalis.ava.preferences.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for application-level dependencies
 *
 * Provides:
 * - ChatPreferences (user preferences for chat settings)
 * - IntentClassifier (NLU classification engine)
 * - ModelManager (NLU model management)
 * - ActionsManager (actions system wrapper - removes Context from ViewModels)
 * - UserPreferences (app-wide user settings)
 * - Other app-level singletons as needed
 *
 * This module is installed in SingletonComponent for app-wide availability.
 *
 * @author Manoj Jhawar
 * @since 1.0.0-alpha01
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides ChatPreferences singleton
     *
     * ChatPreferences manages user settings like confidence threshold,
     * conversation mode, and other chat-related preferences using DataStore.
     */
    @Provides
    @Singleton
    fun provideChatPreferences(
        @ApplicationContext context: Context
    ): ChatPreferences {
        return ChatPreferences.getInstance(context)
    }

    /**
     * Provides IntentClassifier singleton
     *
     * IntentClassifier is the core NLU engine for intent classification.
     * Uses ONNX Runtime with MobileBERT model for on-device inference.
     */
    @Provides
    @Singleton
    fun provideIntentClassifier(
        @ApplicationContext context: Context
    ): IntentClassifier {
        return IntentClassifier.getInstance(context)
    }

    /**
     * Provides ModelManager singleton
     *
     * ModelManager handles NLU model lifecycle (download, loading, caching).
     */
    @Provides
    @Singleton
    fun provideModelManager(
        @ApplicationContext context: Context
    ): ModelManager {
        return ModelManager(context)
    }

    /**
     * Provides UserPreferences singleton
     *
     * UserPreferences manages app-wide user settings (crash reporting, analytics, theme).
     * DataStore is provided by DataStoreModule.
     */
    @Provides
    @Singleton
    fun provideUserPreferences(
        dataStore: DataStore<Preferences>
    ): UserPreferences {
        return UserPreferences(dataStore)
    }

    /**
     * Provides ActionsManager singleton
     *
     * ActionsManager wraps the Actions system, encapsulating Context dependency.
     * This allows ViewModels to use actions without needing Context injection.
     *
     * Benefits:
     * - ViewModels no longer need @ApplicationContext
     * - Easier to test (mock ActionsManager)
     * - Better separation of concerns
     *
     * Note: ActionsManager is automatically provided by Hilt through @Inject constructor,
     * so this explicit provider is optional. Kept for consistency with other singleton services.
     */
    @Provides
    @Singleton
    fun provideActionsManager(
        @ApplicationContext context: Context,
        appResolverService: AppResolverService,
        preferencePromptManager: PreferencePromptManager
    ): ActionsManager {
        return ActionsManager(context, appResolverService, preferencePromptManager)
    }

    /**
     * Provides IntentLearningManager singleton
     *
     * IntentLearningManager handles Phase 2 learning functionality:
     * - Extracts intent hints from LLM responses
     * - Stores learned intents in database
     * - Triggers NLU re-embedding for improved classification
     *
     * This enables the self-improving AI system where LLM teaches NLU,
     * reducing CPU/GPU load and improving battery life over time.
     */
    @Provides
    @Singleton
    fun provideIntentLearningManager(
        @ApplicationContext context: Context
    ): IntentLearningManager {
        return IntentLearningManager(context)
    }

    // ==================== ADR-013: Self-Learning NLU ====================

    /**
     * Provides InferenceManager singleton
     *
     * InferenceManager handles battery/thermal-aware inference backend selection:
     * - Monitors battery level and charging status
     * - Monitors thermal status (Android 10+)
     * - Switches between local and cloud LLM based on device state
     *
     * @see ADR-013: Self-Learning NLU with LLM-as-Teacher Architecture
     */
    @Provides
    @Singleton
    fun provideInferenceManager(
        @ApplicationContext context: Context
    ): InferenceManager {
        return InferenceManager(context)
    }

    /**
     * Provides NLUSelfLearner singleton
     *
     * NLUSelfLearner orchestrates automatic NLU learning from LLM classifications:
     * - Receives LLM teacher results
     * - Computes embeddings for new utterances
     * - Saves to database for future NLU matching
     * - Schedules background processing via WorkManager
     *
     * @see ADR-013: Self-Learning NLU with LLM-as-Teacher Architecture
     */
    @Provides
    @Singleton
    fun provideNLUSelfLearner(
        intentClassifier: IntentClassifier,
        workManager: WorkManager
    ): NLUSelfLearner {
        return NLUSelfLearner(intentClassifier, workManager)
    }
}
