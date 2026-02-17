/**
 * ElementClassification.kt - KMP-portable element classification utilities
 *
 * Extracted from app-layer ElementExtractor.kt to enable cross-platform reuse.
 * Contains: container type classification, element deduplication model,
 * and element hash computation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceoscore

import com.augmentalis.foundation.util.HashUtils

/**
 * Information about a duplicate element found during extraction.
 * Used to track elements with identical hash signatures across a single scrape pass.
 */
data class DuplicateInfo(
    val hash: String,
    val element: ElementInfo,
    val firstSeenIndex: Int
)

/**
 * Known dynamic container class names.
 * These are scrollable/recycling containers whose child count should NOT be included
 * in structural signatures (because it varies with scroll position).
 */
val DYNAMIC_CONTAINER_TYPES: Set<String> = setOf(
    "RecyclerView", "ListView", "GridView",
    "ViewPager", "ViewPager2",
    "ScrollView", "HorizontalScrollView", "NestedScrollView",
    "LazyColumn", "LazyRow", "LazyVerticalGrid", "LazyHorizontalGrid"
)

/**
 * Check if a class name represents a dynamic container (RecyclerView, ListView, etc.).
 *
 * Matches against the simple class name suffix, so both
 * "androidx.recyclerview.widget.RecyclerView" and "RecyclerView" match.
 *
 * @param className Fully-qualified or simple class name to check
 * @return true if the class is a known dynamic container type
 */
fun isDynamicContainer(className: String): Boolean {
    val simpleName = className.substringAfterLast(".")
    return DYNAMIC_CONTAINER_TYPES.any { simpleName.contains(it, ignoreCase = true) }
}

/**
 * Calculate a deduplication hash for an element.
 *
 * Uses className + resourceId + text to produce a 16-char hex hash.
 * Two elements with identical class, ID, and text will produce the same hash,
 * which is used to detect duplicates within a single scrape pass.
 *
 * @param element The element to hash
 * @return 16-character hex hash string
 */
fun calculateElementHash(element: ElementInfo): String {
    val hashInput = "${element.className}|${element.resourceId}|${element.text}"
    return HashUtils.calculateHash(hashInput).take(16)
}
