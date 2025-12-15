package com.augmentalis.webavanue.feature.screenshot

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Get current time formatted for filename
 * Format: YYYYMMDD_HHMMSS
 */
internal actual fun currentFormattedTime(): String {
    val now = Clock.System.now()
    val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())

    val year = localDateTime.year
    val month = localDateTime.monthNumber.toString().padStart(2, '0')
    val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
    val hour = localDateTime.hour.toString().padStart(2, '0')
    val minute = localDateTime.minute.toString().padStart(2, '0')
    val second = localDateTime.second.toString().padStart(2, '0')

    return "${year}${month}${day}_${hour}${minute}${second}"
}
