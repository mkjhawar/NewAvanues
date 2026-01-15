package com.augmentalis.avacode.plugins

/**
 * Centralized error handling for plugin operations.
 *
 * Provides consistent error messages and logging across the plugin system.
 */
object PluginErrorHandler {
    private const val TAG = "PluginErrorHandler"

    /**
     * Error categories for classification.
     */
    enum class ErrorCategory {
        MANIFEST,
        VALIDATION,
        FILESYSTEM,
        DEPENDENCY,
        PERMISSION,
        INSTALLATION,
        RUNTIME,
        SECURITY,
        NAMESPACE,
        TRANSACTION
    }

    /**
     * Error context for detailed logging.
     */
    data class ErrorContext(
        val category: ErrorCategory,
        val pluginId: String?,
        val operation: String,
        val details: String,
        val exception: Throwable?
    )

    /**
     * Handle and log an error with context.
     *
     * @param context Error context
     * @return Appropriate PluginException
     */
    fun handleError(context: ErrorContext): PluginException {
        // Log the error with full context
        logError(context)

        // Return appropriate exception type
        return createException(context)
    }

    /**
     * Log error with context.
     */
    private fun logError(context: ErrorContext) {
        val pluginInfo = context.pluginId?.let { "Plugin: $it | " } ?: ""
        val message = "${context.category.name} ERROR | ${pluginInfo}${context.operation}: ${context.details}"

        when (context.category) {
            ErrorCategory.MANIFEST,
            ErrorCategory.VALIDATION,
            ErrorCategory.FILESYSTEM -> {
                PluginLog.e(TAG, message, context.exception)
            }
            ErrorCategory.SECURITY,
            ErrorCategory.PERMISSION -> {
                PluginLog.w(TAG, "SECURITY: $message", context.exception)
            }
            else -> {
                PluginLog.e(TAG, message, context.exception)
            }
        }
    }

    /**
     * Create appropriate exception from context.
     */
    private fun createException(context: ErrorContext): PluginException {
        return when (context.category) {
            ErrorCategory.MANIFEST -> {
                if (context.details.contains("not found", ignoreCase = true)) {
                    ManifestNotFoundException(context.pluginId ?: "unknown")
                } else {
                    ManifestInvalidException(context.details, context.exception)
                }
            }
            ErrorCategory.VALIDATION -> {
                ManifestInvalidException(context.details, context.exception)
            }
            ErrorCategory.FILESYSTEM -> {
                InstallationFailedException(
                    context.pluginId ?: "unknown",
                    context.details,
                    context.exception
                )
            }
            ErrorCategory.DEPENDENCY -> {
                if (context.details.contains("circular", ignoreCase = true)) {
                    CircularDependencyException(emptyList())
                } else {
                    DependencyUnresolvedException(
                        context.pluginId ?: "unknown",
                        "",
                        context.details
                    )
                }
            }
            ErrorCategory.PERMISSION -> {
                PermissionDeniedException(
                    context.pluginId ?: "unknown",
                    Permission.ACCESSIBILITY_SERVICES
                )
            }
            ErrorCategory.INSTALLATION -> {
                InstallationFailedException(
                    context.pluginId ?: "unknown",
                    context.details,
                    context.exception
                )
            }
            ErrorCategory.RUNTIME -> {
                PluginRuntimeException(
                    context.pluginId ?: "unknown",
                    context.details,
                    context.exception
                )
            }
            ErrorCategory.SECURITY -> {
                SecurityViolationException(
                    context.pluginId ?: "unknown",
                    context.details
                )
            }
            ErrorCategory.NAMESPACE -> {
                NamespaceCollisionException(context.pluginId ?: "unknown")
            }
            ErrorCategory.TRANSACTION -> {
                TransactionFailedException(
                    context.details,
                    context.exception
                )
            }
        }
    }

    /**
     * Wrap an operation with error handling.
     *
     * @param category Error category
     * @param pluginId Plugin identifier (optional)
     * @param operation Operation description
     * @param block Operation to execute
     * @return Result of operation
     * @throws PluginException if operation fails
     */
    inline fun <T> withErrorHandling(
        category: ErrorCategory,
        pluginId: String? = null,
        operation: String,
        block: () -> T
    ): T {
        return try {
            block()
        } catch (e: PluginException) {
            // Already a plugin exception, just rethrow
            throw e
        } catch (e: Exception) {
            // Wrap in appropriate plugin exception
            val context = ErrorContext(
                category = category,
                pluginId = pluginId,
                operation = operation,
                details = e.message ?: "Unknown error",
                exception = e
            )
            throw handleError(context)
        }
    }

    /**
     * Validate a condition and throw if false.
     *
     * @param condition Condition to check
     * @param category Error category
     * @param pluginId Plugin identifier (optional)
     * @param lazyMessage Error message provider
     * @throws PluginException if condition is false
     */
    inline fun require(
        condition: Boolean,
        category: ErrorCategory,
        pluginId: String? = null,
        lazyMessage: () -> String
    ) {
        if (!condition) {
            val context = ErrorContext(
                category = category,
                pluginId = pluginId,
                operation = "Validation",
                details = lazyMessage(),
                exception = null
            )
            throw handleError(context)
        }
    }

    /**
     * Get user-friendly error message from exception.
     *
     * @param exception The exception
     * @return User-friendly error message
     */
    fun getUserMessage(exception: PluginException): String {
        return when (exception) {
            is ManifestNotFoundException -> {
                "Plugin manifest file not found. Please ensure the plugin is correctly installed."
            }
            is ManifestInvalidException -> {
                "Plugin manifest is invalid or corrupted. ${exception.message}"
            }
            is DependencyUnresolvedException -> {
                "Cannot install plugin due to missing dependencies: ${exception.message}"
            }
            is CircularDependencyException -> {
                "Plugin has circular dependencies and cannot be loaded: ${exception.message}"
            }
            is PermissionDeniedException -> {
                exception.message ?: "Plugin requires permissions that have not been granted"
            }
            is InstallationFailedException -> {
                exception.message ?: "Failed to install plugin"
            }
            is PluginRuntimeException -> {
                exception.message ?: "Plugin encountered a runtime error"
            }
            is SecurityViolationException -> {
                exception.message ?: "Security violation detected from plugin"
            }
            is NamespaceCollisionException -> {
                exception.message ?: "Plugin conflicts with an already installed plugin"
            }
            else -> {
                "An error occurred: ${exception.message}"
            }
        }
    }
}
