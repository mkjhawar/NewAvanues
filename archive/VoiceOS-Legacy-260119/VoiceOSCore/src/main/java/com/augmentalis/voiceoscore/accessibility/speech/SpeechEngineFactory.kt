/**
 * SpeechEngineFactory.kt - Default Factory Implementation for Speech Engines
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-22
 *
 * Part of SOLID Refactoring Phase 2: Open/Closed Principle (Factory Pattern)
 * Plan: VoiceOS-Plan-SOLID-Refactoring-5221222-V1.md
 *
 * PURPOSE:
 * Concrete factory implementation that creates speech engine adapters.
 * Centralizes engine creation logic in a single location.
 *
 * EXTENSIBILITY:
 * Adding a new engine requires:
 * 1. Create new adapter class (e.g., WhisperEngineAdapter)
 * 2. Add case to when statement in createEngine()
 * 3. No changes to SpeechEngineManager needed!
 *
 * This demonstrates the Open/Closed Principle:
 * - OPEN for extension (add new engines easily)
 * - CLOSED for modification (no changes to manager)
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * DISABLED ENGINES DOCUMENTATION
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * The following engines are currently disabled in this implementation:
 *
 * 1. WHISPER ENGINE (OpenAI Whisper.cpp)
 *    Status: Disabled (throws IllegalArgumentException)
 *    Reason: Requires manual NDK setup and native library compilation
 *    Enable: Complete NDK configuration → see build.gradle.kts TODO
 *    Dependencies:
 *      - whisper.cpp native library (JNI bindings)
 *      - GGML model files (150MB-2.5GB depending on model size)
 *      - NDK r25c or later
 *    Setup instructions: See WHISPER_SETUP.md (to be created)
 *    When enabled:
 *      - Uncomment WhisperEngineAdapter creation in createEngine()
 *      - Ensure NDK is properly configured in build.gradle.kts
 *      - Download and configure GGML models
 *
 * 2. ANDROID_STT ENGINE (Google Cloud Speech via Android APIs)
 *    Status: Enabled but may be disabled based on user preference
 *    Reason: User preference for Vivoka-only operation
 *    Current: Fully functional via GoogleEngineAdapter
 *    Disable: To disable, throw IllegalArgumentException similar to WHISPER
 *    Note: No code changes needed if user wants to re-enable
 *
 * 3. VOSK ENGINE (Open-source offline recognition)
 *    Status: Enabled via VoskEngineAdapter
 *    Dependencies: VoskEngineAdapter class (implemented)
 *    Note: Fully functional - no issues
 *
 * 4. AZURE ENGINE (Microsoft Azure Cognitive Services)
 *    Status: Enabled via AzureEngineAdapter
 *    Dependencies: AzureEngineAdapter class (implemented)
 *    Note: Fully functional - no issues
 *
 * To enable disabled engines:
 * - Review dependencies and prerequisites above
 * - Uncomment relevant code in createEngine() method
 * - Add required native libraries and models
 * - Run comprehensive integration tests
 *
 * ═══════════════════════════════════════════════════════════════════════════
 */
package com.augmentalis.voiceoscore.accessibility.speech

import android.content.Context
import android.util.Log
import com.augmentalis.speechrecognition.SpeechEngine

/**
 * Default factory implementation for creating speech engines
 *
 * Creates appropriate engine adapter based on the engine type.
 * Supports Vivoka, Google (Android STT), and Azure (stub).
 *
 * DESIGN PATTERN: Factory Pattern
 * - Encapsulates object creation
 * - Single point of engine instantiation
 * - Easy to extend with new engines
 *
 * THREAD SAFETY:
 * - Thread-safe: No mutable state
 * - Can be used concurrently from multiple threads
 *
 * @see ISpeechEngineFactory
 * @see ISpeechEngine
 */
class SpeechEngineFactory : ISpeechEngineFactory {

    companion object {
        private const val TAG = "SpeechEngineFactory"
    }

    /**
     * Create a speech engine instance based on type
     *
     * Creates the appropriate adapter for the specified engine type.
     * Each adapter wraps the underlying engine implementation and
     * provides a unified ISpeechEngine interface.
     *
     * SUPPORTED ENGINES:
     * - ANDROID_STT: Google Cloud Speech via Android APIs
     * - WHISPER: OpenAI Whisper.cpp (offline)
     * - VOSK: Vosk offline recognition
     * - AZURE: Microsoft Azure Cognitive Services (cloud)
     * - GOOGLE_CLOUD: Falls back to ANDROID_STT (deprecated)
     *
     * NOT SUPPORTED (Use Vivoka directly):
     * - VIVOKA: Handled by SpeechEngineManager hybrid approach (not via factory)
     *
     * @param engineType Type of engine to create
     * @param context Android application context
     * @return Configured engine adapter instance
     *
     * @throws IllegalArgumentException if engine type is VIVOKA or not recognized
     */
    override fun createEngine(engineType: SpeechEngine, context: Context): ISpeechEngine {
        Log.d(TAG, "Creating speech engine: $engineType")

        return when (engineType) {
            SpeechEngine.VIVOKA -> {
                val errorMsg = "VIVOKA engine should be handled directly by SpeechEngineManager (hybrid approach)"
                Log.e(TAG, errorMsg)
                throw IllegalArgumentException(errorMsg)
            }

            SpeechEngine.ANDROID_STT -> {
                Log.i(TAG, "Creating Google (Android STT) engine adapter")
                GoogleEngineAdapter(context)
            }

            SpeechEngine.WHISPER -> {
                val errorMsg = "WHISPER engine requires manual NDK setup (see build.gradle.kts TODO)"
                Log.e(TAG, errorMsg)
                throw IllegalArgumentException(errorMsg)
            }

            SpeechEngine.VOSK -> {
                Log.i(TAG, "Creating Vosk engine adapter")
                VoskEngineAdapter(context)
            }

            SpeechEngine.AZURE -> {
                Log.i(TAG, "Creating Azure Cognitive Services engine adapter")
                AzureEngineAdapter(context)
            }

            SpeechEngine.GOOGLE_CLOUD -> {
                Log.w(TAG, "GOOGLE_CLOUD deprecated, falling back to ANDROID_STT")
                GoogleEngineAdapter(context)
            }

            else -> {
                val errorMsg = "Unsupported engine type: $engineType"
                Log.e(TAG, errorMsg)
                throw IllegalArgumentException(errorMsg)
            }
        }
    }
}
