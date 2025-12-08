// filename: apps/ava-standalone/src/androidTest/kotlin/com/augmentalis/ava/fakes/FakeTestLauncherViewModel.kt
// created: 2025-11-16
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.fakes

import androidx.lifecycle.ViewModel
import com.augmentalis.ava.ui.testing.ITestLauncherViewModel
import com.augmentalis.ava.ui.testing.TestCase
import com.augmentalis.ava.ui.testing.TestStatus
import com.augmentalis.ava.ui.testing.TestSuite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake TestLauncherViewModel for testing
 *
 * Provides same interface as real TestLauncherViewModel but with in-memory state
 */
class FakeTestLauncherViewModel : ViewModel(), ITestLauncherViewModel {

    private val _testSuites = MutableStateFlow(createTestSuites())
    override val testSuites: StateFlow<List<TestSuite>> = _testSuites.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    override fun runAllTests() {
        _isRunning.value = true
    }

    override fun runTestSuite(suiteName: String) {
        // No-op for testing
    }

    // Test helper - Set initial suites for testing
    fun setTestSuites(suites: List<TestSuite>) {
        _testSuites.value = suites
    }

    private fun createTestSuites(): List<TestSuite> {
        return listOf(
            TestSuite(
                name = "Language Detection",
                tests = listOf(
                    TestCase("English detection"),
                    TestCase("Spanish detection")
                )
            ),
            TestSuite(
                name = "Token Sampling",
                tests = listOf(
                    TestCase("Temperature sampling"),
                    TestCase("Top-K sampling")
                )
            )
        )
    }
}
