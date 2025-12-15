package com.augmentalis.cockpit.mvp.curved

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * CurvedImage - Custom view for rendering curved bitmap snapshots
 *
 * Takes bitmap snapshots of content (WebViews, etc.) and applies
 * curved matrix transformation for pseudo-spatial display effect.
 *
 * Curve is controlled by leftHeight and rightHeight (0.0-1.0):
 * - 1.0 = full height (no squish)
 * - < 1.0 = squeezed vertically (creates curve effect)
 *
 * Example:
 * - Left window: leftHeight=1.0, rightHeight=0.5 (curves to right)
 * - Center window: leftHeight=1.0, rightHeight=1.0 (flat)
 * - Right window: leftHeight=0.5, rightHeight=1.0 (curves to left)
 */
class CurvedImage : View {

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        refreshSurfaceCoordinates()
    }

    /**
     * Bitmap snapshot of content to be curved and displayed
     */
    var image: Bitmap? = null
        set(value) {
            field = value
            refreshImageCoordinates()
            invalidate()
        }

    private val imagePaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }

    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(width, height, oldw, oldh)
        refreshSurfaceCoordinates(width.toFloat(), height.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        image?.let { bitmap ->
            // Save canvas state
            canvas.save()

            // Apply curved 3D perspective transformation
            val matrix = Matrix()
            val camera = Camera()

            // Calculate rotation angle based on curve intensity
            val rotationY = calculateRotationAngle()

            camera.save()
            camera.rotateY(rotationY)  // Y-axis rotation for left/right curve

            // Get transformation matrix from camera
            camera.getMatrix(matrix)
            camera.restore()

            // Center the rotation around view center
            val centerX = width / 2f
            val centerY = height / 2f
            matrix.preTranslate(-centerX, -centerY)
            matrix.postTranslate(centerX, centerY)

            // Scale bitmap to fit view
            val scaleX = width.toFloat() / bitmap.width
            val scaleY = height.toFloat() / bitmap.height
            matrix.preScale(scaleX, scaleY)

            // Draw bitmap with curved transformation
            canvas.drawBitmap(bitmap, matrix, imagePaint)

            // Restore canvas
            canvas.restore()
        }
    }

    /**
     * Height percentage for left and right edges (0.0-1.0)
     * Controls curve intensity via 3D rotation
     */
    private var leftHeight = 1.0f
    private var rightHeight = 1.0f

    /**
     * Set curve intensity by adjusting edge heights
     *
     * @param leftHeight Left edge height (0.0-1.0, 1.0 = full height)
     * @param rightHeight Right edge height (0.0-1.0, 1.0 = full height)
     */
    fun setHeightPercentage(leftHeight: Float, rightHeight: Float) {
        this.leftHeight = leftHeight.coerceIn(0f, 1f)
        this.rightHeight = rightHeight.coerceIn(0f, 1f)
        invalidate()  // Trigger redraw with new curve
    }

    /**
     * Calculate Y-axis rotation angle from height percentages
     *
     * Creates smooth curved perspective:
     * - leftHeight < rightHeight: Rotate right (right edge forward)
     * - rightHeight < leftHeight: Rotate left (left edge forward)
     * - Equal heights: No rotation (flat)
     */
    private fun calculateRotationAngle(): Float {
        val heightDiff = rightHeight - leftHeight
        // Map height difference [-1, 1] to rotation angle [-45°, 45°]
        // Increased from 25° to 45° for more visible curve effect
        return heightDiff * 45f
    }

    /**
     * No longer needed with Camera-based approach
     * Kept for compatibility
     */
    private fun refreshSurfaceCoordinates(width: Float = this.width.toFloat(), height: Float = this.height.toFloat()) {
        // Now handled by Camera transformation in onDraw
    }

    private fun refreshImageCoordinates() {
        // Now handled by Camera transformation in onDraw
        invalidate()
    }
}
