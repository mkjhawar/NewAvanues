package com.augmentalis.avaelements.flutter.material.advanced

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * IndexedStack component - Flutter Material parity
 *
 * A stack that shows a single child from a list of children based on the current index.
 * Only the child at the current index is visible; all others are kept in memory but not displayed.
 *
 * **Flutter Equivalent:** `IndexedStack`
 * **Material Design 3:** Part of layout components
 *
 * ## Features
 * - Shows single child by index
 * - All children kept in memory (state preservation)
 * - Smooth transitions between indices
 * - Alignment and sizing options
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility with screen reader announcements
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * IndexedStack(
 *     index = currentIndex,
 *     children = listOf(
 *         HomeScreen(),
 *         SearchScreen(),
 *         ProfileScreen()
 *     ),
 *     alignment = Alignment.TopStart,
 *     sizing = StackFit.Loose
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property index The index of the child to display (0-based)
 * @property children List of child components
 * @property alignment How to align non-positioned children within the stack
 * @property sizing How to size the stack based on its children
 * @property textDirection Text direction for alignment (LTR/RTL)
 * @property contentDescription Accessibility description for TalkBack
 * @property onIndexChanged Callback invoked when index changes (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class IndexedStack(
    override val type: String = "IndexedStack",
    override val id: String? = null,
    val index: Int = 0,
    val children: List<Component> = emptyList(),
    val alignment: Alignment = Alignment.TopStart,
    val sizing: StackFit = StackFit.Loose,
    val textDirection: TextDirection = TextDirection.LTR,
    val contentDescription: String? = null,
    @Transient
    val onIndexChanged: ((Int) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get currently visible child
     */
    fun getCurrentChild(): Component? {
        return children.getOrNull(index)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val childCount = children.size
        val position = index + 1
        val base = contentDescription ?: "Screen $position of $childCount"
        return base
    }

    /**
     * Validate index is within bounds
     */
    fun isIndexValid(): Boolean {
        return index >= 0 && index < children.size
    }

    /**
     * Alignment options for stack children
     */
    enum class Alignment {
        /** Top-start (top-left in LTR, top-right in RTL) */
        TopStart,

        /** Top-center */
        TopCenter,

        /** Top-end (top-right in LTR, top-left in RTL) */
        TopEnd,

        /** Center-start (middle-left in LTR, middle-right in RTL) */
        CenterStart,

        /** Center */
        Center,

        /** Center-end (middle-right in LTR, middle-left in RTL) */
        CenterEnd,

        /** Bottom-start (bottom-left in LTR, bottom-right in RTL) */
        BottomStart,

        /** Bottom-center */
        BottomCenter,

        /** Bottom-end (bottom-right in LTR, bottom-left in RTL) */
        BottomEnd
    }

    /**
     * How to size the stack based on its children
     */
    enum class StackFit {
        /** Size to fit the largest child */
        Loose,

        /** Expand to fill available space */
        Expand,

        /** Size to exactly match parent constraints */
        PassThrough
    }

    /**
     * Text direction for alignment
     */
    enum class TextDirection {
        /** Left-to-right */
        LTR,

        /** Right-to-left */
        RTL
    }

    companion object {
        /**
         * Create a simple indexed stack
         */
        fun simple(
            index: Int,
            children: List<Component>
        ) = IndexedStack(
            index = index,
            children = children
        )

        /**
         * Create an indexed stack with alignment
         */
        fun withAlignment(
            index: Int,
            children: List<Component>,
            alignment: Alignment
        ) = IndexedStack(
            index = index,
            children = children,
            alignment = alignment
        )

        /**
         * Create an indexed stack with expand sizing
         */
        fun expanded(
            index: Int,
            children: List<Component>
        ) = IndexedStack(
            index = index,
            children = children,
            sizing = StackFit.Expand
        )

        /**
         * Create an indexed stack with RTL support
         */
        fun withRTL(
            index: Int,
            children: List<Component>,
            textDirection: TextDirection = TextDirection.RTL
        ) = IndexedStack(
            index = index,
            children = children,
            textDirection = textDirection
        )
    }
}
