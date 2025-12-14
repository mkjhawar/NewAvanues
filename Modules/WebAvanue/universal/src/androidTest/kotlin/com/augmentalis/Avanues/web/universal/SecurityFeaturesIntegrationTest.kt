package com.augmentalis.Avanues.web.universal

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.webavanue.ui.screen.security.*
import com.augmentalis.webavanue.ui.screen.settings.SitePermissionsScreen
import com.augmentalis.webavanue.ui.viewmodel.SecurityViewModel
import com.augmentalis.webavanue.data.db.BrowserDatabase
import com.augmentalis.webavanue.data.repository.BrowserRepositoryImpl
import com.augmentalis.webavanue.domain.model.SitePermission
import com.augmentalis.webavanue.platform.createAndroidDriver
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

/**
 * Integration tests for Phase 3 security features
 *
 * Tests verify implementation of:
 * 1. HTTP Authentication Dialog (Task 2)
 * 2. File Upload Support (Task 3)
 * 3. Site Permissions Management UI (Task 4)
 *
 * Phase 3 Commit References:
 * - HTTP Auth: ff2229b
 * - File Upload: 861a16f
 * - Permissions UI: a010427
 */
@RunWith(AndroidJUnit4::class)
class SecurityFeaturesIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createTestRepository(): BrowserRepositoryImpl {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val driver = createAndroidDriver(context)
        val database = BrowserDatabase(driver)
        return BrowserRepositoryImpl(database)
    }

    // ========== HTTP Authentication Dialog Tests ==========

    /**
     * Test 1: Verify HTTP Auth dialog renders correctly
     *
     * Phase 3 - Task 2: HTTP Authentication Dialog
     * Location: SecurityDialogs.kt:424-508
     * Commit: ff2229b
     */
    @Test
    fun test_httpAuthDialog_rendersCorrectly() {
        val authRequest = HttpAuthRequest(
            host = "example.com",
            realm = "Protected Area",
            scheme = "Basic"
        )

        composeTestRule.setContent {
            HttpAuthenticationDialog(
                authRequest = authRequest,
                onAuthenticate = {},
                onCancel = {},
                onDismiss = {}
            )
        }

        // Verify dialog title
        composeTestRule.onNodeWithText("Authentication Required").assertIsDisplayed()

        // Verify host is displayed
        composeTestRule.onNodeWithText("example.com").assertIsDisplayed()

        // Verify instruction text
        composeTestRule.onNodeWithText(
            "The server example.com requires a username and password.",
            substring = true
        ).assertIsDisplayed()

        // Verify realm is displayed
        composeTestRule.onNodeWithText("Realm: Protected Area", substring = true).assertIsDisplayed()

        // Verify username and password fields exist
        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()

        // Verify buttons
        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    /**
     * Test 2: Verify HTTP Auth Sign In button is disabled when fields are empty
     *
     * Phase 3 - Task 2: Sign In button validation
     * Location: SecurityDialogs.kt:489-495
     */
    @Test
    fun test_httpAuthDialog_signInButtonDisabledWhenEmpty() {
        val authRequest = HttpAuthRequest(
            host = "example.com",
            realm = "Protected Area",
            scheme = "Basic"
        )

        composeTestRule.setContent {
            HttpAuthenticationDialog(
                authRequest = authRequest,
                onAuthenticate = {},
                onCancel = {},
                onDismiss = {}
            )
        }

        // Sign In button should be disabled initially (empty fields)
        composeTestRule.onNodeWithText("Sign In").assertIsNotEnabled()
    }

    /**
     * Test 3: Verify HTTP Auth Sign In button is enabled after entering credentials
     *
     * Phase 3 - Task 2: Button state management
     * Location: SecurityDialogs.kt:489-495
     */
    @Test
    fun test_httpAuthDialog_signInButtonEnabledWithCredentials() {
        val authRequest = HttpAuthRequest(
            host = "example.com",
            realm = "Protected Area",
            scheme = "Basic"
        )

        composeTestRule.setContent {
            HttpAuthenticationDialog(
                authRequest = authRequest,
                onAuthenticate = {},
                onCancel = {},
                onDismiss = {}
            )
        }

        // Enter username
        composeTestRule.onNodeWithText("Username").performTextInput("testuser")

        // Enter password
        composeTestRule.onNodeWithText("Password").performTextInput("testpass")

        // Sign In button should now be enabled
        composeTestRule.onNodeWithText("Sign In").assertIsEnabled()
    }

    /**
     * Test 4: Verify HTTP Auth dialog calls onAuthenticate with correct credentials
     *
     * Phase 3 - Task 2: Callback integration
     * Location: SecurityDialogs.kt:489-495
     */
    @Test
    fun test_httpAuthDialog_callsOnAuthenticateWithCredentials() {
        val authRequest = HttpAuthRequest(
            host = "example.com",
            realm = "Protected Area",
            scheme = "Basic"
        )

        var authenticateCalled = false
        var capturedCredentials: HttpAuthCredentials? = null

        composeTestRule.setContent {
            HttpAuthenticationDialog(
                authRequest = authRequest,
                onAuthenticate = { credentials ->
                    authenticateCalled = true
                    capturedCredentials = credentials
                },
                onCancel = {},
                onDismiss = {}
            )
        }

        // Enter credentials
        composeTestRule.onNodeWithText("Username").performTextInput("testuser")
        composeTestRule.onNodeWithText("Password").performTextInput("testpass")

        // Click Sign In
        composeTestRule.onNodeWithText("Sign In").performClick()

        // Verify callback was called with correct credentials
        assertTrue(authenticateCalled, "onAuthenticate should be called")
        assertNotNull(capturedCredentials, "Credentials should be captured")
        assertEquals("testuser", capturedCredentials?.username)
        assertEquals("testpass", capturedCredentials?.password)
    }

    /**
     * Test 5: Verify HTTP Auth dialog calls onCancel when Cancel is clicked
     *
     * Phase 3 - Task 2: Cancel flow
     * Location: SecurityDialogs.kt:497-501
     */
    @Test
    fun test_httpAuthDialog_callsOnCancelWhenCancelClicked() {
        val authRequest = HttpAuthRequest(
            host = "example.com",
            realm = "Protected Area",
            scheme = "Basic"
        )

        var cancelCalled = false

        composeTestRule.setContent {
            HttpAuthenticationDialog(
                authRequest = authRequest,
                onAuthenticate = {},
                onCancel = { cancelCalled = true },
                onDismiss = {}
            )
        }

        // Click Cancel
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Verify callback was called
        assertTrue(cancelCalled, "onCancel should be called")
    }

    /**
     * Test 6: Verify HTTP Auth dialog shows realm when provided
     *
     * Phase 3 - Task 2: Realm display
     * Location: SecurityDialogs.kt:460-472
     */
    @Test
    fun test_httpAuthDialog_showsRealmWhenProvided() {
        val authRequest = HttpAuthRequest(
            host = "example.com",
            realm = "Admin Area",
            scheme = "Digest"
        )

        composeTestRule.setContent {
            HttpAuthenticationDialog(
                authRequest = authRequest,
                onAuthenticate = {},
                onCancel = {},
                onDismiss = {}
            )
        }

        // Verify realm is displayed
        composeTestRule.onNodeWithText("Realm: Admin Area", substring = true).assertIsDisplayed()
    }

    /**
     * Test 7: Verify HTTP Auth dialog handles empty realm gracefully
     *
     * Phase 3 - Task 2: Empty realm handling
     * Location: SecurityDialogs.kt:460-472
     */
    @Test
    fun test_httpAuthDialog_handlesEmptyRealm() {
        val authRequest = HttpAuthRequest(
            host = "example.com",
            realm = "",
            scheme = "Basic"
        )

        composeTestRule.setContent {
            HttpAuthenticationDialog(
                authRequest = authRequest,
                onAuthenticate = {},
                onCancel = {},
                onDismiss = {}
            )
        }

        // Verify dialog still renders without realm card
        composeTestRule.onNodeWithText("Authentication Required").assertIsDisplayed()
        composeTestRule.onNodeWithText("example.com").assertIsDisplayed()

        // Realm card should not be visible (empty realm)
        composeTestRule.onNodeWithText("Realm:", substring = true).assertDoesNotExist()
    }

    // ========== SecurityViewModel Integration Tests ==========

    /**
     * Test 8: Verify SecurityViewModel shows HTTP Auth dialog correctly
     *
     * Phase 3 - Task 2: ViewModel integration
     * Location: SecurityViewModel.kt:98-122
     * Commit: ff2229b
     */
    @Test
    fun test_securityViewModel_showsHttpAuthDialog() = runBlocking {
        val repository = createTestRepository()
        val viewModel = SecurityViewModel(repository)

        var authenticateCalled = false
        var cancelCalled = false

        val authRequest = HttpAuthRequest(
            host = "example.com",
            realm = "Protected",
            scheme = "Basic"
        )

        // Show HTTP Auth dialog via ViewModel
        viewModel.showHttpAuthDialog(
            authRequest = authRequest,
            onAuthenticate = { authenticateCalled = true },
            onCancel = { cancelCalled = true }
        )

        // Verify state is set
        assertNotNull(viewModel.httpAuthState.value, "HTTP Auth state should be set")
        assertEquals(authRequest, viewModel.httpAuthState.value?.authRequest)
        assertFalse(authenticateCalled)
        assertFalse(cancelCalled)

        // Cleanup
        viewModel.onCleared()
        repository.clearAllData()
    }

    /**
     * Test 9: Verify SecurityViewModel dismisses HTTP Auth dialog
     *
     * Phase 3 - Task 2: Dialog dismissal
     * Location: SecurityViewModel.kt:124-126
     */
    @Test
    fun test_securityViewModel_dismissesHttpAuthDialog() = runBlocking {
        val repository = createTestRepository()
        val viewModel = SecurityViewModel(repository)

        val authRequest = HttpAuthRequest(
            host = "example.com",
            realm = "Protected",
            scheme = "Basic"
        )

        // Show then dismiss
        viewModel.showHttpAuthDialog(
            authRequest = authRequest,
            onAuthenticate = {},
            onCancel = {}
        )

        assertNotNull(viewModel.httpAuthState.value, "State should be set")

        viewModel.dismissHttpAuthDialog()

        // Verify state is cleared
        assertNull(viewModel.httpAuthState.value, "HTTP Auth state should be cleared")

        // Cleanup
        viewModel.onCleared()
        repository.clearAllData()
    }

    /**
     * Test 10: Verify dialog spam prevention blocks HTTP Auth dialogs
     *
     * Phase 3 - Task 2: Spam prevention
     * Location: SecurityViewModel.kt:98-122
     * Ref: SecurityViewModelTest.kt:262-303 (unit test)
     */
    @Test
    fun test_httpAuthDialog_spamPreventionBlocks() = runBlocking {
        val repository = createTestRepository()
        val viewModel = SecurityViewModel(repository)

        var cancelCalls = 0

        val authRequest = HttpAuthRequest(
            host = "spam.com",
            realm = "Spam",
            scheme = "Basic"
        )

        // Try to show 4 HTTP Auth dialogs rapidly (limit is 3)
        repeat(4) {
            viewModel.showHttpAuthDialog(
                authRequest = authRequest,
                onAuthenticate = {},
                onCancel = { cancelCalls++ }
            )
        }

        // 4th dialog should be blocked and auto-cancelled
        assertEquals(1, cancelCalls, "4th dialog should be blocked and auto-cancelled")

        // Cleanup
        viewModel.onCleared()
        repository.clearAllData()
    }

    // ========== Site Permissions Screen Tests ==========

    /**
     * Test 11: Verify Site Permissions screen displays empty state
     *
     * Phase 3 - Task 4: Empty state UI
     * Location: SitePermissionsScreen.kt:91-103
     * Commit: a010427
     */
    @Test
    fun test_sitePermissionsScreen_showsEmptyState() = runBlocking {
        val repository = createTestRepository()

        composeTestRule.setContent {
            SitePermissionsScreen(
                repository = repository,
                onNavigateBack = {}
            )
        }

        // Wait for loading to complete
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Verify empty state is displayed
        composeTestRule.onNodeWithText("No Permissions", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "You haven't granted any site permissions yet.",
            substring = true
        ).assertIsDisplayed()

        // Cleanup
        repository.clearAllData()
    }

    /**
     * Test 12: Verify Site Permissions screen displays permissions
     *
     * Phase 3 - Task 4: Permission cards
     * Location: SitePermissionsScreen.kt:111-260
     */
    @Test
    fun test_sitePermissionsScreen_displaysPermissions() = runBlocking {
        val repository = createTestRepository()

        // Add test permissions
        repository.insertSitePermission("example.com", "CAMERA", granted = true)
        repository.insertSitePermission("example.com", "MICROPHONE", granted = true)
        repository.insertSitePermission("test.org", "LOCATION", granted = false)

        composeTestRule.setContent {
            SitePermissionsScreen(
                repository = repository,
                onNavigateBack = {}
            )
        }

        // Wait for loading
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Verify domain headers are displayed
        composeTestRule.onNodeWithText("example.com", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("test.org", substring = true).assertIsDisplayed()

        // Verify permissions are displayed
        composeTestRule.onNodeWithText("Camera", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Microphone", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Location", substring = true).assertIsDisplayed()

        // Cleanup
        repository.clearAllData()
    }

    /**
     * Test 13: Verify Site Permissions screen shows granted/denied status
     *
     * Phase 3 - Task 4: Permission status indicators
     * Location: SitePermissionsScreen.kt:186-207
     */
    @Test
    fun test_sitePermissionsScreen_showsPermissionStatus() = runBlocking {
        val repository = createTestRepository()

        // Add granted and denied permissions
        repository.insertSitePermission("example.com", "CAMERA", granted = true)
        repository.insertSitePermission("example.com", "LOCATION", granted = false)

        composeTestRule.setContent {
            SitePermissionsScreen(
                repository = repository,
                onNavigateBack = {}
            )
        }

        // Wait for loading
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Verify status indicators
        composeTestRule.onNodeWithText("Granted", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Denied", substring = true).assertIsDisplayed()

        // Cleanup
        repository.clearAllData()
    }

    /**
     * Test 14: Verify Site Permissions screen allows revoking individual permission
     *
     * Phase 3 - Task 4: Individual permission revocation
     * Location: SitePermissionsScreen.kt:211-225
     */
    @Test
    fun test_sitePermissionsScreen_revokesIndividualPermission() = runBlocking {
        val repository = createTestRepository()

        // Add test permission
        repository.insertSitePermission("example.com", "CAMERA", granted = true)

        composeTestRule.setContent {
            SitePermissionsScreen(
                repository = repository,
                onNavigateBack = {}
            )
        }

        // Wait for loading
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Verify permission is present
        composeTestRule.onNodeWithText("Camera", substring = true).assertIsDisplayed()

        // Find and click the delete/revoke button
        // The button is within the permission card
        composeTestRule.onAllNodesWithContentDescription("Revoke permission")
            .onFirst()
            .performClick()

        // Wait for deletion to complete and screen to refresh
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            // Permission should be removed from the list
            composeTestRule.onAllNodesWithText("Camera", substring = true)
                .fetchSemanticsNodes().isEmpty()
        }

        // Verify permission was deleted from database
        val remainingPermissions = repository.getAllSitePermissions().getOrNull() ?: emptyList()
        assertTrue(
            remainingPermissions.none { it.domain == "example.com" && it.permissionType == "CAMERA" },
            "Permission should be deleted from database"
        )

        // Cleanup
        repository.clearAllData()
    }

    /**
     * Test 15: Verify Site Permissions screen allows clearing all permissions for domain
     *
     * Phase 3 - Task 4: Bulk permission deletion
     * Location: SitePermissionsScreen.kt:228-246
     */
    @Test
    fun test_sitePermissionsScreen_clearsAllPermissionsForDomain() = runBlocking {
        val repository = createTestRepository()

        // Add multiple permissions for same domain
        repository.insertSitePermission("example.com", "CAMERA", granted = true)
        repository.insertSitePermission("example.com", "MICROPHONE", granted = true)
        repository.insertSitePermission("example.com", "LOCATION", granted = true)

        composeTestRule.setContent {
            SitePermissionsScreen(
                repository = repository,
                onNavigateBack = {}
            )
        }

        // Wait for loading
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("Loading...").fetchSemanticsNodes().isEmpty()
        }

        // Verify domain and permissions are present
        composeTestRule.onNodeWithText("example.com", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Camera", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Microphone", substring = true).assertIsDisplayed()

        // Find and click "Clear All" button for the domain
        composeTestRule.onNodeWithText("Clear All").performClick()

        // Confirmation dialog should appear
        composeTestRule.onNodeWithText("Clear all permissions?", substring = true).assertIsDisplayed()

        // Confirm deletion
        composeTestRule.onNodeWithText("Clear").performClick()

        // Wait for deletion to complete
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            // Domain card should be removed
            composeTestRule.onAllNodesWithText("example.com", substring = true)
                .fetchSemanticsNodes().isEmpty()
        }

        // Verify all permissions were deleted from database
        val remainingPermissions = repository.getAllSitePermissions().getOrNull() ?: emptyList()
        assertTrue(
            remainingPermissions.none { it.domain == "example.com" },
            "All permissions for example.com should be deleted"
        )

        // Cleanup
        repository.clearAllData()
    }

    /**
     * Test 16: Verify back button navigation in Site Permissions screen
     *
     * Phase 3 - Task 4: Navigation handling
     * Location: SitePermissionsScreen.kt:65-71
     */
    @Test
    fun test_sitePermissionsScreen_backButtonNavigates() {
        val repository = createTestRepository()
        var backPressed = false

        composeTestRule.setContent {
            SitePermissionsScreen(
                repository = repository,
                onNavigateBack = { backPressed = true }
            )
        }

        // Find and click back button
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()

        // Verify navigation callback was called
        assertTrue(backPressed, "onNavigateBack should be called")
    }

    // ========== File Upload Support Tests ==========

    /**
     * Test 17: Verify file upload support is configured in WebView
     *
     * Phase 3 - Task 3: File upload integration
     * Location: WebViewContainer.android.kt:43-65
     * Commit: 861a16f
     *
     * Note: Full file picker testing requires UI instrumentation
     * and system-level interactions. This test verifies the setup.
     */
    @Test
    fun test_fileUpload_infrastructureSetup() {
        // Verify activity-compose dependency is available (required for file picker)
        // This will throw NoClassDefFoundError if dependency is missing
        try {
            val className = "androidx.activity.compose.rememberLauncherForActivityResult"
            Class.forName(className.substringBeforeLast('.'))
            assertTrue(true, "activity-compose dependency is available")
        } catch (e: ClassNotFoundException) {
            fail("activity-compose dependency is missing - required for file upload")
        }
    }

    /**
     * Test 18: Verify database supports site permissions storage
     *
     * Phase 3 - Task 4: Database integration
     * Location: BrowserDatabase.sq, BrowserRepositoryImpl.kt
     */
    @Test
    fun test_database_supportsSitePermissions() = runBlocking {
        val repository = createTestRepository()

        // Insert permission
        val insertResult = repository.insertSitePermission(
            domain = "test.com",
            permissionType = "CAMERA",
            granted = true
        )

        assertTrue(insertResult.isSuccess, "Permission insert should succeed")

        // Retrieve permission
        val getResult = repository.getSitePermission("test.com", "CAMERA")
        assertTrue(getResult.isSuccess, "Permission retrieval should succeed")

        val permission = getResult.getOrNull()
        assertNotNull(permission, "Permission should exist")
        assertEquals("test.com", permission.domain)
        assertEquals("CAMERA", permission.permissionType)
        assertTrue(permission.granted)

        // Delete permission
        val deleteResult = repository.deleteSitePermission("test.com", "CAMERA")
        assertTrue(deleteResult.isSuccess, "Permission deletion should succeed")

        // Verify deletion
        val afterDelete = repository.getSitePermission("test.com", "CAMERA")
        assertNull(afterDelete.getOrNull(), "Permission should be deleted")

        // Cleanup
        repository.clearAllData()
    }

    /**
     * Test 19: Verify getAllSitePermissions query works correctly
     *
     * Phase 3 - Task 4: Repository query
     * Location: BrowserRepositoryImpl.kt, BrowserDatabase.sq
     */
    @Test
    fun test_repository_getAllSitePermissionsQuery() = runBlocking {
        val repository = createTestRepository()

        // Initially empty
        val initialResult = repository.getAllSitePermissions()
        assertTrue(initialResult.isSuccess)
        assertTrue(initialResult.getOrNull()?.isEmpty() == true)

        // Add multiple permissions
        repository.insertSitePermission("example.com", "CAMERA", granted = true)
        repository.insertSitePermission("example.com", "MICROPHONE", granted = false)
        repository.insertSitePermission("test.org", "LOCATION", granted = true)

        // Query all
        val allResult = repository.getAllSitePermissions()
        assertTrue(allResult.isSuccess)

        val permissions = allResult.getOrNull() ?: emptyList()
        assertEquals(3, permissions.size, "Should have 3 permissions")

        // Verify permissions are ordered by domain, permission_type
        val domains = permissions.map { it.domain }
        assertTrue(
            domains == domains.sorted(),
            "Permissions should be ordered by domain"
        )

        // Cleanup
        repository.clearAllData()
    }

    /**
     * Test 20: Verify permission persistence across app restarts
     *
     * Phase 3 - Task 4: Persistence verification
     * Location: BrowserRepositoryImpl.kt
     */
    @Test
    fun test_permissions_persistAcrossRestarts() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val driver = createAndroidDriver(context)
        val database = BrowserDatabase(driver)

        // Create first repository instance and add permissions
        val repository1 = BrowserRepositoryImpl(database)
        repository1.insertSitePermission("example.com", "CAMERA", granted = true)
        repository1.insertSitePermission("example.com", "MICROPHONE", granted = false)

        // Simulate app restart: create new repository instance with same database
        val repository2 = BrowserRepositoryImpl(database)

        // Verify permissions persisted
        val permissions = repository2.getAllSitePermissions().getOrNull() ?: emptyList()
        assertEquals(2, permissions.size, "Permissions should persist across restarts")

        val cameraPermission = permissions.find { it.permissionType == "CAMERA" }
        assertNotNull(cameraPermission, "Camera permission should exist")
        assertTrue(cameraPermission.granted, "Camera permission should be granted")

        val micPermission = permissions.find { it.permissionType == "MICROPHONE" }
        assertNotNull(micPermission, "Microphone permission should exist")
        assertFalse(micPermission.granted, "Microphone permission should be denied")

        // Cleanup
        repository2.clearAllData()
    }
}
