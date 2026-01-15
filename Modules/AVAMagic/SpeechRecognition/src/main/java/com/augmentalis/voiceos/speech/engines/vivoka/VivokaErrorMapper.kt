package com.augmentalis.voiceos.speech.engines.vivoka

// Use the structured SpeechError data class
import com.augmentalis.voiceos.speech.engines.common.SpeechError

/**
 * Vivoka-specific error codes (local definition as SDK doesn't export these)
 * TODO: Update when Vivoka SDK provides official error codes
 */
object RecognizerError {
    const val ERROR_UNKNOWN = -1
    const val ERROR_NETWORK_TIMEOUT = 1
    const val ERROR_NETWORK = 2
    const val ERROR_AUDIO = 3
    const val ERROR_SERVER = 4
    const val ERROR_CLIENT = 5
    const val ERROR_SPEECH_TIMEOUT = 6
    const val ERROR_NO_MATCH = 7
    const val ERROR_RECOGNIZER_BUSY = 8
    const val ERROR_INSUFFICIENT_PERMISSIONS = 9
    const val ERROR_NOT_INITIALIZED = 10
    const val ERROR_LICENSE_INVALID = 11
    const val ERROR_MODEL_NOT_FOUND = 12
}

/**
 * Maps Vivoka VSDK errors to common SpeechError format
 * Provides detailed error information for better debugging and recovery
 */
object VivokaErrorMapper {
    
    /**
     * Map Vivoka error codes to SpeechError
     */
    fun mapError(errorCode: String?, message: String?): SpeechError {
        val code = errorCode?.toIntOrNull() ?: RecognizerError.ERROR_UNKNOWN
        
        return when (code) {
            RecognizerError.ERROR_NETWORK_TIMEOUT -> SpeechError(
                code = SpeechError.ERROR_NETWORK,
                message = message ?: "Network timeout",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.RETRY_WITH_BACKOFF
            )
            
            RecognizerError.ERROR_NETWORK -> SpeechError(
                code = SpeechError.ERROR_NETWORK,
                message = message ?: "Network error",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.CHECK_CONNECTION
            )
            
            RecognizerError.ERROR_AUDIO -> SpeechError(
                code = SpeechError.ERROR_AUDIO,
                message = message ?: "Audio recording error",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.REINITIALIZE_AUDIO
            )
            
            RecognizerError.ERROR_SERVER -> SpeechError(
                code = SpeechError.ERROR_SERVER,
                message = message ?: "Server error",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.RETRY_WITH_BACKOFF
            )
            
            RecognizerError.ERROR_CLIENT -> SpeechError(
                code = SpeechError.ERROR_CLIENT,
                message = message ?: "Client error",
                isRecoverable = false,
                suggestedAction = SpeechError.Action.CHECK_CONFIGURATION
            )
            
            RecognizerError.ERROR_SPEECH_TIMEOUT -> SpeechError(
                code = SpeechError.ERROR_SPEECH_TIMEOUT,
                message = message ?: "Speech timeout - no speech detected",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.PROMPT_USER
            )
            
            RecognizerError.ERROR_NO_MATCH -> SpeechError(
                code = SpeechError.ERROR_NO_MATCH,
                message = message ?: "No recognition match found",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.RETRY
            )
            
            RecognizerError.ERROR_RECOGNIZER_BUSY -> SpeechError(
                code = SpeechError.ERROR_BUSY,
                message = message ?: "Recognizer is busy",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.WAIT_AND_RETRY
            )
            
            RecognizerError.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechError(
                code = SpeechError.ERROR_PERMISSIONS,
                message = message ?: "Insufficient permissions",
                isRecoverable = false,
                suggestedAction = SpeechError.Action.REQUEST_PERMISSIONS
            )
            
            RecognizerError.ERROR_NOT_INITIALIZED -> SpeechError(
                code = SpeechError.ERROR_NOT_INITIALIZED,
                message = message ?: "SDK not initialized",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.REINITIALIZE
            )
            
            RecognizerError.ERROR_LICENSE_INVALID -> SpeechError(
                code = SpeechError.ERROR_LICENSE,
                message = message ?: "Invalid license",
                isRecoverable = false,
                suggestedAction = SpeechError.Action.CHECK_LICENSE
            )
            
            RecognizerError.ERROR_MODEL_NOT_FOUND -> SpeechError(
                code = SpeechError.ERROR_MODEL,
                message = message ?: "Model not found",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.DOWNLOAD_MODEL
            )
            
            else -> SpeechError(
                code = SpeechError.ERROR_UNKNOWN,
                message = message ?: "Unknown error (code: $code)",
                isRecoverable = false,
                suggestedAction = SpeechError.Action.LOG_AND_REPORT
            )
        }
    }
    
    /**
     * Map Vivoka exception to SpeechError
     */
    fun mapException(exception: Exception): SpeechError {
        return when (exception) {
            is SecurityException -> SpeechError(
                code = SpeechError.ERROR_PERMISSIONS,
                message = "Security exception: ${exception.message}",
                isRecoverable = false,
                suggestedAction = SpeechError.Action.REQUEST_PERMISSIONS
            )
            
            is IllegalStateException -> SpeechError(
                code = SpeechError.ERROR_INVALID_STATE,
                message = "Invalid state: ${exception.message}",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.REINITIALIZE
            )

            // Note: OutOfMemoryError branch removed - it's an Error not Exception,
            // so it would never be caught here. OOM errors should be handled at a higher level.

            else -> SpeechError(
                code = SpeechError.ERROR_UNKNOWN,
                message = "Exception: ${exception.message}",
                isRecoverable = false,
                suggestedAction = SpeechError.Action.LOG_AND_REPORT
            )
        }
    }
    
    /**
     * Get user-friendly error message
     */
    fun getUserMessage(error: SpeechError): String {
        return when (error.code) {
            SpeechError.ERROR_NETWORK -> "Please check your internet connection"
            SpeechError.ERROR_AUDIO -> "Microphone access error. Please check permissions"
            SpeechError.ERROR_SPEECH_TIMEOUT -> "No speech detected. Please try again"
            SpeechError.ERROR_NO_MATCH -> "Could not understand. Please speak clearly"
            SpeechError.ERROR_BUSY -> "System is busy. Please wait"
            SpeechError.ERROR_PERMISSIONS -> "Microphone permission required"
            SpeechError.ERROR_LICENSE -> "License validation failed"
            SpeechError.ERROR_MODEL -> "Speech model not available"
            else -> "An error occurred. Please try again"
        }
    }
}

