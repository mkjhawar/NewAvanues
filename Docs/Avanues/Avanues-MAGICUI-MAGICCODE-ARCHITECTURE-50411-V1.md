# AvaUI/AvaCode Architecture Restructure
**Date:** 2025-11-04
**Branch:** component-consolidation-251104
**Status:** 🔄 IN PROGRESS

## Problem Statement

Current package structure has redundant naming and unclear separation:
```
com.augmentalis.avaelements.components.magicform.*     ❌ Too nested
com.augmentalis.avaelements.components.magicdisplay.*  ❌ Redundant "components"
com.augmentalis.avamagic.components.*                   ❌ Inconsistent naming
```

**Issues:**
1. `avaelements` IS the component library, but we have `.components` sub-package
2. No clear separation between UI components (AvaUI) and code generation (AvaCode)
3. Inconsistent naming between Core (`avaelements`) and Foundation (`avamagic`)

## Proposed Architecture

### Top-Level Structure

```
com.augmentalis/
├── avaui/              # UI Component Library
│   ├── core/            # Core type definitions (58 components)
│   │   ├── form/
│   │   ├── display/
│   │   ├── feedback/
│   │   ├── navigation/
│   │   ├── layout/
│   │   └── data/
│   │
│   ├── foundation/      # Platform-agnostic implementations
│   │   └── (Button.kt, Card.kt, etc.)
│   │
│   ├── android/         # Android Compose renderers
│   ├── ios/             # iOS SwiftUI renderers
│   └── web/             # Web/JS renderers
│
├── avacode/           # Code Generation & DSL
│   ├── parser/          # .vos DSL parser
│   ├── generator/       # Code generation
│   ├── runtime/         # Runtime interpreter
│   └── compiler/        # Compilation
│
└── avaelements/       # Shared core types (if needed)
    └── common/          # Common interfaces/types
```

### Package Naming

**Current (BEFORE):**
```kotlin
// Core definitions
package com.augmentalis.avaelements.components.magicform
package com.augmentalis.avaelements.components.magicdisplay

// Foundation implementations
package com.augmentalis.avamagic.components
```

**Proposed (AFTER):**
```kotlin
// Core definitions
package com.augmentalis.avaui.core.form
package com.augmentalis.avaui.core.display

// Foundation implementations
package com.augmentalis.avaui.foundation

// Android renderers
package com.augmentalis.avaui.android

// iOS renderers
package com.augmentalis.avaui.ios
```

### Module Structure

**Current:**
```
Universal/IDEAMagic/Components/
├── Core/                 (avaelements.components.*)
├── Foundation/           (avamagic.components)
├── Renderers/Android/
└── Renderers/iOS/
```

**Proposed:**
```
Universal/AvaUI/
├── Core/                 (avaui.core.*)
├── Foundation/           (avaui.foundation)
├── Android/              (avaui.android)
└── iOS/                  (avaui.ios)

Universal/AvaCode/
├── Parser/               (avacode.parser)
├── Generator/            (avacode.generator)
└── Runtime/              (avacode.runtime)
```

## Migration Plan

### Phase 1: Rename Core Module Packages ✅ (Partially Done)
**Status:** Magic prefix added, but packages still under `avaelements.components.*`

**Action:**
- Rename `com.augmentalis.avaelements.components.magicform` → `com.augmentalis.avaui.core.form`
- Rename `com.augmentalis.avaelements.components.magicdisplay` → `com.augmentalis.avaui.core.display`
- Rename `com.augmentalis.avaelements.components.magicfeedback` → `com.augmentalis.avaui.core.feedback`
- Rename `com.augmentalis.avaelements.components.magicnavigation` → `com.augmentalis.avaui.core.navigation`
- Rename `com.augmentalis.avaelements.components.magiclayout` → `com.augmentalis.avaui.core.layout`
- Rename `com.augmentalis.avaelements.components.magicdata` → `com.augmentalis.avaui.core.data`

**Files affected:** 58 Core component definitions

### Phase 2: Rename Foundation Module Packages
**Status:** Not started

**Action:**
- Rename `com.augmentalis.avamagic.components` → `com.augmentalis.avaui.foundation`

**Files affected:** 36 Foundation implementation files

### Phase 3: Update All Imports
**Status:** Not started

**Action:**
- Find all files importing from old packages
- Update imports to new package structure
- Use automated sed script for bulk updates

**Estimated files:** 100+ across the codebase

### Phase 4: Rename Module Directories
**Status:** Not started

**Action:**
```bash
# Rename modules
mv Universal/IDEAMagic/Components/Core Universal/AvaUI/Core
mv Universal/IDEAMagic/Components/Foundation Universal/AvaUI/Foundation
mv Universal/IDEAMagic/Components/Renderers/Android Universal/AvaUI/Android
mv Universal/IDEAMagic/Components/Renderers/iOS Universal/AvaUI/iOS
```

### Phase 5: Update Build Configuration
**Status:** Not started

**Action:**
- Update settings.gradle.kts module paths
- Update build.gradle.kts group IDs
- Update Android library wrappers

### Phase 6: Separate AvaCode
**Status:** Future work

**Action:**
- Identify all code generation related modules
- Move to `Universal/AvaCode/`
- Update package names to `com.augmentalis.avacode.*`

## Benefits

### 1. Clear Separation of Concerns
- **AvaUI**: Pure UI component library
- **AvaCode**: DSL parsing, code generation, runtime

### 2. Simpler Package Names
- `avaui.core.form.TextField` ✅ (was: `avaelements.components.magicform.TextField` ❌)
- `avaui.foundation.Button` ✅ (was: `avamagic.components.Button` ❌)

### 3. Consistent Naming
- No more `avaelements` vs `avamagic` confusion
- All UI code under `avaui.*`
- All codegen under `avacode.*`

### 4. Scalability
- Easy to add new platforms: `avaui.desktop`, `avaui.wasm`
- Clear where new features go
- Better for external developers to understand

### 5. Framework Clarity
- AvaUI = Component library (like Material UI, Chakra UI)
- AvaCode = Build tooling (like SwiftGen, KSP)

## Import Changes

### Core Definitions

**Before:**
```kotlin
import com.augmentalis.avaelements.components.magicform.TextField
import com.augmentalis.avaelements.components.magicdisplay.Avatar
```

**After:**
```kotlin
import com.augmentalis.avaui.core.form.TextField
import com.augmentalis.avaui.core.display.Avatar
```

### Foundation Implementations

**Before:**
```kotlin
import com.augmentalis.avamagic.components.MagicButton
import com.augmentalis.avamagic.components.MagicCard
```

**After:**
```kotlin
import com.augmentalis.avaui.foundation.MagicButton
import com.augmentalis.avaui.foundation.MagicCard
```

### Cross-Module References

**Before:**
```kotlin
// Foundation importing from Core
import com.augmentalis.avaelements.components.magicform.*
```

**After:**
```kotlin
// Foundation importing from Core
import com.augmentalis.avaui.core.form.*
```

## Automation Strategy

### Bulk Package Rename Script

```bash
#!/bin/bash

# Phase 1: Rename Core packages
find Universal/IDEAMagic/Components/Core -name "*.kt" -exec sed -i '' \
  's/package com\.augmentalis\.avaelements\.components\.magicform/package com.augmentalis.avaui.core.form/g' {} \;

find Universal/IDEAMagic/Components/Core -name "*.kt" -exec sed -i '' \
  's/package com\.augmentalis\.avaelements\.components\.magicdisplay/package com.augmentalis.avaui.core.display/g' {} \;

# ... repeat for all 6 categories

# Phase 2: Rename Foundation packages
find Universal/IDEAMagic/Components/Foundation -name "*.kt" -exec sed -i '' \
  's/package com\.augmentalis\.avamagic\.components/package com.augmentalis.avaui.foundation/g' {} \;

# Phase 3: Update all imports
find Universal -name "*.kt" -exec sed -i '' \
  's/import com\.augmentalis\.avaelements\.components\.magicform/import com.augmentalis.avaui.core.form/g' {} \;

# ... repeat for all packages
```

## Rollout Strategy

### Option A: Big Bang (Recommended for YOLO mode)
1. Rename all packages at once
2. Update all imports at once
3. Fix compilation errors
4. Test everything

**Pros:**
- Fast (1-2 hours)
- Clean break
- No intermediate broken state

**Cons:**
- High risk if something goes wrong
- Large commit

### Option B: Incremental
1. Rename Core packages (6 categories, one at a time)
2. Update imports for each
3. Then rename Foundation
4. Then update remaining imports

**Pros:**
- Safer
- Can rollback individual steps

**Cons:**
- Slower (4-6 hours)
- Codebase in mixed state during migration

## Testing Strategy

### Unit Tests
- All 282 plugin-system tests should pass
- Component-specific tests should pass

### Build Verification
- Core module compiles
- Foundation module compiles
- Android renderers compile
- iOS renderers compile

### Integration Tests
- Android apps build successfully
- Sample apps run correctly

## Documentation Updates

### Files to Update
- [ ] README.md (update package examples)
- [ ] Framework comparison document
- [ ] Component documentation
- [ ] API documentation
- [ ] Developer guides

## Timeline

**Estimated:** 2-3 hours in YOLO mode

- Phase 1 (Core packages): 30 min
- Phase 2 (Foundation packages): 20 min
- Phase 3 (Update imports): 30 min
- Phase 4 (Module directories): 10 min
- Phase 5 (Build config): 20 min
- Phase 6 (Test & fix): 30-60 min

## Risks

1. **Import References:** May miss some imports that need updating
   - **Mitigation:** Use comprehensive grep to find all references

2. **Android/iOS Renderers:** May have hardcoded package names
   - **Mitigation:** Test build after package rename

3. **Documentation:** May reference old package names
   - **Mitigation:** Update docs as final step

4. **External Dependencies:** Other modules may import components
   - **Mitigation:** Update all Universal/ modules together

## Success Criteria

- [ ] All Core definitions use `avaui.core.*` packages
- [ ] All Foundation implementations use `avaui.foundation` package
- [ ] All imports updated to new packages
- [ ] Core module compiles successfully
- [ ] Foundation module compiles successfully
- [ ] All tests pass
- [ ] Android apps build successfully
- [ ] Framework comparison document updated

## Next Steps

1. **Approve architecture** - User confirmation
2. **Execute Phase 1** - Rename Core packages
3. **Execute Phase 2** - Rename Foundation packages
4. **Execute Phase 3** - Update all imports
5. **Execute Phase 4-5** - Rename directories & update build config
6. **Test & verify** - Compile, test, fix

---

**Prepared by:** IDEACODE MCP Agent
**Date:** 2025-11-04
**Status:** Awaiting approval to proceed
