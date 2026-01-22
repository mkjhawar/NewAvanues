/**
 * CoroutineTestRule.kt - JUnit rule for coroutine testing
 *
 * Provides test dispatcher for coroutine-based tests.
 * Replaces main dispatcher with test dispatcher.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-26
 */

package com.augmentalis.voiceoscore.test.infrastructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule for setting up coroutine test environment.
 *
 * Usage:
 * ```
 * @get:Rule
 * val coroutineRule = CoroutineTestRule()
 *
 * @Test
 * fun testSomething() = runTest(coroutineRule.testDispatcher) {
 *     // Test code with coroutines
 * }
 * ```
 */
@ExperimentalCoroutinesApi
class CoroutineTestRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}
