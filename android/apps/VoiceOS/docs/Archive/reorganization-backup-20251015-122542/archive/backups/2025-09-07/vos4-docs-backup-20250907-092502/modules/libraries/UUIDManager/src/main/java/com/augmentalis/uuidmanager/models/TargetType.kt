/**
 * TargetType.kt - Voice targeting types from legacy UIKitVoiceCommandSystem
 * Path: libraries/UUIDManager/src/main/java/com/ai/uuidmgr/models/TargetType.kt
 * 
 * Extracted from: /VOS4/apps/VoiceUI/migration/legacy-backup/uikit/voice/UIKitVoiceCommandSystem.kt
 * Lines: 50-58
 * 
 * EXACT port of 7 targeting methods from working legacy implementation
 */

package com.augmentalis.uuidmanager.models

/**
 * Voice target types - EXACT copy from legacy implementation
 * 
 * Features:
 * - UUID: Direct UUID targeting
 * - NAME: Element name/label
 * - TYPE: Element type (button, text, etc.)
 * - POSITION: Spatial position (first, last, third)
 * - HIERARCHY: Parent/child navigation
 * - CONTEXT: Context-based targeting
 * - RECENT: Recently used elements
 */
enum class TargetType {
    UUID,           // Direct UUID targeting
    NAME,           // Element name/label
    TYPE,           // Element type (button, text, etc.)
    POSITION,       // Spatial position (first, last, third)
    HIERARCHY,      // Parent/child navigation
    CONTEXT,        // Context-based targeting
    RECENT          // Recently used elements
}