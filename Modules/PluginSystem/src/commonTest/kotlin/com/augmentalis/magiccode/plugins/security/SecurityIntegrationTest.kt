/**
 * SecurityIntegrationTest.kt - Integration tests for plugin security components
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Comprehensive tests for:
 * - TrustStore add/remove/check operations
 * - PluginSandbox permission checking
 * - PermissionEscalationDetector
 * - SecurityAuditLogger
 * - PermissionManifestParser
 */
package com.augmentalis.magiccode.plugins.security

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for TrustStore.
 */
class TrustStoreTest {

    @Test
    fun testAddTrustedKey() {
        // Arrange
        val trustStore = TrustStore()
        val publisherId = "com.example.publisher"
        val keyPath = "/keys/publisher.pub"

        // Act
        trustStore.addTrustedKey(publisherId, keyPath)

        // Assert
        assertTrue(trustStore.isTrusted(publisherId), "Publisher should be trusted")
        assertEquals(keyPath, trustStore.getPublicKeyPath(publisherId), "Key path should match")
    }

    @Test
    fun testRemoveTrustedKey() {
        // Arrange
        val trustStore = TrustStore()
        val publisherId = "com.example.publisher"
        trustStore.addTrustedKey(publisherId, "/keys/publisher.pub")

        // Act
        val removed = trustStore.removeTrustedKey(publisherId)

        // Assert
        assertTrue(removed, "Should return true when key removed")
        assertFalse(trustStore.isTrusted(publisherId), "Publisher should not be trusted after removal")
        assertNull(trustStore.getPublicKeyPath(publisherId), "Key path should be null after removal")
    }

    @Test
    fun testRemoveNonExistentKey() {
        // Arrange
        val trustStore = TrustStore()

        // Act
        val removed = trustStore.removeTrustedKey("com.nonexistent.publisher")

        // Assert
        assertFalse(removed, "Should return false when key not found")
    }

    @Test
    fun testIsTrusted() {
        // Arrange
        val trustStore = TrustStore()
        val trustedId = "com.trusted.publisher"
        val untrustedId = "com.untrusted.publisher"
        trustStore.addTrustedKey(trustedId, "/keys/trusted.pub")

        // Assert
        assertTrue(trustStore.isTrusted(trustedId), "Should be trusted")
        assertFalse(trustStore.isTrusted(untrustedId), "Should not be trusted")
    }

    @Test
    fun testGetTrustedPublishers() {
        // Arrange
        val trustStore = TrustStore()
        trustStore.addTrustedKey("com.publisher.a", "/keys/a.pub")
        trustStore.addTrustedKey("com.publisher.b", "/keys/b.pub")
        trustStore.addTrustedKey("com.publisher.c", "/keys/c.pub")

        // Act
        val publishers = trustStore.getTrustedPublishers()

        // Assert
        assertEquals(3, publishers.size, "Should have 3 publishers")
        assertTrue(publishers.contains("com.publisher.a"), "Should contain publisher A")
        assertTrue(publishers.contains("com.publisher.b"), "Should contain publisher B")
        assertTrue(publishers.contains("com.publisher.c"), "Should contain publisher C")
    }

    @Test
    fun testClearTrustStore() {
        // Arrange
        val trustStore = TrustStore()
        trustStore.addTrustedKey("com.publisher.a", "/keys/a.pub")
        trustStore.addTrustedKey("com.publisher.b", "/keys/b.pub")

        // Act
        trustStore.clear()

        // Assert
        assertEquals(0, trustStore.getTrustedPublishers().size, "Should be empty after clear")
        assertFalse(trustStore.isTrusted("com.publisher.a"), "Should not be trusted after clear")
    }

    @Test
    fun testUpdateTrustedKey() {
        // Arrange
        val trustStore = TrustStore()
        val publisherId = "com.example.publisher"
        trustStore.addTrustedKey(publisherId, "/keys/old.pub")

        // Act
        trustStore.addTrustedKey(publisherId, "/keys/new.pub")

        // Assert
        assertEquals("/keys/new.pub", trustStore.getPublicKeyPath(publisherId), "Key path should be updated")
    }
}

/**
 * Integration tests for PluginSandbox.
 */
class PluginSandboxTest {

    @Test
    fun testCheckPermission() {
        // Arrange
        val sandbox = DefaultPluginSandbox()
        val pluginId = "com.test.plugin"
        sandbox.grantPermission(pluginId, PluginPermission.NETWORK_ACCESS)

        // Assert
        assertTrue(
            sandbox.checkPermission(pluginId, PluginPermission.NETWORK_ACCESS),
            "Should have NETWORK_ACCESS"
        )
        assertFalse(
            sandbox.checkPermission(pluginId, PluginPermission.FILE_SYSTEM_WRITE),
            "Should not have FILE_SYSTEM_WRITE"
        )
    }

    @Test
    fun testEnforcePermissionSuccess() {
        // Arrange
        val sandbox = DefaultPluginSandbox()
        val pluginId = "com.test.plugin"
        sandbox.grantPermission(pluginId, PluginPermission.NETWORK_ACCESS)

        // Act & Assert (no exception)
        sandbox.enforcePermission(pluginId, PluginPermission.NETWORK_ACCESS)
    }

    @Test
    fun testEnforcePermissionFailure() {
        // Arrange
        val sandbox = DefaultPluginSandbox()
        val pluginId = "com.test.plugin"

        // Act & Assert
        val exception = assertFailsWith<PermissionDeniedException> {
            sandbox.enforcePermission(pluginId, PluginPermission.FILE_SYSTEM_WRITE)
        }
        assertEquals(pluginId, exception.pluginId, "Exception should contain plugin ID")
        assertEquals(PluginPermission.FILE_SYSTEM_WRITE, exception.permission, "Exception should contain permission")
    }

    @Test
    fun testGrantPermission() {
        // Arrange
        val sandbox = DefaultPluginSandbox()
        val pluginId = "com.test.plugin"

        // Act
        sandbox.grantPermission(pluginId, PluginPermission.NETWORK_ACCESS)
        sandbox.grantPermission(pluginId, PluginPermission.FILE_SYSTEM_READ)

        // Assert
        val permissions = sandbox.getGrantedPermissions(pluginId)
        assertEquals(2, permissions.size, "Should have 2 permissions")
        assertTrue(permissions.contains(PluginPermission.NETWORK_ACCESS), "Should have NETWORK_ACCESS")
        assertTrue(permissions.contains(PluginPermission.FILE_SYSTEM_READ), "Should have FILE_SYSTEM_READ")
    }

    @Test
    fun testRevokePermission() {
        // Arrange
        val sandbox = DefaultPluginSandbox()
        val pluginId = "com.test.plugin"
        sandbox.grantPermission(pluginId, PluginPermission.NETWORK_ACCESS)
        sandbox.grantPermission(pluginId, PluginPermission.FILE_SYSTEM_READ)

        // Act
        sandbox.revokePermission(pluginId, PluginPermission.NETWORK_ACCESS)

        // Assert
        assertFalse(
            sandbox.checkPermission(pluginId, PluginPermission.NETWORK_ACCESS),
            "Should not have NETWORK_ACCESS after revoke"
        )
        assertTrue(
            sandbox.checkPermission(pluginId, PluginPermission.FILE_SYSTEM_READ),
            "Should still have FILE_SYSTEM_READ"
        )
    }

    @Test
    fun testRevokeAllPermissions() {
        // Arrange
        val sandbox = DefaultPluginSandbox()
        val pluginId = "com.test.plugin"
        sandbox.grantPermission(pluginId, PluginPermission.NETWORK_ACCESS)
        sandbox.grantPermission(pluginId, PluginPermission.FILE_SYSTEM_READ)
        sandbox.grantPermission(pluginId, PluginPermission.NOTIFICATIONS)

        // Act
        sandbox.revokeAllPermissions(pluginId)

        // Assert
        val permissions = sandbox.getGrantedPermissions(pluginId)
        assertTrue(permissions.isEmpty(), "Should have no permissions after revoke all")
    }

    @Test
    fun testHasAllPermissions() {
        // Arrange
        val sandbox = DefaultPluginSandbox()
        val pluginId = "com.test.plugin"
        sandbox.grantPermission(pluginId, PluginPermission.NETWORK_ACCESS)
        sandbox.grantPermission(pluginId, PluginPermission.FILE_SYSTEM_READ)

        // Assert
        assertTrue(
            sandbox.hasAllPermissions(
                pluginId,
                setOf(PluginPermission.NETWORK_ACCESS, PluginPermission.FILE_SYSTEM_READ)
            ),
            "Should have all requested permissions"
        )
        assertFalse(
            sandbox.hasAllPermissions(
                pluginId,
                setOf(PluginPermission.NETWORK_ACCESS, PluginPermission.FILE_SYSTEM_WRITE)
            ),
            "Should not have all when one is missing"
        )
    }

    @Test
    fun testHasAnyPermission() {
        // Arrange
        val sandbox = DefaultPluginSandbox()
        val pluginId = "com.test.plugin"
        sandbox.grantPermission(pluginId, PluginPermission.NETWORK_ACCESS)

        // Assert
        assertTrue(
            sandbox.hasAnyPermission(
                pluginId,
                setOf(PluginPermission.NETWORK_ACCESS, PluginPermission.FILE_SYSTEM_WRITE)
            ),
            "Should have at least one permission"
        )
        assertFalse(
            sandbox.hasAnyPermission(
                pluginId,
                setOf(PluginPermission.FILE_SYSTEM_READ, PluginPermission.FILE_SYSTEM_WRITE)
            ),
            "Should not have any of the requested permissions"
        )
    }

    @Test
    fun testPermissionDeniedExceptionMessage() {
        // Arrange
        val exception = PermissionDeniedException(
            pluginId = "com.test.plugin",
            permission = PluginPermission.NETWORK_ACCESS,
            operation = "httpRequest"
        )

        // Assert
        assertTrue(
            exception.message?.contains("com.test.plugin") == true,
            "Message should contain plugin ID"
        )
        assertTrue(
            exception.message?.contains("NETWORK_ACCESS") == true,
            "Message should contain permission"
        )
        assertTrue(
            exception.message?.contains("httpRequest") == true,
            "Message should contain operation"
        )
    }
}

/**
 * Integration tests for PermissionEscalationDetector.
 */
class PermissionEscalationDetectorTest {

    @Test
    fun testRecordEscalation() {
        // Arrange
        val detector = PermissionEscalationDetector()
        val pluginId = "com.test.plugin"

        // Act
        detector.recordEscalation(
            pluginId = pluginId,
            permission = PluginPermission.NETWORK_ACCESS,
            operation = "httpRequest",
            type = EscalationType.DENIED_PERMISSION
        )

        // Assert
        val stats = detector.getStats(pluginId)
        assertEquals(1, stats.totalAttempts, "Should have 1 attempt")
        assertEquals(1, stats.recentAttempts, "Should have 1 recent attempt")
        assertNotNull(stats.lastAttemptTime, "Should have last attempt time")
    }

    @Test
    fun testManifestPermissionTracking() {
        // Arrange
        val detector = PermissionEscalationDetector()
        val pluginId = "com.test.plugin"
        val manifestPerms = setOf(PluginPermission.FILE_SYSTEM_READ, PluginPermission.NOTIFICATIONS)

        // Act
        detector.registerManifestPermissions(pluginId, manifestPerms)

        // Assert
        val registered = detector.getManifestPermissions(pluginId)
        assertNotNull(registered, "Should have registered permissions")
        assertEquals(2, registered.size, "Should have 2 permissions")
        assertTrue(registered.contains(PluginPermission.FILE_SYSTEM_READ), "Should contain FILE_SYSTEM_READ")
    }

    @Test
    fun testCheckEscalation() {
        // Arrange
        val detector = PermissionEscalationDetector()
        val pluginId = "com.test.plugin"
        detector.registerManifestPermissions(
            pluginId,
            setOf(PluginPermission.FILE_SYSTEM_READ)
        )
        val grantedPermissions = setOf(PluginPermission.FILE_SYSTEM_READ)

        // Assert
        assertNull(
            detector.checkEscalation(pluginId, PluginPermission.FILE_SYSTEM_READ, grantedPermissions),
            "Should not detect escalation for granted+declared permission"
        )
        assertEquals(
            EscalationType.UNDECLARED_PERMISSION,
            detector.checkEscalation(pluginId, PluginPermission.NETWORK_ACCESS, grantedPermissions),
            "Should detect undeclared permission"
        )
        assertEquals(
            EscalationType.DENIED_PERMISSION,
            detector.checkEscalation(pluginId, PluginPermission.FILE_SYSTEM_READ, emptySet()),
            "Should detect denied permission"
        )
    }

    @Test
    fun testBlockingPlugin() {
        // Arrange
        val detector = PermissionEscalationDetector(
            config = EscalationDetectorConfig(
                maxAttemptsBeforeBlock = 3,
                enableAutomaticBlocking = true
            )
        )
        val pluginId = "com.suspicious.plugin"

        // Act
        repeat(5) { i ->
            detector.recordEscalation(
                pluginId = pluginId,
                permission = PluginPermission.NETWORK_ACCESS,
                operation = "attempt_$i",
                type = EscalationType.DENIED_PERMISSION
            )
        }

        // Assert
        assertTrue(detector.isBlocked(pluginId), "Plugin should be blocked after threshold")
    }

    @Test
    fun testUnblockPlugin() {
        // Arrange
        val detector = PermissionEscalationDetector()
        val pluginId = "com.test.plugin"
        detector.blockPlugin(pluginId)
        assertTrue(detector.isBlocked(pluginId), "Plugin should be blocked")

        // Act
        detector.unblockPlugin(pluginId)

        // Assert
        assertFalse(detector.isBlocked(pluginId), "Plugin should be unblocked")
    }

    @Test
    fun testEscalationCallback() {
        // Arrange
        val detector = PermissionEscalationDetector()
        val pluginId = "com.test.plugin"
        var callbackInvoked = false
        var receivedEvent: EscalationEvent? = null

        detector.addCallback(object : EscalationCallback {
            override fun onEscalationDetected(event: EscalationEvent) {
                callbackInvoked = true
                receivedEvent = event
            }

            override fun onEscalationThresholdReached(pluginId: String, attemptCount: Int, period: Long) {}
        })

        // Act
        detector.recordEscalation(
            pluginId = pluginId,
            permission = PluginPermission.NETWORK_ACCESS,
            operation = "test",
            type = EscalationType.DENIED_PERMISSION
        )

        // Assert
        assertTrue(callbackInvoked, "Callback should be invoked")
        assertNotNull(receivedEvent, "Should receive event")
        assertEquals(pluginId, receivedEvent?.pluginId, "Event should have correct plugin ID")
    }

    @Test
    fun testGetHistory() {
        // Arrange
        val detector = PermissionEscalationDetector()
        val pluginId = "com.test.plugin"

        repeat(5) { i ->
            detector.recordEscalation(
                pluginId = pluginId,
                permission = PluginPermission.values()[i % PluginPermission.values().size],
                operation = "operation_$i",
                type = EscalationType.DENIED_PERMISSION
            )
        }

        // Act
        val history = detector.getHistory(pluginId, limit = 3)

        // Assert
        assertEquals(3, history.size, "Should return limited history")
    }

    @Test
    fun testClearHistory() {
        // Arrange
        val detector = PermissionEscalationDetector()
        val pluginId = "com.test.plugin"
        detector.recordEscalation(
            pluginId = pluginId,
            permission = PluginPermission.NETWORK_ACCESS,
            operation = "test",
            type = EscalationType.DENIED_PERMISSION
        )

        // Act
        detector.clearHistory(pluginId)

        // Assert
        val stats = detector.getStats(pluginId)
        assertEquals(0, stats.totalAttempts, "Should have no attempts after clear")
    }

    @Test
    fun testEscalationEventDescription() {
        // Arrange
        val event = EscalationEvent(
            pluginId = "com.test.plugin",
            requestedPermission = PluginPermission.NETWORK_ACCESS,
            operation = "httpRequest",
            timestamp = System.currentTimeMillis(),
            escalationType = EscalationType.DENIED_PERMISSION
        )

        // Assert
        val description = event.description
        assertTrue(description.contains("com.test.plugin"), "Description should contain plugin ID")
        assertTrue(description.contains("NETWORK_ACCESS"), "Description should contain permission")
        assertTrue(description.contains("httpRequest"), "Description should contain operation")
    }
}

/**
 * Integration tests for SecurityAuditLogger.
 */
class SecurityAuditLoggerTest {

    @Test
    fun testLogSignatureVerification() {
        // Arrange
        val logger = SecurityAuditLogger.create()
        val pluginId = "com.test.plugin"

        // Act
        logger.logSignatureVerification(pluginId, success = true, algorithm = "RSA-SHA256")

        // Assert
        val events = logger.getRecentEvents(10)
        assertTrue(events.isNotEmpty(), "Should have logged event")
        assertEquals(SecurityEventCategory.SIGNATURE_VERIFICATION, events.first().category)
        assertEquals(pluginId, events.first().pluginId)
    }

    @Test
    fun testLogPermissionEvents() {
        // Arrange
        val logger = SecurityAuditLogger.create()
        val pluginId = "com.test.plugin"

        // Act
        logger.logPermissionGranted(pluginId, PluginPermission.NETWORK_ACCESS)
        logger.logPermissionRevoked(pluginId, PluginPermission.NETWORK_ACCESS)

        // Assert
        val events = logger.getEventsByPlugin(pluginId, 10)
        assertEquals(2, events.size, "Should have 2 events")
        assertTrue(
            events.any { it.details["action"] == "GRANT" },
            "Should have grant event"
        )
        assertTrue(
            events.any { it.details["action"] == "REVOKE" },
            "Should have revoke event"
        )
    }

    @Test
    fun testGetEventsByCategory() {
        // Arrange
        val logger = SecurityAuditLogger.create()
        logger.logSignatureVerification("plugin1", success = true)
        logger.logPermissionGranted("plugin2", PluginPermission.NETWORK_ACCESS)
        logger.logTrustedKeyAdded("publisher1", "/keys/pub.key")

        // Act
        val signatureEvents = logger.getEventsByCategory(SecurityEventCategory.SIGNATURE_VERIFICATION)
        val permissionEvents = logger.getEventsByCategory(SecurityEventCategory.PERMISSION)
        val trustEvents = logger.getEventsByCategory(SecurityEventCategory.TRUST_STORE)

        // Assert
        assertEquals(1, signatureEvents.size, "Should have 1 signature event")
        assertEquals(1, permissionEvents.size, "Should have 1 permission event")
        assertEquals(1, trustEvents.size, "Should have 1 trust event")
    }

    @Test
    fun testGetEventsBySeverity() {
        // Arrange
        val logger = SecurityAuditLogger.create()
        logger.logSignatureVerification("plugin1", success = true)  // INFO
        logger.logSignatureVerification("plugin2", success = false, reason = "invalid")  // WARNING
        logger.logPluginBlocked("plugin3", "escalation")  // ALERT

        // Act
        val warningAndAbove = logger.getEventsBySeverity(SecurityEventSeverity.WARNING)
        val alertOnly = logger.getEventsBySeverity(SecurityEventSeverity.ALERT)

        // Assert
        assertEquals(2, warningAndAbove.size, "Should have 2 WARNING+ events")
        assertEquals(1, alertOnly.size, "Should have 1 ALERT event")
    }

    @Test
    fun testEventCountByCategory() {
        // Arrange
        val logger = SecurityAuditLogger.create()
        logger.logPermissionGranted("plugin1", PluginPermission.NETWORK_ACCESS)
        logger.logPermissionGranted("plugin2", PluginPermission.FILE_SYSTEM_READ)
        logger.logSignatureVerification("plugin3", success = true)

        // Act
        val counts = logger.getEventCountByCategory()

        // Assert
        assertEquals(2, counts[SecurityEventCategory.PERMISSION], "Should have 2 permission events")
        assertEquals(1, counts[SecurityEventCategory.SIGNATURE_VERIFICATION], "Should have 1 signature event")
    }

    @Test
    fun testExternalHandler() {
        // Arrange
        val logger = SecurityAuditLogger.create()
        val handledEvents = mutableListOf<SecurityAuditEvent>()

        logger.addHandler(object : SecurityAuditHandler {
            override fun handleEvent(event: SecurityAuditEvent) {
                handledEvents.add(event)
            }
        })

        // Act
        logger.logPermissionGranted("plugin1", PluginPermission.NETWORK_ACCESS)

        // Assert
        assertEquals(1, handledEvents.size, "Handler should receive event")
    }

    @Test
    fun testEventToJson() {
        // Arrange
        val event = SecurityAuditEvent(
            eventId = "test_123",
            category = SecurityEventCategory.PERMISSION,
            severity = SecurityEventSeverity.INFO,
            message = "Test message",
            timestamp = 1234567890L,
            pluginId = "com.test.plugin",
            details = mapOf("key" to "value")
        )

        // Act
        val json = event.toJson()

        // Assert
        assertTrue(json.contains("\"eventId\":\"test_123\""), "JSON should contain event ID")
        assertTrue(json.contains("\"category\":\"PERMISSION\""), "JSON should contain category")
        assertTrue(json.contains("\"pluginId\":\"com.test.plugin\""), "JSON should contain plugin ID")
    }

    @Test
    fun testClearEvents() {
        // Arrange
        val logger = SecurityAuditLogger.create()
        logger.logPermissionGranted("plugin1", PluginPermission.NETWORK_ACCESS)
        logger.logPermissionGranted("plugin2", PluginPermission.FILE_SYSTEM_READ)

        // Act
        logger.clearEvents()

        // Assert
        val events = logger.getRecentEvents(100)
        assertTrue(events.isEmpty(), "Should have no events after clear")
    }

    @Test
    fun testSeverityFiltering() {
        // Arrange
        val logger = SecurityAuditLogger.create(
            config = SecurityAuditConfig(minSeverity = SecurityEventSeverity.WARNING)
        )

        // Act
        logger.log(
            category = SecurityEventCategory.PERMISSION,
            severity = SecurityEventSeverity.INFO,
            message = "This should be filtered"
        )
        logger.log(
            category = SecurityEventCategory.PERMISSION,
            severity = SecurityEventSeverity.WARNING,
            message = "This should be logged"
        )

        // Assert
        val events = logger.getRecentEvents(100)
        assertEquals(1, events.size, "Should only have 1 event (INFO filtered)")
        assertEquals("This should be logged", events.first().message)
    }
}

/**
 * Integration tests for PermissionManifestParser.
 */
class PermissionManifestParserTest {

    @Test
    fun testParseSimplePermissions() {
        // Arrange
        val parser = PermissionManifestParser()
        val json = """
            {
                "permissions": ["NETWORK_ACCESS", "FILE_SYSTEM_READ", "NOTIFICATIONS"]
            }
        """.trimIndent()

        // Act
        val result = parser.parseJson(json)

        // Assert
        assertTrue(result.isValid, "Should be valid")
        assertEquals(3, result.permissions.size, "Should have 3 permissions")
        assertTrue(
            result.permissionSet.contains(PluginPermission.NETWORK_ACCESS),
            "Should contain NETWORK_ACCESS"
        )
    }

    @Test
    fun testParsePermissionsWithRationales() {
        // Arrange
        val parser = PermissionManifestParser()
        val json = """
            {
                "permissions": ["NETWORK_ACCESS", "FILE_SYSTEM_READ"],
                "permissionRationales": {
                    "NETWORK_ACCESS": "Required for API calls",
                    "FILE_SYSTEM_READ": "To load configuration"
                }
            }
        """.trimIndent()

        // Act
        val result = parser.parseJson(json)

        // Assert
        assertTrue(result.isValid, "Should be valid")
        val networkPerm = result.permissions.find { it.permission == PluginPermission.NETWORK_ACCESS }
        assertEquals("Required for API calls", networkPerm?.rationale, "Should have rationale")
    }

    @Test
    fun testParseUnknownPermission() {
        // Arrange
        val parser = PermissionManifestParser()
        val json = """
            {
                "permissions": ["NETWORK_ACCESS", "UNKNOWN_PERMISSION"]
            }
        """.trimIndent()

        // Act
        val result = parser.parseJson(json)

        // Assert
        assertEquals(1, result.permissions.size, "Should have 1 valid permission")
        assertEquals(1, result.warnings.size, "Should have 1 warning for unknown permission")
        assertTrue(result.warnings.first().contains("Unknown"), "Warning should mention unknown")
    }

    @Test
    fun testDetectDangerousCombinations() {
        // Arrange
        val parser = PermissionManifestParser()
        val json = """
            {
                "permissions": ["NETWORK_ACCESS", "FILE_SYSTEM_WRITE"]
            }
        """.trimIndent()

        // Act
        val result = parser.parseJson(json)

        // Assert
        assertTrue(result.isValid, "Should be valid despite dangerous combination")
        assertTrue(result.dangerousCombinations.isNotEmpty(), "Should detect dangerous combination")
        assertEquals(
            RiskLevel.HIGH,
            result.dangerousCombinations.first().riskLevel,
            "Should be HIGH risk"
        )
    }

    @Test
    fun testDetectCriticalCombination() {
        // Arrange
        val parser = PermissionManifestParser()
        val json = """
            {
                "permissions": ["BACKGROUND_EXECUTION", "NETWORK_ACCESS", "FILE_SYSTEM_WRITE"]
            }
        """.trimIndent()

        // Act
        val result = parser.parseJson(json)

        // Assert
        val criticalCombo = result.dangerousCombinations.find { it.riskLevel == RiskLevel.CRITICAL }
        assertNotNull(criticalCombo, "Should detect CRITICAL risk combination")
    }

    @Test
    fun testParsePermissionName() {
        // Arrange
        val parser = PermissionManifestParser()

        // Assert - various naming conventions
        assertEquals(PluginPermission.NETWORK_ACCESS, parser.parsePermissionName("NETWORK_ACCESS"))
        assertEquals(PluginPermission.NETWORK_ACCESS, parser.parsePermissionName("network_access"))
        assertEquals(PluginPermission.NETWORK_ACCESS, parser.parsePermissionName("network-access"))
        assertEquals(PluginPermission.FILE_SYSTEM_READ, parser.parsePermissionName("FILE_SYSTEM_READ"))
        assertNull(parser.parsePermissionName("INVALID_PERMISSION"))
    }

    @Test
    fun testValidateSyntaxDuplicates() {
        // Arrange
        val parser = PermissionManifestParser()
        val declarations = listOf(
            PermissionDeclaration(PluginPermission.NETWORK_ACCESS),
            PermissionDeclaration(PluginPermission.NETWORK_ACCESS)  // Duplicate
        )

        // Act
        val errors = parser.validatePermissionSyntax(declarations)

        // Assert
        assertEquals(1, errors.size, "Should have 1 error for duplicate")
        assertTrue(errors.first().contains("Duplicate"), "Error should mention duplicate")
    }

    @Test
    fun testFindMissingPermissions() {
        // Arrange
        val parser = PermissionManifestParser()
        val declared = setOf(PluginPermission.NETWORK_ACCESS)
        val required = setOf(PluginPermission.NETWORK_ACCESS, PluginPermission.FILE_SYSTEM_READ)

        // Act
        val missing = parser.findMissingPermissions(declared, required)

        // Assert
        assertEquals(1, missing.size, "Should have 1 missing permission")
        assertTrue(missing.contains(PluginPermission.FILE_SYSTEM_READ), "Should identify FILE_SYSTEM_READ as missing")
    }

    @Test
    fun testFindExcessivePermissions() {
        // Arrange
        val parser = PermissionManifestParser()
        val declared = setOf(
            PluginPermission.NETWORK_ACCESS,
            PluginPermission.FILE_SYSTEM_READ,
            PluginPermission.NOTIFICATIONS
        )
        val usedByCapabilities = setOf(PluginPermission.NETWORK_ACCESS)

        // Act
        val excessive = parser.findExcessivePermissions(declared, usedByCapabilities)

        // Assert
        assertEquals(2, excessive.size, "Should have 2 excessive permissions")
        assertTrue(excessive.contains(PluginPermission.FILE_SYSTEM_READ), "Should identify FILE_SYSTEM_READ as excessive")
        assertTrue(excessive.contains(PluginPermission.NOTIFICATIONS), "Should identify NOTIFICATIONS as excessive")
    }

    @Test
    fun testToJson() {
        // Arrange
        val parser = PermissionManifestParser()
        val declarations = listOf(
            PermissionDeclaration(PluginPermission.NETWORK_ACCESS, "For API calls"),
            PermissionDeclaration(PluginPermission.FILE_SYSTEM_READ, null)
        )

        // Act
        val json = parser.toJson(declarations)

        // Assert
        assertTrue(json.contains("NETWORK_ACCESS"), "JSON should contain NETWORK_ACCESS")
        assertTrue(json.contains("FILE_SYSTEM_READ"), "JSON should contain FILE_SYSTEM_READ")
        assertTrue(json.contains("For API calls"), "JSON should contain rationale")
    }

    @Test
    fun testParseNoPermissions() {
        // Arrange
        val parser = PermissionManifestParser()
        val json = """
            {
                "id": "com.example.plugin",
                "name": "No Permissions Plugin"
            }
        """.trimIndent()

        // Act
        val result = parser.parseJson(json)

        // Assert
        assertTrue(result.isValid, "Should be valid")
        assertTrue(result.permissions.isEmpty(), "Should have no permissions")
    }

    @Test
    fun testParseInvalidJson() {
        // Arrange
        val parser = PermissionManifestParser()
        val json = "this is not valid json"

        // Act
        val result = parser.parseJson(json)

        // Assert
        assertFalse(result.isValid, "Should be invalid")
        assertTrue(result.errors.isNotEmpty(), "Should have errors")
        assertTrue(result.errors.first().contains("Invalid JSON"), "Error should mention invalid JSON")
    }
}

/**
 * Integration tests for combined security components.
 */
class SecurityComponentIntegrationTest {

    @Test
    fun testSandboxWithAuditLogger() {
        // Arrange
        val auditLogger = SecurityAuditLogger.create()
        val sandbox = DefaultPluginSandbox(auditLogger = auditLogger)
        val pluginId = "com.test.plugin"

        // Act
        sandbox.grantPermission(pluginId, PluginPermission.NETWORK_ACCESS)
        sandbox.enforcePermission(pluginId, PluginPermission.NETWORK_ACCESS)

        // Assert
        val events = auditLogger.getEventsByPlugin(pluginId)
        assertTrue(events.isNotEmpty(), "Should have logged events")
        assertTrue(
            events.any { it.details["action"] == "GRANT" },
            "Should have grant event"
        )
    }

    @Test
    fun testEscalationDetectorWithAuditLogger() {
        // Arrange
        val auditLogger = SecurityAuditLogger.create()
        val detector = PermissionEscalationDetector(auditLogger = auditLogger)
        val pluginId = "com.test.plugin"

        // Act
        detector.recordEscalation(
            pluginId = pluginId,
            permission = PluginPermission.NETWORK_ACCESS,
            operation = "httpRequest",
            type = EscalationType.DENIED_PERMISSION
        )

        // Assert
        val escalationEvents = auditLogger.getEventsByCategory(SecurityEventCategory.ESCALATION)
        assertEquals(1, escalationEvents.size, "Should have 1 escalation event")
    }

    @Test
    fun testFullSecurityWorkflow() {
        // Arrange
        val auditLogger = SecurityAuditLogger.create()
        val sandbox = DefaultPluginSandbox(auditLogger = auditLogger)
        val detector = PermissionEscalationDetector(auditLogger = auditLogger)
        val parser = PermissionManifestParser()
        val trustStore = TrustStore()

        val pluginId = "com.example.plugin"
        val publisherId = "com.example.publisher"

        // 1. Parse manifest permissions
        val manifestJson = """
            {
                "permissions": ["NETWORK_ACCESS", "FILE_SYSTEM_READ"]
            }
        """.trimIndent()
        val parseResult = parser.parseJson(manifestJson)
        assertTrue(parseResult.isValid, "Manifest should be valid")

        // 2. Register manifest permissions with detector
        detector.registerManifestPermissions(pluginId, parseResult.permissionSet)

        // 3. Add publisher to trust store
        trustStore.addTrustedKey(publisherId, "/keys/publisher.pub")
        auditLogger.logTrustedKeyAdded(publisherId, "/keys/publisher.pub")

        // 4. Log signature verification
        auditLogger.logSignatureVerification(pluginId, success = true, algorithm = "RSA-SHA256")

        // 5. Grant manifest-declared permissions
        for (declaration in parseResult.permissions) {
            sandbox.grantPermission(pluginId, declaration.permission)
        }

        // 6. Plugin operates within permissions
        sandbox.enforcePermission(pluginId, PluginPermission.NETWORK_ACCESS)

        // 7. Attempt escalation (undeclared permission)
        val escalationType = detector.checkEscalation(
            pluginId,
            PluginPermission.NOTIFICATIONS,  // Not declared
            sandbox.getGrantedPermissions(pluginId)
        )
        assertEquals(EscalationType.UNDECLARED_PERMISSION, escalationType, "Should detect escalation")

        // 8. Verify audit trail
        val allEvents = auditLogger.getRecentEvents(100)
        assertTrue(allEvents.size >= 4, "Should have multiple security events")

        // Verify event types
        val categories = allEvents.map { it.category }.toSet()
        assertTrue(categories.contains(SecurityEventCategory.TRUST_STORE), "Should have trust store event")
        assertTrue(categories.contains(SecurityEventCategory.SIGNATURE_VERIFICATION), "Should have signature event")
        assertTrue(categories.contains(SecurityEventCategory.PERMISSION), "Should have permission events")
    }

    @Test
    fun testEscalationFlowCollection() = runBlocking {
        // Arrange
        val detector = PermissionEscalationDetector()
        val pluginId = "com.test.plugin"
        val collectedEvents = mutableListOf<EscalationEvent>()

        // Start collecting events
        val collectJob = launch {
            detector.escalationFlow.take(3).collect { event ->
                collectedEvents.add(event)
            }
        }

        // Allow collector to start
        delay(50)

        // Act - record escalations
        repeat(3) { i ->
            detector.recordEscalation(
                pluginId = pluginId,
                permission = PluginPermission.NETWORK_ACCESS,
                operation = "operation_$i",
                type = EscalationType.DENIED_PERMISSION
            )
        }

        // Wait for collection
        withTimeout(1000) {
            collectJob.join()
        }

        // Assert
        assertEquals(3, collectedEvents.size, "Should have collected 3 events")
    }
}
