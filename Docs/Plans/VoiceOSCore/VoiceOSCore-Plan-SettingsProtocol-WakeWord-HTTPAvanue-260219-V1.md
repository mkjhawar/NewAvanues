# Plan: Self-Announcing Settings Protocol + Wake-Word + HTTPAvanue
**Date:** 2026-02-19 | **Version:** V1

---

## Part 1: Consistent Self-Announcing Settings Protocol

### Problem
Each module that contributes settings to the UnifiedSettingsScreen does so via `ISettingsProvider` + `@IntoSet` Hilt injection. But the protocol isn't formalized — modules can register arbitrary settings without consistent patterns for:
- Setting categories and ordering
- Data binding (read/write/observe)
- Cross-device sync (settings from GlassClient receiver need to appear on phone)
- Module self-announcement (what settings does this module provide?)

### Existing Pattern (Chapter 90)
```kotlin
// Current: each module creates a SettingsProvider
@Provides @IntoSet @JvmSuppressWildcards
fun provideMyModuleSettings(): ISettingsProvider = MyModuleSettingsProvider()

// Provider returns SettingsSection with items
class MyModuleSettingsProvider : ISettingsProvider {
    override val priority: Int = 300
    override fun getSections(): List<SettingsSection> { ... }
}
```

### Proposed: Formal Self-Announcing Protocol

Each module declares a `ModuleSettingsManifest` in commonMain that describes ALL settings it provides, their types, defaults, and sync behavior:

```kotlin
// commonMain — Foundation module
@Serializable
data class ModuleSettingsManifest(
    val moduleId: String,             // "voiceoscore", "remotecast", "glassavanue"
    val moduleName: String,           // "VoiceOS", "RemoteCast", "GlassAvanue"
    val version: Int,                 // Schema version for migration
    val settings: List<SettingDefinition>
)

@Serializable
data class SettingDefinition(
    val key: String,                  // "wake_word_enabled"
    val type: SettingType,            // BOOLEAN, STRING, INT, FLOAT, ENUM, COLOR
    val category: String,             // "Voice", "Display", "Connection", "Battery"
    val label: String,                // "Enable Wake Word"
    val description: String,          // "Listen for wake word before processing commands"
    val defaultValue: String,         // "true" (serialized)
    val options: List<String>?,       // For ENUM: ["off", "hey_voiceos", "custom"]
    val syncPolicy: SyncPolicy,       // LOCAL_ONLY, SYNC_TO_RECEIVER, SYNC_TO_SENDER
    val restartRequired: Boolean,     // Does changing this need service restart?
    val voicePhrase: String?          // "Voice: toggle wake word" (AVID for settings screen)
)

enum class SettingType { BOOLEAN, STRING, INT, FLOAT, ENUM, COLOR }
enum class SyncPolicy {
    LOCAL_ONLY,       // Only on this device
    SYNC_TO_RECEIVER, // Phone → glasses (quality, stream mode)
    SYNC_TO_SENDER,   // Glasses → phone (voice sensitivity, wake word)
    BIDIRECTIONAL     // Both directions (shared prefs)
}
```

### Module Registration (KMP commonMain)
```kotlin
// Each module declares its manifest as an object
object VoiceOSCoreSettings {
    val manifest = ModuleSettingsManifest(
        moduleId = "voiceoscore",
        moduleName = "VoiceOS",
        version = 1,
        settings = listOf(
            SettingDefinition(
                key = "wake_word_enabled", type = BOOLEAN,
                category = "Voice", label = "Enable Wake Word",
                defaultValue = "false", syncPolicy = BIDIRECTIONAL,
                voicePhrase = "Voice: toggle wake word"
            ),
            SettingDefinition(
                key = "wake_word_phrase", type = ENUM,
                category = "Voice", label = "Wake Word",
                defaultValue = "hey_voiceos",
                options = listOf("hey_voiceos", "hey_avanue", "custom"),
                syncPolicy = BIDIRECTIONAL
            ),
            SettingDefinition(
                key = "voice_confidence_threshold", type = FLOAT,
                category = "Voice", label = "Confidence Threshold",
                defaultValue = "0.7", syncPolicy = LOCAL_ONLY
            ),
            // ... more settings
        )
    )
}

object RemoteCastSettings {
    val manifest = ModuleSettingsManifest(
        moduleId = "remotecast",
        moduleName = "RemoteCast",
        version = 1,
        settings = listOf(
            SettingDefinition(
                key = "cast_quality", type = ENUM,
                category = "Connection", label = "Stream Quality",
                defaultValue = "HD", options = listOf("SD", "HD", "FHD"),
                syncPolicy = SYNC_TO_RECEIVER
            ),
            SettingDefinition(
                key = "cast_mode", type = ENUM,
                category = "Connection", label = "Streaming Mode",
                defaultValue = "auto", options = listOf("auto", "mjpeg", "webrtc"),
                syncPolicy = LOCAL_ONLY
            ),
        )
    )
}
```

### Auto-Generated Settings UI
The `UnifiedSettingsScreen` reads all manifests and auto-generates the UI:
```kotlin
@Composable
fun UnifiedSettingsScreen(manifests: List<ModuleSettingsManifest>) {
    // Group by category across ALL modules
    val byCategory = manifests.flatMap { m -> m.settings.map { s -> s.category to (m to s) } }
        .groupBy { it.first }

    // Render: Voice section (from VoiceOSCore + GlassAvanue)
    //         Connection section (from RemoteCast)
    //         Display section (from AvanueUI + HUDManager)
    //         Battery section (from VoiceOSCore)
}
```

### Receiver Settings Integration
When GlassClient connects to GlassAvanue:
1. Phone sends all manifests with `syncPolicy = SYNC_TO_RECEIVER`
2. Glasses display those settings in their local settings screen
3. User changes setting on glasses → change relayed via CMD packet → phone applies
4. Phone sends all manifests with `syncPolicy = SYNC_TO_SENDER`
5. Glasses settings (like wake word choice) sync to phone for backup

Protocol: new message type `SET\0` (settings sync):
```
SET\0 header + JSON payload: {moduleId, key, value}
```

### Files to Create/Modify
| File | Location | Action |
|------|----------|--------|
| ModuleSettingsManifest.kt | Foundation commonMain | NEW — manifest data classes |
| SettingDefinition.kt | Foundation commonMain | NEW — setting definition types |
| VoiceOSCoreSettings.kt | VoiceOSCore commonMain | NEW — VoiceOS settings manifest |
| RemoteCastSettings.kt | RemoteCast commonMain | NEW — RemoteCast settings manifest |
| HUDManagerSettings.kt | HUDManager (future module) | NEW — HUD settings manifest |
| GlassAvanueSettings.kt | GlassAvanue app | NEW — app-specific settings |
| ManifestSettingsProvider.kt | App androidMain | NEW — auto-generates ISettingsProvider from manifests |
| SettingsSyncManager.kt | RemoteCast commonMain | NEW — sync settings via SET\0 protocol |

---

## Part 2: Wake-Word Implementation (KMP)

### Architecture
```
commonMain:
  IWakeWordEngine (interface)
  WakeWordSettings (data class)
  WakeWordState (sealed class: Idle/Listening/Detected/Error)

androidMain:
  VivokaWakeWordEngine (uses Vivoka wake-word API)
  AndroidSpeechWakeWordEngine (fallback: Android SpeechRecognizer hotword)

iosMain:
  AppleWakeWordEngine (Apple Speech framework keyword detection)

desktopMain:
  VoskWakeWordEngine (Vosk open-source keyword spotting)
```

### KMP Interface (commonMain)
```kotlin
interface IWakeWordEngine {
    val state: StateFlow<WakeWordState>
    val availableWakeWords: List<String>
    fun startListening(wakeWord: String)
    fun stopListening()
    fun addCustomWakeWord(phrase: String): Boolean
}

sealed class WakeWordState {
    object Idle : WakeWordState()
    data class Listening(val wakeWord: String) : WakeWordState()
    data class Detected(val wakeWord: String, val confidence: Float) : WakeWordState()
    data class Error(val message: String) : WakeWordState()
}

data class WakeWordSettings(
    val enabled: Boolean = false,
    val wakeWord: String = "hey voiceos",  // default
    val customWakeWords: List<String> = emptyList(),
    val sensitivity: Float = 0.5f  // 0.0 (least sensitive) to 1.0 (most)
)
```

### Settings Provider
```kotlin
object WakeWordSettingsManifest {
    val manifest = ModuleSettingsManifest(
        moduleId = "wakeword",
        moduleName = "Wake Word",
        version = 1,
        settings = listOf(
            SettingDefinition(key = "wake_word_enabled", type = BOOLEAN,
                category = "Voice", label = "Enable Wake Word",
                description = "Low-power listening for wake word. Saves battery when not actively commanding.",
                defaultValue = "false", syncPolicy = BIDIRECTIONAL),
            SettingDefinition(key = "wake_word_phrase", type = ENUM,
                category = "Voice", label = "Wake Word Phrase",
                defaultValue = "hey_voiceos",
                options = listOf("hey_voiceos", "hey_avanue", "ok_glasses", "custom"),
                syncPolicy = BIDIRECTIONAL),
            SettingDefinition(key = "wake_word_custom", type = STRING,
                category = "Voice", label = "Custom Wake Word",
                defaultValue = "", syncPolicy = BIDIRECTIONAL),
            SettingDefinition(key = "wake_word_sensitivity", type = FLOAT,
                category = "Voice", label = "Wake Word Sensitivity",
                description = "Higher = more responsive but more false positives",
                defaultValue = "0.5", syncPolicy = LOCAL_ONLY),
        )
    )
}
```

### Integration with SpeechEngineManager
```
Normal flow (no wake word):
  Mic ON → Vivoka full grammar → continuous recognition → process commands

Wake word flow:
  Mic ON → WakeWordEngine (low-power hotword) → DETECTED! →
  → Switch to Vivoka full grammar → recognize command → process →
  → Timeout (3-5 sec no speech) → back to WakeWordEngine
```

Battery impact: ~10%/hr → ~2-3%/hr during idle (wake-word mode uses ~10x less CPU than full grammar).

### Files to Create/Modify
| File | Location | Action |
|------|----------|--------|
| IWakeWordEngine.kt | VoiceOSCore commonMain | NEW — KMP interface |
| WakeWordState.kt | VoiceOSCore commonMain | NEW — state sealed class |
| WakeWordSettings.kt | VoiceOSCore commonMain | NEW — settings data class |
| VivokaWakeWordEngine.kt | VoiceOSCore androidMain | NEW — Vivoka integration |
| SpeechEngineManager.kt | VoiceOSCore commonMain | MODIFY — add wake-word gating |
| VivokaAndroidEngine.kt | VoiceOSCore androidMain | MODIFY — wire enableWakeWord() |

---

## Part 3: HTTPAvanue Discussion (See Below)

### Current HTTP Usage in Codebase
| Usage | Current Library | Size | Purpose |
|-------|----------------|------|---------|
| Cloud API calls (AI/LLM) | Ktor Client 2.3.7 | ~3-5 MB | HTTP client for Groq/OpenAI/Claude |
| License validation | Ktor Client | (shared) | HTTPS to license server |
| VOS SFTP sync | JSch 0.2.16 | ~1 MB | SFTP file transfer |
| RemoteCast MJPEG | Raw ServerSocket | 0 KB | Custom TCP protocol |
| Screen share (legacy) | NanoHTTPD (Java) | 117 KB | HTTPS + WebSocket server |

### HTTPAvanue Proposal
Rewrite NanoHTTPD as a modern Kotlin HTTP server module.
Replace Ktor Client with HTTPAvanue Client (optional, bigger scope).

**To be discussed: viability, scope, HTTP/2 necessity.**

---

## Execution Order

| Step | What | Est. |
|------|------|------|
| 1 | ModuleSettingsManifest + SettingDefinition (Foundation) | 2 files |
| 2 | VoiceOSCoreSettings + RemoteCastSettings manifests | 2 files |
| 3 | ManifestSettingsProvider (auto-UI from manifests) | 1 file |
| 4 | IWakeWordEngine + WakeWordState (KMP commonMain) | 2 files |
| 5 | VivokaWakeWordEngine (androidMain) | 1 file |
| 6 | Wire into SpeechEngineManager (gating logic) | 1 file modify |
| 7 | SettingsSyncManager (SET\0 protocol for receiver) | 1 file |
| 8 | HTTPAvanue (if decided) | 3-5 files |
