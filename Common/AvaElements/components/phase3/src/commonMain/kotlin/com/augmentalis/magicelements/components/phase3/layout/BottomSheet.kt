package com.augmentalis.magicelements.components.phase3.layout

import kotlinx.serialization.Serializable
import com.augmentalis.magicelements.core.types.Component

@Serializable
data class BottomSheet(
    val isOpen: Boolean = false,
    val title: String? = null,
    val showDragHandle: Boolean = true,
    val dismissible: Boolean = true,
    val height: BottomSheetHeight = BottomSheetHeight.Auto,
    val backgroundColor: String? = null,
    val cornerRadius: Float = 16f,
    val content: List<Component> = emptyList(),
    val onDismiss: String? = null
) : Component

@Serializable
enum class BottomSheetHeight {
    Auto, Half, Full, FitContent
}
