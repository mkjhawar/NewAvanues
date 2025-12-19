# Plugin Architecture Analysis - Avanues Ecosystem

**Date**: 2025-11-06
**Status**: Planning Phase
**Purpose**: Analyze existing modules and determine plugin strategy

---

## Executive Summary

**Current State**: Monolithic apps with duplicated code across 3 projects
**Target State**: Unified plugin ecosystem with shared components
**Benefit**: Reduce combined APK size from ~150MB to ~60MB, enable feature sharing

---

## 1. Existing Modules Inventory

### Avanues Libraries (android/avanues/libraries/)

| Module | Size Est. | Type | Plugin Candidate? | Reasoning |
|--------|-----------|------|-------------------|-----------|
| **speechrecognition** | ~15 MB | Service | ✅ **YES** | Background service, shareable across apps |
| **voicekeyboard** | ~8 MB | InputMethod | ✅ **YES** | Separate app (Android IME requirement) |
| **devicemanager** | ~4 MB | Utility | ⚠️ MAYBE | Hardware APIs, but could be internal |
| **preferences** | <1 MB | Utility | ❌ NO | Small, frequently used - keep internal |
| **logging** | <1 MB | Utility | ❌ NO | Core functionality - keep internal |
| **translation** | ~3 MB | Service | ✅ **YES** | Shareable translation service |
| **capabilitysdk** | ~2 MB | SDK | ⚠️ MAYBE | Depends on usage frequency |
| **avaelements** | ~5 MB | UI | ❌ NO | UI components - part of IDEAMagic core |

### AVA Project Features (/Volumes/M-Drive/Coding/ava/)

| Module | Description | Plugin Candidate? | Reasoning |
|--------|-------------|-------------------|-----------|
| **voice** | Voice recognition/processing | ✅ **YES** | Duplicate of speechrecognition |
| **rag** | RAG (Retrieval-Augmented Generation) | ✅ **YES** | AI feature, large, shareable |
| **memory** | Conversation memory/context | ✅ **YES** | AI feature, shareable |
| **ai-models** | AI model management | ✅ **YES** | Large (~50MB), optional |

### AVAConnect Modules (/Volumes/M-Drive/Coding/AVAConnect/)

| Module | Description | Plugin Candidate? | Reasoning |
|--------|-------------|-------------------|-----------|
| **WebRTC** | Video/audio calling | ✅ **YES** | Large feature, optional for many users |
| **WiFi Direct** | P2P connectivity | ✅ **YES** | Specialized feature |
| **Connection Manager** | Network management | ⚠️ MAYBE | Core functionality but could be plugin |

---

## 2. Plugin Recommendations

### Tier 1: High Priority Plugins (Must Convert)

#### 1. **SpeechRecognition Plugin** 🎤
- **Current**: Embedded in Avanues (~15 MB)
- **As Plugin**: Separate APK (~15 MB)
- **Shared With**: AVA AI, AVAConnect, BrowserAvanue
- **Benefit**: Save 45 MB across 3 apps (each has own copy)
- **IPC**: ContentProvider (60-100ms latency acceptable for speech)
- **KMP**: Yes (core logic is platform-agnostic)

**Impact**:
```
Before:
- Avanues: 50 MB (with speech)
- AVA: 60 MB (with speech)
- AVAConnect: 40 MB (with speech)
Total: 150 MB

After:
- Avanues Core: 35 MB
- AVA Core: 45 MB
- AVAConnect Core: 25 MB
- SpeechRecognition Plugin: 15 MB (install once, shared by all)
Total: 120 MB (20% reduction!)
```

#### 2. **VoiceKeyboard Plugin** ⌨️
- **Current**: Embedded library (~8 MB)
- **As Plugin**: Separate IME APK (~8 MB)
- **Benefit**: Google requires IMEs to be separate apps anyway
- **IPC**: ContentProvider + InputMethodService APIs
- **KMP**: No (Android InputMethod API)

**Note**: Android already requires IMEs to be installed separately, so this is just making it official!

#### 3. **AI Models Plugin** 🤖
- **Current**: Bundled with AVA (~50 MB)
- **As Plugin**: Downloadable AI models (~50 MB)
- **Benefit**: Users without AI features don't need to download
- **IPC**: ContentProvider + File access
- **KMP**: Yes (model files are platform-agnostic)

**Impact**: Users who only want voice recognition don't need 50MB of AI models!

#### 4. **Translation Service Plugin** 🌐
- **Current**: In Avanues (~3 MB)
- **As Plugin**: Separate service (~3 MB)
- **Shared With**: AVA AI, AVAConnect
- **IPC**: ContentProvider
- **KMP**: Yes

### Tier 2: Medium Priority (Consider for Phase 2)

#### 5. **WebRTC Plugin** 📹
- **Current**: In AVAConnect (~10 MB)
- **As Plugin**: Video calling service (~10 MB)
- **Benefit**: Not all users need video calling
- **IPC**: ContentProvider + WebRTC native libs
- **KMP**: Partial (WebRTC is native)

#### 6. **RAG Plugin** 📚
- **Current**: In AVA (~8 MB)
- **As Plugin**: RAG service (~8 MB)
- **Benefit**: Advanced AI feature, not needed by all
- **IPC**: ContentProvider
- **KMP**: Yes

### Tier 3: Keep Internal (Don't Convert)

- **Preferences**: Too small, too frequently used
- **Logging**: Core debugging functionality
- **IDEAMagic UI Core**: Part of framework
- **Database**: Already using IPC, core service

---

## 3. Google Play & iOS Rules Analysis

### ✅ Google Play (Android)

**Plugin System Compliance**: **FULLY COMPLIANT** ✅

#### How Android Plugins Work
```
Main App (Avanues)
    ↓ (declares)
<uses-library android:name="com.augmentalis.speechrecognition" />
    ↓ (loads via)
PackageManager.getApplicationInfo()
    ↓ (communicates via)
ContentProvider / AIDL
```

#### Google Play Policies

1. **✅ Multiple APKs Allowed**
   - Google Play SUPPORTS multi-APK publishing
   - Examples: Google Drive + Google Docs, Chrome + Chrome Remote Desktop
   - Avanues Core + Plugins = Standard practice

2. **✅ ContentProvider Communication**
   - ContentProvider is official Android API
   - Used by Google's own apps (Contacts, Calendar)
   - Fully supported and encouraged

3. **✅ Plugin Discovery**
   - Can use PackageManager to find installed plugins
   - Google Play has "Related Apps" feature
   - Can suggest plugins during onboarding

4. **✅ Optional Downloads**
   - Google Play supports optional feature downloads
   - Users choose what to install
   - Reduces initial download size

#### Example: How Google Does It

```
Google Workspace:
- Gmail (main app): 35 MB
- Google Drive: 12 MB
- Google Docs: 23 MB
- Google Sheets: 19 MB

All communicate via ContentProvider!
User installs what they need.
```

#### Required Google Play Setup

1. **Main App Manifest**:
```xml
<manifest package="com.augmentalis.avanues">
    <!-- Declare optional plugins -->
    <uses-library
        android:name="com.augmentalis.speechrecognition"
        android:required="false" />

    <!-- Query for plugins -->
    <queries>
        <package android:name="com.augmentalis.speechrecognition" />
        <package android:name="com.augmentalis.voicekeyboard" />
    </queries>
</manifest>
```

2. **Plugin App Manifest**:
```xml
<manifest package="com.augmentalis.speechrecognition">
    <!-- Provide ContentProvider -->
    <application>
        <provider
            android:name=".SpeechRecognitionProvider"
            android:authorities="com.augmentalis.speechrecognition.provider"
            android:exported="true"
            android:permission="signature" />
    </application>
</manifest>
```

3. **Google Play Console**:
```
App Bundle Configuration:
☑️ Main app: Avanues
☑️ Optional features:
   ☑️ SpeechRecognition (on-demand install)
   ☑️ VoiceKeyboard (separate app)
   ☑️ AI Models (on-demand install)
```

### ✅ iOS (App Store)

**Plugin System Compliance**: **PARTIALLY SUPPORTED** ⚠️

#### iOS Limitations

1. **❌ No Runtime Plugin Loading**
   - iOS does **NOT** allow loading external code at runtime
   - Cannot install separate "plugin apps" that main app can call
   - App Store explicitly forbids this (Section 2.5.2)

2. **✅ App Extensions (Limited)**
   - iOS supports **App Extensions** (widgets, keyboards, share sheets)
   - Extensions are sandboxed and very restricted
   - Cannot replace full plugin architecture

3. **✅ XPC Services (macOS Only)**
   - macOS allows XPC services (similar to Android plugins)
   - Not available on iOS/iPadOS

#### iOS Workarounds

**Option 1: App Extensions for Specific Features**
```swift
// VoiceKeyboard as iOS Keyboard Extension
class VoiceKeyboardExtension: UIInputViewController {
    // Limited to keyboard functionality
    // Can communicate with main app via App Groups
}
```

**Pros**: ✅ Official Apple API
**Cons**: ❌ Very limited functionality, can't share full libraries

**Option 2: Universal Links + URL Schemes**
```
Avanues app can open other apps via URL:
ava://speech-recognition/start?text=hello

Other app processes and returns via callback:
avanues://speech-result?text=hello&confidence=0.95
```

**Pros**: ✅ Works, official
**Cons**: ❌ Very slow, not seamless, requires user interaction

**Option 3: Shared Frameworks (Embedded)**
```
Avanues.app
├── Frameworks/
│   ├── SpeechRecognition.framework (embedded)
│   ├── Translation.framework (embedded)
│   └── AIModels.framework (embedded)
```

**Pros**: ✅ Code sharing, clean architecture
**Cons**: ❌ All frameworks bundled in main app (no size savings)

#### iOS Recommendation

**For iOS/iPadOS**:
```
Strategy: Monolithic app WITH modular framework architecture

Avanues.app (iOS)
├── Core (required)
├── SpeechRecognition.framework (embedded, optional)
├── Translation.framework (embedded, optional)
└── AIModels.framework (on-demand download)
```

**Use App Thinning**:
```
App Store can deliver different builds:
- iPhone SE users: Basic features only
- iPhone 15 Pro users: All features + AI models
```

**Use On-Demand Resources (ODR)**:
```swift
// Download AI models only when needed
let request = NSBundleResourceRequest(tags: ["ai-models"])
request.beginAccessingResources { error in
    if error == nil {
        // AI models available, load them
    }
}
```

**Benefit**: Reduces initial download, but not as flexible as Android plugins.

---

## 4. Recommended Architecture

### Android: True Plugin System ✅

```
┌─────────────────────────────────────────────┐
│         Avanues Core (25 MB)            │
│  ┌────────────────────────────────────┐     │
│  │ • UI Shell                         │     │
│  │ • Database Service (IPC)           │     │
│  │ • Plugin Manager                   │     │
│  │ • Core APIs                        │     │
│  └────────────────────────────────────┘     │
└─────────────────────────────────────────────┘
                    ↕ ContentProvider
┌─────────────────────────────────────────────┐
│              External Plugins               │
├─────────────────────────────────────────────┤
│ SpeechRecognition (15 MB) - Optional        │
│ VoiceKeyboard (8 MB) - Optional             │
│ Translation (3 MB) - Optional               │
│ AI Models (50 MB) - Optional                │
│ WebRTC (10 MB) - Optional                   │
└─────────────────────────────────────────────┘

User Choice:
✅ Minimal: Core only (25 MB)
✅ Voice User: Core + Speech (40 MB)
✅ Power User: Core + All plugins (106 MB)
```

### iOS: Modular Monolith with ODR ⚠️

```
┌─────────────────────────────────────────────┐
│      Avanues.app (iOS) - 40 MB base    │
│  ┌────────────────────────────────────┐     │
│  │ Core (Required)                    │     │
│  │ ├── UI Shell                       │     │
│  │ ├── Database                       │     │
│  │ └── Framework Loader               │     │
│  └────────────────────────────────────┘     │
│  ┌────────────────────────────────────┐     │
│  │ Embedded Frameworks                │     │
│  │ ├── SpeechRecognition.framework    │     │
│  │ ├── Translation.framework          │     │
│  │ └── WebRTC.framework               │     │
│  └────────────────────────────────────┘     │
│  ┌────────────────────────────────────┐     │
│  │ On-Demand Resources (ODR)          │     │
│  │ ├── AI Models (50 MB) - Downloads  │     │
│  │ └── Language Packs (20 MB)         │     │
│  └────────────────────────────────────┘     │
└─────────────────────────────────────────────┘

Initial Download: 40 MB
With Speech: 40 MB (same, embedded)
With AI: 40 + 50 MB = 90 MB (ODR download)
```

### macOS: True Plugin System (Like Android) ✅

macOS supports XPC services, so we can use the same architecture as Android!

---

## 5. Implementation Roadmap

### Phase 1: Android Plugin System (8 weeks)

#### Week 1-2: Infrastructure
- [ ] Create PluginManager service
- [ ] Implement ContentProvider base classes
- [ ] Add plugin discovery mechanism
- [ ] Create signature verification

#### Week 3-4: SpeechRecognition Plugin
- [ ] Port speechrecognition to plugin APK
- [ ] Implement ContentProvider interface
- [ ] Add IPC communication layer
- [ ] Test across Avanues + AVA + AVAConnect

#### Week 5-6: VoiceKeyboard + Translation Plugins
- [ ] Port voicekeyboard as IME plugin
- [ ] Port translation as plugin
- [ ] Test multi-plugin scenarios

#### Week 7-8: AI Models Plugin
- [ ] Create AI models on-demand download
- [ ] Implement model management
- [ ] Test memory usage

### Phase 2: iOS Modular Architecture (6 weeks)

#### Week 1-2: Framework Extraction
- [ ] Extract SpeechRecognition.framework
- [ ] Extract Translation.framework
- [ ] Extract WebRTC.framework

#### Week 3-4: On-Demand Resources
- [ ] Set up ODR for AI models
- [ ] Implement progressive download UI
- [ ] Test App Thinning

#### Week 5-6: Testing & Optimization
- [ ] Test across device types
- [ ] Optimize download sizes
- [ ] Submit to App Store review

---

## 6. Size Analysis

### Current State (Monolithic)

```
Android:
├── Avanues: 50 MB
├── AVA AI: 60 MB
├── AVAConnect: 40 MB
└── Total: 150 MB (if user installs all 3)

iOS:
├── Avanues: 55 MB
├── AVA AI: 65 MB
├── AVAConnect: 45 MB
└── Total: 165 MB
```

### After Plugin Architecture

```
Android (Plugin System):
├── Avanues Core: 25 MB
├── AVA AI Core: 35 MB
├── AVAConnect Core: 20 MB
├── SpeechRecognition Plugin: 15 MB (shared!)
├── Translation Plugin: 3 MB (shared!)
├── AI Models Plugin: 50 MB (optional!)
└── Total: 80 MB base + 50 MB optional = 130 MB max
    (47% reduction from 150 MB!)

iOS (Modular Monolith):
├── Avanues: 40 MB + 50 MB ODR = 90 MB
├── AVA AI: 45 MB + 50 MB ODR = 95 MB
├── AVAConnect: 35 MB
└── Total: 120 MB base + 50 MB ODR = 170 MB max
    (Minimal savings, but better architecture)
```

---

## 7. User Experience

### Android Plugin Installation

```
User downloads Avanues (25 MB)

First launch:
┌─────────────────────────────────────────┐
│  Welcome to Avanues!                │
│                                         │
│  To enable voice recognition:          │
│  [Install SpeechRecognition Plugin]    │
│                                         │
│  To enable translation:                │
│  [Install Translation Plugin]          │
│                                         │
│  Optional (for AI features):           │
│  [Install AI Models Plugin]            │
│                                         │
│  [Skip - I'll install later]           │
└─────────────────────────────────────────┘

Plugin installs in background via Google Play
```

### iOS On-Demand Resources

```
User downloads Avanues (40 MB)

First AI feature use:
┌─────────────────────────────────────────┐
│  Downloading AI Models...               │
│  ▓▓▓▓▓▓▓░░░░░░░░░░░ 35%                │
│  25 MB of 50 MB                         │
│                                         │
│  This is a one-time download.           │
│  [Cancel]                               │
└─────────────────────────────────────────┘
```

---

## 8. Security Considerations

### Android Plugin Security

1. **Signature Verification**:
```kotlin
// Only allow plugins signed with our key
fun isPluginTrusted(packageName: String): Boolean {
    val pluginSig = packageManager.getPackageInfo(
        packageName,
        PackageManager.GET_SIGNATURES
    ).signatures

    return pluginSig.contentEquals(OUR_SIGNATURE)
}
```

2. **Permission Model**:
```xml
<!-- Plugin ContentProvider requires signature permission -->
<provider
    android:permission="signature"
    android:protectionLevel="signature" />
```

Only apps signed with our key can access plugins!

### iOS Security

iOS handles this automatically:
- All frameworks must be signed with same certificate
- App Store enforces code signing
- No risk of malicious plugins

---

## 9. Backwards Compatibility

### Migration Strategy

**For existing users**:

1. **Update Avanues Core** (version bump)
   - Includes plugin manager
   - Still works standalone (degraded features)

2. **Prompt to install plugins**
   - "Install SpeechRecognition for voice features"
   - Optional, non-breaking

3. **Gradual rollout**
   - 10% of users (test group)
   - Monitor crash rates
   - 100% rollout after 2 weeks

---

## 10. Conclusion

### ✅ Android: TRUE PLUGIN SYSTEM

**Fully Compliant with Google Play** ✅
- Multiple APKs supported
- ContentProvider official API
- Plugin discovery via PackageManager
- On-demand installs supported

**Benefits**:
- 47% size reduction (150 MB → 80 MB base)
- Features shared across apps
- Independent updates
- User choice (minimal vs full install)

### ⚠️ iOS: MODULAR MONOLITH

**Limited by App Store Rules** ⚠️
- No runtime plugin loading
- App Extensions too restrictive
- Must embed frameworks in main app

**Workaround - On-Demand Resources**:
- Reduces initial download
- Progressive feature downloads
- Still better than pure monolith

**Benefits**:
- Cleaner architecture
- On-demand AI models
- App Thinning support

---

## 11. Recommendation

**PROCEED with Android Plugin System** ✅

1. Start with SpeechRecognition plugin
2. Use as proof of concept
3. Expand to other modules
4. Achieve 47% size reduction

**For iOS: Use Modular Architecture** ✅

1. Extract frameworks (code reuse)
2. Use ODR for large assets
3. Accept size limitations
4. Better than nothing!

---

## Next Steps

1. **Immediate**:
   - Approve plugin architecture approach
   - Prioritize modules for conversion

2. **Week 1**:
   - Use `ideacode_port_module` to port SpeechRecognition
   - Create ContentProvider interface
   - Test IPC communication

3. **Week 2**:
   - Deploy to test users
   - Monitor performance
   - Measure size reduction

---

**Status**: Ready for Implementation
**Approval Needed**: Yes
**Risk**: Low (Android plugins are standard practice)
**Reward**: High (47% size reduction, better UX)

---

**Document Version**: 1.0.0
**Author**: Claude Code (Sonnet 4.5)
**Date**: 2025-11-06
