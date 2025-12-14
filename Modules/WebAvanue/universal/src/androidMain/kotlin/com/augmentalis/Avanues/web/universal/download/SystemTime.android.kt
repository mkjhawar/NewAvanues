package com.augmentalis.webavanue.feature.download

/**
 * Android implementation of system time
 */
internal actual fun currentTimeMillis(): Long {
    return java.lang.System.currentTimeMillis()
}
