package com.augmentalis.httpavanue.middleware

import com.augmentalis.httpavanue.http.HttpResponse
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Date header middleware — auto-adds RFC 7231 Date header to all responses.
 *
 * Closes NanoHTTPD gap #3: automatic Date header generation.
 * Format: "Date: Sat, 22 Feb 2026 08:30:00 GMT"
 */
fun dateHeaderMiddleware() = middleware { request, next ->
    val response = next(request)
    if ("Date" !in response.headers) {
        response.copy(
            headers = response.headers + ("Date" to formatHttpDate()),
        )
    } else {
        response
    }
}

private val DAY_NAMES = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val MONTH_NAMES = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/**
 * Format current UTC time in RFC 7231 / IMF-fixdate format:
 *   "Sat, 22 Feb 2026 08:30:00 GMT"
 */
internal fun formatHttpDate(): String {
    val now = Clock.System.now()
    val utc = now.toLocalDateTime(TimeZone.UTC)
    val dayOfWeek = utc.dayOfWeek.ordinal // Monday = 0
    val dayName = DAY_NAMES[dayOfWeek]
    val monthName = MONTH_NAMES[utc.monthNumber - 1]
    return "$dayName, %02d $monthName ${utc.year} %02d:%02d:%02d GMT".format(
        utc.dayOfMonth, utc.hour, utc.minute, utc.second
    )
}
