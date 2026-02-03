package com.augmentalis.avaelements.flutter.material.display

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * AvatarGroup component - Flutter Material parity
 *
 * A Material Design 3 component for displaying multiple overlapping avatars.
 * Commonly used to show group members, collaborators, or participants.
 *
 * **Web Equivalent:** `AvatarGroup` (MUI)
 * **Material Design 3:** https://m3.material.io/components/avatars/overview
 *
 * ## Features
 * - Overlapping avatar display
 * - Configurable maximum visible count
 * - "+N more" indicator for overflow
 * - Left-to-right or right-to-left stacking
 * - Customizable avatar size and spacing
 * - Click to view all members
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * AvatarGroup(
 *     avatars = listOf(
 *         Avatar("user1", "John Doe", "https://example.com/avatar1.jpg"),
 *         Avatar("user2", "Jane Smith", "https://example.com/avatar2.jpg"),
 *         Avatar("user3", "Bob Johnson", null, "BJ"),
 *         Avatar("user4", "Alice Williams", "https://example.com/avatar4.jpg")
 *     ),
 *     max = 3,
 *     size = 40f,
 *     spacing = -8f,
 *     onPressed = {
 *         // Show all members
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property avatars List of avatar data
 * @property max Maximum number of avatars to display (remaining shown as "+N")
 * @property size Avatar size in dp
 * @property spacing Spacing between avatars (negative for overlap)
 * @property direction Stacking direction (left-to-right or right-to-left)
 * @property showTooltip Whether to show tooltip with names on hover
 * @property borderColor Border color for each avatar
 * @property borderWidth Border width in dp
 * @property contentDescription Accessibility description for TalkBack
 * @property onPressed Callback invoked when group is pressed (not serialized)
 * @property onAvatarPressed Callback invoked when individual avatar is pressed (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class AvatarGroup(
    override val type: String = "AvatarGroup",
    override val id: String? = null,
    val avatars: List<Avatar>,
    val max: Int = 5,
    val size: Float = 40f,
    val spacing: Float = -8f,
    val direction: Direction = Direction.LeftToRight,
    val showTooltip: Boolean = true,
    val borderColor: String? = null,
    val borderWidth: Float = 2f,
    val contentDescription: String? = null,
    @Transient
    val onPressed: (() -> Unit)? = null,
    @Transient
    val onAvatarPressed: ((String) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val base = contentDescription ?: "Avatar group"
        val count = avatars.size
        val visible = minOf(count, max)
        val remaining = maxOf(0, count - max)
        val info = if (remaining > 0) {
            "$visible of $count members, $remaining more"
        } else {
            "$count members"
        }
        return "$base, $info"
    }

    /**
     * Get visible avatars (up to max)
     */
    fun getVisibleAvatars(): List<Avatar> {
        return avatars.take(max)
    }

    /**
     * Get count of remaining avatars
     */
    fun getRemainingCount(): Int {
        return maxOf(0, avatars.size - max)
    }

    /**
     * Avatar data
     */
    data class Avatar(
        val id: String,
        val name: String,
        val imageUrl: String? = null,
        val initials: String? = null,
        val backgroundColor: String? = null
    )

    /**
     * Stacking direction
     */
    enum class Direction {
        /** Avatars stacked left to right */
        LeftToRight,

        /** Avatars stacked right to left */
        RightToLeft
    }

    companion object {
        /**
         * Create a simple avatar group
         */
        fun simple(
            avatars: List<Avatar>,
            max: Int = 5,
            onPressed: (() -> Unit)? = null
        ) = AvatarGroup(
            avatars = avatars,
            max = max,
            onPressed = onPressed
        )

        /**
         * Create an avatar group from user IDs and names
         */
        fun fromUsers(
            users: List<Pair<String, String>>,
            max: Int = 5,
            onPressed: (() -> Unit)? = null
        ) = AvatarGroup(
            avatars = users.map { (id, name) ->
                Avatar(
                    id = id,
                    name = name,
                    initials = name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("")
                )
            },
            max = max,
            onPressed = onPressed
        )
    }
}
