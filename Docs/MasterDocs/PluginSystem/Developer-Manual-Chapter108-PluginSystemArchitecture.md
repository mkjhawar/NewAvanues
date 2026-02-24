# Developer Manual Chapter 108: PluginSystem Architecture

## Overview

**PluginSystem** is a comprehensive, production-grade KMP plugin framework that enables dynamic extension of the NewAvanues platform across handlers, AI/speech services, themes, overlays, and wake words.

### Key Statistics

| Metric | Value |
|--------|-------|
| **Total Kotlin Files** | 89 |
| **Source Sets** | commonMain + androidMain + iosMain + jvmMain |
| **Package Root** | `com.augmentalis.magiccode.plugins` |
| **Plugin Type Contracts** | 12 (VoiceOS, Speech, AI/LLM) |
| **Builtin Plugins** | 7 (Handler, Gesture, Navigation, UIInteraction, AppLauncher, SystemCommand, TextInput) |
| **Discovery Methods** | 4 (FileSystem, Remote, Builtin, Composite) |
| **Manifest Formats** | YAML + AVU DSL (bidirectional) |
| **Security Layers** | 3 (Signature Verification, Permission Storage, Sandbox) |

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│           PluginSystem Framework (KMP)                  │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │  UniversalPlugin Interface (Root Contract)       │  │
│  │  + 12 Plugin Type Contracts                      │  │
│  └──────────────────────────────────────────────────┘  │
│                          ▲                              │
│                          │ implements                   │
│                          │                              │
│  ┌──────────────────────────────────────────────────┐  │
│  │  BasePlugin SDK (boilerplate reduction)          │  │
│  │  + manifest parsing + lifecycle callbacks        │  │
│  └──────────────────────────────────────────────────┘  │
│                          ▲                              │
│         ┌────────────────┼────────────────┐            │
│         │                │                │            │
│   ┌───────────┐    ┌──────────┐   ┌────────────┐     │
│   │ Handler   │    │Speech    │   │Theme/LLM   │     │
│   │Plugins    │    │Plugins   │   │Plugins     │     │
│   └───────────┘    └──────────┘   └────────────┘     │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Lifecycle & Dispatch                            │  │
│  │  • Discovery → Manifest Parsing → Validation    │  │
│  │  • Requirements Check → Conflict Detection      │  │
│  │  • Namespace Creation → Class Loading           │  │
│  │  • Registration → State Management              │  │
│  └──────────────────────────────────────────────────┘  │
│                          ▲                              │
│         ┌────────────────┼────────────────┐            │
│         │                │                │            │
│  ┌──────────────┐ ┌────────────┐ ┌─────────────┐    │
│  │Security      │ │Asset       │ │Event Bus    │    │
│  │Framework     │ │System      │ │(local+gRPC) │    │
│  └──────────────┘ └────────────┘ └─────────────┘    │
│                                                         │
│  Platform: Android | iOS | Desktop (JVM) | Web        │
└─────────────────────────────────────────────────────────┘
```

---

## Section 1: Plugin Lifecycle Management

The plugin system manages 8 distinct phases in the plugin initialization and registration flow:

### 1.1 Phase Overview

| Phase | Step | Handler | Output |
|-------|------|---------|--------|
| **1** | Discovery | `*PluginDiscovery` | Plugin file list (filesystem/remote/builtin) |
| **2** | Manifest Parsing | `PluginManifestParser` | `PluginMetadata` object (YAML/AVU) |
| **3** | Validation | `ManifestValidator` | Syntax + schema errors (if any) |
| **4** | Requirements Check | `RequirementsValidator` | Missing dependencies (if any) |
| **5** | Conflict Detection | `ConflictDetector` | ID/namespace/permission conflicts |
| **6** | Namespace Creation | `PluginNamespaceManager` | Isolated class loader + file directory |
| **7** | Directory Validation | `PluginDirectoryValidator` | Plugin directory exists + readable |
| **8** | Class Loading & Registration | `PluginClassLoader`, `PluginRegistry` | Plugin instance + registered handlers |

### 1.2 State Machine

Plugin states flow as follows:

```
UNINITIALIZED
    ↓
INITIALIZING (phases 1-7 running)
    ↓
ACTIVE (phase 8 complete, plugin functional)
    ├── → PAUSED (pause() called)
    │       ↓
    │    ACTIVE (resume() called)
    │
    ├── → STOPPED (stop() called)
    │       ↓
    │    (terminal state)
    │
    └── → ERROR (exception during any phase)
             ↓
          (terminal state, manual intervention required)
```

### 1.3 Key Classes

```kotlin
// Root interface
interface UniversalPlugin {
    fun getId(): String
    fun getVersion(): String
    fun onEnable()
    fun onDisable()
    fun onPause()
    fun onResume()
}

// Lifecycle manager
class PluginLifecycleManager {
    suspend fun initialize(metadata: PluginMetadata): PluginState
    suspend fun pause(pluginId: String)
    suspend fun resume(pluginId: String)
    suspend fun stop(pluginId: String)
    fun getState(pluginId: String): PluginState
}
```

---

## Section 2: Security Model (3 Layers)

### 2.1 Layer 1: Signature Verification

All plugins must be cryptographically signed before loading. The system supports RSA and ECDSA signatures.

| Property | Value |
|----------|-------|
| **Algorithms** | RSA-2048, RSA-4096, ECDSA P-256, ECDSA P-384 |
| **Signature File Format** | `.sig` (binary, 256-512 bytes depending on algo) |
| **TrustStore** | App-bundled public keys (PEM format) |
| **Verification Time (P95)** | < 50ms per plugin |
| **Revocation** | TrustStore updates via app version increment |

```kotlin
interface SignatureVerifier {
    suspend fun verify(
        pluginFile: File,
        signatureFile: File,
        trustStore: TrustStore
    ): Boolean
}

// Implementation: RSA + ECDSA support per platform
// androidMain: Android Security provider
// iosMain: Security.framework
// jvmMain: Java crypto APIs
```

### 2.2 Layer 2: Permission Storage

Granted permissions are encrypted and stored securely. The system uses AES-256-GCM with platform-specific key derivation.

| Property | Value |
|----------|-------|
| **Encryption Algorithm** | AES-256-GCM |
| **Integrity** | GCM authentication tag |
| **Platform Key Derivation** | Android Keystore (encrypted), iOS Keychain (secure enclave), JVM (PBKDF2) |
| **Revocation TTL** | Immediate (no caching) |
| **Access Speed (P95)** | < 5ms per lookup |

```kotlin
interface PermissionStorage {
    suspend fun grantPermission(pluginId: String, permission: String)
    suspend fun revokePermission(pluginId: String, permission: String)
    suspend fun hasPermission(pluginId: String, permission: String): Boolean
    suspend fun listPermissions(pluginId: String): List<String>
}

// Implementations per platform:
// androidMain: EncryptedSharedPreferences + Keystore
// iosMain: Keychain + UserDefaults
// jvmMain: Java crypto + file encryption
```

### 2.3 Layer 3: PluginSandbox

Plugins run in isolated namespaces with restricted file access and classpath isolation.

```kotlin
class PluginSandbox {
    // Namespace isolation
    fun createIsolatedClassLoader(
        pluginFile: File,
        manifest: PluginMetadata
    ): PluginClassLoader

    // File access constraints
    fun allowedDirectories(pluginId: String): List<File>

    // Traversal prevention (blocks ../ paths)
    fun sanitizePath(path: String): String

    // Audit logging
    fun logSecurityEvent(event: SecurityEvent)
}

// Supporting classes
class SecurityAuditLogger {
    fun logSignatureVerification(pluginId: String, result: Boolean)
    fun logPermissionCheck(pluginId: String, permission: String, granted: Boolean)
    fun logSandboxViolation(pluginId: String, violation: String)
}

class PermissionEscalationDetector {
    fun detectUnusual(pluginId: String, request: PermissionRequest): Boolean
}
```

---

## Section 3: Asset System

### 3.1 URI Scheme & Resolution Pipeline

Assets are referenced via `plugin://plugin-id/category/filename` URIs. Resolution follows a 10-step pipeline:

```
INPUT: plugin://my-handler/images/logo.png
  ↓
[Step 1] Parse URI → {pluginId: "my-handler", category: "images", file: "logo.png"}
  ↓
[Step 2] Validate plugin exists & is ACTIVE
  ↓
[Step 3] Check AssetCache (LRU, 100MB default)
  ↓ (cache hit)
Return cached bytes
  ↓
[Step 4] Lookup manifest AssetManifest.images["logo.png"]
  ↓
[Step 5] Resolve plugin file location
  ↓
[Step 6] Load file from plugin directory
  ↓
[Step 7] Verify checksum (SHA-256)
  ↓
[Step 8] Deserialize AssetMetadata
  ↓
[Step 9] Cache result (LRU eviction if needed)
  ↓
[Step 10] Return asset bytes
```

### 3.2 Supported Categories

| Category | Use Case | Example |
|----------|----------|---------|
| `images` | UI icons, backgrounds | `plugin://handler/images/icon.png` |
| `fonts` | Custom typefaces | `plugin://theme/fonts/OpenSans.ttf` |
| `icons` | Vector drawables | `plugin://handler/icons/play.svg` |
| `themes` | Theme YAML files | `plugin://mytheme/themes/dark.yaml` |
| `custom` | Plugin-specific data | `plugin://nlp/custom/model.bin` |

### 3.3 Key Classes

```kotlin
class AssetResolver {
    suspend fun resolve(uri: String): AssetData
    suspend fun resolveStream(uri: String): InputStream
}

class AssetCache {
    fun get(key: String): AssetData?
    fun put(key: String, data: AssetData)
    fun evict(key: String)
}

data class AssetMetadata(
    val filename: String,
    val mimeType: String,
    val size: Long,
    val checksum: String, // SHA-256
    val lastModified: Long
)

class FallbackAssetProvider {
    // Graceful degradation when asset missing
    fun resolveFallback(category: String): AssetData
}
```

---

## Section 4: Theme Support

### 4.1 Theme YAML Schema

Plugins can define custom themes in YAML format. The system validates and hot-reloads themes without restart.

```yaml
# plugin_root/themes/dark.yaml
version: "1.0"
name: "Dark Mode"
description: "High-contrast dark theme"

colors:
  primary: "#BB86FC"
  secondary: "#03DAC6"
  tertiary: "#FB8500"
  surface: "#121212"
  background: "#000000"
  error: "#CF6679"

typography:
  headlineFont: "plugin://theme/fonts/Roboto-Bold.ttf"
  bodyFont: "plugin://theme/fonts/Roboto-Regular.ttf"
  displaySize: 28
  headlineSize: 24
  bodySize: 14

customFonts:
  - name: "CustomSerif"
    path: "plugin://theme/fonts/CustomSerif.ttf"
    weight: 400
    style: "normal"

extend:
  borderRadius: 12
  elevation: 8
```

### 4.2 Theme Manager

```kotlin
class ThemeManager {
    suspend fun parseTheme(themeFile: File): ThemeDefinition
    suspend fun validateTheme(theme: ThemeDefinition): List<ValidationError>
    fun applyTheme(theme: ThemeDefinition)
    fun reloadTheme(pluginId: String, themeName: String)
    fun listAvailableThemes(pluginId: String): List<ThemeDefinition>
}

data class ThemeDefinition(
    val version: String,
    val name: String,
    val colors: ColorPalette,
    val typography: TypographyDefinition,
    val customFonts: List<FontDefinition>
)
```

### 4.3 Platform-Specific Font Loading

```kotlin
expect class FontLoader {
    suspend fun loadFont(
        path: String,
        weight: Int,
        style: String
    ): PlatformFont
}

// androidMain: AssetManager + Typeface
// iosMain: UIFont + CTFont
// jvmMain: java.awt.Font
```

---

## Section 5: AVU DSL Integration

### 5.1 Protocol Codes

Plugins can use AVU DSL format (pipe-delimited, percent-encoded) for manifests. The system supports bidirectional YAML ↔ AVU conversion.

| Code | Meaning | Scope |
|------|---------|-------|
| `PLG` | Plugin declaration | Root |
| `DSC` | Description | Plugin |
| `AUT` | Author/maintainer | Plugin |
| `PCP` | Capabilities list | Plugin |
| `MOD` | Module dependency | Dependencies |
| `DEP` | Version constraint | Dependencies |
| `PRM` | Permission request | Permissions |
| `PLT` | Platform target | Target |
| `AST` | Asset definition | Assets |
| `CFG` | Configuration entry | Config |
| `KEY` | API key placeholder | Config |
| `HKS` | Hotkey binding | Config |

### 5.2 AVU Format Example

```
PLG:my-handler|v1.0.0|com.augmentalis.myplugin
DSC:Custom voice handler for media commands
AUT:John Smith
PCP:HANDLER|SYNONYM_PROVIDER|OVERLAY
MOD:VoiceOSCore|v2.1.0
MOD:HTTPAvanue|>=v2.0.0,<v3.0.0
PRM:RECORD_AUDIO|INTERNET|READ_CLIPBOARD
PLT:android|ios|desktop
AST:images/icon.png|media-icon.png|1024
CFG:API_KEY|sk-|%25encoded%25value
HKS:alt+v|play_pause
```

### 5.3 Manifest Parser (Bidirectional)

```kotlin
class AvuManifestParser {
    // Parse AVU text → PluginMetadata
    suspend fun parseAvu(text: String): PluginMetadata

    // Parse YAML → PluginMetadata
    suspend fun parseYaml(yaml: String): PluginMetadata

    // Serialize PluginMetadata → AVU text
    fun toAvu(metadata: PluginMetadata): String

    // Serialize PluginMetadata → YAML
    fun toYaml(metadata: PluginMetadata): String
}

// .avp text plugin format (NOT APK/JAR)
// Stored as plain text manifest + plugin code in isolated directory
```

---

## Section 6: Universal Plugin Contracts

### 6.1 Root Interface

```kotlin
interface UniversalPlugin {
    fun getId(): String
    fun getVersion(): String
    fun getAuthor(): String
    fun getDescription(): String
    fun getRequiredPermissions(): List<String>
    fun getDependencies(): List<PluginDependency>

    fun onEnable()
    fun onDisable()
    fun onPause()
    fun onResume()
}
```

### 6.2 VoiceOS Plugin Types (5 contracts)

| Contract | Purpose | Key Methods |
|----------|---------|-------------|
| **HandlerPlugin** | Custom voice command handlers | `canHandle(ActionCategory): Boolean`, `handle(StaticCommand): HandlerResult` |
| **SynonymProviderPlugin** | Extended voice command phrases | `getSynonyms(commandId: String): List<String>` |
| **OverlayPlugin** | Custom screen overlays | `createOverlay(): OverlayComposable`, `onElementDetected(Element)` |
| **ThemeProviderPlugin** | Custom UI themes | `getTheme(id: String): ThemeDefinition`, `listThemes(): List<String>` |
| **CommandPersistencePlugin** | Custom command storage | `save(command: StaticCommand)`, `load(commandId: String): StaticCommand` |

### 6.3 Speech & AI Plugin Types (7 contracts)

| Contract | Purpose | Key Methods |
|----------|---------|-------------|
| **SpeechEnginePlugin** | Custom speech recognition | `recognize(audio: ByteArray): String` |
| **TTSPlugin** | Custom text-to-speech | `synthesize(text: String, locale: String): AudioData` |
| **WakeWordPlugin** | Custom wake word detection | `detect(audio: ByteArray): Boolean` |
| **NLUPlugin** | Natural language understanding | `parse(text: String): Intent` |
| **LLMPlugin** | Custom LLM provider | `complete(prompt: String): String` |
| **RAGPlugin** | Retrieval-augmented generation | `retrieve(query: String): List<Document>` |
| **EmbeddingPlugin** | Text embedding service | `embed(text: String): Vector` |

### 6.4 BasePlugin SDK (Boilerplate Reduction)

```kotlin
abstract class BasePlugin : UniversalPlugin {
    protected lateinit var metadata: PluginMetadata
    protected lateinit var context: PluginContext
    protected lateinit var sandbox: PluginSandbox

    final override fun getId() = metadata.id
    final override fun getVersion() = metadata.version

    // Subclasses override:
    protected abstract fun initialize()
    protected abstract fun shutdown()

    // Base handles lifecycle:
    final override fun onEnable() {
        initialize()
        onAfterEnable()
    }

    protected open fun onAfterEnable() {}
    protected open fun onBeforePause() {}
    // etc.
}

// Usage:
class MyHandlerPlugin : BasePlugin(), HandlerPlugin {
    override fun initialize() {
        log("Plugin enabled: ${metadata.id}")
    }

    override fun canHandle(category: ActionCategory) =
        category == ActionCategory.MEDIA

    override suspend fun handle(cmd: StaticCommand) =
        HandlerResult.SUCCESS
}
```

---

## Section 7: Discovery & Installation

### 7.1 Discovery Methods

The system supports 4 independent discovery strategies, composable via `CompositePluginDiscovery`:

| Method | Source | Timing | Use Case |
|--------|--------|--------|----------|
| **FileSystem** | `/plugins/` directory (app install dir) | Startup + hot-reload | Local development, bundled plugins |
| **Remote** | HTTP/gRPC endpoint (configurable) | Periodic sync (1h default) | Marketplace sync, cloud plugins |
| **Builtin** | Hardcoded 7 plugins (Java classpath) | Startup | System plugins, fallback handlers |
| **Composite** | Combines above (priority order) | Per-source schedule | Production: Builtin → FileSystem → Remote |

### 7.2 Installation Flow

```kotlin
class PluginInstaller {
    suspend fun install(
        source: PluginSource,  // URL or File
        opts: InstallOptions
    ): InstallResult {
        // 1. Download (if remote)
        // 2. Verify signature
        // 3. Parse manifest
        // 4. Validate
        // 5. Check requirements
        // 6. Create namespace
        // 7. Extract to isolated directory
        // 8. Load classes
        // 9. Register handlers
        // 10. Persist metadata
    }
}

data class InstallOptions(
    val autoStart: Boolean = true,
    val allowPermissions: List<String> = emptyList(),
    val trustStore: TrustStore? = null
)
```

### 7.3 Builtin Plugins

7 system plugins are always available (cannot be disabled):

| ID | Type | Purpose | Example Commands |
|----|----|---------|-----------|
| `handler-system` | HandlerPlugin | System commands (reboot, shutdown) | "restart device", "shutdown" |
| `gesture-tap` | HandlerPlugin | Touch gesture handlers | "double tap", "long press" |
| `nav-browser` | HandlerPlugin | Browser navigation | "go back", "refresh page" |
| `ui-interaction` | HandlerPlugin | Generic UI clicks | "click", "swipe", "scroll" |
| `app-launcher` | HandlerPlugin | App launching | "open gmail", "launch maps" |
| `cmd-text-input` | HandlerPlugin | Text input commands | "type", "paste", "select all" |
| `rag-local` | RAGPlugin | Local document search (SQLite backend) | Used by LLM context retrieval |

---

## Section 8: Event System

### 8.1 Local Event Bus

```kotlin
class PluginEventBus {
    suspend inline fun <reified T : PluginEvent> emit(event: T)

    suspend inline fun <reified T : PluginEvent> subscribe(
        noinline handler: suspend (T) -> Unit
    ): Subscription
}

sealed class PluginEvent {
    data class PluginLoaded(val pluginId: String) : PluginEvent()
    data class PluginUnloaded(val pluginId: String) : PluginEvent()
    data class PermissionRequested(val pluginId: String, val permission: String) : PluginEvent()
    data class ThemeChanged(val themeName: String) : PluginEvent()
    data class HandlerExecuted(val handlerName: String, val success: Boolean) : PluginEvent()
}
```

### 8.2 Remote Event Bus (gRPC)

For distributed plugin systems, events can be published to remote subscribers via gRPC.

```kotlin
class GrpcPluginEventBus(
    private val channel: ManagedChannel
) : PluginEventBus {
    override suspend fun <T : PluginEvent> emit(event: T) {
        val stub = PluginEventServiceGrpc.newStub(channel)
        stub.publishEvent(event.toProto())
    }
}
```

---

## Section 9: Platform Implementations

### 9.1 Cross-Platform Architecture

| Component | commonMain | androidMain | iosMain | jvmMain |
|-----------|-----------|-----------|---------|---------|
| **SignatureVerifier** | interface | BouncyCastleImpl | SecurityFrameworkImpl | JavaCryptoImpl |
| **PermissionStorage** | interface | EncryptedSharedPrefImpl | KeychainImpl | FileEncryptionImpl |
| **FileIO** | interface | AndroidFileSystemImpl | IOSFileSystemImpl | JavaFileSystemImpl |
| **PluginClassLoader** | interface | DexClassLoaderImpl | DynamicLibLoaderImpl | URLClassLoaderImpl |
| **ZipExtractor** | interface | AndroidZipImpl | FoundationZipImpl | JavaZipImpl |
| **ChecksumCalculator** | interface | CryptoImpl | CommonCryptoImpl | MessageDigestImpl |
| **FontLoader** | interface | TypefaceLoaderImpl | UIFontLoaderImpl | AwtFontLoaderImpl |

### 9.2 Expect/Actual Pattern (CommonMain Interface)

```kotlin
// commonMain/kotlin/com/augmentalis/plugins/security/SignatureVerifier.kt
expect interface SignatureVerifier {
    suspend fun verify(
        pluginFile: File,
        signatureFile: File,
        trustStore: TrustStore
    ): Boolean
}

// androidMain/kotlin/...
actual class AndroidSignatureVerifier : SignatureVerifier {
    actual override suspend fun verify(...) {
        // Android Security provider + RSA/ECDSA
    }
}

// iosMain/kotlin/...
actual class IOSSignatureVerifier : SignatureVerifier {
    actual override suspend fun verify(...) {
        // Security.framework + CommonCrypto
    }
}

// jvmMain/kotlin/...
actual class JvmSignatureVerifier : SignatureVerifier {
    actual override suspend fun verify(...) {
        // java.security + Bouncy Castle fallback
    }
}
```

### 9.3 Initialization (Per Platform)

```kotlin
// commonMain
class PluginSystemFactory {
    companion object {
        fun create(context: PlatformContext): PluginSystem {
            return PluginSystemImpl(
                signatureVerifier = createSignatureVerifier(),
                permissionStorage = createPermissionStorage(),
                fileIO = createFileIO(),
                classLoader = createClassLoader()
            )
        }
    }
}

// androidMain
actual fun createPermissionStorage(): PermissionStorage =
    AndroidPermissionStorage(context.getSharedPreferences(...))

// iosMain
actual fun createPermissionStorage(): PermissionStorage =
    IOSPermissionStorage(KeychainManager())

// jvmMain
actual fun createPermissionStorage(): PermissionStorage =
    JvmPermissionStorage(encryptedPropertiesFile)
```

---

## Section 10: Marketplace Integration

### 10.1 Marketplace API

```kotlin
interface MarketplaceApi {
    suspend fun search(query: String): List<PluginListing>
    suspend fun fetchListing(pluginId: String): PluginListing
    suspend fun fetchVersions(pluginId: String): List<VersionInfo>
    suspend fun checkUpdates(): List<UpdateNotification>
    suspend fun submitPlugin(spec: PluginPackageSpec): SubmissionResult
}

data class PluginListing(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val rating: Float,
    val downloads: Long,
    val downloadUrl: String,
    val signatureUrl: String,
    val tags: List<String>,
    val requirementsTier: String // "free", "premium", "enterprise"
)

data class UpdateNotification(
    val pluginId: String,
    val currentVersion: String,
    val latestVersion: String,
    val changelog: String,
    val securityCritical: Boolean
)
```

### 10.2 Plugin Package Spec (Submission)

```kotlin
data class PluginPackageSpec(
    val jarFile: File,
    val manifestFile: File,
    val signatureFile: File,
    val assets: Map<String, File>,
    val metadata: PluginMetadata,
    val screenshots: List<File>?,
    val privacyPolicy: String?
)

class SubmissionValidator {
    suspend fun validate(spec: PluginPackageSpec): SubmissionResult {
        // 1. Verify JAR integrity
        // 2. Validate manifest schema
        // 3. Check signature algorithms (RSA-2048+, ECDSA P-256+)
        // 4. Scan for malware (basic analysis)
        // 5. Verify permissions match manifest claims
        // 6. Check version semantics (semver)
    }
}

data class SubmissionResult(
    val approved: Boolean,
    val reviewId: String,
    val errors: List<String>,
    val warnings: List<String>
)
```

### 10.3 Marketplace Cache

```kotlin
class MarketplaceCache {
    suspend fun searchLocal(query: String): List<PluginListing>
    suspend fun syncRemote(api: MarketplaceApi): Long // returns sync timestamp
    suspend fun getCachedListing(pluginId: String): PluginListing?
    fun invalidateCache()
}
```

---

## Section 11: Build Configuration

### 11.1 Gradle Dependencies

```gradle
// Modules/PluginSystem/build.gradle.kts

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":Modules:VoiceOSCore"))
            implementation(project(":Modules:Rpc"))
            implementation(project(":Modules:Database"))
            implementation(project(":Modules:AVU"))

            implementation("com.charleskorn.kaml:kaml:0.55.0") // YAML parsing
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

            // Semver for version constraints
            implementation("org.semver4j:semver4j:5.1.0")
        }

        androidMain.dependencies {
            implementation("com.google.android.gms:play-services-base:18.0.0")
            implementation("androidx.security:security-crypto:1.1.0-alpha06")
        }

        iosMain.dependencies {
            // Native framework imports via Cocoapods
        }

        jvmMain.dependencies {
            implementation("org.bouncycastle:bcprov-jdk15on:1.71")
            implementation("org.bouncycastle:bcpkix-jdk15on:1.71")
        }
    }
}
```

### 11.2 Known Issues (Pre-Existing)

| Issue | Scope | Impact | Workaround |
|-------|-------|--------|-----------|
| FileIO/PluginClassLoader expect/actual API mismatch | All platforms | Module doesn't compile currently | Align function signatures in androidMain/iosMain/jvmMain |
| Missing `kotlinx.datetime` in POM | Publishing | Marketplace submissions fail | Add explicit dependency in plugin consumer apps |
| Signature verification timeout (rare) | Remote plugins | Long initialization | Implement per-platform caching + timeout config |

### 11.3 Runtime Gradle Properties (Optional)

```properties
# gradle.properties
plugin.security.strict=true           # Enforce all 3 security layers
plugin.cache.size.mb=256              # Asset cache size
plugin.marketplace.sync.hours=1       # Remote sync interval
plugin.classloader.parent=boot         # Parent classloader strategy
```

---

## Section 12: Key Files Reference

### 12.1 Core Interfaces & Classes

| File | Location | Purpose |
|------|----------|---------|
| `UniversalPlugin.kt` | commonMain | Root plugin interface |
| `PluginMetadata.kt` | commonMain | Manifest data model |
| `PluginLifecycleManager.kt` | commonMain | Lifecycle orchestration |
| `SignatureVerifier.kt` | commonMain (interface) | Signature verification contract |
| `PluginClassLoader.kt` | commonMain (interface) | Dynamic class loading contract |
| `AssetResolver.kt` | commonMain | Asset resolution + caching |
| `ThemeManager.kt` | commonMain | Theme parsing + hot-reload |
| `AvuManifestParser.kt` | commonMain | YAML ↔ AVU bidirectional conversion |
| `PluginEventBus.kt` | commonMain | Local event publishing |
| `GrpcPluginEventBus.kt` | commonMain | Remote event publishing |

### 12.2 Plugin Type Contracts

| File | Location | Contract |
|------|----------|----------|
| `HandlerPlugin.kt` | commonMain | Voice command handlers |
| `SynonymProviderPlugin.kt` | commonMain | Command phrase synonyms |
| `OverlayPlugin.kt` | commonMain | UI overlays |
| `ThemeProviderPlugin.kt` | commonMain | Theme definitions |
| `SpeechEnginePlugin.kt` | commonMain | Speech recognition |
| `TTSPlugin.kt` | commonMain | Text-to-speech |
| `WakeWordPlugin.kt` | commonMain | Wake word detection |
| `LLMPlugin.kt` | commonMain | LLM providers |
| `RAGPlugin.kt` | commonMain | Document retrieval |
| `NLUPlugin.kt` | commonMain | Natural language understanding |
| `EmbeddingPlugin.kt` | commonMain | Text embeddings |
| `CommandPersistencePlugin.kt` | commonMain | Custom command storage |

### 12.3 Platform-Specific Implementations

| File | Location | Purpose |
|------|----------|---------|
| `AndroidSignatureVerifier.kt` | androidMain | RSA/ECDSA via Android Security provider |
| `AndroidPermissionStorage.kt` | androidMain | EncryptedSharedPreferences + Keystore |
| `IOSSignatureVerifier.kt` | iosMain | RSA/ECDSA via Security.framework |
| `IOSPermissionStorage.kt` | iosMain | Keychain + UserDefaults |
| `JvmSignatureVerifier.kt` | jvmMain | Java crypto APIs + Bouncy Castle |
| `JvmPermissionStorage.kt` | jvmMain | AES-256-GCM file encryption |

### 12.4 Utilities

| File | Location | Purpose |
|------|----------|---------|
| `ManifestValidator.kt` | commonMain | YAML/AVU schema validation |
| `ConflictDetector.kt` | commonMain | ID/namespace/permission conflict detection |
| `SecurityAuditLogger.kt` | commonMain | Security event logging |
| `PermissionEscalationDetector.kt` | commonMain | Unusual permission pattern detection |
| `PluginInstaller.kt` | commonMain | Plugin installation workflow |
| `MarketplaceApi.kt` | commonMain | Marketplace API client |
| `PluginRegistry.kt` | commonMain | Plugin instance registry (singleton) |

---

## Quick Reference: Adding a Custom Plugin

### Step 1: Create Plugin Class

```kotlin
import com.augmentalis.magiccode.plugins.*

class MyCustomHandler : BasePlugin(), HandlerPlugin {
    override fun initialize() {
        log("MyCustomHandler initialized")
    }

    override fun canHandle(category: ActionCategory) =
        category in listOf(ActionCategory.MEDIA, ActionCategory.DEVICE)

    override suspend fun handle(cmd: StaticCommand): HandlerResult {
        return when (cmd.phrase) {
            "play next" -> doPlayNext()
            "previous track" -> doPreviousTrack()
            else -> HandlerResult.UNHANDLED
        }
    }

    private suspend fun doPlayNext() = HandlerResult.SUCCESS
    private suspend fun doPreviousTrack() = HandlerResult.SUCCESS
}
```

### Step 2: Create Plugin Manifest (YAML)

```yaml
# plugin_manifest.yaml
id: com.example.my-handler
version: 1.0.0
name: "My Custom Handler"
author: "John Doe"
description: "Custom media control handler"
mainClass: "com.example.MyCustomHandler"

permissions:
  - SEND_BROADCAST
  - READ_MEDIA_AUDIO

dependencies:
  - id: VoiceOSCore
    version: ">=2.1.0"

platforms:
  - android
  - ios
```

### Step 3: Package & Sign

```bash
# Create plugin JAR
kotlinc src/ -d my-handler.jar

# Generate signature (RSA-2048)
openssl dgst -sha256 -sign private.pem -out my-handler.sig my-handler.jar

# Install locally
adb push my-handler.jar /data/data/com.augmentalis.voiceos/plugins/
adb push my-handler.sig /data/data/com.augmentalis.voiceos/plugins/
```

### Step 4: Verify Installation

```kotlin
// In app code
val system = PluginSystemFactory.create(context)
val discovery = FileSystemPluginDiscovery(pluginDir = File("/data/data/.../plugins"))
val plugins = discovery.discover()

// Plugin auto-registers on load
val registry = system.pluginRegistry
val handlers = registry.getHandlers()  // includes MyCustomHandler
```

---

## Kotlin 2.1.0 K2 Compiler Compatibility

The PluginSystem uses KMP expect/actual classes extensively for platform abstraction. Kotlin 2.1.0's K2 compiler introduced stricter requirements that affect this module:

### Explicit Constructor Declarations

K2 no longer synthesizes implicit default constructors for `expect class` declarations. All expect classes that are instantiated from common code must have explicit constructor declarations:

```kotlin
// Before (Kotlin <2.1.0) — implicit default constructor
expect class FileIO {
    fun readFileAsString(path: String): String
}

// After (Kotlin 2.1.0+) — explicit constructor required
expect class FileIO() {
    fun readFileAsString(path: String): String
}
```

**Affected expect classes** (all updated 2026-02-23):

| Class | Location | Purpose |
|-------|----------|---------|
| `FileIO` | `platform/FileIO.kt` | File system operations |
| `ZipExtractor` | `platform/ZipExtractor.kt` | ZIP archive extraction |
| `SignatureVerifier` | `security/SignatureVerifier.kt` | Cryptographic signature verification |
| `PluginClassLoader` | `platform/PluginClassLoader.kt` | AVU DSL plugin loading |

### Compiler Flag

The module's `build.gradle.kts` includes `-Xexpect-actual-classes` in `compilerOptions.freeCompilerArgs` to enable full expect/actual class support with K2.

---

## Summary

The PluginSystem provides a secure, extensible framework for dynamic plugin loading across Android, iOS, and desktop platforms. Its 3-layer security model (signature verification, permission storage, sandbox isolation), comprehensive lifecycle management, asset system, and multi-format manifest support (YAML + AVU DSL) enable enterprise-grade plugin distribution while maintaining platform safety and user privacy.

For questions, refer to the **PluginSystem** module source at `/Volumes/M-Drive/Coding/NewAvanues/Modules/PluginSystem/`, or consult the **Marketplace** documentation in Chapter 109 (forthcoming).

---

## Expect/Actual Fix Log (260223)

### JVM PermissionStorage — Rewritten

The JVM actual was completely out of sync with the expect declaration:

| Old (Broken) | New (Fixed) |
|-------------|-------------|
| `class JvmPermissionStorage : PermissionStorage` (treated as interface) | `actual class PermissionStorage` (proper actual) |
| `save(state: PluginPermissionState)` | `savePermission(pluginId, permission)` |
| `load(pluginId): PluginPermissionState?` | `hasPermission(pluginId, permission): Boolean` |
| `delete(pluginId)` | `revokePermission(pluginId, permission)` |
| `loadAll(): Map<...>` | `getAllPermissions(pluginId): Set<String>` |
| JSON file storage | `java.util.prefs.Preferences` |

New implementation stores permissions as comma-separated sets under per-plugin Preferences nodes at `/com/augmentalis/magiccode/plugins/permissions/{pluginId}/`.

### JVM + iOS FontLoader — Nested Types Removed

Both platforms re-declared `LoadResult`, `LoadedFont`, and `FontFormat` as nested `actual` types inside `FontLoader`. The expect class references top-level types from commonMain:

- `FontLoadResult` (sealed class) — was re-declared as `FontLoader.LoadResult`
- `LoadedFont` (data class) — was re-declared as `FontLoader.LoadedFont`
- `FontFormat` (enum) — was re-declared as `FontLoader.FontFormat`

Fix: Removed all nested re-declarations, switched to top-level types. Return types updated from `LoadResult` to `FontLoadResult`.

### IosPluginExample.kt — Deleted

Stale example file referencing non-existent `PluginClassLoader.register()` API. The registration pattern changed but the example was never updated.

---

## Android Executor Warning Fixes (260224)

The 5 Android executor files in `androidMain/kotlin/.../executors/` accumulated compiler warnings during the Kotlin 2.1.0 upgrade. All were fixed in commit `130340b5c`.

### Files Changed

| File | Key Fixes |
|------|-----------|
| `LiveDataFlowBridge.kt` | DRY `mainScope` extraction, `@OptIn(FlowPreview::class)` for `debounce()`, `@Suppress("DEPRECATION")` for `observeForever` |
| `AndroidNavigationExecutor.kt` | `GESTURE_TIMEOUT_MS` constant (500ms) replacing magic numbers, `@OptIn(ExperimentalForeignApi::class)` |
| `AndroidSelectionExecutor.kt` | `try-finally` blocks for `AccessibilityNodeInfo.recycle()`, `BOUNDS_MATCH_TOLERANCE` constant |
| `AndroidTextInputExecutor.kt` | PII-safe logging (text length instead of content), `@Suppress("DEPRECATION")` for clipboard APIs |
| `AndroidUIInteractionExecutor.kt` | `BOUNDS_MATCH_TOLERANCE` constant (5px), removed shadowed `findNodeByText`/`findNodeByViewId` extensions |

### Pattern: DRY mainScope in LiveDataFlowBridge

Before:
```kotlin
// Repeated in 3+ places
CoroutineScope(Dispatchers.Main + SupervisorJob())
```

After:
```kotlin
private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
```

### Pattern: try-finally for AccessibilityNodeInfo

`AccessibilityNodeInfo` objects MUST be recycled to prevent memory leaks. The executors now use `try-finally`:

```kotlin
val node = findNode(...)
try {
    // Use node
} finally {
    node.recycle()
}
```

### Pattern: PII-Safe Logging

Text input content is never logged. Instead, log the length:

```kotlin
// BAD: logDebug(TAG, "Typing: $text")
// GOOD:
logDebug(TAG, "Typing text (${text.length} chars)")
```

### Kotlin 2.1.0 Deprecation Suppressions

| Suppression | Reason |
|-------------|--------|
| `@OptIn(FlowPreview::class)` | `debounce()` is still @FlowPreview in kotlinx.coroutines |
| `@Suppress("DEPRECATION")` on `observeForever` | AndroidX lifecycle deprecation, no replacement available for non-lifecycle-aware contexts |
| `@Suppress("DEPRECATION")` on clipboard APIs | `ClipboardManager.setPrimaryClip` deprecation in API 33+, still functional |

---

**Document Info**
Author: Manoj Jhawar
Chapter: 108
Module: PluginSystem
Created: 2026-02-22
Last Updated: 2026-02-24
