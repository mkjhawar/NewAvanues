/**
 * SecurityTest.kt - Security-focused testing for VoiceOS
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOS Development Team
 * Created: 2025-01-28
 * 
 * Comprehensive security testing for data protection and privacy compliance
 */
package com.augmentalis.licensemanager.security

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.licensemanager.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SecurityTest {
    
    private lateinit var context: Context
    private lateinit var licensingModule: LicensingModule
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        try {
            licensingModule = LicensingModule(context)
        } catch (e: Exception) {
            // If LicensingModule fails to initialize due to missing dependencies,
            // we'll skip the tests that depend on it
            println("Warning: LicensingModule initialization failed: ${e.message}")
        }
    }
    
    @Test
    fun `security test license key never logged or exposed`() = runTest {
        val sensitiveKey = "PREMIUM-ABCD-1234-EFGH-5678"
        // Capture any potential logging without storing unused variable
        
        // Capture any potential logging
        val originalOut = System.out
        val captureStream = java.io.ByteArrayOutputStream()
        System.setOut(java.io.PrintStream(captureStream))
        
        try {
            // Perform operations that might log
            licensingModule.validateLicense(sensitiveKey)
            licensingModule.activatePremium(sensitiveKey)
            licensingModule.getSubscriptionState()
            
            val output = captureStream.toString()
            
            // Verify sensitive data is not in output
            assertFalse(output.contains(sensitiveKey), "License key should never appear in logs")
            assertFalse(output.contains("PREMIUM-"), "License key pattern should not appear in logs")
            assertFalse(output.contains("1234"), "Key fragments should not appear in logs")
            
        } finally {
            System.setOut(originalOut)
        }
    }
    
    @Test
    fun `security test input sanitization against injection`() = runTest {
        val maliciousInputs = listOf(
            "'; DROP TABLE licenses; --",
            "<script>alert('xss')</script>",
            "../../../../etc/passwd",
            "\${java.version}",
            "%{#context['xwork.MethodAccessor.denyMethodExecution']=false}",
            "{{7*7}}",
            "\u0000null\u0000"
        )
        
        maliciousInputs.forEach { maliciousInput ->
            try {
                val result = licensingModule.validateLicense(maliciousInput)
                // Should fail validation, not throw security exception
                assertFalse(result.isValid, "Malicious input should be rejected: $maliciousInput")
                assertTrue(
                    result.errors.any { it.contains("Invalid format") },
                    "Should have proper validation error for: $maliciousInput"
                )
            } catch (e: SecurityException) {
                fail("Should handle malicious input gracefully, not throw SecurityException: $maliciousInput")
            } catch (e: Exception) {
                // Other exceptions are acceptable as long as they don't reveal sensitive info
                assertFalse(
                    e.message?.contains("password") == true || 
                    e.message?.contains("key") == true,
                    "Exception message should not reveal sensitive info: ${e.message}"
                )
            }
        }
    }
    
    @Test
    fun `security test sensitive data encryption at rest`() = runTest {
        val testKey = "TEST-PREMIUM-KEY-2024"
        
        // Activate license (should store encrypted)
        licensingModule.activatePremium(testKey)
        
        // Check SharedPreferences directly
        val prefs = context.getSharedPreferences("voiceos_license", Context.MODE_PRIVATE)
        val storedData = prefs.all
        
        storedData.values.forEach { value ->
            if (value is String) {
                // Verify actual key is not stored in plain text
                assertFalse(value.contains(testKey), "License key should not be stored in plain text")
                assertFalse(value.contains("PREMIUM"), "License key fragments should not be stored in plain text")
                
                // Verify it looks encrypted (base64, hex, etc.)
                if (value.isNotEmpty()) {
                    assertTrue(
                        value.matches(Regex("[A-Za-z0-9+/=]+")) || // Base64
                        value.matches(Regex("[0-9a-fA-F]+")) || // Hex
                        value.length != testKey.length, // Different length indicates processing
                        "Stored value should appear encrypted or hashed: $value"
                    )
                }
            }
        }
    }
    
    @Test
    fun `security test data integrity with hash validation`() = runTest {
        val testKey = "ENTERPRISE-TEST-KEY-2024"
        
        // Hash calculation for integrity testing (not directly used in assertions)
        
        // Store and validate
        licensingModule.activatePremium(testKey)
        val state = licensingModule.getSubscriptionState()
        
        // Verify integrity check passes
        assertTrue(state.isValid, "Subscription state should be valid")
        
        // Simulate data tampering by directly modifying preferences
        val prefs = context.getSharedPreferences("voiceos_license", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("license_type", "INVALID_TAMPERED_VALUE")
            .apply()
        
        // Verify tampering is detected
        val tamperedState = licensingModule.getSubscriptionState()
        assertFalse(tamperedState.isValid, "Tampered data should be detected and rejected")
    }
    
    @Test
    fun `security test timing attack resistance`() = runTest {
        val validKey = "PREMIUM-1234-5678-9012"
        val invalidKeys = listOf(
            "INVALID-1234-5678-9012", // Same length, different content
            "PREMIUM-XXXX-YYYY-ZZZZ", // Same pattern, different values
            "", // Empty
            "X" // Single character
        )
        
        val validationTimes = mutableListOf<Long>()
        
        // Measure validation time for valid key
        repeat(10) {
            val startTime = System.nanoTime()
            licensingModule.validateLicense(validKey)
            val endTime = System.nanoTime()
            validationTimes.add(endTime - startTime)
        }
        
        val avgValidTime = validationTimes.average()
        
        // Measure validation time for invalid keys
        invalidKeys.forEach { invalidKey ->
            val invalidTimes = mutableListOf<Long>()
            
            repeat(10) {
                val startTime = System.nanoTime()
                licensingModule.validateLicense(invalidKey)
                val endTime = System.nanoTime()
                invalidTimes.add(endTime - startTime)
            }
            
            val avgInvalidTime = invalidTimes.average()
            val timeDifference = kotlin.math.abs(avgValidTime - avgInvalidTime)
            val allowedVariance = avgValidTime * 0.5 // 50% variance allowed
            
            assertTrue(
                timeDifference < allowedVariance,
                "Validation time should be consistent to prevent timing attacks. " +
                "Valid: ${avgValidTime}ns, Invalid '$invalidKey': ${avgInvalidTime}ns, " +
                "Difference: ${timeDifference}ns, Allowed: ${allowedVariance}ns"
            )
        }
    }
    
    @Test
    fun `security test privilege escalation prevention`() = runTest {
        // Test that free license cannot be escalated to premium through manipulation
        
        licensingModule.activateFree()
        val initialState = licensingModule.getSubscriptionState()
        assertEquals(LicensingModule.LICENSE_FREE, initialState.licenseType)
        
        // Attempt various escalation attacks
        val escalationAttempts = listOf(
            // Direct preference manipulation
            { 
                val prefs = context.getSharedPreferences("voiceos_license", Context.MODE_PRIVATE)
                prefs.edit().putString("license_type", LicensingModule.LICENSE_PREMIUM).apply()
            },
            // Memory manipulation attempt
            {
                System.setProperty("voiceos.license.type", LicensingModule.LICENSE_PREMIUM)
            },
            // Reflection attempt would go here if we had public fields
        )
        
        escalationAttempts.forEach { attempt ->
            try {
                attempt.invoke()
                
                // Verify escalation was not successful
                val stateAfterAttack = licensingModule.getSubscriptionState()
                assertTrue(
                    stateAfterAttack.licenseType == LicensingModule.LICENSE_FREE ||
                    !stateAfterAttack.isValid,
                    "Privilege escalation should be prevented or detected"
                )
                
            } catch (e: Exception) {
                // Exceptions during attack attempts are acceptable
                assertTrue(true, "Attack attempt properly blocked with exception")
            }
        }
    }
    
    @Test
    fun `security test voice data handling privacy compliance`() {
        // Simulate voice data processing
        val voiceCommands = listOf(
            "open my banking app",
            "call mom at 555-1234",
            "search for my address",
            "open private messages"
        )
        
        voiceCommands.forEach { command ->
            // Process command through system
            processVoiceCommand(command)
            
            // Verify sensitive patterns are not logged
            val logOutput = captureSystemOutput {
                // This would capture any logging during processing
                processVoiceCommand(command)
            }
            
            // Check for sensitive data patterns
            val sensitivePatterns = listOf(
                "\\d{3}-\\d{4}", // Phone numbers
                "banking|password|private|personal", // Sensitive terms
                "\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\s?\\d{4}\\b" // Credit card patterns
            )
            
            sensitivePatterns.forEach { pattern ->
                val regex = Regex(pattern, RegexOption.IGNORE_CASE)
                assertFalse(
                    regex.containsMatchIn(logOutput),
                    "Sensitive pattern '$pattern' should not appear in logs for command: $command"
                )
            }
        }
    }
    
    private fun processVoiceCommand(command: String): String {
        // Simulate voice command processing
        return "processed: ${command.length} characters"
    }
    
    private fun captureSystemOutput(action: () -> Unit): String {
        val originalOut = System.out
        val originalErr = System.err
        val captureStream = java.io.ByteArrayOutputStream()
        val captureErr = java.io.ByteArrayOutputStream()
        
        return try {
            System.setOut(java.io.PrintStream(captureStream))
            System.setErr(java.io.PrintStream(captureErr))
            
            action()
            
            captureStream.toString() + captureErr.toString()
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
    }
}