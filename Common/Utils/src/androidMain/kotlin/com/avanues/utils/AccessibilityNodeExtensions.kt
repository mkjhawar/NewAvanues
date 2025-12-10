/**
 * AccessibilityNodeExtensions.kt - Safe lifecycle management for AccessibilityNodeInfo
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-13
 *
 * CRITICAL: AccessibilityNodeInfo instances MUST be recycled after use to prevent memory leaks.
 * These extension functions ensure proper lifecycle management using Kotlin's resource pattern.
 *
 * Design Pattern: Resource Acquisition Is Initialization (RAII)
 * - Similar to Kotlin's `use()` for Closeable resources
 * - Automatic cleanup via finally blocks
 * - Exception-safe (recycles even on error)
 * - Zero runtime overhead (inline functions)
 *
 * Usage Examples:
 *
 * Example 1: Basic node access
 * ```
 * rootNode.useNode { root ->
 *     val text = root.text.toString()
 *     root.performAction(ACTION_CLICK)
 * }
 * // root is automatically recycled here
 * ```
 *
 * Example 2: Nullable variant
 * ```
 * service.rootInActiveWindow.useNodeOrNull { root ->
 *     processNode(root)
 * } ?: return CommandResult.failure("No root")
 * ```
 *
 * Example 3: Child iteration
 * ```
 * parentNode.forEachChild { child ->
 *     if (child.isClickable) {
 *         child.performAction(ACTION_CLICK)
 *     }
 * }
 * // All children automatically recycled
 * ```
 *
 * Example 4: Indexed child access
 * ```
 * parentNode.useChild(0) { firstChild ->
 *     firstChild.text.toString()
 * }
 * // firstChild automatically recycled
 * ```
 *
 * Benefits:
 * - Prevents memory leaks (automatic recycling)
 * - Exception-safe (finally blocks)
 * - Compile-time safety (type system enforces usage)
 * - Self-documenting (clear lifecycle scope)
 * - Zero overhead (inline functions compile away)
 * - Hard to misuse (compiler prevents most mistakes)
 *
 * References:
 * - Kotlin stdlib use(): https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/use.html
 * - Android AccessibilityNodeInfo: https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo
 */
package com.avanues.utils

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Execute block with this node and automatically recycle when done
 *
 * Returns null if node is null, otherwise returns block result.
 * Guarantees node is recycled even if block throws exception.
 *
 * @param T Return type of the block
 * @param block Lambda to execute with the node
 * @return Result of block, or null if node is null
 *
 * Example:
 * ```
 * val text = node.useNode { n ->
 *     n.text?.toString() ?: ""
 * }
 * ```
 */
inline fun <T> AccessibilityNodeInfo?.useNode(block: (AccessibilityNodeInfo) -> T): T? {
    if (this == null) return null
    return try {
        block(this)
    } finally {
        // CRITICAL: Always recycle, even on exception
        this.recycle()
    }
}

/**
 * Execute block with this node and automatically recycle when done
 *
 * Similar to useNode, but allows block to return null.
 * Useful for chaining operations that might fail.
 *
 * @param T Return type of the block (nullable)
 * @param block Lambda to execute with the node
 * @return Result of block, or null if node is null or block returns null
 *
 * Example:
 * ```
 * val result = service.rootInActiveWindow.useNodeOrNull { root ->
 *     findElement(root)
 * } ?: return CommandResult.failure("Not found")
 * ```
 */
inline fun <T> AccessibilityNodeInfo?.useNodeOrNull(block: (AccessibilityNodeInfo) -> T?): T? {
    if (this == null) return null
    return try {
        block(this)
    } finally {
        // CRITICAL: Always recycle, even on exception
        this.recycle()
    }
}

/**
 * Safely get child at index and execute block
 *
 * Automatically recycles child when done.
 * Returns null if child doesn't exist or is null.
 *
 * @param T Return type of the block
 * @param index Zero-based index of child to access
 * @param block Lambda to execute with the child node
 * @return Result of block, or null if child doesn't exist
 *
 * Example:
 * ```
 * val firstChildText = parent.useChild(0) { child ->
 *     child.text?.toString()
 * }
 * ```
 */
inline fun <T> AccessibilityNodeInfo.useChild(index: Int, block: (AccessibilityNodeInfo) -> T): T? {
    return getChild(index).useNode(block)
}

/**
 * Iterate over all children safely, automatically recycling each child
 *
 * IMPORTANT: Does NOT recycle the parent node (caller's responsibility).
 * Each child is recycled after the block executes.
 *
 * Exception-safe: If block throws for a child, that child is still recycled
 * before exception propagates.
 *
 * @param block Lambda to execute for each child node
 *
 * Example:
 * ```
 * parentNode.forEachChild { child ->
 *     if (child.isClickable) {
 *         ConditionalLogger.d(TAG) { "Clickable: ${child.text}" }
 *     }
 * }
 * // All children automatically recycled
 * ```
 *
 * Performance Note:
 * - Inline function = zero overhead
 * - Equivalent to manual loop with try-finally
 * - No object allocation
 */
inline fun AccessibilityNodeInfo.forEachChild(block: (AccessibilityNodeInfo) -> Unit) {
    for (i in 0 until childCount) {
        getChild(i)?.let { child ->
            try {
                block(child)
            } finally {
                // CRITICAL: Always recycle child, even on exception
                child.recycle()
            }
        }
    }
}

/**
 * Iterate over all children safely with index, automatically recycling each child
 *
 * Similar to forEachChild, but provides index to the block.
 * Useful when you need to know child position.
 *
 * @param block Lambda to execute for each child (receives index and child)
 *
 * Example:
 * ```
 * parentNode.forEachChildIndexed { index, child ->
 *     ConditionalLogger.d(TAG) { "Child $index: ${child.text}" }
 * }
 * ```
 */
inline fun AccessibilityNodeInfo.forEachChildIndexed(block: (Int, AccessibilityNodeInfo) -> Unit) {
    for (i in 0 until childCount) {
        getChild(i)?.let { child ->
            try {
                block(i, child)
            } finally {
                // CRITICAL: Always recycle child, even on exception
                child.recycle()
            }
        }
    }
}

/**
 * Map over children safely, collecting results and recycling nodes
 *
 * Transforms each child using the block and collects results into a list.
 * All children are recycled automatically.
 *
 * @param T Return type for each child
 * @param block Lambda to transform each child
 * @return List of transformed results (empty if no children)
 *
 * Example:
 * ```
 * val childTexts = parent.mapChildren { child ->
 *     child.text?.toString() ?: ""
 * }
 * ```
 */
inline fun <T> AccessibilityNodeInfo.mapChildren(block: (AccessibilityNodeInfo) -> T): List<T> {
    val results = mutableListOf<T>()
    forEachChild { child ->
        results.add(block(child))
    }
    return results
}

/**
 * Find first child matching predicate, automatically recycling non-matches
 *
 * IMPORTANT: The returned node is NOT recycled - caller must recycle it
 * or use it with useNode { }.
 *
 * All non-matching children are recycled automatically.
 *
 * @param predicate Lambda to test each child
 * @return First matching child, or null if none match
 *
 * Example:
 * ```
 * val clickableChild = parent.findChild { it.isClickable }
 * clickableChild.useNode { child ->
 *     child.performAction(ACTION_CLICK)
 * }
 * ```
 */
inline fun AccessibilityNodeInfo.findChild(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
    for (i in 0 until childCount) {
        val child = getChild(i) ?: continue
        try {
            if (predicate(child)) {
                return child // Caller must recycle
            }
        } finally {
            // If not returned, recycle
            if (!predicate(child)) {
                child.recycle()
            }
        }
    }
    return null
}
