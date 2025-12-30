package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.magicelements.components.phase3.layout.BottomSheet
import com.augmentalis.magicelements.components.phase3.layout.BottomSheetHeight
import com.augmentalis.magicelements.renderer.ios.bridge.*

object BottomSheetMapper {
    fun map(component: BottomSheet, renderChild: (Any) -> SwiftUIComponent): SwiftUIComponent {
        val children = component.content.map { renderChild(it) }

        val heightFraction = when (component.height) {
            BottomSheetHeight.Auto -> null
            BottomSheetHeight.Half -> 0.5
            BottomSheetHeight.Full -> 1.0
            BottomSheetHeight.FitContent -> null
        }

        return SwiftUIComponent(
            type = "Sheet",
            props = mapOf(
                "isPresented" to component.isOpen,
                "showDragIndicator" to component.showDragHandle,
                "detents" to if (heightFraction != null) listOf(heightFraction) else listOf("medium", "large"),
                "cornerRadius" to component.cornerRadius,
                "backgroundColor" to component.backgroundColor,
                "interactiveDismissDisabled" to !component.dismissible
            ),
            children = if (component.title != null) {
                listOf(
                    SwiftUIComponent(type = "Text", props = mapOf("content" to component.title, "font" to "headline"))
                ) + children
            } else children
        )
    }
}
