/*
 * Copyright (c) 2025 Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * All rights reserved.
 *
 * VoiceOS - Voice-First Accessibility Platform
 * HierarchyModels - Data models for app hierarchy representation
 */

package com.augmentalis.voiceoscore.learnapp.hierarchy

/**
 * Represents the complete hierarchy of an application.
 * Contains all screens, their elements, and navigation edges.
 */
data class AppHierarchy(
    val packageName: String,
    val appName: String?,
    val nodes: List<HierarchyNode>,
    val edges: List<HierarchyEdge>,
    val rootScreenHash: String?,
    val stats: HierarchyStats,
    val generatedAt: Long
) {
    companion object {
        fun empty(packageName: String) = AppHierarchy(
            packageName = packageName,
            appName = null,
            nodes = emptyList(),
            edges = emptyList(),
            rootScreenHash = null,
            stats = HierarchyStats(0, 0, 0, 0, 0),
            generatedAt = System.currentTimeMillis()
        )
    }
}

/**
 * Represents a single screen/node in the app hierarchy.
 */
data class HierarchyNode(
    val screenHash: String,
    val activityName: String?,
    val windowTitle: String?,
    val screenType: String?,
    val navigationLevel: Int,
    val elements: List<HierarchyElement>,
    val visitCount: Int
)

/**
 * Represents a UI element within a screen.
 */
data class HierarchyElement(
    val uuid: String?,
    val elementHash: String,
    val className: String,
    val label: String?,
    val isClickable: Boolean,
    val isEditable: Boolean,
    val isScrollable: Boolean,
    val semanticRole: String?
)

/**
 * Represents a navigation edge between two screens.
 */
data class HierarchyEdge(
    val fromScreenHash: String,
    val toScreenHash: String,
    val triggerElementHash: String?,
    val triggerAction: String,
    val transitionCount: Int
)

/**
 * Statistics about the app hierarchy.
 */
data class HierarchyStats(
    val totalScreens: Int,
    val totalElements: Int,
    val totalEdges: Int,
    val maxDepth: Int,
    val clickableElements: Int
)
