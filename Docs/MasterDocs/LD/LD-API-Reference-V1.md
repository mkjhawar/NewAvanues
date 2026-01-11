# LD-API-Reference-V1 - API Reference Living Document

**Type:** Living Document | **Version:** 1 | **Last Updated:** 2026-01-11

---

## API Reference Summary

This document provides quick reference to the most commonly used APIs across the Avanues Platform.

---

## VoiceOSCoreNG APIs

### VoiceOSCoreNG Facade

```kotlin
class VoiceOSCoreNG {
    // Lifecycle
    suspend fun initialize()
    suspend fun dispose()

    // Command Processing
    suspend fun processCommand(text: String, confidence: Float = 1.0f): HandlerResult

    // Voice Control
    suspend fun startListening(): Result<Unit>
    suspend fun stopListening()
    suspend fun updateCommands(commands: List<String>): Result<Unit>

    // Dynamic Commands
    suspend fun updateDynamicCommands(commands: List<QuantizedCommand>): Result<Unit>
    fun clearDynamicCommands()
    val dynamicCommandCount: Int

    // State
    val state: StateFlow<ServiceState>
    val commandResults: SharedFlow<CommandResult>
}
```

### Android Quick Setup

```kotlin
// Create instance
val core = VoiceOSCoreNG.createForAndroid(accessibilityService)

// Initialize
core.initialize()

// Process command
val result = core.processCommand("scroll down")
```

---

## AVA APIs

### ActionsManager

```kotlin
class ActionsManager {
    fun initialize()
    fun hasHandler(intent: String): Boolean
    suspend fun executeAction(intent: String, utterance: String): ActionResult
    fun getCategoryForIntent(intent: String): String
}
```

### ChatViewModel

```kotlin
class ChatViewModel {
    fun sendMessage(userMessage: String)
    val messages: StateFlow<List<Message>>
    val isGenerating: StateFlow<Boolean>
}
```

### MemoryManager

```kotlin
interface MemoryManager {
    suspend fun remember(type: MemoryType, content: String, importance: Float): MemoryEntry
    suspend fun recall(id: String): MemoryEntry?
    suspend fun search(query: String, limit: Int): List<MemoryEntry>
    suspend fun consolidateMemories()
}
```

---

## LLM APIs

### LLMProvider Interface

```kotlin
interface LLMProvider {
    suspend fun initialize(config: LLMConfig): Result<Unit>
    suspend fun generateResponse(prompt: String, options: GenerationOptions): Flow<LLMResponse>
    suspend fun chat(messages: List<ChatMessage>, options: GenerationOptions): Flow<LLMResponse>
    suspend fun stop()
    suspend fun cleanup()
}
```

### HybridResponseGenerator

```kotlin
class HybridResponseGenerator : ResponseGenerator {
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
    data class Streaming(val chunk: String) : LLMResponse()
    data class Complete(val fullText: String, val usage: TokenUsage) : LLMResponse()
    data class Error(val message: String) : LLMResponse()
}
```

---

## NLU APIs

### ClassifyIntentUseCase

```kotlin
class ClassifyIntentUseCase {
    fun execute(utterance: String, language: String = "en-US"): Result<IntentClassification>
    fun supportsLanguage(code: String): Boolean
    fun supportedLanguages(): List<String>
}
```

### HybridIntentClassifier

```kotlin
class HybridIntentClassifier(config: ClassifierConfig) {
    fun classify(input: String): ClassificationResult
    fun classifyFast(input: String): IntentMatch?
    fun index(intents: List<UnifiedIntent>)
}
```

### Classification Result

```kotlin
data class IntentClassification(
    val intent: String,
    val confidence: Float,
    val entities: Map<String, String>
)

data class ClassificationResult(
    val matches: List<IntentMatch>,
    val method: MatchMethod,
    val confidence: Float,
    val processingTimeMs: Long
)
```

---

## RAG APIs

### RAGRepository Interface

```kotlin
interface RAGRepository {
    suspend fun addDocument(request: AddDocumentRequest): Result<AddDocumentResult>
    suspend fun search(query: SearchQuery): Result<SearchResponse>
    suspend fun deleteDocument(documentId: String): Result<Unit>
    fun listDocuments(status: DocumentStatus?): Flow<Document>
}
```

### RAGChatEngine

```kotlin
class RAGChatEngine(
    ragRepository: RAGRepository,
    llmProvider: LLMProvider,
    config: ChatConfig
) {
    fun ask(question: String, history: List<Message> = emptyList()): Flow<ChatResponse>
}
```

### Search Types

```kotlin
data class SearchQuery(
    val query: String,
    val maxResults: Int = 10,
    val minSimilarity: Float = 0.5f,
    val filters: SearchFilters = SearchFilters()
)

data class SearchResult(
    val chunk: Chunk,
    val similarity: Float,
    val document: Document?
)
```

---

## Common Library APIs

### VUID Generation

```kotlin
object VUIDGenerator {
    fun generateCompact(packageName: String, version: String, typeName: String): String
    fun generateMessageVuid(): String
    fun generateConversationVuid(): String
    fun isValid(vuid: String): Boolean
    fun parse(vuid: String): ParsedVuid?
}
```

### VoiceOSResult

```kotlin
sealed class VoiceOSResult<T, E> {
    data class Success<T, E>(val value: T) : VoiceOSResult<T, E>()
    data class Failure<T, E>(val error: E) : VoiceOSResult<T, E>()

    fun <R> map(transform: (T) -> R): VoiceOSResult<R, E>
    fun <R> flatMap(transform: (T) -> VoiceOSResult<R, E>): VoiceOSResult<R, E>
    fun <F> mapError(transform: (E) -> F): VoiceOSResult<T, F>
}
```

### ThemeManager

```kotlin
object ThemeManager {
    suspend fun setUniversalTheme(theme: Theme)
    fun getUniversalTheme(): Theme
    fun observeUniversalTheme(): StateFlow<Theme>
    suspend fun setAppTheme(appId: String, theme: Theme)
    fun getTheme(appId: String): Theme
}
```

### AssetManager

```kotlin
object AssetManager {
    suspend fun registerIconLibrary(library: IconLibrary)
    suspend fun getIcon(reference: String): Icon?
    suspend fun searchIcons(query: String): List<IconSearchResult>
}
```

---

## WebAvanue APIs

### DOMScraperBridge

```kotlin
class DOMScraperBridge {
    suspend fun scrapeDOM(): List<DOMElement>
    fun executeClick(vuid: String)
    fun executeInput(vuid: String, text: String)
    fun executeScroll(direction: String)
}
```

### VoiceCommandGenerator

```kotlin
class VoiceCommandGenerator {
    fun matchCommand(utterance: String, elements: List<DOMElement>): MatchResult

    data class MatchResult(
        val element: DOMElement?,
        val confidence: Float,
        val alternatives: List<DOMElement>
    )
}
```

---

## Handler Results

```kotlin
sealed class HandlerResult {
    data class Success(val message: String, val data: Any? = null) : HandlerResult()
    data class Failure(val reason: String, val recoverable: Boolean = true) : HandlerResult()
    object NotHandled : HandlerResult()
    data class RequiresInput(val prompt: String, val inputType: InputType) : HandlerResult()
    data class AwaitingSelection(val message: String, val matchCount: Int) : HandlerResult()
}
```

---

## Configuration Classes

### ServiceConfiguration (VoiceOSCoreNG)

```kotlin
data class ServiceConfiguration(
    val voiceLanguage: String = "en-US",
    val confidenceThreshold: Float = 0.7f,
    val speechEngine: String = "ANDROID_STT",
    val enableWakeWord: Boolean = true,
    val wakeWord: String = "hey voice",
    val autoStartListening: Boolean = true
)
```

### LLMConfig

```kotlin
data class LLMConfig(
    val modelPath: String,
    val device: String = "opencl",
    val maxMemoryMB: Int = 2048,
    val apiKey: String? = null
)
```

### ClassifierConfig (NLU)

```kotlin
data class ClassifierConfig(
    val exactMatchThreshold: Float = 0.95f,
    val fuzzyMinSimilarity: Float = 0.7f,
    val semanticMinSimilarity: Float = 0.6f,
    val maxCandidates: Int = 5
)
```

### RAGConfig

```kotlin
data class RAGConfig(
    val embeddingConfig: EmbeddingConfig = EmbeddingConfig(),
    val storageConfig: StorageConfig = StorageConfig(),
    val chunkingConfig: ChunkingConfig = ChunkingConfig()
)
```

---

## Enums Reference

### CommandActionType (VoiceOSCoreNG)
`CLICK`, `LONG_CLICK`, `SCROLL_UP`, `SCROLL_DOWN`, `BACK`, `HOME`, `OPEN_APP`, `VOICE_MUTE`, etc.

### ActionCategory
`SYSTEM`, `NAVIGATION`, `APP`, `UI`, `INPUT`, `MEDIA`, `ACCESSIBILITY`, `CUSTOM`

### ServiceState
`Uninitialized`, `Initializing`, `Ready`, `Listening`, `Processing`, `Error`, `Paused`, `Stopped`

### MatchMethod (NLU)
`EXACT`, `FUZZY`, `SEMANTIC`, `HYBRID`

### DocumentType (RAG)
`PDF`, `DOCX`, `TXT`, `MD`, `HTML`, `EPUB`, `RTF`

### ChunkingStrategy
`FIXED_SIZE`, `SEMANTIC`, `HYBRID`

---

## Quick Reference Card

| Task | Module | Method |
|------|--------|--------|
| Process voice command | VoiceOSCoreNG | `core.processCommand(text)` |
| Classify intent | NLU | `useCase.execute(text)` |
| Generate response | LLM | `generator.generateResponse(msg, class)` |
| Search documents | RAG | `repo.search(query)` |
| Ask with context | RAG | `engine.ask(question)` |
| Execute action | AVA | `manager.executeAction(intent)` |
| Generate VUID | Common | `VUIDGenerator.generateCompact()` |

---

**Maintainer:** Platform Team
**Next Review:** Weekly
