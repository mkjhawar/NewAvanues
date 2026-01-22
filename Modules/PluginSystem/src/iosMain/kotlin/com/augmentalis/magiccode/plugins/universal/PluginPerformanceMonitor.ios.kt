/**
 * PluginPerformanceMonitor.ios.kt - iOS platform implementation
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22 (Phase 4)
 */
package com.augmentalis.magiccode.plugins.universal

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * iOS implementation of getCurrentTimeMs.
 *
 * Uses NSDate.timeIntervalSince1970 for wall-clock time.
 */
actual fun getCurrentTimeMs(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
