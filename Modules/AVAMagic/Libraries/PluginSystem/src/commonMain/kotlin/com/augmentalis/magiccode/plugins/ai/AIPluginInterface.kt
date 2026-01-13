package com.augmentalis.avacode.plugins.ai

/**
 * Base interface for AI-powered plugins.
 *
 * Defines the contract for plugins that provide AI capabilities
 * (LLM, NLP, sentiment analysis, text generation, etc.).
 */
interface AIPluginInterface {
    /**
     * Get plugin capabilities.
     *
     * @return Set of capability identifiers
     */
    fun getCapabilities(): Set<String>

    /**
     * Check if plugin supports a specific capability.
     *
     * @param capability Capability identifier
     * @return true if supported
     */
    fun supportsCapability(capability: String): Boolean {
        return capability in getCapabilities()
    }

    /**
     * Initialize AI plugin with configuration.
     *
     * @param config Configuration map
     */
    suspend fun initialize(config: Map<String, Any>)

    /**
     * Shutdown and cleanup resources.
     */
    suspend fun shutdown()
}

/**
 * Text generation AI plugin interface.
 *
 * For plugins that generate text (LLMs, text completion, etc.).
 */
interface TextGenerationPlugin : AIPluginInterface {
    /**
     * Generate text from prompt.
     *
     * @param request Generation request
     * @return Generation response
     */
    suspend fun generateText(request: TextGenerationRequest): TextGenerationResponse
}

/**
 * Text generation request.
 */
data class TextGenerationRequest(
    val prompt: String,
    val maxTokens: Int = 100,
    val temperature: Double = 0.7,
    val stopSequences: List<String> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Text generation response.
 */
data class TextGenerationResponse(
    val text: String,
    val tokensUsed: Int,
    val finishReason: FinishReason,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Reason text generation finished.
 */
enum class FinishReason {
    COMPLETED,
    MAX_TOKENS,
    STOP_SEQUENCE,
    ERROR
}

/**
 * NLP (Natural Language Processing) plugin interface.
 */
interface NLPPlugin : AIPluginInterface {
    /**
     * Analyze sentiment of text.
     *
     * @param text Text to analyze
     * @return Sentiment analysis result
     */
    suspend fun analyzeSentiment(text: String): SentimentResult

    /**
     * Extract entities from text.
     *
     * @param text Text to analyze
     * @return List of extracted entities
     */
    suspend fun extractEntities(text: String): List<Entity>

    /**
     * Classify text into categories.
     *
     * @param text Text to classify
     * @param categories Possible categories
     * @return Classification result
     */
    suspend fun classifyText(text: String, categories: List<String>): ClassificationResult
}

/**
 * Sentiment analysis result.
 */
data class SentimentResult(
    val sentiment: Sentiment,
    val confidence: Double,
    val scores: Map<Sentiment, Double> = emptyMap()
)

/**
 * Sentiment classification.
 */
enum class Sentiment {
    POSITIVE,
    NEGATIVE,
    NEUTRAL
}

/**
 * Named entity.
 */
data class Entity(
    val text: String,
    val type: EntityType,
    val startIndex: Int,
    val endIndex: Int,
    val confidence: Double
)

/**
 * Entity type.
 */
enum class EntityType {
    PERSON,
    ORGANIZATION,
    LOCATION,
    DATE,
    TIME,
    MONEY,
    PERCENT,
    OTHER
}

/**
 * Text classification result.
 */
data class ClassificationResult(
    val category: String,
    val confidence: Double,
    val scores: Map<String, Double> = emptyMap()
)

/**
 * Embedding generation plugin interface.
 */
interface EmbeddingPlugin : AIPluginInterface {
    /**
     * Generate embeddings for text.
     *
     * @param texts List of texts to embed
     * @return List of embedding vectors
     */
    suspend fun generateEmbeddings(texts: List<String>): List<Embedding>
}

/**
 * Text embedding vector.
 */
data class Embedding(
    val vector: FloatArray,
    val model: String,
    val dimensions: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Embedding

        if (!vector.contentEquals(other.vector)) return false
        if (model != other.model) return false
        if (dimensions != other.dimensions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vector.contentHashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + dimensions
        return result
    }
}
