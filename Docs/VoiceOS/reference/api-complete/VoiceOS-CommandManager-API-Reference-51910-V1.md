# CommandManager - Complete API Reference

**Module Type:** managers
**Generated:** 2025-10-19 22:04:02 PDT
**Timestamp:** 251019-2203
**Location:** `modules/managers/CommandManager`

---

## Overview

This document provides a complete file-by-file, class-by-class reference for the CommandManager module.

## Files


### File: `build/generated/ksp/debug/java/com/augmentalis/commandmanager/context/CommandUsageDao_Impl.java`

**Package:** `com.augmentalis.commandmanager.context`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.database.Cursor;
  - import android.os.CancellationSignal;
  - import androidx.annotation.NonNull;
  - import androidx.room.CoroutinesRoom;
  - import androidx.room.EntityInsertionAdapter;
  - import androidx.room.RoomDatabase;
  - import androidx.room.RoomSQLiteQuery;
  - import androidx.room.SharedSQLiteStatement;
  - import androidx.room.util.CursorUtil;
  - import androidx.room.util.DBUtil;
  - ... and 16 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/commandmanager/context/ContextPreferenceDao_Impl.java`

**Package:** `com.augmentalis.commandmanager.context`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.database.Cursor;
  - import android.os.CancellationSignal;
  - import androidx.annotation.NonNull;
  - import androidx.annotation.Nullable;
  - import androidx.room.CoroutinesRoom;
  - import androidx.room.EntityDeletionOrUpdateAdapter;
  - import androidx.room.EntityInsertionAdapter;
  - import androidx.room.RoomDatabase;
  - import androidx.room.RoomSQLiteQuery;
  - import androidx.room.SharedSQLiteStatement;
  - ... and 18 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/commandmanager/context/LearningDatabase_Impl.java`

**Package:** `com.augmentalis.commandmanager.context`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import androidx.annotation.NonNull;
  - import androidx.room.DatabaseConfiguration;
  - import androidx.room.InvalidationTracker;
  - import androidx.room.RoomDatabase;
  - import androidx.room.RoomOpenHelper;
  - import androidx.room.migration.AutoMigrationSpec;
  - import androidx.room.migration.Migration;
  - import androidx.room.util.DBUtil;
  - import androidx.room.util.TableInfo;
  - import androidx.sqlite.db.SupportSQLiteDatabase;
  - ... and 13 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/commandmanager/database/CommandDatabase_Impl.java`

**Package:** `com.augmentalis.commandmanager.database`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import androidx.annotation.NonNull;
  - import androidx.room.DatabaseConfiguration;
  - import androidx.room.InvalidationTracker;
  - import androidx.room.RoomDatabase;
  - import androidx.room.RoomOpenHelper;
  - import androidx.room.migration.AutoMigrationSpec;
  - import androidx.room.migration.Migration;
  - import androidx.room.util.DBUtil;
  - import androidx.room.util.TableInfo;
  - import androidx.sqlite.db.SupportSQLiteDatabase;
  - ... and 13 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/commandmanager/database/CommandUsageDao_Impl.java`

**Package:** `com.augmentalis.commandmanager.database`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.database.Cursor;
  - import android.os.CancellationSignal;
  - import androidx.annotation.NonNull;
  - import androidx.annotation.Nullable;
  - import androidx.room.CoroutinesRoom;
  - import androidx.room.EntityInsertionAdapter;
  - import androidx.room.RoomDatabase;
  - import androidx.room.RoomSQLiteQuery;
  - import androidx.room.SharedSQLiteStatement;
  - import androidx.room.util.CursorUtil;
  - ... and 17 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/commandmanager/database/DatabaseVersionDao_Impl.java`

**Package:** `com.augmentalis.commandmanager.database`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.database.Cursor;
  - import android.os.CancellationSignal;
  - import androidx.annotation.NonNull;
  - import androidx.annotation.Nullable;
  - import androidx.room.CoroutinesRoom;
  - import androidx.room.EntityInsertionAdapter;
  - import androidx.room.RoomDatabase;
  - import androidx.room.RoomSQLiteQuery;
  - import androidx.room.SharedSQLiteStatement;
  - import androidx.room.util.CursorUtil;
  - ... and 14 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/commandmanager/database/VoiceCommandDao_Impl.java`

**Package:** `com.augmentalis.commandmanager.database`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.database.Cursor;
  - import android.os.CancellationSignal;
  - import androidx.annotation.NonNull;
  - import androidx.annotation.Nullable;
  - import androidx.room.CoroutinesRoom;
  - import androidx.room.EntityDeletionOrUpdateAdapter;
  - import androidx.room.EntityInsertionAdapter;
  - import androidx.room.RoomDatabase;
  - import androidx.room.RoomSQLiteQuery;
  - import androidx.room.SharedSQLiteStatement;
  - ... and 19 more

---

### File: `src/androidTest/java/com/augmentalis/commandmanager/ui/CommandManagerUITest.kt`

**Package:** ``

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/commandmanager/actions/AppActions.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - object AppActions 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import android.accessibilityservice.AccessibilityService
  - import android.app.ActivityManager
  - import android.app.usage.UsageStats
  - import android.app.usage.UsageStatsManager
  - import android.content.Context
  - import android.content.Intent
  - import android.content.pm.PackageManager
  - import android.os.Build

---

### File: `src/main/java/com/augmentalis/commandmanager/actions/BaseAction.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - abstract class BaseAction 
  - interface AccessibilityActionPerformer 
  - interface TouchActionPerformer 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.graphics.PointF
  - import android.graphics.Rect
  - import android.view.accessibility.AccessibilityNodeInfo

---

### File: `src/main/java/com/augmentalis/commandmanager/actions/CursorActions.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - object CursorActions 
  - enum class CursorDirection 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.Intent
  - import android.util.Log
  - import com.augmentalis.voiceos.cursor.VoiceCursorAPI
  - import com.augmentalis.voiceos.cursor.core.CursorConfig
  - import com.augmentalis.voiceos.cursor.core.CursorOffset
  - import com.augmentalis.voiceos.cursor.core.CursorType
  - import com.augmentalis.voiceos.cursor.view.CursorAction
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext

---

### File: `src/main/java/com/augmentalis/commandmanager/actions/DictationActions.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - object DictationActions 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.content.Intent
  - import android.os.Build
  - import android.provider.Settings
  - import android.view.accessibility.AccessibilityNodeInfo
  - import android.view.inputmethod.InputMethodManager

---

### File: `src/main/java/com/augmentalis/commandmanager/actions/DragActions.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - object DragActions 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import android.accessibilityservice.AccessibilityService
  - import android.accessibilityservice.GestureDescription
  - import android.content.Context
  - import android.graphics.Path
  - import android.graphics.Point
  - import android.os.Build
  - import android.view.WindowManager
  - import kotlin.math.cos
  - import kotlin.math.sin

---

### File: `src/main/java/com/augmentalis/commandmanager/actions/EditingActions.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - class EditingActions(

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.content.ClipData
  - import android.content.ClipboardManager
  - import android.content.Context
  - import android.os.Build
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.commandmanager.models.*

---

### File: `src/main/java/com/augmentalis/commandmanager/actions/GestureActions.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - object GestureActions 
  - enum class GestureType 
  - enum class ActionType 
  - data class GestureCommand(
  - data class ActionResult(

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import android.content.Context
  - import android.util.Log
  - import kotlinx.coroutines.*

---

### File: `src/main/java/com/augmentalis/commandmanager/actions/MacroActions.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - class MacroActions(
  - interface CommandExecutor 

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import com.augmentalis.commandmanager.models.*
  - import kotlinx.coroutines.delay

---

### File: `src/main/java/com/augmentalis/commandmanager/actions/NavigationActions.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - object NavigationActions 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.content.Intent
  - import android.provider.Settings

---

### File: `src/main/java/com/augmentalis/commandmanager/actions/NotificationActions.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - object NotificationActions 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.service.notification.StatusBarNotification
  - import android.speech.tts.TextToSpeech
  - import android.util.Log
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext

---

### File: `src/main/java/com/augmentalis/commandmanager/actions/OverlayActions.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - object OverlayActions 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.os.Vibrator
  - import android.os.VibrationEffect
  - import android.os.Build
  - import android.provider.Settings
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/commandmanager/actions/ScrollActions.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - object ScrollActions 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import android.accessibilityservice.AccessibilityService
  - import android.accessibilityservice.GestureDescription
  - import android.content.Context
  - import android.graphics.Path
  - import android.graphics.Point
  - import android.os.Build
  - import android.view.WindowManager

---

### File: `src/main/java/com/augmentalis/commandmanager/actions/ShortcutActions.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - object ShortcutActions 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.os.Build
  - import android.util.Log
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext

---

### File: `src/main/java/com/augmentalis/commandmanager/actions/SystemActions.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - object SystemActions 
  - object UUIDSystemActions 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import android.Manifest
  - import android.accessibilityservice.AccessibilityService
  - import android.bluetooth.BluetoothAdapter
  - import android.bluetooth.BluetoothManager
  - import android.content.Context
  - import android.content.Intent
  - import android.net.ConnectivityManager
  - import android.net.NetworkCapabilities
  - import android.net.wifi.WifiManager
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/commandmanager/actions/TextActions.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - object TextActions 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import android.accessibilityservice.AccessibilityService
  - import android.content.ClipData
  - import android.content.ClipboardManager
  - import android.content.Context
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.commandmanager.models.AccessibilityActions.ACTION_SELECT_ALL

---

### File: `src/main/java/com/augmentalis/commandmanager/actions/VolumeActions.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - object VolumeActions 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.media.AudioManager

---

### File: `src/main/java/com/augmentalis/commandmanager/cache/CacheTier.kt`

**Package:** `com.augmentalis.commandmanager.cache`

**Classes/Interfaces/Objects:**
  - enum class CacheTier 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/commandmanager/cache/CommandCache.kt`

**Package:** `com.augmentalis.commandmanager.cache`

**Classes/Interfaces/Objects:**
  - class CommandCache(private val context
  - data class CacheStatistics(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import android.util.LruCache
  - import com.augmentalis.commandmanager.database.CommandDatabase
  - import com.augmentalis.commandmanager.models.Command
  - import com.augmentalis.commandmanager.models.CommandSource
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.GlobalScope
  - import kotlinx.coroutines.launch
  - import kotlinx.coroutines.withContext
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/commandmanager/CommandHandler.kt`

**Package:** `com.augmentalis.commandmanager`

**Classes/Interfaces/Objects:**
  - interface CommandHandler 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/commandmanager/CommandManager.kt`

**Package:** `com.augmentalis.commandmanager`

**Classes/Interfaces/Objects:**
  - class CommandManager(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.commandmanager.models.*
  - import com.augmentalis.commandmanager.actions.*
  - import com.augmentalis.commandmanager.loader.VOSCommandIngestion
  - import com.augmentalis.voiceos.speech.confidence.ConfidenceScorer
  - import com.augmentalis.voiceos.speech.confidence.ConfidenceLevel
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.launch

---

### File: `src/main/java/com/augmentalis/commandmanager/CommandRegistry.kt`

**Package:** `com.augmentalis.commandmanager`

**Classes/Interfaces/Objects:**
  - object CommandRegistry 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import java.util.concurrent.ConcurrentHashMap

---

### File: `src/main/java/com/augmentalis/commandmanager/context/CommandContext.kt`

**Package:** `com.augmentalis.commandmanager.context`

**Classes/Interfaces/Objects:**
  - sealed class CommandContext 
  - enum class LocationType 
  - enum class ActivityType 
  - enum class AppCategory 
  - enum class TimeOfDay 
  - data class TimeRange(
  - object ContextBuilder 

**Public Functions:**

**Imports:**
  - import java.util.Calendar

---

### File: `src/main/java/com/augmentalis/commandmanager/context/CommandContextManager.kt`

**Package:** `com.augmentalis.commandmanager.context`

**Classes/Interfaces/Objects:**
  - class CommandContextManager(
  - data class ContextInfo(
  - data class ContextRecord(
  - enum class CommandResolutionSource 
  - data class ContextAnalyticsReport(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.commandmanager.cache.CommandCache
  - import com.augmentalis.commandmanager.database.VoiceCommandDao
  - import com.augmentalis.commandmanager.dynamic.VoiceCommand
  - import com.augmentalis.commandmanager.learning.HybridLearningService
  - import com.augmentalis.commandmanager.models.Command
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - ... and 4 more

---

### File: `src/main/java/com/augmentalis/commandmanager/context/ContextDetector.kt`

**Package:** `com.augmentalis.commandmanager.context`

**Classes/Interfaces/Objects:**
  - class ContextDetector(

**Public Functions:**

**Imports:**
  - import android.Manifest
  - import android.accessibilityservice.AccessibilityService
  - import android.app.ActivityManager
  - import android.app.usage.UsageStatsManager
  - import android.content.Context
  - import android.content.pm.PackageManager
  - import android.location.LocationManager
  - import android.os.Build
  - import android.view.accessibility.AccessibilityNodeInfo
  - import androidx.core.content.ContextCompat
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/commandmanager/context/ContextManager.kt`

**Package:** `com.augmentalis.commandmanager.context`

**Classes/Interfaces/Objects:**
  - class ContextManager(
  - interface ContextProvider 
  - interface LegacyContextRule 
  - data class ContextSuggestion(
  - enum class SuggestionType 
  - class AppContextProvider 
  - class UIContextProvider 
  - class SystemContextProvider 
  - class TextInputAvailabilityRule 
  - class AppSpecificCommandRule 
  - class UIElementAvailabilityRule 

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.app.ActivityManager
  - import android.app.usage.UsageStatsManager
  - import android.content.Context
  - import android.os.Build
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.commandmanager.models.Command
  - import com.augmentalis.commandmanager.models.CommandDefinition
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.MutableStateFlow
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/commandmanager/context/ContextMatcher.kt`

**Package:** `com.augmentalis.commandmanager.context`

**Classes/Interfaces/Objects:**
  - class ContextMatcher 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.CommandDefinition

---

### File: `src/main/java/com/augmentalis/commandmanager/context/ContextRule.kt`

**Package:** `com.augmentalis.commandmanager.context`

**Classes/Interfaces/Objects:**
  - data class ContextRule(
  - enum class MatchMode 
  - class ContextRuleBuilder 
  - object ExampleContextRules 
  - class ContextRuleRegistry 

**Public Functions:**
  - fun contextRule(id: String, commandId: String, block: ContextRuleBuilder.() -> Unit): ContextRule 

**Imports:**
  - import com.augmentalis.commandmanager.models.CommandDefinition

---

### File: `src/main/java/com/augmentalis/commandmanager/context/ContextSuggester.kt`

**Package:** `com.augmentalis.commandmanager.context`

**Classes/Interfaces/Objects:**
  - class ContextSuggester(
  - data class CommandSuggestion(
  - enum class ConfidenceLevel 
  - data class SuggestionStatistics(
  - data class SuggestionFilter(

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.CommandDefinition
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.asStateFlow
  - import kotlinx.coroutines.withContext

---

### File: `src/main/java/com/augmentalis/commandmanager/context/LearningDatabase.kt`

**Package:** `com.augmentalis.commandmanager.context`

**Classes/Interfaces/Objects:**
  - abstract class LearningDatabase 
  - data class CommandUsageEntity(
  - data class ContextPreferenceEntity(
  - interface CommandUsageDao 
  - interface ContextPreferenceDao 
  - data class CommandUsageAggregate(
  - data class ContextUsageAggregate(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.room.*
  - import kotlinx.coroutines.flow.Flow

---

### File: `src/main/java/com/augmentalis/commandmanager/context/PreferenceLearner.kt`

**Package:** `com.augmentalis.commandmanager.context`

**Classes/Interfaces/Objects:**
  - class PreferenceLearner(
  - data class CommandStats(
  - data class ContextPreference(
  - data class LearningStatistics(

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.CommandDefinition
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext
  - import kotlin.math.ln
  - import kotlin.math.max
  - import kotlin.math.min

---

### File: `src/main/java/com/augmentalis/commandmanager/database/CommandDatabase.kt`

**Package:** `com.augmentalis.commandmanager.database`

**Classes/Interfaces/Objects:**
  - abstract class CommandDatabase 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.room.Database
  - import androidx.room.Room
  - import androidx.room.RoomDatabase

---

### File: `src/main/java/com/augmentalis/commandmanager/database/CommandUsageDao.kt`

**Package:** `com.augmentalis.commandmanager.database`

**Classes/Interfaces/Objects:**
  - interface CommandUsageDao 
  - data class CommandUsageStats(
  - data class CommandSuccessRate(

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Dao
  - import androidx.room.Insert
  - import androidx.room.Query

---

### File: `src/main/java/com/augmentalis/commandmanager/database/CommandUsageEntity.kt`

**Package:** `com.augmentalis.commandmanager.database`

**Classes/Interfaces/Objects:**
  - data class CommandUsageEntity(

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.Index
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/commandmanager/database/DatabaseVersionDao.kt`

**Package:** `com.augmentalis.commandmanager.database`

**Classes/Interfaces/Objects:**
  - interface DatabaseVersionDao 

**Public Functions:**

**Imports:**
  - import androidx.room.Dao
  - import androidx.room.Insert
  - import androidx.room.OnConflictStrategy
  - import androidx.room.Query

---

### File: `src/main/java/com/augmentalis/commandmanager/database/DatabaseVersionEntity.kt`

**Package:** `com.augmentalis.commandmanager.database`

**Classes/Interfaces/Objects:**
  - data class DatabaseVersionEntity(

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/commandmanager/database/VoiceCommandDao.kt`

**Package:** `com.augmentalis.commandmanager.database`

**Classes/Interfaces/Objects:**
  - interface VoiceCommandDao 
  - data class LocaleStats(

**Public Functions:**

**Imports:**
  - import androidx.room.*
  - import kotlinx.coroutines.flow.Flow

---

### File: `src/main/java/com/augmentalis/commandmanager/database/VoiceCommandEntity.kt`

**Package:** `com.augmentalis.commandmanager.database`

**Classes/Interfaces/Objects:**
  - data class VoiceCommandEntity(

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.Index
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/commandmanager/definitions/CommandDefinitions.kt`

**Package:** `com.augmentalis.commandmanager.definitions`

**Classes/Interfaces/Objects:**
  - class CommandDefinitions 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import java.util.concurrent.ConcurrentHashMap

---

### File: `src/main/java/com/augmentalis/commandmanager/dynamic/CommandNamespace.kt`

**Package:** `com.augmentalis.commandmanager.dynamic`

**Classes/Interfaces/Objects:**
  - class CommandNamespace(
  - data class NamespaceStatistics(
  - data class NamespaceConfig(
  - sealed class NamespaceValidationResult 
  - object NamespaceHelper 

**Public Functions:**

**Imports:**
  - import java.util.concurrent.ConcurrentHashMap
  - import java.util.concurrent.locks.ReentrantReadWriteLock
  - import kotlin.concurrent.read
  - import kotlin.concurrent.write

---

### File: `src/main/java/com/augmentalis/commandmanager/dynamic/CommandPersistence.kt`

**Package:** `com.augmentalis.commandmanager.dynamic`

**Classes/Interfaces/Objects:**
  - data class CommandEntity(
  - class CommandStorage(
  - object CommandExporter 
  - class PersistentCommandRegistry(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/commandmanager/dynamic/CommandPriority.kt`

**Package:** `com.augmentalis.commandmanager.dynamic`

**Classes/Interfaces/Objects:**
  - object CommandPriority 
  - data class PrioritySuggestion(
  - data class PriorityDistribution(

**Public Functions:**

**Imports:**
  - import kotlin.math.abs

---

### File: `src/main/java/com/augmentalis/commandmanager/dynamic/ConflictDetector.kt`

**Package:** `com.augmentalis.commandmanager.dynamic`

**Classes/Interfaces/Objects:**
  - class ConflictDetector(

**Public Functions:**

**Imports:**
  - import kotlin.math.abs

---

### File: `src/main/java/com/augmentalis/commandmanager/dynamic/ConflictInfo.kt`

**Package:** `com.augmentalis.commandmanager.dynamic`

**Classes/Interfaces/Objects:**
  - data class ConflictInfo(
  - enum class ConflictType 
  - enum class ConflictSeverity 
  - data class ResolutionSuggestion(
  - enum class ResolutionStrategy 
  - sealed class ConflictDetectionResult 
  - data class ConflictResolutionAction(
  - sealed class ConflictResolutionResult 
  - data class ConflictDetectionConfig(
  - data class ConflictStatistics(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/commandmanager/dynamic/DynamicCommandRegistry.kt`

**Package:** `com.augmentalis.commandmanager.dynamic`

**Classes/Interfaces/Objects:**
  - class DynamicCommandRegistry(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import java.util.concurrent.ConcurrentHashMap
  - import java.util.concurrent.locks.ReentrantReadWriteLock
  - import kotlin.concurrent.read
  - import kotlin.concurrent.write

---

### File: `src/main/java/com/augmentalis/commandmanager/dynamic/NamespaceManager.kt`

**Package:** `com.augmentalis.commandmanager.dynamic`

**Classes/Interfaces/Objects:**
  - class NamespaceManager(
  - data class NamespaceHealthReport(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import java.util.concurrent.ConcurrentHashMap
  - import java.util.concurrent.locks.ReentrantReadWriteLock
  - import kotlin.concurrent.read
  - import kotlin.concurrent.write

---

### File: `src/main/java/com/augmentalis/commandmanager/dynamic/RegistrationListener.kt`

**Package:** `com.augmentalis.commandmanager.dynamic`

**Classes/Interfaces/Objects:**
  - interface RegistrationListener 
  - abstract class RegistrationListenerAdapter 
  - class LoggingRegistrationListener(
  - class CompositeRegistrationListener(
  - data class RegistryStatistics(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/commandmanager/dynamic/VoiceCommand.kt`

**Package:** `com.augmentalis.commandmanager.dynamic`

**Classes/Interfaces/Objects:**
  - data class VoiceCommand(
  - enum class PriorityLevel 
  - enum class CommandCategory 
  - data class CommandExecutionContext(
  - sealed class CommandResult 
  - enum class ErrorCode 
  - data class VoiceCommandData(

**Public Functions:**

**Imports:**
  - import android.os.Parcelable
  - import kotlinx.parcelize.Parcelize

---

### File: `src/main/java/com/augmentalis/commandmanager/handlers/CursorCommandHandler.kt`

**Package:** `com.augmentalis.commandmanager.handlers`

**Classes/Interfaces/Objects:**
  - class CursorCommandHandler private constructor(
  - interface CommandRouter 
  - data class IntegrationStatus(
  - enum class CursorDirection 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.commandmanager.CommandHandler
  - import com.augmentalis.commandmanager.CommandRegistry
  - import com.augmentalis.commandmanager.actions.CursorActions
  - import com.augmentalis.commandmanager.actions.CursorDirection
  - import com.augmentalis.voiceos.cursor.core.CursorType
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/commandmanager/history/CommandHistory.kt`

**Package:** `com.augmentalis.commandmanager.history`

**Classes/Interfaces/Objects:**
  - class CommandHistory 
  - data class CommandHistoryStatistics(
  - data class CommandFrequency(
  - data class CommandLastUsed(
  - data class CommandUsageTrend(
  - data class HistoryConfiguration(
  - data class CommandHistoryExport(

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import java.util.concurrent.ConcurrentLinkedQueue
  - import java.util.concurrent.atomic.AtomicLong

---

### File: `src/main/java/com/augmentalis/commandmanager/learning/CommandLearningDao.kt`

**Package:** `com.augmentalis.commandmanager.learning`

**Classes/Interfaces/Objects:**
  - interface CommandLearningDao 

**Public Functions:**

**Imports:**
  - import androidx.room.*

---

### File: `src/main/java/com/augmentalis/commandmanager/learning/CommandLearningEntity.kt`

**Package:** `com.augmentalis.commandmanager.learning`

**Classes/Interfaces/Objects:**
  - data class CommandLearningEntity(

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.Index
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/commandmanager/learning/HybridLearningService.kt`

**Package:** `com.augmentalis.commandmanager.learning`

**Classes/Interfaces/Objects:**
  - class HybridLearningService(
  - data class PatternAnalysis(
  - data class ErrorPrediction(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.launch
  - import kotlin.math.min

---

### File: `src/main/java/com/augmentalis/commandmanager/loader/ArrayJsonParser.kt`

**Package:** `com.augmentalis.commandmanager.loader`

**Classes/Interfaces/Objects:**
  - class ArrayJsonParser 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.database.VoiceCommandEntity
  - import org.json.JSONArray
  - import org.json.JSONObject
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/commandmanager/loader/CommandFileWatcher.kt`

**Package:** `com.augmentalis.commandmanager.loader`

**Classes/Interfaces/Objects:**
  - class CommandFileWatcher(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.os.FileObserver
  - import android.os.FileObserver.MODIFY
  - import android.os.FileObserver.CLOSE_WRITE
  - import android.os.FileObserver.CREATE
  - import android.os.FileObserver.DELETE
  - import android.os.FileObserver.MOVED_FROM
  - import android.os.FileObserver.MOVED_TO
  - import android.util.Log
  - import kotlinx.coroutines.*
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/commandmanager/loader/CommandLoader.kt`

**Package:** `com.augmentalis.commandmanager.loader`

**Classes/Interfaces/Objects:**
  - class CommandLoader(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.commandmanager.database.CommandDatabase
  - import com.augmentalis.commandmanager.database.DatabaseVersionDao
  - import com.augmentalis.commandmanager.database.DatabaseVersionEntity
  - import com.augmentalis.commandmanager.database.VoiceCommandDao
  - import java.io.FileNotFoundException
  - import java.util.Locale

---

### File: `src/main/java/com/augmentalis/commandmanager/loader/CommandLocalizer.kt`

**Package:** `com.augmentalis.commandmanager.loader`

**Classes/Interfaces/Objects:**
  - class CommandLocalizer(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.commandmanager.database.CommandDatabase
  - import com.augmentalis.commandmanager.database.VoiceCommandEntity
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.asStateFlow
  - import kotlinx.coroutines.withContext
  - import java.util.Locale

---

### File: `src/main/java/com/augmentalis/commandmanager/loader/CommandResolver.kt`

**Package:** `com.augmentalis.commandmanager.loader`

**Classes/Interfaces/Objects:**
  - class CommandResolver(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.commandmanager.database.CommandUsageDao
  - import com.augmentalis.commandmanager.database.CommandUsageEntity
  - import com.augmentalis.commandmanager.database.VoiceCommandDao
  - import com.augmentalis.commandmanager.database.VoiceCommandEntity

---

### File: `src/main/java/com/augmentalis/commandmanager/loader/UnifiedJSONParser.kt`

**Package:** `com.augmentalis.commandmanager.loader`

**Classes/Interfaces/Objects:**
  - data class UnifiedCommandFile(
  - data class UnifiedFileInfo(
  - data class CommandSegment(
  - data class Metadata(
  - data class LoadResult(
  - class UnifiedJSONParser(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.commandmanager.models.VOSCommand
  - import com.augmentalis.commandmanager.database.VoiceCommandEntity
  - import org.json.JSONArray
  - import org.json.JSONException
  - import org.json.JSONObject
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext
  - import java.io.IOException

---

### File: `src/main/java/com/augmentalis/commandmanager/loader/VOSCommandIngestion.kt`

**Package:** `com.augmentalis.commandmanager.loader`

**Classes/Interfaces/Objects:**
  - data class IngestionResult(
  - data class IngestionProgress(
  - class VOSCommandIngestion(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.commandmanager.database.CommandDatabase
  - import com.augmentalis.commandmanager.database.VoiceCommandEntity
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext

---

### File: `src/main/java/com/augmentalis/commandmanager/loader/VOSFileParser.kt`

**Package:** `com.augmentalis.commandmanager.loader`

**Classes/Interfaces/Objects:**
  - data class VOSFile(
  - data class FileInfo(
  - class VOSFileParser(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.commandmanager.models.VOSCommand
  - import com.augmentalis.commandmanager.database.VoiceCommandEntity
  - import org.json.JSONArray
  - import org.json.JSONException
  - import org.json.JSONObject
  - import java.io.IOException

---

### File: `src/main/java/com/augmentalis/commandmanager/macros/CommandMacro.kt`

**Package:** `com.augmentalis.commandmanager.macros`

**Classes/Interfaces/Objects:**
  - data class CommandMacro(
  - data class MacroValidation(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/commandmanager/macros/MacroContext.kt`

**Package:** `com.augmentalis.commandmanager.macros`

**Classes/Interfaces/Objects:**
  - class MacroContext(
  - sealed class ExecutionState 

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.view.accessibility.AccessibilityNodeInfo
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow

---

### File: `src/main/java/com/augmentalis/commandmanager/macros/MacroDSL.kt`

**Package:** `com.augmentalis.commandmanager.macros`

**Classes/Interfaces/Objects:**
  - class MacroBuilder(private val name
  - class CommandBuilder 
  - class ConditionalBuilder 
  - class LoopBuilder(private val count
  - class LoopWhileBuilder 
  - class WaitForBuilder 
  - class StepListBuilder 

**Public Functions:**
  - fun macro(name: String, builder: MacroBuilder.() -> Unit): CommandMacro 
  - fun MacroContext.screenContains(text: String): Boolean 
  - fun MacroContext.isVisible(text: String): Boolean 

**Imports:**
  - import java.util.UUID

---

### File: `src/main/java/com/augmentalis/commandmanager/macros/MacroExecutor.kt`

**Package:** `com.augmentalis.commandmanager.macros`

**Classes/Interfaces/Objects:**
  - class MacroExecutor(
  - data class MacroExecutionResult(
  - data class CommandResult(
  - class MacroExecutionException(message

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.util.Log
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.withTimeout

---

### File: `src/main/java/com/augmentalis/commandmanager/macros/MacroStep.kt`

**Package:** `com.augmentalis.commandmanager.macros`

**Classes/Interfaces/Objects:**
  - sealed class MacroStep 
  - data class VoiceCommand(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/commandmanager/models/ActionResult.kt`

**Package:** `com.augmentalis.commandmanager.models`

**Classes/Interfaces/Objects:**
  - data class ActionResult(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/commandmanager/models/CommandModels.kt`

**Package:** `com.augmentalis.commandmanager.models`

**Classes/Interfaces/Objects:**
  - data class Command(
  - enum class CommandSource 
  - data class CommandContext(
  - data class CommandResult(
  - data class CommandError(
  - enum class ErrorCode 
  - data class CommandDefinition(
  - data class CommandParameter(
  - enum class ParameterType 
  - data class CommandHistoryEntry(
  - data class CommandEvent(
  - enum class EventType 
  - data class CommandInfo(
  - data class CommandStats(
  - enum class CommandCategory 
  - object AccessibilityActions 

**Public Functions:**

**Imports:**
  - import android.content.Context

---

### File: `src/main/java/com/augmentalis/commandmanager/models/VOSCommand.kt`

**Package:** `com.augmentalis.commandmanager.models`

**Classes/Interfaces/Objects:**
  - data class VOSCommand(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/commandmanager/monitor/ServiceCallback.kt`

**Package:** `com.augmentalis.commandmanager.monitor`

**Classes/Interfaces/Objects:**
  - interface ServiceCallback 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/commandmanager/plugins/ActionComposer.kt`

**Package:** `com.augmentalis.commandmanager.plugins`

**Classes/Interfaces/Objects:**
  - class ActionComposer 
  - abstract class CompositeAction 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.commandmanager.dynamic.VoiceCommand
  - import com.augmentalis.commandmanager.dynamic.CommandResult
  - import com.augmentalis.commandmanager.dynamic.ErrorCode
  - import kotlinx.coroutines.*

---

### File: `src/main/java/com/augmentalis/commandmanager/plugins/ActionDiscoveryAPI.kt`

**Package:** `com.augmentalis.commandmanager.plugins`

**Classes/Interfaces/Objects:**
  - interface ActionDiscoveryAPI 
  - data class ActionMetadata(
  - data class PluginInfo(
  - class ActionDiscoveryAPIImpl(

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.dynamic.CommandCategory

---

### File: `src/main/java/com/augmentalis/commandmanager/plugins/ActionPlugin.kt`

**Package:** `com.augmentalis.commandmanager.plugins`

**Classes/Interfaces/Objects:**
  - interface ActionPlugin 
  - enum class PluginPermission 
  - data class PluginPermissions(
  - class PluginInitializationException(
  - class PluginExecutionException(
  - class PluginLoadException(
  - data class PluginMetadata(
  - enum class PluginState 
  - interface PluginLifecycleListener 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.commandmanager.dynamic.VoiceCommand
  - import com.augmentalis.commandmanager.dynamic.CommandResult

---

### File: `src/main/java/com/augmentalis/commandmanager/plugins/ActionTelemetry.kt`

**Package:** `com.augmentalis.commandmanager.plugins`

**Classes/Interfaces/Objects:**
  - class ActionTelemetry(
  - data class ActionStats(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import android.util.Log
  - import com.augmentalis.commandmanager.dynamic.CommandResult
  - import com.augmentalis.commandmanager.dynamic.ErrorCode
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.launch
  - import org.json.JSONArray
  - import org.json.JSONObject
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/commandmanager/plugins/PluginHotReload.kt`

**Package:** `com.augmentalis.commandmanager.plugins`

**Classes/Interfaces/Objects:**
  - class PluginHotReload(
  - interface HotReloadListener 

**Public Functions:**

**Imports:**
  - import android.os.FileObserver
  - import android.util.Log
  - import kotlinx.coroutines.*
  - import java.io.File

---

### File: `src/main/java/com/augmentalis/commandmanager/plugins/PluginManager.kt`

**Package:** `com.augmentalis.commandmanager.plugins`

**Classes/Interfaces/Objects:**
  - class PluginManager(
  - data class PluginStats(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.pm.PackageInfo
  - import android.content.pm.PackageManager
  - import android.content.pm.Signature
  - import android.util.Log
  - import dalvik.system.DexClassLoader
  - import dalvik.system.PathClassLoader
  - import kotlinx.coroutines.*
  - import java.io.File
  - import java.io.IOException
  - ... and 11 more

---

### File: `src/main/java/com/augmentalis/commandmanager/plugins/PluginVersioning.kt`

**Package:** `com.augmentalis.commandmanager.plugins`

**Classes/Interfaces/Objects:**
  - class PluginVersioning 
  - data class SemanticVersion(
  - sealed class CompatibilityResult 
  - data class MigrationPlan(
  - sealed class MigrationStep 
  - interface MigratablePlugin 

**Public Functions:**

**Imports:**
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/commandmanager/processor/CommandProcessor.kt`

**Package:** `com.augmentalis.commandmanager.processor`

**Classes/Interfaces/Objects:**
  - class CommandProcessor(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.commandmanager.actions.*
  - import com.augmentalis.commandmanager.models.*
  - import com.augmentalis.commandmanager.definitions.CommandDefinitions
  - import java.util.concurrent.ConcurrentHashMap
  - import kotlin.text.Regex

---

### File: `src/main/java/com/augmentalis/commandmanager/processor/NodeFinder.kt`

**Package:** `com.augmentalis.commandmanager.processor`

**Classes/Interfaces/Objects:**
  - object NodeFinder 

**Public Functions:**
  - fun AccessibilityNodeInfo.calculateHash(): String 
  - fun AccessibilityNodeInfo.calculateHashWithBounds(): String 
  - fun AccessibilityNodeInfo.toDebugString(): String 
  - fun AccessibilityNodeInfo.verifyHash(expectedHash: String): Boolean 

**Imports:**
  - import android.graphics.Rect
  - import android.util.Log
  - import android.view.accessibility.AccessibilityNodeInfo
  - import java.security.MessageDigest

---

### File: `src/main/java/com/augmentalis/commandmanager/registry/CommandRegistry.kt`

**Package:** `com.augmentalis.commandmanager.registry`

**Classes/Interfaces/Objects:**
  - class CommandRegistry 
  - data class RegisteredCommand(
  - data class CommandMetadata(
  - sealed class CommandRegistryEvent 
  - sealed class CommandRegistrationResult 
  - sealed class CommandUnregistrationResult 
  - data class CommandRegistryStatistics(
  - data class CommandRegistryExport(
  - data class CommandImportResult(
  - interface CommandValidationRule 
  - data class CommandValidationResult(

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import com.augmentalis.commandmanager.definitions.*
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import java.util.concurrent.ConcurrentHashMap

---

### File: `src/main/java/com/augmentalis/commandmanager/registry/DynamicCommandRegistry.kt`

**Package:** `com.augmentalis.commandmanager.registry`

**Classes/Interfaces/Objects:**
  - class DynamicCommandRegistry 
  - data class VoiceCommand(
  - enum class ActionType 
  - data class ConflictInfo(
  - enum class ConflictType 
  - sealed class DynamicRegistryEvent 
  - data class DynamicRegistryStatistics(

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import java.util.concurrent.ConcurrentHashMap
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/commandmanager/routing/IntentDispatcher.kt`

**Package:** `com.augmentalis.commandmanager.routing`

**Classes/Interfaces/Objects:**
  - class IntentDispatcher(
  - data class UserFeedback(
  - data class RoutingAnalytics(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.commandmanager.models.Command
  - import com.augmentalis.commandmanager.models.CommandResult
  - import com.augmentalis.commandmanager.models.CommandError
  - import com.augmentalis.commandmanager.models.ErrorCode
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.launch

---

### File: `src/main/java/com/augmentalis/commandmanager/routing/RoutingContext.kt`

**Package:** `com.augmentalis.commandmanager.routing`

**Classes/Interfaces/Objects:**
  - data class RoutingContext(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/commandmanager/ui/CommandManagerActivity.kt`

**Package:** `com.augmentalis.commandmanager.ui`

**Classes/Interfaces/Objects:**
  - class CommandManagerActivity 

**Public Functions:**
  - fun CommandManagerTheme(content: @Composable () -> Unit) 
  - fun CommandManagerScreen(

**Imports:**
  - import android.content.Context
  - import android.content.Intent
  - import android.os.Bundle
  - import androidx.activity.ComponentActivity
  - import androidx.activity.compose.setContent
  - import androidx.activity.viewModels
  - import androidx.compose.animation.*
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.clickable
  - ... and 35 more

---

### File: `src/main/java/com/augmentalis/commandmanager/ui/CommandManagerSettingsFragment.kt`

**Package:** `com.augmentalis.commandmanager.ui`

**Classes/Interfaces/Objects:**
  - class CommandManagerSettingsFragment 

**Public Functions:**

**Imports:**
  - import android.os.Bundle
  - import android.view.LayoutInflater
  - import android.view.View
  - import android.view.ViewGroup
  - import android.widget.Toast
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.rememberScrollState
  - import androidx.compose.foundation.verticalScroll
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - ... and 11 more

---

### File: `src/main/java/com/augmentalis/commandmanager/ui/CommandViewModel.kt`

**Package:** `com.augmentalis.commandmanager.ui`

**Classes/Interfaces/Objects:**
  - class CommandViewModel(private val context
  - class CommandViewModelFactory(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import androidx.lifecycle.LiveData
  - import androidx.lifecycle.MutableLiveData
  - import androidx.lifecycle.ViewModel
  - import androidx.lifecycle.ViewModelProvider
  - import androidx.lifecycle.viewModelScope
  - import com.augmentalis.commandmanager.models.*
  - import com.augmentalis.commandmanager.processor.CommandProcessor
  - import com.augmentalis.commandmanager.history.CommandHistory
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/commandmanager/ui/editor/CommandCreationWizard.kt`

**Package:** `com.augmentalis.commandmanager.ui.editor`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun CommandCreationWizard(

**Imports:**
  - import androidx.compose.animation.AnimatedContent
  - import androidx.compose.animation.ExperimentalAnimationApi
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.rememberScrollState
  - import androidx.compose.foundation.text.KeyboardOptions
  - import androidx.compose.foundation.verticalScroll
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - ... and 6 more

---

### File: `src/main/java/com/augmentalis/commandmanager/ui/editor/CommandEditorScreen.kt`

**Package:** `com.augmentalis.commandmanager.ui.editor`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun CommandEditorScreen(

**Imports:**
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.LazyColumn
  - import androidx.compose.foundation.lazy.items
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.platform.LocalContext
  - ... and 4 more

---

### File: `src/main/java/com/augmentalis/commandmanager/ui/editor/CommandEditorViewModel.kt`

**Package:** `com.augmentalis.commandmanager.ui.editor`

**Classes/Interfaces/Objects:**
  - class CommandEditorViewModel 
  - data class CommandEditorUiState(
  - data class WizardState(
  - enum class WizardStep 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.net.Uri
  - import android.util.Log
  - import androidx.lifecycle.ViewModel
  - import androidx.lifecycle.viewModelScope
  - import com.augmentalis.commandmanager.registry.ActionType
  - import com.augmentalis.commandmanager.registry.ConflictInfo
  - import com.augmentalis.commandmanager.registry.DynamicCommandRegistry
  - import com.augmentalis.commandmanager.registry.VoiceCommand
  - import kotlinx.coroutines.flow.MutableStateFlow
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/commandmanager/ui/editor/CommandImportExport.kt`

**Package:** `com.augmentalis.commandmanager.ui.editor`

**Classes/Interfaces/Objects:**
  - class CommandImportExport 
  - sealed class ImportResult 
  - sealed class ExportFileResult 
  - data class ValidationResult(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.net.Uri
  - import android.util.Log
  - import com.augmentalis.commandmanager.registry.ActionType
  - import com.augmentalis.commandmanager.registry.VoiceCommand
  - import org.json.JSONArray
  - import org.json.JSONObject
  - import java.io.File
  - import java.io.FileOutputStream
  - import java.io.IOException
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/commandmanager/ui/editor/CommandLibraryBrowser.kt`

**Package:** `com.augmentalis.commandmanager.ui.editor`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun CommandLibraryBrowser(

**Imports:**
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.LazyColumn
  - import androidx.compose.foundation.lazy.items
  - import androidx.compose.foundation.lazy.grid.GridCells
  - import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
  - import androidx.compose.foundation.lazy.grid.items
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - ... and 6 more

---

### File: `src/main/java/com/augmentalis/commandmanager/ui/editor/CommandTemplate.kt`

**Package:** `com.augmentalis.commandmanager.ui.editor`

**Classes/Interfaces/Objects:**
  - data class CommandTemplate(
  - enum class TemplateCategory(val displayName
  - class TemplateBuilder 
  - data class TemplateCollection(
  - data class TemplateFilter(
  - data class TemplateCustomization(

**Public Functions:**
  - fun commandTemplate(init: TemplateBuilder.() -> Unit): CommandTemplate 

**Imports:**
  - import android.os.Parcelable
  - import com.augmentalis.commandmanager.registry.ActionType
  - import kotlinx.parcelize.Parcelize

---

### File: `src/main/java/com/augmentalis/commandmanager/ui/editor/CommandTestingPanel.kt`

**Package:** `com.augmentalis.commandmanager.ui.editor`

**Classes/Interfaces/Objects:**
  - data class TestHistoryEntry(

**Public Functions:**
  - fun CommandTestingPanel(

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.LazyColumn
  - import androidx.compose.foundation.lazy.items
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - ... and 7 more

---

### File: `src/main/java/com/augmentalis/commandmanager/ui/editor/TemplateRepository.kt`

**Package:** `com.augmentalis.commandmanager.ui.editor`

**Classes/Interfaces/Objects:**
  - object TemplateRepository 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.registry.ActionType

---

### File: `src/main/java/com/augmentalis/commandmanager/ui/GlassmorphismUtils.kt`

**Package:** `com.augmentalis.commandmanager.ui`

**Classes/Interfaces/Objects:**
  - data class GlassMorphismConfig(
  - object CommandColors 
  - object CommandGlassConfigs 

**Public Functions:**
  - fun Modifier.glassMorphism(

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.border
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.draw.blur
  - import androidx.compose.ui.graphics.Brush
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.unit.Dp
  - import androidx.compose.ui.unit.dp

---

### File: `src/main/java/com/augmentalis/commandmanager/ui/TestableComposables.kt`

**Package:** `com.augmentalis.commandmanager.ui`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun CommandManagerContent(
  - fun ErrorDisplay(
  - fun SuccessDisplay(

**Imports:**
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.commandmanager.models.*

---

### File: `src/main/java/com/augmentalis/commandmanager/validation/CommandValidator.kt`

**Package:** `com.augmentalis.commandmanager.validation`

**Classes/Interfaces/Objects:**
  - class CommandValidator 
  - interface ValidationRule 
  - interface ErrorHandler 
  - data class ValidationResult(
  - data class ValidationViolation(
  - enum class ViolationSeverity 
  - enum class ValidationCategory 
  - data class ErrorHandlingResult(
  - enum class ErrorAction 
  - data class RecoveryAction(
  - enum class RecoveryType 
  - sealed class ValidationEvent 
  - data class ValidationStatistics(
  - class CommandIdFormatRule 
  - class PatternsNotEmptyRule 
  - class ParameterDefinitionRule 
  - class CategoryConsistencyRule 
  - class ContextRequirementRule 
  - class ContextAvailabilityRule 
  - class PermissionRequirementRule 
  - class CommandInjectionRule 
  - class PrivacyProtectionRule 
  - class UnknownCommandHandler 
  - class InvalidParametersHandler 
  - class MissingContextHandler 
  - class ExecutionFailedHandler 
  - class PermissionDeniedHandler 
  - class TimeoutHandler 
  - class CancelledHandler 
  - class NetworkErrorHandler 
  - class ModuleNotAvailableHandler 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.*
  - import com.augmentalis.commandmanager.definitions.*
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow

---

### File: `src/test/java/com/augmentalis/commandmanager/actions/CursorActionsTest.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - class CursorActionsTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceos.cursor.VoiceCursorAPI
  - import com.augmentalis.voiceos.cursor.core.CursorConfig
  - import com.augmentalis.voiceos.cursor.core.CursorOffset
  - import com.augmentalis.voiceos.cursor.core.CursorType
  - import io.mockk.*
  - import kotlinx.coroutines.test.runTest
  - import org.junit.*
  - import org.junit.Assert.*

---

### File: `src/test/java/com/augmentalis/commandmanager/actions/EditingActionsTest.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - class EditingActionsTest 

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.content.ClipData
  - import android.content.ClipboardManager
  - import android.content.Context
  - import android.os.Build
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.commandmanager.models.*
  - import io.mockk.*
  - import kotlinx.coroutines.test.runTest
  - import org.junit.*
  - ... and 1 more

---

### File: `src/test/java/com/augmentalis/commandmanager/actions/MacroActionsTest.kt`

**Package:** `com.augmentalis.commandmanager.actions`

**Classes/Interfaces/Objects:**
  - class MacroActionsTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.commandmanager.actions.CommandExecutor
  - import com.augmentalis.commandmanager.models.*
  - import io.mockk.*
  - import kotlinx.coroutines.test.runTest
  - import org.junit.*
  - import org.junit.Assert.*

---

### File: `src/test/java/com/augmentalis/commandmanager/cache/CommandCacheTest.kt`

**Package:** `com.augmentalis.commandmanager.cache`

**Classes/Interfaces/Objects:**
  - class CommandCacheTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.commandmanager.models.Command
  - import com.augmentalis.commandmanager.models.CommandSource
  - import io.mockk.*
  - import kotlinx.coroutines.test.runTest
  - import org.junit.*
  - import org.junit.Assert.*
  - import kotlin.system.measureTimeMillis

---

### File: `src/test/java/com/augmentalis/commandmanager/CommandRegistryTest.kt`

**Package:** `com.augmentalis.commandmanager`

**Classes/Interfaces/Objects:**
  - class CommandRegistryTest 

**Public Functions:**

**Imports:**
  - import kotlinx.coroutines.runBlocking
  - import org.junit.After
  - import org.junit.Assert.*
  - import org.junit.Before
  - import org.junit.Test

---

### File: `src/test/java/com/augmentalis/commandmanager/context/CommandContextManagerTest.kt`

**Package:** `com.augmentalis.commandmanager.context`

**Classes/Interfaces/Objects:**
  - class CommandContextManagerTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.commandmanager.cache.CommandCache
  - import com.augmentalis.commandmanager.database.VoiceCommandDao
  - import com.augmentalis.commandmanager.learning.HybridLearningService
  - import com.augmentalis.commandmanager.models.Command
  - import com.augmentalis.commandmanager.models.CommandSource
  - import io.mockk.*
  - import kotlinx.coroutines.test.runTest
  - import org.junit.*
  - import org.junit.Assert.*

---

### File: `src/test/java/com/augmentalis/commandmanager/context/ContextAwareCommandsTest.kt`

**Package:** `com.augmentalis.commandmanager.context`

**Classes/Interfaces/Objects:**
  - class ContextAwareCommandsTest 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.CommandDefinition
  - import com.augmentalis.commandmanager.models.ParameterType
  - import kotlinx.coroutines.runBlocking
  - import org.junit.Before
  - import org.junit.Test
  - import org.junit.Assert.*
  - import org.mockito.Mock
  - import org.mockito.Mockito.*
  - import org.mockito.MockitoAnnotations
  - import java.util.Calendar

---

### File: `src/test/java/com/augmentalis/commandmanager/dynamic/DynamicCommandRegistryTest.kt`

**Package:** `com.augmentalis.commandmanager.dynamic`

**Classes/Interfaces/Objects:**
  - class DynamicCommandRegistryTest 

**Public Functions:**

**Imports:**
  - import kotlinx.coroutines.test.runTest
  - import org.junit.After
  - import org.junit.Assert.*
  - import org.junit.Before
  - import org.junit.Test

---

### File: `src/test/java/com/augmentalis/commandmanager/integration/CommandManagerIntegrationTest.kt`

**Package:** `com.augmentalis.commandmanager.integration`

**Classes/Interfaces/Objects:**
  - class CommandManagerIntegrationTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.commandmanager.CommandManager
  - import com.augmentalis.commandmanager.models.*
  - import com.augmentalis.voiceos.speech.confidence.ConfidenceLevel
  - import io.mockk.*
  - import kotlinx.coroutines.test.runTest
  - import org.junit.*
  - import org.junit.Assert.*
  - import kotlin.system.measureTimeMillis

---

### File: `src/test/java/com/augmentalis/commandmanager/learning/HybridLearningServiceTest.kt`

**Package:** `com.augmentalis.commandmanager.learning`

**Classes/Interfaces/Objects:**
  - class HybridLearningServiceTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import io.mockk.*
  - import kotlinx.coroutines.test.runTest
  - import org.junit.*
  - import org.junit.Assert.*

---

### File: `src/test/java/com/augmentalis/commandmanager/plugins/PluginManagerTest.kt`

**Package:** `com.augmentalis.commandmanager.plugins`

**Classes/Interfaces/Objects:**
  - class PluginManagerTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.commandmanager.dynamic.VoiceCommand
  - import com.augmentalis.commandmanager.dynamic.CommandResult
  - import com.augmentalis.commandmanager.dynamic.ErrorCode
  - import io.mockk.*
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.test.runTest
  - import org.junit.*
  - ... and 2 more

---

### File: `src/test/java/com/augmentalis/commandmanager/routing/IntentDispatcherTest.kt`

**Package:** `com.augmentalis.commandmanager.routing`

**Classes/Interfaces/Objects:**
  - class IntentDispatcherTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.commandmanager.models.*
  - import io.mockk.*
  - import kotlinx.coroutines.test.runTest
  - import org.junit.*
  - import org.junit.Assert.*

---

### File: `src/test/java/com/augmentalis/commandmanager/ui/CommandViewModelTest.kt`

**Package:** `com.augmentalis.commandmanager.ui`

**Classes/Interfaces/Objects:**
  - class CommandViewModelTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.arch.core.executor.testing.InstantTaskExecutorRule
  - import androidx.lifecycle.Observer
  - import com.augmentalis.commandmanager.models.*
  - import kotlinx.coroutines.ExperimentalCoroutinesApi
  - import kotlinx.coroutines.test.StandardTestDispatcher
  - import kotlinx.coroutines.test.TestCoroutineScheduler
  - import kotlinx.coroutines.test.runTest
  - import kotlinx.coroutines.test.setMain
  - import kotlinx.coroutines.Dispatchers
  - ... and 11 more

---

## Summary

**Total Files:** 117

**Module Structure:**
```
                  build
                    generated
                      ap_generated_sources
                        debug
                          out
                      ksp
                        debug
                          java
                            com
                              augmentalis
                                commandmanager
                                  context
                                  database
                          kotlin
                      res
                        pngs
                          debug
                        resValues
                          debug
                    intermediates
                      aapt_friendly_merged_manifests
                        debug
                          processDebugManifest
                            aapt
                      aar_metadata
                        debug
                          writeDebugAarMetadata
                      annotation_processor_list
                        debug
                          javaPreCompileDebug
                      compile_library_classes_jar
                        debug
                          bundleLibCompileToJarDebug
                      compile_r_class_jar
                        debug
                          generateDebugRFile
                      compile_symbol_list
                        debug
                          generateDebugRFile
                      compiled_local_resources
                        debug
                          compileDebugLibraryResources
                            out
                      data_binding_layout_info_type_package
                        debug
                          packageDebugResources
                            out
                      incremental
                        debug
                          packageDebugResources
```

---

**Generated by:** VOS4 API Documentation Generator
**Timestamp:** 251019-2203
