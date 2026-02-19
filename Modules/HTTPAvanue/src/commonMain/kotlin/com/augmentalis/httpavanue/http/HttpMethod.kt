package com.augmentalis.httpavanue.http

/**
 * HTTP methods (RFC 7231)
 */
enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD, CONNECT, TRACE;

    companion object {
        fun from(value: String) =
            entries.firstOrNull { it.name == value.uppercase() }
                ?: throw IllegalArgumentException("Invalid HTTP method: $value")
    }
}
