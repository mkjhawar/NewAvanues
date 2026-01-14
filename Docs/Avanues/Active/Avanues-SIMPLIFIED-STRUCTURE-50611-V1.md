# AVANUES Simplified Structure

**Date**: 2025-11-06
**Status**: 🔵 REVISED PROPOSAL
**Version**: 2.0.0

---

## Key Changes from v1.0.0

### User Feedback Incorporated

1. ✅ **VOS4 → VoiceOS**: Rename to VoiceOS (clearer branding)
2. ✅ **No NLU module**: AVA already handles NLU, defer decision on moving it
3. ✅ **Simplified structure**: `/AVANUES/modules/` instead of `/Universal/IDEAMagic/`

---

## Proposed Structure (Simplified)

### Before (Current - Overly Nested)
```
Avanues/
├── Universal/
│   └── IDEAMagic/              ❌ Unnecessary nesting
│       ├── AvaUI/
│       ├── AvaCode/
│       ├── Database/
│       └── VoiceOSBridge/
├── android/
├── ios/
└── desktop/
```

### After (AVANUES - Clean)
```
AVANUES/
├── modules/                     ✅ Cross-platform (KMP) modules
│   ├── AvaUI/                # UI framework
│   ├── AvaCode/              # Forms & Workflows DSL
│   ├── MagicData/              # Database system
│   ├── VoiceOS/                # Voice OS components
│   ├── Templates/              # App generation
│   ├── Plugins/                # Plugin infrastructure (future)
│   └── IPC/                    # IPC layer (future)
│
├── android/                     # Android platform code
│   ├── app/                    # Main AVANUES Android app
│   ├── avanues/
│   │   ├── libraries/          # Android-specific libraries
│   │   └── modules/            # Internal modules
│   └── plugins/                # External plugins (separate apps)
│
├── ios/                         # iOS platform code
│   ├── AVANUES/                # Main AVANUES iOS app
│   ├── Extensions/             # App Extensions
│   └── Frameworks/             # iOS frameworks
│
├── desktop/                     # Desktop platform code
│   ├── macos/                  # macOS app
│   ├── windows/                # Windows app
│   └── linux/                  # Linux app
│
├── apps/                        # Sample/demo apps
│   ├── AVA-AI/                 # AVA AI reference app
│   ├── AVAConnect/             # AVAConnect reference app
│   └── demos/                  # Demo applications
│
├── docs/                        # Documentation
│   ├── Active/                 # Current work
│   ├── Future-Ideas/           # Future enhancements
│   ├── Archive/                # Historical docs
│   └── manuals/                # Developer manuals
│
└── tools/                       # Development tools
    ├── cli/                    # AVANUES CLI
    ├── generators/             # Code generators
    └── validators/             # Validation tools
```

**Why this is better**:
- ✅ **Simpler paths**: `AVANUES/modules/AvaUI` vs `Avanues/Universal/IDEAMagic/AvaUI`
- ✅ **Less nesting**: 2 levels instead of 3
- ✅ **More intuitive**: "modules" clearly indicates KMP modules
- ✅ **Standard convention**: Follows typical project organization
- ✅ **Clearer branding**: "AVANUES" in path, not buried inside

---

## VoiceOS Structure (Simplified)

### User Feedback
1. **VOS4 → VoiceOS**: Better name (more descriptive)
2. **No NLU**: AVA handles this, don't duplicate

### Revised VoiceOS Structure

```
AVANUES/modules/VoiceOS/
├── Core/
│   ├── src/commonMain/kotlin/com/augmentalis/avanues/voiceos/core/
│   │   ├── ipc/
│   │   │   ├── IPCManager.kt              # Cross-app IPC
│   │   │   ├── AppMessage.kt              # Message protocol
│   │   │   ├── Subscription.kt            # Pub/sub
│   │   │   └── MessageFilter.kt           # Filtering
│   │   │
│   │   ├── command/
│   │   │   ├── CommandRouter.kt           # Command routing
│   │   │   ├── CommandMatch.kt            # Command matching
│   │   │   └── FuzzyMatcher.kt            # Fuzzy matching
│   │   │
│   │   ├── security/
│   │   │   └── SecurityManager.kt         # Security/permissions
│   │   │
│   │   ├── capability/
│   │   │   ├── CapabilityRegistry.kt      # App capabilities
│   │   │   ├── AppCapability.kt           # Capability defs
│   │   │   └── CapabilityFilter.kt        # Filtering
│   │   │
│   │   ├── state/
│   │   │   └── StateManager.kt            # State management
│   │   │
│   │   └── event/
│   │       └── EventBus.kt                # Event distribution
│   │
│   ├── src/androidMain/kotlin/...         # Android-specific
│   ├── src/iosMain/kotlin/...             # iOS-specific
│   └── build.gradle.kts
│
├── Recognition/                            # Speech-to-Text
│   ├── src/commonMain/kotlin/com/augmentalis/avanues/voiceos/recognition/
│   │   ├── SpeechRecognizer.kt            # STT interface
│   │   ├── RecognitionConfig.kt           # Configuration
│   │   ├── RecognitionResult.kt           # Result model
│   │   └── LanguageModel.kt               # Language support
│   │
│   ├── src/androidMain/kotlin/...
│   │   ├── engines/
│   │   │   ├── WhisperEngine.kt           # OpenAI Whisper (offline)
│   │   │   ├── VoskEngine.kt              # VOSK (offline)
│   │   │   ├── AndroidSTTEngine.kt        # Android built-in
│   │   │   └── GoogleCloudEngine.kt       # Google Cloud
│   │   │
│   │   └── models/                        # Whisper/VOSK models
│   │
│   └── build.gradle.kts
│
├── Synthesis/                              # Text-to-Speech
│   ├── src/commonMain/kotlin/com/augmentalis/avanues/voiceos/synthesis/
│   │   ├── TextToSpeech.kt                # TTS interface
│   │   ├── SynthesisConfig.kt             # Configuration
│   │   ├── Voice.kt                       # Voice profile
│   │   └── Prosody.kt                     # Pitch/rate/volume
│   │
│   ├── src/androidMain/kotlin/...
│   │   ├── engines/
│   │   │   ├── GoogleTTS.kt               # Google TTS
│   │   │   ├── AndroidTTS.kt              # Android built-in
│   │   │   └── ElevenLabsTTS.kt           # ElevenLabs (future)
│   │   │
│   │   └── voices/                        # Voice profiles
│   │
│   └── build.gradle.kts
│
└── Input/                                  # Voice Input
    ├── src/commonMain/kotlin/com/augmentalis/avanues/voiceos/input/
    │   ├── VoiceKeyboard.kt               # Voice keyboard interface
    │   └── InputConfig.kt                 # Configuration
    │
    ├── src/androidMain/kotlin/...         # Android IME implementation
    └── build.gradle.kts
```

**Changes from v1.0.0**:
- ❌ **Removed NLU module** - AVA handles this
- ✅ **VOS4 → VoiceOS** - Clearer name
- ✅ **Simpler paths** - `modules/VoiceOS` instead of `Universal/IDEAMagic/VOS4`

---

## Package Name Strategy

### Question: Keep `com.augmentalis.avamagic.*` or change to `com.augmentalis.avanues.*`?

#### Option A: Keep IDEAMagic Packages (Minimal Change)
```kotlin
// Packages stay the same
package com.augmentalis.avamagic.ui.core
package com.augmentalis.avamagic.avacode.forms
package com.augmentalis.avamagic.magicdata.core
package com.augmentalis.avamagic.voiceos.core  // Changed from vos4

// Directory structure
AVANUES/modules/AvaUI/src/commonMain/kotlin/com/augmentalis/avamagic/ui/core/
```

**Advantages**:
- ✅ Less work (no package renames)
- ✅ Existing imports still work
- ✅ Packages don't have to match directory names

**Disadvantages**:
- ⚠️ "avamagic" in package but "AVANUES" in directory (inconsistent)
- ⚠️ Less intuitive for new developers

#### Option B: Change to AVANUES Packages (Recommended)
```kotlin
// New package structure
package com.augmentalis.avanues.ui.core
package com.augmentalis.avanues.avacode.forms
package com.augmentalis.avanues.magicdata.core
package com.augmentalis.avanues.voiceos.core

// Directory structure
AVANUES/modules/AvaUI/src/commonMain/kotlin/com/augmentalis/avanues/ui/core/
```

**Advantages**:
- ✅ Consistent branding (AVANUES everywhere)
- ✅ More intuitive
- ✅ Future-proof (if we extract modules, packages make sense)

**Disadvantages**:
- ⚠️ More work (update ~150 files)
- ⚠️ Breaking change for any external dependencies

**Recommendation**: **Option B** - Change to `com.augmentalis.avanues.*`
- We're doing a major rename anyway
- Better to do it all at once
- More professional/consistent

---

## Complete Module List

### Core Modules (Cross-Platform)

```
AVANUES/modules/
├── AvaUI/                    # UI Framework (Phases 1-4, complete)
│   ├── Foundation/             # Colors, Typography, Layout
│   ├── Core/                   # Components, 3D transforms
│   └── Adapters/               # Compose, SwiftUI, HTML renderers
│
├── AvaCode/                  # DSL Framework (Phases 5-6, complete)
│   ├── Forms/                  # Form validation DSL
│   └── Workflows/              # State machine DSL
│
├── MagicData/                  # Database System (renamed)
│   ├── Core/                   # Collection-based storage
│   ├── IPC/                    # AIDL/ContentProvider
│   └── Adapters/               # SQLite, Realm, MongoDB
│
├── VoiceOS/                    # Voice OS Components (renamed from VOS4/VoiceOSBridge)
│   ├── Core/                   # IPC, commands, security
│   ├── Recognition/            # STT engines
│   ├── Synthesis/              # TTS engines (NEW)
│   └── Input/                  # Voice keyboard
│
├── Templates/                  # App Templates (Phase 7, in progress)
│   ├── Core/                   # Template engine
│   └── Library/                # E-Commerce, Task Management, etc.
│
├── Plugins/                    # Plugin Infrastructure (future)
│   ├── Core/                   # Plugin manager
│   ├── Registry/               # Plugin discovery
│   └── Security/               # Signature verification
│
└── IPC/                        # IPC Infrastructure (future)
    ├── AIDL/                   # AIDL base classes
    ├── ContentProvider/        # ContentProvider base
    └── Protocols/              # IPC protocol definitions
```

---

## Migration Plan (Revised)

### Phase 1: Directory Rename + VoiceOS Core (6-8 hours)

#### Task 1.1: Rename Avanues → AVANUES
```bash
cd /Volumes/M-Drive/Coding/
mv Avanues AVANUES
```

#### Task 1.2: Restructure to /modules/
```bash
cd AVANUES

# Create new structure
mkdir modules

# Move Universal/IDEAMagic/* to modules/
mv Universal/IDEAMagic/AvaUI modules/
mv Universal/IDEAMagic/AvaCode modules/
mv Universal/IDEAMagic/Database modules/MagicData
mv Universal/IDEAMagic/VoiceOSBridge modules/VoiceOS
mv Universal/IDEAMagic/Templates modules/
mv Universal/IDEAMagic/Libraries modules/

# Remove now-empty directories
rmdir Universal/IDEAMagic
rmdir Universal
```

#### Task 1.3: Update Package Names
```bash
# Update all package declarations
# avamagic → avanues
find modules/ -name "*.kt" -exec sed -i '' 's/package com\.augmentalis\.avamagic/package com.augmentalis.avanues/g' {} +

# voiceosbridge → voiceos
find modules/ -name "*.kt" -exec sed -i '' 's/package net\.ideahq\.avamagic\.voiceosbridge/package com.augmentalis.avanues.voiceos/g' {} +

# database → magicdata
find modules/ -name "*.kt" -exec sed -i '' 's/package com\.augmentalis\.avamagic\.database/package com.augmentalis.avanues.magicdata/g' {} +
```

#### Task 1.4: Update Imports
```bash
# Update all imports across entire project
find . -name "*.kt" -exec sed -i '' 's/import com\.augmentalis\.avamagic/import com.augmentalis.avanues/g' {} +
find . -name "*.kt" -exec sed -i '' 's/import net\.ideahq\.avamagic\.voiceosbridge/import com.augmentalis.avanues.voiceos/g' {} +
```

#### Task 1.5: Update Directory Structure in Source Files
```bash
# Move source files to match new package names
# For example:
# From: modules/AvaUI/src/commonMain/kotlin/com/augmentalis/avamagic/ui/
# To:   modules/AvaUI/src/commonMain/kotlin/com/augmentalis/avanues/ui/

cd modules/AvaUI/src/commonMain/kotlin/com/augmentalis/
mkdir -p avanues
mv avamagic/* avanues/
rmdir avamagic

# Repeat for all modules
```

#### Task 1.6: Update Gradle Files
```bash
# Update settings.gradle.kts
sed -i '' 's/:Universal:IDEAMagic:/:modules:/g' settings.gradle.kts

# Example changes:
# Before: include(":Universal:IDEAMagic:AvaUI:Foundation")
# After:  include(":modules:AvaUI:Foundation")

# Update all build.gradle.kts files
find . -name "build.gradle.kts" -exec sed -i '' 's/:Universal:IDEAMagic:/:modules:/g' {} +
```

**Verification**:
```bash
# Clean build to verify everything works
./gradlew clean
./gradlew build

# Expected: 0 errors
```

**Estimated time**: 6-8 hours

### Phase 2: Add VoiceOS Synthesis (4-6 hours)

**Only create Synthesis module** - No NLU (AVA handles it)

#### Task 2.1: Create Module Structure
```bash
mkdir -p modules/VoiceOS/Synthesis/src/{commonMain,androidMain,iosMain}/kotlin/com/augmentalis/avanues/voiceos/synthesis
```

#### Task 2.2: Implement Core Interfaces
```kotlin
// modules/VoiceOS/Synthesis/src/commonMain/kotlin/.../TextToSpeech.kt
package com.augmentalis.avanues.voiceos.synthesis

interface TextToSpeech {
    suspend fun synthesize(text: String, voice: Voice): ByteArray
    suspend fun speak(text: String, voice: Voice)
    fun getAvailableVoices(): List<Voice>
    fun stop()
}

// Voice.kt
data class Voice(
    val id: String,
    val name: String,
    val language: String,
    val gender: Gender
)

enum class Gender { MALE, FEMALE, NEUTRAL }

// Prosody.kt
data class Prosody(
    val pitch: Float = 1.0f,      // 0.5 - 2.0
    val rate: Float = 1.0f,       // 0.5 - 2.0
    val volume: Float = 1.0f      // 0.0 - 1.0
)
```

#### Task 2.3: Android Implementation
```kotlin
// modules/VoiceOS/Synthesis/src/androidMain/kotlin/.../AndroidTTS.kt
package com.augmentalis.avanues.voiceos.synthesis

import android.content.Context
import android.speech.tts.TextToSpeech as AndroidTTS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidTextToSpeech(private val context: Context) : TextToSpeech {
    private val tts = AndroidTTS(context) { status -> /* init */ }

    override suspend fun speak(text: String, voice: Voice) {
        withContext(Dispatchers.Main) {
            tts.speak(text, AndroidTTS.QUEUE_FLUSH, null, null)
        }
    }
}
```

**Estimated time**: 4-6 hours

### Phase 3: Documentation Updates (3-4 hours)

#### Task 3.1: Update All Documentation
```bash
# Replace Avanues → AVANUES
find docs/ -name "*.md" -exec sed -i '' 's/Avanues/AVANUES/g' {} +

# Replace Database → MagicData
find docs/ -name "*.md" -exec sed -i '' 's/Database/MagicData/g' {} +

# Replace VOS4 → VoiceOS
find docs/ -name "*.md" -exec sed -i '' 's/VOS4/VoiceOS/g' {} +

# Replace Universal/IDEAMagic → modules
find docs/ -name "*.md" -exec sed -i '' 's/Universal\/IDEAMagic/modules/g' {} +
```

#### Task 3.2: Update Developer Manuals
- [ ] IDEAMAGIC-UI-DEVELOPER-MANUAL.md → AVANUES-UI-DEVELOPER-MANUAL.md
- [ ] Update all package references
- [ ] Add VoiceOS chapter
- [ ] Update examples with new paths

#### Task 3.3: Create AVANUES Platform Guide
```markdown
# AVANUES Platform Developer Guide

## Structure
AVANUES/
├── modules/       # Cross-platform modules
├── android/       # Android platform
├── ios/          # iOS platform
└── desktop/      # Desktop platform

## Modules
- AvaUI: Declarative UI framework
- AvaCode: Forms & Workflows DSL
- MagicData: Database system
- VoiceOS: Voice operating system
- Templates: App generation
```

**Estimated time**: 3-4 hours

### Phase 4: Testing & Validation (6-8 hours)

Same as previous proposal

**Total Revised Estimate**: **19-26 hours** (vs 28-40 hours in v1.0.0)

---

## Gradle Configuration Changes

### settings.gradle.kts (Before)
```kotlin
// Current
include(":Universal:IDEAMagic:AvaUI:Foundation")
include(":Universal:IDEAMagic:AvaUI:Core")
include(":Universal:IDEAMagic:AvaCode:Forms")
include(":Universal:IDEAMagic:Database:Core")
include(":Universal:IDEAMagic:VoiceOSBridge")
```

### settings.gradle.kts (After)
```kotlin
// Simplified
include(":modules:AvaUI:Foundation")
include(":modules:AvaUI:Core")
include(":modules:AvaCode:Forms")
include(":modules:MagicData:Core")
include(":modules:VoiceOS:Core")
include(":modules:VoiceOS:Recognition")
include(":modules:VoiceOS:Synthesis")
include(":modules:VoiceOS:Input")
```

**Much cleaner!**

---

## Build Dependencies (Before/After)

### Before
```kotlin
dependencies {
    implementation(project(":Universal:IDEAMagic:AvaUI:Foundation"))
    implementation(project(":Universal:IDEAMagic:Database:Core"))
}
```

### After
```kotlin
dependencies {
    implementation(project(":modules:AvaUI:Foundation"))
    implementation(project(":modules:MagicData:Core"))
}
```

**Shorter, clearer!**

---

## Package Import Examples

### Before
```kotlin
import com.augmentalis.avamagic.ui.core.Button
import com.augmentalis.avamagic.database.Database
import net.ideahq.avamagic.voiceosbridge.ipc.IPCManager
```

### After
```kotlin
import com.augmentalis.avanues.ui.core.Button
import com.augmentalis.avanues.magicdata.MagicDataClient
import com.augmentalis.avanues.voiceos.core.ipc.IPCManager
```

**Consistent branding!**

---

## Comparison: v1.0.0 vs v2.0.0

| Aspect | v1.0.0 | v2.0.0 | Better? |
|--------|--------|--------|---------|
| **Top directory** | Avanues | AVANUES | ✅ Clearer |
| **Module location** | Universal/IDEAMagic/ | modules/ | ✅ Simpler |
| **Voice OS name** | VOS4 | VoiceOS | ✅ More descriptive |
| **NLU module** | Create new | Use AVA's | ✅ No duplication |
| **Path depth** | 3 levels | 2 levels | ✅ Less nesting |
| **Package prefix** | avamagic | avanues | ✅ Consistent |
| **Estimated effort** | 28-40 hours | 19-26 hours | ✅ 32% faster |

---

## Risk Assessment

| Risk | v1.0.0 | v2.0.0 | Notes |
|------|--------|--------|-------|
| **Breaking builds** | Medium | Medium | Same (use migration branch) |
| **Package rename errors** | High | High | More files, but same approach |
| **Lost functionality** | Low | Low | Same (comprehensive testing) |
| **Directory confusion** | Low | Very Low | Simpler structure |
| **Effort overrun** | Medium | Low | Fewer components to create |

---

## Final Structure Overview

```
AVANUES/
├── modules/
│   ├── AvaUI/         ✅ UI framework (complete)
│   ├── AvaCode/       ✅ Forms & Workflows (complete)
│   ├── MagicData/       ✅ Database (rename only)
│   ├── VoiceOS/         🟡 Voice OS
│   │   ├── Core/        ✅ Exists (rename VoiceOSBridge)
│   │   ├── Recognition/ ✅ Exists (move from android/libraries/)
│   │   ├── Synthesis/   ➕ Create new (4-6 hours)
│   │   └── Input/       ✅ Exists (move from android/libraries/)
│   ├── Templates/       🟡 In progress (Phase 7 Week 1 done)
│   ├── Plugins/         🔵 Future
│   └── IPC/             🔵 Future
│
├── android/             ✅ Keep as-is
├── ios/                 ✅ Keep as-is
├── desktop/             ✅ Keep as-is
├── apps/                ✅ Keep as-is
├── docs/                ✅ Update references
└── tools/               ✅ Keep as-is
```

**Legend**:
- ✅ Exists, needs rename/move
- ➕ Create new
- 🟡 Partially complete
- 🔵 Future work

---

## Next Steps

### Immediate
1. ✅ **User approval** of simplified structure
2. ✅ **Confirm**: Change packages to `com.augmentalis.avanues.*`?
3. ✅ **Confirm**: VOS4 → VoiceOS?
4. ✅ **Confirm**: Skip NLU (use AVA's)?

### This Week (If Approved)
5. ✅ Create `avanues-simplified` branch
6. ✅ Execute Phase 1 (directory + package renames)
7. ✅ Execute Phase 2 (VoiceOS Synthesis)
8. ✅ Verify builds

### Next Week
9. ✅ Complete documentation updates
10. ✅ Comprehensive testing
11. ✅ Merge to main

---

## Summary

**What Changed from v1.0.0**:
- ✅ Simpler structure: `/modules/` instead of `/Universal/IDEAMagic/`
- ✅ VOS4 → VoiceOS (better name)
- ✅ No NLU module (AVA handles it)
- ✅ Consistent packages: `com.augmentalis.avanues.*`
- ✅ Faster migration: 19-26 hours (32% reduction)

**What Stayed the Same**:
- ✅ Same functionality
- ✅ Same platform targets (Android, iOS, Desktop)
- ✅ Same migration approach (branch + testing)
- ✅ Same components (AvaUI, AvaCode, MagicData, VoiceOS, Templates)

**Result**: Cleaner, simpler, faster migration to AVANUES platform!

---

**Status**: 🔵 AWAITING USER APPROVAL
**Version**: 2.0.0 (Simplified)
**Estimated Effort**: 19-26 hours (vs 28-40 in v1.0.0)
**Time Savings**: 32% faster migration

---

**Document Version**: 2.0.0
**Author**: Claude Code (Sonnet 4.5)
**Date**: 2025-11-06
