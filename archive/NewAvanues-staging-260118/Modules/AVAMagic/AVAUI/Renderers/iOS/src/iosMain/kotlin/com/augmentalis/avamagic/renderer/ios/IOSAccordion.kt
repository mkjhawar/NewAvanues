package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*
import com.augmentalis.avamagic.ui.core.data.AccordionComponent
import com.augmentalis.avamagic.ui.core.data.AccordionItem

@OptIn(ExperimentalForeignApi::class)
class IOSAccordionRenderer {
    fun render(component: AccordionComponent): UIView {
        val scrollView = UIScrollView().apply {
            frame = CGRectMake(0.0, 0.0, 375.0, 600.0)
            backgroundColor = UIColor.systemBackgroundColor
        }

        var yOffset = 0.0

        component.items.forEachIndexed { index, item ->
            val isExpanded = index in component.expandedIndices

            // Header
            val headerView = createHeaderView(
                item = item,
                index = index,
                isExpanded = isExpanded,
                yOffset = yOffset,
                onToggle = { component.onToggle?.invoke(index) }
            )
            scrollView.addSubview(headerView)
            yOffset += 56.0

            // Content (only if expanded)
            if (isExpanded) {
                val contentView = createContentView(
                    item = item,
                    yOffset = yOffset
                )
                scrollView.addSubview(contentView)
                yOffset += 100.0 // Placeholder height
            }

            // Divider
            val divider = UIView().apply {
                frame = CGRectMake(0.0, yOffset, 375.0, 1.0)
                backgroundColor = UIColor.separatorColor
            }
            scrollView.addSubview(divider)
            yOffset += 1.0
        }

        scrollView.contentSize = CGSizeMake(375.0, yOffset)

        return scrollView
    }

    private fun createHeaderView(
        item: AccordionItem,
        index: Int,
        isExpanded: Boolean,
        yOffset: Double,
        onToggle: () -> Unit
    ): UIView {
        return UIView().apply {
            frame = CGRectMake(0.0, yOffset, 375.0, 56.0)
            backgroundColor = UIColor.systemBackgroundColor

            // Title
            val label = UILabel().apply {
                frame = CGRectMake(16.0, 0.0, 300.0, 56.0)
                text = item.title
                font = UIFont.boldSystemFontOfSize(16.0)
                textColor = UIColor.labelColor
            }
            addSubview(label)

            // Chevron icon
            val iconImageView = UIImageView().apply {
                frame = CGRectMake(343.0, 20.0, 16.0, 16.0)
                image = UIImage.systemImageNamed(
                    if (isExpanded) "chevron.up" else "chevron.down"
                )
                tintColor = UIColor.systemGrayColor
            }
            addSubview(iconImageView)

            // Tap gesture
            val tapGesture = UITapGestureRecognizer()
            addGestureRecognizer(tapGesture)
            isUserInteractionEnabled = true
            // Note: In production, connect to onToggle

            // Accessibility
            isAccessibilityElement = true
            accessibilityLabel = item.title
            accessibilityTraits = UIAccessibilityTraitButton
            accessibilityHint = if (isExpanded) "Collapse section" else "Expand section"
        }
    }

    private fun createContentView(item: AccordionItem, yOffset: Double): UIView {
        return UIView().apply {
            frame = CGRectMake(0.0, yOffset, 375.0, 100.0)
            backgroundColor = UIColor.systemGray6Color

            // Content placeholder
            val label = UILabel().apply {
                frame = CGRectMake(16.0, 16.0, 343.0, 68.0)
                text = "Content for ${item.title}"
                font = UIFont.systemFontOfSize(14.0)
                textColor = UIColor.secondaryLabelColor
                numberOfLines = 0
            }
            addSubview(label)
        }
    }
}
