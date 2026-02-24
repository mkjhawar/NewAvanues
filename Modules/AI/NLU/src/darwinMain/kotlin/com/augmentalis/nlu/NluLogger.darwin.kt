/**
 * NluLogger.darwin.kt - Darwin (iOS/macOS) logging via NSLog
 */
package com.augmentalis.nlu

import platform.Foundation.NSLog

actual fun nluLogDebug(tag: String, message: String) {
    NSLog("D/$tag: $message")
}

actual fun nluLogInfo(tag: String, message: String) {
    NSLog("I/$tag: $message")
}

actual fun nluLogWarn(tag: String, message: String) {
    NSLog("W/$tag: $message")
}

actual fun nluLogError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        NSLog("E/$tag: $message - ${throwable.message}")
    } else {
        NSLog("E/$tag: $message")
    }
}
