package com.avanues.logging

/**
 * JS/Browser logger factory implementation.
 *
 * Creates [JsLogger] instances backed by the browser/Node.js console API.
 */
actual object LoggerFactory {
    actual fun getLogger(tag: String): Logger = JsLogger(tag)
}
