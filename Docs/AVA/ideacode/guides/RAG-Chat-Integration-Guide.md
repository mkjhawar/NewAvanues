# RAG + LLM Chat Integration Guide

**Feature:** Context-aware chat using RAG document retrieval + MLC-LLM generation
**Status:** ✅ Ready for Integration (2025-11-06)
**Components:** RAGChatEngine + MLCLLMProvider + LocalLLMProvider + MLC-LLM

---

## Overview

AVA's RAG+LLM chat system combines document retrieval with on-device LLM generation for accurate, source-backed responses.

**Architecture:**
```
User Question
    ↓
RAGChatEngine
    ↓
Search Documents (RAGRepository)
    ↓
Assemble Context (top 5 chunks)
    ↓
MLCLLMProvider
    ↓
LocalLLMProvider → ALCEngine → MLC-LLM
    ↓
Streaming Response + Source Citations
```

**Benefits:**
- **Accurate:** Answers grounded in actual documents
- **Source-backed:** Every answer cites specific pages/documents
- **Privacy-first:** 100% on-device, no cloud API calls
- **Fast:** <100ms first token, streaming responses
- **Offline:** Works without internet connection

---

## Quick Start

### 1. Initialize Components

```kotlin
class MainActivity : ComponentActivity() {

    private lateinit var ragRepository: SQLiteRAGRepository
    private lateinit var llmProvider: MLCLLMProvider
    private lateinit var chatEngine: RAGChatEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // 1. Initialize RAG repository
            ragRepository = SQLiteRAGRepository(
                context = applicationContext,
                embeddingProvider = ONNXEmbeddingProvider(applicationContext),
                enableClustering = true
            )

            // 2. Initialize LLM provider
            llmProvider = MLCLLMProvider(applicationContext)
            llmProvider.initialize().onFailure {
                showError("Failed to initialize LLM: ${it.message}")
                return@launch
            }

            // 3. Create chat engine
            chatEngine = RAGChatEngine(
                ragRepository = ragRepository,
                llmProvider = llmProvider,
                config = ChatConfig(
                    maxContextChunks = 5,       // Top 5 relevant chunks
                    minSimilarity = 0.7f,        // 70% minimum match
                    maxContextLength = 2000,     // ~500 tokens
                    maxHistoryMessages = 10      // Last 10 messages
                )
            )

            // Ready to chat!
            askQuestion("How do I reset the device?")
        }
    }
}
```

### 2. Ask Questions

```kotlin
suspend fun askQuestion(question: String) {
    var fullResponse = ""
    val sources = mutableListOf<Source>()

    chatEngine.ask(question).collect { response ->
        when (response) {
            is ChatResponse.Streaming -> {
                // Update UI with each token
                fullResponse += response.text
                updateChatUI(fullResponse)
            }

            is ChatResponse.Complete -> {
                // Show sources
                sources.addAll(response.sources)
                displaySources(sources)
            }

            is ChatResponse.NoContext -> {
                // No relevant documents found
                showMessage(response.message)
            }

            is ChatResponse.Error -> {
                // Handle error
                showError(response.message)
            }
        }
    }
}
```

### 3. Display Sources

```kotlin
fun displaySources(sources: List<Source>) {
    sources.forEach { source ->
        println("""
            📄 ${source.title}
            📖 Page ${source.page ?: "N/A"}
            📊 Relevance: ${(source.similarity * 100).toInt()}%
            💬 "${source.snippet}"
        """.trimIndent())
    }
}
```

---

## Complete Example: RAG Chat ViewModel

```kotlin
class RAGChatViewModel(
    private val ragRepository: RAGRepository,
    private val llmProvider: MLCLLMProvider
) : ViewModel() {

    private val chatEngine = RAGChatEngine(
        ragRepository = ragRepository,
        llmProvider = llmProvider,
        config = ChatConfig()
    )

    // UI State
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    /**
     * Send a question to RAG chat
     */
    fun askQuestion(question: String) {
        viewModelScope.launch {
            _isGenerating.value = true

            // Add user message
            _messages.value += ChatMessage(
                role = MessageRole.USER,
                content = question,
                timestamp = System.currentTimeMillis()
            )

            var assistantResponse = ""
            val sources = mutableListOf<Source>()

            try {
                chatEngine.ask(
                    question = question,
                    conversationHistory = _messages.value
                ).collect { response ->
                    when (response) {
                        is ChatResponse.Streaming -> {
                            assistantResponse += response.text
                            // Update assistant message in real-time
                            updateAssistantMessage(assistantResponse)
                        }

                        is ChatResponse.Complete -> {
                            sources.addAll(response.sources)
                            // Final update with sources
                            updateAssistantMessage(assistantResponse, sources)
                            _isGenerating.value = false
                        }

                        is ChatResponse.NoContext -> {
                            assistantResponse = response.message
                            updateAssistantMessage(assistantResponse)
                            _isGenerating.value = false
                        }

                        is ChatResponse.Error -> {
                            assistantResponse = "Error: ${response.message}"
                            updateAssistantMessage(assistantResponse)
                            _isGenerating.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Chat error")
                updateAssistantMessage("Error: ${e.message}")
                _isGenerating.value = false
            }
        }
    }

    private fun updateAssistantMessage(content: String, sources: List<Source> = emptyList()) {
        val updatedMessages = _messages.value.toMutableList()

        // Find or create assistant message
        val lastIndex = updatedMessages.indexOfLast { it.role == MessageRole.ASSISTANT }
        if (lastIndex >= 0) {
            updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(
                content = content,
                sources = sources
            )
        } else {
            updatedMessages.add(
                ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = content,
                    sources = sources,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        _messages.value = updatedMessages
    }

    /**
     * Clear conversation history
     */
    fun clearChat() {
        _messages.value = emptyList()
    }

    /**
     * Stop generation
     */
    fun stopGeneration() {
        viewModelScope.launch {
            llmProvider.stop()
            _isGenerating.value = false
        }
    }
}

data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val sources: List<Source> = emptyList(),
    val timestamp: Long
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}
```

---

## Compose UI Example

```kotlin
@Composable
fun RAGChatScreen(viewModel: RAGChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                ChatMessageCard(message)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask a question...") },
                enabled = !isGenerating
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (isGenerating) {
                IconButton(onClick = { viewModel.stopGeneration() }) {
                    Icon(Icons.Default.Close, "Stop")
                }
            } else {
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.askQuestion(inputText)
                            inputText = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, "Send")
                }
            }
        }
    }
}

@Composable
fun ChatMessageCard(message: ChatMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (message.role == MessageRole.USER) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Role
            Text(
                text = message.role.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Content
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium
            )

            // Sources
            if (message.sources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Sources:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                message.sources.forEach { source ->
                    SourceChip(source)
                }
            }
        }
    }
}

@Composable
fun SourceChip(source: Source) {
    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${source.title} (p.${source.page ?: "?"}) - ${(source.similarity * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
```

---

## Configuration

### ChatConfig Options

```kotlin
data class ChatConfig(
    val maxContextChunks: Int = 5,           // How many document chunks to include
    val minSimilarity: Float = 0.7f,         // Minimum relevance threshold (70%)
    val maxContextLength: Int = 2000,        // Max characters of context (~500 tokens)
    val maxHistoryMessages: Int = 10,        // Conversation history length
    val systemPrompt: String = """
        You are AVA, an intelligent assistant with access to technical documentation and manuals.
        Your role is to help users find information from their documents accurately and efficiently.
    """.trimIndent()
)
```

### MLCLLMProvider Options

```kotlin
val llmProvider = MLCLLMProvider(
    context = applicationContext,
    modelPath = "/path/to/gemma-2b-it/"  // Optional, defaults to external files dir
)

// Initialize with custom model
llmProvider.initialize(modelPath = "/sdcard/models/custom-model/")
```

---

## Performance Tuning

### Context Size vs Quality

| maxContextChunks | Context Quality | Response Speed | Memory Usage |
|------------------|----------------|----------------|--------------|
| 3                | Lower          | Faster         | ~300 MB      |
| **5 (default)**  | **Good**       | **Good**       | **~400 MB**  |
| 7                | Better         | Slower         | ~500 MB      |
| 10               | Best           | Slowest        | ~600 MB      |

### Similarity Threshold

| minSimilarity | Effect | When to Use |
|---------------|--------|-------------|
| 0.6           | More results, less precise | Exploratory queries |
| **0.7 (default)** | **Balanced** | **General use** |
| 0.8           | Fewer results, very precise | Exact information |
| 0.9           | Very few results, extremely precise | Critical lookups |

### Generation Speed

| Device Type | CPU | GPU | Notes |
|-------------|-----|-----|-------|
| **High-end** | 30 tok/s | 80 tok/s | Flagship devices |
| **Mid-range** | 20 tok/s | 50 tok/s | Most users |
| **Low-end** | 10 tok/s | 30 tok/s | Budget devices |

---

## Troubleshooting

### Issue 1: "LLM not initialized"

**Cause:** MLCLLMProvider.initialize() not called or failed

**Solution:**
```kotlin
val result = llmProvider.initialize()
result.onFailure { error ->
    Timber.e(error, "Failed to initialize")
    // Check if model files exist
    // Check permissions
}
```

### Issue 2: No context found

**Cause:** No documents indexed or query doesn't match documents

**Solution:**
- Check if documents are indexed: `repository.listDocuments().collect { ... }`
- Lower `minSimilarity` threshold
- Rephrase question to match document terminology

### Issue 3: Slow responses

**Cause:** Large context or slow device

**Solution:**
- Reduce `maxContextChunks` from 5 to 3
- Reduce `maxContextLength` from 2000 to 1500
- Use GPU acceleration if available
- Consider smaller model (e.g., TinyLlama)

### Issue 4: Out of memory

**Cause:** Too many chunks or large model

**Solution:**
- Reduce `maxContextChunks`
- Close other apps
- Use int8 quantized model
- Consider incremental cleanup

---

## Best Practices

### ✅ DO:
- Initialize MLCLLMProvider once at app startup
- Reuse the same chatEngine instance
- Show streaming progress to user
- Display source citations prominently
- Handle NoContext responses gracefully
- Monitor memory usage
- Test on low-end devices

### ❌ DON'T:
- Create new chatEngine for each question
- Block UI thread during generation
- Ignore error responses
- Hide source information
- Set maxContextChunks > 10
- Forget to handle cleanup in onDestroy()

---

## Advanced: Custom Prompts

### Custom System Prompt

```kotlin
val config = ChatConfig(
    systemPrompt = """
        You are a car diagnostic assistant with access to vehicle repair manuals.
        You help users diagnose and fix vehicle issues.

        Guidelines:
        - Always cite the specific manual and page number
        - Warn about safety concerns (high voltage, moving parts, etc.)
        - Suggest when professional help is needed
        - Use clear, step-by-step instructions
    """.trimIndent()
)
```

### RAG Prompt Template

The RAG system automatically constructs prompts like this:

```
You are AVA, an intelligent assistant...

Context from documents:
[Source: 2019 Honda Accord Repair Manual, Page 42, Relevance: 95%]
To reset the oil life indicator:
1. Turn ignition to ON (engine off)
2. Press and hold SELECT/RESET button for 10 seconds
3. Display will show OIL LIFE RESET

[Source: 2019 Honda Accord Owner's Manual, Page 58, Relevance: 87%]
The maintenance minder system calculates engine oil life...

User question: How do I reset the oil life indicator?

Instructions:
- Answer based ONLY on the provided context
- Cite specific sources (document name, page number)
- If the context doesn't contain the answer, say "I don't have that information"
- Be conversational but accurate
- Keep responses concise unless detail is requested

Answer:
```

---

## Related Documentation

- [RAG Quick Start Guide](RAG-Quick-Start-Guide.md)
- [Web Document Import Guide](Web-Document-Import-Guide.md)
- [Developer Manual Chapter 28](Developer-Manual-Chapter28-RAG.md)
- [LLM Setup Guide](LLM-SETUP.md)

---

**Last Updated:** 2025-11-06
**Status:** Production Ready ✅
**Dependencies:** RAGChatEngine, MLCLLMProvider, MLC-LLM, ONNX Runtime
