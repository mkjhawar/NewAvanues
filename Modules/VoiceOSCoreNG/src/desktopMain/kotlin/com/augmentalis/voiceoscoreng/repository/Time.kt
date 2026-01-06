/**
 * Time.kt - Desktop (JVM) time utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscoreng.repository

/**
 * Desktop implementation using System.currentTimeMillis()
 */
actual fun currentTimeMillis(): Long = System.currentTimeMillis()
