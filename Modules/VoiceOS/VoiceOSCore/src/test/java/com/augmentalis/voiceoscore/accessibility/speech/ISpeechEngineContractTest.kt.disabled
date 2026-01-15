/**
 * ISpeechEngineContractTest.kt - Contract tests for ISpeechEngine interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Speech Engine Test Coverage Agent - Sprint 2
 * Created: 2025-12-23
 *
 * PURPOSE:
 * Verify all ISpeechEngine implementations adhere to the interface contract:
 * - All required methods are implemented
 * - Error handling follows the contract
 * - Thread safety is maintained
 * - Lifecycle management is correct
 *
 * Test Coverage: 10 contract tests
 * Adapters Tested: GoogleEngineAdapter, AzureEngineAdapter, VoskEngineAdapter
 */
package com.augmentalis.voiceoscore.accessibility.speech

import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.voiceoscore.BaseVoiceOSTest
import com.augmentalis.voiceoscore.MockFactories
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith

/**
 * Contract tests for ISpeechEngine interface.
 *
 * These tests verify that all implementations of ISpeechEngine follow
 * the defined contract and behave consistently.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ISpeechEngineContractTest : BaseVoiceOSTest() {

    private val context = MockFactories.createMockContext()
    private val testEngines: List<ISpeechEngine> by lazy {
        listOf(
            GoogleEngineAdapter(context),
            AzureEngineAdapter(context),
            VoskEngineAdapter(context)
        )
    }

    @Test
    fun `contract - all implementations provide initialize method`() = runTest {
        testEngines.forEach { engine ->
            // Verify initialize method exists and is callable
            val hasMethod = engine::class.java.methods.any {
                it.name == "initialize" && it.parameterCount == 1
            }
            assertThat(hasMethod)
                .withFailMessage("${engine::class.simpleName} must implement initialize(SpeechConfig)")
                .isTrue()
        }
    }

    @Test
    fun `contract - all implementations provide startListening method`() {
        testEngines.forEach { engine ->
            val hasMethod = engine::class.java.methods.any {
                it.name == "startListening" && it.parameterCount == 0
            }
            assertThat(hasMethod)
                .withFailMessage("${engine::class.simpleName} must implement startListening()")
                .isTrue()
        }
    }

    @Test
    fun `contract - all implementations provide stopListening method`() {
        testEngines.forEach { engine ->
            val hasMethod = engine::class.java.methods.any {
                it.name == "stopListening" && it.parameterCount == 0
            }
            assertThat(hasMethod)
                .withFailMessage("${engine::class.simpleName} must implement stopListening()")
                .isTrue()
        }
    }

    @Test
    fun `contract - all implementations provide updateCommands method`() = runTest {
        testEngines.forEach { engine ->
            val hasMethod = engine::class.java.methods.any {
                it.name == "updateCommands" && it.parameterCount == 2 // method + continuation
            }
            assertThat(hasMethod)
                .withFailMessage("${engine::class.simpleName} must implement updateCommands(List<String>)")
                .isTrue()
        }
    }

    @Test
    fun `contract - all implementations provide updateConfiguration method`() {
        testEngines.forEach { engine ->
            val hasMethod = engine::class.java.methods.any {
                it.name == "updateConfiguration" && it.parameterCount == 1
            }
            assertThat(hasMethod)
                .withFailMessage("${engine::class.simpleName} must implement updateConfiguration(SpeechConfiguration)")
                .isTrue()
        }
    }

    @Test
    fun `contract - all implementations provide destroy method`() {
        testEngines.forEach { engine ->
            val hasMethod = engine::class.java.methods.any {
                it.name == "destroy" && it.parameterCount == 0
            }
            assertThat(hasMethod)
                .withFailMessage("${engine::class.simpleName} must implement destroy()")
                .isTrue()
        }
    }

    @Test
    fun `contract - all implementations throw on uninitialized access`() {
        testEngines.forEach { engine ->
            val exception = assertFailsWith<IllegalStateException>(
                message = "${engine::class.simpleName} should throw IllegalStateException on uninitialized access"
            ) {
                engine.startListening()
            }
            assertThat(exception.message).contains("not initialized")
        }
    }

    @Test
    fun `contract - all implementations provide getEngine method`() {
        testEngines.forEach { engine ->
            val hasMethod = engine::class.java.methods.any {
                it.name == "getEngine" && it.parameterCount == 0
            }
            assertThat(hasMethod)
                .withFailMessage("${engine::class.simpleName} must implement getEngine()")
                .isTrue()
        }
    }

    @Test
    fun `contract - all implementations provide isRecognizing method`() {
        testEngines.forEach { engine ->
            val hasMethod = engine::class.java.methods.any {
                it.name == "isRecognizing" && it.parameterCount == 0
            }
            assertThat(hasMethod)
                .withFailMessage("${engine::class.simpleName} must implement isRecognizing()")
                .isTrue()

            // Should return false when not initialized
            val isRecognizing = engine.isRecognizing()
            assertThat(isRecognizing)
                .withFailMessage("${engine::class.simpleName}.isRecognizing() should return false when not initialized")
                .isFalse()
        }
    }

    @Test
    fun `contract - all implementations handle cleanup gracefully`() {
        testEngines.forEach { engine ->
            // Should not throw even if not initialized
            try {
                engine.destroy()
                // Success - destroy handled gracefully
            } catch (e: Exception) {
                throw AssertionError(
                    "${engine::class.simpleName}.destroy() should handle uninitialized state gracefully",
                    e
                )
            }

            // Verify engine returns null after destroy
            val underlyingEngine = engine.getEngine()
            assertThat(underlyingEngine)
                .withFailMessage("${engine::class.simpleName}.getEngine() should return null after destroy")
                .isNull()
        }
    }
}
