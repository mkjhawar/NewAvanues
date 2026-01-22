package com.augmentalis.ava.platform

import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.LogRecord
import java.util.logging.SimpleFormatter
import java.util.logging.StreamHandler

/**
 * Desktop (JVM) implementation of Logger using java.util.logging.Logger.
 *
 * Maps platform-agnostic LogLevel to java.util.logging.Level.
 */
actual object Logger {

    private var minLevel: LogLevel = LogLevel.DEBUG

    init {
        // Configure java.util.logging to use a simple console format
        setupLogging()
    }

    actual fun v(tag: String, message: String) {
        if (shouldLog(LogLevel.VERBOSE)) {
            getLogger(tag).log(Level.FINEST, message)
        }
    }

    actual fun d(tag: String, message: String) {
        if (shouldLog(LogLevel.DEBUG)) {
            getLogger(tag).log(Level.FINE, message)
        }
    }

    actual fun i(tag: String, message: String) {
        if (shouldLog(LogLevel.INFO)) {
            getLogger(tag).log(Level.INFO, message)
        }
    }

    actual fun w(tag: String, message: String) {
        if (shouldLog(LogLevel.WARN)) {
            getLogger(tag).log(Level.WARNING, message)
        }
    }

    actual fun w(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.WARN)) {
            getLogger(tag).log(Level.WARNING, message, throwable)
        }
    }

    actual fun e(tag: String, message: String) {
        if (shouldLog(LogLevel.ERROR)) {
            getLogger(tag).log(Level.SEVERE, message)
        }
    }

    actual fun e(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.ERROR)) {
            getLogger(tag).log(Level.SEVERE, message, throwable)
        }
    }

    actual fun setMinLevel(level: LogLevel) {
        minLevel = level
        updateLoggerLevels()
    }

    private fun getLogger(tag: String): java.util.logging.Logger {
        return java.util.logging.Logger.getLogger(tag)
    }

    private fun shouldLog(level: LogLevel): Boolean {
        return level.ordinal >= minLevel.ordinal
    }

    private fun setupLogging() {
        // Remove default handlers
        val rootLogger = java.util.logging.Logger.getLogger("")
        rootLogger.handlers.forEach { rootLogger.removeHandler(it) }

        // Add custom console handler with simple formatting
        val handler = object : StreamHandler(System.out, SimpleFormatter()) {
            override fun publish(record: LogRecord) {
                super.publish(record)
                flush()
            }
        }
        handler.level = Level.ALL
        rootLogger.addHandler(handler)
        rootLogger.level = Level.ALL

        updateLoggerLevels()
    }

    private fun updateLoggerLevels() {
        val javaLevel = when (minLevel) {
            LogLevel.VERBOSE -> Level.FINEST
            LogLevel.DEBUG -> Level.FINE
            LogLevel.INFO -> Level.INFO
            LogLevel.WARN -> Level.WARNING
            LogLevel.ERROR -> Level.SEVERE
        }

        val rootLogger = java.util.logging.Logger.getLogger("")
        rootLogger.level = javaLevel
        rootLogger.handlers.forEach { it.level = javaLevel }
    }
}
