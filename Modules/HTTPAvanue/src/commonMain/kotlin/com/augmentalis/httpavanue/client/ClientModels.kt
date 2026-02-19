package com.augmentalis.httpavanue.client

import com.augmentalis.httpavanue.core.serialization.ByteArraySerializer
import com.augmentalis.httpavanue.http.HttpMethod
import com.augmentalis.httpavanue.platform.currentTimeMillis
import kotlinx.serialization.Serializable

@Serializable
data class ClientRequest(
    val method: HttpMethod,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    @Serializable(with = ByteArraySerializer::class)
    val body: ByteArray? = null,
    val timeout: Long = 30000,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClientRequest) return false
        return method == other.method && url == other.url && headers == other.headers &&
            (body?.contentEquals(other.body) != false) && timeout == other.timeout
    }
    override fun hashCode(): Int {
        var result = method.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        result = 31 * result + timeout.hashCode()
        return result
    }
}

@Serializable
data class ClientResponse(
    val status: Int,
    val headers: Map<String, String>,
    @Serializable(with = ByteArraySerializer::class)
    val body: ByteArray?,
    val requestTime: Long = currentTimeMillis(),
    val requestDuration: Long = 0,
) {
    val isSuccess: Boolean get() = status in 200..299
    val isRedirect: Boolean get() = status in 300..399
    val isClientError: Boolean get() = status in 400..499
    val isServerError: Boolean get() = status in 500..599

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClientResponse) return false
        return status == other.status && headers == other.headers &&
            (body?.contentEquals(other.body) != false) &&
            requestTime == other.requestTime && requestDuration == other.requestDuration
    }
    override fun hashCode(): Int {
        var result = status
        result = 31 * result + headers.hashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        result = 31 * result + requestTime.hashCode()
        result = 31 * result + requestDuration.hashCode()
        return result
    }
}
