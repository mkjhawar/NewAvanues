package com.augmentalis.magicelements.components.phase3.navigation

import kotlinx.serialization.Serializable
import com.augmentalis.magicelements.core.types.Component

@Serializable
data class FloatingMenu(
    val isOpen: Boolean = false,
    val items: List<FloatingMenuItem> = emptyList(),
    val mainIcon: String = "plus",
    val mainIconOpen: String = "xmark",
    val position: FloatingMenuPosition = FloatingMenuPosition.BottomRight,
    val spacing: Float = 16f,
    val backgroundColor: String? = null,
    val iconColor: String? = null,
    val onToggle: String? = null
) : Component

@Serializable
data class FloatingMenuItem(
    val icon: String,
    val label: String? = null,
    val backgroundColor: String? = null,
    val onTap: String? = null
)

@Serializable
enum class FloatingMenuPosition {
    BottomRight, BottomLeft, BottomCenter, TopRight, TopLeft
}
