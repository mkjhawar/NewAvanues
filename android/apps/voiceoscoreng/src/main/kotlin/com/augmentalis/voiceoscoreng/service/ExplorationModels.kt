package com.augmentalis.voiceoscoreng.service

import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.DisplayCommand

/**
 * Data models for accessibility exploration results.
 *
 * Extracted from VoiceOSAccessibilityService.kt for SOLID compliance.
 * Single Responsibility: Define exploration data structures only.
 */

/**
 * Complete result of exploring an accessibility tree.
 */
data class ExplorationResult(
    val packageName: String,
    val timestamp: Long,
    val duration: Long,
    val totalElements: Int,
    val clickableElements: Int,
    val scrollableElements: Int,
    val elements: List<ElementInfo>,
    val vuids: List<VUIDInfo>,
    val hierarchy: List<HierarchyNode>,
    val duplicates: List<DuplicateInfo>,
    val deduplicationStats: DeduplicationStats,
    val commands: List<DisplayCommand>,
    val avuOutput: String,
    val elementLabels: Map<Int, String> = emptyMap()
)

/**
 * VUID information for a single element.
 */
data class VUIDInfo(
    val element: ElementInfo,
    val vuid: String,
    val hash: String
)

/**
 * Node in the element hierarchy tree.
 */
data class HierarchyNode(
    val index: Int,
    val depth: Int,
    val parentIndex: Int?,
    val childCount: Int,
    val className: String
)

/**
 * Information about a duplicate element found during deduplication.
 */
data class DuplicateInfo(
    val hash: String,
    val element: ElementInfo,
    val firstSeenIndex: Int
)

/**
 * Statistics from deduplication process.
 */
data class DeduplicationStats(
    val totalHashes: Int,
    val uniqueHashes: Int,
    val duplicateCount: Int,
    val duplicateElements: List<DuplicateInfo>
)

// DisplayCommand is in VoiceOSCore module (com.augmentalis.voiceoscore.DisplayCommand)
