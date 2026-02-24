package com.augmentalis.httpavanue.middleware

import com.augmentalis.httpavanue.http.HttpMethod
import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse

/**
 * Auto HEAD middleware — intercepts HEAD requests, runs the GET handler,
 * strips the response body, and preserves Content-Length.
 *
 * This removes the need to register separate HEAD handlers for every GET route.
 */
fun autoHeadMiddleware() = middleware { request, next ->
    if (request.method != HttpMethod.HEAD) {
        return@middleware next(request)
    }
    // Run as GET, then strip body
    val getRequest = request.copy(method = HttpMethod.GET)
    val response = next(getRequest)
    val contentLength = response.body?.size?.toString()
    val headers = response.headers.toMutableMap()
    if (contentLength != null) headers["Content-Length"] = contentLength
    response.copy(
        headers = headers,
        body = null, // Strip body for HEAD
    )
}
