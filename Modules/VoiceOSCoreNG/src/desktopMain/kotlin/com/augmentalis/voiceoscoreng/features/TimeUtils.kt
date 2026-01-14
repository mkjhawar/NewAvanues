/**
 * TimeUtils.kt - Desktop (JVM) implementation of time utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 */
package com.augmentalis.voiceoscoreng.features

/**
 * Desktop/JVM implementation of currentTimeMillis
 */
actual fun currentTimeMillis(): Long = System.currentTimeMillis()
