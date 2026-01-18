/**
 * TargetType.kt - Voice targeting types from legacy UIKitVoiceCommandSystem
 *
 * EXACT port of 7 targeting methods from working legacy implementation
 */

package com.augmentalis.avidcreator.models

/**
 * Voice target types - EXACT copy from legacy implementation
 *
 * Features:
 * - AVID: Direct AVID targeting
 * - NAME: Element name/label
 * - TYPE: Element type (button, text, etc.)
 * - POSITION: Spatial position (first, last, third)
 * - HIERARCHY: Parent/child navigation
 * - CONTEXT: Context-based targeting
 * - RECENT: Recently used elements
 */
enum class TargetType {
    AVID,           // Direct AVID targeting (formerly UUID)
    NAME,           // Element name/label
    TYPE,           // Element type (button, text, etc.)
    POSITION,       // Spatial position (first, last, third)
    HIERARCHY,      // Parent/child navigation
    CONTEXT,        // Context-based targeting
    RECENT          // Recently used elements
}
