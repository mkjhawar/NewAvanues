package com.augmentalis.argscanner

import android.content.Context
import android.graphics.Bitmap
import com.augmentalis.argscanner.generator.DSLGenerator
import com.augmentalis.argscanner.generator.VosExporter
import com.augmentalis.argscanner.integration.UUIDIntegration
import com.augmentalis.argscanner.models.ARScanSession
import com.augmentalis.argscanner.models.DSLGenerationConfig
import com.augmentalis.argscanner.models.ScannedObject
import com.augmentalis.argscanner.models.SpatialRelationship
import com.augmentalis.argscanner.scanner.ObjectDetector
import com.augmentalis.argscanner.scanner.SpatialTracker
import com.google.ar.core.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * ARGScanner - Main facade class for AR-based UI DSL generation
 *
 * Provides high-level API for:
 * - Starting/stopping AR scanning sessions
 * - Detecting and tracking objects
 * - Generating AVAMagic UI DSL
 * - Exporting to .vos files
 * - Voice control integration
 *
 * Usage:
 * ```kotlin
 * val scanner = ARGScanner(context)
 * scanner.initialize()
 *
 * // Start session
 * val session = scanner.startSession("My Room Scan")
 *
 * // Process AR frames
 * scanner.processFrame(arFrame, cameraFrame)
 *
 * // Complete and export
 * val result = scanner.completeSession()
 * ```
 *
 * Created by: Manoj Jhawar, manoj@ideahq.net
 */
class ARGScanner(private val context: Context) {

    // Components
    private val objectDetector = ObjectDetector(context)
    private val spatialTracker = SpatialTracker(context)
    private val dslGenerator = DSLGenerator()
    private val vosExporter = VosExporter(context)
    private val uuidIntegration = UUIDIntegration(context)

    // Coroutine scope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Current session state
    private var currentSession: ARScanSession? = null
    private val scannedObjects = mutableListOf<ScannedObject>()
    private val spatialRelationships = mutableListOf<SpatialRelationship>()

    // Configuration
    private var config: DSLGenerationConfig = DSLGenerationConfig.forIndoorRoom()

    // Callbacks
    private var onObjectDetected: ((ScannedObject) -> Unit)? = null
    private var onSessionProgress: ((ScanProgress) -> Unit)? = null

    /**
     * Initialize ARGScanner
     *
     * Must be called before starting sessions
     */
    fun initialize(): Boolean {
        return spatialTracker.initialize()
    }

    /**
     * Start new scanning session
     *
     * @param name Session name
     * @param description Optional description
     * @param environment Environment type
     * @param roomType Room type
     * @param config DSL generation configuration
     * @return Created session
     */
    fun startSession(
        name: String,
        description: String? = null,
        environment: ARScanSession.Environment = ARScanSession.Environment.INDOOR,
        roomType: ARScanSession.RoomType? = null,
        config: DSLGenerationConfig = DSLGenerationConfig.forIndoorRoom()
    ): ARScanSession {
        // End previous session if active
        currentSession?.let { endSession() }

        // Create new session
        val session = ARScanSession.create(
            name = name,
            description = description,
            environment = environment,
            roomType = roomType
        )

        currentSession = session
        scannedObjects.clear()
        spatialRelationships.clear()
        this.config = config

        // Reset tracking statistics
        spatialTracker.resetStatistics()

        return session
    }

    /**
     * Process AR frame
     *
     * Detects objects and updates spatial tracking
     *
     * @param arFrame ARCore frame
     * @param cameraFrame Camera bitmap
     */
    suspend fun processFrame(arFrame: Frame, cameraFrame: Bitmap) {
        val session = currentSession ?: return

        // Process AR frame for spatial tracking
        val frameResult = spatialTracker.processFrame(arFrame)

        // Detect objects in camera frame
        val detectedResults = objectDetector.detectObjects(
            bitmap = cameraFrame,
            sessionId = session.sessionId
        )

        // Update objects with AR positioning
        detectedResults.forEach { result ->
            // Calculate accurate 3D position
            val position = spatialTracker.calculatePosition(
                screenX = result.mlKitObject.boundingBox.centerX() / cameraFrame.width.toFloat(),
                screenY = result.mlKitObject.boundingBox.centerY() / cameraFrame.height.toFloat(),
                frame = arFrame
            )

            // Update scanned object with AR data
            val updatedObject = position?.let { pos ->
                result.scannedObject.copy(
                    position = pos,
                    rotation = spatialTracker.calculateRotation(arFrame.camera.pose)
                )
            } ?: result.scannedObject

            // Filter by confidence threshold
            if (config.meetsConfidenceThreshold(updatedObject.confidence) &&
                !config.shouldExcludeLabel(updatedObject.label)
            ) {
                // Add or update object
                val existingIndex = scannedObjects.indexOfFirst { it.uuid == updatedObject.uuid }
                if (existingIndex >= 0) {
                    scannedObjects[existingIndex] = updatedObject
                } else {
                    scannedObjects.add(updatedObject)
                    onObjectDetected?.invoke(updatedObject)
                }
            }
        }

        // Update spatial relationships
        if (scannedObjects.size >= 2 && config.enableGrouping) {
            val newRelationships = spatialTracker.calculateRelationships(
                objects = scannedObjects,
                sessionId = session.sessionId
            )
            spatialRelationships.clear()
            spatialRelationships.addAll(newRelationships)
        }

        // Report progress
        onSessionProgress?.invoke(
            ScanProgress(
                objectsDetected = scannedObjects.size,
                confidentObjects = scannedObjects.count { it.isConfident(config.minConfidence) },
                relationshipsDetected = spatialRelationships.size,
                trackingQuality = frameResult.trackingQuality
            )
        )
    }

    /**
     * Complete current session
     *
     * Generates DSL and exports to file
     *
     * @param autoExport Whether to automatically export to file
     * @return Completion result
     */
    suspend fun completeSession(autoExport: Boolean = true): SessionCompletionResult {
        val session = currentSession ?: return SessionCompletionResult.Error("No active session")

        // Register objects with UUID Creator
        val registeredObjects = if (config.useUUIDCreator) {
            uuidIntegration.registerObjects(scannedObjects)
        } else {
            scannedObjects
        }

        // Complete session with statistics
        val completedSession = ARScanSession.complete(
            session = session,
            totalObjects = registeredObjects.size,
            confidentObjects = registeredObjects.count { it.isConfident(config.minConfidence) },
            totalRelationships = spatialRelationships.size,
            trackingQuality = spatialTracker.getTrackingSessionQuality(),
            averageFps = spatialTracker.getAverageFps()
        )

        // Generate DSL
        val dslContent = dslGenerator.generate(
            session = completedSession,
            objects = registeredObjects,
            relationships = spatialRelationships,
            config = config
        )

        // Export if requested
        val exportPath = if (autoExport) {
            when (val exportResult = vosExporter.exportWithMetadata(
                session = completedSession,
                dslContent = dslContent
            )) {
                is VosExporter.ExportResult.Success -> exportResult.filePath
                is VosExporter.ExportResult.Error -> null
            }
        } else {
            null
        }

        // Update session with export info
        val finalSession = completedSession.copy(
            dslGenerated = true,
            exported = exportPath != null,
            exportedAt = if (exportPath != null) System.currentTimeMillis() else null,
            exportPath = exportPath
        )

        currentSession = finalSession

        return SessionCompletionResult.Success(
            session = finalSession,
            dslContent = dslContent,
            objectsCount = registeredObjects.size,
            exportPath = exportPath
        )
    }

    /**
     * End session without completion
     */
    fun endSession() {
        currentSession = null
        scannedObjects.clear()
        spatialRelationships.clear()
    }

    /**
     * Set DSL generation configuration
     */
    fun setConfig(config: DSLGenerationConfig) {
        this.config = config
    }

    /**
     * Set object detected callback
     */
    fun setOnObjectDetected(callback: (ScannedObject) -> Unit) {
        this.onObjectDetected = callback
    }

    /**
     * Set session progress callback
     */
    fun setOnSessionProgress(callback: (ScanProgress) -> Unit) {
        this.onSessionProgress = callback
    }

    /**
     * Get current session
     */
    fun getCurrentSession(): ARScanSession? = currentSession

    /**
     * Get scanned objects
     */
    fun getScannedObjects(): List<ScannedObject> = scannedObjects.toList()

    /**
     * Get spatial relationships
     */
    fun getSpatialRelationships(): List<SpatialRelationship> = spatialRelationships.toList()

    /**
     * Execute voice command
     */
    fun executeVoiceCommand(command: String): Boolean {
        return uuidIntegration.executeVoiceCommand(command)
    }

    /**
     * Pause AR tracking
     */
    fun pause() {
        spatialTracker.pause()
    }

    /**
     * Resume AR tracking
     */
    fun resume() {
        spatialTracker.resume()
    }

    /**
     * Clean up resources
     */
    fun close() {
        objectDetector.close()
        spatialTracker.close()
        endSession()
    }

    /**
     * Scan progress data
     */
    data class ScanProgress(
        val objectsDetected: Int,
        val confidentObjects: Int,
        val relationshipsDetected: Int,
        val trackingQuality: Float
    )

    /**
     * Session completion result
     */
    sealed class SessionCompletionResult {
        data class Success(
            val session: ARScanSession,
            val dslContent: String,
            val objectsCount: Int,
            val exportPath: String?
        ) : SessionCompletionResult()

        data class Error(val message: String) : SessionCompletionResult()
    }
}
