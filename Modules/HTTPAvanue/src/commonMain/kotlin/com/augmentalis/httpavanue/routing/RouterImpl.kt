package com.augmentalis.httpavanue.routing

import com.augmentalis.httpavanue.http.HttpMethod
import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse
import com.augmentalis.httpavanue.middleware.Middleware
import com.augmentalis.httpavanue.middleware.MiddlewarePipeline

/**
 * Enhanced router with middleware support, route grouping, and before/after hooks
 */
class RouterImpl(
    private val prefix: String = "",
    private val middlewares: List<Middleware> = emptyList(),
) {
    private val registry = RouteRegistry()
    private val beforeHandlers = mutableListOf<suspend (HttpRequest) -> Unit>()
    private val afterHandlers = mutableListOf<suspend (HttpRequest, HttpResponse) -> Unit>()

    fun route(method: HttpMethod, pattern: String, middleware: List<Middleware> = emptyList(), handler: RouteHandler) {
        val fullPattern = if (prefix.isNotEmpty()) "$prefix$pattern" else pattern
        val allMiddlewares = this.middlewares + middleware
        val wrappedHandler: RouteHandler = { request ->
            beforeHandlers.forEach { it(request) }
            val response = if (allMiddlewares.isEmpty()) handler(request)
            else MiddlewarePipeline(allMiddlewares).execute(request, handler)
            afterHandlers.forEach { it(request, response) }
            response
        }
        registry.register(method, fullPattern, wrappedHandler)
    }

    fun findRoute(request: HttpRequest) = registry.find(request.method, request.path)

    suspend fun handle(request: HttpRequest): HttpResponse {
        val match = findRoute(request) ?: return HttpResponse.notFound()
        val (entry, params) = match
        val requestWithParams = request.copy(pathParams = params)
        return try { entry.handler(requestWithParams) }
        catch (e: Exception) { HttpResponse.internalError(e.message ?: "Internal server error") }
    }

    fun before(handler: suspend (HttpRequest) -> Unit) { beforeHandlers.add(handler) }
    fun after(handler: suspend (HttpRequest, HttpResponse) -> Unit) { afterHandlers.add(handler) }

    fun group(prefix: String, middleware: List<Middleware> = emptyList(), block: RouterImpl.() -> Unit) {
        val groupPrefix = if (this.prefix.isNotEmpty()) "${this.prefix}$prefix" else prefix
        val groupRouter = RouterImpl(groupPrefix, this.middlewares + middleware)
        groupRouter.block()
        groupRouter.registry.entries().forEach { entry ->
            registry.register(entry.method, entry.pattern, entry.handler)
        }
    }

    fun get(pattern: String, middleware: List<Middleware> = emptyList(), handler: RouteHandler) = route(HttpMethod.GET, pattern, middleware, handler)
    fun post(pattern: String, middleware: List<Middleware> = emptyList(), handler: RouteHandler) = route(HttpMethod.POST, pattern, middleware, handler)
    fun put(pattern: String, middleware: List<Middleware> = emptyList(), handler: RouteHandler) = route(HttpMethod.PUT, pattern, middleware, handler)
    fun delete(pattern: String, middleware: List<Middleware> = emptyList(), handler: RouteHandler) = route(HttpMethod.DELETE, pattern, middleware, handler)
    fun patch(pattern: String, middleware: List<Middleware> = emptyList(), handler: RouteHandler) = route(HttpMethod.PATCH, pattern, middleware, handler)
    fun options(pattern: String, middleware: List<Middleware> = emptyList(), handler: RouteHandler) = route(HttpMethod.OPTIONS, pattern, middleware, handler)
    fun head(pattern: String, middleware: List<Middleware> = emptyList(), handler: RouteHandler) = route(HttpMethod.HEAD, pattern, middleware, handler)

    fun routes(): List<RouteEntry> = registry.entries()
    fun routesCount(): Int = registry.size()

    companion object {
        fun withMiddleware(vararg middlewares: Middleware) = RouterImpl(middlewares = middlewares.toList())
    }
}

fun routerImpl(block: RouterImpl.() -> Unit) = RouterImpl().apply(block)
