package com.augmentalis.magiccode.plugins.core

/**
 * Logging interface for plugin system.
 *
 * Provides centralized logging with different severity levels and security audit capabilities.
 * Platform-specific implementations can route to appropriate logging frameworks.
 */
interface PluginLogger {
    /**
     * Log debug-level message.
     *
     * @param tag Tag identifying the source
     * @param message Log message
     * @param throwable Optional exception to log
     */
    fun debug(tag: String, message: String, throwable: Throwable? = null)

    /**
     * Log info-level message.
     *
     * @param tag Tag identifying the source
     * @param message Log message
     * @param throwable Optional exception to log
     */
    fun info(tag: String, message: String, throwable: Throwable? = null)

    /**
     * Log warning-level message.
     *
     * @param tag Tag identifying the source
     * @param message Log message
     * @param throwable Optional exception to log
     */
    fun warn(tag: String, message: String, throwable: Throwable? = null)

    /**
     * Log error-level message.
     *
     * @param tag Tag identifying the source
     * @param message Log message
     * @param throwable Optional exception to log
     */
    fun error(tag: String, message: String, throwable: Throwable? = null)

    /**
     * Log security audit event.
     *
     * Security events are logged at WARNING level with [SECURITY] prefix for
     * easy filtering and monitoring. Use for:
     * - Permission grants/denials
     * - Encryption key operations
     * - Migration events
     * - Security credential changes
     *
     * @param tag Tag identifying the security subsystem
     * @param event Security event description
     * @param throwable Optional exception related to the security event
     */
    fun security(tag: String, event: String, throwable: Throwable? = null)
}

/**
 * Default console logger implementation.
 *
 * Simple logger that prints to console/stdout. Platform-specific implementations
 * should provide more sophisticated logging (Android Logcat, iOS OSLog, etc.).
 */
class ConsolePluginLogger : PluginLogger {
    override fun debug(tag: String, message: String, throwable: Throwable?) {
        println("[DEBUG] [$tag] $message")
        throwable?.printStackTrace()
    }

    override fun info(tag: String, message: String, throwable: Throwable?) {
        println("[INFO] [$tag] $message")
        throwable?.printStackTrace()
    }

    override fun warn(tag: String, message: String, throwable: Throwable?) {
        println("[WARN] [$tag] $message")
        throwable?.printStackTrace()
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        println("[ERROR] [$tag] $message")
        throwable?.printStackTrace()
    }

    override fun security(tag: String, event: String, throwable: Throwable?) {
        println("[SECURITY] [$tag] $event")
        throwable?.printStackTrace()
    }
}

/**
 * Global logger instance.
 *
 * Provides centralized logging with security audit capabilities. Can be configured
 * to use platform-specific logger (Android Logcat, iOS OSLog, etc.).
 *
 * ## Security Audit Logging
 *
 * Use [security] method for security-relevant events:
 * - **Permission grants/denials**: When plugins request/receive permissions
 * - **Encryption operations**: Key generation, migration, recovery
 * - **Migration events**: Plain-text → encrypted storage transitions
 * - **Security credential changes**: Key invalidation, credential clearing
 *
 * Security events are logged with [SECURITY] prefix for easy filtering:
 * ```
 * adb logcat | grep SECURITY
 * ```
 *
 * ## Usage Example
 * ```kotlin
 * // Regular logging
 * PluginLog.d("MyTag", "Debug message")
 * PluginLog.i("MyTag", "Info message")
 * PluginLog.w("MyTag", "Warning message")
 * PluginLog.e("MyTag", "Error message", exception)
 *
 * // Security audit logging
 * PluginLog.security("PermissionStorage", "Permission granted: CAMERA to MyPlugin")
 * PluginLog.security("KeyManager", "Master key regenerated after credential clear")
 * PluginLog.security("Migration", "Migrated 15 permissions to encrypted storage")
 * ```
 *
 * @since 1.0.0
 */
object PluginLog {
    private var logger: PluginLogger = ConsolePluginLogger()

    /**
     * Set custom logger implementation.
     *
     * Allows platform-specific logger injection (Android Logcat, iOS OSLog, etc.).
     *
     * @param customLogger Logger instance to use
     * @since 1.0.0
     */
    fun setLogger(customLogger: PluginLogger) {
        logger = customLogger
    }

    /**
     * Log debug-level message.
     *
     * @param tag Tag identifying the source subsystem
     * @param message Debug message
     * @param throwable Optional exception to log
     * @since 1.0.0
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        logger.debug(tag, message, throwable)
    }

    /**
     * Log info-level message.
     *
     * @param tag Tag identifying the source subsystem
     * @param message Info message
     * @param throwable Optional exception to log
     * @since 1.0.0
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        logger.info(tag, message, throwable)
    }

    /**
     * Log warning-level message.
     *
     * @param tag Tag identifying the source subsystem
     * @param message Warning message
     * @param throwable Optional exception to log
     * @since 1.0.0
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        logger.warn(tag, message, throwable)
    }

    /**
     * Log error-level message.
     *
     * @param tag Tag identifying the source subsystem
     * @param message Error message
     * @param throwable Optional exception to log
     * @since 1.0.0
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        logger.error(tag, message, throwable)
    }

    /**
     * Log security audit event.
     *
     * Security events are logged with [SECURITY] prefix for easy filtering and
     * monitoring. Use for permission operations, encryption events, and security
     * credential changes.
     *
     * ## Security Event Categories
     * - **Permission events**: Grants, denials, revocations
     * - **Encryption events**: Key generation, rotation, invalidation
     * - **Migration events**: Plain-text → encrypted transitions
     * - **Credential events**: Security credential changes, device wipes
     *
     * ## Filtering Security Logs
     * ```bash
     * # Android
     * adb logcat | grep SECURITY
     *
     * # iOS
     * log stream --predicate 'eventMessage CONTAINS "SECURITY"'
     * ```
     *
     * @param tag Tag identifying the security subsystem (e.g., "KeyManager", "PermissionStorage")
     * @param event Security event description (be specific for audit trails)
     * @param throwable Optional exception related to the security event
     * @since 1.1.0
     */
    fun security(tag: String, event: String, throwable: Throwable? = null) {
        logger.security(tag, event, throwable)
    }
}
