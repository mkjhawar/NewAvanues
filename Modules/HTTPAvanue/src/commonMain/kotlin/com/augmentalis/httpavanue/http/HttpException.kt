package com.augmentalis.httpavanue.http

/**
 * Base exception for HTTP-related errors
 */
open class HttpException(
    val statusCode: Int,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    companion object {
        fun badRequest(message: String = "Bad Request", cause: Throwable? = null) =
            HttpException(400, message, cause)
        fun unauthorized(message: String = "Unauthorized", cause: Throwable? = null) =
            HttpException(401, message, cause)
        fun forbidden(message: String = "Forbidden", cause: Throwable? = null) =
            HttpException(403, message, cause)
        fun notFound(message: String = "Not Found", cause: Throwable? = null) =
            HttpException(404, message, cause)
        fun conflict(message: String = "Conflict", cause: Throwable? = null) =
            HttpException(409, message, cause)
        fun serverError(message: String = "Internal Server Error", cause: Throwable? = null) =
            HttpException(500, message, cause)
        fun serviceUnavailable(message: String = "Service Unavailable", cause: Throwable? = null) =
            HttpException(503, message, cause)
    }
}

/**
 * Exception thrown when request body exceeds maximum allowed size
 */
class PayloadTooLargeException(message: String) : HttpException(413, message)

/**
 * Exception thrown when HTTP client operations fail
 */
class HttpClientException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
