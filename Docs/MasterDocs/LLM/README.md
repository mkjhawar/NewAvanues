# LLM Module - Large Language Model Processing Engine

**Version:** 2.0 | **Platform:** Kotlin Multiplatform | **Last Updated:** 2026-01-11

---

## Executive Summary

The LLM Module implements the **Adaptive LLM Coordinator (ALC)** that orchestrates intelligent response generation using both on-device and cloud-based large language models.

### Key Capabilities

| Capability | Description |
|------------|-------------|
| **On-Device Processing** | 95%+ local inference using MLC LLM and TVM runtime |
| **Cloud Fallback** | Multi-provider chain: OpenRouter → Anthropic → Google → OpenAI |
| **Multilingual Support** | Dynamic model switching for 140+ languages |
| **Streaming Responses** | Real-time token streaming for typewriter effects |
| **Cost-Aware** | Spending limits, cost tracking, battery/thermal constraints |
| **Hybrid Generation** | Automatic fallback ensures zero user-facing failures |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    AVA Chat UI                               │
│              (ChatViewModel, ChatScreen)                     │
└──────────────────────────┬──────────────────────────────────┘
                           │ generateResponse()
                           ▼
┌─────────────────────────────────────────────────────────────┐
│           Response Generation Layer                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ HybridResponseGenerator (Fallback Chain)              │   │
│  │ • Orchestrates all response sources                   │   │
│  │ • Implements timeout & error handling                 │   │
│  │ • Tracks metrics & success rates                      │   │
│  └──────────────────────────────────────────────────────┘   │
│         ↓              ↓              ↓                      │
│    (Primary)      (Secondary)    (Fallback)                  │
└─────────────────────────────────────────────────────────────┘
         │                  │                │
         ▼                  ▼                ▼
    ┌─────────┐         ┌──────────┐    ┌──────────┐
    │ Local   │         │ Cloud    │    │Template  │
    │ LLM     │         │LLM Chain │    │Generator │
    │Provider │         │Provider  │    │          │
    └────┬────┘         └────┬─────┘    └──────────┘
         │                   │
         ▼                   ▼
    ┌─────────────────┐  OpenRouter → Anthropic → Google → OpenAI
    │ On-Device Models│
    │ (MLC + TVM)     │
    │ • Gemma 2B (En) │
    │ • Gemma 3 4B    │
    │ • Qwen, Mistral │
    └─────────────────┘
```

---

## Module Structure

```
Modules/LLM/
├── src/
│   ├── commonMain/kotlin/com/augmentalis/llm/
│   │   ├── domain/              # Core interfaces & models
│   │   │   ├── LLMProvider.kt   # Provider interface
│   │   │   ├── LLMResponse.kt   # Response sealed class
│   │   │   └── ChatMessage.kt   # Conversation model
│   │   ├── response/            # Response generation
│   │   │   ├── HybridResponseGenerator.kt
│   │   │   ├── LLMResponseGenerator.kt
│   │   │   ├── TemplateResponseGenerator.kt
│   │   │   └── LLMContextBuilder.kt
│   │   └── cache/               # Token caching
│   │
│   └── androidMain/kotlin/com/augmentalis/llm/
│       ├── provider/            # LLM providers
│       │   ├── LocalLLMProvider.kt
│       │   ├── CloudLLMProvider.kt
│       │   ├── AnthropicProvider.kt
│       │   ├── OpenAIProvider.kt
│       │   ├── GoogleAIProvider.kt
│       │   └── OpenRouterProvider.kt
│       ├── alc/                 # Adaptive LLM Coordinator
│       │   ├── ALCEngine.kt     # Multilingual orchestrator
│       │   ├── inference/       # Inference strategies
│       │   ├── tokenizer/       # Tokenization
│       │   └── memory/          # KV cache management
│       ├── security/            # API key management
│       └── di/                  # Hilt modules
```

---

## Class Inventory

### Core Interfaces

| Class | File | Purpose |
|-------|------|---------|
| `LLMProvider` | `domain/LLMProvider.kt` | Abstract LLM provider interface |
| `ResponseGenerator` | `response/ResponseGenerator.kt` | Response generation strategy |
| `IInferenceStrategy` | `alc/interfaces/IInferenceStrategy.kt` | Model inference abstraction |
| `IMemoryManager` | `alc/interfaces/IMemoryManager.kt` | KV cache memory management |

### Providers (60+ classes total)

| Provider | Models | Purpose |
|----------|--------|---------|
| `LocalLLMProvider` | Gemma 2B, Gemma 3 4B, Qwen, Mistral | On-device inference |
| `AnthropicProvider` | Claude 3.x | Claude API |
| `OpenAIProvider` | GPT-4 Turbo, GPT-4o | OpenAI API |
| `GoogleAIProvider` | Gemini Pro | Google AI API |
| `OpenRouterProvider` | 100+ models | Multi-model aggregator |
| `CloudLLMProvider` | All cloud models | Intelligent fallback |

### Response Generation

| Class | Purpose |
|-------|---------|
| `HybridResponseGenerator` | LLM + Cloud + Template fallback (recommended) |
| `LLMResponseGenerator` | Local LLM only |
| `TemplateResponseGenerator` | Fast intent-to-template mapping |
| `LLMContextBuilder` | Mobile-optimized prompt builder |

---

## Public API Reference

### LLMProvider Interface

```kotlin
interface LLMProvider {
    suspend fun initialize(config: LLMConfig): Result<Unit>
    suspend fun generateResponse(prompt: String, options: GenerationOptions): Flow<LLMResponse>
    suspend fun chat(messages: List<ChatMessage>, options: GenerationOptions): Flow<LLMResponse>
    suspend fun stop()
    suspend fun cleanup()
    fun isGenerating(): Boolean
    fun estimateCost(inputTokens: Int, outputTokens: Int): Double
}
```

### HybridResponseGenerator (Recommended)

```kotlin
class HybridResponseGenerator(
    context: Context,
    llmProvider: LocalLLMProvider,
    cloudLLMProvider: CloudLLMProvider? = null
) : ResponseGenerator {

    suspend fun generateResponse(
        userMessage: String,
        classification: IntentClassification,
        context: ResponseContext = ResponseContext()
    ): Flow<ResponseChunk>
}
```

### Response Types

```kotlin
sealed class LLMResponse {
    data class Streaming(val chunk: String, val tokenCount: Int?) : LLMResponse()
    data class Complete(val fullText: String, val usage: TokenUsage) : LLMResponse()
    data class Error(val message: String, val code: String?) : LLMResponse()
}

sealed class ResponseChunk {
    data class Text(val content: String) : ResponseChunk()
    data class Complete(val fullText: String, val metadata: Map<String, Any>) : ResponseChunk()
    data class Error(val message: String) : ResponseChunk()
}
```

---

## Configuration

### LLMConfig

```kotlin
data class LLMConfig(
    val modelPath: String,           // File path or model ID
    val modelLib: String? = null,    // MLC library name
    val apiKey: String? = null,      // For cloud providers
    val device: String = "opencl",   // "opencl", "vulkan", "cpu"
    val maxMemoryMB: Int = 2048
)
```

### GenerationOptions

```kotlin
data class GenerationOptions(
    val temperature: Float = 0.7f,
    val maxTokens: Int? = null,
    val topP: Float = 0.95f,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val stopSequences: List<String> = emptyList()
)
```

---

## Usage Examples

### Basic Usage

```kotlin
// Initialize hybrid generator
val generator = HybridResponseGenerator(
    context = context,
    llmProvider = LocalLLMProvider(context),
    cloudLLMProvider = CloudLLMProvider(context, apiKeyManager)
)

// Generate response
generator.generateResponse(
    userMessage = "What time is it?",
    classification = IntentClassification(intent = "show_time", confidence = 0.9f)
).collect { chunk ->
    when (chunk) {
        is ResponseChunk.Text -> print(chunk.content)
        is ResponseChunk.Complete -> println("\nDone: ${chunk.fullText}")
        is ResponseChunk.Error -> println("Error: ${chunk.message}")
    }
}
```

### Advanced: Direct LLM Chat

```kotlin
val messages = listOf(
    ChatMessage(role = "system", content = "You are a helpful assistant."),
    ChatMessage(role = "user", content = "Explain quantum computing in 50 words.")
)

llmProvider.chat(messages, GenerationOptions(maxTokens = 100)).collect { response ->
    when (response) {
        is LLMResponse.Streaming -> print(response.chunk)
        is LLMResponse.Complete -> println("\nTokens: ${response.usage.totalTokens}")
        is LLMResponse.Error -> println("Error: ${response.message}")
    }
}
```

---

## Performance Characteristics

### Latency Profile

| Generator | Latency |
|-----------|---------|
| Template | <1ms |
| Local LLM (first token) | 1-3s |
| Local LLM (50 tokens) | 5-20s |
| Cloud LLM (stream start) | 0.5-1s |
| Hybrid (fallback) | <1ms |

### Memory Usage

| Device | Model | RAM |
|--------|-------|-----|
| 2GB | TinyLlama 1B | 600MB |
| 4GB | Gemma 2B | 1.2GB |
| 6GB | Gemma 3 4B | 2.3GB |
| 8GB+ | Mistral 7B | 4GB |

### Cost Profile (Cloud)

| Provider | Cost per 1K requests |
|----------|---------------------|
| Local LLM | $0.00 |
| OpenRouter | $0.50-$2.00 |
| Google Gemini | $0.15-$0.50 |
| Anthropic | $3.50-$15.00 |
| OpenAI GPT-4 | $4.00-$20.00 |

---

## Integration with AVA

```kotlin
// AVA ChatViewModel integration
class ChatViewModel(
    private val responseGenerator: ResponseGenerator
) : ViewModel() {

    fun sendMessage(userMessage: String) {
        viewModelScope.launch {
            val classification = nluClassifier.classify(userMessage)

            responseGenerator.generateResponse(userMessage, classification)
                .collect { chunk ->
                    when (chunk) {
                        is ResponseChunk.Text -> _messageFlow.emit(chunk.content)
                        is ResponseChunk.Complete -> saveToDatabase(chunk.fullText)
                        is ResponseChunk.Error -> showError(chunk.message)
                    }
                }
        }
    }
}
```

---

## Related Documentation

- [AVA Architecture](../AVA/README.md)
- [NLU Module](../NLU/README.md)
- [VoiceOSCoreNG](../VoiceOSCoreNG/README.md)

---

**Author:** AVA AI Team | **Last Updated:** 2026-01-11
