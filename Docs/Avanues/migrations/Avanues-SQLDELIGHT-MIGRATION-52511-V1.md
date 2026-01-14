# Avanues Android Modules Room → SQLDelight Migration Plan

**Repository:** Avanues
**Modules:**
1. `android/apps/voiceos/app/` - VoiceOS Android App
2. `android/standalone-libraries/uuidcreator/` - UUID Generator Library
**Priority:** LOW
**Estimated Duration:** 2-3 days
**Depends On:** None (independent modules)

---

## Current State

### 1. VoiceOS App (`android/apps/voiceos/app/`)

```kotlin
// Room dependencies in build.gradle.kts
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
```

### 2. UUID Creator (`android/standalone-libraries/uuidcreator/`)

```kotlin
// Room Database (AndroidX) - Hybrid storage with in-memory cache
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
```

### Already Migrated (Reference)

**AssetManager** already uses SQLDelight:
```kotlin
// Universal/Libraries/AvaElements/AssetManager/build.gradle.kts
id("app.cash.sqldelight") version "2.0.1"
```

Use AssetManager as reference for migration patterns.

---

## Migration Tasks

### Task 4.1: Migrate VoiceOS App

1. Add SQLDelight dependencies
2. Create schema files based on existing Room entities
3. Update database access in app
4. Test voice functionality

### Task 4.2: Migrate UUID Creator

1. Add SQLDelight dependencies
2. Create schema for UUID storage
3. Update UUID generator to use SQLDelight
4. Maintain backward compatibility

---

## Git Commits

```bash
# voiceos/app migration
git commit -m "refactor(db): migrate VoiceOS app from Room to SQLDelight (Phase 4.1)

- Add SQLDelight configuration
- Create voice data schema
- Update database access
- Remove Room dependencies

Part of: Room→SQLDelight Cross-Platform Migration Phase 4"

# uuidcreator migration
git commit -m "refactor(db): migrate uuidcreator from Room to SQLDelight (Phase 4.2)

- Add SQLDelight configuration
- Create UUID storage schema
- Update generator implementation
- Remove Room dependencies

Part of: Room→SQLDelight Cross-Platform Migration Phase 4"
```

---

## Verification

- [ ] VoiceOS app builds and runs
- [ ] Voice recordings save correctly
- [ ] UUID generator works
- [ ] UUID persistence verified
- [ ] No Room dependencies remain

---

**Parent Plan:** [ROOM-TO-SQLDELIGHT-MIGRATION-PLAN.md](/Volumes/M-Drive/Coding/AVA/docs/migrations/ROOM-TO-SQLDELIGHT-MIGRATION-PLAN.md)
