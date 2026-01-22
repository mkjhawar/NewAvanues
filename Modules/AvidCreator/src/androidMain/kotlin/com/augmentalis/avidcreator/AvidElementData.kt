/**
 * AvidElementData.kt - AVID element data for IPC
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Code Quality Expert
 * Created: 2025-11-10
 * Updated: 2026-01-15 - Migrated to AVID naming
 */
package com.augmentalis.avidcreator

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.augmentalis.avidcreator.AvidElement
import com.augmentalis.avidcreator.AvidPosition

/**
 * AVID element data for IPC
 *
 * Simplified parcelable version of AvidElement without action lambdas.
 * Action lambdas cannot be serialized across process boundaries, so they are omitted.
 *
 * Use toAvidElement() to convert to full AvidElement (without actions).
 * Use fromAvidElement() to create from existing AvidElement.
 */
@Parcelize
data class AvidElementData(
    /**
     * Unique identifier for this element
     */
    val avid: String,

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
     * Convert to AvidElement (without actions)
     *
     * Actions cannot be transmitted via IPC, so the returned element
     * will have an empty actions map. Use executeAction via the IPC
     * service instead.
     *
     * @return AvidElement representation
     */
    fun toAvidElement(): AvidElement {
        return AvidElement(
            avid = avid,
            name = name,
            type = type,
            position = AvidPosition(
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
         * Create from AvidElement
         *
         * Converts a full AvidElement to parcelable data format.
         * Action lambdas are intentionally omitted as they cannot be serialized.
         *
         * @param element Source AvidElement
         * @return Parcelable AvidElementData
         */
        fun fromAvidElement(element: AvidElement): AvidElementData {
            return AvidElementData(
                avid = element.avid,
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
         * @param avid Element AVID
         * @param name Element name
         * @param type Element type
         * @return AvidElementData with minimal information
         */
        fun minimal(avid: String, name: String? = null, type: String = "unknown"): AvidElementData {
            return AvidElementData(
                avid = avid,
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

// Backward compatibility alias
@Deprecated("Use AvidElementData instead", ReplaceWith("AvidElementData"))
typealias VuidElementData = AvidElementData
