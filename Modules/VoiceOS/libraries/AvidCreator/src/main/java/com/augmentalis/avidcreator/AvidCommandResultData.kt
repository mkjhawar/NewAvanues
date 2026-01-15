/**
 * AvidCommandResultData.kt - AVID command execution result for IPC
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Code Quality Expert
 * Created: 2025-11-10
 * Updated: 2026-01-15 - Migrated to AVID naming
 */
package com.augmentalis.avidcreator

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.augmentalis.avidcreator.models.AvidCommandResult

/**
 * AVID command execution result for IPC
 *
 * Parcelable version of AvidCommandResult.
 * Communicates voice command execution results across process boundaries.
 */
@Parcelize
data class AvidCommandResultData(
    /**
     * Whether the command executed successfully
     */
    val success: Boolean,

    /**
     * AVID of target element (if command targeted a specific element)
     */
    val targetAvid: String? = null,

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
     * Convert to AvidCommandResult
     *
     * @return AvidCommandResult representation
     */
    fun toAvidCommandResult(): AvidCommandResult {
        return AvidCommandResult(
            success = success,
            targetAvid = targetAvid,
            action = action,
            message = message,
            error = error,
            executionTime = executionTime
        )
    }

    companion object {
        /**
         * Create from AvidCommandResult
         *
         * @param result Source AvidCommandResult
         * @return Parcelable AvidCommandResultData
         */
        fun fromAvidCommandResult(result: AvidCommandResult): AvidCommandResultData {
            return AvidCommandResultData(
                success = result.success,
                targetAvid = result.targetAvid,
                action = result.action,
                message = result.message,
                error = result.error,
                executionTime = result.executionTime
            )
        }

        /**
         * Create success result
         *
         * @param targetAvid Target element AVID
         * @param action Action that was executed
         * @param message Success message
         * @param executionTime Execution time in milliseconds
         * @return Success result
         */
        fun success(
            targetAvid: String? = null,
            action: String? = null,
            message: String = "Command executed successfully",
            executionTime: Long = 0L
        ): AvidCommandResultData {
            return AvidCommandResultData(
                success = true,
                targetAvid = targetAvid,
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
        ): AvidCommandResultData {
            return AvidCommandResultData(
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
        fun elementNotFound(searchCriteria: String): AvidCommandResultData {
            return failure("Element not found: $searchCriteria")
        }

        /**
         * Create "action not supported" failure
         *
         * @param action Action that was attempted
         * @param targetAvid Target element AVID
         * @return Failure result with appropriate error message
         */
        fun actionNotSupported(action: String, targetAvid: String? = null): AvidCommandResultData {
            return AvidCommandResultData(
                success = false,
                targetAvid = targetAvid,
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

