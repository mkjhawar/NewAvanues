package com.augmentalis.httpavanue.platform

/**
 * Platform-specific current time in milliseconds.
 * Replaces AvaConnect's PlatformContext.currentTimeMillis().
 */
expect fun currentTimeMillis(): Long
