# VOS4 Naming Conventions

**File:** NAMING-CONVENTIONS.md
**Created:** 2025-09-03 17:30
**Purpose:** Clear naming rules to avoid redundancy and improve navigation

---

## 🎯 Core Principles

1. **NO REDUNDANCY** - Never repeat context that's already in the path
2. **CLARITY** - Names should be immediately understandable
3. **BREVITY** - Shorter is better if still clear
4. **CONSISTENCY** - Same pattern everywhere

---

## ❌ BAD Examples (What NOT to do)

```
❌ /apps/VoiceAccessibility/.../voiceaccessibility/service/VoiceOSAccessibility.kt
   → "voice" and "accessibility" repeated 3 times!

❌ /libraries/SpeechRecognition/.../speechrecognition/speechengines/VivokaEngine.kt
   → "speech" repeated, "engines" redundant

❌ /apps/VoiceRecognition/.../voicerecognition/service/VoiceRecognitionService.kt
   → "voice" and "recognition" repeated 3 times!
```

---

## ✅ GOOD Examples (What TO do)

```
✅ /apps/VoiceAccessibility/.../voiceos/accessibility/VoiceOSService.kt
   → Clean, no redundancy

✅ /libraries/SpeechRecognition/.../voiceos/engines/Vivoka.kt
   → Simple, clear

✅ /apps/VoiceRecognition/.../voiceos/service/RecognitionService.kt
   → Context clear from path
```

---

## 📋 Naming Rules

### 1. Package Names
**Rule:** Use `com.augmentalis.voiceos.[module]` not `com.augmentalis.[redundantmodulename]`

**UPDATED 2025-09-03:** Package structure changed from `vos4` to `voiceos`

```kotlin
// BAD
package com.augmentalis.voiceaccessibility.service

// OLD (deprecated)
package com.augmentalis.vos4.accessibility

// GOOD (current standard)
package com.augmentalis.voiceos.accessibility
```

### 2. Service Names
**Rule:** Don't repeat "Service" or module name if it's in the path

**UPDATED 2025-09-03:** MicService renamed to VoiceOnSentry

```kotlin
// BAD
class VoiceOSAccessibilityService : AccessibilityService()
class VoiceOSForegroundService : Service()

// OLD (deprecated)
class MicService : Service()

// GOOD (current standard)
class VoiceOSService : AccessibilityService()  // We know it's accessibility
class VoiceOnSentry : Service()                // Clear purpose - guards voice access
```

### 3. Engine Names
**Rule:** Just use the provider name, not "Engine" suffix

```kotlin
// BAD
class VivokaEngine
class AndroidSTTEngine

// GOOD
class Vivoka
class AndroidSTT
```

### 4. Manager Names
**Rule:** Be specific about what's managed, avoid generic "Manager"

```kotlin
// BAD
class SpeechRecognitionManager
class ServiceManager

// GOOD
class SpeechCoordinator
class ServiceLifecycle
```

### 5. Module Names
**Rule:** Single word when possible, max two words

```
// BAD
VoiceAccessibilityModule
SpeechRecognitionLibrary

// GOOD
Accessibility
Speech
VoiceUI
```

---

## 🗂️ Recommended Structure

### Apps
```
/apps/
├── VoiceOS/           # Main app
├── VoiceUI/          # UI components app
└── Accessibility/    # Accessibility app (simplified!)
```

### Libraries
```
/libraries/
├── Speech/           # Not SpeechRecognition
├── Commands/         # Not CommandProcessing
└── Learning/         # Not LearningSystem
```

### Services
```
/[module]/services/
├── VoiceOSService.kt    # Main accessibility service
├── MicService.kt        # Foreground mic service
└── Coordinator.kt       # Service coordination
```

---

## 🔄 Migration Plan

### Current → New
**UPDATED 2025-09-03:** Latest naming changes

```
VoiceOSAccessibility → VoiceOSService
VoiceOSForegroundService → MicService → VoiceOnSentry (final)
SpeechRecognitionManager → SpeechCoordinator
VivokaEngine → Vivoka
AndroidSTTEngine → AndroidSTT
VoskEngine → Vosk
GoogleCloudEngine → GoogleCloud
WhisperEngine → Whisper

Package Migration:
com.augmentalis.vos4.* → com.augmentalis.voiceos.*
```

---

## 📏 Path Length Guidelines

**Maximum Path Depth:** Keep paths under 100 characters total

```
❌ TOO LONG (142 chars):
/Volumes/M Drive/Coding/Warp/vos4/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceaccessibility/service/VoiceOSAccessibility.kt

✅ BETTER (98 chars):
/Volumes/M Drive/Coding/Warp/vos4/apps/Accessibility/src/main/java/com/augmentalis/voiceos/VoiceOSService.kt
```

---

## 🚨 Enforcement

1. **Review** - Check names before creating files
2. **Refactor** - Fix redundant names immediately
3. **Document** - Update when patterns emerge
4. **Automate** - Consider linting rules

---

## 💡 Quick Decision Tree

When naming something new:

1. **Is the context already in the path?** → Don't repeat it
2. **Can it be one word?** → Use one word
3. **Is the type obvious?** → Don't add suffix
4. **Will someone understand it?** → If no, add minimal context

---

**Remember:** Every character in a path should add value. If it doesn't, remove it.