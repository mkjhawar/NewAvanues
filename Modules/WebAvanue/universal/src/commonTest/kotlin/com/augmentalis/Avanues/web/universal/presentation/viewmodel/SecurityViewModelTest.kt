package com.augmentalis.webavanue.ui.viewmodel

import com.augmentalis.webavanue.FakeBrowserRepository
import com.augmentalis.webavanue.ui.screen.security.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

/**
 * SecurityViewModelTest - Unit tests for SecurityViewModel
 *
 * Tests:
 * - SSL error dialog state management
 * - Permission request dialog state management
 * - JavaScript dialog state management (alert, confirm, prompt)
 * - Dialog spam prevention
 * - Permission persistence
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SecurityViewModelTest {

    private lateinit var repository: FakeBrowserRepository
    private lateinit var viewModel: SecurityViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeBrowserRepository()
        viewModel = SecurityViewModel(repository)
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
        viewModel.onCleared()
    }

    // ========== SSL Error Dialog Tests ==========

    @Test
    fun `showSslErrorDialog sets SSL error state`() = runTest {
        // Given
        val sslErrorInfo = SslErrorInfo(
            errorType = SslErrorType.EXPIRED,
            url = "https://expired.badssl.com",
            certificateInfo = null,
            primaryError = "Certificate expired"
        )
        var goBackCalled = false
        var proceedCalled = false

        // When
        viewModel.showSslErrorDialog(
            sslErrorInfo = sslErrorInfo,
            onGoBack = { goBackCalled = true },
            onProceedAnyway = { proceedCalled = true }
        )

        // Then
        assertNotNull(viewModel.sslErrorState.value)
        assertEquals(sslErrorInfo, viewModel.sslErrorState.value?.sslErrorInfo)
        assertFalse(goBackCalled)
        assertFalse(proceedCalled)
    }

    @Test
    fun `dismissSslErrorDialog clears state`() = runTest {
        // Given
        val sslErrorInfo = SslErrorInfo(
            errorType = SslErrorType.EXPIRED,
            url = "https://expired.badssl.com",
            certificateInfo = null,
            primaryError = "Certificate expired"
        )
        viewModel.showSslErrorDialog(
            sslErrorInfo = sslErrorInfo,
            onGoBack = {},
            onProceedAnyway = {}
        )

        // When
        viewModel.dismissSslErrorDialog()

        // Then
        assertNull(viewModel.sslErrorState.value)
    }

    @Test
    fun `SSL error dialog onGoBack callback works`() = runTest {
        // Given
        val sslErrorInfo = SslErrorInfo(
            errorType = SslErrorType.EXPIRED,
            url = "https://expired.badssl.com",
            certificateInfo = null,
            primaryError = "Certificate expired"
        )
        var goBackCalled = false

        viewModel.showSslErrorDialog(
            sslErrorInfo = sslErrorInfo,
            onGoBack = { goBackCalled = true },
            onProceedAnyway = {}
        )

        // When
        viewModel.sslErrorState.value?.onGoBack?.invoke()

        // Then
        assertTrue(goBackCalled)
        assertNull(viewModel.sslErrorState.value) // Should auto-dismiss
    }

    // ========== Permission Request Dialog Tests ==========

    @Test
    fun `showPermissionRequestDialog sets permission state`() = runTest {
        // Given
        val domain = "example.com"
        val permissions = listOf(PermissionType.CAMERA, PermissionType.MICROPHONE)
        var allowCalled = false
        var denyCalled = false

        // When
        viewModel.showPermissionRequestDialog(
            domain = domain,
            permissions = permissions,
            onAllow = { allowCalled = true },
            onDeny = { denyCalled = true }
        )

        // Then
        assertNotNull(viewModel.permissionRequestState.value)
        assertEquals(domain, viewModel.permissionRequestState.value?.permissionRequest?.domain)
        assertEquals(permissions, viewModel.permissionRequestState.value?.permissionRequest?.permissions)
        assertFalse(allowCalled)
        assertFalse(denyCalled)
    }

    @Test
    fun `dismissPermissionDialog clears state`() = runTest {
        // Given
        viewModel.showPermissionRequestDialog(
            domain = "example.com",
            permissions = listOf(PermissionType.CAMERA),
            onAllow = {},
            onDeny = {}
        )

        // When
        viewModel.dismissPermissionDialog()

        // Then
        assertNull(viewModel.permissionRequestState.value)
    }

    @Test
    fun `permission dialog onAllow callback works`() = runTest {
        // Given
        var allowCalled = false
        var rememberChoice = false

        viewModel.showPermissionRequestDialog(
            domain = "example.com",
            permissions = listOf(PermissionType.CAMERA),
            onAllow = { remember ->
                allowCalled = true
                rememberChoice = remember
            },
            onDeny = {}
        )

        // When
        viewModel.permissionRequestState.value?.onAllow?.invoke(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(allowCalled)
        assertTrue(rememberChoice)
        assertNull(viewModel.permissionRequestState.value) // Should auto-dismiss
    }

    // ========== JavaScript Dialog Tests ==========

    @Test
    fun `showJsAlertDialog sets alert state`() = runTest {
        // Given
        val domain = "example.com"
        val message = "Hello world"
        var dismissCalled = false

        // When
        viewModel.showJsAlertDialog(
            domain = domain,
            message = message,
            onDismiss = { dismissCalled = true }
        )

        // Then
        assertNotNull(viewModel.jsAlertState.value)
        assertEquals(domain, viewModel.jsAlertState.value?.domain)
        assertEquals(message, viewModel.jsAlertState.value?.message)
        assertFalse(dismissCalled)
    }

    @Test
    fun `showJsConfirmDialog sets confirm state`() = runTest {
        // Given
        val domain = "example.com"
        val message = "Are you sure?"
        var confirmCalled = false
        var cancelCalled = false

        // When
        viewModel.showJsConfirmDialog(
            domain = domain,
            message = message,
            onConfirm = { confirmCalled = true },
            onCancel = { cancelCalled = true }
        )

        // Then
        assertNotNull(viewModel.jsConfirmState.value)
        assertEquals(domain, viewModel.jsConfirmState.value?.domain)
        assertEquals(message, viewModel.jsConfirmState.value?.message)
        assertFalse(confirmCalled)
        assertFalse(cancelCalled)
    }

    @Test
    fun `showJsPromptDialog sets prompt state`() = runTest {
        // Given
        val domain = "example.com"
        val message = "Enter your name"
        val defaultValue = "John"
        var confirmCalled = false
        var inputValue = ""

        // When
        viewModel.showJsPromptDialog(
            domain = domain,
            message = message,
            defaultValue = defaultValue,
            onConfirm = { input ->
                confirmCalled = true
                inputValue = input
            },
            onCancel = {}
        )

        // Then
        assertNotNull(viewModel.jsPromptState.value)
        assertEquals(domain, viewModel.jsPromptState.value?.domain)
        assertEquals(message, viewModel.jsPromptState.value?.message)
        assertEquals(defaultValue, viewModel.jsPromptState.value?.defaultValue)
        assertFalse(confirmCalled)
    }

    // ========== Dialog Spam Prevention Tests ==========

    @Test
    fun `dialog spam prevention blocks after 3 dialogs`() = runTest {
        // Given
        var denyCalls = 0

        // When - Show 4 permission dialogs rapidly
        repeat(4) {
            viewModel.showPermissionRequestDialog(
                domain = "spam.com",
                permissions = listOf(PermissionType.CAMERA),
                onAllow = {},
                onDeny = { denyCalls++ }
            )
        }

        // Then - First 3 should show, 4th should be auto-denied
        assertEquals(1, denyCalls) // 4th dialog was blocked and auto-denied
    }

    @Test
    fun `dialog spam prevention blocks SSL dialogs too`() = runTest {
        // Given
        var goBackCalls = 0
        val sslErrorInfo = SslErrorInfo(
            errorType = SslErrorType.EXPIRED,
            url = "https://spam.com",
            certificateInfo = null,
            primaryError = "Certificate expired"
        )

        // When - Show 4 SSL error dialogs rapidly
        repeat(4) {
            viewModel.showSslErrorDialog(
                sslErrorInfo = sslErrorInfo,
                onGoBack = { goBackCalls++ },
                onProceedAnyway = {}
            )
        }

        // Then - First 3 should show, 4th should be blocked
        assertEquals(1, goBackCalls) // 4th dialog was blocked and auto-called goBack
    }

    @Test
    fun `dialog spam prevention blocks JS alert dialogs`() = runTest {
        // Given
        var dismissCalls = 0

        // When - Show 4 JS alert dialogs rapidly
        repeat(4) {
            viewModel.showJsAlertDialog(
                domain = "spam.com",
                message = "Spam message",
                onDismiss = { dismissCalls++ }
            )
        }

        // Then - First 3 should show, 4th should be blocked
        assertEquals(1, dismissCalls) // 4th dialog was blocked and auto-dismissed
    }

    // ========== Permission Persistence Tests ==========

    @Test
    fun `hasRememberedPermission returns false when no permission stored`() = runTest {
        // When
        val hasPermission = viewModel.hasRememberedPermission(
            domain = "example.com",
            permission = PermissionType.CAMERA
        )

        // Then
        assertFalse(hasPermission) // FakeBrowserRepository always returns null
    }

    // ========== Cleanup Tests ==========

    @Test
    fun `onCleared cancels coroutine scope`() = runTest {
        // When
        viewModel.onCleared()

        // Then - No exception should be thrown
        // CoroutineScope should be cancelled gracefully
    }
}
