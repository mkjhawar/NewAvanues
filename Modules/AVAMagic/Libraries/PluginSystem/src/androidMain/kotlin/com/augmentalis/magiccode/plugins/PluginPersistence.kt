package com.augmentalis.avacode.plugins

import com.augmentalis.avacode.plugins.PluginLog

/**
 * Android plugin persistence implementation.
 *
 * Currently uses in-memory storage. SQLDelight migration planned for future.
 * See: libraries/core/database/ for SQLDelight patterns when ready.
 */

/**
 * Create default Android persistence implementation.
 *
 * Returns in-memory storage. When SQLDelight migration is complete,
 * this can be updated to use persistent storage.
 *
 * @param appDataDir Application data directory (unused until SQLDelight migration)
 * @return InMemoryPluginPersistence instance
 */
actual fun createDefaultPluginPersistence(appDataDir: String): PluginPersistence {
    PluginLog.d(
        "AndroidPluginPersistence",
        "Using in-memory plugin storage. SQLDelight migration pending."
    )
    return InMemoryPluginPersistence()
}
