/**
 * JvmLogger.kt - JVM-specific logger implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.commandmanager

import java.io.PrintStream

/**
 * JVM implementation of Logger using System.out/err
 *
 * Features:
 * - Lazy evaluation (message lambda only called if logging enabled)
 * - Uses System.out for INFO and below
 * - Uses System.err for WARN and above
 * - Colored output (if terminal supports ANSI)
 */
class JvmLogger(private val tag: String) : Logger {

    // ANSI color codes
    private val RESET = "\u001B[0m"
    private val GRAY = "\u001B[90m"
    private val BLUE = "\u001B[34m"
    private val GREEN = "\u001B[32m"
    private val YELLOW = "\u001B[33m"
    private val RED = "\u001B[31m"
    private val MAGENTA = "\u001B[35m"

    private val stdErr: PrintStream get() = java.lang.System.err

    override fun v(message: () -> String) {
        if (isLoggable(LogLevel.VERBOSE)) {
            println("${GRAY}VERBOSE [$tag]: ${message()}${RESET}")
        }
    }

    override fun d(message: () -> String) {
        if (isLoggable(LogLevel.DEBUG)) {
            println("${BLUE}DEBUG [$tag]: ${message()}${RESET}")
        }
    }

    override fun i(message: () -> String) {
        if (isLoggable(LogLevel.INFO)) {
            println("${GREEN}INFO [$tag]: ${message()}${RESET}")
        }
    }

    override fun w(message: () -> String) {
        if (isLoggable(LogLevel.WARN)) {
            stdErr.println("${YELLOW}WARN [$tag]: ${message()}${RESET}")
        }
    }

    override fun e(message: () -> String) {
        if (isLoggable(LogLevel.ERROR)) {
            stdErr.println("${RED}ERROR [$tag]: ${message()}${RESET}")
        }
    }

    override fun e(message: () -> String, throwable: Throwable) {
        if (isLoggable(LogLevel.ERROR)) {
            stdErr.println("${RED}ERROR [$tag]: ${message()}${RESET}")
            throwable.printStackTrace()
        }
    }

    override fun wtf(message: () -> String) {
        if (isLoggable(LogLevel.ASSERT)) {
            stdErr.println("${MAGENTA}WTF [$tag]: ${message()}${RESET}")
        }
    }

    override fun isLoggable(level: LogLevel): Boolean {
        // JVM logs everything by default (can be configured via system properties)
        val minLevel = java.lang.System.getProperty("log.level") ?: "VERBOSE"
        val configuredLevel = runCatching {
            LogLevel.valueOf(minLevel.uppercase())
        }.getOrDefault(LogLevel.VERBOSE)

        return level.priority >= configuredLevel.priority
    }
}
