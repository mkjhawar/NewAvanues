package com.augmentalis.httpavanue.middleware

import com.avanues.logging.Logger
import com.avanues.logging.LoggerFactory
import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse

class LoggerMiddleware(private val logger: Logger = LoggerFactory.getLogger("HTTP")) : Middleware {
    override suspend fun handle(request: HttpRequest, next: suspend (HttpRequest) -> HttpResponse): HttpResponse {
        logger.d { "-> ${request.method} ${request.uri}" }
        val response = next(request)
        logger.d { "<- ${response.status} ${request.method} ${request.uri}" }
        return response
    }
}

fun loggerMiddleware() = LoggerMiddleware()
