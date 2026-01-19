/**
 * TargetTypeModel.kt - Voice targeting types
 *
 * Cross-platform KMP enum for targeting methods.
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
package com.augmentalis.avidcreator

/**
 * Voice target types for element targeting
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
