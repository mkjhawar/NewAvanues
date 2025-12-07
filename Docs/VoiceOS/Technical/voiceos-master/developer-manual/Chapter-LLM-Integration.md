# Developer Manual: LLM Integration

**Version:** 1.0
**Date:** 2025-11-18
**Author:** VoiceOS/AVA Architecture Team

---

## Overview

The Large Language Model (LLM) component provides conversational AI, complex reasoning, and multi-step task planning for VoiceOS/AVA. LLMs are loaded on-demand to minimize resource usage.

---

## Architecture

### Processing Pipeline

```
User Speech
    ↓
VoiceOS Command Matching (<50ms)
    ↓ No Match
NLU Intent Classification (50-200ms)
    ↓ Complex/Conversational
LLM Reasoning (500-2000ms)
    ↓
Response Generation
```

### When LLM is Used

1. **Conversational queries** - "Tell me about..."
2. **Complex reasoning** - Multi-step problems
3. **Context continuation** - Follow-up questions
4. **No NLU match** - Fallback for unknown intents

---

## Model Registry

### AVA Naming Convention

Format: `AVA-{MODEL}-{PARAMS}-{QUANT}.tar`

| AVA Filename | Original Model | Size | Language |
|--------------|----------------|------|----------|
| **AVA-GEM-2B-Q4.tar** | Gemma 2B IT Q4 | ~2 GB | English |
| **AVA-G3M-4B-Q4.tar** | Gemma 3 4B Q4 | ~2.5 GB | English (higher quality) |
| **AVA-FLOR-1B-Q4.tar** | FLOR 1.3B | ~1.3 GB | Spanish |
| **AVA-CROI-1B-Q4.tar** | Croissant 1.3B | ~1.3 GB | French |
| **AVA-RINN-3B-Q4.tar** | Rinna 3.6B | ~3 GB | Japanese |
| **AVA-QWEN-2B-Q4.tar** | Qwen 1.8B | ~1.8 GB | Chinese |
| **AVA-TUCA-1B-Q4.tar** | Tucano 1.1B | ~1.1 GB | Portuguese |

### Model Type Codes

| Code | Model Family |
|------|-------------|
| **GEM** | Google Gemma 2.x |
| **G3M** | Google Gemma 3.x |
| **FLOR** | Spanish FLOR |
| **CROI** | French Croissant |
| **RINN** | Japanese Rinna |
| **QWEN** | Chinese Qwen |
| **TUCA** | Portuguese Tucano |

---

## Cloud Storage

### Download URLs

| Environment | Base URL |
|-------------|----------|
| **Release** | `https://www.augmentalis.com/avanuevoiceosava/ava/llm/` |
| **Debug** | `http://fs.dilonline.in/avanue_files/ava/llm/` |

### Server Structure

```
avanuevoiceosava/ava/llm/
├── manifest.json
├── en/
│   ├── AVA-GEM-2B-Q4.tar
│   └── AVA-G3M-4B-Q4.tar
├── es/
│   └── AVA-FLOR-1B-Q4.tar
├── fr/
│   └── AVA-CROI-1B-Q4.tar
├── ja/
│   └── AVA-RINN-3B-Q4.tar
├── zh/
│   └── AVA-QWEN-2B-Q4.tar
└── pt/
    └── AVA-TUCA-1B-Q4.tar
```

### Manifest Format

```json
{
    "version": "1.0.0",
    "languages": {
        "en": {
            "default": {
                "model": "AVA-GEM-2B-Q4.tar",
                "size": 2147483648,
                "checksum": "sha256:abc123..."
            },
            "quality": {
                "model": "AVA-G3M-4B-Q4.tar",
                "size": 2684354560,
                "checksum": "sha256:def456..."
            }
        },
        "es": {
            "default": {
                "model": "AVA-FLOR-1B-Q4.tar",
                "size": 1395864371
            }
        }
    },
    "minAppVersion": "1.0.0"
}
```

---

## Local Storage

### Device Paths

**Shared AVA folder (recommended):**
```
/storage/emulated/0/.ava/llm/
├── en/
│   └── AVA-GEM-2B-Q4.tar
└── manifest.json
```

**App-specific:**
```
context.filesDir/models/llm/
└── en/
    └── AVA-GEM-2B-Q4.tar
```

---

## Language Pack Manager

### LLMLanguagePackManager

```kotlin
class LLMLanguagePackManager(private val context: Context) {

    private val sharedPath = File(
        Environment.getExternalStorageDirectory(),
        ".ava/llm"
    )

    /**
     * Get list of installed languages
     */
    fun getInstalledLanguages(): List<String> {
        return sharedPath.listFiles()
            ?.filter { it.isDirectory && hasValidModel(it) }
            ?.map { it.name }
            ?: emptyList()
    }

    /**
     * Check if language is installed
     */
    fun isLanguageInstalled(languageCode: String): Boolean {
        val langDir = File(sharedPath, languageCode)
        return hasValidModel(langDir)
    }

    /**
     * Get model path for language
     */
    fun getModelPath(languageCode: String): String? {
        val langDir = File(sharedPath, languageCode)
        val tarFile = langDir.listFiles()
            ?.find { it.name.endsWith(".tar") && it.name.startsWith("AVA-") }

        return tarFile?.absolutePath
    }

    /**
     * Get model name for language (from config)
     */
    fun getModelName(languageCode: String): String {
        return when (languageCode) {
            "en" -> "AVA-GEM-2B-Q4"
            "es" -> "AVA-FLOR-1B-Q4"
            "fr" -> "AVA-CROI-1B-Q4"
            "de" -> "AVA-LEO-7B-Q4"
            "ja" -> "AVA-RINN-3B-Q4"
            "zh" -> "AVA-QWEN-2B-Q4"
            "pt" -> "AVA-TUCA-1B-Q4"
            else -> "AVA-GEM-2B-Q4" // Fallback to English
        }
    }

    /**
     * Download language pack
     */
    suspend fun downloadLanguage(
        languageCode: String,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {

        try {
            val langDir = File(sharedPath, languageCode)
            langDir.mkdirs()

            val modelName = getModelName(languageCode)
            val baseUrl = if (BuildConfig.DEBUG) {
                "http://fs.dilonline.in/avanue_files/ava/llm/"
            } else {
                "https://www.augmentalis.com/avanuevoiceosava/ava/llm/"
            }

            val url = "$baseUrl$languageCode/$modelName.tar"
            val destination = File(langDir, "$modelName.tar")

            downloadFile(url, destination, onProgress)

            Result.Success(Unit)

        } catch (e: Exception) {
            Result.Error(
                message = "Failed to download LLM for $languageCode: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Delete language pack to free storage
     */
    fun deleteLanguage(languageCode: String): Result<Unit> {
        val langDir = File(sharedPath, languageCode)
        return try {
            langDir.deleteRecursively()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete: ${e.message}")
        }
    }

    /**
     * Get storage size for language
     */
    fun getLanguageSize(languageCode: String): Long {
        val langDir = File(sharedPath, languageCode)
        return langDir.walkTopDown().sumOf { it.length() }
    }

    private fun hasValidModel(langDir: File): Boolean {
        return langDir.listFiles()?.any {
            it.name.endsWith(".tar") && it.length() > MIN_MODEL_SIZE
        } == true
    }

    companion object {
        const val MIN_MODEL_SIZE = 100 * 1024 * 1024L // 100 MB minimum
    }
}
```

---

## ALC Engine (Adaptive LLM Coordinator)

### Core Engine

```kotlin
class ALCEngine(
    private val context: Context,
    private val languagePackManager: LLMLanguagePackManager
) {
    private val mutex = Mutex()
    private var currentLanguage = "en"
    private var currentEngine: ALCEngineSingleLanguage? = null

    /**
     * Initialize with default language (English)
     */
    suspend fun initialize(): Result<Unit> {
        return switchLanguage("en")
    }

    /**
     * Switch to different language
     */
    suspend fun switchLanguage(languageCode: String): Result<Unit> = mutex.withLock {
        Timber.d("Switching LLM to: $languageCode")

        // Check if language pack is installed
        if (!languagePackManager.isLanguageInstalled(languageCode)) {
            return Result.Error("Language pack not installed: $languageCode")
        }

        // Don't switch if already active
        if (languageCode == currentLanguage && currentEngine != null) {
            return Result.Success(Unit)
        }

        // Cleanup current model (free 1-7 GB)
        currentEngine?.cleanup()

        // Load new model
        val modelPath = languagePackManager.getModelPath(languageCode)
            ?: return Result.Error("Model path not found")

        val engine = ALCEngineSingleLanguage(context)
        val result = engine.initialize(modelPath)

        if (result is Result.Success) {
            currentLanguage = languageCode
            currentEngine = engine
            Timber.i("Switched to LLM: $languageCode")
        }

        return result
    }

    /**
     * Chat with current model
     */
    fun chat(
        messages: List<ChatMessage>,
        options: GenerationOptions = GenerationOptions()
    ): Flow<LLMResponse> {
        return currentEngine?.chat(messages, options)
            ?: flow { emit(LLMResponse.Error("Engine not initialized")) }
    }

    /**
     * Stop generation
     */
    suspend fun stop() {
        currentEngine?.stop()
    }

    /**
     * Cleanup all resources
     */
    suspend fun cleanup() = mutex.withLock {
        currentEngine?.cleanup()
        currentEngine = null
    }
}
```

### Generation Options

```kotlin
data class GenerationOptions(
    val maxTokens: Int = 256,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val stopSequences: List<String> = emptyList()
)
```

### Chat Message

```kotlin
data class ChatMessage(
    val role: Role,
    val content: String
) {
    enum class Role {
        SYSTEM, USER, ASSISTANT
    }
}
```

---

## Memory Management

### Only One Model at a Time

```kotlin
class LLMMemoryManager {
    /**
     * Critical: Only one LLM can be loaded at a time
     * Each model is 1-7 GB in memory
     */

    suspend fun ensureMemoryAvailable(requiredMB: Long): Boolean {
        val runtime = Runtime.getRuntime()
        val freeMemory = runtime.freeMemory() / (1024 * 1024)
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        val availableMemory = maxMemory - (runtime.totalMemory() - freeMemory) / (1024 * 1024)

        if (availableMemory < requiredMB) {
            // Force garbage collection
            System.gc()
            delay(100)

            // Check again
            val newAvailable = runtime.freeMemory() / (1024 * 1024)
            return newAvailable >= requiredMB
        }

        return true
    }
}
```

### Auto-Unload on Inactivity

```kotlin
class ALCEngineWithTimeout(context: Context) {
    private var unloadJob: Job? = null

    fun startUnloadTimer() {
        unloadJob?.cancel()
        unloadJob = scope.launch {
            delay(5.minutes)
            if (!isActive) {
                cleanup()
                Timber.i("LLM unloaded due to inactivity")
            }
        }
    }

    fun resetUnloadTimer() {
        unloadJob?.cancel()
        startUnloadTimer()
    }
}
```

---

## Download UI

### Language Selection Screen

```kotlin
@Composable
fun LLMLanguageScreen(viewModel: LLMLanguageViewModel) {
    val installedLanguages by viewModel.installedLanguages.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    Column {
        Text("Language Models", style = MaterialTheme.typography.h5)

        // Installed
        Text("Installed", style = MaterialTheme.typography.subtitle1)
        installedLanguages.forEach { lang ->
            LanguageRow(
                language = lang,
                size = viewModel.getSize(lang.code),
                onDelete = { viewModel.deleteLanguage(lang.code) }
            )
        }

        Divider()

        // Available for download
        Text("Available", style = MaterialTheme.typography.subtitle1)
        availableLanguages.forEach { lang ->
            LanguageRow(
                language = lang,
                downloadProgress = downloadProgress[lang.code],
                onDownload = { viewModel.downloadLanguage(lang.code) }
            )
        }
    }
}

@Composable
fun LanguageRow(
    language: LanguageInfo,
    size: String? = null,
    downloadProgress: Float? = null,
    onDelete: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(language.name)
            Text(
                "${language.modelName} • ${size ?: language.downloadSize}",
                style = MaterialTheme.typography.caption
            )
        }

        when {
            downloadProgress != null -> {
                CircularProgressIndicator(progress = downloadProgress)
            }
            onDelete != null -> {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete")
                }
            }
            onDownload != null -> {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, "Download")
                }
            }
        }
    }
}
```

---

## Configuration

### Master Config

In `.ava/ASR/config.json`:

```json
{
    "llm": {
        "baseUrl": {
            "release": "https://www.augmentalis.com/avanuevoiceosava/ava/llm/",
            "debug": "http://fs.dilonline.in/avanue_files/ava/llm/"
        },
        "models": {
            "en": {
                "default": "AVA-GEM-2B-Q4.tar",
                "quality": "AVA-G3M-4B-Q4.tar"
            },
            "es": {
                "default": "AVA-FLOR-1B-Q4.tar"
            },
            "fr": {
                "default": "AVA-CROI-1B-Q4.tar"
            },
            "ja": {
                "default": "AVA-RINN-3B-Q4.tar"
            },
            "zh": {
                "default": "AVA-QWEN-2B-Q4.tar"
            }
        },
        "defaultLanguage": "en",
        "unloadTimeoutMinutes": 5
    }
}
```

---

## Error Handling

```kotlin
sealed class LLMError : Exception() {
    object LanguageNotInstalled : LLMError()
    object ModelLoadFailed : LLMError()
    object OutOfMemory : LLMError()
    object GenerationFailed : LLMError()

    data class DownloadFailed(
        val languageCode: String,
        override val message: String
    ) : LLMError()
}
```

---

## Performance

### Benchmarks

| Operation | Time | Notes |
|-----------|------|-------|
| Model load | 3-8s | One-time per language switch |
| First token | 200-500ms | Cold start |
| Token generation | 20-50ms | Per token |
| 100 token response | 2-5s | Typical response |

### Optimization Tips

1. **Preload on WiFi** - Download models when on WiFi
2. **Keep loaded** - Don't unload if user is active
3. **Smaller models** - Use 2B instead of 7B when possible
4. **Q4 quantization** - Best size/quality trade-off

---

## Testing

```kotlin
@Test
fun `LLM switches language correctly`() = runTest {
    val engine = ALCEngine(context, languagePackManager)
    engine.initialize()

    // Switch to Spanish
    val result = engine.switchLanguage("es")
    assertTrue(result is Result.Success)

    // Generate in Spanish
    val response = engine.chat(listOf(
        ChatMessage(Role.USER, "Hola, como estas?")
    )).first()

    assertTrue(response is LLMResponse.Token)
}

@Test
fun `LLM unloads after timeout`() = runTest {
    val engine = ALCEngineWithTimeout(context)
    engine.initialize()

    // Wait for timeout
    advanceTimeBy(6.minutes)

    // Should be unloaded
    assertNull(engine.currentEngine)
}
```

---

## Related Documents

- ADR-VoiceOS-AVA-Architecture-Integration-251118.md
- Chapter-ASR-Engine-Architecture.md
- Chapter-NLU-Integration.md
- AVA-MODEL-NAMING-REGISTRY.md

---

**Document Status:** Draft
**Review Status:** Pending
**Next Steps:** Technical review, API stabilization
