package com.augmentalis.voiceavanue.access

import android.content.Context
import android.util.Log
import com.augmentalis.voiceavanue.config.DatabaseConfig

/**
 * Factory for creating DatabaseAccess instances.
 *
 * Selects implementation based on DatabaseConfig.USE_IPC_DATABASE flag:
 * - true: DatabaseClientAdapter (IPC-based, process isolation)
 * - false: DatabaseDirectAdapter (legacy, direct access)
 *
 * Usage:
 * ```kotlin
 * val database = DatabaseAccessFactory.create(context)
 * database.connect()
 * // Use database operations
 * database.disconnect()
 * ```
 *
 * Migration Strategy:
 * 1. Start with USE_IPC_DATABASE = false (legacy behavior)
 * 2. Test IPC implementation in development
 * 3. Enable for beta users (canary)
 * 4. Monitor metrics for 48 hours
 * 5. Enable for all users if stable
 * 6. Remove legacy code after 2 weeks
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
object DatabaseAccessFactory {

    private const val TAG = "DatabaseAccessFactory"

    /**
     * Create DatabaseAccess instance based on configuration.
     *
     * @param context Application context
     * @return DatabaseAccess implementation (IPC or direct)
     */
    fun create(context: Context): DatabaseAccess {
        return if (DatabaseConfig.USE_IPC_DATABASE) {
            Log.d(TAG, "Creating IPC-based database access (process isolation)")
            DatabaseClientAdapter(context.applicationContext)
        } else {
            Log.d(TAG, "Creating direct database access (legacy mode)")
            DatabaseDirectAdapter(context.applicationContext)
        }
    }

    /**
     * Create IPC-based database access explicitly.
     * Ignores USE_IPC_DATABASE flag.
     *
     * Useful for testing IPC layer even when flag is disabled.
     *
     * @param context Application context
     * @return DatabaseClientAdapter
     */
    fun createIpc(context: Context): DatabaseAccess {
        Log.d(TAG, "Creating IPC database access (explicit)")
        return DatabaseClientAdapter(context.applicationContext)
    }

    /**
     * Create direct database access explicitly.
     * Ignores USE_IPC_DATABASE flag.
     *
     * Useful for testing legacy behavior or performance comparison.
     *
     * @param context Application context
     * @return DatabaseDirectAdapter
     */
    fun createDirect(context: Context): DatabaseAccess {
        Log.d(TAG, "Creating direct database access (explicit)")
        return DatabaseDirectAdapter(context.applicationContext)
    }
}
