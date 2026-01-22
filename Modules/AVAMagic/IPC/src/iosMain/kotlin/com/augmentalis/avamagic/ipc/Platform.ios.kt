package com.augmentalis.avamagic.ipc

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * iOS platform implementation for currentTimeMillis
 */
actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
