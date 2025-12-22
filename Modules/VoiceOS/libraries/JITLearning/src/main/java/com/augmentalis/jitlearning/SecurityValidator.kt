/**
 * SecurityValidator.kt - Input validation and security verification for JIT Service
 *
 * SECURITY (2025-12-12): Comprehensive security layer to prevent:
 * - Unauthorized access (CVE 7.8/10 HIGH)
 * - SQL injection attacks
 * - Path traversal attacks
 * - XSS attacks via ContentDescription
 * - Buffer overflow attacks
 * - Resource exhaustion attacks
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Track B - Security & Validation)
 *
 * @since 2.2.0 (Security Hardening)
 */

package com.augmentalis.jitlearning

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.util.Log
import java.util.regex.Pattern

/**
 * Security Manager for JITLearningService
 *
 * Handles both:
 * 1. Caller verification (UID + signature checking)
 * 2. Input validation (SQL injection, XSS, path traversal, etc.)
 */
class SecurityManager(private val context: Context) {
    companion object {
        private const val TAG = "JITSecurityManager"
        private const val PERMISSION_NAME = "com.augmentalis.voiceos.permission.JIT_CONTROL"
    }

    /**
     * Verify caller has required permission and signature
     *
     * SECURITY: This method MUST be called at the start of EVERY AIDL method
     *
     * @throws SecurityException if caller is unauthorized
     */
    fun verifyCallerPermission() {
        val callingUid = Binder.getCallingUid()
        val callingPid = Binder.getCallingPid()

        // Check permission
        val permissionResult = context.checkPermission(
            PERMISSION_NAME,
            callingPid,
            callingUid
        )

        if (permissionResult != PackageManager.PERMISSION_GRANTED) {
            val callerPackages = context.packageManager.getPackagesForUid(callingUid)
            val callerInfo = callerPackages?.joinToString() ?: "unknown"

            Log.e(TAG, "SECURITY VIOLATION: Unauthorized access attempt from UID $callingUid ($callerInfo)")
            throw SecurityException(
                "Access denied: Caller does not have $PERMISSION_NAME permission. " +
                "Only apps signed with the same certificate can access this service."
            )
        }

        // Verify signature (additional layer of defense)
        verifyCallerSignature(callingUid)

        Log.d(TAG, "Caller verified: UID $callingUid, PID $callingPid")
    }

    /**
     * Verify caller's signature matches our own
     *
     * Additional security layer beyond permission check
     */
    private fun verifyCallerSignature(callingUid: Int) {
        val callerPackages = context.packageManager.getPackagesForUid(callingUid) ?: return

        // Get our own package signature
        val myPackageName = context.packageName
        val mySignatures = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val signingInfo = context.packageManager.getPackageInfo(
                    myPackageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                ).signingInfo

                if (signingInfo?.hasMultipleSigners() == true) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo?.signingCertificateHistory
                }
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    myPackageName,
                    PackageManager.GET_SIGNATURES
                ).signatures
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get own signature", e)
            throw SecurityException("Failed to verify signature")
        }

        // Check if any caller package has matching signature
        for (callerPackage in callerPackages) {
            val callerSignatures = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val signingInfo = context.packageManager.getPackageInfo(
                        callerPackage,
                        PackageManager.GET_SIGNING_CERTIFICATES
                    ).signingInfo

                    if (signingInfo?.hasMultipleSigners() == true) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo?.signingCertificateHistory
                    }
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(
                        callerPackage,
                        PackageManager.GET_SIGNATURES
                    ).signatures
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get signature for $callerPackage", e)
                continue
            }

            // Compare signatures
            if (mySignatures.contentEquals(callerSignatures)) {
                Log.d(TAG, "Signature verified for $callerPackage")
                return
            }
        }

        // No matching signature found
        Log.e(TAG, "SECURITY VIOLATION: Signature mismatch for UID $callingUid")
        throw SecurityException("Signature verification failed")
    }
}

/**
 * Input Validator for JIT Service
 *
 * SECURITY: Validates all user inputs to prevent injection attacks
 */
object InputValidator {
    private const val TAG = "InputValidator"

    // Validation limits
    private const val MAX_PACKAGE_NAME_LENGTH = 255
    private const val MAX_UUID_LENGTH = 64
    private const val MAX_SCREEN_HASH_LENGTH = 64
    private const val MAX_TEXT_INPUT_LENGTH = 10000
    private const val MAX_SELECTOR_LENGTH = 512
    private const val MAX_NODE_ID_LENGTH = 512
    private const val MAX_DISTANCE = 10000
    private const val MAX_BOUNDS_DIMENSION = 100000

    // Package name regex: standard Android package format
    private val PACKAGE_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

    // UUID regex: alphanumeric + hyphens only
    private val UUID_PATTERN = Pattern.compile("^[a-zA-Z0-9-]+$")

    // Screen hash regex: hexadecimal only
    private val SCREEN_HASH_PATTERN = Pattern.compile("^[a-fA-F0-9]+$")

    // Node ID regex: standard resource ID format
    private val NODE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_./:*-]+$")

    /**
     * Validate package name
     * @throws IllegalArgumentException if invalid
     */
    fun validatePackageName(packageName: String?) {
        require(!packageName.isNullOrBlank()) { "Package name cannot be null or empty" }
        require(packageName.length <= MAX_PACKAGE_NAME_LENGTH) {
            "Package name too long: ${packageName.length} > $MAX_PACKAGE_NAME_LENGTH"
        }
        require(PACKAGE_NAME_PATTERN.matcher(packageName).matches()) {
            "Invalid package name format: $packageName"
        }
        require(!packageName.contains("..")) {
            "Package name contains path traversal pattern"
        }
        require(!packageName.contains("'") && !packageName.contains("\"")) {
            "Package name contains SQL injection characters"
        }
    }

    /**
     * Validate UUID
     * @throws IllegalArgumentException if invalid
     */
    fun validateUuid(uuid: String?) {
        require(!uuid.isNullOrBlank()) { "UUID cannot be null or empty" }
        require(uuid.length <= MAX_UUID_LENGTH) {
            "UUID too long: ${uuid.length} > $MAX_UUID_LENGTH"
        }
        require(UUID_PATTERN.matcher(uuid).matches()) {
            "Invalid UUID format: $uuid"
        }
    }

    /**
     * Validate screen hash
     * @throws IllegalArgumentException if invalid
     */
    fun validateScreenHash(screenHash: String?) {
        require(!screenHash.isNullOrBlank()) { "Screen hash cannot be null or empty" }
        require(screenHash.length <= MAX_SCREEN_HASH_LENGTH) {
            "Screen hash too long: ${screenHash.length} > $MAX_SCREEN_HASH_LENGTH"
        }
        require(SCREEN_HASH_PATTERN.matcher(screenHash).matches()) {
            "Invalid screen hash format (must be hexadecimal): $screenHash"
        }
    }

    /**
     * Validate text input
     * @throws IllegalArgumentException if invalid
     */
    fun validateTextInput(text: String?) {
        // Null is allowed for clearing text
        if (text == null) return

        require(text.length <= MAX_TEXT_INPUT_LENGTH) {
            "Text input too long: ${text.length} > $MAX_TEXT_INPUT_LENGTH"
        }

        // Check for XSS attempts
        require(!text.contains("<script", ignoreCase = true)) {
            "Text contains potential XSS attack"
        }
        require(!text.contains("javascript:", ignoreCase = true)) {
            "Text contains potential XSS attack"
        }

        // Check for SQL injection attempts
        val sqlKeywords = listOf("DROP", "DELETE", "INSERT", "UPDATE", "SELECT", "';", "--;", "/*", "*/")
        for (keyword in sqlKeywords) {
            require(!text.contains(keyword, ignoreCase = true)) {
                "Text contains potential SQL injection pattern: $keyword"
            }
        }
    }

    /**
     * Validate selector string
     * @throws IllegalArgumentException if invalid
     */
    fun validateSelector(selector: String?) {
        require(!selector.isNullOrBlank()) { "Selector cannot be null or empty" }
        require(selector.length <= MAX_SELECTOR_LENGTH) {
            "Selector too long: ${selector.length} > $MAX_SELECTOR_LENGTH"
        }

        // Must be in format "type:pattern"
        val parts = selector.split(":", limit = 2)
        require(parts.size == 2) {
            "Invalid selector format. Expected 'type:pattern', got: $selector"
        }

        val type = parts[0].lowercase()
        require(type in listOf("class", "id", "text", "desc")) {
            "Invalid selector type. Expected class/id/text/desc, got: $type"
        }

        // Check for path traversal
        require(!selector.contains("../")) {
            "Selector contains path traversal pattern"
        }
    }

    /**
     * Validate node ID
     * @throws IllegalArgumentException if invalid
     */
    fun validateNodeId(nodeId: String?) {
        require(!nodeId.isNullOrBlank()) { "Node ID cannot be null or empty" }
        require(nodeId.length <= MAX_NODE_ID_LENGTH) {
            "Node ID too long: ${nodeId.length} > $MAX_NODE_ID_LENGTH"
        }
        require(NODE_ID_PATTERN.matcher(nodeId).matches()) {
            "Invalid node ID format: $nodeId"
        }
        require(!nodeId.contains("../")) {
            "Node ID contains path traversal pattern"
        }
    }

    /**
     * Validate scroll direction
     * @throws IllegalArgumentException if invalid
     */
    fun validateScrollDirection(direction: String?) {
        require(!direction.isNullOrBlank()) { "Scroll direction cannot be null or empty" }
        val normalized = direction.lowercase()
        require(normalized in listOf("up", "down", "left", "right")) {
            "Invalid scroll direction. Expected up/down/left/right, got: $direction"
        }
    }

    /**
     * Validate scroll distance
     * @throws IllegalArgumentException if invalid
     */
    fun validateDistance(distance: Int) {
        require(distance >= 0) { "Distance cannot be negative: $distance" }
        require(distance <= MAX_DISTANCE) {
            "Distance too large: $distance > $MAX_DISTANCE"
        }
    }

    /**
     * Validate bounds
     * @throws IllegalArgumentException if invalid
     */
    fun validateBounds(left: Int, top: Int, right: Int, bottom: Int) {
        require(left >= 0) { "Left bound cannot be negative: $left" }
        require(top >= 0) { "Top bound cannot be negative: $top" }
        require(right >= left) { "Right bound must be >= left: right=$right, left=$left" }
        require(bottom >= top) { "Bottom bound must be >= top: bottom=$bottom, top=$top" }

        require(right <= MAX_BOUNDS_DIMENSION) {
            "Right bound too large: $right > $MAX_BOUNDS_DIMENSION"
        }
        require(bottom <= MAX_BOUNDS_DIMENSION) {
            "Bottom bound too large: $bottom > $MAX_BOUNDS_DIMENSION"
        }

        // Sanity check: bounds shouldn't be absurdly large
        val width = right - left
        val height = bottom - top
        require(width <= MAX_BOUNDS_DIMENSION) {
            "Width too large: $width > $MAX_BOUNDS_DIMENSION"
        }
        require(height <= MAX_BOUNDS_DIMENSION) {
            "Height too large: $height > $MAX_BOUNDS_DIMENSION"
        }
    }
}
