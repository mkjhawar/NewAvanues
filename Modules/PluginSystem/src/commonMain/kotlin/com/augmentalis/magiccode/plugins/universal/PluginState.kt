package com.augmentalis.magiccode.plugins.universal

/**
 * Extended plugin lifecycle state for Universal Plugin Architecture.
 *
 * Represents the complete lifecycle of a plugin from initialization to shutdown,
 * including intermediate states for pause/resume functionality critical for
 * accessibility-first applications where resource management is essential.
 *
 * ## State Transitions
 * ```
 * UNINITIALIZED -> INITIALIZING -> ACTIVE
 *                                    |
 *                      +-------------+-------------+
 *                      |             |             |
 *                      v             v             v
 *                   PAUSED       STOPPING       ERROR
 *                      |             |             |
 *                      v             v             v
 *                  RESUMING      STOPPED        FAILED
 *                      |
 *                      v
 *                   ACTIVE
 * ```
 *
 * ## Thread Safety
 * State transitions should be performed atomically using the plugin's state management.
 * Observe state changes via [UniversalPlugin.stateFlow].
 *
 * @since 1.0.0
 * @see UniversalPlugin
 * @see UniversalPlugin.stateFlow
 */
enum class PluginState {
    /**
     * Plugin has been loaded but not yet initialized.
     *
     * This is the initial state when a plugin is first registered with the system.
     * From this state, the plugin can only transition to [INITIALIZING].
     */
    UNINITIALIZED,

    /**
     * Plugin initialization is in progress.
     *
     * Resources are being allocated, configurations are being applied,
     * and connections are being established. This is a transient state
     * that will transition to either [ACTIVE] or [FAILED].
     */
    INITIALIZING,

    /**
     * Plugin is fully active and available for use.
     *
     * All capabilities are operational and the plugin is ready to
     * handle requests. This is the normal operational state.
     */
    ACTIVE,

    /**
     * Plugin is temporarily suspended to conserve resources.
     *
     * Used when the application is backgrounded or when resources
     * need to be freed temporarily. The plugin maintains its state
     * but stops processing requests. Transition to [RESUMING] to
     * return to active state.
     */
    PAUSED,

    /**
     * Plugin is resuming from a paused state.
     *
     * Resources are being re-acquired and connections are being
     * re-established. This is a transient state that will transition
     * to [ACTIVE] or [ERROR].
     */
    RESUMING,

    /**
     * Plugin encountered a recoverable error.
     *
     * The plugin is not operational but may be able to recover.
     * Use health check diagnostics to determine the cause and
     * potentially trigger reinitialization.
     */
    ERROR,

    /**
     * Plugin shutdown is in progress.
     *
     * Resources are being released and connections are being closed.
     * This is a transient state that will transition to [STOPPED].
     */
    STOPPING,

    /**
     * Plugin has been gracefully stopped.
     *
     * All resources have been released. The plugin can be
     * reinitialized by transitioning back to [INITIALIZING].
     */
    STOPPED,

    /**
     * Plugin encountered an unrecoverable failure.
     *
     * The plugin cannot operate and requires manual intervention
     * or complete reinstallation. This is a terminal state.
     */
    FAILED;

    /**
     * Check if the plugin is in an operational state.
     *
     * @return true if the plugin can process requests
     */
    fun isOperational(): Boolean = this == ACTIVE

    /**
     * Check if the plugin is in a transitional state.
     *
     * Transitional states are temporary and will move to a stable state.
     *
     * @return true if the plugin is transitioning between states
     */
    fun isTransitional(): Boolean = this in setOf(INITIALIZING, RESUMING, STOPPING)

    /**
     * Check if the plugin is in an error state.
     *
     * @return true if the plugin has encountered an error or failure
     */
    fun isError(): Boolean = this in setOf(ERROR, FAILED)

    /**
     * Check if the plugin can be paused.
     *
     * @return true if the plugin can transition to PAUSED state
     */
    fun canPause(): Boolean = this == ACTIVE

    /**
     * Check if the plugin can be resumed.
     *
     * @return true if the plugin can transition to RESUMING state
     */
    fun canResume(): Boolean = this == PAUSED

    /**
     * Check if the plugin can be shut down.
     *
     * @return true if the plugin can transition to STOPPING state
     */
    fun canShutdown(): Boolean = this in setOf(ACTIVE, PAUSED, ERROR)
}
