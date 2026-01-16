/*
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 *
 * PlatformTime.desktop.kt - Desktop (JVM) implementation for time utilities
 *
 * Author: Manoj Jhawar
 * Created: 2026-01-16
 */

package com.augmentalis.voiceoscoreng.safety

/**
 * Get current time in milliseconds (Desktop/JVM implementation).
 */
internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
