@file:Suppress("UNUSED")
package com.augmentalis.llm.domain

/**
 * Type aliases for backward compatibility
 *
 * The domain types have moved to the commonMain package (com.augmentalis.llm).
 * These aliases ensure existing Android code continues to work without changes.
 */

// Import all common types
import com.augmentalis.llm.LLMResponse as CommonLLMResponse
import com.augmentalis.llm.TokenUsage as CommonTokenUsage
import com.augmentalis.llm.ChatMessage as CommonChatMessage
import com.augmentalis.llm.MessageRole as CommonMessageRole
import com.augmentalis.llm.LLMConfig as CommonLLMConfig
import com.augmentalis.llm.GenerationOptions as CommonGenerationOptions
import com.augmentalis.llm.LLMProviderInfo as CommonLLMProviderInfo
import com.augmentalis.llm.LLMCapabilities as CommonLLMCapabilities
import com.augmentalis.llm.ProviderHealth as CommonProviderHealth
import com.augmentalis.llm.HealthStatus as CommonHealthStatus
import com.augmentalis.llm.ProviderType as CommonProviderType
import com.augmentalis.llm.LLMProvider as CommonLLMProvider
import com.augmentalis.llm.LLMResult as CommonLLMResult

// Type aliases for backward compatibility
typealias LLMResponse = CommonLLMResponse
typealias TokenUsage = CommonTokenUsage
typealias ChatMessage = CommonChatMessage
typealias MessageRole = CommonMessageRole
typealias LLMConfig = CommonLLMConfig
typealias GenerationOptions = CommonGenerationOptions
typealias LLMProviderInfo = CommonLLMProviderInfo
typealias LLMCapabilities = CommonLLMCapabilities
typealias ProviderHealth = CommonProviderHealth
typealias HealthStatus = CommonHealthStatus
typealias ProviderType = CommonProviderType
typealias LLMProvider = CommonLLMProvider
// Note: LLMResult is a generic type and cannot be directly aliased.
// Use the full type com.augmentalis.llm.LLMResult<T> in code, or import it directly.
