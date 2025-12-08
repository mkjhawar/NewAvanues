# Namespace - Architecture Review
**Module:** Core Architecture
**Author:** Manoj Jhawar
**Created:** 240820
**Last Updated:** 240820

## Changelog
- 240820: Initial review of namespace and filing structure

## Current Issues Identified

### 1. Redundant/Duplicate Modules
- **audio** AND **audiomgr** - Should be just one
- Empty modules: browser, filemanager, keyboard, launcher, communication, updatesystem

### 2. Inconsistent Naming Patterns
- Some use descriptive names: smartglasses, accessibility
- Some use abbreviations: srstt (should be consistent)
- Some use "mgr" suffix: audiomgr, commandmgr (inconsistent)

### 3. Namespace Depth
Current: `com.ai.vos.module.subpackage.class`
Could be: `com.ai.module.class` (flatter, simpler)

## Proposed Streamlined Structure

### Option A: Ultra-Flat (Recommended)
```
com.ai.core.*           // Core framework
com.ai.audio.*          // Audio management
com.ai.speech.*         // Speech recognition (not srstt)
com.ai.commands.*       // Command processing
com.ai.data.*           // Database/persistence
com.ai.access.*         // Accessibility
com.ai.glasses.*        // Smart glasses
com.ai.device.*         // Device info
com.ai.license.*        // Licensing
com.ai.locale.*         // Localization
com.ai.overlay.*        // Overlay UI
com.ai.voiceui.*        // Voice UI (standalone app)
```

### Option B: Grouped by Function
```
com.ai.vos.core.*       // Core only
com.ai.input.*          // audio, speech, commands
com.ai.output.*         // overlay, voiceui
com.ai.device.*         // deviceinfo, glasses, access
com.ai.system.*         // data, license, locale
```

### Option C: Keep Current but Cleanup
```
com.ai.vos.*            // Keep current structure
- Remove duplicate modules
- Rename srstt → speech
- Remove "mgr" suffixes
- Delete empty modules
```

## Module Consolidation Recommendations

### Delete These Empty Modules
- browser (empty)
- filemanager (empty)
- keyboard (empty)
- launcher (empty)
- communication (empty)
- updatesystem (empty)

### Merge These Duplicates
- audio + audiomgr → audio
- srstt → speech (clearer name)

### Rename for Clarity
- commandmgr → commands
- database → data
- smartglasses → glasses (shorter)
- accessibility → access (shorter)
- deviceinfo → device
- licensing → license
- localization → locale

## Benefits of Streamlined Structure

1. **Reduced Complexity**: Fewer nested packages
2. **Clearer Organization**: Obvious module purposes
3. **Easier Imports**: Shorter import statements
4. **Better IDE Navigation**: Less directory traversal
5. **Consistent Naming**: No mix of patterns

## Impact Analysis

### Current Line Count:
```
com.ai.vos.core.*: ~320 LOC
com.ai.vos.audiomgr.*: ~1,850 LOC
com.ai.vos.srstt.*: ~12,500 LOC
com.ai.vos.commandmgr.*: ~2,100 LOC
com.ai.vos.database.*: ~3,200 LOC
com.ai.vos.accessibility.*: ~2,400 LOC
com.ai.vos.smartglasses.*: ~4,100 LOC
Total: ~26,470 LOC
```

### Migration Effort:
- **Low**: Just namespace/folder renames
- **Medium**: Update all imports
- **High**: Update documentation

## Recommendation

**Go with Option A: Ultra-Flat Structure**

Reasons:
1. Simplest to understand and navigate
2. Each module is its own top-level package
3. No unnecessary nesting
4. Clear separation between VOS and standalone apps (voiceui)
5. Follows modern Android practices (many Google apps use flat structures)

## Next Steps

1. Delete all empty modules
2. Merge duplicate modules (audio + audiomgr)
3. Rename modules to shorter, clearer names
4. Migrate to flatter namespace structure
5. Update all imports and references
6. Update documentation