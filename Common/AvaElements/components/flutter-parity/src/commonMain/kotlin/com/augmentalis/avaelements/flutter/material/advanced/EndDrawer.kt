package com.augmentalis.avaelements.flutter.material.advanced

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * EndDrawer component - Flutter Material parity
 *
 * A Material Design drawer that slides in from the trailing edge (right in LTR, left in RTL).
 * Typically used for secondary navigation or contextual actions.
 *
 * **Flutter Equivalent:** `endDrawer` property of `Scaffold`
 * **Material Design 3:** https://m3.material.io/components/navigation-drawer/overview
 *
 * ## Features
 * - Slides from trailing edge (RTL-aware)
 * - Modal overlay with scrim
 * - Swipe to open/close gestures
 * - Customizable width
 * - Material3 theming with elevation
 * - Dark mode support
 * - TalkBack accessibility with proper navigation semantics
 * - WCAG 2.1 Level AA compliant
 * - Keyboard navigation support (Escape to close)
 *
 * ## Usage Example
 * ```kotlin
 * Scaffold(
 *     endDrawer = EndDrawer(
 *         child = Column(
 *             children = listOf(
 *                 DrawerHeader("Settings"),
 *                 ListTile("Account", icon = "person"),
 *                 ListTile("Preferences", icon = "settings"),
 *                 Divider(),
 *                 ListTile("Logout", icon = "logout")
 *             )
 *         ),
 *         backgroundColor = "surface",
 *         width = 280f
 *     )
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property child Drawer content (typically Column with navigation items)
 * @property backgroundColor Background color of drawer
 * @property elevation Elevation (shadow depth) of drawer
 * @property shadowColor Color of shadow
 * @property surfaceTintColor Surface tint color for Material 3
 * @property shape Drawer shape (corner radius, etc.)
 * @property width Width of drawer in dp
 * @property semanticsLabel Accessibility label for drawer
 * @property clipBehavior How to clip drawer content
 * @property enableOpenDragGesture Whether to allow swipe-to-open
 * @property scrimColor Color of modal scrim overlay
 * @property drawerEdgeDragWidth Width of edge area that triggers swipe-to-open
 * @property contentDescription Accessibility description for TalkBack
 * @property onDrawerChanged Callback invoked when drawer opens/closes (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class EndDrawer(
    override val type: String = "EndDrawer",
    override val id: String? = null,
    val child: Component? = null,
    val backgroundColor: String? = null,
    val elevation: Float = 1f,
    val shadowColor: String? = null,
    val surfaceTintColor: String? = null,
    val shape: String? = null,
    val width: Float = DEFAULT_WIDTH,
    val semanticsLabel: String? = null,
    val clipBehavior: ClipBehavior = ClipBehavior.None,
    val enableOpenDragGesture: Boolean = true,
    val scrimColor: String? = null,
    val drawerEdgeDragWidth: Float = DEFAULT_EDGE_DRAG_WIDTH,
    val contentDescription: String? = null,
    @Transient
    val onDrawerChanged: ((Boolean) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        return contentDescription ?: semanticsLabel ?: "Navigation drawer"
    }

    /**
     * Get accessibility role
     */
    fun getAccessibilityRole(): String {
        return "navigation"
    }

    /**
     * Get drawer position based on text direction
     * @param isRTL Whether current layout is RTL
     * @return "end" (always trailing edge, which is RTL-aware)
     */
    fun getDrawerPosition(isRTL: Boolean): String {
        return "end" // Always trailing edge
    }

    /**
     * Get effective drawer width
     */
    fun getEffectiveWidth(): Float {
        return width.coerceIn(MIN_WIDTH, MAX_WIDTH)
    }

    /**
     * Check if swipe-to-open is enabled
     */
    fun isSwipeEnabled(): Boolean {
        return enableOpenDragGesture && drawerEdgeDragWidth > 0f
    }

    /**
     * Clip behavior for drawer content
     */
    enum class ClipBehavior {
        /** No clipping */
        None,

        /** Clip to bounds */
        HardEdge,

        /** Clip with anti-aliasing */
        AntiAlias,

        /** Clip with anti-aliasing and save layer */
        AntiAliasWithSaveLayer
    }

    companion object {
        /**
         * Default drawer width (in dp) - Material 3 spec
         */
        const val DEFAULT_WIDTH = 280f

        /**
         * Minimum drawer width (in dp)
         */
        const val MIN_WIDTH = 240f

        /**
         * Maximum drawer width (in dp)
         */
        const val MAX_WIDTH = 400f

        /**
         * Default edge drag width for swipe-to-open (in dp)
         */
        const val DEFAULT_EDGE_DRAG_WIDTH = 20f

        /**
         * Default elevation (in dp) - Material 3
         */
        const val DEFAULT_ELEVATION = 1f

        /**
         * Create a simple end drawer
         */
        fun simple(
            child: Component
        ) = EndDrawer(
            child = child
        )

        /**
         * Create an end drawer with custom width
         */
        fun withWidth(
            child: Component,
            width: Float
        ) = EndDrawer(
            child = child,
            width = width
        )

        /**
         * Create an end drawer with custom colors
         */
        fun withColors(
            child: Component,
            backgroundColor: String,
            scrimColor: String? = null
        ) = EndDrawer(
            child = child,
            backgroundColor = backgroundColor,
            scrimColor = scrimColor
        )

        /**
         * Create an end drawer with no swipe gesture
         */
        fun noSwipe(
            child: Component
        ) = EndDrawer(
            child = child,
            enableOpenDragGesture = false
        )

        /**
         * Create an end drawer with callback
         */
        fun withCallback(
            child: Component,
            onDrawerChanged: ((Boolean) -> Unit)? = null
        ) = EndDrawer(
            child = child,
            onDrawerChanged = onDrawerChanged
        )

        /**
         * Create a narrow end drawer (for icons/actions)
         */
        fun narrow(
            child: Component
        ) = EndDrawer(
            child = child,
            width = 240f
        )

        /**
         * Create a wide end drawer (for detailed content)
         */
        fun wide(
            child: Component
        ) = EndDrawer(
            child = child,
            width = 360f
        )

        /**
         * Create an end drawer with custom elevation
         */
        fun withElevation(
            child: Component,
            elevation: Float
        ) = EndDrawer(
            child = child,
            elevation = elevation
        )
    }
}
