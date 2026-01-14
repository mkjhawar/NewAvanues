# IPC Methods - Avanues Ecosystem

**Inter-Process Communication Documentation**

## Overview

The Avanues ecosystem uses multiple IPC mechanisms to enable communication between:
- VoiceOS ↔ Avanue Platform Apps
- Avanue Apps ↔ Avanue Apps
- Avanue Apps ↔ External Apps

## Android IPC Methods

### 1. Intent-Based Communication

**Use Case**: Simple one-way data transfer, launching activities, broadcasting events

**Implementation**:
```kotlin
// Sending app
val intent = Intent("com.augmentalis.avanue.ACTION_TRANSFER_UI")
intent.putExtra("ui_data", serializedAvaUI)
intent.setPackage("com.augmentalis.avanue.core")
startActivity(intent)

// Receiving app (AndroidManifest.xml)
<activity android:name=".UIReceiverActivity">
    <intent-filter>
        <action android:name="com.augmentalis.avanue.ACTION_TRANSFER_UI" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

**Actions**:
- `com.augmentalis.avanue.ACTION_TRANSFER_UI` - Transfer AvaUI component
- `com.augmentalis.voiceos.ACTION_SPEAK` - Text-to-speech request
- `com.augmentalis.voiceos.ACTION_LISTEN` - Speech-to-text request

### 2. AIDL (Android Interface Definition Language)

**Use Case**: Bidirectional communication, background services, real-time data

**Implementation**:
```aidl
// IVoiceOSService.aidl
package com.augmentalis.voiceos;

interface IVoiceOSService {
    String speak(String text, int priority);
    String listen(int timeoutMs);
    boolean isVoiceEnabled();
    void registerCallback(IVoiceCallback callback);
}
```

**Services**:
- `IVoiceOSService` - Core VoiceOS accessibility service
- `IAvaUIService` - UI component transfer service
- `ICapabilityService` - Capability discovery service

### 3. ContentProvider

**Use Case**: Large data transfer, shared data access, databases

**Implementation**:
```kotlin
// Provider
class AvaUIProvider : ContentProvider() {
    companion object {
        val CONTENT_URI = Uri.parse("content://com.augmentalis.avanue.avaui")
    }

    override fun query(...): Cursor? {
        // Return AvaUI components
    }
}

// Consumer
val cursor = contentResolver.query(
    AvaUIProvider.CONTENT_URI,
    arrayOf("ui_id", "ui_data"),
    "category = ?",
    arrayOf("authentication"),
    null
)
```

**Providers**:
- `AvaUIProvider` - UI component library
- `ThemeProvider` - Theme definitions
- `AssetProvider` - Icons, images, fonts

### 4. BroadcastReceiver

**Use Case**: System-wide notifications, event broadcasting

**Implementation**:
```kotlin
// Sender
val intent = Intent("com.augmentalis.voiceos.EVENT_VOICE_STATE_CHANGED")
intent.putExtra("enabled", true)
sendBroadcast(intent, "com.augmentalis.permission.VOICE_EVENTS")

// Receiver
class VoiceStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val enabled = intent.getBooleanExtra("enabled", false)
        // Handle state change
    }
}
```

**Broadcasts**:
- `com.augmentalis.voiceos.EVENT_VOICE_STATE_CHANGED` - Voice accessibility state
- `com.augmentalis.avanue.EVENT_THEME_CHANGED` - Theme update
- `com.augmentalis.avanue.EVENT_CAPABILITY_REGISTERED` - New capability available

### 5. Messenger (Handler-based)

**Use Case**: Lightweight message passing, simple request/response

**Implementation**:
```kotlin
// Service
class MessengerService : Service() {
    private val messenger = Messenger(IncomingHandler())

    override fun onBind(intent: Intent): IBinder = messenger.binder

    private class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_REQUEST_UI -> handleUIRequest(msg)
            }
        }
    }
}

// Client
val service = Messenger(binder)
val msg = Message.obtain(null, MSG_REQUEST_UI)
msg.replyTo = messenger
service.send(msg)
```

## iOS IPC Methods

### 1. URL Schemes

**Use Case**: App launching, deep linking, simple data transfer

**Implementation**:
```swift
// Receiving app (Info.plist)
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>voiceos</string>
        </array>
    </dict>
</array>

// Sending app
if let url = URL(string: "voiceos://speak?text=Hello") {
    UIApplication.shared.open(url)
}

// Receiving app (SceneDelegate)
func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
    guard let url = URLContexts.first?.url else { return }
    // Handle URL
}
```

**Schemes**:
- `voiceos://` - VoiceOS actions
- `avanue://` - Avanue platform actions
- `avaui://` - UI component transfer

### 2. App Groups

**Use Case**: Shared data storage, file sharing between apps

**Implementation**:
```swift
// Enable App Group in capabilities: group.com.augmentalis.avanue

// Writing data
let defaults = UserDefaults(suiteName: "group.com.augmentalis.avanue")
defaults?.set(magicUIData, forKey: "shared_ui")

// Reading data
let defaults = UserDefaults(suiteName: "group.com.augmentalis.avanue")
let uiData = defaults?.data(forKey: "shared_ui")
```

### 3. Universal Links

**Use Case**: Web-to-app, seamless transitions, deep linking

**Implementation**:
```swift
// Associated Domains: applinks:avanue.augmentalis.com

func application(_ application: UIApplication, continue userActivity: NSUserActivity, restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
    guard userActivity.activityType == NSUserActivityTypeBrowsingWeb,
          let url = userActivity.webpageURL else {
        return false
    }
    // Handle universal link
    return true
}
```

## Cross-Platform IPC Strategy

### Capability Discovery

All apps register capabilities in a manifest:

```json
{
  "app_id": "com.augmentalis.aiavanue",
  "capabilities": [
    {
      "name": "text_generation",
      "version": "1.0",
      "android_ipc": {
        "method": "aidl",
        "service": "com.augmentalis.aianue.AIService"
      },
      "ios_ipc": {
        "method": "url_scheme",
        "scheme": "aiavanue://ai/"
      }
    }
  ]
}
```

### VoiceOSBridge

Unified API across platforms:

```kotlin
// commonMain
expect class VoiceOSBridge {
    suspend fun speak(text: String): Result<Unit>
    suspend fun listen(): Result<String>
    fun isAvailable(): Boolean
}

// androidMain
actual class VoiceOSBridge {
    actual suspend fun speak(text: String): Result<Unit> {
        // Use AIDL
    }
}

// iosMain
actual class VoiceOSBridge {
    actual suspend fun speak(text: String): Result<Unit> {
        // Use URL scheme
    }
}
```

## Security & Permissions

### Android Permissions

```xml
<!-- Custom permissions for IPC -->
<permission
    android:name="com.augmentalis.permission.VOICE_EVENTS"
    android:protectionLevel="signature" />

<permission
    android:name="com.augmentalis.permission.UI_TRANSFER"
    android:protectionLevel="normal" />

<!-- Declare usage -->
<uses-permission android:name="com.augmentalis.permission.VOICE_EVENTS" />
```

### iOS Entitlements

```xml
<key>com.apple.security.application-groups</key>
<array>
    <string>group.com.augmentalis.avanue</string>
</array>
```

## Best Practices

1. **Version Compatibility**: Always check API version before IPC
2. **Error Handling**: Handle missing apps/services gracefully
3. **Data Size Limits**:
   - Intent extras: ~500KB
   - AIDL: ~1MB per transaction
   - ContentProvider: unlimited (cursor-based)
4. **Timeout Handling**: Set appropriate timeouts for synchronous calls
5. **Security**: Use signature-level permissions for sensitive operations

## Testing IPC

### Android
```kotlin
@Test
fun testVoiceOSIPCConnection() {
    val intent = Intent("com.augmentalis.voiceos.ACTION_SPEAK")
    val resolveInfo = context.packageManager.resolveService(intent, 0)
    assertNotNull(resolveInfo)
}
```

### iOS
```swift
func testURLSchemeAvailable() {
    let url = URL(string: "voiceos://test")!
    XCTAssertTrue(UIApplication.shared.canOpenURL(url))
}
```

---

**Last Updated:** 2025-11-09
**IDEACODE Version:** 5.0
**Created by Manoj Jhawar, manoj@ideahq.net**
