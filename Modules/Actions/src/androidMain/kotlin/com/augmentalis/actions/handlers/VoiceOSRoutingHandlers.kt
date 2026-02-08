package com.augmentalis.actions.handlers

import android.content.Context
import android.util.Log
import com.augmentalis.actions.ActionResult
import com.augmentalis.actions.IntentActionHandler
import com.augmentalis.actions.VoiceOSConnection

/**
 * Generic VoiceOS routing handler for accessibility commands.
 *
 * This class generates handlers for intents that must be executed
 * by VoiceOS accessibility service.
 */
abstract class VoiceOSRoutingHandler(
    override val intent: String,
    private val category: String
) : IntentActionHandler {

    companion object {
        private const val TAG = "VoiceOSRouter"
    }

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Routing $intent to VoiceOS (category: $category)")

            val voiceOS = VoiceOSConnection.getInstance(context)
            val result = voiceOS.executeCommand(intent, category)

            when (result) {
                is VoiceOSConnection.CommandResult.Success -> {
                    ActionResult.Success(message = result.message)
                }
                is VoiceOSConnection.CommandResult.Failure -> {
                    ActionResult.Failure(message = result.error)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to route $intent to VoiceOS", e)
            ActionResult.Failure(
                message = "Failed to execute $intent: ${e.message}",
                exception = e
            )
        }
    }
}

// ========================================
// Cursor Control Handlers
// ========================================

class CenterCursorActionHandler : VoiceOSRoutingHandler("center_cursor", "cursor")
class ShowCursorActionHandler : VoiceOSRoutingHandler("show_cursor", "cursor")
class HideCursorActionHandler : VoiceOSRoutingHandler("hide_cursor", "cursor")
class HandCursorActionHandler : VoiceOSRoutingHandler("hand_cursor", "cursor")
class NormalCursorActionHandler : VoiceOSRoutingHandler("normal_cursor", "cursor")
class ChangeCursorActionHandler : VoiceOSRoutingHandler("change_cursor", "cursor")

// ========================================
// Gesture Handlers
// ========================================

class SwipeUpActionHandler : VoiceOSRoutingHandler("swipe_up", "gesture")
class SwipeDownActionHandler : VoiceOSRoutingHandler("swipe_down", "gesture")
class SwipeLeftActionHandler : VoiceOSRoutingHandler("swipe_left", "gesture")
class SwipeRightActionHandler : VoiceOSRoutingHandler("swipe_right", "gesture")
class PinchOpenActionHandler : VoiceOSRoutingHandler("pinch_open", "gesture")
class PinchCloseActionHandler : VoiceOSRoutingHandler("pinch_close", "gesture")

// ========================================
// Scroll Handlers
// ========================================

class ScrollUpActionHandler : VoiceOSRoutingHandler("scroll_up", "scroll")
class ScrollDownActionHandler : VoiceOSRoutingHandler("scroll_down", "scroll")

// ========================================
// Click Handlers
// ========================================

class SingleClickActionHandler : VoiceOSRoutingHandler("single_click", "gesture")
class DoubleClickActionHandler : VoiceOSRoutingHandler("double_click", "gesture")
class LongPressActionHandler : VoiceOSRoutingHandler("long_press", "gesture")
class SelectActionHandler : VoiceOSRoutingHandler("select", "editing")

// ========================================
// Keyboard Handlers
// ========================================

class BackspaceActionHandler : VoiceOSRoutingHandler("backspace", "keyboard")
class EnterActionHandler : VoiceOSRoutingHandler("enter", "keyboard")
class ClearTextActionHandler : VoiceOSRoutingHandler("clear_text", "keyboard")
class OpenKeyboardActionHandler : VoiceOSRoutingHandler("open_keyboard", "keyboard")
class CloseKeyboardActionHandler : VoiceOSRoutingHandler("close_keyboard", "keyboard")
class HideKeyboardActionHandler : VoiceOSRoutingHandler("hide_keyboard", "keyboard")
class DismissKeyboardActionHandler : VoiceOSRoutingHandler("dismiss_keyboard", "keyboard")
class ChangeKeyboardActionHandler : VoiceOSRoutingHandler("change_keyboard", "keyboard")
class SwitchKeyboardActionHandler : VoiceOSRoutingHandler("switch_keyboard", "keyboard")
class KeyboardModeActionHandler : VoiceOSRoutingHandler("keyboard_mode", "keyboard")
class KeyboardModeChangeActionHandler : VoiceOSRoutingHandler("keyboard_mode_change", "keyboard")
class KeyboardChangeActionHandler : VoiceOSRoutingHandler("keyboard_change", "keyboard")

// ========================================
// Drag Handlers
// ========================================

class DragStartActionHandler : VoiceOSRoutingHandler("drag_start", "drag")
class DragStopActionHandler : VoiceOSRoutingHandler("drag_stop", "drag")
class DragUpDownActionHandler : VoiceOSRoutingHandler("drag_up_down", "drag")

// ========================================
// Dictation Handlers
// ========================================

class DictationActionHandler : VoiceOSRoutingHandler("dictation", "dictation")
class EndDictationActionHandler : VoiceOSRoutingHandler("end_dictation", "dictation")

// ========================================
// UI Control Handlers
// ========================================

class ShowNumberActionHandler : VoiceOSRoutingHandler("show_number", "overlays")
class HideNumberActionHandler : VoiceOSRoutingHandler("hide_number", "overlays")
class ShowHelpActionHandler : VoiceOSRoutingHandler("show_help", "overlays")
class HideHelpActionHandler : VoiceOSRoutingHandler("hide_help", "overlays")
class ShowCommandActionHandler : VoiceOSRoutingHandler("show_command", "overlays")
class HideCommandActionHandler : VoiceOSRoutingHandler("hide_command", "overlays")
class ScanCommandsActionHandler : VoiceOSRoutingHandler("scan_commands", "overlays")

// ========================================
// Gaze Handlers
// ========================================

class GazeOnActionHandler : VoiceOSRoutingHandler("gaze_on", "gaze")
class GazeOffActionHandler : VoiceOSRoutingHandler("gaze_off", "gaze")

// ========================================
// System Handlers
// ========================================

class ShutDownActionHandler : VoiceOSRoutingHandler("shut_down", "system")
class RebootActionHandler : VoiceOSRoutingHandler("reboot", "system")
class TurnOffDisplayActionHandler : VoiceOSRoutingHandler("turn_off_display", "system")

// ========================================
// Confirmation Handlers
// ========================================

class ConfirmActionHandler : VoiceOSRoutingHandler("confirm", "dialog")
class CancelActionHandler : VoiceOSRoutingHandler("cancel", "dialog")
class CloseActionHandler : VoiceOSRoutingHandler("close", "dialog")
class SubmitActionHandler : VoiceOSRoutingHandler("submit", "dialog")

// ========================================
// Volume Level Handlers
// ========================================

class IncreaseVolumeActionHandler : VoiceOSRoutingHandler("increase_volume", "volume")
class DecreaseVolumeActionHandler : VoiceOSRoutingHandler("decrease_volume", "volume")
class MuteVolumeActionHandler : VoiceOSRoutingHandler("mute_volume", "volume")
class SetVolumeMaxActionHandler : VoiceOSRoutingHandler("set_volume_max", "volume")
class SetVolume1ActionHandler : VoiceOSRoutingHandler("set_volume_1", "volume")
class SetVolume2ActionHandler : VoiceOSRoutingHandler("set_volume_2", "volume")
class SetVolume3ActionHandler : VoiceOSRoutingHandler("set_volume_3", "volume")
class SetVolume4ActionHandler : VoiceOSRoutingHandler("set_volume_4", "volume")
class SetVolume5ActionHandler : VoiceOSRoutingHandler("set_volume_5", "volume")
class SetVolume6ActionHandler : VoiceOSRoutingHandler("set_volume_6", "volume")
class SetVolume7ActionHandler : VoiceOSRoutingHandler("set_volume_7", "volume")
class SetVolume8ActionHandler : VoiceOSRoutingHandler("set_volume_8", "volume")
class SetVolume9ActionHandler : VoiceOSRoutingHandler("set_volume_9", "volume")
class SetVolume10ActionHandler : VoiceOSRoutingHandler("set_volume_10", "volume")
class SetVolume11ActionHandler : VoiceOSRoutingHandler("set_volume_11", "volume")
class SetVolume12ActionHandler : VoiceOSRoutingHandler("set_volume_12", "volume")
class SetVolume13ActionHandler : VoiceOSRoutingHandler("set_volume_13", "volume")
class SetVolume14ActionHandler : VoiceOSRoutingHandler("set_volume_14", "volume")
class SetVolume15ActionHandler : VoiceOSRoutingHandler("set_volume_15", "volume")

// ========================================
// Alias Handlers (map to existing handlers)
// ========================================

class TurnOnWifiActionHandler : IntentActionHandler {
    override val intent = "turn_on_wifi"
    override suspend fun execute(context: Context, utterance: String) =
        WifiOnActionHandler().execute(context, utterance)
}

class TurnOffWifiActionHandler : IntentActionHandler {
    override val intent = "turn_off_wifi"
    override suspend fun execute(context: Context, utterance: String) =
        WifiOffActionHandler().execute(context, utterance)
}

class TurnOnBluetoothActionHandler : IntentActionHandler {
    override val intent = "turn_on_bluetooth"
    override suspend fun execute(context: Context, utterance: String) =
        BluetoothOnActionHandler().execute(context, utterance)
}

class TurnOffBluetoothActionHandler : IntentActionHandler {
    override val intent = "turn_off_bluetooth"
    override suspend fun execute(context: Context, utterance: String) =
        BluetoothOffActionHandler().execute(context, utterance)
}
