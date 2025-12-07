/**
 * VoiceOSException.kt - Base exception hierarchy for VoiceOS
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-09
 * Extracted to KMP: 2025-11-17
 * Phase: 4 (Code Quality) → Phase 5 (KMP Migration)
 */
package com.augmentalis.voiceos.exceptions

/**
 * Base exception for all VoiceOS-specific errors
 *
 * Provides a common base for all custom exceptions in the VoiceOS system,
 * enabling consistent error handling and reporting.
 *
 * @property message Error message describing what went wrong
 * @property cause Optional underlying exception that caused this error
 * @property errorCode Optional error code for programmatic error handling
 */
open class VoiceOSException(
    message: String,
    cause: Throwable? = null,
    val errorCode: String? = null
) : RuntimeException(message, cause) {

    /**
     * Get full error details including error code if available
     */
    fun getFullMessage(): String {
        return if (errorCode != null) {
            "[$errorCode] $message"
        } else {
            message ?: "Unknown VoiceOS error"
        }
    }

    /**
     * Check if this exception was caused by a specific exception type
     */
    inline fun <reified T : Throwable> isCausedBy(): Boolean {
        var current: Throwable? = cause
        while (current != null) {
            if (current is T) return true
            current = current.cause
        }
        return false
    }
}

/**
 * Database-related exceptions
 */
sealed class DatabaseException(
    message: String,
    cause: Throwable? = null,
    errorCode: String? = null
) : VoiceOSException(message, cause, errorCode) {

    /**
     * Database backup operation failed
     */
    class BackupException(
        message: String,
        cause: Throwable? = null
    ) : DatabaseException(message, cause, "DB_BACKUP_FAILED")

    /**
     * Database restore operation failed
     */
    class RestoreException(
        message: String,
        cause: Throwable? = null
    ) : DatabaseException(message, cause, "DB_RESTORE_FAILED")

    /**
     * Database integrity check failed
     */
    class IntegrityException(
        message: String,
        cause: Throwable? = null,
        val corruptionDetails: List<String> = emptyList()
    ) : DatabaseException(message, cause, "DB_INTEGRITY_FAILED") {
        override fun toString(): String {
            return if (corruptionDetails.isNotEmpty()) {
                "${getFullMessage()}: ${corruptionDetails.joinToString(", ")}"
            } else {
                getFullMessage()
            }
        }
    }

    /**
     * Database migration failed
     */
    class MigrationException(
        message: String,
        cause: Throwable? = null,
        val fromVersion: Int? = null,
        val toVersion: Int? = null
    ) : DatabaseException(message, cause, "DB_MIGRATION_FAILED") {
        override fun toString(): String {
            return if (fromVersion != null && toVersion != null) {
                "${getFullMessage()} (v$fromVersion → v$toVersion)"
            } else {
                getFullMessage()
            }
        }
    }

    /**
     * Database transaction failed
     */
    class TransactionException(
        message: String,
        cause: Throwable? = null
    ) : DatabaseException(message, cause, "DB_TRANSACTION_FAILED")
}

/**
 * Security-related exceptions
 */
sealed class SecurityException(
    message: String,
    cause: Throwable? = null,
    errorCode: String? = null
) : VoiceOSException(message, cause, errorCode) {

    /**
     * Encryption operation failed
     */
    class EncryptionException(
        message: String,
        cause: Throwable? = null
    ) : SecurityException(message, cause, "SECURITY_ENCRYPTION_FAILED")

    /**
     * Decryption operation failed
     */
    class DecryptionException(
        message: String,
        cause: Throwable? = null
    ) : SecurityException(message, cause, "SECURITY_DECRYPTION_FAILED")

    /**
     * Signature validation failed
     */
    class SignatureException(
        message: String,
        cause: Throwable? = null,
        val packageName: String? = null
    ) : SecurityException(message, cause, "SECURITY_SIGNATURE_INVALID") {
        override fun toString(): String {
            return if (packageName != null) {
                "${getFullMessage()} (package: $packageName)"
            } else {
                getFullMessage()
            }
        }
    }

    /**
     * Unauthorized access attempt
     */
    class UnauthorizedException(
        message: String,
        cause: Throwable? = null,
        val packageName: String? = null
    ) : SecurityException(message, cause, "SECURITY_UNAUTHORIZED") {
        override fun toString(): String {
            return if (packageName != null) {
                "${getFullMessage()} (package: $packageName)"
            } else {
                getFullMessage()
            }
        }
    }

    /**
     * Keystore operation failed
     */
    class KeystoreException(
        message: String,
        cause: Throwable? = null
    ) : SecurityException(message, cause, "SECURITY_KEYSTORE_FAILED")
}

/**
 * Command processing exceptions
 */
sealed class CommandException(
    message: String,
    cause: Throwable? = null,
    errorCode: String? = null
) : VoiceOSException(message, cause, errorCode) {

    /**
     * Command execution failed
     */
    class ExecutionException(
        message: String,
        cause: Throwable? = null,
        val commandText: String? = null
    ) : CommandException(message, cause, "COMMAND_EXECUTION_FAILED") {
        override fun toString(): String {
            return if (commandText != null) {
                "${getFullMessage()} (command: \"$commandText\")"
            } else {
                getFullMessage()
            }
        }
    }

    /**
     * Command parsing failed
     */
    class ParsingException(
        message: String,
        cause: Throwable? = null,
        val rawInput: String? = null
    ) : CommandException(message, cause, "COMMAND_PARSING_FAILED") {
        override fun toString(): String {
            return if (rawInput != null) {
                "${getFullMessage()} (input: \"$rawInput\")"
            } else {
                getFullMessage()
            }
        }
    }

    /**
     * Command rate limit exceeded
     */
    class RateLimitException(
        message: String,
        cause: Throwable? = null,
        val commandId: String? = null,
        val retryAfterMs: Long? = null
    ) : CommandException(message, cause, "COMMAND_RATE_LIMIT") {
        override fun toString(): String {
            val details = mutableListOf<String>()
            commandId?.let { details.add("command: $it") }
            retryAfterMs?.let { details.add("retry after: ${it}ms") }
            return if (details.isNotEmpty()) {
                "${getFullMessage()} (${details.joinToString(", ")})"
            } else {
                getFullMessage()
            }
        }
    }

    /**
     * Command circuit breaker open
     */
    class CircuitBreakerException(
        message: String,
        cause: Throwable? = null,
        val commandId: String? = null
    ) : CommandException(message, cause, "COMMAND_CIRCUIT_BREAKER_OPEN")
}

/**
 * Web scraping exceptions
 */
sealed class ScrapingException(
    message: String,
    cause: Throwable? = null,
    errorCode: String? = null
) : VoiceOSException(message, cause, errorCode) {

    /**
     * Element scraping failed
     */
    class ElementException(
        message: String,
        cause: Throwable? = null,
        val elementId: String? = null
    ) : ScrapingException(message, cause, "SCRAPING_ELEMENT_FAILED")

    /**
     * Hierarchy scraping failed
     */
    class HierarchyException(
        message: String,
        cause: Throwable? = null
    ) : ScrapingException(message, cause, "SCRAPING_HIERARCHY_FAILED")

    /**
     * Cache operation failed
     */
    class CacheException(
        message: String,
        cause: Throwable? = null
    ) : ScrapingException(message, cause, "SCRAPING_CACHE_FAILED")
}

/**
 * Privacy/consent exceptions
 */
sealed class PrivacyException(
    message: String,
    cause: Throwable? = null,
    errorCode: String? = null
) : VoiceOSException(message, cause, errorCode) {

    /**
     * Consent not granted for operation
     */
    class ConsentException(
        message: String,
        cause: Throwable? = null,
        val consentType: String? = null
    ) : PrivacyException(message, cause, "PRIVACY_CONSENT_REQUIRED") {
        override fun toString(): String {
            return if (consentType != null) {
                "${getFullMessage()} (type: $consentType)"
            } else {
                getFullMessage()
            }
        }
    }

    /**
     * Data retention policy violation
     */
    class RetentionException(
        message: String,
        cause: Throwable? = null
    ) : PrivacyException(message, cause, "PRIVACY_RETENTION_VIOLATION")
}

/**
 * Accessibility service exceptions
 */
sealed class AccessibilityException(
    message: String,
    cause: Throwable? = null,
    errorCode: String? = null
) : VoiceOSException(message, cause, errorCode) {

    /**
     * Accessibility service not connected
     */
    class ServiceException(
        message: String,
        cause: Throwable? = null
    ) : AccessibilityException(message, cause, "ACCESSIBILITY_SERVICE_UNAVAILABLE")

    /**
     * Node traversal failed
     */
    class NodeException(
        message: String,
        cause: Throwable? = null
    ) : AccessibilityException(message, cause, "ACCESSIBILITY_NODE_FAILED")

    /**
     * Action performance failed
     */
    class ActionException(
        message: String,
        cause: Throwable? = null,
        val actionId: Int? = null
    ) : AccessibilityException(message, cause, "ACCESSIBILITY_ACTION_FAILED")
}
