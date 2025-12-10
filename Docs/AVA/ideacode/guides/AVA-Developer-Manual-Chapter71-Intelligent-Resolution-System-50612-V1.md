# Developer Manual - Chapter 71: Intelligent App & Platform Resolution

## Overview

AVA's Intelligent Resolution System provides a **zero-config, learn-once** experience. Instead of asking users repetitive questions, AVA:

1. **Auto-detects** installed apps and smart home platforms
2. **Asks once** when multiple options exist
3. **Remembers forever** user preferences
4. **Learns** from behavior patterns

### Design Philosophy

| Principle | Implementation |
|-----------|----------------|
| **Zero Config** | Auto-detect everything possible |
| **Ask Once** | Never repeat the same question |
| **Smart Defaults** | If only one option, use it automatically |
| **Graceful Degradation** | Guide user when setup needed |
| **Privacy First** | All preferences stored locally, encrypted |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              AVA CORE                                   │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │                    AppResolverService                             │ │
│  │  - Scans installed apps by capability (email, music, maps, etc.)  │ │
│  │  - Returns best app based on preferences                          │ │
│  │  - Triggers preference prompt when needed                         │ │
│  └───────────────────────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │                  UserPreferencesRepository                        │ │
│  │  - Stores preferred apps per capability                           │ │
│  │  - Stores smart home credentials (encrypted)                      │ │
│  │  - Tracks usage patterns for smart suggestions                    │ │
│  └───────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ Depends on
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      SMART HOME MODULE                                  │
│                    common/SmartHome/                                    │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │  SmartHomeSetupService                                            │ │
│  │  - Pre-configured platform definitions                            │ │
│  │  - OAuth flow handlers                                            │ │
│  │  - Device discovery                                               │ │
│  └───────────────────────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────────────────────┐ │
│  │  Platform Adapters                                                │ │
│  │  - GoogleHomeAdapter                                              │ │
│  │  - AlexaAdapter                                                   │ │
│  │  - HomeAssistantAdapter                                           │ │
│  │  - MatterAdapter                                                  │ │
│  │  - HomeKitAdapter (iOS)                                           │ │
│  └───────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Module Structure

```
common/
├── core/
│   └── Services/
│       └── src/commonMain/kotlin/.../
│           ├── AppResolverService.kt
│           ├── CapabilityRegistry.kt
│           └── PreferencePromptManager.kt
│   └── Data/
│       └── src/commonMain/kotlin/.../
│           ├── UserPreferencesRepository.kt
│           └── PreferencesSchema.sq
│
└── SmartHome/
    ├── build.gradle.kts
    └── src/
        ├── commonMain/kotlin/.../
        │   ├── SmartHomeSetupService.kt
        │   ├── SmartHomePlatformAdapter.kt
        │   ├── DeviceResolver.kt
        │   ├── SmartHomeDevice.kt
        │   └── adapters/
        │       ├── GoogleHomeAdapter.kt
        │       ├── AlexaAdapter.kt
        │       ├── HomeAssistantAdapter.kt
        │       └── MatterAdapter.kt
        ├── androidMain/kotlin/.../
        │   ├── AndroidSmartHomeSetup.kt
        │   └── adapters/
        │       └── MatterAdapterAndroid.kt
        └── iosMain/kotlin/.../
            ├── IOSSmartHomeSetup.kt
            └── adapters/
                └── HomeKitAdapter.kt
```

---

## AppResolverService

### Purpose

Automatically finds the best app for any capability (email, music, maps, etc.) without user intervention.

### Capability Registry

```kotlin
// CapabilityRegistry.kt
object CapabilityRegistry {

    val capabilities = mapOf(
        // Communication
        "email" to CapabilityDefinition(
            id = "email",
            displayName = "Email",
            androidIntents = listOf(
                IntentSpec(Intent.ACTION_SENDTO, "mailto:"),
                IntentSpec(Intent.ACTION_SEND, "message/rfc822")
            ),
            iosUrlSchemes = listOf("mailto:"),
            knownApps = listOf(
                KnownApp("com.google.android.gm", "Gmail", Platform.ANDROID),
                KnownApp("com.microsoft.office.outlook", "Outlook", Platform.BOTH),
                KnownApp("com.apple.mobilemail", "Apple Mail", Platform.IOS)
            )
        ),

        "sms" to CapabilityDefinition(
            id = "sms",
            displayName = "Text Messages",
            androidIntents = listOf(
                IntentSpec(Intent.ACTION_SENDTO, "sms:"),
                IntentSpec(Intent.ACTION_SENDTO, "smsto:")
            ),
            iosUrlSchemes = listOf("sms:"),
            knownApps = listOf(
                KnownApp("com.google.android.apps.messaging", "Messages", Platform.ANDROID),
                KnownApp("com.whatsapp", "WhatsApp", Platform.BOTH),
                KnownApp("com.facebook.orca", "Messenger", Platform.BOTH)
            )
        ),

        // Media
        "music" to CapabilityDefinition(
            id = "music",
            displayName = "Music Player",
            androidIntents = listOf(
                IntentSpec("android.media.action.MEDIA_PLAY_FROM_SEARCH"),
                IntentSpec(Intent.ACTION_VIEW, "audio/*")
            ),
            iosUrlSchemes = listOf("music:", "spotify:"),
            knownApps = listOf(
                KnownApp("com.spotify.music", "Spotify", Platform.BOTH),
                KnownApp("com.google.android.apps.youtube.music", "YouTube Music", Platform.ANDROID),
                KnownApp("com.apple.Music", "Apple Music", Platform.IOS),
                KnownApp("com.amazon.mp3", "Amazon Music", Platform.BOTH)
            )
        ),

        "video" to CapabilityDefinition(
            id = "video",
            displayName = "Video Player",
            androidIntents = listOf(
                IntentSpec(Intent.ACTION_VIEW, "video/*"),
                IntentSpec(Intent.ACTION_SEARCH, "com.google.android.youtube")
            ),
            iosUrlSchemes = listOf("youtube:", "netflix:"),
            knownApps = listOf(
                KnownApp("com.google.android.youtube", "YouTube", Platform.BOTH),
                KnownApp("com.netflix.mediaclient", "Netflix", Platform.BOTH),
                KnownApp("com.amazon.avod.thirdpartyclient", "Prime Video", Platform.BOTH)
            )
        ),

        // Navigation
        "maps" to CapabilityDefinition(
            id = "maps",
            displayName = "Maps & Navigation",
            androidIntents = listOf(
                IntentSpec(Intent.ACTION_VIEW, "geo:"),
                IntentSpec(Intent.ACTION_VIEW, "google.navigation:")
            ),
            iosUrlSchemes = listOf("maps:", "comgooglemaps:", "waze:"),
            knownApps = listOf(
                KnownApp("com.google.android.apps.maps", "Google Maps", Platform.BOTH),
                KnownApp("com.apple.Maps", "Apple Maps", Platform.IOS),
                KnownApp("com.waze", "Waze", Platform.BOTH)
            )
        ),

        // Productivity
        "calendar" to CapabilityDefinition(
            id = "calendar",
            displayName = "Calendar",
            androidIntents = listOf(
                IntentSpec(Intent.ACTION_INSERT, "vnd.android.cursor.item/event")
            ),
            iosUrlSchemes = listOf("calshow:"),
            knownApps = listOf(
                KnownApp("com.google.android.calendar", "Google Calendar", Platform.BOTH),
                KnownApp("com.apple.mobilecal", "Apple Calendar", Platform.IOS),
                KnownApp("com.microsoft.office.outlook", "Outlook", Platform.BOTH)
            )
        ),

        "notes" to CapabilityDefinition(
            id = "notes",
            displayName = "Notes",
            androidIntents = listOf(
                IntentSpec(Intent.ACTION_SEND, "text/plain")
            ),
            iosUrlSchemes = listOf("mobilenotes:"),
            knownApps = listOf(
                KnownApp("com.google.android.keep", "Google Keep", Platform.BOTH),
                KnownApp("com.apple.mobilenotes", "Apple Notes", Platform.IOS),
                KnownApp("com.evernote", "Evernote", Platform.BOTH),
                KnownApp("com.microsoft.office.onenote", "OneNote", Platform.BOTH)
            )
        ),

        // Browser
        "browser" to CapabilityDefinition(
            id = "browser",
            displayName = "Web Browser",
            androidIntents = listOf(
                IntentSpec(Intent.ACTION_VIEW, "http:"),
                IntentSpec(Intent.ACTION_VIEW, "https:")
            ),
            iosUrlSchemes = listOf("http:", "https:"),
            knownApps = listOf(
                KnownApp("com.android.chrome", "Chrome", Platform.BOTH),
                KnownApp("com.apple.mobilesafari", "Safari", Platform.IOS),
                KnownApp("org.mozilla.firefox", "Firefox", Platform.BOTH),
                KnownApp("com.brave.browser", "Brave", Platform.BOTH)
            )
        ),

        // Ride sharing
        "rideshare" to CapabilityDefinition(
            id = "rideshare",
            displayName = "Ride Sharing",
            androidIntents = listOf(),
            iosUrlSchemes = listOf("uber:", "lyft:"),
            knownApps = listOf(
                KnownApp("com.ubercab", "Uber", Platform.BOTH),
                KnownApp("com.lyft.android", "Lyft", Platform.BOTH)
            )
        ),

        // Food delivery
        "food_delivery" to CapabilityDefinition(
            id = "food_delivery",
            displayName = "Food Delivery",
            androidIntents = listOf(),
            iosUrlSchemes = listOf(),
            knownApps = listOf(
                KnownApp("com.ubercab.eats", "Uber Eats", Platform.BOTH),
                KnownApp("com.dd.doordash", "DoorDash", Platform.BOTH),
                KnownApp("com.grubhub.android", "Grubhub", Platform.BOTH)
            )
        )
    )
}

data class CapabilityDefinition(
    val id: String,
    val displayName: String,
    val androidIntents: List<IntentSpec>,
    val iosUrlSchemes: List<String>,
    val knownApps: List<KnownApp>
)

data class IntentSpec(
    val action: String,
    val dataScheme: String? = null,
    val mimeType: String? = null
)

data class KnownApp(
    val packageName: String,
    val displayName: String,
    val platform: Platform
)
```

### AppResolverService Implementation

```kotlin
// AppResolverService.kt
class AppResolverService(
    private val context: Context,
    private val preferencesRepo: UserPreferencesRepository,
    private val capabilityRegistry: CapabilityRegistry = CapabilityRegistry
) {
    private val packageManager = context.packageManager

    /**
     * Resolves the best app for a capability.
     *
     * Flow:
     * 1. Check if user has a saved preference → use it
     * 2. Scan for installed apps with this capability
     * 3. If only one app → auto-select and save
     * 4. If multiple apps → return MultipleAvailable for UI to prompt
     * 5. If no apps → return NoneAvailable
     */
    suspend fun resolveApp(capability: String): AppResolution {
        val definition = capabilityRegistry.capabilities[capability]
            ?: return AppResolution.UnknownCapability(capability)

        // 1. Check saved preference
        val savedPackage = preferencesRepo.getPreferredApp(capability)
        if (savedPackage != null) {
            if (isAppInstalled(savedPackage)) {
                return AppResolution.Resolved(
                    packageName = savedPackage,
                    appName = getAppName(savedPackage),
                    source = ResolutionSource.USER_PREFERENCE
                )
            } else {
                // App was uninstalled, clear preference
                preferencesRepo.clearPreferredApp(capability)
            }
        }

        // 2. Scan for installed apps
        val installedApps = findInstalledApps(definition)

        return when {
            installedApps.isEmpty() -> {
                AppResolution.NoneAvailable(
                    capability = capability,
                    suggestedApps = definition.knownApps.take(3)
                )
            }
            installedApps.size == 1 -> {
                // Auto-select single app
                val app = installedApps.first()
                preferencesRepo.setPreferredApp(capability, app.packageName)
                AppResolution.Resolved(
                    packageName = app.packageName,
                    appName = app.appName,
                    source = ResolutionSource.AUTO_DETECTED
                )
            }
            else -> {
                AppResolution.MultipleAvailable(
                    capability = capability,
                    apps = installedApps,
                    recommendedIndex = findRecommendedApp(installedApps, definition)
                )
            }
        }
    }

    /**
     * Save user's app preference for a capability.
     * Called after user selects from multiple options.
     */
    suspend fun setPreference(capability: String, packageName: String, rememberChoice: Boolean = true) {
        if (rememberChoice) {
            preferencesRepo.setPreferredApp(capability, packageName)
        }
    }

    /**
     * Get all preferences for settings screen.
     */
    suspend fun getAllPreferences(): Map<String, AppPreference> {
        return capabilityRegistry.capabilities.keys.associateWith { capability ->
            val savedPackage = preferencesRepo.getPreferredApp(capability)
            val installedApps = findInstalledApps(capabilityRegistry.capabilities[capability]!!)

            AppPreference(
                capability = capability,
                displayName = capabilityRegistry.capabilities[capability]!!.displayName,
                selectedApp = savedPackage?.let { pkg ->
                    installedApps.find { it.packageName == pkg }
                },
                availableApps = installedApps,
                canChange = installedApps.size > 1
            )
        }
    }

    private fun findInstalledApps(definition: CapabilityDefinition): List<InstalledApp> {
        val found = mutableSetOf<InstalledApp>()

        // Method 1: Query by intent
        for (intentSpec in definition.androidIntents) {
            val intent = Intent(intentSpec.action).apply {
                intentSpec.dataScheme?.let { data = Uri.parse(it) }
                intentSpec.mimeType?.let { type = it }
            }

            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .forEach { resolveInfo ->
                    found.add(InstalledApp(
                        packageName = resolveInfo.activityInfo.packageName,
                        appName = resolveInfo.loadLabel(packageManager).toString(),
                        icon = resolveInfo.loadIcon(packageManager)
                    ))
                }
        }

        // Method 2: Check known apps directly
        for (knownApp in definition.knownApps) {
            if (knownApp.platform != Platform.IOS && isAppInstalled(knownApp.packageName)) {
                found.add(InstalledApp(
                    packageName = knownApp.packageName,
                    appName = knownApp.displayName,
                    icon = getAppIcon(knownApp.packageName)
                ))
            }
        }

        return found.distinctBy { it.packageName }.sortedBy { it.appName }
    }

    private fun findRecommendedApp(apps: List<InstalledApp>, definition: CapabilityDefinition): Int {
        // Recommend based on known app priority (first in list = most popular)
        val knownPackages = definition.knownApps.map { it.packageName }

        apps.forEachIndexed { index, app ->
            if (app.packageName in knownPackages) {
                return index
            }
        }
        return 0
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }
}

// Resolution Results
sealed class AppResolution {
    data class Resolved(
        val packageName: String,
        val appName: String,
        val source: ResolutionSource
    ) : AppResolution()

    data class MultipleAvailable(
        val capability: String,
        val apps: List<InstalledApp>,
        val recommendedIndex: Int = 0
    ) : AppResolution()

    data class NoneAvailable(
        val capability: String,
        val suggestedApps: List<KnownApp>
    ) : AppResolution()

    data class UnknownCapability(val capability: String) : AppResolution()
}

enum class ResolutionSource {
    USER_PREFERENCE,    // User explicitly chose this app
    AUTO_DETECTED,      // Only one app available, auto-selected
    USAGE_PATTERN       // Learned from user behavior
}

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
)
```

---

## UserPreferencesRepository

### Database Schema (SQLDelight)

```sql
-- PreferencesSchema.sq

-- App preferences per capability
CREATE TABLE AppPreferences (
    capability TEXT PRIMARY KEY NOT NULL,
    package_name TEXT NOT NULL,
    set_at INTEGER NOT NULL,           -- Unix timestamp
    set_by TEXT NOT NULL DEFAULT 'user' -- 'user', 'auto', 'usage'
);

-- Smart home platform credentials (encrypted)
CREATE TABLE SmartHomePlatforms (
    platform_id TEXT PRIMARY KEY NOT NULL,
    credentials_encrypted TEXT NOT NULL,
    iv TEXT NOT NULL,                   -- Initialization vector
    configured_at INTEGER NOT NULL,
    last_used_at INTEGER,
    device_count INTEGER DEFAULT 0
);

-- Smart home devices cache
CREATE TABLE SmartHomeDevices (
    device_id TEXT PRIMARY KEY NOT NULL,
    platform_id TEXT NOT NULL,
    name TEXT NOT NULL,
    room TEXT,
    device_type TEXT NOT NULL,
    capabilities TEXT NOT NULL,         -- JSON array
    platform_device_id TEXT NOT NULL,
    last_state TEXT,                    -- JSON
    last_updated INTEGER,
    FOREIGN KEY (platform_id) REFERENCES SmartHomePlatforms(platform_id)
);

-- Usage patterns for learning
CREATE TABLE UsagePatterns (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    capability TEXT NOT NULL,
    package_name TEXT NOT NULL,
    used_at INTEGER NOT NULL,
    context TEXT                        -- JSON: time_of_day, location, etc.
);

-- Queries
getPreferredApp:
SELECT package_name FROM AppPreferences WHERE capability = ?;

setPreferredApp:
INSERT OR REPLACE INTO AppPreferences (capability, package_name, set_at, set_by)
VALUES (?, ?, ?, ?);

clearPreferredApp:
DELETE FROM AppPreferences WHERE capability = ?;

getAllPreferences:
SELECT * FROM AppPreferences;

-- Smart Home queries
getPlatformCredentials:
SELECT * FROM SmartHomePlatforms WHERE platform_id = ?;

savePlatformCredentials:
INSERT OR REPLACE INTO SmartHomePlatforms
(platform_id, credentials_encrypted, iv, configured_at, device_count)
VALUES (?, ?, ?, ?, ?);

getDevicesForPlatform:
SELECT * FROM SmartHomeDevices WHERE platform_id = ?;

getAllDevices:
SELECT * FROM SmartHomeDevices;

saveDevice:
INSERT OR REPLACE INTO SmartHomeDevices
(device_id, platform_id, name, room, device_type, capabilities, platform_device_id, last_state, last_updated)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);

-- Usage tracking
recordUsage:
INSERT INTO UsagePatterns (capability, package_name, used_at, context)
VALUES (?, ?, ?, ?);

getMostUsedApp:
SELECT package_name, COUNT(*) as usage_count
FROM UsagePatterns
WHERE capability = ?
GROUP BY package_name
ORDER BY usage_count DESC
LIMIT 1;
```

### Repository Implementation

```kotlin
// UserPreferencesRepository.kt
class UserPreferencesRepository(
    private val database: AvaDatabase,
    private val encryptionService: EncryptionService
) {
    private val queries = database.preferencesSchemaQueries

    // ==================== App Preferences ====================

    suspend fun getPreferredApp(capability: String): String? = withContext(Dispatchers.IO) {
        queries.getPreferredApp(capability).executeAsOneOrNull()
    }

    suspend fun setPreferredApp(
        capability: String,
        packageName: String,
        setBy: String = "user"
    ) = withContext(Dispatchers.IO) {
        queries.setPreferredApp(
            capability = capability,
            package_name = packageName,
            set_at = System.currentTimeMillis(),
            set_by = setBy
        )
    }

    suspend fun clearPreferredApp(capability: String) = withContext(Dispatchers.IO) {
        queries.clearPreferredApp(capability)
    }

    suspend fun getAllPreferences(): Map<String, String> = withContext(Dispatchers.IO) {
        queries.getAllPreferences().executeAsList()
            .associate { it.capability to it.package_name }
    }

    // ==================== Smart Home Credentials ====================

    suspend fun savePlatformCredentials(
        platformId: String,
        credentials: PlatformCredentials
    ) = withContext(Dispatchers.IO) {
        val (encrypted, iv) = encryptionService.encrypt(credentials.toJson())
        queries.savePlatformCredentials(
            platform_id = platformId,
            credentials_encrypted = encrypted,
            iv = iv,
            configured_at = System.currentTimeMillis(),
            device_count = 0
        )
    }

    suspend fun getPlatformCredentials(platformId: String): PlatformCredentials? =
        withContext(Dispatchers.IO) {
            val row = queries.getPlatformCredentials(platformId).executeAsOneOrNull()
                ?: return@withContext null

            val decrypted = encryptionService.decrypt(row.credentials_encrypted, row.iv)
            PlatformCredentials.fromJson(decrypted)
        }

    suspend fun isPlatformConfigured(platformId: String): Boolean = withContext(Dispatchers.IO) {
        queries.getPlatformCredentials(platformId).executeAsOneOrNull() != null
    }

    // ==================== Smart Home Devices ====================

    suspend fun saveDevices(devices: List<SmartHomeDevice>) = withContext(Dispatchers.IO) {
        devices.forEach { device ->
            queries.saveDevice(
                device_id = device.id,
                platform_id = device.platform,
                name = device.name,
                room = device.room,
                device_type = device.type.name,
                capabilities = device.capabilities.toJson(),
                platform_device_id = device.platformDeviceId,
                last_state = device.state?.toJson(),
                last_updated = System.currentTimeMillis()
            )
        }
    }

    suspend fun getAllDevices(): List<SmartHomeDevice> = withContext(Dispatchers.IO) {
        queries.getAllDevices().executeAsList().map { it.toSmartHomeDevice() }
    }

    suspend fun getDevicesForPlatform(platformId: String): List<SmartHomeDevice> =
        withContext(Dispatchers.IO) {
            queries.getDevicesForPlatform(platformId).executeAsList()
                .map { it.toSmartHomeDevice() }
        }

    // ==================== Usage Learning ====================

    suspend fun recordAppUsage(
        capability: String,
        packageName: String,
        context: UsageContext? = null
    ) = withContext(Dispatchers.IO) {
        queries.recordUsage(
            capability = capability,
            package_name = packageName,
            used_at = System.currentTimeMillis(),
            context = context?.toJson()
        )
    }

    suspend fun getMostUsedApp(capability: String): String? = withContext(Dispatchers.IO) {
        queries.getMostUsedApp(capability).executeAsOneOrNull()?.package_name
    }
}

// Credential types for different platforms
sealed class PlatformCredentials {
    abstract fun toJson(): String

    data class OAuth(
        val accessToken: String,
        val refreshToken: String?,
        val expiresAt: Long?
    ) : PlatformCredentials()

    data class Token(
        val serverUrl: String,
        val accessToken: String
    ) : PlatformCredentials()

    data class Local(
        val discoveredDevices: List<String>
    ) : PlatformCredentials()

    companion object {
        fun fromJson(json: String): PlatformCredentials = /* parse */
    }
}
```

---

## Smart Home Module

### SmartHomeSetupService

```kotlin
// SmartHomeSetupService.kt
class SmartHomeSetupService(
    private val context: Context,
    private val preferencesRepo: UserPreferencesRepository,
    private val adapters: Map<String, SmartHomePlatformAdapter>
) {
    /**
     * Pre-configured platform definitions.
     * User only needs to authenticate - no manual configuration.
     */
    val availablePlatforms = listOf(
        PlatformConfig(
            id = "google_home",
            name = "Google Home",
            description = "Control devices connected to Google Home",
            authType = AuthType.OAUTH,
            oauthConfig = OAuthConfig(
                clientId = BuildConfig.GOOGLE_HOME_CLIENT_ID,
                clientSecret = BuildConfig.GOOGLE_HOME_CLIENT_SECRET,
                authUrl = "https://accounts.google.com/o/oauth2/v2/auth",
                tokenUrl = "https://oauth2.googleapis.com/token",
                scopes = listOf(
                    "https://www.googleapis.com/auth/homegraph"
                ),
                redirectUri = "com.augmentalis.ava:/oauth2callback"
            ),
            iconRes = R.drawable.ic_google_home,
            detectionPackage = "com.google.android.apps.chromecast.app"
        ),

        PlatformConfig(
            id = "alexa",
            name = "Amazon Alexa",
            description = "Control Alexa-enabled smart home devices",
            authType = AuthType.OAUTH,
            oauthConfig = OAuthConfig(
                clientId = BuildConfig.ALEXA_CLIENT_ID,
                clientSecret = BuildConfig.ALEXA_CLIENT_SECRET,
                authUrl = "https://www.amazon.com/ap/oa",
                tokenUrl = "https://api.amazon.com/auth/o2/token",
                scopes = listOf(
                    "alexa::smart_home"
                ),
                redirectUri = "com.augmentalis.ava:/oauth2callback"
            ),
            iconRes = R.drawable.ic_alexa,
            detectionPackage = "com.amazon.dee.app"
        ),

        PlatformConfig(
            id = "home_assistant",
            name = "Home Assistant",
            description = "Connect to your Home Assistant server",
            authType = AuthType.TOKEN,
            tokenConfig = TokenConfig(
                fields = listOf(
                    InputField(
                        id = "server_url",
                        label = "Server URL",
                        hint = "https://your-home.duckdns.org:8123",
                        inputType = InputType.URL,
                        validation = { it.startsWith("http") }
                    ),
                    InputField(
                        id = "access_token",
                        label = "Long-Lived Access Token",
                        hint = "eyJ0eXAiOiJKV1...",
                        inputType = InputType.PASSWORD,
                        helpUrl = "https://www.home-assistant.io/docs/authentication/#your-account-profile"
                    )
                )
            ),
            iconRes = R.drawable.ic_home_assistant,
            detectionPackage = "io.homeassistant.companion.android"
        ),

        PlatformConfig(
            id = "matter",
            name = "Matter/Thread",
            description = "Discover Matter-compatible devices on your network",
            authType = AuthType.LOCAL_DISCOVERY,
            iconRes = R.drawable.ic_matter,
            platformRestriction = null // Both platforms
        ),

        PlatformConfig(
            id = "homekit",
            name = "Apple HomeKit",
            description = "Control HomeKit accessories",
            authType = AuthType.SYSTEM,
            iconRes = R.drawable.ic_homekit,
            platformRestriction = Platform.IOS
        ),

        PlatformConfig(
            id = "smartthings",
            name = "Samsung SmartThings",
            description = "Control SmartThings devices",
            authType = AuthType.OAUTH,
            oauthConfig = OAuthConfig(
                clientId = BuildConfig.SMARTTHINGS_CLIENT_ID,
                clientSecret = BuildConfig.SMARTTHINGS_CLIENT_SECRET,
                authUrl = "https://api.smartthings.com/oauth/authorize",
                tokenUrl = "https://api.smartthings.com/oauth/token",
                scopes = listOf("r:devices:*", "x:devices:*"),
                redirectUri = "com.augmentalis.ava:/oauth2callback"
            ),
            iconRes = R.drawable.ic_smartthings,
            detectionPackage = "com.samsung.android.oneconnect"
        )
    )

    /**
     * Auto-detect which platforms the user likely has based on installed apps.
     */
    suspend fun detectInstalledPlatforms(): List<DetectedPlatform> {
        val detected = mutableListOf<DetectedPlatform>()
        val pm = context.packageManager

        for (platform in availablePlatforms) {
            if (platform.detectionPackage != null) {
                val isInstalled = try {
                    pm.getPackageInfo(platform.detectionPackage, 0)
                    true
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }

                if (isInstalled) {
                    val isConfigured = preferencesRepo.isPlatformConfigured(platform.id)
                    detected.add(DetectedPlatform(
                        config = platform,
                        isInstalled = true,
                        isConfigured = isConfigured
                    ))
                }
            }
        }

        return detected
    }

    /**
     * Get all platforms with their configuration status.
     */
    suspend fun getAllPlatforms(): List<PlatformStatus> {
        return availablePlatforms
            .filter { it.platformRestriction == null || it.platformRestriction == Platform.current }
            .map { platform ->
                val isInstalled = platform.detectionPackage?.let { pkg ->
                    try {
                        context.packageManager.getPackageInfo(pkg, 0)
                        true
                    } catch (e: Exception) { false }
                } ?: true

                val isConfigured = preferencesRepo.isPlatformConfigured(platform.id)
                val deviceCount = if (isConfigured) {
                    preferencesRepo.getDevicesForPlatform(platform.id).size
                } else 0

                PlatformStatus(
                    config = platform,
                    isInstalled = isInstalled,
                    isConfigured = isConfigured,
                    deviceCount = deviceCount
                )
            }
    }

    /**
     * Start the setup flow for a platform.
     */
    suspend fun setupPlatform(platformId: String): SetupFlow {
        val platform = availablePlatforms.find { it.id == platformId }
            ?: return SetupFlow.Error("Unknown platform: $platformId")

        return when (platform.authType) {
            AuthType.OAUTH -> SetupFlow.OAuth(
                authUrl = buildOAuthUrl(platform.oauthConfig!!),
                platformId = platformId
            )
            AuthType.TOKEN -> SetupFlow.TokenInput(
                fields = platform.tokenConfig!!.fields,
                platformId = platformId
            )
            AuthType.LOCAL_DISCOVERY -> {
                // Start Matter discovery
                SetupFlow.Discovery(platformId = platformId)
            }
            AuthType.SYSTEM -> {
                // iOS HomeKit - use system API
                SetupFlow.SystemAuth(platformId = platformId)
            }
        }
    }

    /**
     * Complete OAuth flow with authorization code.
     */
    suspend fun completeOAuthSetup(platformId: String, authCode: String): SetupResult {
        val platform = availablePlatforms.find { it.id == platformId }
            ?: return SetupResult.Error("Unknown platform")

        return try {
            // Exchange code for tokens
            val tokens = exchangeOAuthCode(platform.oauthConfig!!, authCode)

            // Save credentials
            preferencesRepo.savePlatformCredentials(
                platformId = platformId,
                credentials = PlatformCredentials.OAuth(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    expiresAt = tokens.expiresAt
                )
            )

            // Discover devices
            val adapter = adapters[platformId]!!
            val devices = adapter.discoverDevices()
            preferencesRepo.saveDevices(devices)

            SetupResult.Success(
                platformName = platform.name,
                deviceCount = devices.size,
                devices = devices
            )
        } catch (e: Exception) {
            SetupResult.Error("Setup failed: ${e.message}")
        }
    }

    /**
     * Complete token-based setup (Home Assistant).
     */
    suspend fun completeTokenSetup(
        platformId: String,
        fields: Map<String, String>
    ): SetupResult {
        val platform = availablePlatforms.find { it.id == platformId }
            ?: return SetupResult.Error("Unknown platform")

        // Validate required fields
        for (field in platform.tokenConfig!!.fields) {
            val value = fields[field.id]
            if (value.isNullOrBlank()) {
                return SetupResult.Error("${field.label} is required")
            }
            if (field.validation != null && !field.validation.invoke(value)) {
                return SetupResult.Error("Invalid ${field.label}")
            }
        }

        return try {
            val serverUrl = fields["server_url"]!!
            val token = fields["access_token"]!!

            // Test connection
            val adapter = adapters[platformId] as HomeAssistantAdapter
            adapter.testConnection(serverUrl, token)

            // Save credentials
            preferencesRepo.savePlatformCredentials(
                platformId = platformId,
                credentials = PlatformCredentials.Token(
                    serverUrl = serverUrl,
                    accessToken = token
                )
            )

            // Discover devices
            val devices = adapter.discoverDevices()
            preferencesRepo.saveDevices(devices)

            SetupResult.Success(
                platformName = platform.name,
                deviceCount = devices.size,
                devices = devices
            )
        } catch (e: Exception) {
            SetupResult.Error("Connection failed: ${e.message}")
        }
    }

    private fun buildOAuthUrl(config: OAuthConfig): String {
        return Uri.parse(config.authUrl).buildUpon()
            .appendQueryParameter("client_id", config.clientId)
            .appendQueryParameter("redirect_uri", config.redirectUri)
            .appendQueryParameter("scope", config.scopes.joinToString(" "))
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("state", generateState())
            .build()
            .toString()
    }
}

// Data classes
data class PlatformConfig(
    val id: String,
    val name: String,
    val description: String,
    val authType: AuthType,
    val oauthConfig: OAuthConfig? = null,
    val tokenConfig: TokenConfig? = null,
    val iconRes: Int,
    val detectionPackage: String? = null,
    val platformRestriction: Platform? = null
)

enum class AuthType {
    OAUTH,           // Google, Alexa, SmartThings
    TOKEN,           // Home Assistant
    LOCAL_DISCOVERY, // Matter/Thread
    SYSTEM           // HomeKit (iOS system API)
}

data class OAuthConfig(
    val clientId: String,
    val clientSecret: String,
    val authUrl: String,
    val tokenUrl: String,
    val scopes: List<String>,
    val redirectUri: String
)

data class TokenConfig(
    val fields: List<InputField>
)

data class InputField(
    val id: String,
    val label: String,
    val hint: String,
    val inputType: InputType,
    val helpUrl: String? = null,
    val validation: ((String) -> Boolean)? = null
)

sealed class SetupFlow {
    data class OAuth(val authUrl: String, val platformId: String) : SetupFlow()
    data class TokenInput(val fields: List<InputField>, val platformId: String) : SetupFlow()
    data class Discovery(val platformId: String) : SetupFlow()
    data class SystemAuth(val platformId: String) : SetupFlow()
    data class Error(val message: String) : SetupFlow()
}

sealed class SetupResult {
    data class Success(
        val platformName: String,
        val deviceCount: Int,
        val devices: List<SmartHomeDevice>
    ) : SetupResult()
    data class Error(val message: String) : SetupResult()
}
```

---

## Platform Adapters

### GoogleHomeAdapter

```kotlin
// adapters/GoogleHomeAdapter.kt
class GoogleHomeAdapter(
    private val httpClient: HttpClient,
    private val preferencesRepo: UserPreferencesRepository
) : SmartHomePlatformAdapter {

    override val platformId = "google_home"
    override val platformName = "Google Home"

    private suspend fun getAccessToken(): String {
        val credentials = preferencesRepo.getPlatformCredentials(platformId)
            as? PlatformCredentials.OAuth
            ?: throw IllegalStateException("Google Home not configured")

        // Check if token expired and refresh if needed
        if (credentials.expiresAt != null &&
            credentials.expiresAt < System.currentTimeMillis()) {
            return refreshToken(credentials)
        }

        return credentials.accessToken
    }

    override suspend fun discoverDevices(): List<SmartHomeDevice> {
        val token = getAccessToken()

        val response = httpClient.post("https://homegraph.googleapis.com/v1/devices:sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(mapOf("agentUserId" to getUserId()))
        }

        val body = response.body<GoogleSyncResponse>()

        return body.devices.map { device ->
            SmartHomeDevice(
                id = "${platformId}_${device.id}",
                name = device.name.defaultNames.firstOrNull() ?: device.id,
                type = mapGoogleDeviceType(device.type),
                room = device.roomHint,
                platform = platformId,
                platformDeviceId = device.id,
                capabilities = mapGoogleTraits(device.traits)
            )
        }
    }

    override suspend fun executeCommand(
        device: SmartHomeDevice,
        command: SmartHomeCommand
    ): ActionResult {
        val token = getAccessToken()

        val googleCommand = when (command.action) {
            "on" -> GoogleCommand("action.devices.commands.OnOff", mapOf("on" to true))
            "off" -> GoogleCommand("action.devices.commands.OnOff", mapOf("on" to false))
            "dim" -> GoogleCommand("action.devices.commands.BrightnessAbsolute",
                mapOf("brightness" to command.brightness))
            "set_temperature" -> GoogleCommand("action.devices.commands.ThermostatTemperatureSetpoint",
                mapOf("thermostatTemperatureSetpoint" to command.temperature))
            else -> return ActionResult.Error("Unsupported command: ${command.action}")
        }

        val response = httpClient.post("https://homegraph.googleapis.com/v1/devices:execute") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(GoogleExecuteRequest(
                commands = listOf(GoogleExecuteCommand(
                    devices = listOf(GoogleDeviceRef(device.platformDeviceId)),
                    execution = listOf(googleCommand)
                ))
            ))
        }

        return if (response.status.isSuccess()) {
            ActionResult.Success("${device.name} ${command.action}")
        } else {
            ActionResult.Error("Failed to control ${device.name}")
        }
    }

    private fun mapGoogleDeviceType(type: String): DeviceType = when (type) {
        "action.devices.types.LIGHT" -> DeviceType.LIGHT
        "action.devices.types.SWITCH" -> DeviceType.SWITCH
        "action.devices.types.THERMOSTAT" -> DeviceType.THERMOSTAT
        "action.devices.types.LOCK" -> DeviceType.LOCK
        "action.devices.types.CAMERA" -> DeviceType.CAMERA
        else -> DeviceType.SWITCH
    }

    private fun mapGoogleTraits(traits: List<String>): Set<Capability> {
        return traits.mapNotNull { trait ->
            when (trait) {
                "action.devices.traits.OnOff" -> Capability.ON_OFF
                "action.devices.traits.Brightness" -> Capability.BRIGHTNESS
                "action.devices.traits.ColorSetting" -> Capability.RGB_COLOR
                "action.devices.traits.TemperatureSetting" -> Capability.TEMPERATURE_SET
                "action.devices.traits.LockUnlock" -> Capability.LOCK_UNLOCK
                else -> null
            }
        }.toSet()
    }
}
```

### HomeAssistantAdapter

```kotlin
// adapters/HomeAssistantAdapter.kt
class HomeAssistantAdapter(
    private val httpClient: HttpClient,
    private val preferencesRepo: UserPreferencesRepository
) : SmartHomePlatformAdapter {

    override val platformId = "home_assistant"
    override val platformName = "Home Assistant"

    private suspend fun getConfig(): PlatformCredentials.Token {
        return preferencesRepo.getPlatformCredentials(platformId)
            as? PlatformCredentials.Token
            ?: throw IllegalStateException("Home Assistant not configured")
    }

    suspend fun testConnection(serverUrl: String, token: String): Boolean {
        val response = httpClient.get("$serverUrl/api/") {
            header("Authorization", "Bearer $token")
        }
        return response.status.isSuccess()
    }

    override suspend fun discoverDevices(): List<SmartHomeDevice> {
        val config = getConfig()

        val response = httpClient.get("${config.serverUrl}/api/states") {
            header("Authorization", "Bearer ${config.accessToken}")
        }

        val states = response.body<List<HAState>>()

        return states
            .filter { it.entity_id.startsWith("light.") ||
                     it.entity_id.startsWith("switch.") ||
                     it.entity_id.startsWith("climate.") ||
                     it.entity_id.startsWith("lock.") ||
                     it.entity_id.startsWith("cover.") ||
                     it.entity_id.startsWith("fan.") }
            .map { state ->
                SmartHomeDevice(
                    id = "${platformId}_${state.entity_id}",
                    name = state.attributes.friendly_name ?: state.entity_id,
                    type = mapHAEntityType(state.entity_id),
                    room = extractRoom(state.attributes),
                    platform = platformId,
                    platformDeviceId = state.entity_id,
                    capabilities = inferCapabilities(state),
                    state = DeviceState(
                        isOn = state.state == "on",
                        brightness = state.attributes.brightness?.let { (it / 255.0 * 100).toInt() },
                        temperature = state.attributes.temperature
                    )
                )
            }
    }

    override suspend fun executeCommand(
        device: SmartHomeDevice,
        command: SmartHomeCommand
    ): ActionResult {
        val config = getConfig()

        val (domain, service) = when {
            device.platformDeviceId.startsWith("light.") -> {
                "light" to if (command.action == "on") "turn_on" else "turn_off"
            }
            device.platformDeviceId.startsWith("switch.") -> {
                "switch" to if (command.action == "on") "turn_on" else "turn_off"
            }
            device.platformDeviceId.startsWith("climate.") -> {
                "climate" to "set_temperature"
            }
            device.platformDeviceId.startsWith("lock.") -> {
                "lock" to command.action
            }
            device.platformDeviceId.startsWith("cover.") -> {
                "cover" to if (command.action == "on") "open_cover" else "close_cover"
            }
            else -> return ActionResult.Error("Unsupported device type")
        }

        val serviceData = mutableMapOf<String, Any>(
            "entity_id" to device.platformDeviceId
        )

        command.brightness?.let { serviceData["brightness_pct"] = it }
        command.temperature?.let { serviceData["temperature"] = it }
        command.rgbColor?.let { serviceData["rgb_color"] = parseRgb(it) }

        val response = httpClient.post("${config.serverUrl}/api/services/$domain/$service") {
            header("Authorization", "Bearer ${config.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(serviceData)
        }

        return if (response.status.isSuccess()) {
            ActionResult.Success("${device.name} ${command.action}")
        } else {
            ActionResult.Error("Failed: ${response.status}")
        }
    }

    override suspend fun getDeviceState(device: SmartHomeDevice): DeviceState {
        val config = getConfig()

        val response = httpClient.get(
            "${config.serverUrl}/api/states/${device.platformDeviceId}"
        ) {
            header("Authorization", "Bearer ${config.accessToken}")
        }

        val state = response.body<HAState>()

        return DeviceState(
            isOn = state.state == "on",
            brightness = state.attributes.brightness?.let { (it / 255.0 * 100).toInt() },
            temperature = state.attributes.temperature,
            colorTemp = state.attributes.color_temp
        )
    }

    private fun mapHAEntityType(entityId: String): DeviceType = when {
        entityId.startsWith("light.") -> DeviceType.LIGHT
        entityId.startsWith("switch.") -> DeviceType.SWITCH
        entityId.startsWith("climate.") -> DeviceType.THERMOSTAT
        entityId.startsWith("lock.") -> DeviceType.LOCK
        entityId.startsWith("cover.") -> DeviceType.BLINDS
        entityId.startsWith("fan.") -> DeviceType.FAN
        else -> DeviceType.SWITCH
    }
}
```

---

## UI Components

### App Preference Prompt (Bottom Sheet)

```kotlin
// ui/AppPreferenceBottomSheet.kt
@Composable
fun AppPreferenceBottomSheet(
    capability: String,
    apps: List<InstalledApp>,
    recommendedIndex: Int,
    onAppSelected: (InstalledApp, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val capabilityName = CapabilityRegistry.capabilities[capability]?.displayName ?: capability

    BottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Choose $capabilityName App",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AVA found multiple apps. Which would you like to use?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            apps.forEachIndexed { index, app ->
                AppOptionCard(
                    app = app,
                    isRecommended = index == recommendedIndex,
                    onClick = { onAppSelected(app, true) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { onAppSelected(apps[recommendedIndex], false) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Always ask me")
            }
        }
    }
}

@Composable
private fun AppOptionCard(
    app: InstalledApp,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            app.icon?.let { icon ->
                Image(
                    bitmap = icon.toBitmap().asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium
                )
                if (isRecommended) {
                    Text(
                        text = "Recommended",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select"
            )
        }
    }
}
```

### Smart Home Setup Screen

```kotlin
// ui/SmartHomeSetupScreen.kt
@Composable
fun SmartHomeSetupScreen(
    viewModel: SmartHomeSetupViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val platforms by viewModel.platforms.collectAsState()
    val setupState by viewModel.setupState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Home Setup") },
                navigationIcon = {
                    IconButton(onClick = onComplete) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Detected platforms section
            val detected = platforms.filter { it.isInstalled && !it.isConfigured }
            if (detected.isNotEmpty()) {
                item {
                    Text(
                        text = "Detected on your device",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(detected) { platform ->
                    PlatformCard(
                        platform = platform,
                        onConnect = { viewModel.startSetup(platform.config.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            // Configured platforms
            val configured = platforms.filter { it.isConfigured }
            if (configured.isNotEmpty()) {
                item {
                    Text(
                        text = "Connected",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(configured) { platform ->
                    ConfiguredPlatformCard(
                        platform = platform,
                        onRefresh = { viewModel.refreshDevices(platform.config.id) },
                        onDisconnect = { viewModel.disconnect(platform.config.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            // Other platforms
            val other = platforms.filter { !it.isInstalled && !it.isConfigured }
            if (other.isNotEmpty()) {
                item {
                    Text(
                        text = "Other Platforms",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(other) { platform ->
                    PlatformCard(
                        platform = platform,
                        onConnect = { viewModel.startSetup(platform.config.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // Handle setup flow
    when (val state = setupState) {
        is SetupState.OAuth -> {
            OAuthWebView(
                url = state.authUrl,
                onCodeReceived = { code -> viewModel.completeOAuth(state.platformId, code) },
                onCancel = { viewModel.cancelSetup() }
            )
        }
        is SetupState.TokenInput -> {
            TokenInputDialog(
                fields = state.fields,
                onSubmit = { values -> viewModel.completeTokenSetup(state.platformId, values) },
                onCancel = { viewModel.cancelSetup() }
            )
        }
        is SetupState.Success -> {
            SetupSuccessDialog(
                platformName = state.platformName,
                deviceCount = state.deviceCount,
                onDismiss = { viewModel.dismissResult() }
            )
        }
        is SetupState.Error -> {
            ErrorDialog(
                message = state.message,
                onDismiss = { viewModel.dismissResult() }
            )
        }
        else -> {}
    }
}

@Composable
private fun PlatformCard(
    platform: PlatformStatus,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(platform.config.iconRes),
                contentDescription = platform.config.name,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = platform.config.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = platform.config.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (platform.isInstalled) {
                    Text(
                        text = "App detected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Button(onClick = onConnect) {
                Text("Connect")
            }
        }
    }
}

@Composable
private fun ConfiguredPlatformCard(
    platform: PlatformStatus,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(platform.config.iconRes),
                contentDescription = platform.config.name,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = platform.config.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Connected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "${platform.deviceCount} devices",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "Refresh devices")
            }

            IconButton(onClick = onDisconnect) {
                Icon(Icons.Default.LinkOff, "Disconnect")
            }
        }
    }
}
```

---

## Integration with Intent Handlers

### Using AppResolverService in Action Handlers

```kotlin
// Example: EmailActionHandler.kt
class EmailActionHandler(
    private val appResolver: AppResolverService,
    private val preferencePromptManager: PreferencePromptManager
) : ActionHandler {

    override val intentId = "send_email"

    override suspend fun handle(
        input: String,
        params: Map<String, Any>?,
        ctx: ActionContext
    ): ActionResult {
        // Extract email parameters (recipient, subject, body)
        val emailParams = extractEmailParams(input, params)

        // Resolve email app
        when (val resolution = appResolver.resolveApp("email")) {
            is AppResolution.Resolved -> {
                // Launch email app directly
                return launchEmailApp(resolution.packageName, emailParams)
            }

            is AppResolution.MultipleAvailable -> {
                // Show selection UI (only happens once)
                preferencePromptManager.showAppSelection(
                    capability = "email",
                    apps = resolution.apps,
                    recommendedIndex = resolution.recommendedIndex,
                    onSelected = { app, remember ->
                        if (remember) {
                            appResolver.setPreference("email", app.packageName)
                        }
                        launchEmailApp(app.packageName, emailParams)
                    }
                )
                return ActionResult.Pending("Waiting for app selection")
            }

            is AppResolution.NoneAvailable -> {
                return ActionResult.Error(
                    "No email app installed. Would you like to install Gmail?"
                )
            }

            else -> return ActionResult.Error("Could not resolve email app")
        }
    }

    private fun launchEmailApp(packageName: String, params: EmailParams): ActionResult {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${params.recipient}")
            putExtra(Intent.EXTRA_SUBJECT, params.subject)
            putExtra(Intent.EXTRA_TEXT, params.body)
            setPackage(packageName)
        }

        context.startActivity(intent)
        return ActionResult.Success("Opening email to ${params.recipient}")
    }
}
```

---

## Implementation Status

### Phase 1: Core Infrastructure - ✅ COMPLETE (2025-12-05)

| Component | Status | Location |
|-----------|--------|----------|
| `CapabilityRegistry` | ✅ Done | `common/core/Domain/src/androidMain/kotlin/.../resolution/CapabilityRegistry.kt` |
| `AppResolverService` | ✅ Done | `common/core/Domain/src/androidMain/kotlin/.../resolution/AppResolverService.kt` |
| `AppPreferencesRepository` | ✅ Done | `common/core/Domain/src/commonMain/.../repository/AppPreferencesRepository.kt` |
| `AppPreferencesRepositoryImpl` | ✅ Done | `common/core/Data/src/main/kotlin/.../repository/AppPreferencesRepositoryImpl.kt` |
| `PreferencePromptManager` | ✅ Done | `common/core/Domain/src/androidMain/kotlin/.../resolution/PreferencePromptManager.kt` |
| `AppPreferences.sq` | ✅ Done | `common/core/Data/src/main/sqldelight/.../db/AppPreferences.sq` |
| `AppPreferenceBottomSheet` | ✅ Done | `common/Chat/src/main/kotlin/.../ui/components/AppPreferenceBottomSheet.kt` |
| `ResolutionModule` (Hilt) | ✅ Done | `android/ava/src/main/kotlin/.../di/ResolutionModule.kt` |
| `ActionResult.NeedsResolution` | ✅ Done | `common/Actions/src/main/kotlin/.../ActionResult.kt` |
| `SendEmailActionHandler` | ✅ Done | Updated with NeedsResolution |
| `SendTextActionHandler` | ✅ Done | Updated with NeedsResolution |
| Settings "Default Apps" | ✅ Done | `SettingsScreen.kt` - new section |
| **UI Wiring (C4 Fix)** | ✅ Done | `ChatViewModel.kt` - app preference sheet implementation |

**Capabilities Implemented (11):**
- email, sms, phone, music, video, maps, calendar, notes, browser, rideshare, food_delivery

### Phase 1.5: UI Integration - ✅ COMPLETE (2025-12-05)

**C4 Fix: App Resolution UI Wiring**

The app resolution UI has been fully wired in ChatViewModel, completing the user experience for app preference selection:

| Component | Implementation | Status |
|-----------|----------------|--------|
| State Management | `showAppPreferenceSheet: StateFlow<Boolean>` | ✅ Complete |
| Sheet Trigger | Detection of `ActionResult.NeedsResolution` | ✅ Complete |
| User Selection | `onAppSelected(capability, packageName)` | ✅ Complete |
| Preference Persistence | `appPreferenceManager.setPreferredApp()` | ✅ Complete |
| Sheet Dismissal | `dismissAppPreferenceSheet()` | ✅ Complete |

**Implementation Details:**

```kotlin
// ChatViewModel.kt - App Preference Sheet State
private val _showAppPreferenceSheet = MutableStateFlow(false)
val showAppPreferenceSheet: StateFlow<Boolean> = _showAppPreferenceSheet.asStateFlow()

// Handle ActionResult.NeedsResolution
when (actionResult) {
    is ActionResult.NeedsResolution -> {
        _showAppPreferenceSheet.value = true
    }
}

// User selects preferred app
fun onAppSelected(capability: String, packageName: String) {
    viewModelScope.launch {
        appPreferenceManager.setPreferredApp(capability, packageName)
        dismissAppPreferenceSheet()
    }
}

fun dismissAppPreferenceSheet() {
    _showAppPreferenceSheet.value = false
}
```

**User Flow (Now Complete):**

1. User: "Send email to John"
2. `SendEmailActionHandler` detects multiple email apps
3. Returns `ActionResult.NeedsResolution(capability = "email", apps = [Gmail, Outlook])`
4. ChatViewModel sets `showAppPreferenceSheet = true`
5. UI displays `AppPreferenceBottomSheet` with app choices
6. User selects Gmail
7. ChatViewModel calls `appPreferenceManager.setPreferredApp("email", "com.google.android.gm")`
8. Preference saved to database
9. Sheet dismissed
10. Next time: "Send email to John" → Opens Gmail directly (no prompt)

**Related:** See [Chapter 73 - Production Readiness](Developer-Manual-Chapter73-Production-Readiness-Security.md#c4-app-resolution-ui) for P1 fix details.

### Phase 2: Smart Home Module (Planned)
- [ ] `SmartHomeSetupService` - Platform definitions
- [ ] `HomeAssistantAdapter` - Full implementation
- [ ] `GoogleHomeAdapter` - OAuth + API
- [ ] Device discovery & caching

### Phase 3: UI Components (Partially Complete)
- [x] `AppPreferenceBottomSheet`
- [ ] `SmartHomeSetupScreen`
- [ ] OAuth WebView handling
- [ ] Token input dialogs

### Phase 4: Platform Adapters (Planned)
- [ ] `AlexaAdapter`
- [ ] `MatterAdapter`
- [ ] `HomeKitAdapter` (iOS)
- [ ] `SmartThingsAdapter`

### Phase 5: Integration (Partially Complete)
- [x] Update communication action handlers to use `AppResolverService`
- [ ] Update remaining action handlers
- [ ] Add smart home commands to `IntentRouter`
- [ ] End-to-end testing
- [x] Settings screen for preferences

---

## Best Practices

### Do's

| Practice | Reason |
|----------|--------|
| Cache device states | Reduce API calls |
| Encrypt all credentials | Security |
| Fuzzy match device names | Natural language varies |
| Auto-refresh tokens | Prevent auth failures |
| Show setup progress | User feedback |

### Don'ts

| Anti-Pattern | Why Avoid |
|--------------|-----------|
| Ask same question twice | Frustrates users |
| Store tokens in plain text | Security risk |
| Hardcode API keys | Use BuildConfig |
| Block UI during discovery | Poor UX |
| Ignore rate limits | APIs will block you |

---

## Author

Manoj Jhawar

---

## Related Chapters

- **Chapter 65**: NLU Intent System - Intent classification
- **Chapter 49**: Action Handlers - Handler implementation
- **Chapter 70**: Self-Learning NLU - Learning from behavior
- **Chapter 73**: Production Readiness & Security - P1 fixes including C4 app resolution UI

---

**Updated:** 2025-12-06 (added C4 fix implementation details - UI wiring complete)
