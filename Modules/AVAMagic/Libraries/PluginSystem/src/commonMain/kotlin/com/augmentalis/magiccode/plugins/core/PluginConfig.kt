package com.augmentalis.avacode.plugins.core

/**
 * Plugin system configuration.
 *
 * Global configuration for plugin loading, validation, and resource management.
 */
data class PluginConfig(
    /**
     * Maximum plugin size in bytes.
     * Default: 200 MB (209,715,200 bytes)
     */
    val maxPluginSizeBytes: Long = 200 * 1024 * 1024,

    /**
     * Enable hot-reload for theme development.
     * Default: false (production), true (development)
     */
    val enableThemeHotReload: Boolean = false,

    /**
     * Asset cache size limit in bytes.
     * Default: 50 MB (52,428,800 bytes)
     */
    val assetCacheSizeBytes: Long = 50 * 1024 * 1024,

    /**
     * Maximum number of concurrent plugin operations.
     * Default: 3
     */
    val maxConcurrentOperations: Int = 3,

    /**
     * Theme switching timeout in milliseconds.
     * Target: <500ms for 50 UI elements
     */
    val themeSwitchTimeoutMs: Long = 500,

    /**
     * Asset resolution timeout in milliseconds.
     */
    val assetResolutionTimeoutMs: Long = 5000,

    /**
     * Plugin installation timeout in milliseconds.
     * Target: <10s
     */
    val installTimeoutMs: Long = 10000,

    /**
     * Enable strict manifest validation.
     * If true, fail on any manifest validation error.
     * If false, log warnings for non-critical errors.
     */
    val strictManifestValidation: Boolean = true,

    /**
     * Checkpoint retention period in milliseconds.
     * Checkpoints older than this are automatically cleaned up.
     * Default: 7 days
     */
    val checkpointRetentionMs: Long = 7 * 24 * 60 * 60 * 1000,

    /**
     * Enable plugin sandboxing.
     * Should always be true in production.
     */
    val enableSandboxing: Boolean = true,

    /**
     * Enable code signing verification.
     * Should always be true in production for REGISTERED and VERIFIED plugins.
     */
    val enableCodeSigning: Boolean = true
)
