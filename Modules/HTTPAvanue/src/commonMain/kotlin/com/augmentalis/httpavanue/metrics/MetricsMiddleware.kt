package com.augmentalis.httpavanue.metrics

import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse
import com.augmentalis.httpavanue.middleware.Middleware
import com.augmentalis.httpavanue.platform.currentTimeMillis

/**
 * Middleware that automatically collects metrics for every request
 */
class MetricsMiddleware(private val metrics: ServerMetrics = ServerMetrics()) : Middleware {
    override suspend fun handle(
        request: HttpRequest,
        next: suspend (HttpRequest) -> HttpResponse,
    ): HttpResponse {
        metrics.recordRequest(request.method.name, request.path)
        val startTime = currentTimeMillis()
        val response = next(request)
        val duration = currentTimeMillis() - startTime
        metrics.recordResponse(response.status, duration)
        return response
    }

    fun getMetrics() = metrics
}

fun metricsMiddleware(metrics: ServerMetrics = ServerMetrics()) = MetricsMiddleware(metrics)
