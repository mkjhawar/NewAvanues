package com.augmentalis.cockpit.mvp

/**
 * Workspace display modes
 *
 * Supports three parallel workspace implementations:
 * 1. FLAT - Traditional 2D grid workspace
 * 2. SPATIAL - 3D curved spatial projection
 * 3. CURVED - ViewPager2 with curved bitmap previews (real content rendering)
 */
enum class WorkspaceMode {
    /**
     * Flat 2D workspace - Traditional grid layout
     * Uses WorkspaceView with horizontal/vertical arrangements
     */
    FLAT,

    /**
     * Spatial 3D workspace - Curved arc projection
     * Uses SpatialWorkspaceView with pseudo-spatial rendering
     */
    SPATIAL,

    /**
     * Curved ViewPager workspace - Real content with curved previews
     * Uses CurvedWorkspaceView with ViewPager2 and bitmap transformation
     * Renders actual WebView content, captures snapshots, applies curve
     */
    CURVED;

    /**
     * Get display name for UI
     */
    fun getDisplayName(): String = when (this) {
        FLAT -> "Cockpit Workspace"
        SPATIAL -> "Spatial Workspace"
        CURVED -> "Curved Workspace"
    }

    /**
     * Cycle to next mode
     */
    fun next(): WorkspaceMode = when (this) {
        FLAT -> SPATIAL
        SPATIAL -> CURVED
        CURVED -> FLAT
    }

    /**
     * Cycle to previous mode
     */
    fun previous(): WorkspaceMode = when (this) {
        FLAT -> CURVED
        SPATIAL -> FLAT
        CURVED -> SPATIAL
    }
}
