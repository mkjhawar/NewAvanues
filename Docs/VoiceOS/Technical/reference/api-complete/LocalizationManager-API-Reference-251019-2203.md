# LocalizationManager - Complete API Reference

**Module Type:** managers
**Generated:** 2025-10-19 22:04:06 PDT
**Timestamp:** 251019-2203
**Location:** `modules/managers/LocalizationManager`

---

## Overview

This document provides a complete file-by-file, class-by-class reference for the LocalizationManager module.

## Files


### File: `build/generated/ksp/debug/kotlin/com/augmentalis/localizationmanager/data/LocalizationDatabase_Impl.kt`

**Package:** `com.augmentalis.localizationmanager.`data``

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import androidx.room.DatabaseConfiguration
  - import androidx.room.InvalidationTracker
  - import androidx.room.RoomDatabase
  - import androidx.room.RoomOpenHelper
  - import androidx.room.migration.AutoMigrationSpec
  - import androidx.room.migration.Migration
  - import androidx.room.util.TableInfo
  - import androidx.room.util.TableInfo.Companion.read
  - import androidx.room.util.dropFtsSyncTriggers
  - import androidx.sqlite.db.SupportSQLiteDatabase
  - ... and 14 more

---

### File: `build/generated/ksp/debug/kotlin/com/augmentalis/localizationmanager/data/PreferencesDao_Impl.kt`

**Package:** `com.augmentalis.localizationmanager.`data``

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.database.Cursor
  - import android.os.CancellationSignal
  - import androidx.room.CoroutinesRoom
  - import androidx.room.CoroutinesRoom.Companion.execute
  - import androidx.room.EntityInsertionAdapter
  - import androidx.room.RoomDatabase
  - import androidx.room.RoomSQLiteQuery
  - import androidx.room.RoomSQLiteQuery.Companion.acquire
  - import androidx.room.SharedSQLiteStatement
  - import androidx.room.util.createCancellationSignal
  - ... and 16 more

---

### File: `src/androidTest/java/com/augmentalis/localizationmanager/ui/LocalizationManagerActivityTest.kt`

**Package:** `com.augmentalis.localizationmanager.ui`

**Classes/Interfaces/Objects:**
  - class LocalizationManagerActivityTest 

**Public Functions:**

**Imports:**
  - import androidx.compose.ui.test.*
  - import androidx.compose.ui.test.junit4.createComposeRule
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import org.junit.Before
  - import org.junit.Rule
  - import org.junit.Test
  - import org.junit.runner.RunWith

---

### File: `src/main/java/com/augmentalis/localizationmanager/data/LocalizationDatabase.kt`

**Package:** `com.augmentalis.localizationmanager.data`

**Classes/Interfaces/Objects:**
  - abstract class LocalizationDatabase 

**Public Functions:**

**Imports:**
  - import androidx.room.Database
  - import androidx.room.Room
  - import androidx.room.RoomDatabase
  - import android.content.Context

---

### File: `src/main/java/com/augmentalis/localizationmanager/data/PreferencesDao.kt`

**Package:** `com.augmentalis.localizationmanager.data`

**Classes/Interfaces/Objects:**
  - interface PreferencesDao 

**Public Functions:**

**Imports:**
  - import androidx.room.*
  - import kotlinx.coroutines.flow.Flow

---

### File: `src/main/java/com/augmentalis/localizationmanager/data/UserPreference.kt`

**Package:** `com.augmentalis.localizationmanager.data`

**Classes/Interfaces/Objects:**
  - data class UserPreference(
  - object PreferenceKeys 
  - object PreferenceDefaults 
  - enum class DebounceDuration(val displayName
  - enum class DetailLevel(val displayName

**Public Functions:**

**Imports:**
  - import androidx.room.Entity
  - import androidx.room.PrimaryKey

---

### File: `src/main/java/com/augmentalis/localizationmanager/LocalizationModule.kt`

**Package:** `com.augmentalis.localizationmanager`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.*
  - import java.util.*

---

### File: `src/main/java/com/augmentalis/localizationmanager/repository/PreferencesRepository.kt`

**Package:** `com.augmentalis.localizationmanager.repository`

**Classes/Interfaces/Objects:**
  - class PreferencesRepository(

**Public Functions:**

**Imports:**
  - import com.augmentalis.localizationmanager.data.*
  - import kotlinx.coroutines.flow.Flow
  - import kotlinx.coroutines.flow.map

---

### File: `src/main/java/com/augmentalis/localizationmanager/ui/components/AnimatedLanguageDisplay.kt`

**Package:** `com.augmentalis.localizationmanager.ui.components`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun AnimatedCurrentLanguage(
  - fun AnimatedCurrentLanguageWithHighlight(
  - fun AnimatedLanguageChip(
  - fun LanguageTransitionIndicator(

**Imports:**
  - import androidx.compose.animation.*
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.text.font.FontWeight
  - import androidx.compose.ui.graphics.graphicsLayer
  - import androidx.compose.ui.unit.TextUnit
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/localizationmanager/ui/components/MessageHandler.kt`

**Package:** `com.augmentalis.localizationmanager.ui.components`

**Classes/Interfaces/Objects:**
  - enum class MessageType(

**Public Functions:**
  - fun MessageHandler(
  - fun ErrorMessage(
  - fun SuccessMessage(
  - fun WarningMessage(
  - fun InfoMessage(

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.CheckCircle
  - import androidx.compose.material.icons.filled.Error
  - import androidx.compose.material.icons.filled.Info
  - import androidx.compose.material.icons.filled.Warning
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - ... and 8 more

---

### File: `src/main/java/com/augmentalis/localizationmanager/ui/components/SettingsDialog.kt`

**Package:** `com.augmentalis.localizationmanager.ui.components`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun SettingsDialog(
  - fun DebounceDurationSelector(
  - fun SettingsSection(
  - fun SettingToggle(

**Imports:**
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.rememberScrollState
  - import androidx.compose.foundation.selection.selectable
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.foundation.verticalScroll
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Alignment
  - ... and 10 more

---

### File: `src/main/java/com/augmentalis/localizationmanager/ui/GlassmorphismUtils.kt`

**Package:** `com.augmentalis.localizationmanager.ui`

**Classes/Interfaces/Objects:**
  - data class GlassMorphismConfig(
  - object LocalizationColors 
  - object LocalizationGlassConfigs 

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

### File: `src/main/java/com/augmentalis/localizationmanager/ui/LocalizationManagerActivity.kt`

**Package:** `com.augmentalis.localizationmanager.ui`

**Classes/Interfaces/Objects:**
  - class LocalizationManagerActivity 

**Public Functions:**
  - fun LocalizationManagerContent(viewModel: LocalizationViewModel) 
  - fun HeaderSection(
  - fun CurrentLanguageCard(
  - fun LanguageStatisticsCard(
  - fun QuickActionsCard(
  - fun DownloadedLanguagesCard(
  - fun AvailableLanguagesSection(
  - fun RecentTranslationsCard(
  - fun TranslationCapabilitiesCard(
  - fun StatItem(
  - fun ActionButton(
  - fun DownloadedLanguageChip(
  - fun LanguageCard(
  - fun TranslationItem(
  - fun DownloadProgressOverlay(
  - fun LanguageSelectorDialog(
  - fun TranslationDialog(
  - fun LanguageSelectionItem(
  - fun ErrorDisplay(
  - fun SuccessDisplay(
  - fun getLanguageDisplayName(code: String): String 
  - fun getRegionColor(region: String): Color 
  - fun formatBytes(bytes: Long): String 
  - fun formatTime(timestamp: Long): String 
  - fun formatTimestamp(timestamp: Long): String 
  - fun StatisticsDetailDialog(
  - fun DetailStatCard(

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
  - ... and 33 more

---

### File: `src/main/java/com/augmentalis/localizationmanager/ui/LocalizationViewModel.kt`

**Package:** `com.augmentalis.localizationmanager.ui`

**Classes/Interfaces/Objects:**
  - data class LanguageInfo(
  - data class TranslationPair(
  - data class LanguageStatistics(
  - data class DownloadProgress(
  - enum class DownloadStatus 
  - data class TranslationRequest(
  - data class TranslationResult(
  - data class LocalizationUiState(
  - class LocalizationViewModelFactory(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import androidx.lifecycle.LiveData
  - import androidx.lifecycle.MutableLiveData
  - import androidx.lifecycle.ViewModel
  - import androidx.lifecycle.ViewModelProvider
  - import androidx.lifecycle.viewModelScope
  - import com.augmentalis.localizationmanager.LocalizationModule
  - import com.augmentalis.localizationmanager.repository.PreferencesRepository
  - import com.augmentalis.localizationmanager.data.DebounceDuration
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/localizationmanager/ui/TestableComposables.kt`

**Package:** `com.augmentalis.localizationmanager.ui`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun LocalizationManagerScreen() 

**Imports:**
  - import androidx.compose.runtime.Composable
  - import androidx.lifecycle.viewmodel.compose.viewModel
  - import android.content.Context
  - import androidx.compose.ui.platform.LocalContext
  - import com.augmentalis.localizationmanager.data.LocalizationDatabase
  - import com.augmentalis.localizationmanager.repository.PreferencesRepository

---

### File: `src/test/java/com/augmentalis/localizationmanager/ui/LocalizationViewModelTest.kt`

**Package:** `com.augmentalis.localizationmanager.ui`

**Classes/Interfaces/Objects:**
  - class LocalizationViewModelTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import androidx.arch.core.executor.testing.InstantTaskExecutorRule
  - import androidx.lifecycle.Observer
  - import com.augmentalis.localizationmanager.LocalizationModule
  - import com.augmentalis.localizationmanager.repository.PreferencesRepository
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.ExperimentalCoroutinesApi
  - import kotlinx.coroutines.test.*
  - import org.junit.After
  - ... and 12 more

---

## Summary

**Total Files:** 16

**Module Structure:**
```
                  build
                    generated
                      ksp
                        debug
                          kotlin
                            com
                              augmentalis
                                localizationmanager
                                  data
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
                            merged.dir
                            stripped.dir
                        mergeDebugJniLibFolders
                        mergeDebugShaders
                        packageDebugAssets
```

---

**Generated by:** VOS4 API Documentation Generator
**Timestamp:** 251019-2203
