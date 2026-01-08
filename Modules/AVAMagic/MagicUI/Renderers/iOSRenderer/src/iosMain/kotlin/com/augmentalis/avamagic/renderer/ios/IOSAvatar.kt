package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.magicui.ui.core.display.AvatarComponent
import com.augmentalis.magicui.ui.core.display.AvatarShape

@OptIn(ExperimentalForeignApi::class)
class IOSAvatarRenderer {
    fun render(component: AvatarComponent): UIView {
        val size = getSizeValue(component.size)

        val avatarView = UIView().apply {
            frame = CGRectMake(0.0, 0.0, size, size)
            backgroundColor = UIColor.systemGray4Color

            // Shape
            when (component.shape) {
                AvatarShape.CIRCLE -> {
                    layer.cornerRadius = size / 2
                }
                AvatarShape.SQUARE -> {
                    layer.cornerRadius = 0.0
                }
                AvatarShape.ROUNDED -> {
                    layer.cornerRadius = size / 8
                }
            }
            layer.masksToBounds = true
        }

        when {
            // Image URL
            component.imageUrl != null -> {
                val imageView = UIImageView().apply {
                    frame = avatarView.bounds
                    contentMode = UIViewContentModeScaleAspectFill
                    // Load image from URL
                    // In production, use SDWebImage or similar
                }
                avatarView.addSubview(imageView)
            }

            // Initials
            component.initials != null -> {
                val label = UILabel().apply {
                    frame = avatarView.bounds
                    text = component.initials
                    textColor = UIColor.whiteColor
                    backgroundColor = UIColor.systemBlueColor
                    font = UIFont.boldSystemFontOfSize(size / 3)
                    textAlignment = NSTextAlignmentCenter
                }
                avatarView.addSubview(label)
            }

            // Icon
            component.icon != null -> {
                val iconImageView = UIImageView().apply {
                    frame = CGRectMake(size / 4, size / 4, size / 2, size / 2)
                    image = UIImage.systemImageNamed(component.icon)
                    tintColor = UIColor.systemGrayColor
                }
                avatarView.addSubview(iconImageView)
            }

            else -> {
                // Default person icon
                val iconImageView = UIImageView().apply {
                    frame = CGRectMake(size / 4, size / 4, size / 2, size / 2)
                    image = UIImage.systemImageNamed("person.fill")
                    tintColor = UIColor.systemGrayColor
                }
                avatarView.addSubview(iconImageView)
            }
        }

        // Accessibility
        avatarView.isAccessibilityElement = true
        avatarView.accessibilityLabel = component.initials ?: "Avatar"
        avatarView.accessibilityTraits = UIAccessibilityTraitImage

        return avatarView
    }

    private fun getSizeValue(size: com.augmentalis.avanues.avamagic.components.core.ComponentSize): Double {
        return when (size) {
            com.augmentalis.avanues.avamagic.components.core.ComponentSize.SM -> 32.0
            com.augmentalis.avanues.avamagic.components.core.ComponentSize.MD -> 48.0
            com.augmentalis.avanues.avamagic.components.core.ComponentSize.LG -> 64.0
        }
    }
}
