/**
 * SecurityManager.android.kt - Android security validation for JIT Service
 *
 * Android-specific security layer for caller verification.
 * Migrated from JITLearning library.
 *
 * SECURITY (2025-12-12): Comprehensive security layer to prevent:
 * - Unauthorized access (CVE 7.8/10 HIGH)
 * - Caller impersonation attacks
 *
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 * Author: Manoj Jhawar
 * Created: 2026-01-16
 * Migrated from: JITLearning/SecurityValidator.kt
 *
 * @since 3.0.0 (KMP Migration)
 */

package com.augmentalis.voiceoscoreng.jit

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.util.Log

/**
 * Security Manager for JITLearningService (Android)
 *
 * Handles caller verification via:
 * 1. UID + permission checking
 * 2. Signature verification
 *
 * SECURITY: This must be called at the start of EVERY AIDL method.
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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

    /**
     * Check if caller has permission without throwing.
     *
     * @return true if caller has valid permission and signature
     */
    fun hasCallerPermission(): Boolean {
        return try {
            verifyCallerPermission()
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
