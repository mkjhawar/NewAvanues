/**
 * InputHandler.kt - IHandler for keyboard show/hide commands
 *
 * Handles: show keyboard, hide keyboard
 * Uses AccessibilityService soft keyboard control.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.handlers

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

private const val TAG = "InputHandler"

class InputHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.INPUT

    override val supportedActions: List<String> = listOf(
        "show keyboard", "open keyboard", "keyboard",
        "hide keyboard", "close keyboard", "dismiss keyboard"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val phrase = command.phrase.lowercase().trim()
        Log.d(TAG, "InputHandler.execute: '$phrase'")

        return when {
            phrase in listOf("show keyboard", "open keyboard", "keyboard") -> showKeyboard()
            phrase in listOf("hide keyboard", "close keyboard", "dismiss keyboard") -> hideKeyboard()
            else -> HandlerResult.notHandled()
        }
    }

    private fun showKeyboard(): HandlerResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val success = service.softKeyboardController.showMode == AccessibilityService.SHOW_MODE_AUTO
                // Force show by setting to auto mode which respects focus
                service.softKeyboardController.setShowMode(AccessibilityService.SHOW_MODE_AUTO)

                // Also try focusing on the current input field to trigger keyboard
                val rootNode = service.rootInActiveWindow
                val focusedNode = rootNode?.findFocus(android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT)
                focusedNode?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS)

                HandlerResult.success("Keyboard shown")
            } catch (e: Exception) {
                Log.e(TAG, "Show keyboard failed", e)
                HandlerResult.failure("Cannot show keyboard: ${e.message}")
            }
        } else {
            HandlerResult.failure("Keyboard control requires Android 7+")
        }
    }

    private fun hideKeyboard(): HandlerResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                service.softKeyboardController.setShowMode(AccessibilityService.SHOW_MODE_HIDDEN)
                // Reset to auto after a brief delay so keyboard works normally next time
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        service.softKeyboardController.setShowMode(AccessibilityService.SHOW_MODE_AUTO)
                    } catch (_: Exception) {}
                }, 500)
                HandlerResult.success("Keyboard hidden")
            } catch (e: Exception) {
                Log.e(TAG, "Hide keyboard failed", e)
                HandlerResult.failure("Cannot hide keyboard: ${e.message}")
            }
        } else {
            HandlerResult.failure("Keyboard control requires Android 7+")
        }
    }
}
