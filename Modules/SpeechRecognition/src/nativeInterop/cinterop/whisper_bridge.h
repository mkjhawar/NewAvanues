/**
 * whisper_bridge.h - Minimal C bridge for whisper.cpp iOS cinterop
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 *
 * Thin C API that wraps whisper.cpp for Kotlin/Native cinterop.
 * Uses opaque void* for context and simple types for parameters/results
 * to avoid exposing complex whisper.cpp/ggml structs to Kotlin.
 *
 * This header is consumed by cinterop to generate Kotlin bindings.
 * The implementation (whisper_bridge.c) is compiled into a static library
 * alongside whisper.cpp for the iOS target architecture (arm64).
 *
 * Build steps:
 * 1. Compile whisper.cpp + ggml for iOS arm64:
 *    cd Modules/Whisper && mkdir build-ios && cd build-ios
 *    cmake .. -DCMAKE_TOOLCHAIN_FILE=../cmake/ios.toolchain.cmake \
 *             -DPLATFORM=OS64 -DCMAKE_BUILD_TYPE=Release
 *    make -j$(sysctl -n hw.ncpu)
 *
 * 2. Compile this bridge:
 *    clang -c whisper_bridge.c -I../../Whisper/include -I../../Whisper/ggml/include \
 *          -target arm64-apple-ios15.0 -isysroot $(xcrun --sdk iphoneos --show-sdk-path) \
 *          -o whisper_bridge.o
 *
 * 3. Create static library:
 *    ar rcs libwhisper_bridge.a whisper_bridge.o
 *
 * 4. Link with libwhisper.a and libggml.a from step 1
 */

#ifndef WHISPER_BRIDGE_H
#define WHISPER_BRIDGE_H

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

/** Sample rate expected by whisper (16kHz) */
#define WHISPER_BRIDGE_SAMPLE_RATE 16000

/** Opaque context handle */
typedef void* WhisperBridgeContext;

// --- Lifecycle ---

/**
 * Initialize a whisper context from a model file.
 * @param model_path Path to the ggml model file (e.g., ggml-base.en.bin)
 * @return Opaque context handle, or NULL on failure
 */
WhisperBridgeContext whisper_bridge_init(const char* model_path);

/**
 * Free a whisper context and all associated resources.
 * @param ctx Context handle from whisper_bridge_init (NULL-safe)
 */
void whisper_bridge_free(WhisperBridgeContext ctx);

// --- Transcription ---

/**
 * Run full transcription on audio data.
 *
 * @param ctx       Context handle
 * @param n_threads Number of threads (0 = auto)
 * @param samples   Audio samples as 32-bit float, 16kHz mono
 * @param n_samples Number of samples
 * @param language  Language code (e.g., "en", "es", "auto")
 * @param translate If true, translate to English
 * @return 0 on success, non-zero on failure
 */
int whisper_bridge_transcribe(
    WhisperBridgeContext ctx,
    int n_threads,
    const float* samples,
    int n_samples,
    const char* language,
    bool translate
);

// --- Results ---

/**
 * Get the number of text segments from the last transcription.
 */
int whisper_bridge_segment_count(WhisperBridgeContext ctx);

/**
 * Get the text of a segment.
 * @return Pointer to text (valid until next transcription call)
 */
const char* whisper_bridge_segment_text(WhisperBridgeContext ctx, int index);

/**
 * Get the start timestamp of a segment (in centiseconds, multiply by 10 for ms).
 */
int64_t whisper_bridge_segment_t0(WhisperBridgeContext ctx, int index);

/**
 * Get the end timestamp of a segment (in centiseconds, multiply by 10 for ms).
 */
int64_t whisper_bridge_segment_t1(WhisperBridgeContext ctx, int index);

/**
 * Get the number of tokens in a segment (for confidence calculation).
 */
int whisper_bridge_segment_token_count(WhisperBridgeContext ctx, int segment_index);

/**
 * Get the probability of a token [0.0, 1.0].
 */
float whisper_bridge_segment_token_prob(
    WhisperBridgeContext ctx,
    int segment_index,
    int token_index
);

/**
 * Get the detected language code after transcription (e.g., "en", "es").
 * @return Language string (valid until context is freed), or "unknown"
 */
const char* whisper_bridge_detected_language(WhisperBridgeContext ctx);

// --- System Info ---

/**
 * Get system info string (CPU features, SIMD support, etc.)
 */
const char* whisper_bridge_system_info(void);

#ifdef __cplusplus
}
#endif

#endif /* WHISPER_BRIDGE_H */
