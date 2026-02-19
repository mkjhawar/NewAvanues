package com.augmentalis.httpavanue.middleware

import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse
import com.augmentalis.httpavanue.platform.readResource

class StaticFileMiddleware(
    private val urlPrefix: String,
    private val resourcePath: String,
    private val defaultFile: String = "index.html",
) : Middleware {
    override suspend fun handle(request: HttpRequest, next: suspend (HttpRequest) -> HttpResponse): HttpResponse {
        if (!request.path.startsWith(urlPrefix)) return next(request)
        var filePath = request.path.removePrefix(urlPrefix).trimStart('/')
        if (filePath.isEmpty() || filePath.endsWith("/")) filePath += defaultFile
        val fullPath = if (resourcePath.isEmpty()) filePath else "$resourcePath/$filePath"
        val content = readResource(fullPath) ?: return next(request)
        return HttpResponse(status = 200, statusMessage = "OK", headers = mapOf("Content-Type" to getContentType(filePath)), body = content.encodeToByteArray())
    }

    private fun getContentType(path: String) = when (path.substringAfterLast('.', "")) {
        "html", "htm" -> "text/html; charset=utf-8"; "css" -> "text/css; charset=utf-8"
        "js" -> "application/javascript; charset=utf-8"; "json" -> "application/json; charset=utf-8"
        "xml" -> "application/xml; charset=utf-8"; "txt" -> "text/plain; charset=utf-8"
        "png" -> "image/png"; "jpg", "jpeg" -> "image/jpeg"; "gif" -> "image/gif"
        "svg" -> "image/svg+xml"; "ico" -> "image/x-icon"; "webp" -> "image/webp"
        "woff" -> "font/woff"; "woff2" -> "font/woff2"; "ttf" -> "font/ttf"
        "mp4" -> "video/mp4"; "webm" -> "video/webm"; "mp3" -> "audio/mpeg"
        else -> "application/octet-stream"
    }
}

fun staticFileMiddleware(urlPrefix: String = "/", resourcePath: String = "", defaultFile: String = "index.html") =
    StaticFileMiddleware(urlPrefix, resourcePath, defaultFile)
