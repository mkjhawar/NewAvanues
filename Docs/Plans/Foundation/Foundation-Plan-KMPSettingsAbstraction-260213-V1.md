# Foundation-Plan-KMPSettingsAbstraction-260213-V1

## Overview
Extract app-level settings data models and persistence interfaces to Foundation KMP commonMain, enabling iOS/Desktop/Web to share settings infrastructure.

**Branch:** kmpvoiceos-update
**Platforms:** Android (existing), iOS (new), Desktop (new)
**Swarm:** Yes (3 independent workstreams)
**Estimated Tasks:** 12 | Sequential: 4h | Parallel: 2h

---

## Phase 1: Data Model Extraction (Foundation commonMain)

### Task 1.1: Create AvanuesSettings.kt in Foundation commonMain
- Move `data class AvanuesSettings` (pure Kotlin, 32 fields) from app
- Move `data class PersistedSynonym` (pure Kotlin, 4 fields) from app
- Include companion object with all default values
- Package: `com.augmentalis.foundation.settings.models`

### Task 1.2: Create DeveloperSettings.kt in Foundation commonMain
- Move `data class DeveloperSettings` (pure Kotlin, 8 fields) from app
- Include companion object with defaults
- Package: `com.augmentalis.foundation.settings.models`

### Task 1.3: Create SettingsKeys.kt in Foundation commonMain
- Extract all DataStore key NAME strings as constants
- Group by category (cursor, voice, theme, sync, developer)
- Package: `com.augmentalis.foundation.settings`

### Task 1.4: Create SettingsMigration.kt in Foundation commonMain
- Move `migrateVariantToPalette()` and `migrateVariantToStyle()` functions
- These are pure String→String mapping functions with no Android deps
- Package: `com.augmentalis.foundation.settings`

## Phase 2: Abstraction Interfaces (Foundation commonMain)

### Task 2.1: Create ISettingsStore interface
- Generic typed interface for settings read/write
- `fun settings(): Flow<T>` for reactive reading
- `suspend fun update(block: (T) -> T)` for atomic writes
- Package: `com.augmentalis.foundation.settings`

### Task 2.2: Create ICredentialStore interface
- `suspend fun store(key: String, value: String)`
- `suspend fun retrieve(key: String): String?`
- `suspend fun delete(key: String)`
- `suspend fun hasCredential(key: String): Boolean`
- Package: `com.augmentalis.foundation.settings`

## Phase 3: Update App Imports

### Task 3.1: Update AvanuesSettingsRepository.kt
- Import AvanuesSettings from Foundation instead of local
- Import PersistedSynonym from Foundation
- Import SettingsKeys from Foundation
- Import migration functions from Foundation
- Remove local data class definitions (they now live in Foundation)

### Task 3.2: Update DeveloperPreferences.kt
- Import DeveloperSettings from Foundation instead of local
- Remove local data class definition

### Task 3.3: Update all UI files importing settings models
- Grep for `com.augmentalis.voiceavanue.data.AvanuesSettings`
- Update to `com.augmentalis.foundation.settings.models.AvanuesSettings`
- Same for DeveloperSettings

## Phase 4: Build & Verify

### Task 4.1: Compile all affected modules
- `./gradlew :Modules:Foundation:compileDebugKotlinAndroid :apps:avanues:compileDebugKotlin`

### Task 4.2: Commit and push to kmpvoiceos-update + sync branches

---

## File Map

| New File (Foundation commonMain) | Source |
|--------------------------------|--------|
| `settings/models/AvanuesSettings.kt` | apps/avanues/.../data/AvanuesSettingsRepository.kt |
| `settings/models/DeveloperSettings.kt` | apps/avanues/.../data/DeveloperPreferences.kt |
| `settings/SettingsKeys.kt` | apps/avanues/.../data/AvanuesSettingsRepository.kt + DeveloperPreferences.kt |
| `settings/SettingsMigration.kt` | apps/avanues/.../data/AvanuesSettingsRepository.kt |
| `settings/ISettingsStore.kt` | New |
| `settings/ICredentialStore.kt` | New |

## Dependencies
- Foundation has NO new dependencies (pure Kotlin data classes + interfaces)
- App retains DataStore, Hilt, security-crypto deps for Android implementations
- kotlinx.coroutines.core already in Foundation (for Flow in ISettingsStore)
- kotlinx.serialization.json needed in Foundation for PersistedSynonym (check if already present)
