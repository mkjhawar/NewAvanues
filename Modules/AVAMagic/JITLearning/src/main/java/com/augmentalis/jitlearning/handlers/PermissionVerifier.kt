/**
 * PermissionVerifier.kt - Implementation of permission verification
 *
 * Validates caller permissions for AIDL service access.
 * Extracted from JITLearningService as part of SOLID refactoring.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Track C)
 *
 * @since 2.2.0 (SOLID Refactoring)
 */

package com.augmentalis.jitlearning.handlers

import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Process
import android.util.Log

/**
 * Permission Verifier
 *
 * Validates caller permissions for service access.
 *
 * Security Policy:
 * - Only VoiceOSCore and LearnApp can access service
 * - Caller UID must match expected process
 * - Package signature must match VoiceOS signature
 *
 * Thread Safety: Thread-safe
 */
class PermissionVerifier(
    private val context: Context
) : IPermissionVerifier {

    companion object {
        private const val TAG = "PermissionVerifier"
        private const val VOICEOS_PACKAGE = "com.augmentalis.voiceos"
        private const val LEARNAPP_PACKAGE = "com.augmentalis.learnapp"
    }

    override fun verifyCallerPermission() {
        val callingUid = Binder.getCallingUid()
        val myUid = Process.myUid()

        // Allow same process
        if (callingUid == myUid) {
            return
        }

        // Check package name
        val callerPackage = getCallerPackageName()
        if (callerPackage == null) {
            throw SecurityException("Cannot determine caller package")
        }

        // Verify caller is VoiceOS or LearnApp
        if (callerPackage != VOICEOS_PACKAGE && callerPackage != LEARNAPP_PACKAGE) {
            throw SecurityException("Unauthorized package: $callerPackage")
        }

        // Verify signature
        if (!checkSignature(callerPackage)) {
            throw SecurityException("Invalid signature for package: $callerPackage")
        }

        Log.d(TAG, "Caller verified: $callerPackage (UID: $callingUid)")
    }

    override fun checkSignature(packageName: String): Boolean {
        return try {
            val pm = context.packageManager

            // Get caller signature
            val callerInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }

            // Get our signature
            val ourInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            // Compare signatures
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val callerSignatures = callerInfo.signingInfo?.apkContentsSigners
                val ourSignatures = ourInfo.signingInfo?.apkContentsSigners
                callerSignatures?.contentEquals(ourSignatures) == true
            } else {
                @Suppress("DEPRECATION")
                val callerSignatures = callerInfo.signatures
                @Suppress("DEPRECATION")
                val ourSignatures = ourInfo.signatures
                callerSignatures?.contentEquals(ourSignatures) == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check signature for $packageName", e)
            false
        }
    }

    override fun getCallerUid(): Int {
        return Binder.getCallingUid()
    }

    override fun getCallerPackageName(): String? {
        val uid = getCallerUid()
        val pm = context.packageManager
        return pm.getNameForUid(uid)
    }

    override fun isVoiceOSCore(): Boolean {
        val callerPackage = getCallerPackageName()
        return callerPackage == VOICEOS_PACKAGE
    }

    override fun isLearnApp(): Boolean {
        val callerPackage = getCallerPackageName()
        return callerPackage == LEARNAPP_PACKAGE
    }
}
