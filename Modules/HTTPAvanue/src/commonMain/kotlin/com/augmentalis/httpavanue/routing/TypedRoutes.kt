package com.augmentalis.httpavanue.routing

import com.augmentalis.httpavanue.http.HttpResponse
import com.augmentalis.httpavanue.http.HttpStatus
import com.augmentalis.httpavanue.middleware.parseJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Typed Route DSL — auto JSON serialize/deserialize for route handlers.
 *
 * Extension functions on RouterImpl that handle JSON body parsing and
 * response serialization, so handlers work with Kotlin types instead
 * of raw ByteArrays and strings.
 */

@PublishedApi internal val typedJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

inline fun <reified Req, reified Resp> RouterImpl.postTyped(
    pattern: String,
    crossinline handler: suspend (com.augmentalis.httpavanue.http.HttpRequest, Req) -> Resp,
) {
    post(pattern) { request ->
        val body = request.parseJson<Req>(typedJson)
            ?: return@post HttpResponse(
                status = HttpStatus.BAD_REQUEST.code,
                statusMessage = HttpStatus.BAD_REQUEST.message,
                headers = mapOf("Content-Type" to "application/json"),
                body = """{"error":"Invalid or missing JSON body"}""".encodeToByteArray(),
            )
        val result = handler(request, body)
        HttpResponse(
            status = HttpStatus.OK.code,
            statusMessage = HttpStatus.OK.message,
            headers = mapOf("Content-Type" to "application/json"),
            body = typedJson.encodeToString(result).encodeToByteArray(),
        )
    }
}

inline fun <reified Resp> RouterImpl.getTyped(
    pattern: String,
    crossinline handler: suspend (com.augmentalis.httpavanue.http.HttpRequest) -> Resp,
) {
    get(pattern) { request ->
        val result = handler(request)
        HttpResponse(
            status = HttpStatus.OK.code,
            statusMessage = HttpStatus.OK.message,
            headers = mapOf("Content-Type" to "application/json"),
            body = typedJson.encodeToString(result).encodeToByteArray(),
        )
    }
}

inline fun <reified Req, reified Resp> RouterImpl.putTyped(
    pattern: String,
    crossinline handler: suspend (com.augmentalis.httpavanue.http.HttpRequest, Req) -> Resp,
) {
    put(pattern) { request ->
        val body = request.parseJson<Req>(typedJson)
            ?: return@put HttpResponse(
                status = HttpStatus.BAD_REQUEST.code,
                statusMessage = HttpStatus.BAD_REQUEST.message,
                headers = mapOf("Content-Type" to "application/json"),
                body = """{"error":"Invalid or missing JSON body"}""".encodeToByteArray(),
            )
        val result = handler(request, body)
        HttpResponse(
            status = HttpStatus.OK.code,
            statusMessage = HttpStatus.OK.message,
            headers = mapOf("Content-Type" to "application/json"),
            body = typedJson.encodeToString(result).encodeToByteArray(),
        )
    }
}

inline fun <reified Resp> RouterImpl.deleteTyped(
    pattern: String,
    crossinline handler: suspend (com.augmentalis.httpavanue.http.HttpRequest) -> Resp,
) {
    delete(pattern) { request ->
        val result = handler(request)
        HttpResponse(
            status = HttpStatus.OK.code,
            statusMessage = HttpStatus.OK.message,
            headers = mapOf("Content-Type" to "application/json"),
            body = typedJson.encodeToString(result).encodeToByteArray(),
        )
    }
}

inline fun <reified Req, reified Resp> RouterImpl.patchTyped(
    pattern: String,
    crossinline handler: suspend (com.augmentalis.httpavanue.http.HttpRequest, Req) -> Resp,
) {
    patch(pattern) { request ->
        val body = request.parseJson<Req>(typedJson)
            ?: return@patch HttpResponse(
                status = HttpStatus.BAD_REQUEST.code,
                statusMessage = HttpStatus.BAD_REQUEST.message,
                headers = mapOf("Content-Type" to "application/json"),
                body = """{"error":"Invalid or missing JSON body"}""".encodeToByteArray(),
            )
        val result = handler(request, body)
        HttpResponse(
            status = HttpStatus.OK.code,
            statusMessage = HttpStatus.OK.message,
            headers = mapOf("Content-Type" to "application/json"),
            body = typedJson.encodeToString(result).encodeToByteArray(),
        )
    }
}
