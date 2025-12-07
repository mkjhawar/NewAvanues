package com.augmentalis.Avanues.web.universal.presentation.viewmodel

import com.augmentalis.Avanues.web.universal.presentation.ui.security.*
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * SecurityViewModel - Manages browser security state and dialogs
 *
 * Responsibilities:
 * - Show/hide SSL error dialogs
 * - Show/hide permission request dialogs
 * - Show/hide JavaScript dialogs (alert/confirm/prompt)
 * - Prevent dialog spam (max 3 per 10 seconds)
 * - Persist permission choices to database
 *
 * Phase 1 Security Fixes:
 * - CWE-295: SSL certificate validation
 * - CWE-276: User consent for permissions
 * - CWE-1021: JavaScript dialog restrictions
 */
class SecurityViewModel(
    private val repository: BrowserRepository
) {
    // Coroutine scope for ViewModel
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ========== SSL Error Dialog State ==========

    private val _sslErrorState = MutableStateFlow<SslErrorDialogState?>(null)
    val sslErrorState: StateFlow<SslErrorDialogState?> = _sslErrorState.asStateFlow()

    // ========== Permission Request Dialog State ==========

    private val _permissionRequestState = MutableStateFlow<PermissionDialogState?>(null)
    val permissionRequestState: StateFlow<PermissionDialogState?> = _permissionRequestState.asStateFlow()

    // ========== JavaScript Dialog State ==========

    private val _jsAlertState = MutableStateFlow<JsAlertDialogState?>(null)
    val jsAlertState: StateFlow<JsAlertDialogState?> = _jsAlertState.asStateFlow()

    private val _jsConfirmState = MutableStateFlow<JsConfirmDialogState?>(null)
    val jsConfirmState: StateFlow<JsConfirmDialogState?> = _jsConfirmState.asStateFlow()

    private val _jsPromptState = MutableStateFlow<JsPromptDialogState?>(null)
    val jsPromptState: StateFlow<JsPromptDialogState?> = _jsPromptState.asStateFlow()

    // ========== HTTP Authentication Dialog State ==========

    private val _httpAuthState = MutableStateFlow<HttpAuthDialogState?>(null)
    val httpAuthState: StateFlow<HttpAuthDialogState?> = _httpAuthState.asStateFlow()

    // ========== Dialog Spam Prevention ==========
    // FIX P1-6: Use synchronized list to prevent race conditions
    private val dialogTimestamps = java.util.Collections.synchronizedList(mutableListOf<Long>())
    private val maxDialogsPerWindow = 3
    private val timeWindowMs = 10_000L // 10 seconds

    // ========== SSL Error Handling ==========

    /**
     * Show SSL error dialog
     *
     * @param sslErrorInfo SSL error details from WebView
     * @param onGoBack Callback when user chooses to go back (safe)
     * @param onProceedAnyway Callback when user chooses to proceed (dangerous)
     */
    fun showSslErrorDialog(
        sslErrorInfo: SslErrorInfo,
        onGoBack: () -> Unit,
        onProceedAnyway: () -> Unit
    ) {
        if (isDialogSpamDetected()) {
            println("⚠️  Dialog spam detected! Blocking SSL error dialog.")
            onGoBack() // Default to safe action
            return
        }

        _sslErrorState.value = SslErrorDialogState(
            sslErrorInfo = sslErrorInfo,
            onGoBack = {
                onGoBack()
                dismissSslErrorDialog()
            },
            onProceedAnyway = {
                onProceedAnyway()
                dismissSslErrorDialog()
            }
        )

        recordDialogShown()
    }

    fun dismissSslErrorDialog() {
        _sslErrorState.value = null
    }

    // ========== Permission Request Handling ==========

    /**
     * Show permission request dialog
     *
     * @param domain Domain requesting permission
     * @param permissions List of requested permissions
     * @param onAllow Callback when user grants permission (with remember flag)
     * @param onDeny Callback when user denies permission
     */
    fun showPermissionRequestDialog(
        domain: String,
        permissions: List<PermissionType>,
        onAllow: (remember: Boolean) -> Unit,
        onDeny: () -> Unit
    ) {
        if (isDialogSpamDetected()) {
            println("⚠️  Dialog spam detected! Blocking permission request dialog.")
            onDeny() // Default to safe action
            return
        }

        _permissionRequestState.value = PermissionDialogState(
            permissionRequest = PermissionRequest(
                domain = domain,
                permissions = permissions,
                requestId = "${domain}-${Clock.System.now().toEpochMilliseconds()}"
            ),
            onAllow = { remember ->
                // Persist permission if user checked "remember"
                if (remember) {
                    persistPermissions(domain, permissions, granted = true)
                }
                onAllow(remember)
                dismissPermissionDialog()
            },
            onDeny = {
                onDeny()
                dismissPermissionDialog()
            }
        )

        recordDialogShown()
    }

    fun dismissPermissionDialog() {
        _permissionRequestState.value = null
    }

    /**
     * Check if domain has remembered permission
     *
     * @param domain Domain to check
     * @param permission Permission type to check
     * @return true if permission was granted and remembered
     */
    suspend fun hasRememberedPermission(domain: String, permission: PermissionType): Boolean {
        return try {
            val result = repository.getSitePermission(domain, permission.name)
            result.getOrNull()?.granted ?: false
        } catch (e: Exception) {
            println("⚠️  Error checking remembered permission: ${e.message}")
            false
        }
    }

    /**
     * Persist permission choice to database
     */
    private fun persistPermissions(domain: String, permissions: List<PermissionType>, granted: Boolean) {
        viewModelScope.launch {
            try {
                permissions.forEach { permission ->
                    repository.insertSitePermission(
                        domain = domain,
                        permissionType = permission.name,
                        granted = granted
                    )
                }
                println("✅ Persisted permissions for $domain: ${permissions.joinToString { it.name }} = $granted")
            } catch (e: Exception) {
                println("⚠️  Error persisting permissions: ${e.message}")
            }
        }
    }

    // ========== JavaScript Dialog Handling ==========

    /**
     * Show JavaScript alert dialog
     */
    fun showJsAlertDialog(
        domain: String,
        message: String,
        onDismiss: () -> Unit
    ) {
        if (isDialogSpamDetected()) {
            println("⚠️  Dialog spam detected! Blocking JS alert dialog.")
            onDismiss()
            return
        }

        _jsAlertState.value = JsAlertDialogState(
            domain = domain,
            message = message,
            onDismiss = {
                onDismiss()
                dismissJsAlertDialog()
            }
        )

        recordDialogShown()
    }

    fun dismissJsAlertDialog() {
        _jsAlertState.value = null
    }

    /**
     * Show JavaScript confirm dialog
     */
    fun showJsConfirmDialog(
        domain: String,
        message: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ) {
        if (isDialogSpamDetected()) {
            println("⚠️  Dialog spam detected! Blocking JS confirm dialog.")
            onCancel() // Default to safe action
            return
        }

        _jsConfirmState.value = JsConfirmDialogState(
            domain = domain,
            message = message,
            onConfirm = {
                onConfirm()
                dismissJsConfirmDialog()
            },
            onCancel = {
                onCancel()
                dismissJsConfirmDialog()
            }
        )

        recordDialogShown()
    }

    fun dismissJsConfirmDialog() {
        _jsConfirmState.value = null
    }

    /**
     * Show JavaScript prompt dialog
     */
    fun showJsPromptDialog(
        domain: String,
        message: String,
        defaultValue: String,
        onConfirm: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        if (isDialogSpamDetected()) {
            println("⚠️  Dialog spam detected! Blocking JS prompt dialog.")
            onCancel() // Default to safe action
            return
        }

        _jsPromptState.value = JsPromptDialogState(
            domain = domain,
            message = message,
            defaultValue = defaultValue,
            onConfirm = { input ->
                onConfirm(input)
                dismissJsPromptDialog()
            },
            onCancel = {
                onCancel()
                dismissJsPromptDialog()
            }
        )

        recordDialogShown()
    }

    fun dismissJsPromptDialog() {
        _jsPromptState.value = null
    }

    // ========== HTTP Authentication Handling ==========

    /**
     * Show HTTP authentication dialog
     *
     * @param authRequest HTTP auth request details (host, realm, scheme)
     * @param onAuthenticate Callback when user provides credentials
     * @param onCancel Callback when user cancels authentication
     */
    fun showHttpAuthDialog(
        authRequest: HttpAuthRequest,
        onAuthenticate: (HttpAuthCredentials) -> Unit,
        onCancel: () -> Unit
    ) {
        if (isDialogSpamDetected()) {
            println("⚠️  Dialog spam detected! Blocking HTTP auth dialog.")
            onCancel() // Default to safe action
            return
        }

        _httpAuthState.value = HttpAuthDialogState(
            authRequest = authRequest,
            onAuthenticate = { credentials ->
                onAuthenticate(credentials)
                dismissHttpAuthDialog()
            },
            onCancel = {
                onCancel()
                dismissHttpAuthDialog()
            }
        )

        recordDialogShown()
    }

    fun dismissHttpAuthDialog() {
        _httpAuthState.value = null
    }

    // ========== Dialog Spam Prevention ==========

    /**
     * Check if dialog spam is detected
     *
     * Algorithm:
     * - Track timestamps of last N dialogs
     * - If more than 3 dialogs shown in last 10 seconds, block new dialogs
     * - Prevents malicious sites from spamming users with dialogs
     *
     * FIX P1-6: @Synchronized for atomic check-and-update
     * @return true if spam detected, false otherwise
     */
    @Synchronized
    private fun isDialogSpamDetected(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()

        // Remove timestamps older than time window
        dialogTimestamps.removeAll { timestamp ->
            (now - timestamp) > timeWindowMs
        }

        // Check if we've hit the limit
        return dialogTimestamps.size >= maxDialogsPerWindow
    }

    /**
     * Record that a dialog was shown
     * FIX P1-6: @Synchronized for atomic add
     */
    @Synchronized
    private fun recordDialogShown() {
        val now = Clock.System.now().toEpochMilliseconds()
        dialogTimestamps.add(now)

        // Log spam prevention status
        if (dialogTimestamps.size >= maxDialogsPerWindow - 1) {
            println("Dialog spam warning: ${dialogTimestamps.size}/$maxDialogsPerWindow dialogs in last 10s")
        }
    }

    /**
     * Cleanup ViewModel
     */
    fun onCleared() {
        viewModelScope.cancel()
    }
}

// ========== Dialog State Data Classes ==========

data class SslErrorDialogState(
    val sslErrorInfo: SslErrorInfo,
    val onGoBack: () -> Unit,
    val onProceedAnyway: () -> Unit
)

data class PermissionDialogState(
    val permissionRequest: PermissionRequest,
    val onAllow: (remember: Boolean) -> Unit,
    val onDeny: () -> Unit
)

data class JsAlertDialogState(
    val domain: String,
    val message: String,
    val onDismiss: () -> Unit
)

data class JsConfirmDialogState(
    val domain: String,
    val message: String,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)

data class JsPromptDialogState(
    val domain: String,
    val message: String,
    val defaultValue: String,
    val onConfirm: (String) -> Unit,
    val onCancel: () -> Unit
)

data class HttpAuthDialogState(
    val authRequest: HttpAuthRequest,
    val onAuthenticate: (HttpAuthCredentials) -> Unit,
    val onCancel: () -> Unit
)
