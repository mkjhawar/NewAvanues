/**
 * PlatformTime.kt - JVM platform time implementation for discovery
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 */
package com.augmentalis.magiccode.plugins.discovery

/**
 * JVM implementation of currentTimeMillis for discovery package.
 */
internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
