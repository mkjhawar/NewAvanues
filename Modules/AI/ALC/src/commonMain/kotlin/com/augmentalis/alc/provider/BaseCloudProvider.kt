package com.augmentalis.alc.provider

import com.augmentalis.alc.domain.*
import com.augmentalis.alc.engine.ILLMProvider
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlin.time.TimeSource
import kotlinx.datetime.Clock

/**
 * Base class for cloud LLM providers
 *
 * Provides common HTTP client setup and error handling.
 */
abstract class BaseCloudProvider(
    protected val config: ProviderConfig
) : ILLMProvider {

    protected val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    protected val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeout
            connectTimeoutMillis = 10000
        }
    }

    protected abstract val baseUrl: String
    protected abstract val authHeader: Pair<String, String>

    override suspend fun complete(
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): LLMResponse.Complete {
        val mark = TimeSource.Monotonic.markNow()
        val builder = StringBuilder()
        var usage = TokenUsage(0, 0)

        chat(messages, options.copy(stream = false)).collect { response ->
            when (response) {
                is LLMResponse.Streaming -> builder.append(response.chunk)
                is LLMResponse.Complete -> {
                    usage = response.usage
                }
                is LLMResponse.Error -> throw Exception(response.message)
            }
        }

        return LLMResponse.Complete(
            fullText = builder.toString(),
            usage = usage,
            model = config.model,
            latencyMs = mark.elapsedNow().inWholeMilliseconds
        )
    }

    override suspend fun healthCheck(): ProviderHealth {
        return try {
            val mark = TimeSource.Monotonic.markNow()
            val models = getModels()
            val latency = mark.elapsedNow().inWholeMilliseconds

            ProviderHealth(
                provider = providerType,
                status = if (models.isNotEmpty()) HealthStatus.HEALTHY else HealthStatus.DEGRADED,
                latencyMs = latency,
                lastCheck = currentTimeMillis()
            )
        } catch (e: Exception) {
            ProviderHealth(
                provider = providerType,
                status = HealthStatus.UNHEALTHY,
                lastCheck = currentTimeMillis(),
                errorMessage = e.message
            )
        }
    }

    // Platform-agnostic current time (for timestamps)
    private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

    protected suspend fun streamRequest(
        endpoint: String,
        body: Any,
        parseChunk: (String) -> LLMResponse?
    ): Flow<LLMResponse> = flow {
        try {
            client.preparePost("$baseUrl$endpoint") {
                contentType(ContentType.Application.Json)
                header(authHeader.first, authHeader.second)
                setBody(body)
            }.execute { response ->
                val channel: ByteReadChannel = response.bodyAsChannel()
                val buffer = StringBuilder()

                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break

                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") break

                        parseChunk(data)?.let { emit(it) }
                    }
                }
            }
        } catch (e: Exception) {
            emit(LLMResponse.Error(
                message = e.message ?: "Unknown error",
                code = "STREAM_ERROR",
                retryable = true
            ))
        }
    }
}
