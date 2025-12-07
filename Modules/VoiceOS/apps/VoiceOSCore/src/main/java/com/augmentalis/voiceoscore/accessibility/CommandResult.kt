/**
 * CommandResult.kt - Voice command execution result
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Code Quality Expert
 * Created: 2025-11-10
 */
package com.augmentalis.voiceoscore.accessibility

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Command execution result
 *
 * Parcelable data class for IPC communication.
 * Used to communicate voice command execution results across process boundaries.
 */
@Parcelize
data class CommandResult(
    /**
     * The command that was executed
     */
    val command: String,

    /**
     * Whether the command executed successfully
     */
    val success: Boolean,

    /**
     * Optional result or error message
     */
    val message: String? = null,

    /**
     * Command execution time in milliseconds
     */
    val executionTime: Long = 0L,

    /**
     * Error code (0 = no error)
     */
    val errorCode: Int = 0
) : Parcelable {

    companion object {
        /**
         * Create success result
         */
        fun success(command: String, message: String? = null, executionTime: Long = 0L): CommandResult {
            return CommandResult(
                command = command,
                success = true,
                message = message,
                executionTime = executionTime,
                errorCode = 0
            )
        }

        /**
         * Create failure result
         */
        fun failure(command: String, message: String, errorCode: Int = -1, executionTime: Long = 0L): CommandResult {
            return CommandResult(
                command = command,
                success = false,
                message = message,
                executionTime = executionTime,
                errorCode = errorCode
            )
        }
    }
}
