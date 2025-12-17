/**
 * ConsentDialog.kt - WindowManager-based consent overlay for app learning
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/ui/ConsentDialog.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-24
 * Updated: 2025-10-28 (v1.0.5 - Refactored to use WidgetOverlayHelper for thread safety)
 *
 * WindowManager-based overlay for app learning consent.
 * Uses WindowManager.addView() directly to bypass Dialog's Activity requirement.
 * This is the only reliable way to show overlays from AccessibilityService context.
 */

package com.augmentalis.learnapp.ui

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.augmentalis.learnapp.R
import com.augmentalis.learnapp.ui.widgets.WidgetOverlayHelper


/**
 * Consent Dialog (WindowManager Version)
 *
 * Direct WindowManager overlay for asking user permission to learn app.
 * Uses WindowManager.addView() instead of Dialog to work reliably in
 * AccessibilityService context without requiring Activity.
 *
 * ## UI Layout
 *
 * ```
 * ┌────────────────────────────────────────┐
 * │                                        │
 * │   Learn Instagram?                     │
 * │   ───────────────────────────────      │
 * │                                        │
 * │   VoiceOS will explore Instagram       │
 * │   to enable voice commands.            │
 * │                                        │
 * │   This will:                           │
 * │   • Click buttons and menus            │
 * │   • Navigate between screens           │
 * │   • Skip dangerous actions             │
 * │   • Take ~2-5 minutes                  │
 * │                                        │
 * │              ┌────┐      ┌────┐        │
 * │              │ No │      │ Yes│        │
 * │              └────┘      └────┘        │
 * │                                        │
 * │   ☐ Don't ask again for this app      │
 * │                                        │
 * └────────────────────────────────────────┘
 * ```
 *
 * ## Thread Safety
 *
 * All UI operations use WidgetOverlayHelper.ensureMainThread() for optimal performance.
 * If already on main thread, executes immediately (no Handler delay).
 * If on background thread, posts to main thread. Safe to call from any thread.
 *
 * ## AccessibilityService Compatibility
 *
 * Uses WindowManager directly with TYPE_ACCESSIBILITY_OVERLAY to bypass
 * Dialog's Activity requirement. This is the only reliable way to show
 * overlays from AccessibilityService context.
 *
 * ## Fix History
 *
 * - v1.0.5 (2025-10-28): Fixed BadTokenException race condition - Refactored to use WidgetOverlayHelper
 *   - Replaced Handler.post() with WidgetOverlayHelper.ensureMainThread()
 *   - Eliminates race condition: immediate execution on main thread (no Handler delay)
 *   - Pattern consistency with ProgressOverlay
 *   - Performance improvement: no unnecessary message queue overhead
 * - v1.0.4 (2025-10-28): Fixed window flags - Added FLAG_NOT_FOCUSABLE, removed FLAG_WATCH_OUTSIDE_TOUCH
 * - v1.0.3 (2025-10-28): Fixed BadTokenException - Always use TYPE_ACCESSIBILITY_OVERLAY (not TYPE_APPLICATION_OVERLAY)
 * - v1.0.2 (2025-10-25): Switched to WindowManager.addView() - Dialog doesn't work with Application context
 * - v1.0.1 (2025-10-24): Attempted custom Dialog class (still crashed)
 * - v1.0.0 (2025-10-24): Initial widget-based implementation
 *
 * @param context Application or Service context
 *
 * @since 1.0.0
 */
class ConsentDialog(private val context: AccessibilityService) {

    /**
     * WindowManager for adding overlay views
     */
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * Helper for thread-safe WindowManager operations
     * Uses ensureMainThread() pattern for optimal performance
     */
    private val helper = WidgetOverlayHelper( windowManager)

    /**
     * Currently displayed view (if any)
     */
    private var currentView: View? = null

    /**
     * Show consent dialog
     *
     * Displays overlay asking user permission to learn app using WindowManager.
     * This approach works reliably in AccessibilityService context.
     *
     * Thread Safety: Uses WidgetOverlayHelper.ensureMainThread() for optimal performance.
     * If already on main thread (typical for AccessibilityService callbacks), executes
     * immediately without Handler delay. This prevents race conditions with context lifecycle.
     *
     * @param appName Human-readable app name
     * @param onApprove Callback when user approves (with dontAskAgain flag)
     * @param onDecline Callback when user declines (with dontAskAgain flag)
     */
    fun show(
        appName: String,
        onApprove: (dontAskAgain: Boolean) -> Unit,
        onDecline: (dontAskAgain: Boolean) -> Unit
    ) {
        helper.ensureMainThread {
            // Remove any existing view first
            currentView?.let { view ->
                try {
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    // View already removed, ignore
                }
            }

            // Inflate custom view
            val customView = LayoutInflater.from(context)
                .inflate(R.layout.layout_consent_dialog, null)

            // Configure title
            customView.findViewById<TextView>(R.id.title_text).text = "Learn $appName?"

            // Configure description
            customView.findViewById<TextView>(R.id.description_text).text =
                "VoiceOS will explore $appName to enable voice commands."

            // Get checkbox reference
            val dontAskCheckbox = customView.findViewById<CheckBox>(R.id.checkbox_dont_ask)

            // Wire up Deny button
            customView.findViewById<Button>(R.id.btn_deny).setOnClickListener {
                val dontAskAgain = dontAskCheckbox.isChecked
                dismiss()
                onDecline(dontAskAgain)
            }

            // Wire up Allow button
            customView.findViewById<Button>(R.id.btn_allow).setOnClickListener {
                val dontAskAgain = dontAskCheckbox.isChecked
                dismiss()
                onApprove(dontAskAgain)
            }

            // Create window layout parameters for AccessibilityService overlay
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            // Center the dialog
            params.gravity = Gravity.CENTER

            // Add view to window manager
            windowManager.addView(customView, params)
            currentView = customView
        }
    }

    /**
     * Dismiss dialog
     *
     * Removes overlay from window manager if currently displayed.
     * Uses ensureMainThread() for thread-safe removal.
     */
    fun dismiss() {
        helper.ensureMainThread {
            currentView?.let { view ->
                try {
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    // View already removed or not attached, ignore
                }
                currentView = null
            }
        }
    }

    /**
     * Check if dialog is currently showing
     *
     * @return true if overlay is visible
     */
    fun isShowing(): Boolean {
        return currentView != null && currentView?.parent != null
    }
}
