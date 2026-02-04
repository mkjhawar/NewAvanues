package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Leaderboard component - Flutter Material parity
 *
 * A ranked list display for showing competitive standings, scores, or rankings.
 * Commonly used in gaming, fitness, and competitive applications.
 *
 * **Flutter Equivalent:** Custom implementation
 * **Material Design 3:** https://m3.material.io/components/lists/overview
 *
 * ## Features
 * - Ranked item display with position indicators
 * - Avatar/image support
 * - Badge/medal support for top positions
 * - Score/metric display
 * - Highlight current user
 * - Click handler support
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * Leaderboard(
 *     title = "Top Players",
 *     items = listOf(
 *         LeaderboardItem(
 *             id = "user1",
 *             rank = 1,
 *             name = "Alice Johnson",
 *             score = "9,850",
 *             avatar = "avatar1.png",
 *             badge = "gold"
 *         ),
 *         LeaderboardItem(
 *             id = "user2",
 *             rank = 2,
 *             name = "Bob Smith",
 *             score = "8,720",
 *             badge = "silver"
 *         )
 *     ),
 *     currentUserId = "user3"
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property title Optional title for the leaderboard
 * @property items List of leaderboard entries
 * @property currentUserId Optional ID of current user to highlight
 * @property showTopBadges Whether to show badges for top 3
 * @property maxItems Maximum number of items to display
 * @property contentDescription Accessibility description for TalkBack
 * @property onItemClick Callback invoked when item is clicked
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class Leaderboard(
    override val type: String = "Leaderboard",
    override val id: String? = null,
    val title: String? = null,
    val items: List<LeaderboardItem> = emptyList(),
    val currentUserId: String? = null,
    val showTopBadges: Boolean = true,
    val maxItems: Int? = null,
    val contentDescription: String? = null,
    @Transient
    val onItemClick: ((String) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Single leaderboard entry
     */
    data class LeaderboardItem(
        val id: String,
        val rank: Int,
        val name: String,
        val score: String,
        val avatar: String? = null,
        val badge: String? = null,
        val subtitle: String? = null
    )

    /**
     * Get items to display (respecting maxItems)
     */
    fun getDisplayItems(): List<LeaderboardItem> {
        return maxItems?.let { items.take(it) } ?: items
    }

    /**
     * Check if item is current user
     */
    fun isCurrentUser(item: LeaderboardItem): Boolean {
        return currentUserId != null && item.id == currentUserId
    }

    companion object {
        /**
         * Create a top 10 leaderboard
         */
        fun top10(
            title: String? = null,
            items: List<LeaderboardItem>
        ) = Leaderboard(
            title = title,
            items = items,
            maxItems = 10
        )

        /**
         * Create a leaderboard with highlighted user
         */
        fun withCurrentUser(
            title: String? = null,
            items: List<LeaderboardItem>,
            currentUserId: String
        ) = Leaderboard(
            title = title,
            items = items,
            currentUserId = currentUserId
        )
    }
}
