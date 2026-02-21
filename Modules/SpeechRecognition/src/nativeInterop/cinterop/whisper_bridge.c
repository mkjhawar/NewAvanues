/**
 * whisper_bridge.c - Implementation of the minimal C bridge for whisper.cpp
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 *
 * Wraps whisper.cpp's C API into simpler function calls for Kotlin/Native cinterop.
 * This file includes the full whisper.h internally but only exposes whisper_bridge.h
 * to the cinterop layer.
 */

#include "whisper_bridge.h"
#include "whisper.h"

#include <stdlib.h>
#include <string.h>

// --- Lifecycle ---

WhisperBridgeContext whisper_bridge_init(const char* model_path) {
    if (!model_path) return NULL;

    struct whisper_context_params cparams = whisper_context_default_params();
    struct whisper_context* ctx = whisper_init_from_file_with_params(model_path, cparams);

    return (WhisperBridgeContext)ctx;
}

void whisper_bridge_free(WhisperBridgeContext ctx) {
    if (!ctx) return;
    whisper_free((struct whisper_context*)ctx);
}

// --- Transcription ---

int whisper_bridge_transcribe(
    WhisperBridgeContext ctx,
    int n_threads,
    const float* samples,
    int n_samples,
    const char* language,
    bool translate
) {
    if (!ctx || !samples || n_samples <= 0) return -1;

    struct whisper_context* wctx = (struct whisper_context*)ctx;

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime   = false;
    params.print_progress   = false;
    params.print_timestamps = false;
    params.print_special    = false;
    params.translate        = translate;
    params.language         = language ? language : "en";
    params.n_threads        = n_threads > 0 ? n_threads : 4;
    params.offset_ms        = 0;
    params.no_context       = true;
    params.single_segment   = false;

    whisper_reset_timings(wctx);

    return whisper_full(wctx, params, samples, n_samples);
}

// --- Results ---

int whisper_bridge_segment_count(WhisperBridgeContext ctx) {
    if (!ctx) return 0;
    return whisper_full_n_segments((struct whisper_context*)ctx);
}

const char* whisper_bridge_segment_text(WhisperBridgeContext ctx, int index) {
    if (!ctx) return "";
    return whisper_full_get_segment_text((struct whisper_context*)ctx, index);
}

int64_t whisper_bridge_segment_t0(WhisperBridgeContext ctx, int index) {
    if (!ctx) return 0;
    return whisper_full_get_segment_t0((struct whisper_context*)ctx, index);
}

int64_t whisper_bridge_segment_t1(WhisperBridgeContext ctx, int index) {
    if (!ctx) return 0;
    return whisper_full_get_segment_t1((struct whisper_context*)ctx, index);
}

int whisper_bridge_segment_token_count(WhisperBridgeContext ctx, int segment_index) {
    if (!ctx) return 0;
    return whisper_full_n_tokens((struct whisper_context*)ctx, segment_index);
}

float whisper_bridge_segment_token_prob(
    WhisperBridgeContext ctx,
    int segment_index,
    int token_index
) {
    if (!ctx) return 0.0f;
    return whisper_full_get_token_p((struct whisper_context*)ctx, segment_index, token_index);
}

const char* whisper_bridge_detected_language(WhisperBridgeContext ctx) {
    if (!ctx) return "unknown";
    struct whisper_context* wctx = (struct whisper_context*)ctx;
    int lang_id = whisper_full_lang_id(wctx);
    const char* lang_str = whisper_lang_str(lang_id);
    return lang_str ? lang_str : "unknown";
}

// --- System Info ---

const char* whisper_bridge_system_info(void) {
    return whisper_print_system_info();
}
