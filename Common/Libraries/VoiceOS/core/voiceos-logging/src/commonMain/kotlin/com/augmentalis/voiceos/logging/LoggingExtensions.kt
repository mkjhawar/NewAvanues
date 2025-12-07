/**
 * LoggingExtensions.kt - Convenience extensions for logging
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.logging

/**
 * Extension function to get a logger for any class
 *
 * Usage:
 * ```kotlin
 * class MyClass {
 *     private val log = logger()
 *
 *     fun doSomething() {
 *         log.d { "Doing something" }
 *     }
 * }
 * ```
 */
inline fun <reified T : Any> T.logger(): PIISafeLogger {
    return PIILoggingWrapper.getLogger(T::class.simpleName ?: "Unknown")
}

/**
 * Extension function to get a logger by class reference
 *
 * Usage:
 * ```kotlin
 * val log = loggerFor<MyClass>()
 * log.d { "Message" }
 * ```
 */
inline fun <reified T : Any> loggerFor(): PIISafeLogger {
    return PIILoggingWrapper.getLogger(T::class.simpleName ?: "Unknown")
}
