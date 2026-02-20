package com.augmentalis.httpavanue.http

import com.augmentalis.httpavanue.client.ClientRequest
import com.augmentalis.httpavanue.client.ClientResponse

/**
 * HTTP client interface for making outbound HTTP requests
 */
interface HttpClient {
    suspend fun request(request: ClientRequest): ClientResponse
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): ClientResponse
    suspend fun post(url: String, body: ByteArray? = null, headers: Map<String, String> = emptyMap()): ClientResponse
    suspend fun put(url: String, body: ByteArray? = null, headers: Map<String, String> = emptyMap()): ClientResponse
    suspend fun delete(url: String, headers: Map<String, String> = emptyMap()): ClientResponse
    fun close()
}
