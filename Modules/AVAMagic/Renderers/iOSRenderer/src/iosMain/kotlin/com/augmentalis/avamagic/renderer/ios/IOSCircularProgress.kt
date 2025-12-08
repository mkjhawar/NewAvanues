package com.augmentalis.avamagic.renderer.ios

import platform.UIKit.*
import platform.Foundation.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
class IOSCircularProgressRenderer {
    fun render(value: Float = 0f, max: Float = 100f, indeterminate: Boolean = false): UIView {
        if (indeterminate) {
            // Use UIActivityIndicatorView for indeterminate
            return UIActivityIndicatorView(UIActivityIndicatorViewStyleMedium).apply {
                frame = CGRectMake(0.0, 0.0, 40.0, 40.0)
                startAnimating()
                color = UIColor.systemBlueColor
            }
        } else {
            // Custom circular progress view
            val progressView = UIView().apply {
                frame = CGRectMake(0.0, 0.0, 60.0, 60.0)
                backgroundColor = UIColor.clearColor
            }

            // Background circle
            val backgroundLayer = CAShapeLayer().apply {
                val center = CGPointMake(30.0, 30.0)
                val radius = 25.0
                val path = UIBezierPath.bezierPathWithArcCenter(
                    center = center,
                    radius = radius,
                    startAngle = 0.0,
                    endAngle = 6.28319, // 2 * PI
                    clockwise = true
                ).CGPath

                this.path = path
                strokeColor = UIColor.systemGray5Color.CGColor
                fillColor = UIColor.clearColor.CGColor
                lineWidth = 5.0
            }
            progressView.layer.addSublayer(backgroundLayer)

            // Progress circle
            val progressLayer = CAShapeLayer().apply {
                val center = CGPointMake(30.0, 30.0)
                val radius = 25.0
                val startAngle = -1.5708 // -PI/2 (top of circle)
                val endAngle = startAngle + (6.28319 * (value / max))
                val path = UIBezierPath.bezierPathWithArcCenter(
                    center = center,
                    radius = radius,
                    startAngle = startAngle,
                    endAngle = endAngle,
                    clockwise = true
                ).CGPath

                this.path = path
                strokeColor = UIColor.systemBlueColor.CGColor
                fillColor = UIColor.clearColor.CGColor
                lineWidth = 5.0
                lineCap = kCALineCapRound
            }
            progressView.layer.addSublayer(progressLayer)

            return progressView
        }
    }
}
