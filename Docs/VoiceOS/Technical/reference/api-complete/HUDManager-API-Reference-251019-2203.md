# HUDManager - Complete API Reference

**Module Type:** managers
**Generated:** 2025-10-19 22:04:05 PDT
**Timestamp:** 251019-2203
**Location:** `modules/managers/HUDManager`

---

## Overview

This document provides a complete file-by-file, class-by-class reference for the HUDManager module.

## Files


### File: `src/main/java/com/augmentalis/hudmanager/accessibility/Enhancer.kt`

**Package:** `com.augmentalis.hudmanager.accessibility`

**Classes/Interfaces/Objects:**
  - class Enhancer(
  - enum class AccessibilityMode 
  - data class AccessibilityColorScheme(
  - data class AccessibilityState(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.compose.runtime.mutableStateOf
  - import androidx.compose.runtime.State
  - import androidx.compose.ui.graphics.Color
  - import com.augmentalis.hudmanager.stubs.VOSAccessibilitySvc
  - import com.augmentalis.hudmanager.HUDMode
  - import com.augmentalis.hudmanager.models.VoiceCommand
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.*

---

### File: `src/main/java/com/augmentalis/hudmanager/api/HUDIntent.kt`

**Package:** `com.augmentalis.hudmanager.api`

**Classes/Interfaces/Objects:**
  - object HUDIntent 
  - object HUDPosition 
  - object HUDPriority 
  - object HUDModes 
  - object AccessibilityModes 
  - object DataVisualizationTypes 

**Public Functions:**

**Imports:**
  - import android.content.Intent
  - import android.os.Bundle

---

### File: `src/main/java/com/augmentalis/hudmanager/core/ContextManager.kt`

**Package:** `com.augmentalis.hudmanager.core`

**Classes/Interfaces/Objects:**
  - class ContextManager(
  - data class ContextSnapshot(
  - data class EnvironmentPattern(
  - enum class UserActivity 
  - enum class TimeCategory 
  - enum class Environment 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.hardware.Sensor
  - import android.hardware.SensorEvent
  - import android.hardware.SensorEventListener
  - import android.hardware.SensorManager
  - import android.location.Location
  - import android.location.LocationListener
  - import android.location.LocationManager
  - import androidx.compose.runtime.mutableStateOf
  - import androidx.compose.runtime.State
  - ... and 6 more

---

### File: `src/main/java/com/augmentalis/hudmanager/HUDManager.kt`

**Package:** `com.augmentalis.hudmanager`

**Classes/Interfaces/Objects:**
  - class HUDManager constructor(
  - enum class HUDMode 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.Intent
  - import androidx.compose.runtime.mutableStateOf
  - import androidx.compose.runtime.State
  - import androidx.lifecycle.LiveData
  - import androidx.lifecycle.MutableLiveData
  - import com.augmentalis.hudmanager.core.*
  - import com.augmentalis.hudmanager.core.Environment
  - import com.augmentalis.hudmanager.spatial.*
  - import com.augmentalis.hudmanager.models.*
  - ... and 13 more

---

### File: `src/main/java/com/augmentalis/hudmanager/models/HUDModels.kt`

**Package:** `com.augmentalis.hudmanager.models`

**Classes/Interfaces/Objects:**
  - data class HUDState(
  - data class RenderingStats(
  - data class SpatialData(
  - data class Vector3D(
  - data class HUDElement(
  - enum class HUDElementType 
  - data class ElementBounds(
  - data class HUDConfig(
  - data class CalibrationPoint(
  - data class ElementCollision(
  - enum class CollisionSeverity 
  - data class VoiceCommand(
  - enum class UIContext 
  - data class GazeTarget(

**Public Functions:**

**Imports:**
  - import androidx.compose.ui.graphics.Color
  - import com.augmentalis.hudmanager.spatial.SpatialPosition
  - import com.augmentalis.hudmanager.spatial.NotificationPriority
  - import com.augmentalis.hudmanager.HUDMode

---

### File: `src/main/java/com/augmentalis/hudmanager/provider/HUDContentProvider.kt`

**Package:** `com.augmentalis.hudmanager.provider`

**Classes/Interfaces/Objects:**
  - class HUDContentProvider 

**Public Functions:**

**Imports:**
  - import android.content.ContentProvider
  - import android.content.ContentValues
  - import android.content.UriMatcher
  - import android.database.Cursor
  - import android.database.MatrixCursor
  - import android.net.Uri
  - import android.os.Bundle
  - import android.util.Log
  - import com.augmentalis.hudmanager.stubs.HUDRenderer
  - import com.augmentalis.hudmanager.stubs.HUDSystem
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/hudmanager/rendering/HUDRenderer.kt`

**Package:** `com.augmentalis.hudmanager.rendering`

**Classes/Interfaces/Objects:**
  - class HUDRenderer(
  - enum class RenderMode 
  - enum class HUDElementType 
  - data class HUDElement(
  - data class VoiceCommandData(
  - data class ConfidenceData(
  - data class RenderCommand(
  - enum class RenderCommandType 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.*
  - import android.view.SurfaceHolder
  - import android.view.SurfaceView
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.platform.ComposeView
  - import com.augmentalis.hudmanager.ui.*
  - import com.augmentalis.hudmanager.spatial.*
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.*
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/hudmanager/settings/HUDSettings.kt`

**Package:** `com.augmentalis.hudmanager.settings`

**Classes/Interfaces/Objects:**
  - data class HUDSettings(
  - enum class HUDDisplayMode 
  - data class DisplayElements(
  - data class PositioningSettings(
  - data class VisualSettings(
  - data class PrivacySettings(
  - data class PerformanceSettings(
  - data class AccessibilitySettings(
  - enum class TextSize 
  - enum class IconSize 
  - enum class LayoutStyle 
  - enum class AnchorPoint 
  - enum class ColorTheme 
  - enum class ShadowQuality 
  - enum class TextureQuality 
  - enum class ColorBlindMode 

**Public Functions:**

**Imports:**
  - import kotlinx.serialization.Serializable

---

### File: `src/main/java/com/augmentalis/hudmanager/settings/HUDSettingsManager.kt`

**Package:** `com.augmentalis.hudmanager.settings`

**Classes/Interfaces/Objects:**
  - class HUDSettingsManager private constructor(private val context
  - enum class DisplayElement 
  - enum class HUDPreset 
  - enum class PerformanceMode 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow
  - import kotlinx.serialization.encodeToString
  - import kotlinx.serialization.decodeFromString
  - import kotlinx.serialization.json.Json

---

### File: `src/main/java/com/augmentalis/hudmanager/spatial/GazeTarget.kt`

**Package:** `com.augmentalis.hudmanager.spatial`

**Classes/Interfaces/Objects:**
  - data class GazeTarget(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/hudmanager/spatial/GazeTracker.kt`

**Package:** `com.augmentalis.hudmanager.spatial`

**Classes/Interfaces/Objects:**
  - class GazeTracker(
  - data class UIElement(
  - data class ElementBounds(
  - data class Point2D(
  - enum class UIElementType 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.camera.core.*
  - import androidx.camera.core.resolutionselector.ResolutionSelector
  - import androidx.camera.core.resolutionselector.ResolutionStrategy
  - import androidx.camera.lifecycle.ProcessCameraProvider
  - import androidx.compose.runtime.mutableStateOf
  - import androidx.compose.runtime.State
  - import com.augmentalis.hudmanager.models.GazeTarget
  - import com.augmentalis.hudmanager.models.Vector3D
  - import com.google.mlkit.vision.common.InputImage
  - ... and 8 more

---

### File: `src/main/java/com/augmentalis/hudmanager/spatial/SpatialRenderer.kt`

**Package:** `com.augmentalis.hudmanager.spatial`

**Classes/Interfaces/Objects:**
  - class SpatialRenderer(
  - data class SpatialElement(
  - data class RenderableElement(
  - data class ScreenPosition(
  - data class HeadOrientation(
  - data class ARVisionStyle(
  - enum class SpatialElementType 
  - enum class RenderLayer 
  - data class HUDNotification(
  - data class ControlPanelData(
  - data class HUDAction(
  - enum class NotificationPriority 
  - enum class SpatialAnchor 
  - enum class PanelStyle 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.graphics.*
  - import com.augmentalis.hudmanager.ui.*
  - import com.augmentalis.hudmanager.rendering.*
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.*
  - import kotlin.math.*

---

### File: `src/main/java/com/augmentalis/hudmanager/spatial/VoiceIndicatorSystem.kt`

**Package:** `com.augmentalis.hudmanager.spatial`

**Classes/Interfaces/Objects:**
  - class VoiceIndicatorSystem(
  - data class SpatialVoiceCommand(
  - data class SpatialPosition(
  - data class CommandStyle(
  - enum class CommandSize 
  - enum class AnimationType 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.compose.runtime.mutableStateOf
  - import androidx.compose.runtime.State
  - import androidx.compose.ui.graphics.Color
  - import com.augmentalis.hudmanager.models.VoiceCommand
  - import com.augmentalis.hudmanager.models.UIContext
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.*

---

### File: `src/main/java/com/augmentalis/hudmanager/stubs/VoiceUIStubs.kt`

**Package:** `com.augmentalis.hudmanager.stubs`

**Classes/Interfaces/Objects:**
  - class DatabaseModule 
  - object VOSAccessibilitySvc 
  - object HUDRenderer 
  - object HUDSystem 
  - class HUDIntent 
  - enum class HUDMode 
  - enum class RenderMode 
  - data class OrientationData(
  - object voiceui 
  - object voiceos 
  - object voiceaccessibility 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/hudmanager/ui/ARVisionTheme.kt`

**Package:** `com.augmentalis.hudmanager.ui`

**Classes/Interfaces/Objects:**
  - object ARVisionColors 
  - object ARVisionTypography 
  - object LiquidAnimations 
  - object ARVisionShapes 

**Public Functions:**
  - fun glassMorphismBrush(
  - fun Modifier.glassPanel(
  - fun Modifier.liquidButton(
  - fun Modifier.vibrancy(
  - fun FloatingHUDElement(
  - fun ConfidenceIndicator(
  - fun VoiceCommandBadge(

**Imports:**
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.ui.graphics.Shape
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.draw.blur
  - import androidx.compose.ui.draw.clip
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/hudmanager/ui/HUDSettingsUI.kt`

**Package:** `com.augmentalis.hudmanager.ui`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun HUDSettingsScreen(
  - fun MasterToggle(
  - fun DisplayModeSelector(
  - fun PresetSelector(
  - fun PresetButton(
  - fun DisplayElementsSection(
  - fun VisualSettingsSection(
  - fun ThemeButton(
  - fun PositioningSection(
  - fun PrivacySettingsSection(
  - fun PrivacyToggle(
  - fun PerformanceModeSelector(
  - fun PerformanceModeButton(
  - fun PerformanceSettingsSection(
  - fun SectionHeader(text: String) 

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.LazyColumn
  - import androidx.compose.foundation.lazy.items
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - import androidx.compose.material.icons.automirrored.filled.*
  - import androidx.compose.material3.*
  - ... and 13 more

---

### File: `src/test/java/com/augmentalis/hudmanager/HUDManagerTest.kt`

**Package:** `com.augmentalis.hudmanager`

**Classes/Interfaces/Objects:**
  - class HUDManagerTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.arch.core.executor.testing.InstantTaskExecutorRule
  - import androidx.lifecycle.Observer
  - import com.augmentalis.hudmanager.models.*
  - import com.augmentalis.hudmanager.spatial.SpatialPosition
  - import com.augmentalis.hudmanager.models.UIContext
  - import com.augmentalis.hudmanager.models.VoiceCommand
  - import kotlinx.coroutines.ExperimentalCoroutinesApi
  - import kotlinx.coroutines.test.*
  - import kotlinx.coroutines.Dispatchers
  - ... and 11 more

---

## Summary

**Total Files:** 17

**Module Structure:**
```
                  build
                    generated
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
                      java_res
                        debug
                          processDebugJavaRes
                            out
                              com
                                augmentalis
                                  hudmanager
```

---

**Generated by:** VOS4 API Documentation Generator
**Timestamp:** 251019-2203
