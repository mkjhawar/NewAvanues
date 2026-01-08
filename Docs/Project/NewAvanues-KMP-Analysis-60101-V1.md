# NewAvanues KMP Analysis Report

**Date:** 2026-01-01
**Repository:** /Volumes/M-Drive/Coding/NewAvanues
**Branch:** Avanues-Main

---

## Summary

| Category | Total Modules | KMP | Android-Only | Status |
|----------|---------------|-----|--------------|--------|
| Modules/ | 127 | 91 | 36 | ✅ ACTIVE |
| Common/ | 44 | 36 | 8 | ⚠️ ORPHANED |
| **Active Total** | **127** | **91** | **36** | - |

> **Note:** Only `Modules/` is in `settings.gradle.kts`. Common/ is NOT built and can be archived/deleted.

---

## Repository Structure

```
NewAvanues/
├── android/                    # Android apps (platform-specific)
│   └── apps/
│       ├── ava/               # AVA Android app
│       ├── VoiceOS/           # VoiceOS Android app
│       ├── webavanue/         # WebAvanue Android app
│       └── testapp/           # Test app
├── desktop/                    # Desktop apps (placeholder)
├── ios/                        # iOS apps (placeholder)
├── Common/                     # Shared libraries (36 KMP, 8 Android-only)
├── Modules/                    # Feature modules (91 KMP, 36 Android-only)
├── Shared/                     # Assets and configs
├── Tools/                      # Native tools (C++/Python)
├── vivoka/                     # Vendor AAR files
├── web/                        # Web apps (placeholder)
└── backend/                    # Backend services (placeholder)
```

---

## KMP Modules (Already Multiplatform)

### Highlighted: Recently Converted to KMP

#### VoiceOSCoreNG (Full KMP)
- **Path:** `Modules/VoiceOSCoreNG/`
- **Platforms:** Android, iOS (x64, arm64, simulatorArm64), Desktop (JVM)
- **Status:** ✅ Fully KMP - Next-generation VoiceOS core
- **Source Sets:** commonMain, commonTest, androidMain, androidUnitTest, androidInstrumentedTest, iosMain, iosTest, desktopMain, desktopTest

#### Common/VUID (Full KMP)
- **Path:** `Common/VUID/`
- **Platforms:** Android, Desktop (JVM), iOS (conditional), JS/Web (conditional)
- **Status:** ✅ Fully KMP - Voice Unique Identifier generation
- **Source Sets:** commonMain, commonTest, androidMain, desktopMain, iosMain (conditional), jsMain (conditional)

---

### Modules/ Directory

#### Modules/VoiceOS/ (11 core + 11 libraries + 5 managers = 27 KMP)

**Core (All KMP):**
| Module | Path | Platforms |
|--------|------|-----------|
| accessibility-types | `Modules/VoiceOS/core/accessibility-types/` | Android, iOS, JVM |
| command-models | `Modules/VoiceOS/core/command-models/` | Android, iOS, JVM |
| constants | `Modules/VoiceOS/core/constants/` | Android, iOS, JVM |
| database | `Modules/VoiceOS/core/database/` | Android, iOS, JVM |
| exceptions | `Modules/VoiceOS/core/exceptions/` | Android, iOS, JVM |
| hash | `Modules/VoiceOS/core/hash/` | Android, iOS, JVM |
| json-utils | `Modules/VoiceOS/core/json-utils/` | Android, iOS, JVM |
| result | `Modules/VoiceOS/core/result/` | Android, iOS, JVM |
| text-utils | `Modules/VoiceOS/core/text-utils/` | Android, iOS, JVM |
| validation | `Modules/VoiceOS/core/validation/` | Android, iOS, JVM |
| voiceos-logging | `Modules/VoiceOS/core/voiceos-logging/` | Android, iOS, JVM |

**Libraries (2 KMP, 9 Android-only):**
| Module | Path | KMP? |
|--------|------|------|
| PluginSystem | `Modules/VoiceOS/libraries/PluginSystem/` | ✅ KMP |
| UniversalIPC | `Modules/VoiceOS/libraries/UniversalIPC/` | ✅ KMP |
| DeviceManager | `Modules/VoiceOS/libraries/DeviceManager/` | ❌ Android |
| JITLearning | `Modules/VoiceOS/libraries/JITLearning/` | ❌ Android |
| LearnAppCore | `Modules/VoiceOS/libraries/LearnAppCore/` | ❌ Android |
| SpeechRecognition | `Modules/VoiceOS/libraries/SpeechRecognition/` | ❌ Android |
| UUIDCreator | `Modules/VoiceOS/libraries/UUIDCreator/` | ❌ Android |
| VivokaSDK | `Modules/VoiceOS/libraries/VivokaSDK/` | ❌ Android |
| VoiceKeyboard | `Modules/VoiceOS/libraries/VoiceKeyboard/` | ❌ Android |
| VoiceOsLogging | `Modules/VoiceOS/libraries/VoiceOsLogging/` | ❌ Android |
| VoiceUIElements | `Modules/VoiceOS/libraries/VoiceUIElements/` | ❌ Android |

**Managers (All Android-only):**
| Module | Path |
|--------|------|
| CommandManager | `Modules/VoiceOS/managers/CommandManager/` |
| HUDManager | `Modules/VoiceOS/managers/HUDManager/` |
| LicenseManager | `Modules/VoiceOS/managers/LicenseManager/` |
| LocalizationManager | `Modules/VoiceOS/managers/LocalizationManager/` |
| VoiceDataManager | `Modules/VoiceOS/managers/VoiceDataManager/` |

**Apps (All Android-only - by design):**
| App | Path |
|-----|------|
| VoiceOS | `Modules/VoiceOS/apps/VoiceOS/` |
| VoiceOSCore | `Modules/VoiceOS/apps/VoiceOSCore/` |
| VoiceUI | `Modules/VoiceOS/apps/VoiceUI/` |
| VoiceCursor | `Modules/VoiceOS/apps/VoiceCursor/` |
| VoiceRecognition | `Modules/VoiceOS/apps/VoiceRecognition/` |
| VoiceOSIPCTest | `Modules/VoiceOS/apps/VoiceOSIPCTest/` |

---

#### Modules/VoiceOSCoreNG/ (KMP)
| Module | Platforms | Notes |
|--------|-----------|-------|
| VoiceOSCoreNG | Android, iOS (x3), Desktop | Next-gen core, fully multiplatform |

---

#### Modules/UniversalRPC/ (KMP)
| Module | Platforms | Notes |
|--------|-----------|-------|
| UniversalRPC | Android, iOS (x3), Desktop | gRPC via Wire |

---

#### Modules/WebAvanue/ (KMP)
| Module | Path | KMP? | Notes |
|--------|------|------|-------|
| coredata | `Modules/WebAvanue/coredata/` | ✅ KMP | SQLDelight database |
| universal | `Modules/WebAvanue/universal/` | ✅ KMP | UI layer (iOS/Desktop disabled) |

---

#### Modules/Shared/ (All KMP)
| Module | Path | Platforms |
|--------|------|-----------|
| NLU | `Modules/Shared/NLU/` | Android, iOS, Desktop |
| Platform | `Modules/Shared/Platform/` | Android, iOS, Desktop |
| LaasSDK | `Modules/Shared/LaasSDK/` | Android, iOS, Desktop |

---

#### Modules/AVA/ (8 KMP, 4 Android-only)

**KMP Modules:**
| Module | Path |
|--------|------|
| core/Domain | `Modules/AVA/core/Domain/` |
| core/Utils | `Modules/AVA/core/Utils/` |
| core/Data | `Modules/AVA/core/Data/` |
| core/Theme | `Modules/AVA/core/Theme/` |
| Actions | `Modules/AVA/Actions/` |
| memory | `Modules/AVA/memory/` |
| RAG | `Modules/AVA/RAG/` |
| Teach | `Modules/AVA/Teach/` |

**Android-Only:**
| Module | Path | Reason |
|--------|------|--------|
| LLM | `Modules/AVA/LLM/` | TVM native libs |
| Chat | `Modules/AVA/Chat/` | Hilt DI |
| Overlay | `Modules/AVA/Overlay/` | Android UI |
| WakeWord | `Modules/AVA/WakeWord/` | Platform-specific |

---

#### Modules/AVAMagic/ (91 KMP modules)

**Core (All KMP):**
- accessibility-types, command-models, constants, database, exceptions, hash, json-utils, result, text-utils, validation, voiceos-logging, Responsive

**AVAUI/ (All KMP - 21 modules):**
- Adapters, ARGScanner, AssetManager, Core, CoreTypes, Data, DesignSystem, Display, Feedback, Floating, Foundation, Input, IPCConnector, Layout, Navigation, Renderers/Android, Renderers/Desktop, Renderers/iOS, StateManagement, TemplateLibrary, Theme, ThemeBridge, ThemeBuilder, UIConvertor, VoiceCommandRouter

**MagicUI/ (All KMP - 23 modules):**
- Components/*, Core, CoreTypes, DesignSystem, Foundation, Renderers/*, StateManagement, Theme, ThemeBridge, UIConvertor

**Other KMP:**
- AVACode, AVURuntime, Data, IPC/DSLSerializer, Libraries/PluginSystem, Libraries/Preferences, Libraries/UniversalIPC, MagicCode/Forms, MagicCode/Templates/Core, MagicCode/Workflows, Observability, PluginRecovery, VoiceIntegration

**Android-Only:**
- apps/* (VoiceCursor, VoiceOS, VoiceOSCore, VoiceOSIPCTest, VoiceRecognition, VoiceUI)
- Libraries/* (DeviceManager, JITLearning, LearnAppCore, SpeechRecognition, UUIDCreator, VivokaSDK, VoiceKeyboard, VoiceOsLogging, VoiceUIElements)
- managers/* (CommandManager, HUDManager, LicenseManager, LocalizationManager, VoiceDataManager)
- MagicTools/LanguageServer (JVM only)

---

### Common/ Directory - ⚠️ ORPHANED/LEGACY

**IMPORTANT:** The entire `Common/` directory is NOT in `settings.gradle.kts` - these are orphaned legacy files, NOT part of the build!

Only `Modules/` is active. The `Common/` directories can be deleted or archived.

#### Common/VUID/ (KMP - but orphaned)
| Module | Platforms | Notes |
|--------|-----------|-------|
| VUID | Android, Desktop, iOS (cond), JS (cond) | Voice Unique Identifier - ORPHANED |

#### Common/VoiceOS/ (Orphaned - 11 modules)
| Module | Path |
|--------|------|
| accessibility-types | `Common/VoiceOS/accessibility-types/` |
| command-models | `Common/VoiceOS/command-models/` |
| constants | `Common/VoiceOS/constants/` |
| database | `Common/VoiceOS/database/` |
| exceptions | `Common/VoiceOS/exceptions/` |
| hash | `Common/VoiceOS/hash/` |
| json-utils | `Common/VoiceOS/json-utils/` |
| result | `Common/VoiceOS/result/` |
| text-utils | `Common/VoiceOS/text-utils/` |
| validation | `Common/VoiceOS/validation/` |
| voiceos-logging | `Common/VoiceOS/voiceos-logging/` |

#### Common/Core/ (All KMP - 8 modules)
| Module | Path |
|--------|------|
| AssetManager | `Common/Core/AssetManager/` |
| AvaCode | `Common/Core/AvaCode/` |
| AvaUI | `Common/Core/AvaUI/` |
| Database | `Common/Core/Database/` |
| ThemeBridge | `Common/Core/ThemeBridge/` |
| ThemeManager | `Common/Core/ThemeManager/` |
| UIConvertor | `Common/Core/UIConvertor/` |
| VoiceOSBridge | `Common/Core/VoiceOSBridge/` |

#### Common/AvaElements/ (All KMP - 14 modules)
| Module | Path |
|--------|------|
| AssetManager | `Common/AvaElements/AssetManager/` |
| Core | `Common/AvaElements/Core/` |
| Phase3Components | `Common/AvaElements/Phase3Components/` |
| PluginSystem | `Common/AvaElements/PluginSystem/` |
| Renderers/Android | `Common/AvaElements/Renderers/Android/` |
| Renderers/Desktop | `Common/AvaElements/Renderers/Desktop/` |
| Renderers/iOS | `Common/AvaElements/Renderers/iOS/` |
| StateManagement | `Common/AvaElements/StateManagement/` |
| TemplateLibrary | `Common/AvaElements/TemplateLibrary/` |
| ThemeBuilder | `Common/AvaElements/ThemeBuilder/` |
| components/flutter-parity | `Common/AvaElements/components/flutter-parity/` |
| components/phase1 | `Common/AvaElements/components/phase1/` |
| components/phase3 | `Common/AvaElements/components/phase3/` |
| components/unified | `Common/AvaElements/components/unified/` |

#### Other KMP:
| Module | Path |
|--------|------|
| Cockpit | `Common/Cockpit/` |
| Database | `Common/Database/` |
| SpatialRendering | `Common/SpatialRendering/` |

#### Android-Only (Need Migration):
| Module | Path | Status | Files to Migrate |
|--------|------|--------|------------------|
| uuidcreator | `Common/uuidcreator/` | Room → SQLDelight needed | 10 files (4 DAOs, 4 Entities, 1 Database, 1 doc) |
| argscanner | `Common/argscanner/` | Room → SQLDelight needed | 3 Entity files |
| UI | `Common/UI/` | Has androidMain but build not KMP | 1 file (GlassmorphismUtils.kt) |
| Utils | `Common/Utils/` | Uses SQLDelight via Common:Database | Module build not KMP |
| Libraries/argscanner | `Common/Libraries/argscanner/` | Duplicate of above | - |
| Libraries/uuidcreator | `Common/Libraries/uuidcreator/` | Duplicate of above | - |
| ThirdParty/Vosk | `Common/ThirdParty/Vosk/` | Asset bundle only | N/A |

**Room Files Still Present:**
```
Common/uuidcreator/src/main/java/com/augmentalis/uuidcreator/database/
├── dao/
│   ├── UUIDAliasDao.kt          # @Dao - needs SQLDelight query
│   ├── UUIDAnalyticsDao.kt      # @Dao - needs SQLDelight query
│   ├── UUIDElementDao.kt        # @Dao - needs SQLDelight query
│   └── UUIDHierarchyDao.kt      # @Dao - needs SQLDelight query
├── entities/
│   ├── UUIDAliasEntity.kt       # @Entity - needs .sq schema
│   ├── UUIDAnalyticsEntity.kt   # @Entity - needs .sq schema
│   ├── UUIDElementEntity.kt     # @Entity - needs .sq schema
│   └── UUIDHierarchyEntity.kt   # @Entity - needs .sq schema
└── UUIDCreatorDatabase.kt       # RoomDatabase - remove

Common/argscanner/src/main/kotlin/com/augmentalis/argscanner/models/
├── ARScanSession.kt             # @Entity - needs .sq schema
├── ScannedObject.kt             # @Entity - needs .sq schema
└── SpatialRelationship.kt       # @Entity - needs .sq schema
```

---

## Android-Only Modules (Active - May Need Conversion)

> **Note:** Common/* modules are ORPHANED (not in settings.gradle.kts) and don't need migration.

### Quick Wins
| Module | Path | Blocker | Effort |
|--------|------|---------|--------|
| WebAvanue iOS/Desktop | `Modules/WebAvanue/universal/` | Targets disabled | LOW |

### Platform-Specific by Design (No conversion needed)
| Module | Reason |
|--------|--------|
| VoiceOS/apps/* | Android apps - consume KMP libs |
| VoiceOS/libraries/* | Platform-specific (AIDL, Android services) |
| VoiceOS/managers/* | Android service integrations |
| AVA/LLM | TVM native dependencies |
| AVA/Chat | Hilt DI (Android-specific) |
| AVA/Overlay | Android WindowManager |
| AVA/WakeWord | Platform-specific audio |

### SQLDelight Migration Already Complete
| Module | Evidence |
|--------|----------|
| Modules/VoiceOS/libraries/UUIDCreator | Uses `SQLDelightVUIDRepositoryAdapter.kt` (created 2025-11-25) |
| Modules/VoiceOS/core/database | Has SQLDelight schemas in `sqldelight/com/augmentalis/database/vuid/` |

---

## Platform Target Summary

| Platform | Module Count | Notes |
|----------|--------------|-------|
| Android | 171 | 100% coverage |
| iOS | 100+ | Conditional compilation |
| Desktop/JVM | 100+ | Conditional compilation |
| Web/JS | 2 | VUID, AssetManager only |

---

## File Structure Tree (Modules with KMP Status)

```
Modules/
├── AVA/                           # AI Assistant
│   ├── Actions/                   # ✅ KMP
│   ├── Chat/                      # ❌ Android (Hilt)
│   ├── LLM/                       # ❌ Android (TVM)
│   ├── memory/                    # ✅ KMP
│   ├── Overlay/                   # ❌ Android
│   ├── RAG/                       # ✅ KMP
│   ├── Teach/                     # ✅ KMP
│   ├── WakeWord/                  # ❌ Android
│   └── core/
│       ├── Data/                  # ✅ KMP
│       ├── Domain/                # ✅ KMP
│       ├── Theme/                 # ✅ KMP
│       └── Utils/                 # ✅ KMP
│
├── AVAMagic/                      # UI Framework (91 KMP)
│   ├── apps/                      # ❌ Android (6 apps)
│   ├── AVAUI/                     # ✅ KMP (21 modules)
│   ├── AVACode/                   # ✅ KMP
│   ├── AVURuntime/                # ✅ KMP
│   ├── Core/                      # ✅ KMP (12 modules)
│   ├── Data/                      # ✅ KMP
│   ├── IPC/                       # ✅ KMP (2 modules)
│   ├── Libraries/                 # Mixed (2 KMP, 9 Android)
│   ├── MagicCode/                 # ✅ KMP (3 modules)
│   ├── MagicTools/                # ❌ JVM (LanguageServer)
│   ├── MagicUI/                   # ✅ KMP (23 modules)
│   ├── managers/                  # ❌ Android (5 modules)
│   ├── Observability/             # ✅ KMP
│   ├── PluginRecovery/            # ✅ KMP
│   └── VoiceIntegration/          # ✅ KMP
│
├── Shared/                        # Cross-platform
│   ├── LaasSDK/                   # ✅ KMP
│   ├── NLU/                       # ✅ KMP
│   └── Platform/                  # ✅ KMP
│
├── UniversalRPC/                  # ✅ KMP (gRPC)
│
├── VoiceOS/                       # Voice Accessibility
│   ├── apps/                      # ❌ Android (6 apps)
│   ├── core/                      # ✅ KMP (11 modules)
│   ├── libraries/                 # Mixed (2 KMP, 9 Android)
│   └── managers/                  # ❌ Android (5 modules)
│
├── VoiceOSCoreNG/                 # ✅ KMP (Next-gen core)
│
└── WebAvanue/                     # Browser
    ├── coredata/                  # ✅ KMP
    └── universal/                 # ✅ KMP (iOS/Desktop disabled)

Common/
├── AvaElements/                   # ✅ KMP (14 modules)
├── Cockpit/                       # ✅ KMP
├── Core/                          # ✅ KMP (8 modules)
├── Database/                      # ✅ KMP
├── SpatialRendering/              # ✅ KMP
├── VoiceOS/                       # ✅ KMP (11 modules)
├── VUID/                          # ✅ KMP
├── argscanner/                    # ❌ Android - Room (3 @Entity files need SQLDelight)
├── uuidcreator/                   # ❌ Android - Room (10 files: 4 DAOs, 4 Entities, 1 DB)
├── UI/                            # ❌ Android - 1 file only (GlassmorphismUtils.kt)
├── Utils/                         # ❌ Android - uses Common:Database (SQLDelight)
├── Libraries/
│   ├── argscanner/                # ❌ Duplicate of Common/argscanner
│   └── uuidcreator/               # ❌ Duplicate of Common/uuidcreator
└── ThirdParty/
    └── Vosk/                      # ❌ Android assets only
```

---

## Notes

1. **VoiceOSCoreNG** is the reference implementation for full KMP with all platform targets enabled unconditionally
2. **Common/** directory is ORPHANED - NOT in `settings.gradle.kts`, can be archived/deleted
3. **SQLDelight migration is COMPLETE** - `Modules/VoiceOS/libraries/UUIDCreator` uses `SQLDelightVUIDRepositoryAdapter.kt`
4. Apps are Android-only by design - they consume KMP modules
5. **No Room→SQLDelight migration needed** - Room files in Common/ are orphaned legacy code
6. Most AVAMagic modules are already KMP (91 modules)
7. Only active conversion opportunity: Enable iOS/Desktop targets in `Modules/WebAvanue/universal/`

---

## Recommended Actions

1. **Archive Common/** - It's not in the build; delete or move to `_archive/`
2. **Enable WebAvanue targets** - Uncomment iOS/Desktop in `Modules/WebAvanue/universal/build.gradle.kts`
3. **No migration work needed** - SQLDelight conversion was already completed

---

**Author:** Claude Analysis
**Version:** 1.1
**Updated:** 2026-01-01
