package com.augmentalis.webavanue

import com.augmentalis.webavanue.*
import com.augmentalis.webavanue.Logger
import com.augmentalis.webavanue.BrowserRepository
import com.augmentalis.foundation.viewmodel.BaseViewModel
import com.augmentalis.foundation.state.NullableState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock

/**
 * SecurityViewModel - Manages browser security state and dialogs
 *
 * Refactored to use StateFlow utilities for reduced boilerplate.
 *
 * Responsibilities:
 * - Show/hide SSL error dialogs
 * - Show/hide permission request dialogs
 * - Show/hide JavaScript dialogs (alert/confirm/prompt)
 * - Prevent dialog spam (max 3 per 10 seconds)
 * - Persist permission choices to database
 * - Securely store and retrieve HTTP authentication credentials
 */
class SecurityViewModel(
    private val repository: BrowserRepository,
    private var secureStorage: SecureStorageProvider? = null
) : BaseViewModel() {

    // ========== Dialog States ==========
    private val _sslErrorState = NullableState<SslErrorDialogState>()
    val sslErrorState: StateFlow<SslErrorDialogState?> = _sslErrorState.flow

    private val _permissionRequestState = NullableState<PermissionDialogState>()
    val permissionRequestState: StateFlow<PermissionDialogState?> = _permissionRequestState.flow

    private val _jsAlertState = NullableState<JsAlertDialogState>()
    val jsAlertState: StateFlow<JsAlertDialogState?> = _jsAlertState.flow

    private val _jsConfirmState = NullableState<JsConfirmDialogState>()
    val jsConfirmState: StateFlow<JsConfirmDialogState?> = _jsConfirmState.flow

    private val _jsPromptState = NullableState<JsPromptDialogState>()
    val jsPromptState: StateFlow<JsPromptDialogState?> = _jsPromptState.flow

    private val _httpAuthState = NullableState<HttpAuthDialogState>()
    val httpAuthState: StateFlow<HttpAuthDialogState?> = _httpAuthState.flow

    // ========== Dialog Spam Prevention ==========
    private val dialogTimestamps = java.util.Collections.synchronizedList(mutableListOf<Long>())
    private val maxDialogsPerWindow = 3
    private val timeWindowMs = 10_000L

    // ========== SSL Error Handling ==========

    fun showSslErrorDialog(
        sslErrorInfo: SslErrorInfo,
        onGoBack: () -> Unit,
        onProceedAnyway: () -> Unit
    ) {
        if (isDialogSpamDetected()) {
            Logger.warn("SecurityViewModel", "Dialog spam detected! Blocking SSL error dialog")
            onGoBack()
            return
        }

        _sslErrorState.set(SslErrorDialogState(
            sslErrorInfo = sslErrorInfo,
            onGoBack = { onGoBack(); dismissSslErrorDialog() },
            onProceedAnyway = { onProceedAnyway(); dismissSslErrorDialog() }
        ))
        recordDialogShown()
    }

    fun dismissSslErrorDialog() = _sslErrorState.clear()

    // ========== Permission Request Handling ==========

    fun showPermissionRequestDialog(
        domain: String,
        permissions: List<PermissionType>,
        onAllow: (remember: Boolean) -> Unit,
        onDeny: () -> Unit
    ) {
        if (isDialogSpamDetected()) {
            Logger.warn("SecurityViewModel", "Dialog spam detected! Blocking permission request dialog")
            onDeny()
            return
        }

        _permissionRequestState.set(PermissionDialogState(
            permissionRequest = PermissionRequest(
                domain = domain,
                permissions = permissions,
                requestId = "${domain}-${Clock.System.now().toEpochMilliseconds()}"
            ),
            onAllow = { remember ->
                if (remember) persistPermissions(domain, permissions, granted = true)
                onAllow(remember)
                dismissPermissionDialog()
            },
            onDeny = { onDeny(); dismissPermissionDialog() }
        ))
        recordDialogShown()
    }

    fun dismissPermissionDialog() = _permissionRequestState.clear()

    suspend fun hasRememberedPermission(domain: String, permission: PermissionType): Boolean {
        return try {
            repository.getSitePermission(domain, permission.name).getOrNull()?.granted ?: false
        } catch (e: Exception) {
            Logger.error("SecurityViewModel", "Error checking remembered permission: ${e.message}", e)
            false
        }
    }

    private fun persistPermissions(domain: String, permissions: List<PermissionType>, granted: Boolean) {
        launch {
            try {
                permissions.forEach { permission ->
                    repository.insertSitePermission(domain = domain, permissionType = permission.name, granted = granted)
                }
                Logger.info("SecurityViewModel", "Persisted permissions for $domain: ${permissions.joinToString { it.name }} = $granted")
            } catch (e: Exception) {
                Logger.error("SecurityViewModel", "Error persisting permissions: ${e.message}", e)
            }
        }
    }

    // ========== JavaScript Dialog Handling ==========

    fun showJsAlertDialog(domain: String, message: String, onDismiss: () -> Unit) {
        if (isDialogSpamDetected()) {
            Logger.warn("SecurityViewModel", "Dialog spam detected! Blocking JS alert dialog")
            onDismiss()
            return
        }
        _jsAlertState.set(JsAlertDialogState(domain, message, onDismiss = { onDismiss(); dismissJsAlertDialog() }))
        recordDialogShown()
    }

    fun dismissJsAlertDialog() = _jsAlertState.clear()

    fun showJsConfirmDialog(domain: String, message: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
        if (isDialogSpamDetected()) {
            Logger.warn("SecurityViewModel", "Dialog spam detected! Blocking JS confirm dialog")
            onCancel()
            return
        }
        _jsConfirmState.set(JsConfirmDialogState(
            domain = domain,
            message = message,
            onConfirm = { onConfirm(); dismissJsConfirmDialog() },
            onCancel = { onCancel(); dismissJsConfirmDialog() }
        ))
        recordDialogShown()
    }

    fun dismissJsConfirmDialog() = _jsConfirmState.clear()

    fun showJsPromptDialog(domain: String, message: String, defaultValue: String, onConfirm: (String) -> Unit, onCancel: () -> Unit) {
        if (isDialogSpamDetected()) {
            Logger.warn("SecurityViewModel", "Dialog spam detected! Blocking JS prompt dialog")
            onCancel()
            return
        }
        _jsPromptState.set(JsPromptDialogState(
            domain = domain,
            message = message,
            defaultValue = defaultValue,
            onConfirm = { input -> onConfirm(input); dismissJsPromptDialog() },
            onCancel = { onCancel(); dismissJsPromptDialog() }
        ))
        recordDialogShown()
    }

    fun dismissJsPromptDialog() = _jsPromptState.clear()

    // ========== HTTP Authentication Handling ==========

    fun showHttpAuthDialog(authRequest: HttpAuthRequest, onAuthenticate: (HttpAuthCredentials) -> Unit, onCancel: () -> Unit) {
        if (isDialogSpamDetected()) {
            Logger.warn("SecurityViewModel", "Dialog spam detected! Blocking HTTP auth dialog")
            onCancel()
            return
        }

        launch {
            val storedCredentials = getStoredCredentials(authRequest.host)
            if (storedCredentials != null) {
                println("✅ Using stored credentials for ${authRequest.host}")
                onAuthenticate(storedCredentials)
                return@launch
            }

            _httpAuthState.set(HttpAuthDialogState(
                authRequest = authRequest,
                onAuthenticate = { credentials ->
                    if (credentials.remember) storeCredentials(authRequest.host, credentials)
                    onAuthenticate(credentials)
                    dismissHttpAuthDialog()
                },
                onCancel = { onCancel(); dismissHttpAuthDialog() }
            ))
            recordDialogShown()
        }
    }

    fun dismissHttpAuthDialog() = _httpAuthState.clear()

    // ========== Dialog Spam Prevention ==========

    @Synchronized
    private fun isDialogSpamDetected(): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        dialogTimestamps.removeAll { (now - it) > timeWindowMs }
        return dialogTimestamps.size >= maxDialogsPerWindow
    }

    @Synchronized
    private fun recordDialogShown() {
        val now = Clock.System.now().toEpochMilliseconds()
        dialogTimestamps.add(now)
        if (dialogTimestamps.size >= maxDialogsPerWindow - 1) {
            Logger.warn("SecurityViewModel", "Dialog spam warning: ${dialogTimestamps.size}/$maxDialogsPerWindow dialogs in last 10s")
        }
    }

    // ========== Secure Credential Storage ==========

    fun setSecureStorage(storage: SecureStorageProvider) {
        this.secureStorage = storage
    }

    private fun storeCredentials(url: String, credentials: HttpAuthCredentials) {
        launch {
            try {
                secureStorage?.storeCredential(url, credentials.username, credentials.password, credentials.remember)
                println("✅ Credentials stored securely for $url")
            } catch (e: Exception) {
                println("⚠️  Failed to store credentials: ${e.message}")
            }
        }
    }

    private suspend fun getStoredCredentials(url: String): HttpAuthCredentials? {
        return try {
            secureStorage?.getCredential(url)
        } catch (e: Exception) {
            println("⚠️  Failed to retrieve credentials: ${e.message}")
            null
        }
    }

    fun removeStoredCredentials(url: String) {
        launch {
            try {
                secureStorage?.removeCredential(url)
                println("✅ Credentials removed for $url")
            } catch (e: Exception) {
                println("⚠️  Failed to remove credentials: ${e.message}")
            }
        }
    }

    fun clearAllStoredCredentials() {
        launch {
            try {
                secureStorage?.clearAll()
                println("✅ All credentials cleared")
            } catch (e: Exception) {
                println("⚠️  Failed to clear credentials: ${e.message}")
            }
        }
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

// ========== Secure Storage Provider Interface ==========

interface SecureStorageProvider {
    fun storeCredential(url: String, username: String, password: String, remember: Boolean)
    fun getCredential(url: String): HttpAuthCredentials?
    fun hasCredential(url: String): Boolean
    fun removeCredential(url: String)
    fun clearAll()
    fun getStoredUrls(): List<String>
}
