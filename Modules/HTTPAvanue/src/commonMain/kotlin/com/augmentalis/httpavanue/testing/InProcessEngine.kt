package com.augmentalis.httpavanue.testing

import com.augmentalis.httpavanue.http.HttpMethod
import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse
import com.augmentalis.httpavanue.middleware.MiddlewarePipeline
import com.augmentalis.httpavanue.routing.Router
import com.augmentalis.httpavanue.server.HttpServer

/**
 * In-process test engine — handles HTTP requests through the middleware
 * pipeline and router without network binding.
 *
 * Useful for testing route handlers, middleware logic, and request
 * processing without starting a real server or creating sockets.
 */
class InProcessEngine(
    private val router: Router,
    private val middlewarePipeline: MiddlewarePipeline = MiddlewarePipeline.empty(),
) {
    /**
     * Handle a request through the full middleware pipeline + router.
     */
    suspend fun handle(request: HttpRequest): HttpResponse {
        return middlewarePipeline.execute(request) { req -> router.handle(req) }
    }

    /** Convenience: GET request */
    suspend fun get(
        path: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse = handle(HttpRequest(
        method = HttpMethod.GET,
        uri = path,
        headers = headers,
    ))

    /** Convenience: POST request with body */
    suspend fun post(
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse = handle(HttpRequest(
        method = HttpMethod.POST,
        uri = path,
        headers = if (body != null) headers + ("Content-Type" to "application/json") else headers,
        body = body?.encodeToByteArray(),
    ))

    /** Convenience: PUT request with body */
    suspend fun put(
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse = handle(HttpRequest(
        method = HttpMethod.PUT,
        uri = path,
        headers = if (body != null) headers + ("Content-Type" to "application/json") else headers,
        body = body?.encodeToByteArray(),
    ))

    /** Convenience: DELETE request */
    suspend fun delete(
        path: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse = handle(HttpRequest(
        method = HttpMethod.DELETE,
        uri = path,
        headers = headers,
    ))
}
