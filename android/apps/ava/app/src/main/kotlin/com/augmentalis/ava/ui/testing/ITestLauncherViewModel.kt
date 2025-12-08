// filename: apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/testing/ITestLauncherViewModel.kt
// created: 2025-11-16
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.ui.testing

import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for TestLauncherViewModel to enable isolated component testing
 *
 * This interface defines the contract that both the real TestLauncherViewModel
 * and FakeTestLauncherViewModel (for tests) must implement.
 *
 * Benefits:
 * - Enables isolated component testing without MainActivity navigation
 * - Allows tests to use FakeViewModel without complex setup
 * - Maintains clean architecture and testability
 *
 * Created: 2025-11-16 (Technical Debt Resolution - UI Test Coverage)
 */
interface ITestLauncherViewModel {
    /**
     * List of test suites exposed to TestLauncherScreen
     */
    val testSuites: StateFlow<List<TestSuite>>

    /**
     * Whether tests are currently running
     */
    val isRunning: StateFlow<Boolean>

    /**
     * Run all test suites
     */
    fun runAllTests()

    /**
     * Run a specific test suite by name
     */
    fun runTestSuite(suiteName: String)
}
