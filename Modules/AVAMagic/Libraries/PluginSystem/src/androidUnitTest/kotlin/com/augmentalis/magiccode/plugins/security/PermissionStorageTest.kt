package com.augmentalis.avacode.plugins.security

import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.*

/**
 * Automated unit tests for PermissionStorage.
 *
 * Tests the encrypted permission storage API using a mock implementation:
 * - savePermission()
 * - hasPermission()
 * - getAllPermissions()
 * - revokePermission()
 * - clearAllPermissions()
 * - isEncrypted()
 * - getEncryptionStatus()
 * - migrateToEncrypted()
 *
 * Note: These are unit tests using mock storage. Full encryption tests are in androidInstrumentedTest/.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PermissionStorageTest {

    companion object {
        private const val TEST_PLUGIN_ID = "com.test.plugin"
        private const val CAMERA_PERMISSION = "android.permission.CAMERA"
        private const val LOCATION_PERMISSION = "android.permission.ACCESS_FINE_LOCATION"
        private const val MICROPHONE_PERMISSION = "android.permission.RECORD_AUDIO"
    }

    private lateinit var storage: MockPermissionStorage

    @BeforeTest
    fun setup() {
        storage = MockPermissionStorage()
    }

    /**
     * Test: Basic save and retrieve single permission
     */
    @Test
    fun testSaveAndRetrieveSinglePermission() {
        // Save permission
        storage.savePermission(TEST_PLUGIN_ID, CAMERA_PERMISSION)

        // Verify saved
        assertTrue(storage.hasPermission(TEST_PLUGIN_ID, CAMERA_PERMISSION))

        // Verify in getAllPermissions
        val permissions = storage.getAllPermissions(TEST_PLUGIN_ID)
        assertEquals(1, permissions.size)
        assertTrue(permissions.contains(CAMERA_PERMISSION))
    }

    /**
     * Test: Save multiple permissions for same plugin
     */
    @Test
    fun testSaveMultiplePermissions() {
        // Save three permissions
        storage.savePermission(TEST_PLUGIN_ID, CAMERA_PERMISSION)
        storage.savePermission(TEST_PLUGIN_ID, LOCATION_PERMISSION)
        storage.savePermission(TEST_PLUGIN_ID, MICROPHONE_PERMISSION)

        // Verify all saved
        assertTrue(storage.hasPermission(TEST_PLUGIN_ID, CAMERA_PERMISSION))
        assertTrue(storage.hasPermission(TEST_PLUGIN_ID, LOCATION_PERMISSION))
        assertTrue(storage.hasPermission(TEST_PLUGIN_ID, MICROPHONE_PERMISSION))

        // Verify getAllPermissions returns all three
        val permissions = storage.getAllPermissions(TEST_PLUGIN_ID)
        assertEquals(3, permissions.size)
        assertTrue(permissions.contains(CAMERA_PERMISSION))
        assertTrue(permissions.contains(LOCATION_PERMISSION))
        assertTrue(permissions.contains(MICROPHONE_PERMISSION))
    }

    /**
     * Test: Revoke permission
     */
    @Test
    fun testRevokePermission() {
        // Save permissions
        storage.savePermission(TEST_PLUGIN_ID, CAMERA_PERMISSION)
        storage.savePermission(TEST_PLUGIN_ID, LOCATION_PERMISSION)

        // Revoke camera
        storage.revokePermission(TEST_PLUGIN_ID, CAMERA_PERMISSION)

        // Verify camera revoked
        assertFalse(storage.hasPermission(TEST_PLUGIN_ID, CAMERA_PERMISSION))

        // Verify location still granted
        assertTrue(storage.hasPermission(TEST_PLUGIN_ID, LOCATION_PERMISSION))

        // Verify getAllPermissions only contains location
        val permissions = storage.getAllPermissions(TEST_PLUGIN_ID)
        assertEquals(1, permissions.size)
        assertTrue(permissions.contains(LOCATION_PERMISSION))
    }

    /**
     * Test: Clear all permissions for plugin
     */
    @Test
    fun testClearAllPermissions() {
        // Save multiple permissions
        storage.savePermission(TEST_PLUGIN_ID, CAMERA_PERMISSION)
        storage.savePermission(TEST_PLUGIN_ID, LOCATION_PERMISSION)
        storage.savePermission(TEST_PLUGIN_ID, MICROPHONE_PERMISSION)

        // Clear all
        storage.clearAllPermissions(TEST_PLUGIN_ID)

        // Verify all cleared
        assertFalse(storage.hasPermission(TEST_PLUGIN_ID, CAMERA_PERMISSION))
        assertFalse(storage.hasPermission(TEST_PLUGIN_ID, LOCATION_PERMISSION))
        assertFalse(storage.hasPermission(TEST_PLUGIN_ID, MICROPHONE_PERMISSION))

        // Verify getAllPermissions returns empty
        val permissions = storage.getAllPermissions(TEST_PLUGIN_ID)
        assertTrue(permissions.isEmpty())
    }

    /**
     * Test: isEncrypted() returns correct status
     */
    @Test
    fun testIsEncrypted() {
        // Test encrypted storage
        val encryptedStorage = MockPermissionStorage(isEncrypted = true)
        assertTrue(encryptedStorage.isEncrypted())

        // Test plain-text storage
        val plainStorage = MockPermissionStorage(isEncrypted = false)
        assertFalse(plainStorage.isEncrypted())
    }

    /**
     * Test: getEncryptionStatus() provides details
     */
    @Test
    fun testGetEncryptionStatus() {
        // Test hardware-backed encrypted storage
        val status = storage.getEncryptionStatus()
        assertTrue(status.isEncrypted)
        assertTrue(status.isHardwareBacked)
        assertEquals("AES256-GCM", status.keyAlgorithm)

        // Test plain-text storage
        val plainStorage = MockPermissionStorage(isEncrypted = false, isHardwareBacked = false)
        val plainStatus = plainStorage.getEncryptionStatus()
        assertFalse(plainStatus.isEncrypted)
        assertFalse(plainStatus.isHardwareBacked)
        assertEquals("NONE", plainStatus.keyAlgorithm)
    }

    /**
     * Test: migrateToEncrypted() migration process
     */
    @Test
    fun testMigrateToEncrypted() = runBlocking {
        // Add some permissions before migration
        storage.savePermission(TEST_PLUGIN_ID, CAMERA_PERMISSION)
        storage.savePermission(TEST_PLUGIN_ID, LOCATION_PERMISSION)

        // First migration should succeed
        val result1 = storage.migrateToEncrypted()
        assertTrue(result1 is MigrationResult.Success)
        assertEquals(2, (result1 as MigrationResult.Success).permissionsMigrated)

        // Verify migration complete in status
        val status = storage.getEncryptionStatus()
        assertTrue(status.migrationComplete)

        // Second migration should return AlreadyMigrated
        val result2 = storage.migrateToEncrypted()
        assertTrue(result2 is MigrationResult.AlreadyMigrated)
    }

    /**
     * Test: Idempotent operations
     */
    @Test
    fun testIdempotentOperations() {
        // Save same permission twice
        storage.savePermission(TEST_PLUGIN_ID, CAMERA_PERMISSION)
        storage.savePermission(TEST_PLUGIN_ID, CAMERA_PERMISSION)

        // Should only be stored once
        val permissions = storage.getAllPermissions(TEST_PLUGIN_ID)
        assertEquals(1, permissions.size)

        // Revoke non-existent permission (should not error)
        storage.revokePermission(TEST_PLUGIN_ID, LOCATION_PERMISSION)

        // Clear empty storage (should not error)
        val emptyStorage = MockPermissionStorage()
        emptyStorage.clearAllPermissions("nonexistent.plugin")
    }

    /**
     * Test: Query non-existent permission returns false
     */
    @Test
    fun testQueryNonExistentPermission() {
        assertFalse(storage.hasPermission(TEST_PLUGIN_ID, CAMERA_PERMISSION))
    }

    /**
     * Test: Query non-existent plugin returns empty set
     */
    @Test
    fun testQueryNonExistentPlugin() {
        val permissions = storage.getAllPermissions("nonexistent.plugin")
        assertTrue(permissions.isEmpty())
    }

    /**
     * Test: Multiple plugins with separate permissions
     */
    @Test
    fun testMultiplePlugins() {
        val pluginA = "com.example.pluginA"
        val pluginB = "com.example.pluginB"

        // Grant different permissions to each plugin
        storage.savePermission(pluginA, CAMERA_PERMISSION)
        storage.savePermission(pluginB, LOCATION_PERMISSION)

        // Verify pluginA has camera but not location
        assertTrue(storage.hasPermission(pluginA, CAMERA_PERMISSION))
        assertFalse(storage.hasPermission(pluginA, LOCATION_PERMISSION))

        // Verify pluginB has location but not camera
        assertTrue(storage.hasPermission(pluginB, LOCATION_PERMISSION))
        assertFalse(storage.hasPermission(pluginB, CAMERA_PERMISSION))

        // Verify getAllPermissions for each plugin
        assertEquals(1, storage.getAllPermissions(pluginA).size)
        assertEquals(1, storage.getAllPermissions(pluginB).size)
    }

    /**
     * Test: Error handling - blank pluginId
     */
    @Test
    fun testErrorHandlingBlankPluginId() {
        assertFailsWith<IllegalArgumentException> {
            storage.savePermission("", CAMERA_PERMISSION)
        }
    }

    /**
     * Test: Error handling - blank permission
     */
    @Test
    fun testErrorHandlingBlankPermission() {
        assertFailsWith<IllegalArgumentException> {
            storage.savePermission(TEST_PLUGIN_ID, "")
        }
    }
}
