# File Naming Convention Fix Plan

## Date: 2025-09-02
## Issue: Improper file naming with suffixes violating VOS4 standards

## Files Requiring Renaming

### Current Issues Identified

| Current Name | Issue | Proper Name | Reasoning |
|-------------|-------|------------|-----------|
| `UIScrapingEngineOptimized.kt` | Suffix "Optimized" | `UIScrapingEngineV2.kt` | Version suffix for enhanced implementation |
| `UIScrapingEngineFixed.kt` | Suffix "Fixed" | `UIScrapingEngine.kt` | Replace original with corrected version |
| `AppCommandManagerOptimized.kt` | Suffix "Optimized" | `AppCommandManagerV2.kt` | Version suffix for enhanced implementation |
| `VoiceAccessibilityServiceOptimized.kt` | Suffix "Optimized" | `VoiceOSAccessibility.kt` | Clean name without unnecessary prefixes/suffixes |

## VOS4 Naming Standards

### Correct Patterns
- **Version Variants**: Use `V2`, `V3` suffix (e.g., `UIScrapingEngineV2.kt`)
- **Specialized Variants**: Use descriptive prefix (e.g., `EnhancedVoiceAccessibilityService.kt`)
- **Implementation Variants**: Use pattern suffix (e.g., `AppCommandManagerAsync.kt`)
- **Platform Variants**: Use platform suffix (e.g., `UIScrapingEngineAndroid12.kt`)

### Incorrect Patterns (Avoid)
- ❌ `*Optimized.kt` - Vague, all code should be optimized
- ❌ `*Fixed.kt` - Implies broken code exists
- ❌ `*New.kt` - Becomes outdated quickly
- ❌ `*Old.kt` - Use version control instead
- ❌ `*Temp.kt` - No temporary files in production
- ❌ `*Test.kt` - Unless in test directory

## Migration Strategy

### Phase 1: Immediate Actions
1. **UIScrapingEngineFixed.kt** → **UIScrapingEngine.kt**
   - This is the corrected version, should replace original
   - Delete original if it has issues
   - Or rename original to `UIScrapingEngineLegacy.kt` if needed for reference

2. **UIScrapingEngineOptimized.kt** → **UIScrapingEngineV2.kt**
   - Keep as V2 implementation with performance enhancements
   - Document that this has issues and needs fixes from "Fixed" version

### Phase 2: Proper Implementations
1. **AppCommandManagerOptimized.kt** → **AppCommandManagerV2.kt**
   - Version 2 with lazy loading and caching

2. **VoiceAccessibilityServiceOptimized.kt** → **VoiceOSAccessibility.kt**
   - Clean, clear naming for the main accessibility service

### Phase 3: Consolidation
1. Merge fixes from `UIScrapingEngineFixed.kt` into `UIScrapingEngineV2.kt`
2. Create single `UIScrapingEngineV2.kt` with all improvements
3. Update base `UIScrapingEngine.kt` if keeping legacy version

## File Renaming Commands

```bash
# Phase 1 - Immediate renaming
mv UIScrapingEngineFixed.kt UIScrapingEngineV2.kt
rm UIScrapingEngineOptimized.kt  # Remove flawed version

mv AppCommandManagerOptimized.kt AppCommandManagerV2.kt
mv VoiceAccessibilityServiceOptimized.kt VoiceOSAccessibility.kt
```

## Import Updates Required

### Files to Update
1. Any file importing `UIScrapingEngineOptimized`
2. Any file importing `UIScrapingEngineFixed`
3. Any file importing `AppCommandManagerOptimized`
4. Any file importing `VoiceAccessibilityServiceOptimized`

### Import Changes
```kotlin
// Before
import com.augmentalis.voiceaccessibility.extractors.UIScrapingEngineOptimized
import com.augmentalis.voiceaccessibility.extractors.UIScrapingEngineFixed
import com.augmentalis.voiceaccessibility.managers.AppCommandManagerOptimized
import com.augmentalis.voiceaccessibility.service.VoiceAccessibilityServiceOptimized

// After
import com.augmentalis.voiceaccessibility.extractors.UIScrapingEngineV2
import com.augmentalis.voiceaccessibility.managers.AppCommandManagerV2
import com.augmentalis.voiceaccessibility.service.VoiceOSAccessibility
```

## Class Name Updates

### Update Class Declarations
```kotlin
// Before
class UIScrapingEngineOptimized(...)
class UIScrapingEngineFixed(...)
class AppCommandManagerOptimized(...)
class VoiceAccessibilityServiceOptimized(...)

// After
class UIScrapingEngineV2(...)
class AppCommandManagerV2(...)
class VoiceOSAccessibility(...)
```

## Documentation Updates

### Update References in:
1. `PERFORMANCE_OPTIMIZATIONS.md`
2. `CODE_REVIEW_FIXES.md`
3. Any README files
4. Code comments referencing old names

## Validation Checklist

- [ ] All files renamed according to VOS4 standards
- [ ] No "Optimized", "Fixed", "New", "Old" suffixes remain
- [ ] All imports updated
- [ ] All class names updated
- [ ] Documentation updated
- [ ] Code compiles without errors
- [ ] No broken references

## Alternative Naming Options

If version suffixes are not preferred:

| Component | Alternative Names |
|-----------|------------------|
| UIScrapingEngine | `CachedUIScrapingEngine`, `ProfiledUIScrapingEngine` |
| AppCommandManager | `LazyAppCommandManager`, `CachedAppCommandManager` |
| VoiceAccessibilityService | `PerformanceVoiceAccessibilityService`, `FastVoiceAccessibilityService` |

## Recommendation

Use **Version Suffixes (V2)** for implementations that enhance existing components, and **Descriptive Prefixes** for specialized services. This follows VOS4 patterns seen in other modules.

---

**Author**: VOS4 Development Team
**Date**: 2025-09-02
**Status**: READY FOR EXECUTION