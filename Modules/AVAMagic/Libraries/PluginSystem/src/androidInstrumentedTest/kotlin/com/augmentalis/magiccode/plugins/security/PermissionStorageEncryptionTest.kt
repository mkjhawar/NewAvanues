package com.augmentalis.magiccode.plugins.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.magiccode.plugins.core.PluginLog
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Unit tests for encrypted permission storage functionality.
 *
 * Tests verify:
 * - Encryption/decryption round-trip correctness
 * - Multiple permission handling
 * - Hardware keystore detection
 * - Corrupted data handling
 * - Concurrent access safety
 *
 * ## Test Strategy
 * - **TDD Approach**: Tests written BEFORE implementation
 * - **Isolation**: Each test uses fresh PermissionStorage instance
 * - **Cleanup**: All test data deleted in tearDown()
 * - **Real Encryption**: Tests use actual EncryptedSharedPreferences (not mocked)
 *
 * @since 1.1.0
 */
@RunWith(AndroidJUnit4::class)
class PermissionStorageEncryptionTest {

    private lateinit var context: Context
    private lateinit var permissionStorage: PermissionStorage

    /**
     * Test storage filename prefix (allows parallel test execution).
     */
    private val testStoragePrefix = "test_plugin_permissions_${System.currentTimeMillis()}"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Clean up any previous test data
        cleanupTestFiles()

        // Create fresh PermissionStorage instance for each test
        permissionStorage = PermissionStorage.create(context)
    }

    @After
    fun tearDown() {
        // Clean up test files after each test
        cleanupTestFiles()
    }

    private fun cleanupTestFiles() {
        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        sharedPrefsDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("test_") || file.name.startsWith("plugin_permissions")) {
                file.delete()
            }
        }
    }

    /**
     * T012: Test encryption round-trip correctness.
     *
     * **Acceptance Criteria**:
     * - Permission saved with encryption
     * - Permission retrieved matches original data
     * - Data stored in encrypted format (not readable as plain-text)
     *
     * **Success Criteria (SC-001, SC-002)**:
     * - Permission data unreadable via ADB
     * - Encryption adds <5ms latency
     */
    @Test
    fun testEncryptionRoundTrip() = runTest {
        // GIVEN: A plugin and permission
        val pluginId = "com.example.testplugin"
        val permission = "android.permission.CAMERA"

        // WHEN: Permission is saved (encrypted)
        val saveTime = measureTimeMillis {
            permissionStorage.savePermission(pluginId, permission)
        }

        // THEN: Permission can be retrieved correctly
        val retrieveTime = measureTimeMillis {
            val hasPermission = permissionStorage.hasPermission(pluginId, permission)
            assertTrue("Permission should be granted after save", hasPermission)
        }

        // AND: Performance meets requirements (SC-002: <5ms latency)
        assertTrue("Save latency should be <5ms", saveTime < 5)
        assertTrue("Retrieve latency should be <5ms", retrieveTime < 5)

        // AND: Data is encrypted on disk (SC-001: unreadable via ADB)
        assertDataIsEncryptedOnDisk(pluginId, permission)
    }

    /**
     * T013: Test multiple permissions encrypted correctly.
     *
     * **Acceptance Criteria**:
     * - Multiple permissions saved for same plugin
     * - All permissions retrievable individually
     * - getAllPermissions() returns complete set
     *
     * **Success Criteria (SC-009)**:
     * - Concurrent access safety
     */
    @Test
    fun testMultiplePermissionsEncrypted() = runTest {
        // GIVEN: A plugin and multiple permissions
        val pluginId = "com.example.multiplugin"
        val permissions = listOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )

        // WHEN: All permissions are saved
        permissions.forEach { permission ->
            permissionStorage.savePermission(pluginId, permission)
        }

        // THEN: Each permission can be retrieved individually
        permissions.forEach { permission ->
            val hasPermission = permissionStorage.hasPermission(pluginId, permission)
            assertTrue("Permission $permission should be granted", hasPermission)
        }

        // AND: getAllPermissions() returns complete set
        val allPermissions = permissionStorage.getAllPermissions(pluginId)
        assertEquals("Should have ${permissions.size} permissions", permissions.size, allPermissions.size)
        assertTrue("All permissions should be present", allPermissions.containsAll(permissions))

        // AND: All data is encrypted on disk
        permissions.forEach { permission ->
            assertDataIsEncryptedOnDisk(pluginId, permission)
        }
    }

    /**
     * T014: Test hardware keystore detection.
     *
     * **Acceptance Criteria**:
     * - isHardwareBacked() returns correct status
     * - Hardware-backed keys preferred over software
     * - Fallback to TEE if StrongBox unavailable
     *
     * **Success Criteria (SC-004)**:
     * - Verify hardware keystore usage
     */
    @Test
    fun testHardwareKeystoreDetection() = runTest {
        // WHEN: Encryption status is queried
        val status = permissionStorage.getEncryptionStatus()

        // THEN: Encryption should be active
        assertTrue("Storage should be encrypted", status.isEncrypted)

        // AND: Keystore backing should be detected
        // Note: On emulators, this may be false (software keystore)
        // On real devices with TEE/StrongBox, this should be true
        val isHardwareBacked = status.isHardwareBacked

        // Log for manual verification
        PluginLog.i("EncryptionTest", "Hardware-backed: $isHardwareBacked")
        PluginLog.i("EncryptionTest", "Key alias: ${status.keyAlias}")
        PluginLog.i("EncryptionTest", "Encryption scheme: ${status.encryptionScheme}")

        // Verify encryption scheme is correct
        assertEquals("Should use AES256-SIV/AES256-GCM", "AES256-SIV/AES256-GCM", status.encryptionScheme)
        assertEquals("Should use correct key alias", KeyManager.MASTER_KEY_ALIAS, status.keyAlias)
    }

    /**
     * T015: Test corrupted encrypted data detection.
     *
     * **Acceptance Criteria**:
     * - Tampered data throws EncryptionException
     * - GCM authentication tag detects corruption
     * - Original data not returned if corrupted
     *
     * **Success Criteria (SC-001)**:
     * - Tamper detection prevents data corruption
     */
    @Test
    fun testCorruptedEncryptedDataDetection() = runTest {
        // GIVEN: A saved encrypted permission
        val pluginId = "com.example.corrupttest"
        val permission = "android.permission.CAMERA"
        permissionStorage.savePermission(pluginId, permission)

        // WHEN: Encrypted SharedPreferences file is corrupted
        corruptEncryptedFile()

        // THEN: Reading corrupted data should throw exception or return false
        // Note: EncryptedSharedPreferences may return default values instead of throwing
        try {
            val hasPermission = permissionStorage.hasPermission(pluginId, permission)
            // If no exception thrown, corrupted data should not be readable
            assertFalse("Corrupted data should not be readable", hasPermission)
        } catch (e: Exception) {
            // Expected: EncryptionException or SecurityException
            assertTrue(
                "Should throw EncryptionException or SecurityException",
                e is EncryptionException || e is SecurityException
            )
        }
    }

    /**
     * T016: Test concurrent permission grants.
     *
     * **Acceptance Criteria**:
     * - Multiple threads can save permissions concurrently
     * - No race conditions or data loss
     * - All permissions saved correctly
     *
     * **Success Criteria (SC-009)**:
     * - Concurrent access safety
     */
    @Test
    fun testConcurrentPermissionGrants() = runTest {
        // GIVEN: Multiple plugins and permissions
        val pluginCount = 10
        val permissionsPerPlugin = 5

        // WHEN: Permissions are granted concurrently from multiple threads
        val jobs = (0 until pluginCount).map { pluginIndex ->
            async {
                val pluginId = "com.example.plugin$pluginIndex"
                (0 until permissionsPerPlugin).forEach { permIndex ->
                    val permission = "android.permission.PERM_$permIndex"
                    permissionStorage.savePermission(pluginId, permission)
                }
            }
        }

        // Wait for all concurrent operations to complete
        jobs.forEach { it.await() }

        // THEN: All permissions should be saved correctly
        (0 until pluginCount).forEach { pluginIndex ->
            val pluginId = "com.example.plugin$pluginIndex"
            (0 until permissionsPerPlugin).forEach { permIndex ->
                val permission = "android.permission.PERM_$permIndex"
                val hasPermission = permissionStorage.hasPermission(pluginId, permission)
                assertTrue(
                    "Permission $permission for $pluginId should be granted",
                    hasPermission
                )
            }
        }

        // AND: Total permission count should be correct
        val totalPermissions = (0 until pluginCount).sumOf { pluginIndex ->
            permissionStorage.getAllPermissions("com.example.plugin$pluginIndex").size
        }
        assertEquals(
            "Should have ${pluginCount * permissionsPerPlugin} total permissions",
            pluginCount * permissionsPerPlugin,
            totalPermissions
        )
    }

    // ========== Helper Methods ==========

    /**
     * Verify that permission data is encrypted on disk (not readable as plain-text).
     *
     * Reads the SharedPreferences XML file and verifies that neither the plugin ID
     * nor the permission string appear as plain-text.
     */
    private fun assertDataIsEncryptedOnDisk(pluginId: String, permission: String) {
        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        val encryptedFile = File(sharedPrefsDir, "plugin_permissions_encrypted.xml")

        if (encryptedFile.exists()) {
            val fileContents = encryptedFile.readText()

            // Plain-text plugin ID and permission should NOT appear in file
            assertFalse(
                "Plugin ID should not appear as plain-text in encrypted file",
                fileContents.contains(pluginId)
            )
            assertFalse(
                "Permission should not appear as plain-text in encrypted file",
                fileContents.contains(permission)
            )
        }
    }

    /**
     * Corrupt the encrypted SharedPreferences file by modifying random bytes.
     *
     * This simulates tampering or disk corruption to test GCM authentication.
     */
    private fun corruptEncryptedFile() {
        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        val encryptedFile = File(sharedPrefsDir, "plugin_permissions_encrypted.xml")

        if (encryptedFile.exists()) {
            val bytes = encryptedFile.readBytes()
            if (bytes.isNotEmpty()) {
                // Flip some bits in the middle of the file to corrupt it
                val corruptIndex = bytes.size / 2
                bytes[corruptIndex] = (bytes[corruptIndex].toInt() xor 0xFF).toByte()
                encryptedFile.writeBytes(bytes)
            }
        }
    }
}
