package com.augmentalis.Avanues.web.universal.download

/**
 * Android implementation of system time
 */
internal actual fun currentTimeMillis(): Long {
    return java.lang.System.currentTimeMillis()
}
