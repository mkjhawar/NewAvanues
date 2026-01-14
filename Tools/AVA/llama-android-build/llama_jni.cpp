/**
 * llama.cpp JNI Wrapper for AVA
 *
 * Provides JNI bindings for GGUFInferenceStrategy.kt
 *
 * Build: See build-jni.sh
 *
 * Created: 2025-12-03
 * Author: AVA AI Team
 */

#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "llama.h"

#define TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// JNI package path
#define JNI_PREFIX Java_com_augmentalis_ava_features_llm_alc_inference_GGUFInferenceStrategy_

// Concatenate macro
#define JNI_FUNC(name) JNI_PREFIX##name

extern "C" {

/**
 * Load GGUF model from path
 *
 * @param modelPath Path to .gguf file
 * @param contextLength Maximum context length
 * @param gpuLayers Number of layers to offload to GPU (-1 = auto)
 * @return Model pointer (jlong) or 0 on failure
 */
JNIEXPORT jlong JNICALL
JNI_FUNC(nativeLoadModel)(JNIEnv *env, jobject thiz,
                          jstring modelPath, jint contextLength, jint gpuLayers) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading model: %s (ctx=%d, gpu_layers=%d)", path, contextLength, gpuLayers);

    llama_model_params params = llama_model_default_params();
    params.n_gpu_layers = gpuLayers >= 0 ? gpuLayers : 99; // Auto = all layers

    llama_model *model = llama_load_model_from_file(path, params);
    env->ReleaseStringUTFChars(modelPath, path);

    if (model == nullptr) {
        LOGE("Failed to load model");
        return 0;
    }

    LOGI("Model loaded successfully");
    return reinterpret_cast<jlong>(model);
}

/**
 * Create inference context for a loaded model
 *
 * @param modelPtr Model pointer from nativeLoadModel
 * @param contextLength Context window size
 * @return Context pointer (jlong) or 0 on failure
 */
JNIEXPORT jlong JNICALL
JNI_FUNC(nativeCreateContext)(JNIEnv *env, jobject thiz,
                              jlong modelPtr, jint contextLength) {
    llama_model *model = reinterpret_cast<llama_model*>(modelPtr);
    if (model == nullptr) {
        LOGE("Invalid model pointer");
        return 0;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = contextLength;
    ctx_params.n_batch = 512;
    ctx_params.n_threads = 4;
    ctx_params.n_threads_batch = 4;

    llama_context *ctx = llama_new_context_with_model(model, ctx_params);
    if (ctx == nullptr) {
        LOGE("Failed to create context");
        return 0;
    }

    LOGI("Context created (n_ctx=%d)", contextLength);
    return reinterpret_cast<jlong>(ctx);
}

/**
 * Free model resources
 */
JNIEXPORT void JNICALL
JNI_FUNC(nativeFreeModel)(JNIEnv *env, jobject thiz, jlong modelPtr) {
    llama_model *model = reinterpret_cast<llama_model*>(modelPtr);
    if (model != nullptr) {
        llama_free_model(model);
        LOGI("Model freed");
    }
}

/**
 * Free context resources
 */
JNIEXPORT void JNICALL
JNI_FUNC(nativeFreeContext)(JNIEnv *env, jobject thiz, jlong contextPtr) {
    llama_context *ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (ctx != nullptr) {
        llama_free(ctx);
        LOGI("Context freed");
    }
}

/**
 * Tokenize input text
 *
 * @param contextPtr Context pointer
 * @param text Text to tokenize
 * @return Array of token IDs
 */
JNIEXPORT jintArray JNICALL
JNI_FUNC(nativeTokenize)(JNIEnv *env, jobject thiz,
                         jlong contextPtr, jstring text) {
    llama_context *ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (ctx == nullptr) {
        LOGE("Invalid context pointer");
        return nullptr;
    }

    const llama_model *model = llama_get_model(ctx);
    const char *str = env->GetStringUTFChars(text, nullptr);

    // Tokenize with enough space
    std::vector<llama_token> tokens(strlen(str) + 16);
    int n_tokens = llama_tokenize(model, str, strlen(str), tokens.data(), tokens.size(), true, false);

    env->ReleaseStringUTFChars(text, str);

    if (n_tokens < 0) {
        LOGE("Tokenization failed");
        return nullptr;
    }

    tokens.resize(n_tokens);

    jintArray result = env->NewIntArray(n_tokens);
    env->SetIntArrayRegion(result, 0, n_tokens, reinterpret_cast<jint*>(tokens.data()));

    return result;
}

/**
 * Prefill context with tokens (prompt processing)
 *
 * @param contextPtr Context pointer
 * @param tokens Token array
 * @return true if successful
 */
JNIEXPORT jboolean JNICALL
JNI_FUNC(nativePrefill)(JNIEnv *env, jobject thiz,
                        jlong contextPtr, jintArray tokens) {
    llama_context *ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (ctx == nullptr) {
        LOGE("Invalid context pointer");
        return JNI_FALSE;
    }

    jsize n_tokens = env->GetArrayLength(tokens);
    jint *token_data = env->GetIntArrayElements(tokens, nullptr);

    // Create batch for prefill
    llama_batch batch = llama_batch_init(n_tokens, 0, 1);

    for (int i = 0; i < n_tokens; i++) {
        llama_batch_add(batch, token_data[i], i, {0}, false);
    }
    batch.logits[batch.n_tokens - 1] = true; // Enable logits for last token

    env->ReleaseIntArrayElements(tokens, token_data, 0);

    // Decode
    if (llama_decode(ctx, batch) != 0) {
        LOGE("Prefill decode failed");
        llama_batch_free(batch);
        return JNI_FALSE;
    }

    llama_batch_free(batch);
    return JNI_TRUE;
}

/**
 * Sample next token with given parameters
 *
 * @param contextPtr Context pointer
 * @param temperature Sampling temperature
 * @param topP Top-p (nucleus) sampling
 * @param topK Top-k sampling
 * @param repeatPenalty Repetition penalty
 * @return Sampled token ID
 */
JNIEXPORT jint JNICALL
JNI_FUNC(nativeSampleToken)(JNIEnv *env, jobject thiz,
                            jlong contextPtr,
                            jfloat temperature, jfloat topP, jint topK, jfloat repeatPenalty) {
    llama_context *ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (ctx == nullptr) {
        LOGE("Invalid context pointer");
        return -1;
    }

    const llama_model *model = llama_get_model(ctx);
    int n_vocab = llama_n_vocab(model);

    // Get logits from last token
    float *logits = llama_get_logits(ctx);

    // Create candidates
    std::vector<llama_token_data> candidates;
    candidates.reserve(n_vocab);
    for (llama_token token_id = 0; token_id < n_vocab; token_id++) {
        candidates.emplace_back(llama_token_data{token_id, logits[token_id], 0.0f});
    }

    llama_token_data_array candidates_p = {candidates.data(), candidates.size(), false};

    // Apply sampling
    llama_sample_top_k(ctx, &candidates_p, topK, 1);
    llama_sample_top_p(ctx, &candidates_p, topP, 1);
    llama_sample_temp(ctx, &candidates_p, temperature);

    llama_token new_token = llama_sample_token(ctx, &candidates_p);

    return static_cast<jint>(new_token);
}

/**
 * Check if token is end-of-sequence
 */
JNIEXPORT jboolean JNICALL
JNI_FUNC(nativeIsEOS)(JNIEnv *env, jobject thiz,
                      jlong contextPtr, jint token) {
    llama_context *ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (ctx == nullptr) return JNI_TRUE;

    const llama_model *model = llama_get_model(ctx);
    return llama_token_is_eog(model, token) ? JNI_TRUE : JNI_FALSE;
}

/**
 * Convert token ID to text
 */
JNIEXPORT jstring JNICALL
JNI_FUNC(nativeTokenToText)(JNIEnv *env, jobject thiz,
                            jlong contextPtr, jint token) {
    llama_context *ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (ctx == nullptr) {
        return env->NewStringUTF("");
    }

    const llama_model *model = llama_get_model(ctx);

    char buf[256];
    int n = llama_token_to_piece(model, token, buf, sizeof(buf), 0, false);

    if (n < 0) {
        return env->NewStringUTF("");
    }

    buf[n] = '\0';
    return env->NewStringUTF(buf);
}

/**
 * Accept token into context (for next iteration)
 */
JNIEXPORT void JNICALL
JNI_FUNC(nativeAcceptToken)(JNIEnv *env, jobject thiz,
                            jlong contextPtr, jint token) {
    llama_context *ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (ctx == nullptr) return;

    // Create single-token batch
    llama_batch batch = llama_batch_init(1, 0, 1);
    int n_past = llama_get_kv_cache_token_count(ctx);

    llama_batch_add(batch, token, n_past, {0}, true);

    if (llama_decode(ctx, batch) != 0) {
        LOGE("Failed to accept token");
    }

    llama_batch_free(batch);
}

/**
 * Get logits for inference (optional, for advanced sampling)
 */
JNIEXPORT jfloatArray JNICALL
JNI_FUNC(nativeInfer)(JNIEnv *env, jobject thiz,
                      jlong contextPtr, jintArray inputTokens) {
    llama_context *ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (ctx == nullptr) {
        LOGE("Invalid context pointer");
        return nullptr;
    }

    const llama_model *model = llama_get_model(ctx);
    int n_vocab = llama_n_vocab(model);

    // Get logits
    float *logits = llama_get_logits(ctx);

    jfloatArray result = env->NewFloatArray(n_vocab);
    env->SetFloatArrayRegion(result, 0, n_vocab, logits);

    return result;
}

} // extern "C"
