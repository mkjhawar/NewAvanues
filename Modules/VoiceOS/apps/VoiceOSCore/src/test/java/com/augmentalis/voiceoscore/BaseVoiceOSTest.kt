/**
 * BaseVoiceOSTest.kt - Base class for all VoiceOS unit tests
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Database Test Coverage Agent - Sprint 1
 * Created: 2025-12-23
 *
 * Provides common test infrastructure for VoiceOS unit tests including:
 * - Coroutine test dispatchers
 * - MockK initialization
 * - InstantTaskExecutor for LiveData testing
 */

package com.augmentalis.voiceoscore

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule

/**
 * Base test class providing common test infrastructure.
 *
 * ## Features:
 * - Coroutine testing with TestScope and TestDispatcher
 * - Automatic MockK initialization
 * - InstantTaskExecutor for synchronous LiveData operations
 * - Main dispatcher replacement for unit tests
 *
 * ## Usage:
 * ```kotlin
 * class MyTest : BaseVoiceOSTest() {
 *     @Test
 *     fun `my test`() = runTest {
 *         // Test code with coroutines
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseVoiceOSTest {

    /**
     * Rule to execute LiveData operations synchronously.
     * Required for testing LiveData in unit tests.
     */
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    /**
     * Test dispatcher for coroutine testing.
     * Provides deterministic execution order for tests.
     */
    protected val testDispatcher = StandardTestDispatcher()

    /**
     * Test scope for launching coroutines in tests.
     */
    protected val testScope = TestScope(testDispatcher)

    /**
     * Set up test environment before each test.
     * - Initializes MockK annotations
     * - Replaces Main dispatcher with test dispatcher
     */
    @Before
    open fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * Clean up test environment after each test.
     * - Resets Main dispatcher to default
     */
    @After
    open fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Run a test with coroutines using the test scope.
     *
     * ## Usage:
     * ```kotlin
     * @Test
     * fun `my test`() = runTest {
     *     // Suspend functions can be called here
     *     val result = myRepository.fetchData()
     *     assertEquals(expected, result)
     * }
     * ```
     *
     * @param block Test block to execute
     */
    protected fun runTest(block: suspend TestScope.() -> Unit) {
        testScope.runTest(block)
    }
}
