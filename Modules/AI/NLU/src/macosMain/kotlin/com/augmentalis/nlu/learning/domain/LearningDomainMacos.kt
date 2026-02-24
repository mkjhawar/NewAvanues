package com.augmentalis.nlu.learning.domain

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * macOS implementation of platform utilities
 */
actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
