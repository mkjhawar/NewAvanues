package com.augmentalis.httpavanue.middleware

import com.augmentalis.httpavanue.auth.AuthenticationManager
import com.augmentalis.httpavanue.http.HttpResponse
import com.augmentalis.httpavanue.http.HttpStatus

fun authenticationMiddleware(
    authManager: AuthenticationManager,
    requireAuth: Boolean = true,
    excludedPaths: Set<String> = setOf("/health", "/login", "/register"),
): Middleware = middleware { request, next ->
    if (request.path in excludedPaths) return@middleware next(request)
    val authHeader = request.headers["Authorization"]
    if (authHeader == null) {
        if (requireAuth) HttpResponse.error(HttpStatus.UNAUTHORIZED, "Missing Authorization header")
        else next(request)
    } else if (!authHeader.startsWith("Bearer ")) {
        HttpResponse.error(HttpStatus.UNAUTHORIZED, "Invalid Authorization header format")
    } else {
        val token = authHeader.removePrefix("Bearer ").trim()
        if (token.isEmpty()) HttpResponse.error(HttpStatus.UNAUTHORIZED, "Missing authentication token")
        else {
            val validation = authManager.validateToken(token)
            if (validation.valid) {
                val contextUpdates = mutableMapOf<String, String>()
                contextUpdates["deviceId"] = validation.deviceId ?: "unknown"
                validation.capabilities?.let { contextUpdates["capabilities"] = it.joinToString(",") }
                next(request.copy(context = request.context + contextUpdates))
            } else HttpResponse.error(HttpStatus.FORBIDDEN, validation.error ?: "Invalid or expired token")
        }
    }
}

fun optionalAuthenticationMiddleware(authManager: AuthenticationManager, excludedPaths: Set<String> = emptySet()) =
    authenticationMiddleware(authManager, requireAuth = false, excludedPaths = excludedPaths)
