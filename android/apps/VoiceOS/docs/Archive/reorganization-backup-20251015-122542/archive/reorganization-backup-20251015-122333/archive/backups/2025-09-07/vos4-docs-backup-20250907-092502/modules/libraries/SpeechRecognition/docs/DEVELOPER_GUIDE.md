# Whisper Developer Guide

## Table of Contents
1. [Development Setup](#development-setup)
2. [Native Library Build](#native-library-build)
3. [JNI Implementation](#jni-implementation)
4. [Model Management Architecture](#model-management-architecture)
5. [Audio Processing Pipeline](#audio-processing-pipeline)
6. [Testing and Debugging](#testing-and-debugging)
7. [Performance Profiling](#performance-profiling)
8. [Contributing Guidelines](#contributing-guidelines)

## Development Setup

### Prerequisites

- Android Studio Arctic Fox or later
- NDK version 25.2.9519653 or later
- CMake 3.22.1 or later
- Minimum 16GB RAM for building native libraries
- macOS, Linux, or Windows with WSL2

### Environment Setup

```bash
# Clone the repository
git clone https://github.com/yourusername/VOS4.git
cd VOS4

# Set up NDK path
export ANDROID_NDK_HOME=$HOME/Android/Sdk/ndk/25.2.9519653
export PATH=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/darwin-x86_64/bin:$PATH

# Initialize submodules (whisper.cpp)
git submodule update --init --recursive
```

### Project Structure

```
libraries/SpeechRecognition/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/augmentalis/speechrecognition/
│   │   │       ├── engines/
│   │   │       │   ├── whisper/
│   │   │       │   │   ├── WhisperEngine.kt
│   │   │       │   │   ├── WhisperJNI.kt
│   │   │       │   │   ├── WhisperModelManager.kt
│   │   │       │   │   └── WhisperModels.kt
│   │   │       │   └── ...
│   │   │       └── ui/
│   │   │           └── whisper/
│   │   │               └── WhisperModelDownloadDialog.kt
│   │   ├── cpp/
│   │   │   ├── whisper/
│   │   │   │   ├── whisper.cpp (submodule)
│   │   │   │   └── ...
│   │   │   ├── jni/
│   │   │   │   ├── whisper_jni.cpp
│   │   │   │   └── audio_processor.cpp
│   │   │   └── CMakeLists.txt
│   │   └── jniLibs/
│   │       ├── arm64-v8a/
│   │       │   └── libwhisper-vos4.so
│   │       └── armeabi-v7a/
│   │           └── libwhisper-vos4.so
│   └── test/
│       └── java/
│           └── whisper/
│               └── WhisperEngineTest.kt
└── build.gradle.kts
```

## Native Library Build

### CMake Configuration

```cmake
# CMakeLists.txt
cmake_minimum_required(VERSION 3.22.1)
project("whisper-vos4")

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Architecture detection
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(WHISPER_ARM_NEON ON)
    add_definitions(-DWHISPER_ARM_NEON)
elseif(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(WHISPER_ARM_NEON ON)
    add_definitions(-DWHISPER_ARM_NEON)
    # Use smaller models for 32-bit
    add_definitions(-DWHISPER_TINY_ONLY)
endif()

# Whisper.cpp configuration
set(WHISPER_BUILD_TESTS OFF)
set(WHISPER_BUILD_EXAMPLES OFF)
set(WHISPER_SUPPORT_OPENBLAS OFF)
set(WHISPER_SUPPORT_CUBLAS OFF)

# Add whisper.cpp
add_subdirectory(${CMAKE_SOURCE_DIR}/whisper.cpp)

# JNI library
add_library(whisper-vos4 SHARED
    jni/whisper_jni.cpp
    jni/audio_processor.cpp
)

# Link libraries
target_link_libraries(whisper-vos4
    whisper
    android
    log
    OpenSLES
)

# Optimization flags
target_compile_options(whisper-vos4 PRIVATE
    -O3
    -ffast-math
    -funroll-loops
    -ftree-vectorize
)

if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    target_compile_options(whisper-vos4 PRIVATE
        -march=armv8-a+fp+simd
    )
elseif(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    target_compile_options(whisper-vos4 PRIVATE
        -march=armv7-a
        -mfpu=neon
        -mfloat-abi=softfp
    )
endif()
```

### Build Script

```bash
#!/bin/bash
# build_native.sh

# Clean previous builds
rm -rf build
rm -rf ../src/main/jniLibs

# Build for ARM64
mkdir -p build/arm64-v8a
cd build/arm64-v8a
cmake ../.. \
    -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-21 \
    -DCMAKE_BUILD_TYPE=Release
make -j$(nproc)
cd ../..

# Build for ARMv7
mkdir -p build/armeabi-v7a
cd build/armeabi-v7a
cmake ../.. \
    -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake \
    -DANDROID_ABI=armeabi-v7a \
    -DANDROID_PLATFORM=android-21 \
    -DCMAKE_BUILD_TYPE=Release
make -j$(nproc)
cd ../..

# Copy libraries
mkdir -p ../src/main/jniLibs/arm64-v8a
mkdir -p ../src/main/jniLibs/armeabi-v7a
cp build/arm64-v8a/libwhisper-vos4.so ../src/main/jniLibs/arm64-v8a/
cp build/armeabi-v7a/libwhisper-vos4.so ../src/main/jniLibs/armeabi-v7a/

echo "Build complete!"
```

## JNI Implementation

### JNI Bridge (whisper_jni.cpp)

```cpp
#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <memory>
#include "whisper.h"

#define LOG_TAG "WhisperJNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Global references
static std::unique_ptr<whisper_context, decltype(&whisper_free)> g_context(nullptr, whisper_free);
static jobject g_callback = nullptr;
static JavaVM* g_jvm = nullptr;

// JNI initialization
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

// Initialize Whisper with model
extern "C" JNIEXPORT jboolean JNICALL
Java_com_augmentalis_speechrecognition_engines_whisper_WhisperJNI_initNative(
    JNIEnv* env,
    jobject thiz,
    jstring model_path,
    jint threads) {
    
    const char* path = env->GetStringUTFChars(model_path, nullptr);
    
    // Initialize parameters
    whisper_context_params params = whisper_context_default_params();
    params.use_gpu = false;
    
    // Load model
    g_context.reset(whisper_init_from_file_with_params(path, params));
    env->ReleaseStringUTFChars(model_path, path);
    
    if (!g_context) {
        LOGE("Failed to load model");
        return JNI_FALSE;
    }
    
    LOGD("Model loaded successfully");
    return JNI_TRUE;
}

// Transcribe audio
extern "C" JNIEXPORT jobject JNICALL
Java_com_augmentalis_speechrecognition_engines_whisper_WhisperJNI_transcribeNative(
    JNIEnv* env,
    jobject thiz,
    jfloatArray audio_data,
    jint sample_rate,
    jstring language,
    jboolean translate) {
    
    if (!g_context) {
        LOGE("Context not initialized");
        return nullptr;
    }
    
    // Get audio data
    jsize audio_length = env->GetArrayLength(audio_data);
    std::vector<float> audio(audio_length);
    env->GetFloatArrayRegion(audio_data, 0, audio_length, audio.data());
    
    // Set up parameters
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    
    // Language setting
    const char* lang = nullptr;
    if (language != nullptr) {
        lang = env->GetStringUTFChars(language, nullptr);
        params.language = lang;
    } else {
        params.language = "auto";
    }
    
    // Translation mode
    params.translate = translate;
    
    // Performance settings
    params.n_threads = 4;
    params.print_progress = false;
    params.print_timestamps = true;
    params.token_timestamps = true;
    
    // Run inference
    int result = whisper_full(g_context.get(), params, audio.data(), audio_length);
    
    if (lang != nullptr) {
        env->ReleaseStringUTFChars(language, lang);
    }
    
    if (result != 0) {
        LOGE("Transcription failed: %d", result);
        return nullptr;
    }
    
    // Build result object
    jclass resultClass = env->FindClass(
        "com/augmentalis/speechrecognition/models/RecognitionResult");
    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "()V");
    jobject resultObj = env->NewObject(resultClass, constructor);
    
    // Get transcription
    const int n_segments = whisper_full_n_segments(g_context.get());
    std::string full_text;
    
    for (int i = 0; i < n_segments; i++) {
        const char* text = whisper_full_get_segment_text(g_context.get(), i);
        full_text += text;
    }
    
    // Set fields
    jfieldID textField = env->GetFieldID(resultClass, "text", "Ljava/lang/String;");
    env->SetObjectField(resultObj, textField, env->NewStringUTF(full_text.c_str()));
    
    // Detect language
    const int lang_id = whisper_full_lang_id(g_context.get());
    const char* detected_lang = whisper_lang_str(lang_id);
    
    jfieldID langField = env->GetFieldID(resultClass, "language", "Ljava/lang/String;");
    env->SetObjectField(resultObj, langField, env->NewStringUTF(detected_lang));
    
    // Calculate confidence (simplified)
    float avg_prob = 0.0f;
    for (int i = 0; i < n_segments; i++) {
        avg_prob += whisper_full_get_segment_prob(g_context.get(), i);
    }
    avg_prob /= n_segments;
    
    jfieldID confField = env->GetFieldID(resultClass, "confidence", "F");
    env->SetFloatField(resultObj, confField, avg_prob);
    
    return resultObj;
}

// Release resources
extern "C" JNIEXPORT void JNICALL
Java_com_augmentalis_speechrecognition_engines_whisper_WhisperJNI_releaseNative(
    JNIEnv* env,
    jobject thiz) {
    
    g_context.reset();
    
    if (g_callback != nullptr) {
        env->DeleteGlobalRef(g_callback);
        g_callback = nullptr;
    }
    
    LOGD("Resources released");
}

// Stream processing callback
void stream_callback(whisper_context* ctx, whisper_state* state, int progress, void* user_data) {
    if (g_callback == nullptr || g_jvm == nullptr) {
        return;
    }
    
    JNIEnv* env;
    bool attached = false;
    
    // Attach thread if needed
    if (g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        if (g_jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            return;
        }
        attached = true;
    }
    
    // Call Java callback
    jclass callbackClass = env->GetObjectClass(g_callback);
    jmethodID onProgress = env->GetMethodID(callbackClass, "onProgress", "(I)V");
    env->CallVoidMethod(g_callback, onProgress, progress);
    
    if (attached) {
        g_jvm->DetachCurrentThread();
    }
}
```

### Kotlin JNI Wrapper

```kotlin
// WhisperJNI.kt
package com.augmentalis.speechrecognition.engines.whisper

import com.augmentalis.speechrecognition.models.RecognitionResult

/**
 * JNI wrapper for Whisper native library
 */
class WhisperJNI {
    
    companion object {
        init {
            System.loadLibrary("whisper-vos4")
        }
    }
    
    /**
     * Initialize Whisper with model file
     */
    external fun initNative(
        modelPath: String,
        threads: Int = 4
    ): Boolean
    
    /**
     * Transcribe audio data
     */
    external fun transcribeNative(
        audioData: FloatArray,
        sampleRate: Int,
        language: String? = null,
        translate: Boolean = false
    ): RecognitionResult?
    
    /**
     * Release native resources
     */
    external fun releaseNative()
    
    /**
     * Set progress callback
     */
    external fun setProgressCallback(callback: ProgressCallback)
    
    /**
     * Get model information
     */
    external fun getModelInfo(): ModelInfo
    
    /**
     * Stream processing
     */
    external fun processStream(
        audioChunk: FloatArray,
        isFinal: Boolean
    ): RecognitionResult?
    
    interface ProgressCallback {
        fun onProgress(progress: Int)
    }
    
    data class ModelInfo(
        val name: String,
        val parameters: Long,
        val multilingual: Boolean,
        val languages: List<String>
    )
}
```

## Model Management Architecture

### WhisperModelManager Implementation

```kotlin
// WhisperModelManager.kt
package com.augmentalis.speechrecognition.engines.whisper

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Manages Whisper model downloads and caching
 */
class WhisperModelManager(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val modelDir = File(context.filesDir, "whisper_models")
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()
    
    init {
        modelDir.mkdirs()
    }
    
    /**
     * Download model from Hugging Face
     */
    suspend fun downloadModel(model: WhisperModel): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0f, 0, model.getSizeBytes() / 1024 / 1024))
        
        val modelFile = File(modelDir, model.fileName)
        
        // Check if already cached
        if (isModelCached(model)) {
            emit(DownloadState.Success(modelFile))
            return@flow
        }
        
        // Check available space
        if (!hasEnoughSpace(model)) {
            emit(DownloadState.Error("Insufficient storage space"))
            return@flow
        }
        
        try {
            // Download from Hugging Face
            val url = getModelUrl(model)
            val request = Request.Builder().url(url).build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadState.Error("Download failed: ${response.code}"))
                    return@flow
                }
                
                val body = response.body ?: throw Exception("Empty response")
                val contentLength = body.contentLength()
                
                withContext(Dispatchers.IO) {
                    body.byteStream().use { input ->
                        modelFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytesRead = 0L
                            
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                
                                val progress = totalBytesRead.toFloat() / contentLength
                                val downloadedMB = totalBytesRead / 1024 / 1024
                                val totalMB = contentLength / 1024 / 1024
                                
                                emit(DownloadState.Downloading(progress, downloadedMB, totalMB))
                            }
                        }
                    }
                }
                
                // Verify checksum
                if (!verifyChecksum(modelFile, model)) {
                    modelFile.delete()
                    emit(DownloadState.Error("Checksum verification failed"))
                    return@flow
                }
                
                emit(DownloadState.Success(modelFile))
            }
        } catch (e: Exception) {
            modelFile.delete()
            emit(DownloadState.Error(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get model URL based on device architecture
     */
    private fun getModelUrl(model: WhisperModel): String {
        val arch = getDeviceArchitecture()
        val modelName = when {
            arch == "arm64-v8a" -> model.fileName
            arch == "armeabi-v7a" && model == WhisperModel.TINY -> model.fileName
            else -> WhisperModel.TINY.fileName // Fallback
        }
        
        return "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-$modelName"
    }
    
    /**
     * Verify model checksum
     */
    private fun verifyChecksum(file: File, model: WhisperModel): Boolean {
        val calculated = calculateSHA256(file)
        return calculated == model.checksum
    }
    
    /**
     * Calculate SHA256 checksum
     */
    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Check if model is cached
     */
    fun isModelCached(model: WhisperModel): Boolean {
        val modelFile = File(modelDir, model.fileName)
        return modelFile.exists() && modelFile.length() == model.getSizeBytes()
    }
    
    /**
     * Get cached models
     */
    fun getCachedModels(): List<WhisperModel> {
        return WhisperModel.values().filter { isModelCached(it) }
    }
    
    /**
     * Delete cached model
     */
    fun deleteModel(model: WhisperModel): Boolean {
        val modelFile = File(modelDir, model.fileName)
        return modelFile.delete()
    }
    
    /**
     * Clear all cached models
     */
    fun clearCache() {
        modelDir.listFiles()?.forEach { it.delete() }
    }
    
    /**
     * Get available storage space
     */
    fun getAvailableSpace(): Long {
        val stat = StatFs(modelDir.path)
        return stat.availableBytes
    }
    
    /**
     * Check if there's enough space for model
     */
    private fun hasEnoughSpace(model: WhisperModel): Boolean {
        val required = model.getSizeBytes() + (50 * 1024 * 1024) // 50MB buffer
        return getAvailableSpace() > required
    }
    
    /**
     * Get device architecture
     */
    private fun getDeviceArchitecture(): String {
        return when {
            android.os.Build.SUPPORTED_64_BIT_ABIS.isNotEmpty() -> "arm64-v8a"
            android.os.Build.SUPPORTED_32_BIT_ABIS.contains("armeabi-v7a") -> "armeabi-v7a"
            else -> "unknown"
        }
    }
    
    /**
     * Get recommended model for device
     */
    fun getRecommendedModel(): WhisperModel {
        val memoryMB = getAvailableMemory() / 1024 / 1024
        val arch = getDeviceArchitecture()
        
        return when {
            arch == "armeabi-v7a" -> WhisperModel.TINY
            memoryMB < 1000 -> WhisperModel.TINY
            memoryMB < 2000 -> WhisperModel.BASE
            memoryMB < 4000 -> WhisperModel.SMALL
            else -> WhisperModel.MEDIUM
        }
    }
    
    /**
     * Get available memory
     */
    private fun getAvailableMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
    }
}

/**
 * Download state
 */
sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(
        val progress: Float,
        val downloadedMB: Long,
        val totalMB: Long
    ) : DownloadState()
    data class Success(val modelFile: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
```

## Audio Processing Pipeline

### Audio Processor Implementation

```cpp
// audio_processor.cpp
#include <vector>
#include <cmath>
#include <algorithm>

class AudioProcessor {
private:
    static constexpr int SAMPLE_RATE = 16000;
    static constexpr int FRAME_SIZE = 512;
    
    std::vector<float> buffer;
    float energy_threshold = 0.01f;
    int silence_frames = 0;
    int max_silence_frames = 50; // ~1.6 seconds at 16kHz
    
public:
    /**
     * Process audio chunk
     */
    bool processChunk(const float* input, size_t size) {
        // Apply pre-emphasis filter
        std::vector<float> filtered(size);
        filtered[0] = input[0];
        for (size_t i = 1; i < size; i++) {
            filtered[i] = input[i] - 0.97f * input[i - 1];
        }
        
        // Calculate energy
        float energy = calculateEnergy(filtered.data(), size);
        
        // Voice Activity Detection
        if (energy < energy_threshold) {
            silence_frames++;
            if (silence_frames > max_silence_frames) {
                return false; // End of speech
            }
        } else {
            silence_frames = 0;
        }
        
        // Add to buffer
        buffer.insert(buffer.end(), filtered.begin(), filtered.end());
        
        return true;
    }
    
    /**
     * Calculate signal energy
     */
    float calculateEnergy(const float* signal, size_t size) {
        float sum = 0.0f;
        for (size_t i = 0; i < size; i++) {
            sum += signal[i] * signal[i];
        }
        return sum / size;
    }
    
    /**
     * Apply noise reduction
     */
    void reduceNoise(float* signal, size_t size) {
        // Simple spectral subtraction
        std::vector<float> spectrum(size);
        
        // FFT would go here
        // For simplicity, using a basic high-pass filter
        for (size_t i = 1; i < size - 1; i++) {
            signal[i] = 0.5f * signal[i] + 
                       0.25f * signal[i - 1] + 
                       0.25f * signal[i + 1];
        }
    }
    
    /**
     * Normalize audio
     */
    void normalize(float* signal, size_t size) {
        // Find max amplitude
        float max_val = 0.0f;
        for (size_t i = 0; i < size; i++) {
            max_val = std::max(max_val, std::abs(signal[i]));
        }
        
        // Normalize to [-1, 1]
        if (max_val > 0.0f) {
            float scale = 0.95f / max_val;
            for (size_t i = 0; i < size; i++) {
                signal[i] *= scale;
            }
        }
    }
    
    /**
     * Get processed buffer
     */
    std::vector<float> getBuffer() const {
        return buffer;
    }
    
    /**
     * Clear buffer
     */
    void clear() {
        buffer.clear();
        silence_frames = 0;
    }
};
```

### Kotlin Audio Capture

```kotlin
// AudioCapture.kt
package com.augmentalis.speechrecognition.engines.whisper

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Audio capture for Whisper
 */
class AudioCapture {
    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BUFFER_SIZE_SAMPLES = 1024
    }
    
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    
    private val _audioFlow = MutableSharedFlow<FloatArray>()
    val audioFlow: SharedFlow<FloatArray> = _audioFlow.asSharedFlow()
    
    /**
     * Start audio capture
     */
    fun startCapture() {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        ) * 2
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )
        
        audioRecord?.startRecording()
        
        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ShortArray(BUFFER_SIZE_SAMPLES)
            val floatBuffer = FloatArray(BUFFER_SIZE_SAMPLES)
            
            while (isActive) {
                val read = audioRecord?.read(buffer, 0, BUFFER_SIZE_SAMPLES) ?: 0
                
                if (read > 0) {
                    // Convert PCM16 to float
                    for (i in 0 until read) {
                        floatBuffer[i] = buffer[i] / 32768.0f
                    }
                    
                    _audioFlow.emit(floatBuffer.copyOf(read))
                }
            }
        }
    }
    
    /**
     * Stop audio capture
     */
    fun stopCapture() {
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
    
    /**
     * Process audio with Voice Activity Detection
     */
    fun processWithVAD(
        audioData: FloatArray,
        threshold: Float = 0.01f
    ): VADResult {
        val energy = audioData.map { it * it }.sum() / audioData.size
        val isSpeech = energy > threshold
        
        return VADResult(
            isSpeech = isSpeech,
            energy = energy,
            audioData = if (isSpeech) audioData else null
        )
    }
    
    data class VADResult(
        val isSpeech: Boolean,
        val energy: Float,
        val audioData: FloatArray?
    )
}
```

## Testing and Debugging

### Unit Tests

```kotlin
// WhisperEngineTest.kt
package com.augmentalis.speechrecognition.engines.whisper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WhisperEngineTest {
    
    private lateinit var context: Context
    private lateinit var whisperEngine: WhisperEngine
    private lateinit var modelManager: WhisperModelManager
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        whisperEngine = WhisperEngine(context)
        modelManager = WhisperModelManager(context)
    }
    
    @Test
    fun testModelDownload() = runBlocking {
        // Download tiny model for testing
        val states = mutableListOf<DownloadState>()
        
        modelManager.downloadModel(WhisperModel.TINY).collect { state ->
            states.add(state)
            if (state is DownloadState.Success || state is DownloadState.Error) {
                // Terminal state reached
                return@collect
            }
        }
        
        // Verify download completed
        val lastState = states.last()
        assertTrue("Download should succeed", lastState is DownloadState.Success)
        
        // Verify file exists
        assertTrue("Model should be cached", modelManager.isModelCached(WhisperModel.TINY))
    }
    
    @Test
    fun testTranscription() = runBlocking {
        // Load test audio
        val testAudio = loadTestAudio("test_english.wav")
        
        // Initialize engine
        val config = SpeechConfig(
            engine = SpeechEngine.WHISPER,
            mode = SpeechMode.FREE_SPEECH,
            language = "en"
        )
        
        val initialized = whisperEngine.initialize(config)
        assertTrue("Engine should initialize", initialized)
        
        // Transcribe
        val result = whisperEngine.transcribe(testAudio)
        
        assertNotNull("Result should not be null", result)
        assertEquals("Language should be English", "en", result?.language)
        assertTrue("Confidence should be high", result?.confidence ?: 0f > 0.7f)
        assertFalse("Text should not be empty", result?.text.isNullOrEmpty())
    }
    
    @Test
    fun testLanguageDetection() = runBlocking {
        val languages = mapOf(
            "test_spanish.wav" to "es",
            "test_french.wav" to "fr",
            "test_german.wav" to "de"
        )
        
        val config = SpeechConfig(
            engine = SpeechEngine.WHISPER,
            language = "auto" // Auto-detect
        )
        
        whisperEngine.initialize(config)
        
        languages.forEach { (audioFile, expectedLang) ->
            val audio = loadTestAudio(audioFile)
            val result = whisperEngine.transcribe(audio)
            
            assertEquals(
                "Language should be detected as $expectedLang",
                expectedLang,
                result?.language
            )
        }
    }
    
    @Test
    fun testMemoryManagement() {
        // Test memory is properly released
        val runtime = Runtime.getRuntime()
        val beforeMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Initialize and release multiple times
        repeat(5) {
            whisperEngine.initialize(SpeechConfig())
            whisperEngine.release()
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(1000)
        
        val afterMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryLeak = afterMemory - beforeMemory
        
        assertTrue(
            "Memory leak should be minimal (< 10MB)",
            memoryLeak < 10 * 1024 * 1024
        )
    }
    
    @Test
    fun testConcurrentAccess() = runBlocking {
        // Test thread safety
        val jobs = List(10) { index ->
            launch {
                val audio = generateTestAudio(1000)
                val result = whisperEngine.transcribe(audio)
                assertNotNull("Result $index should not be null", result)
            }
        }
        
        jobs.forEach { it.join() }
    }
    
    private fun loadTestAudio(filename: String): FloatArray {
        // Load test audio from assets
        val inputStream = context.assets.open("test_audio/$filename")
        // WAV parsing code here
        return floatArrayOf() // Placeholder
    }
    
    private fun generateTestAudio(samples: Int): FloatArray {
        // Generate sine wave for testing
        return FloatArray(samples) { i ->
            kotlin.math.sin(2 * kotlin.math.PI * 440 * i / 16000).toFloat()
        }
    }
}
```

### Integration Tests

```kotlin
// WhisperIntegrationTest.kt
@RunWith(AndroidJUnit4::class)
class WhisperIntegrationTest {
    
    @Test
    fun testEndToEndTranscription() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = WhisperEngine(context)
        
        // Download model if needed
        val modelManager = WhisperModelManager(context)
        if (!modelManager.isModelCached(WhisperModel.BASE)) {
            modelManager.downloadModel(WhisperModel.BASE).first { 
                it is DownloadState.Success 
            }
        }
        
        // Initialize
        engine.initialize(SpeechConfig())
        
        // Start listening
        engine.startListening()
        
        // Simulate audio input
        val audioCapture = AudioCapture()
        audioCapture.startCapture()
        
        delay(5000) // Record for 5 seconds
        
        audioCapture.stopCapture()
        engine.stopListening()
        
        // Get results
        val results = engine.getResults()
        assertFalse("Should have results", results.isEmpty())
    }
}
```

## Performance Profiling

### Profiling Tools

```kotlin
// PerformanceProfiler.kt
class PerformanceProfiler {
    private val metrics = mutableListOf<PerformanceMetric>()
    
    fun profile(name: String, block: () -> Unit) {
        val startTime = System.nanoTime()
        val startMemory = Runtime.getRuntime().totalMemory()
        
        block()
        
        val endTime = System.nanoTime()
        val endMemory = Runtime.getRuntime().totalMemory()
        
        metrics.add(
            PerformanceMetric(
                name = name,
                timeMs = (endTime - startTime) / 1_000_000,
                memoryBytes = endMemory - startMemory
            )
        )
    }
    
    fun generateReport(): String {
        return metrics.joinToString("\n") { metric ->
            "${metric.name}: ${metric.timeMs}ms, ${metric.memoryBytes / 1024}KB"
        }
    }
    
    data class PerformanceMetric(
        val name: String,
        val timeMs: Long,
        val memoryBytes: Long
    )
}
```

### Benchmarking

```kotlin
// WhisperBenchmark.kt
class WhisperBenchmark {
    
    @Test
    fun benchmarkModels() {
        val models = listOf(
            WhisperModel.TINY,
            WhisperModel.BASE,
            WhisperModel.SMALL
        )
        
        val results = models.map { model ->
            benchmarkModel(model)
        }
        
        // Generate report
        results.forEach { result ->
            println("Model: ${result.model}")
            println("  Load time: ${result.loadTimeMs}ms")
            println("  Inference time: ${result.inferenceTimeMs}ms")
            println("  RTF: ${result.rtf}")
            println("  Memory: ${result.memoryMB}MB")
        }
    }
    
    private fun benchmarkModel(model: WhisperModel): BenchmarkResult {
        val engine = WhisperEngine(context)
        
        // Measure load time
        val loadStart = System.currentTimeMillis()
        engine.loadModel(model)
        val loadTime = System.currentTimeMillis() - loadStart
        
        // Measure inference time
        val testAudio = generateTestAudio(16000 * 10) // 10 seconds
        val inferenceStart = System.currentTimeMillis()
        engine.transcribe(testAudio)
        val inferenceTime = System.currentTimeMillis() - inferenceStart
        
        // Calculate Real-Time Factor
        val audioDuration = 10000 // 10 seconds in ms
        val rtf = inferenceTime.toFloat() / audioDuration
        
        // Measure memory
        val runtime = Runtime.getRuntime()
        val memoryMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        
        engine.release()
        
        return BenchmarkResult(
            model = model,
            loadTimeMs = loadTime,
            inferenceTimeMs = inferenceTime,
            rtf = rtf,
            memoryMB = memoryMB
        )
    }
    
    data class BenchmarkResult(
        val model: WhisperModel,
        val loadTimeMs: Long,
        val inferenceTimeMs: Long,
        val rtf: Float,
        val memoryMB: Long
    )
}
```

## Contributing Guidelines

### Code Style

Follow the project's coding standards:

1. **Kotlin**
   - Use Kotlin coding conventions
   - Prefer immutability
   - Use coroutines for async operations
   - Document public APIs

2. **C++**
   - Follow Google C++ Style Guide
   - Use RAII for resource management
   - Prefer smart pointers
   - Document complex algorithms

3. **JNI**
   - Check for null pointers
   - Release local references
   - Handle exceptions properly
   - Use global references sparingly

### Pull Request Process

1. **Fork and Branch**
   ```bash
   git checkout -b feature/whisper-improvement
   ```

2. **Make Changes**
   - Write clean, documented code
   - Add unit tests
   - Update documentation

3. **Test**
   ```bash
   ./gradlew test
   ./gradlew connectedAndroidTest
   ```

4. **Submit PR**
   - Clear description
   - Reference issues
   - Include test results

### Testing Requirements

- Unit test coverage > 80%
- Integration tests for new features
- Performance benchmarks for optimizations
- Memory leak testing

### Documentation

- Update API documentation
- Add code examples
- Update changelog
- Include migration guides if needed

---

*Last updated: 2025-08-31*
*Version: 2.1.0*
