package com.augmentalis.voiceoscoreng.functions

/**
 * Desktop (JVM) implementation of platform-specific functions.
 */
actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
