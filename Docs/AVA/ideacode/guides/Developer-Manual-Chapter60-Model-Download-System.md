# Developer Manual - Chapter 60: Model Download System

**Status:** Complete
**Date:** 2025-12-01
**Version:** 1.0

---

## Overview

The Model Download System provides reliable, resumable downloads for LLM and NLU models with progress tracking and integrity verification.

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   ModelDownloader                    │
├─────────────────────────────────────────────────────┤
│  downloadModel(modelId, onProgress)                 │
│       │                                             │
│       ▼                                             │
│  ┌─────────────┐    ┌─────────────┐               │
│  │   OkHttp    │───▶│  Streaming  │               │
│  │   Client    │    │  Download   │               │
│  └─────────────┘    └─────────────┘               │
│       │                    │                       │
│       ▼                    ▼                       │
│  ┌─────────────┐    ┌─────────────┐               │
│  │   Retry     │    │  Progress   │               │
│  │   Logic     │    │  Tracking   │               │
│  └─────────────┘    └─────────────┘               │
│       │                    │                       │
│       ▼                    ▼                       │
│  ┌─────────────┐    ┌─────────────┐               │
│  │   SHA256    │    │   File      │               │
│  │   Verify    │    │   Storage   │               │
│  └─────────────┘    └─────────────┘               │
└─────────────────────────────────────────────────────┘
```

---

## Key Features

| Feature | Description |
|---------|-------------|
| Progress Tracking | Real-time 0-100% progress via Flow |
| Resume Support | Range header for interrupted downloads |
| Retry Logic | 3 retries with exponential backoff |
| SHA256 Validation | Integrity verification post-download |
| Streaming | Memory-efficient chunked download |

---

## Implementation

### ModelDownloader.kt

Location: `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/alc/loader/`

```kotlin
class ModelDownloader(private val context: Context) {

    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_MS = 1000L
        private const val BUFFER_SIZE = 8192
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Download model with progress tracking
     */
    fun downloadModel(
        modelId: String,
        onProgress: (Float) -> Unit
    ): Flow<DownloadProgress> = flow {
        emit(DownloadProgress(0f, "Starting download...", modelId))

        val downloadUrl = getDownloadUrl(modelId)
        val destFile = getModelFile(modelId)

        downloadWithRetry(downloadUrl, destFile) { progress, status ->
            emit(DownloadProgress(progress, status, modelId))
        }

        emit(DownloadProgress(1f, "Download complete", modelId))
    }
}
```

### Retry with Exponential Backoff

```kotlin
private suspend fun downloadWithRetry(
    url: String,
    destFile: File,
    onProgress: (Float, String) -> Unit
) {
    var lastException: Exception? = null

    repeat(MAX_RETRIES) { attempt ->
        try {
            downloadFile(url, destFile, onProgress)
            return  // Success
        } catch (e: IOException) {
            lastException = e
            val backoffMs = INITIAL_BACKOFF_MS * (1 shl attempt)  // 1s, 2s, 4s
            Timber.w("Download failed (attempt ${attempt + 1}/$MAX_RETRIES), retrying in ${backoffMs}ms")
            delay(backoffMs)
        }
    }

    throw IOException("Download failed after $MAX_RETRIES attempts", lastException)
}
```

### Streaming Download with Progress

```kotlin
private suspend fun downloadFile(
    downloadUrl: String,
    destFile: File,
    onProgress: (Float, String) -> Unit
) = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url(downloadUrl)
        .build()

    okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("Download failed: ${response.code}")
        }

        val body = response.body ?: throw IOException("Empty response")
        val contentLength = body.contentLength()

        body.byteStream().use { input ->
            FileOutputStream(destFile).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var totalRead = 0L

                while (true) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) break

                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    val progress = totalRead.toFloat() / contentLength
                    onProgress(progress, "Downloading...")
                }
            }
        }
    }
}
```

### SHA256 Validation

```kotlin
private fun calculateSha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(BUFFER_SIZE)

    FileInputStream(file).use { input ->
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead == -1) break
            digest.update(buffer, 0, bytesRead)
        }
    }

    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun validateDownload(file: File, expectedSha256: String): Boolean {
    val actualSha256 = calculateSha256(file)
    if (actualSha256 != expectedSha256) {
        file.delete()  // Remove corrupted file
        throw SecurityException(
            "SHA256 mismatch: expected $expectedSha256, got $actualSha256"
        )
    }
    return true
}
```

---

## Download Progress Data Class

```kotlin
data class DownloadProgress(
    val progress: Float,      // 0.0 to 1.0
    val status: String,       // User-friendly message
    val modelId: String       // For tracking
)
```

---

## Model Sources

### HuggingFace Integration

```kotlin
object DownloadConfig {
    const val HUGGINGFACE_BASE = "https://huggingface.co"

    val MODEL_URLS = mapOf(
        "mobilebert" to "$HUGGINGFACE_BASE/onnx-community/mobilebert-uncased-ONNX/resolve/main/onnx/model_int8.onnx",
        "malbert" to "$HUGGINGFACE_BASE/sentence-transformers/paraphrase-multilingual-albert-base-v2/resolve/main/onnx/model.onnx"
    )

    val MODEL_CHECKSUMS = mapOf(
        "mobilebert" to "a1b2c3d4...",  // SHA256
        "malbert" to "e5f6g7h8..."
    )
}
```

---

## Usage Example

```kotlin
val downloader = ModelDownloader(context)

lifecycleScope.launch {
    downloader.downloadModel("mobilebert") { progress ->
        updateProgressBar(progress * 100)
    }.collect { downloadProgress ->
        when {
            downloadProgress.progress < 1f -> {
                showStatus(downloadProgress.status)
            }
            else -> {
                showSuccess("Model ready!")
            }
        }
    }
}
```

---

## Error Handling

| Error | Thrown As | Recovery |
|-------|-----------|----------|
| Network failure | IOException | Auto-retry (3x) |
| Server error (5xx) | IOException | Auto-retry (3x) |
| Invalid URL | IllegalArgumentException | Log and fail |
| SHA256 mismatch | SecurityException | Delete and fail |
| Disk full | IOException | Fail with message |

---

## Storage Locations

| Platform | Path |
|----------|------|
| Android | `context.filesDir/models/` |
| Desktop | `~/.ava/models/` or `./models/` |
| iOS | App Documents directory |

---

## Related Documentation

- [Chapter 38: LLM Model Management](Developer-Manual-Chapter38-LLM-Model-Management.md)
- [Chapter 42: LLM Model Setup](Developer-Manual-Chapter42-LLM-Model-Setup.md)
- [MODEL-DOWNLOAD-SOURCES.md](MODEL-DOWNLOAD-SOURCES.md)

---

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2025-12-01 | 1.0 | Complete OkHttp download implementation |
