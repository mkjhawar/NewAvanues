package com.augmentalis.llm

import org.slf4j.LoggerFactory

/**
 * Desktop logging utility
 *
 * Provides a simple logging API similar to Timber but using SLF4J.
 */
object DesktopLogger {
    private val logger = LoggerFactory.getLogger("LLM")

    fun d(message: String) {
        logger.debug(message)
    }

    fun i(message: String) {
        logger.info(message)
    }

    fun w(message: String) {
        logger.warn(message)
    }

    fun w(throwable: Throwable, message: String) {
        logger.warn(message, throwable)
    }

    fun e(message: String) {
        logger.error(message)
    }

    fun e(throwable: Throwable, message: String) {
        logger.error(message, throwable)
    }
}
