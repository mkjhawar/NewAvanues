/**
 * HUDRenderer.kt
 * Path: /CodeImport/HUDManager/src/main/java/com/augmentalis/hudmanager/rendering/HUDRenderer.kt
 * 
 * Created: 2025-01-23
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: High-performance AR rendering system with ARVision styling
 * Provides 90-120 FPS rendering for smooth HUD experience
 */

package com.augmentalis.voiceoscore.managers.hudmanager.rendering

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import com.augmentalis.voiceoscore.managers.hudmanager.ui.*
import com.augmentalis.voiceoscore.managers.hudmanager.spatial.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

/**
 * Real-time HUD rendering engine
 * Optimized for smart glasses and AR displays
 */
class HUDRenderer(
    private val context: Context
) : SurfaceView(context), SurfaceHolder.Callback {
    
    // Rendering state
    private var isRendering = false
    private var targetFPS = 90
    private var renderMode = RenderMode.SPATIAL_AR
    
    // Rendering thread and coroutines
    private var renderingScope: CoroutineScope? = null
    private var renderThread: Thread? = null
    
    // Surface and graphics
    private var surfaceReady = false
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    
    // HUD elements to render — CopyOnWriteArrayList for safe concurrent reads/iteration
    private val hudElements = CopyOnWriteArrayList<HUDElement>()
    private val renderQueue = CopyOnWriteArrayList<RenderCommand>()
    
    // Performance metrics
    private var frameCount = 0
    private var lastFPSCheck = 0L
    private var currentFPS = 0f
    
    // ARVision liquid effects
    private var liquidAnimationTime = 0f
    private val vibrancyIntensity = 1.0f
    
    init {
        holder.addCallback(this)
        setZOrderOnTop(true) // Render above other views
    }
    
    /**
     * Initialize the HUD renderer
     */
    fun initialize(): Boolean {
        return try {
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Start HUD rendering at target FPS
     */
    suspend fun startRendering(
        targetFPS: Int = 90,
        renderMode: RenderMode = RenderMode.SPATIAL_AR
    ) {
        if (isRendering) return
        
        this.targetFPS = targetFPS
        this.renderMode = renderMode
        isRendering = true
        
        renderingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        
        renderingScope?.launch {
            startRenderLoop()
        }
    }
    
    /**
     * Stop HUD rendering
     */
    fun stopRendering() {
        isRendering = false
        renderingScope?.cancel()
        renderThread?.interrupt()
    }
    
    /**
     * Add HUD element to render queue
     */
    fun addHUDElement(element: HUDElement) {
        hudElements.add(element)
    }

    /**
     * Remove HUD element from render queue
     */
    fun removeHUDElement(elementId: String) {
        hudElements.removeIf { it.id == elementId }
    }

    /**
     * Clear all HUD elements
     */
    fun clearHUDElements() {
        hudElements.clear()
        renderQueue.clear()
    }
    
    /**
     * Update mode-specific rendering
     */
    fun updateModeRendering(mode: com.augmentalis.voiceoscore.managers.hudmanager.stubs.HUDMode) {
        when (mode) {
            com.augmentalis.voiceoscore.managers.hudmanager.stubs.HUDMode.MEETING -> {
                // Minimal, non-intrusive rendering
                paint.alpha = 128
            }
            com.augmentalis.voiceoscore.managers.hudmanager.stubs.HUDMode.DRIVING -> {
                // High contrast, voice-only indicators
                paint.alpha = 255
            }
            com.augmentalis.voiceoscore.managers.hudmanager.stubs.HUDMode.WORKSHOP -> {
                // Bold, safety-focused rendering
                paint.alpha = 220
            }
            else -> {
                // Standard rendering
                paint.alpha = 200
            }
        }
    }
    
    /**
     * Adjust rendering for head movement
     */
    fun adjustForHeadMovement(orientationData: Any) {
        // CopyOnWriteArrayList iteration is safe without explicit synchronization
        hudElements.forEach { element ->
            element.adjustForHeadMovement(orientationData)
        }
    }
    
    /**
     * Main rendering loop
     */
    private suspend fun startRenderLoop() {
        val targetFrameTime = 1000L / targetFPS
        var lastFrameTime = System.currentTimeMillis()
        
        while (isRendering) {
            val frameStart = System.currentTimeMillis()
            
            if (surfaceReady) {
                renderFrame()
            }
            
            // Calculate frame timing
            val frameTime = System.currentTimeMillis() - frameStart
            val sleepTime = maxOf(0, targetFrameTime - frameTime)
            
            if (sleepTime > 0) {
                delay(sleepTime)
            }
            
            // Update performance metrics
            updateFPSMetrics(frameStart)
            liquidAnimationTime += (frameStart - lastFrameTime) / 1000f
            lastFrameTime = frameStart
        }
    }
    
    /**
     * Render single frame with ARVision effects
     */
    private fun renderFrame() {
        val canvas = holder.lockCanvas() ?: return
        
        try {
            // Clear canvas with transparent background
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            
            // Render background effects
            renderBackgroundEffects(canvas)
            
            // Render HUD elements with liquid animations
            // CopyOnWriteArrayList snapshot iteration is safe on the Canvas thread
            hudElements.forEach { element ->
                renderHUDElement(canvas, element)
            }
            
            // Render performance overlay (debug mode)
            @Suppress("ConstantConditionIf")
            if (false) { // Performance overlay disabled in production
                renderPerformanceOverlay(canvas)
            }
            
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }
    
    /**
     * Render background glass morphism effects
     */
    private fun renderBackgroundEffects(canvas: Canvas) {
        when (renderMode) {
            RenderMode.SPATIAL_AR -> {
                // ARVision glass morphism backdrop
                val gradient = LinearGradient(
                    0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(),
                    intArrayOf(
                        Color.argb((25 * vibrancyIntensity).toInt(), 255, 255, 255),
                        Color.argb((15 * vibrancyIntensity).toInt(), 255, 255, 255),
                        Color.argb((5 * vibrancyIntensity).toInt(), 255, 255, 255),
                        Color.TRANSPARENT
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)
                paint.shader = null
                
                // Add subtle particle effects for depth
                renderARVisionParticles(canvas)
            }
            RenderMode.OVERLAY -> {
                // Minimal overlay with liquid blur
                renderLiquidOverlay(canvas)
            }
            RenderMode.IMMERSIVE -> {
                // Full ARVision environment rendering
                renderEnvironmentEffects(canvas)
            }
        }
    }
    
    /**
     * Render individual HUD element with liquid effects
     */
    private fun renderHUDElement(canvas: Canvas, element: HUDElement) {
        canvas.save()
        
        // Apply spatial positioning
        canvas.translate(
            element.position.x * canvas.width,
            element.position.y * canvas.height
        )
        
        // Apply liquid animation transforms
        val liquidScale = 1.0f + sin(liquidAnimationTime * 2f + element.animationOffset) * 0.02f
        canvas.scale(liquidScale, liquidScale)
        
        // Render based on element type
        when (element.type) {
            HUDElementType.VOICE_COMMAND -> renderVoiceCommand(canvas, element)
            HUDElementType.CONFIDENCE_BAR -> renderConfidenceBar(canvas, element)
            HUDElementType.NOTIFICATION -> renderNotification(canvas, element)
            HUDElementType.CONTROL_PANEL -> renderControlPanel(canvas, element)
            HUDElementType.GAZE_INDICATOR -> renderGazeIndicator(canvas, element)
        }
        
        canvas.restore()
    }
    
    /**
     * Render voice command with ARVision styling
     */
    private fun renderVoiceCommand(canvas: Canvas, element: HUDElement) {
        val data = element.data as VoiceCommandData
        
        // Glass morphism background
        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = getARVisionColor(data.category)
            alpha = (150 * data.confidence).toInt()
        }
        
        // Liquid shape
        val cornerRadius = 16f + sin(liquidAnimationTime + element.animationOffset) * 4f
        val rect = RectF(-100f, -20f, 100f, 20f)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)
        
        // Vibrancy border
        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.WHITE
            alpha = (100 * vibrancyIntensity).toInt()
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
        
        // Text with adaptive styling
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 28f * element.scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(data.text, 0f, 8f, textPaint)
    }
    
    /**
     * Render confidence bar with liquid animation
     */
    private fun renderConfidenceBar(canvas: Canvas, element: HUDElement) {
        val data = element.data as ConfidenceData
        val barWidth = 160f
        val barHeight = 12f
        
        // Background
        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(50, 255, 255, 255)
        }
        val bgRect = RectF(-barWidth/2, -barHeight/2, barWidth/2, barHeight/2)
        canvas.drawRoundRect(bgRect, barHeight/2, barHeight/2, bgPaint)
        
        // Confidence fill with liquid movement
        val fillWidth = barWidth * data.confidence
        val liquidOffset = sin(liquidAnimationTime * 3f) * 2f
        
        val fillPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                -fillWidth/2, 0f, fillWidth/2, 0f,
                intArrayOf(
                    Color.argb(180, 0, 122, 255),
                    Color.argb(255, 52, 199, 89),
                    Color.argb(180, 0, 122, 255)
                ),
                floatArrayOf(0f, 0.5f + liquidOffset * 0.1f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        
        val fillRect = RectF(-fillWidth/2, -barHeight/2, fillWidth/2, barHeight/2)
        canvas.drawRoundRect(fillRect, barHeight/2, barHeight/2, fillPaint)
    }
    
    /**
     * Get ARVision color for category
     */
    private fun getARVisionColor(category: String): Int {
        return when (category) {
            "NAVIGATION" -> Color.argb(200, 0, 122, 255)    // Blue
            "GESTURE" -> Color.argb(200, 52, 199, 89)       // Green
            "SYSTEM" -> Color.argb(200, 255, 149, 0)        // Orange
            "ACCESSIBILITY" -> Color.argb(200, 175, 82, 222) // Purple
            else -> Color.argb(200, 117, 117, 117)          // Gray
        }
    }
    
    /**
     * Render ARVision particle effects
     */
    private fun renderARVisionParticles(canvas: Canvas) {
        val particlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb((15 * vibrancyIntensity).toInt(), 255, 255, 255)
        }
        
        // Floating particles with liquid movement
        for (i in 0 until 8) {
            val phase = liquidAnimationTime + i * 0.8f
            val x = (sin(phase) * canvas.width * 0.3f) + canvas.width * 0.5f
            val y = (cos(phase * 0.6f) * canvas.height * 0.2f) + canvas.height * 0.5f
            val radius = 1.5f + sin(phase * 2f) * 0.5f
            
            canvas.drawCircle(x, y, radius, particlePaint)
        }
    }
    
    /**
     * Render liquid overlay for minimal mode
     */
    private fun renderLiquidOverlay(canvas: Canvas) {
        val overlayPaint = Paint().apply {
            isAntiAlias = true
            shader = RadialGradient(
                canvas.width * 0.5f, canvas.height * 0.5f,
                canvas.width * 0.7f,
                intArrayOf(
                    Color.argb((10 * vibrancyIntensity).toInt(), 255, 255, 255),
                    Color.TRANSPARENT
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), overlayPaint)
    }
    
    /**
     * Render environment effects for immersive mode
     */
    private fun renderEnvironmentEffects(canvas: Canvas) {
        // ARVision environmental glass effects
        val envPaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb((20 * vibrancyIntensity).toInt(), 255, 255, 255)
        }
        
        // Animated environmental elements
        for (i in 0 until 15) {
            val phase = liquidAnimationTime * 0.3f + i * 0.4f
            val x = (sin(phase) * canvas.width * 0.4f) + canvas.width * 0.5f
            val y = (cos(phase * 0.8f + i) * canvas.height * 0.3f) + canvas.height * 0.5f
            val radius = 2f + sin(phase * 1.5f) * 1f
            
            canvas.drawCircle(x, y, radius, envPaint)
        }
        
        // Add depth blur effect
        val blurPaint = Paint().apply {
            isAntiAlias = true
            maskFilter = android.graphics.BlurMaskFilter(8f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            color = Color.argb((5 * vibrancyIntensity).toInt(), 255, 255, 255)
        }
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), blurPaint)
    }
    
    /**
     * Render performance overlay
     */
    private fun renderPerformanceOverlay(canvas: Canvas) {
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.GREEN
            textSize = 24f
            typeface = Typeface.MONOSPACE
        }
        
        canvas.drawText("FPS: ${currentFPS.toInt()}", 20f, 50f, textPaint)
        canvas.drawText("Elements: ${hudElements.size}", 20f, 80f, textPaint)
    }
    
    /**
     * Update FPS metrics
     */
    private fun updateFPSMetrics(currentTime: Long) {
        frameCount++
        
        if (currentTime - lastFPSCheck >= 1000) {
            currentFPS = frameCount / ((currentTime - lastFPSCheck) / 1000f)
            frameCount = 0
            lastFPSCheck = currentTime
        }
    }
    
    // SurfaceHolder.Callback implementations
    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReady = true
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Handle surface changes
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        stopRendering()
    }
    
    /**
     * Render notification element
     */
    @Suppress("UNUSED_PARAMETER")
    private fun renderNotification(canvas: Canvas, element: HUDElement) {
        // Stub implementation
    }

    /**
     * Render control panel element
     */
    @Suppress("UNUSED_PARAMETER")
    private fun renderControlPanel(canvas: Canvas, element: HUDElement) {
        // Stub implementation
    }

    /**
     * Render gaze indicator element
     */
    @Suppress("UNUSED_PARAMETER")
    private fun renderGazeIndicator(canvas: Canvas, element: HUDElement) {
        // Stub implementation
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        stopRendering()
        clearHUDElements()
    }
}

/**
 * Render modes for different environments
 */
enum class RenderMode {
    SPATIAL_AR,    // 3D spatial AR rendering
    OVERLAY,       // Simple 2D overlay
    IMMERSIVE      // Full immersive environment
}

/**
 * HUD element types
 */
enum class HUDElementType {
    VOICE_COMMAND,
    CONFIDENCE_BAR,
    NOTIFICATION,
    CONTROL_PANEL,
    GAZE_INDICATOR
}

/**
 * Base HUD element
 */
data class HUDElement(
    val id: String,
    val type: HUDElementType,
    val position: SpatialPosition,
    val data: Any,
    val scale: Float = 1.0f,
    val animationOffset: Float = 0f
) {
    @Suppress("UNUSED_PARAMETER")
    fun adjustForHeadMovement(orientationData: Any) {
        // Apply head movement compensation
        // Implementation depends on orientation data structure
    }
}

/**
 * Voice command rendering data
 */
data class VoiceCommandData(
    val text: String,
    val confidence: Float,
    val category: String
)

/**
 * Confidence bar rendering data  
 */
data class ConfidenceData(
    val confidence: Float,
    val label: String = ""
)

/**
 * Render command for processing queue
 */
data class RenderCommand(
    val type: RenderCommandType,
    val element: HUDElement,
    val timestamp: Long = System.currentTimeMillis()
)

enum class RenderCommandType {
    ADD,
    UPDATE, 
    REMOVE,
    CLEAR
}