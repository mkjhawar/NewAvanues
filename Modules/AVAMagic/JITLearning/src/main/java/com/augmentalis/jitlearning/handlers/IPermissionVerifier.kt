/**
 * IPermissionVerifier.kt - Interface for permission verification
 *
 * Handles UID verification, signature checks, and permission validation.
 * Extracted from JITLearningService as part of SOLID refactoring.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Track C)
 *
 * @since 2.2.0 (SOLID Refactoring)
 */

package com.augmentalis.jitlearning.handlers

/**
 * Permission Verifier Interface
 *
 * Responsibilities:
 * - Verify caller UID matches expected process
 * - Check package signature matches VoiceOS
 * - Validate AIDL caller permissions
 * - Enforce security policies
 *
 * Single Responsibility: Security and permission validation
 */
interface IPermissionVerifier {
    /**
     * Verify caller has permission to access service.
     *
     * Checks UID and signature against expected values.
     *
     * @throws SecurityException if caller not authorized
     */
    fun verifyCallerPermission()

    /**
     * Check if package signature matches expected signature.
     *
     * @param packageName Package to verify
     * @return True if signature matches
     */
    fun checkSignature(packageName: String): Boolean

    /**
     * Get caller UID.
     *
     * @return Calling process UID
     */
    fun getCallerUid(): Int

    /**
     * Get caller package name.
     *
     * @return Package name of caller or null
     */
    fun getCallerPackageName(): String?

    /**
     * Check if caller is VoiceOSCore process.
     *
     * @return True if caller is VoiceOSCore
     */
    fun isVoiceOSCore(): Boolean

    /**
     * Check if caller is LearnApp process.
     *
     * @return True if caller is LearnApp
     */
    fun isLearnApp(): Boolean
}
