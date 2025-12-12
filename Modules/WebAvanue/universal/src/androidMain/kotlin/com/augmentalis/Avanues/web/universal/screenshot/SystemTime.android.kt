package com.augmentalis.Avanues.web.universal.screenshot

import java.text.SimpleDateFormat
import java.util.*

/**
 * Get current time formatted for filename
 * Format: YYYYMMDD_HHMMSS
 */
internal actual fun currentFormattedTime(): String {
    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    return dateFormat.format(Date())
}
