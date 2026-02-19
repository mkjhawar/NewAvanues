package com.augmentalis.httpavanue.middleware

import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse

/**
 * Middleware for HTTP request/response processing (Chain of Responsibility)
 */
interface Middleware {
    suspend fun handle(
        request: HttpRequest,
        next: suspend (HttpRequest) -> HttpResponse,
    ): HttpResponse
}

/**
 * Middleware pipeline for chaining multiple middlewares
 */
class MiddlewarePipeline(
    val middlewares: List<Middleware> = emptyList(),
) {
    suspend fun execute(
        request: HttpRequest,
        terminal: suspend (HttpRequest) -> HttpResponse,
    ): HttpResponse {
        if (middlewares.isEmpty()) return terminal(request)
        var handler = terminal
        middlewares.asReversed().forEach { middleware ->
            val nextHandler = handler
            handler = { req -> middleware.handle(req, nextHandler) }
        }
        return handler(request)
    }

    fun add(middleware: Middleware) = MiddlewarePipeline(middlewares + middleware)
    fun addAll(vararg middleware: Middleware) = MiddlewarePipeline(middlewares + middleware)

    companion object {
        fun empty() = MiddlewarePipeline()
        fun of(vararg middlewares: Middleware) = MiddlewarePipeline(middlewares.toList())
    }
}

/**
 * DSL for creating middleware
 */
fun middleware(block: suspend (HttpRequest, suspend (HttpRequest) -> HttpResponse) -> HttpResponse) =
    object : Middleware {
        override suspend fun handle(request: HttpRequest, next: suspend (HttpRequest) -> HttpResponse) =
            block(request, next)
    }
