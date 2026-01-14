/**
 * OverlayTypes.kt - Re-exports overlay types for the overlay package
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-08
 *
 * Provides type aliases and re-exports for overlay types from the features package,
 * making them accessible from the overlay package for consistency.
 */
@file:Suppress("unused")

package com.augmentalis.voiceoscoreng.overlay

// Import the original sealed class and its members
import com.augmentalis.voiceoscoreng.features.OverlayData as FeaturesOverlayData
import com.augmentalis.voiceoscoreng.features.CommandState as FeaturesCommandState
import com.augmentalis.voiceoscoreng.features.NumberedItem as FeaturesNumberedItem
import com.augmentalis.voiceoscoreng.features.MenuItem as FeaturesMenuItem
import com.augmentalis.voiceoscoreng.features.Rect as FeaturesRect

/**
 * Sealed class representing data that can be displayed in an overlay.
 *
 * Access nested types via:
 * - OverlayData.Status
 * - OverlayData.Confidence
 * - OverlayData.NumberedItems
 * - OverlayData.ContextMenu
 *
 * @see com.augmentalis.voiceoscoreng.features.OverlayData
 */
typealias OverlayData = FeaturesOverlayData

// Re-export sealed class members for direct access
// These enable usage like: Status(...) instead of OverlayData.Status(...)
typealias Status = FeaturesOverlayData.Status
typealias Confidence = FeaturesOverlayData.Confidence
typealias NumberedItems = FeaturesOverlayData.NumberedItems
typealias ContextMenu = FeaturesOverlayData.ContextMenu

/**
 * State of command execution in the voice processing pipeline.
 * @see com.augmentalis.voiceoscoreng.features.CommandState
 */
typealias CommandState = FeaturesCommandState

/**
 * Numbered item for selection overlay.
 * @see com.augmentalis.voiceoscoreng.features.NumberedItem
 */
typealias NumberedItem = FeaturesNumberedItem

/**
 * Menu item for context menu overlay.
 * @see com.augmentalis.voiceoscoreng.features.MenuItem
 */
typealias MenuItem = FeaturesMenuItem

/**
 * Rectangle bounds in screen coordinates.
 * @see com.augmentalis.voiceoscoreng.features.Rect
 */
typealias Rect = FeaturesRect
