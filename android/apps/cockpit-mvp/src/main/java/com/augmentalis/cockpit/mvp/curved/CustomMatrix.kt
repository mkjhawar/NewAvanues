package com.augmentalis.cockpit.mvp.curved

import android.graphics.Matrix

/**
 * CustomMatrix for curved image transformation
 *
 * Extends Matrix to support setPolyToPoly-style transformations
 * for creating curved display effects on bitmap snapshots.
 *
 * Used by CurvedImage to distort bitmaps into curved shapes
 * for pseudo-spatial display on flat LCD screens.
 */
class CustomMatrix : Matrix() {

    private val src = FloatArray(8)
    private val dst = FloatArray(8)

    /**
     * Set source coordinates (original bitmap corners)
     * @param points TopLeft xy, TopRight xy, BottomLeft xy, BottomRight xy
     */
    fun setSrc(points: FloatArray) {
        require(points.size == 8) { "Source points must have 8 values (4 corners, 2 coords each)" }
        points.copyInto(src)
        updateMatrix()
    }

    /**
     * Set destination coordinates (curved display corners)
     * @param points TopLeft xy, TopRight xy, BottomLeft xy, BottomRight xy
     */
    fun setDst(points: FloatArray) {
        require(points.size == 8) { "Destination points must have 8 values (4 corners, 2 coords each)" }
        points.copyInto(dst)
        updateMatrix()
    }

    /**
     * Update matrix transformation based on src/dst points
     */
    private fun updateMatrix() {
        if (src.all { it == 0f } || dst.all { it == 0f }) return
        setPolyToPoly(src, 0, dst, 0, 4)
    }
}
