/**
 * DesktopLogger.kt - Desktop/JVM-specific logger implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-02 (consolidated from duplicate implementations)
 */
package com.avanues.logging

/**
 * Desktop/JVM implementation of Logger using System.out/err
 *
 * Features:
 * - Lazy evaluation (message lambda only called if logging enabled)
 * - Uses System.out for INFO and below
 * - Uses System.err for WARN and above
 * - ANSI colored output (if terminal supports it)
 */
internal class DesktopLogger(private val tag: String) : Logger {

    // ANSI color codes
    private val reset = "\u001B[0m"
    private val gray = "\u001B[90m"
    private val blue = "\u001B[34m"
    private val green = "\u001B[32m"
    private val yellow = "\u001B[33m"
    private val red = "\u001B[31m"
    private val magenta = "\u001B[35m"

    override fun v(message: () -> String) {
        if (isLoggable(LogLevel.VERBOSE)) {
            println("${gray}VERBOSE [$tag]: ${message()}${reset}")
        }
    }

    override fun d(message: () -> String) {
        if (isLoggable(LogLevel.DEBUG)) {
            println("${blue}DEBUG [$tag]: ${message()}${reset}")
        }
    }

    override fun i(message: () -> String) {
        if (isLoggable(LogLevel.INFO)) {
            println("${green}INFO [$tag]: ${message()}${reset}")
        }
    }

    override fun w(message: () -> String) {
        if (isLoggable(LogLevel.WARN)) {
            System.err.println("${yellow}WARN [$tag]: ${message()}${reset}")
        }
    }

    override fun e(message: () -> String) {
        if (isLoggable(LogLevel.ERROR)) {
            System.err.println("${red}ERROR [$tag]: ${message()}${reset}")
        }
    }

    override fun e(message: () -> String, throwable: Throwable) {
        if (isLoggable(LogLevel.ERROR)) {
            System.err.println("${red}ERROR [$tag]: ${message()}${reset}")
            throwable.printStackTrace()
        }
    }

    override fun wtf(message: () -> String) {
        if (isLoggable(LogLevel.ASSERT)) {
            System.err.println("${magenta}WTF [$tag]: ${message()}${reset}")
        }
    }

    override fun isLoggable(level: LogLevel): Boolean {
        // Desktop respects globalMinLevel and system property
        val systemLevel = System.getProperty("log.level")?.uppercase()?.let {
            runCatching { LogLevel.valueOf(it) }.getOrNull()
        }
        val effectiveMinLevel = systemLevel ?: globalMinLevel
        return level.priority >= effectiveMinLevel.priority
    }
}
