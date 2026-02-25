/**
 * NluLogger.android.kt - Android logging via android.util.Log
 */
package com.augmentalis.nlu

import android.util.Log

actual fun nluLogDebug(tag: String, message: String) {
    Log.d(tag, message)
}

actual fun nluLogInfo(tag: String, message: String) {
    Log.i(tag, message)
}

actual fun nluLogWarn(tag: String, message: String) {
    Log.w(tag, message)
}

actual fun nluLogError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        Log.e(tag, message, throwable)
    } else {
        Log.e(tag, message)
    }
}
