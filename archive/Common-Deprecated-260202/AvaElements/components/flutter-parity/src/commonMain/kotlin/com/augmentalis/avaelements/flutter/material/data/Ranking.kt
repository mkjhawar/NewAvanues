package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Ranking component - Flutter Material parity
 *
 * A position/rank indicator for showing an entity's standing or placement.
 * Used within leaderboards, search results, or any ranked list.
 *
 * **Flutter Equivalent:** Custom implementation
 * **Material Design 3:** https://m3.material.io/components/badges/overview
 *
 * ## Features
 * - Numeric rank display
 * - Special styling for top positions (1st, 2nd, 3rd)
 * - Medal/badge support
 * - Change indicator (rank up/down)
 * - Compact and expanded modes
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * Ranking(
 *     position = 1,
 *     change = +3,
 *     showBadge = true,
 *     size = Size.Large
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property position Current rank/position (1-based)
 * @property change Optional change from previous rank (positive = moved up)
 * @property showBadge Whether to show badge for top positions
 * @property size Display size
 * @property label Optional label text (e.g., "Rank")
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class Ranking(
    override val type: String = "Ranking",
    override val id: String? = null,
    val position: Int,
    val change: Int? = null,
    val showBadge: Boolean = false,
    val size: Size = Size.Medium,
    val label: String? = null,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Display size options
     */
    enum class Size {
        /** Small badge-style display */
        Small,

        /** Medium standard display */
        Medium,

        /** Large prominent display */
        Large
    }

    /**
     * Get badge type for top positions
     */
    fun getBadgeType(): BadgeType? {
        return when {
            !showBadge -> null
            position == 1 -> BadgeType.Gold
            position == 2 -> BadgeType.Silver
            position == 3 -> BadgeType.Bronze
            else -> null
        }
    }

    /**
     * Get ordinal suffix (1st, 2nd, 3rd, etc.)
     */
    fun getOrdinal(): String {
        val suffix = when {
            position % 100 in 11..13 -> "th"
            position % 10 == 1 -> "st"
            position % 10 == 2 -> "nd"
            position % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$position$suffix"
    }

    /**
     * Get change direction
     */
    fun getChangeDirection(): ChangeDirection? {
        return when {
            change == null -> null
            change > 0 -> ChangeDirection.Up
            change < 0 -> ChangeDirection.Down
            else -> ChangeDirection.Same
        }
    }

    /**
     * Badge type for top positions
     */
    enum class BadgeType {
        Gold,
        Silver,
        Bronze
    }

    /**
     * Change direction indicator
     */
    enum class ChangeDirection {
        Up,
        Down,
        Same
    }

    companion object {
        /**
         * Create a first place ranking with gold badge
         */
        fun first() = Ranking(
            position = 1,
            showBadge = true,
            size = Size.Large
        )

        /**
         * Create a ranking with change indicator
         */
        fun withChange(
            position: Int,
            change: Int
        ) = Ranking(
            position = position,
            change = change
        )
    }
}
