/**
 * NavigationEdge.kt - Navigation edge data model
 * Path: libraries/AvidCreator/src/main/java/com/augmentalis/learnapp/models/NavigationEdge.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Data model for navigation graph edges (screen transitions)
 */

package com.augmentalis.voiceoscore.learnapp.models

/**
 * Navigation Edge
 *
 * Represents a navigation transition from one screen to another via an element click.
 *
 * ## Example
 *
 * ```
 * Screen A (hash: abc123)
 *   |
 *   | Click element UUID: btn-xyz789
 *   ↓
 * Screen B (hash: def456)
 * ```
 *
 * This would be represented as:
 * ```kotlin
 * NavigationEdge(
 *     fromScreenHash = "abc123",
 *     clickedElementUuid = "btn-xyz789",
 *     toScreenHash = "def456",
 *     timestamp = System.currentTimeMillis()
 * )
 * ```
 *
 * @property fromScreenHash Hash of source screen
 * @property clickedElementUuid UUID of clicked element
 * @property toScreenHash Hash of destination screen
 * @property timestamp When transition occurred
 *
 * @since 1.0.0
 */
data class NavigationEdge(
    val fromScreenHash: String,
    val clickedElementUuid: String,
    val toScreenHash: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun toString(): String {
        return "$fromScreenHash --[$clickedElementUuid]--> $toScreenHash"
    }
}
