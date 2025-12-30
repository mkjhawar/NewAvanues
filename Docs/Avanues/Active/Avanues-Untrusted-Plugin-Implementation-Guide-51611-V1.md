<!--
Filename: Untrusted-Plugin-Implementation-Guide.md
Created: 2025-10-26
Project: AvaCode Plugin Infrastructure
Purpose: Implementation guide for untrusted plugin support with enforcement
Last Modified: 2025-10-26
Version: v1.0.0
-->

# Untrusted Plugin Implementation Guide

**Date:** 2025-10-26
**Context:** Optional enhancement for supporting plugins from unverified developers
**Prerequisite:** Read Sandboxing-Analysis-251026.md first

---

## Executive Summary

This guide explains how to add support for **untrusted plugins** (from unverified developers) with proper enforcement and isolation. This is an **optional enhancement** - the system already works for verified/registered developers.

**Key Concepts:**
1. **Developer Verification Level** - Classify plugins by trust level
2. **Isolated Process Mode** - Run untrusted plugins in separate processes (Android only)
3. **Enforcement Points** - Where to check and enforce trust levels
4. **User Warnings** - UI indicators for untrusted plugins

---

## Table of Contents

1. [Trust Level System](#trust-level-system)
2. [Android Isolated Process Implementation](#android-isolated-process-implementation)
3. [Enforcement Architecture](#enforcement-architecture)
4. [User Experience & Warnings](#user-experience--warnings)
5. [Platform-Specific Strategies](#platform-specific-strategies)
6. [Testing & Security Audit](#testing--security-audit)

---

## 1. Trust Level System

### 1.1 Define Trust Levels

Add to `PluginManifest.kt`:

```kotlin
/**
 * Developer verification level for tiered security.
 *
 * See FR-021 for full specification.
 */
enum class DeveloperVerificationLevel {
    /**
     * Verified developer (highest trust).
     *
     * - Manual code review required
     * - For high-risk categories (accessibility, payments, camera, location)
     * - Full system access after approval
     */
    VERIFIED,

    /**
     * Registered developer (medium-high trust).
     *
     * - Code signing with verified identity
     * - Selective review for high-risk categories
     * - Tamper detection via digital signatures
     */
    REGISTERED,

    /**
     * Unverified developer (lowest trust).
     *
     * - No code review or identity verification
     * - Sandboxing enforced (ClassLoader + process isolation on Android)
     * - Limited to low-risk operations
     * - User warnings displayed
     */
    UNVERIFIED,

    /**
     * First-party plugin (full trust).
     *
     * - Developed internally by app team
     * - Same trust as host application
     * - No restrictions
     */
    FIRST_PARTY
}

/**
 * Isolation mode for plugin execution.
 */
enum class IsolationMode {
    /**
     * In-process execution (default).
     *
     * - Plugin runs in same process as host app
     * - ClassLoader isolation only
     * - Fast, low overhead
     * - Used for VERIFIED, REGISTERED, FIRST_PARTY plugins
     */
    IN_PROCESS,

    /**
     * Isolated process execution (Android only).
     *
     * - Plugin runs in separate Android process
     * - Full memory isolation via OS
     * - IPC overhead (slower)
     * - Used for UNVERIFIED plugins on Android
     */
    ISOLATED_PROCESS,

    /**
     * Containerized execution (future).
     *
     * - Plugin runs in Docker container or VM
     * - Maximum isolation
     * - High overhead
     * - Not currently implemented
     */
    CONTAINERIZED
}
```

### 1.2 Update PluginManifest

Add verification metadata:

```kotlin
@Serializable
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val entrypoint: String,
    val capabilities: List<String>,
    val resources: Map<String, String>,
    val dependencies: List<PluginDependency> = emptyList(),
    val permissions: List<Permission> = emptyList(),
    val manifestVersion: String,

    // NEW: Developer verification metadata
    val developerInfo: DeveloperInfo? = null,
    val signature: PluginSignature? = null
) {
    /**
     * Get effective verification level for this plugin.
     *
     * Determines trust level and isolation mode.
     */
    fun getVerificationLevel(): DeveloperVerificationLevel {
        // First-party plugins (bundled with app)
        if (developerInfo?.isFirstParty == true) {
            return DeveloperVerificationLevel.FIRST_PARTY
        }

        // Verified developers (manual review completed)
        if (developerInfo?.isVerified == true) {
            return DeveloperVerificationLevel.VERIFIED
        }

        // Registered developers (code signing verified)
        if (signature != null && signature.isValid) {
            return DeveloperVerificationLevel.REGISTERED
        }

        // Default: Unverified
        return DeveloperVerificationLevel.UNVERIFIED
    }

    /**
     * Get required isolation mode for this plugin.
     *
     * Based on verification level and platform capabilities.
     */
    fun getIsolationMode(platform: Platform): IsolationMode {
        return when (getVerificationLevel()) {
            DeveloperVerificationLevel.FIRST_PARTY,
            DeveloperVerificationLevel.VERIFIED,
            DeveloperVerificationLevel.REGISTERED -> {
                // Trusted plugins: in-process execution
                IsolationMode.IN_PROCESS
            }
            DeveloperVerificationLevel.UNVERIFIED -> {
                // Untrusted plugins: isolated process on Android, reject on others
                when (platform) {
                    Platform.ANDROID -> IsolationMode.ISOLATED_PROCESS
                    Platform.IOS -> throw UnsupportedOperationException(
                        "Unverified plugins not supported on iOS (App Store restriction)"
                    )
                    Platform.JVM -> throw UnsupportedOperationException(
                        "Unverified plugins not recommended on desktop (no OS sandboxing)"
                    )
                }
            }
        }
    }
}

@Serializable
data class DeveloperInfo(
    val developerId: String,
    val developerName: String,
    val email: String? = null,
    val website: String? = null,
    val isFirstParty: Boolean = false,
    val isVerified: Boolean = false,
    val verificationDate: Long? = null,
    val reviewerNotes: String? = null
)

@Serializable
data class PluginSignature(
    val algorithm: String,  // "RSA-SHA256", "ECDSA-SHA256"
    val publicKeyFingerprint: String,
    val signatureData: String,  // Base64-encoded signature
    val certificateChain: List<String>? = null,
    val isValid: Boolean = false
)

enum class Platform {
    ANDROID, IOS, JVM
}
```

---

## 2. Android Isolated Process Implementation

### 2.1 Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                      Host App Process                     │
│  ┌─────────────────────────────────────────────────────┐ │
│  │           PluginRegistry (Main Thread)              │ │
│  │                                                      │ │
│  │  [Trusted Plugin A] ← In-process (fast)             │ │
│  │  [Trusted Plugin B] ← In-process (fast)             │ │
│  └──────────────────┬──────────────────────────────────┘ │
│                     │ Binder IPC                          │
└─────────────────────┼─────────────────────────────────────┘
                      │
                      ↓
┌─────────────────────────────────────────────────────────┐
│               Isolated Plugin Process (UID: 10001)       │
│  ┌─────────────────────────────────────────────────────┐ │
│  │           PluginSandboxService                       │ │
│  │                                                      │ │
│  │  [Untrusted Plugin X] ← Isolated (slow but safe)    │ │
│  └─────────────────────────────────────────────────────┘ │
│                                                           │
│  Restricted access:                                       │
│  - Cannot read host app memory                            │
│  - Cannot access other plugins                            │
│  - Limited file system access                             │
│  - All calls go through IPC                               │
└─────────────────────────────────────────────────────────┘
```

### 2.2 Implementation: AndroidPluginSandbox.kt

Create file: `runtime/plugin-system/src/androidMain/kotlin/com/augmentalis/avacode/plugins/platform/AndroidPluginSandbox.kt`

```kotlin
package com.augmentalis.avacode.plugins.platform

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import com.augmentalis.avacode.plugins.core.PluginLog

/**
 * Android implementation of plugin sandboxing using isolated processes.
 *
 * For UNVERIFIED plugins only. Trusted plugins run in-process.
 *
 * ## Architecture
 * - Untrusted plugins run in separate Android process (android:process=":plugin_sandbox")
 * - Communication via Binder IPC (AIDL)
 * - Memory isolation enforced by OS
 * - Crash isolation (plugin crash doesn't crash host app)
 *
 * ## Performance Trade-offs
 * - IPC overhead: ~0.1-1ms per call (10-100x slower than in-process)
 * - Memory overhead: ~50-100MB per isolated process
 * - Startup latency: ~100-500ms to spawn process
 *
 * ## Security Benefits
 * - Complete memory isolation
 * - Cannot read host app memory
 * - Cannot access other plugins
 * - Crash doesn't affect host app
 * - Resource limits (CPU, memory) can be enforced
 */
class AndroidPluginSandbox(
    private val context: Context
) {
    companion object {
        private const val TAG = "AndroidPluginSandbox"
    }

    private val connections = mutableMapOf<String, SandboxConnection>()

    /**
     * Load plugin in isolated process.
     *
     * @param pluginId Plugin identifier
     * @param pluginPath Path to plugin APK/JAR
     * @param className Entrypoint class name
     * @return SandboxedPlugin proxy
     */
    suspend fun loadPlugin(
        pluginId: String,
        pluginPath: String,
        className: String
    ): SandboxedPlugin {
        PluginLog.i(TAG, "Loading plugin $pluginId in isolated process")

        // Check if already connected
        val existingConnection = connections[pluginId]
        if (existingConnection != null && existingConnection.isConnected) {
            PluginLog.d(TAG, "Reusing existing sandbox connection for $pluginId")
            return SandboxedPlugin(pluginId, existingConnection.service!!)
        }

        // Create connection to isolated process
        val connection = SandboxConnection(pluginId)
        val intent = Intent(context, PluginSandboxService::class.java).apply {
            putExtra("pluginId", pluginId)
            putExtra("pluginPath", pluginPath)
            putExtra("className", className)
        }

        // Bind to isolated service
        val bound = context.bindService(
            intent,
            connection,
            Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT
        )

        if (!bound) {
            throw IllegalStateException("Failed to bind to sandbox service for $pluginId")
        }

        // Wait for connection
        connection.waitForConnection()

        connections[pluginId] = connection
        return SandboxedPlugin(pluginId, connection.service!!)
    }

    /**
     * Unload plugin and terminate isolated process.
     *
     * @param pluginId Plugin identifier
     */
    fun unloadPlugin(pluginId: String) {
        PluginLog.i(TAG, "Unloading sandboxed plugin $pluginId")

        val connection = connections.remove(pluginId)
        if (connection != null) {
            try {
                connection.service?.unload()
            } catch (e: Exception) {
                PluginLog.e(TAG, "Error unloading plugin $pluginId", e)
            }
            context.unbindService(connection)
        }
    }

    /**
     * Connection to isolated plugin service.
     */
    private class SandboxConnection(
        private val pluginId: String
    ) : ServiceConnection {
        private val latch = java.util.concurrent.CountDownLatch(1)
        var service: IPluginSandboxService? = null
        var isConnected = false

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            PluginLog.d(TAG, "Sandbox service connected for $pluginId")
            service = IPluginSandboxService.Stub.asInterface(binder)
            isConnected = true
            latch.countDown()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            PluginLog.w(TAG, "Sandbox service disconnected for $pluginId")
            service = null
            isConnected = false
        }

        fun waitForConnection() {
            if (!latch.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                throw IllegalStateException("Timeout waiting for sandbox connection: $pluginId")
            }
        }
    }
}

/**
 * Proxy for sandboxed plugin running in isolated process.
 *
 * All method calls go through Binder IPC.
 */
class SandboxedPlugin(
    private val pluginId: String,
    private val service: IPluginSandboxService
) {
    companion object {
        private const val TAG = "SandboxedPlugin"
    }

    /**
     * Invoke plugin method via IPC.
     *
     * @param methodName Method to invoke
     * @param args Serialized arguments (JSON)
     * @return Serialized result (JSON)
     */
    suspend fun invoke(methodName: String, args: String): String {
        PluginLog.d(TAG, "Invoking $pluginId.$methodName via IPC")

        return try {
            service.invokeMethod(methodName, args)
        } catch (e: RemoteException) {
            PluginLog.e(TAG, "IPC error invoking $pluginId.$methodName", e)
            throw IllegalStateException("Plugin crashed or unavailable: $pluginId", e)
        }
    }

    /**
     * Get plugin info.
     */
    fun getInfo(): String {
        return service.getPluginInfo()
    }
}
```

### 2.3 AIDL Interface Definition

Create file: `runtime/plugin-system/src/androidMain/aidl/com/augmentalis/avacode/plugins/platform/IPluginSandboxService.aidl`

```java
package com.augmentalis.avacode.plugins.platform;

/**
 * AIDL interface for isolated plugin process communication.
 *
 * This interface defines the IPC contract between the host app
 * and the isolated plugin sandbox process.
 */
interface IPluginSandboxService {
    /**
     * Load plugin in isolated process.
     *
     * @param pluginPath Path to plugin APK/JAR
     * @param className Entrypoint class name
     * @return Success status
     */
    boolean loadPlugin(String pluginPath, String className);

    /**
     * Invoke plugin method.
     *
     * @param methodName Method to invoke
     * @param args Serialized arguments (JSON)
     * @return Serialized result (JSON)
     */
    String invokeMethod(String methodName, String args);

    /**
     * Get plugin info (name, version, etc.).
     *
     * @return JSON-serialized plugin info
     */
    String getPluginInfo();

    /**
     * Unload plugin and release resources.
     */
    void unload();
}
```

### 2.4 Isolated Service Implementation

Create file: `runtime/plugin-system/src/androidMain/kotlin/com/augmentalis/avacode/plugins/platform/PluginSandboxService.kt`

```kotlin
package com.augmentalis.avacode.plugins.platform

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.augmentalis.avacode.plugins.core.PluginLog
import dalvik.system.DexClassLoader
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Android service running in isolated process for untrusted plugins.
 *
 * Declared in AndroidManifest.xml with android:process=":plugin_sandbox"
 * and android:isolatedProcess="true" for maximum isolation.
 */
class PluginSandboxService : Service() {
    companion object {
        private const val TAG = "PluginSandboxService"
    }

    private val binder = PluginSandboxBinder()
    private var classLoader: DexClassLoader? = null
    private var pluginInstance: Any? = null

    override fun onBind(intent: Intent?): IBinder {
        PluginLog.i(TAG, "PluginSandboxService onBind (isolated process)")

        // Extract plugin info from intent
        val pluginId = intent?.getStringExtra("pluginId")
        val pluginPath = intent?.getStringExtra("pluginPath")
        val className = intent?.getStringExtra("className")

        if (pluginPath != null && className != null) {
            loadPluginInternal(pluginPath, className)
        }

        return binder
    }

    private fun loadPluginInternal(pluginPath: String, className: String): Boolean {
        return try {
            PluginLog.i(TAG, "Loading plugin in isolated process: $className")

            // Create optimized dex directory
            val optimizedDir = File(cacheDir, "dex_opt")
            if (!optimizedDir.exists()) {
                optimizedDir.mkdirs()
            }

            // Create DexClassLoader
            classLoader = DexClassLoader(
                pluginPath,
                optimizedDir.absolutePath,
                null,
                this::class.java.classLoader
            )

            // Load and instantiate plugin class
            val clazz = classLoader!!.loadClass(className)
            pluginInstance = clazz.getDeclaredConstructor().newInstance()

            PluginLog.i(TAG, "Plugin loaded successfully in isolated process")
            true
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to load plugin in isolated process", e)
            false
        }
    }

    /**
     * AIDL Binder implementation.
     */
    private inner class PluginSandboxBinder : IPluginSandboxService.Stub() {
        override fun loadPlugin(pluginPath: String, className: String): Boolean {
            return loadPluginInternal(pluginPath, className)
        }

        override fun invokeMethod(methodName: String, args: String): String {
            if (pluginInstance == null) {
                throw IllegalStateException("Plugin not loaded")
            }

            return try {
                // Use reflection to invoke method
                val method = pluginInstance!!::class.java.getMethod(
                    methodName,
                    String::class.java
                )
                val result = method.invoke(pluginInstance, args)

                // Serialize result to JSON
                Json.encodeToString(result)
            } catch (e: Exception) {
                PluginLog.e(TAG, "Error invoking plugin method: $methodName", e)
                Json.encodeToString(mapOf("error" to (e.message ?: "Unknown error")))
            }
        }

        override fun getPluginInfo(): String {
            return Json.encodeToString(mapOf(
                "loaded" to (pluginInstance != null),
                "className" to (pluginInstance?.javaClass?.name ?: "none")
            ))
        }

        override fun unload() {
            PluginLog.i(TAG, "Unloading plugin from isolated process")
            pluginInstance = null
            classLoader = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PluginLog.i(TAG, "PluginSandboxService destroyed")
        binder.unload()
    }
}
```

### 2.5 AndroidManifest.xml Configuration

Add to your app's `AndroidManifest.xml`:

```xml
<manifest>
    <application>
        <!-- Isolated plugin sandbox service -->
        <service
            android:name="com.augmentalis.avacode.plugins.platform.PluginSandboxService"
            android:process=":plugin_sandbox"
            android:isolatedProcess="true"
            android:exported="false"
            android:permission="android.permission.BIND_JOB_SERVICE">

            <!-- Restrict to same-app access only -->
            <intent-filter>
                <action android:name="com.augmentalis.avacode.PLUGIN_SANDBOX" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

**Key attributes:**
- `android:process=":plugin_sandbox"` - Runs in separate process
- `android:isolatedProcess="true"` - Maximum isolation (no storage access, limited IPC)
- `android:exported="false"` - Only accessible by same app
- `android:permission` - Requires permission to bind

---

## 3. Enforcement Architecture

### 3.1 PluginLoader Integration

Update `PluginLoader.kt` to enforce trust levels:

```kotlin
class PluginLoader(
    private val pluginBaseDir: String,
    private val registry: PluginRegistry,
    private val permissionManager: PermissionManager,
    private val androidSandbox: AndroidPluginSandbox? = null  // NEW: Android only
) {
    /**
     * Load plugin with trust level enforcement.
     */
    suspend fun loadPlugin(
        manifest: PluginManifest,
        namespace: PluginNamespace
    ): LoadResult {
        // Step 1: Determine verification level
        val verificationLevel = manifest.getVerificationLevel()

        PluginLog.i(TAG, "Loading plugin ${manifest.id} (trust: $verificationLevel)")

        // Step 2: Enforce platform-specific restrictions
        val platform = getCurrentPlatform()

        if (verificationLevel == DeveloperVerificationLevel.UNVERIFIED) {
            when (platform) {
                Platform.IOS -> {
                    return LoadResult.Failure(
                        "Unverified plugins are not supported on iOS due to App Store restrictions. " +
                        "Plugin must be verified or registered."
                    )
                }
                Platform.JVM -> {
                    // Warn but allow (no good sandboxing options on desktop)
                    PluginLog.w(TAG, "Loading unverified plugin on desktop - limited sandboxing available")
                    // Could prompt user to confirm
                }
                Platform.ANDROID -> {
                    // Will use isolated process (handled below)
                }
            }
        }

        // Step 3: Load plugin with appropriate isolation
        val isolationMode = try {
            manifest.getIsolationMode(platform)
        } catch (e: UnsupportedOperationException) {
            return LoadResult.Failure(e.message ?: "Platform does not support this plugin")
        }

        return when (isolationMode) {
            IsolationMode.IN_PROCESS -> {
                // Trusted plugin: standard ClassLoader-based loading
                loadPluginInProcess(manifest, namespace)
            }
            IsolationMode.ISOLATED_PROCESS -> {
                // Untrusted plugin: isolated process (Android only)
                if (androidSandbox == null) {
                    return LoadResult.Failure("Isolated process mode not available on this platform")
                }
                loadPluginIsolated(manifest, namespace, androidSandbox)
            }
            IsolationMode.CONTAINERIZED -> {
                return LoadResult.Failure("Containerized mode not yet implemented")
            }
        }
    }

    /**
     * Load trusted plugin in-process (standard path).
     */
    private suspend fun loadPluginInProcess(
        manifest: PluginManifest,
        namespace: PluginNamespace
    ): LoadResult {
        // Existing implementation
        val classLoader = PluginClassLoader()
        val pluginPath = getPluginPath(manifest.id, namespace)

        return try {
            val instance = classLoader.loadClass(manifest.entrypoint, pluginPath)
            LoadResult.Success(instance)
        } catch (e: Exception) {
            LoadResult.Failure("Failed to load plugin: ${e.message}")
        }
    }

    /**
     * Load untrusted plugin in isolated process (Android only).
     */
    private suspend fun loadPluginIsolated(
        manifest: PluginManifest,
        namespace: PluginNamespace,
        sandbox: AndroidPluginSandbox
    ): LoadResult {
        val pluginPath = getPluginPath(manifest.id, namespace)

        return try {
            val sandboxedPlugin = sandbox.loadPlugin(
                pluginId = manifest.id,
                pluginPath = pluginPath,
                className = manifest.entrypoint
            )

            PluginLog.i(TAG, "Plugin ${manifest.id} loaded in isolated process")
            LoadResult.Success(sandboxedPlugin)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to load plugin in isolated process", e)
            LoadResult.Failure("Failed to load plugin in sandbox: ${e.message}")
        }
    }
}
```

### 3.2 Manifest Validation

Add validation at installation time:

```kotlin
class PluginInstaller(
    private val registry: PluginRegistry,
    private val signatureVerifier: SignatureVerifier
) {
    /**
     * Install plugin with trust level validation.
     */
    suspend fun installPlugin(
        packagePath: String,
        trustStore: TrustStore
    ): InstallResult {
        // Step 1: Extract and parse manifest
        val manifest = extractManifest(packagePath)
            ?: return InstallResult.Failure("Invalid plugin package: manifest not found")

        // Step 2: Verify signature if present
        val signature = manifest.signature
        if (signature != null) {
            when (val result = signatureVerifier.verify(packagePath, signature, trustStore)) {
                is SignatureVerifier.VerificationResult.Valid -> {
                    PluginLog.i(TAG, "Signature verified for ${manifest.id}")
                }
                is SignatureVerifier.VerificationResult.Invalid -> {
                    return InstallResult.Failure(
                        "Signature verification failed: ${result.reason}"
                    )
                }
            }
        }

        // Step 3: Determine verification level
        val verificationLevel = manifest.getVerificationLevel()

        // Step 4: Platform-specific validation
        val platform = getCurrentPlatform()

        try {
            manifest.getIsolationMode(platform)
        } catch (e: UnsupportedOperationException) {
            // Reject plugin if platform doesn't support required isolation
            return InstallResult.Failure(e.message ?: "Platform compatibility issue")
        }

        // Step 5: Get user consent for untrusted plugins
        if (verificationLevel == DeveloperVerificationLevel.UNVERIFIED) {
            val userApproved = showUntrustedPluginWarning(manifest)
            if (!userApproved) {
                return InstallResult.Cancelled("User declined to install unverified plugin")
            }
        }

        // Step 6: Proceed with installation
        return proceedWithInstallation(manifest, packagePath)
    }

    /**
     * Show warning dialog for unverified plugins.
     */
    private suspend fun showUntrustedPluginWarning(manifest: PluginManifest): Boolean {
        // Implementation depends on UI framework
        // For Android, use AlertDialog
        // For JVM, use Swing dialog
        // For iOS, reject (not supported)

        return false  // Placeholder
    }
}
```

---

## 4. User Experience & Warnings

### 4.1 Visual Indicators

Display trust level in plugin UI:

```kotlin
/**
 * Get visual indicator for plugin trust level.
 */
fun getTrustIndicator(manifest: PluginManifest): TrustIndicator {
    return when (manifest.getVerificationLevel()) {
        DeveloperVerificationLevel.FIRST_PARTY -> TrustIndicator(
            icon = "✓",
            color = "#4CAF50",  // Green
            label = "First-Party",
            description = "Developed by app team"
        )
        DeveloperVerificationLevel.VERIFIED -> TrustIndicator(
            icon = "✓",
            color = "#2196F3",  // Blue
            label = "Verified",
            description = "Manually reviewed and approved"
        )
        DeveloperVerificationLevel.REGISTERED -> TrustIndicator(
            icon = "⚠",
            color = "#FF9800",  // Orange
            label = "Registered",
            description = "Code-signed, identity verified"
        )
        DeveloperVerificationLevel.UNVERIFIED -> TrustIndicator(
            icon = "⚠",
            color = "#F44336",  // Red
            label = "Unverified",
            description = "From unknown developer - runs in sandbox"
        )
    }
}

data class TrustIndicator(
    val icon: String,
    val color: String,
    val label: String,
    val description: String
)
```

### 4.2 Installation Warning Dialog

Example for Android:

```kotlin
fun showUntrustedPluginWarningDialog(
    context: Context,
    manifest: PluginManifest
): Boolean {
    return suspendCoroutine { continuation ->
        AlertDialog.Builder(context)
            .setTitle("⚠ Unverified Plugin")
            .setMessage("""
                You are about to install a plugin from an UNVERIFIED developer.

                Plugin: ${manifest.name}
                Developer: ${manifest.author}

                Risks:
                • This plugin has NOT been reviewed
                • Developer identity is NOT verified
                • May contain malicious code

                Protection:
                ✓ Plugin will run in isolated process
                ✓ Limited access to your data
                ✓ Cannot access other apps

                Only install if you trust the source.
            """.trimIndent())
            .setPositiveButton("Install Anyway") { _, _ ->
                continuation.resume(true)
            }
            .setNegativeButton("Cancel") { _, _ ->
                continuation.resume(false)
            }
            .setCancelable(false)
            .show()
    }
}
```

---

## 5. Platform-Specific Strategies

### 5.1 Android Strategy

**Enforcement:**
1. ✅ **IN_PROCESS** for VERIFIED/REGISTERED/FIRST_PARTY plugins
2. ✅ **ISOLATED_PROCESS** for UNVERIFIED plugins
3. ✅ User warning dialog before installation
4. ✅ Visual indicator in plugin list

**Implementation Checklist:**
- [ ] Create `AndroidPluginSandbox.kt`
- [ ] Define `IPluginSandboxService.aidl`
- [ ] Implement `PluginSandboxService`
- [ ] Add AndroidManifest.xml service declaration
- [ ] Update PluginLoader to use sandbox for unverified plugins
- [ ] Create warning dialog UI
- [ ] Add trust level badges to plugin UI

**Estimated Effort:** 3-4 days

### 5.2 iOS Strategy

**Enforcement:**
1. ✅ **REJECT** all UNVERIFIED plugins
2. ✅ Only allow VERIFIED/REGISTERED/FIRST_PARTY
3. ✅ Show error message explaining App Store restrictions

**Implementation Checklist:**
- [ ] Update PluginInstaller to reject unverified plugins on iOS
- [ ] Add clear error message
- [ ] Document iOS plugin requirements for developers

**Estimated Effort:** 1 hour (just validation logic)

### 5.3 JVM Strategy

**Enforcement:**
1. ⚠ **WARN** but allow UNVERIFIED plugins
2. ✅ Display strong warning dialog
3. ✅ Require user confirmation with checkbox: "I understand the risks"
4. ⚠ Consider disabling by default (require config flag to enable)

**Implementation Checklist:**
- [ ] Update PluginInstaller with warning dialog (Swing)
- [ ] Add configuration flag: `allowUnverifiedPluginsOnDesktop = false` (default)
- [ ] Document risks in user documentation
- [ ] Consider adding option to run in Docker container (advanced users)

**Estimated Effort:** 2 days

---

## 6. Testing & Security Audit

### 6.1 Test Scenarios

**Test 1: Verified Plugin Loads In-Process**
```kotlin
@Test
fun testVerifiedPluginLoadsInProcess() = runTest {
    val manifest = createManifest(
        verificationLevel = DeveloperVerificationLevel.VERIFIED
    )

    val result = pluginLoader.loadPlugin(manifest, namespace)

    assertTrue(result is LoadResult.Success)
    // Verify plugin is NOT in isolated process
    assertFalse(result.instance is SandboxedPlugin)
}
```

**Test 2: Unverified Plugin Loads in Isolated Process (Android)**
```kotlin
@Test
fun testUnverifiedPluginUsesIsolatedProcess() = runTest {
    val manifest = createManifest(
        verificationLevel = DeveloperVerificationLevel.UNVERIFIED
    )

    val result = pluginLoader.loadPlugin(manifest, namespace)

    assertTrue(result is LoadResult.Success)
    // Verify plugin IS in isolated process
    assertTrue(result.instance is SandboxedPlugin)
}
```

**Test 3: Unverified Plugin Rejected on iOS**
```kotlin
@Test
fun testUnverifiedPluginRejectedOnIOS() = runTest {
    val manifest = createManifest(
        verificationLevel = DeveloperVerificationLevel.UNVERIFIED
    )

    val result = pluginInstaller.installPlugin(manifest, Platform.IOS)

    assertTrue(result is InstallResult.Failure)
    assertTrue(result.reason.contains("not supported on iOS"))
}
```

**Test 4: Malicious Plugin Cannot Escape Sandbox**
```kotlin
@Test
fun testMaliciousPluginCannotEscapeSandbox() = runTest {
    // Create plugin that tries to:
    // 1. Read host app memory
    // 2. Access other plugin data
    // 3. Write to unauthorized directories
    val maliciousPlugin = createMaliciousPlugin()

    val result = pluginLoader.loadPluginIsolated(maliciousPlugin)

    // Verify all malicious attempts fail
    assertFalse(maliciousPlugin.canReadHostMemory())
    assertFalse(maliciousPlugin.canAccessOtherPlugins())
    assertFalse(maliciousPlugin.canWriteUnauthorized())
}
```

### 6.2 Security Audit Checklist

Before enabling untrusted plugin support in production:

**Code Review:**
- [ ] Review all IPC serialization code (prevent injection attacks)
- [ ] Verify no shared memory between processes
- [ ] Check for race conditions in process lifecycle
- [ ] Ensure proper resource cleanup on plugin unload/crash

**Penetration Testing:**
- [ ] Attempt to escape sandbox (memory access, file system, IPC)
- [ ] Test denial-of-service attacks (resource exhaustion)
- [ ] Verify permission enforcement cannot be bypassed
- [ ] Test IPC message tampering

**Android-Specific:**
- [ ] Verify `isolatedProcess="true"` is set
- [ ] Test with Android security scanners (e.g., MobSF)
- [ ] Verify SELinux policies are enforced
- [ ] Test on multiple Android versions (API 21-34)

**Documentation:**
- [ ] Document security model clearly
- [ ] Provide plugin developer guidelines
- [ ] Create security incident response plan
- [ ] Define plugin review process for "verified" status

**Legal/Compliance:**
- [ ] Review terms of service for plugin marketplace
- [ ] Define liability for malicious plugins
- [ ] Consider plugin insurance
- [ ] Consult legal team on indemnification

---

## 7. Configuration & Deployment

### 7.1 Feature Flag

Add configuration to control untrusted plugin support:

```kotlin
data class PluginSystemConfig(
    /**
     * Allow untrusted plugins from unverified developers.
     *
     * When false, only VERIFIED/REGISTERED/FIRST_PARTY plugins are allowed.
     *
     * Default: false (safer)
     */
    val allowUnverifiedPlugins: Boolean = false,

    /**
     * Show strong warnings for unverified plugins.
     *
     * Default: true (always warn users)
     */
    val warnOnUnverifiedPlugins: Boolean = true,

    /**
     * Require additional confirmation for unverified plugins.
     *
     * Default: true (require checkbox: "I understand the risks")
     */
    val requireExplicitConfirmation: Boolean = true,

    /**
     * Platform-specific sandbox strategy.
     */
    val sandboxStrategy: SandboxStrategy = SandboxStrategy.AUTO
)

enum class SandboxStrategy {
    /**
     * Automatic: Use isolated process on Android, reject on iOS, warn on JVM.
     */
    AUTO,

    /**
     * Strict: Reject unverified plugins on all platforms.
     */
    STRICT,

    /**
     * Permissive: Allow unverified plugins everywhere with warnings (NOT RECOMMENDED).
     */
    PERMISSIVE
}
```

### 7.2 Gradle Build Configuration

Add to `build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        // Enable AIDL for IPC
        buildFeatures {
            aidl = true
        }
    }

    // Create build variant for untrusted plugin support
    flavorDimensions += "trust"
    productFlavors {
        create("verified") {
            dimension = "trust"
            buildConfigField("Boolean", "ALLOW_UNVERIFIED_PLUGINS", "false")
        }
        create("marketplace") {
            dimension = "trust"
            buildConfigField("Boolean", "ALLOW_UNVERIFIED_PLUGINS", "true")
        }
    }
}
```

Usage:
- `./gradlew assembleVerifiedDebug` - Only verified plugins (safer)
- `./gradlew assembleMarketplaceDebug` - Allow unverified plugins (marketplace mode)

---

## 8. Migration Path

### Phase 1: Preparation (1-2 days)
1. Add trust level enums to `PluginManifest`
2. Update validation logic
3. Add configuration flags (disabled by default)
4. Update documentation

### Phase 2: Android Implementation (3-4 days)
1. Create `AndroidPluginSandbox.kt`
2. Define AIDL interface
3. Implement `PluginSandboxService`
4. Update `PluginLoader` integration
5. Add warning dialogs
6. Test isolated process loading

### Phase 3: iOS/JVM Enforcement (1-2 days)
1. Add iOS rejection logic
2. Add JVM warning dialogs
3. Update platform detection
4. Test on all platforms

### Phase 4: Security Audit (3-5 days)
1. Code review
2. Penetration testing
3. Security scanning
4. Fix findings

### Phase 5: Beta Testing (2-3 weeks)
1. Deploy to beta users
2. Monitor crash reports
3. Measure performance impact
4. Gather feedback

### Phase 6: Production Rollout (1 week)
1. Enable via feature flag (5% → 25% → 50% → 100%)
2. Monitor metrics (crash rate, IPC latency, memory usage)
3. Document any issues
4. Provide developer support

**Total Estimated Time:** 6-8 weeks (with thorough testing and audit)

---

## 9. Performance Impact

### Expected Overhead (Android Isolated Process)

| Metric | In-Process | Isolated Process | Overhead |
|--------|------------|------------------|----------|
| **Method Call** | ~0.01ms | ~0.1-1ms | 10-100x slower |
| **Memory** | ~10MB | ~60-110MB | +50-100MB |
| **Startup** | ~10-50ms | ~100-500ms | ~10x slower |
| **Battery** | Baseline | +5-10% | Measurable impact |

### Mitigation Strategies

1. **Batch IPC Calls**
   ```kotlin
   // Bad: Multiple IPC calls
   plugin.setProperty("x", "1")
   plugin.setProperty("y", "2")
   plugin.execute()  // 3 IPC calls

   // Good: Single IPC call
   plugin.executeBatch("""
       {"x": "1", "y": "2", "action": "execute"}
   """)  // 1 IPC call
   ```

2. **Cache Results**
   ```kotlin
   // Cache plugin info to avoid repeated IPC
   val info = plugin.getInfo()  // IPC call
   cache[pluginId] = info
   ```

3. **Async Operations**
   ```kotlin
   // Don't block UI thread waiting for IPC
   launch(Dispatchers.IO) {
       val result = plugin.invoke("heavyOperation", args)
       withContext(Dispatchers.Main) {
           updateUI(result)
       }
   }
   ```

---

## Conclusion

Implementing untrusted plugin support requires:

1. **Trust Level System** - Classify plugins by verification level
2. **Isolated Process (Android)** - Full memory isolation via OS
3. **Enforcement Points** - Reject/warn at installation and load time
4. **User Warnings** - Clear communication of risks
5. **Security Audit** - Professional review before production

**Key Decision:**
- **Do you need untrusted plugin support?**
  - If YES → Implement Android isolated process (3-4 days) + audit (1 week)
  - If NO → Current implementation sufficient (verified/registered only)

**Recommendation:**
- Start with **VERIFIED/REGISTERED plugins only** (current state)
- Add **untrusted plugin support later** if marketplace demand justifies the complexity
- Focus on **developer onboarding** for verified status instead

---

**Created by Manoj Jhawar, manoj@ideahq.net**

**Guide Version:** 1.0.0
**Implementation Time:** 6-8 weeks (full cycle)
**Complexity:** HIGH (Android), LOW (iOS/JVM)

**End of Implementation Guide**
