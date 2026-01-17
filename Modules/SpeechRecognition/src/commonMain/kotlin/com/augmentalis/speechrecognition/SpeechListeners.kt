/**
 * SpeechListeners.kt - Speech recognition listener types (functional)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 * Updated: 2026-01-18 - Migrated to KMP commonMain
 *
 * Using functional types instead of interfaces per VOS4 standards.
 */
package com.augmentalis.speechrecognition

/**
 * Functional type for speech result callbacks
 */
typealias OnSpeechResultListener = (result: RecognitionResult) -> Unit

/**
 * Functional type for error callbacks
 */
typealias OnSpeechErrorListener = (error: SpeechError) -> Unit

/**
 * Functional type for error callbacks (string version for compatibility)
 */
typealias OnSpeechErrorStringListener = (error: String, code: Int) -> Unit

/**
 * Functional type for state change callbacks
 */
typealias OnStateChangeListener = (state: ServiceState, message: String?) -> Unit

/**
 * Functional type for partial result callbacks
 */
typealias OnPartialResultListener = (partialText: String) -> Unit

/**
 * Functional type for ready state callbacks
 */
typealias OnReadyListener = () -> Unit

/**
 * Combined listener holder for speech services.
 * Direct implementation - no interface.
 */
class SpeechListenerManager {
    var onResult: OnSpeechResultListener? = null
    var onError: OnSpeechErrorListener? = null
    var onErrorString: OnSpeechErrorStringListener? = null
    var onStateChange: OnStateChangeListener? = null
    var onPartialResult: OnPartialResultListener? = null
    var onReady: OnReadyListener? = null

    fun notifyResult(result: RecognitionResult) {
        onResult?.invoke(result)
    }

    fun notifyError(error: SpeechError) {
        onError?.invoke(error)
        onErrorString?.invoke(error.message, error.code)
    }

    fun notifyError(error: String, code: Int = -1) {
        onErrorString?.invoke(error, code)
        onError?.invoke(SpeechError(code, error))
    }

    fun notifyStateChange(state: ServiceState, message: String? = null) {
        onStateChange?.invoke(state, message)
    }

    fun notifyPartialResult(partialText: String) {
        onPartialResult?.invoke(partialText)
    }

    fun notifyReady() {
        onReady?.invoke()
    }

    fun clear() {
        onResult = null
        onError = null
        onErrorString = null
        onStateChange = null
        onPartialResult = null
        onReady = null
    }
}
