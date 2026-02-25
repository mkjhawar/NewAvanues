package com.augmentalis.avanueui.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*

/**
 * iOS Renderer for Popover Component
 *
 * Renders a popover using UIPopoverPresentationController.
 */
@OptIn(ExperimentalForeignApi::class)
class IOSPopoverRenderer {
    fun render(
        content: String,
        sourceView: UIView? = null,
        sourceRect: CValue<CGRect>? = null,
        permittedArrowDirections: UIPopoverArrowDirection = UIPopoverArrowDirectionAny
    ): UIViewController {
        // Create content view controller
        val contentViewController = UIViewController().apply {
            preferredContentSize = CGSizeMake(280.0, 200.0)

            view.backgroundColor = UIColor.systemBackgroundColor

            // Content label
            val label = UILabel().apply {
                frame = CGRectMake(16.0, 16.0, 248.0, 168.0)
                text = content
                font = UIFont.systemFontOfSize(14.0)
                textColor = UIColor.labelColor
                numberOfLines = 0
            }
            view.addSubview(label)
        }

        // Configure popover presentation
        contentViewController.modalPresentationStyle = UIModalPresentationPopover

        contentViewController.popoverPresentationController?.apply {
            // Source view and rect would be set by caller
            this.permittedArrowDirections = permittedArrowDirections
            backgroundColor = UIColor.systemBackgroundColor
        }

        return contentViewController
    }

    fun show(
        popoverViewController: UIViewController,
        from sourceViewController: UIViewController,
        sourceView: UIView,
        sourceRect: CValue<CGRect>
    ) {
        popoverViewController.popoverPresentationController?.apply {
            this.sourceView = sourceView
            this.sourceRect = sourceRect
        }

        sourceViewController.presentViewController(
            popoverViewController,
            animated = true,
            completion = null
        )
    }
}
