/**
 * whisper_jni.cpp - JNI wrapper for OpenAI Whisper
 * 
 * Copyright (C) Augmentalis Inc
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * JNI bridge between Kotlin WhisperEngine and whisper.cpp
 */

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <memory>
#include "whisper/whisper.h"

#define LOG_TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Global whisper context holder
static std::unique_ptr<whisper_context, decltype(&whisper_free)> g_whisper_ctx(nullptr, whisper_free);

extern "C" {

/**
 * Initialize Whisper context
 * Returns native context pointer or 0 on failure
 */
JNIEXPORT jlong JNICALL
Java_com_augmentalis_speechrecognition_speechengines_WhisperNative_initContext(
    JNIEnv *env, jobject /* this */) {
    
    LOGI("Initializing Whisper context");
    
    // Initialize whisper parameters
    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = true; // Enable GPU if available
    
    // For now, return a placeholder value
    // In real implementation, this would create actual context
    return static_cast<jlong>(1);
}

/**
 * Load Whisper model from file
 * Returns true on success
 */
JNIEXPORT jboolean JNICALL
Java_com_augmentalis_speechrecognition_speechengines_WhisperNative_loadModel(
    JNIEnv *env, jobject /* this */, jlong context, jstring modelPath) {
    
    if (context == 0) {
        LOGE("Invalid context");
        return JNI_FALSE;
    }
    
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading Whisper model from: %s", path);
    
    // Load the model
    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = true;
    
    whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(modelPath, path);
    
    if (ctx == nullptr) {
        LOGE("Failed to load model");
        return JNI_FALSE;
    }
    
    // Store the context globally
    g_whisper_ctx.reset(ctx);
    
    LOGI("Model loaded successfully");
    return JNI_TRUE;
}

/**
 * Transcribe audio data
 * Returns transcribed text or empty string on failure
 */
JNIEXPORT jstring JNICALL
Java_com_augmentalis_speechrecognition_speechengines_WhisperNative_transcribe(
    JNIEnv *env, jobject /* this */, jlong context, jfloatArray audioData, jint sampleRate) {
    
    if (context == 0 || !g_whisper_ctx) {
        LOGE("Invalid context or model not loaded");
        return env->NewStringUTF("");
    }
    
    // Get audio data
    jsize audioLength = env->GetArrayLength(audioData);
    jfloat *audio = env->GetFloatArrayElements(audioData, nullptr);
    
    if (audio == nullptr) {
        LOGE("Failed to get audio data");
        return env->NewStringUTF("");
    }
    
    LOGD("Transcribing %d samples at %d Hz", audioLength, sampleRate);
    
    // Setup whisper parameters
    whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    
    // Configure parameters
    wparams.print_progress = false;
    wparams.print_special = false;
    wparams.print_realtime = false;
    wparams.print_timestamps = false;
    wparams.translate = false;
    wparams.language = "en"; // Default to English
    wparams.n_threads = 4;
    wparams.offset_ms = 0;
    wparams.duration_ms = 0;
    wparams.suppress_blank = true;
    // suppress_non_speech_tokens may not be available in all versions
    // wparams.suppress_non_speech_tokens = true;
    
    // Process audio
    std::vector<float> audioVector(audio, audio + audioLength);
    
    // Run inference
    int result = whisper_full(g_whisper_ctx.get(), wparams, audioVector.data(), audioLength);
    
    // Release audio data
    env->ReleaseFloatArrayElements(audioData, audio, JNI_ABORT);
    
    if (result != 0) {
        LOGE("Transcription failed with code: %d", result);
        return env->NewStringUTF("");
    }
    
    // Get transcription result
    std::string transcription;
    const int n_segments = whisper_full_n_segments(g_whisper_ctx.get());
    
    for (int i = 0; i < n_segments; ++i) {
        const char *text = whisper_full_get_segment_text(g_whisper_ctx.get(), i);
        if (text != nullptr) {
            transcription += text;
            transcription += " ";
        }
    }
    
    LOGI("Transcription complete: %s", transcription.c_str());
    return env->NewStringUTF(transcription.c_str());
}

/**
 * Get segment count from last transcription
 */
JNIEXPORT jint JNICALL
Java_com_augmentalis_speechrecognition_speechengines_WhisperNative_getSegmentCount(
    JNIEnv *env, jobject /* this */, jlong context) {
    
    if (context == 0 || !g_whisper_ctx) {
        return 0;
    }
    
    return whisper_full_n_segments(g_whisper_ctx.get());
}

/**
 * Get segment text by index
 */
JNIEXPORT jstring JNICALL
Java_com_augmentalis_speechrecognition_speechengines_WhisperNative_getSegmentText(
    JNIEnv *env, jobject /* this */, jlong context, jint index) {
    
    if (context == 0 || !g_whisper_ctx) {
        return env->NewStringUTF("");
    }
    
    const char *text = whisper_full_get_segment_text(g_whisper_ctx.get(), index);
    return env->NewStringUTF(text ? text : "");
}

/**
 * Get segment start time in milliseconds
 */
JNIEXPORT jlong JNICALL
Java_com_augmentalis_speechrecognition_speechengines_WhisperNative_getSegmentStartTime(
    JNIEnv *env, jobject /* this */, jlong context, jint index) {
    
    if (context == 0 || !g_whisper_ctx) {
        return 0;
    }
    
    int64_t t0 = whisper_full_get_segment_t0(g_whisper_ctx.get(), index);
    // Convert from samples to milliseconds (assuming 16kHz)
    return (t0 * 1000) / 16000;
}

/**
 * Get segment end time in milliseconds
 */
JNIEXPORT jlong JNICALL
Java_com_augmentalis_speechrecognition_speechengines_WhisperNative_getSegmentEndTime(
    JNIEnv *env, jobject /* this */, jlong context, jint index) {
    
    if (context == 0 || !g_whisper_ctx) {
        return 0;
    }
    
    int64_t t1 = whisper_full_get_segment_t1(g_whisper_ctx.get(), index);
    // Convert from samples to milliseconds (assuming 16kHz)
    return (t1 * 1000) / 16000;
}

/**
 * Set language for transcription
 */
JNIEXPORT void JNICALL
Java_com_augmentalis_speechrecognition_speechengines_WhisperNative_setLanguage(
    JNIEnv *env, jobject /* this */, jlong context, jstring language) {
    
    if (context == 0) {
        return;
    }
    
    const char *lang = env->GetStringUTFChars(language, nullptr);
    LOGI("Setting language to: %s", lang);
    // Store language for next transcription
    env->ReleaseStringUTFChars(language, lang);
}

/**
 * Enable/disable translation to English
 */
JNIEXPORT void JNICALL
Java_com_augmentalis_speechrecognition_speechengines_WhisperNative_setTranslate(
    JNIEnv *env, jobject /* this */, jlong context, jboolean translate) {
    
    LOGI("Setting translation mode: %s", translate ? "enabled" : "disabled");
    // Store translation mode for next transcription
}

/**
 * Free Whisper context and resources
 */
JNIEXPORT void JNICALL
Java_com_augmentalis_speechrecognition_speechengines_WhisperNative_freeContext(
    JNIEnv *env, jobject /* this */, jlong context) {
    
    LOGI("Freeing Whisper context");
    
    if (g_whisper_ctx) {
        g_whisper_ctx.reset();
    }
}

/**
 * Get Whisper version string
 */
JNIEXPORT jstring JNICALL
Java_com_augmentalis_speechrecognition_speechengines_WhisperNative_getVersion(
    JNIEnv *env, jobject /* this */) {
    
    return env->NewStringUTF("whisper.cpp 1.5.4");
}

} // extern "C"
