/*
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 *
 * PlatformTime.ios.kt - iOS implementation for time utilities
 *
 * Author: Manoj Jhawar
 * Created: 2026-01-16
 */

package com.augmentalis.voiceoscoreng.safety

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * Get current time in milliseconds (iOS implementation).
 */
internal actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()
