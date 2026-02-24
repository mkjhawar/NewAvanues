/**
 * NluLogger.desktop.kt - Desktop/JVM logging via java.util.logging
 */
package com.augmentalis.nlu

import java.util.logging.Level
import java.util.logging.Logger

private val logger = Logger.getLogger("NLU")

actual fun nluLogDebug(tag: String, message: String) {
    logger.fine("$tag: $message")
}

actual fun nluLogInfo(tag: String, message: String) {
    logger.info("$tag: $message")
}

actual fun nluLogWarn(tag: String, message: String) {
    logger.warning("$tag: $message")
}

actual fun nluLogError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        logger.log(Level.SEVERE, "$tag: $message", throwable)
    } else {
        logger.severe("$tag: $message")
    }
}
