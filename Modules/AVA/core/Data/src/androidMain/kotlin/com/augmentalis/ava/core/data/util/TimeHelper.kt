package com.augmentalis.ava.core.data.util

/**
 * Android implementation of TimeHelper
 */
actual object TimeHelper {
    actual fun currentTimeMillis(): Long = System.currentTimeMillis()
}
