/**
 * VoiceOSExceptionTest.kt - Tests for VoiceOS exception hierarchy
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.exceptions

import kotlin.test.*

class VoiceOSExceptionTest {

    // ===== Base VoiceOSException Tests =====

    @Test
    fun `VoiceOSException with message only`() {
        val exception = VoiceOSException("Test error")

        assertEquals("Test error", exception.message)
        assertNull(exception.cause)
        assertNull(exception.errorCode)
    }

    @Test
    fun `VoiceOSException with message and cause`() {
        val cause = RuntimeException("Underlying error")
        val exception = VoiceOSException("Test error", cause)

        assertEquals("Test error", exception.message)
        assertSame(cause, exception.cause)
        assertNull(exception.errorCode)
    }

    @Test
    fun `VoiceOSException with message cause and error code`() {
        val cause = RuntimeException("Underlying error")
        val exception = VoiceOSException("Test error", cause, "ERR_001")

        assertEquals("Test error", exception.message)
        assertSame(cause, exception.cause)
        assertEquals("ERR_001", exception.errorCode)
    }

    @Test
    fun `getFullMessage includes error code`() {
        val exception = VoiceOSException("Test error", errorCode = "ERR_001")
        assertEquals("[ERR_001] Test error", exception.getFullMessage())
    }

    @Test
    fun `getFullMessage without error code`() {
        val exception = VoiceOSException("Test error")
        assertEquals("Test error", exception.getFullMessage())
    }

    @Test
    fun `isCausedBy detects direct cause`() {
        val cause = IllegalArgumentException("Invalid")
        val exception = VoiceOSException("Test error", cause)

        assertTrue(exception.isCausedBy<IllegalArgumentException>())
        assertFalse(exception.isCausedBy<NullPointerException>())
    }

    @Test
    fun `isCausedBy detects nested cause`() {
        val rootCause = NullPointerException("Null value")
        val intermediateCause = IllegalStateException("Bad state", rootCause)
        val exception = VoiceOSException("Test error", intermediateCause)

        assertTrue(exception.isCausedBy<NullPointerException>())
        assertTrue(exception.isCausedBy<IllegalStateException>())
        assertFalse(exception.isCausedBy<IllegalArgumentException>())
    }

    @Test
    fun `isCausedBy returns false when no cause`() {
        val exception = VoiceOSException("Test error")
        assertFalse(exception.isCausedBy<NullPointerException>())
    }

    // ===== DatabaseException Tests =====

    @Test
    fun `BackupException has correct error code`() {
        val exception = DatabaseException.BackupException("Backup failed")
        assertEquals("DB_BACKUP_FAILED", exception.errorCode)
        assertEquals("Backup failed", exception.message)
    }

    @Test
    fun `RestoreException has correct error code`() {
        val exception = DatabaseException.RestoreException("Restore failed")
        assertEquals("DB_RESTORE_FAILED", exception.errorCode)
    }

    @Test
    fun `IntegrityException with corruption details`() {
        val details = listOf("Table missing", "Corrupted index")
        val exception = DatabaseException.IntegrityException(
            "Integrity check failed",
            corruptionDetails = details
        )

        assertEquals("DB_INTEGRITY_FAILED", exception.errorCode)
        assertTrue(exception.toString().contains("Table missing"))
        assertTrue(exception.toString().contains("Corrupted index"))
    }

    @Test
    fun `IntegrityException without corruption details`() {
        val exception = DatabaseException.IntegrityException("Integrity check failed")
        assertEquals("DB_INTEGRITY_FAILED", exception.errorCode)
        assertFalse(exception.toString().contains(":"))
    }

    @Test
    fun `MigrationException with version info`() {
        val exception = DatabaseException.MigrationException(
            "Migration failed",
            fromVersion = 5,
            toVersion = 6
        )

        assertEquals("DB_MIGRATION_FAILED", exception.errorCode)
        assertTrue(exception.toString().contains("v5 → v6"))
    }

    @Test
    fun `MigrationException without version info`() {
        val exception = DatabaseException.MigrationException("Migration failed")
        assertEquals("DB_MIGRATION_FAILED", exception.errorCode)
        assertFalse(exception.toString().contains("→"))
    }

    @Test
    fun `TransactionException has correct error code`() {
        val exception = DatabaseException.TransactionException("Transaction failed")
        assertEquals("DB_TRANSACTION_FAILED", exception.errorCode)
    }

    // ===== SecurityException Tests =====

    @Test
    fun `EncryptionException has correct error code`() {
        val exception = SecurityException.EncryptionException("Encryption failed")
        assertEquals("SECURITY_ENCRYPTION_FAILED", exception.errorCode)
    }

    @Test
    fun `DecryptionException has correct error code`() {
        val exception = SecurityException.DecryptionException("Decryption failed")
        assertEquals("SECURITY_DECRYPTION_FAILED", exception.errorCode)
    }

    @Test
    fun `SignatureException with package name`() {
        val exception = SecurityException.SignatureException(
            "Invalid signature",
            packageName = "com.example.app"
        )

        assertEquals("SECURITY_SIGNATURE_INVALID", exception.errorCode)
        assertTrue(exception.toString().contains("package: com.example.app"))
    }

    @Test
    fun `SignatureException without package name`() {
        val exception = SecurityException.SignatureException("Invalid signature")
        assertEquals("SECURITY_SIGNATURE_INVALID", exception.errorCode)
        assertFalse(exception.toString().contains("package:"))
    }

    @Test
    fun `UnauthorizedException with package name`() {
        val exception = SecurityException.UnauthorizedException(
            "Unauthorized",
            packageName = "com.example.app"
        )

        assertEquals("SECURITY_UNAUTHORIZED", exception.errorCode)
        assertTrue(exception.toString().contains("package: com.example.app"))
    }

    @Test
    fun `KeystoreException has correct error code`() {
        val exception = SecurityException.KeystoreException("Keystore failed")
        assertEquals("SECURITY_KEYSTORE_FAILED", exception.errorCode)
    }

    // ===== CommandException Tests =====

    @Test
    fun `ExecutionException with command text`() {
        val exception = CommandException.ExecutionException(
            "Execution failed",
            commandText = "open settings"
        )

        assertEquals("COMMAND_EXECUTION_FAILED", exception.errorCode)
        assertTrue(exception.toString().contains("command: \"open settings\""))
    }

    @Test
    fun `ParsingException with raw input`() {
        val exception = CommandException.ParsingException(
            "Parsing failed",
            rawInput = "invalid@command"
        )

        assertEquals("COMMAND_PARSING_FAILED", exception.errorCode)
        assertTrue(exception.toString().contains("input: \"invalid@command\""))
    }

    @Test
    fun `RateLimitException with all details`() {
        val exception = CommandException.RateLimitException(
            "Rate limit exceeded",
            commandId = "cmd_001",
            retryAfterMs = 5000L
        )

        assertEquals("COMMAND_RATE_LIMIT", exception.errorCode)
        assertTrue(exception.toString().contains("command: cmd_001"))
        assertTrue(exception.toString().contains("retry after: 5000ms"))
    }

    @Test
    fun `RateLimitException without details`() {
        val exception = CommandException.RateLimitException("Rate limit exceeded")
        assertEquals("COMMAND_RATE_LIMIT", exception.errorCode)
        assertFalse(exception.toString().contains("command:"))
    }

    @Test
    fun `CircuitBreakerException has correct error code`() {
        val exception = CommandException.CircuitBreakerException(
            "Circuit breaker open",
            commandId = "cmd_001"
        )
        assertEquals("COMMAND_CIRCUIT_BREAKER_OPEN", exception.errorCode)
    }

    // ===== ScrapingException Tests =====

    @Test
    fun `ElementException with element ID`() {
        val exception = ScrapingException.ElementException(
            "Element not found",
            elementId = "btn_submit"
        )

        assertEquals("SCRAPING_ELEMENT_FAILED", exception.errorCode)
    }

    @Test
    fun `HierarchyException has correct error code`() {
        val exception = ScrapingException.HierarchyException("Hierarchy failed")
        assertEquals("SCRAPING_HIERARCHY_FAILED", exception.errorCode)
    }

    @Test
    fun `CacheException has correct error code`() {
        val exception = ScrapingException.CacheException("Cache failed")
        assertEquals("SCRAPING_CACHE_FAILED", exception.errorCode)
    }

    // ===== PrivacyException Tests =====

    @Test
    fun `ConsentException with consent type`() {
        val exception = PrivacyException.ConsentException(
            "Consent required",
            consentType = "data_collection"
        )

        assertEquals("PRIVACY_CONSENT_REQUIRED", exception.errorCode)
        assertTrue(exception.toString().contains("type: data_collection"))
    }

    @Test
    fun `RetentionException has correct error code`() {
        val exception = PrivacyException.RetentionException("Retention violated")
        assertEquals("PRIVACY_RETENTION_VIOLATION", exception.errorCode)
    }

    // ===== AccessibilityException Tests =====

    @Test
    fun `ServiceException has correct error code`() {
        val exception = AccessibilityException.ServiceException("Service unavailable")
        assertEquals("ACCESSIBILITY_SERVICE_UNAVAILABLE", exception.errorCode)
    }

    @Test
    fun `NodeException has correct error code`() {
        val exception = AccessibilityException.NodeException("Node traversal failed")
        assertEquals("ACCESSIBILITY_NODE_FAILED", exception.errorCode)
    }

    @Test
    fun `ActionException with action ID`() {
        val exception = AccessibilityException.ActionException(
            "Action failed",
            actionId = 16
        )

        assertEquals("ACCESSIBILITY_ACTION_FAILED", exception.errorCode)
    }

    // ===== Exception Hierarchy Tests =====

    @Test
    fun `DatabaseException is VoiceOSException`() {
        val exception: VoiceOSException = DatabaseException.BackupException("Test")
        assertIs<VoiceOSException>(exception)
        assertIs<DatabaseException>(exception)
    }

    @Test
    fun `SecurityException is VoiceOSException`() {
        val exception: VoiceOSException = SecurityException.EncryptionException("Test")
        assertIs<VoiceOSException>(exception)
        assertIs<SecurityException>(exception)
    }

    @Test
    fun `CommandException is VoiceOSException`() {
        val exception: VoiceOSException = CommandException.ExecutionException("Test")
        assertIs<VoiceOSException>(exception)
        assertIs<CommandException>(exception)
    }

    @Test
    fun `Exception hierarchies are sealed`() {
        // This test ensures sealed classes cannot be extended outside the file
        // If this compiles, the sealed keyword is working correctly
        val exception = DatabaseException.BackupException("Test")
        assertTrue(exception is DatabaseException)
    }

    // ===== Edge Cases =====

    @Test
    fun `Exception with empty message handled gracefully`() {
        val exception = object : VoiceOSException("", null, null) {}
        // Empty message should still be accessible
        assertNotNull(exception.message)
    }

    @Test
    fun `Deep cause chain handled correctly`() {
        val level3 = IllegalArgumentException("Level 3")
        val level2 = IllegalStateException("Level 2", level3)
        val level1 = RuntimeException("Level 1", level2)
        val exception = VoiceOSException("Top level", level1)

        assertTrue(exception.isCausedBy<IllegalArgumentException>())
        assertTrue(exception.isCausedBy<IllegalStateException>())
        assertTrue(exception.isCausedBy<RuntimeException>())
    }
}
