# Developer Manual - Chapter 50: External Storage Migration & .AVAVoiceAvanues

**Version**: 1.0
**Date**: 2025-11-27
**Author**: Manoj Jhawar
**Related ADR**: ADR-010-External-Storage-AVAVoiceAvanues

---

## Table of Contents

1. [Overview](#1-overview)
2. [Why External Storage?](#2-why-external-storage)
3. [Folder Structure](#3-folder-structure)
4. [Migration Architecture](#4-migration-architecture)
5. [ExternalStorageMigration API](#5-externalstoragem migration-api)
6. [Integration Guide](#6-integration-guide)
7. [Developer Modes](#7-developer-modes)
8. [Deployment Commands](#8-deployment-commands)
9. [Testing Migration](#9-testing-migration)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Overview

AVA AI uses external storage (`/sdcard/`) to store large AI models (embeddings, LLM, wake word) that cannot fit in the APK. As of version 1.1.0, AVA has migrated from the legacy `ava-ai-models` folder to the new `.AVAVoiceAvanues` folder structure.

### Key Features

- **Hidden by Default**: Dot prefix (`.AVAVoiceAvanues`) hides folders from file managers
- **Automatic Migration**: Detects and migrates legacy folder names automatically
- **State Preservation**: Respects developer-created visible folders
- **Ecosystem Branding**: Folder name reflects AVA + Voice Avanues ecosystem
- **Zero Data Loss**: Safe migration with graceful fallback

### Design Goals

1. **Privacy**: Hidden folders prevent accidental user deletion
2. **Consistency**: Same folder structure across all AVA ecosystem apps
3. **Safety**: Atomic rename or verified copy-delete migration
4. **Backward Compatible**: App works even if migration fails

---

## 2. Why External Storage?

### The Problem

Android APK size limits make bundling large models impractical:

| Model Type | Size | Location |
|------------|------|----------|
| **NLU English** | 25.5 MB | APK (assets) |
| **NLU Multilingual** | 113 MB | External storage |
| **RAG English** | 20 MB | APK (assets) |
| **RAG Multilingual** | 30 MB | External storage |
| **LLM (Gemma 2B)** | 1.5 GB | External storage |
| **Wake Word** | 2 MB | External storage |

**Total APK Size**: ~50 MB (acceptable)
**Total External Storage**: ~1.6 GB (user-controlled)

### Storage Strategy

AVA uses a **hybrid storage model**:

1. **APK (Internal)**: English models (always available)
2. **External Storage**: Multilingual models + LLM (optional upgrade)
3. **Hierarchical Loading**: External → Internal → Assets fallback

**Code Example:**
```kotlin
val nluModel = when (languageMode) {
    MULTILINGUAL -> {
        File("/sdcard/.AVAVoiceAvanues/.embeddings/AVA-384-Multi-INT8.AON")
            .takeIf { it.exists() }
            ?: getNluEnglishModel()  // Fallback to APK
    }
    ENGLISH_ONLY -> getNluEnglishModel()
}
```

---

## 3. Folder Structure

### New Standard (v1.1.0+)

```
/sdcard/.AVAVoiceAvanues/              ← HIDDEN (dot prefix)
│                                      ← Auto-migrates from legacy folders
│
├── .embeddings/                       ← HIDDEN (NLU + RAG models)
│   ├── AVA-384-Multi-INT8.AON         (113MB) NLU Multilingual (mALBERT)
│   ├── AVA-384-MiniLM-Multi-INT8.AON  (30MB) RAG Multilingual (MiniLM)
│   └── AVA-384-Base-INT8.AON          (22MB) English (legacy)
│
├── .llm/                              ← HIDDEN (LLM models)
│   ├── Phi-2-Q4.ALM                   (1.5GB) Proprietary archive
│   ├── Gemma-2B-Q4.ALM                (1.2GB) Proprietary archive
│   └── .phi-2-q4/                     ← Extracted (hidden)
│       ├── AVA-Phi-2-Q4.ADco          TVM device code
│       ├── AVALibrary.ADco            TVM library
│       ├── ava-model-config.json      3-letter schema
│       └── params_shard_*.bin         Model weights
│
└── .wakeword/                         ← HIDDEN (Wake word models)
    └── porcupine_params.pv            Porcupine model
```

### Legacy Folder Names (Pre-v1.1.0)

AVA automatically migrates from these legacy folder names:

- `/sdcard/ava-ai-models/` (visible)
- `/sdcard/.ava-ai-models/` (hidden)
- `/sdcard/ava-models/` (variant)
- `/sdcard/.ava-models/` (variant hidden)

**Migration Priority**: First match wins (top to bottom)

### Developer Mode Exception

If a developer manually creates a **visible** folder, AVA respects this:

```
/sdcard/AVAVoiceAvanues/               ← VISIBLE (no dot, developer created)
├── .embeddings/                       ← Still hidden
├── .llm/                              ← Still hidden
└── .wakeword/                         ← Still hidden
```

**Rule**: Parent folder visibility is preserved, but subfolders are **always hidden**.

---

## 4. Migration Architecture

### Component Diagram

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

### Migration Flow

```kotlin
┌─────────────────────────────────────┐
│  App Starts                         │
└──────────────┬──────────────────────┘
               │
               ↓
┌──────────────────────────────────────────────────────┐
│  ExternalStorageMigration.migrateIfNeeded()          │
└──────────────┬───────────────────────────────────────┘
               │
               ↓
        ┌──────┴──────┐
        │ Legacy      │
        │ Folder?     │
        └──────┬──────┘
               │
       ┌───────┴────────┐
       │ Yes            │ No
       ↓                ↓
┌──────────────┐  ┌─────────────────────┐
│ Migrate      │  │ Return new folder   │
│ (rename or   │  │ .AVAVoiceAvanues    │
│  copy)       │  │ (hidden by default) │
└──────┬───────┘  └─────────────────────┘
       │
       ↓
┌──────────────────┐
│ Preserve hidden/ │
│ visible state    │
└──────────────────┘
```

---

## 5. ExternalStorageMigration API

### Core Functions

#### `migrateIfNeeded(): Pair<File, Boolean>`

Detects legacy folders and migrates to new structure.

**Returns:**
- `Pair<File, Boolean>` - (folder, wasMigrated)
  - `folder`: Active external storage folder (new or legacy)
  - `wasMigrated`: `true` if migration occurred, `false` otherwise

**Usage:**
```kotlin
val (externalFolder, migrated) = ExternalStorageMigration.migrateIfNeeded()

if (migrated) {
    Log.i("Storage", "Migrated to: ${externalFolder.path}")
} else {
    Log.i("Storage", "Using folder: ${externalFolder.path}")
}
```

**Migration Logic:**
1. Scan for legacy folders (4 variants)
2. If found, migrate to new name (preserving hidden/visible state)
3. If not found, return new folder (hidden by default)
4. Attempt atomic `rename()` first
5. If rename fails, fall back to verified `copy + delete`

---

#### `getExternalStorageFolder(): File`

Get the active external storage folder (migrated or new).

**Returns:**
- `File` - The active folder (`/sdcard/.AVAVoiceAvanues` or visible variant)

**Usage:**
```kotlin
val folder = ExternalStorageMigration.getExternalStorageFolder()
// Returns: /sdcard/.AVAVoiceAvanues (or /sdcard/AVAVoiceAvanues if visible)
```

---

#### `getEmbeddingsFolder(): File`

Get the embeddings subfolder (auto-creates if missing).

**Returns:**
- `File` - The `.embeddings` folder (always hidden)

**Usage:**
```kotlin
val embeddingsFolder = ExternalStorageMigration.getEmbeddingsFolder()
// Returns: /sdcard/.AVAVoiceAvanues/.embeddings/
```

---

#### `getLLMFolder(): File`

Get the LLM models subfolder (auto-creates if missing).

**Returns:**
- `File` - The `.llm` folder (always hidden)

**Usage:**
```kotlin
val llmFolder = ExternalStorageMigration.getLLMFolder()
// Returns: /sdcard/.AVAVoiceAvanues/.llm/
```

---

#### `getWakeWordFolder(): File`

Get the wake word models subfolder (auto-creates if missing).

**Returns:**
- `File` - The `.wakeword` folder (always hidden)

**Usage:**
```kotlin
val wakewordFolder = ExternalStorageMigration.getWakeWordFolder()
// Returns: /sdcard/.AVAVoiceAvanues/.wakeword/
```

---

### Implementation Example

**File:** `Universal/AVA/Core/Common/src/androidMain/kotlin/com/augmentalis/ava/core/common/storage/ExternalStorageMigration.kt`

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

    private val externalStorageRoot = Environment.getExternalStorageDirectory()

    fun migrateIfNeeded(): Pair<File, Boolean> {
        // 1. Check for legacy folders
        for (legacyName in LEGACY_FOLDER_NAMES) {
            val legacyFolder = File(externalStorageRoot, legacyName)
            if (legacyFolder.exists() && legacyFolder.isDirectory) {
                return migrateLegacyFolder(legacyFolder, legacyName)
            }
        }

        // 2. No legacy folder, return new folder (hidden by default)
        val newFolder = File(externalStorageRoot, NEW_FOLDER_HIDDEN)
        if (!newFolder.exists()) {
            newFolder.mkdirs()
        }
        return Pair(newFolder, false)
    }

    private fun migrateLegacyFolder(
        legacyFolder: File,
        legacyName: String
    ): Pair<File, Boolean> {
        // Preserve hidden/visible state
        val wasHidden = legacyName.startsWith(".")
        val newFolderName = if (wasHidden) NEW_FOLDER_HIDDEN else NEW_FOLDER_VISIBLE
        val newFolder = File(externalStorageRoot, newFolderName)

        // Skip if new folder already exists
        if (newFolder.exists()) {
            Timber.i("Migration skipped: new folder already exists")
            return Pair(newFolder, false)
        }

        // Attempt rename (atomic operation)
        val renamed = legacyFolder.renameTo(newFolder)

        if (renamed) {
            Timber.i("Successfully migrated: $legacyName → $newFolderName")
            return Pair(newFolder, true)
        } else {
            // Fallback: copy and delete
            Timber.w("Rename failed, using copy-delete fallback")
            return copyAndDeleteLegacy(legacyFolder, newFolder, wasHidden)
        }
    }

    private fun copyAndDeleteLegacy(
        legacyFolder: File,
        newFolder: File,
        wasHidden: Boolean
    ): Pair<File, Boolean> {
        try {
            // Copy recursively
            legacyFolder.copyRecursively(newFolder, overwrite = false)

            // Verify file counts match
            val legacyCount = legacyFolder.walkTopDown().count()
            val newCount = newFolder.walkTopDown().count()

            if (legacyCount == newCount) {
                // Safe to delete legacy
                legacyFolder.deleteRecursively()
                Timber.i("Migration successful via copy-delete")
                return Pair(newFolder, true)
            } else {
                // Verification failed, keep legacy
                Timber.e("Migration verification failed: $legacyCount != $newCount")
                newFolder.deleteRecursively()
                return Pair(legacyFolder, false)
            }
        } catch (e: Exception) {
            Timber.e(e, "Migration failed, using legacy folder")
            return Pair(legacyFolder, false)
        }
    }

    fun getExternalStorageFolder(): File {
        val (folder, _) = migrateIfNeeded()
        return folder
    }

    fun getEmbeddingsFolder(): File {
        val root = getExternalStorageFolder()
        return File(root, ".embeddings").also {
            if (!it.exists()) it.mkdirs()
        }
    }

    fun getLLMFolder(): File {
        val root = getExternalStorageFolder()
        return File(root, ".llm").also {
            if (!it.exists()) it.mkdirs()
        }
    }

    fun getWakeWordFolder(): File {
        val root = getExternalStorageFolder()
        return File(root, ".wakeword").also {
            if (!it.exists()) it.mkdirs()
        }
    }
}
```

---

## 6. Integration Guide

### Step 1: Update Model Loading

Replace hardcoded paths with `ExternalStorageMigration` API:

**Before (Hardcoded Path):**
```kotlin
val modelFile = File("/sdcard/ava-ai-models/embeddings/AVA-384-Multi-INT8.AON")
```

**After (Migration-Aware):**
```kotlin
val embeddingsFolder = ExternalStorageMigration.getEmbeddingsFolder()
val modelFile = File(embeddingsFolder, "AVA-384-Multi-INT8.AON")
```

---

### Step 2: Hierarchical Model Loading

Implement fallback chain: External → Internal → Assets

**NLU Model Loading:**
```kotlin
fun loadNLUModel(languageMode: LanguageMode): File {
    return when (languageMode) {
        MULTILINGUAL -> {
            val external = File(
                ExternalStorageMigration.getEmbeddingsFolder(),
                "AVA-384-Multi-INT8.AON"
            )
            external.takeIf { it.exists() } ?: getNluEnglishModel()
        }
        ENGLISH_ONLY -> getNluEnglishModel()
    }
}

private fun getNluEnglishModel(): File {
    // APK assets fallback
    return File(context.filesDir, "malbert_v1.onnx")
}
```

**RAG Model Loading:**
```kotlin
fun loadRAGModel(languageMode: LanguageMode): File {
    return when (languageMode) {
        MULTILINGUAL -> {
            val external = File(
                ExternalStorageMigration.getEmbeddingsFolder(),
                "AVA-384-MiniLM-Multi-INT8.AON"
            )
            external.takeIf { it.exists() } ?: getRagEnglishModel()
        }
        ENGLISH_ONLY -> getRagEnglishModel()
    }
}

private fun getRagEnglishModel(): File {
    // APK assets fallback
    return File(context.filesDir, "minilm_v1.onnx")
}
```

**LLM Discovery:**
```kotlin
fun discoverLLMModels(): List<File> {
    val llmFolder = ExternalStorageMigration.getLLMFolder()
    return llmFolder.listFiles { file ->
        file.extension == "ALM"
    }?.toList() ?: emptyList()
}
```

---

### Step 3: Initialize Migration at App Startup

**In your Application class or MainActivity:**

```kotlin
class AVAApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Migrate external storage (if needed)
        val (folder, migrated) = ExternalStorageMigration.migrateIfNeeded()

        if (migrated) {
            Timber.i("External storage migrated to: ${folder.path}")
        } else {
            Timber.i("Using external storage: ${folder.path}")
        }
    }
}
```

**Estimated Migration Time**: ~100ms for 1.6GB of data (rename) or ~30s (copy-delete fallback)

---

## 7. Developer Modes

### Hidden Mode (Default)

**User downloads app → Creates hidden folder**

```
/sdcard/.AVAVoiceAvanues/    ← Hidden (dot prefix)
```

**Characteristics:**
- Not visible in file managers
- Reduces accidental deletion risk
- Standard for production users

---

### Visible Mode (Developer)

**Developer manually creates visible folder → AVA respects it**

```bash
# Create visible folder manually
adb shell mkdir -p /sdcard/AVAVoiceAvanues
```

**Result:**
```
/sdcard/AVAVoiceAvanues/     ← Visible (no dot)
├── .embeddings/             ← Still hidden
├── .llm/                    ← Still hidden
└── .wakeword/               ← Still hidden
```

**Characteristics:**
- Parent folder visible for easy access
- Subfolders always hidden (security)
- Useful for development/debugging

---

## 8. Deployment Commands

### Deploy Models to Device

**Step 1: Push Embedding Models**
```bash
# Multilingual NLU
adb push ava-ai-models-external/.embeddings/AVA-384-Multi-INT8.AON \
    /sdcard/.AVAVoiceAvanues/.embeddings/

# Multilingual RAG
adb push ava-ai-models-external/.embeddings/AVA-384-MiniLM-Multi-INT8.AON \
    /sdcard/.AVAVoiceAvanues/.embeddings/
```

**Step 2: Push LLM Models**
```bash
# Phi-2 4-bit quantized
adb push ava-ai-models-external/.llm/Phi-2-Q4.ALM \
    /sdcard/.AVAVoiceAvanues/.llm/

# Gemma 2B 4-bit quantized
adb push ava-ai-models-external/.llm/Gemma-2B-Q4.ALM \
    /sdcard/.AVAVoiceAvanues/.llm/
```

**Step 3: Verify Deployment**
```bash
# List all models
adb shell ls -lh /sdcard/.AVAVoiceAvanues/.embeddings/
adb shell ls -lh /sdcard/.AVAVoiceAvanues/.llm/

# Check folder sizes
adb shell du -sh /sdcard/.AVAVoiceAvanues/
```

---

### Clean Deployment (Fresh Install)

```bash
# Remove old folder (if exists)
adb shell rm -rf /sdcard/.AVAVoiceAvanues

# Create new structure
adb shell mkdir -p /sdcard/.AVAVoiceAvanues/.embeddings
adb shell mkdir -p /sdcard/.AVAVoiceAvanues/.llm
adb shell mkdir -p /sdcard/.AVAVoiceAvanues/.wakeword

# Deploy models (see Step 1-2 above)
```

---

## 9. Testing Migration

### Test Cases

**TC-001: Fresh Install (No Legacy Folder)**
```kotlin
@Test
fun testFreshInstall() {
    // Setup: No legacy folder exists
    val (folder, migrated) = ExternalStorageMigration.migrateIfNeeded()

    // Assert: Creates hidden folder
    assertEquals("/sdcard/.AVAVoiceAvanues", folder.path)
    assertFalse(migrated)
    assertTrue(folder.exists())
}
```

**TC-002: Migrate Visible Legacy Folder**
```kotlin
@Test
fun testMigrateVisibleLegacy() {
    // Setup: Create visible legacy folder
    val legacy = File("/sdcard/ava-ai-models")
    legacy.mkdirs()

    // Act: Migrate
    val (folder, migrated) = ExternalStorageMigration.migrateIfNeeded()

    // Assert: Migrates to visible new folder
    assertEquals("/sdcard/AVAVoiceAvanues", folder.path)
    assertTrue(migrated)
    assertFalse(legacy.exists())
}
```

**TC-003: Migrate Hidden Legacy Folder**
```kotlin
@Test
fun testMigrateHiddenLegacy() {
    // Setup: Create hidden legacy folder
    val legacy = File("/sdcard/.ava-ai-models")
    legacy.mkdirs()

    // Act: Migrate
    val (folder, migrated) = ExternalStorageMigration.migrateIfNeeded()

    // Assert: Migrates to hidden new folder
    assertEquals("/sdcard/.AVAVoiceAvanues", folder.path)
    assertTrue(migrated)
    assertFalse(legacy.exists())
}
```

**TC-004: Both Legacy and New Exist**
```kotlin
@Test
fun testBothFoldersExist() {
    // Setup: Both legacy and new exist
    val legacy = File("/sdcard/ava-ai-models")
    val newFolder = File("/sdcard/.AVAVoiceAvanues")
    legacy.mkdirs()
    newFolder.mkdirs()

    // Act: Migrate
    val (folder, migrated) = ExternalStorageMigration.migrateIfNeeded()

    // Assert: Uses new folder, skips migration
    assertEquals("/sdcard/.AVAVoiceAvanues", folder.path)
    assertFalse(migrated)
    assertTrue(legacy.exists())  // Legacy not deleted
}
```

**TC-005: Copy-Delete Fallback**
```kotlin
@Test
fun testCopyDeleteFallback() {
    // Setup: Legacy folder with files
    val legacy = File("/sdcard/ava-ai-models")
    legacy.mkdirs()
    File(legacy, "test-model.bin").writeText("data")

    // Mock: Rename fails (force fallback)
    // ... (implementation-specific)

    // Act: Migrate
    val (folder, migrated) = ExternalStorageMigration.migrateIfNeeded()

    // Assert: Copy-delete succeeded
    assertTrue(migrated)
    assertTrue(File(folder, "test-model.bin").exists())
    assertFalse(legacy.exists())
}
```

---

### Manual Testing on Device

**Test Scenario 1: Fresh Install**
```bash
# 1. Remove any existing folders
adb shell rm -rf /sdcard/.AVAVoiceAvanues /sdcard/AVAVoiceAvanues
adb shell rm -rf /sdcard/ava-ai-models /sdcard/.ava-ai-models

# 2. Start app
adb shell am start -n com.augmentalis.ava/.MainActivity

# 3. Verify hidden folder created
adb shell ls -la /sdcard/ | grep AVA
# Expected: .AVAVoiceAvanues (hidden)
```

**Test Scenario 2: Migration from Legacy**
```bash
# 1. Create legacy folder with test file
adb shell mkdir -p /sdcard/ava-ai-models/.embeddings
adb shell "echo 'test data' > /sdcard/ava-ai-models/.embeddings/test.txt"

# 2. Start app (triggers migration)
adb shell am start -n com.augmentalis.ava/.MainActivity

# 3. Verify migration
adb shell ls -la /sdcard/ | grep ava
# Expected: AVAVoiceAvanues (visible, preserved state)

# 4. Verify file migrated
adb shell cat /sdcard/AVAVoiceAvanues/.embeddings/test.txt
# Expected: "test data"
```

---

## 10. Troubleshooting

### Issue: Migration Fails Silently

**Symptoms:**
- App starts but uses legacy folder
- No error messages in logs

**Diagnosis:**
```bash
# Check logs for migration messages
adb logcat | grep -i "ExternalStorageMigration"
```

**Possible Causes:**
1. **Permissions**: App lacks `WRITE_EXTERNAL_STORAGE` permission
2. **Storage Full**: Not enough space for copy-delete fallback
3. **File Lock**: Another app has files locked

**Solutions:**
1. Grant permissions: `adb shell pm grant com.augmentalis.ava android.permission.WRITE_EXTERNAL_STORAGE`
2. Free up space: ~2x folder size needed for copy-delete
3. Force stop other apps: `adb shell am force-stop <package>`

---

### Issue: Folder Remains Visible After Migration

**Symptoms:**
- Folder migrated successfully
- But new folder is visible (no dot prefix)

**Diagnosis:**
```bash
# Check folder name
adb shell ls -la /sdcard/ | grep AVA
```

**Cause:**
- Legacy folder was **visible** (no dot prefix)
- Migration **correctly** preserves hidden/visible state

**Solution:**
This is **expected behavior**. AVA respects developer-created visible folders.

To force hidden:
```bash
# Rename manually
adb shell mv /sdcard/AVAVoiceAvanues /sdcard/.AVAVoiceAvanues

# Restart app
adb shell am force-stop com.augmentalis.ava
adb shell am start -n com.augmentalis.ava/.MainActivity
```

---

### Issue: Models Not Found After Migration

**Symptoms:**
- Migration successful
- But app shows "Model not found" errors

**Diagnosis:**
```bash
# List files in new folder
adb shell find /sdcard/.AVAVoiceAvanues -type f
```

**Possible Causes:**
1. **Subfolders Missing**: `.embeddings`, `.llm`, `.wakeword` not created
2. **Files Not Migrated**: Copy failed for some files
3. **Wrong Path in Code**: Code still uses legacy paths

**Solutions:**
1. Auto-create subfolders: `getEmbeddingsFolder()` creates if missing
2. Re-run migration with logging enabled
3. Update all code to use `ExternalStorageMigration` API (see Section 6)

---

### Issue: Permission Denied on Android 10+

**Symptoms:**
- Migration fails on Android 10+ devices
- Error: `java.io.FileNotFoundException: Permission denied`

**Cause:**
- Android 10+ enforces **Scoped Storage**
- Direct `/sdcard/` access restricted

**Solution:**
Use app-specific external directory with fallback:

```kotlin
fun getExternalStorageRoot(): File {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 10+ Scoped Storage
        context.getExternalFilesDir(null)
            ?: Environment.getExternalStorageDirectory()
    } else {
        // Android 9 and below
        Environment.getExternalStorageDirectory()
    }
}
```

**Updated Path (Android 10+):**
```
/sdcard/Android/data/com.augmentalis.ava/files/.AVAVoiceAvanues/
```

---

### Issue: Models Download Fails

**Symptoms:**
- User in multilingual mode
- App tries to download models but fails

**Diagnosis:**
```bash
# Check network connectivity
adb logcat | grep -i "download"
```

**Possible Causes:**
1. **No Internet**: Device offline
2. **Server Unavailable**: Download server down
3. **Storage Full**: Not enough space
4. **AOSP Detection**: App uses wrong download URL (Google Play vs AOSP)

**Solutions:**
1. Fallback to English APK model (always works)
2. Implement retry logic with exponential backoff
3. Check available space before download:
   ```kotlin
   val availableBytes = externalFolder.freeSpace
   if (availableBytes < modelSizeBytes) {
       // Show error: Not enough space
   }
   ```
4. Detect OS type:
   ```kotlin
   fun isAOSP(): Boolean {
       return !hasGooglePlayServices(context)
   }
   ```

---

## Related Documentation

- **ADR-010**: [External Storage Migration to .AVAVoiceAvanues](architecture/android/ADR-010-External-Storage-AVAVoiceAvanues.md)
- **ADR-011**: [3-Letter JSON Schema Standard](architecture/shared/ADR-011-3Letter-JSON-Schema-Standard.md)
- **Developer Manual Chapter 51**: [3-Letter JSON Schema](Developer-Manual-Chapter51-3Letter-JSON-Schema.md)
- **Folder Structure**: [ava-ai-models-external/FOLDER-STRUCTURE.txt](../ava-ai-models-external/FOLDER-STRUCTURE.txt)
- **External Storage Setup**: [docs/build/EXTERNAL-STORAGE-SETUP.md](build/EXTERNAL-STORAGE-SETUP.md)

---

**Version**: 1.0
**Date**: 2025-11-27
**Author**: Manoj Jhawar
**Maintained By**: AVA AI Team
