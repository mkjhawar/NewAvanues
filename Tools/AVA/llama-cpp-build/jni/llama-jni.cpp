/**
 * JNI Bridge for GGUFInferenceStrategy
 *
 * Bridges Kotlin GGUFInferenceStrategy to llama.cpp C API
 *
 * Package: com.augmentalis.ava.features.llm.alc.inference
 * Class: GGUFInferenceStrategy
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-04
 */

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <unistd.h>
#include "llama.h"
#include "common.h"

#define TAG "llama-jni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Global state for current session
static llama_model* g_model = nullptr;
static llama_context* g_ctx = nullptr;
static llama_sampler* g_sampler = nullptr;
static std::vector<llama_token> g_tokens;

/**
 * Load a GGUF model
 *
 * @param modelPath Path to .gguf file
 * @param contextLength Maximum context length
 * @param gpuLayers Number of layers on GPU
 * @return Model pointer (0 on failure)
 */
extern "C" JNIEXPORT jlong JNICALL
Java_com_augmentalis_ava_features_llm_alc_inference_GGUFInferenceStrategy_nativeLoadModel(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPath,
        jint contextLength,
        jint gpuLayers) {

    // Initialize backend
    llama_backend_init();

    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading model: %s", path);

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = gpuLayers;

    llama_model* model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(modelPath, path);

    if (!model) {
        LOGE("Failed to load model");
        return 0;
    }

    g_model = model;
    LOGI("Model loaded successfully");
    return reinterpret_cast<jlong>(model);
}

/**
 * Create inference context
 *
 * @param modelPtr Model pointer from nativeLoadModel
 * @param contextLength Context length
 * @return Context pointer (0 on failure)
 */
extern "C" JNIEXPORT jlong JNICALL
Java_com_augmentalis_ava_features_llm_alc_inference_GGUFInferenceStrategy_nativeCreateContext(
        JNIEnv* env,
        jobject /* this */,
        jlong modelPtr,
        jint contextLength) {

    llama_model* model = reinterpret_cast<llama_model*>(modelPtr);
    if (!model) {
        LOGE("Invalid model pointer");
        return 0;
    }

    int n_threads = std::max(1, std::min(8, (int)sysconf(_SC_NPROCESSORS_ONLN) - 2));
    LOGI("Using %d threads, context length: %d", n_threads, contextLength);

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = contextLength;
    ctx_params.n_threads = n_threads;
    ctx_params.n_threads_batch = n_threads;

    llama_context* ctx = llama_init_from_model(model, ctx_params);
    if (!ctx) {
        LOGE("Failed to create context");
        return 0;
    }

    // Create sampler
    auto sparams = llama_sampler_chain_default_params();
    sparams.no_perf = true;
    g_sampler = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(g_sampler, llama_sampler_init_greedy());

    g_ctx = ctx;
    LOGI("Context created successfully");
    return reinterpret_cast<jlong>(ctx);
}

/**
 * Free model resources
 */
extern "C" JNIEXPORT void JNICALL
Java_com_augmentalis_ava_features_llm_alc_inference_GGUFInferenceStrategy_nativeFreeModel(
        JNIEnv* env,
        jobject /* this */,
        jlong modelPtr) {

    llama_model* model = reinterpret_cast<llama_model*>(modelPtr);
    if (model) {
        llama_model_free(model);
        if (g_model == model) g_model = nullptr;
        LOGI("Model freed");
    }
}

/**
 * Free context resources
 */
extern "C" JNIEXPORT void JNICALL
Java_com_augmentalis_ava_features_llm_alc_inference_GGUFInferenceStrategy_nativeFreeContext(
        JNIEnv* env,
        jobject /* this */,
        jlong contextPtr) {

    llama_context* ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (ctx) {
        llama_free(ctx);
        if (g_ctx == ctx) g_ctx = nullptr;
        LOGI("Context freed");
    }

    if (g_sampler) {
        llama_sampler_free(g_sampler);
        g_sampler = nullptr;
    }

    llama_backend_free();
}

/**
 * Run inference on token sequence
 *
 * @param contextPtr Context pointer
 * @param tokens Input token IDs
 * @return Logits array for next token prediction
 */
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_augmentalis_ava_features_llm_alc_inference_GGUFInferenceStrategy_nativeInfer(
        JNIEnv* env,
        jobject /* this */,
        jlong contextPtr,
        jintArray tokens) {

    llama_context* ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (!ctx) {
        LOGE("Invalid context pointer");
        return nullptr;
    }

    jint* tokenData = env->GetIntArrayElements(tokens, nullptr);
    jsize tokenCount = env->GetArrayLength(tokens);

    // Create batch
    llama_batch batch = llama_batch_init(tokenCount, 0, 1);

    for (int i = 0; i < tokenCount; i++) {
        batch.token[i] = tokenData[i];
        batch.pos[i] = i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i] = (i == tokenCount - 1);
    }
    batch.n_tokens = tokenCount;

    env->ReleaseIntArrayElements(tokens, tokenData, 0);

    // Decode
    if (llama_decode(ctx, batch) != 0) {
        LOGE("llama_decode failed");
        llama_batch_free(batch);
        return nullptr;
    }

    // Get logits
    const float* logits = llama_get_logits_ith(ctx, tokenCount - 1);
    const llama_model* model = llama_get_model(ctx);
    const llama_vocab* vocab = llama_model_get_vocab(model);
    int vocab_size = llama_vocab_n_tokens(vocab);

    jfloatArray result = env->NewFloatArray(vocab_size);
    env->SetFloatArrayRegion(result, 0, vocab_size, logits);

    llama_batch_free(batch);
    return result;
}

/**
 * Tokenize text string
 *
 * @param contextPtr Context pointer
 * @param text Input text
 * @return Token IDs
 */
extern "C" JNIEXPORT jintArray JNICALL
Java_com_augmentalis_ava_features_llm_alc_inference_GGUFInferenceStrategy_nativeTokenize(
        JNIEnv* env,
        jobject /* this */,
        jlong contextPtr,
        jstring text) {

    llama_context* ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (!ctx) {
        LOGE("Invalid context pointer");
        return nullptr;
    }

    const char* textStr = env->GetStringUTFChars(text, nullptr);

    std::vector<llama_token> tokens = common_tokenize(ctx, textStr, true, true);
    g_tokens = tokens;

    env->ReleaseStringUTFChars(text, textStr);

    jintArray result = env->NewIntArray(tokens.size());
    env->SetIntArrayRegion(result, 0, tokens.size(), reinterpret_cast<jint*>(tokens.data()));

    LOGI("Tokenized to %d tokens", (int)tokens.size());
    return result;
}

/**
 * Process prompt tokens (prefill phase)
 *
 * @param contextPtr Context pointer
 * @param tokens Prompt tokens
 * @return true on success
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_augmentalis_ava_features_llm_alc_inference_GGUFInferenceStrategy_nativePrefill(
        JNIEnv* env,
        jobject /* this */,
        jlong contextPtr,
        jintArray tokens) {

    llama_context* ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (!ctx) {
        LOGE("Invalid context pointer");
        return JNI_FALSE;
    }

    jint* tokenData = env->GetIntArrayElements(tokens, nullptr);
    jsize tokenCount = env->GetArrayLength(tokens);

    // Clear KV cache
    llama_memory_clear(llama_get_memory(ctx), true);

    // Create batch for prefill
    llama_batch batch = llama_batch_init(tokenCount, 0, 1);

    for (int i = 0; i < tokenCount; i++) {
        batch.token[i] = tokenData[i];
        batch.pos[i] = i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i] = (i == tokenCount - 1);
    }
    batch.n_tokens = tokenCount;

    // Store for continued generation
    g_tokens.clear();
    for (int i = 0; i < tokenCount; i++) {
        g_tokens.push_back(tokenData[i]);
    }

    env->ReleaseIntArrayElements(tokens, tokenData, 0);

    if (llama_decode(ctx, batch) != 0) {
        LOGE("Prefill decode failed");
        llama_batch_free(batch);
        return JNI_FALSE;
    }

    llama_batch_free(batch);
    LOGI("Prefill completed for %d tokens", tokenCount);
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
extern "C" JNIEXPORT jint JNICALL
Java_com_augmentalis_ava_features_llm_alc_inference_GGUFInferenceStrategy_nativeSampleToken(
        JNIEnv* env,
        jobject /* this */,
        jlong contextPtr,
        jfloat temperature,
        jfloat topP,
        jint topK,
        jfloat repeatPenalty) {

    llama_context* ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (!ctx || !g_sampler) {
        LOGE("Invalid context or sampler");
        return -1;
    }

    // Sample using the sampler (currently greedy)
    llama_token token = llama_sampler_sample(g_sampler, ctx, -1);

    return token;
}

/**
 * Check if token is end-of-sequence
 */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_augmentalis_ava_features_llm_alc_inference_GGUFInferenceStrategy_nativeIsEOS(
        JNIEnv* env,
        jobject /* this */,
        jlong contextPtr,
        jint token) {

    llama_context* ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (!ctx) return JNI_TRUE;

    const llama_model* model = llama_get_model(ctx);
    const llama_vocab* vocab = llama_model_get_vocab(model);

    return llama_vocab_is_eog(vocab, token) ? JNI_TRUE : JNI_FALSE;
}

/**
 * Convert token ID to text
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_augmentalis_ava_features_llm_alc_inference_GGUFInferenceStrategy_nativeTokenToText(
        JNIEnv* env,
        jobject /* this */,
        jlong contextPtr,
        jint token) {

    llama_context* ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (!ctx) return env->NewStringUTF("");

    std::string piece = common_token_to_piece(ctx, token);
    return env->NewStringUTF(piece.c_str());
}

/**
 * Accept token into context (for continued generation)
 */
extern "C" JNIEXPORT void JNICALL
Java_com_augmentalis_ava_features_llm_alc_inference_GGUFInferenceStrategy_nativeAcceptToken(
        JNIEnv* env,
        jobject /* this */,
        jlong contextPtr,
        jint token) {

    llama_context* ctx = reinterpret_cast<llama_context*>(contextPtr);
    if (!ctx) return;

    g_tokens.push_back(token);
    int pos = g_tokens.size() - 1;

    // Create single-token batch
    llama_batch batch = llama_batch_init(1, 0, 1);
    batch.token[0] = token;
    batch.pos[0] = pos;
    batch.n_seq_id[0] = 1;
    batch.seq_id[0][0] = 0;
    batch.logits[0] = true;
    batch.n_tokens = 1;

    if (llama_decode(ctx, batch) != 0) {
        LOGE("Accept token decode failed");
    }

    llama_batch_free(batch);
}
