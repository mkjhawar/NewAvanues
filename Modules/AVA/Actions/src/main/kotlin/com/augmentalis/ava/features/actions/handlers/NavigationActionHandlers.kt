package com.augmentalis.ava.features.actions.handlers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.augmentalis.ava.features.actions.ActionResult
import com.augmentalis.ava.features.actions.IntentActionHandler
import com.augmentalis.ava.features.actions.VoiceOSConnection

/**
 * Action handler for going home.
 */
class GoHomeActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "GoHomeHandler"
    }

    override val intent = "go_home"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Going home for utterance: '$utterance'")

            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(homeIntent)

            Log.i(TAG, "Navigated to home")
            ActionResult.Success(message = "Going home")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to go home", e)
            ActionResult.Failure(
                message = "Failed to go home: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for navigate_home (alias for go_home).
 */
class NavigateHomeActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "NavigateHomeHandler"
    }

    override val intent = "navigate_home"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return GoHomeActionHandler().execute(context, utterance)
    }
}

/**
 * Action handler for going back.
 *
 * Routes to VoiceOS accessibility service for execution.
 */
class GoBackActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "GoBackHandler"
        private const val CATEGORY = "navigation"
    }

    override val intent = "go_back"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Going back for utterance: '$utterance'")

            val voiceOS = VoiceOSConnection.getInstance(context)
            val result = voiceOS.executeCommand(intent, CATEGORY)

            when (result) {
                is VoiceOSConnection.CommandResult.Success -> {
                    ActionResult.Success(message = result.message)
                }
                is VoiceOSConnection.CommandResult.Failure -> {
                    ActionResult.Failure(message = result.error)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to go back", e)
            ActionResult.Failure(
                message = "Failed to go back: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for back (alias for go_back).
 */
class BackActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "BackHandler"
    }

    override val intent = "back"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return GoBackActionHandler().execute(context, utterance)
    }
}

/**
 * Action handler for opening recent apps.
 *
 * Routes to VoiceOS accessibility service for execution.
 */
class RecentAppsActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "RecentAppsHandler"
        private const val CATEGORY = "navigation"
    }

    override val intent = "recent_apps"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Opening recent apps for utterance: '$utterance'")

            val voiceOS = VoiceOSConnection.getInstance(context)
            val result = voiceOS.executeCommand(intent, CATEGORY)

            when (result) {
                is VoiceOSConnection.CommandResult.Success -> {
                    ActionResult.Success(message = result.message)
                }
                is VoiceOSConnection.CommandResult.Failure -> {
                    ActionResult.Failure(message = result.error)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open recent apps", e)
            ActionResult.Failure(
                message = "Failed to open recent apps: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for open_recent_apps (alias for recent_apps).
 */
class OpenRecentAppsActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "OpenRecentAppsHandler"
    }

    override val intent = "open_recent_apps"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return RecentAppsActionHandler().execute(context, utterance)
    }
}

/**
 * Action handler for showing notifications.
 */
class NotificationsActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "NotificationsHandler"
    }

    override val intent = "notifications"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Showing notifications for utterance: '$utterance'")

            // Use statusbar service to expand notifications
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val expandMethod = statusBarManager.getMethod("expandNotificationsPanel")
            expandMethod.invoke(statusBarService)

            Log.i(TAG, "Opened notifications panel")
            ActionResult.Success(message = "Showing notifications")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notifications", e)
            ActionResult.Failure(
                message = "Failed to show notifications: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for show_notifications (alias for notifications).
 */
class ShowNotificationsActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "ShowNotificationsHandler"
    }

    override val intent = "show_notifications"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return NotificationsActionHandler().execute(context, utterance)
    }
}

/**
 * Action handler for hiding notifications.
 */
class HideNotificationsActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "HideNotificationsHandler"
    }

    override val intent = "hide_notifications"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Hiding notifications for utterance: '$utterance'")

            // Use statusbar service to collapse notifications
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val collapseMethod = statusBarManager.getMethod("collapsePanels")
            collapseMethod.invoke(statusBarService)

            Log.i(TAG, "Collapsed notifications panel")
            ActionResult.Success(message = "Notifications hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide notifications", e)
            ActionResult.Failure(
                message = "Failed to hide notifications: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for opening browser.
 */
class OpenBrowserActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "OpenBrowserHandler"
    }

    override val intent = "open_browser"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Opening browser for utterance: '$utterance'")

            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)

            Log.i(TAG, "Opened browser")
            ActionResult.Success(message = "Opening browser")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open browser", e)
            ActionResult.Failure(
                message = "Failed to open browser: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for menu.
 *
 * Routes to VoiceOS accessibility service for execution.
 */
class MenuActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "MenuHandler"
        private const val CATEGORY = "menu"
    }

    override val intent = "menu"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Opening menu for utterance: '$utterance'")

            val voiceOS = VoiceOSConnection.getInstance(context)
            val result = voiceOS.executeCommand(intent, CATEGORY)

            when (result) {
                is VoiceOSConnection.CommandResult.Success -> {
                    ActionResult.Success(message = result.message)
                }
                is VoiceOSConnection.CommandResult.Failure -> {
                    ActionResult.Failure(message = result.error)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open menu", e)
            ActionResult.Failure(
                message = "Failed to open menu: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for return_to_dashboard.
 */
class ReturnToDashboardActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "ReturnToDashboardHandler"
    }

    override val intent = "return_to_dashboard"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return GoHomeActionHandler().execute(context, utterance)
    }
}
