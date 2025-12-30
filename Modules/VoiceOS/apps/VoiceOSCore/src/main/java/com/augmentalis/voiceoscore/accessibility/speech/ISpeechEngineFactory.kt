/**
 * ISpeechEngineFactory.kt - Factory Interface for Speech Engine Creation
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
 * Factory interface for creating speech engine instances.
 * Enables polymorphic engine creation without tight coupling to concrete types.
 *
 * BENEFITS:
 * - Open/Closed: Add new engines without modifying existing code
 * - Dependency Inversion: Clients depend on factory abstraction, not concrete factory
 * - Testability: Easy to inject mock factory for testing
 */
package com.augmentalis.voiceoscore.accessibility.speech

import android.content.Context
import com.augmentalis.speechrecognition.SpeechEngine

/**
 * Factory interface for creating speech engine instances
 *
 * Defines the contract for speech engine factories. Implementations
 * are responsible for creating the appropriate engine adapter based
 * on the engine type.
 *
 * DESIGN PATTERN: Abstract Factory
 * - Abstracts engine creation logic
 * - Supports multiple engine types
 * - Enables dependency injection
 *
 * @see SpeechEngineFactory
 * @see ISpeechEngine
 */
interface ISpeechEngineFactory {

    /**
     * Create a speech engine instance
     *
     * Creates and returns an appropriate engine adapter based on the
     * specified engine type. The returned engine is not initialized -
     * caller must call initialize() before use.
     *
     * @param engineType Type of engine to create (VIVOKA, ANDROID_STT, etc.)
     * @param context Android application context
     * @return Speech engine instance conforming to ISpeechEngine interface
     *
     * @throws IllegalArgumentException if engine type is not supported
     */
    fun createEngine(engineType: SpeechEngine, context: Context): ISpeechEngine
}
