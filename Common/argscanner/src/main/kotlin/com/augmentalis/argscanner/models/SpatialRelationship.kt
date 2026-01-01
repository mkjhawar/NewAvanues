package com.augmentalis.argscanner.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlin.math.sqrt

/**
 * SpatialRelationship - Represents spatial relationship between two scanned objects
 *
 * Captures how objects relate to each other in 3D space:
 * - Distance between objects
 * - Relative positioning (above, below, left, right, front, back)
 * - Proximity relationships
 * - Grouped objects (e.g., desk with chair)
 *
 * Used for generating intelligent UI layouts based on physical spatial relationships.
 *
 * Created by: Manoj Jhawar, manoj@ideahq.net
 */
@Entity(tableName = "spatial_relationships")
@Serializable
data class SpatialRelationship(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val sessionId: String,  // Foreign key to ARScanSession

    // Related Objects
    val sourceObjectUuid: String,  // First object
    val targetObjectUuid: String,  // Second object

    // Distance Data
    val distance: Float,  // Distance in meters between object centers
    val horizontalDistance: Float,  // Distance in XZ plane (ignoring height)
    val verticalDistance: Float,  // Distance in Y axis (height difference)

    // Relative Position
    val relativePosition: RelativePosition,  // Direction from source to target

    // Proximity
    val proximityLevel: ProximityLevel,  // How close objects are

    // Grouping
    val isGrouped: Boolean = false,  // Whether objects should be grouped in UI
    val groupType: GroupType? = null,  // Type of grouping (if applicable)

    // Metadata
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Relative position in 3D space
     */
    enum class RelativePosition {
        ABOVE,       // Target is above source
        BELOW,       // Target is below source
        LEFT,        // Target is to the left
        RIGHT,       // Target is to the right
        FRONT,       // Target is in front
        BACK,        // Target is behind
        ABOVE_LEFT,  // Combinations
        ABOVE_RIGHT,
        BELOW_LEFT,
        BELOW_RIGHT,
        FRONT_LEFT,
        FRONT_RIGHT,
        BACK_LEFT,
        BACK_RIGHT
    }

    /**
     * Proximity classification
     */
    enum class ProximityLevel {
        TOUCHING,     // < 0.1 meters (10 cm)
        VERY_CLOSE,   // 0.1 - 0.5 meters
        CLOSE,        // 0.5 - 1.0 meters
        NEARBY,       // 1.0 - 2.0 meters
        FAR,          // 2.0 - 5.0 meters
        VERY_FAR      // > 5.0 meters
    }

    /**
     * Group type for related objects
     */
    enum class GroupType {
        WORKSPACE,    // Desk + chair + monitor
        FURNITURE,    // Sofa + table + lamp
        SHELF,        // Multiple items on same surface
        STACK,        // Vertically stacked items
        ROW,          // Horizontally aligned items
        CLUSTER       // Random grouping
    }

    companion object {
        /**
         * Create spatial relationship from two scanned objects
         */
        fun from(
            sessionId: String,
            source: ScannedObject,
            target: ScannedObject
        ): SpatialRelationship {
            // Calculate distances
            val dx = target.position.x - source.position.x
            val dy = target.position.y - source.position.y
            val dz = target.position.z - source.position.z

            val distance = sqrt(dx * dx + dy * dy + dz * dz)
            val horizontalDistance = sqrt(dx * dx + dz * dz)
            val verticalDistance = kotlin.math.abs(dy)

            // Determine relative position
            val relativePosition = calculateRelativePosition(dx, dy, dz)

            // Determine proximity
            val proximityLevel = calculateProximityLevel(distance)

            // Check if should be grouped
            val (isGrouped, groupType) = determineGrouping(
                source, target, distance, relativePosition
            )

            return SpatialRelationship(
                sessionId = sessionId,
                sourceObjectUuid = source.uuid,
                targetObjectUuid = target.uuid,
                distance = distance,
                horizontalDistance = horizontalDistance,
                verticalDistance = verticalDistance,
                relativePosition = relativePosition,
                proximityLevel = proximityLevel,
                isGrouped = isGrouped,
                groupType = groupType
            )
        }

        /**
         * Calculate relative position based on delta values
         */
        private fun calculateRelativePosition(
            dx: Float,
            dy: Float,
            dz: Float
        ): RelativePosition {
            val absX = kotlin.math.abs(dx)
            val absY = kotlin.math.abs(dy)
            val absZ = kotlin.math.abs(dz)

            // Determine primary axis
            return when {
                // Vertical relationships dominate
                absY > absX && absY > absZ -> {
                    if (dy > 0) RelativePosition.ABOVE else RelativePosition.BELOW
                }
                // Horizontal relationships
                absX > absZ -> {
                    when {
                        absY > 0.3f -> {
                            // Combined vertical + horizontal
                            if (dy > 0) {
                                if (dx > 0) RelativePosition.ABOVE_RIGHT else RelativePosition.ABOVE_LEFT
                            } else {
                                if (dx > 0) RelativePosition.BELOW_RIGHT else RelativePosition.BELOW_LEFT
                            }
                        }
                        else -> if (dx > 0) RelativePosition.RIGHT else RelativePosition.LEFT
                    }
                }
                // Depth relationships
                else -> {
                    when {
                        absY > 0.3f -> {
                            // Combined vertical + depth
                            if (dy > 0) {
                                if (dz > 0) RelativePosition.ABOVE else RelativePosition.ABOVE
                            } else {
                                if (dz > 0) RelativePosition.BELOW else RelativePosition.BELOW
                            }
                        }
                        else -> if (dz > 0) RelativePosition.FRONT else RelativePosition.BACK
                    }
                }
            }
        }

        /**
         * Calculate proximity level based on distance
         */
        private fun calculateProximityLevel(distance: Float): ProximityLevel {
            return when {
                distance < 0.1f -> ProximityLevel.TOUCHING
                distance < 0.5f -> ProximityLevel.VERY_CLOSE
                distance < 1.0f -> ProximityLevel.CLOSE
                distance < 2.0f -> ProximityLevel.NEARBY
                distance < 5.0f -> ProximityLevel.FAR
                else -> ProximityLevel.VERY_FAR
            }
        }

        /**
         * Determine if objects should be grouped and group type
         */
        private fun determineGrouping(
            source: ScannedObject,
            target: ScannedObject,
            distance: Float,
            relativePosition: RelativePosition
        ): Pair<Boolean, GroupType?> {
            // Group if objects are close
            if (distance > 2.0f) return Pair(false, null)

            // Detect workspace (desk + chair + monitor)
            val isWorkspace = setOf(
                "desk", "chair", "monitor", "keyboard", "mouse"
            ).any { it in source.label.lowercase() || it in target.label.lowercase() }

            if (isWorkspace) {
                return Pair(true, GroupType.WORKSPACE)
            }

            // Detect furniture grouping
            val isFurniture = setOf(
                "sofa", "couch", "table", "lamp", "cushion"
            ).any { it in source.label.lowercase() || it in target.label.lowercase() }

            if (isFurniture && distance < 1.5f) {
                return Pair(true, GroupType.FURNITURE)
            }

            // Detect stacking (vertically aligned)
            if (relativePosition == RelativePosition.ABOVE || relativePosition == RelativePosition.BELOW) {
                return Pair(true, GroupType.STACK)
            }

            // Detect rows (horizontally aligned)
            if (relativePosition == RelativePosition.LEFT || relativePosition == RelativePosition.RIGHT) {
                return Pair(true, GroupType.ROW)
            }

            // Default: cluster if close
            if (distance < 1.0f) {
                return Pair(true, GroupType.CLUSTER)
            }

            return Pair(false, null)
        }
    }

    /**
     * Check if objects are touching or very close
     */
    fun areTouching(): Boolean {
        return proximityLevel == ProximityLevel.TOUCHING ||
               proximityLevel == ProximityLevel.VERY_CLOSE
    }

    /**
     * Check if objects are vertically aligned
     */
    fun areVerticallyAligned(): Boolean {
        return relativePosition == RelativePosition.ABOVE ||
               relativePosition == RelativePosition.BELOW
    }

    /**
     * Check if objects are horizontally aligned
     */
    fun areHorizontallyAligned(): Boolean {
        return relativePosition == RelativePosition.LEFT ||
               relativePosition == RelativePosition.RIGHT
    }
}
