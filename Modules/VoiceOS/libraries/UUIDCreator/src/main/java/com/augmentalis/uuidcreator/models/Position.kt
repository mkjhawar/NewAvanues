/**
 * Position.kt - Spatial position from legacy UIKitVoiceCommandSystem
 * Path: libraries/UUIDManager/src/main/java/com/ai/uuidmgr/models/Position.kt
 * 
 * Extracted from: /VOS4/apps/VoiceUI/migration/legacy-backup/uikit/voice/UIKitVoiceCommandSystem.kt
 * Lines: 87-94
 * 
 * EXACT port of Position data class from working legacy implementation
 */

package com.augmentalis.uuidcreator.models

/**
 * Spatial position - EXACT copy from legacy implementation
 * 
 * @param x X coordinate
 * @param y Y coordinate
 * @param z Z coordinate (default 0)
 * @param index Element index in list
 * @param row Row position in grid
 * @param column Column position in grid
 */
data class Position(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val index: Int = 0,
    val row: Int = 0,
    val column: Int = 0
)