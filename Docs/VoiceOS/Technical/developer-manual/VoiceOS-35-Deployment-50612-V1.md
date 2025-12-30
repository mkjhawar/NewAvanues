# Chapter 35: Deployment

**VOS4 Developer Manual**
**Version:** 4.0
**Last Updated:** 2025-11-02
**Chapter:** 35 of 35

---

## Table of Contents

- [35.1 Deployment Overview](#351-deployment-overview)
- [35.2 APK Generation](#352-apk-generation)
- [35.3 Signing & Release](#353-signing--release)
- [35.4 Vivoka Model Deployment](#354-vivoka-model-deployment)
- [35.5 Version Management](#355-version-management)
- [35.6 Update Strategy](#356-update-strategy)
- [35.7 Distribution Channels](#357-distribution-channels)
- [35.8 Beta Testing](#358-beta-testing)
- [35.9 Monitoring and Analytics](#359-monitoring-and-analytics)
- [35.10 Rollback Procedures](#3510-rollback-procedures)
- [35.11 Deployment Scripts](#3511-deployment-scripts)

---

## 35.1 Deployment Overview

### 35.1.1 Deployment Process

VOS4 follows a structured deployment pipeline:

```
┌─────────────────────────────────────────────┐
│         VOS4 Deployment Pipeline             │
├─────────────────────────────────────────────┤
│                                              │
│  1. Development                             │
│     ├── Feature branches                     │
│     ├── Code review                          │
│     └── Merge to develop                     │
│                                              │
│  2. Testing                                  │
│     ├── Unit tests (389 test files)         │
│     ├── Integration tests                    │
│     └── UI tests                             │
│                                              │
│  3. Build                                    │
│     ├── Debug build (testing)                │
│     └── Release build (distribution)         │
│                                              │
│  4. Signing                                  │
│     ├── Debug keystore (dev/test)           │
│     └── Release keystore (production)        │
│                                              │
│  5. Distribution                             │
│     ├── Internal testing                     │
│     ├── Beta testing                         │
│     └── Production release                   │
│                                              │
│  6. Monitoring                               │
│     ├── Crash reporting                      │
│     ├── Performance metrics                  │
│     └── User analytics                       │
│                                              │
└─────────────────────────────────────────────┘
```

### 35.1.2 Release Cadence

**VOS4 Release Schedule:**

- **Major Releases**: Quarterly (4.0, 5.0, etc.)
- **Minor Releases**: Monthly (4.1, 4.2, etc.)
- **Patch Releases**: As needed (4.0.1, 4.0.2, etc.)
- **Hotfixes**: Emergency fixes (< 24 hours)

**Current Version (as of 2025-11-02):**
- **Version Name**: 3.0.0
- **Version Code**: 1
- **Target SDK**: 34 (Android 14)
- **Min SDK**: 29 (Android 10)

### 35.1.3 Deployment Checklist

**Pre-Deployment Checklist:**

```markdown
## Pre-Deployment Checklist

### Code Quality
- [ ] All tests passing (unit, integration, UI)
- [ ] Code review completed and approved
- [ ] No critical lint warnings
- [ ] Static analysis clean (Detekt, Ktlint)
- [ ] ProGuard rules tested

### Documentation
- [ ] CHANGELOG.md updated
- [ ] README.md reflects latest version
- [ ] API documentation up to date
- [ ] Migration guide created (if breaking changes)
- [ ] User documentation updated

### Build
- [ ] Debug build successful
- [ ] Release build successful
- [ ] APK size acceptable (<100MB)
- [ ] No build warnings
- [ ] Native libraries included

### Security
- [ ] Security review completed
- [ ] No hardcoded secrets
- [ ] Permissions justified
- [ ] ProGuard enabled for release
- [ ] Certificate pinning configured (if applicable)

### Testing
- [ ] Manual testing on Android 10-14
- [ ] Accessibility testing (TalkBack)
- [ ] Performance benchmarks passed
- [ ] Battery drain acceptable
- [ ] Memory leaks checked

### Versioning
- [ ] Version code incremented
- [ ] Version name updated
- [ ] Git tag created
- [ ] Release notes drafted
```

---

## 35.2 APK Generation

### 35.2.1 Build Commands

**Debug APK:**

```bash
# Build debug APK (unsigned, debuggable)
./gradlew assembleDebug

# Output location:
# app/build/outputs/apk/debug/app-debug.apk
# Size: ~70-80MB (with all dependencies)
```

**Release APK:**

```bash
# Build release APK (unsigned)
./gradlew assembleRelease

# Output location:
# app/build/outputs/apk/release/app-release-unsigned.apk
# Size: ~45-55MB (minified, shrunk)
```

**Bundle (AAB):**

```bash
# Build Android App Bundle (for Play Store)
./gradlew bundleRelease

# Output location:
# app/build/outputs/bundle/release/app-release.aab
# Size: ~30-35MB (smaller than APK)
```

### 35.2.2 Output Locations

**Build Output Structure:**

```
app/build/outputs/
├── apk/
│   ├── debug/
│   │   ├── app-debug.apk
│   │   └── output-metadata.json
│   └── release/
│       ├── app-release-unsigned.apk
│       ├── app-release.apk (if signed)
│       └── output-metadata.json
├── bundle/
│   └── release/
│       └── app-release.aab
├── logs/
│   └── manifest-merger-release-report.txt
└── mapping/
    └── release/
        ├── mapping.txt         # ProGuard mapping
        ├── configuration.txt   # ProGuard config
        ├── seeds.txt           # Kept classes
        └── usage.txt           # Removed classes
```

### 35.2.3 APK Structure

**APK Contents:**

```
app-release.apk
├── AndroidManifest.xml          # App manifest
├── META-INF/
│   ├── MANIFEST.MF
│   ├── CERT.SF                  # Certificate signature
│   └── CERT.RSA                 # Certificate
├── classes.dex                  # Dalvik bytecode (optimized)
├── classes2.dex                 # Additional classes (if multidex)
├── res/                         # Resources (optimized)
│   ├── drawable/
│   ├── layout/
│   ├── values/
│   └── ...
├── resources.arsc               # Resource table
├── lib/                         # Native libraries
│   ├── arm64-v8a/
│   │   ├── libwhisper_jni.so
│   │   └── ...
│   └── armeabi-v7a/
│       ├── libwhisper_jni.so
│       └── ...
└── assets/                      # Asset files
    ├── models/
    └── ...
```

**APK Size Breakdown:**

```
Total APK Size: ~50MB (release, ARM-only)
├── Code (classes.dex): ~8MB
├── Resources: ~5MB
├── Native libs (ARM): ~30MB
│   ├── arm64-v8a: ~20MB
│   └── armeabi-v7a: ~10MB
└── Assets: ~7MB
    └── Models/data: ~7MB

Without x86/x86_64: Saves ~150MB
```

---

## 35.3 Signing & Release

### 35.3.1 Keystore Management

**Debug Keystore:**

```bash
# Debug keystore location (auto-generated)
~/.android/debug.keystore

# Debug keystore credentials (standard)
Keystore password: android
Key alias: androiddebugkey
Key password: android
```

**Release Keystore:**

```bash
# Create release keystore (one-time setup)
keytool -genkey -v \
  -keystore voiceos-release.keystore \
  -alias voiceos \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD

# Store securely (NOT in version control)
# Recommended: Use environment variables or secret manager
```

**Keystore Information:**

```bash
# List keystore contents
keytool -list -v -keystore voiceos-release.keystore

# Export certificate
keytool -exportcert -alias voiceos \
  -keystore voiceos-release.keystore \
  -file voiceos.crt

# Get SHA1 fingerprint (for Play Console)
keytool -list -v -keystore voiceos-release.keystore | grep SHA1
```

### 35.3.2 Signing Configuration

**gradle.properties (gitignored):**

```properties
# Signing configuration (DO NOT commit)
RELEASE_STORE_FILE=/path/to/voiceos-release.keystore
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=voiceos
RELEASE_KEY_PASSWORD=your_key_password
```

**app/build.gradle.kts:**

```kotlin
android {
    signingConfigs {
        create("release") {
            // Read from gradle.properties or environment
            storeFile = file(project.properties["RELEASE_STORE_FILE"] as String?
                ?: System.getenv("RELEASE_STORE_FILE")
                ?: "")
            storePassword = project.properties["RELEASE_STORE_PASSWORD"] as String?
                ?: System.getenv("RELEASE_STORE_PASSWORD")
            keyAlias = project.properties["RELEASE_KEY_ALIAS"] as String?
                ?: System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = project.properties["RELEASE_KEY_PASSWORD"] as String?
                ?: System.getenv("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... other config
        }
    }
}
```

### 35.3.3 Release Builds

**Build Signed Release APK:**

```bash
# Build and sign in one command
./gradlew assembleRelease

# Output:
# app/build/outputs/apk/release/app-release.apk (signed)
```

**Manual Signing (if needed):**

```bash
# Build unsigned APK
./gradlew assembleRelease

# Sign APK manually
jarsigner -verbose -sigalg SHA1withRSA \
  -digestalg SHA1 \
  -keystore voiceos-release.keystore \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  voiceos

# Verify signature
jarsigner -verify -verbose -certs \
  app/build/outputs/apk/release/app-release-unsigned.apk

# Align APK (optimize)
zipalign -v 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  app/build/outputs/apk/release/app-release.apk
```

---

## 35.4 Vivoka Model Deployment

### 35.4.1 Deployment Overview

VoiceOS supports three deployment strategies for Vivoka speech recognition models:

1. **APK Bundle** (Simple): Models embedded in APK (~100-200MB per language)
2. **External Pre-Deploy** (Recommended for Development): Models deployed to device storage
3. **On-Demand Download** (Production): Models downloaded via Firebase when needed

**Key Benefits:**
- Smaller APK size (~50MB vs 150-250MB with models)
- Faster development iteration (no rebuild for model changes)
- User flexibility (pre-load for offline, download on-demand)
- Survives app uninstall (shared folder option)

### 35.4.2 Deployment Strategy Matrix

| Strategy | APK Size | Network Required | Survives Uninstall | Best For |
|----------|----------|------------------|-------------------|----------|
| **APK Bundle** | +100-200MB/lang | No | No | Offline demos, single language |
| **External Pre-Deploy** | Small (~50MB) | No | Yes (shared folder) | Development/Testing |
| **On-Demand Download** | Small (~50MB) | Yes (first use) | No | Production, multi-language |

### 35.4.3 External Pre-Deployment (Development)

**Use Case:** Development, testing, rapid iteration without rebuilding APK.

#### Step 1: Prepare Model Files

**Required Structure:**
```
vsdk/
├── config/
│   └── vsdk.json                    # VSDK configuration (REQUIRED)
└── data/
    └── csdk/
        └── asr/
            ├── acmod/              # Acoustic models (REQUIRED)
            ├── clc/                # Language components (for dynamic grammar)
            ├── ctx/                # Context files (REQUIRED)
            └── lm/                 # Language models (for free speech)
```

**Where to Obtain:**
- VDK Studio export
- Internal model repository
- Firebase storage backup
- Contact Vivoka support

#### Step 2: Deploy to Device

**Three Deployment Options:**

**Option A: Shared Hidden Folder (RECOMMENDED)**
- Path: `/storage/emulated/0/.voiceos/vivoka/vsdk/`
- **Survives app uninstall** ✅
- Accessible via file manager (hidden)
- Shared across all VoiceOS installations

```bash
# Deploy via ADB
adb push ./vsdk /storage/emulated/0/.voiceos/vivoka/vsdk/

# Verify deployment
adb shell ls -la /storage/emulated/0/.voiceos/vivoka/vsdk/config/
adb shell ls -la /storage/emulated/0/.voiceos/vivoka/vsdk/data/csdk/asr/acmod/
```

**Option B: External App-Specific Storage**
- Path: `/storage/emulated/0/Android/data/com.augmentalis.voiceos/files/vsdk/`
- Accessible via file manager
- **Deleted on app uninstall** ❌

```bash
# Deploy via ADB
adb push ./vsdk /storage/emulated/0/Android/data/com.augmentalis.voiceos/files/vsdk/

# Verify
adb shell ls -la /storage/emulated/0/Android/data/com.augmentalis.voiceos/files/vsdk/
```

**Option C: Internal App Storage**
- Path: `/data/data/com.augmentalis.voiceos/files/vsdk/`
- Not accessible via file manager
- Requires `run-as` or root access
- **Deleted on app uninstall** ❌

```bash
# Deploy via ADB (requires run-as)
adb push ./vsdk /data/local/tmp/
adb shell run-as com.augmentalis.voiceos cp -r /data/local/tmp/vsdk /data/data/com.augmentalis.voiceos/files/
adb shell rm -rf /data/local/tmp/vsdk

# Verify
adb shell run-as com.augmentalis.voiceos ls -la /data/data/com.augmentalis.voiceos/files/vsdk/
```

#### Step 3: Verify Deployment

```bash
# Check model size (should be ~100-200MB)
adb shell "du -sh /storage/emulated/0/.voiceos/vivoka/vsdk/"

# Clear app data to force initialization
adb shell pm clear com.augmentalis.voiceos

# Launch app
adb shell am start -n com.augmentalis.voiceos/.MainActivity

# Monitor logs
adb logcat -s VivokaPathResolver:* VivokaConfig:* VivokaInitializer:*
```

**Expected Log Output:**
```
VivokaPathResolver: Searching for VSDK in locations:
  - /data/data/com.augmentalis.voiceos/files/vsdk
  - /storage/emulated/0/Android/data/com.augmentalis.voiceos/files/vsdk
  - /storage/emulated/0/.voiceos/vivoka/vsdk
VivokaPathResolver: Found valid VSDK directory at: /storage/emulated/0/.voiceos/vivoka/vsdk
VivokaConfig: Using EXTERNAL VSDK location (pre-deployed or fallback)
VivokaConfig: Asset paths configured - assets: /storage/emulated/0/.voiceos/vivoka/vsdk
VivokaConfig: Vivoka configuration initialized successfully
```

### 35.4.4 On-Demand Download (Production)

**Use Case:** Production releases, multi-language support, automatic language switching.

#### Firebase Remote Config Setup

**1. Configure Model URLs:**

```json
{
  "es_json": "https://www.augmentalis.com/avanuevoiceosava/Spanish/spanish.json",
  "es_voice_resource": "https://www.augmentalis.com/avanuevoiceosava/Spanish/es_voice_resource.zip",
  "fr_json": "https://www.augmentalis.com/avanuevoiceosava/French/french.json",
  "fr_voice_resource": "https://www.augmentalis.com/avanuevoiceosava/French/fr_voice_resource.zip",
  "ja_json": "https://www.augmentalis.com/avanuevoiceosava/Japanese/japanese.json",
  "ja_voice_resource": "https://www.augmentalis.com/avanuevoiceosava/Japanese/ja_voice_resource.zip"
}
```

**2. Download Flow Implementation:**

```kotlin
// In MainActivity.kt or SettingsActivity.kt
firebaseRepo.getLanguageResource("es") { status ->
    when (status) {
        is FileStatus.Downloading -> {
            // Update progress UI
            binding.progressBar.progress = status.progress
            binding.statusText.text = "Downloading Spanish: ${status.progress}%"
        }
        FileStatus.Extracting -> {
            binding.statusText.text = "Extracting Spanish model files..."
        }
        FileStatus.Completed -> {
            binding.statusText.text = "Spanish ready!"
            // Re-initialize Vivoka engine
            speechEngineManager.initializeEngine(SpeechEngine.VIVOKA)
        }
        is FileStatus.Error -> {
            // Show error dialog with retry option
            showErrorDialog("Download failed: ${status.error}")
        }
    }
}
```

**3. Handle Download Failures:**

```kotlin
class ModelDownloadManager(
    private val firebaseRepo: FirebaseRepository,
    private val pathResolver: VivokaPathResolver
) {
    fun downloadLanguageModel(
        languageCode: String,
        maxRetries: Int = 3
    ): Flow<DownloadStatus> = flow {
        var attempt = 0
        var lastError: Exception? = null

        while (attempt < maxRetries) {
            try {
                firebaseRepo.getLanguageResource(languageCode) { status ->
                    emit(status)
                }
                return@flow // Success
            } catch (e: Exception) {
                lastError = e
                attempt++
                if (attempt < maxRetries) {
                    delay(2000L * attempt) // Exponential backoff
                }
            }
        }

        emit(FileStatus.Error("Failed after $maxRetries attempts: ${lastError?.message}"))
    }
}
```

### 35.4.5 CI/CD Integration

**Build Configurations:**

**Debug Builds:**
```gradle
android {
    buildTypes {
        debug {
            // Don't bundle models in debug builds
            // Developers use pre-deployed external models
            sourceSets {
                main {
                    assets.srcDirs = []  // Exclude all assets
                }
            }
        }
    }
}
```

**Release Builds:**

**Option A: Bundle Base English Only**
```gradle
android {
    buildTypes {
        release {
            // Bundle minimal English model for offline fallback
            sourceSets {
                main {
                    assets.srcDirs = ['src/main/assets_en']
                }
            }
        }
    }
}
```

**Option B: Pure On-Demand (Recommended)**
```gradle
android {
    buildTypes {
        release {
            // No bundled models - pure on-demand download
            // Smallest APK size (~50MB)
            sourceSets {
                main {
                    assets.srcDirs = []
                }
            }
        }
    }
}
```

### 35.4.6 Testing Checklist

**Before Release:**
- [ ] Verify all 3 path locations checked in priority order
- [ ] Test with no models (download works)
- [ ] Test with external models (no download triggered)
- [ ] Test with internal models (takes priority)
- [ ] Verify Firebase URLs are correct and accessible
- [ ] Test download failure handling (retry logic)
- [ ] Verify model extraction works correctly
- [ ] Test language switching (download + initialization)
- [ ] Test offline mode (no network, using cached models)
- [ ] Verify models persist across app restarts

**Performance Metrics:**
- APK size without models: ~50MB (vs 150-250MB with models)
- First download time: ~60 seconds per language (over Wi-Fi)
- External model detection: <100ms
- Model initialization: ~2-3 seconds
- Download failure retry: 2s, 4s, 6s exponential backoff

### 35.4.7 Related Documentation

**Developer Manual:**
- **Chapter 7.5.1:** Vivoka Model Deployment (implementation details)
- **Chapter 7:** SpeechRecognition Library (architecture)

**Active Documentation:**
- `Vivoka-Model-File-Paths-251120.md` - Technical reference
- `README_VIVOKA_DYNAMIC_MODULE.md` - Firebase integration guide

**User Manual:**
- **Appendix F:** Managing Voice Recognition Models (end-user guide)

**Code References:**
- `VivokaPathResolver.kt:48` - Multi-location resolution
- `VivokaConfig.kt:87` - Path setup and validation
- `VivokaInitializer.kt` - Download and extraction logic

---

## 35.5 Version Management

### 35.5.1 Version Codes

**Version Code Strategy:**

```kotlin
// app/build.gradle.kts (lines 20-21)
versionCode = 1      // Integer, auto-incremented
versionName = "3.0.0"  // Semantic version
```

**Version Code Scheme:**

```
Version Code Format: XXYYZZZZ
XX   = Major version (01-99)
YY   = Minor version (00-99)
ZZZZ = Patch/Build (0000-9999)

Examples:
3.0.0   → 03000000 = 3000000
3.1.0   → 03010000 = 3010000
3.1.5   → 03010005 = 3010005
4.0.0   → 04000000 = 4000000
```

**Implementation:**

```kotlin
// Automated version code calculation
val majorVersion = 3
val minorVersion = 0
val patchVersion = 0

val versionCode = majorVersion * 1000000 +
                  minorVersion * 10000 +
                  patchVersion

defaultConfig {
    versionCode = versionCode  // 3000000
    versionName = "$majorVersion.$minorVersion.$patchVersion"  // "3.0.0"
}
```

### 35.5.2 Version Names

**Semantic Versioning (SemVer):**

```
Format: MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]

Examples:
3.0.0           # Stable release
3.1.0-alpha.1   # Alpha pre-release
3.1.0-beta.2    # Beta pre-release
3.1.0-rc.1      # Release candidate
3.1.0+20251102  # Build metadata
```

**Version Bump Rules:**

- **MAJOR**: Breaking changes, incompatible API changes
- **MINOR**: New features, backwards-compatible
- **PATCH**: Bug fixes, backwards-compatible

**Examples:**

```
2.5.3 → 3.0.0  # Breaking change (accessibility service refactor)
3.0.0 → 3.1.0  # New feature (offline voice recognition)
3.1.0 → 3.1.1  # Bug fix (crash on Android 10)
```

### 35.5.3 Semantic Versioning

**Version Naming Convention:**

```kotlin
// Version name with pre-release
versionName = "3.1.0-beta.2"

// Version name with build metadata
versionName = "3.1.0+20251102.commit.a1b2c3d"

// Full version string
versionName = "3.1.0-beta.2+20251102"
```

**Git Tagging:**

```bash
# Create version tag
git tag -a v3.1.0 -m "Release version 3.1.0"

# Push tags to remote
git push origin v3.1.0

# List all tags
git tag -l "v*"

# Delete tag (if mistake)
git tag -d v3.1.0
git push origin :refs/tags/v3.1.0
```

---

## 35.6 Update Strategy

### 35.6.1 OTA Updates

**Over-The-Air Update Mechanism:**

```kotlin
// Firebase Remote Config for update prompts
class UpdateManager(
    private val remoteConfig: FirebaseRemoteConfig
) {
    companion object {
        private const val KEY_MIN_VERSION_CODE = "min_version_code"
        private const val KEY_LATEST_VERSION = "latest_version"
        private const val KEY_UPDATE_MESSAGE = "update_message"
        private const val KEY_FORCE_UPDATE = "force_update"
    }

    fun checkForUpdates(): UpdateInfo {
        val currentVersionCode = BuildConfig.VERSION_CODE
        val minVersionCode = remoteConfig.getLong(KEY_MIN_VERSION_CODE)
        val latestVersion = remoteConfig.getString(KEY_LATEST_VERSION)
        val updateMessage = remoteConfig.getString(KEY_UPDATE_MESSAGE)
        val forceUpdate = remoteConfig.getBoolean(KEY_FORCE_UPDATE)

        return when {
            currentVersionCode < minVersionCode -> UpdateInfo.ForceUpdate(
                latestVersion, updateMessage
            )
            forceUpdate -> UpdateInfo.RecommendedUpdate(
                latestVersion, updateMessage
            )
            else -> UpdateInfo.NoUpdate
        }
    }

    sealed class UpdateInfo {
        object NoUpdate : UpdateInfo()
        data class ForceUpdate(val version: String, val message: String) : UpdateInfo()
        data class RecommendedUpdate(val version: String, val message: String) : UpdateInfo()
    }
}
```

**Update Prompt UI:**

```kotlin
@Composable
fun UpdateDialog(
    updateInfo: UpdateManager.UpdateInfo,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    when (updateInfo) {
        is UpdateManager.UpdateInfo.ForceUpdate -> {
            AlertDialog(
                onDismissRequest = { /* Cannot dismiss */ },
                title = { Text("Update Required") },
                text = { Text(updateInfo.message) },
                confirmButton = {
                    Button(onClick = onUpdate) {
                        Text("Update Now")
                    }
                }
            )
        }

        is UpdateManager.UpdateInfo.RecommendedUpdate -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Update Available") },
                text = { Text(updateInfo.message) },
                confirmButton = {
                    Button(onClick = onUpdate) {
                        Text("Update")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Later")
                    }
                }
            )
        }

        else -> {}
    }
}
```

### 35.6.2 Migration Handling

**Database Migrations:**

```kotlin
// Room database migrations
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new column
        database.execSQL("ALTER TABLE commands ADD COLUMN confidence REAL DEFAULT 0.8")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create new table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS app_settings (
                id INTEGER PRIMARY KEY NOT NULL,
                key TEXT NOT NULL,
                value TEXT NOT NULL,
                UNIQUE(key)
            )
        """.trimIndent())
    }
}

// Apply migrations
Room.databaseBuilder(context, AppDatabase::class.java, "voiceos.db")
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
    .build()
```

**SharedPreferences Migration:**

```kotlin
class PreferencesMigration(private val context: Context) {

    fun migrate() {
        val currentVersion = getCurrentVersion()
        val targetVersion = BuildConfig.VERSION_CODE

        if (currentVersion < targetVersion) {
            when {
                currentVersion < 3000000 -> migrateFrom2x()
                currentVersion < 3010000 -> migrateFrom30x()
            }

            setCurrentVersion(targetVersion)
        }
    }

    private fun migrateFrom2x() {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Rename preference keys
        prefs.getString("old_key", null)?.let { value ->
            prefs.edit()
                .putString("new_key", value)
                .remove("old_key")
                .apply()
        }
    }

    private fun migrateFrom30x() {
        // Migration from 3.0.x to 3.1.x
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Convert int to long
        val oldValue = prefs.getInt("timeout_ms", 5000)
        prefs.edit()
            .putLong("timeout_ms", oldValue.toLong())
            .apply()
    }

    private fun getCurrentVersion(): Int {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("app_version", 0)
    }

    private fun setCurrentVersion(version: Int) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("app_version", version)
            .apply()
    }
}
```

### 35.6.3 Backwards Compatibility

**API Compatibility:**

```kotlin
// Handle API changes gracefully
object CompatibilityHelper {

    @RequiresApi(29)
    fun getAccessibilityService(context: Context): AccessibilityManager {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

        return when {
            Build.VERSION.SDK_INT >= 31 -> {
                // Android 12+ (API 31)
                manager.apply {
                    // Use new APIs
                }
            }
            Build.VERSION.SDK_INT >= 30 -> {
                // Android 11 (API 30)
                manager.apply {
                    // Use compatible APIs
                }
            }
            else -> {
                // Android 10 (API 29)
                manager.apply {
                    // Use fallback APIs
                }
            }
        }
    }

    fun requestPermission(activity: Activity, permission: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(arrayOf(permission), 0)
        } else {
            // Permission auto-granted on older versions
        }
    }
}
```

---

## 35.7 Distribution Channels

### 35.7.1 Google Play Store

**Publishing Steps:**

1. **Create App:**
   - Go to Google Play Console
   - Create new app: "VoiceOS"
   - Fill in app details

2. **Upload APK/AAB:**
   ```bash
   ./gradlew bundleRelease
   # Upload: app/build/outputs/bundle/release/app-release.aab
   ```

3. **Configure Store Listing:**
   - Title: "VoiceOS - Voice Controlled Operating System"
   - Short description: "Control your device with voice commands"
   - Full description: Detailed features
   - Screenshots: 2-8 screenshots per device type
   - Feature graphic: 1024x500 px
   - Icon: 512x512 px

4. **Content Rating:**
   - Complete questionnaire
   - Get rating (likely E for Everyone)

5. **Pricing & Distribution:**
   - Free or Paid
   - Select countries
   - Device categories (phone, tablet, Wear OS, TV, Auto)

6. **Release:**
   - Internal testing → Closed testing → Open testing → Production

### 35.7.2 F-Droid

**F-Droid Metadata:**

```yaml
# metadata/com.augmentalis.voiceos.yml
Categories:
  - Accessibility
License: GPL-3.0-or-later
AuthorName: Augmentalis
AuthorEmail: support@augmentalis.com
WebSite: https://github.com/augmentalis/voiceos
SourceCode: https://github.com/augmentalis/voiceos
IssueTracker: https://github.com/augmentalis/voiceos/issues

AutoName: VoiceOS
Summary: Voice-controlled operating system
Description: |-
    VoiceOS provides comprehensive voice control for Android devices.

    Features:
    * Voice commands for navigation
    * Offline speech recognition
    * Custom command creation
    * Accessibility-first design

RepoType: git
Repo: https://github.com/augmentalis/voiceos.git

Builds:
  - versionName: '3.0.0'
    versionCode: 1
    commit: v3.0.0
    gradle:
      - yes
    rm:
      - vivoka/*.aar  # Remove proprietary libraries
    prebuild: echo "org.gradle.jvmargs=-Xmx2g" >> gradle.properties

AutoUpdateMode: Version v%v
UpdateCheckMode: Tags
CurrentVersion: '3.0.0'
CurrentVersionCode: 1
```

### 35.7.3 Direct Distribution

**Self-Hosted APK:**

```bash
# Generate release APK
./gradlew assembleRelease

# Create SHA256 checksum
sha256sum app/build/outputs/apk/release/app-release.apk > app-release.apk.sha256

# Host on website
# https://downloads.augmentalis.com/voiceos/
# ├── app-release.apk
# ├── app-release.apk.sha256
# └── VERSION.txt
```

**VERSION.txt:**

```json
{
  "version_name": "3.0.0",
  "version_code": 1,
  "min_sdk": 29,
  "target_sdk": 34,
  "size_bytes": 52428800,
  "sha256": "a1b2c3d4e5f6...",
  "release_date": "2025-11-02",
  "release_notes": "https://github.com/augmentalis/voiceos/releases/tag/v3.0.0",
  "download_url": "https://downloads.augmentalis.com/voiceos/app-release.apk"
}
```

### 35.7.4 Enterprise Distribution

**Enterprise MDM:**

```xml
<!-- managed_configurations.xml -->
<restrictions xmlns:android="http://schemas.android.com/apk/res/android">

    <restriction
        android:key="voice_engine"
        android:title="Default Voice Engine"
        android:restrictionType="choice"
        android:defaultValue="android_stt">
        <choice android:value="android_stt">Android STT</choice>
        <choice android:value="vosk">Vosk Offline</choice>
    </restriction>

    <restriction
        android:key="confidence_threshold"
        android:title="Confidence Threshold"
        android:restrictionType="integer"
        android:defaultValue="80"
        android:min="50"
        android:max="100"/>

    <restriction
        android:key="allow_custom_commands"
        android:title="Allow Custom Commands"
        android:restrictionType="bool"
        android:defaultValue="true"/>

</restrictions>
```

**Read Configuration:**

```kotlin
class EnterpriseConfigManager(private val context: Context) {

    fun getConfiguration(): EnterpriseConfig {
        val restrictions = context.getSystemService(Context.RESTRICTIONS_SERVICE)
            as RestrictionsManager

        val bundle = restrictions.applicationRestrictions

        return EnterpriseConfig(
            voiceEngine = bundle.getString("voice_engine") ?: "android_stt",
            confidenceThreshold = bundle.getInt("confidence_threshold", 80) / 100f,
            allowCustomCommands = bundle.getBoolean("allow_custom_commands", true)
        )
    }
}

data class EnterpriseConfig(
    val voiceEngine: String,
    val confidenceThreshold: Float,
    val allowCustomCommands: Boolean
)
```

---

## 35.8 Beta Testing

### 35.8.1 Internal Testing

**Internal Test Track (Play Console):**

```
Purpose: Quick validation before wider testing
Testers: Development team (up to 100 testers)
Review: No review required
Distribution: Via email list
Release notes: Internal notes only
```

**Setup:**

1. Go to Play Console → Testing → Internal testing
2. Create release
3. Upload AAB
4. Add testers (email addresses)
5. Release

**Tester Access:**

```
Internal testers receive:
- Email with opt-in link
- Direct download from Play Store
- Automatic updates
- Can leave feedback
```

### 35.8.2 Closed Beta

**Closed Testing Track:**

```
Purpose: Wider testing with selected users
Testers: Up to 1000 testers (can request more)
Review: May require review
Distribution: Via opt-in link or email list
Feedback: Google Play Console feedback
```

**Beta Invitation Email:**

```
Subject: Join VoiceOS Beta Testing

Hi [Name],

You're invited to join the VoiceOS beta testing program!

VoiceOS is a voice-controlled operating system that allows you to
control your Android device using voice commands.

As a beta tester, you'll:
- Get early access to new features
- Help shape the product with your feedback
- Report bugs and suggest improvements

To join:
1. Click this link: [Opt-in URL]
2. Accept the invitation
3. Install VoiceOS from the Play Store

Thank you for helping us improve VoiceOS!

The VoiceOS Team
```

### 35.8.3 Open Beta

**Open Testing Track:**

```
Purpose: Public beta before production release
Testers: Unlimited (anyone can join)
Review: Requires review (like production)
Distribution: Public opt-in link
Visibility: Listed in Play Store
```

**Open Beta Guidelines:**

1. **Feature Complete**: All major features implemented
2. **Stable**: No critical bugs
3. **Performance**: Meets performance targets
4. **Documentation**: Help docs available
5. **Feedback Channel**: Issue tracker or forum

**Release Notes Example:**

```markdown
# VoiceOS v3.1.0 Beta

## What's New
- Offline voice recognition with Vosk
- Custom command creation
- Improved accessibility support
- Material 3 UI redesign

## Known Issues
- Voice recognition may be slower on Android 10
- Some commands not working in landscape mode
- Rare crash when switching voice engines

## How to Provide Feedback
- Report bugs: https://github.com/augmentalis/voiceos/issues
- Suggest features: https://github.com/augmentalis/voiceos/discussions
- Email: beta@augmentalis.com

## Requirements
- Android 10 or higher
- ~50MB free space
- Microphone access

Thank you for testing VoiceOS!
```

---

## 35.9 Monitoring and Analytics

### 35.9.1 Crash Reporting

**Firebase Crashlytics Setup:**

```kotlin
// app/build.gradle.kts
plugins {
    id("com.google.firebase.crashlytics")
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")
}

// Application class
class VoiceOSApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Crashlytics
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            setCustomKey("version_code", BuildConfig.VERSION_CODE)
            setCustomKey("version_name", BuildConfig.VERSION_NAME)
        }
    }
}
```

**Custom Crash Reporting:**

```kotlin
class CrashReporter {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    fun logNonFatalException(exception: Exception, context: String) {
        crashlytics.apply {
            setCustomKey("context", context)
            recordException(exception)
        }
    }

    fun logVoiceRecognitionError(
        engine: String,
        errorMessage: String,
        audioLength: Long
    ) {
        crashlytics.apply {
            setCustomKey("engine", engine)
            setCustomKey("error_message", errorMessage)
            setCustomKey("audio_length_ms", audioLength)
            recordException(VoiceRecognitionException(errorMessage))
        }
    }

    fun setUserIdentifier(userId: String) {
        crashlytics.setUserId(userId)
    }
}

class VoiceRecognitionException(message: String) : Exception(message)
```

### 35.9.2 Performance Monitoring

**Firebase Performance:**

```kotlin
// Track custom traces
class PerformanceMonitor {
    private val performance = FirebasePerformance.getInstance()

    fun trackVoiceRecognition(block: () -> Unit) {
        val trace = performance.newTrace("voice_recognition")
        trace.start()

        try {
            block()
            trace.putAttribute("status", "success")
        } catch (e: Exception) {
            trace.putAttribute("status", "error")
            trace.putAttribute("error_type", e.javaClass.simpleName)
            throw e
        } finally {
            trace.stop()
        }
    }

    fun trackCommandExecution(
        command: String,
        block: () -> Unit
    ) {
        val trace = performance.newTrace("command_execution")
        trace.putAttribute("command", command)
        trace.start()

        try {
            block()
        } finally {
            trace.stop()
        }
    }
}

// Usage
val perfMonitor = PerformanceMonitor()

perfMonitor.trackVoiceRecognition {
    recognitionManager.startListening()
}
```

### 35.9.3 User Analytics

**Firebase Analytics:**

```kotlin
class AnalyticsTracker(private val context: Context) {
    private val analytics = FirebaseAnalytics.getInstance(context)

    fun logCommandExecuted(command: String, success: Boolean) {
        analytics.logEvent("command_executed") {
            param("command_text", command)
            param("success", if (success) 1L else 0L)
        }
    }

    fun logVoiceEngineChanged(newEngine: String) {
        analytics.logEvent("voice_engine_changed") {
            param("engine", newEngine)
        }
    }

    fun logScreenView(screenName: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
    }

    fun logUserProperty(key: String, value: String) {
        analytics.setUserProperty(key, value)
    }
}

// Extension function for cleaner syntax
inline fun FirebaseAnalytics.logEvent(
    name: String,
    block: Bundle.() -> Unit
) {
    val bundle = Bundle().apply(block)
    logEvent(name, bundle)
}
```

---

## 35.10 Rollback Procedures

### 35.10.1 Play Store Rollback

**Halt Rollout:**

1. Go to Play Console → Production
2. Click "Halt rollout"
3. Users with old version keep it
4. Users with new version keep it
5. No new users get new version

**Rollback to Previous Version:**

1. Production → Releases
2. Find previous release
3. Click "Release to production"
4. Staged rollout or full release
5. Users gradually updated to old version

**Emergency Rollback:**

```bash
# In extreme cases, can update directly
# 1. Build previous version
git checkout v2.5.3
./gradlew bundleRelease

# 2. Upload to Play Console
# 3. Release immediately (100%)
# 4. Users auto-update within 24 hours
```

### 35.10.2 Direct Distribution Rollback

**Rollback Procedure:**

```bash
# 1. Remove new version from download server
rm https://downloads.augmentalis.com/voiceos/app-release.apk

# 2. Restore previous version
cp app-release-v2.5.3.apk https://downloads.augmentalis.com/voiceos/app-release.apk

# 3. Update VERSION.txt
cat > VERSION.txt << EOF
{
  "version_name": "2.5.3",
  "version_code": 2050003,
  ...
}
EOF

# 4. Notify users
# Email: "Critical issue found in v3.0.0. Please downgrade to v2.5.3"
```

### 35.10.3 Data Recovery

**Database Rollback:**

```kotlin
class DatabaseRollbackManager(private val context: Context) {

    fun rollbackToVersion(targetVersion: Int) {
        val currentDatabase = context.getDatabasePath("voiceos.db")
        val backupDir = File(context.filesDir, "db_backups")

        // Find backup for target version
        val backup = File(backupDir, "voiceos_v$targetVersion.db")

        if (backup.exists()) {
            // Close current database
            AppDatabase.getInstance(context).close()

            // Replace with backup
            backup.copyTo(currentDatabase, overwrite = true)

            // Reopen database
            AppDatabase.getInstance(context)
        } else {
            throw IllegalStateException("No backup found for version $targetVersion")
        }
    }

    fun createBackup(version: Int) {
        val currentDatabase = context.getDatabasePath("voiceos.db")
        val backupDir = File(context.filesDir, "db_backups").apply { mkdirs() }
        val backup = File(backupDir, "voiceos_v$version.db")

        currentDatabase.copyTo(backup, overwrite = true)
    }
}
```

---

## 35.11 Deployment Scripts

### 35.11.1 Build Script

**build_release.sh:**

```bash
#!/bin/bash
# VoiceOS Release Build Script

set -e  # Exit on error

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'  # No Color

echo "🚀 VoiceOS Release Build Script"
echo "================================"

# Check environment
if [ -z "$RELEASE_STORE_FILE" ]; then
    echo -e "${RED}Error: RELEASE_STORE_FILE not set${NC}"
    exit 1
fi

# Clean build
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Run tests
echo "🧪 Running tests..."
./gradlew test || {
    echo -e "${RED}Tests failed!${NC}"
    exit 1
}

# Build release
echo "🔨 Building release APK..."
./gradlew assembleRelease || {
    echo -e "${RED}Build failed!${NC}"
    exit 1
}

# Verify APK
APK_PATH="app/build/outputs/apk/release/app-release.apk"
if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}APK not found!${NC}"
    exit 1
fi

# Get APK info
APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo -e "${GREEN}✅ Build successful!${NC}"
echo "📦 APK location: $APK_PATH"
echo "📊 APK size: $APK_SIZE"

# Generate checksum
echo "🔐 Generating SHA256 checksum..."
sha256sum "$APK_PATH" > "$APK_PATH.sha256"

# Copy to distribution folder
DIST_DIR="dist/$(date +%Y%m%d)"
mkdir -p "$DIST_DIR"
cp "$APK_PATH" "$DIST_DIR/"
cp "$APK_PATH.sha256" "$DIST_DIR/"

echo -e "${GREEN}✅ Release build complete!${NC}"
echo "Distribution folder: $DIST_DIR"
```

### 35.11.2 Deployment Script

**deploy.sh:**

```bash
#!/bin/bash
# VoiceOS Deployment Script

set -e

echo "📤 VoiceOS Deployment Script"
echo "============================"

# Check version
VERSION=$(grep "versionName" app/build.gradle.kts | cut -d'"' -f2)
echo "Version: $VERSION"

# Confirm deployment
read -p "Deploy version $VERSION to production? (yes/no) " -n 3 -r
echo
if [[ ! $REPLY =~ ^yes$ ]]; then
    echo "Deployment cancelled"
    exit 1
fi

# Build AAB
echo "🔨 Building release bundle..."
./gradlew bundleRelease

AAB_PATH="app/build/outputs/bundle/release/app-release.aab"

# Upload to Play Console (requires Google Play Developer API)
echo "📤 Uploading to Play Console..."
# This would use Google Play Developer API or manual upload

# Create GitHub release
echo "📝 Creating GitHub release..."
gh release create "v$VERSION" \
    --title "VoiceOS v$VERSION" \
    --notes-file CHANGELOG.md \
    "$AAB_PATH"

# Tag version
echo "🏷️ Creating git tag..."
git tag -a "v$VERSION" -m "Release version $VERSION"
git push origin "v$VERSION"

echo "✅ Deployment complete!"
```

### 35.11.3 Rollback Script

**rollback.sh:**

```bash
#!/bin/bash
# VoiceOS Rollback Script

set -e

echo "⏮️ VoiceOS Rollback Script"
echo "========================="

# List recent releases
echo "Recent releases:"
git tag -l "v*" | tail -5

# Get target version
read -p "Enter version to rollback to (e.g., 2.5.3): " TARGET_VERSION

if [ -z "$TARGET_VERSION" ]; then
    echo "Error: Version required"
    exit 1
fi

# Confirm rollback
read -p "Rollback to version $TARGET_VERSION? (yes/no) " -n 3 -r
echo
if [[ ! $REPLY =~ ^yes$ ]]; then
    echo "Rollback cancelled"
    exit 1
fi

# Checkout target version
echo "📥 Checking out version $TARGET_VERSION..."
git checkout "v$TARGET_VERSION"

# Build previous version
echo "🔨 Building version $TARGET_VERSION..."
./gradlew bundleRelease

# Upload to Play Console
echo "📤 Uploading to Play Console..."
# Upload AAB manually or via API

# Revert to main
git checkout main

echo "✅ Rollback complete!"
echo "⚠️  Remember to monitor crash reports and user feedback"
```

---

## Summary

This chapter covered VOS4's comprehensive deployment process:

1. **Deployment Overview**: Structured pipeline from development to production
2. **APK Generation**: Debug, release, and bundle builds
3. **Signing & Release**: Keystore management, signing configuration
4. **Vivoka Model Deployment**: External pre-deployment, on-demand download, multi-location fallback
5. **Version Management**: Semantic versioning, version codes
6. **Update Strategy**: OTA updates, migrations, backwards compatibility
7. **Distribution Channels**: Play Store, F-Droid, direct distribution
8. **Beta Testing**: Internal, closed, and open testing tracks
9. **Monitoring**: Crash reporting, performance monitoring, analytics
10. **Rollback Procedures**: Play Store rollback, data recovery
11. **Deployment Scripts**: Automated build, deploy, and rollback scripts

**Key Takeaways:**

- **Version Strategy**: SemVer 3.0.0, version code 3000000
- **APK Size**: ~50MB (ARM-only, minified, shrunk, without models)
- **Vivoka Models**: External pre-deployment for dev, on-demand download for production
- **Multi-Location Fallback**: Internal → External App → Shared Hidden → Download
- **Distribution**: Multi-channel (Play Store, F-Droid, direct)
- **Monitoring**: Firebase Crashlytics + Analytics
- **Beta Testing**: Internal → Closed → Open → Production
- **Rollback**: Fast rollback procedures for emergencies
- **Automation**: Scripts for build, deploy, rollback
- **Security**: Signed releases, keystore management
- **Updates**: OTA updates with forced/recommended options
- **Analytics**: Track usage, crashes, performance

**Deployment Checklist:**
- ✅ All tests passing
- ✅ Version bumped
- ✅ CHANGELOG updated
- ✅ APK signed
- ✅ Beta tested
- ✅ Monitoring configured
- ✅ Rollback plan ready
- ✅ Release notes written

**End of VOS4 Developer Manual**

---

**Document Information:**
- **File**: `/Volumes/M-Drive/Coding/Warp/vos4/docs/developer-manual/35-Deployment.md`
- **Version**: 4.0
- **Last Updated**: 2025-11-02
- **Part of**: VOS4 Developer Manual (Chapter 35 of 35 - FINAL CHAPTER)

**Previous Chapter:** [Chapter 34: Build System](34-Build-System.md)
**Return to:** [Table of Contents](00-Table-of-Contents.md)
