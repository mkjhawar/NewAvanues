# AVAMagic Full Migration Plan

**Source:** `/Volumes/M-Drive/Coding/Avanues` (branch: `avamagic/modularization`)
**Target:** `/Volumes/M-Drive/Coding/NewAvanues/Modules/AVAMagic`
**Date:** 2025-12-29

---

## Chain of Thought (CoT) Analysis

### Current State Assessment

| Metric | Avanues | NewAvanues | Gap |
|--------|---------|------------|-----|
| Kotlin Files | ~2,944 | ~264 | 91% remaining |
| UI Components | 110+ | 87 | ~23 missing |
| Android Mappers | 40 | 37 | 3 missing |
| Modules | 45+ | 30+ | ~15 missing |

### Why Migration is Needed
1. **Consolidation** - Single source of truth for AVAMagic
2. **Namespace Standardization** - `com.augmentalis.avamagic.*`
3. **KMP Structure** - Proper multiplatform setup
4. **No Duplication** - Eliminate parallel development

---

## Tree of Thought (ToT) - Approach Analysis

### Approach A: Bulk Copy + Refactor
```
Pros: Fast initial transfer, complete coverage
Cons: May copy obsolete code, namespace conflicts
Risk: Medium
Time: 2-3 days
```

### Approach B: Module-by-Module Migration
```
Pros: Controlled, testable, selective
Cons: Slower, may miss dependencies
Risk: Low
Time: 5-7 days
```

### Approach C: Component-First (Selected) ✅ RECOMMENDED
```
Pros: Priority components first, incremental verification
Cons: Requires dependency analysis
Risk: Low
Time: 4-5 days
```

**Decision:** Approach C - Migrate by priority, verify each phase

---

## Recursive of Thought (RoT) - Deep Analysis

### Layer 1: Core Dependencies
```
Universal/Core/AvaUI → AVAUI/Core
  └── ComponentModel, ComponentPosition, Logger, Result
  └── DSL: VosParser, VosTokenizer, VosAstNode
  └── Events: EventBus, CallbackAdapter
  └── Registry: ComponentRegistry, BuiltInComponents
```

### Layer 2: Component Library
```
Universal/Libraries/AvaElements/Core → AVAUI/Data
  └── All component data classes (87+ present, 23+ missing)
  └── DSL builders
  └── Theme definitions
```

### Layer 3: Platform Renderers
```
modules/AVAMagic/Components/Renderers → AVAUI/Renderers
  └── Android (37 mappers present, need verification)
  └── iOS (SwiftUI bridge - NOT migrated)
  └── Web (React bridge - NOT migrated)
  └── Desktop (Compose Desktop - partial)
```

### Layer 4: Advanced Features
```
modules/AVAMagic/CodeGen → MagicCode (partial)
modules/AVAMagic/IPC → IPC (present)
modules/AVAMagic/Components/ARGScanner → AVAUI/ARGScanner (empty)
modules/AVAMagic/Components/IPCConnector → AVAUI/IPCConnector (empty)
```

---

## Migration Phases

### Phase 1: Core Runtime (Priority: CRITICAL)
Files to migrate from `Universal/Core/AvaUI/`:

| File | Target | Status |
|------|--------|--------|
| MagicUIRuntime.kt | AVAUI/Core | ⚠️ Missing |
| core/Logger.kt | AVAUI/Core | ⚠️ Missing |
| core/Result.kt | AVAUI/Core | ⚠️ Missing |
| core/VosFile.kt | AVAUI/Core | ⚠️ Missing |
| dsl/VosParser.kt | AVURuntime | ⚠️ Missing |
| dsl/VosTokenizer.kt | AVURuntime | ⚠️ Missing |
| dsl/VosAstNode.kt | AVURuntime | ⚠️ Missing |
| dsl/VosValue.kt | AVURuntime | ⚠️ Missing |
| dsl/VosLambda.kt | AVURuntime | ⚠️ Missing |
| events/EventBus.kt | AVAUI/Core | ⚠️ Missing |
| events/CallbackAdapter.kt | AVAUI/Core | ⚠️ Missing |
| events/EventContext.kt | AVAUI/Core | ⚠️ Missing |
| registry/ComponentRegistry.kt | AVAUI/Core | ✅ Present |
| registry/BuiltInComponents.kt | AVAUI/Core | ⚠️ Missing |
| registry/ComponentDescriptor.kt | AVAUI/Core | ✅ Present |

**Tasks:** 12 files, ~2,400 lines

### Phase 2: Lifecycle & Layout
Files from `Universal/Core/AvaUI/`:

| File | Target | Status |
|------|--------|--------|
| lifecycle/AppLifecycle.kt | AVAUI/Core | ⚠️ Missing |
| lifecycle/ResourceManager.kt | AVAUI/Core | ⚠️ Missing |
| lifecycle/StateManager.kt | AVAUI/Core | ⚠️ Missing |
| layout/LayoutFormat.kt | AVAUI/Core | ⚠️ Missing |
| layout/LayoutLoader.kt | AVAUI/Core | ⚠️ Missing |
| instantiation/ComponentInstantiator.kt | AVAUI/Core | ⚠️ Missing |
| instantiation/DefaultValueProvider.kt | AVAUI/Core | ⚠️ Missing |
| instantiation/PropertyMapper.kt | AVAUI/Core | ⚠️ Missing |
| instantiation/TypeCoercion.kt | AVAUI/Core | ⚠️ Missing |

**Tasks:** 9 files, ~1,800 lines

### Phase 3: Voice & IMU Integration
Files from `Universal/Core/AvaUI/`:

| File | Target | Status |
|------|--------|--------|
| voice/VoiceCommandRouter.kt | AVAUI/VoiceCommandRouter | ⚠️ Verify |
| voice/CommandMatcher.kt | AVAUI/VoiceCommandRouter | ⚠️ Missing |
| voice/ActionDispatcher.kt | AVAUI/VoiceCommandRouter | ⚠️ Missing |
| imu/IMUOrientationData.kt | VoiceIntegration | ⚠️ Missing |
| imu/MotionProcessor.kt | VoiceIntegration | ⚠️ Missing |

**Tasks:** 5 files, ~1,000 lines

### Phase 4: Theme System
Files from `Universal/Core/AvaUI/theme/` and `Universal/Core/ThemeManager/`:

| File | Target | Status |
|------|--------|--------|
| ThemeConfig.kt | UI/ThemeManager | ⚠️ Verify |
| loaders/JsonThemeLoader.kt | UI/ThemeManager | ⚠️ Missing |
| loaders/JsonThemeSerializer.kt | UI/ThemeManager | ⚠️ Missing |
| loaders/YamlThemeLoader.kt | UI/ThemeManager | ⚠️ Missing |
| loaders/YamlThemeSerializer.kt | UI/ThemeManager | ⚠️ Missing |
| ThemeManager.kt | UI/ThemeManager | ⚠️ Verify |
| ThemeOverride.kt | UI/ThemeManager | ⚠️ Missing |
| ThemeRepository.kt | UI/ThemeManager | ⚠️ Missing |
| ThemeSync.kt | UI/ThemeManager | ⚠️ Missing |

**Tasks:** 9 files, ~1,500 lines

### Phase 5: Platform Adapters (iOS/Web)
Files from `modules/AVAMagic/Components/Adapters/`:

| File | Target | Status |
|------|--------|--------|
| iosMain/iOSRenderer.kt | AVAUI/Renderers/iOS | ⚠️ Missing |
| iosMain/iOSRenderHelpers.kt | AVAUI/Renderers/iOS | ⚠️ Missing |
| iosMain/SwiftUIBridge.kt | AVAUI/Renderers/iOS | ⚠️ Missing |
| iosMain/SwiftUIInterop.kt | AVAUI/Renderers/iOS | ⚠️ Missing |
| jsMain/ReactBridge.kt | AVAUI/Renderers/Web | ⚠️ Missing |
| jsMain/ReactComponentLoader.kt | AVAUI/Renderers/Web | ⚠️ Missing |

**Tasks:** 6 files, ~1,200 lines

### Phase 6: ARGScanner & IPC
Files from `modules/AVAMagic/Components/`:

| File | Target | Status |
|------|--------|--------|
| ARGScanner/ARGModels.kt | AVAUI/ARGScanner | ⚠️ Missing |
| ARGScanner/ARGParser.kt | AVAUI/ARGScanner | ⚠️ Missing |
| ARGScanner/ARGRegistry.kt | AVAUI/ARGScanner | ⚠️ Missing |
| ARGScanner/ARGScanner.kt | AVAUI/ARGScanner | ⚠️ Missing |
| IPCConnector/CircuitBreaker.kt | AVAUI/IPCConnector | ⚠️ Missing |
| IPCConnector/ConnectionManager.kt | AVAUI/IPCConnector | ⚠️ Missing |
| IPCConnector/ContentProviderConnector.kt | AVAUI/IPCConnector | ⚠️ Missing |
| IPCConnector/IPCModels.kt | AVAUI/IPCConnector | ⚠️ Missing |
| IPCConnector/RateLimiter.kt | AVAUI/IPCConnector | ⚠️ Missing |
| IPCConnector/ServiceConnector.kt | AVAUI/IPCConnector | ⚠️ Missing |

**Tasks:** 10 files, ~2,000 lines

### Phase 7: Asset Management (Enhanced)
Files from `Universal/Libraries/AvaElements/AssetManager/`:

| File | Target | Status |
|------|--------|--------|
| AssetManager.kt | AVAUI/AssetManager | ✅ Verify |
| AssetProcessor.kt | AVAUI/AssetManager | ✅ Verify |
| AssetStorage.kt | AVAUI/AssetManager | ✅ Verify |
| LocalAssetStorage.kt | AVAUI/AssetManager | ⚠️ Missing |
| library/FontAwesomeLibrary.kt | AVAUI/AssetManager | ⚠️ Missing |
| library/IconLibraryProvider.kt | AVAUI/AssetManager | ⚠️ Missing |
| library/RemoteIconLibrary.kt | AVAUI/AssetManager | ⚠️ Missing |
| models/AssetLibrary.kt | AVAUI/AssetManager | ⚠️ Missing |
| models/Icon.kt | AVAUI/AssetManager | ⚠️ Missing |
| models/ImageAsset.kt | AVAUI/AssetManager | ⚠️ Missing |
| utils/AssetUtils.kt | AVAUI/AssetManager | ⚠️ Missing |
| db/DatabaseDriverFactory.kt | AVAUI/AssetManager | ⚠️ Missing |

**Tasks:** 12 files, ~2,400 lines

### Phase 8: Missing Components
Compare and migrate remaining components from `Universal/Libraries/AvaElements/Core/`:

**Missing UI Components to identify:**
- Compare data classes in both repos
- Migrate missing component definitions
- Update namespace to `com.augmentalis.avamagic.*`

**Tasks:** ~23 components, ~1,500 lines

---

## Task Summary

| Phase | Files | Est. Lines | Priority |
|-------|-------|------------|----------|
| 1. Core Runtime | 12 | 2,400 | CRITICAL |
| 2. Lifecycle & Layout | 9 | 1,800 | HIGH |
| 3. Voice & IMU | 5 | 1,000 | HIGH |
| 4. Theme System | 9 | 1,500 | MEDIUM |
| 5. Platform Adapters | 6 | 1,200 | MEDIUM |
| 6. ARGScanner & IPC | 10 | 2,000 | MEDIUM |
| 7. Asset Management | 12 | 2,400 | LOW |
| 8. Missing Components | ~23 | 1,500 | LOW |
| **TOTAL** | **~86** | **~13,800** | |

---

## Namespace Mapping

| Old Namespace | New Namespace |
|---------------|---------------|
| com.augmentalis.voiceos.magicui | com.augmentalis.avamagic.core |
| com.augmentalis.magicelements | com.augmentalis.avamagic.components |
| com.augmentalis.universal.assetmanager | com.augmentalis.avamagic.assets |
| com.augmentalis.universal.thememanager | com.augmentalis.avamagic.themes |
| net.ideahq.ideamagic | com.augmentalis.avamagic.adapters |

---

## Folder Structure Rules

1. **No redundant `src`** - Only use `src/commonMain/kotlin/...` where KMP requires
2. **Module naming** - PascalCase for modules, lowercase for KMP targets
3. **Flat where possible** - Avoid deep nesting
4. **Package = Path** - Package structure mirrors folder structure

---

## Verification Checklist

For each phase:
- [ ] Files copied with correct namespace
- [ ] build.gradle.kts updated with dependencies
- [ ] settings.gradle.kts includes module
- [ ] Compiles without errors
- [ ] No circular dependencies
- [ ] Tests pass (if applicable)

---

## Commands

```bash
# Verify migration progress
cd /Volumes/M-Drive/Coding/NewAvanues/Modules/AVAMagic
find . -name "*.kt" | wc -l

# Build check
./gradlew :Modules:AVAMagic:AVAUI:Core:build

# Full build
./gradlew :Modules:AVAMagic:build
```

---

**Created:** 2025-12-29
**Version:** 1.0
**Status:** READY FOR EXECUTION
