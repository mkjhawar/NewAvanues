# Whisper.cpp Android Integration Setup

## Status: NOT READY FOR COMPILATION

The WhisperEngineAdapter has been fully implemented (`WhisperEngineAdapter.kt.todo`) but requires manual setup of the whisper.cpp native library.

## Why is it disabled?

Whisper.cpp is not available as a Maven dependency. It requires:
1. Cloning the whisper.cpp repository
2. Building the native library with Android NDK
3. Creating JNI bindings
4. Packaging the .so files in the app

## Setup Instructions

### 1. Clone whisper.cpp

```bash
cd /path/to/your/workspace
git clone https://github.com/ggerganov/whisper.cpp.git
cd whisper.cpp/examples/whisper.android.java
```

### 2. Build Native Library

Follow the instructions in:
https://github.com/ggerganov/whisper.cpp/tree/master/examples/whisper.android.java

This will generate:
- `libwhisper.so` for multiple architectures (arm64-v8a, armeabi-v7a, x86, x86_64)

### 3. Copy Libraries to VoiceOSCore

```bash
mkdir -p Modules/VoiceOS/apps/VoiceOSCore/src/main/jniLibs/arm64-v8a
mkdir -p Modules/VoiceOS/apps/VoiceOSCore/src/main/jniLibs/armeabi-v7a
mkdir -p Modules/VoiceOS/apps/VoiceOSCore/src/main/jniLibs/x86
mkdir -p Modules/VoiceOS/apps/VoiceOSCore/src/main/jniLibs/x86_64

# Copy .so files
cp whisper.cpp/examples/whisper.android.java/app/src/main/jniLibs/*/libwhisper.so \
   Modules/VoiceOS/apps/VoiceOSCore/src/main/jniLibs/
```

### 4. Add JNI Wrapper

Create `WhisperContext.kt` with JNI bindings to the native library.

Reference implementation:
https://github.com/ggerganov/whisper.cpp/blob/master/examples/whisper.android.java/app/src/main/java/com/whispercppdemo/whisper/WhisperContext.kt

### 5. Enable in Build

In `build.gradle.kts`, uncomment:
```kotlin
// implementation("com.whispercpp:whisper:0.1.0")  // Replace with local JNI setup
```

### 6. Enable in Factory

In `SpeechEngineFactory.kt`, replace error with:
```kotlin
SpeechEngine.WHISPER -> {
    Log.i(TAG, "Creating Whisper.cpp engine adapter")
    WhisperEngineAdapter(context)
}
```

### 7. Rename Adapter

```bash
mv WhisperEngineAdapter.kt.todo WhisperEngineAdapter.kt
```

### 8. Download Models

Download GGML models from:
https://huggingface.co/ggerganov/whisper.cpp/tree/main

Recommended models:
- `ggml-tiny.en.bin` (75 MB, fastest, English-only)
- `ggml-base.en.bin` (142 MB, good balance, English-only)
- `ggml-small.en.bin` (466 MB, high accuracy, English-only)

Place in `Modules/VoiceOS/apps/VoiceOSCore/src/main/assets/models/whisper/`

## Implementation Status

✅ WhisperEngineAdapter.kt - Full implementation (479 lines)
❌ Native library setup - Requires manual build
❌ JNI bindings - Requires WhisperContext.kt
❌ Model files - Requires download

## References

- Official Android example: https://github.com/ggerganov/whisper.cpp/tree/master/examples/whisper.android.java
- Model repository: https://huggingface.co/ggerganov/whisper.cpp
- Whisper.cpp main: https://github.com/ggerganov/whisper.cpp

---

**Note**: Once setup is complete, Whisper will provide state-of-the-art offline speech recognition with support for 99+ languages and advanced features like language detection, translation, and word-level timestamps.
