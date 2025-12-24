package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.magicui.ui.core.feedback.ToastComponent
import com.augmentalis.magicui.components.core.Severity

@OptIn(ExperimentalForeignApi::class)
class IOSToastRenderer {
    fun render(component: ToastComponent): UIView {
        val screenWidth = UIScreen.mainScreen.bounds.useContents { size.width }
        val yPosition = when (component.position.toString()) {
            "BOTTOM" -> UIScreen.mainScreen.bounds.useContents { size.height } - 120.0
            "TOP" -> 70.0
            else -> UIScreen.mainScreen.bounds.useContents { size.height } - 120.0
        }

        return UIView().apply {
            frame = CGRectMake(32.0, yPosition, screenWidth - 64.0, 48.0)
            backgroundColor = getSeverityColor(component.severity)
            layer.cornerRadius = 24.0
            layer.masksToBounds = true
            layer.shadowColor = UIColor.blackColor.CGColor
            layer.shadowOpacity = 0.3f
            layer.shadowOffset = CGSizeMake(0.0, 2.0)
            layer.shadowRadius = 8.0

            // Icon
            val iconImageView = UIImageView().apply {
                frame = CGRectMake(16.0, 12.0, 24.0, 24.0)
                image = UIImage.systemImageNamed(getSeverityIcon(component.severity))
                tintColor = UIColor.whiteColor
            }
            addSubview(iconImageView)

            // Message label
            val label = UILabel().apply {
                frame = CGRectMake(48.0, 0.0, screenWidth - 128.0, 48.0)
                text = component.message
                textColor = UIColor.whiteColor
                font = UIFont.systemFontOfSize(14.0)
                numberOfLines = 2
            }
            addSubview(label)
        }
    }

    private fun getSeverityColor(severity: Severity): UIColor {
        return when (severity) {
            Severity.SUCCESS -> UIColor.systemGreenColor
            Severity.ERROR -> UIColor.systemRedColor
            Severity.WARNING -> UIColor.systemOrangeColor
            Severity.INFO -> UIColor.systemBlueColor
        }
    }

    private fun getSeverityIcon(severity: Severity): String {
        return when (severity) {
            Severity.SUCCESS -> "checkmark.circle.fill"
            Severity.ERROR -> "xmark.circle.fill"
            Severity.WARNING -> "exclamationmark.triangle.fill"
            Severity.INFO -> "info.circle.fill"
        }
    }
}
