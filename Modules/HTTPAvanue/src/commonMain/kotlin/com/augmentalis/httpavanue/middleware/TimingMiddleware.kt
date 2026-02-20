package com.augmentalis.httpavanue.middleware

import com.avanues.logging.Logger
import com.avanues.logging.LoggerFactory
import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse
import kotlinx.datetime.Clock

class TimingMiddleware(
    private val logger: Logger = LoggerFactory.getLogger("Timing"),
    private val warnThresholdMs: Long = 1000,
) : Middleware {
    override suspend fun handle(request: HttpRequest, next: suspend (HttpRequest) -> HttpResponse): HttpResponse {
        val startTime = Clock.System.now()
        val response = next(request)
        val durationMs = (Clock.System.now() - startTime).inWholeMilliseconds
        val logMessage = "${request.method} ${request.uri} - ${durationMs}ms"
        if (durationMs > warnThresholdMs) logger.w { "$logMessage (SLOW)" } else logger.d { logMessage }
        return response.copy(headers = response.headers + ("X-Response-Time" to "${durationMs}ms"))
    }
}

fun timingMiddleware(warnThresholdMs: Long = 1000) = TimingMiddleware(warnThresholdMs = warnThresholdMs)
