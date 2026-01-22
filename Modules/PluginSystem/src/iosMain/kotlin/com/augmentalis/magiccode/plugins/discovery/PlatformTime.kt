/**
 * PlatformTime.kt - iOS platform time implementation for discovery
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 */
package com.augmentalis.magiccode.plugins.discovery

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * iOS implementation of currentTimeMillis for discovery package.
 */
internal actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
