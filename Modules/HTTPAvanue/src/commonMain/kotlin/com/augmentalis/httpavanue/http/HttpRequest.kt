package com.augmentalis.httpavanue.http

/**
 * Immutable HTTP request representation.
 * Not serializable — this is a server-internal pipeline type (middleware → router → handler).
 * For client-side serializable request/response types, see ClientModels.kt.
 */
data class HttpRequest(
    val method: HttpMethod,
    val uri: String,
    val version: String = "HTTP/1.1",
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
    val queryParams: Map<String, List<String>> = emptyMap(),
    val pathParams: Map<String, String> = emptyMap(),
    val context: Map<String, String> = emptyMap(),
) {
    val path: String by lazy { uri.substringBefore('?').substringBefore('#') }
    val contentLength: Long by lazy { headers["Content-Length"]?.toLongOrNull() ?: body?.size?.toLong() ?: 0L }
    val contentType: String? by lazy { headers["Content-Type"] }
    val isKeepAlive: Boolean by lazy { headers["Connection"]?.lowercase() != "close" }
    val bodyString: String? by lazy { body?.decodeToString() }

    fun header(name: String) = headers[name]
    fun queryParam(name: String) = queryParams[name]?.firstOrNull()
    fun queryParams(name: String) = queryParams[name] ?: emptyList()
    fun pathParam(name: String) = pathParams[name]

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is HttpRequest -> false
        else -> method == other.method && uri == other.uri && version == other.version &&
            headers == other.headers && body?.contentEquals(other.body) != false &&
            queryParams == other.queryParams && pathParams == other.pathParams && context == other.context
    }

    override fun hashCode(): Int {
        var result = method.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        result = 31 * result + queryParams.hashCode()
        result = 31 * result + pathParams.hashCode()
        result = 31 * result + context.hashCode()
        return result
    }
}
