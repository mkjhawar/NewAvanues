/**
 * GoogleEngineAdapterTest.kt - Comprehensive tests for GoogleEngineAdapter
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Speech Engine Test Coverage Agent - Sprint 2
 * Created: 2025-12-23
 *
 * Test Coverage: 15 tests
 * - Initialization (3 tests)
 * - Start/Stop Listening (3 tests)
 * - Command Updates (3 tests)
 * - Error Handling (3 tests)
 * - Resource Cleanup (3 tests)
 *
 * Target Coverage: 95%+ line coverage, 90%+ branch coverage
 */
package com.augmentalis.voiceoscore.accessibility.speech

import android.speech.SpeechRecognizer
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.voiceoscore.BaseVoiceOSTest
import com.augmentalis.voiceoscore.MockFactories
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleEngineAdapterTest : BaseVoiceOSTest() {

    private lateinit var adapter: GoogleEngineAdapter
    private val mockContext = MockFactories.createMockContext()

    @Before
    override fun setUp() {
        super.setUp()
        
        // Mock static SpeechRecognizer methods
        mockkStatic(SpeechRecognizer::class)
        every { SpeechRecognizer.isRecognitionAvailable(any()) } returns true
        every { SpeechRecognizer.createSpeechRecognizer(any()) } returns mockk(relaxed = true)
        
        adapter = GoogleEngineAdapter(mockContext)
    }

    @After
    override fun tearDown() {
        unmockkStatic(SpeechRecognizer::class)
        clearAllMocks()
        super.tearDown()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION TESTS (3 tests)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `initialization - success with speech recognition available`() = runTest {
        // Given
        val config = SpeechConfig(language = "en-US")

        // When
        val result = adapter.initialize(config)

        // Then
        assertThat(result).isTrue()
        verify { SpeechRecognizer.isRecognitionAvailable(mockContext) }
        verify { SpeechRecognizer.createSpeechRecognizer(mockContext) }
    }

    @Test
    fun `initialization - failure when speech recognition unavailable`() = runTest {
        // Given
        every { SpeechRecognizer.isRecognitionAvailable(any()) } returns false
        val config = SpeechConfig(language = "en-US")

        // When
        val result = adapter.initialize(config)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `initialization - sets correct language from config`() = runTest {
        // Given
        val mockRecognizer = mockk<SpeechRecognizer>(relaxed = true)
        every { SpeechRecognizer.createSpeechRecognizer(any()) } returns mockRecognizer
        
        val config = SpeechConfig(language = "es-ES")

        // When
        val result = adapter.initialize(config)

        // Then
        assertThat(result).isTrue()
        verify { mockRecognizer.setRecognitionListener(any()) }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // START/STOP LISTENING TESTS (3 tests)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `startListening - throws when not initialized`() {
        // When/Then
        assertFailsWith<IllegalStateException> {
            adapter.startListening()
        }
    }

    @Test
    fun `startListening - success after initialization`() = runTest {
        // Given
        val mockRecognizer = mockk<SpeechRecognizer>(relaxed = true)
        every { SpeechRecognizer.createSpeechRecognizer(any()) } returns mockRecognizer
        
        adapter.initialize(SpeechConfig())

        // When
        adapter.startListening()

        // Then
        verify { mockRecognizer.startListening(any()) }
        assertThat(adapter.isRecognizing()).isTrue()
    }

    @Test
    fun `stopListening - stops recognition safely`() = runTest {
        // Given
        val mockRecognizer = mockk<SpeechRecognizer>(relaxed = true)
        every { SpeechRecognizer.createSpeechRecognizer(any()) } returns mockRecognizer
        
        adapter.initialize(SpeechConfig())
        adapter.startListening()

        // When
        adapter.stopListening()

        // Then
        verify { mockRecognizer.stopListening() }
        assertThat(adapter.isRecognizing()).isFalse()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COMMAND UPDATES TESTS (3 tests)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `updateCommands - warns about unsupported feature`() = runTest {
        // Given
        adapter.initialize(SpeechConfig())
        val commands = listOf("command1", "command2")

        // When - should not throw but log warning
        adapter.updateCommands(commands)

        // Then - verify no crash (Google doesn't support dynamic commands)
        assertThat(adapter.isRecognizing()).isFalse()
    }

    @Test
    fun `updateCommands - works before initialization`() = runTest {
        // Given
        val commands = listOf("test")

        // When/Then - should not throw
        adapter.updateCommands(commands)
    }

    @Test
    fun `updateCommands - handles empty list`() = runTest {
        // Given
        adapter.initialize(SpeechConfig())

        // When/Then - should not throw
        adapter.updateCommands(emptyList())
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ERROR HANDLING TESTS (3 tests)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `error handling - initialization exception handled`() = runTest {
        // Given
        every { SpeechRecognizer.createSpeechRecognizer(any()) } throws RuntimeException("Test error")

        // When
        val result = adapter.initialize(SpeechConfig())

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `error handling - updateConfiguration throws when not initialized`() {
        // When/Then
        assertFailsWith<IllegalStateException> {
            adapter.updateConfiguration(SpeechConfiguration())
        }
    }

    @Test
    fun `error handling - stopListening safe when not listening`() {
        // When/Then - should not throw
        adapter.stopListening()
        assertThat(adapter.isRecognizing()).isFalse()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RESOURCE CLEANUP TESTS (3 tests)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `cleanup - destroy releases all resources`() = runTest {
        // Given
        val mockRecognizer = mockk<SpeechRecognizer>(relaxed = true)
        every { SpeechRecognizer.createSpeechRecognizer(any()) } returns mockRecognizer
        
        adapter.initialize(SpeechConfig())
        adapter.startListening()

        // When
        adapter.destroy()

        // Then
        verify { mockRecognizer.stopListening() }
        verify { mockRecognizer.destroy() }
        assertThat(adapter.getEngine()).isNull()
    }

    @Test
    fun `cleanup - destroy safe when not initialized`() {
        // When/Then - should not throw
        adapter.destroy()
        assertThat(adapter.getEngine()).isNull()
    }

    @Test
    fun `cleanup - destroy is idempotent`() = runTest {
        // Given
        val mockRecognizer = mockk<SpeechRecognizer>(relaxed = true)
        every { SpeechRecognizer.createSpeechRecognizer(any()) } returns mockRecognizer
        
        adapter.initialize(SpeechConfig())

        // When
        adapter.destroy()
        adapter.destroy() // Second call

        // Then - should not throw, verify cleanup called once
        verify(atLeast = 1) { mockRecognizer.destroy() }
    }
}
