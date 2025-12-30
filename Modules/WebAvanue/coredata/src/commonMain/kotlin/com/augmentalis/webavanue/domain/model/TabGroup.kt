package com.augmentalis.webavanue.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a tab group in the WebAvanue browser (Chrome-like).
 * Tab groups organize related tabs with a color indicator and optional title.
 */
@Serializable
data class TabGroup(
    val id: String,
    val title: String = "",
    val color: TabGroupColor,
    val isCollapsed: Boolean = false,
    val position: Int = 0,
    val createdAt: Instant
) {
    companion object {
        fun create(
            title: String = "",
            color: TabGroupColor = TabGroupColor.BLUE
        ): TabGroup {
            val now = kotlinx.datetime.Clock.System.now()
            return TabGroup(
                id = generateGroupId(),
                title = title,
                color = color,
                createdAt = now
            )
        }

        private fun generateGroupId(): String {
            return "group_${System.currentTimeMillis()}_${(0..9999).random()}"
        }
    }
}

/**
 * Chrome-like tab group colors
 */
@Serializable
enum class TabGroupColor(val colorHex: String) {
    BLUE("#3B82F6"),
    GREEN("#10B981"),
    YELLOW("#F59E0B"),
    RED("#EF4444"),
    PURPLE("#8B5CF6"),
    PINK("#EC4899"),
    CYAN("#06B6D4"),
    ORANGE("#F97316");

    companion object {
        fun fromString(value: String): TabGroupColor {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: BLUE
        }
    }
}
