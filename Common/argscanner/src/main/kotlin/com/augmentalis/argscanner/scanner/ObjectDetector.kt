package com.augmentalis.argscanner.scanner

import android.content.Context
import android.graphics.Bitmap
import com.augmentalis.argscanner.models.ScannedObject
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.sqrt

/**
 * ObjectDetector - ML Kit-based object detection for AR scanning
 *
 * Detects and classifies objects in camera frames using Google ML Kit:
 * - Object detection (bounding boxes)
 * - Image labeling (classification)
 * - Confidence scoring
 *
 * Converts ML Kit results to ScannedObject models.
 *
 * Created by: Manoj Jhawar, manoj@ideahq.net
 */
class ObjectDetector(private val context: Context) {

    // ML Kit Object Detector
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)  // Real-time detection
            .enableMultipleObjects()  // Detect multiple objects
            .enableClassification()   // Enable classification
            .build()
    )

    // ML Kit Image Labeler (for better classification)
    private val imageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.6f)  // 60% minimum confidence
            .build()
    )

    /**
     * Detect objects in a bitmap image
     *
     * @param bitmap Camera frame as bitmap
     * @param sessionId Current scan session ID
     * @param arPosition Optional AR position for detected objects
     * @return List of detected objects with ML classifications
     */
    suspend fun detectObjects(
        bitmap: Bitmap,
        sessionId: String,
        arPosition: ScannedObject.Position3D? = null
    ): List<DetectedObjectResult> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val results = mutableListOf<DetectedObjectResult>()

        try {
            // Step 1: Detect objects (bounding boxes)
            val detectedObjects = objectDetector.process(image).await()

            // Step 2: Get image labels for better classification
            val labels = imageLabeler.process(image).await()

            // Step 3: Match objects with labels
            detectedObjects.forEachIndexed { index, detectedObject ->
                // Find best matching label based on bounding box overlap
                val bestLabel = findBestLabel(detectedObject, labels)

                // Convert to ScannedObject
                val scannedObject = convertToScannedObject(
                    detectedObject = detectedObject,
                    label = bestLabel?.text ?: "unknown",
                    confidence = bestLabel?.confidence ?: 0.5f,
                    sessionId = sessionId,
                    arPosition = arPosition,
                    index = index
                )

                results.add(
                    DetectedObjectResult(
                        scannedObject = scannedObject,
                        mlKitObject = detectedObject,
                        trackingId = detectedObject.trackingId
                    )
                )
            }

        } catch (e: Exception) {
            // Log error but don't crash
            e.printStackTrace()
        }

        return results
    }

    /**
     * Find best matching label for detected object
     *
     * Uses spatial overlap between object bounding box and label confidence
     */
    private fun findBestLabel(
        detectedObject: DetectedObject,
        labels: List<com.google.mlkit.vision.label.ImageLabel>
    ): com.google.mlkit.vision.label.ImageLabel? {
        // If ML Kit already provided labels, use the best one
        if (detectedObject.labels.isNotEmpty()) {
            val mlKitLabel = detectedObject.labels.maxByOrNull { it.confidence }
            return labels.find { it.text == mlKitLabel?.text } ?: labels.maxByOrNull { it.confidence }
        }

        // Otherwise, use the highest confidence label
        return labels.maxByOrNull { it.confidence }
    }

    /**
     * Convert ML Kit DetectedObject to ScannedObject model
     */
    private fun convertToScannedObject(
        detectedObject: DetectedObject,
        label: String,
        confidence: Float,
        sessionId: String,
        arPosition: ScannedObject.Position3D?,
        index: Int
    ): ScannedObject {
        // Calculate bounding box dimensions (from 2D detection)
        val boundingBox = detectedObject.boundingBox
        val width = (boundingBox.width() / 1000f)  // Convert pixels to approximate meters
        val height = (boundingBox.height() / 1000f)
        val depth = estimateDepth(width, height)  // Estimate depth from width/height

        // Use AR position if available, otherwise estimate from 2D detection
        val position = arPosition ?: ScannedObject.Position3D(
            x = (boundingBox.centerX() - 500f) / 500f,  // Normalize to -1..1
            y = -(boundingBox.centerY() - 500f) / 500f,  // Invert Y (camera vs world coords)
            z = 2.0f  // Default depth 2 meters
        )

        // Estimate rotation (default facing camera)
        val rotation = ScannedObject.Rotation3D(
            pitch = 0f,
            yaw = 0f,
            roll = 0f
        )

        // Determine tracking quality from confidence
        val trackingQuality = when {
            confidence >= 0.9f -> ScannedObject.TrackingQuality.EXCELLENT
            confidence >= 0.75f -> ScannedObject.TrackingQuality.GOOD
            confidence >= 0.6f -> ScannedObject.TrackingQuality.NORMAL
            else -> ScannedObject.TrackingQuality.POOR
        }

        // Generate UUID (will be replaced by UUIDCreator in integration step)
        val uuid = java.util.UUID.randomUUID().toString()

        return ScannedObject(
            uuid = uuid,
            sessionId = sessionId,
            label = label,
            confidence = confidence,
            position = position,
            rotation = rotation,
            boundingBox = ScannedObject.BoundingBox3D(
                width = width,
                height = height,
                depth = depth
            ),
            trackingQuality = trackingQuality,
            voiceCommands = generateDefaultVoiceCommands(label)
        )
    }

    /**
     * Estimate depth from 2D bounding box dimensions
     *
     * Larger objects appear larger on screen when closer
     */
    private fun estimateDepth(width: Float, height: Float): Float {
        val area = width * height
        return when {
            area > 0.5f -> 0.3f  // Large object, likely close
            area > 0.2f -> 0.5f  // Medium object
            area > 0.1f -> 0.8f  // Small object
            else -> 1.0f         // Tiny object, likely far
        }
    }

    /**
     * Generate default voice commands for an object
     */
    private fun generateDefaultVoiceCommands(label: String): List<String> {
        val cleanLabel = label.replace("_", " ").lowercase()
        return listOf(
            "select $cleanLabel",
            "show $cleanLabel",
            "highlight $cleanLabel",
            "navigate to $cleanLabel"
        )
    }

    /**
     * Clean up resources
     */
    fun close() {
        objectDetector.close()
        imageLabeler.close()
    }

    /**
     * Result container for detected objects
     */
    data class DetectedObjectResult(
        val scannedObject: ScannedObject,
        val mlKitObject: DetectedObject,
        val trackingId: Int?  // ML Kit tracking ID for frame-to-frame tracking
    )
}
