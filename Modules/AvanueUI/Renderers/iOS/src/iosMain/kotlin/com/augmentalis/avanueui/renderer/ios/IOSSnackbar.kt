package com.augmentalis.avanueui.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.avanueui.ui.core.feedback.SnackbarComponent

@OptIn(ExperimentalForeignApi::class)
class IOSSnackbarRenderer {
    fun render(component: SnackbarComponent): UIView {
        val screenWidth = UIScreen.mainScreen.bounds.useContents { size.width }
        val yPosition = when (component.position.toString()) {
            "BOTTOM" -> UIScreen.mainScreen.bounds.useContents { size.height } - 100.0
            "TOP" -> 50.0
            else -> UIScreen.mainScreen.bounds.useContents { size.height } - 100.0
        }

        return UIView().apply {
            frame = CGRectMake(16.0, yPosition, screenWidth - 32.0, 56.0)
            backgroundColor = UIColor.darkGrayColor
            layer.cornerRadius = 8.0
            layer.masksToBounds = true

            // Message label
            val label = UILabel().apply {
                frame = CGRectMake(16.0, 0.0, screenWidth - 96.0, 56.0)
                text = component.message
                textColor = UIColor.whiteColor
                font = UIFont.systemFontOfSize(14.0)
                numberOfLines = 2
            }
            addSubview(label)

            // Action button if provided
            component.actionLabel?.let { actionText ->
                val button = UIButton.buttonWithType(UIButtonTypeSystem).apply {
                    frame = CGRectMake(screenWidth - 80.0, 12.0, 64.0, 32.0)
                    setTitle(actionText, UIControlStateNormal)
                    setTitleColor(UIColor.systemBlueColor, UIControlStateNormal)
                }
                addSubview(button)
            }

            // Auto-dismiss after duration
            // Note: Use NSTimer or dispatch_after in production
        }
    }
}
