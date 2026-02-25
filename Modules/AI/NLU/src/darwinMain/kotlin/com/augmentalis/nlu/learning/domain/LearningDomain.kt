package com.augmentalis.nlu.learning.domain

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * Darwin (iOS + macOS) implementation of platform time utility.
 */
actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
