package com.augmentalis.httpavanue.middleware

import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse

/**
 * Forwarded Headers middleware — normalizes proxy headers into request context.
 * Reads X-Forwarded-For, X-Forwarded-Proto, X-Forwarded-Host, X-Real-IP
 * and writes canonical values into request.context for downstream handlers.
 *
 * Only trusts proxy headers when the remote address is in [trustedProxies].
 */
data class ForwardedHeadersConfig(
    val trustedProxies: Set<String> = setOf("127.0.0.1", "::1"),
)

fun forwardedHeadersMiddleware(config: ForwardedHeadersConfig = ForwardedHeadersConfig()) = middleware { request, next ->
    val remoteIp = request.remoteAddress?.substringBefore(':') ?: ""
    val isTrusted = remoteIp in config.trustedProxies

    val updatedContext = request.context.toMutableMap()
    if (isTrusted) {
        request.header("X-Forwarded-For")?.let { forwarded ->
            updatedContext["remote_address"] = forwarded.split(',').first().trim()
        }
        request.header("X-Real-IP")?.let { realIp ->
            if ("remote_address" !in updatedContext) updatedContext["remote_address"] = realIp.trim()
        }
        request.header("X-Forwarded-Proto")?.let { proto ->
            updatedContext["scheme"] = proto.trim().lowercase()
        }
        request.header("X-Forwarded-Host")?.let { host ->
            updatedContext["host"] = host.trim()
        }
    }

    next(request.copy(context = updatedContext))
}
