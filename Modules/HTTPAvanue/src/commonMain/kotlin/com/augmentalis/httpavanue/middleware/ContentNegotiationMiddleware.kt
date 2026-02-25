package com.augmentalis.httpavanue.middleware

import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse
import com.augmentalis.httpavanue.http.HttpStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Content negotiation middleware — parses the Accept header and ensures
 * the response Content-Type matches what the client requested.
 *
 * Currently supports JSON negotiation. Responds with 406 Not Acceptable
 * when the client explicitly rejects all supported types.
 */
data class ContentNegotiationConfig(
    val supportedTypes: List<String> = listOf("application/json", "text/plain", "text/html"),
    val defaultType: String = "application/json",
)

fun contentNegotiationMiddleware(config: ContentNegotiationConfig = ContentNegotiationConfig()) = middleware { request, next ->
    val accept = request.header("Accept") ?: "*/*"
    val acceptedTypes = parseAcceptHeader(accept)

    // Check if any of our supported types match the client's Accept
    val matchedType = acceptedTypes.firstOrNull { accepted ->
        accepted == "*/*" || accepted == "*" ||
            config.supportedTypes.any { supported ->
                accepted == supported || matchesMimeType(accepted, supported)
            }
    }

    if (matchedType == null && acceptedTypes.isNotEmpty() && "*/*" !in acceptedTypes) {
        return@middleware HttpResponse(
            status = HttpStatus.NOT_ACCEPTABLE.code,
            statusMessage = HttpStatus.NOT_ACCEPTABLE.message,
            headers = mapOf("Content-Type" to "application/json"),
            body = """{"error":"Not Acceptable","supported":${config.supportedTypes}}""".encodeToByteArray(),
        )
    }

    val response = next(request)

    // Set Content-Type if not already set
    if ("Content-Type" !in response.headers) {
        val selectedType = when {
            matchedType != null && matchedType != "*/*" && matchedType != "*" -> matchedType
            else -> config.defaultType
        }
        response.copy(
            headers = response.headers + ("Content-Type" to selectedType),
        )
    } else {
        response
    }
}

/** Typed JSON response factory — serializes value to JSON and sets Content-Type. */
inline fun <reified T> HttpResponse.Companion.jsonTyped(value: T, json: Json = defaultJson): HttpResponse {
    val body = json.encodeToString(value).encodeToByteArray()
    return HttpResponse(
        status = HttpStatus.OK.code,
        statusMessage = HttpStatus.OK.message,
        headers = mapOf("Content-Type" to "application/json"),
        body = body,
    )
}

@PublishedApi internal val defaultJson = Json { ignoreUnknownKeys = true }

private fun parseAcceptHeader(accept: String): List<String> {
    return accept.split(',')
        .map { part ->
            val typeAndParams = part.trim().split(';')
            val type = typeAndParams[0].trim()
            val quality = typeAndParams
                .firstOrNull { it.trim().startsWith("q=") }
                ?.substringAfter("q=")?.trim()?.toFloatOrNull() ?: 1.0f
            type to quality
        }
        .sortedByDescending { it.second }
        .map { it.first }
}

private fun matchesMimeType(pattern: String, target: String): Boolean {
    val (patternType, patternSub) = pattern.split('/').let {
        if (it.size == 2) it[0] to it[1] else return false
    }
    val (targetType, targetSub) = target.split('/').let {
        if (it.size == 2) it[0] to it[1] else return false
    }
    return (patternType == "*" || patternType == targetType) &&
        (patternSub == "*" || patternSub == targetSub)
}
