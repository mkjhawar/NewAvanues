/**
 * NluLogger.js.kt - JS/Browser logging via console API
 */
package com.augmentalis.nlu

actual fun nluLogDebug(tag: String, message: String) {
    console.log("D/$tag: $message")
}

actual fun nluLogInfo(tag: String, message: String) {
    console.log("I/$tag: $message")
}

actual fun nluLogWarn(tag: String, message: String) {
    console.warn("W/$tag: $message")
}

actual fun nluLogError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        console.error("E/$tag: $message - ${throwable.message}")
    } else {
        console.error("E/$tag: $message")
    }
}
