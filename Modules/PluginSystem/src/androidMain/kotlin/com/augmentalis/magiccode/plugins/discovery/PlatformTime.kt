/**
 * PlatformTime.kt - Android platform time implementation for discovery
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 */
package com.augmentalis.magiccode.plugins.discovery

/**
 * Android implementation of currentTimeMillis for discovery package.
 */
internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
