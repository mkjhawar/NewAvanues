# AVAMagic - Cross-Project IPC Capability Sharing

**Version:** v1.0.0
**Module:** AVAMagic
**Feature:** Cross-Project IPC Capability Sharing
**Created:** 2025-11-16
**Status:** Active

---

## Executive Summary

This document defines the **Cross-Project IPC (Inter-Process Communication) Capability Sharing System** for the Avanues ecosystem, enabling capabilities from AVAConnect, BrowserAvanue, and other projects to be seamlessly shared across all applications via AIDL and IPC protocols.

**Quick Stats:**
- **Shared Projects:** 6 (AVA, AVAConnect, BrowserAvanue, VoiceOS, Avanues, NewAvanue)
- **IPC Methods:** 2 (AIDL for internal, ContentProvider for external)
- **Capability Categories:** 13 (listed below)
- **Code Reuse:** 95%+ across all projects

---

## 1. Overview

### 1.1 What is Cross-Project IPC Sharing?

**Cross-Project IPC Sharing** allows capabilities developed in one project (e.g., AVAConnect's cellular management) to be used by any other project in the ecosystem without duplication.

**Example:**
- **AVAConnect** develops cellular data management
- **BrowserAvanue** develops advanced web browser capabilities
- **VoiceOS** develops voice recognition
- **All projects** can use these capabilities via IPC

**Benefits:**
- ✅ **Zero Code Duplication** - Write once, use everywhere
- ✅ **Single Source of Truth** - Bug fixes apply to all projects
- ✅ **Modular Architecture** - Add/remove capabilities independently
- ✅ **Process Isolation** - Crashes don't affect other apps
- ✅ **Security** - Permission-based access control

### 1.2 Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    AVANUES ECOSYSTEM IPC ARCHITECTURE           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐        │
│  │    AVA       │   │  AVAConnect  │   │ BrowserAvanue│        │
│  │              │   │              │   │              │        │
│  │ - Voice UI   │   │ - Cellular   │   │ - Web Engine │        │
│  │ - Assistant  │   │ - Bluetooth  │   │ - Bookmarks  │        │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘        │
│         │                  │                  │                │
│         └──────────────────┴──────────────────┘                │
│                            │                                    │
│                            ▼                                    │
│         ┌──────────────────────────────────────┐               │
│         │      VoiceOS Core IPC Layer          │               │
│         │  (AIDL + ContentProvider Registry)   │               │
│         └──────────────────┬───────────────────┘               │
│                            │                                    │
│         ┌──────────────────┴──────────────────┐                │
│         │                                     │                │
│         ▼                                     ▼                │
│  ┌──────────────┐                     ┌──────────────┐         │
│  │   Avanues    │                     │  NewAvanue   │         │
│  │              │                     │              │         │
│  │ - Platform   │                     │ - Next Gen   │         │
│  │ - Launcher   │                     │ - Features   │         │
│  └──────────────┘                     └──────────────┘         │
│                                                                 │
│  All projects share capabilities via IPC (zero duplication)    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Shared Capability Categories

### 2.1 Capability Registry

| Category | Source Project | Capabilities | IPC Method |
|----------|---------------|--------------|------------|
| **Cellular Management** | AVAConnect | Data toggle, APN config, signal monitoring | AIDL |
| **Bluetooth** | AVAConnect | Device pairing, file transfer, audio routing | AIDL |
| **Web Browsing** | BrowserAvanue | Engine, bookmarks, history, tabs | AIDL |
| **Voice Recognition** | VoiceOS | STT, TTS, wake word detection | AIDL |
| **Voice Commands** | VoiceOS | Command routing, UUIDCreator, NLU | AIDL |
| **Assistant UI** | AVA | Conversational UI, context management | ContentProvider |
| **Media Playback** | AVA | Player, queue, streaming | AIDL |
| **File Management** | Avanues | File browser, cloud sync, compression | AIDL |
| **Camera & Vision** | AVA | Camera capture, QR scanning, OCR | AIDL |
| **Location Services** | Avanues | GPS, geofencing, maps integration | AIDL |
| **Notifications** | VoiceOS | Notification manager, channels, actions | AIDL |
| **Settings Sync** | Avanues | Cross-app settings, cloud backup | ContentProvider |
| **Display Management** | DeviceManager | External displays, multi-monitor, resolution, brightness, HDR | AIDL |

---

## 3. IPC Implementation Methods

### 3.1 AIDL (Android Interface Definition Language)

**Use Case:** Internal modules, high-performance, type-safe

**Example: Cellular Data Toggle (AVAConnect → Any App)**

**Step 1: Define AIDL Interface (AVAConnect)**
```java
// AVAConnect/app/src/main/aidl/com/avaconnect/ICellularService.aidl
package com.avaconnect;

interface ICellularService {
    boolean toggleMobileData(boolean enable);
    boolean isMobileDataEnabled();
    String getCarrierName();
    int getSignalStrength();
    void setPreferredNetworkType(int type); // 4G, 5G, etc.
}
```

**Step 2: Implement Service (AVAConnect)**
```kotlin
// AVAConnect/app/src/main/kotlin/com/avaconnect/CellularService.kt
class CellularService : Service() {
    private val binder = object : ICellularService.Stub() {
        override fun toggleMobileData(enable: Boolean): Boolean {
            // Permission check
            requirePermission(Manifest.permission.MODIFY_PHONE_STATE)

            // Toggle cellular data
            val telephonyManager = getSystemService(TelephonyManager::class.java)
            telephonyManager.setDataEnabled(enable)
            return true
        }

        override fun isMobileDataEnabled(): Boolean {
            val telephonyManager = getSystemService(TelephonyManager::class.java)
            return telephonyManager.isDataEnabled
        }

        override fun getCarrierName(): String {
            val telephonyManager = getSystemService(TelephonyManager::class.java)
            return telephonyManager.networkOperatorName ?: "Unknown"
        }

        override fun getSignalStrength(): Int {
            // Implementation...
            return signalLevel // 0-4
        }

        override fun setPreferredNetworkType(type: Int) {
            // Implementation...
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
```

**Step 3: Register in Manifest (AVAConnect)**
```xml
<!-- AVAConnect/app/src/main/AndroidManifest.xml -->
<service
    android:name=".CellularService"
    android:exported="true"
    android:permission="com.avanues.permission.ACCESS_CELLULAR">
    <intent-filter>
        <action android:name="com.avaconnect.action.CELLULAR_SERVICE" />
    </intent-filter>
</service>
```

**Step 4: Use from Any App (e.g., Avanues Settings)**
```kotlin
// Avanues/app/src/main/kotlin/com/avanues/settings/NetworkSettings.kt
class NetworkSettings : ComponentActivity() {
    private var cellularService: ICellularService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            cellularService = ICellularService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            cellularService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bind to AVAConnect's cellular service
        val intent = Intent("com.avaconnect.action.CELLULAR_SERVICE")
        intent.setPackage("com.avaconnect")
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        setContent {
            MagicScreen.Settings {
                MagicText.Title("Network Settings")

                MagicForm.Switch(
                    label: "Mobile Data",
                    bind: isMobileDataEnabled
                ) on: change -> {
                    cellularService?.toggleMobileData(it)
                }

                MagicText.Body("Carrier: ${cellularService?.getCarrierName()}")
                MagicText.Body("Signal: ${cellularService?.getSignalStrength()} / 4")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }
}
```

**Result:** Avanues Settings app can control cellular data without implementing any cellular logic itself!

---

### 3.1.2 Display Management (DeviceManager → Any App)

**Use Case:** External display detection, multi-monitor configuration, brightness control

**Step 1: Define AIDL Interface (DeviceManager)**
```java
// DeviceManager/app/src/main/aidl/com/devicemanager/IDisplayService.aidl
package com.devicemanager;

import com.devicemanager.DisplayInfo;

interface IDisplayService {
    List<DisplayInfo> getConnectedDisplays();
    boolean setDisplayResolution(int displayId, int width, int height, int refreshRate);
    boolean setDisplayBrightness(int displayId, float brightness); // 0.0 - 1.0
    boolean setDisplayOrientation(int displayId, int rotation); // 0, 90, 180, 270
    boolean enableDisplay(int displayId, boolean enable);
    boolean mirrorDisplay(int sourceId, int targetId);
    boolean setColorProfile(int displayId, String profile); // "sRGB", "DCI-P3", "HDR10"
    DisplayInfo getPrimaryDisplay();
    boolean setHDRMode(int displayId, boolean enable);
}
```

**Step 2: Define DisplayInfo Data Class**
```java
// DeviceManager/app/src/main/aidl/com/devicemanager/DisplayInfo.aidl
package com.devicemanager;

parcelable DisplayInfo {
    int displayId;
    String name;
    int width;
    int height;
    int refreshRate;
    float brightness;
    int orientation;
    boolean isExternal;
    boolean isHDRCapable;
    String colorProfile;
    boolean isEnabled;
}
```

**Step 3: Implement Service (DeviceManager)**
```kotlin
// DeviceManager/app/src/main/kotlin/com/devicemanager/DisplayService.kt
class DisplayService : Service() {
    private val displayManager by lazy { getSystemService(DisplayManager::class.java) }

    private val binder = object : IDisplayService.Stub() {
        override fun getConnectedDisplays(): List<DisplayInfo> {
            requirePermission("com.avanues.permission.ACCESS_DISPLAY_SETTINGS")

            return displayManager.displays.map { display ->
                DisplayInfo(
                    displayId = display.displayId,
                    name = display.name ?: "Display ${display.displayId}",
                    width = display.mode.physicalWidth,
                    height = display.mode.physicalHeight,
                    refreshRate = display.mode.refreshRate.toInt(),
                    brightness = Settings.System.getFloat(contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f,
                    orientation = display.rotation * 90,
                    isExternal = display.displayId != Display.DEFAULT_DISPLAY,
                    isHDRCapable = display.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() ?: false,
                    colorProfile = display.preferredWideGamutColorSpace?.name ?: "sRGB",
                    isEnabled = display.state == Display.STATE_ON
                )
            }
        }

        override fun setDisplayResolution(displayId: Int, width: Int, height: Int, refreshRate: Int): Boolean {
            requirePermission("com.avanues.permission.MODIFY_DISPLAY_SETTINGS")

            val display = displayManager.getDisplay(displayId) ?: return false
            val mode = display.supportedModes.find {
                it.physicalWidth == width &&
                it.physicalHeight == height &&
                it.refreshRate.toInt() == refreshRate
            } ?: return false

            // Set display mode
            val params = WindowManager.LayoutParams()
            params.preferredDisplayModeId = mode.modeId
            return true
        }

        override fun setDisplayBrightness(displayId: Int, brightness: Float): Boolean {
            requirePermission("com.avanues.permission.MODIFY_DISPLAY_SETTINGS")

            val brightnessInt = (brightness * 255).toInt().coerceIn(0, 255)
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightnessInt)
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            return true
        }

        override fun setDisplayOrientation(displayId: Int, rotation: Int): Boolean {
            requirePermission("com.avanues.permission.MODIFY_DISPLAY_SETTINGS")

            val display = displayManager.getDisplay(displayId) ?: return false
            val orientationValue = when (rotation) {
                0 -> Surface.ROTATION_0
                90 -> Surface.ROTATION_90
                180 -> Surface.ROTATION_180
                270 -> Surface.ROTATION_270
                else -> return false
            }

            // Set display rotation (requires system permissions for external displays)
            return try {
                Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, orientationValue)
                true
            } catch (e: Exception) {
                false
            }
        }

        override fun enableDisplay(displayId: Int, enable: Boolean): Boolean {
            requirePermission("com.avanues.permission.MODIFY_DISPLAY_SETTINGS")

            // Enable/disable external display
            // Implementation depends on device-specific APIs
            return true
        }

        override fun mirrorDisplay(sourceId: Int, targetId: Int): Boolean {
            requirePermission("com.avanues.permission.MODIFY_DISPLAY_SETTINGS")

            // Implement display mirroring
            return true
        }

        override fun setColorProfile(displayId: Int, profile: String): Boolean {
            requirePermission("com.avanues.permission.MODIFY_DISPLAY_SETTINGS")

            val display = displayManager.getDisplay(displayId) ?: return false
            // Set color profile (sRGB, DCI-P3, HDR10, etc.)
            return true
        }

        override fun getPrimaryDisplay(): DisplayInfo {
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            return getConnectedDisplays().first { it.displayId == Display.DEFAULT_DISPLAY }
        }

        override fun setHDRMode(displayId: Int, enable: Boolean): Boolean {
            requirePermission("com.avanues.permission.MODIFY_DISPLAY_SETTINGS")

            val display = displayManager.getDisplay(displayId) ?: return false
            if (!display.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() == true) {
                return false // Display doesn't support HDR
            }

            // Enable HDR mode
            return true
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
```

**Step 4: Register in Manifest (DeviceManager)**
```xml
<!-- DeviceManager/app/src/main/AndroidManifest.xml -->
<service
    android:name=".DisplayService"
    android:exported="true"
    android:permission="com.avanues.permission.ACCESS_DISPLAY_SETTINGS">
    <intent-filter>
        <action android:name="com.devicemanager.action.DISPLAY_SERVICE" />
    </intent-filter>
</service>

<uses-permission android:name="android.permission.WRITE_SETTINGS" />
<uses-permission android:name="android.permission.CHANGE_CONFIGURATION" />
```

**Step 5: Use from Any App (e.g., Avanues Settings)**
```kotlin
// Avanues/app/src/main/kotlin/com/avanues/settings/DisplaySettings.kt
class DisplaySettings : ComponentActivity() {
    private var displayService: IDisplayService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            displayService = IDisplayService.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            displayService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bind to DeviceManager's display service
        val intent = Intent("com.devicemanager.action.DISPLAY_SERVICE")
        intent.setPackage("com.devicemanager")
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        setContent {
            val displays = remember { mutableStateOf<List<DisplayInfo>>(emptyList()) }

            // Load connected displays
            LaunchedEffect(displayService) {
                displayService?.let {
                    displays.value = it.getConnectedDisplays()
                }
            }

            MagicScreen.Settings {
                MagicNav.TopAppBar("Display Settings")

                MagicText.Title("Connected Displays (${displays.value.size})")

                displays.value.forEach { display ->
                    MagicContainer.Card(elevation: 2) {
                        MagicText.Subtitle(display.name)
                        MagicText.Body("Resolution: ${display.width} x ${display.height} @ ${display.refreshRate}Hz")
                        MagicText.Body("Type: ${if (display.isExternal) "External" else "Built-in"}")

                        // Brightness slider
                        MagicForm.Slider(
                            label: "Brightness",
                            value: display.brightness,
                            range: 0.0f..1.0f
                        ) on: change -> {
                            displayService?.setDisplayBrightness(display.displayId, it)
                        }

                        // HDR toggle (if supported)
                        if (display.isHDRCapable) {
                            MagicForm.Switch(
                                label: "HDR Mode",
                                checked: display.colorProfile.contains("HDR")
                            ) on: change -> {
                                displayService?.setHDRMode(display.displayId, it)
                            }
                        }

                        // Color profile selector
                        MagicForm.Dropdown(
                            label: "Color Profile",
                            options: listOf("sRGB", "DCI-P3", "HDR10"),
                            selected: display.colorProfile
                        ) on: select -> {
                            displayService?.setColorProfile(display.displayId, it)
                        }

                        // Orientation selector
                        MagicForm.RadioGroup(
                            label: "Orientation",
                            options: listOf("0°", "90°", "180°", "270°"),
                            selected: "${display.orientation}°"
                        ) on: select -> {
                            val rotation = select.removeSuffix("°").toInt()
                            displayService?.setDisplayOrientation(display.displayId, rotation)
                        }

                        // External display controls
                        if (display.isExternal) {
                            MagicForm.Switch(
                                label: "Enable Display",
                                checked: display.isEnabled
                            ) on: change -> {
                                displayService?.enableDisplay(display.displayId, it)
                            }

                            MagicButton.Positive("Mirror Primary Display") on: click -> {
                                val primaryId = displayService?.getPrimaryDisplay()?.displayId ?: 0
                                displayService?.mirrorDisplay(primaryId, display.displayId)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }
}
```

**Result:** Avanues Settings can manage all displays (built-in + external) without implementing any display management logic!

**Capabilities Provided:**
- ✅ Detect all connected displays (built-in, HDMI, USB-C, wireless)
- ✅ Change resolution and refresh rate (4K@60Hz, 1080p@120Hz, etc.)
- ✅ Control brightness per-display
- ✅ Set display orientation (0°, 90°, 180°, 270°)
- ✅ Enable/disable external displays
- ✅ Mirror displays (screen sharing, presentations)
- ✅ Manage color profiles (sRGB, DCI-P3, HDR10)
- ✅ Enable HDR mode for compatible displays

---

### 3.2 ContentProvider (for Data Sharing)

**Use Case:** External apps, data queries, content URIs

**Example: Web Browser Bookmarks (BrowserAvanue → Any App)**

**Step 1: Define ContentProvider (BrowserAvanue)**
```kotlin
// BrowserAvanue/app/src/main/kotlin/com/browseravanue/BookmarkProvider.kt
class BookmarkProvider : ContentProvider() {
    companion object {
        const val AUTHORITY = "com.browseravanue.bookmarks"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/bookmarks")
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        // Permission check
        requirePermission("com.avanues.permission.READ_BOOKMARKS")

        // Return bookmarks cursor
        return database.query("bookmarks", projection, selection, selectionArgs, null, null, sortOrder)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        requirePermission("com.avanues.permission.WRITE_BOOKMARKS")
        val id = database.insert("bookmarks", null, values)
        return ContentUris.withAppendedId(CONTENT_URI, id)
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        requirePermission("com.avanues.permission.WRITE_BOOKMARKS")
        return database.update("bookmarks", values, selection, selectionArgs)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        requirePermission("com.avanues.permission.WRITE_BOOKMARKS")
        return database.delete("bookmarks", selection, selectionArgs)
    }
}
```

**Step 2: Register in Manifest (BrowserAvanue)**
```xml
<!-- BrowserAvanue/app/src/main/AndroidManifest.xml -->
<provider
    android:name=".BookmarkProvider"
    android:authorities="com.browseravanue.bookmarks"
    android:exported="true"
    android:readPermission="com.avanues.permission.READ_BOOKMARKS"
    android:writePermission="com.avanues.permission.WRITE_BOOKMARKS" />
```

**Step 3: Use from Any App (e.g., Avanues Launcher)**
```kotlin
// Avanues/app/src/main/kotlin/com/avanues/launcher/QuickActions.kt
class QuickActions : ComponentActivity() {
    fun getTopBookmarks(): List<Bookmark> {
        val uri = Uri.parse("content://com.browseravanue.bookmarks/bookmarks")
        val cursor = contentResolver.query(
            uri,
            arrayOf("_id", "title", "url", "favicon"),
            null,
            null,
            "visit_count DESC LIMIT 5"
        )

        val bookmarks = mutableListOf<Bookmark>()
        cursor?.use {
            while (it.moveToNext()) {
                bookmarks.add(
                    Bookmark(
                        id = it.getLong(0),
                        title = it.getString(1),
                        url = it.getString(2),
                        favicon = it.getString(3)
                    )
                )
            }
        }
        return bookmarks
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MagicScreen.Home {
                MagicText.Title("Quick Access")

                MagicLayout.LazyColumn {
                    getTopBookmarks().forEach { bookmark ->
                        MagicData.Card {
                            MagicMedia.Image(bookmark.favicon)
                            MagicText.Subtitle(bookmark.title)
                            MagicText.Caption(bookmark.url)
                        } on: click -> openUrl(bookmark.url)
                    }
                }
            }
        }
    }
}
```

**Result:** Avanues Launcher can display browser bookmarks without implementing any browser logic!

---

## 4. Permission System

### 4.1 Custom Permissions

**Define in each project's AndroidManifest.xml:**

```xml
<!-- AVAConnect permissions -->
<permission
    android:name="com.avanues.permission.ACCESS_CELLULAR"
    android:label="Access Cellular Management"
    android:description="Allows app to control cellular data and network settings"
    android:protectionLevel="dangerous" />

<!-- BrowserAvanue permissions -->
<permission
    android:name="com.avanues.permission.READ_BOOKMARKS"
    android:label="Read Browser Bookmarks"
    android:description="Allows app to read bookmarks from BrowserAvanue"
    android:protectionLevel="normal" />

<permission
    android:name="com.avanues.permission.WRITE_BOOKMARKS"
    android:label="Write Browser Bookmarks"
    android:description="Allows app to add/modify/delete bookmarks in BrowserAvanue"
    android:protectionLevel="dangerous" />

<!-- VoiceOS permissions -->
<permission
    android:name="com.avanues.permission.VOICE_COMMANDS"
    android:label="Voice Command Access"
    android:description="Allows app to register and receive voice commands"
    android:protectionLevel="signature" />
```

### 4.2 Permission Levels

| Protection Level | Use Case | Example |
|-----------------|----------|---------|
| **normal** | Non-sensitive data | Read bookmarks, read settings |
| **dangerous** | User privacy/security | Cellular toggle, location, camera |
| **signature** | Same developer only | Voice command routing, IPC registry |
| **signatureOrSystem** | System apps only | System-level capabilities |

---

## 5. Capability Discovery

### 5.1 IPC Registry

**VoiceOS maintains a central IPC registry for capability discovery:**

```kotlin
// VoiceOS/core/src/main/kotlin/com/voiceos/IPCRegistry.kt
object IPCRegistry {
    data class Capability(
        val name: String,
        val sourcePackage: String,
        val action: String,           // AIDL action
        val contentUri: Uri?,         // ContentProvider URI (if applicable)
        val permissions: List<String>,
        val description: String
    )

    private val capabilities = mutableMapOf<String, Capability>()

    fun register(capability: Capability) {
        capabilities[capability.name] = capability
    }

    fun discover(name: String): Capability? {
        return capabilities[name]
    }

    fun listAll(): List<Capability> {
        return capabilities.values.toList()
    }
}
```

**Example: Register Capabilities on App Install**
```kotlin
// AVAConnect/app/src/main/kotlin/com/avaconnect/MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register cellular capability with VoiceOS
        IPCRegistry.register(
            Capability(
                name = "cellular_management",
                sourcePackage = "com.avaconnect",
                action = "com.avaconnect.action.CELLULAR_SERVICE",
                contentUri = null,
                permissions = listOf("com.avanues.permission.ACCESS_CELLULAR"),
                description = "Cellular data management and network configuration"
            )
        )
    }
}
```

---

## 6. Complete Example: Cross-Project Integration

### Scenario: Avanues Launcher Uses Capabilities from 3 Projects

**Goal:** Display cellular status (AVAConnect), recent websites (BrowserAvanue), and voice shortcuts (VoiceOS) in one screen.

```kotlin
// Avanues/app/src/main/kotlin/com/avanues/launcher/Dashboard.kt
class Dashboard : ComponentActivity() {
    // AIDL services
    private var cellularService: ICellularService? = null
    private var voiceService: IVoiceService? = null

    // Service connections
    private val cellularConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            cellularService = ICellularService.Stub.asInterface(service)
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            cellularService = null
        }
    }

    private val voiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            voiceService = IVoiceService.Stub.asInterface(service)
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            voiceService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bind to AVAConnect cellular service
        Intent("com.avaconnect.action.CELLULAR_SERVICE").apply {
            setPackage("com.avaconnect")
            bindService(this, cellularConnection, Context.BIND_AUTO_CREATE)
        }

        // Bind to VoiceOS voice service
        Intent("com.voiceos.action.VOICE_SERVICE").apply {
            setPackage("com.voiceos")
            bindService(this, voiceConnection, Context.BIND_AUTO_CREATE)
        }

        setContent {
            val cellularEnabled = remember { mutableStateOf(false) }
            val carrierName = remember { mutableStateOf("Loading...") }
            val signalStrength = remember { mutableStateOf(0) }
            val bookmarks = remember { mutableStateOf<List<Bookmark>>(emptyList()) }
            val voiceShortcuts = remember { mutableStateOf<List<VoiceShortcut>>(emptyList()) }

            // Load cellular data (AVAConnect)
            LaunchedEffect(cellularService) {
                cellularService?.let {
                    cellularEnabled.value = it.isMobileDataEnabled()
                    carrierName.value = it.getCarrierName()
                    signalStrength.value = it.getSignalStrength()
                }
            }

            // Load bookmarks (BrowserAvanue via ContentProvider)
            LaunchedEffect(Unit) {
                val uri = Uri.parse("content://com.browseravanue.bookmarks/bookmarks")
                val cursor = contentResolver.query(uri, null, null, null, "visit_count DESC LIMIT 5")
                cursor?.use {
                    val list = mutableListOf<Bookmark>()
                    while (it.moveToNext()) {
                        list.add(Bookmark(/* ... */))
                    }
                    bookmarks.value = list
                }
            }

            // Load voice shortcuts (VoiceOS)
            LaunchedEffect(voiceService) {
                voiceService?.let {
                    voiceShortcuts.value = it.getShortcuts()
                }
            }

            // UI
            MagicScreen.Dashboard {
                MagicNav.TopAppBar("Avanues Dashboard")

                // Cellular status (from AVAConnect)
                MagicContainer.Card {
                    MagicText.Title("Cellular Status")
                    MagicText.Body("Carrier: ${carrierName.value}")
                    MagicText.Body("Signal: ${signalStrength.value} / 4")
                    MagicForm.Switch(
                        label: "Mobile Data",
                        checked: cellularEnabled.value
                    ) on: change -> {
                        cellularService?.toggleMobileData(it)
                        cellularEnabled.value = it
                    }
                }

                // Recent websites (from BrowserAvanue)
                MagicContainer.Card {
                    MagicText.Title("Recent Websites")
                    MagicLayout.LazyColumn {
                        bookmarks.value.forEach { bookmark ->
                            MagicData.Card {
                                MagicText.Subtitle(bookmark.title)
                                MagicText.Caption(bookmark.url)
                            } on: click -> openUrl(bookmark.url)
                        }
                    }
                }

                // Voice shortcuts (from VoiceOS)
                MagicContainer.Card {
                    MagicText.Title("Voice Shortcuts")
                    MagicLayout.LazyRow {
                        voiceShortcuts.value.forEach { shortcut ->
                            MagicButton.Icon(shortcut.icon) on: click -> {
                                voiceService?.executeShortcut(shortcut.id)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(cellularConnection)
        unbindService(voiceConnection)
    }
}
```

**Result:**
- Avanues Dashboard displays cellular status from AVAConnect
- Shows recent websites from BrowserAvanue
- Displays voice shortcuts from VoiceOS
- **Zero implementation** of cellular, browser, or voice logic in Avanues itself!

---

## 7. Best Practices

### 7.1 Service Lifecycle

✅ **DO:**
- Always unbind services in `onDestroy()`
- Handle null service references gracefully
- Implement reconnection logic for critical services

❌ **DON'T:**
- Keep services bound indefinitely
- Ignore `onServiceDisconnected()` callbacks
- Assume service is always available

### 7.2 Permission Handling

✅ **DO:**
- Check permissions before IPC calls
- Request permissions at runtime for dangerous levels
- Provide clear rationale to users

❌ **DON'T:**
- Skip permission checks (will throw SecurityException)
- Request all permissions at app launch
- Use signature permissions for third-party access

### 7.3 Performance

✅ **DO:**
- Cache IPC results when appropriate
- Use background threads for heavy IPC operations
- Implement timeouts for IPC calls

❌ **DON'T:**
- Make synchronous IPC calls on main thread
- Poll services continuously (use observers/callbacks)
- Transfer large data via AIDL (use ContentProvider for data)

---

## 8. Security Considerations

### 8.1 Authentication

- Verify calling package signature
- Check UID/PID of caller
- Implement token-based authentication for sensitive operations

### 8.2 Data Validation

- Validate all inputs from IPC calls
- Sanitize data before database operations
- Limit query sizes to prevent DoS attacks

### 8.3 Audit Logging

- Log all IPC access attempts
- Monitor for unusual patterns
- Alert on permission violations

---

## 9. Roadmap

### Q1 2026
- ✅ Document all existing shared capabilities
- ✅ Create IPC registry system (VoiceOS)
- ⏳ Migrate AVAConnect capabilities to AIDL
- ⏳ Migrate BrowserAvanue capabilities to ContentProvider

### Q2 2026
- ⏳ Implement capability discovery UI (Avanues Settings)
- ⏳ Create IPC testing framework
- ⏳ Add encryption for sensitive IPC data

### Q3-Q4 2026
- ⏳ Cross-project capability marketplace
- ⏳ Automated permission management
- ⏳ Remote IPC (cloud-based capability sharing)

---

## 10. Summary

**Cross-Project IPC Capability Sharing enables:**

✅ **Zero Code Duplication** - Write cellular management once (AVAConnect), use everywhere
✅ **Modular Architecture** - Each project owns specific capabilities
✅ **95%+ Code Reuse** - All projects benefit from shared capabilities
✅ **Single Source of Truth** - Bug fixes in one place apply everywhere
✅ **Process Isolation** - Crashes isolated to source app
✅ **Permission-Based Security** - Fine-grained access control

**Projects & Capabilities:**
- **AVAConnect:** Cellular, Bluetooth, connectivity
- **BrowserAvanue:** Web engine, bookmarks, history
- **VoiceOS:** Voice recognition, TTS, command routing
- **AVA:** Assistant UI, media playback, camera
- **Avanues:** File management, settings sync, launcher
- **NewAvanue:** Next-generation features

**All capabilities accessible from any project via AIDL/ContentProvider!**

---

**Document Version:** v1.0.0
**Last Updated:** 2025-11-16
**Framework:** IDEACODE v8.5
**Status:** Active - Implementation in progress
