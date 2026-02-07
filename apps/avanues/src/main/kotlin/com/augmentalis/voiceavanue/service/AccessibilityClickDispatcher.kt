package com.augmentalis.voiceavanue.service

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import com.augmentalis.voicecursor.core.ClickDispatcher

/**
 * Implements ClickDispatcher using AccessibilityService gesture dispatch.
 */
class AccessibilityClickDispatcher : ClickDispatcher {

    override fun dispatchClick(x: Int, y: Int) {
        val service = VoiceAvanueAccessibilityService.getInstance()
        if (service != null) {
            service.dispatchGesture(
                GestureDescription.Builder()
                    .addStroke(
                        GestureDescription.StrokeDescription(
                            Path().apply { moveTo(x.toFloat(), y.toFloat()) },
                            0L, 50L
                        )
                    ).build(),
                null, null
            )
        } else {
            Log.w("ClickDispatcher", "AccessibilityService not available")
        }
    }

    override fun dispatchLongPress(x: Int, y: Int) {
        val service = VoiceAvanueAccessibilityService.getInstance()
        if (service != null) {
            service.dispatchGesture(
                GestureDescription.Builder()
                    .addStroke(
                        GestureDescription.StrokeDescription(
                            Path().apply { moveTo(x.toFloat(), y.toFloat()) },
                            0L, 1000L
                        )
                    ).build(),
                null, null
            )
        }
    }
}
