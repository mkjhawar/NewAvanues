/**
 * VoiceOSConstants.kt - Centralized constants for VoiceOS platform
 *
 * Purpose:
 * - Eliminates hardcoded magic numbers throughout codebase
 * - Provides single source of truth for configuration values
 * - Enables easy tuning without modifying business logic
 * - Improves code readability and maintainability
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-09
 * Extracted to KMP: 2025-11-16
 */
package com.augmentalis.commandmanager

/**
 * VoiceOS Core constants - centralized configuration values
 *
 * All magic numbers extracted from across the codebase for maintainability.
 * Grouped by category for easy navigation.
 */
object VoiceOSConstants {

    /**
     * Tree Traversal Constants
     * Used by SafeNodeTraverser and AccessibilityNodeManager
     */
    object TreeTraversal {
        /** Maximum depth for accessibility tree traversal to prevent stack overflow */
        const val MAX_DEPTH = 50

        /** Warning threshold for slow traversal operations (milliseconds) */
        const val TRAVERSAL_SLOW_THRESHOLD_MS = 100L
    }

    /**
     * Timing and Debouncing Constants
     * Used by Debouncer and various event handlers
     */
    object Timing {
        /** Default throttle/debounce delay for events (milliseconds) */
        const val THROTTLE_DELAY_MS = 500L

        /** Initialization delay for service startup (milliseconds) */
        const val INIT_DELAY_MS = 200L

        /** Event debounce interval (milliseconds) */
        const val EVENT_DEBOUNCE_MS = 1000L

        /** Memory monitor check interval (milliseconds) */
        const val MEMORY_MONITOR_INTERVAL_MS = 30000L

        /** Performance benchmark threshold - slow operation warning (milliseconds) */
        const val PERFORMANCE_WARNING_THRESHOLD_MS = 100L
    }

    /**
     * Cache and Buffer Sizes
     * Used throughout for in-memory data structures
     */
    object Cache {
        /** Default cache size for most LRU caches */
        const val DEFAULT_CACHE_SIZE = 100

        /** Large cache size for frequently accessed data */
        const val LARGE_CACHE_SIZE = 500

        /** Small cache size for limited resources */
        const val SMALL_CACHE_SIZE = 50
    }

    /**
     * Database Constants
     * Used by Room database operations and migrations
     */
    object Database {
        /** Batch size for bulk insert operations */
        const val BATCH_INSERT_SIZE = 100

        /** Query timeout for database operations (milliseconds) */
        const val QUERY_TIMEOUT_MS = 5000L

        /** Maximum retry attempts for failed transactions */
        const val MAX_TRANSACTION_RETRIES = 3

        /** Data retention period for old scraped data (days) */
        const val DATA_RETENTION_DAYS = 30

        /** Maximum database size before cleanup (MB) */
        const val MAX_DATABASE_SIZE_MB = 100

        /** Maximum number of database backups to retain */
        const val MAX_BACKUP_COUNT = 10
    }

    /**
     * Performance and Resource Limits
     */
    object Performance {
        /** Maximum memory footprint increase allowed (MB) */
        const val MAX_MEMORY_INCREASE_MB = 2

        /** Target latency for permission operations (milliseconds) */
        const val PERMISSION_OPERATION_TARGET_LATENCY_MS = 5L

        /** Maximum concurrent operations */
        const val MAX_CONCURRENT_OPERATIONS = 10

        /** Thread pool size for background operations */
        const val BACKGROUND_THREAD_POOL_SIZE = 4
    }

    /**
     * Rate Limiting Constants
     * Used for command throttling and anti-spam
     */
    object RateLimit {
        /** Maximum commands per minute per user */
        const val MAX_COMMANDS_PER_MINUTE = 60

        /** Maximum commands per hour per user */
        const val MAX_COMMANDS_PER_HOUR = 1000

        /** Cooldown period after rate limit hit (milliseconds) */
        const val RATE_LIMIT_COOLDOWN_MS = 60000L

        /** Sliding window size for rate limiting (milliseconds) */
        const val RATE_LIMIT_WINDOW_MS = 60000L
    }

    /**
     * Circuit Breaker Constants
     * Used for fault tolerance in database and external calls
     */
    object CircuitBreaker {
        /** Failure threshold before opening circuit */
        const val FAILURE_THRESHOLD = 5

        /** Success threshold to close circuit */
        const val SUCCESS_THRESHOLD = 2

        /** Timeout in open state before attempting half-open (milliseconds) */
        const val OPEN_TIMEOUT_MS = 30000L

        /** Timeout for half-open state (milliseconds) */
        const val HALF_OPEN_TIMEOUT_MS = 10000L
    }

    /**
     * Logging Constants
     */
    object Logging {
        /** Maximum log message length before truncation */
        const val MAX_LOG_LENGTH = 4000

        /** Maximum number of log entries to retain in memory */
        const val MAX_LOG_BUFFER_SIZE = 1000

        /** PII redaction - maximum characters to show before/after redaction */
        const val PII_CONTEXT_LENGTH = 3
    }

    /**
     * UI and Overlay Constants
     */
    object UI {
        /** Default overlay z-index for numbered overlays */
        const val OVERLAY_Z_INDEX = 1000

        /** Animation duration for UI transitions (milliseconds) */
        const val ANIMATION_DURATION_MS = 300L

        /** Toast duration (milliseconds) */
        const val TOAST_DURATION_MS = 2000L

        /** Grid overlay spacing (dp) */
        const val GRID_SPACING_DP = 50
    }

    /**
     * Security Constants
     */
    object Security {
        /** AES encryption key size (bits) */
        const val AES_KEY_SIZE_BITS = 256

        /** Maximum failed authentication attempts before lockout */
        const val MAX_AUTH_ATTEMPTS = 5

        /** Authentication lockout duration (milliseconds) */
        const val AUTH_LOCKOUT_MS = 300000L

        /** Session timeout (milliseconds) */
        const val SESSION_TIMEOUT_MS = 3600000L
    }

    /**
     * Network and API Constants
     */
    object Network {
        /** HTTP connection timeout (milliseconds) */
        const val CONNECTION_TIMEOUT_MS = 10000L

        /** HTTP read timeout (milliseconds) */
        const val READ_TIMEOUT_MS = 15000L

        /** Maximum retry attempts for network operations */
        const val MAX_NETWORK_RETRIES = 3

        /** Backoff multiplier for retry attempts */
        const val RETRY_BACKOFF_MULTIPLIER = 2
    }

    /**
     * Voice Recognition Constants
     */
    object VoiceRecognition {
        /** Minimum confidence threshold for accepting voice commands */
        const val MIN_CONFIDENCE_THRESHOLD = 0.7f

        /** Maximum audio recording duration (milliseconds) */
        const val MAX_RECORDING_DURATION_MS = 30000L

        /** Silence detection threshold (milliseconds) */
        const val SILENCE_THRESHOLD_MS = 2000L
    }

    /**
     * Regex and Input Validation Constants
     */
    object Validation {
        /** Maximum input length for user text fields */
        const val MAX_INPUT_LENGTH = 1000

        /** Maximum regex pattern complexity (character count) */
        const val MAX_REGEX_PATTERN_LENGTH = 500

        /** Regex timeout to prevent ReDoS attacks (milliseconds) */
        const val REGEX_TIMEOUT_MS = 1000L
    }

    /**
     * File and Storage Constants
     */
    object Storage {
        /** Maximum file size for uploads (MB) */
        const val MAX_UPLOAD_SIZE_MB = 10

        /** Cache directory cleanup threshold (MB) */
        const val CACHE_CLEANUP_THRESHOLD_MB = 100

        /** Backup retention count */
        const val MAX_BACKUP_COUNT = 5
    }

    /**
     * Testing and Debug Constants
     */
    object Testing {
        /** Test timeout for integration tests (milliseconds) */
        const val TEST_TIMEOUT_MS = 30000L

        /** Maximum test retry attempts */
        const val MAX_TEST_RETRIES = 3

        /** Test data generation seed for reproducibility */
        const val TEST_SEED = 42L
    }

    /**
     * Accessibility Constants
     */
    object Accessibility {
        /** Maximum clickable elements to track */
        const val MAX_TRACKED_ELEMENTS = 1000

        /** Minimum element size for interaction (dp) */
        const val MIN_ELEMENT_SIZE_DP = 24

        /** Maximum hierarchy depth to scan */
        const val MAX_HIERARCHY_DEPTH = 50
    }

    /**
     * Metrics and Observability Constants
     * Used by CommandMetricsCollector and analytics components
     */
    object Metrics {
        /** Maximum number of unique commands to track in metrics */
        const val MAX_METRICS_COMMANDS = 500

        /** Metrics time window duration (milliseconds) - 1 hour */
        const val METRICS_WINDOW_DURATION_MS = 3600000L

        /** Maximum execution times to store per command */
        const val MAX_STORED_EXECUTION_TIMES = 1000

        /** Percentile thresholds for performance analysis */
        const val P50_PERCENTILE = 50.0
        const val P95_PERCENTILE = 95.0
        const val P99_PERCENTILE = 99.0

        /** Metrics export interval (milliseconds) - 5 minutes */
        const val METRICS_EXPORT_INTERVAL_MS = 300000L

        /** Slow command threshold (milliseconds) - commands slower than this are flagged */
        const val SLOW_COMMAND_THRESHOLD_MS = 1000L
    }

    /**
     * Overlay Display Constants
     * Used by NumberOverlay, GridOverlay, CommandDisambiguationOverlay, and CursorMenuOverlay
     */
    object Overlays {
        /** Short auto-hide delay for transient overlays (milliseconds) */
        const val AUTO_HIDE_SHORT_MS = 5000L

        /** Medium auto-hide delay for standard overlays (milliseconds) */
        const val AUTO_HIDE_MEDIUM_MS = 10000L

        /** Long auto-hide delay for informational overlays (milliseconds) */
        const val AUTO_HIDE_LONG_MS = 30000L

        /** Extended auto-hide delay for learning mode overlays (milliseconds) */
        const val AUTO_HIDE_EXTENDED_MS = 45000L

        /** Maximum number of overlays visible simultaneously */
        const val MAX_OVERLAYS_VISIBLE = 99

        /** Maximum number of labels to show on overlay */
        const val MAX_LABELS_VISIBLE = 100

        /** Overlay fade-in duration (milliseconds) */
        const val FADE_IN_DURATION_MS = 200L

        /** Overlay fade-out duration (milliseconds) */
        const val FADE_OUT_DURATION_MS = 300L

        /** Minimum overlay display time before allowing dismissal (milliseconds) */
        const val MIN_DISPLAY_TIME_MS = 1000L
    }

    /**
     * Animation Constants
     * Used throughout UI components for consistent animation timing
     */
    object Animation {
        /** Short animation duration for quick transitions (milliseconds) */
        const val SHORT_DURATION_MS = 200L

        /** Medium animation duration for standard transitions (milliseconds) */
        const val MEDIUM_DURATION_MS = 300L

        /** Long animation duration for complex transitions (milliseconds) */
        const val LONG_DURATION_MS = 500L

        /** Fade animation duration (milliseconds) */
        const val FADE_DURATION_MS = 200L
    }

    /**
     * Battery and Power Management Constants
     * Used for power-aware feature gating
     */
    object Battery {
        /** Minimum battery level required for learning mode (percentage) */
        const val MIN_BATTERY_LEVEL_FOR_LEARNING = 20
    }
}
