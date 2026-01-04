package com.augmentalis.argscanner.scanner

import android.content.Context
import com.augmentalis.argscanner.models.ScannedObject
import com.augmentalis.argscanner.models.SpatialRelationship
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import kotlin.math.sqrt

/**
 * SpatialTracker - ARCore-based spatial tracking and positioning
 *
 * Provides accurate 3D positioning for detected objects using ARCore:
 * - Camera pose tracking
 * - Plane detection (floor, walls, surfaces)
 * - Anchor placement
 * - 3D coordinate transformation
 *
 * Enhances ML Kit detections with precise AR positioning.
 *
 * Created by: Manoj Jhawar, manoj@ideahq.net
 */
class SpatialTracker(private val context: Context) {

    private var arSession: Session? = null
    private val anchors = mutableMapOf<String, Anchor>()  // UUID -> Anchor
    private val detectedPlanes = mutableListOf<Plane>()

    // Tracking statistics
    private var totalFrames = 0
    private var goodTrackingFrames = 0

    /**
     * Initialize ARCore session
     *
     * @return true if initialization successful
     */
    fun initialize(): Boolean {
        return try {
            arSession = Session(context)
            arSession?.let { session ->
                val config = session.config
                config.planeFindingMode = com.google.ar.core.Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.updateMode = com.google.ar.core.Config.UpdateMode.LATEST_CAMERA_IMAGE
                session.configure(config)
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Process AR frame and update spatial data
     *
     * @param frame ARCore frame
     * @return Updated tracking state and camera pose
     */
    fun processFrame(frame: Frame): FrameProcessingResult {
        totalFrames++

        val camera = frame.camera
        val trackingState = camera.trackingState
        val cameraPose = if (trackingState == TrackingState.TRACKING) {
            goodTrackingFrames++
            camera.pose
        } else {
            null
        }

        // Update detected planes
        frame.getUpdatedTrackables(Plane::class.java).forEach { plane ->
            if (plane.trackingState == TrackingState.TRACKING) {
                if (!detectedPlanes.contains(plane)) {
                    detectedPlanes.add(plane)
                }
            }
        }

        return FrameProcessingResult(
            trackingState = trackingState,
            cameraPose = cameraPose,
            planesDetected = detectedPlanes.size,
            trackingQuality = calculateTrackingQuality()
        )
    }

    /**
     * Calculate 3D position for detected object
     *
     * Uses ray casting from 2D screen coordinates to 3D world space
     *
     * @param screenX Screen X coordinate (0..1)
     * @param screenY Screen Y coordinate (0..1)
     * @param frame Current AR frame
     * @return 3D position or null if couldn't calculate
     */
    fun calculatePosition(
        screenX: Float,
        screenY: Float,
        frame: Frame
    ): ScannedObject.Position3D? {
        val camera = frame.camera
        if (camera.trackingState != TrackingState.TRACKING) {
            return null
        }

        // Perform hit test against detected planes
        val hits = frame.hitTest(screenX, screenY)
        val hit = hits.firstOrNull { hitResult ->
            val trackable = hitResult.trackable
            trackable is Plane && trackable.isPoseInPolygon(hitResult.hitPose)
        }

        return hit?.let { hitResult ->
            val pose = hitResult.hitPose
            ScannedObject.Position3D(
                x = pose.tx(),  // Translation X
                y = pose.ty(),  // Translation Y
                z = pose.tz()   // Translation Z
            )
        } ?: estimatePositionFromCameraPose(screenX, screenY, camera.pose)
    }

    /**
     * Estimate position from camera pose when plane hit test fails
     *
     * Projects screen coordinates into 3D space at default depth
     */
    private fun estimatePositionFromCameraPose(
        screenX: Float,
        screenY: Float,
        cameraPose: Pose
    ): ScannedObject.Position3D {
        // Default depth: 2 meters from camera
        val depth = 2.0f

        // Calculate world position from screen coordinates
        // This is a simplified estimation; ideally use camera projection matrix
        val worldX = cameraPose.tx() + (screenX - 0.5f) * depth
        val worldY = cameraPose.ty() - (screenY - 0.5f) * depth  // Invert Y
        val worldZ = cameraPose.tz() - depth

        return ScannedObject.Position3D(
            x = worldX,
            y = worldY,
            z = worldZ
        )
    }

    /**
     * Calculate rotation from AR pose
     */
    fun calculateRotation(pose: Pose): ScannedObject.Rotation3D {
        val quaternion = pose.rotationQuaternion

        // Convert quaternion to Euler angles (simplified)
        val pitch = kotlin.math.atan2(
            2.0 * (quaternion[3] * quaternion[0] + quaternion[1] * quaternion[2]),
            1.0 - 2.0 * (quaternion[0] * quaternion[0] + quaternion[1] * quaternion[1])
        ).toFloat() * 180f / Math.PI.toFloat()

        val yaw = kotlin.math.asin(
            2.0 * (quaternion[3] * quaternion[1] - quaternion[2] * quaternion[0])
        ).toFloat() * 180f / Math.PI.toFloat()

        val roll = kotlin.math.atan2(
            2.0 * (quaternion[3] * quaternion[2] + quaternion[0] * quaternion[1]),
            1.0 - 2.0 * (quaternion[1] * quaternion[1] + quaternion[2] * quaternion[2])
        ).toFloat() * 180f / Math.PI.toFloat()

        return ScannedObject.Rotation3D(
            pitch = pitch,
            yaw = yaw,
            roll = roll
        )
    }

    /**
     * Create anchor for tracked object
     *
     * Anchors maintain stable position in AR space
     */
    fun createAnchor(uuid: String, pose: Pose): Anchor? {
        return arSession?.createAnchor(pose)?.also { anchor ->
            anchors[uuid] = anchor
        }
    }

    /**
     * Get anchor for object
     */
    fun getAnchor(uuid: String): Anchor? {
        return anchors[uuid]
    }

    /**
     * Calculate spatial relationships between all objects
     */
    fun calculateRelationships(
        objects: List<ScannedObject>,
        sessionId: String
    ): List<SpatialRelationship> {
        val relationships = mutableListOf<SpatialRelationship>()

        // Calculate pairwise relationships
        for (i in objects.indices) {
            for (j in i + 1 until objects.size) {
                val relationship = SpatialRelationship.from(
                    sessionId = sessionId,
                    source = objects[i],
                    target = objects[j]
                )
                relationships.add(relationship)
            }
        }

        return relationships
    }

    /**
     * Calculate tracking quality percentage
     */
    private fun calculateTrackingQuality(): Float {
        return if (totalFrames > 0) {
            (goodTrackingFrames.toFloat() / totalFrames) * 100f
        } else {
            0f
        }
    }

    /**
     * Get average FPS
     */
    fun getAverageFps(): Float {
        // Simplified: assume 30 FPS target, scale by tracking quality
        return 30f * (calculateTrackingQuality() / 100f)
    }

    /**
     * Get tracking session quality
     */
    fun getTrackingSessionQuality(): com.augmentalis.argscanner.models.ARScanSession.TrackingSessionQuality {
        val quality = calculateTrackingQuality()
        return when {
            quality >= 85f -> com.augmentalis.argscanner.models.ARScanSession.TrackingSessionQuality.EXCELLENT
            quality >= 60f -> com.augmentalis.argscanner.models.ARScanSession.TrackingSessionQuality.GOOD
            quality >= 30f -> com.augmentalis.argscanner.models.ARScanSession.TrackingSessionQuality.FAIR
            else -> com.augmentalis.argscanner.models.ARScanSession.TrackingSessionQuality.POOR
        }
    }

    /**
     * Reset tracking statistics
     */
    fun resetStatistics() {
        totalFrames = 0
        goodTrackingFrames = 0
    }

    /**
     * Pause AR session
     */
    fun pause() {
        arSession?.pause()
    }

    /**
     * Resume AR session
     */
    fun resume() {
        try {
            arSession?.resume()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Clean up resources
     */
    fun close() {
        anchors.values.forEach { it.detach() }
        anchors.clear()
        detectedPlanes.clear()
        arSession?.close()
        arSession = null
    }

    /**
     * Frame processing result
     */
    data class FrameProcessingResult(
        val trackingState: TrackingState,
        val cameraPose: Pose?,
        val planesDetected: Int,
        val trackingQuality: Float
    )
}
