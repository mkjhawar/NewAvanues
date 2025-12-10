package com.augmentalis.cockpit.mvp.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.workspace.Vector3D
import com.avanues.cockpit.layout.LayoutPreset

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
    private val layoutPreset: LayoutPreset
) {

    // Cache last rendered windows for hit detection
    private var lastRenderedWindows: List<RenderedWindow> = emptyList()

    // Pre-allocated paints for performance
    private val glassSurfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.parseColor("#14FFFFFF")  // Ocean Theme glassSurface
    }

    private val glassBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = android.graphics.Color.parseColor("#26FFFFFF")  // Ocean Theme glassBorder
    }

    private val glassTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.parseColor("#F2FFFFFF")  // Ocean Theme textPrimary
        textSize = 40f
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.parseColor("#40000000")  // Ocean Theme glassShadow
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
     * 5. Render each window with glassmorphic styling
     *
     * @param canvas Target canvas for rendering
     * @param windows List of windows to render
     * @param windowColors Map of window ID to color hex string
     * @param selectedWindowId ID of selected window (for highlighting), null if none
     * @param centerPoint Center point for layout calculation
     * @param scaleFactor Uniform scale factor for all windows (0.5x to 2.0x)
     */
    fun render(
        canvas: Canvas,
        windows: List<AppWindow>,
        windowColors: Map<String, String>,
        selectedWindowId: String? = null,
        centerPoint: Vector3D = Vector3D(0f, 0f, -2f),
        scaleFactor: Float = 1.0f
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
            renderWindow(canvas, it, scaleFactor, isSelected)
        }
    }

    /**
     * Render a single window with glassmorphic styling
     *
     * @param canvas Target canvas
     * @param renderedWindow Window with projection metadata
     * @param scaleFactor Global scale factor for UI elements
     * @param isSelected Whether this window is selected (for highlighting)
     */
    private fun renderWindow(
        canvas: Canvas,
        renderedWindow: RenderedWindow,
        scaleFactor: Float,
        isSelected: Boolean = false
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
        }
        val borderPaint = Paint(glassBorderPaint).apply {
            alpha = (255 * quad.opacity).toInt()
            // Thicker border when selected
            strokeWidth = if (isSelected) 4f * scaleFactor else 2f * scaleFactor
            // Brighter color when selected
            color = if (isSelected)
                android.graphics.Color.parseColor("#CCFFFFFF")
            else
                android.graphics.Color.parseColor("#26FFFFFF")
        }
        val textPaint = Paint(glassTextPaint).apply {
            alpha = (255 * quad.opacity).toInt()
            // Scale text based on perspective AND scale factor
            textSize = 40f * quad.topLeft.scale * scaleFactor
        }

        // Draw shadow (offset down-right, scaled)
        val shadowPath = Path(path).apply {
            offset(8f * scaleFactor, 8f * scaleFactor)
        }
        canvas.drawPath(shadowPath, shadowPaint)

        // Draw glassmorphic surface
        canvas.drawPath(path, surfacePaint)

        // Draw color accent at top (3dp height, scaled)
        val accentHeight = 12f * scaleFactor
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
            alpha = (255 * quad.opacity).toInt()
        }
        canvas.drawPath(accentPath, accentPaint)

        // Draw border
        canvas.drawPath(path, borderPaint)

        // Draw window title (centered)
        val centerX = (quad.topLeft.screenX + quad.topRight.screenX) / 2f
        val centerY = (quad.topLeft.screenY + quad.bottomLeft.screenY) / 2f

        // Measure text to center it
        val textWidth = textPaint.measureText(window.title)
        val textX = centerX - (textWidth / 2f)
        val textY = centerY  // Baseline

        canvas.drawText(window.title, textX, textY, textPaint)

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
