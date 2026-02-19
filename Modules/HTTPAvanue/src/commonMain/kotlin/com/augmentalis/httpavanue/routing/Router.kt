package com.augmentalis.httpavanue.routing

import com.augmentalis.httpavanue.http.HttpMethod
import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse

typealias RouteHandler = suspend (HttpRequest) -> HttpResponse

data class Route(
    val method: HttpMethod,
    val pattern: String,
    val handler: RouteHandler,
) {
    private val pathSegments = pattern.split('/').filter { it.isNotEmpty() }

    fun match(method: HttpMethod, path: String): Map<String, String>? {
        if (this.method != method) return null
        val requestSegments = path.split('/').filter { it.isNotEmpty() }
        if (pathSegments.size != requestSegments.size) return null
        val params = mutableMapOf<String, String>()
        pathSegments.forEachIndexed { index, segment ->
            val requestSegment = requestSegments[index]
            when {
                segment.startsWith(':') -> params[segment.substring(1)] = requestSegment
                segment != requestSegment -> return null
            }
        }
        return params
    }
}

/**
 * HTTP Router with pattern matching
 */
class Router {
    private val routes = mutableListOf<Route>()

    fun route(method: HttpMethod, pattern: String, handler: RouteHandler) {
        routes.add(Route(method, pattern, handler))
    }

    fun findRoute(request: HttpRequest): Pair<Route, Map<String, String>>? {
        routes.forEach { route ->
            route.match(request.method, request.path)?.let { params -> return route to params }
        }
        return null
    }

    suspend fun handle(request: HttpRequest): HttpResponse {
        val (route, params) = findRoute(request) ?: return HttpResponse.notFound()
        val requestWithParams = request.copy(pathParams = params)
        return try {
            route.handler(requestWithParams)
        } catch (e: Exception) {
            HttpResponse.internalError(e.message ?: "Internal server error")
        }
    }

    fun get(pattern: String, handler: RouteHandler) = route(HttpMethod.GET, pattern, handler)
    fun post(pattern: String, handler: RouteHandler) = route(HttpMethod.POST, pattern, handler)
    fun put(pattern: String, handler: RouteHandler) = route(HttpMethod.PUT, pattern, handler)
    fun delete(pattern: String, handler: RouteHandler) = route(HttpMethod.DELETE, pattern, handler)
    fun patch(pattern: String, handler: RouteHandler) = route(HttpMethod.PATCH, pattern, handler)
    fun options(pattern: String, handler: RouteHandler) = route(HttpMethod.OPTIONS, pattern, handler)
    fun head(pattern: String, handler: RouteHandler) = route(HttpMethod.HEAD, pattern, handler)
}

fun router(block: Router.() -> Unit) = Router().apply(block)
