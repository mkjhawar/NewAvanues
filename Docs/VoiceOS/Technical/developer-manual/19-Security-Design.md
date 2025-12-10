# Chapter 19: Security Design

**VOS4 Developer Manual**
**Version:** 4.0.0
**Last Updated:** 2025-11-02
**Status:** Complete

---

## Table of Contents

1. [Introduction](#introduction)
2. [Security Philosophy](#security-philosophy)
3. [Permission Management](#permission-management)
4. [Data Encryption](#data-encryption)
5. [Privacy Considerations](#privacy-considerations)
6. [Accessibility Service Security](#accessibility-service-security)
7. [Secure Communication](#secure-communication)
8. [API Key Management](#api-key-management)
9. [User Data Protection](#user-data-protection)
10. [Threat Model](#threat-model)
11. [Security Testing](#security-testing)
12. [Compliance and Standards](#compliance-and-standards)

---

## Introduction

VOS4, as a voice-enabled operating system running as an accessibility service, has access to sensitive user data and system-level capabilities. This chapter details the comprehensive security architecture designed to protect user privacy and prevent unauthorized access.

### Security Priorities

1. **User Privacy First**: Minimize data collection, maximize data protection
2. **Principle of Least Privilege**: Request only necessary permissions
3. **Defense in Depth**: Multiple layers of security
4. **Transparency**: Clear communication about data usage
5. **Secure by Default**: Security features enabled out-of-the-box

### Security Targets

| Security Domain | Status | Implementation |
|----------------|--------|----------------|
| Permission Hardening | ✅ Complete | Runtime + Manifest |
| Data Encryption | ✅ Complete | EncryptedSharedPreferences + SQLCipher |
| Accessibility Data Protection | ✅ Complete | Filtered + Ephemeral |
| Voice Data Security | ✅ Complete | On-device + Encrypted |
| Network Security | ✅ Complete | TLS 1.3 + Certificate Pinning |
| API Key Protection | ✅ Complete | NDK + Obfuscation |
| User Authentication | 🔄 Planned | Biometric + PIN |
| Audit Logging | 🔄 Planned | Tamper-proof logs |

---

## Security Philosophy

### Core Security Principles

VOS4's security design follows these fundamental principles:

#### 1. **Privacy by Design**

Privacy is not an afterthought but a fundamental design constraint:

```kotlin
/**
 * AccessibilityScrapingManager.kt - Privacy-first design
 *
 * Security Principles Applied:
 * 1. Minimal Data Collection: Only scrape necessary UI elements
 * 2. Ephemeral Storage: Auto-delete old data after 7 days
 * 3. User Control: Settings to disable/limit scraping
 * 4. Transparency: Log all scraping operations (debug mode)
 */
class AccessibilityScrapingManager @Inject constructor(
    private val database: AppScrapingDatabase,
    private val preferences: EncryptedSharedPreferences
) {
    // Privacy-conscious scraping
    fun shouldScrapeApp(packageName: String): Boolean {
        // Never scrape sensitive apps
        if (packageName in SENSITIVE_PACKAGES) {
            Log.d(TAG, "Skipping sensitive app: $packageName")
            return false
        }

        // Respect user's scraping preferences
        if (!preferences.getBoolean("scraping_enabled", false)) {
            return false
        }

        // Check user's app blacklist
        val blacklist = preferences.getStringSet("blacklisted_apps", emptySet())
        if (packageName in blacklist) {
            return false
        }

        return true
    }

    companion object {
        // Apps we never scrape (privacy)
        private val SENSITIVE_PACKAGES = setOf(
            "com.android.settings",           // System settings
            "com.android.systemui",           // System UI
            "com.google.android.gms",         // Google Play Services
            "com.android.vending",            // Google Play Store
            "com.android.packageinstaller",   // Package installer
            // Banking apps
            "com.chase.sig.android",
            "com.bankofamerica.bmobilebanking",
            "com.usaa.mobile.android.usaa",
            // Password managers
            "com.lastpass.lpandroid",
            "com.onepassword.android",
            "com.dashlane",
            // Secure messaging
            "org.signal.android",
            "org.telegram.messenger",
            "com.whatsapp"
        )
    }
}
```

#### 2. **Zero-Knowledge Architecture**

VOS4 processes data locally and doesn't transmit sensitive information:

```kotlin
// SpeechRecognitionEngine.kt - On-device processing
class SpeechRecognitionEngine {
    // Vosk model runs entirely on-device
    private var voskModel: Model? = null
    private var voskRecognizer: Recognizer? = null

    fun initialize(context: Context) {
        // Load model from local assets (no network)
        val modelPath = extractModelFromAssets(context)
        voskModel = Model(modelPath)
        voskRecognizer = Recognizer(voskModel, SAMPLE_RATE)

        Log.i(TAG, "Vosk initialized (100% on-device, zero cloud)")
    }

    fun recognizeSpeech(audioData: ShortArray): RecognitionResult {
        // All processing happens locally
        val result = voskRecognizer?.recognize(audioData)

        // No data leaves the device
        return RecognitionResult(
            text = result?.text ?: "",
            confidence = result?.confidence ?: 0.0f,
            timestamp = System.currentTimeMillis(),
            processingLocation = "ON_DEVICE" // Never "CLOUD"
        )
    }

    companion object {
        const val SAMPLE_RATE = 16000
    }
}
```

#### 3. **Secure by Default**

Security features are enabled by default, not opt-in:

```kotlin
// SecurityConfig.kt - Secure defaults
object SecurityConfig {
    // Security features enabled by default
    const val ENCRYPTION_ENABLED = true
    const val SCRAPING_REQUIRES_CONSENT = true
    const val VOICE_DATA_EPHEMERAL = true
    const val TELEMETRY_ANONYMOUS = true
    const val NETWORK_TLS_REQUIRED = true

    // Conservative defaults
    const val MAX_SCRAPING_RETENTION_DAYS = 7
    const val MAX_VOICE_RECORDING_LENGTH_SECONDS = 10
    const val SESSION_TIMEOUT_MINUTES = 30

    // User can disable features but cannot weaken security
    fun isFeatureAllowed(feature: String, userPreference: Boolean): Boolean {
        return when (feature) {
            "encryption" -> true // Always encrypted, cannot be disabled
            "tls" -> true // Always use TLS, cannot be disabled
            "scraping" -> userPreference && SCRAPING_REQUIRES_CONSENT
            "telemetry" -> userPreference
            else -> false
        }
    }
}
```

---

## Permission Management

### Permission Strategy

VOS4 follows the principle of least privilege, requesting only essential permissions.

### Manifest Permissions

```xml
<!-- app/src/main/AndroidManifest.xml - Permission analysis -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- ========== CRITICAL PERMISSIONS ========== -->

    <!-- Accessibility Service (System grants when user enables service) -->
    <uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE"
        tools:ignore="ProtectedPermissions" />

    <!-- Microphone (Required for voice commands) -->
    <!-- Justification: Core feature - voice input -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <!-- Overlay (Required for floating UI) -->
    <!-- Justification: Voice cursor and HUD overlays -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

    <!-- ========== STANDARD PERMISSIONS ========== -->

    <!-- Foreground Service (Background microphone access) -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />

    <!-- Notifications (Required for foreground service) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- ========== OPTIONAL PERMISSIONS ========== -->

    <!-- Internet (Optional: Model downloads, telemetry) -->
    <!-- User can disable in settings -->
    <uses-permission android:name="android.permission.INTERNET" />

    <!-- Wake Lock (Keep service running) -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <!-- Boot Completed (Auto-start on boot) -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <!-- ========== UTILITY PERMISSIONS ========== -->

    <!-- Vibration (Haptic feedback) -->
    <uses-permission android:name="android.permission.VIBRATE" />

    <!-- WiFi State (Check network type for battery optimization) -->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Settings Modification (Change system settings via voice) -->
    <uses-permission android:name="android.permission.WRITE_SETTINGS" />

    <!-- Audio Control (Volume adjustment via voice) -->
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />

    <!-- ========== STORAGE PERMISSIONS (API-Level Aware) ========== -->

    <!-- API 30+ (Android 11+): MANAGE_EXTERNAL_STORAGE for speech models -->
    <!-- Required for accessing non-media files (.voiceos folder) in shared storage -->
    <!-- User must manually enable in system settings (not runtime dialog) -->
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
        tools:ignore="ScopedStorage" />

    <!-- Legacy permissions for API 23-29 (Android 6-10) -->
    <!-- READ_EXTERNAL_STORAGE deprecated on API 33+, non-functional on API 36+ -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="29" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="29"
        tools:ignore="ScopedStorage" />

    <!-- ========== PERMISSIONS WE DON'T REQUEST ========== -->

    <!-- ❌ Location: Not needed for VOS4 core functionality -->
    <!-- ❌ Camera: Not needed for VOS4 core functionality -->
    <!-- ❌ Contacts: Not needed for VOS4 core functionality -->
    <!-- ❌ SMS: Not needed for VOS4 core functionality -->
    <!-- ❌ Phone: Not needed for VOS4 core functionality -->
    <!-- ❌ Calendar: Not needed for VOS4 core functionality -->

    <!-- ========== PACKAGE QUERIES ========== -->

    <!-- Query installed apps (for voice commands like "open Gmail") -->
    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent>
    </queries>

</manifest>
```

### Runtime Permission Handling

```kotlin
// PermissionManager.kt - Secure permission handling
class PermissionManager @Inject constructor(
    private val context: Context,
    private val preferences: EncryptedSharedPreferences
) {
    // Required permissions for core functionality
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.SYSTEM_ALERT_WINDOW
    )

    // Optional permissions (graceful degradation)
    private val optionalPermissions = arrayOf(
        Manifest.permission.POST_NOTIFICATIONS
    )

    suspend fun requestRequiredPermissions(activity: Activity): PermissionResult {
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) !=
                PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            return PermissionResult.Granted
        }

        // Request permissions
        return suspendCancellableCoroutine { continuation ->
            val requestCode = Random.nextInt()

            // Store continuation for callback
            permissionCallbacks[requestCode] = { granted ->
                if (granted) {
                    logPermissionGrant(missingPermissions)
                    continuation.resume(PermissionResult.Granted)
                } else {
                    logPermissionDenial(missingPermissions)
                    continuation.resume(PermissionResult.Denied(missingPermissions))
                }
            }

            ActivityCompat.requestPermissions(
                activity,
                missingPermissions.toTypedArray(),
                requestCode
            )
        }
    }

    fun checkAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.contains(context.packageName)
    }

    private fun logPermissionGrant(permissions: List<String>) {
        val timestamp = System.currentTimeMillis()
        permissions.forEach { permission ->
            Log.i(TAG, "Permission granted: $permission at $timestamp")
            // Store in audit log
            preferences.edit()
                .putLong("permission_granted_$permission", timestamp)
                .apply()
        }
    }

    private fun logPermissionDenial(permissions: List<String>) {
        val timestamp = System.currentTimeMillis()
        permissions.forEach { permission ->
            Log.w(TAG, "Permission denied: $permission at $timestamp")
            preferences.edit()
                .putLong("permission_denied_$permission", timestamp)
                .apply()
        }
    }

    sealed class PermissionResult {
        object Granted : PermissionResult()
        data class Denied(val missingPermissions: List<String>) : PermissionResult()
    }

    companion object {
        private const val TAG = "PermissionManager"
        private val permissionCallbacks =
            mutableMapOf<Int, (Boolean) -> Unit>()
    }
}
```

### Storage Permission Migration (API 30+)

**Critical Update (2025-11-21):** VOS4 has migrated from `READ_EXTERNAL_STORAGE` to `MANAGE_EXTERNAL_STORAGE` for Android 11+ (API 30+) devices.

#### Why the Migration?

**Android Storage Permission Evolution:**

| Android Version | API Level | Storage Permission Approach |
|----------------|-----------|----------------------------|
| Android 6-10 | API 23-29 | `READ_EXTERNAL_STORAGE` + `WRITE_EXTERNAL_STORAGE` |
| Android 11-12 | API 30-32 | `READ_EXTERNAL_STORAGE` (deprecated, limited scope) |
| Android 13+ | API 33+ | `READ_EXTERNAL_STORAGE` **completely deprecated** |
| Android 14+ | API 34+ | Granular media permissions (`READ_MEDIA_*`) only |
| Android 16 | API 36 | `READ_EXTERNAL_STORAGE` **non-functional** |

**Problem:** VOS4 requires access to **non-media files** (speech recognition models in `.voiceos` folder). The new granular media permissions (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`) only work for standard media types, not custom model files.

**Solution:** `MANAGE_EXTERNAL_STORAGE` is the only permission that allows broad file access for non-media files on Android 11+.

#### Implementation

```kotlin
// PermissionManager.kt - API-aware storage permission handling
class PermissionManager(private val context: Context) {

    companion object {
        // Modern permission for API 30+ (Android 11+)
        private val REQUIRED_PERMISSIONS_MODERN = arrayOf(
            Manifest.permission.MANAGE_EXTERNAL_STORAGE
        )

        // Legacy permissions for API 23-29 (Android 6-10)
        private val REQUIRED_PERMISSIONS_LEGACY = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        fun getRequiredPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                REQUIRED_PERMISSIONS_MODERN
            } else {
                REQUIRED_PERMISSIONS_LEGACY
            }
        }
    }

    /**
     * Check storage permission using correct method for API level
     * API 30+: Environment.isExternalStorageManager()
     * API 23-29: ContextCompat.checkSelfPermission()
     */
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: Check "All files access" permission
            Environment.isExternalStorageManager()
        } else {
            // API 23-29: Check legacy runtime permissions
            val permissions = getRequiredPermissions()
            permissions.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
            }
        }
    }

    /**
     * Create intent to request storage permission
     * API 30+: Opens "All files access" settings (manual enable)
     * API 23-29: Returns app settings intent
     */
    fun createStoragePermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: Direct to "All files access" settings page
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            // API 23-29: Fallback to app settings
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}
```

#### User Flow (API 30+)

**Important:** `MANAGE_EXTERNAL_STORAGE` cannot be requested via runtime permission dialog on API 30+. User must manually enable it in system settings.

1. **App Launch**
   - Check: `Environment.isExternalStorageManager()`
   - If `false` → Show rationale dialog

2. **Rationale Dialog**
   ```
   Title: "Storage Access Required"
   Message: "VoiceOS needs 'All files access' permission to read
            speech recognition models from shared storage..."
   Buttons: [Open Settings] [Cancel]
   ```

3. **User Action**
   - Clicks "Open Settings"
   - Navigate to: Settings → Apps → VoiceOS → Special app access → All files access
   - Enable toggle: "Allow access to manage all files"

4. **Return to App**
   - App resumes
   - Re-check: `Environment.isExternalStorageManager()`
   - If `true` → Permission granted, load models
   - If `false` → Show fallback option (use internal storage)

#### Google Play Store Compliance

**MANAGE_EXTERNAL_STORAGE** requires Play Store approval:

**Declaration Form Requirements:**
- **Use Case:** System-level voice control app requiring external speech recognition model deployment
- **Justification:**
  - Manual model deployment via ADB by advanced users
  - Sharing models across app reinstalls
  - Reducing APK size (models in external storage)
  - Non-media files (custom `.model` extensions) not supported by granular permissions

**Valid Use Case Category:**
- File managers
- Backup/restore apps
- **Document management apps** ← VoiceOS qualifies here
- Apps requiring access to files across multiple directories

**Video Demonstration Required:**
- Show model deployment via ADB
- Demonstrate model loading from `.voiceos` folder
- Explain why shared storage is necessary

#### Fallback Strategy

If Play Store rejects `MANAGE_EXTERNAL_STORAGE`, implement fallback:

```kotlin
fun getModelsDirectory(): File {
    return if (hasStoragePermission()) {
        // Preferred: Shared storage for manual deployment
        File(Environment.getExternalStorageDirectory(), ".voiceos/models")
    } else {
        // Fallback: App-specific storage (auto-extracted from APK)
        // Location: /sdcard/Android/data/com.augmentalis.voiceos/files/models/
        context.getExternalFilesDir("models")!!
    }
}
```

**Fallback Characteristics:**
- ✅ No permissions required
- ✅ Direct File API access
- ✅ Works on all API levels
- ❌ Data deleted when app uninstalled
- ❌ Not accessible via ADB (without root)

#### Security Implications

**MANAGE_EXTERNAL_STORAGE grants broad file access:**

**Mitigations:**
1. **Principle of Least Privilege:** Only access `.voiceos` folder, not entire storage
2. **Transparent Usage:** Clearly document what files are accessed
3. **User Control:** Allow users to disable external models in settings
4. **Audit Logging:** Log all file access for debugging

**Code Example:**
```kotlin
// Restrict file access to .voiceos folder only
private fun loadModels() {
    val voiceosDir = File(Environment.getExternalStorageDirectory(), ".voiceos/models")

    // Security check: Only access our designated folder
    if (!voiceosDir.canonicalPath.startsWith(
        Environment.getExternalStorageDirectory().canonicalPath + "/.voiceos"
    )) {
        Log.e(TAG, "Security violation: Attempted access outside .voiceos folder")
        return
    }

    // Safe to proceed
    voiceosDir.listFiles()?.forEach { modelFile ->
        loadModel(modelFile)
    }
}
```

### Permission Rationale UI

```kotlin
// PermissionRationaleFragment.kt - Transparent permission explanation
@Composable
fun PermissionRationaleScreen(
    onAllowClick: () -> Unit,
    onDenyClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineMedium
        )

        // Microphone permission
        PermissionCard(
            icon = Icons.Default.Mic,
            title = "Microphone Access",
            description = "Required for voice commands and speech recognition. " +
                    "All processing happens on-device. No audio is sent to cloud servers.",
            required = true
        )

        // Overlay permission
        PermissionCard(
            icon = Icons.Default.Visibility,
            title = "Display Over Other Apps",
            description = "Required for voice cursor and floating controls. " +
                    "Allows VOS4 to show UI elements on top of other apps.",
            required = true
        )

        // Accessibility permission
        PermissionCard(
            icon = Icons.Default.Accessibility,
            title = "Accessibility Service",
            description = "Required to detect UI elements and perform voice-controlled actions. " +
                    "VOS4 only accesses UI data of apps you explicitly enable. " +
                    "Sensitive apps (banking, passwords) are automatically excluded.",
            required = true
        )

        Spacer(modifier = Modifier.weight(1f))

        // Privacy notice
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Column {
                    Text(
                        text = "Your Privacy is Protected",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "All data stays on your device. No cloud uploads. " +
                                "You can review and delete data at any time.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDenyClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Not Now")
            }
            Button(
                onClick = onAllowClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Grant Permissions")
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    required: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (required)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.secondary
            )
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (required) {
                        Chip(
                            label = { Text("Required") },
                            colors = ChipDefaults.chipColors(
                                backgroundColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

---

## Data Encryption

### Encryption Strategy

VOS4 uses multiple layers of encryption:

```
┌──────────────────────────────────────┐
│     VOS4 Encryption Architecture     │
├──────────────────────────────────────┤
│ Voice Recordings    │ Not stored     │
│ User Preferences    │ EncryptedPrefs │
│ Database (Room)     │ SQLCipher      │
│ API Keys            │ NDK + AES-256  │
│ Network Traffic     │ TLS 1.3        │
└──────────────────────────────────────┘
```

### EncryptedSharedPreferences

```kotlin
// SecurePreferences.kt - Encrypted preferences
object SecurePreferences {

    fun create(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "voiceos_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}

// Usage
class SettingsManager @Inject constructor(context: Context) {
    private val securePrefs = SecurePreferences.create(context)

    fun saveApiKey(key: String) {
        // Stored encrypted
        securePrefs.edit()
            .putString("api_key", key)
            .apply()
    }

    fun getApiKey(): String? {
        // Decrypted automatically
        return securePrefs.getString("api_key", null)
    }
}
```

### Database Encryption with SQLCipher

```kotlin
// VoiceOSAppDatabase.kt - Encrypted database
@Database(
    entities = [Screen::class, AccessibleElement::class, Gesture::class],
    version = 4
)
abstract class VoiceOSAppDatabase : RoomDatabase() {

    companion object {
        @Volatile
        private var INSTANCE: VoiceOSAppDatabase? = null

        fun getInstance(context: Context): VoiceOSAppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildEncryptedDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildEncryptedDatabase(context: Context): VoiceOSAppDatabase {
            // Generate encryption key from Android Keystore
            val passphrase = getOrCreateDatabaseKey(context)

            val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase))

            return Room.databaseBuilder(
                context.applicationContext,
                VoiceOSAppDatabase::class.java,
                "voiceos_database"
            )
                .openHelperFactory(factory) // SQLCipher encryption
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
        }

        private fun getOrCreateDatabaseKey(context: Context): CharArray {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            val alias = "voiceos_database_key"

            if (!keyStore.containsAlias(alias)) {
                // Generate new key
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore"
                )

                keyGenerator.init(
                    KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )

                keyGenerator.generateKey()
            }

            // Retrieve key
            val secretKey = keyStore.getKey(alias, null) as SecretKey
            val encoded = secretKey.encoded

            // Convert to char array for SQLCipher
            return String(encoded, Charsets.UTF_8).toCharArray()
        }
    }
}
```

### Network Encryption

```kotlin
// NetworkSecurityConfig.kt - TLS 1.3 enforcement
object NetworkSecurityConfig {

    fun createSecureOkHttpClient(): OkHttpClient {
        // Create SSL context with TLS 1.3
        val sslContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(null, null, null)

        // Certificate pinning
        val certificatePinner = CertificatePinner.Builder()
            .add("api.voiceos.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()

        return OkHttpClient.Builder()
            .sslSocketFactory(
                sslContext.socketFactory,
                X509TrustManager()
            )
            .certificatePinner(certificatePinner)
            .connectionSpecs(
                listOf(
                    ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
                        .cipherSuites(
                            CipherSuite.TLS_AES_128_GCM_SHA256,
                            CipherSuite.TLS_AES_256_GCM_SHA384,
                            CipherSuite.TLS_CHACHA20_POLY1305_SHA256
                        )
                        .build()
                )
            )
            .build()
    }
}

// network_security_config.xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Disable cleartext traffic (HTTP) -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>

    <!-- Pin certificates for API domain -->
    <domain-config>
        <domain includeSubdomains="true">api.voiceos.com</domain>
        <pin-set expiration="2026-01-01">
            <pin digest="SHA-256">AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=</pin>
            <!-- Backup pin -->
            <pin digest="SHA-256">BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=</pin>
        </pin-set>
    </domain-config>
</network-security-config>
```

---

## Privacy Considerations

### Data Minimization

VOS4 collects only essential data:

```kotlin
// DataCollectionPolicy.kt - Minimal data collection
object DataCollectionPolicy {

    // Data we collect
    data class CollectedData(
        // Essential for functionality
        val accessibilityData: AccessibilityData, // UI elements (ephemeral)
        val voiceCommands: VoiceCommandLog,       // Command history (local only)
        val userPreferences: UserPreferences,     // Settings

        // Optional (user can disable)
        val usageStatistics: UsageStats? = null,  // Anonymous usage data
        val crashReports: CrashReport? = null     // Anonymous crash data
    )

    // Data we DON'T collect
    // - No voice recordings stored
    // - No location data
    // - No contact list
    // - No message content
    // - No browsing history
    // - No app usage across apps
    // - No biometric data
    // - No health data

    fun shouldCollectData(dataType: String, userConsent: Boolean): Boolean {
        return when (dataType) {
            "accessibility" -> true // Required for functionality
            "commands" -> true // Required for functionality
            "preferences" -> true // Required for functionality
            "usage_stats" -> userConsent // Optional
            "crash_reports" -> userConsent // Optional
            else -> false // Deny by default
        }
    }
}
```

### Data Retention

```kotlin
// DataRetentionManager.kt - Automatic data deletion
class DataRetentionManager @Inject constructor(
    private val database: AppScrapingDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    // Retention periods
    companion object {
        const val ACCESSIBILITY_DATA_RETENTION_DAYS = 7
        const val COMMAND_HISTORY_RETENTION_DAYS = 30
        const val CRASH_REPORTS_RETENTION_DAYS = 90
    }

    suspend fun enforceRetentionPolicy() = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()

        // Delete old accessibility data
        val accessibilityThreshold = now -
            TimeUnit.DAYS.toMillis(ACCESSIBILITY_DATA_RETENTION_DAYS.toLong())
        val deletedScreens = database.screenDao().deleteOlderThan(accessibilityThreshold)
        Log.i(TAG, "Deleted $deletedScreens old screens (retention policy)")

        // Delete old command history
        val commandThreshold = now -
            TimeUnit.DAYS.toMillis(COMMAND_HISTORY_RETENTION_DAYS.toLong())
        val deletedCommands = database.commandDao().deleteOlderThan(commandThreshold)
        Log.i(TAG, "Deleted $deletedCommands old commands (retention policy)")

        // Vacuum database to reclaim space
        database.query("VACUUM", null)
    }

    fun scheduleRetentionEnforcement(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresCharging(true) // Only when charging
            .setRequiresBatteryNotLow(true)
            .build()

        val retentionWork = PeriodicWorkRequestBuilder<RetentionWorker>(
            1, TimeUnit.DAYS // Daily
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(retentionWork)
    }
}
```

### Anonymization

```kotlin
// TelemetryManager.kt - Anonymous telemetry
class TelemetryManager {

    data class TelemetryEvent(
        val eventType: String,
        val timestamp: Long,
        val anonymousDeviceId: String, // Not tied to user
        val appVersion: String,
        val osVersion: String,
        // No personally identifiable information
        val metadata: Map<String, String> = emptyMap()
    )

    private fun generateAnonymousDeviceId(): String {
        // Generate random ID (not tied to device)
        val prefs = context.getSharedPreferences("telemetry", Context.MODE_PRIVATE)
        var id = prefs.getString("anonymous_id", null)

        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("anonymous_id", id).apply()
        }

        return id
    }

    fun logEvent(eventType: String, metadata: Map<String, String> = emptyMap()) {
        // Remove any PII from metadata
        val sanitizedMetadata = metadata.filterKeys { key ->
            key !in BLACKLISTED_KEYS
        }.mapValues { (_, value) ->
            sanitizeValue(value)
        }

        val event = TelemetryEvent(
            eventType = eventType,
            timestamp = System.currentTimeMillis(),
            anonymousDeviceId = generateAnonymousDeviceId(),
            appVersion = BuildConfig.VERSION_NAME,
            osVersion = Build.VERSION.RELEASE,
            metadata = sanitizedMetadata
        )

        sendEvent(event)
    }

    private fun sanitizeValue(value: String): String {
        // Remove potential PII
        return value
            .replace(Regex("[0-9]{3}-[0-9]{3}-[0-9]{4}"), "[PHONE]") // Phone numbers
            .replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "[EMAIL]") // Emails
            .replace(Regex("\\b[0-9]{15,16}\\b"), "[CARD]") // Credit cards
    }

    companion object {
        private val BLACKLISTED_KEYS = setOf(
            "email", "phone", "name", "address", "ssn", "dob", "password"
        )
    }
}
```

---

## Accessibility Service Security

### Accessibility Service Configuration

```xml
<!-- res/xml/accessibility_service_config.xml - Secure configuration -->
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowContentChanged|typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagRetrieveInteractiveWindows|flagReportViewIds"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100"
    android:packageNames="" />
<!-- Empty packageNames = all apps, but we filter in code -->
```

### Secure Event Processing

```kotlin
// VoiceOSService.kt - Secure accessibility event handling
class VoiceOSService : AccessibilityService() {

    // Launcher apps we never scrape (privacy)
    private val launcherPackages = setOf(
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.samsung.android.app.launcher"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // Security check: Don't process events from sensitive apps
        if (!shouldProcessPackage(event.packageName?.toString())) {
            return
        }

        // Security check: Don't process password fields
        if (event.source?.isPassword == true) {
            Log.d(TAG, "Ignoring password field (security)")
            return
        }

        // Security check: Rate limiting to prevent abuse
        if (!rateLimiter.tryAcquire()) {
            Log.w(TAG, "Rate limit exceeded (security)")
            return
        }

        processAccessibilityEvent(event)
    }

    private fun shouldProcessPackage(packageName: String?): Boolean {
        packageName ?: return false

        // Never process launcher
        if (packageName in launcherPackages) {
            return false
        }

        // Never process sensitive packages
        if (packageName in AccessibilityScrapingManager.SENSITIVE_PACKAGES) {
            Log.d(TAG, "Skipping sensitive package: $packageName")
            return false
        }

        // Check user blacklist
        val blacklist = preferences.getStringSet("blacklisted_apps", emptySet())
        if (packageName in blacklist) {
            Log.d(TAG, "Skipping blacklisted package: $packageName")
            return false
        }

        return true
    }

    private fun processAccessibilityEvent(event: AccessibilityEvent) {
        // Extract only necessary information
        val source = event.source ?: return

        try {
            // Security: Don't store raw AccessibilityNodeInfo (contains PII)
            val sanitizedData = extractSanitizedData(source)

            // Store only sanitized data
            scrapingManager.processElement(sanitizedData)
        } finally {
            // Important: Recycle node to prevent memory leaks
            source.recycle()
        }
    }

    private fun extractSanitizedData(node: AccessibilityNodeInfo): SanitizedElement {
        return SanitizedElement(
            viewId = node.viewIdResourceName ?: "",
            contentDescription = node.contentDescription?.toString() ?: "",
            text = if (node.isPassword) "" else node.text?.toString() ?: "",
            bounds = node.boundsInScreen.toString(),
            isClickable = node.isClickable,
            isScrollable = node.isScrollable,
            // Exclude sensitive data
            isPassword = node.isPassword // Flag but don't store content
        )
    }

    companion object {
        private const val TAG = "VoiceOSService"
        private val rateLimiter = RateLimiter.create(100.0) // 100 events/sec max
    }
}
```

### Accessibility Data Filtering

```kotlin
// AccessibilityDataFilter.kt - Filter sensitive data
object AccessibilityDataFilter {

    // Content types to never store
    private val SENSITIVE_CONTENT_TYPES = setOf(
        "password",
        "creditCard",
        "ssn",
        "pin",
        "securityCode",
        "cvv"
    )

    // Text patterns to filter
    private val SENSITIVE_PATTERNS = listOf(
        Regex("[0-9]{3}-[0-9]{2}-[0-9]{4}"), // SSN
        Regex("[0-9]{15,16}"), // Credit card
        Regex("[0-9]{3,4}"), // PIN/CVV
        Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}") // Email
    )

    fun shouldFilterElement(element: AccessibilityNodeInfo): Boolean {
        // Filter password fields
        if (element.isPassword) {
            return true
        }

        // Filter by input type
        if (element.inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD != 0) {
            return true
        }

        // Filter by content description
        val contentDesc = element.contentDescription?.toString()?.lowercase()
        if (contentDesc != null && SENSITIVE_CONTENT_TYPES.any { it in contentDesc }) {
            return true
        }

        // Filter by text content patterns
        val text = element.text?.toString() ?: return false
        return SENSITIVE_PATTERNS.any { pattern ->
            pattern.containsMatchIn(text)
        }
    }

    fun sanitizeText(text: String): String {
        var sanitized = text

        SENSITIVE_PATTERNS.forEach { pattern ->
            sanitized = sanitized.replace(pattern, "[REDACTED]")
        }

        return sanitized
    }
}
```

---

## Secure Communication

### API Communication

```kotlin
// ApiClient.kt - Secure API communication
class ApiClient {
    private val client = NetworkSecurityConfig.createSecureOkHttpClient()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.voiceos.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    interface VoiceOSApi {
        @POST("v1/commands/sync")
        suspend fun syncCommands(
            @Header("Authorization") authToken: String,
            @Body commands: List<Command>
        ): Response<SyncResponse>

        @GET("v1/models/download")
        suspend fun downloadModel(
            @Query("model_id") modelId: String,
            @Header("Authorization") authToken: String
        ): Response<ResponseBody>
    }

    private val api = retrofit.create(VoiceOSApi::class.java)

    suspend fun syncCommands(commands: List<Command>): Result<SyncResponse> {
        return try {
            // Get auth token from secure storage
            val token = securePrefs.getString("auth_token", null)
                ?: return Result.failure(Exception("Not authenticated"))

            val response = api.syncCommands("Bearer $token", commands)

            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "API communication failed", e)
            Result.failure(e)
        }
    }
}
```

### Inter-Process Communication (IPC)

```kotlin
// SecureIPC.kt - Secure cross-app communication
class SecureIPC {

    // AIDL interface for IPC
    interface IVoiceOSService {
        fun executeCommand(command: String, signature: String): CommandResult
    }

    // Server side (VoiceOSCore)
    class VoiceOSServiceImpl : IVoiceOSService.Stub() {
        override fun executeCommand(command: String, signature: String): CommandResult {
            // Verify signature
            if (!verifySignature(command, signature)) {
                throw SecurityException("Invalid signature")
            }

            // Verify caller
            val callingUid = Binder.getCallingUid()
            if (!isAuthorizedCaller(callingUid)) {
                throw SecurityException("Unauthorized caller")
            }

            // Execute command
            return commandManager.execute(command)
        }

        private fun verifySignature(data: String, signature: String): Boolean {
            // HMAC-SHA256 signature verification
            val key = securePrefs.getString("ipc_key", null) ?: return false
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
            val expected = Base64.encodeToString(mac.doFinal(data.toByteArray()), Base64.NO_WRAP)
            return expected == signature
        }

        private fun isAuthorizedCaller(uid: Int): Boolean {
            // Only allow VOS4 modules
            val packages = packageManager.getPackagesForUid(uid) ?: return false
            return packages.any { it.startsWith("com.augmentalis.") }
        }
    }

    // Client side (other VOS4 modules)
    class VoiceOSClient(context: Context) {
        private var service: IVoiceOSService? = null

        fun connect() {
            val intent = Intent("com.augmentalis.voiceoscore.IVoiceOSService")
            intent.setPackage("com.augmentalis.voiceos")

            context.bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        }

        suspend fun executeCommand(command: String): CommandResult {
            val service = service ?: throw IllegalStateException("Not connected")

            // Sign command
            val signature = signCommand(command)

            return service.executeCommand(command, signature)
        }

        private fun signCommand(command: String): String {
            val key = securePrefs.getString("ipc_key", null)
                ?: throw IllegalStateException("No IPC key")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA256"))
            return Base64.encodeToString(mac.doFinal(command.toByteArray()), Base64.NO_WRAP)
        }

        private val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = IVoiceOSService.Stub.asInterface(binder)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
            }
        }
    }
}
```

---

## API Key Management

### Native (NDK) Key Storage

```cpp
// native-keys.cpp - API keys stored in native code (harder to extract)
#include <jni.h>
#include <string>
#include <android/log.h>

// Obfuscated API key (split into parts)
static const char KEY_PART_1[] = "AIzaSy";
static const char KEY_PART_2[] = "BqK9z";
static const char KEY_PART_3[] = "cW5X";

// XOR obfuscation key
static const char XOR_KEY = 0x5A;

// Decrypt function
std::string decryptKey(const char* encrypted, size_t length) {
    std::string decrypted;
    for (size_t i = 0; i < length; i++) {
        decrypted += encrypted[i] ^ XOR_KEY;
    }
    return decrypted;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_augmentalis_voiceos_security_NativeKeyStore_getApiKey(
    JNIEnv* env,
    jobject /* this */) {

    // Reconstruct key from parts
    std::string fullKey = std::string(KEY_PART_1) + KEY_PART_2 + KEY_PART_3;

    // Decrypt (XOR obfuscation)
    std::string decrypted = decryptKey(fullKey.c_str(), fullKey.length());

    return env->NewStringUTF(decrypted.c_str());
}

// Additional security: Validate caller
extern "C" JNIEXPORT jboolean JNICALL
Java_com_augmentalis_voiceos_security_NativeKeyStore_validateCaller(
    JNIEnv* env,
    jobject /* this */,
    jstring packageName) {

    const char* pkg = env->GetStringUTFChars(packageName, nullptr);
    bool valid = strcmp(pkg, "com.augmentalis.voiceos") == 0;
    env->ReleaseStringUTFChars(packageName, pkg);

    return valid;
}
```

### Kotlin Wrapper

```kotlin
// NativeKeyStore.kt - Kotlin wrapper for native keys
object NativeKeyStore {
    init {
        System.loadLibrary("native-keys")
    }

    private external fun getApiKey(): String
    private external fun validateCaller(packageName: String): Boolean

    fun getSecureApiKey(context: Context): String? {
        // Validate caller
        if (!validateCaller(context.packageName)) {
            Log.e(TAG, "Invalid caller attempting to access API key")
            return null
        }

        // Additional runtime check
        if (BuildConfig.DEBUG && !isDebuggerAttached()) {
            // Allow in debug builds without debugger
            return getApiKey()
        } else if (!BuildConfig.DEBUG) {
            // Production build
            return getApiKey()
        }

        // Block if debugger is attached
        Log.e(TAG, "Debugger detected, blocking API key access")
        return null
    }

    private fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    private const val TAG = "NativeKeyStore"
}
```

### Runtime Key Generation

```kotlin
// DynamicKeyManager.kt - Generate keys at runtime
class DynamicKeyManager {

    fun generateSessionKey(): String {
        // Generate random key for this session
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        val secretKey = keyGenerator.generateKey()

        return Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
    }

    fun deriveKeyFromUserInput(pin: String, salt: ByteArray): SecretKey {
        // Derive key from user PIN using PBKDF2
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(
            pin.toCharArray(),
            salt,
            100000, // iterations
            256 // key length
        )
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }
}
```

---

## User Data Protection

### Data Export (GDPR Compliance)

```kotlin
// DataExporter.kt - GDPR-compliant data export
class DataExporter @Inject constructor(
    private val database: VoiceOSAppDatabase,
    private val preferences: EncryptedSharedPreferences
) {
    suspend fun exportUserData(): UserDataExport = withContext(Dispatchers.IO) {
        UserDataExport(
            exportDate = Date(),
            version = BuildConfig.VERSION_NAME,
            personalData = exportPersonalData(),
            usageData = exportUsageData(),
            preferences = exportPreferences()
        )
    }

    private suspend fun exportPersonalData(): PersonalData {
        return PersonalData(
            userId = preferences.getString("user_id", "anonymous") ?: "anonymous",
            commands = database.commandDao().getAllCommands(),
            scrapedScreens = database.screenDao().getAll(),
            // No voice recordings - we don't store them
            voiceRecordings = emptyList()
        )
    }

    private suspend fun exportUsageData(): UsageData {
        return UsageData(
            totalCommands = database.commandDao().getCount(),
            mostUsedCommands = database.commandDao().getMostUsed(10),
            sessionDuration = getTotalSessionDuration(),
            // All data is anonymous
            isAnonymous = true
        )
    }

    private fun exportPreferences(): Map<String, Any> {
        return preferences.all.filterKeys { key ->
            // Don't export internal keys
            !key.startsWith("_internal")
        }
    }

    fun saveExportToFile(export: UserDataExport, outputFile: File) {
        val json = Gson().toJson(export)
        outputFile.writeText(json)
        Log.i(TAG, "User data exported to: ${outputFile.absolutePath}")
    }
}
```

### Data Deletion (Right to be Forgotten)

```kotlin
// DataDeletor.kt - Complete data deletion
class DataDeletor @Inject constructor(
    private val database: VoiceOSAppDatabase,
    private val preferences: EncryptedSharedPreferences,
    private val context: Context
) {
    suspend fun deleteAllUserData(): DeletionResult = withContext(Dispatchers.IO) {
        try {
            // 1. Delete database
            database.clearAllTables()
            Log.i(TAG, "Database cleared")

            // 2. Delete preferences
            preferences.edit().clear().commit()
            Log.i(TAG, "Preferences cleared")

            // 3. Delete cached files
            context.cacheDir.deleteRecursively()
            Log.i(TAG, "Cache cleared")

            // 4. Delete app-specific files
            context.filesDir.deleteRecursively()
            Log.i(TAG, "Files cleared")

            // 5. Close database connection
            database.close()

            DeletionResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete user data", e)
            DeletionResult.Failure(e)
        }
    }

    sealed class DeletionResult {
        object Success : DeletionResult()
        data class Failure(val error: Exception) : DeletionResult()
    }
}
```

---

## Threat Model

### Threat Analysis

VOS4 considers these threat vectors:

#### 1. **Malicious Apps**

**Threat:** A malicious app tries to exploit VOS4's accessibility service to steal data.

**Mitigation:**
- Package name filtering (whitelist/blacklist)
- Signature verification for IPC
- Rate limiting on accessibility events
- Never process password fields

```kotlin
// Protection against malicious apps
private fun isAuthorizedApp(packageName: String): Boolean {
    try {
        val packageInfo = packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_SIGNATURES
        )

        // Check if app signature matches trusted list
        val signature = packageInfo.signatures[0].toCharsString()
        return signature in TRUSTED_SIGNATURES
    } catch (e: PackageManager.NameNotFoundException) {
        return false
    }
}
```

#### 2. **Man-in-the-Middle (MITM)**

**Threat:** Attacker intercepts network traffic to steal data.

**Mitigation:**
- TLS 1.3 enforcement
- Certificate pinning
- No cleartext traffic
- On-device processing (minimize network)

#### 3. **Local Data Theft**

**Threat:** Attacker gains physical access to device.

**Mitigation:**
- Database encryption (SQLCipher)
- Encrypted SharedPreferences
- No sensitive data in plain text
- Auto-lock after inactivity

#### 4. **Reverse Engineering**

**Threat:** Attacker decompiles app to extract secrets.

**Mitigation:**
- R8 obfuscation
- Native code for sensitive operations
- Key obfuscation
- Runtime integrity checks

```kotlin
// Runtime integrity check
object IntegrityChecker {
    fun checkAppIntegrity(context: Context): Boolean {
        // Check if app is signed with release key
        val signature = getAppSignature(context)
        if (signature != EXPECTED_SIGNATURE) {
            Log.e(TAG, "App signature mismatch (tampered)")
            return false
        }

        // Check if running on rooted device
        if (isDeviceRooted()) {
            Log.w(TAG, "Device is rooted (security warning)")
            // Don't block, but warn user
        }

        return true
    }

    private fun isDeviceRooted(): Boolean {
        // Check for common root indicators
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/system/xbin/su",
            "/system/bin/su"
        )

        return rootPaths.any { File(it).exists() }
    }
}
```

---

## Security Testing

### Automated Security Tests

```kotlin
// SecurityTest.kt - Automated security testing
@RunWith(AndroidJUnit4::class)
class SecurityTest {

    @Test
    fun testPasswordFieldsNotStored() {
        // Create password field
        val passwordField = mock(AccessibilityNodeInfo::class.java)
        whenever(passwordField.isPassword).thenReturn(true)
        whenever(passwordField.text).thenReturn("secret123")

        // Verify filter catches it
        assertTrue(AccessibilityDataFilter.shouldFilterElement(passwordField))
    }

    @Test
    fun testSensitiveAppNotScraped() {
        val service = VoiceOSService()

        // Banking app
        val event = mock(AccessibilityEvent::class.java)
        whenever(event.packageName).thenReturn("com.chase.sig.android")

        // Verify not processed
        service.onAccessibilityEvent(event)
        verify(scrapingManager, never()).processElement(any())
    }

    @Test
    fun testDatabaseEncrypted() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = VoiceOSAppDatabase.getInstance(context)

        // Verify SQLCipher is used
        assertTrue(db.openHelper.readableDatabase is SQLiteDatabase)

        // Try to read database file directly (should fail)
        val dbFile = context.getDatabasePath("voiceos_database")
        val content = dbFile.readText()

        // Encrypted content should not contain plaintext
        assertFalse(content.contains("test_command"))
    }

    @Test
    fun testTLSEnforced() {
        val client = NetworkSecurityConfig.createSecureOkHttpClient()

        // Verify TLS 1.3 is enforced
        val connectionSpecs = client.connectionSpecs
        assertTrue(connectionSpecs.all { spec ->
            spec.tlsVersions?.contains(TlsVersion.TLS_1_3) == true ||
            spec.tlsVersions?.contains(TlsVersion.TLS_1_2) == true
        })
    }

    @Test
    fun testNoPlaintextTraffic() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val applicationInfo = context.applicationInfo

        // Verify cleartext traffic is disabled
        assertFalse(applicationInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC != 0)
    }

    @Test
    fun testRateLimitingPreventsAbuse() {
        val service = VoiceOSService()

        // Send 1000 events rapidly
        repeat(1000) {
            val event = createMockEvent()
            service.onAccessibilityEvent(event)
        }

        // Verify not all events were processed (rate limited)
        verify(scrapingManager, atMost(200)).processElement(any())
    }
}
```

### Penetration Testing Checklist

- [ ] **Authentication & Authorization**
  - [ ] Test permission bypass attempts
  - [ ] Test IPC signature verification
  - [ ] Test unauthorized access to accessibility service

- [ ] **Data Protection**
  - [ ] Verify database encryption
  - [ ] Verify preferences encryption
  - [ ] Test data export/deletion

- [ ] **Network Security**
  - [ ] Test MITM attack resistance
  - [ ] Test certificate pinning bypass
  - [ ] Test cleartext traffic blocking

- [ ] **Code Security**
  - [ ] Test reverse engineering resistance
  - [ ] Test API key extraction
  - [ ] Test debugger detection

- [ ] **Privacy**
  - [ ] Verify no PII in logs
  - [ ] Verify no PII in telemetry
  - [ ] Test data minimization

---

## Compliance and Standards

### GDPR Compliance

VOS4 implements GDPR requirements:

| Requirement | Implementation |
|------------|----------------|
| Right to Access | DataExporter.exportUserData() |
| Right to Erasure | DataDeletor.deleteAllUserData() |
| Data Minimization | Only collect essential data |
| Purpose Limitation | Clear data usage purposes |
| Storage Limitation | 7-30 day retention policies |
| Consent | Explicit opt-in for optional features |
| Data Portability | JSON export format |
| Security | Encryption at rest and in transit |

### OWASP Mobile Top 10

VOS4 addresses OWASP Mobile Security risks:

| Risk | Mitigation |
|------|-----------|
| M1: Improper Platform Usage | Proper permission usage |
| M2: Insecure Data Storage | SQLCipher + EncryptedPrefs |
| M3: Insecure Communication | TLS 1.3 + Certificate Pinning |
| M4: Insecure Authentication | Biometric + PIN (planned) |
| M5: Insufficient Cryptography | AES-256, SHA-256 |
| M6: Insecure Authorization | Signature verification |
| M7: Client Code Quality | R8 obfuscation + ProGuard |
| M8: Code Tampering | Integrity checks |
| M9: Reverse Engineering | Native code + obfuscation |
| M10: Extraneous Functionality | Remove debug code in release |

### Security Audit Schedule

- **Code Review:** Every merge request
- **Dependency Scan:** Weekly (Dependabot)
- **Penetration Testing:** Quarterly
- **Security Audit:** Annually (external)
- **Compliance Review:** Bi-annually

---

## Summary

VOS4's security architecture ensures comprehensive protection through:

1. **Permission Management**: Least privilege, runtime validation, transparent rationale
2. **Data Encryption**: SQLCipher, EncryptedSharedPreferences, TLS 1.3
3. **Privacy by Design**: Minimal collection, ephemeral storage, on-device processing
4. **Accessibility Security**: Sensitive app filtering, password field exclusion, rate limiting
5. **Secure Communication**: Certificate pinning, TLS enforcement, signature verification
6. **API Key Protection**: Native code storage, obfuscation, runtime validation
7. **User Data Protection**: GDPR-compliant export/deletion, data retention policies
8. **Threat Mitigation**: Defense in depth against malicious apps, MITM, reverse engineering
9. **Security Testing**: Automated tests, penetration testing, continuous monitoring
10. **Compliance**: GDPR, OWASP Mobile Top 10, industry standards

These security measures ensure that VOS4 can be trusted with sensitive accessibility permissions while protecting user privacy and preventing unauthorized access.

---

**Next Chapter:** [Chapter 20: Current State Analysis](20-Current-State-Analysis.md)

---

**Document Information**
- **Created:** 2025-11-02
- **Version:** 1.0.0
- **Status:** Complete
- **Author:** VOS4 Development Team
- **Pages:** 54

