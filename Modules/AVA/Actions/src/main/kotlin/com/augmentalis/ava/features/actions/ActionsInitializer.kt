package com.augmentalis.ava.features.actions

import android.content.Context
import android.util.Log
import com.augmentalis.ava.features.actions.handlers.*

/**
 * Initializer for registering action handlers.
 *
 * Call initialize() early in app lifecycle (e.g., Application.onCreate or before
 * ChatViewModel is used) to register all action handlers.
 *
 * Design:
 * - Singleton pattern for easy access
 * - Thread-safe initialization
 * - Idempotent: safe to call multiple times
 *
 * Usage:
 * ```
 * // In Application.onCreate or MainActivity.onCreate
 * ActionsInitializer.initialize(context)
 * ```
 *
 * Future: Could be extended to support dynamic handler registration via plugins.
 */
object ActionsInitializer {

    private const val TAG = "ActionsInitializer"

    @Volatile
    private var isInitialized = false

    /**
     * Initialize and register all action handlers.
     *
     * This method is idempotent - safe to call multiple times.
     * Only the first call will actually register handlers.
     *
     * @param context Application context (unused currently, but kept for future use)
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "Actions already initialized, skipping")
            return
        }

        synchronized(this) {
            if (isInitialized) {
                return
            }

            Log.d(TAG, "Initializing action handlers...")
            val startTime = System.currentTimeMillis()

            // ADR-014 Phase B (C7): Register all built-in action handlers with error handling
            // Wrap registration in try-catch to prevent crashes from individual handler failures
            try {
                IntentActionHandlerRegistry.registerAll(
                // Core handlers
                TimeActionHandler(),
                AlarmActionHandler(),
                WeatherActionHandler(),
                CalculationActionHandler(),

                // Communication handlers (P0 - Week 1)
                SendTextActionHandler(),
                MakeCallActionHandler(),
                // Communication handlers (P1 - Week 2)
                SendEmailActionHandler(),

                // Productivity handlers (P0 - Week 1)
                SearchWebActionHandler(),
                NavigateURLActionHandler(),
                // Productivity handlers (P2 - Week 3)
                CreateReminderActionHandler(),
                CreateCalendarEventActionHandler(),
                AddTodoActionHandler(),
                CreateNoteActionHandler(),
                // Productivity handlers (P3 - Week 4)
                CheckCalendarActionHandler(),

                // Navigation handlers (P1 - Week 2)
                GetDirectionsActionHandler(),
                FindNearbyActionHandler(),
                // Navigation handlers (P3 - Week 4)
                ShowTrafficActionHandler(),
                ShareLocationActionHandler(),
                SaveLocationActionHandler(),

                // Media handlers (P1 - Week 2)
                PlayVideoActionHandler(),
                // Media handlers (P2 - Week 3)
                ResumeMusicActionHandler(),

                // Device Control handlers (P2 - Week 3)
                SetTimerActionHandler(),

                // App and settings handlers
                OpenSettingsActionHandler(),
                OpenAppActionHandler(),
                OpenSecurityActionHandler(),
                OpenConnectionActionHandler(),
                OpenSoundActionHandler(),
                OpenDisplayActionHandler(),
                OpenAboutActionHandler(),
                QuickSettingsActionHandler(),

                // System control handlers
                BluetoothOnActionHandler(),
                BluetoothOffActionHandler(),
                WifiOnActionHandler(),
                WifiOffActionHandler(),
                VolumeUpActionHandler(),
                VolumeDownActionHandler(),
                VolumeMuteActionHandler(),
                MuteActionHandler(),
                VolumeUnmuteActionHandler(),
                BatteryStatusActionHandler(),
                FlashlightOnActionHandler(),
                FlashlightOffActionHandler(),
                AirplaneModeOnActionHandler(),
                AirplaneModeOffActionHandler(),
                BrightnessUpActionHandler(),
                BrightnessDownActionHandler(),
                LockScreenActionHandler(),
                ScreenshotActionHandler(),

                // Media control handlers
                PlayMusicActionHandler(),
                PauseMusicActionHandler(),
                NextTrackActionHandler(),
                PreviousTrackActionHandler(),
                ShuffleOnActionHandler(),
                RepeatModeActionHandler(),

                // Navigation handlers
                GoHomeActionHandler(),
                NavigateHomeActionHandler(),
                GoBackActionHandler(),
                BackActionHandler(),
                RecentAppsActionHandler(),
                OpenRecentAppsActionHandler(),
                NotificationsActionHandler(),
                ShowNotificationsActionHandler(),
                HideNotificationsActionHandler(),
                OpenBrowserActionHandler(),
                MenuActionHandler(),
                ReturnToDashboardActionHandler(),

                // VoiceOS routing handlers - Cursor
                CenterCursorActionHandler(),
                ShowCursorActionHandler(),
                HideCursorActionHandler(),
                HandCursorActionHandler(),
                NormalCursorActionHandler(),
                ChangeCursorActionHandler(),

                // VoiceOS routing handlers - Gestures
                SwipeUpActionHandler(),
                SwipeDownActionHandler(),
                SwipeLeftActionHandler(),
                SwipeRightActionHandler(),
                PinchOpenActionHandler(),
                PinchCloseActionHandler(),

                // VoiceOS routing handlers - Scroll
                ScrollUpActionHandler(),
                ScrollDownActionHandler(),

                // VoiceOS routing handlers - Clicks
                SingleClickActionHandler(),
                DoubleClickActionHandler(),
                LongPressActionHandler(),
                SelectActionHandler(),

                // VoiceOS routing handlers - Keyboard
                BackspaceActionHandler(),
                EnterActionHandler(),
                ClearTextActionHandler(),
                OpenKeyboardActionHandler(),
                CloseKeyboardActionHandler(),
                HideKeyboardActionHandler(),
                DismissKeyboardActionHandler(),
                ChangeKeyboardActionHandler(),
                SwitchKeyboardActionHandler(),
                KeyboardModeActionHandler(),
                KeyboardModeChangeActionHandler(),
                KeyboardChangeActionHandler(),

                // VoiceOS routing handlers - Drag
                DragStartActionHandler(),
                DragStopActionHandler(),
                DragUpDownActionHandler(),

                // VoiceOS routing handlers - Dictation
                DictationActionHandler(),
                EndDictationActionHandler(),

                // VoiceOS routing handlers - UI Controls
                ShowNumberActionHandler(),
                HideNumberActionHandler(),
                ShowHelpActionHandler(),
                HideHelpActionHandler(),
                ShowCommandActionHandler(),
                HideCommandActionHandler(),
                ScanCommandsActionHandler(),

                // VoiceOS routing handlers - Gaze
                GazeOnActionHandler(),
                GazeOffActionHandler(),

                // VoiceOS routing handlers - System
                ShutDownActionHandler(),
                RebootActionHandler(),
                TurnOffDisplayActionHandler(),

                // VoiceOS routing handlers - Confirmation
                ConfirmActionHandler(),
                CancelActionHandler(),
                CloseActionHandler(),
                SubmitActionHandler(),

                // VoiceOS routing handlers - Volume Levels
                IncreaseVolumeActionHandler(),
                DecreaseVolumeActionHandler(),
                MuteVolumeActionHandler(),
                SetVolumeMaxActionHandler(),
                SetVolume1ActionHandler(),
                SetVolume2ActionHandler(),
                SetVolume3ActionHandler(),
                SetVolume4ActionHandler(),
                SetVolume5ActionHandler(),
                SetVolume6ActionHandler(),
                SetVolume7ActionHandler(),
                SetVolume8ActionHandler(),
                SetVolume9ActionHandler(),
                SetVolume10ActionHandler(),
                SetVolume11ActionHandler(),
                SetVolume12ActionHandler(),
                SetVolume13ActionHandler(),
                SetVolume14ActionHandler(),
                SetVolume15ActionHandler(),

                // VoiceOS Expanded Handlers (Phase 3)
                CopyActionHandler(),
                PasteActionHandler(),
                CutActionHandler(),
                SelectAllActionHandler(),
                AppSearchActionHandler(),
                ScreenDescribeActionHandler(),
                ScreenFindActionHandler(),
                MediaCastActionHandler(),

                // Alias handlers
                TurnOnWifiActionHandler(),
                TurnOffWifiActionHandler(),
                TurnOnBluetoothActionHandler(),
                TurnOffBluetoothActionHandler()
                )

                val initTime = System.currentTimeMillis() - startTime
                val registeredIntents = IntentActionHandlerRegistry.getRegisteredIntents()

                Log.i(TAG, "Action handlers initialized in ${initTime}ms")
                Log.i(TAG, "Registered ${registeredIntents.size} handlers: $registeredIntents")

                isInitialized = true
            } catch (e: Exception) {
                Log.e(TAG, "Handler registration failed: ${e.message}", e)
                // Still mark as initialized to prevent infinite retry loops
                // App will continue with whatever handlers were registered before the failure
                val registeredIntents = IntentActionHandlerRegistry.getRegisteredIntents()
                Log.w(TAG, "Partial initialization: ${registeredIntents.size} handlers registered")
                isInitialized = true
            }
        }
    }

    /**
     * Check if actions have been initialized.
     *
     * @return True if initialize() has been called
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * Reset initialization state (for testing only).
     */
    internal fun reset() {
        synchronized(this) {
            isInitialized = false
            IntentActionHandlerRegistry.clear()
            Log.d(TAG, "Actions reset (testing only)")
        }
    }
}
