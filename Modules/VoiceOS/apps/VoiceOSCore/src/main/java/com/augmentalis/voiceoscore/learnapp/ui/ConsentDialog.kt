/**
 * ConsentDialog.kt - WindowManager-based consent overlay for app learning
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/ui/ConsentDialog.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-24
 * Updated: 2025-12-08 (v1.0.6 - Removed Material3 components to fix Compose lifecycle crash)
 *
 * WindowManager-based overlay for app learning consent.
 * Uses WindowManager.addView() directly to bypass Dialog's Activity requirement.
 * This is the only reliable way to show overlays from AccessibilityService context.
 */

package com.augmentalis.voiceoscore.learnapp.ui

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.augmentalis.voiceoscore.R
import com.augmentalis.voiceoscore.learnapp.ui.widgets.WidgetOverlayHelper
import com.augmentalis.voiceoscore.utils.MaterialThemeHelper


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
 * - v1.0.6 (2025-12-08): Fixed Compose lifecycle crash - Removed Material3 components
 *   - Replaced Material3 Button styles with custom ripple drawables
 *   - Replaced androidx.cardview.widget.CardView with LinearLayout + custom background
 *   - Eliminated Compose dependency that caused ViewTreeLifecycleOwner crash
 *   - Material3 components internally use Compose, which requires LifecycleOwner not available in AccessibilityService
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
     * @param onSkip Callback when user skips (activates JIT learning mode)
     */
    fun show(
        appName: String,
        onApprove: (dontAskAgain: Boolean) -> Unit,
        onDecline: (dontAskAgain: Boolean) -> Unit,
        onSkip: () -> Unit
    ) {
        Log.d(TAG, "show() called for appName: $appName")
        helper.ensureMainThread {
            Log.d(TAG, "ensureMainThread lambda executing for $appName")
            // Remove any existing view first
            currentView?.let { view ->
                try {
                    Log.d(TAG, "Removing existing view")
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    // View already removed, ignore
                    Log.d(TAG, "Exception removing existing view (expected if none): ${e.message}")
                }
            }

            // Inflate custom view with Material theme for MaterialComponents.Button styles
            Log.d(TAG, "Inflating consent dialog layout")
            val customView = MaterialThemeHelper.inflateOverlay(context, R.layout.learnapp_layout_consent_dialog)
            Log.d(TAG, "Layout inflated successfully")

            // Configure title
            customView.findViewById<TextView>(R.id.title_text).text = "Learn $appName?"

            // Configure description
            customView.findViewById<TextView>(R.id.description_text).text =
                "VoiceOS will explore $appName to enable voice commands."

            // Get checkbox reference
            val dontAskCheckbox = customView.findViewById<CheckBox>(R.id.checkbox_dont_ask)

            // Wire up Deny button with touch logging
            val btnDeny = customView.findViewById<Button>(R.id.btn_deny)
            btnDeny.setOnTouchListener { v, event ->
                Log.d(TAG, "btn_deny TOUCH: action=${event.action}, x=${event.x}, y=${event.y}")
                false  // Don't consume - let onClick handle it
            }
            btnDeny.setOnClickListener {
                Log.d(TAG, "✓ btn_deny CLICKED - dismissing and calling onDecline")
                val dontAskAgain = dontAskCheckbox.isChecked
                dismiss()
                onDecline(dontAskAgain)
            }

            // Wire up Allow button with touch logging
            val btnAllow = customView.findViewById<Button>(R.id.btn_allow)
            btnAllow.setOnTouchListener { v, event ->
                Log.d(TAG, "btn_allow TOUCH: action=${event.action}, x=${event.x}, y=${event.y}")
                false  // Don't consume - let onClick handle it
            }
            btnAllow.setOnClickListener {
                Log.d(TAG, "✓ btn_allow CLICKED - dismissing and calling onApprove")
                val dontAskAgain = dontAskCheckbox.isChecked
                dismiss()
                onApprove(dontAskAgain)
            }

            // Wire up Skip button with touch logging
            val btnSkip = customView.findViewById<Button>(R.id.btn_skip)
            btnSkip?.setOnTouchListener { v, event ->
                Log.d(TAG, "btn_skip TOUCH: action=${event.action}, x=${event.x}, y=${event.y}")
                false  // Don't consume - let onClick handle it
            }
            btnSkip?.setOnClickListener {
                Log.d(TAG, "✓ btn_skip CLICKED - dismissing and calling onSkip")
                dismiss()
                onSkip()
            }

            // Wrap dialog in full-screen container with semi-transparent background
            val container = android.widget.FrameLayout(context).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#80000000")) // Semi-transparent black
                // Don't set clickable - let touch events pass through to children naturally
            }

            // Add dialog view to container, centered
            val dialogLayoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            container.addView(customView, dialogLayoutParams)

            // Create window layout parameters for interactive overlay
            // FIX (2025-11-30): Use TYPE_ACCESSIBILITY_OVERLAY to be in same layer as other VoiceOS overlays
            // Without FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE to receive touches
            Log.d(TAG, "Creating WindowManager.LayoutParams")
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                FLAG_LAYOUT_IN_SCREEN,  // Only FLAG_LAYOUT_IN_SCREEN - no NOT_FOCUSABLE or NOT_TOUCHABLE
                PixelFormat.TRANSLUCENT
            )

            // Center the dialog
            params.gravity = Gravity.CENTER
            Log.d(TAG, "LayoutParams configured: type=${params.type}, flags=${params.flags}, gravity=${params.gravity}")

            // Note: For TYPE_ACCESSIBILITY_OVERLAY, Android automatically uses the
            // AccessibilityService's token - no need to set params.token

            // Add container to window manager
            try {
                Log.d(TAG, "About to call windowManager.addView()")
                windowManager.addView(container, params)
                Log.i(TAG, "✓ WindowManager.addView() succeeded - dialog should be visible")
                currentView = container
            } catch (e: Exception) {
                Log.e(TAG, "✗ FAILED to add view to WindowManager", e)
                Log.e(TAG, "Exception type: ${e.javaClass.name}")
                Log.e(TAG, "Exception message: ${e.message}")
                e.printStackTrace()
            }
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

    companion object {
        private const val TAG = "ConsentDialog"
    }
}
