# Avanues ARG File Registry System

**Date:** 2025-11-10 05:40 PST
**Author:** System Architect
**Project:** Avanues Ecosystem
**Status:** Architecture Specification
**Version:** 1.0.0

---

## Executive Summary

This document specifies the **ARG (AvanueRegistry) File System** - a decentralized capability registration mechanism where apps self-declare their capabilities by placing `.ARG` files in a centralized registry folder.

**Key Concept:** Instead of API calls or manual database entries, apps simply drop a `{app-name}.ARG` file into the registry, and the Avanues Kernel automatically discovers and loads it into the local database.

---

## 1. System Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                    Device File System                          │
│                                                                │
│  /data/data/com.augmentalis.avanue.kernel/                    │
│  └── registry/                    ← Centralized Registry Folder│
│      ├── ava-ai.ARG                                           │
│      ├── taskmaker.ARG                                        │
│      ├── barcode-reader.ARG                                   │
│      ├── cockpit.ARG                                          │
│      └── avaconnect.ARG                                       │
│                                                                │
└─────────────────────────┬──────────────────────────────────────┘
                          │
                          │ File Watcher (inotify)
                          │
┌─────────────────────────▼──────────────────────────────────────┐
│              Avanues Kernel - Registry Scanner                 │
│                                                                │
│  Services:                                                     │
│  • File Watcher (detects new/changed .ARG files)              │
│  • ARG Parser (parses JSON/YAML)                              │
│  • Validator (schema validation)                              │
│  • Database Loader (inserts into SQLite)                      │
│  • Hot Reload (updates runtime when files change)             │
│                                                                │
└─────────────────────────┬──────────────────────────────────────┘
                          │
                          │ Parsed & Validated
                          │
┌─────────────────────────▼──────────────────────────────────────┐
│           Local Registry Database (SQLite)                     │
│                                                                │
│  Tables:                                                       │
│  • app_registry (loaded from ARG files)                       │
│  • provided_capabilities                                      │
│  • required_capabilities                                      │
│  • ipc_endpoints                                              │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## 2. ARG File Format

### 2.1 File Structure

**Filename:** `{app-id}.ARG` (e.g., `ava-ai.ARG`, `taskmaker.ARG`)

**Format:** JSON (with YAML support optional)

**Location:** `/data/data/com.augmentalis.avanue.kernel/registry/`

### 2.2 Schema Definition

```json
{
  "$schema": "https://avanues.com/schemas/arg-v1.json",
  "version": "1.0",
  "app": {
    "id": "com.augmentalis.avanue.ava",
    "name": "AVA AI Assistant",
    "display_name": "AVA",
    "version": "1.0.0",
    "type": "STANDALONE",
    "category": "AI",
    "description": "Privacy-first AI assistant with patented multi-LLM system",
    "icon": "res://drawable/ava_icon",
    "developer": {
      "name": "Augmentalis",
      "email": "support@augmentalis.com",
      "website": "https://avanues.com/apps/ava"
    }
  },
  "capabilities": {
    "provides": [
      {
        "id": "ai.text-generation",
        "version": "1.0.0",
        "name": "AI Text Generation",
        "description": "Generate natural language text using LLM models",
        "ipc": {
          "type": "AIDL",
          "interface": "com.augmentalis.avanue.ava.IAIService",
          "action": "com.augmentalis.avanue.ava.AI_SERVICE"
        },
        "permissions": [
          "android.permission.INTERNET"
        ],
        "rate_limits": {
          "max_requests_per_hour": 100,
          "max_concurrent": 5
        }
      },
      {
        "id": "ai.voice-recognition",
        "version": "1.0.0",
        "name": "Voice Recognition",
        "description": "Convert speech to text",
        "ipc": {
          "type": "AIDL",
          "interface": "com.augmentalis.avanue.ava.IVoiceService",
          "action": "com.augmentalis.avanue.ava.VOICE_SERVICE"
        }
      },
      {
        "id": "ai.translation",
        "version": "1.0.0",
        "name": "Language Translation",
        "description": "Translate text between 50+ languages",
        "ipc": {
          "type": "AIDL",
          "interface": "com.augmentalis.avanue.ava.ITranslationService",
          "action": "com.augmentalis.avanue.ava.TRANSLATION_SERVICE"
        }
      }
    ],
    "requires": []
  },
  "metadata": {
    "size_bytes": 125829120,
    "min_android_version": 26,
    "target_android_version": 34,
    "keywords": ["ai", "assistant", "voice", "text", "translation"],
    "license": "Proprietary",
    "privacy_policy": "https://avanues.com/privacy",
    "changelog_url": "https://avanues.com/apps/ava/changelog"
  },
  "registration": {
    "registered_at": "2025-11-10T05:40:00Z",
    "last_updated": "2025-11-10T05:40:00Z",
    "checksum": "sha256:abc123..."
  }
}
```

---

## 3. Example ARG Files

### 3.1 Standalone App: AVA AI

**File:** `/data/data/com.augmentalis.avanue.kernel/registry/ava-ai.ARG`

```json
{
  "version": "1.0",
  "app": {
    "id": "com.augmentalis.avanue.ava",
    "name": "AVA AI Assistant",
    "version": "1.0.0",
    "type": "STANDALONE",
    "category": "AI"
  },
  "capabilities": {
    "provides": [
      {
        "id": "ai.text-generation",
        "version": "1.0.0",
        "ipc": {
          "type": "AIDL",
          "interface": "com.augmentalis.avanue.ava.IAIService",
          "action": "com.augmentalis.avanue.ava.AI_SERVICE"
        }
      }
    ],
    "requires": []
  }
}
```

### 3.2 IPC-Dependent App: TaskMaker

**File:** `/data/data/com.augmentalis.avanue.kernel/registry/taskmaker.ARG`

```json
{
  "version": "1.0",
  "app": {
    "id": "com.augmentalis.avanue.taskmaker",
    "name": "TaskMaker",
    "version": "1.0.0",
    "type": "IPC_DEPENDENT",
    "category": "PRODUCTIVITY"
  },
  "capabilities": {
    "provides": [
      {
        "id": "tasks.management",
        "version": "1.0.0",
        "ipc": {
          "type": "CONTENT_PROVIDER",
          "authority": "com.augmentalis.avanue.taskmaker.provider",
          "content_uri": "content://com.augmentalis.avanue.taskmaker.provider/tasks"
        }
      }
    ],
    "requires": [
      {
        "id": "ui.avaelements",
        "min_version": "2.0.0",
        "source": "KERNEL"
      },
      {
        "id": "database.magicdata",
        "min_version": "1.5.0",
        "source": "KERNEL"
      },
      {
        "id": "ai.text-generation",
        "min_version": "1.0.0",
        "optional": true,
        "fallback": "basic_mode"
      }
    ]
  },
  "metadata": {
    "size_bytes": 3145728,
    "keywords": ["tasks", "todo", "productivity", "ai"]
  }
}
```

### 3.3 Kernel Self-Registration

**File:** `/data/data/com.augmentalis.avanue.kernel/registry/avanues-kernel.ARG`

```json
{
  "version": "1.0",
  "app": {
    "id": "com.augmentalis.avanue.kernel",
    "name": "Avanues Kernel",
    "version": "1.0.0",
    "type": "SYSTEM",
    "category": "SYSTEM"
  },
  "capabilities": {
    "provides": [
      {
        "id": "ui.avaelements",
        "version": "2.0.1",
        "name": "AvaElements UI Framework",
        "ipc": {
          "type": "SYSTEM",
          "note": "Embedded in kernel, no IPC needed"
        }
      },
      {
        "id": "database.magicdata",
        "version": "1.5.0",
        "name": "MagicData Database System",
        "ipc": {
          "type": "CONTENT_PROVIDER",
          "authority": "com.augmentalis.avanue.kernel.magicdata",
          "content_uri": "content://com.augmentalis.avanue.kernel.magicdata"
        }
      },
      {
        "id": "connectivity.avaconnect",
        "version": "1.0.0",
        "name": "AvaConnect Connectivity Framework",
        "ipc": {
          "type": "AIDL",
          "interface": "com.augmentalis.avanue.kernel.IAvaConnect",
          "action": "com.augmentalis.avanue.kernel.AVACONNECT"
        }
      }
    ],
    "requires": []
  }
}
```

---

## 4. Registry Folder Structure

```
/data/data/com.augmentalis.avanue.kernel/
├── registry/
│   ├── avanues-kernel.ARG           ← System capabilities
│   ├── ava-ai.ARG                   ← Standalone app
│   ├── taskmaker.ARG                ← IPC-dependent app
│   ├── barcode-reader.ARG           ← IPC-dependent app
│   ├── cockpit.ARG                  ← Standalone app
│   ├── docreader.ARG                ← IPC-dependent app
│   └── avaconnect.ARG               ← Native app
│
├── registry-backup/                 ← Backup before updates
│   └── {timestamp}/
│       └── *.ARG
│
└── registry-invalid/                ← Failed validation
    └── {app-id}.ARG.invalid
```

### 4.1 Access Permissions

```xml
<!-- Avanues Kernel owns the registry folder -->
<manifest>
    <permission
        android:name="com.augmentalis.avanue.WRITE_REGISTRY"
        android:protectionLevel="signature" />

    <permission
        android:name="com.augmentalis.avanue.READ_REGISTRY"
        android:protectionLevel="normal" />
</manifest>

<!-- Apps can write their own ARG file on install -->
<!-- Kernel enforces: each app can only write {app-package-name}.ARG -->
```

---

## 5. ARG File Lifecycle

### 5.1 App Installation Flow

```kotlin
class AppInstaller {

    fun onPackageInstalled(packageName: String) {
        // Step 1: App writes its ARG file
        val argFile = File(
            "/data/data/com.augmentalis.avanue.kernel/registry",
            "${packageName}.ARG"
        )

        // Step 2: Kernel detects new file (FileObserver)
        registryScanner.onFileCreated(argFile)

        // Step 3: Parse and validate
        val manifest = argParser.parse(argFile)
        val validation = argValidator.validate(manifest)

        if (!validation.isValid) {
            // Move to invalid folder
            moveToInvalid(argFile, validation.errors)
            notifyApp(packageName, "ARG validation failed: ${validation.errors}")
            return
        }

        // Step 4: Load into database
        registryDatabase.insertApp(manifest)

        // Step 5: Notify system
        broadcastCapabilityChanged(manifest.capabilities.provides)
    }
}
```

### 5.2 App Update Flow

```kotlin
fun onPackageUpdated(packageName: String) {
    val argFile = File(registryPath, "${packageName}.ARG")

    if (!argFile.exists()) {
        log.warn("App $packageName updated but no ARG file found")
        return
    }

    // Backup old file
    backupARGFile(argFile)

    // Parse new version
    val newManifest = argParser.parse(argFile)

    // Check for breaking changes
    val oldManifest = registryDatabase.getAppManifest(packageName)
    val breakingChanges = detectBreakingChanges(oldManifest, newManifest)

    if (breakingChanges.isNotEmpty()) {
        // Notify dependent apps
        notifyDependentApps(packageName, breakingChanges)
    }

    // Update database
    registryDatabase.updateApp(newManifest)

    // Reload IPC endpoints
    ipcManager.reloadEndpoints(packageName)
}
```

### 5.3 App Uninstall Flow

```kotlin
fun onPackageRemoved(packageName: String) {
    val argFile = File(registryPath, "${packageName}.ARG")

    // Get capabilities before removal
    val manifest = registryDatabase.getAppManifest(packageName)

    // Check if any apps depend on this
    val dependents = registryDatabase.findDependents(
        manifest.capabilities.provides.map { it.id }
    )

    if (dependents.isNotEmpty()) {
        // Show warning to user
        showUninstallWarning(
            app = packageName,
            affectedApps = dependents,
            message = """
                Uninstalling ${manifest.app.name} will break:
                ${dependents.joinToString("\n") { "• ${it.name}" }}
            """
        )
    }

    // Remove from database
    registryDatabase.removeApp(packageName)

    // Delete ARG file
    argFile.delete()

    // Broadcast capability removed
    broadcastCapabilityRemoved(manifest.capabilities.provides)
}
```

---

## 6. File Watcher & Hot Reload

### 6.1 File Observer

```kotlin
class ARGFileWatcher(private val registryPath: String) {

    private val observer = object : FileObserver(
        registryPath,
        CREATE or MODIFY or DELETE or MOVED_TO or MOVED_FROM
    ) {
        override fun onEvent(event: Int, path: String?) {
            if (path == null || !path.endsWith(".ARG")) return

            val argFile = File(registryPath, path)

            when (event and ALL_EVENTS) {
                CREATE, MOVED_TO -> {
                    log.info("New ARG file detected: $path")
                    handleFileCreated(argFile)
                }

                MODIFY -> {
                    log.info("ARG file modified: $path")
                    handleFileModified(argFile)
                }

                DELETE, MOVED_FROM -> {
                    log.info("ARG file removed: $path")
                    handleFileDeleted(argFile)
                }
            }
        }
    }

    fun startWatching() {
        observer.startWatching()
        log.info("ARG file watcher started for: $registryPath")
    }

    fun stopWatching() {
        observer.stopWatching()
    }

    private fun handleFileCreated(file: File) {
        coroutineScope.launch {
            try {
                // Parse and validate
                val manifest = argParser.parse(file)
                val validation = argValidator.validate(manifest)

                if (validation.isValid) {
                    // Insert into database
                    registryDatabase.insertApp(manifest)

                    // Notify capability added
                    eventBus.post(CapabilityAddedEvent(manifest))
                } else {
                    // Move to invalid folder
                    moveToInvalid(file, validation.errors)
                }
            } catch (e: Exception) {
                log.error("Failed to process ARG file: ${file.name}", e)
            }
        }
    }

    private fun handleFileModified(file: File) {
        coroutineScope.launch {
            try {
                // Re-parse and update
                val manifest = argParser.parse(file)
                val validation = argValidator.validate(manifest)

                if (validation.isValid) {
                    registryDatabase.updateApp(manifest)
                    eventBus.post(CapabilityUpdatedEvent(manifest))
                } else {
                    moveToInvalid(file, validation.errors)
                }
            } catch (e: Exception) {
                log.error("Failed to update ARG file: ${file.name}", e)
            }
        }
    }

    private fun handleFileDeleted(file: File) {
        val appId = file.nameWithoutExtension
        registryDatabase.removeApp(appId)
        eventBus.post(CapabilityRemovedEvent(appId))
    }
}
```

### 6.2 Hot Reload

```kotlin
class CapabilityHotReload {

    fun onCapabilityAdded(event: CapabilityAddedEvent) {
        val manifest = event.manifest

        // Notify all running apps
        manifest.capabilities.provides.forEach { capability ->
            // Broadcast to apps waiting for this capability
            val intent = Intent("com.augmentalis.avanue.CAPABILITY_AVAILABLE")
            intent.putExtra("capability_id", capability.id)
            intent.putExtra("provider_app", manifest.app.id)
            context.sendBroadcast(intent)
        }

        // Update runtime capability cache
        capabilityCache.refresh()

        // Log analytics
        analytics.log("capability_registered", mapOf(
            "app_id" to manifest.app.id,
            "capabilities" to manifest.capabilities.provides.map { it.id }
        ))
    }

    fun onCapabilityRemoved(event: CapabilityRemovedEvent) {
        // Find apps that depend on removed capabilities
        val dependents = registryDatabase.findDependents(event.appId)

        // Notify them
        dependents.forEach { dependent ->
            val intent = Intent("com.augmentalis.avanue.CAPABILITY_UNAVAILABLE")
            intent.setPackage(dependent.appId)
            intent.putExtra("capability_id", event.capabilityIds)
            context.sendBroadcast(intent)
        }

        // Clear capability cache
        capabilityCache.invalidate(event.appId)
    }
}
```

---

## 7. Validation & Security

### 7.1 ARG Validator

```kotlin
class ARGValidator {

    fun validate(manifest: AppManifest): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Required fields
        if (manifest.app.id.isBlank()) {
            errors.add(ValidationError("app.id is required"))
        }

        // App ID format (reverse domain notation)
        if (!manifest.app.id.matches(Regex("^[a-z0-9._]+$"))) {
            errors.add(ValidationError("app.id must be lowercase with dots/underscores"))
        }

        // Version format (semver)
        if (!manifest.app.version.matches(Regex("^\\d+\\.\\d+\\.\\d+$"))) {
            errors.add(ValidationError("app.version must be semver (e.g., 1.0.0)"))
        }

        // Capability IDs (no duplicates)
        val provideIds = manifest.capabilities.provides.map { it.id }
        if (provideIds.size != provideIds.distinct().size) {
            errors.add(ValidationError("Duplicate capability IDs in 'provides'"))
        }

        // IPC configuration
        manifest.capabilities.provides.forEach { capability ->
            when (capability.ipc.type) {
                "AIDL" -> {
                    if (capability.ipc.interface.isNullOrBlank()) {
                        errors.add(ValidationError(
                            "Capability ${capability.id}: AIDL requires 'interface' field"
                        ))
                    }
                }
                "CONTENT_PROVIDER" -> {
                    if (capability.ipc.authority.isNullOrBlank()) {
                        errors.add(ValidationError(
                            "Capability ${capability.id}: ContentProvider requires 'authority'"
                        ))
                    }
                }
            }
        }

        // Required capabilities exist
        manifest.capabilities.requires.forEach { required ->
            // Check if any installed app provides this capability
            val providers = registryDatabase.findProviders(required.id)
            if (providers.isEmpty() && !required.optional) {
                errors.add(ValidationError(
                    "Required capability '${required.id}' has no providers"
                ))
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}
```

### 7.2 Security Checks

```kotlin
class ARGSecurityChecker {

    fun verifyFileOwnership(argFile: File, packageName: String): Boolean {
        // Ensure file name matches package name
        val fileName = argFile.nameWithoutExtension
        if (fileName != packageName) {
            log.warn("Package $packageName tried to write ${fileName}.ARG (denied)")
            return false
        }

        // Verify file permissions (only owner can write)
        if (!argFile.canWrite()) {
            log.warn("ARG file ${argFile.name} is not writable")
            return false
        }

        return true
    }

    fun checkSignature(argFile: File, expectedSignature: String): Boolean {
        // Verify file hasn't been tampered with
        val actualChecksum = calculateChecksum(argFile)
        return actualChecksum == expectedSignature
    }
}
```

---

## 8. CLI Tools

### 8.1 ARG File Generator

```bash
# Generate ARG file from template
$ avanues arg generate --app-id com.example.myapp --type standalone

Generated: /data/data/com.augmentalis.avanue.kernel/registry/com.example.myapp.ARG

Edit this file to declare your capabilities, then:
  avanues arg validate com.example.myapp.ARG
  avanues arg install com.example.myapp.ARG
```

### 8.2 ARG Validator

```bash
# Validate ARG file
$ avanues arg validate taskmaker.ARG

✓ Schema valid
✓ App ID format correct
✓ Version format valid (1.0.0)
⚠ Warning: Optional capability 'ai.text-generation' has no providers
✓ All required capabilities available

Result: VALID (1 warning)
```

### 8.3 Registry Inspector

```bash
# List all registered apps
$ avanues registry list

Registered Apps (6):
  1. avanues-kernel (SYSTEM) - v1.0.0
     Provides: ui.avaelements, database.magicdata, connectivity.avaconnect

  2. ava-ai (STANDALONE) - v1.0.0
     Provides: ai.text-generation, ai.voice-recognition, ai.translation

  3. taskmaker (IPC_DEPENDENT) - v1.0.0
     Provides: tasks.management
     Requires: ui.avaelements, database.magicdata, ai.text-generation (optional)

# Show ARG file contents
$ avanues registry show ava-ai

File: /data/data/com.augmentalis.avanue.kernel/registry/ava-ai.ARG
{
  "app": { "id": "com.augmentalis.avanue.ava", ... },
  "capabilities": { "provides": [ ... ] }
}

# Reload registry from disk
$ avanues registry reload

Scanning registry folder...
✓ Loaded 6 ARG files
✓ Database updated
```

---

## 9. Benefits

### For Apps:
- 📝 **Self-registration** - Just drop a file, no API calls
- 🔄 **Hot reload** - Update capabilities without restart
- ✅ **Validation** - Schema validation before registration
- 🔍 **Discoverability** - Automatic detection by kernel

### For Kernel:
- 📁 **File-based** - Simple scanning, no complex APIs
- 🔒 **Secure** - File permissions enforce ownership
- 🚀 **Fast** - Direct file reads, no network
- 💾 **Backup** - Easy to backup/restore registry

### For Developers:
- 🛠️ **Standard format** - Same schema for all apps
- 📚 **Human-readable** - JSON format, easy to edit
- 🧪 **Testable** - Can validate before deployment
- 📖 **Self-documenting** - Capabilities declared in one file

---

## 10. Implementation Roadmap

### Phase 1: Core (Week 1)
- [ ] Define ARG JSON schema
- [ ] Implement ARG parser
- [ ] Implement ARG validator
- [ ] Setup registry folder structure

### Phase 2: Scanner (Week 1)
- [ ] Implement FileObserver for registry folder
- [ ] Build ARG file loader
- [ ] Load ARG files into database
- [ ] Handle validation errors

### Phase 3: Lifecycle (Week 2)
- [ ] Integrate with PackageManager
- [ ] Handle app install/update/uninstall
- [ ] Implement hot reload
- [ ] Add event broadcasting

### Phase 4: Tools (Week 1)
- [ ] Build ARG generator CLI
- [ ] Build ARG validator CLI
- [ ] Build registry inspector CLI
- [ ] Write developer documentation

---

## Next Steps

**READY TO IMPLEMENT!**

Should I:
1. **Create ARG schema files** (JSON schema + Kotlin data classes)
2. **Implement ARG parser** (read files → database)
3. **Build file watcher** (detect new ARG files)
4. **Fix YamlParser.kt first** (still blocking)

What's your priority?

---

**Author:** Manoj Jhawar, manoj@ideahq.net
**Created:** 2025-11-10 05:40:20 PST
