/**
 * ConsentDialogManager.kt - Manages consent dialog lifecycle and state
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/ui/ConsentDialogManager.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 * Updated: 2025-10-24 (Migrated from Compose to widgets)
 *
 * Manages consent dialog showing, hiding, and user response handling.
 * Migrated from Compose to widget-based implementation for AccessibilityService compatibility.
 */

package com.augmentalis.voiceoscore.learnapp.ui

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.augmentalis.voiceoscore.learnapp.detection.LearnedAppTracker
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Consent Dialog Manager
 *
 * Manages lifecycle of consent dialog overlay.
 * Handles showing, hiding, and user responses.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val manager = ConsentDialogManager(context, learnedAppTracker)
 *
 * // Show dialog
 * manager.showConsentDialog("com.instagram.android", "Instagram")
 *
 * // Observe user responses
 * manager.consentResponses.collect { response ->
 *     when (response) {
 *         is ConsentResponse.Approved -> {
 *             // Start exploration
 *             startExploration(response.packageName)
 *         }
 *         is ConsentResponse.Declined -> {
 *             // Do nothing
 *         }
 *     }
 * }
 * ```
 *
 * ## Permissions Required
 *
 * - SYSTEM_ALERT_WINDOW permission for overlay
 *
 * ## Migration Notes
 *
 * Previously used Compose with custom lifecycle owner (MyLifecycleOwner).
 * Now uses widget-based AlertDialog which works reliably in AccessibilityService context.
 * All Compose dependencies removed.
 *
 * @property context Application context
 * @property learnedAppTracker Tracker for marking apps as dismissed
 *
 * @since 1.0.0
 */
class ConsentDialogManager(
    private val context: AccessibilityService,
    private val learnedAppTracker: LearnedAppTracker
) {

    /**
     * Developer settings for configurable timeouts/intervals
     */
    private val developerSettings = LearnAppDeveloperSettings(context)

    /**
     * Coroutine scope for launching suspend functions
     */
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Widget-based consent dialog (replaces Compose version)
     */
    private val consentDialog = ConsentDialog(context)

    /**
     * Current package name being shown
     */
    private var currentPackageName: String? = null

    /**
     * Session cache of packages with consent decisions
     * Prevents re-prompting for apps where user already decided (this session)
     * FR-011: Prevent consent dialog from showing more than once per app per session
     * FIX (2025-11-30): Use thread-safe set to prevent race conditions
     */
    private val sessionConsentCache: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * Shared flow for consent responses
     */
    private val _consentResponses = MutableSharedFlow<ConsentResponse>(replay = 0)
    val consentResponses: SharedFlow<ConsentResponse> = _consentResponses.asSharedFlow()

    /**
     * FIX (2025-11-30): P2 - Permission recovery mechanism
     * Queue of pending consent requests that couldn't be shown due to missing permission.
     * When permission is granted, these will be retried.
     */
    private data class PendingConsentRequest(
        val packageName: String,
        val appName: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val pendingRequests = ConcurrentLinkedQueue<PendingConsentRequest>()
    private var permissionMonitorActive = false

    companion object {
        private const val TAG = "ConsentDialogManager"
        // FIX (2025-12-05): Now configurable via LearnAppDeveloperSettings
        // Default values kept as fallback (if settings unavailable)
        private const val PERMISSION_CHECK_INTERVAL_MS = 1000L  // Fallback: Check every 1 second
        private const val PENDING_REQUEST_EXPIRY_MS = 60000L   // Fallback: Expire after 1 minute
    }

    /**
     * Show consent dialog
     *
     * Displays dialog asking user permission to learn app.
     *
     * FR-003: Dialog must remain stable without flickering
     * FR-011: Prevent showing more than once per app per session
     *
     * @param packageName Package name of app
     * @param appName Human-readable app name
     */
    suspend fun showConsentDialog(packageName: String, appName: String) {
        // Check if user already made a decision for this package this session
        if (sessionConsentCache.contains(packageName)) {
            // Already decided - don't re-prompt (prevents flickering on continuous events)
            return
        }

        // Check if dialog already showing for the SAME package
        if (consentDialog.isShowing() && currentPackageName == packageName) {
            // Already showing for this app - don't hide/re-show (prevents flicker)
            return
        }

        // If showing for DIFFERENT package, hide old dialog first
        if (consentDialog.isShowing()) {
            hideConsentDialog()
        }

        // Check overlay permission
        if (!hasOverlayPermission()) {
            // FIX (2025-11-30): P2 - Queue request for retry when permission is granted
            Log.w(TAG, "Overlay permission not granted - queuing request for $packageName")
            pendingRequests.offer(PendingConsentRequest(packageName, appName))
            startPermissionMonitor()
            return
        }

        currentPackageName = packageName

        // Show widget-based dialog
        // Note: consentDialog.show() handles main thread internally via ensureMainThread()
        consentDialog.show(
            appName = appName,
            onApprove = { dontAskAgain ->
                scope.launch {
                    handleApproval(packageName, dontAskAgain)
                }
            },
            onDecline = { dontAskAgain ->
                scope.launch {
                    handleDeclination(packageName, dontAskAgain)
                }
            },
            onSkip = {
                scope.launch {
                    handleSkip(packageName)
                }
            }
        )
    }

    /**
     * Hide consent dialog
     *
     * Dismisses dialog from screen.
     */
    suspend fun hideConsentDialog() {
        withContext(Dispatchers.Main) {
            consentDialog.dismiss()
            currentPackageName = null
        }
    }

    /**
     * Handle user approval
     *
     * FIX (2025-12-02): Hide dialog BEFORE emitting response to prevent window detection blocking
     *
     * @param packageName Package name
     * @param dontAskAgain If true, don't show dialog again for this app
     */
    private suspend fun handleApproval(packageName: String, dontAskAgain: Boolean) {
        // Add to session cache to prevent re-prompting
        sessionConsentCache.add(packageName)

        // Mark as learned if "don't ask again" checked
        if (dontAskAgain) {
            learnedAppTracker.markAsLearned(packageName)
        }

        // FIX: Hide dialog BEFORE starting exploration
        // This ensures the app window is not blocked by the consent overlay
        hideConsentDialog()

        // Wait for dialog animation + window manager refresh
        // This allows the app window to become fully visible and detectable
        // FIX (2025-12-05): Wired to configurable setting
        delay(developerSettings.getDialogAnimationDelayMs())

        // Emit approved response (exploration will start with clear window)
        _consentResponses.emit(
            ConsentResponse.Approved(
                packageName = packageName
            )
        )
    }

    /**
     * Handle user declination
     *
     * FIX (2025-12-02): Hide dialog after declining to clean up UI state
     *
     * @param packageName Package name
     * @param dontAskAgain If true, don't show dialog again for this app
     */
    private suspend fun handleDeclination(packageName: String, dontAskAgain: Boolean) {
        // Add to session cache to prevent re-prompting
        sessionConsentCache.add(packageName)

        // Mark as dismissed
        if (dontAskAgain) {
            learnedAppTracker.markAsDismissed(packageName)
        }

        // FIX: Hide dialog after declining
        hideConsentDialog()

        // Emit declined response
        _consentResponses.emit(
            ConsentResponse.Declined(
                packageName = packageName
            )
        )
    }

    /**
     * Handle user skip (activate just-in-time learning)
     *
     * FIX (2025-12-02): Hide dialog after skipping to clean up UI state
     *
     * @param packageName Package name
     */
    private suspend fun handleSkip(packageName: String) {
        // Add to session cache to prevent re-prompting
        sessionConsentCache.add(packageName)

        // FIX: Hide dialog after skipping
        hideConsentDialog()

        // Emit skipped response (triggers JIT learning mode)
        _consentResponses.emit(
            ConsentResponse.Skipped(
                packageName = packageName
            )
        )
    }

    /**
     * Check if overlay permission is granted
     *
     * @return true if SYSTEM_ALERT_WINDOW permission granted
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true  // Permission not required on older Android versions
        }
    }

    /**
     * Check if dialog is currently visible
     *
     * @return true if dialog visible
     */
    fun isDialogShowing(): Boolean {
        return consentDialog.isShowing()
    }

    /**
     * Get current package name (if dialog showing)
     *
     * @return Package name or null
     */
    fun getCurrentPackage(): String? {
        return currentPackageName
    }

    /**
     * Clear session consent cache
     *
     * Useful for testing or manual reset scenarios.
     * Note: This doesn't affect persistent "don't ask again" decisions.
     */
    fun clearSessionCache() {
        sessionConsentCache.clear()
    }

    /**
     * Check if package has a consent decision this session
     *
     * @param packageName Package name to check
     * @return true if user already made a decision for this package
     */
    fun hasSessionDecision(packageName: String): Boolean {
        return sessionConsentCache.contains(packageName)
    }

    /**
     * Start permission monitor
     *
     * FIX (2025-11-30): P2 - Monitors for overlay permission being granted.
     * When granted, retries pending consent requests.
     */
    private fun startPermissionMonitor() {
        if (permissionMonitorActive) {
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Permission monitor already active")
            }
            return
        }

        permissionMonitorActive = true
        Log.i(TAG, "Starting permission monitor for pending consent requests")

        scope.launch {
            while (isActive && pendingRequests.isNotEmpty()) {
                delay(developerSettings.getPermissionCheckIntervalMs())

                // Clean up expired requests
                val now = System.currentTimeMillis()
                pendingRequests.removeIf { request ->
                    val expired = now - request.timestamp > developerSettings.getPendingRequestExpiryMs()
                    if (expired) {
                        if (developerSettings.isVerboseLoggingEnabled()) {
                            Log.d(TAG, "Expired pending request for ${request.packageName}")
                        }
                    }
                    expired
                }

                // Check if permission was granted
                if (hasOverlayPermission() && pendingRequests.isNotEmpty()) {
                    Log.i(TAG, "Overlay permission granted - processing ${pendingRequests.size} pending requests")

                    // Process oldest pending request (only one at a time)
                    val request = pendingRequests.poll()
                    if (request != null && !sessionConsentCache.contains(request.packageName)) {
                        if (developerSettings.isVerboseLoggingEnabled()) {
                            Log.d(TAG, "Retrying consent dialog for ${request.packageName}")
                        }
                        showConsentDialog(request.packageName, request.appName)
                    }
                }

                // Stop monitoring if no more pending requests
                if (pendingRequests.isEmpty()) {
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        Log.d(TAG, "No more pending requests - stopping permission monitor")
                    }
                    break
                }
            }

            permissionMonitorActive = false
        }
    }

    /**
     * Get pending request count (for diagnostics)
     *
     * @return Number of pending consent requests awaiting permission
     */
    fun getPendingRequestCount(): Int = pendingRequests.size

    /**
     * Cleanup (call in onDestroy)
     */
    fun cleanup() {
        scope.launch {
            hideConsentDialog()
        }
        pendingRequests.clear()
        sessionConsentCache.clear()
        permissionMonitorActive = false
        scope.cancel()
    }
}

// Note: ConsentResponse is defined in ConsentResponse.kt
