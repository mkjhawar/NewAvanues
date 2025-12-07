# Developer Manual: ASR Engine Architecture

**Version:** 1.0
**Date:** 2025-11-18
**Author:** VoiceOS/AVA Architecture Team

---

## Overview

VoiceOS uses a cascading Automatic Speech Recognition (ASR) system that discovers and loads available engines at runtime. This architecture allows:

- Minimal APK size (no bundled models)
- Shared models between VoiceOS and AVA
- User choice of preferred engine
- Easy model updates without app updates

---

## ASR Engine Priority

VoiceOS attempts to load engines in this order:

| Priority | Engine | Type | Approx Size | Latency | Accuracy |
|----------|--------|------|-------------|---------|----------|
| 1 | **Vivoka** | Commercial | ~70 MB | 50-100ms | Excellent |
| 2 | **Android TTS** | System | 0 MB | 100-300ms | Good |
| 3 | **Vosk** | Open Source | ~50 MB | 100-300ms | Good |
| 4 | **Whisper** | Open Source | ~40 MB | 200-500ms | Excellent |
| 5 | **Google** | Cloud | 0 MB | 200-500ms | Excellent |

### Engine Characteristics

#### Vivoka (Recommended)
- Best overall accuracy and latency
- Commercial license required
- Supports streaming recognition
- Low memory footprint

#### Android TTS
- Uses device's built-in speech recognition
- Zero additional download
- Quality varies by device/manufacturer
- Not available on all devices

#### Vosk
- Open source, offline
- Good accuracy for commands
- Supports 20+ languages
- Larger vocabulary models available

#### Whisper
- OpenAI's multilingual model
- Excellent for natural speech
- Higher latency (not streaming)
- Best for dictation use cases

#### Google Cloud
- Network required
- Best overall accuracy
- Fallback when offline not possible
- Usage limits may apply

---

## Storage Architecture

### Shared Model Location

All ASR models are stored in a hidden folder accessible to both VoiceOS and AVA:

```
/storage/emulated/0/.ava/
└── ASR/
    ├── config.json           # Master configuration
    ├── vivoka/
    │   ├── en_us/
    │   │   ├── acoustic.bin
    │   │   ├── language.bin
    │   │   └── lexicon.bin
    │   ├── es_es/
    │   └── manifest.json
    ├── vosk/
    │   ├── en_us/
    │   │   └── model/
    │   └── manifest.json
    ├── whisper/
    │   ├── base/
    │   │   └── ggml-base.bin
    │   └── manifest.json
    └── google/
        └── manifest.json     # Config only
```

### Why Hidden Folder?

- `.ava` prefix hides from gallery/file browsers
- Survives app uninstall (user data)
- Shared between VoiceOS and AVA apps
- Single download serves both apps

### Permissions Required

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- For Android 11+ -->
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
```

---

## Discovery System

### ASRDiscoveryManager

The central class that manages engine discovery and initialization:

```kotlin
class ASRDiscoveryManager(private val context: Context) {

    private val asrBasePath = File(
        Environment.getExternalStorageDirectory(),
        ".ava/ASR"
    )

    /**
     * Discover and return best available ASR engine
     */
    suspend fun discoverASREngine(): ASREngine {
        val engines = listOf(
            ASREngineType.VIVOKA,
            ASREngineType.ANDROID_TTS,
            ASREngineType.VOSK,
            ASREngineType.WHISPER,
            ASREngineType.GOOGLE
        )

        for (engineType in engines) {
            val engine = checkEngine(engineType)
            if (engine != null) {
                Timber.i("ASR engine found: $engineType")
                return engine
            }
        }

        // No engine available - prompt for download
        return promptForDownload()
    }

    /**
     * Check if specific engine is available
     */
    fun isEngineAvailable(type: ASREngineType): Boolean {
        return checkEngine(type) != null
    }

    /**
     * Get all available engines
     */
    fun getAvailableEngines(): List<ASREngineType> {
        return ASREngineType.values().filter { isEngineAvailable(it) }
    }
}
```

### Vivoka Configuration Format

Vivoka uses a detailed configuration JSON that defines recognizers and models:

```json
{
  "version": "2.0",
  "csdk": {
    "log": {
      "cache": {
        "enabled": false
      }
    },
    "asr": {
      "recognizers": {
        "rec": {
          "acmods": [
            "acmod6_6000_enu_gen_car_f16_v1_0_1.dat"
          ]
        }
      },
      "models": {
        "english-dictation": {
          "acmod": "acmod6_6000_enu_gen_car_f16_v1_0_1.dat",
          "extra_models": {
            "LM1": "lm-navigation_enu_vocon_car_restricted.dat",
            "LM2": "lm-assistant_enu_vocon_car_restricted.dat"
          },
          "file": "ctx_enu_vocon_car.fcf",
          "type": "free-speech"
        },
        "asrenu-US": {
          "file": "asrenu-US.fcf",
          "type": "dynamic",
          "acmod": "acmod6_6000_enu_gen_car_f16_v1_0_1.dat",
          "settings": {
            "LH_SEARCH_PARAM_MAXNBEST": 1,
            "LH_SEARCH_PARAM_TSILENCE": 600
          },
          "slots": {
            "itemName": {
              "slot": "ASRENU-US#item",
              "category": "name",
              "allow_custom_phonetic": true
            }
          },
          "lexicon": {
            "clc": "clc_enu_cfg3_v6_5_000000.dat"
          }
        }
      }
    }
  }
}
```

**Key configuration sections:**
- `recognizers.rec.acmods` - Acoustic model files
- `models.*.type` - "free-speech" (dictation) or "dynamic" (grammar-based)
- `models.*.extra_models` - Additional language models for domains
- `models.*.slots` - Dynamic vocabulary slots
- `models.*.lexicon.clc` - Phonetic/lexical data

### Vivoka Folder Structure

After download and extraction:

```
context.filesDir/vsdk/data/csdk/asr/
├── acmod/    # Acoustic models (grammar & free speech)
│   └── acmod6_6000_enu_gen_car_f16_v1_0_1.dat
├── clc/      # Language components (dynamic grammar only)
│   └── clc_enu_cfg3_v6_5_000000.dat
├── ctx/      # Context files (.fcf)
│   ├── ctx_enu_vocon_car.fcf
│   └── asrenu-US.fcf
└── lm/       # Language models (free speech only)
    ├── lm-navigation_enu_vocon_car_restricted.dat
    └── lm-assistant_enu_vocon_car_restricted.dat
```

### Engine Validation

Validation checks:
1. Configuration JSON exists and is valid
2. All referenced `.dat` and `.fcf` files present
3. Acoustic model matches specified file
4. Required slots defined for dynamic grammar

---

## Download System

### First-Run Flow

When no engine is available:

1. **Permission Request**
   - Request storage permission
   - Explain why it's needed
   - Handle denial gracefully

2. **Engine Selection Dialog**
   - Show available engines
   - Display size, features, quality
   - Default: Vivoka en_us
   - Allow language selection

3. **Download Progress**
   - Show download progress
   - Support pause/resume
   - Handle network errors
   - Verify checksum after download

4. **Extraction & Validation**
   - Extract to ASR folder
   - Validate manifest
   - Initialize engine
   - Ready to use

### Download Configuration

#### Firebase Remote Config Integration

VoiceOS uses Firebase Remote Config to dynamically fetch download URLs without app updates:

```kotlin
class FirebaseRemoteConfigRepository(private val context: Context) {

    private val remoteConfig = Firebase.remoteConfig

    suspend fun init() {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600 // 1 hour cache
        }
        remoteConfig.setConfigSettingsAsync(configSettings).await()
        remoteConfig.fetchAndActivate().await()
    }

    fun getLanguageResourceUrl(languageId: String): String {
        val paramName = "${languageId}_voice_resource"
        val debugSuffix = if (BuildConfig.DEBUG) "_debug" else ""
        return remoteConfig.getString(paramName + debugSuffix)
    }

    fun getLanguageConfigUrl(languageId: String): String {
        val paramName = "${languageId}_json"
        val debugSuffix = if (BuildConfig.DEBUG) "_debug" else ""
        return remoteConfig.getString(paramName + debugSuffix)
    }
}
```

#### Remote Config Parameters

| Parameter | Description | Example URL |
|-----------|-------------|-------------|
| `es_json` | Spanish config (release) | `https://www.augmentalis.com/avanuevoiceosava/Spanish/spanish.json` |
| `es_voice_resource` | Spanish resources (release) | `https://www.augmentalis.com/avanuevoiceosava/Spanish/es_voice_resource.zip` |
| `es_json_debug` | Spanish config (debug) | `http://fs.dilonline.in/avanue_files/spanish.json` |
| `es_voice_resource_debug` | Spanish resources (debug) | `http://fs.dilonline.in/avanue_files/es_voice_resource.zip` |

#### Server Authentication (Release)

Release server requires basic auth:

```kotlin
private fun createAuthenticatedConnection(url: String): HttpURLConnection {
    val connection = URL(url).openConnection() as HttpURLConnection

    // Only for release server
    if (url.contains("augmentalis.com")) {
        val auth = "avanuevoiceos:!AvA\$Avanue123#"
        val encodedAuth = Base64.encodeToString(auth.toByteArray(), Base64.NO_WRAP)
        connection.setRequestProperty("Authorization", "Basic $encodedAuth")
    }

    return connection
}
```

#### Master Config File

Local config at `.ava/ASR/config.json`:

```json
{
    "version": "1.0",
    "defaultEngine": "vivoka",
    "defaultLanguage": "en_us",
    "downloadUrls": {
        "vivoka": {
            "baseUrl": {
                "release": "https://www.augmentalis.com/avanuevoiceosava/",
                "debug": "http://fs.dilonline.in/avanue_files/"
            },
            "languages": {
                "en_us": {
                    "config": "English/english.json",
                    "resources": "English/en_voice_resource.zip"
                },
                "es_es": {
                    "config": "Spanish/spanish.json",
                    "resources": "Spanish/es_voice_resource.zip"
                },
                "fr_fr": {
                    "config": "French/french.json",
                    "resources": "French/fr_voice_resource.zip"
                },
                "ja_jp": {
                    "config": "Japanese/japanese.json",
                    "resources": "Japanese/jp_voice_resource.zip"
                }
            }
        },
        "vosk": {
            "baseUrl": "https://alphacephei.com/vosk/models/",
            "languages": {
                "en_us": "vosk-model-small-en-us-0.15.zip",
                "es_es": "vosk-model-small-es-0.42.zip"
            }
        },
        "whisper": {
            "baseUrl": "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/",
            "models": {
                "base": "ggml-base.bin",
                "small": "ggml-small.bin"
            }
        }
    },
    "enginePriority": ["vivoka", "android_tts", "vosk", "whisper", "google"]
}
```

### Download Manager

```kotlin
class ASRDownloadManager(private val context: Context) {

    suspend fun downloadEngine(
        engine: ASREngineType,
        language: String,
        onProgress: (Float) -> Unit
    ): Result<File> {

        // Get download URL from config
        val url = getDownloadUrl(engine, language)
            ?: return Result.Error("Download URL not found")

        // Download with progress
        val tempFile = downloadFile(url, onProgress)

        // Verify checksum
        if (!verifyChecksum(tempFile, engine, language)) {
            tempFile.delete()
            return Result.Error("Checksum verification failed")
        }

        // Extract to ASR folder
        val targetDir = File(asrBasePath, engine.folderName)
        extractZip(tempFile, targetDir)
        tempFile.delete()

        return Result.Success(targetDir)
    }

    private suspend fun downloadFile(
        url: String,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {

        val tempFile = File(context.cacheDir, "asr_download.zip")
        val connection = URL(url).openConnection() as HttpURLConnection

        connection.connectTimeout = 30000
        connection.readTimeout = 30000

        val fileSize = connection.contentLength
        val input = connection.inputStream
        val output = FileOutputStream(tempFile)

        val buffer = ByteArray(8192)
        var totalRead = 0L
        var bytesRead: Int

        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
            totalRead += bytesRead
            if (fileSize > 0) {
                onProgress(totalRead.toFloat() / fileSize)
            }
        }

        output.close()
        input.close()

        tempFile
    }
}
```

---

## Engine Interface

All ASR engines implement a common interface:

```kotlin
interface ASREngine {

    val type: ASREngineType
    val isStreaming: Boolean
    val supportedLanguages: List<String>

    /**
     * Initialize the engine
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Start listening for speech
     */
    suspend fun startListening(
        language: String = "en_us",
        onPartialResult: (String) -> Unit = {},
        onFinalResult: (String) -> Unit
    )

    /**
     * Stop listening
     */
    suspend fun stopListening()

    /**
     * Check if currently listening
     */
    fun isListening(): Boolean

    /**
     * Release resources
     */
    suspend fun release()
}
```

### Engine Implementations

#### VivokaEngine

```kotlin
class VivokaEngine(private val modelPath: File) : ASREngine {

    override val type = ASREngineType.VIVOKA
    override val isStreaming = true
    override val supportedLanguages = listOf("en_us", "es_es", "fr_fr")

    private var vivokaInstance: VivokaASR? = null

    override suspend fun initialize(): Result<Unit> {
        return try {
            vivokaInstance = VivokaASR.create(modelPath.absolutePath)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to initialize Vivoka: ${e.message}")
        }
    }

    override suspend fun startListening(
        language: String,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit
    ) {
        vivokaInstance?.startRecognition(
            language = language,
            partialCallback = onPartialResult,
            finalCallback = onFinalResult
        )
    }

    // ... other methods
}
```

#### VoskEngine

```kotlin
class VoskEngine(private val modelPath: File) : ASREngine {

    override val type = ASREngineType.VOSK
    override val isStreaming = true

    private var model: Model? = null
    private var recognizer: KaldiRecognizer? = null

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            model = Model(modelPath.absolutePath)
            recognizer = KaldiRecognizer(model, 16000.0f)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to initialize Vosk: ${e.message}")
        }
    }

    // ... other methods
}
```

---

## Integration with VoiceOS

### SpeechRecognitionManager

Central manager that uses ASRDiscoveryManager:

```kotlin
class SpeechRecognitionManager(
    private val context: Context,
    private val discoveryManager: ASRDiscoveryManager
) {
    private var currentEngine: ASREngine? = null

    suspend fun initialize(): Result<Unit> {
        // Discover best available engine
        val engine = discoveryManager.discoverASREngine()

        // Initialize it
        val result = engine.initialize()
        if (result is Result.Success) {
            currentEngine = engine
            Timber.i("Initialized ASR engine: ${engine.type}")
        }

        return result
    }

    suspend fun startListening(
        onResult: (String) -> Unit
    ) {
        currentEngine?.startListening(
            onFinalResult = onResult
        ) ?: throw IllegalStateException("ASR not initialized")
    }

    /**
     * Switch to different engine
     */
    suspend fun switchEngine(type: ASREngineType): Result<Unit> {
        if (!discoveryManager.isEngineAvailable(type)) {
            return Result.Error("Engine not available: $type")
        }

        // Release current engine
        currentEngine?.release()

        // Initialize new engine
        val engine = discoveryManager.getEngine(type)
        val result = engine.initialize()

        if (result is Result.Success) {
            currentEngine = engine
        }

        return result
    }
}
```

---

## Settings UI

### Engine Selection Screen

Allow users to:
- View installed engines
- Download new engines/languages
- Set preferred engine
- Manage storage (delete unused)

```kotlin
@Composable
fun ASRSettingsScreen(
    viewModel: ASRSettingsViewModel
) {
    val installedEngines by viewModel.installedEngines.collectAsState()
    val currentEngine by viewModel.currentEngine.collectAsState()

    Column {
        Text("Speech Recognition Engine")

        // Installed engines
        installedEngines.forEach { engine ->
            EngineRow(
                engine = engine,
                isSelected = engine.type == currentEngine,
                onSelect = { viewModel.selectEngine(engine.type) },
                onDelete = { viewModel.deleteEngine(engine.type) }
            )
        }

        // Download more
        Button(onClick = { viewModel.showDownloadDialog() }) {
            Text("Download Engine")
        }
    }
}
```

---

## Error Handling

### Common Errors

```kotlin
sealed class ASRError : Exception() {

    object NoEngineAvailable : ASRError()
    object PermissionDenied : ASRError()

    data class DownloadFailed(
        override val message: String
    ) : ASRError()

    data class InitializationFailed(
        val engine: ASREngineType,
        override val message: String
    ) : ASRError()

    data class RecognitionFailed(
        override val message: String
    ) : ASRError()
}
```

### Fallback Strategy

If primary engine fails, automatically fall to next in priority:

```kotlin
suspend fun recognizeWithFallback(audio: ByteArray): String {
    val engines = discoveryManager.getAvailableEngines()

    for (engineType in engines) {
        try {
            val result = recognize(engineType, audio)
            if (result.isNotEmpty()) {
                return result
            }
        } catch (e: ASRError.RecognitionFailed) {
            Timber.w("Engine $engineType failed, trying next")
            continue
        }
    }

    throw ASRError.NoEngineAvailable
}
```

---

## Testing

### Unit Tests

```kotlin
@Test
fun `discovery finds Vivoka when available`() {
    // Setup: Create vivoka manifest
    val vivokaDir = File(testAsrPath, "vivoka")
    vivokaDir.mkdirs()
    File(vivokaDir, "manifest.json").writeText(validManifest)

    // Test
    val engine = discoveryManager.discoverASREngine()

    // Verify
    assertEquals(ASREngineType.VIVOKA, engine.type)
}

@Test
fun `discovery falls back to Vosk when Vivoka missing`() {
    // Setup: Only Vosk available
    val voskDir = File(testAsrPath, "vosk")
    voskDir.mkdirs()
    File(voskDir, "manifest.json").writeText(validVoskManifest)

    // Test
    val engine = discoveryManager.discoverASREngine()

    // Verify
    assertEquals(ASREngineType.VOSK, engine.type)
}
```

### Integration Tests

```kotlin
@Test
fun `download and initialize Vosk engine`() = runTest {
    // Download
    val result = downloadManager.downloadEngine(
        ASREngineType.VOSK,
        "en_us",
        onProgress = {}
    )
    assertTrue(result is Result.Success)

    // Verify files exist
    assertTrue(File(asrPath, "vosk/en_us/model").exists())

    // Initialize
    val engine = VoskEngine(File(asrPath, "vosk/en_us"))
    val initResult = engine.initialize()
    assertTrue(initResult is Result.Success)
}
```

---

## Performance Considerations

### Memory Management

- Only one engine loaded at a time
- Unload when switching engines
- Clear cache after extended idle

### Battery Optimization

- Use streaming engines when available
- Reduce sample rate for simple commands
- Batch non-urgent recognition

### Storage Management

- Allow users to delete unused engines
- Show storage usage per engine
- Warn when storage low

---

## Migration Guide

### From Bundled Vosk

If migrating from APK-bundled Vosk:

1. Check if `.ava/ASR/vosk` exists
2. If not, copy from assets to shared location
3. Delete from assets in next release
4. APK size reduces by ~50 MB

```kotlin
suspend fun migrateFromBundledVosk() {
    val sharedVosk = File(asrBasePath, "vosk")

    if (!sharedVosk.exists()) {
        // Copy from assets
        context.assets.open("vosk-model-en-us.zip").use { input ->
            extractZip(input, sharedVosk)
        }
        Timber.i("Migrated Vosk to shared location")
    }
}
```

---

## Appendix

### Engine Type Enum

```kotlin
enum class ASREngineType(
    val folderName: String,
    val displayName: String,
    val isCloud: Boolean
) {
    VIVOKA("vivoka", "Vivoka", false),
    ANDROID_TTS("android_tts", "System", false),
    VOSK("vosk", "Vosk", false),
    WHISPER("whisper", "Whisper", false),
    GOOGLE("google", "Google Cloud", true)
}
```

### Related Documents

- ADR-VoiceOS-AVA-Architecture-Integration-251118.md
- Chapter-Plugin-Architecture.md (TBD)
- Chapter-NLU-Integration.md (TBD)

---

**Document Status:** Draft
**Review Status:** Pending
**Next Steps:** Technical review, API stabilization
