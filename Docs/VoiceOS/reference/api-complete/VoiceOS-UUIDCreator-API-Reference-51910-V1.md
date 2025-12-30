# UUIDCreator - Complete API Reference

**Module Type:** libraries
**Generated:** 2025-10-19 22:03:59 PDT
**Timestamp:** 251019-2203
**Location:** `modules/libraries/UUIDCreator`

---

## Overview

This document provides a complete file-by-file, class-by-class reference for the UUIDCreator module.

## Files


### File: `build/generated/ksp/debug/java/com/augmentalis/uuidcreator/database/dao/UUIDAliasDao_Impl.java`

**Package:** `com.augmentalis.uuidcreator.database.dao`

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

### File: `build/generated/ksp/debug/java/com/augmentalis/uuidcreator/database/dao/UUIDAnalyticsDao_Impl.java`

**Package:** `com.augmentalis.uuidcreator.database.dao`

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
  - ... and 20 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/uuidcreator/database/dao/UUIDElementDao_Impl.java`

**Package:** `com.augmentalis.uuidcreator.database.dao`

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

### File: `build/generated/ksp/debug/java/com/augmentalis/uuidcreator/database/dao/UUIDHierarchyDao_Impl.java`

**Package:** `com.augmentalis.uuidcreator.database.dao`

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

### File: `build/generated/ksp/debug/java/com/augmentalis/uuidcreator/database/UUIDCreatorDatabase_Impl.java`

**Package:** `com.augmentalis.uuidcreator.database`

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
  - ... and 21 more

---

### File: `src/main/java/com/augmentalis/uuidcreator/alias/UuidAliasManager.kt`

**Package:** `com.augmentalis.uuidcreator.alias`

**Classes/Interfaces/Objects:**
  - class UuidAliasManager(
  - data class AliasStats(

**Public Functions:**

**Imports:**
  - import com.augmentalis.uuidcreator.database.UUIDCreatorDatabase
  - import com.augmentalis.uuidcreator.database.entities.UUIDElementEntity
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext

---

### File: `src/main/java/com/augmentalis/uuidcreator/analytics/UuidAnalytics.kt`

**Package:** `com.augmentalis.uuidcreator.analytics`

**Classes/Interfaces/Objects:**
  - class UuidAnalytics(
  - sealed class AnalyticsEvent 
  - data class UsageStats(
  - data class PerformanceStats(
  - data class UsageTrend(
  - data class UsageReport(
  - data class AnalyticsSummary(

**Public Functions:**

**Imports:**
  - import com.augmentalis.uuidcreator.database.repository.UUIDRepository
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.SharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import kotlinx.coroutines.withContext

---

### File: `src/main/java/com/augmentalis/uuidcreator/api/IUUIDManager.kt`

**Package:** `com.augmentalis.uuidcreator.api`

**Classes/Interfaces/Objects:**
  - interface IUUIDManager 

**Public Functions:**

**Imports:**
  - import com.augmentalis.uuidcreator.models.*

---

### File: `src/main/java/com/augmentalis/uuidcreator/compose/ComposeExtensions.kt`

**Package:** `com.augmentalis.uuidcreator.compose`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun Modifier.withUUID(
  - fun Modifier.assignUUID(
  - fun Modifier.uuidButton(
  - fun Modifier.uuidTextInput(
  - fun Modifier.uuidNavigable(
  - fun Modifier.uuidContainer(
  - fun rememberUUID(
  - fun UUIDScope(
  - fun VoiceCommandHandler(

**Imports:**
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.composed
  - import androidx.compose.ui.platform.debugInspectorInfo
  - import com.augmentalis.uuidcreator.UUIDCreator
  - import com.augmentalis.uuidcreator.models.UUIDElement
  - import com.augmentalis.uuidcreator.models.UUIDPosition
  - import com.augmentalis.uuidcreator.models.UUIDMetadata
  - import java.util.UUID

---

### File: `src/main/java/com/augmentalis/uuidcreator/core/UUIDGenerator.kt`

**Package:** `com.augmentalis.uuidcreator.core`

**Classes/Interfaces/Objects:**
  - object UUIDGenerator 

**Public Functions:**

**Imports:**
  - import java.util.UUID
  - import java.util.concurrent.atomic.AtomicLong

---

### File: `src/main/java/com/augmentalis/uuidcreator/core/UUIDRegistry.kt`

**Package:** `com.augmentalis.uuidcreator.core`

**Classes/Interfaces/Objects:**
  - class UUIDRegistry(
  - data class RegistryStats(

**Public Functions:**

**Imports:**
  - import com.augmentalis.uuidcreator.database.repository.UUIDRepository
  - import com.augmentalis.uuidcreator.models.UUIDElement
  - import kotlinx.coroutines.flow.*

---

### File: `src/main/java/com/augmentalis/uuidcreator/database/converters/ModelEntityConverters.kt`

**Package:** `com.augmentalis.uuidcreator.database.converters`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun UUIDElement.toEntity(): UUIDElementEntity 
  - fun UUIDElementEntity.toModel(
  - fun List<UUIDElement>.toEntities(): List<UUIDElementEntity> 
  - fun List<UUIDElementEntity>.toModels(
  - fun createHierarchyEntity(
  - fun List<UUIDHierarchyEntity>.toChildrenMap(): Map<String, MutableList<String>> 
  - fun createAnalyticsEntity(uuid: String): UUIDAnalyticsEntity 
  - fun UUIDAnalyticsEntity.recordAccess(

**Imports:**
  - import com.augmentalis.uuidcreator.database.entities.UUIDAnalyticsEntity
  - import com.augmentalis.uuidcreator.database.entities.UUIDElementEntity
  - import com.augmentalis.uuidcreator.database.entities.UUIDHierarchyEntity
  - import com.augmentalis.uuidcreator.models.UUIDElement
  - import com.augmentalis.uuidcreator.models.UUIDMetadata
  - import com.augmentalis.uuidcreator.models.UUIDPosition
  - import com.google.gson.Gson

---

### File: `src/main/java/com/augmentalis/uuidcreator/database/converters/UUIDCreatorTypeConverters.kt`

**Package:** `com.augmentalis.uuidcreator.database.converters`

**Classes/Interfaces/Objects:**
  - class UUIDCreatorTypeConverters 

**Public Functions:**

**Imports:**
  - import androidx.room.TypeConverter
  - import com.augmentalis.uuidcreator.models.UUIDMetadata
  - import com.augmentalis.uuidcreator.models.UUIDPosition
  - import com.google.gson.Gson
  - import com.google.gson.GsonBuilder

---

### File: `src/main/java/com/augmentalis/uuidcreator/database/dao/UUIDAliasDao.kt`

**Package:** `com.augmentalis.uuidcreator.database.dao`

**Classes/Interfaces/Objects:**
  - interface UUIDAliasDao 

**Public Functions:**

**Imports:**
  - import androidx.room.*
  - import com.augmentalis.uuidcreator.database.entities.UUIDAliasEntity

---

### File: `src/main/java/com/augmentalis/uuidcreator/database/dao/UUIDAnalyticsDao.kt`

**Package:** `com.augmentalis.uuidcreator.database.dao`

**Classes/Interfaces/Objects:**
  - interface UUIDAnalyticsDao 

**Public Functions:**

**Imports:**
  - import androidx.room.*
  - import com.augmentalis.uuidcreator.database.entities.UUIDAnalyticsEntity

---

### File: `src/main/java/com/augmentalis/uuidcreator/database/dao/UUIDElementDao.kt`

**Package:** `com.augmentalis.uuidcreator.database.dao`

**Classes/Interfaces/Objects:**
  - interface UUIDElementDao 

**Public Functions:**

**Imports:**
  - import androidx.room.*
  - import com.augmentalis.uuidcreator.database.entities.UUIDElementEntity

---

### File: `src/main/java/com/augmentalis/uuidcreator/database/dao/UUIDHierarchyDao.kt`

**Package:** `com.augmentalis.uuidcreator.database.dao`

**Classes/Interfaces/Objects:**
  - interface UUIDHierarchyDao 

**Public Functions:**

**Imports:**
  - import androidx.room.*
  - import com.augmentalis.uuidcreator.database.entities.UUIDHierarchyEntity

---

### File: `src/main/java/com/augmentalis/uuidcreator/database/entities/UUIDAliasEntity.kt`

**Package:** `com.augmentalis.uuidcreator.database.entities`

**Classes/Interfaces/Objects:**
  - data class UUIDAliasEntity(

**Public Functions:**

**Imports:**
  - import androidx.room.*

---

### File: `src/main/java/com/augmentalis/uuidcreator/database/entities/UUIDAnalyticsEntity.kt`

**Package:** `com.augmentalis.uuidcreator.database.entities`

**Classes/Interfaces/Objects:**
  - data class UUIDAnalyticsEntity(

**Public Functions:**

**Imports:**
  - import androidx.room.*

---

### File: `src/main/java/com/augmentalis/uuidcreator/database/entities/UUIDElementEntity.kt`

**Package:** `com.augmentalis.uuidcreator.database.entities`

**Classes/Interfaces/Objects:**
  - data class UUIDElementEntity(

**Public Functions:**

**Imports:**
  - import androidx.room.*

---

### File: `src/main/java/com/augmentalis/uuidcreator/database/entities/UUIDHierarchyEntity.kt`

**Package:** `com.augmentalis.uuidcreator.database.entities`

**Classes/Interfaces/Objects:**
  - data class UUIDHierarchyEntity(

**Public Functions:**

**Imports:**
  - import androidx.room.*

---

### File: `src/main/java/com/augmentalis/uuidcreator/database/repository/UUIDRepository.kt`

**Package:** `com.augmentalis.uuidcreator.database.repository`

**Classes/Interfaces/Objects:**
  - class UUIDRepository(

**Public Functions:**

**Imports:**
  - import com.augmentalis.uuidcreator.database.dao.UUIDAnalyticsDao
  - import com.augmentalis.uuidcreator.database.dao.UUIDElementDao
  - import com.augmentalis.uuidcreator.database.dao.UUIDHierarchyDao
  - import com.augmentalis.uuidcreator.database.converters.*
  - import com.augmentalis.uuidcreator.models.UUIDElement
  - import kotlinx.coroutines.CoroutineDispatcher
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext
  - import java.util.concurrent.ConcurrentHashMap

---

### File: `src/main/java/com/augmentalis/uuidcreator/database/UUIDCreatorDatabase.kt`

**Package:** `com.augmentalis.uuidcreator.database`

**Classes/Interfaces/Objects:**
  - abstract class UUIDCreatorDatabase 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.room.Database
  - import androidx.room.Room
  - import androidx.room.RoomDatabase
  - import androidx.room.TypeConverters
  - import com.augmentalis.uuidcreator.database.converters.UUIDCreatorTypeConverters
  - import com.augmentalis.uuidcreator.database.dao.UUIDAliasDao
  - import com.augmentalis.uuidcreator.database.dao.UUIDAnalyticsDao
  - import com.augmentalis.uuidcreator.database.dao.UUIDElementDao
  - import com.augmentalis.uuidcreator.database.dao.UUIDHierarchyDao
  - ... and 4 more

---

### File: `src/main/java/com/augmentalis/uuidcreator/formats/CustomUuidGenerator.kt`

**Package:** `com.augmentalis.uuidcreator.formats`

**Classes/Interfaces/Objects:**
  - object CustomUuidGenerator 

**Public Functions:**

**Imports:**
  - import com.augmentalis.uuidcreator.core.UUIDGenerator
  - import java.util.UUID

---

### File: `src/main/java/com/augmentalis/uuidcreator/hierarchy/HierarchicalUuidManager.kt`

**Package:** `com.augmentalis.uuidcreator.hierarchy`

**Classes/Interfaces/Objects:**
  - class HierarchicalUuidManager(
  - data class UuidTree(
  - data class HierarchyValidationResult(

**Public Functions:**

**Imports:**
  - import com.augmentalis.uuidcreator.database.repository.UUIDRepository
  - import com.augmentalis.uuidcreator.models.UUIDElement
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext

---

### File: `src/main/java/com/augmentalis/uuidcreator/integration/UUIDAccessibilityService.kt`

**Package:** `com.augmentalis.uuidcreator.integration`

**Classes/Interfaces/Objects:**
  - class UUIDAccessibilityService(
  - data class ScanStats(

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.view.accessibility.AccessibilityEvent
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.uuidcreator.UUIDCreator
  - import com.augmentalis.uuidcreator.alias.UuidAliasManager
  - import com.augmentalis.uuidcreator.models.UUIDElement
  - import com.augmentalis.uuidcreator.models.UUIDMetadata
  - import com.augmentalis.uuidcreator.models.UUIDAccessibility
  - import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
  - ... and 4 more

---

### File: `src/main/java/com/augmentalis/uuidcreator/integration/UUIDVoiceCommandProcessor.kt`

**Package:** `com.augmentalis.uuidcreator.integration`

**Classes/Interfaces/Objects:**
  - class UUIDVoiceCommandProcessor(
  - data class CommandStats(
  - data class CommandHistoryEntry(

**Public Functions:**

**Imports:**
  - import com.augmentalis.uuidcreator.UUIDCreator
  - import com.augmentalis.uuidcreator.alias.UuidAliasManager
  - import com.augmentalis.uuidcreator.analytics.UuidAnalytics

---

### File: `src/main/java/com/augmentalis/uuidcreator/integration/VOS4UUIDIntegration.kt`

**Package:** `com.augmentalis.uuidcreator.integration`

**Classes/Interfaces/Objects:**
  - class VOS4UUIDIntegration private constructor(
  - data class IntegrationStats(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.uuidcreator.UUIDCreator
  - import com.augmentalis.uuidcreator.alias.UuidAliasManager
  - import com.augmentalis.uuidcreator.analytics.UuidAnalytics
  - import com.augmentalis.uuidcreator.database.UUIDCreatorDatabase
  - import com.augmentalis.uuidcreator.database.repository.UUIDRepository
  - import com.augmentalis.uuidcreator.hierarchy.HierarchicalUuidManager
  - import com.augmentalis.uuidcreator.models.UUIDElement
  - import com.augmentalis.uuidcreator.monitoring.CollisionMonitor
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/uuidcreator/models/CommandResult.kt`

**Package:** `com.augmentalis.uuidcreator.models`

**Classes/Interfaces/Objects:**
  - data class CommandResult(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/uuidcreator/models/Position.kt`

**Package:** `com.augmentalis.uuidcreator.models`

**Classes/Interfaces/Objects:**
  - data class Position(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/uuidcreator/models/TargetType.kt`

**Package:** `com.augmentalis.uuidcreator.models`

**Classes/Interfaces/Objects:**
  - enum class TargetType 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/uuidcreator/models/UUIDCommandResult.kt`

**Package:** `com.augmentalis.uuidcreator.models`

**Classes/Interfaces/Objects:**
  - data class UUIDCommandResult(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/uuidcreator/models/UUIDElement.kt`

**Package:** `com.augmentalis.uuidcreator.models`

**Classes/Interfaces/Objects:**
  - data class UUIDElement(

**Public Functions:**

**Imports:**
  - import java.util.UUID

---

### File: `src/main/java/com/augmentalis/uuidcreator/models/UUIDHierarchy.kt`

**Package:** `com.augmentalis.uuidcreator.models`

**Classes/Interfaces/Objects:**
  - data class UUIDHierarchy(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/uuidcreator/models/UUIDMetadata.kt`

**Package:** `com.augmentalis.uuidcreator.models`

**Classes/Interfaces/Objects:**
  - data class UUIDMetadata(
  - data class UUIDAccessibility(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/uuidcreator/models/UUIDPosition.kt`

**Package:** `com.augmentalis.uuidcreator.models`

**Classes/Interfaces/Objects:**
  - data class UUIDPosition(
  - data class UUIDBounds(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/uuidcreator/models/VoiceCommand.kt`

**Package:** `com.augmentalis.uuidcreator.models`

**Classes/Interfaces/Objects:**
  - data class VoiceCommand(

**Public Functions:**

**Imports:**
  - import java.util.UUID

---

### File: `src/main/java/com/augmentalis/uuidcreator/models/VoiceTarget.kt`

**Package:** `com.augmentalis.uuidcreator.models`

**Classes/Interfaces/Objects:**
  - data class VoiceTarget(

**Public Functions:**

**Imports:**
  - import java.util.UUID

---

### File: `src/main/java/com/augmentalis/uuidcreator/monitoring/CollisionMonitor.kt`

**Package:** `com.augmentalis.uuidcreator.monitoring`

**Classes/Interfaces/Objects:**
  - class CollisionMonitor(
  - sealed class CollisionResult 
  - sealed class CollisionEvent 
  - enum class ResolutionStrategy 
  - data class CollisionLogEntry(
  - data class CollisionStats(

**Public Functions:**

**Imports:**
  - import com.augmentalis.uuidcreator.database.repository.UUIDRepository
  - import com.augmentalis.uuidcreator.models.UUIDElement
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.SharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import kotlin.time.Duration
  - import kotlin.time.Duration.Companion.minutes

---

### File: `src/main/java/com/augmentalis/uuidcreator/spatial/SpatialNavigator.kt`

**Package:** `com.augmentalis.uuidcreator.spatial`

**Classes/Interfaces/Objects:**
  - class SpatialNavigator(private val registry

**Public Functions:**

**Imports:**
  - import com.augmentalis.uuidcreator.core.UUIDRegistry
  - import com.augmentalis.uuidcreator.models.*
  - import kotlin.math.*

---

### File: `src/main/java/com/augmentalis/uuidcreator/targeting/TargetResolver.kt`

**Package:** `com.augmentalis.uuidcreator.targeting`

**Classes/Interfaces/Objects:**
  - class TargetResolver(private val registry

**Public Functions:**

**Imports:**
  - import com.augmentalis.uuidcreator.core.UUIDRegistry
  - import com.augmentalis.uuidcreator.models.*

---

### File: `src/main/java/com/augmentalis/uuidcreator/thirdparty/AccessibilityFingerprint.kt`

**Package:** `com.augmentalis.uuidcreator.thirdparty`

**Classes/Interfaces/Objects:**
  - data class AccessibilityFingerprint(

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo
  - import java.security.MessageDigest

---

### File: `src/main/java/com/augmentalis/uuidcreator/thirdparty/PackageVersionResolver.kt`

**Package:** `com.augmentalis.uuidcreator.thirdparty`

**Classes/Interfaces/Objects:**
  - class PackageVersionResolver(
  - data class VersionInfo(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.pm.PackageManager
  - import android.os.Build

---

### File: `src/main/java/com/augmentalis/uuidcreator/thirdparty/ThirdPartyUuidCache.kt`

**Package:** `com.augmentalis.uuidcreator.thirdparty`

**Classes/Interfaces/Objects:**
  - class ThirdPartyUuidCache(
  - data class CacheEntry(

**Public Functions:**

**Imports:**
  - import java.util.concurrent.ConcurrentHashMap
  - import java.util.concurrent.atomic.AtomicLong

---

### File: `src/main/java/com/augmentalis/uuidcreator/thirdparty/ThirdPartyUuidGenerator.kt`

**Package:** `com.augmentalis.uuidcreator.thirdparty`

**Classes/Interfaces/Objects:**
  - class ThirdPartyUuidGenerator(
  - data class ThirdPartyUuidComponents(
  - data class CacheStats(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.pm.PackageManager
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.uuidcreator.core.UUIDGenerator
  - import kotlinx.coroutines.sync.Mutex
  - import kotlinx.coroutines.sync.withLock

---

### File: `src/main/java/com/augmentalis/uuidcreator/thirdparty/UuidStabilityTracker.kt`

**Package:** `com.augmentalis.uuidcreator.thirdparty`

**Classes/Interfaces/Objects:**
  - class UuidStabilityTracker(
  - data class UuidMapping(
  - data class StabilityReport(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.uuidcreator.database.UUIDCreatorDatabase
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext

---

### File: `src/main/java/com/augmentalis/uuidcreator/ui/GlassmorphismUtils.kt`

**Package:** `com.augmentalis.uuidcreator.ui`

**Classes/Interfaces/Objects:**
  - data class GlassMorphismConfig(
  - object UUIDColors 
  - object UUIDGlassConfigs 

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

### File: `src/main/java/com/augmentalis/uuidcreator/ui/UUIDManagerActivity.kt`

**Package:** `com.augmentalis.uuidcreator.ui`

**Classes/Interfaces/Objects:**
  - class UUIDManagerActivity 

**Public Functions:**
  - fun UUIDManagerTheme(content: @Composable () -> Unit) 
  - fun UUIDManagerScreen(viewModel: UUIDViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) 
  - fun RegistryStatisticsCard(stats: RegistryStatistics) 
  - fun StatItem(label: String, value: Int, color: Color) 
  - fun TypeChip(type: String, count: Int) 
  - fun QuickActionsCard(
  - fun ActionButton(
  - fun SelectedElementCard(
  - fun NavigationPad(onNavigate: (String) -> Unit) 
  - fun DetailRow(label: String, value: String) 
  - fun CommandResultCard(result: CommandResultInfo) 
  - fun SearchBar(
  - fun ElementCard(
  - fun CommandHistoryCard(item: CommandHistoryItem) 
  - fun RegisterElementDialog(
  - fun VoiceCommandDialog(
  - fun CommandExample(command: String) 
  - fun ExportDialog(

**Imports:**
  - import android.os.Bundle
  - import androidx.activity.ComponentActivity
  - import androidx.activity.compose.setContent
  - import androidx.activity.viewModels
  - import androidx.compose.animation.*
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.*
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.*
  - import androidx.compose.foundation.lazy.grid.*
  - ... and 27 more

---

### File: `src/main/java/com/augmentalis/uuidcreator/ui/UUIDViewModel.kt`

**Package:** `com.augmentalis.uuidcreator.ui`

**Classes/Interfaces/Objects:**
  - data class UUIDUiState(
  - data class UUIDElementInfo(
  - data class CommandHistoryItem(
  - data class RegistryStatistics(
  - data class CommandResultInfo(
  - class UUIDViewModel(

**Public Functions:**

**Imports:**
  - import androidx.lifecycle.LiveData
  - import androidx.lifecycle.MutableLiveData
  - import androidx.lifecycle.ViewModel
  - import androidx.lifecycle.viewModelScope
  - import com.augmentalis.uuidcreator.UUIDCreator
  - import com.augmentalis.uuidcreator.models.*
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.launch
  - import kotlinx.coroutines.flow.*
  - import java.text.SimpleDateFormat
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/uuidcreator/UUIDCreator.kt`

**Package:** `com.augmentalis.uuidcreator`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.uuidcreator.api.IUUIDManager
  - import com.augmentalis.uuidcreator.core.*
  - import com.augmentalis.uuidcreator.database.UUIDCreatorDatabase
  - import com.augmentalis.uuidcreator.database.repository.UUIDRepository
  - import com.augmentalis.uuidcreator.models.*
  - import com.augmentalis.uuidcreator.targeting.TargetResolver
  - import com.augmentalis.uuidcreator.spatial.SpatialNavigator
  - import kotlinx.coroutines.*
  - ... and 4 more

---

## Summary

**Total Files:** 50

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
                                uuidcreator
                                  database
                                    dao
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
