/*
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 *
 * ContainerClassifier.kt - Classifies UI containers by persistence behavior
 *
 * Part of VoiceOSCore Hybrid Persistence System (Phase 1.3-1.4).
 * Provides nuanced classification of container types to determine
 * whether their children should be persisted, cached, or ignored.
 *
 * Author: Manoj Jhawar
 * Created: 2026-01-22
 * Related: Hybrid Persistence Plan, DynamicContentDetector.kt
 *
 * ## Classification Strategy:
 * 1. ALWAYS_DYNAMIC - Never persist children (data-driven containers)
 * 2. CONDITIONALLY_DYNAMIC - Depends on app context and content type
 * 3. STATIC - Usually safe to persist children (layout containers)
 *
 * @since 2.1.0 (Hybrid Persistence)
 */

package com.augmentalis.voiceoscore

/**
 * Container persistence behavior classification.
 *
 * Determines how the Hybrid Persistence system should treat
 * children of a container during command learning and storage.
 *
 * This classification is more nuanced than simple dynamic detection,
 * considering the container's role in the UI hierarchy.
 */
enum class ContainerBehavior {
    /**
     * Container children are always data-driven and should never be persisted.
     *
     * These containers display dynamic data from adapters, databases, or
     * network sources. Their children change based on data, not user actions.
     *
     * Examples:
     * - RecyclerView (list items from adapter)
     * - ListView (legacy list items)
     * - LazyColumn/LazyRow (Compose equivalents)
     * - ViewPager/ViewPager2 (pages from adapter)
     * - GridView (grid items from adapter)
     *
     * Persistence Strategy: Never persist individual children.
     * Instead, persist container-level commands like "scroll" or "item 3".
     */
    ALWAYS_DYNAMIC,

    /**
     * Container behavior depends on app context and content type.
     *
     * These containers can hold either:
     * - Static content (settings screen, about page) -> Safe to persist
     * - Dynamic content (feed, search results) -> Don't persist
     *
     * The determination requires additional context:
     * - Check if children have stable resource IDs
     * - Check if content changes across visits
     * - Check parent app/screen context
     *
     * Examples:
     * - ScrollView (static form vs. dynamic feed)
     * - NestedScrollView (detail page vs. feed)
     * - HorizontalScrollView (tab bar vs. carousel)
     *
     * Persistence Strategy: Use content fingerprinting to decide.
     * If content is stable across 3+ visits, consider persisting.
     */
    CONDITIONALLY_DYNAMIC,

    /**
     * Container children are typically static and safe to persist.
     *
     * These are layout containers that organize UI components.
     * Their children are defined in layouts, not data-driven.
     *
     * Examples:
     * - FrameLayout (single child container)
     * - LinearLayout (row/column arrangement)
     * - RelativeLayout (relative positioning)
     * - ConstraintLayout (constraint-based positioning)
     * - CoordinatorLayout (coordinated behaviors)
     * - CardView (card container)
     *
     * Persistence Strategy: Persist children normally.
     * Children usually have stable structure across app sessions.
     */
    STATIC
}

/**
 * Container Classifier - Classifies UI containers by persistence behavior
 *
 * Stateless classifier that examines container class names to determine
 * how the Hybrid Persistence system should treat their children.
 *
 * This provides a more nuanced approach than DynamicContentDetector's
 * binary dynamic/static classification, allowing for context-dependent
 * decisions in the CONDITIONALLY_DYNAMIC case.
 *
 * Usage:
 * ```kotlin
 * val behavior = ContainerClassifier.classifyContainer(element.className)
 * when (behavior) {
 *     ALWAYS_DYNAMIC -> skipChildPersistence()
 *     CONDITIONALLY_DYNAMIC -> checkContentFingerprint()
 *     STATIC -> persistChildren()
 * }
 * ```
 */
object ContainerClassifier {

    // ============================================================
    // ALWAYS_DYNAMIC containers - Never persist children
    // ============================================================

    /**
     * Class name patterns for data-driven containers.
     *
     * These containers use adapters or data sources to populate children.
     * Child elements are ephemeral and should not be persisted.
     */
    private val ALWAYS_DYNAMIC_PATTERNS = listOf(
        // Android RecyclerView family
        "RecyclerView",
        "androidx.recyclerview.widget.RecyclerView",

        // Legacy ListView family
        "ListView",
        "android.widget.ListView",
        "ExpandableListView",
        "android.widget.ExpandableListView",

        // GridView
        "GridView",
        "android.widget.GridView",

        // ViewPager family
        "ViewPager",
        "ViewPager2",
        "androidx.viewpager.widget.ViewPager",
        "androidx.viewpager2.widget.ViewPager2",

        // Jetpack Compose lazy containers
        "LazyColumn",
        "LazyRow",
        "LazyVerticalGrid",
        "LazyHorizontalGrid",
        "LazyVerticalStaggeredGrid",
        "LazyHorizontalStaggeredGrid",
        "Pager",
        "HorizontalPager",
        "VerticalPager",

        // Compose foundation
        "androidx.compose.foundation.lazy.LazyColumn",
        "androidx.compose.foundation.lazy.LazyRow",
        "androidx.compose.foundation.lazy.grid.LazyVerticalGrid",
        "androidx.compose.foundation.pager.HorizontalPager",

        // Third-party common patterns
        "RecyclerListView",  // React Native
        "FlatList",          // React Native
        "SectionList"        // React Native
    )

    // ============================================================
    // CONDITIONALLY_DYNAMIC containers - Depends on context
    // ============================================================

    /**
     * Class name patterns for scrollable containers that may be static or dynamic.
     *
     * These containers can hold either static layouts or dynamic content.
     * Additional context (content fingerprinting) is needed to decide.
     */
    private val CONDITIONALLY_DYNAMIC_PATTERNS = listOf(
        // Standard ScrollView
        "ScrollView",
        "android.widget.ScrollView",

        // Nested scroll support
        "NestedScrollView",
        "androidx.core.widget.NestedScrollView",

        // Horizontal scrolling
        "HorizontalScrollView",
        "android.widget.HorizontalScrollView",

        // Compose scrollable containers (non-lazy)
        "verticalScroll",
        "horizontalScroll",
        "androidx.compose.foundation.verticalScroll",
        "androidx.compose.foundation.horizontalScroll",

        // Material ScrollableTabRow
        "ScrollableTabRow",

        // Custom scroll containers (common naming patterns)
        "ScrollContainer",
        "ScrollableContainer"
    )

    // ============================================================
    // STATIC containers - Safe to persist children
    // ============================================================

    /**
     * Class name patterns for layout containers with static children.
     *
     * These containers organize UI elements defined in layouts.
     * Children are typically stable and safe to persist.
     */
    private val STATIC_PATTERNS = listOf(
        // FrameLayout family
        "FrameLayout",
        "android.widget.FrameLayout",

        // LinearLayout family
        "LinearLayout",
        "android.widget.LinearLayout",
        "AppCompatLinearLayout",

        // RelativeLayout
        "RelativeLayout",
        "android.widget.RelativeLayout",

        // ConstraintLayout
        "ConstraintLayout",
        "androidx.constraintlayout.widget.ConstraintLayout",
        "MotionLayout",
        "androidx.constraintlayout.motion.widget.MotionLayout",

        // CoordinatorLayout
        "CoordinatorLayout",
        "androidx.coordinatorlayout.widget.CoordinatorLayout",

        // CardView
        "CardView",
        "androidx.cardview.widget.CardView",
        "MaterialCardView",
        "com.google.android.material.card.MaterialCardView",

        // AppBarLayout family
        "AppBarLayout",
        "com.google.android.material.appbar.AppBarLayout",
        "CollapsingToolbarLayout",
        "Toolbar",

        // TableLayout
        "TableLayout",
        "android.widget.TableLayout",
        "TableRow",

        // Compose layout containers
        "Box",
        "Row",
        "Column",
        "Surface",
        "Card",
        "Scaffold",
        "androidx.compose.foundation.layout.Box",
        "androidx.compose.foundation.layout.Row",
        "androidx.compose.foundation.layout.Column",
        "androidx.compose.material.Surface",
        "androidx.compose.material3.Surface",

        // Material components
        "BottomNavigationView",
        "NavigationRailView",
        "NavigationView",
        "DrawerLayout",
        "BottomSheetDialog",

        // Common wrapper patterns
        "ViewGroup",
        "android.view.ViewGroup"
    )

    // ============================================================
    // Scrollable container patterns (for helper function)
    // ============================================================

    /**
     * All patterns that indicate a container can scroll.
     *
     * Combines ALWAYS_DYNAMIC and CONDITIONALLY_DYNAMIC patterns
     * since both categories involve scrollable behavior.
     */
    private val SCROLLABLE_PATTERNS = ALWAYS_DYNAMIC_PATTERNS + CONDITIONALLY_DYNAMIC_PATTERNS

    // ============================================================
    // Public API
    // ============================================================

    /**
     * Classify a container by its class name.
     *
     * Determines the persistence behavior for the container's children
     * based on the container's class name.
     *
     * @param className Full or simple class name of the container
     * @return ContainerBehavior classification
     *
     * @sample
     * ```kotlin
     * val behavior = ContainerClassifier.classifyContainer("RecyclerView")
     * // Returns: ALWAYS_DYNAMIC
     *
     * val behavior2 = ContainerClassifier.classifyContainer("ConstraintLayout")
     * // Returns: STATIC
     *
     * val behavior3 = ContainerClassifier.classifyContainer("ScrollView")
     * // Returns: CONDITIONALLY_DYNAMIC
     * ```
     */
    fun classifyContainer(className: String): ContainerBehavior {
        val normalizedName = className.trim()
        if (normalizedName.isEmpty()) {
            return ContainerBehavior.STATIC // Default to static for unknown
        }

        // Check ALWAYS_DYNAMIC first (most specific)
        if (matchesAnyPattern(normalizedName, ALWAYS_DYNAMIC_PATTERNS)) {
            return ContainerBehavior.ALWAYS_DYNAMIC
        }

        // Check CONDITIONALLY_DYNAMIC second
        if (matchesAnyPattern(normalizedName, CONDITIONALLY_DYNAMIC_PATTERNS)) {
            return ContainerBehavior.CONDITIONALLY_DYNAMIC
        }

        // Check STATIC patterns
        if (matchesAnyPattern(normalizedName, STATIC_PATTERNS)) {
            return ContainerBehavior.STATIC
        }

        // Default: treat unknown containers as STATIC
        // This is safer than assuming dynamic, as most custom containers
        // are layout wrappers that don't change their children
        return ContainerBehavior.STATIC
    }

    /**
     * Check if a container is scrollable.
     *
     * Returns true if the container can scroll its content, regardless
     * of whether it's always dynamic or conditionally dynamic.
     *
     * Useful for determining if scroll-based commands (scroll up/down)
     * should be generated for this container.
     *
     * @param className Full or simple class name of the container
     * @return true if the container supports scrolling
     *
     * @sample
     * ```kotlin
     * ContainerClassifier.isScrollableContainer("RecyclerView") // true
     * ContainerClassifier.isScrollableContainer("ScrollView") // true
     * ContainerClassifier.isScrollableContainer("ConstraintLayout") // false
     * ```
     */
    fun isScrollableContainer(className: String): Boolean {
        val normalizedName = className.trim()
        if (normalizedName.isEmpty()) {
            return false
        }

        return matchesAnyPattern(normalizedName, SCROLLABLE_PATTERNS)
    }

    /**
     * Get a human-readable description of the container behavior.
     *
     * Useful for logging and debugging purposes.
     *
     * @param behavior The container behavior
     * @return Human-readable description
     */
    fun descriptionFor(behavior: ContainerBehavior): String {
        return when (behavior) {
            ContainerBehavior.ALWAYS_DYNAMIC ->
                "Data-driven container - never persist children"
            ContainerBehavior.CONDITIONALLY_DYNAMIC ->
                "Scrollable container - check content fingerprint"
            ContainerBehavior.STATIC ->
                "Layout container - safe to persist children"
        }
    }

    /**
     * Get the recommended persistence strategy for a behavior.
     *
     * @param behavior The container behavior
     * @return Recommended action for the Hybrid Persistence system
     */
    fun persistenceStrategyFor(behavior: ContainerBehavior): String {
        return when (behavior) {
            ContainerBehavior.ALWAYS_DYNAMIC ->
                "Skip child persistence; persist container-level commands only"
            ContainerBehavior.CONDITIONALLY_DYNAMIC ->
                "Use content fingerprinting; persist if stable across visits"
            ContainerBehavior.STATIC ->
                "Persist children normally with standard fingerprinting"
        }
    }

    // ============================================================
    // Private helpers
    // ============================================================

    /**
     * Check if className matches any pattern in the list.
     *
     * Supports both exact matches and contains matching for flexibility.
     * Handles both simple class names ("RecyclerView") and fully qualified
     * names ("androidx.recyclerview.widget.RecyclerView").
     *
     * @param className The class name to check
     * @param patterns List of patterns to match against
     * @return true if any pattern matches
     */
    private fun matchesAnyPattern(className: String, patterns: List<String>): Boolean {
        // Extract simple name for contains matching
        val simpleName = className.substringAfterLast(".")

        return patterns.any { pattern ->
            // Exact match (case-insensitive)
            className.equals(pattern, ignoreCase = true) ||
            // Simple name exact match
            simpleName.equals(pattern, ignoreCase = true) ||
            // Contains match for partial patterns
            className.contains(pattern, ignoreCase = true) ||
            // Pattern ends with class name (fully qualified match)
            pattern.endsWith(simpleName, ignoreCase = true)
        }
    }
}
