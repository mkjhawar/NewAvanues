package com.augmentalis.webavanue

/**
 * Android implementation of system time
 */
internal actual fun currentTimeMillis(): Long {
    return java.lang.System.currentTimeMillis()
}
