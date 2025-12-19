package com.augmentalis.voiceavanue.config

/**
 * Database configuration and feature flags.
 * Controls migration from direct database access to IPC-based access.
 *
 * Migration Strategy:
 * 1. Start with USE_IPC_DATABASE = false (direct access, current behavior)
 * 2. Test IPC layer in development builds
 * 3. Enable for beta users (canary deployment)
 * 4. Monitor for 48 hours (crashes, latency, memory)
 * 5. Enable for all users if metrics are good
 * 6. After 2 weeks of stability, remove direct access code
 *
 * Expected Benefits:
 * - 20 MB memory freed from main process
 * - Database crashes isolated to :database process
 * - Cross-app data sharing enabled (AVA AI, AVAConnect, BrowserAvanue)
 * - <50ms IPC latency (acceptable for non-UI-blocking operations)
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
object DatabaseConfig {

    /**
     * Feature flag: Use IPC-based database access.
     *
     * When true:
     * - DatabaseService runs in separate :database process
     * - All access goes through DatabaseClient (AIDL IPC)
     * - ContentProvider available for cross-app access
     * - Main process memory reduced by ~20 MB
     *
     * When false:
     * - Direct database access (current behavior)
     * - Database runs in main process
     * - Legacy code path
     *
     * Default: false (safe migration, test first)
     */
    const val USE_IPC_DATABASE = false

    /**
     * Enable detailed IPC logging for debugging.
     * Should be false in production builds.
     */
    const val DEBUG_IPC = false

    /**
     * IPC connection timeout in milliseconds.
     * If connection takes longer, operation fails.
     */
    const val IPC_CONNECT_TIMEOUT_MS = 5000L

    /**
     * Maximum number of automatic reconnection attempts.
     * After this, manual reconnection required.
     */
    const val MAX_RECONNECT_ATTEMPTS = 3

    /**
     * Delay between reconnection attempts in milliseconds.
     */
    const val RECONNECT_DELAY_MS = 1000L

    /**
     * Service idle timeout in milliseconds.
     * Service may be killed after this period of inactivity.
     */
    const val SERVICE_IDLE_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes

    /**
     * Health check interval in milliseconds.
     * Client checks service health periodically.
     */
    const val HEALTH_CHECK_INTERVAL_MS = 30 * 1000L // 30 seconds

    /**
     * Enable ContentProvider for cross-app access.
     * Requires USE_IPC_DATABASE = true to be useful.
     */
    const val ENABLE_CONTENT_PROVIDER = true

    /**
     * ContentProvider authority.
     * Must match AndroidManifest.xml declaration.
     */
    const val CONTENT_PROVIDER_AUTHORITY = "com.augmentalis.voiceavanue.database"
}
