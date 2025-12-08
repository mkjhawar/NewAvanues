package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.avanues.avamagic.ui.core.layout.DividerComponent
import com.augmentalis.avanues.avamagic.components.core.Orientation

@OptIn(ExperimentalForeignApi::class)
class IOSDividerRenderer {
    fun render(component: DividerComponent): UIView {
        val thickness = component.thickness.toDouble()
        val indent = component.indent.toDouble()

        val dividerView = when (component.orientation) {
            Orientation.Horizontal -> {
                UIView().apply {
                    frame = CGRectMake(indent, 0.0, 375.0 - (indent * 2), thickness)
                }
            }
            Orientation.Vertical -> {
                UIView().apply {
                    frame = CGRectMake(0.0, indent, thickness, 375.0 - (indent * 2))
                }
            }
        }

        dividerView.apply {
            backgroundColor = parseColor(component.color).withAlphaComponent(0.3)

            // Accessibility
            isAccessibilityElement = false // Dividers are decorative
        }

        return dividerView
    }

    private fun parseColor(color: com.augmentalis.avanues.avamagic.components.core.Color): UIColor {
        return when (color) {
            com.augmentalis.avanues.avamagic.components.core.Color.Blue -> UIColor.systemBlueColor
            com.augmentalis.avanues.avamagic.components.core.Color.Green -> UIColor.systemGreenColor
            com.augmentalis.avanues.avamagic.components.core.Color.Red -> UIColor.systemRedColor
            com.augmentalis.avanues.avamagic.components.core.Color.Orange -> UIColor.systemOrangeColor
            com.augmentalis.avanues.avamagic.components.core.Color.Purple -> UIColor.systemPurpleColor
            com.augmentalis.avanues.avamagic.components.core.Color.Gray -> UIColor.systemGrayColor
            com.augmentalis.avanues.avamagic.components.core.Color.Black -> UIColor.blackColor
            com.augmentalis.avanues.avamagic.components.core.Color.White -> UIColor.whiteColor
        }
    }
}
