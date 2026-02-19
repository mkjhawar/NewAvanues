package com.augmentalis.httpavanue.middleware

import com.augmentalis.httpavanue.http.*

data class CorsConfig(
    val allowedOrigins: Set<String> = setOf("*"),
    val allowedMethods: Set<HttpMethod> = setOf(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.OPTIONS),
    val allowedHeaders: Set<String> = setOf("Content-Type", "Authorization", "X-Requested-With"),
    val exposedHeaders: Set<String> = emptySet(),
    val allowCredentials: Boolean = false,
    val maxAge: Long = 86400,
)

class CorsMiddleware(private val config: CorsConfig = CorsConfig()) : Middleware {
    override suspend fun handle(request: HttpRequest, next: suspend (HttpRequest) -> HttpResponse): HttpResponse {
        val origin = request.header("Origin")
        if (request.method == HttpMethod.OPTIONS) return createPreflightResponse(origin)
        return addCorsHeaders(next(request), origin)
    }

    private fun createPreflightResponse(origin: String?): HttpResponse {
        val headers = mutableMapOf<String, String>()
        if (origin != null && isOriginAllowed(origin)) {
            headers["Access-Control-Allow-Origin"] = origin
            headers["Access-Control-Allow-Methods"] = config.allowedMethods.joinToString { it.name }
            headers["Access-Control-Allow-Headers"] = config.allowedHeaders.joinToString()
            headers["Access-Control-Max-Age"] = config.maxAge.toString()
            if (config.allowCredentials) headers["Access-Control-Allow-Credentials"] = "true"
        }
        return HttpResponse(status = HttpStatus.NO_CONTENT.code, statusMessage = HttpStatus.NO_CONTENT.message, headers = headers)
    }

    private fun addCorsHeaders(response: HttpResponse, origin: String?): HttpResponse {
        if (origin == null || !isOriginAllowed(origin)) return response
        val corsHeaders = mutableMapOf("Access-Control-Allow-Origin" to origin)
        if (config.exposedHeaders.isNotEmpty()) corsHeaders["Access-Control-Expose-Headers"] = config.exposedHeaders.joinToString()
        if (config.allowCredentials) corsHeaders["Access-Control-Allow-Credentials"] = "true"
        return response.copy(headers = response.headers + corsHeaders)
    }

    private fun isOriginAllowed(origin: String) = config.allowedOrigins.contains("*") || config.allowedOrigins.contains(origin)
}

fun corsMiddleware(config: CorsConfig = CorsConfig()) = CorsMiddleware(config)
fun permissiveCors() = CorsMiddleware(CorsConfig(allowedOrigins = setOf("*")))
