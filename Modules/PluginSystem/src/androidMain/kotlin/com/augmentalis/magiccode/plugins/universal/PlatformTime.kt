/**
 * PlatformTime.kt - Android platform time implementation
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 */
package com.augmentalis.magiccode.plugins.universal

/**
 * Android implementation of currentTimeMillis.
 */
internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
