package com.augmentalis.uuidcreator.models

/**
 * Spatial position information for UI elements (VUID migration)
 *
 * Migration: UUID → VUID (VoiceUniqueID)
 * Created: 2025-12-23
 */
data class VUIDPosition(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val depth: Float = 0f,
    val index: Int = 0,
    val row: Int = 0,
    val column: Int = 0,
    val bounds: VUIDBounds? = null
) {
    /**
     * Convert to deprecated UUIDPosition for backwards compatibility
     */
    fun toUUIDPosition(): UUIDPosition = UUIDPosition(
        x = x,
        y = y,
        z = z,
        width = width,
        height = height,
        depth = depth,
        index = index,
        row = row,
        column = column,
        bounds = bounds?.toUUIDBounds()
    )

    companion object {
        /**
         * Convert from deprecated UUIDPosition
         */
        fun fromUUIDPosition(position: UUIDPosition): VUIDPosition = VUIDPosition(
            x = position.x,
            y = position.y,
            z = position.z,
            width = position.width,
            height = position.height,
            depth = position.depth,
            index = position.index,
            row = position.row,
            column = position.column,
            bounds = position.bounds?.let { VUIDBounds.fromUUIDBounds(it) }
        )
    }
}

/**
 * Bounding box for precise positioning (VUID migration)
 */
data class VUIDBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    /**
     * Convert to deprecated UUIDBounds for backwards compatibility
     */
    fun toUUIDBounds(): UUIDBounds = UUIDBounds(
        left = left,
        top = top,
        right = right,
        bottom = bottom
    )

    companion object {
        /**
         * Convert from deprecated UUIDBounds
         */
        fun fromUUIDBounds(bounds: UUIDBounds): VUIDBounds = VUIDBounds(
            left = bounds.left,
            top = bounds.top,
            right = bounds.right,
            bottom = bounds.bottom
        )
    }
}
