# VoiceUI-CGPT Adaptation Guide
## Porting CGPT Code to MagicUI with ObjectBox→Room Migration

**Document:** 09 of 12  
**Version:** 1.0  
**Created:** 2025-10-13  
**Status:** Implementation Ready  

---

## Overview

Complete guide for adapting VoiceUI-CGPT code into MagicUI:
- **File-by-file porting strategy**
- **ObjectBox → Room migration**
- **Namespace changes** (`com.voiceui.cgpt` → `com.augmentalis.magicui`)
- **VOS4 integration** (replace CGPT services with VOS4)
- **Code adaptation patterns**

**Key Changes:**
1. Replace ObjectBox with Room
2. Update namespaces
3. Integrate with VOS4 (UUIDCreator, CommandManager, etc.)
4. Simplify API to match VOS4 style

---

## 1. Overview of CGPT Codebase

### 1.1 VoiceUI-CGPT Structure (What We Have)

```
VoiceUI-CGPT/
├── runtime/              (24 files) - Runtime engine
├── preview/              (8 files) - Live preview system
├── debug/                (12 files) - Debug tools
├── plugin/               (10 files) - Plugin architecture
├── tests/                (15 files) - Testing framework
├── theme/                (12 files) - Theme system
├── layout/               (12 files) - Layout engine
├── models/               (20 files) - Data models
└── uuid/                 (4 files) - UUID system (replace with VOS4)
```

**What to Port:**
- ✅ Runtime engine (adapt)
- ✅ Preview system (adapt)
- ✅ Debug tools (adapt)
- ✅ Plugin architecture (adapt)
- ✅ Testing framework (adapt)
- ⚠️ Theme system (merge with our themes)
- ❌ UUID system (use VOS4 UUIDCreator instead)

---

## 2. ObjectBox → Room Migration

### 2.1 ObjectBox Patterns

**ObjectBox Code (CGPT):**
```kotlin
// ObjectBox entity
@Entity
data class ComponentData(
    @Id var id: Long = 0,
    var name: String = "",
    var type: String = "",
    var metadata: String = ""
)

// ObjectBox box access
val box = ObjectBox.boxStore.boxFor(ComponentData::class.java)

// ObjectBox operations
box.put(component)                    // Save
val all = box.all                     // Get all
val found = box.query()
    .equal(ComponentData_.name, "button")
    .build()
    .find()                           // Query
box.remove(component)                 // Delete
```

### 2.2 Room Equivalent

**Room Code (MagicUI):**
```kotlin
// Room entity (use @MagicEntity)
@MagicEntity
data class ComponentData(
    val id: Long = 0,  // Auto-increment
    val name: String,
    val type: String,
    val metadata: String
)

// Room operations (via MagicDB)
MagicDB.save(component)               // Save
val all = MagicDB.getAll<ComponentData>()  // Get all
val found = MagicDB.query<ComponentData>(
    "SELECT * FROM componentdata WHERE name = ?",
    "button"
)                                     // Query
MagicDB.delete(component)             // Delete
```

### 2.3 Migration Pattern

```kotlin
// STEP 1: Replace ObjectBox imports
// REMOVE:
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

// ADD:
import androidx.room.*
import com.augmentalis.magicui.database.MagicDB
import com.augmentalis.magicui.annotations.MagicEntity

// STEP 2: Update entity annotations
// BEFORE (ObjectBox):
@Entity
data class MyData(
    @Id var id: Long = 0,
    var field: String = ""
)

// AFTER (Room):
@MagicEntity
data class MyData(
    val id: Long = 0,
    val field: String
)

// STEP 3: Replace box access
// BEFORE (ObjectBox):
private val box: Box<MyData> = ObjectBox.boxStore.boxFor(MyData::class.java)

// AFTER (Room):
// No field needed - use MagicDB directly

// STEP 4: Update CRUD operations
// BEFORE (ObjectBox):
box.put(data)
val all = box.all
val query = box.query().equal(MyData_.field, "value").build().find()
box.remove(data)

// AFTER (Room):
MagicDB.save(data)
val all = MagicDB.getAll<MyData>()
val query = MagicDB.query<MyData>("SELECT * FROM mydata WHERE field = ?", "value")
MagicDB.delete(data)
```

---

## 3. File-by-File Adaptation

### 3.1 Runtime Engine Files

**Source:** `VoiceUI-CGPT/runtime/`
**Target:** `MagicUI/src/main/java/com/augmentalis/magicui/runtime/`

| CGPT File | Action | Changes Needed |
|-----------|--------|----------------|
| `RuntimeConfig.kt` | ✅ Port | Update namespaces |
| `CommandExecutionSnapshot.kt` | ✅ Port | ObjectBox → Room |
| `CommandMetricsTracker.kt` | ✅ Port | ObjectBox → Room |
| `LayoutHotReloadEngine.kt` | ✅ Port | Minimal changes |
| `VoiceUIModeSwitcher.kt` | ✅ Port | Update to VOS4 |
| `CommandDelayBuffer.kt` | ✅ Port | Minimal changes |
| `IntentHistoryChain.kt` | ✅ Port | ObjectBox → Room |
| All others | ✅ Port | Various adaptations |

**Example Adaptation:**

**CGPT Original (`CommandMetricsTracker.kt`):**
```kotlin
package com.voiceui.cgpt.runtime

import io.objectbox.Box
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class CommandMetric(
    @Id var id: Long = 0,
    var commandName: String = "",
    var executionTime: Long = 0,
    var timestamp: Long = System.currentTimeMillis()
)

class CommandMetricsTracker(private val box: Box<CommandMetric>) {
    fun track(commandName: String, executionTime: Long) {
        box.put(CommandMetric(
            commandName = commandName,
            executionTime = executionTime
        ))
    }
    
    fun getMetrics(): List<CommandMetric> = box.all
}
```

**MagicUI Adapted:**
```kotlin
package com.augmentalis.magicui.runtime

import com.augmentalis.magicui.database.MagicDB
import com.augmentalis.magicui.annotations.MagicEntity

@MagicEntity
data class CommandMetric(
    val id: Long = 0,
    val commandName: String,
    val executionTime: Long,
    val timestamp: Long = System.currentTimeMillis()
)

class CommandMetricsTracker {
    suspend fun track(commandName: String, executionTime: Long) {
        MagicDB.save(CommandMetric(
            commandName = commandName,
            executionTime = executionTime
        ))
    }
    
    suspend fun getMetrics(): List<CommandMetric> = MagicDB.getAll<CommandMetric>()
}
```

**Changes Made:**
1. ✅ Namespace updated
2. ✅ ObjectBox imports → Room/MagicDB
3. ✅ `@Entity` → `@MagicEntity`
4. ✅ `var` → `val` (immutable)
5. ✅ `Box<T>` → `MagicDB` API
6. ✅ Functions now `suspend` (coroutines)

---

## 4. Preview System Adaptation

### 4.1 Hot Reload Engine

**Source:** `VoiceUI-CGPT/preview/LayoutHotReloadEngine.kt`

**Adaptation Strategy:**
- ✅ Keep hot reload logic
- ⚠️ Replace file watching with Compose hot reload
- ✅ Integrate with MagicUIModule

**Adapted Code:**
```kotlin
package com.augmentalis.magicui.preview

import androidx.compose.runtime.*
import kotlinx.coroutines.*

/**
 * Hot reload engine for MagicUI
 * Adapted from VoiceUI-CGPT
 */
class HotReloadEngine(
    private val scope: CoroutineScope
) {
    
    private val _reloadTrigger = mutableStateOf(0)
    val reloadTrigger: State<Int> = _reloadTrigger
    
    /**
     * Trigger hot reload
     */
    fun reload() {
        _reloadTrigger.value += 1
    }
    
    /**
     * Watch for code changes
     */
    fun startWatching() {
        scope.launch {
            // In development, trigger reload on code save
            // Uses Android Studio's hot reload hook
            while (isActive) {
                delay(1000)
                // Check for changes
                if (hasCodeChanged()) {
                    reload()
                }
            }
        }
    }
    
    private fun hasCodeChanged(): Boolean {
        // Hook into IDE's file watching
        return false  // Simplified
    }
}
```

---

## 5. Plugin Architecture Adaptation

### 5.1 Plugin System

**Source:** `VoiceUI-CGPT/plugin/PluginManager.kt`

**Adaptation:** Minimal changes, already compatible

```kotlin
package com.augmentalis.magicui.plugin

/**
 * Plugin manager for MagicUI
 * Adapted from VoiceUI-CGPT with minimal changes
 */
class PluginManager {
    
    private val plugins = mutableMapOf<String, MagicUIPlugin>()
    
    /**
     * Register plugin
     */
    fun registerPlugin(plugin: MagicUIPlugin) {
        plugins[plugin.name] = plugin
        plugin.onRegister(this)
    }
    
    /**
     * Unregister plugin
     */
    fun unregisterPlugin(name: String) {
        plugins[name]?.onUnregister()
        plugins.remove(name)
    }
    
    /**
     * Get plugin
     */
    fun getPlugin(name: String): MagicUIPlugin? = plugins[name]
    
    /**
     * List all plugins
     */
    fun listPlugins(): List<MagicUIPlugin> = plugins.values.toList()
}

/**
 * Plugin interface (from CGPT, unchanged)
 */
interface MagicUIPlugin {
    val name: String
    val version: String
    
    fun onRegister(manager: PluginManager)
    fun onUnregister()
    fun registerComponents(registry: ComponentRegistry)
}
```

---

## 6. Testing Framework Adaptation

### 6.1 Test Infrastructure

**Source:** `VoiceUI-CGPT/tests/`

**Files to Port:**
- `ComponentTest.kt` - Component testing utilities
- `IntegrationTest.kt` - Integration test helpers
- `SnapshotTest.kt` - Snapshot testing
- `UITest.kt` - UI testing framework

**Example Adaptation:**

**CGPT Original:**
```kotlin
// From VoiceUI-CGPT
class ComponentTest {
    fun testComponent(component: VoiceUIComponent) {
        // Test logic
    }
}
```

**MagicUI Adapted:**
```kotlin
package com.augmentalis.magicui.testing

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule

/**
 * Component testing utilities
 * Adapted from VoiceUI-CGPT
 */
class MagicComponentTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * Test MagicUI component
     */
    fun testComponent(
        componentName: String,
        render: @Composable () -> Unit,
        assertions: ComposeContentTestRule.() -> Unit
    ) {
        composeTestRule.setContent {
            render()
        }
        
        composeTestRule.assertions()
    }
}
```

---

## 7. Namespace Migration

### 7.1 Find & Replace Namespaces

**Global Replacements:**

| CGPT Namespace | MagicUI Namespace |
|----------------|-------------------|
| `com.voiceui.cgpt` | `com.augmentalis.magicui` |
| `com.voiceui.cgpt.runtime` | `com.augmentalis.magicui.runtime` |
| `com.voiceui.cgpt.preview` | `com.augmentalis.magicui.preview` |
| `com.voiceui.cgpt.theme` | `com.augmentalis.magicui.theme` |
| `com.voiceui.cgpt.debug` | `com.augmentalis.magicui.debug` |
| `com.voiceui.cgpt.plugin` | `com.augmentalis.magicui.plugin` |

**Script for mass replacement:**
```bash
# Replace all namespaces
find VoiceUI-CGPT -name "*.kt" -exec sed -i '' 's/com\.voiceui\.cgpt/com.augmentalis.magicui/g' {} \;

# Replace ObjectBox imports
find VoiceUI-CGPT -name "*.kt" -exec sed -i '' 's/import io\.objectbox\./\/\/ REMOVED: ObjectBox - /g' {} \;
```

---

## 8. Integration Changes

### 8.1 Replace CGPT Services with VOS4

**CGPT Services (remove):**
```kotlin
// CGPT has its own UUID system
class CGPTUUIDManager { }

// CGPT has its own command system
class CGPTCommandProcessor { }

// CGPT has its own localization
class CGPTLocalizer { }
```

**Use VOS4 Instead:**
```kotlin
// Use VOS4 UUIDCreator
import com.augmentalis.uuidcreator.api.IUUIDManager
val uuidManager = IUUIDManager.getInstance(context)

// Use VOS4 CommandManager
import com.augmentalis.commandmanager.CommandManager
val commandManager = CommandManager.getInstance(context)

// Use VOS4 LocalizationManager
import com.augmentalis.localizationmanager.LocalizationManager
val localization = LocalizationManager.getInstance(context)
```

---

## 9. Specific File Adaptations

### 9.1 Runtime Files

**File:** `RuntimeConfig.kt`

**CGPT Version:**
```kotlin
package com.voiceui.cgpt.runtime

data class RuntimeConfig(
    val enableHotReload: Boolean = true,
    val enableDebug: Boolean = false,
    val cacheSize: Int = 100
)
```

**MagicUI Version (no changes needed):**
```kotlin
package com.augmentalis.magicui.runtime

data class RuntimeConfig(
    val enableHotReload: Boolean = true,
    val enableDebug: Boolean = false,
    val cacheSize: Int = 100
)
```

**Changes:** Only namespace

---

### 9.2 Preview System

**File:** `LayoutHotReloadEngine.kt`

**CGPT Version:**
```kotlin
package com.voiceui.cgpt.preview

class LayoutHotReloadEngine {
    fun reload() {
        // Hot reload logic
    }
}
```

**MagicUI Version:**
```kotlin
package com.augmentalis.magicui.preview

import com.augmentalis.magicui.core.MagicUIModule

class LayoutHotReloadEngine(
    private val magicUIModule: MagicUIModule
) {
    fun reload() {
        // Trigger MagicUI recomposition
        magicUIModule.recomposeAll()
    }
}
```

**
