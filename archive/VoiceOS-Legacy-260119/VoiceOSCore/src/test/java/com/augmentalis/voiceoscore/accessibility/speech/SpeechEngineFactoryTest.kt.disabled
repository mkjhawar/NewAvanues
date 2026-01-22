/**
 * SpeechEngineFactoryTest.kt - Unit Tests for Speech Engine Factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-22
 *
 * Part of SOLID Refactoring Phase 2: Factory Pattern Tests
 * Plan: VoiceOS-Plan-SOLID-Refactoring-5221222-V1.md
 *
 * PURPOSE:
 * Verify that the factory pattern implementation works correctly:
 * - Creates correct engine adapter for each engine type
 * - Handles unsupported engine types appropriately
 * - Maintains backward compatibility
 */
package com.augmentalis.voiceoscore.accessibility.speech

import android.content.Context
import com.augmentalis.speechrecognition.SpeechEngine
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SpeechEngineFactory
 *
 * COVERAGE:
 * - Factory creates correct adapter for each engine type
 * - Factory handles fallbacks correctly
 * - Created engines conform to ISpeechEngine interface
 */
class SpeechEngineFactoryTest {

    private lateinit var factory: SpeechEngineFactory
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        factory = SpeechEngineFactory()
        mockContext = mockk(relaxed = true)
    }

    @Test
    fun `createEngine with VIVOKA returns VivokaEngineAdapter`() {
        // Act
        val engine = factory.createEngine(SpeechEngine.VIVOKA, mockContext)

        // Assert
        assertTrue("Expected VivokaEngineAdapter", engine is VivokaEngineAdapter)
        assertNotNull("Engine should not be null", engine)
    }

    @Test
    fun `createEngine with ANDROID_STT returns GoogleEngineAdapter`() {
        // Act
        val engine = factory.createEngine(SpeechEngine.ANDROID_STT, mockContext)

        // Assert
        assertTrue("Expected GoogleEngineAdapter", engine is GoogleEngineAdapter)
        assertNotNull("Engine should not be null", engine)
    }

    @Test
    fun `createEngine with WHISPER returns AzureEngineAdapter (stub)`() {
        // Act
        val engine = factory.createEngine(SpeechEngine.WHISPER, mockContext)

        // Assert
        assertTrue("Expected AzureEngineAdapter (stub)", engine is AzureEngineAdapter)
        assertNotNull("Engine should not be null", engine)
    }

    @Test
    fun `createEngine with GOOGLE_CLOUD falls back to GoogleEngineAdapter`() {
        // Act
        val engine = factory.createEngine(SpeechEngine.GOOGLE_CLOUD, mockContext)

        // Assert
        assertTrue("Expected GoogleEngineAdapter (fallback)", engine is GoogleEngineAdapter)
        assertNotNull("Engine should not be null", engine)
    }

    @Test
    fun `createEngine with VOSK falls back to VivokaEngineAdapter`() {
        // Act
        val engine = factory.createEngine(SpeechEngine.VOSK, mockContext)

        // Assert
        assertTrue("Expected VivokaEngineAdapter (fallback)", engine is VivokaEngineAdapter)
        assertNotNull("Engine should not be null", engine)
    }

    @Test
    fun `all created engines implement ISpeechEngine`() {
        // Arrange
        val engineTypes = listOf(
            SpeechEngine.VIVOKA,
            SpeechEngine.ANDROID_STT,
            SpeechEngine.WHISPER,
            SpeechEngine.GOOGLE_CLOUD,
            SpeechEngine.VOSK
        )

        // Act & Assert
        engineTypes.forEach { engineType ->
            val engine = factory.createEngine(engineType, mockContext)
            assertTrue(
                "Engine for $engineType should implement ISpeechEngine",
                engine is ISpeechEngine
            )
        }
    }

    @Test
    fun `factory creates new instance each time`() {
        // Act
        val engine1 = factory.createEngine(SpeechEngine.VIVOKA, mockContext)
        val engine2 = factory.createEngine(SpeechEngine.VIVOKA, mockContext)

        // Assert
        assertNotSame("Factory should create new instances", engine1, engine2)
    }

    @Test
    fun `created engines have required interface methods`() {
        // Arrange
        val engine = factory.createEngine(SpeechEngine.VIVOKA, mockContext)

        // Assert - verify ISpeechEngine interface is implemented
        assertNotNull("Engine should have initialize method", engine::initialize)
        assertNotNull("Engine should have startListening method", engine::startListening)
        assertNotNull("Engine should have stopListening method", engine::stopListening)
        assertNotNull("Engine should have updateCommands method", engine::updateCommands)
        assertNotNull("Engine should have updateConfiguration method", engine::updateConfiguration)
        assertNotNull("Engine should have isRecognizing method", engine::isRecognizing)
        assertNotNull("Engine should have getEngine method", engine::getEngine)
        assertNotNull("Engine should have destroy method", engine::destroy)
    }
}
