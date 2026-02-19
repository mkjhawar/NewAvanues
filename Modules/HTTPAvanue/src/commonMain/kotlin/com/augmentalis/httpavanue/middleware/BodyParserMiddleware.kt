package com.augmentalis.httpavanue.middleware

import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse
import kotlinx.serialization.json.Json

class BodyParserMiddleware(private val json: Json = Json { ignoreUnknownKeys = true }) : Middleware {
    override suspend fun handle(request: HttpRequest, next: suspend (HttpRequest) -> HttpResponse) = next(request)
}

inline fun <reified T> HttpRequest.parseJson(json: Json = Json { ignoreUnknownKeys = true }): T? {
    val bodyString = body?.decodeToString() ?: return null
    return try { json.decodeFromString<T>(bodyString) } catch (_: Exception) { null }
}

fun HttpRequest.bodyAsText(): String? = body?.decodeToString()
fun bodyParser(json: Json = Json { ignoreUnknownKeys = true }) = BodyParserMiddleware(json)
