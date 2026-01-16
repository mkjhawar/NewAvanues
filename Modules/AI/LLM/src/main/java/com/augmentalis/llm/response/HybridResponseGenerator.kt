package com.augmentalis.llm.response

import android.content.Context
import com.augmentalis.llm.provider.LocalLLMProvider
import com.augmentalis.llm.domain.LLMConfig
import com.augmentalis.llm.inference.InferenceManager
import com.augmentalis.nlu.IntentClassification
import com.augmentalis.ava.core.common.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withTimeout
import timber.log.Timber

/**
 * Hybrid response generator with automatic fallback
 *
 * Strategy (Fallback Chain):
 * 1. Try Local LLM first (privacy-first, offline)
 * 2. Fall back to Cloud LLM if:
 *    - Local LLM not initialized
 *    - Local LLM generation fails
 *    - Local LLM times out
 * 3. Fall back to template if:
 *    - All LLMs fail
 *    - Cost limits exceeded
 *    - Battery/thermal constraints (InferenceManager)
 * 4. Track success rate for adaptive switching
 *
 * Benefits:
 * - Best of both worlds: Local when possible, cloud when needed, templates always
 * - Graceful degradation: User never sees "failed" state
 * - Performance monitoring: Tracks which generator is used
 * - Cost-aware: Respects spending limits for cloud providers
 *
 * Usage:
 * ```kotlin
 * val generator = HybridResponseGenerator(
 *     context,
 *     localLLMProvider,
 *     cloudLLMProvider,
 *     inferenceManager
 * )
 * generator.initialize(config) // Optional, enables LLMs
 *
 * // Always works, uses best available generator
 * generator.generateResponse(message, classification)
 * ```
 *
 * Created: 2025-11-10
 * Updated: 2025-12-05 - Added cloud LLM fallback support
 * Author: AVA AI Team
 */
class HybridResponseGenerator(
    private val context: Context,
    private val llmProvider: LocalLLMProvider,
    private val cloudLLMProvider: com.augmentalis.llm.provider.CloudLLMProvider? = null,
    private val inferenceManager: InferenceManager? = null
) : ResponseGenerator {

    companion object {
        private const val TAG = "HybridResponseGenerator"

        /**
         * Timeout for LLM generation (ms)
         *
         * Issue V-01 Fix: Increased from 2000ms to 30000ms.
         *
         * Rationale:
         * - 2s was too short for on-device LLM inference
         * - First-token latency on mobile can be 1-3s alone
         * - Full response generation: 5-20s depending on model size
         * - 30s allows complete responses while still providing a safety net
         *
         * Device-specific considerations:
         * - High-end (8GB+ RAM): Usually responds in 3-8s
         * - Mid-range (4-6GB RAM): Usually responds in 5-15s
         * - Low-end (2-4GB RAM): May take 10-25s
         *
         * If timeout is hit, falls back to template (instant response).
         */
        private const val LLM_TIMEOUT_MS = 30_000L

        /**
         * Confidence threshold for template vs LLM decision
         * Above this threshold: Use templates (fast, battery-efficient for known intents)
         * Below this threshold: Use LLM (flexible, handles unknown queries)
         * This optimizes battery/speed while maintaining flexibility for unknowns
         */
        private const val TEMPLATE_CONFIDENCE_THRESHOLD = 0.6f
    }

    private val templateGenerator = TemplateResponseGenerator()
    private val llmGenerator = LLMResponseGenerator(context, llmProvider)

    // Metrics
    private var localLLMSuccessCount = 0
    private var localLLMFailureCount = 0
    private var cloudLLMSuccessCount = 0
    private var cloudLLMFailureCount = 0
    private var templateFallbackCount = 0

    // Latency tracking for analytics (H-07)
    private val localLLMLatencies = mutableListOf<Long>()
    private val cloudLLMLatencies = mutableListOf<Long>()
    private val templateLatencies = mutableListOf<Long>()
    private var totalLatency = 0L
    private var totalRequests = 0

    /**
     * Initialize LLM generator (optional)
     *
     * If not called, hybrid generator uses templates only.
     * If initialization fails, falls back to templates automatically.
     *
     * @param config LLM configuration
     * @return Result indicating LLM init status (templates always work)
     */
    suspend fun initialize(config: LLMConfig): Result<Unit> {
        return try {
            val result = llmGenerator.initialize(config)
            when (result) {
                is Result.Success -> {
                    Timber.i("Hybrid generator: LLM initialized, using LLM+template mode")
                }
                is Result.Error -> {
                    Timber.w("Hybrid generator: LLM init failed, using template-only mode")
                }
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Hybrid generator: LLM init exception, using template-only mode")
            Result.Error(e, "LLM initialization failed")
        }
    }

    /**
     * Generate response with automatic fallback chain
     *
     * Logic (Fallback Chain):
     * 1. Check if Local LLM is ready and should be used
     * 2. If yes: Try Local LLM with timeout
     * 3. If Local LLM fails: Try Cloud LLM (if configured)
     * 4. If Cloud LLM fails: Fall back to template
     * 5. Always return a response (never fails)
     *
     * @param userMessage Original user utterance
     * @param classification NLU classification result
     * @param context Additional context
     * @return Flow of response chunks (guaranteed to succeed)
     */
    override suspend fun generateResponse(
        userMessage: String,
        classification: IntentClassification,
        context: ResponseContext
    ): Flow<ResponseChunk> = flow {
        val startTime = System.currentTimeMillis()

        // Decide which generator to use
        val useLocalLLM = shouldUseLocalLLM(classification)
        val useCloudLLM = shouldUseCloudLLM(classification)

        // Try Local LLM first
        if (useLocalLLM) {
            Timber.d("Hybrid: Attempting Local LLM generation (confidence: ${classification.confidence})")

            try {
                var llmError: Throwable? = null
                var hasEmitted = false

                withTimeout(LLM_TIMEOUT_MS) {
                    llmGenerator.generateResponse(userMessage, classification, context)
                        .catch { e ->
                            Timber.w(e, "Hybrid: Local LLM generation failed")
                            llmError = e
                        }
                        .collect { chunk ->
                            when (chunk) {
                                is ResponseChunk.Error -> {
                                    llmError = chunk.exception
                                }
                                else -> {
                                    emit(chunk)
                                    hasEmitted = true
                                }
                            }
                        }
                }

                // If Local LLM succeeded
                if (hasEmitted && llmError == null) {
                    localLLMSuccessCount++
                    val latency = System.currentTimeMillis() - startTime
                    trackLatency(latency, "local_llm")
                    Timber.i("Hybrid: Local LLM generation succeeded in ${latency}ms")
                    return@flow
                } else {
                    localLLMFailureCount++
                }

            } catch (e: Exception) {
                Timber.w("Hybrid: Local LLM exception (${e.message})")
                localLLMFailureCount++
            }
        }

        // Try Cloud LLM fallback
        if (useCloudLLM && cloudLLMProvider != null) {
            Timber.d("Hybrid: Attempting Cloud LLM fallback")

            try {
                var cloudError: Throwable? = null
                var hasEmitted = false

                // Convert ResponseChunk to LLMResponse flow
                val messages = listOf(
                    com.augmentalis.llm.domain.ChatMessage(
                        role = com.augmentalis.llm.domain.MessageRole.USER,
                        content = userMessage
                    )
                )

                cloudLLMProvider.chat(messages, com.augmentalis.llm.domain.GenerationOptions())
                    .catch { e ->
                        Timber.w(e, "Hybrid: Cloud LLM generation failed")
                        cloudError = e
                    }
                    .collect { llmResponse ->
                        when (llmResponse) {
                            is com.augmentalis.llm.domain.LLMResponse.Streaming -> {
                                emit(ResponseChunk.Text(llmResponse.chunk))
                                hasEmitted = true
                            }
                            is com.augmentalis.llm.domain.LLMResponse.Complete -> {
                                emit(ResponseChunk.Complete(
                                    fullText = llmResponse.fullText,
                                    metadata = mapOf(
                                        "generator" to "cloud_llm",
                                        "tokens_input" to llmResponse.usage.promptTokens,
                                        "tokens_output" to llmResponse.usage.completionTokens
                                    )
                                ))
                                hasEmitted = true
                            }
                            is com.augmentalis.llm.domain.LLMResponse.Error -> {
                                cloudError = llmResponse.exception
                            }
                        }
                    }

                // If Cloud LLM succeeded
                if (hasEmitted && cloudError == null) {
                    cloudLLMSuccessCount++
                    val latency = System.currentTimeMillis() - startTime
                    trackLatency(latency, "cloud_llm")
                    Timber.i("Hybrid: Cloud LLM generation succeeded in ${latency}ms")
                    return@flow
                } else {
                    cloudLLMFailureCount++
                }

            } catch (e: Exception) {
                Timber.w("Hybrid: Cloud LLM exception (${e.message})")
                cloudLLMFailureCount++
            }
        }

        // Fall back to template (always succeeds)
        Timber.d("Hybrid: Using template fallback")
        templateFallbackCount++

        templateGenerator.generateResponse(userMessage, classification, context)
            .collect { chunk ->
                emit(chunk)
            }

        val latency = System.currentTimeMillis() - startTime
        trackLatency(latency, "template")
        Timber.i("Hybrid: Template response generated in ${latency}ms")
    }

    /**
     * Track latency for analytics (H-07)
     *
     * Stores latency metrics for each generator type to enable:
     * - Average latency calculation per generator
     * - Overall response time monitoring
     * - Performance trend analysis
     *
     * @param latency Response generation time in milliseconds
     * @param generatorTag Which generator was used ("local_llm", "cloud_llm", "template")
     */
    private fun trackLatency(latency: Long, generatorTag: String) {
        totalLatency += latency
        totalRequests++

        when (generatorTag) {
            "local_llm" -> localLLMLatencies.add(latency)
            "cloud_llm" -> cloudLLMLatencies.add(latency)
            "template" -> templateLatencies.add(latency)
        }

        // Keep only last 100 samples per generator to prevent memory growth
        if (localLLMLatencies.size > 100) localLLMLatencies.removeAt(0)
        if (cloudLLMLatencies.size > 100) cloudLLMLatencies.removeAt(0)
        if (templateLatencies.size > 100) templateLatencies.removeAt(0)
    }

    /**
     * Decide whether to use Local LLM
     *
     * Criteria (ADR-014 updated):
     * 1. Check InferenceManager for battery/thermal constraints
     * 2. Local LLM must be ready
     * 3. HIGH confidence (>= threshold): Use templates (fast, battery-efficient)
     * 4. LOW confidence (< threshold): Use LLM (flexible, handles unknown)
     * 5. Local LLM success rate should be acceptable (>50%)
     *
     * @param classification NLU classification
     * @return true if should use Local LLM, false otherwise
     */
    private fun shouldUseLocalLLM(classification: IntentClassification): Boolean {
        // ADR-014: Check InferenceManager for battery/thermal constraints
        if (inferenceManager != null) {
            val backend = inferenceManager.selectBackend()
            when (backend) {
                InferenceManager.InferenceBackend.QUEUED -> {
                    Timber.w("Hybrid: InferenceManager says QUEUED (thermal critical), using template")
                    return false
                }
                InferenceManager.InferenceBackend.NLU_ONLY -> {
                    Timber.w("Hybrid: InferenceManager says NLU_ONLY (battery critical), using template")
                    return false
                }
                InferenceManager.InferenceBackend.CLOUD_LLM -> {
                    // TODO: Future - implement cloud LLM provider
                    // For now, fall through to local LLM if available
                    Timber.i("Hybrid: InferenceManager prefers CLOUD_LLM, but using LOCAL_LLM for now")
                }
                InferenceManager.InferenceBackend.LOCAL_LLM -> {
                    Timber.d("Hybrid: InferenceManager approves LOCAL_LLM")
                }
            }
        }

        // Check if LLM is ready
        if (!llmGenerator.isReady()) {
            return false
        }

        // INVERTED LOGIC: Use LLM for LOW confidence (unknown queries)
        // Use templates for HIGH confidence (known intents, fast & efficient)
        if (classification.confidence >= TEMPLATE_CONFIDENCE_THRESHOLD) {
            Timber.d("Hybrid: High confidence (${classification.confidence}), using template for efficiency")
            return false
        }

        // Low confidence - LLM is better for unknown/unclear queries
        Timber.d("Hybrid: Low confidence (${classification.confidence}), using LLM for flexibility")

        // Check success rate (after enough attempts)
        val totalAttempts = localLLMSuccessCount + localLLMFailureCount
        if (totalAttempts >= 10) {
            val successRate = localLLMSuccessCount.toFloat() / totalAttempts
            if (successRate < 0.5f) {
                Timber.w("Hybrid: Local LLM success rate too low ($successRate), disabling temporarily")
                return false
            }
        }

        return true
    }

    /**
     * Decide whether to use Cloud LLM
     *
     * Criteria:
     * 1. Cloud LLM provider must be configured
     * 2. InferenceManager must allow CLOUD_LLM
     * 3. Local LLM must have failed or be unavailable
     *
     * @param classification NLU classification
     * @return true if should use Cloud LLM, false otherwise
     */
    private fun shouldUseCloudLLM(classification: IntentClassification): Boolean {
        // Must have cloud provider
        if (cloudLLMProvider == null) {
            return false
        }

        // Check InferenceManager
        if (inferenceManager != null) {
            val backend = inferenceManager.selectBackend()
            when (backend) {
                InferenceManager.InferenceBackend.QUEUED,
                InferenceManager.InferenceBackend.NLU_ONLY -> {
                    Timber.d("Hybrid: InferenceManager blocks cloud LLM ($backend)")
                    return false
                }
                InferenceManager.InferenceBackend.CLOUD_LLM -> {
                    Timber.d("Hybrid: InferenceManager prefers CLOUD_LLM")
                    return true
                }
                InferenceManager.InferenceBackend.LOCAL_LLM -> {
                    // Local preferred, but we only use cloud as fallback
                    return false
                }
            }
        }

        // Low confidence cases can use cloud
        if (classification.confidence < TEMPLATE_CONFIDENCE_THRESHOLD) {
            return true
        }

        return false
    }

    /**
     * Hybrid generator is always ready (templates always work)
     */
    override fun isReady(): Boolean = true

    /**
     * Get generator info with metrics
     */
    override fun getInfo(): GeneratorInfo {
        val requestCount = localLLMSuccessCount + localLLMFailureCount +
                           cloudLLMSuccessCount + cloudLLMFailureCount + templateFallbackCount
        val localLLMUsageRate = if (requestCount > 0) {
            (localLLMSuccessCount.toFloat() / requestCount * 100).toInt()
        } else {
            0
        }
        val cloudLLMUsageRate = if (requestCount > 0) {
            (cloudLLMSuccessCount.toFloat() / requestCount * 100).toInt()
        } else {
            0
        }

        // Calculate average latencies (H-07)
        val avgOverallLatency: Long? = if (totalRequests > 0) {
            totalLatency / totalRequests
        } else {
            null
        }
        val avgLocalLLMLatency: Long? = if (localLLMLatencies.isNotEmpty()) {
            localLLMLatencies.average().toLong()
        } else {
            null
        }
        val avgCloudLLMLatency: Long? = if (cloudLLMLatencies.isNotEmpty()) {
            cloudLLMLatencies.average().toLong()
        } else {
            null
        }
        val avgTemplateLatency: Long? = if (templateLatencies.isNotEmpty()) {
            templateLatencies.average().toLong()
        } else {
            null
        }

        return GeneratorInfo(
            name = "Hybrid Response Generator (Local LLM + Cloud LLM + Template)",
            type = GeneratorType.HYBRID,
            supportsStreaming = true,
            averageLatencyMs = avgOverallLatency,
            metadata = mapOf(
                "local_llm_ready" to llmGenerator.isReady().toString(),
                "cloud_llm_ready" to (cloudLLMProvider != null).toString(),
                "local_llm_success_count" to localLLMSuccessCount.toString(),
                "local_llm_failure_count" to localLLMFailureCount.toString(),
                "cloud_llm_success_count" to cloudLLMSuccessCount.toString(),
                "cloud_llm_failure_count" to cloudLLMFailureCount.toString(),
                "template_fallback_count" to templateFallbackCount.toString(),
                "local_llm_usage_rate" to "$localLLMUsageRate%",
                "cloud_llm_usage_rate" to "$cloudLLMUsageRate%",
                "llm_timeout_ms" to LLM_TIMEOUT_MS.toString(),
                "template_confidence_threshold" to TEMPLATE_CONFIDENCE_THRESHOLD.toString(),
                // Latency analytics (H-07)
                "avg_overall_latency_ms" to (avgOverallLatency?.toString() ?: "N/A"),
                "avg_local_llm_latency_ms" to (avgLocalLLMLatency?.toString() ?: "N/A"),
                "avg_cloud_llm_latency_ms" to (avgCloudLLMLatency?.toString() ?: "N/A"),
                "avg_template_latency_ms" to (avgTemplateLatency?.toString() ?: "N/A"),
                "total_requests" to totalRequests.toString()
            )
        )
    }

    /**
     * Get metrics for monitoring
     */
    fun getMetrics(): HybridMetrics {
        val avgOverallLatency = if (totalRequests > 0) {
            totalLatency / totalRequests
        } else {
            0L
        }
        val avgLocalLLMLatency = if (localLLMLatencies.isNotEmpty()) {
            localLLMLatencies.average().toLong()
        } else {
            0L
        }
        val avgCloudLLMLatency = if (cloudLLMLatencies.isNotEmpty()) {
            cloudLLMLatencies.average().toLong()
        } else {
            0L
        }
        val avgTemplateLatency = if (templateLatencies.isNotEmpty()) {
            templateLatencies.average().toLong()
        } else {
            0L
        }

        return HybridMetrics(
            localLLMSuccessCount = localLLMSuccessCount,
            localLLMFailureCount = localLLMFailureCount,
            cloudLLMSuccessCount = cloudLLMSuccessCount,
            cloudLLMFailureCount = cloudLLMFailureCount,
            templateFallbackCount = templateFallbackCount,
            // Latency analytics (H-07)
            avgOverallLatencyMs = avgOverallLatency,
            avgLocalLLMLatencyMs = avgLocalLLMLatency,
            avgCloudLLMLatencyMs = avgCloudLLMLatency,
            avgTemplateLatencyMs = avgTemplateLatency,
            totalRequests = totalRequests
        )
    }

    /**
     * Reset metrics
     */
    fun resetMetrics() {
        localLLMSuccessCount = 0
        localLLMFailureCount = 0
        cloudLLMSuccessCount = 0
        cloudLLMFailureCount = 0
        templateFallbackCount = 0
        // Reset latency tracking (H-07)
        localLLMLatencies.clear()
        cloudLLMLatencies.clear()
        templateLatencies.clear()
        totalLatency = 0L
        totalRequests = 0
        Timber.d("Hybrid generator metrics reset")
    }

    /**
     * Cleanup resources
     */
    suspend fun cleanup() {
        llmGenerator.cleanup()
        Timber.d("Hybrid generator cleaned up")
    }
}

/**
 * Metrics for hybrid generator
 *
 * Updated for H-07: Added latency tracking analytics
 */
data class HybridMetrics(
    val localLLMSuccessCount: Int,
    val localLLMFailureCount: Int,
    val cloudLLMSuccessCount: Int,
    val cloudLLMFailureCount: Int,
    val templateFallbackCount: Int,
    // Latency analytics (H-07)
    val avgOverallLatencyMs: Long = 0L,
    val avgLocalLLMLatencyMs: Long = 0L,
    val avgCloudLLMLatencyMs: Long = 0L,
    val avgTemplateLatencyMs: Long = 0L,
    val totalRequests: Int = 0
) {
    val requestCount: Int
        get() = localLLMSuccessCount + localLLMFailureCount +
                cloudLLMSuccessCount + cloudLLMFailureCount + templateFallbackCount

    val localLLMSuccessRate: Float
        get() = if (localLLMSuccessCount + localLLMFailureCount > 0) {
            localLLMSuccessCount.toFloat() / (localLLMSuccessCount + localLLMFailureCount)
        } else {
            0f
        }

    val cloudLLMSuccessRate: Float
        get() = if (cloudLLMSuccessCount + cloudLLMFailureCount > 0) {
            cloudLLMSuccessCount.toFloat() / (cloudLLMSuccessCount + cloudLLMFailureCount)
        } else {
            0f
        }

    val localLLMUsageRate: Float
        get() = if (requestCount > 0) {
            localLLMSuccessCount.toFloat() / requestCount
        } else {
            0f
        }

    val cloudLLMUsageRate: Float
        get() = if (requestCount > 0) {
            cloudLLMSuccessCount.toFloat() / requestCount
        } else {
            0f
        }
}
