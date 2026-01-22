/**
 * PluginPerformanceMonitor.android.kt - Android platform implementation
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22 (Phase 4)
 */
package com.augmentalis.magiccode.plugins.universal

/**
 * Android implementation of getCurrentTimeMs.
 *
 * Uses System.currentTimeMillis() for wall-clock time.
 */
actual fun getCurrentTimeMs(): Long = System.currentTimeMillis()
