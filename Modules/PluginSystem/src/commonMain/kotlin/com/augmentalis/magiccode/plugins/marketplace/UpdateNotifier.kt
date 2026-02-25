package com.augmentalis.magiccode.plugins.marketplace

import com.augmentalis.magiccode.plugins.core.PluginLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * Background service for checking and notifying about plugin updates.
 *
 * UpdateNotifier periodically checks the marketplace for updates to installed
 * plugins and notifies registered listeners when updates are available.
 * It provides both callback-based and StateFlow-based notification mechanisms.
 *
 * ## Features
 * - Periodic background checking with configurable interval
 * - Manual update check triggering
 * - Multiple notification callbacks
 * - StateFlow for reactive UI updates
 * - Debouncing to prevent excessive API calls
 * - Error handling with retry logic
 *
 * ## Usage
 * ```kotlin
 * val notifier = UpdateNotifier(marketplaceApi)
 *
 * // Register callback
 * notifier.addUpdateListener { updates ->
 *     println("${updates.size} updates available!")
 * }
 *
 * // Or observe StateFlow
 * notifier.availableUpdates.collect { updates ->
 *     updateUI(updates)
 * }
 *
 * // Start background checking
 * notifier.startPeriodicChecks(installedPlugins)
 *
 * // Manual check
 * notifier.checkNow()
 *
 * // Cleanup
 * notifier.stop()
 * ```
 *
 * ## Check Interval
 * Default check interval is 6 hours. Minimum allowed interval is 15 minutes
 * to prevent excessive API usage.
 *
 * @param marketplaceApi Marketplace API for checking updates
 * @param checkIntervalMs Interval between automatic checks (default 6 hours)
 * @param scope CoroutineScope for background operations
 * @since 1.0.0
 * @see MarketplaceApi.checkUpdates
 * @see UpdateInfo
 */
class UpdateNotifier(
    private val marketplaceApi: MarketplaceApi,
    private val checkIntervalMs: Long = 6 * 60 * 60 * 1000L, // 6 hours
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    companion object {
        private const val TAG = "UpdateNotifier"
        private const val MIN_CHECK_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes minimum
        private const val DEBOUNCE_DELAY_MS = 30_000L // 30 seconds debounce
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 60_000L // 1 minute between retries
    }

    private val mutex = Mutex()
    private var periodicJob: Job? = null
    private var installedPlugins: Map<String, String> = emptyMap()
    private var lastCheckTime: Long = 0
    private var consecutiveFailures: Int = 0

    // StateFlow for available updates
    private val _availableUpdates = MutableStateFlow<List<UpdateInfo>>(emptyList())

    /**
     * StateFlow of currently available updates.
     *
     * Emits an updated list whenever update checks complete.
     * Empty list means no updates available.
     */
    val availableUpdates: StateFlow<List<UpdateInfo>> = _availableUpdates.asStateFlow()

    // StateFlow for check status
    private val _isChecking = MutableStateFlow(false)

    /**
     * StateFlow indicating whether an update check is in progress.
     */
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    // StateFlow for last check timestamp
    private val _lastCheckTimestamp = MutableStateFlow(0L)

    /**
     * StateFlow of the last successful check timestamp.
     */
    val lastCheckTimestamp: StateFlow<Long> = _lastCheckTimestamp.asStateFlow()

    // Callback listeners
    private val updateListeners = mutableListOf<UpdateListener>()
    private val errorListeners = mutableListOf<ErrorListener>()

    /**
     * Callback interface for update notifications.
     */
    fun interface UpdateListener {
        /**
         * Called when updates are found.
         *
         * @param updates List of available updates
         */
        fun onUpdatesAvailable(updates: List<UpdateInfo>)
    }

    /**
     * Callback interface for error notifications.
     */
    fun interface ErrorListener {
        /**
         * Called when update check fails.
         *
         * @param error The exception that occurred
         */
        fun onCheckFailed(error: Throwable)
    }

    /**
     * Add a listener for update notifications.
     *
     * @param listener Listener to add
     */
    fun addUpdateListener(listener: UpdateListener) {
        synchronized(updateListeners) {
            updateListeners.add(listener)
        }
    }

    /**
     * Remove an update listener.
     *
     * @param listener Listener to remove
     * @return true if listener was removed
     */
    fun removeUpdateListener(listener: UpdateListener): Boolean {
        return synchronized(updateListeners) {
            updateListeners.remove(listener)
        }
    }

    /**
     * Add a listener for error notifications.
     *
     * @param listener Listener to add
     */
    fun addErrorListener(listener: ErrorListener) {
        synchronized(errorListeners) {
            errorListeners.add(listener)
        }
    }

    /**
     * Remove an error listener.
     *
     * @param listener Listener to remove
     * @return true if listener was removed
     */
    fun removeErrorListener(listener: ErrorListener): Boolean {
        return synchronized(errorListeners) {
            errorListeners.remove(listener)
        }
    }

    /**
     * Start periodic update checks.
     *
     * Begins background checking for updates at the configured interval.
     * Only one periodic check job runs at a time - calling this again
     * will update the installed plugins list but not create duplicate jobs.
     *
     * @param installed Map of plugin ID to installed version
     */
    fun startPeriodicChecks(installed: Map<String, String>) {
        scope.launch {
            mutex.withLock {
                installedPlugins = installed

                // Cancel existing job if running
                periodicJob?.cancel()

                // Validate interval
                val interval = maxOf(checkIntervalMs, MIN_CHECK_INTERVAL_MS)

                PluginLog.i(TAG, "Starting periodic update checks (interval: ${interval / 60000} minutes)")

                periodicJob = scope.launch {
                    // Initial check
                    performCheck()

                    // Periodic checks
                    while (isActive) {
                        delay(interval)
                        performCheck()
                    }
                }
            }
        }
    }

    /**
     * Update the list of installed plugins.
     *
     * Updates the internal tracking of installed plugins without
     * restarting the periodic check job.
     *
     * @param installed Updated map of plugin ID to version
     */
    suspend fun updateInstalledPlugins(installed: Map<String, String>) {
        mutex.withLock {
            installedPlugins = installed
            PluginLog.d(TAG, "Updated installed plugins list: ${installed.size} plugins")
        }
    }

    /**
     * Trigger an immediate update check.
     *
     * Performs an update check now, subject to debouncing.
     * If a check was performed recently (within debounce window),
     * this call is ignored.
     *
     * @param force If true, bypass debouncing
     * @return true if check was initiated, false if debounced
     */
    suspend fun checkNow(force: Boolean = false): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()

        // Debounce unless forced
        if (!force && (now - lastCheckTime) < DEBOUNCE_DELAY_MS) {
            PluginLog.d(TAG, "Check debounced (last check was ${(now - lastCheckTime) / 1000}s ago)")
            return false
        }

        return performCheck()
    }

    /**
     * Stop all background checking.
     *
     * Cancels periodic checks and clears listeners.
     * Call this when the notifier is no longer needed.
     */
    fun stop() {
        scope.launch {
            mutex.withLock {
                periodicJob?.cancel()
                periodicJob = null
                PluginLog.i(TAG, "Stopped periodic update checks")
            }
        }
    }

    /**
     * Clear all available updates.
     *
     * Resets the available updates list to empty.
     * Useful after user has acknowledged or installed updates.
     */
    fun clearUpdates() {
        _availableUpdates.value = emptyList()
        PluginLog.d(TAG, "Cleared available updates")
    }

    /**
     * Mark specific updates as acknowledged.
     *
     * Removes specific updates from the available list.
     *
     * @param pluginIds Plugin IDs to remove from updates list
     */
    fun acknowledgeUpdates(pluginIds: Set<String>) {
        val current = _availableUpdates.value
        val filtered = current.filter { it.pluginId !in pluginIds }
        _availableUpdates.value = filtered
        PluginLog.d(TAG, "Acknowledged ${current.size - filtered.size} updates")
    }

    /**
     * Get update for a specific plugin.
     *
     * @param pluginId Plugin ID to check
     * @return UpdateInfo if available, null otherwise
     */
    fun getUpdateFor(pluginId: String): UpdateInfo? {
        return _availableUpdates.value.find { it.pluginId == pluginId }
    }

    /**
     * Check if any updates are available.
     *
     * @return true if at least one update is available
     */
    fun hasUpdates(): Boolean {
        return _availableUpdates.value.isNotEmpty()
    }

    /**
     * Get count of available updates.
     *
     * @return Number of plugins with available updates
     */
    fun getUpdateCount(): Int {
        return _availableUpdates.value.size
    }

    /**
     * Perform the actual update check.
     */
    private suspend fun performCheck(): Boolean {
        val plugins = mutex.withLock { installedPlugins }

        if (plugins.isEmpty()) {
            PluginLog.d(TAG, "No plugins to check for updates")
            return false
        }

        _isChecking.value = true
        PluginLog.i(TAG, "Checking for updates (${plugins.size} plugins)")

        try {
            val result = marketplaceApi.checkUpdates(plugins)

            result.fold(
                onSuccess = { updates ->
                    lastCheckTime = Clock.System.now().toEpochMilliseconds()
                    _lastCheckTimestamp.value = lastCheckTime
                    consecutiveFailures = 0

                    _availableUpdates.value = updates

                    // Notify listeners
                    if (updates.isNotEmpty()) {
                        PluginLog.i(TAG, "Found ${updates.size} updates")
                        notifyUpdateListeners(updates)
                    } else {
                        PluginLog.d(TAG, "No updates available")
                    }
                },
                onFailure = { error ->
                    handleCheckError(error)
                }
            )

            return true
        } catch (e: Exception) {
            handleCheckError(e)
            return false
        } finally {
            _isChecking.value = false
        }
    }

    /**
     * Handle update check errors.
     */
    private fun handleCheckError(error: Throwable) {
        consecutiveFailures++
        PluginLog.e(TAG, "Update check failed (attempt $consecutiveFailures): ${error.message}")

        // Notify error listeners
        notifyErrorListeners(error)

        // Schedule retry if under max attempts
        if (consecutiveFailures < MAX_RETRY_ATTEMPTS) {
            scope.launch {
                delay(RETRY_DELAY_MS * consecutiveFailures) // Exponential backoff
                PluginLog.d(TAG, "Retrying update check...")
                performCheck()
            }
        } else {
            PluginLog.w(TAG, "Max retry attempts reached, will try again at next interval")
        }
    }

    /**
     * Notify all update listeners.
     */
    private fun notifyUpdateListeners(updates: List<UpdateInfo>) {
        val listeners = synchronized(updateListeners) { updateListeners.toList() }
        listeners.forEach { listener ->
            try {
                listener.onUpdatesAvailable(updates)
            } catch (e: Exception) {
                PluginLog.e(TAG, "Error notifying update listener", e)
            }
        }
    }

    /**
     * Notify all error listeners.
     */
    private fun notifyErrorListeners(error: Throwable) {
        val listeners = synchronized(errorListeners) { errorListeners.toList() }
        listeners.forEach { listener ->
            try {
                listener.onCheckFailed(error)
            } catch (e: Exception) {
                PluginLog.e(TAG, "Error notifying error listener", e)
            }
        }
    }

    /**
     * Get status information about the notifier.
     *
     * @return Map of status information
     */
    suspend fun getStatus(): Map<String, Any> {
        return mutex.withLock {
            mapOf(
                "isRunning" to (periodicJob?.isActive == true),
                "isChecking" to _isChecking.value,
                "trackedPlugins" to installedPlugins.size,
                "availableUpdates" to _availableUpdates.value.size,
                "lastCheckTime" to lastCheckTime,
                "consecutiveFailures" to consecutiveFailures,
                "checkIntervalMs" to checkIntervalMs,
                "updateListenerCount" to updateListeners.size,
                "errorListenerCount" to errorListeners.size
            )
        }
    }
}
