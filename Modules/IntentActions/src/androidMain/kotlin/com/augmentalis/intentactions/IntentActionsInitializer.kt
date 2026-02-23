package com.augmentalis.intentactions

import com.augmentalis.intentactions.actions.AddTodoAction
import com.augmentalis.intentactions.actions.CalculateAction
import com.augmentalis.intentactions.actions.CheckCalendarAction
import com.augmentalis.intentactions.actions.CreateCalendarEventAction
import com.augmentalis.intentactions.actions.CreateNoteAction
import com.augmentalis.intentactions.actions.CreateReminderAction
import com.augmentalis.intentactions.actions.FindNearbyAction
import com.augmentalis.intentactions.actions.GetDirectionsAction
import com.augmentalis.intentactions.actions.GetTimeAction
import com.augmentalis.intentactions.actions.GetWeatherAction
import com.augmentalis.intentactions.actions.MakeCallAction
import com.augmentalis.intentactions.actions.NavigateURLAction
import com.augmentalis.intentactions.actions.OpenAppAction
import com.augmentalis.intentactions.actions.OpenBrowserAction
import com.augmentalis.intentactions.actions.OpenSettingsAction
import com.augmentalis.intentactions.actions.OpenSettingsSubsectionAction
import com.augmentalis.intentactions.actions.PlayVideoAction
import com.augmentalis.intentactions.actions.ResumeMusicAction
import com.augmentalis.intentactions.actions.SaveLocationAction
import com.augmentalis.intentactions.actions.SendEmailAction
import com.augmentalis.intentactions.actions.SendTextAction
import com.augmentalis.intentactions.actions.SetAlarmAction
import com.augmentalis.intentactions.actions.SetTimerAction
import com.augmentalis.intentactions.actions.ShareLocationAction
import com.augmentalis.intentactions.actions.ShowTrafficAction
import com.augmentalis.intentactions.actions.WebSearchAction

/**
 * Registers all IIntentAction implementations with the IntentActionRegistry.
 * Call once at app startup.
 */
object IntentActionsInitializer {
    private var initialized = false

    fun initialize() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            IntentActionRegistry.registerAll(
                // Communication (3)
                SendEmailAction,
                SendTextAction,
                MakeCallAction,
                // Navigation (5)
                GetDirectionsAction,
                FindNearbyAction,
                ShowTrafficAction,
                ShareLocationAction,
                SaveLocationAction,
                // Productivity (8)
                SetAlarmAction,
                SetTimerAction,
                CreateReminderAction,
                CreateCalendarEventAction,
                AddTodoAction,
                CreateNoteAction,
                CheckCalendarAction,
                GetTimeAction,
                // Search (3) + Weather (1)
                WebSearchAction,
                NavigateURLAction,
                CalculateAction,
                GetWeatherAction,
                // Media Launch (4)
                PlayVideoAction,
                ResumeMusicAction,
                OpenBrowserAction,
                OpenAppAction,
                // Settings (2)
                OpenSettingsAction,
                OpenSettingsSubsectionAction
            )
            initialized = true
        }
    }
}
