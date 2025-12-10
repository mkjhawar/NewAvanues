// filename: apps/ava-standalone/src/androidTest/kotlin/com/augmentalis/ava/CustomTestRunner.kt
// created: 2025-11-15
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom test runner for Hilt instrumentation tests
 *
 * Configures tests to use HiltTestApplication instead of the main AvaApplication,
 * allowing proper dependency injection in tests.
 *
 * Usage in build.gradle.kts:
 * ```
 * android {
 *     defaultConfig {
 *         testInstrumentationRunner = "com.augmentalis.ava.CustomTestRunner"
 *     }
 * }
 * ```
 */
class CustomTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
