// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/AvaApplication.kt
// created: 2025-11-02 15:30:00 -0800
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.augmentalis.ava.crashreporting.CrashReporter
import com.augmentalis.ava.features.llm.provider.LocalLLMProvider
import com.augmentalis.ava.features.nlu.NLUInitializer
import com.augmentalis.ava.preferences.UserPreferences
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * AVA AI Application class
 *
 * Initializes core services and dependencies for the application.
 * Uses Hilt for dependency injection to improve testability and modularity.
 *
 * ## Architecture
 * - **Dependency Injection**: Hilt manages all dependencies across the app
 * - **Logging**: Timber is configured for debug/release builds
 * - **Lifecycle**: Application-scoped components are initialized here
 *
 * ## IDEACODE Framework Compliance
 * - Uses Hilt DI for testability and modularity (Phase 2)
 * - Follows Android Architecture Components best practices
 * - Enables comprehensive test coverage through DI
 *
 * @author Manoj Jhawar
 * @since 1.0.0-alpha01
 * © Augmentalis Inc, Intelligent Devices LLC
 */
@HiltAndroidApp
class AvaApplication : Application(), Configuration.Provider {

    // Application-scoped coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // User preferences (injected by Hilt)
    @Inject
    lateinit var userPreferences: UserPreferences

    // ADR-013: HiltWorkerFactory for background embedding computation
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // ADR-014 Phase B2: Eager LLM initialization
    @Inject
    lateinit var localLLMProvider: LocalLLMProvider

    // NLU Initializer (lazy, created when needed)
    private val nluInitializer by lazy { NLUInitializer(this) }

    // ADR-014 Phase B1: NLU ready state for blocking startup
    private val _isNLUReady = MutableStateFlow(false)
    val isNLUReady: StateFlow<Boolean> = _isNLUReady.asStateFlow()

    // ADR-014 Phase B2: LLM ready state
    private val _isLLMReady = MutableStateFlow(false)
    val isLLMReady: StateFlow<Boolean> = _isLLMReady.asStateFlow()

    // Phase 2: Intent category repository for database-driven lookup
    @Inject
    lateinit var intentCategoryRepository: com.augmentalis.ava.core.data.repository.IntentCategoryRepository

    /**
     * WorkManager configuration for Hilt-injected workers.
     * Required for EmbeddingComputeWorker to receive dependencies via @AssistedInject.
     *
     * @see ADR-013: Self-Learning NLU with LLM-as-Teacher Architecture
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("AVA Application initialized")
        Timber.d("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Timber.d("Build Type: ${BuildConfig.BUILD_TYPE}")

        // Load native libraries for llama.cpp GGUF inference
        loadNativeLibraries()

        // Database is initialized via Hilt DI (SQLDelight)
        // Repositories are provided by DatabaseModule and RepositoryModule
        Timber.d("Database: SQLDelight with Hilt DI")

        // Initialize crash reporting (disabled by default for privacy)
        initializeCrashReporting()

        // Phase 2: Seed intent category database if empty
        seedIntentCategories()

        // Initialize NLU engine in background
        initializeNLU()

        // ADR-014 Phase B2: Eager LLM initialization
        initializeLLM()
    }

    /**
     * Initialize crash reporting based on user preferences
     *
     * Crash reporting is disabled by default to respect user privacy.
     * Users can enable it in Settings.
     */
    private fun initializeCrashReporting() {
        applicationScope.launch {
            try {
                // Read crash reporting preference from DataStore
                val enabled = userPreferences.crashReportingEnabled.first()

                CrashReporter.initialize(this@AvaApplication, enabled = enabled)

                if (enabled) {
                    Timber.d("Crash reporting initialized and enabled")
                } else {
                    Timber.d("Crash reporting initialized but disabled (user privacy)")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to read crash reporting preference, defaulting to disabled")
                CrashReporter.initialize(this@AvaApplication, enabled = false)
            }
        }
    }

    /**
     * Initialize NLU engine asynchronously
     *
     * Downloads models if needed and initializes the intent classifier.
     * This runs in the background to avoid blocking app startup.
     */
    private fun initializeNLU() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                Timber.d("Starting NLU initialization...")

                val result = nluInitializer.initialize { progress ->
                    Timber.v("NLU download progress: ${(progress * 100).toInt()}%")
                }

                when (result) {
                    is com.augmentalis.ava.core.common.Result.Success -> {
                        val status = result.data
                        Timber.i(
                            "NLU initialized successfully: ${status.message} " +
                            "(model size: ${status.modelSize / 1024 / 1024} MB)"
                        )
                        // ADR-014 Phase B1: Set ready state
                        _isNLUReady.value = true
                    }
                    is com.augmentalis.ava.core.common.Result.Error -> {
                        Timber.e(result.exception, "NLU initialization failed: ${result.message}")
                        Timber.w("App will continue without NLU features")
                        // Still set ready state even on error (allows app to function with LLM only)
                        _isNLUReady.value = true
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during NLU initialization")
                // Still set ready state to allow app to function
                _isNLUReady.value = true
            }
        }
    }

    /**
     * ADR-014 Phase B2: Eager LLM initialization
     *
     * Triggers LLM model discovery and loading at app startup.
     * This runs in the background to avoid blocking the main thread.
     */
    private fun initializeLLM() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                Timber.d("Starting LLM initialization...")

                // Trigger model discovery (this loads available models)
                val availableModels = localLLMProvider.getAvailableModels()
                Timber.i("LLM models discovered: ${availableModels.size}")

                // Set ready state
                _isLLMReady.value = true
                Timber.i("LLM initialization complete")
            } catch (e: Exception) {
                Timber.e(e, "LLM initialization failed")
                // Still set ready state to allow app to function with template responses
                _isLLMReady.value = true
            }
        }
    }

    /**
     * Phase 2: Seed intent category database on first launch
     *
     * Populates database with all known intent-category mappings if database is empty.
     * This enables database-driven category lookup instead of hardcoded registry.
     */
    private fun seedIntentCategories() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                val totalCount = intentCategoryRepository.getTotalCount()

                if (totalCount == 0L) {
                    Timber.i("Intent category database is empty, seeding...")
                    val seeder = com.augmentalis.ava.features.actions.CategorySeeder(intentCategoryRepository)
                    seeder.seedCategories()
                    Timber.i("Intent category database seeding complete")
                } else {
                    Timber.d("Intent category database already seeded ($totalCount mappings)")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to seed intent categories")
                // Non-fatal error - app continues with fallback category inference
            }
        }
    }

    /**
     * Load native libraries for llama.cpp inference
     *
     * Loads libllama-android.so which contains:
     * - llama.cpp core library
     * - ggml backends (CPU, GPU)
     * - JNI wrapper for GGUFInferenceStrategy
     *
     * Gracefully handles failures to allow fallback to TVM-only mode.
     */
    private fun loadNativeLibraries() {
        try {
            System.loadLibrary("llama-android")
            Timber.i("Native library loaded successfully: libllama-android.so")
            Timber.d("GGUF inference via llama.cpp is available")
        } catch (e: UnsatisfiedLinkError) {
            Timber.e(e, "Failed to load native library: libllama-android.so")
            Timber.w("GGUF inference unavailable - falling back to TVM-only mode")
            Timber.w("This is expected if GGUF models are not installed")
            // Non-fatal: app continues with TVM-based MLC-LLM inference
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error loading native libraries")
        }
    }

    /**
     * Get NLU initializer instance
     * (for dependency injection or manual initialization)
     */
    fun getNLUInitializer(): NLUInitializer = nluInitializer
}
