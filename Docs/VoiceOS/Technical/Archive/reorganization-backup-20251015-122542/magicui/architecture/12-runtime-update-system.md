# MagicUI Runtime Update System
## Dynamic Feature Injection via Encrypted Updates

**Document:** 12 of 12  
**Version:** 1.0  
**Created:** 2025-10-13  
**Status:** Implementation Ready  
**Security Level:** HIGH - Encrypted & Signed  

---

## Executive Summary

The Runtime Update System enables MagicUI to receive encrypted feature updates without requiring app updates. This allows:
- **Hot-loading new components** - Add features dynamically
- **Remote bug fixes** - Fix issues without Play Store updates
- **A/B testing** - Enable features for specific users
- **Gradual rollout** - Progressive feature deployment
- **Emergency responses** - Quick security patches

**Security:** All updates are encrypted (AES-256) and signed (RSA-2048) to prevent tampering.

---

## 1. System Architecture

### 1.1 Update Pipeline

```
Update Server (VOS4 Cloud)
    ↓ (HTTPS + Certificate Pinning)
Encrypted Update Bundle (.mupd file)
    ↓ (Download to device)
Update Validator
    ├─ Signature verification (RSA-2048)
    ├─ Decryption (AES-256-GCM)
    ├─ Version check
    └─ Compatibility check
    ↓
Runtime Injector
    ├─ Load new components
    ├─ Register with MagicUI
    └─ Hot-reload active screens
    ↓
Feature Available
```

### 1.2 Security Layers

```
Layer 1: Transport Security
├─ HTTPS only
├─ Certificate pinning
└─ Anti-MITM protection

Layer 2: Update Integrity
├─ RSA signature verification
├─ Hash validation (SHA-256)
└─ Tamper detection

Layer 3: Update Encryption
├─ AES-256-GCM encryption
├─ Per-device key derivation
└─ Replay attack prevention

Layer 4: Runtime Security
├─ Sandbox execution
├─ Permission checking
└─ Resource isolation
```

---

## 2. Update Package Format

### 2.1 .mupd File Structure

```
MagicUI Update Package (.mupd):
├─ Header (256 bytes)
│  ├─ Magic bytes: "MUPD"
│  ├─ Version: 1.0.0
│  ├─ Target SDK: 26-34
│  ├─ Timestamp: Unix timestamp
│  └─ Signature offset
│
├─ Metadata (JSON, encrypted)
│  ├─ Update ID
│  ├─ Update type (component, theme, fix)
│  ├─ Dependencies
│  ├─ Compatibility matrix
│  └─ Rollback instructions
│
├─ Payload (Encrypted)
│  ├─ Component code (Kotlin bytecode)
│  ├─ Resources (images, strings)
│  ├─ Theme definitions
│  └─ Configuration
│
└─ Signature (RSA-2048)
   └─ Signs entire package
```

### 2.2 Metadata Schema

```json
{
  "updateId": "uuid-v4",
  "version": "1.2.3",
  "type": "component",
  "timestamp": 1697234567,
  "targetDevices": ["all"],
  "minMagicUIVersion": "1.0.0",
  "maxMagicUIVersion": "2.0.0",
  "dependencies": [
    "com.augmentalis.magicui.core >= 1.0.0"
  ],
  "payload": {
    "components": [
      {
        "name": "advancedChart",
        "type": "visual",
        "size": 45678,
        "checksum": "sha256:abc123..."
      }
    ],
    "themes": [],
    "fixes": []
  },
  "rollback": {
    "enabled": true,
    "previousVersion": "1.2.2"
  },
  "flags": {
    "requiresRestart": false,
    "canRollback": true,
    "isExperimental": false
  }
}
```

---

## 3. Implementation

### 3.1 Runtime Update Manager

**File:** `runtime/RuntimeUpdateManager.kt`

```kotlin
package com.augmentalis.magicui.runtime

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Manages runtime updates for MagicUI
 * 
 * Features:
 * - Encrypted update delivery
 * - Signature verification
 * - Hot-loading components
 * - Rollback support
 * - Version management
 */
class RuntimeUpdateManager private constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "RuntimeUpdateManager"
        private const val UPDATE_DIR = "magicui_updates"
        private const val CURRENT_VERSION_KEY = "magic_ui_runtime_version"
        
        @Volatile
        private var instance: RuntimeUpdateManager? = null
        
        fun getInstance(context: Context): RuntimeUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: RuntimeUpdateManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    private val updateDir: File = File(context.filesDir, UPDATE_DIR)
    private val prefs = context.getSharedPreferences("magicui_runtime", Context.MODE_PRIVATE)
    private val componentRegistry = MagicComponentRegistry()
    private val updateQueue = UpdateQueue()
    
    // Public key for signature verification (embedded in app)
    private val publicKey = loadPublicKey()
    
    init {
        updateDir.mkdirs()
    }
    
    /**
     * Check for available updates
     */
    suspend fun checkForUpdates(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion()
            val response = UpdateAPI.checkUpdates(currentVersion)
            
            UpdateCheckResult(
                available = response.available,
                updateInfo = response.updateInfo,
                mandatory = response.mandatory
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check updates", e)
            UpdateCheckResult(available = false)
        }
    }
    
    /**
     * Download and validate update
     */
    suspend fun downloadUpdate(updateId: String): DownloadResult = withContext(Dispatchers.IO) {
        try {
            // Download encrypted package
            val packageFile = UpdateAPI.downloadUpdate(updateId)
            
            // Verify signature
            if (!verifySignature(packageFile)) {
                return@withContext DownloadResult.SignatureInvalid
            }
            
            // Decrypt package
            val decrypted = decryptUpdate(packageFile)
            
            // Validate contents
            if (!validateUpdate(decrypted)) {
                return@withContext DownloadResult.ValidationFailed
            }
            
            // Store for installation
            val storedFile = storeUpdate(decrypted, updateId)
            
            DownloadResult.Success(storedFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            DownloadResult.Failed(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Install update (apply changes)
     */
    suspend fun installUpdate(updateFile: File): InstallResult = withContext(Dispatchers.Main) {
        try {
            // Parse update package
            val update = UpdateParser.parse(updateFile)
            
            // Check compatibility
            if (!isCompatible(update)) {
                return@withContext InstallResult.Incompatible
            }
            
            // Create backup for rollback
            val backup = createBackup()
            
            // Install components
            update.components.forEach { component ->
                try {
                    installComponent(component)
                } catch (e: Exception) {
                    // Rollback on failure
                    restoreBackup(backup)
                    return@withContext InstallResult.Failed(e.message ?: "Component install failed")
                }
            }
            
            // Install themes
            update.themes.forEach { theme ->
                installTheme(theme)
            }
            
            // Apply fixes
            update.fixes.forEach { fix ->
                applyFix(fix)
            }
            
            // Update version
            updateVersion(update.version)
            
            // Notify success
            Log.i(TAG, "Update ${update.version} installed successfully")
            
            InstallResult.Success(update.version)
            
        } catch (e: Exception) {
            Log.e(TAG, "Install failed", e)
            InstallResult.Failed(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Hot-reload active screens with new components
     */
    suspend fun hotReload() = withContext(Dispatchers.Main) {
        // Trigger recomposition of all active MagicScreens
        MagicUIModule.getInstance(context).recomposeAll()
    }
    
    /**
     * Rollback to previous version
     */
    suspend fun rollback(): RollbackResult = withContext(Dispatchers.IO) {
        try {
            val previousVersion = getPreviousVersion()
            if (previousVersion == null) {
                return@withContext RollbackResult.NoPreviousVersion
            }
            
            val backupFile = getBackupFile(previousVersion)
            if (!backupFile.exists()) {
                return@withContext RollbackResult.BackupNotFound
            }
            
            // Restore backup
            restoreBackup(backupFile)
            
            // Update version
            updateVersion(previousVersion)
            
            RollbackResult.Success(previousVersion)
            
        } catch (e: Exception) {
            Log.e(TAG, "Rollback failed", e)
            RollbackResult.Failed(e.message ?: "Unknown error")
        }
    }
    
    // ===== Private Methods =====
    
    private fun verifySignature(packageFile: File): Boolean {
        try {
            // Extract signature from package
            val (data, signature) = extractSignature(packageFile)
            
            // Verify with public key
            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(publicKey)
            sig.update(data)
            
            return sig.verify(signature)
            
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification failed", e)
            return false
        }
    }
    
    private fun decryptUpdate(packageFile: File): ByteArray {
        // Get device-specific key
        val deviceKey = getDeviceKey()
        
        // Decrypt with AES-256-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(deviceKey, "AES")
        
        val encrypted = packageFile.readBytes()
        val iv = encrypted.sliceArray(0..11)  // First 12 bytes
        val ciphertext = encrypted.sliceArray(12 until encrypted.size)
        
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }
    
    private fun getDeviceKey(): ByteArray {
        // Derive device-specific key
        // Uses: Device ID + App signature + Salt
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        
        // Key derivation (PBKDF2)
        val spec = javax.crypto.spec.PBEKeySpec(
            deviceId.toCharArray(),
            "MagicUI-Salt-v1".toByteArray(),
            100000,
            256
        )
        
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
    
    private fun installComponent(component: UpdateComponent) {
        // Load component bytecode
        val classLoader = ComponentClassLoader(
            component.bytecode,
            this::class.java.classLoader
        )
        
        // Instantiate component
        val componentClass = classLoader.loadClass(component.className)
        val instance = componentClass.getDeclaredConstructor().newInstance()
        
        // Register with MagicUI
        componentRegistry.register(component.name, instance)
        
        Log.i(TAG, "Component installed: ${component.name}")
    }
    
    private fun getCurrentVersion(): String {
        return prefs.getString(CURRENT_VERSION_KEY, "1.0.0") ?: "1.0.0"
    }
    
    private fun updateVersion(version: String) {
        prefs.edit().putString(CURRENT_VERSION_KEY, version).apply()
    }
    
    private fun loadPublicKey(): java.security.PublicKey {
        // Public key embedded in app (for signature verification)
        val publicKeyBytes = context.assets.open("magicui_public.key").readBytes()
        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }
}

/**
 * Update check result
 */
data class UpdateCheckResult(
    val available: Boolean,
    val updateInfo: UpdateInfo? = null,
    val mandatory: Boolean = false
)

data class UpdateInfo(
    val updateId: String,
    val version: String,
    val size: Long,
    val description: String,
    val releaseNotes: String
)

/**
 * Download result
 */
sealed class DownloadResult {
    data class Success(val file: File) : DownloadResult()
    object SignatureInvalid : DownloadResult()
    object ValidationFailed : DownloadResult()
    data class Failed(val reason: String) : DownloadResult()
}

/**
 * Install result
 */
sealed class InstallResult {
    data class Success(val version: String) : InstallResult()
    object Incompatible : InstallResult()
    data class Failed(val reason: String) : InstallResult()
}

/**
 * Rollback result
 */
sealed class RollbackResult {
    data class Success(val version: String) : RollbackResult()
    object NoPreviousVersion : RollbackResult()
    object BackupNotFound : RollbackResult()
    data class Failed(val reason: String) : RollbackResult()
}
```

---

## 4. Update API

### 4.1 Update Server API

**File:** `runtime/UpdateAPI.kt`

```kotlin
package com.augmentalis.magicui.runtime

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * API client for MagicUI update server
 */
object UpdateAPI {
    private const val BASE_URL = "https://updates.vos4.com/magicui"
    private const val API_VERSION = "v1"
    
    private val client = OkHttpClient.Builder()
        .certificatePinner(getCertificatePinner())
        .build()
    
    /**
     * Check for available updates
     */
    suspend fun checkUpdates(currentVersion: String): CheckResponse = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/$API_VERSION/check?version=$currentVersion"
        
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "MagicUI/${currentVersion}")
            .addHeader("X-Device-Id", getDeviceId())
            .build()
        
        val response = client.newCall(request).execute()
        
        if (response.isSuccessful) {
            val json = response.body?.string() ?: ""
            parseCheckResponse(json)
        } else {
            CheckResponse(available = false)
        }
    }
    
    /**
     * Download update package
     */
    suspend fun downloadUpdate(updateId: String): File = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/$API_VERSION/download/$updateId"
        
        val request = Request.Builder()
            .url(url)
            .build()
        
        val response = client.newCall(request).execute()
        
        if (response.isSuccessful) {
            val bytes = response.body?.bytes() ?: throw Exception("Empty response")
            
            // Save to temp file
            val tempFile = File.createTempFile("magicui_update_", ".mupd")
            tempFile.writeBytes(bytes)
            tempFile
        } else {
            throw Exception("Download failed: ${response.code}")
        }
    }
    
    /**
     * Report installation status
     */
    suspend fun reportInstallation(
        updateId: String,
        success: Boolean,
        error: String? = null
    ) = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/$API_VERSION/report"
        
        val json = """
        {
            "updateId": "$updateId",
            "success": $success,
            "error": ${error?.let { "\"$it\"" } ?: "null"},
            "timestamp": ${System.currentTimeMillis()},
            "deviceId": "${getDeviceId()}"
        }
        """.trimIndent()
        
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody())
            .build()
        
        client.newCall(request).execute()
    }
    
    private fun getCertificatePinner(): okhttp3.CertificatePinner {
        return okhttp3.CertificatePinner.Builder()
            .add("updates.vos4.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()
    }
    
    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    }
}

data class CheckResponse(
    val available: Boolean,
    val updateInfo: UpdateInfo? = null,
    val mandatory: Boolean = false
)
```

---

## 5. Component Injection

### 5.1 Dynamic Component Loader

**File:** `runtime/ComponentInjector.kt`

```kotlin
package com.augmentalis.magicui.runtime

import androidx.compose.runtime.Composable
import dalvik.system.DexClassLoader
import java.io.File

/**
 * Dynamically loads and injects components at runtime
 */
class ComponentInjector(private val context: Context) {
    
    private val loadedComponents = mutableMapOf<String, LoadedComponent>()
    
    /**
     * Inject component from update package
     */
    fun injectComponent(
        name: String,
        bytecode: ByteArray,
        metadata: ComponentMetadata
    ): Boolean {
        try {
            // Write bytecode to optimized dex file
            val dexFile = writeDexFile(bytecode)
            
            // Load class
            val classLoader = DexClassLoader(
                dexFile.absolutePath,
                context.codeDir.absolutePath,
                null,
                this::class.java.classLoader
            )
            
            val componentClass = classLoader.loadClass(metadata.className)
            
            // Verify it's a valid @Composable
            if (!isValidComposable(componentClass)) {
                throw IllegalArgumentException("Not a valid Composable")
            }
            
            // Create wrapper
            val wrapper = ComponentWrapper(
                name = name,
                composable = componentClass,
                metadata = metadata
            )
            
            // Register with MagicUI
            loadedComponents[name] = LoadedComponent(wrapper, dexFile)
            MagicComponentRegistry.register(name, wrapper)
            
            Log.i("ComponentInjector", "Component injected: $name")
            return true
            
        } catch (e: Exception) {
            Log.e("ComponentInjector", "Injection failed for $name", e)
            return false
        }
    }
    
    /**
     * Unload component
     */
    fun unloadComponent(name: String): Boolean {
        val loaded = loadedComponents[name] ?: return false
        
        // Unregister from MagicUI
        MagicComponentRegistry.unregister(name)
        
        // Delete dex file
        loaded.dexFile.delete()
        
        loadedComponents.remove(name)
        
        Log.i("ComponentInjector", "Component unloaded: $name")
        return true
    }
    
    /**
     * Get loaded component
     */
    fun getComponent(name: String): ComponentWrapper? {
        return loadedComponents[name]?.wrapper
    }
    
    /**
     * List all injected components
     */
    fun listComponents(): List<String> {
        return loadedComponents.keys.toList()
    }
    
    private fun writeDexFile(bytecode: ByteArray): File {
        val dexFile = File(context.codeDir, "component_${System.currentTimeMillis()}.dex")
        dexFile.writeBytes(bytecode)
        return dexFile
    }
    
    private fun isValidComposable(clazz: Class<*>): Boolean {
        // Check for @Composable annotation
        return clazz.methods.any { method ->
            method.annotations.any { it.annotationClass.simpleName == "Composable" }
        }
    }
}

/**
 * Loaded component data
 */
private data class LoadedComponent(
    val wrapper: ComponentWrapper,
    val dexFile: File
)

/**
 * Component wrapper for dynamic components
 */
class ComponentWrapper(
    val name: String,
    val composable: Class<*>,
    val metadata: ComponentMetadata
) {
    @Composable
    fun render(params: Map<String, Any> = emptyMap()) {
        // Invoke the @Composable function with params
        val method = composable.methods.first { it.name == "invoke" }
        method.invoke(null, params)
    }
}

data class ComponentMetadata(
    val className: String,
    val version: String,
    val author: String,
    val description: String,
    val permissions: List<String> = emptyList()
)
```

---

## 6. Update Distribution

### 6.1 Update Server Setup

**Server Requirements:**
- HTTPS enabled
- Certificate pinning configured
- Rate limiting (prevent DoS)
- Access logging
- Update versioning

**Update Distribution:**
```
VOS4 Update Server:
├── Production Updates
│   ├── Stable releases
│   ├── Security patches
│   └── Critical fixes
│
├── Beta Updates
│   ├── New features (opt-in)
│   ├── Experimental components
│   └── Preview releases
│
└── Development Updates
    ├── Nightly builds
    ├── Testing features
    └── Internal only
```

### 6.2 Update Creation Tool

**File:** `tools/UpdatePackager.kt`

```kotlin
/**
 * Tool to create encrypted update packages
 * Run on development machine
 */
class UpdatePackager {
    
    fun createUpdate(
        components: List<ComponentSource>,
        themes: List<ThemeSource>,
        fixes: List<FixSource>,
        version: String,
        privateKey: PrivateKey
    ): File {
        // 1. Compile components to bytecode
        val compiledComponents = components.map { compileComponent(it) }
        
        // 2. Create metadata
        val metadata = UpdateMetadata(
            version = version,
            components = compiledComponents.map { it.metadata },
            timestamp = System.currentTimeMillis()
        )
        
        // 3. Create payload
        val payload = Payload(
            components = compiledComponents,
            themes = themes,
            fixes = fixes,
            metadata = metadata
        )
        
        // 4. Serialize payload
        val payloadBytes = serializePayload(payload)
        
        // 5. Encrypt payload
        val encrypted = encryptPayload(payloadBytes)
        
        // 6. Sign package
        val signature = signPackage(encrypted, privateKey)
        
        // 7. Create .mupd file
        val updateFile = File("magicui_update_${version}.mupd")
        writeUpdatePackage(updateFile, encrypted, signature, metadata)
        
        return updateFile
    }
    
    private fun signPackage(data: ByteArray, privateKey: PrivateKey): ByteArray {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }
}
```

---

## 7. Usage Examples

### 7.1 Check and Install Updates

```kotlin
// In your app
class MainActivity : ComponentActivity() {
    private val updateManager by lazy { 
        RuntimeUpdateManager.getInstance(this)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate()
        
        lifecycleScope.launch {
            // Check for updates
            val result = updateManager.checkForUpdates()
            
            if (result.available) {
                // Download update
                when (val download = updateManager.downloadUpdate(result.updateInfo!!.updateId)) {
                    is DownloadResult.Success -> {
                        // Install update
                        val install = updateManager.installUpdate(download.file)
                        
                        when (install) {
                            is InstallResult.Success -> {
                                // Hot-reload to apply
                                updateManager.hotReload()
                                
                                // Show success message
                                Toast.makeText(
                                    this@MainActivity,
                                    "Updated to ${install.version}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            else -> {
                                // Handle error
                            }
                        }
                    }
                    else -> {
                        // Handle download error
                    }
                }
            }
        }
    }
}
```

### 7.2 Use Dynamically Loaded Component

```kotlin
@Composable
fun MyScreen() {
    MagicScreen("example") {
        // Use built-in component
        button("Static Button") { }
        
        // Use dynamically loaded component (if available)
        dynamicComponent("advancedChart") {
            param("data", salesData)
            param("type", "line")
        }
    }
}

// In MagicUIScope
@Composable
fun dynamicComponent(name: String, params: @Composable ParameterBuilder.() -> Unit) {
    val component = ComponentInjector.getComponent(name)
    
    if (component != null) {
        val builder = ParameterBuilder()
        builder.params()
        component.render(builder.build())
    } else {
        // Fallback if component not loaded
        text("Component '$name' not available")
    }
}
```

---

## 8. Security Considerations

### 8.1 Threat Model

**Threats:**
1. **Man-in-the-Middle** - Attacker intercepts updates
   - Mitigation: Certificate pinning + HTTPS

2. **Tampered Updates** - Malicious code injection
   - Mitigation: RSA signature verification

3. **Replay Attacks** - Old update re-sent
   - Mitigation: Timestamp validation + nonce

4. **Malicious Components** - Harmful code execution
   - Mitigation: Sandboxed execution + permissions

5. **Key Compromise** - Signing key stolen
   - Mitigation: Key rotation + revocation

### 8.2 Security Best Practices

```kotlin
// Security checklist for updates
class UpdateSecurityValidator {
    
    fun validateUpdate(update: UpdatePackage): ValidationResult {
        // 1. Verify signature (CRITICAL)
        if (!verifySignature(update)) {
            return ValidationResult.SIGNATURE_INVALID
        }
        
        // 2. Check timestamp (prevent replay)
        if (update.timestamp < getLastUpdateTimestamp()) {
            return ValidationResult.OUTDATED
        }
        
        // 3. Validate version
        if (!isVersionValid(update.version)) {
            return ValidationResult.VERSION_INVALID
        }
        
        // 4. Check compatibility
        if (!isCompatible(update)) {
            return ValidationResult.INCOMPATIBLE
        }
        
        // 5. Scan for malicious patterns
        if (containsMaliciousCode(update)) {
            return ValidationResult.MALICIOUS
        }
        
        return ValidationResult.VALID
    }
}
```

---

## 9. Rollback System

### 9.1 Automatic Rollback

```kotlin
/**
 * Monitors update health and auto-rollback on failure
 */
class UpdateHealthMonitor(private val context: Context) {
    
    private var crashCount = 0
    private val maxCrashes = 3
    
    fun monitorUpdateHealth(updateVersion: String) {
        // Monitor for crashes after update
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            crashCount++
            
            if (crashCount >= maxCrashes) {
                // Auto-rollback
                lifecycleScope.launch {
                    Log.e("UpdateHealth", "Too many crashes, rolling back...")
                    RuntimeUpdateManager.getInstance(context).rollback()
                }
            }
            
            // Re-throw
            throw throwable
        }
    }
    
    fun markUpdateStable() {
        // Reset crash counter after successful run
        crashCount = 0
    }
}
```

### 9.2 Manual Rollback

```kotlin
// User-initiated rollback
MagicUI.rollbackUpdate()

// Programmatic rollback
lifecycleScope.launch {
    val result = RuntimeUpdateManager.getInstance(context).rollback()
    
    when (result) {
        is RollbackResult.Success -> {
            // Rolled back to ${result.version}
            updateManager.hotReload()
        }
        else -> {
            // Rollback failed
        }
    }
}
```

---

## 10. Update UI

### 10.1 Update Notification

```kotlin
/**
 * Show update available notification
 */
fun showUpdateNotification(updateInfo: UpdateInfo) {
    MagicScreen("update_notification") {
        card {
            text("Update Available", style = headline)
            text("Version ${updateInfo.version}")
            text(updateInfo.description)
            
            row {
