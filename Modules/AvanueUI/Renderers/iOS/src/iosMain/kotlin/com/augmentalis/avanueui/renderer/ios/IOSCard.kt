package com.augmentalis.avanueui.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*

/**
 * iOS Renderer for Card Component
 *
 * Renders material design style cards with elevation, content, and actions.
 */
@OptIn(ExperimentalForeignApi::class)
class IOSCardRenderer {
    fun render(
        title: String? = null,
        subtitle: String? = null,
        content: String? = null,
        imageUrl: String? = null,
        actions: List<String> = emptyList()
    ): UIView {
        val cardView = UIView().apply {
            frame = CGRectMake(16.0, 0.0, 343.0, 300.0)
            backgroundColor = UIColor.systemBackgroundColor
            layer.cornerRadius = 12.0
            layer.masksToBounds = false
            layer.shadowColor = UIColor.blackColor.CGColor
            layer.shadowOpacity = 0.1f
            layer.shadowOffset = CGSizeMake(0.0, 2.0)
            layer.shadowRadius = 8.0
        }

        var yOffset = 16.0

        // Image (if provided)
        imageUrl?.let {
            val imageView = UIImageView().apply {
                frame = CGRectMake(0.0, 0.0, 343.0, 160.0)
                backgroundColor = UIColor.systemGray5Color
                contentMode = UIViewContentModeScaleAspectFill
                layer.cornerRadius = 12.0
                layer.maskedCorners = kCALayerMinXMinYCorner or kCALayerMaxXMinYCorner
                layer.masksToBounds = true
            }
            cardView.addSubview(imageView)
            yOffset = 176.0
        }

        // Title
        title?.let { titleText ->
            val titleLabel = UILabel().apply {
                frame = CGRectMake(16.0, yOffset, 311.0, 24.0)
                text = titleText
                font = UIFont.boldSystemFontOfSize(18.0)
                textColor = UIColor.labelColor
            }
            cardView.addSubview(titleLabel)
            yOffset += 32.0
        }

        // Subtitle
        subtitle?.let { subtitleText ->
            val subtitleLabel = UILabel().apply {
                frame = CGRectMake(16.0, yOffset, 311.0, 18.0)
                text = subtitleText
                font = UIFont.systemFontOfSize(14.0)
                textColor = UIColor.secondaryLabelColor
            }
            cardView.addSubview(subtitleLabel)
            yOffset += 26.0
        }

        // Content
        content?.let { contentText ->
            val contentLabel = UILabel().apply {
                frame = CGRectMake(16.0, yOffset, 311.0, 60.0)
                text = contentText
                font = UIFont.systemFontOfSize(14.0)
                textColor = UIColor.labelColor
                numberOfLines = 3
            }
            cardView.addSubview(contentLabel)
            yOffset += 76.0
        }

        // Actions
        if (actions.isNotEmpty()) {
            var xOffset = 16.0
            actions.forEach { actionLabel ->
                val button = UIButton.buttonWithType(UIButtonTypeSystem).apply {
                    frame = CGRectMake(xOffset, yOffset, 80.0, 32.0)
                    setTitle(actionLabel, UIControlStateNormal)
                }
                cardView.addSubview(button)
                xOffset += 88.0
            }
        }

        // Accessibility
        cardView.isAccessibilityElement = true
        cardView.accessibilityLabel = title ?: "Card"
        cardView.accessibilityTraits = UIAccessibilityTraitButton

        return cardView
    }
}
