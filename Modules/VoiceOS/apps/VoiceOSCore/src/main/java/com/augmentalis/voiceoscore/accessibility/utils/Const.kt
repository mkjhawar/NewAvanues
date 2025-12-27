/**
 * Const.kt - Constants for VoiceOS accessibility service
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-22
 *
 * Purpose: Centralized constants for service configuration, actions, and channels
 */
package com.augmentalis.voiceoscore.accessibility.utils

/**
 * Constants object for VoiceOS accessibility service
 * Provides centralized configuration for actions, channels, and service parameters
 */
object Const {

    // ============================================================================
    // Broadcast Actions
    // ============================================================================

    /**
     * Action for configuration updates
     * Broadcast when service configuration changes
     */
    const val ACTION_CONFIG_UPDATE = "com.augmentalis.voiceoscore.ACTION_CONFIG_UPDATE"

    /**
     * Action for service status updates
     * Broadcast when service state changes
     */
    const val ACTION_SERVICE_STATUS = "com.augmentalis.voiceoscore.ACTION_SERVICE_STATUS"

    /**
     * Action for command execution
     * Broadcast to request command execution
     */
    const val ACTION_EXECUTE_COMMAND = "com.augmentalis.voiceoscore.ACTION_EXECUTE_COMMAND"

    /**
     * Action for voice recognition state
     * Broadcast when recognition starts/stops
     */
    const val ACTION_RECOGNITION_STATE = "com.augmentalis.voiceoscore.ACTION_RECOGNITION_STATE"

    // ============================================================================
    // Notification Channels
    // ============================================================================

    /**
     * Channel ID for foreground service notifications
     */
    const val CHANNEL_ID_SERVICE = "voiceos_service_channel"

    /**
     * Channel name for foreground service
     */
    const val CHANNEL_NAME_SERVICE = "VoiceOS Service"

    /**
     * Channel description for foreground service
     */
    const val CHANNEL_DESC_SERVICE = "VoiceOS accessibility service notifications"

    /**
     * Channel ID for command status notifications
     */
    const val CHANNEL_ID_COMMANDS = "voiceos_commands_channel"

    /**
     * Channel name for commands
     */
    const val CHANNEL_NAME_COMMANDS = "VoiceOS Commands"

    /**
     * Channel description for commands
     */
    const val CHANNEL_DESC_COMMANDS = "Command execution status and feedback"

    /**
     * Channel ID for error notifications
     */
    const val CHANNEL_ID_ERRORS = "voiceos_errors_channel"

    /**
     * Channel name for errors
     */
    const val CHANNEL_NAME_ERRORS = "VoiceOS Errors"

    /**
     * Channel description for errors
     */
    const val CHANNEL_DESC_ERRORS = "Service errors and warnings"

    // ============================================================================
    // Notification IDs
    // ============================================================================

    /**
     * Notification ID for foreground service
     */
    const val NOTIFICATION_ID_SERVICE = 1001

    /**
     * Notification ID for command status
     */
    const val NOTIFICATION_ID_COMMAND = 1002

    /**
     * Notification ID for errors
     */
    const val NOTIFICATION_ID_ERROR = 1003

    // ============================================================================
    // Intent Extras
    // ============================================================================

    /**
     * Extra key for command text
     */
    const val EXTRA_COMMAND = "extra_command"

    /**
     * Extra key for service status
     */
    const val EXTRA_STATUS = "extra_status"

    /**
     * Extra key for error message
     */
    const val EXTRA_ERROR = "extra_error"

    /**
     * Extra key for configuration data
     */
    const val EXTRA_CONFIG = "extra_config"

    /**
     * Extra key for recognition state (true = listening, false = stopped)
     */
    const val EXTRA_RECOGNITION_STATE = "extra_recognition_state"

    // ============================================================================
    // Service Configuration
    // ============================================================================

    /**
     * Default timeout for command execution (milliseconds)
     */
    const val DEFAULT_COMMAND_TIMEOUT_MS = 5000L

    /**
     * Default timeout for service initialization (milliseconds)
     */
    const val DEFAULT_INIT_TIMEOUT_MS = 10000L

    /**
     * Default debounce delay for voice events (milliseconds)
     */
    const val DEFAULT_DEBOUNCE_DELAY_MS = 300L

    /**
     * Maximum retry attempts for failed operations
     */
    const val DEFAULT_MAX_RETRIES = 3

    /**
     * Default delay between retry attempts (milliseconds)
     */
    const val DEFAULT_RETRY_DELAY_MS = 1000L

    // ============================================================================
    // Resource Monitoring Thresholds
    // ============================================================================

    /**
     * Warning threshold for memory usage (percentage)
     */
    const val MEMORY_WARNING_THRESHOLD = 80

    /**
     * Critical threshold for memory usage (percentage)
     */
    const val MEMORY_CRITICAL_THRESHOLD = 90

    /**
     * Warning threshold for CPU usage (percentage)
     */
    const val CPU_WARNING_THRESHOLD = 70

    /**
     * Critical threshold for CPU usage (percentage)
     */
    const val CPU_CRITICAL_THRESHOLD = 85

    /**
     * Interval for resource monitoring checks (milliseconds)
     */
    const val RESOURCE_MONITOR_INTERVAL_MS = 30000L // 30 seconds

    // ============================================================================
    // Event Priority Levels
    // ============================================================================

    /**
     * Critical priority - process immediately
     */
    const val PRIORITY_CRITICAL = 0

    /**
     * High priority - process before normal events
     */
    const val PRIORITY_HIGH = 1

    /**
     * Normal priority - default processing
     */
    const val PRIORITY_NORMAL = 2

    /**
     * Low priority - process when idle
     */
    const val PRIORITY_LOW = 3

    // ============================================================================
    // Accessibility Event Types
    // ============================================================================

    /**
     * Minimum delay between accessibility events (milliseconds)
     * Prevents event flooding
     */
    const val MIN_EVENT_INTERVAL_MS = 100L

    /**
     * Maximum queue size for pending events
     */
    const val MAX_EVENT_QUEUE_SIZE = 100

    // ============================================================================
    // Service States
    // ============================================================================

    /**
     * Service state: Initializing
     */
    const val STATE_INITIALIZING = "initializing"

    /**
     * Service state: Ready
     */
    const val STATE_READY = "ready"

    /**
     * Service state: Listening
     */
    const val STATE_LISTENING = "listening"

    /**
     * Service state: Processing command
     */
    const val STATE_PROCESSING = "processing"

    /**
     * Service state: Error
     */
    const val STATE_ERROR = "error"

    /**
     * Service state: Stopped
     */
    const val STATE_STOPPED = "stopped"
}
