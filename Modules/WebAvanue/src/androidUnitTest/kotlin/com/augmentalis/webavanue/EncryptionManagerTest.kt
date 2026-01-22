package com.augmentalis.webavanue

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for EncryptionManager
 *
 * Tests encryption key management for SQLCipher database encryption.
 * Uses Robolectric for Android Keystore simulation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EncryptionManagerTest {

    private lateinit var context: Context
    private lateinit var encryptionManager: EncryptionManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        encryptionManager = EncryptionManager(context)
    }

    @After
    fun cleanup() {
        // Clean up encryption keys after each test
        try {
            encryptionManager.deleteEncryptionKey()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    @Test
    fun `getOrCreateDatabasePassphrase creates new passphrase on first call`() {
        // GIVEN: No existing passphrase
        assertFalse(encryptionManager.hasEncryptionKey())

        // WHEN: Getting or creating passphrase
        val passphrase = encryptionManager.getOrCreateDatabasePassphrase()

        // THEN: Passphrase is created
        assertNotNull(passphrase)
        assertEquals(32, passphrase.size) // 256 bits = 32 bytes
        assertTrue(encryptionManager.hasEncryptionKey())
    }

    @Test
    fun `getOrCreateDatabasePassphrase returns same passphrase on subsequent calls`() {
        // GIVEN: Initial passphrase created
        val passphrase1 = encryptionManager.getOrCreateDatabasePassphrase()

        // WHEN: Getting passphrase again
        val passphrase2 = encryptionManager.getOrCreateDatabasePassphrase()

        // THEN: Same passphrase is returned
        assertArrayEquals(passphrase1, passphrase2)
    }

    @Test
    fun `rotateEncryptionKey generates new passphrase`() {
        // GIVEN: Initial passphrase
        val oldPassphrase = encryptionManager.getOrCreateDatabasePassphrase()

        // WHEN: Rotating key
        val newPassphrase = encryptionManager.rotateEncryptionKey()

        // THEN: New passphrase is different
        assertFalse(oldPassphrase.contentEquals(newPassphrase))
        assertEquals(32, newPassphrase.size)

        // AND: New passphrase is persisted
        val retrievedPassphrase = encryptionManager.getOrCreateDatabasePassphrase()
        assertArrayEquals(newPassphrase, retrievedPassphrase)
    }

    @Test
    fun `deleteEncryptionKey removes passphrase`() {
        // GIVEN: Passphrase exists
        encryptionManager.getOrCreateDatabasePassphrase()
        assertTrue(encryptionManager.hasEncryptionKey())

        // WHEN: Deleting key
        encryptionManager.deleteEncryptionKey()

        // THEN: Key is removed
        assertFalse(encryptionManager.hasEncryptionKey())
    }

    @Test
    fun `passphrase survives app restart simulation`() {
        // GIVEN: Initial passphrase
        val passphrase1 = encryptionManager.getOrCreateDatabasePassphrase()

        // WHEN: Simulating app restart (creating new EncryptionManager instance)
        val newEncryptionManager = EncryptionManager(context)
        val passphrase2 = newEncryptionManager.getOrCreateDatabasePassphrase()

        // THEN: Same passphrase is retrieved
        assertArrayEquals(passphrase1, passphrase2)
    }

    @Test
    fun `passphrase is cryptographically random`() {
        // GIVEN: Multiple passphrases
        val manager1 = EncryptionManager(context)
        val passphrase1 = manager1.getOrCreateDatabasePassphrase()
        manager1.deleteEncryptionKey()

        val manager2 = EncryptionManager(context)
        val passphrase2 = manager2.getOrCreateDatabasePassphrase()
        manager2.deleteEncryptionKey()

        // THEN: Passphrases are different (statistically impossible to be same if random)
        assertFalse(passphrase1.contentEquals(passphrase2))
    }

    @Test
    fun `passphrase meets security requirements`() {
        // GIVEN: New passphrase
        val passphrase = encryptionManager.getOrCreateDatabasePassphrase()

        // THEN: Passphrase meets security requirements
        assertEquals(32, passphrase.size) // 256 bits for AES-256

        // AND: Passphrase contains varied bytes (not all zeros or all same)
        val uniqueBytes = passphrase.toSet()
        assertTrue("Passphrase should have varied bytes", uniqueBytes.size > 10)
    }
}
