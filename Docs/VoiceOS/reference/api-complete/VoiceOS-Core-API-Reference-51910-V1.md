# VoiceOSCore - Complete API Reference

**Module Type:** apps
**Generated:** 2025-10-19 22:03:41 PDT
**Timestamp:** 251019-2203
**Location:** `modules/apps/VoiceOSCore`

---

## Overview

This document provides a complete file-by-file, class-by-class reference for the VoiceOSCore module.

## Files


### File: `build/generated/aidl_source_output_dir/debug/out/com/augmentalis/voicerecognition/IRecognitionCallback.java`

**Package:** `com.augmentalis.voicerecognition`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/generated/aidl_source_output_dir/debug/out/com/augmentalis/voicerecognition/IVoiceRecognitionService.java`

**Package:** `com.augmentalis.voicerecognition`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/accessibility/di/AccessibilityModule_ProvideInstalledAppsManagerFactory.java`

**Package:** `com.augmentalis.voiceoscore.accessibility.di`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import com.augmentalis.voiceoscore.accessibility.managers.InstalledAppsManager;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.Preconditions;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - import javax.inject.Provider;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/accessibility/di/AccessibilityModule_ProvideSpeechEngineManagerFactory.java`

**Package:** `com.augmentalis.voiceoscore.accessibility.di`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import com.augmentalis.voiceoscore.accessibility.speech.SpeechEngineManager;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.Preconditions;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - import javax.inject.Provider;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/accessibility/Hilt_VoiceOSService.java`

**Package:** `com.augmentalis.voiceoscore.accessibility`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService;
  - import androidx.annotation.CallSuper;
  - import dagger.hilt.android.internal.managers.ServiceComponentManager;
  - import dagger.hilt.internal.GeneratedComponentManagerHolder;
  - import dagger.hilt.internal.UnsafeCasts;
  - import java.lang.Object;
  - import java.lang.Override;
  - import javax.annotation.processing.Generated;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService_GeneratedInjector.java`

**Package:** `com.augmentalis.voiceoscore.accessibility`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import dagger.hilt.InstallIn;
  - import dagger.hilt.android.components.ServiceComponent;
  - import dagger.hilt.codegen.OriginatingElement;
  - import dagger.hilt.internal.GeneratedEntryPoint;
  - import javax.annotation.processing.Generated;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService_MembersInjector.java`

**Package:** `com.augmentalis.voiceoscore.accessibility`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import com.augmentalis.voiceoscore.accessibility.managers.InstalledAppsManager;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ICommandOrchestrator;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IDatabaseManager;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IEventRouter;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ISpeechManager;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService;
  - import dagger.MembersInjector;
  - import dagger.internal.DaggerGenerated;
  - ... and 4 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/learnweb/GeneratedWebCommandDao_Impl.java`

**Package:** `com.augmentalis.voiceoscore.learnweb`

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

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/learnweb/ScrapedWebElementDao_Impl.java`

**Package:** `com.augmentalis.voiceoscore.learnweb`

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

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/learnweb/ScrapedWebsiteDao_Impl.java`

**Package:** `com.augmentalis.voiceoscore.learnweb`

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
  - ... and 17 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/learnweb/WebScrapingDatabase_Impl.java`

**Package:** `com.augmentalis.voiceoscore.learnweb`

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

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/refactoring/di/VoiceOSServiceDirector_ProvideCommandOrchestratorFactory.java`

**Package:** `com.augmentalis.voiceoscore.refactoring.di`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ICommandOrchestrator;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ISpeechManager;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.Preconditions;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - ... and 1 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/refactoring/di/VoiceOSServiceDirector_ProvideDatabaseManagerFactory.java`

**Package:** `com.augmentalis.voiceoscore.refactoring.di`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IDatabaseManager;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.Preconditions;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - import javax.inject.Provider;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/refactoring/di/VoiceOSServiceDirector_ProvideEventRouterFactory.java`

**Package:** `com.augmentalis.voiceoscore.refactoring.di`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IEventRouter;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.Preconditions;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - ... and 1 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/refactoring/di/VoiceOSServiceDirector_ProvideServiceMonitorFactory.java`

**Package:** `com.augmentalis.voiceoscore.refactoring.di`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.Preconditions;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - import javax.inject.Provider;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/refactoring/di/VoiceOSServiceDirector_ProvideSpeechManagerFactory.java`

**Package:** `com.augmentalis.voiceoscore.refactoring.di`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import com.augmentalis.voiceos.speech.engines.vivoka.VivokaEngine;
  - import com.augmentalis.voiceos.speech.engines.vosk.VoskEngine;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ISpeechManager;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.Preconditions;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - ... and 1 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/refactoring/di/VoiceOSServiceDirector_ProvideStateManagerFactory.java`

**Package:** `com.augmentalis.voiceoscore.refactoring.di`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.Preconditions;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - import javax.inject.Provider;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/refactoring/di/VoiceOSServiceDirector_ProvideUIScrapingServiceFactory.java`

**Package:** `com.augmentalis.voiceoscore.refactoring.di`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IDatabaseManager;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.Preconditions;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - import javax.inject.Provider;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/refactoring/di/VoiceOSServiceDirector_ProvideVivokaEngineFactory.java`

**Package:** `com.augmentalis.voiceoscore.refactoring.di`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import com.augmentalis.voiceos.speech.engines.vivoka.VivokaEngine;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.Preconditions;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - import javax.inject.Provider;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/refactoring/di/VoiceOSServiceDirector_ProvideVoskEngineFactory.java`

**Package:** `com.augmentalis.voiceoscore.refactoring.di`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import com.augmentalis.voiceos.speech.engines.vosk.VoskEngine;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.Preconditions;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - import javax.inject.Provider;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/refactoring/impl/CommandOrchestratorImpl_Factory.java`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ISpeechManager;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - import javax.inject.Provider;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/refactoring/impl/EventRouterImpl_Factory.java`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - import javax.inject.Provider;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/refactoring/impl/ServiceMonitorImpl_Factory.java`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - import javax.inject.Provider;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/refactoring/impl/SpeechManagerImpl_Factory.java`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import com.augmentalis.voiceos.speech.engines.vivoka.VivokaEngine;
  - import com.augmentalis.voiceos.speech.engines.vosk.VoskEngine;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - import javax.inject.Provider;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/refactoring/impl/StateManagerImpl_Factory.java`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/refactoring/impl/UIScrapingServiceImpl_Factory.java`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context;
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IDatabaseManager;
  - import dagger.internal.DaggerGenerated;
  - import dagger.internal.Factory;
  - import dagger.internal.QualifierMetadata;
  - import dagger.internal.ScopeMetadata;
  - import javax.annotation.processing.Generated;
  - import javax.inject.Provider;

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/scraping/dao/ElementRelationshipDao_Impl.java`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

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

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/scraping/dao/ElementStateHistoryDao_Impl.java`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

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
  - ... and 19 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/scraping/dao/GeneratedCommandDao_Impl.java`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

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

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/scraping/dao/ScrapedAppDao_Impl.java`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

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

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/scraping/dao/ScrapedElementDao_Impl.java`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

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
  - import androidx.room.RoomDatabaseKt;
  - import androidx.room.RoomSQLiteQuery;
  - ... and 21 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/scraping/dao/ScrapedHierarchyDao_Impl.java`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

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
  - ... and 19 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/scraping/dao/ScreenContextDao_Impl.java`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

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

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/scraping/dao/ScreenTransitionDao_Impl.java`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

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
  - ... and 18 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/scraping/dao/UserInteractionDao_Impl.java`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

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
  - ... and 18 more

---

### File: `build/generated/ksp/debug/java/com/augmentalis/voiceoscore/scraping/database/AppScrapingDatabase_Impl.java`

**Package:** `com.augmentalis.voiceoscore.scraping.database`

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
  - ... and 31 more

---

### File: `build/generated/ksp/debug/java/hilt_aggregated_deps/_com_augmentalis_voiceoscore_accessibility_di_AccessibilityModule.java`

**Package:** `hilt_aggregated_deps`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import dagger.hilt.processor.internal.aggregateddeps.AggregatedDeps;
  - import javax.annotation.processing.Generated;

---

### File: `build/generated/ksp/debug/java/hilt_aggregated_deps/_com_augmentalis_voiceoscore_accessibility_VoiceOSService_GeneratedInjector.java`

**Package:** `hilt_aggregated_deps`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import dagger.hilt.processor.internal.aggregateddeps.AggregatedDeps;
  - import javax.annotation.processing.Generated;

---

### File: `build/generated/ksp/debug/java/hilt_aggregated_deps/_com_augmentalis_voiceoscore_refactoring_di_VoiceOSServiceDirector.java`

**Package:** `hilt_aggregated_deps`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import dagger.hilt.processor.internal.aggregateddeps.AggregatedDeps;
  - import javax.annotation.processing.Generated;

---

### File: `build/generated/source/buildConfig/debug/com/augmentalis/voiceoscore/BuildConfig.java`

**Package:** `com.augmentalis.voiceoscore`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `src/androidTest/java/com/augmentalis/voiceoscore/baseline/VoiceOSServiceAccessibilityEventTest.kt`

**Package:** `com.augmentalis.voiceoscore.baseline`

**Classes/Interfaces/Objects:**
  - class VoiceOSServiceAccessibilityEventTest 

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityServiceInfo
  - import android.content.Context
  - import android.view.accessibility.AccessibilityEvent
  - import android.view.accessibility.AccessibilityNodeInfo
  - import androidx.test.core.app.ApplicationProvider
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.test.runTest
  - import org.junit.After
  - ... and 7 more

---

### File: `src/androidTest/java/com/augmentalis/voiceoscore/baseline/VoiceOSServiceCommandExecutionTest.kt`

**Package:** `com.augmentalis.voiceoscore.baseline`

**Classes/Interfaces/Objects:**
  - class VoiceOSServiceCommandExecutionTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.test.core.app.ApplicationProvider
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.test.runTest
  - import org.junit.After
  - import org.junit.Assert.*
  - import org.junit.Before
  - import org.junit.Test
  - import org.junit.runner.RunWith
  - ... and 1 more

---

### File: `src/androidTest/java/com/augmentalis/voiceoscore/baseline/VoiceOSServicePerformanceBenchmark.kt`

**Package:** `com.augmentalis.voiceoscore.baseline`

**Classes/Interfaces/Objects:**
  - class VoiceOSServicePerformanceBenchmark 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.test.core.app.ApplicationProvider
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.test.runTest
  - import org.junit.After
  - import org.junit.Assert.*
  - import org.junit.Before
  - import org.junit.Test
  - import org.junit.runner.RunWith

---

### File: `src/androidTest/java/com/augmentalis/voiceoscore/baseline/VoiceOSServiceSpeechRecognitionTest.kt`

**Package:** `com.augmentalis.voiceoscore.baseline`

**Classes/Interfaces/Objects:**
  - class VoiceOSServiceSpeechRecognitionTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.test.core.app.ApplicationProvider
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import com.augmentalis.speechrecognition.SpeechEngine
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.test.runTest
  - import org.junit.After
  - import org.junit.Assert.*
  - import org.junit.Before
  - import org.junit.Test
  - ... and 3 more

---

### File: `src/androidTest/java/com/augmentalis/voiceoscore/chaos/ChaosEngineeringTest.kt`

**Package:** `com.augmentalis.voiceoscore.chaos`

**Classes/Interfaces/Objects:**
  - class ChaosEngineeringTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.test.core.app.ApplicationProvider
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import com.augmentalis.voiceoscore.mocks.MockActionCoordinator
  - import com.augmentalis.voiceoscore.mocks.MockVoiceAccessibilityService
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.test.*
  - import org.junit.Before
  - import org.junit.Test
  - import org.junit.runner.RunWith
  - ... and 2 more

---

### File: `src/androidTest/java/com/augmentalis/voiceoscore/integration/AIDLIntegrationTest.kt`

**Package:** `com.augmentalis.voiceoscore.integration`

**Classes/Interfaces/Objects:**
  - class AIDLIntegrationTest 

**Public Functions:**

**Imports:**
  - import android.content.ComponentName
  - import android.content.Context
  - import android.content.Intent
  - import android.content.ServiceConnection
  - import android.os.IBinder
  - import android.os.RemoteException
  - import androidx.test.core.app.ApplicationProvider
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import androidx.test.rule.ServiceTestRule
  - import com.augmentalis.voicerecognition.IVoiceRecognitionService
  - ... and 13 more

---

### File: `src/androidTest/java/com/augmentalis/voiceoscore/integration/VoiceCommandIntegrationTest.kt`

**Package:** `com.augmentalis.voiceoscore.integration`

**Classes/Interfaces/Objects:**
  - class VoiceCommandIntegrationTest 

**Public Functions:**

**Imports:**
  - import android.content.ComponentName
  - import android.content.Context
  - import android.content.Intent
  - import android.content.ServiceConnection
  - import android.os.IBinder
  - import androidx.test.core.app.ApplicationProvider
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import com.augmentalis.voiceoscore.mocks.MockActionCoordinator
  - import com.augmentalis.voiceoscore.mocks.MockVoiceAccessibilityService
  - import com.augmentalis.voicerecognition.IRecognitionCallback
  - ... and 18 more

---

### File: `src/androidTest/java/com/augmentalis/voiceoscore/integration/VoiceCommandPersistenceTest.kt`

**Package:** `com.augmentalis.voiceoscore.integration`

**Classes/Interfaces/Objects:**
  - class VoiceCommandPersistenceTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.room.Room
  - import androidx.test.core.app.ApplicationProvider
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import com.augmentalis.voiceoscore.scraping.AppHashCalculator
  - import com.augmentalis.voiceoscore.scraping.CommandResult
  - import com.augmentalis.voiceoscore.scraping.VoiceCommandProcessor
  - import com.augmentalis.voiceoscore.scraping.database.AppScrapingDatabase
  - import com.augmentalis.voiceoscore.scraping.entities.GeneratedCommandEntity
  - import com.augmentalis.voiceoscore.scraping.entities.ScrapedAppEntity
  - ... and 8 more

---

### File: `src/androidTest/java/com/augmentalis/voiceoscore/mocks/MockActionCoordinator.kt`

**Package:** `com.augmentalis.voiceoscore.mocks`

**Classes/Interfaces/Objects:**
  - class MockActionCoordinator(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.voiceoscore.accessibility.handlers.ActionCategory
  - import java.util.concurrent.ConcurrentHashMap
  - import java.util.concurrent.ConcurrentLinkedQueue
  - import java.util.concurrent.atomic.AtomicInteger

---

### File: `src/androidTest/java/com/augmentalis/voiceoscore/mocks/MockVoiceAccessibilityService.kt`

**Package:** `com.augmentalis.voiceoscore.mocks`

**Classes/Interfaces/Objects:**
  - class MockVoiceAccessibilityService 

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.accessibilityservice.GestureDescription
  - import android.content.Context
  - import android.content.Intent
  - import android.graphics.Path
  - import android.graphics.Rect
  - import android.os.Bundle
  - import android.util.Log
  - import android.view.accessibility.AccessibilityEvent
  - import android.view.accessibility.AccessibilityNodeInfo
  - ... and 4 more

---

### File: `src/androidTest/java/com/augmentalis/voiceoscore/scraping/database/Migration1To2Test.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.database`

**Classes/Interfaces/Objects:**
  - class Migration1To2Test 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.room.Room
  - import androidx.room.testing.MigrationTestHelper
  - import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
  - import androidx.test.core.app.ApplicationProvider
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import androidx.test.platform.app.InstrumentationRegistry
  - import com.augmentalis.voiceoscore.scraping.database.MIGRATION_1_2
  - import org.junit.After
  - import org.junit.Assert.*
  - ... and 5 more

---

### File: `src/androidTest/java/com/augmentalis/voiceoscore/scraping/LearnAppMergeTest.kt`

**Package:** `com.augmentalis.voiceoscore.scraping`

**Classes/Interfaces/Objects:**
  - class LearnAppMergeTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.room.Room
  - import androidx.test.core.app.ApplicationProvider
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import com.augmentalis.voiceoscore.scraping.database.AppScrapingDatabase
  - import com.augmentalis.voiceoscore.scraping.entities.ScrapedAppEntity
  - import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity
  - import kotlinx.coroutines.runBlocking
  - import org.junit.After
  - import org.junit.Assert.assertEquals
  - ... and 7 more

---

### File: `src/androidTest/java/com/augmentalis/voiceoscore/testing/ComparisonFrameworkIntegrationTest.kt`

**Package:** `com.augmentalis.voiceoscore.testing`

**Classes/Interfaces/Objects:**
  - class ComparisonFrameworkIntegrationTest 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.runBlocking
  - import org.junit.After
  - import org.junit.Assert.*
  - import org.junit.Before
  - import org.junit.Test

---

### File: `src/androidTest/java/com/augmentalis/voiceoscore/ui/FloatingEngineSelectorTest.kt`

**Package:** `com.augmentalis.voiceoscore.ui`

**Classes/Interfaces/Objects:**
  - class FloatingEngineSelectorTest 
  - class FloatingEngineSelectorIntegrationTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.compose.ui.test.*
  - import androidx.compose.ui.test.junit4.createComposeRule
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import androidx.test.platform.app.InstrumentationRegistry
  - import com.augmentalis.voiceoscore.ui.components.FloatingEngineSelector
  - import org.junit.Assert.*
  - import org.junit.Before
  - import org.junit.Rule
  - import org.junit.Test
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/client/VoiceRecognitionClient.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.client`

**Classes/Interfaces/Objects:**
  - class VoiceRecognitionClient(private val context

**Public Functions:**

**Imports:**
  - import android.content.ComponentName
  - import android.content.Context
  - import android.content.Intent
  - import android.content.ServiceConnection
  - import android.os.IBinder
  - import android.os.RemoteException
  - import android.util.Log
  - import com.augmentalis.voicerecognition.IRecognitionCallback
  - import com.augmentalis.voicerecognition.IVoiceRecognitionService
  - import kotlinx.coroutines.CoroutineScope
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/config/ServiceConfiguration.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.config`

**Classes/Interfaces/Objects:**
  - data class ServiceConfiguration(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/cursor/BoundaryDetector.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.cursor`

**Classes/Interfaces/Objects:**
  - enum class ScreenEdge 
  - data class BoundaryCheckResult(
  - data class SafeAreaInsets(
  - data class BoundaryConfig(
  - class BoundaryDetector(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.PointF
  - import android.graphics.Rect
  - import android.os.Build
  - import android.util.DisplayMetrics
  - import android.util.Log
  - import android.view.Display
  - import android.view.WindowInsets
  - import android.view.WindowManager
  - import androidx.core.view.WindowInsetsCompat

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/cursor/CommandMapper.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.cursor`

**Classes/Interfaces/Objects:**
  - sealed class CursorAction 
  - data class CommandPattern(
  - data class CommandResult(
  - class CommandMapper 
  - data class CommandMapperStatistics(

**Public Functions:**

**Imports:**
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/cursor/CursorGestureHandler.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.cursor`

**Classes/Interfaces/Objects:**
  - enum class GestureType 
  - data class GestureResult(
  - data class GestureConfig(
  - class CursorGestureHandler(

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.accessibilityservice.GestureDescription
  - import android.graphics.Path
  - import android.graphics.PointF
  - import android.util.Log
  - import android.view.ViewConfiguration
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.delay
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/cursor/CursorHistoryTracker.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.cursor`

**Classes/Interfaces/Objects:**
  - data class HistoricalCursorPosition(
  - class CursorHistoryTracker(
  - data class HistoryStatistics(

**Public Functions:**

**Imports:**
  - import android.graphics.PointF
  - import android.util.Log
  - import java.util.ArrayDeque
  - import kotlin.math.pow
  - import kotlin.math.sqrt

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/cursor/CursorPositionTracker.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.cursor`

**Classes/Interfaces/Objects:**
  - data class CursorPosition(
  - data class ScreenBounds(
  - class CursorPositionTracker(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.res.Configuration
  - import android.graphics.PointF
  - import android.util.DisplayMetrics
  - import android.util.Log
  - import android.view.Display
  - import android.view.WindowManager
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/cursor/CursorStyleManager.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.cursor`

**Classes/Interfaces/Objects:**
  - sealed class CursorStyle 
  - enum class AnimationType 
  - enum class CursorShape 
  - data class StyleConfig(
  - class CursorStyleManager(

**Public Functions:**

**Imports:**
  - import android.graphics.Color
  - import android.util.Log
  - import androidx.compose.ui.graphics.Color as ComposeColor
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/cursor/CursorVisibilityManager.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.cursor`

**Classes/Interfaces/Objects:**
  - enum class VisibilityState 
  - enum class InteractionMode 
  - data class VisibilityConfig(
  - class CursorVisibilityManager(

**Public Functions:**

**Imports:**
  - import android.animation.ValueAnimator
  - import android.util.Log
  - import android.view.animation.DecelerateInterpolator
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.Job
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/cursor/FocusIndicator.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.cursor`

**Classes/Interfaces/Objects:**
  - enum class FocusState(
  - enum class AnimationStyle 
  - data class FocusIndicatorConfig(
  - class FocusIndicator(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.*
  - import android.util.Log
  - import android.view.View
  - import android.view.WindowManager
  - import android.view.animation.AccelerateDecelerateInterpolator
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.Canvas
  - import androidx.compose.foundation.layout.fillMaxSize
  - import androidx.compose.runtime.*
  - ... and 7 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/cursor/SnapToElementHandler.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.cursor`

**Classes/Interfaces/Objects:**
  - data class SnapTarget(
  - class SnapToElementHandler(

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.graphics.Point
  - import android.graphics.Rect
  - import android.util.Log
  - import android.view.accessibility.AccessibilityNodeInfo
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.Job
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.launch
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/cursor/SpeedController.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.cursor`

**Classes/Interfaces/Objects:**
  - enum class CursorSpeed(
  - enum class EasingFunction 
  - class SpeedController 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import kotlin.math.PI
  - import kotlin.math.pow
  - import kotlin.math.sin

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/cursor/VoiceCursorEventHandler.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.cursor`

**Classes/Interfaces/Objects:**
  - sealed class CursorEvent 
  - enum class Direction 
  - data class EventResult(
  - data class EventConfig(
  - class VoiceCursorEventHandler(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.voiceoscore.accessibility.utils.Debouncer
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.Job
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.SharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import kotlinx.coroutines.launch
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/di/AccessibilityModule.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.di`

**Classes/Interfaces/Objects:**
  - object AccessibilityModule 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine
  - import com.augmentalis.voiceoscore.accessibility.managers.ActionCoordinator
  - import com.augmentalis.voiceoscore.accessibility.managers.InstalledAppsManager
  - import com.augmentalis.voiceoscore.accessibility.speech.SpeechEngineManager
  - import dagger.Module
  - import dagger.Provides
  - import dagger.hilt.InstallIn
  - import dagger.hilt.android.components.ServiceComponent
  - import dagger.hilt.android.qualifiers.ApplicationContext
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/extractors/UIScrapingEngine.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.extractors`

**Classes/Interfaces/Objects:**
  - class UIScrapingEngine(

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.graphics.Rect
  - import android.util.Log
  - import android.util.LruCache
  - import android.view.accessibility.AccessibilityEvent
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine.Companion.FORBIDDEN_DESCRIPTIONS
  - import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine.Companion.NEXT_LINE_REGEX
  - import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine.Companion.NUMERIC_PATTERN
  - ... and 8 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/ActionHandler.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - interface ActionHandler 
  - enum class ActionCategory 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/AppHandler.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - class AppHandler(

**Public Functions:**

**Imports:**
  - import android.content.Intent
  - import android.content.pm.PackageManager
  - import android.util.Log
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/BluetoothHandler.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - class BluetoothHandler(

**Public Functions:**

**Imports:**
  - import android.Manifest
  - import android.bluetooth.BluetoothAdapter
  - import android.bluetooth.BluetoothManager
  - import android.content.Context
  - import android.content.Intent
  - import android.content.pm.PackageManager
  - import android.os.Build
  - import android.provider.Settings
  - import android.util.Log
  - import androidx.core.content.ContextCompat
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/DeviceHandler.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - class DeviceHandler(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.Intent
  - import android.media.AudioManager
  - import android.provider.Settings
  - import android.util.Log
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/DragHandler.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - class DragHandler(

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService.GestureResultCallback
  - import android.accessibilityservice.GestureDescription
  - import android.graphics.Path
  - import android.util.Log
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.Job
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.cancel
  - ... and 9 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/GestureHandler.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - class GestureHandler(

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService.GestureResultCallback
  - import android.accessibilityservice.GestureDescription
  - import android.content.res.Resources
  - import android.graphics.Path
  - import android.graphics.Point
  - import android.util.DisplayMetrics
  - import android.util.Log
  - import android.view.ViewConfiguration
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService
  - import com.augmentalis.voiceoscore.accessibility.ui.utils.DisplayUtils
  - ... and 6 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/GesturePathFactory.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - interface GesturePathFactory 
  - class RealGesturePathFactory 

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.GestureDescription
  - import android.graphics.Path

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/HelpMenuHandler.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - class HelpMenuHandler(

**Public Functions:**

**Imports:**
  - import android.content.Intent
  - import android.net.Uri
  - import android.util.Log
  - import android.widget.Toast
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.cancel
  - import kotlinx.coroutines.delay
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/InputHandler.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - class InputHandler(

**Public Functions:**

**Imports:**
  - import android.os.Bundle
  - import android.util.Log
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/NavigationHandler.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - class NavigationHandler(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/NumberHandler.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - class NumberHandler(

**Public Functions:**

**Imports:**
  - import android.graphics.Rect
  - import android.util.Log
  - import android.view.accessibility.AccessibilityNodeInfo
  - import android.widget.Toast
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.cancel
  - import kotlinx.coroutines.delay
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/SelectHandler.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - class SelectHandler(

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.graphics.Rect
  - import android.util.Log
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.cancel

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/SystemHandler.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - class SystemHandler(

**Public Functions:**

**Imports:**
  - import android.content.Intent
  - import android.os.Build
  - import android.provider.Settings
  - import android.util.Log
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService
  - import android.accessibilityservice.AccessibilityService as AndroidAccessibilityService

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/handlers/UIHandler.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - class UIHandler(

**Public Functions:**

**Imports:**
  - import android.graphics.Rect
  - import android.util.Log
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService
  - import android.accessibilityservice.AccessibilityService as AndroidAccessibilityService

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/IVoiceOSService.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility`

**Classes/Interfaces/Objects:**
  - interface IVoiceOSService 

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.view.accessibility.AccessibilityEvent
  - import com.augmentalis.voiceos.cursor.core.CursorOffset

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/managers/ActionCoordinator.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.managers`

**Classes/Interfaces/Objects:**
  - class ActionCoordinator(private val service

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService
  - import com.augmentalis.voiceoscore.accessibility.handlers.ActionCategory
  - import com.augmentalis.voiceoscore.accessibility.handlers.ActionHandler
  - import com.augmentalis.voiceoscore.accessibility.handlers.AppHandler
  - import com.augmentalis.voiceoscore.accessibility.handlers.BluetoothHandler
  - import com.augmentalis.voiceoscore.accessibility.handlers.DeviceHandler
  - import com.augmentalis.voiceoscore.accessibility.handlers.DragHandler
  - import com.augmentalis.voiceoscore.accessibility.handlers.GestureHandler
  - import com.augmentalis.voiceoscore.accessibility.handlers.HelpMenuHandler
  - ... and 15 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/managers/InstalledAppsManager.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.managers`

**Classes/Interfaces/Objects:**
  - class InstalledAppsManager(private val context
  - data class AppsName(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.Intent
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow
  - import kotlinx.coroutines.launch

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/monitor/ConnectionState.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.monitor`

**Classes/Interfaces/Objects:**
  - enum class ConnectionState 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/monitor/ServiceCallback.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.monitor`

**Classes/Interfaces/Objects:**
  - interface ServiceCallback 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/monitor/ServiceMonitor.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.monitor`

**Classes/Interfaces/Objects:**
  - class ServiceMonitor(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import android.util.Log
  - import com.augmentalis.commandmanager.CommandManager
  - import com.augmentalis.commandmanager.monitor.ServiceCallback
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.cancel
  - ... and 6 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/overlays/CommandStatusOverlay.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.overlays`

**Classes/Interfaces/Objects:**
  - enum class CommandState 
  - class CommandStatusOverlay(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.PixelFormat
  - import android.view.Gravity
  - import android.view.WindowManager
  - import androidx.compose.animation.*
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.CircleShape
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - ... and 23 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/overlays/ConfidenceOverlay.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.overlays`

**Classes/Interfaces/Objects:**
  - class ConfidenceOverlay(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.PixelFormat
  - import android.view.Gravity
  - import android.view.WindowManager
  - import androidx.compose.animation.animateColorAsState
  - import androidx.compose.animation.core.animateFloatAsState
  - import androidx.compose.animation.core.tween
  - import androidx.compose.foundation.Canvas
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.layout.*
  - ... and 15 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/overlays/ContextMenuOverlay.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.overlays`

**Classes/Interfaces/Objects:**
  - data class MenuItem(
  - enum class MenuPosition 
  - class ContextMenuOverlay(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.PixelFormat
  - import android.graphics.Point
  - import android.view.Gravity
  - import android.view.WindowManager
  - import androidx.compose.animation.*
  - import androidx.compose.animation.core.tween
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.*
  - ... and 15 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/overlays/NumberedSelectionOverlay.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.overlays`

**Classes/Interfaces/Objects:**
  - data class SelectableItem(
  - class NumberedSelectionOverlay(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.PixelFormat
  - import android.graphics.Rect
  - import android.view.Gravity
  - import android.view.WindowManager
  - import androidx.compose.animation.AnimatedVisibility
  - import androidx.compose.animation.fadeIn
  - import androidx.compose.animation.fadeOut
  - import androidx.compose.animation.scaleIn
  - import androidx.compose.animation.scaleOut
  - ... and 22 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/overlays/NumberOverlayRenderer.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.overlays`

**Classes/Interfaces/Objects:**
  - class NumberOverlayRenderer(

**Public Functions:**

**Imports:**
  - import android.content.res.Resources
  - import android.graphics.*

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/overlays/NumberOverlayStyle.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.overlays`

**Classes/Interfaces/Objects:**
  - data class NumberOverlayStyle(
  - enum class AnchorPoint 
  - enum class BadgeStyle 
  - enum class ElementVoiceState 
  - object NumberOverlayStyles 

**Public Functions:**

**Imports:**
  - import android.graphics.Color
  - import android.graphics.Typeface

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/overlays/OverlayIntegrationExample.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.overlays`

**Classes/Interfaces/Objects:**
  - class OverlayIntegrationExample 

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.graphics.Point
  - import android.graphics.Rect
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - import com.augmentalis.voiceos.speech.confidence.ConfidenceLevel
  - import com.augmentalis.voiceos.speech.confidence.ConfidenceResult
  - import com.augmentalis.voiceos.speech.confidence.ScoringMethod
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/overlays/OverlayManager.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.overlays`

**Classes/Interfaces/Objects:**
  - class OverlayManager(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.Point
  - import android.view.WindowManager
  - import com.augmentalis.voiceos.speech.confidence.ConfidenceResult

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/recognition/VoiceRecognitionBinder.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.recognition`

**Classes/Interfaces/Objects:**
  - class VoiceRecognitionBinder(

**Public Functions:**

**Imports:**
  - import android.content.ComponentName
  - import android.content.Context
  - import android.content.Intent
  - import android.content.ServiceConnection
  - import android.os.IBinder
  - import android.os.RemoteException
  - import android.util.Log
  - import com.augmentalis.voiceoscore.accessibility.managers.ActionCoordinator
  - import com.augmentalis.voicerecognition.IRecognitionCallback
  - import com.augmentalis.voicerecognition.IVoiceRecognitionService
  - ... and 4 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/recognition/VoiceRecognitionManager.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.recognition`

**Classes/Interfaces/Objects:**
  - class VoiceRecognitionManager(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.voiceoscore.accessibility.managers.ActionCoordinator
  - import kotlinx.coroutines.*

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/refactoring/DivergenceDetector.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.refactoring`

**Classes/Interfaces/Objects:**
  - class DivergenceDetector(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import java.util.concurrent.ConcurrentLinkedQueue
  - import java.util.concurrent.atomic.AtomicInteger
  - import java.util.concurrent.atomic.AtomicLong

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/refactoring/RefactoringFeatureFlags.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.refactoring`

**Classes/Interfaces/Objects:**
  - class RefactoringFeatureFlags private constructor(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import android.util.Log
  - import java.util.concurrent.atomic.AtomicBoolean
  - import java.util.concurrent.atomic.AtomicInteger

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/refactoring/RollbackController.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.refactoring`

**Classes/Interfaces/Objects:**
  - class RollbackController(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import java.util.concurrent.atomic.AtomicBoolean
  - import java.util.concurrent.atomic.AtomicLong

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/refactoring/ServiceComparisonFramework.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.refactoring`

**Classes/Interfaces/Objects:**
  - class ServiceComparisonFramework 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.launch
  - import java.util.concurrent.ConcurrentHashMap
  - import java.util.concurrent.atomic.AtomicLong

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/speech/SpeechEngineManager.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.speech`

**Classes/Interfaces/Objects:**
  - class SpeechEngineManager(private val context
  - data class SpeechState(
  - data class SpeechConfigurationData(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechEngine
  - import com.augmentalis.speechrecognition.SpeechMode
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import com.augmentalis.voiceos.speech.api.SpeechListenerManager
  - import com.augmentalis.voiceos.speech.engines.android.AndroidSTTEngine
  - import com.augmentalis.voiceos.speech.engines.vivoka.VivokaEngine
  - import com.augmentalis.voiceos.speech.engines.vosk.VoskEngine
  - ... and 13 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/state/DialogStateMachine.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.state`

**Classes/Interfaces/Objects:**
  - class DialogStateMachine 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import java.util.concurrent.atomic.AtomicReference
  - import java.util.concurrent.ConcurrentHashMap

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/state/UIState.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.state`

**Classes/Interfaces/Objects:**
  - data class UIState(

**Public Functions:**

**Imports:**
  - import android.graphics.Rect
  - import android.view.accessibility.AccessibilityNodeInfo
  - import android.os.Parcelable
  - import kotlinx.parcelize.Parcelize

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/AccessibilityDashboard.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun AccessibilityDashboard(
  - fun DashboardHeader() 
  - fun ServiceStatusCard(
  - fun QuickStatsSection(
  - fun StatisticItem(
  - fun PerformanceModeCard(performanceMode: String) 
  - fun HandlerStatusOverview(configuration: ServiceConfiguration) 
  - fun HandlerStatusItem(
  - fun DashboardNavigation(
  - fun DashboardNavigationCard(
  - fun SystemInfoCard(configuration: ServiceConfiguration) 
  - fun SystemInfoItem(label: String, value: String) 
  - fun StatusIndicatorBadge(isActive: Boolean) 

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.LazyColumn
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Alignment
  - ... and 11 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/AccessibilitySettings.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui`

**Classes/Interfaces/Objects:**
  - class AccessibilitySettings 

**Public Functions:**
  - fun SettingsScreen(
  - fun SettingsTopBar(
  - fun ServiceControlSection(
  - fun HandlerTogglesSection(
  - fun PerformanceSettingsSection(
  - fun CursorSettingsSection(
  - fun AdvancedSettingsSection(
  - fun SettingsSection(
  - fun SettingsToggle(
  - fun SettingsSlider(

**Imports:**
  - import android.content.Intent
  - import android.os.Bundle
  - import android.provider.Settings
  - import androidx.activity.ComponentActivity
  - import androidx.activity.compose.setContent
  - import androidx.activity.viewModels
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.LazyColumn
  - ... and 22 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/ConfidenceIndicator.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun ConfidenceIndicator(
  - fun DetailedConfidenceIndicator(
  - fun CircularConfidenceIndicator(
  - fun ConfidenceBadge(
  - fun ConfidenceThresholdIndicator(

**Imports:**
  - import androidx.compose.animation.core.animateFloatAsState
  - import androidx.compose.animation.core.tween
  - import androidx.compose.foundation.Canvas
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material3.MaterialTheme
  - import androidx.compose.material3.Text
  - import androidx.compose.runtime.Composable
  - import androidx.compose.runtime.getValue
  - ... and 11 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/MainActivity.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui`

**Classes/Interfaces/Objects:**
  - class MainActivity 

**Public Functions:**
  - fun MainScreen(
  - fun HeaderSection() 
  - fun ServiceStatusCard(
  - fun PermissionRow(
  - fun StatusIndicator(isActive: Boolean) 
  - fun QuickStatsCard(configuration: ServiceConfiguration) 
  - fun StatItem(label: String, value: String) 
  - fun NavigationCards(
  - fun NavigationCard(
  - fun FooterSection() 

**Imports:**
  - import android.content.Intent
  - import android.net.Uri
  - import android.os.Bundle
  - import android.provider.Settings
  - import androidx.activity.ComponentActivity
  - import androidx.activity.compose.setContent
  - import androidx.activity.result.contract.ActivityResultContracts
  - import androidx.activity.viewModels
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.clickable
  - ... and 46 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/overlays/BaseOverlay.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui.overlays`

**Classes/Interfaces/Objects:**
  - abstract class BaseOverlay(
  - enum class OverlayType 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.PixelFormat
  - import android.os.Build
  - import android.util.Log
  - import android.view.Gravity
  - import android.view.View
  - import android.view.WindowManager
  - import androidx.compose.runtime.Composable
  - import androidx.compose.runtime.DisposableEffect
  - import androidx.compose.runtime.remember
  - ... and 6 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/overlays/CommandDisambiguationOverlay.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui.overlays`

**Classes/Interfaces/Objects:**
  - data class DuplicateCommandOption(
  - object NumberWordConverter 
  - class CommandDisambiguationOverlay(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.Rect
  - import androidx.compose.animation.*
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.Canvas
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.border
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.LazyColumn
  - ... and 28 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/overlays/CommandLabelOverlay.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui.overlays`

**Classes/Interfaces/Objects:**
  - data class VoiceCommandLabel(
  - class CommandLabelOverlay(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.Rect
  - import android.util.Log
  - import android.view.accessibility.AccessibilityNodeInfo
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.border
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.LazyColumn
  - import androidx.compose.foundation.lazy.items
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - ... and 25 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/overlays/CursorMenuOverlay.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui.overlays`

**Classes/Interfaces/Objects:**
  - class CursorMenuOverlay(
  - enum class CursorAction(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.compose.animation.*
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.CircleShape
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.automirrored.filled.*
  - import androidx.compose.material.icons.filled.*
  - ... and 19 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/overlays/GridOverlay.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui.overlays`

**Classes/Interfaces/Objects:**
  - class GridOverlay(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.compose.animation.*
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.border
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.CircleShape
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material.icons.Icons
  - ... and 21 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/overlays/HelpOverlay.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui.overlays`

**Classes/Interfaces/Objects:**
  - class HelpOverlay(
  - enum class HelpCategory(
  - data class CommandGroup(
  - data class VoiceCommand(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.compose.animation.*
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.LazyColumn
  - import androidx.compose.foundation.lazy.LazyRow
  - import androidx.compose.foundation.lazy.items
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.automirrored.filled.*
  - ... and 12 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/overlays/NumberOverlay.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui.overlays`

**Classes/Interfaces/Objects:**
  - class NumberOverlay(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.Rect
  - import androidx.compose.animation.*
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.CircleShape
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - ... and 19 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/overlays/VoiceStatusOverlay.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui.overlays`

**Classes/Interfaces/Objects:**
  - class VoiceStatusOverlay(context
  - enum class VoiceStatus 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.compose.animation.AnimatedVisibility
  - import androidx.compose.animation.core.*
  - import androidx.compose.animation.fadeIn
  - import androidx.compose.animation.fadeOut
  - import androidx.compose.animation.scaleIn
  - import androidx.compose.animation.scaleOut
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.CircleShape
  - ... and 21 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/screens/CommandTestingScreen.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui.screens`

**Classes/Interfaces/Objects:**
  - data class CommandTestResult(

**Public Functions:**
  - fun CommandTestingScreen(
  - fun TestingHeader(onBack: () -> Unit) 
  - fun CommandInputSection(
  - fun QuickCommandsToggle(
  - fun PredefinedCommandsSection(onCommandSelect: (String) -> Unit) 
  - fun PredefinedCommandItem(
  - fun TestResultsSection(
  - fun TestResultItem(result: CommandTestResult) 

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.LazyColumn
  - import androidx.compose.foundation.lazy.items
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.foundation.text.KeyboardActions
  - import androidx.compose.foundation.text.KeyboardOptions
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - ... and 20 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/screens/SettingsScreen.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui.screens`

**Classes/Interfaces/Objects:**
  - enum class PerformanceMode(val displayName
  - data class HandlerInfo(

**Public Functions:**
  - fun SettingsScreen(
  - fun SettingsHeader(onBack: () -> Unit) 
  - fun PerformanceModeSection(
  - fun PerformanceModeItem(
  - fun HandlerTogglesSection(
  - fun HandlerToggleItem(
  - fun CursorConfigurationSection(
  - fun CacheSettingsSection(
  - fun AdvancedSettingsSection(
  - fun AdvancedSettingItem(

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.LazyColumn
  - import androidx.compose.foundation.selection.selectableGroup
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - import androidx.compose.material.icons.automirrored.filled.*
  - import androidx.compose.material3.*
  - ... and 14 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/theme/Theme.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui.theme`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun AccessibilityTheme(

**Imports:**
  - import androidx.compose.foundation.isSystemInDarkTheme
  - import androidx.compose.material3.MaterialTheme
  - import androidx.compose.material3.darkColorScheme
  - import androidx.compose.material3.lightColorScheme
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.graphics.Color

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/theme/Type.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui.theme`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import androidx.compose.material3.Typography
  - import androidx.compose.ui.text.TextStyle
  - import androidx.compose.ui.text.font.FontFamily
  - import androidx.compose.ui.text.font.FontWeight
  - import androidx.compose.ui.unit.sp

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/utils/DisplayUtils.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui.utils`

**Classes/Interfaces/Objects:**
  - object DisplayUtils 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.res.Resources
  - import android.graphics.Point
  - import android.os.Build
  - import android.util.DisplayMetrics
  - import android.view.WindowManager

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/ui/utils/ThemeUtils.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.ui.utils`

**Classes/Interfaces/Objects:**
  - data class GlassMorphismConfig(
  - data class DepthLevel(val depth
  - object GlassMorphismDefaults 
  - object ThemeUtils 

**Public Functions:**
  - fun Modifier.glassMorphism(
  - fun Modifier.glassMorphismBlur(
  - fun Modifier.floatingCard(
  - fun Modifier.interactiveGlass(
  - fun Modifier.statusIndicator(
  - fun Modifier.navigationCard(
  - fun Modifier.headerGlass(): Modifier 

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.border
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.draw.blur
  - import androidx.compose.ui.draw.clip
  - import androidx.compose.ui.graphics.Brush
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.unit.Dp
  - import androidx.compose.ui.unit.dp

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/utils/Debouncer.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.utils`

**Classes/Interfaces/Objects:**
  - class Debouncer(private val cooldownMillis

**Public Functions:**

**Imports:**
  - import android.os.SystemClock
  - import android.util.Log
  - import java.util.concurrent.ConcurrentHashMap

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/viewmodel/AccessibilityViewModel.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.viewmodel`

**Classes/Interfaces/Objects:**
  - class AccessibilityViewModel(application

**Public Functions:**

**Imports:**
  - import android.app.Application
  - import android.content.ComponentName
  - import android.content.Context
  - import android.provider.Settings
  - import android.view.accessibility.AccessibilityManager
  - import androidx.lifecycle.AndroidViewModel
  - import androidx.lifecycle.LiveData
  - import androidx.lifecycle.MutableLiveData
  - import androidx.lifecycle.viewModelScope
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService
  - ... and 8 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/viewmodel/MainViewModel.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.viewmodel`

**Classes/Interfaces/Objects:**
  - class MainViewModel 
  - data class PermissionSummary(

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityServiceInfo
  - import android.content.ComponentName
  - import android.content.Context
  - import android.net.Uri
  - import android.os.Build
  - import android.provider.Settings
  - import android.view.accessibility.AccessibilityManager
  - import androidx.lifecycle.LiveData
  - import androidx.lifecycle.MutableLiveData
  - import androidx.lifecycle.ViewModel
  - ... and 6 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/viewmodel/SettingsViewModel.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.viewmodel`

**Classes/Interfaces/Objects:**
  - enum class PerformanceMode(
  - data class HandlerInfo(
  - class SettingsViewModel(application

**Public Functions:**

**Imports:**
  - import android.app.Application
  - import android.content.Context
  - import android.util.Log
  - import androidx.lifecycle.AndroidViewModel
  - import androidx.lifecycle.viewModelScope
  - import com.augmentalis.voiceoscore.accessibility.config.ServiceConfiguration
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow
  - import kotlinx.coroutines.launch

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOnSentry.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility`

**Classes/Interfaces/Objects:**
  - class VoiceOnSentry 

**Public Functions:**

**Imports:**
  - import android.app.Notification
  - import android.app.NotificationChannel
  - import android.app.NotificationManager
  - import android.app.PendingIntent
  - import android.content.Intent
  - import android.content.pm.ServiceInfo
  - import android.os.Build
  - import android.os.IBinder
  - import android.util.Log
  - import androidx.core.app.NotificationCompat
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility`

**Classes/Interfaces/Objects:**
  - class VoiceOSService 

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.accessibilityservice.AccessibilityServiceInfo
  - import android.accessibilityservice.GestureDescription.Builder
  - import android.accessibilityservice.GestureDescription.StrokeDescription
  - import android.content.Intent
  - import android.os.Build
  - import android.util.ArrayMap
  - import android.util.Log
  - import android.view.accessibility.AccessibilityEvent
  - import androidx.lifecycle.DefaultLifecycleObserver
  - ... and 39 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/learnweb/GeneratedWebCommandDao.kt`

**Package:** `com.augmentalis.voiceoscore.learnweb`

**Classes/Interfaces/Objects:**
  - interface GeneratedWebCommandDao 

**Public Functions:**

**Imports:**
  - import androidx.room.*

---

### File: `src/main/java/com/augmentalis/voiceoscore/learnweb/LearnWebActivity.kt`

**Package:** `com.augmentalis.voiceoscore.learnweb`

**Classes/Interfaces/Objects:**
  - class LearnWebActivity 

**Public Functions:**

**Imports:**
  - import android.annotation.SuppressLint
  - import android.os.Bundle
  - import android.util.Log
  - import android.view.View
  - import android.webkit.*
  - import android.widget.ProgressBar
  - import android.widget.TextView
  - import android.widget.Toast
  - import androidx.appcompat.app.AppCompatActivity
  - import androidx.lifecycle.lifecycleScope
  - ... and 4 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/learnweb/ScrapedWebElementDao.kt`

**Package:** `com.augmentalis.voiceoscore.learnweb`

**Classes/Interfaces/Objects:**
  - interface ScrapedWebElementDao 

**Public Functions:**

**Imports:**
  - import androidx.room.*

---

### File: `src/main/java/com/augmentalis/voiceoscore/learnweb/ScrapedWebsiteDao.kt`

**Package:** `com.augmentalis.voiceoscore.learnweb`

**Classes/Interfaces/Objects:**
  - data class CacheStats(
  - interface ScrapedWebsiteDao 

**Public Functions:**

**Imports:**
  - import androidx.room.*

---

### File: `src/main/java/com/augmentalis/voiceoscore/learnweb/WebCommandCache.kt`

**Package:** `com.augmentalis.voiceoscore.learnweb`

**Classes/Interfaces/Objects:**
  - class WebCommandCache(private val database
  - sealed class CacheResult 
  - data class WebCacheStats(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import kotlinx.coroutines.*
  - import java.net.URL
  - import java.security.MessageDigest

---

### File: `src/main/java/com/augmentalis/voiceoscore/learnweb/WebCommandGenerator.kt`

**Package:** `com.augmentalis.voiceoscore.learnweb`

**Classes/Interfaces/Objects:**
  - class WebCommandGenerator 
  - data class CommandStats(

**Public Functions:**

**Imports:**
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/voiceoscore/learnweb/WebScrapingDatabase.kt`

**Package:** `com.augmentalis.voiceoscore.learnweb`

**Classes/Interfaces/Objects:**
  - abstract class WebScrapingDatabase 
  - data class ScrapedWebsite(
  - data class ScrapedWebElement(
  - data class GeneratedWebCommand(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.room.Database
  - import androidx.room.Room
  - import androidx.room.RoomDatabase

---

### File: `src/main/java/com/augmentalis/voiceoscore/learnweb/WebViewScrapingEngine.kt`

**Package:** `com.augmentalis.voiceoscore.learnweb`

**Classes/Interfaces/Objects:**
  - class WebViewScrapingEngine(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import android.webkit.WebView
  - import kotlinx.coroutines.suspendCancellableCoroutine
  - import org.json.JSONArray
  - import org.json.JSONObject
  - import java.security.MessageDigest
  - import kotlin.coroutines.resume
  - import kotlin.coroutines.resumeWithException

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/di/RefactoringQualifiers.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.di`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import javax.inject.Qualifier

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/di/RefactoringScope.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.di`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import javax.inject.Scope

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/di/VoiceOSServiceDirector.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.di`

**Classes/Interfaces/Objects:**
  - object VoiceOSServiceDirector 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.*
  - import dagger.Module
  - import dagger.Provides
  - import dagger.hilt.InstallIn
  - import dagger.hilt.android.qualifiers.ApplicationContext
  - import dagger.hilt.components.SingletonComponent
  - import javax.inject.Singleton

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/BurstDetector.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class BurstDetector(

**Public Functions:**

**Imports:**
  - import java.util.concurrent.ConcurrentHashMap

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/CacheDataClasses.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - data class CachedCommands(
  - data class CachedElements(
  - data class CachedGeneratedCommands(
  - data class CachedWebCommands(
  - data class LruCacheEntry<T>(
  - class CacheStatsTracker 
  - class SimpleLruCache<K, V>(private val maxSize

**Public Functions:**

**Imports:**
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IDatabaseManager.*
  - import java.time.Clock
  - import java.time.Instant
  - import java.time.Duration
  - import java.util.concurrent.TimeUnit

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/CommandOrchestratorImpl.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class CommandOrchestratorImpl @Inject constructor(

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.commandmanager.CommandManager
  - import com.augmentalis.commandmanager.models.Command
  - import com.augmentalis.commandmanager.models.CommandContext
  - import com.augmentalis.commandmanager.models.CommandSource
  - import com.augmentalis.voiceoscore.accessibility.managers.ActionCoordinator
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ICommandOrchestrator
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ICommandOrchestrator.*
  - ... and 17 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/DatabaseConfig.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - data class CacheConfig(
  - data class TransactionConfig(
  - data class DatabaseManagerConfig(

**Public Functions:**

**Imports:**
  - import kotlin.time.Duration
  - import kotlin.time.Duration.Companion.milliseconds
  - import kotlin.time.Duration.Companion.seconds

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/DatabaseManagerImpl.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class DatabaseManagerImpl(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.commandmanager.database.CommandDatabase
  - import com.augmentalis.commandmanager.database.VoiceCommandEntity
  - import com.augmentalis.voiceoscore.learnweb.WebScrapingDatabase
  - import com.augmentalis.voiceoscore.learnweb.GeneratedWebCommand as WebCommandEntity
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IDatabaseManager
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IDatabaseManager.*
  - import com.augmentalis.voiceoscore.scraping.database.AppScrapingDatabase
  - import com.augmentalis.voiceoscore.scraping.entities.GeneratedCommandEntity
  - ... and 9 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/ElementHashGenerator.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - object ElementHashGenerator 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService.UIElement
  - import java.security.MessageDigest

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/EventFilter.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class EventFilter(

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityEvent
  - import java.util.concurrent.ConcurrentHashMap

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/EventRouterImpl.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class EventRouterImpl @Inject constructor(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import android.view.accessibility.AccessibilityEvent
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IEventRouter
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IEventRouter.*
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.channels.BufferOverflow
  - import kotlinx.coroutines.channels.Channel
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/healthcheckers/AccessibilityServiceHealthChecker.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl.healthcheckers`

**Classes/Interfaces/Objects:**
  - class AccessibilityServiceHealthChecker(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.ComponentHealth
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.HealthStatus
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.MonitoredComponent

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/healthcheckers/CommandManagerHealthChecker.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl.healthcheckers`

**Classes/Interfaces/Objects:**
  - class CommandManagerHealthChecker(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.ComponentHealth
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.HealthStatus
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.MonitoredComponent

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/healthcheckers/ComponentHealthChecker.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl.healthcheckers`

**Classes/Interfaces/Objects:**
  - interface ComponentHealthChecker 

**Public Functions:**

**Imports:**
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.ComponentHealth

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/healthcheckers/CursorApiHealthChecker.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl.healthcheckers`

**Classes/Interfaces/Objects:**
  - class CursorApiHealthChecker(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.ComponentHealth
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.HealthStatus
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.MonitoredComponent

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/healthcheckers/DatabaseHealthChecker.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl.healthcheckers`

**Classes/Interfaces/Objects:**
  - class DatabaseHealthChecker(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.ComponentHealth
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.HealthStatus
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.MonitoredComponent
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/healthcheckers/EventRouterHealthChecker.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl.healthcheckers`

**Classes/Interfaces/Objects:**
  - class EventRouterHealthChecker(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.ComponentHealth
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.HealthStatus
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.MonitoredComponent

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/healthcheckers/LearnAppHealthChecker.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl.healthcheckers`

**Classes/Interfaces/Objects:**
  - class LearnAppHealthChecker(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.ComponentHealth
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.HealthStatus
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.MonitoredComponent

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/healthcheckers/SpeechEngineHealthChecker.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl.healthcheckers`

**Classes/Interfaces/Objects:**
  - class SpeechEngineHealthChecker(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.ComponentHealth
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.HealthStatus
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.MonitoredComponent

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/healthcheckers/StateManagerHealthChecker.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl.healthcheckers`

**Classes/Interfaces/Objects:**
  - class StateManagerHealthChecker(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.ComponentHealth
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.HealthStatus
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.MonitoredComponent

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/healthcheckers/UIScrapingHealthChecker.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl.healthcheckers`

**Classes/Interfaces/Objects:**
  - class UIScrapingHealthChecker(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.ComponentHealth
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.HealthStatus
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.MonitoredComponent

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/healthcheckers/WebCoordinatorHealthChecker.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl.healthcheckers`

**Classes/Interfaces/Objects:**
  - class WebCoordinatorHealthChecker(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.ComponentHealth
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.HealthStatus
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.MonitoredComponent

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/PerformanceMetricsCollector.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class PerformanceMetricsCollector(

**Public Functions:**

**Imports:**
  - import android.app.ActivityManager
  - import android.content.Context
  - import android.os.BatteryManager
  - import android.os.Build
  - import android.os.Debug
  - import android.util.Log
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.PerformanceSnapshot
  - import java.io.File
  - import java.io.RandomAccessFile
  - import kotlin.math.max

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/PrioritizedEvent.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - data class PrioritizedEvent(

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityEvent

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/ScrapedElementExtractor.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class ScrapedElementExtractor(

**Public Functions:**

**Imports:**
  - import android.graphics.Rect
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService.ElementBounds
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService.ScrapingConfig
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService.UIElement
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/ScreenDiff.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - data class ScreenDiff(
  - data class ElementChange(

**Public Functions:**

**Imports:**
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService.UIElement

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/ServiceMonitorImpl.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class ServiceMonitorImpl @Inject constructor(

**Public Functions:**

**Imports:**
  - import android.app.ActivityManager
  - import android.content.Context
  - import android.os.BatteryManager
  - import android.util.Log
  - import com.augmentalis.voiceoscore.refactoring.impl.healthcheckers.*
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.*
  - import dagger.hilt.android.qualifiers.ApplicationContext
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.channels.BufferOverflow
  - ... and 8 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/SpeechManagerImpl.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class SpeechManagerImpl @Inject constructor(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ISpeechManager
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ISpeechManager.*
  - import com.augmentalis.voiceos.speech.engines.vivoka.VivokaEngine
  - import com.augmentalis.voiceos.speech.engines.vosk.VoskEngine
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import com.augmentalis.voiceos.speech.api.SpeechListenerManager
  - import com.augmentalis.speechrecognition.SpeechConfig as LibrarySpeechConfig
  - import com.augmentalis.speechrecognition.SpeechEngine as LibrarySpeechEngine
  - ... and 8 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/StateManagerImpl.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class StateManagerImpl @Inject constructor() 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager.*
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.cancel
  - import kotlinx.coroutines.channels.BufferOverflow
  - import kotlinx.coroutines.flow.Flow
  - ... and 12 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/impl/UIScrapingServiceImpl.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class UIScrapingServiceImpl @Inject constructor(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.view.accessibility.AccessibilityEvent
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IDatabaseManager
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService.*
  - import dagger.hilt.android.qualifiers.ApplicationContext
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.SharedFlow
  - ... and 4 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/interfaces/ICommandOrchestrator.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.interfaces`

**Classes/Interfaces/Objects:**
  - interface ICommandOrchestrator 

**Public Functions:**

**Imports:**
  - import com.augmentalis.commandmanager.models.Command
  - import com.augmentalis.commandmanager.models.CommandContext
  - import kotlinx.coroutines.flow.Flow

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/interfaces/IDatabaseManager.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.interfaces`

**Classes/Interfaces/Objects:**
  - interface IDatabaseManager 

**Public Functions:**

**Imports:**
  - import kotlinx.coroutines.flow.Flow

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/interfaces/IEventRouter.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.interfaces`

**Classes/Interfaces/Objects:**
  - interface IEventRouter 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityEvent
  - import kotlinx.coroutines.flow.Flow

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/interfaces/IServiceMonitor.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.interfaces`

**Classes/Interfaces/Objects:**
  - interface IServiceMonitor 

**Public Functions:**

**Imports:**
  - import kotlinx.coroutines.flow.Flow

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/interfaces/ISpeechManager.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.interfaces`

**Classes/Interfaces/Objects:**
  - interface ISpeechManager 

**Public Functions:**

**Imports:**
  - import kotlinx.coroutines.flow.Flow

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/interfaces/IStateManager.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.interfaces`

**Classes/Interfaces/Objects:**
  - interface IStateManager 

**Public Functions:**

**Imports:**
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.StateFlow

---

### File: `src/main/java/com/augmentalis/voiceoscore/refactoring/interfaces/IUIScrapingService.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.interfaces`

**Classes/Interfaces/Objects:**
  - interface IUIScrapingService 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityEvent
  - import android.view.accessibility.AccessibilityNodeInfo
  - import kotlinx.coroutines.flow.Flow

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/AccessibilityScrapingIntegration.kt`

**Package:** `com.augmentalis.voiceoscore.scraping`

**Classes/Interfaces/Objects:**
  - class AccessibilityScrapingIntegration(
  - data class LearnAppResult(

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.content.Intent
  - import android.content.IntentFilter
  - import android.content.pm.PackageManager
  - import android.graphics.Rect
  - import android.os.BatteryManager
  - import android.util.Log
  - import android.view.accessibility.AccessibilityEvent
  - import android.view.accessibility.AccessibilityNodeInfo
  - ... and 19 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/AppHashCalculator.kt`

**Package:** `com.augmentalis.voiceoscore.scraping`

**Classes/Interfaces/Objects:**
  - object AppHashCalculator 

**Public Functions:**
  - fun AccessibilityNodeInfo.toHash(): String 
  - fun String.toMD5(): String 

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo
  - import java.security.MessageDigest

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/CommandGenerator.kt`

**Package:** `com.augmentalis.voiceoscore.scraping`

**Classes/Interfaces/Objects:**
  - class CommandGenerator(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.scraping.database.AppScrapingDatabase
  - import com.augmentalis.voiceoscore.scraping.entities.GeneratedCommandEntity
  - import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity
  - import com.augmentalis.voiceoscore.scraping.entities.StateType
  - import org.json.JSONArray

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/dao/ElementRelationshipDao.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

**Classes/Interfaces/Objects:**
  - interface ElementRelationshipDao 

**Public Functions:**

**Imports:**
  - import androidx.room.Dao
  - import androidx.room.Insert
  - import androidx.room.OnConflictStrategy
  - import androidx.room.Query
  - import com.augmentalis.voiceoscore.scraping.entities.ElementRelationshipEntity

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/dao/ElementStateHistoryDao.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

**Classes/Interfaces/Objects:**
  - interface ElementStateHistoryDao 
  - data class ElementStateChangeCount(

**Public Functions:**

**Imports:**
  - import androidx.room.Dao
  - import androidx.room.Insert
  - import androidx.room.Query
  - import com.augmentalis.voiceoscore.scraping.entities.ElementStateHistoryEntity

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/dao/GeneratedCommandDao.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

**Classes/Interfaces/Objects:**
  - interface GeneratedCommandDao 

**Public Functions:**

**Imports:**
  - import androidx.room.Dao
  - import androidx.room.Insert
  - import androidx.room.OnConflictStrategy
  - import androidx.room.Query
  - import androidx.room.Update
  - import com.augmentalis.voiceoscore.scraping.entities.GeneratedCommandEntity

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/dao/ScrapedAppDao.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

**Classes/Interfaces/Objects:**
  - interface ScrapedAppDao 

**Public Functions:**

**Imports:**
  - import androidx.room.Dao
  - import androidx.room.Insert
  - import androidx.room.OnConflictStrategy
  - import androidx.room.Query
  - import androidx.room.Update
  - import com.augmentalis.voiceoscore.scraping.entities.ScrapedAppEntity

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/dao/ScrapedElementDao.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

**Classes/Interfaces/Objects:**
  - interface ScrapedElementDao 

**Public Functions:**

**Imports:**
  - import androidx.room.Dao
  - import androidx.room.Insert
  - import androidx.room.OnConflictStrategy
  - import androidx.room.Query
  - import androidx.room.Update
  - import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/dao/ScrapedHierarchyDao.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

**Classes/Interfaces/Objects:**
  - interface ScrapedHierarchyDao 

**Public Functions:**

**Imports:**
  - import androidx.room.Dao
  - import androidx.room.Insert
  - import androidx.room.OnConflictStrategy
  - import androidx.room.Query
  - import com.augmentalis.voiceoscore.scraping.entities.ScrapedHierarchyEntity

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/dao/ScreenContextDao.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

**Classes/Interfaces/Objects:**
  - interface ScreenContextDao 

**Public Functions:**

**Imports:**
  - import androidx.room.Dao
  - import androidx.room.Insert
  - import androidx.room.OnConflictStrategy
  - import androidx.room.Query
  - import androidx.room.Update
  - import com.augmentalis.voiceoscore.scraping.entities.ScreenContextEntity

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/dao/ScreenTransitionDao.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

**Classes/Interfaces/Objects:**
  - interface ScreenTransitionDao 

**Public Functions:**

**Imports:**
  - import androidx.room.Dao
  - import androidx.room.Insert
  - import androidx.room.OnConflictStrategy
  - import androidx.room.Query
  - import com.augmentalis.voiceoscore.scraping.entities.ScreenTransitionEntity

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/dao/UserInteractionDao.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.dao`

**Classes/Interfaces/Objects:**
  - interface UserInteractionDao 
  - data class ElementInteractionCount(
  - data class InteractionRatio(

**Public Functions:**

**Imports:**
  - import androidx.room.Dao
  - import androidx.room.Insert
  - import androidx.room.Query
  - import com.augmentalis.voiceoscore.scraping.entities.UserInteractionEntity

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/database/AppScrapingDatabase.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.database`

**Classes/Interfaces/Objects:**
  - abstract class AppScrapingDatabase 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.room.Database
  - import androidx.room.Room
  - import androidx.room.RoomDatabase
  - import androidx.room.migration.Migration
  - import androidx.sqlite.db.SupportSQLiteDatabase
  - import com.augmentalis.voiceoscore.scraping.dao.ElementRelationshipDao
  - import com.augmentalis.voiceoscore.scraping.dao.ElementStateHistoryDao
  - import com.augmentalis.voiceoscore.scraping.dao.GeneratedCommandDao
  - import com.augmentalis.voiceoscore.scraping.dao.ScrapedAppDao
  - ... and 19 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/ElementHasher.kt`

**Package:** `com.augmentalis.voiceoscore.scraping`

**Classes/Interfaces/Objects:**
  - object ElementHasher 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo
  - import java.security.MessageDigest

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/entities/ElementRelationshipEntity.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.entities`

**Classes/Interfaces/Objects:**
  - data class ElementRelationshipEntity(
  - object RelationshipType 

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.ForeignKey
  - import androidx.room.Index
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/entities/ElementStateHistoryEntity.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.entities`

**Classes/Interfaces/Objects:**
  - data class ElementStateHistoryEntity(
  - object StateType 
  - object TriggerSource 

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.ForeignKey
  - import androidx.room.Index
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/entities/GeneratedCommandEntity.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.entities`

**Classes/Interfaces/Objects:**
  - data class GeneratedCommandEntity(

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.ForeignKey
  - import androidx.room.Index
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/entities/GeneratedCommandWithPackageName.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.entities`

**Classes/Interfaces/Objects:**
  - data class GeneratedCommandWithPackageName(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/entities/ScrapedAppEntity.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.entities`

**Classes/Interfaces/Objects:**
  - data class ScrapedAppEntity(

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/entities/ScrapedElementEntity.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.entities`

**Classes/Interfaces/Objects:**
  - data class ScrapedElementEntity(

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.ForeignKey
  - import androidx.room.Index
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/entities/ScrapedHierarchyEntity.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.entities`

**Classes/Interfaces/Objects:**
  - data class ScrapedHierarchyEntity(

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.ForeignKey
  - import androidx.room.Index
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/entities/ScreenContextEntity.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.entities`

**Classes/Interfaces/Objects:**
  - data class ScreenContextEntity(

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.ForeignKey
  - import androidx.room.Index
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/entities/ScreenTransitionEntity.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.entities`

**Classes/Interfaces/Objects:**
  - data class ScreenTransitionEntity(

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.ForeignKey
  - import androidx.room.Index
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/entities/UserInteractionEntity.kt`

**Package:** `com.augmentalis.voiceoscore.scraping.entities`

**Classes/Interfaces/Objects:**
  - data class UserInteractionEntity(
  - object InteractionType 

**Public Functions:**

**Imports:**
  - import androidx.room.ColumnInfo
  - import androidx.room.Entity
  - import androidx.room.ForeignKey
  - import androidx.room.Index
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/ScrapingMode.kt`

**Package:** `com.augmentalis.voiceoscore.scraping`

**Classes/Interfaces/Objects:**
  - enum class ScrapingMode 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/ScreenContextInferenceHelper.kt`

**Package:** `com.augmentalis.voiceoscore.scraping`

**Classes/Interfaces/Objects:**
  - class ScreenContextInferenceHelper 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/SemanticInferenceHelper.kt`

**Package:** `com.augmentalis.voiceoscore.scraping`

**Classes/Interfaces/Objects:**
  - class SemanticInferenceHelper 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo

---

### File: `src/main/java/com/augmentalis/voiceoscore/scraping/VoiceCommandProcessor.kt`

**Package:** `com.augmentalis.voiceoscore.scraping`

**Classes/Interfaces/Objects:**
  - class VoiceCommandProcessor(
  - data class CommandResult(

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.content.pm.PackageManager
  - import android.os.Bundle
  - import android.util.Log
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.commandmanager.CommandManager
  - import com.augmentalis.commandmanager.models.Command
  - import com.augmentalis.commandmanager.models.CommandSource
  - import com.augmentalis.voiceoscore.scraping.database.AppScrapingDatabase
  - ... and 4 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/testing/ComparisonFramework.kt`

**Package:** `com.augmentalis.voiceoscore.testing`

**Classes/Interfaces/Objects:**
  - data class ComparisonConfig(
  - class ComparisonFramework(
  - class LoggingRollbackTrigger 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.async
  - import kotlinx.coroutines.withContext
  - import java.util.UUID

---

### File: `src/main/java/com/augmentalis/voiceoscore/testing/ComparisonMetrics.kt`

**Package:** `com.augmentalis.voiceoscore.testing`

**Classes/Interfaces/Objects:**
  - data class ComparisonMetricsSnapshot(
  - data class MethodMetrics(
  - class ComparisonMetricsCollector 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow
  - import java.util.concurrent.ConcurrentHashMap
  - import java.util.concurrent.atomic.AtomicLong

---

### File: `src/main/java/com/augmentalis/voiceoscore/testing/DivergenceAlerts.kt`

**Package:** `com.augmentalis.voiceoscore.testing`

**Classes/Interfaces/Objects:**
  - enum class AlertAction 
  - data class AlertRule(
  - data class DivergenceAlert(
  - interface AlertListener 
  - interface RollbackTrigger 
  - class ComparisonCircuitBreaker(
  - class DivergenceAlertSystem(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.channels.Channel
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.SharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import kotlinx.coroutines.launch
  - import java.util.concurrent.ConcurrentHashMap
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/testing/DivergenceReport.kt`

**Package:** `com.augmentalis.voiceoscore.testing`

**Classes/Interfaces/Objects:**
  - enum class DivergenceSeverity(val priority
  - enum class DivergenceCategory 
  - data class DivergenceDetail(
  - data class ComparisonResult(
  - data class DivergenceStats(
  - class DivergenceReporter 

**Public Functions:**

**Imports:**
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.SharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import java.util.concurrent.ConcurrentHashMap
  - import java.util.concurrent.atomic.AtomicLong

---

### File: `src/main/java/com/augmentalis/voiceoscore/testing/ReturnValueComparator.kt`

**Package:** `com.augmentalis.voiceoscore.testing`

**Classes/Interfaces/Objects:**
  - class ReturnValueComparator 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import kotlinx.coroutines.CompletableDeferred
  - import kotlinx.coroutines.withTimeoutOrNull
  - import kotlin.reflect.KClass

---

### File: `src/main/java/com/augmentalis/voiceoscore/testing/SideEffectComparator.kt`

**Package:** `com.augmentalis.voiceoscore.testing`

**Classes/Interfaces/Objects:**
  - enum class SideEffectType 
  - data class SideEffect(
  - data class SideEffectTrace(
  - class SideEffectTracker 
  - class SideEffectComparator 

**Public Functions:**

**Imports:**
  - import android.content.Intent
  - import android.util.Log
  - import java.util.concurrent.CopyOnWriteArrayList

---

### File: `src/main/java/com/augmentalis/voiceoscore/testing/StateComparator.kt`

**Package:** `com.augmentalis.voiceoscore.testing`

**Classes/Interfaces/Objects:**
  - data class ServiceStateSnapshot(
  - class StateComparator 
  - interface StateExporter 

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService

---

### File: `src/main/java/com/augmentalis/voiceoscore/testing/TimingComparator.kt`

**Package:** `com.augmentalis.voiceoscore.testing`

**Classes/Interfaces/Objects:**
  - data class TimingMeasurement(
  - data class TimingStats(
  - class TimingComparator 

**Public Functions:**
  - inline fun <T> measureTiming(block: () -> T): Pair<T, Long> 

**Imports:**
  - import android.util.Log
  - import java.util.concurrent.ConcurrentHashMap
  - import kotlin.math.abs

---

### File: `src/main/java/com/augmentalis/voiceoscore/ui/components/FloatingEngineSelector.kt`

**Package:** `com.augmentalis.voiceoscore.ui.components`

**Classes/Interfaces/Objects:**
  - data class EngineOption(

**Public Functions:**
  - fun FloatingEngineSelector(

**Imports:**
  - import androidx.compose.animation.AnimatedVisibility
  - import androidx.compose.animation.expandHorizontally
  - import androidx.compose.animation.shrinkHorizontally
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.CircleShape
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.PlayArrow
  - ... and 12 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/ui/LearnAppActivity.kt`

**Package:** `com.augmentalis.voiceoscore.ui`

**Classes/Interfaces/Objects:**
  - class LearnAppActivity 
  - data class AppInfo(

**Public Functions:**
  - fun LearnAppScreen(
  - fun LearnAppHeader() 
  - fun ResultCard(result: LearnAppResult) 
  - fun AppCard(

**Imports:**
  - import android.content.pm.ApplicationInfo
  - import android.content.pm.PackageManager
  - import android.os.Bundle
  - import androidx.activity.ComponentActivity
  - import androidx.activity.compose.setContent
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.Arrangement
  - import androidx.compose.foundation.layout.Box
  - import androidx.compose.foundation.layout.Column
  - ... and 47 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/ui/overlays/NumberOverlayConfig.kt`

**Package:** `com.augmentalis.voiceoscore.ui.overlays`

**Classes/Interfaces/Objects:**
  - data class NumberOverlayConfig(
  - class NumberOverlayConfigManager(private val context
  - class NumberOverlayConfigBuilder 

**Public Functions:**
  - fun NumberOverlayConfig.Companion.build(block: NumberOverlayConfigBuilder.() -> Unit): NumberOverlayConfig 

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import android.content.res.Configuration
  - import android.provider.Settings
  - import androidx.core.content.edit

---

### File: `src/main/java/com/augmentalis/voiceoscore/ui/overlays/NumberOverlayManager.kt`

**Package:** `com.augmentalis.voiceoscore.ui.overlays`

**Classes/Interfaces/Objects:**
  - class NumberOverlayManager(
  - interface WindowFocusListener 
  - data class ManagerStatistics(

**Public Functions:**
  - fun List<AccessibilityNodeInfo>.toOverlayData(

**Imports:**
  - import android.content.Context
  - import android.graphics.PixelFormat
  - import android.graphics.Rect
  - import android.os.Build
  - import android.util.Log
  - import android.view.*
  - import android.view.accessibility.AccessibilityNodeInfo
  - import androidx.core.view.isVisible
  - import kotlinx.coroutines.*
  - import java.lang.ref.WeakReference

---

### File: `src/main/java/com/augmentalis/voiceoscore/ui/overlays/NumberOverlayRenderer.kt`

**Package:** `com.augmentalis.voiceoscore.ui.overlays`

**Classes/Interfaces/Objects:**
  - class NumberOverlayRenderer @JvmOverloads constructor(
  - data class OverlayData(
  - data class PerformanceMetrics(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.*
  - import android.util.AttributeSet
  - import android.view.View
  - import androidx.core.content.res.ResourcesCompat
  - import kotlin.math.max

---

### File: `src/main/java/com/augmentalis/voiceoscore/ui/overlays/NumberOverlayStyle.kt`

**Package:** `com.augmentalis.voiceoscore.ui.overlays`

**Classes/Interfaces/Objects:**
  - data class NumberOverlayStyle(
  - enum class AnchorPoint 
  - enum class ElementVoiceState 
  - enum class StyleVariant 
  - data class RenderConfig(

**Public Functions:**
  - fun StyleVariant.toStyle(): NumberOverlayStyle = when (this) 

**Imports:**
  - import android.graphics.Color
  - import android.graphics.Typeface

---

### File: `src/main/java/com/augmentalis/voiceoscore/url/URLBarInteractionManager.kt`

**Package:** `com.augmentalis.voiceoscore.url`

**Classes/Interfaces/Objects:**
  - class URLBarInteractionManager(
  - class URLBarPreferences(context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import android.os.Bundle
  - import android.os.SystemClock
  - import android.speech.tts.TextToSpeech
  - import android.util.Log
  - import android.view.KeyEvent
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService
  - import java.util.Locale

---

### File: `src/main/java/com/augmentalis/voiceoscore/web/WebCommandCoordinator.kt`

**Package:** `com.augmentalis.voiceoscore.web`

**Classes/Interfaces/Objects:**
  - class WebCommandCoordinator(

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.graphics.Rect
  - import android.util.Log
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.voiceoscore.learnweb.WebScrapingDatabase
  - import com.augmentalis.voiceoscore.learnweb.ScrapedWebElement
  - import com.augmentalis.voiceoscore.learnweb.GeneratedWebCommand
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.withContext
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/voiceoscore/webview/VOSWebInterface.kt`

**Package:** `com.augmentalis.voiceoscore.webview`

**Classes/Interfaces/Objects:**
  - class VOSWebInterface(

**Public Functions:**

**Imports:**
  - import android.webkit.JavascriptInterface
  - import android.util.Log
  - import org.json.JSONArray

---

### File: `src/main/java/com/augmentalis/voiceoscore/webview/VOSWebView.kt`

**Package:** `com.augmentalis.voiceoscore.webview`

**Classes/Interfaces/Objects:**
  - class VOSWebView @JvmOverloads constructor(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.AttributeSet
  - import android.webkit.JavascriptInterface
  - import android.webkit.WebChromeClient
  - import android.webkit.WebResourceRequest
  - import android.webkit.WebView
  - import android.webkit.WebViewClient
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/voiceoscore/webview/VOSWebViewSample.kt`

**Package:** `com.augmentalis.voiceoscore.webview`

**Classes/Interfaces/Objects:**
  - object VOSWebViewSample 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import android.view.ViewGroup
  - import android.widget.FrameLayout

---

### File: `src/main/java/com/augmentalis/voiceoscore/webview/WebCommandExecutor.kt`

**Package:** `com.augmentalis.voiceoscore.webview`

**Classes/Interfaces/Objects:**
  - class WebCommandExecutor(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import android.webkit.WebView
  - import org.json.JSONArray
  - import org.json.JSONObject

---

### File: `src/main/java/com/augmentalis/voicerecognition/RecognitionData.kt`

**Package:** `com.augmentalis.voicerecognition`

**Classes/Interfaces/Objects:**
  - data class RecognitionData(

**Public Functions:**

**Imports:**
  - import android.os.Parcel
  - import android.os.Parcelable

---

### File: `src/test/java/com/augmentalis/voiceoscore/accessibility/handlers/DragHandlerTest.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - class DragHandlerTest 

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.GestureDescription
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService
  - import com.augmentalis.voiceos.cursor.core.CursorOffset
  - import io.mockk.*
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.test.*
  - import org.junit.After
  - import org.junit.Before
  - import org.junit.Test
  - import kotlin.test.assertEquals
  - ... and 2 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/accessibility/handlers/GazeHandlerTest.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - class GazeHandlerTest 

**Public Functions:**

**Imports:**
  - import org.junit.Ignore
  - import org.junit.Test

---

### File: `src/test/java/com/augmentalis/voiceoscore/accessibility/handlers/GestureHandlerTest.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.handlers`

**Classes/Interfaces/Objects:**
  - class GestureHandlerTest 
  - object GestureTestScenarios 

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService.GestureResultCallback
  - import android.accessibilityservice.GestureDescription
  - import android.graphics.Point
  - import android.os.Handler
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService
  - import com.augmentalis.voiceoscore.accessibility.handlers.ActionCategory
  - import io.mockk.*
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.ExperimentalCoroutinesApi
  - ... and 9 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/accessibility/integration/UUIDCreatorIntegrationTest.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.integration`

**Classes/Interfaces/Objects:**
  - class UUIDCreatorIntegrationTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.Rect
  - import android.view.accessibility.AccessibilityNodeInfo
  - import androidx.room.Room
  - import androidx.test.core.app.ApplicationProvider
  - import com.augmentalis.uuidcreator.UUIDCreator
  - import com.augmentalis.uuidcreator.database.UUIDCreatorDatabase
  - import com.augmentalis.uuidcreator.models.*
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.test.*
  - ... and 7 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/accessibility/mocks/MockVoiceAccessibilityService.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.mocks`

**Classes/Interfaces/Objects:**
  - class MockVoiceAccessibilityService 

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.accessibilityservice.GestureDescription
  - import android.content.Intent
  - import android.graphics.Rect
  - import android.os.Bundle
  - import android.util.Log
  - import android.view.accessibility.AccessibilityEvent
  - import com.augmentalis.voiceos.cursor.core.CursorOffset
  - import java.util.concurrent.ConcurrentLinkedQueue
  - import java.util.concurrent.atomic.AtomicBoolean
  - ... and 1 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/accessibility/mocks/MockVoiceRecognitionManager.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.mocks`

**Classes/Interfaces/Objects:**
  - class MockVoiceRecognitionManager(private val actionCoordinator

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.accessibility.managers.ActionCoordinator

---

### File: `src/test/java/com/augmentalis/voiceoscore/accessibility/overlays/ConfidenceOverlayTest.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.overlays`

**Classes/Interfaces/Objects:**
  - class ConfidenceOverlayTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.view.WindowManager
  - import com.augmentalis.voiceos.speech.confidence.ConfidenceLevel
  - import com.augmentalis.voiceos.speech.confidence.ConfidenceResult
  - import com.augmentalis.voiceos.speech.confidence.ScoringMethod
  - import io.mockk.every
  - import io.mockk.mockk
  - import org.junit.Assert.*
  - import org.junit.Before
  - import org.junit.Test

---

### File: `src/test/java/com/augmentalis/voiceoscore/accessibility/overlays/OverlayManagerTest.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.overlays`

**Classes/Interfaces/Objects:**
  - class OverlayManagerTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.Point
  - import android.graphics.Rect
  - import android.view.WindowManager
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.Check
  - import com.augmentalis.voiceos.speech.confidence.Alternate
  - import com.augmentalis.voiceos.speech.confidence.ConfidenceLevel
  - import com.augmentalis.voiceos.speech.confidence.ConfidenceResult
  - import com.augmentalis.voiceos.speech.confidence.ScoringMethod
  - ... and 7 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/accessibility/test/CommandExecutionVerifier.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.test`

**Classes/Interfaces/Objects:**
  - class MockAccessibilityNodeInfo private constructor() 
  - class HandlerInvocationTracker 
  - class ParameterVerifier 
  - class ExecutionResultVerifier 
  - class MockActionHandler(
  - class CommandExecutionVerifier 

**Public Functions:**

**Imports:**
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.voiceoscore.accessibility.handlers.ActionCategory
  - import com.augmentalis.voiceoscore.accessibility.handlers.ActionHandler
  - import java.util.concurrent.ConcurrentHashMap
  - import java.util.concurrent.atomic.AtomicInteger
  - import java.util.concurrent.atomic.AtomicLong

---

### File: `src/test/java/com/augmentalis/voiceoscore/accessibility/test/EndToEndVoiceTest.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.test`

**Classes/Interfaces/Objects:**
  - class MockVoiceRecognitionService 
  - class EndToEndVoiceTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.os.Handler
  - import android.os.Looper
  - import android.util.Log
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService
  - import com.augmentalis.voiceoscore.accessibility.handlers.ActionCategory
  - import com.augmentalis.voiceoscore.accessibility.managers.ActionCoordinator
  - import com.augmentalis.voiceoscore.accessibility.mocks.MockVoiceRecognitionManager
  - import io.mockk.mockk
  - import kotlinx.coroutines.*
  - ... and 5 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/accessibility/test/PerformanceTest.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.test`

**Classes/Interfaces/Objects:**
  - class PerformanceMetrics 
  - class BindingLatencyTest 
  - class CommandProcessingTest 
  - class MemoryUsageTest 
  - class ResourceCleanupTest 
  - class PerformanceTestController 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.os.Debug
  - import android.util.Log
  - import com.augmentalis.voiceoscore.accessibility.VoiceOSService
  - import com.augmentalis.voiceoscore.accessibility.handlers.ActionCategory
  - import com.augmentalis.voiceoscore.accessibility.managers.ActionCoordinator
  - import com.augmentalis.voiceoscore.accessibility.mocks.MockVoiceRecognitionManager
  - import io.mockk.mockk
  - import kotlinx.coroutines.*
  - import java.util.concurrent.ConcurrentHashMap
  - ... and 6 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/accessibility/test/TestUtils.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.test`

**Classes/Interfaces/Objects:**
  - object TestUtils 

**Public Functions:**

**Imports:**
  - import android.content.ComponentName
  - import android.content.Context
  - import android.content.Intent
  - import android.content.ServiceConnection
  - import android.os.Bundle
  - import android.os.IBinder
  - import android.os.RemoteException
  - import android.util.Log
  - import androidx.test.platform.app.InstrumentationRegistry
  - import com.augmentalis.voicerecognition.IRecognitionCallback
  - ... and 13 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/accessibility/test/VoiceCommandTestScenarios.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.test`

**Classes/Interfaces/Objects:**
  - object VoiceCommandTestScenarios 

**Public Functions:**

**Imports:**
  - import com.augmentalis.voiceoscore.accessibility.handlers.ActionCategory

---

### File: `src/test/java/com/augmentalis/voiceoscore/accessibility/tree/AccessibilityTreeProcessorTest.kt`

**Package:** `com.augmentalis.voiceoscore.accessibility.tree`

**Classes/Interfaces/Objects:**
  - class AccessibilityTreeProcessorTest 

**Public Functions:**

**Imports:**
  - import android.graphics.Rect
  - import android.os.Bundle
  - import android.view.accessibility.AccessibilityNodeInfo
  - import io.mockk.*
  - import kotlinx.coroutines.ExperimentalCoroutinesApi
  - import kotlinx.coroutines.test.*
  - import org.junit.After
  - import org.junit.Before
  - import org.junit.Test
  - import kotlin.test.*

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/di/TestVoiceOSServiceDirector.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.di`

**Classes/Interfaces/Objects:**
  - object TestVoiceOSServiceDirector 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.*
  - import com.augmentalis.voiceoscore.refactoring.mocks.*
  - import dagger.Module
  - import dagger.Provides
  - import dagger.hilt.components.SingletonComponent
  - import dagger.hilt.testing.TestInstallIn
  - import javax.inject.Singleton

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/impl/CommandOrchestratorImplTest.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class CommandOrchestratorImplTest 

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import com.augmentalis.commandmanager.CommandManager
  - import com.augmentalis.commandmanager.models.Command
  - import com.augmentalis.commandmanager.models.CommandContext
  - import com.augmentalis.commandmanager.models.CommandSource
  - import com.augmentalis.voiceoscore.accessibility.managers.ActionCoordinator
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ICommandOrchestrator.*
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ISpeechManager
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager
  - ... and 16 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/impl/DatabaseManagerImplTest.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class DatabaseManagerImplTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.commandmanager.database.CommandDatabase
  - import com.augmentalis.commandmanager.database.VoiceCommandDao
  - import com.augmentalis.commandmanager.database.VoiceCommandEntity
  - import com.augmentalis.voiceoscore.learnweb.WebScrapingDatabase
  - import com.augmentalis.voiceoscore.learnweb.GeneratedWebCommandDao
  - import com.augmentalis.voiceoscore.learnweb.ScrapedWebsiteDao
  - import com.augmentalis.voiceoscore.learnweb.GeneratedWebCommand as WebCommandEntity
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IDatabaseManager
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IDatabaseManager.*
  - ... and 25 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/impl/EventRouterImplTest.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class EventRouterImplTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.view.accessibility.AccessibilityEvent
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IEventRouter
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService
  - import io.mockk.*
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.first
  - import kotlinx.coroutines.flow.take
  - import kotlinx.coroutines.flow.toList
  - ... and 10 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/impl/ServiceMonitorImplTest.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class ServiceMonitorImplTest 

**Public Functions:**

**Imports:**
  - import android.app.ActivityManager
  - import android.content.Context
  - import android.os.BatteryManager
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.*
  - import io.mockk.*
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.first
  - import kotlinx.coroutines.flow.take
  - import kotlinx.coroutines.flow.toList
  - import kotlinx.coroutines.test.*
  - ... and 7 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/impl/SpeechManagerImplTest.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class SpeechManagerImplTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ISpeechManager.*
  - import com.augmentalis.voiceos.speech.engines.vivoka.VivokaEngine
  - import com.augmentalis.voiceos.speech.engines.vosk.VoskEngine
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import io.mockk.*
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.first
  - import kotlinx.coroutines.flow.take
  - import kotlinx.coroutines.flow.toList
  - ... and 8 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/impl/StateManagerImplTest.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class StateManagerImplTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.test.core.app.ApplicationProvider
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager.*
  - import kotlinx.coroutines.ExperimentalCoroutinesApi
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.flow.first
  - import kotlinx.coroutines.launch
  - import kotlinx.coroutines.test.runTest
  - import org.junit.After
  - import org.junit.Assert.*
  - ... and 9 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/impl/UIScrapingServiceImplTest.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.impl`

**Classes/Interfaces/Objects:**
  - class UIScrapingServiceImplTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.Rect
  - import android.view.accessibility.AccessibilityEvent
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IDatabaseManager
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService.*
  - import io.mockk.*
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.first
  - ... and 9 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/integration/DIPerformanceTest.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.integration`

**Classes/Interfaces/Objects:**
  - class DIPerformanceTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.*
  - import com.augmentalis.voiceoscore.refactoring.mocks.*
  - import com.augmentalis.voiceoscore.refactoring.utils.RefactoringTestAssertions
  - import com.augmentalis.voiceoscore.refactoring.utils.RefactoringTestUtils
  - import io.mockk.mockk
  - import kotlinx.coroutines.runBlocking
  - import org.junit.Before
  - import org.junit.Test
  - import kotlin.system.measureTimeMillis
  - ... and 1 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/integration/HiltDITest.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.integration`

**Classes/Interfaces/Objects:**
  - class HiltDITest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.test.core.app.ApplicationProvider
  - import com.augmentalis.voiceoscore.refactoring.interfaces.*
  - import com.augmentalis.voiceoscore.refactoring.utils.RefactoringTestAssertions
  - import com.augmentalis.voiceoscore.refactoring.utils.RefactoringTestUtils
  - import dagger.hilt.android.testing.HiltAndroidRule
  - import dagger.hilt.android.testing.HiltAndroidTest
  - import kotlinx.coroutines.runBlocking
  - import org.junit.Before
  - import org.junit.Rule
  - ... and 4 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/integration/MockImplementationsTest.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.integration`

**Classes/Interfaces/Objects:**
  - class MockImplementationsTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.*
  - import com.augmentalis.voiceoscore.refactoring.mocks.*
  - import com.augmentalis.voiceoscore.refactoring.utils.RefactoringTestAssertions
  - import com.augmentalis.voiceoscore.refactoring.utils.RefactoringTestFixtures
  - import com.augmentalis.voiceoscore.refactoring.utils.RefactoringTestUtils
  - import io.mockk.mockk
  - import kotlinx.coroutines.runBlocking
  - import org.junit.Before
  - import org.junit.Test
  - ... and 3 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/mocks/MockCommandOrchestrator.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.mocks`

**Classes/Interfaces/Objects:**
  - class MockCommandOrchestrator(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.commandmanager.models.Command
  - import com.augmentalis.commandmanager.models.CommandContext
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ICommandOrchestrator
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ICommandOrchestrator.*
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import java.util.concurrent.ConcurrentHashMap
  - ... and 3 more

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/mocks/MockDatabaseManager.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.mocks`

**Classes/Interfaces/Objects:**
  - class MockDatabaseManager(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IDatabaseManager
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IDatabaseManager.*
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import java.util.concurrent.atomic.AtomicBoolean

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/mocks/MockEventRouter.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.mocks`

**Classes/Interfaces/Objects:**
  - class MockEventRouter(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.view.accessibility.AccessibilityEvent
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IEventRouter
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IEventRouter.*
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import java.util.concurrent.atomic.AtomicBoolean
  - import java.util.concurrent.atomic.AtomicLong

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/mocks/MockInstalledAppsManager.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.mocks`

**Classes/Interfaces/Objects:**
  - object MockInstalledAppsManagerFactory 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.accessibility.managers.InstalledAppsManager
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/mocks/MockServiceMonitor.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.mocks`

**Classes/Interfaces/Objects:**
  - class MockServiceMonitor(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IServiceMonitor.*
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import java.util.concurrent.atomic.AtomicBoolean

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/mocks/MockSpeechManager.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.mocks`

**Classes/Interfaces/Objects:**
  - class MockSpeechManager(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ISpeechManager
  - import com.augmentalis.voiceoscore.refactoring.interfaces.ISpeechManager.*
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import java.util.concurrent.atomic.AtomicBoolean
  - import java.util.concurrent.atomic.AtomicInteger

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/mocks/MockStateManager.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.mocks`

**Classes/Interfaces/Objects:**
  - class MockStateManager(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IStateManager.*
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import java.util.concurrent.atomic.AtomicBoolean

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/mocks/MockUIScrapingService.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.mocks`

**Classes/Interfaces/Objects:**
  - class MockUIScrapingService(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.view.accessibility.AccessibilityEvent
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService
  - import com.augmentalis.voiceoscore.refactoring.interfaces.IUIScrapingService.*
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.MutableSharedFlow
  - import kotlinx.coroutines.flow.asSharedFlow
  - import java.util.concurrent.atomic.AtomicBoolean
  - import java.util.concurrent.atomic.AtomicInteger

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/utils/RefactoringTestAssertions.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.utils`

**Classes/Interfaces/Objects:**
  - object RefactoringTestAssertions 

**Public Functions:**

**Imports:**
  - import com.augmentalis.voiceoscore.refactoring.interfaces.*
  - import com.augmentalis.voiceoscore.refactoring.mocks.*
  - import org.junit.Assert.*

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/utils/RefactoringTestFixtures.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.utils`

**Classes/Interfaces/Objects:**
  - object RefactoringTestFixtures 

**Public Functions:**

**Imports:**
  - import com.augmentalis.voiceoscore.refactoring.interfaces.*

---

### File: `src/test/java/com/augmentalis/voiceoscore/refactoring/utils/RefactoringTestUtils.kt`

**Package:** `com.augmentalis.voiceoscore.refactoring.utils`

**Classes/Interfaces/Objects:**
  - object RefactoringTestUtils 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.view.accessibility.AccessibilityEvent
  - import android.view.accessibility.AccessibilityNodeInfo
  - import com.augmentalis.commandmanager.models.CommandContext
  - import com.augmentalis.voiceoscore.refactoring.interfaces.*
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Job
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.first
  - ... and 6 more

---

## Summary

**Total Files:** 260

**Module Structure:**
```
                  build
                    generated
                      aidl_source_output_dir
                        debug
                          out
                            com
                              augmentalis
                                voicerecognition
                      ap_generated_sources
                        debug
                          out
                      data_binding_base_class_source_out
                        debug
                          out
                      ksp
                        debug
                          java
                            com
                              augmentalis
                                voiceoscore
                                  accessibility
                                    di
                                  learnweb
                                  refactoring
                                    di
                                    impl
                                  scraping
                                    dao
                                    database
                            hilt_aggregated_deps
                          kotlin
                      res
                        pngs
                          debug
                        resValues
                          debug
                      source
                        buildConfig
                          debug
                            com
                              augmentalis
                                voiceoscore
                    intermediates
                      aapt_friendly_merged_manifests
                        debug
                          processDebugManifest
                            aapt
                      aar_metadata
                        debug
                          writeDebugAarMetadata
```

---

**Generated by:** VOS4 API Documentation Generator
**Timestamp:** 251019-2203
