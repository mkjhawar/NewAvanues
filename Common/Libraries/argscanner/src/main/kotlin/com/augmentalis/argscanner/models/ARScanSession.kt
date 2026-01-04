package com.augmentalis.argscanner.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.augmentalis.argscanner.data.Converters
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * ARScanSession - Represents a complete AR scanning session
 *
 * Captures metadata about an AR scanning session including:
 * - Session timing and duration
 * - Scanned environment information
 * - Number of detected objects
 * - Generated DSL output path
 * - Export status
 *
 * Created by: Manoj Jhawar, manoj@ideahq.net
 */
@Entity(tableName = "ar_scan_sessions")
@TypeConverters(Converters::class)
@Serializable
data class ARScanSession(
    @PrimaryKey
    val sessionId: String = UUID.randomUUID().toString(),

    // Session Metadata
    val name: String,  // User-provided session name
    val description: String? = null,  // Optional description

    // Timing
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,  // null if session is active
    val duration: Long? = null,  // Duration in milliseconds (set when session ends)

    // Environment Data
    val environment: Environment = Environment.INDOOR,
    val roomType: RoomType? = null,  // Type of room scanned
    val estimatedArea: Float? = null,  // Estimated area in square meters

    // Detection Statistics
    val totalObjectsDetected: Int = 0,
    val confidentObjects: Int = 0,  // Objects with confidence >= 70%
    val totalRelationships: Int = 0,  // Number of spatial relationships detected

    // ARCore Data
    val anchorPoints: Int = 0,  // Number of ARCore anchors placed
    val trackingQuality: TrackingSessionQuality = TrackingSessionQuality.UNKNOWN,
    val averageFps: Float? = null,  // Average frames per second during scan

    // DSL Generation
    val dslGenerated: Boolean = false,
    val dslFilePath: String? = null,  // Path to generated .vos file
    val dslFormat: DSLFormat = DSLFormat.AVAMAGIC,

    // Export Status
    val exported: Boolean = false,
    val exportedAt: Long? = null,
    val exportPath: String? = null,

    // Status
    val status: SessionStatus = SessionStatus.ACTIVE,
    val errorMessage: String? = null  // Error message if session failed
) {
    /**
     * Environment type
     */
    enum class Environment {
        INDOOR,      // Indoor environment
        OUTDOOR,     // Outdoor environment
        MIXED        // Mixed environment
    }

    /**
     * Room type classification
     */
    enum class RoomType {
        OFFICE,
        LIVING_ROOM,
        BEDROOM,
        KITCHEN,
        BATHROOM,
        HALLWAY,
        GARAGE,
        WORKSHOP,
        RETAIL,
        WAREHOUSE,
        CLASSROOM,
        OTHER
    }

    /**
     * Tracking session quality
     */
    enum class TrackingSessionQuality {
        UNKNOWN,     // Not yet determined
        POOR,        // <30% good frames
        FAIR,        // 30-60% good frames
        GOOD,        // 60-85% good frames
        EXCELLENT    // >85% good frames
    }

    /**
     * DSL output format
     */
    enum class DSLFormat {
        AVAMAGIC,    // AVAMagic UI DSL (.vos)
        JSON,        // JSON representation
        YAML,        // YAML representation
        XML          // XML representation
    }

    /**
     * Session status
     */
    enum class SessionStatus {
        ACTIVE,       // Currently scanning
        PAUSED,       // Temporarily paused
        COMPLETED,    // Successfully completed
        FAILED,       // Failed with error
        CANCELLED     // Cancelled by user
    }

    /**
     * Check if session is active
     */
    fun isActive(): Boolean {
        return status == SessionStatus.ACTIVE
    }

    /**
     * Check if session is completed successfully
     */
    fun isCompleted(): Boolean {
        return status == SessionStatus.COMPLETED
    }

    /**
     * Calculate duration from start/end times
     */
    fun calculateDuration(): Long? {
        return if (endTime != null) {
            endTime - startTime
        } else if (isActive()) {
            System.currentTimeMillis() - startTime
        } else {
            duration
        }
    }

    /**
     * Get duration in seconds
     */
    fun getDurationSeconds(): Long? {
        return calculateDuration()?.div(1000)
    }

    /**
     * Get duration in minutes
     */
    fun getDurationMinutes(): Long? {
        return calculateDuration()?.div(60000)
    }

    /**
     * Calculate detection rate (objects per minute)
     */
    fun getDetectionRate(): Float? {
        val durationMinutes = getDurationMinutes()
        return if (durationMinutes != null && durationMinutes > 0) {
            totalObjectsDetected.toFloat() / durationMinutes
        } else {
            null
        }
    }

    /**
     * Calculate confidence percentage
     */
    fun getConfidencePercentage(): Float {
        return if (totalObjectsDetected > 0) {
            (confidenObjects.toFloat() / totalObjectsDetected) * 100f
        } else {
            0f
        }
    }

    /**
     * Get summary string
     */
    fun getSummary(): String {
        return buildString {
            append("Session: $name\n")
            append("Objects: $totalObjectsDetected ($confidentObjects confident)\n")
            append("Relationships: $totalRelationships\n")
            append("Duration: ${getDurationSeconds() ?: 0}s\n")
            append("Status: $status")
        }
    }

    companion object {
        /**
         * Create new scan session
         */
        fun create(
            name: String,
            description: String? = null,
            environment: Environment = Environment.INDOOR,
            roomType: RoomType? = null
        ): ARScanSession {
            return ARScanSession(
                name = name,
                description = description,
                environment = environment,
                roomType = roomType,
                status = SessionStatus.ACTIVE
            )
        }

        /**
         * Complete session with statistics
         */
        fun complete(
            session: ARScanSession,
            totalObjects: Int,
            confidenObjects: Int,
            totalRelationships: Int,
            trackingQuality: TrackingSessionQuality,
            averageFps: Float?
        ): ARScanSession {
            val endTime = System.currentTimeMillis()
            val duration = endTime - session.startTime

            return session.copy(
                endTime = endTime,
                duration = duration,
                totalObjectsDetected = totalObjects,
                confidentObjects = confidenObjects,
                totalRelationships = totalRelationships,
                trackingQuality = trackingQuality,
                averageFps = averageFps,
                status = SessionStatus.COMPLETED
            )
        }

        /**
         * Fail session with error
         */
        fun fail(
            session: ARScanSession,
            errorMessage: String
        ): ARScanSession {
            return session.copy(
                endTime = System.currentTimeMillis(),
                duration = System.currentTimeMillis() - session.startTime,
                status = SessionStatus.FAILED,
                errorMessage = errorMessage
            )
        }
    }
}
