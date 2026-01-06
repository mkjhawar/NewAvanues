/**
 * Time.kt - Android time utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscoreng.repository

/**
 * Android implementation using System.currentTimeMillis()
 */
actual fun currentTimeMillis(): Long = System.currentTimeMillis()
