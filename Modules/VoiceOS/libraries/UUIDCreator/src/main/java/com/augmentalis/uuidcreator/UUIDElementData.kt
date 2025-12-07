/**
 * UUIDElementData.kt - UUID element data for IPC
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Code Quality Expert
 * Created: 2025-11-10
 */
package com.augmentalis.uuidcreator

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.augmentalis.uuidcreator.models.UUIDElement
import com.augmentalis.uuidcreator.models.UUIDPosition

/**
 * UUID element data for IPC
 *
 * Simplified parcelable version of UUIDElement without action lambdas.
 * Action lambdas cannot be serialized across process boundaries, so they are omitted.
 *
 * Use toUUIDElement() to convert to full UUIDElement (without actions).
 * Use fromUUIDElement() to create from existing UUIDElement.
 */
@Parcelize
data class UUIDElementData(
    /**
     * Unique identifier for this element
     */
    val uuid: String,

    /**
     * Human-readable name (optional)
     */
    val name: String? = null,

    /**
     * Element type (e.g., "button", "text", "image")
     */
    val type: String = "unknown",

    /**
     * X coordinate in pixels (screen space)
     */
    val x: Float = 0f,

    /**
     * Y coordinate in pixels (screen space)
     */
    val y: Float = 0f,

    /**
     * Element width in pixels
     */
    val width: Float = 0f,

    /**
     * Element height in pixels
     */
    val height: Float = 0f,

    /**
     * Whether element is enabled for interaction
     */
    val isEnabled: Boolean = true,

    /**
     * Additional metadata as JSON string (optional)
     */
    val metadata: String? = null
) : Parcelable {

    /**
     * Convert to UUIDElement (without actions)
     *
     * Actions cannot be transmitted via IPC, so the returned element
     * will have an empty actions map. Use executeAction via the IPC
     * service instead.
     *
     * @return UUIDElement representation
     */
    fun toUUIDElement(): UUIDElement {
        return UUIDElement(
            uuid = uuid,
            name = name,
            type = type,
            position = UUIDPosition(
                x = x,
                y = y,
                width = width,
                height = height
            ),
            isEnabled = isEnabled,
            actions = emptyMap()  // Actions cannot be passed via IPC
        )
    }

    companion object {
        /**
         * Create from UUIDElement
         *
         * Converts a full UUIDElement to parcelable data format.
         * Action lambdas are intentionally omitted as they cannot be serialized.
         *
         * @param element Source UUIDElement
         * @return Parcelable UUIDElementData
         */
        fun fromUUIDElement(element: UUIDElement): UUIDElementData {
            return UUIDElementData(
                uuid = element.uuid,
                name = element.name,
                type = element.type,
                x = element.position?.x ?: 0f,
                y = element.position?.y ?: 0f,
                width = element.position?.width ?: 0f,
                height = element.position?.height ?: 0f,
                isEnabled = element.isEnabled
            )
        }

        /**
         * Create minimal element data
         *
         * @param uuid Element UUID
         * @param name Element name
         * @param type Element type
         * @return UUIDElementData with minimal information
         */
        fun minimal(uuid: String, name: String? = null, type: String = "unknown"): UUIDElementData {
            return UUIDElementData(
                uuid = uuid,
                name = name,
                type = type
            )
        }
    }

    /**
     * Check if element has position information
     *
     * @return true if element has non-zero position/size, false otherwise
     */
    fun hasPosition(): Boolean {
        return x != 0f || y != 0f || width != 0f || height != 0f
    }

    /**
     * Get element bounds as Android Rect-like values
     *
     * @return Quadruple of (left, top, right, bottom)
     */
    fun getBounds(): Quadruple<Float, Float, Float, Float> {
        return Quadruple(x, y, x + width, y + height)
    }

    /**
     * Check if point is within element bounds
     *
     * @param px Point X coordinate
     * @param py Point Y coordinate
     * @return true if point is within bounds, false otherwise
     */
    fun contains(px: Float, py: Float): Boolean {
        return px >= x && px < (x + width) &&
                py >= y && py < (y + height)
    }
}

/**
 * Simple quadruple data class
 */
data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
