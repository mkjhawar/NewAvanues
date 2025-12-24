/**
 * VUIDCommandResultData.kt - UUID command execution result for IPC
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Code Quality Expert
 * Created: 2025-11-10
 */
package com.augmentalis.uuidcreator

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.augmentalis.uuidcreator.models.VUIDCommandResult

/**
 * VUID command execution result for IPC
 *
 * Parcelable version of UUIDCommandResult.
 * Communicates voice command execution results across process boundaries.
 */
@Parcelize
data class UUIDCommandResultData(
    /**
     * Whether the command executed successfully
     */
    val success: Boolean,

    /**
     * VUID of target element (if command targeted a specific element)
     */
    val targetUUID: String? = null,

    /**
     * Action that was executed (e.g., "click", "focus", "select")
     */
    val action: String? = null,

    /**
     * Success message or command description
     */
    val message: String? = null,

    /**
     * Error message (if command failed)
     */
    val error: String? = null,

    /**
     * Command execution time in milliseconds
     */
    val executionTime: Long = 0L
) : Parcelable {

    /**
     * Convert to UUIDCommandResult
     *
     * @return UUIDCommandResult representation
     */
    fun toUUIDCommandResult(): UUIDCommandResult {
        return UUIDCommandResult(
            success = success,
            targetUUID = targetUUID,
            action = action,
            message = message,
            error = error,
            executionTime = executionTime
        )
    }

    companion object {
        /**
         * Create from UUIDCommandResult
         *
         * @param result Source UUIDCommandResult
         * @return Parcelable UUIDCommandResultData
         */
        fun fromUUIDCommandResult(result: UUIDCommandResult): UUIDCommandResultData {
            return UUIDCommandResultData(
                success = result.success,
                targetUUID = result.targetUUID,
                action = result.action,
                message = result.message,
                error = result.error,
                executionTime = result.executionTime
            )
        }

        /**
         * Create success result
         *
         * @param targetUUID Target element UUID
         * @param action Action that was executed
         * @param message Success message
         * @param executionTime Execution time in milliseconds
         * @return Success result
         */
        fun success(
            targetUUID: String? = null,
            action: String? = null,
            message: String = "Command executed successfully",
            executionTime: Long = 0L
        ): UUIDCommandResultData {
            return UUIDCommandResultData(
                success = true,
                targetUUID = targetUUID,
                action = action,
                message = message,
                executionTime = executionTime
            )
        }

        /**
         * Create failure result
         *
         * @param error Error message
         * @param action Action that was attempted
         * @param executionTime Execution time in milliseconds
         * @return Failure result
         */
        fun failure(
            error: String,
            action: String? = null,
            executionTime: Long = 0L
        ): UUIDCommandResultData {
            return UUIDCommandResultData(
                success = false,
                action = action,
                error = error,
                executionTime = executionTime
            )
        }

        /**
         * Create "element not found" failure
         *
         * @param searchCriteria What was being searched for
         * @return Failure result with appropriate error message
         */
        fun elementNotFound(searchCriteria: String): UUIDCommandResultData {
            return failure("Element not found: $searchCriteria")
        }

        /**
         * Create "action not supported" failure
         *
         * @param action Action that was attempted
         * @param targetUUID Target element UUID
         * @return Failure result with appropriate error message
         */
        fun actionNotSupported(action: String, targetUUID: String? = null): UUIDCommandResultData {
            return UUIDCommandResultData(
                success = false,
                targetUUID = targetUUID,
                action = action,
                error = "Action '$action' is not supported on this element"
            )
        }
    }

    /**
     * Check if result indicates a failure
     *
     * @return true if command failed, false if succeeded
     */
    fun isFailed(): Boolean = !success

    /**
     * Get human-readable result summary
     *
     * @return Summary string describing the result
     */
    fun getSummary(): String {
        return when {
            success -> message ?: "Command succeeded"
            else -> error ?: "Command failed"
        }
    }
}
