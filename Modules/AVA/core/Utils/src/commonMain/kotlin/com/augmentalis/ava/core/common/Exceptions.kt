/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.ava.core.common

/**
 * Base exception for all AVA-specific errors
 *
 * Provides a common hierarchy for error handling across the AVA system.
 * All AVA exceptions should inherit from this class.
 */
sealed class AVAException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * Model not loaded or initialization failed
     *
     * Used when:
     * - ONNX model file not found
     * - Model failed to load
     * - Model initialization incomplete
     */
    class ModelNotLoadedException(
        message: String,
        cause: Throwable? = null
    ) : AVAException(message, cause)

    /**
     * Invalid input provided to a function
     *
     * Used when:
     * - Empty or null input where required
     * - Input validation fails
     * - Parameter constraints violated
     */
    class InvalidInputException(
        message: String,
        cause: Throwable? = null
    ) : AVAException(message, cause)

    /**
     * Required resource not found
     *
     * Used when:
     * - File not found
     * - Asset not found
     * - Resource missing from storage
     */
    class ResourceNotFoundException(
        message: String,
        cause: Throwable? = null
    ) : AVAException(message, cause)

    /**
     * Initialization failed or component not initialized
     *
     * Used when:
     * - Component used before initialization
     * - Initialization process failed
     * - Required setup incomplete
     */
    class InitializationException(
        message: String,
        cause: Throwable? = null
    ) : AVAException(message, cause)

    /**
     * Language pack or locale operation failed
     *
     * Used when:
     * - Language pack not installed
     * - Locale not available
     * - Language pack download/installation failed
     */
    class LanguagePackException(
        message: String,
        cause: Throwable? = null
    ) : AVAException(message, cause)

    /**
     * Security or authentication failure
     *
     * Used when:
     * - AON file authentication fails
     * - Package verification fails
     * - Integrity check fails
     * - Signature verification fails
     */
    class SecurityException(
        message: String,
        cause: Throwable? = null
    ) : AVAException(message, cause)

    /**
     * Network or download operation failed
     *
     * Used when:
     * - HTTP connection fails
     * - Download interrupted
     * - Network timeout
     */
    class NetworkException(
        message: String,
        cause: Throwable? = null
    ) : AVAException(message, cause)

    /**
     * Model loading or processing error
     *
     * Used when:
     * - Model file not found in assets
     * - Model download failed
     * - Model format error
     * - Model initialization failed
     */
    class ModelException(
        message: String,
        cause: Throwable? = null
    ) : AVAException(message, cause)
}
