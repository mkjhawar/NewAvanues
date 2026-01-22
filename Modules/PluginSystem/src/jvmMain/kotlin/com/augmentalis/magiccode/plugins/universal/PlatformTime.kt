/**
 * PlatformTime.kt - JVM platform time implementation
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 */
package com.augmentalis.magiccode.plugins.universal

/**
 * JVM implementation of currentTimeMillis.
 */
actual fun currentTimeMillis(): Long = System.currentTimeMillis()
