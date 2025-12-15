package com.augmentalis.cockpit.mvp.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.workspace.Vector3D
import com.avanues.cockpit.layout.presets.LayoutPreset
import com.avanues.cockpit.layout.presets.WindowPosition

/**
 * Spatial window renderer for curved 3D window display
 *
 * Architecture:
 * - Direct Canvas rendering (no HUDManager dependencies)
 * - Uses CurvedProjection for 3D → 2D transformation
 * - Supports any LayoutPreset (Linear, Arc, Theater)
 * - Depth sorting for correct occlusion
 * - Glassmorphic styling consistent with Phase 1
 * - Scale factor support for pinch-to-zoom
 *
 * Performance:
 * - Target: 60 FPS
 * - Optimizations: Depth sorting, visibility culling, pre-allocated paints
 */
class SpatialWindowRenderer(
    private val layoutPreset: LayoutPreset,
    private val qualityManager: AdaptiveQualityManager? = null
) {

    // Cache last rendered windows for hit detection
    private var lastRenderedWindows: List<RenderedWindow> = emptyList()

    // Pre-allocated paints for performance
    private val glassSurfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.parseColor("#08FFFFFF")  // Ocean Theme glassSurface (3%)
    }

    private val glassBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.5f  // Hair-thin border
        color = android.graphics.Color.parseColor("#1AFFFFFF")  // Ocean Theme glassBorder (10%)
    }

    private val glassTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.parseColor("#F2FFFFFF")  // Ocean Theme textPrimary
        textSize = 40f
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.parseColor("#14000000")  // Ocean Theme glassShadow (8%)
    }

    /**
     * Window with rendered metadata
     */
    private data class RenderedWindow(
        val window: AppWindow,
        val quad: CurvedProjection.CurvedQuad,
        val colorAccent: Int
    )

    /**
     * Render all windows with curved projection
     *
     * Process:
     * 1. Calculate positions using layoutPreset
     * 2. Project to 2D with CurvedProjection
     * 3. Apply scale factor to dimensions
     * 4. Sort by depth (far to near)
     * 5. Render each window with glassmorphic styling + WebView bitmaps
     *
     * @param canvas Target canvas for rendering
     * @param windows List of windows to render
     * @param windowColors Map of window ID to color hex string
     * @param selectedWindowId ID of selected window (for highlighting), null if none
     * @param centerPoint Center point for layout calculation
     * @param scaleFactor Uniform scale factor for all windows (0.5x to 2.0x)
     * @param bitmapProvider Function to get bitmap for window ID (null if not yet rendered)
     */
    fun render(
        canvas: Canvas,
        windows: List<AppWindow>,
        windowColors: Map<String, String>,
        selectedWindowId: String? = null,
        centerPoint: Vector3D = Vector3D(0f, 0f, -2f),
        scaleFactor: Float = 1.0f,
        bitmapProvider: ((String) -> android.graphics.Bitmap?)? = null
    ) {
        if (windows.isEmpty()) return

        val screenWidth = canvas.width
        val screenHeight = canvas.height

        // Clear canvas
        canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // Calculate spatial positions using layout preset
        val positions = layoutPreset.calculatePositions(windows, centerPoint)
        val positionMap = positions.associate { it.windowId to it.position }

        // Project all windows to 2D with curved projection
        val renderedWindows = windows.mapNotNull { window ->
            val position = positionMap[window.id] ?: return@mapNotNull null

            // Apply scale factor to window dimensions
            val scaledWindow = window.copy(
                widthMeters = window.widthMeters * scaleFactor,
                heightMeters = window.heightMeters * scaleFactor
            )

            val quad = CurvedProjection.generateCurvedQuad(
                scaledWindow,
                position,
                screenWidth,
                screenHeight
            )

            // Skip if not visible
            if (!quad.topLeft.isVisible) return@mapNotNull null

            val colorHex = windowColors[window.id] ?: "#FF6B9D"
            val colorInt = android.graphics.Color.parseColor(colorHex)

            RenderedWindow(window, quad, colorInt)
        }

        // Sort by depth (far to near for correct occlusion)
        val sortedWindows = renderedWindows.sortedByDescending { it.quad.centerDepth }

        // Cache for hit detection
        lastRenderedWindows = sortedWindows

        // Render each window
        sortedWindows.forEach {
            val isSelected = it.window.id == selectedWindowId
            renderWindow(canvas, it, scaleFactor, isSelected, bitmapProvider)
        }
    }

    /**
     * Render a single window with glassmorphic styling
     *
     * @param canvas Target canvas
     * @param renderedWindow Window with projection metadata
     * @param scaleFactor Global scale factor for UI elements
     * @param isSelected Whether this window is selected (for highlighting)
     * @param bitmapProvider Function to get bitmap for window ID
     */
    private fun renderWindow(
        canvas: Canvas,
        renderedWindow: RenderedWindow,
        scaleFactor: Float,
        isSelected: Boolean = false,
        bitmapProvider: ((String) -> android.graphics.Bitmap?)? = null
    ) {
        val quad = renderedWindow.quad
        val window = renderedWindow.window

        // Create quad path
        val path = Path().apply {
            moveTo(quad.topLeft.screenX, quad.topLeft.screenY)
            lineTo(quad.topRight.screenX, quad.topRight.screenY)
            lineTo(quad.bottomRight.screenX, quad.bottomRight.screenY)
            lineTo(quad.bottomLeft.screenX, quad.bottomLeft.screenY)
            close()
        }

        // Apply opacity based on depth (atmospheric perspective)
        val surfacePaint = Paint(glassSurfacePaint).apply {
            alpha = (255 * quad.opacity).toInt()

            // Apply depth-of-field blur (adaptive)
            val blurRadius = CurvedProjection.calculateDepthBlur(quad.centerDepth)
            val adaptiveBlur = qualityManager?.applyAdaptiveBlur(blurRadius) ?: 0f
            if (adaptiveBlur > 0.5f) {
                maskFilter = android.graphics.BlurMaskFilter(
                    adaptiveBlur,
                    android.graphics.BlurMaskFilter.Blur.NORMAL
                )
            }
        }
        val borderPaint = Paint(glassBorderPaint).apply {
            alpha = (255 * quad.opacity).toInt()
            // Thicker border when selected (hair-thin precision)
            strokeWidth = if (isSelected) 1.5f * scaleFactor else 0.5f * scaleFactor
            // Brighter color when selected
            color = if (isSelected)
                android.graphics.Color.parseColor("#CCFFFFFF")
            else
                android.graphics.Color.parseColor("#1AFFFFFF")
        }
        val textPaint = Paint(glassTextPaint).apply {
            alpha = (255 * quad.opacity).toInt()
            // Scale text based on perspective AND scale factor
            textSize = 40f * quad.topLeft.scale * scaleFactor

            // Apply depth-of-field blur (adaptive)
            val blurRadius = CurvedProjection.calculateDepthBlur(quad.centerDepth)
            val adaptiveBlur = qualityManager?.applyAdaptiveBlur(blurRadius) ?: 0f
            if (adaptiveBlur > 0.5f) {
                maskFilter = android.graphics.BlurMaskFilter(
                    adaptiveBlur,
                    android.graphics.BlurMaskFilter.Blur.NORMAL
                )
            }
        }

        // Draw shadow (offset down-right, scaled)
        val shadowPath = Path(path).apply {
            offset(8f * scaleFactor, 8f * scaleFactor)
        }
        canvas.drawPath(shadowPath, shadowPaint)

        // Draw ambient occlusion at corners (realistic lighting - XR material effect)
        val aoRadius = 16f * scaleFactor
        val aoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#33000000")  // 20% black
            alpha = (255 * quad.opacity).toInt()
        }

        // Helper: Draw radial gradient AO at corner
        fun drawCornerAO(cx: Float, cy: Float) {
            aoPaint.shader = android.graphics.RadialGradient(
                cx, cy, aoRadius,
                intArrayOf(aoPaint.color, android.graphics.Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, aoRadius, aoPaint)
            aoPaint.shader = null  // Reset shader
        }

        // Draw AO at all 4 corners
        drawCornerAO(quad.topLeft.screenX, quad.topLeft.screenY)
        drawCornerAO(quad.topRight.screenX, quad.topRight.screenY)
        drawCornerAO(quad.bottomRight.screenX, quad.bottomRight.screenY)
        drawCornerAO(quad.bottomLeft.screenX, quad.bottomLeft.screenY)

        // Draw glassmorphic surface
        canvas.drawPath(path, surfacePaint)

        // Draw color accent at top (PSEUDO-SPATIAL: taller and more opaque for visibility)
        val accentHeight = 20f * scaleFactor  // Increased from 12f for better visibility
        val accentPath = Path().apply {
            moveTo(quad.topLeft.screenX, quad.topLeft.screenY)
            lineTo(quad.topRight.screenX, quad.topRight.screenY)
            lineTo(quad.topRight.screenX, quad.topRight.screenY + accentHeight)
            lineTo(quad.topLeft.screenX, quad.topLeft.screenY + accentHeight)
            close()
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = renderedWindow.colorAccent
            // Keep accent fully opaque for visibility (don't fade with depth)
            alpha = 255  // Always 100% opaque
        }
        canvas.drawPath(accentPath, accentPaint)

        // Draw border
        canvas.drawPath(path, borderPaint)

        // Draw Fresnel edge glow (edges brighter than center - XR material effect)
        val fresnelPath = Path(path)
        val fresnelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f * scaleFactor
            color = android.graphics.Color.parseColor("#26FFFFFF")  // 15% white
            alpha = (255 * quad.opacity * 0.5f).toInt()  // 50% of window opacity
            maskFilter = android.graphics.BlurMaskFilter(
                4f * scaleFactor,
                android.graphics.BlurMaskFilter.Blur.OUTER
            )
        }
        canvas.drawPath(fresnelPath, fresnelPaint)

        // Draw window content (bitmap or title text fallback)
        val centerX = (quad.topLeft.screenX + quad.topRight.screenX) / 2f
        val centerY = (quad.topLeft.screenY + quad.bottomLeft.screenY) / 2f

        // Try to get bitmap from provider
        val bitmap = bitmapProvider?.invoke(window.id)

        if (bitmap != null) {
            // Draw WebView bitmap content
            val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = (255 * quad.opacity).toInt()
            }

            // Calculate destination rect to fit within quad
            val quadWidth = quad.topRight.screenX - quad.topLeft.screenX
            val quadHeight = quad.bottomLeft.screenY - quad.topLeft.screenY

            // Scale bitmap to fit quad while maintaining aspect ratio
            val destRect = android.graphics.RectF(
                quad.topLeft.screenX,
                quad.topLeft.screenY,
                quad.topRight.screenX,
                quad.bottomLeft.screenY
            )

            val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)

            canvas.drawBitmap(bitmap, srcRect, destRect, bitmapPaint)
        } else {
            // Fallback: Draw window title text (while bitmap is loading)
            val textWidth = textPaint.measureText(window.title)
            val textX = centerX - (textWidth / 2f)
            val textY = centerY  // Baseline

            canvas.drawText(window.title, textX, textY, textPaint)
        }

        // Draw close button (X icon) in top-right corner
        val buttonSize = 32f * quad.topLeft.scale * scaleFactor
        val buttonX = quad.topRight.screenX - buttonSize - (8f * scaleFactor)
        val buttonY = quad.topRight.screenY + (8f * scaleFactor)

        // Draw button background (semi-transparent)
        val buttonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.parseColor("#40000000")
            alpha = (255 * quad.opacity).toInt()
        }
        canvas.drawCircle(
            buttonX + buttonSize / 2f,
            buttonY + buttonSize / 2f,
            buttonSize / 2f,
            buttonBgPaint
        )

        // Draw X icon
        val xPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f * quad.topLeft.scale * scaleFactor
            color = android.graphics.Color.WHITE
            alpha = (255 * quad.opacity).toInt()
        }
        val xPadding = buttonSize * 0.3f
        canvas.drawLine(
            buttonX + xPadding,
            buttonY + xPadding,
            buttonX + buttonSize - xPadding,
            buttonY + buttonSize - xPadding,
            xPaint
        )
        canvas.drawLine(
            buttonX + buttonSize - xPadding,
            buttonY + xPadding,
            buttonX + xPadding,
            buttonY + buttonSize - xPadding,
            xPaint
        )
    }

    /**
     * Check if a touch point hit any close button
     *
     * Returns the window ID if a close button was hit, null otherwise
     * Accounts for scale factor in hit detection
     *
     * @param touchX Touch X coordinate
     * @param touchY Touch Y coordinate
     * @param windows List of windows (for context)
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     * @return Window ID if hit, null otherwise
     */
    fun checkCloseButtonHit(
        touchX: Float,
        touchY: Float,
        windows: List<AppWindow>,
        screenWidth: Int,
        screenHeight: Int
    ): String? {
        // Iterate in reverse order (front to back) for correct hit detection
        for (renderedWindow in lastRenderedWindows.reversed()) {
            val quad = renderedWindow.quad

            // Button size accounts for both perspective and scale factor
            val buttonSize = 32f * quad.topLeft.scale
            val buttonX = quad.topRight.screenX - buttonSize - 8f
            val buttonY = quad.topRight.screenY + 8f

            // Check if touch is within close button bounds
            val buttonCenterX = buttonX + buttonSize / 2f
            val buttonCenterY = buttonY + buttonSize / 2f
            val buttonRadius = buttonSize / 2f

            // Circular hit detection
            val dx = touchX - buttonCenterX
            val dy = touchY - buttonCenterY
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)

            if (distance <= buttonRadius) {
                return renderedWindow.window.id
            }
        }

        return null
    }

    /**
     * Check if a point hits any window (for head cursor highlighting)
     *
     * Uses point-in-polygon test to determine if cursor is over any window
     * Returns the topmost window ID (front-to-back order)
     *
     * @param touchX X coordinate in screen space
     * @param touchY Y coordinate in screen space
     * @return Window ID if hit, null otherwise
     */
    fun checkWindowHit(
        touchX: Float,
        touchY: Float
    ): String? {
        // Iterate in reverse order (front to back) for correct hit detection
        for (renderedWindow in lastRenderedWindows.reversed()) {
            val quad = renderedWindow.quad

            // Check if point is inside quad using ray casting algorithm
            if (isPointInQuad(touchX, touchY, quad)) {
                return renderedWindow.window.id
            }
        }

        return null
    }

    /**
     * Point-in-polygon test for arbitrary quadrilateral
     *
     * Uses ray casting algorithm: cast a ray from the point to infinity
     * and count how many edges it crosses. Odd = inside, even = outside
     */
    private fun isPointInQuad(x: Float, y: Float, quad: CurvedProjection.CurvedQuad): Boolean {
        val vertices = listOf(
            quad.topLeft.screenX to quad.topLeft.screenY,
            quad.topRight.screenX to quad.topRight.screenY,
            quad.bottomRight.screenX to quad.bottomRight.screenY,
            quad.bottomLeft.screenX to quad.bottomLeft.screenY
        )

        var inside = false
        var j = vertices.size - 1

        for (i in vertices.indices) {
            val (xi, yi) = vertices[i]
            val (xj, yj) = vertices[j]

            // Ray casting: check if horizontal ray from point crosses edge
            val intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi)

            if (intersect) inside = !inside
            j = i
        }

        return inside
    }
}
