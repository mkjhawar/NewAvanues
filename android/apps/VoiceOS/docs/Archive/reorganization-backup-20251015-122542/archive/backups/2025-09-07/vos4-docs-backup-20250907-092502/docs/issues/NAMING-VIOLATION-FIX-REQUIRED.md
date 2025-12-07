# NAMING VIOLATION FIX REQUIRED

**Date:** 2025-09-03
**Issue:** Version suffixes violate naming conventions

## Problem Identified

Created files with version suffixes which violates the NO SUFFIX/PREFIX rule:
- ❌ `UIScrapingEngineV2.kt` 
- ❌ `UIScrapingEngineV3.kt`
- ❌ Multiple locations with same violations

## Correct Approach

Per project instructions:
- **NO version numbers** (V2, V3, etc.)
- **NO suffixes** like "New", "Refactored", "Enhanced"
- **NO prefixes** that indicate versions

## Files to Fix

1. `/apps/VoiceAccessibility/.../UIScrapingEngineV2.kt` → DELETE or MERGE into main
2. `/apps/VoiceAccessibility/.../UIScrapingEngineV3.kt` → DELETE or MERGE into main  
3. `/modules/VoiceAccessibility/.../UIScrapingEngineV3.kt` → DELETE

## Correct Solution

The main `UIScrapingEngine.kt` should be ENHANCED directly with new features, not duplicated with version numbers.

## Similar Issues Found

Need to check for other violations:
- Files ending in "New" (like VivokaEngineNew.kt)
- Files ending in "Refactored" (like AndroidSTTEngineRefactored.kt)
- Any other version-indicating suffixes

## Action Required

1. Merge all improvements into the main files
2. Delete version-suffixed files
3. Update all imports
4. Ensure single source of truth for each component