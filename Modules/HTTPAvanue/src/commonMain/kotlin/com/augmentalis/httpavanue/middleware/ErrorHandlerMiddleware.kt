package com.augmentalis.httpavanue.middleware

import com.avanues.logging.Logger
import com.avanues.logging.LoggerFactory
import com.augmentalis.httpavanue.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ErrorResponse(val error: String, val message: String, val status: Int, val path: String? = null)

class ErrorHandlerMiddleware(
    private val logger: Logger = LoggerFactory.getLogger("ErrorHandler"),
    private val includeStackTrace: Boolean = false,
) : Middleware {
    override suspend fun handle(request: HttpRequest, next: suspend (HttpRequest) -> HttpResponse) = try {
        next(request)
    } catch (e: HttpException) {
        logger.w { "HTTP error (${e.statusCode}): ${e.message}" }
        createErrorResponse(HttpStatus.from(e.statusCode), e.message ?: "Bad request", request.uri)
    } catch (e: IllegalArgumentException) {
        logger.w { "Invalid argument: ${e.message}" }
        createErrorResponse(HttpStatus.BAD_REQUEST, e.message ?: "Invalid argument", request.uri)
    } catch (e: Exception) {
        logger.e({ "Unhandled error processing ${request.method} ${request.uri}" }, e)
        createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, if (includeStackTrace) e.stackTraceToString() else "Internal server error", request.uri)
    }

    private fun createErrorResponse(status: HttpStatus, message: String, path: String?): HttpResponse {
        val json = Json.encodeToString(ErrorResponse.serializer(), ErrorResponse(status.message, message, status.code, path))
        return HttpResponse(status.code, status.message, headers = mapOf("Content-Type" to "application/json; charset=UTF-8", "Content-Length" to json.encodeToByteArray().size.toString()), body = json.encodeToByteArray())
    }
}

fun errorHandler(includeStackTrace: Boolean = false) = ErrorHandlerMiddleware(includeStackTrace = includeStackTrace)
