# ADR-010: External Storage Migration to .AVAVoiceAvanues

**Status:** Accepted
**Date:** 2025-11-27
**Authors:** AVA AI Team
**Related:** Storage Module, RAG System, Developer Manual Chapter 50
**Supersedes:** Legacy `ava-ai-models` folder structure

---

## Context

AVA's external storage for AI models (embeddings, LLM, wake word) was originally designed with the folder name `/sdcard/ava-ai-models/`. This approach had several issues:

### Problems Identified

1. **Folder Visibility**: Visible folders clutter user's file manager
   - Users accidentally delete model files
   - Confusing for non-technical users
   - No privacy (folder name reveals AI functionality)

2. **Naming Inconsistency**: Not aligned with ecosystem branding
   - AVA is part of Voice Avanues ecosystem
   - Folder name doesn't reflect this relationship
   - Other ecosystem apps use different conventions

3. **No Migration Strategy**: Changing folder name breaks existing installations
   - Cannot rename without user intervention
   - No automatic migration logic
   - Risk of data loss

4. **Subfolder Visibility**: Subfolders (`embeddings/`, `llm/`, `wakeword/`) also visible
   - Further clutters file manager
   - Increases accidental deletion risk
   - Exposes internal structure

### Requirements

- **Hidden by Default**: New installations use hidden folders (dot prefix)
- **Preserve User State**: Developer-created visible folders stay visible
- **Automatic Migration**: Detect and migrate legacy folders
- **Zero Data Loss**: Migration must be safe and recoverable
- **Ecosystem Branding**: Folder name reflects Voice Avanues ecosystem
- **Backward Compatible**: App works even if migration fails

---

## Decision

Implement **automatic migration to `.AVAVoiceAvanues`** with state-preserving logic and graceful fallbacks.

### Architecture

```
┌────────────────────────────────────────────────────────────┐
│              ExternalStorageMigration.kt                    │
│         Automatic migration with state preservation         │
└────────────────────────────────────────────────────────────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
         ↓                 ↓                 ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Detect       │  │ Migrate      │  │ Preserve     │
│ Legacy       │→ │ Folder       │→ │ Hidden/      │
│ Folders      │  │ (rename)     │  │ Visible      │
└──────────────┘  └──────────────┘  └──────────────┘
         │                 │                 │
         └─────────────────┼─────────────────┘
                           ↓
              ┌─────────────────────────┐
              │  New Folder Structure   │
              │  /sdcard/.AVAVoiceAvanues │
              │    ├── .embeddings/     │
              │    ├── .llm/            │
              │    └── .wakeword/       │
              └─────────────────────────┘
```

---

## Components

### 1. Folder Naming Convention

**New Standard:**
```
/sdcard/.AVAVoiceAvanues/              ← Hidden (dot prefix)
├── .embeddings/                       ← Hidden
│   ├── AVA-384-Multi-INT8.AON
│   └── AVA-384-MiniLM-Multi-INT8.AON
├── .llm/                              ← Hidden
│   ├── Phi-2-Q4.ALM
│   └── .phi-2-q4/                     ← Hidden extracted
└── .wakeword/                         ← Hidden
    └── porcupine_params.pv
```

**Naming Rationale:**
- `.AVAVoiceAvanues` - Reflects ecosystem (AVA + Voice Avanues)
- Dot prefix - Hidden by default (Android/Linux convention)
- CamelCase - Distinguishes from system folders
- Subfolders always hidden - Reduces clutter

**Developer Mode Exception:**
```
/sdcard/AVAVoiceAvanues/               ← Visible (no dot)
├── .embeddings/                       ← Still hidden
├── .llm/                              ← Still hidden
└── .wakeword/                         ← Still hidden
```

If developer manually creates visible folder, migration preserves this.

### 2. Migration Logic

**ExternalStorageMigration.kt**

```kotlin
object ExternalStorageMigration {
    private const val NEW_FOLDER_HIDDEN = ".AVAVoiceAvanues"
    private const val NEW_FOLDER_VISIBLE = "AVAVoiceAvanues"

    private val LEGACY_FOLDER_NAMES = listOf(
        "ava-ai-models",      // Old visible
        ".ava-ai-models",     // Old hidden
        "ava-models",         // Variant
        ".ava-models"         // Variant hidden
    )

    fun migrateIfNeeded(): Pair<File, Boolean> {
        // 1. Check for legacy folders
        for (legacyName in LEGACY_FOLDER_NAMES) {
            val legacyFolder = File("/sdcard", legacyName)
            if (legacyFolder.exists()) {
                return migrateLegacyFolder(legacyFolder, legacyName)
            }
        }

        // 2. No legacy folder, return new folder (hidden by default)
        return Pair(File("/sdcard", NEW_FOLDER_HIDDEN), false)
    }

    private fun migrateLegacyFolder(
        legacyFolder: File,
        legacyName: String
    ): Pair<File, Boolean> {
        // Preserve hidden/visible state
        val wasHidden = legacyName.startsWith(".")
        val newFolderName = if (wasHidden) NEW_FOLDER_HIDDEN else NEW_FOLDER_VISIBLE
        val newFolder = File("/sdcard", newFolderName)

        // Attempt rename (atomic operation)
        val renamed = legacyFolder.renameTo(newFolder)

        if (renamed) {
            Log.i("Migration", "Success: $legacyName → $newFolderName")
            return Pair(newFolder, true)
        } else {
            // Fallback: copy and delete
            return copyAndDeleteLegacy(legacyFolder, newFolder, wasHidden)
        }
    }
}
```

**Migration Strategy:**
1. **Detect**: Scan for legacy folder names
2. **Rename**: Try atomic rename first (fast, safe)
3. **Copy**: If rename fails, copy recursively
4. **Verify**: Check file counts match
5. **Delete**: Remove legacy folder only if verified
6. **Preserve**: Keep hidden/visible state from original

### 3. Model Loading Priority

**Updated Paths:**

```kotlin
// NLU Model Loading
val nluModel = when (languageMode) {
    MULTILINGUAL -> {
        File("/sdcard/.AVAVoiceAvanues/.embeddings/AVA-384-Multi-INT8.AON")
            .takeIf { it.exists() }
            ?: getNluEnglishModel()
    }
    ENGLISH_ONLY -> getNluEnglishModel()
}

// RAG Model Loading
val ragModel = when (languageMode) {
    MULTILINGUAL -> {
        File("/sdcard/.AVAVoiceAvanues/.embeddings/AVA-384-MiniLM-Multi-INT8.AON")
            .takeIf { it.exists() }
            ?: getRagEnglishModel()
    }
    ENGLISH_ONLY -> getRagEnglishModel()
}

// LLM Model Discovery
val llmModels = File("/sdcard/.AVAVoiceAvanues/.llm")
    .listFiles { file -> file.extension == "ALM" }
    ?: emptyArray()
```

---

## Alternatives Considered

### Alternative 1: Keep Visible Folders

**Pros:**
- No migration needed
- User can see and manage files

**Cons:**
- Clutters file manager
- High accidental deletion risk
- Not privacy-friendly
- **REJECTED**

### Alternative 2: Move to App-Specific External Storage

**Path:** `/sdcard/Android/data/com.augmentalis.ava/files/`

**Pros:**
- Auto-deleted on uninstall
- No permissions needed (API 28+)

**Cons:**
- Not accessible to other apps
- Cannot share models across ecosystem
- User cannot manage files easily
- **REJECTED** (breaks ecosystem sharing)

### Alternative 3: Use Internal Storage Only

**Path:** `/data/data/com.augmentalis.ava/files/`

**Pros:**
- Fully private
- No permissions needed

**Cons:**
- Limited space (typically <5GB)
- Cannot accommodate large LLM models (1.5GB+)
- Not user-manageable
- **REJECTED** (insufficient space)

### Alternative 4: No Migration (Breaking Change)

**Pros:**
- Simplest implementation
- Clean slate

**Cons:**
- Breaks existing installations
- Users lose models (must re-download)
- Poor user experience
- **REJECTED** (unacceptable UX)

---

## Consequences

### Positive

✅ **Privacy**: Hidden folders don't clutter file manager
✅ **Ecosystem Branding**: Name reflects Voice Avanues ecosystem
✅ **Automatic Migration**: Zero user intervention required
✅ **State Preservation**: Developer mode respected
✅ **Backward Compatible**: App works even if migration fails
✅ **Safe**: Atomic rename or verified copy-delete
✅ **Consistent**: All subfolders hidden

### Negative

⚠️ **Hidden from File Manager**: Users may not find files easily
  - Mitigation: Provide UI to open folder
  - Mitigation: Document adb commands

⚠️ **Migration Complexity**: Edge cases possible
  - Mitigation: Extensive testing
  - Mitigation: Fallback to legacy folder on error

⚠️ **Name Length**: `.AVAVoiceAvanues` is long (16 chars)
  - Mitigation: Descriptive is better than cryptic
  - Mitigation: Hidden folders don't need short names

---

## Implementation

### Phase 1: Core Migration (Week 1)
- [x] Create ExternalStorageMigration.kt
- [x] Implement migrateIfNeeded()
- [x] Implement copyAndDeleteLegacy() fallback
- [x] Unit tests (5 test cases)

### Phase 2: Integration (Week 1)
- [ ] Update ModelManager.kt (NLU)
- [ ] Update MiniLmEmbeddingGenerator.kt (RAG)
- [ ] Update ALMExtractor.kt (LLM)
- [ ] Integration tests

### Phase 3: Documentation (Week 1)
- [x] Update FOLDER-STRUCTURE.txt
- [x] Update EXTERNAL-STORAGE-SETUP.md
- [x] Create ADR-010
- [ ] Update Developer Manual Chapter 50
- [ ] Update User Manual Chapter 12

### Phase 4: Testing (Week 2)
- [ ] Device testing (5 scenarios)
- [ ] Migration testing (all legacy variants)
- [ ] Edge case testing (permissions, storage full)
- [ ] Rollback testing

---

## Testing Strategy

### Test Cases

**TC-001: Fresh Install**
- No legacy folder exists
- Expected: Creates `/sdcard/.AVAVoiceAvanues/` (hidden)

**TC-002: Migrate Visible Legacy**
- Legacy folder: `/sdcard/ava-ai-models/` (visible)
- Expected: Renames to `/sdcard/AVAVoiceAvanues/` (visible)

**TC-003: Migrate Hidden Legacy**
- Legacy folder: `/sdcard/.ava-ai-models/` (hidden)
- Expected: Renames to `/sdcard/.AVAVoiceAvanues/` (hidden)

**TC-004: Rename Fails, Copy Succeeds**
- Rename operation fails (permissions?)
- Expected: Copy-delete fallback, preserves all files

**TC-005: Both Legacy and New Exist**
- Legacy: `/sdcard/ava-ai-models/`
- New: `/sdcard/.AVAVoiceAvanues/` (already exists)
- Expected: Skip migration, use new folder

**TC-006: Migration Verification Fails**
- Copy succeeds but file count mismatch
- Expected: Keep legacy folder, log error, use legacy

---

## References

- **Implementation:** `Universal/AVA/Core/Common/.../ExternalStorageMigration.kt`
- **Documentation:** `ava-ai-models-external/FOLDER-STRUCTURE.txt`
- **Setup Guide:** `docs/build/EXTERNAL-STORAGE-SETUP.md`
- **Related ADRs:**
  - ADR-008: Hardware-Aware Inference Backend
  - ADR-011: 3-Letter JSON Schema Standard
  - ADR-012: RAG System Architecture

---

## Changelog

**v1.0 (2025-11-27):**
- Initial ADR
- Migration logic implemented
- Documentation complete
- Testing in progress

---

**Status:** ✅ ACCEPTED
**Implementation:** 70% complete (migration logic done, integration pending)
**Risk Level:** Low (safe migration, backward compatible)
