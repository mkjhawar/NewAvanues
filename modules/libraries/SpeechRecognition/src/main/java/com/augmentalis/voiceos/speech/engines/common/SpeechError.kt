/**
 * SpeechError.kt - Data classes for structured error handling
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-09
 * 
 * Provides structured error information with recovery guidance
 * Used by Vivoka and other engines for error handling
 */
package com.augmentalis.voiceos.speech.engines.common

/**
 * Data class representing a speech recognition error with recovery information
 */
data class SpeechError(
    val code: Int,
    val message: String,
    val isRecoverable: Boolean = false,
    val suggestedAction: Action = Action.LOG_AND_REPORT
) {
    
    /**
     * Suggested recovery actions for errors
     */
    enum class Action {
        RETRY,
        RETRY_WITH_BACKOFF,
        WAIT_AND_RETRY,
        CHECK_CONNECTION,
        CHECK_CONFIGURATION,
        CHECK_LICENSE,
        REQUEST_PERMISSIONS,
        REINITIALIZE,
        REINITIALIZE_AUDIO,
        DOWNLOAD_MODEL,
        CLEAR_CACHE,
        PROMPT_USER,
        LOG_AND_REPORT
    }
    
    companion object {
        // Import all error codes from SpeechErrorCodes object
        const val ERROR_UNKNOWN = -1
        const val ERROR_NOT_INITIALIZED = 1001
        const val ERROR_INVALID_STATE = 1002
        const val ERROR_PERMISSIONS = 1003
        
        // Network and connectivity errors
        const val ERROR_NETWORK = 1006
        const val ERROR_NETWORK_TIMEOUT = 1101
        const val ERROR_SERVER = 1102
        const val ERROR_CLIENT = 1103
        
        // Recognition errors
        const val ERROR_AUDIO = 1301
        const val ERROR_SPEECH_TIMEOUT = 1302
        const val ERROR_NO_MATCH = 1303
        const val ERROR_BUSY = 1304
        
        // Resource errors
        const val ERROR_MEMORY = 1401
        const val ERROR_LICENSE = 1402
        const val ERROR_MODEL = 1403
        
        /**
         * Create a standard error for common cases
         */
        fun networkError(message: String? = null): SpeechError {
            return SpeechError(
                code = ERROR_NETWORK,
                message = message ?: "Network error",
                isRecoverable = true,
                suggestedAction = Action.CHECK_CONNECTION
            )
        }
        
        fun audioError(message: String? = null): SpeechError {
            return SpeechError(
                code = ERROR_AUDIO,
                message = message ?: "Audio error",
                isRecoverable = true,
                suggestedAction = Action.REINITIALIZE_AUDIO
            )
        }
        
        fun timeoutError(message: String? = null): SpeechError {
            return SpeechError(
                code = ERROR_SPEECH_TIMEOUT,
                message = message ?: "Speech timeout",
                isRecoverable = true,
                suggestedAction = Action.PROMPT_USER
            )
        }
        
        fun unknownError(message: String? = null): SpeechError {
            return SpeechError(
                code = ERROR_UNKNOWN,
                message = message ?: "Unknown error",
                isRecoverable = false,
                suggestedAction = Action.LOG_AND_REPORT
            )
        }
    }
}
