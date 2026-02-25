package com.augmentalis.avanueui.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.avanueui.ui.core.feedback.ProgressBarComponent

@OptIn(ExperimentalForeignApi::class)
class IOSProgressBarRenderer {
    fun render(component: ProgressBarComponent): UIProgressView {
        return UIProgressView().apply {
            frame = CGRectMake(0.0, 0.0, 300.0, 4.0)
            progressViewStyle = UIProgressViewStyleDefault

            if (component.indeterminate) {
                // iOS doesn't have built-in indeterminate progress view
                // Would use UIActivityIndicatorView instead
                progress = 0.0f
            } else {
                progress = component.value / component.max
            }

            // Tint color based on component color
            progressTintColor = parseColor(component.color)
            trackTintColor = UIColor.systemGray5Color
        }
    }

    private fun parseColor(color: com.augmentalis.avanueui.core.Color): UIColor {
        return when (color) {
            com.augmentalis.avanueui.core.Color.Blue -> UIColor.systemBlueColor
            com.augmentalis.avanueui.core.Color.Green -> UIColor.systemGreenColor
            com.augmentalis.avanueui.core.Color.Red -> UIColor.systemRedColor
            com.augmentalis.avanueui.core.Color.Orange -> UIColor.systemOrangeColor
            com.augmentalis.avanueui.core.Color.Purple -> UIColor.systemPurpleColor
            com.augmentalis.avanueui.core.Color.Gray -> UIColor.systemGrayColor
        }
    }
}
