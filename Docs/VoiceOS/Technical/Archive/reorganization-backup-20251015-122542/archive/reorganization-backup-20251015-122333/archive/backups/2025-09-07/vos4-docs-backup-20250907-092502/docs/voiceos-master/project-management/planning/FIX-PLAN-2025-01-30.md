# VOS4 Build Fix Implementation Plan
**Version:** 1.0  
**Date:** January 30, 2025  
**Priority:** CRITICAL  
**Timeline:** 2 Weeks

## Executive Summary

This document outlines a structured approach to resolve remaining build issues in the VOS4 project, with focus on memory optimization, dependency management, and compilation error resolution. The plan prioritizes keeping Vivoka for testing while making Vosk downloadable on-demand.

---

## Phase 1: Critical Memory Optimization (Days 1-3)

### 1.1 Implement Vosk Download Manager

#### Component: VoskDownloadManager.kt
**Location:** `libraries/SpeechRecognition/src/main/java/com/augmentalis/speechrecognition/download/`

```kotlin
package com.augmentalis.speechrecognition.download

import android.content.Context
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import java.io.File
import java.net.URL
import java.util.zip.ZipInputStream

class VoskDownloadManager(private val context: Context) {
    
    data class ModelInfo(
        val name: String,
        val language: String,
        val size: Long,
        val url: String,
        val checksum: String
    )
    
    private val modelsDir = File(context.filesDir, "vosk_models")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Default models repository
    var modelRepositoryUrl = mutableStateOf("https://alphacephei.com/vosk/models")
    var downloadProgress = mutableStateOf(0f)
    var isDownloading = mutableStateOf(false)
    
    suspend fun downloadModel(modelInfo: ModelInfo): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                isDownloading.value = true
                val modelDir = File(modelsDir, modelInfo.name)
                
                if (modelDir.exists() && verifyModel(modelDir, modelInfo.checksum)) {
                    return@withContext Result.success(modelDir)
                }
                
                // Download model
                val tempFile = File(context.cacheDir, "${modelInfo.name}.zip")
                downloadFile(modelInfo.url, tempFile)
                
                // Extract model
                extractZip(tempFile, modelDir)
                tempFile.delete()
                
                Result.success(modelDir)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                isDownloading.value = false
                downloadProgress.value = 0f
            }
        }
    }
    
    fun getAvailableModels(): List<ModelInfo> {
        // Predefined model list - can be fetched from server
        return listOf(
            ModelInfo(
                name = "vosk-model-small-en-us-0.15",
                language = "en-US",
                size = 40_000_000, // 40MB
                url = "${modelRepositoryUrl.value}/vosk-model-small-en-us-0.15.zip",
                checksum = "abc123..."
            ),
            ModelInfo(
                name = "vosk-model-en-us-0.22",
                language = "en-US", 
                size = 128_000_000, // 128MB
                url = "${modelRepositoryUrl.value}/vosk-model-en-us-0.22.zip",
                checksum = "def456..."
            )
        )
    }
    
    fun isModelDownloaded(modelName: String): Boolean {
        return File(modelsDir, modelName).exists()
    }
    
    fun deleteModel(modelName: String) {
        File(modelsDir, modelName).deleteRecursively()
    }
}
```

#### UI Component: VoskDownloadUI.kt
**Location:** `libraries/SpeechRecognition/src/main/java/com/augmentalis/speechrecognition/ui/`

```kotlin
@Composable
fun VoskModelDownloadScreen(
    downloadManager: VoskDownloadManager,
    onModelSelected: (File) -> Unit
) {
    var selectedModel by remember { mutableStateOf<ModelInfo?>(null) }
    var customUrl by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Custom URL Input
        OutlinedTextField(
            value = customUrl,
            onValueChange = { customUrl = it },
            label = { Text("Custom Model URL (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { 
                    downloadManager.modelRepositoryUrl.value = customUrl
                }) {
                    Icon(Icons.Default.Save, "Set URL")
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Available Models List
        Text(
            "Available Models",
            style = MaterialTheme.typography.headlineSmall
        )
        
        LazyColumn {
            items(downloadManager.getAvailableModels()) { model ->
                ModelCard(
                    model = model,
                    isDownloaded = downloadManager.isModelDownloaded(model.name),
                    onDownload = { 
                        selectedModel = model
                        // Start download
                    },
                    onDelete = {
                        downloadManager.deleteModel(model.name)
                    }
                )
            }
        }
        
        // Download Progress
        if (downloadManager.isDownloading.value) {
            LinearProgressIndicator(
                progress = downloadManager.downloadProgress.value,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

### 1.2 Remove Vosk from Build Dependencies

**File:** `libraries/SpeechRecognition/build.gradle.kts`

```kotlin
dependencies {
    // REMOVE these lines:
    // api("com.alphacephei:vosk-android:0.3.47")
    // implementation(project(":Vosk"))
    
    // ADD dynamic loading support:
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    
    // Keep Vivoka for testing
    compileOnly(files("../../Vivoka/vsdk-6.0.0.aar"))
    compileOnly(files("../../Vivoka/vsdk-csdk-asr-2.0.0.aar"))
    compileOnly(files("../../Vivoka/vsdk-csdk-core-1.0.1.aar"))
}
```

### 1.3 Remove Google Cloud Speech

**File:** `libraries/SpeechRecognition/build.gradle.kts`

```kotlin
dependencies {
    // REMOVE these lines:
    // implementation("com.google.cloud:google-cloud-speech:4.28.0")
    // implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
}
```

---

## Phase 2: Native Library Optimization (Days 3-5)

### 2.1 Configure ABI Filters

**File:** `app/build.gradle.kts` and all app modules

```kotlin
android {
    defaultConfig {
        ndk {
            // Only include ARM architectures for production
            abiFilters("arm64-v8a", "armeabi-v7a")
            // Remove "x86", "x86_64" unless needed for emulator
        }
    }
    
    // Split APKs by ABI for smaller downloads
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }
}
```

### 2.2 Implement Product Flavors

**File:** `libraries/SpeechRecognition/build.gradle.kts`

```kotlin
android {
    flavorDimensions("engine")
    productFlavors {
        create("vivoka") {
            dimension = "engine"
            buildConfigField("String", "SPEECH_ENGINE", "\"VIVOKA\"")
        }
        create("android") {
            dimension = "engine"
            buildConfigField("String", "SPEECH_ENGINE", "\"ANDROID\"")
        }
        create("dynamic") {
            dimension = "engine"
            buildConfigField("String", "SPEECH_ENGINE", "\"DYNAMIC\"")
        }
    }
}

dependencies {
    // Vivoka only for vivoka flavor
    "vivokaImplementation"(files("../../Vivoka/vsdk-6.0.0.aar"))
    "vivokaImplementation"(files("../../Vivoka/vsdk-csdk-asr-2.0.0.aar"))
    "vivokaImplementation"(files("../../Vivoka/vsdk-csdk-core-1.0.1.aar"))
    
    // Dynamic loading for dynamic flavor
    "dynamicImplementation"("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
}
```

---

## Phase 3: Fix VoiceUI Compilation Errors (Days 5-7)

### 3.1 Common Error Patterns & Fixes

#### Pattern 1: @Composable Outside Context
**Problem:** Functions calling @Composable from non-composable scope
**Fix:** Add @Composable annotation or move logic outside

```kotlin
// WRONG
fun buildUI() {
    Text("Hello") // Error: @Composable invocation
}

// CORRECT
@Composable
fun buildUI() {
    Text("Hello")
}
```

#### Pattern 2: Import Statements Mid-File
**Problem:** Import statements not at file beginning
**Fix:** Move all imports to top of file

#### Pattern 3: Type Mismatches in Theme
**Problem:** Custom types vs AndroidX types confusion
**Fix:** Use type aliases or consistent imports

### 3.2 Systematic Fix Approach

1. **Sort errors by file**
2. **Fix imports first**
3. **Add missing @Composable annotations**
4. **Resolve type mismatches**
5. **Fix DSL scope issues**

---

## Phase 4: Gradle Optimization (Days 7-9)

### 4.1 Enhanced Memory Settings

**File:** `gradle.properties`

```properties
# Optimized for large projects
org.gradle.jvmargs=-Xmx6g -XX:MaxMetaspaceSize=2g \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  --enable-native-access=ALL-UNNAMED

# Performance optimization
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn

# Kotlin specific
kotlin.incremental=true
kotlin.incremental.java=true
kotlin.caching.enabled=true
kotlin.parallel.tasks.in.project=true

# Android specific
android.useAndroidX=true
android.enableJetifier=false
android.databinding.incremental=true
android.lifecycleProcessor.incremental=true
```

### 4.2 Build Cache Configuration

**File:** `settings.gradle.kts`

```kotlin
buildCache {
    local {
        isEnabled = true
        directory = File(rootDir, ".gradle/build-cache")
        removeUnusedEntriesAfterDays = 30
    }
}
```

---

## Phase 5: Testing & Validation (Days 9-12)

### 5.1 Build Validation Script

**File:** `scripts/validate-build.sh`

```bash
#!/bin/bash

echo "🔍 VOS4 Build Validation Starting..."

# Clean previous builds
./gradlew clean

# Build libraries first
echo "📚 Building libraries..."
./gradlew :libraries:DeviceManager:build || exit 1
./gradlew :libraries:SpeechRecognition:build || exit 1

# Build apps
echo "📱 Building apps..."
./gradlew :apps:VoiceRecognition:assembleVivokaDebug || exit 1

# Check APK size
APK_SIZE=$(du -h app/build/outputs/apk/vivoka/debug/*.apk | cut -f1)
echo "📦 APK Size: $APK_SIZE"

# Run unit tests
echo "🧪 Running tests..."
./gradlew testVivokaDebugUnitTest

echo "✅ Build validation complete!"
```

### 5.2 Memory Monitoring

```kotlin
// Add to Application class
class VOS4Application : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            // Monitor memory usage
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory() / 1024 / 1024
            val totalMemory = runtime.totalMemory() / 1024 / 1024
            val freeMemory = runtime.freeMemory() / 1024 / 1024
            
            Log.d("Memory", "Max: ${maxMemory}MB, Total: ${totalMemory}MB, Free: ${freeMemory}MB")
        }
    }
}
```

---

## Implementation Schedule

| Week 1 | Week 2 |
|--------|--------|
| **Day 1-2:** Vosk Download Manager | **Day 8-9:** Gradle optimization |
| **Day 3:** Remove dependencies | **Day 10:** Integration testing |
| **Day 4-5:** Native lib optimization | **Day 11:** Performance testing |
| **Day 6-7:** Fix VoiceUI errors | **Day 12:** Documentation |

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| APK Size | < 50MB | Build output |
| Build Time | < 2 minutes | Gradle scan |
| Memory Usage | < 4GB | Task manager |
| Compilation Errors | 0 | Build log |
| Test Coverage | > 60% | JaCoCo report |

---

## Risk Mitigation

| Risk | Mitigation Strategy |
|------|-------------------|
| Vosk download fails | Fallback to Android SpeechRecognizer |
| Vivoka license issues | Document as test-only dependency |
| Memory still insufficient | Use remote build server |
| VoiceUI unfixable | Create new simplified UI |

---

## Rollback Plan

If implementation fails:
1. Restore from Git backup branch
2. Use simplified single-engine approach
3. Remove problematic modules temporarily
4. Focus on core functionality only

---

## Documentation Requirements

- [ ] Update README with new build instructions
- [ ] Document Vosk download process
- [ ] Create migration guide for existing users
- [ ] Update API documentation
- [ ] Add troubleshooting guide

---

**Plan Version:** 1.0  
**Approved By:** Development Team  
**Review Date:** February 1, 2025  
**Implementation Start:** January 31, 2025
