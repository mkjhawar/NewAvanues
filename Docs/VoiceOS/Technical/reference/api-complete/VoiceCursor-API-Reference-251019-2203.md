# VoiceCursor - Complete API Reference

**Module Type:** apps
**Generated:** 2025-10-19 22:03:40 PDT
**Timestamp:** 251019-2203
**Location:** `modules/apps/VoiceCursor`

---

## Overview

This document provides a complete file-by-file, class-by-class reference for the VoiceCursor module.

## Files


### File: `src/main/java/com/augmentalis/voiceos/cursor/calibration/ClickAccuracyManager.kt`

**Package:** `com.augmentalis.voiceos.cursor.calibration`

**Classes/Interfaces/Objects:**
  - class ClickAccuracyManager(private val context
  - data class ClickTarget(
  - data class ClickTargetResult(
  - enum class TargetType 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import android.graphics.Rect
  - import android.view.View
  - import android.view.ViewGroup
  - import android.view.accessibility.AccessibilityNodeInfo
  - import androidx.compose.ui.geometry.Offset
  - import com.augmentalis.voiceos.cursor.view.ClickCalibrationData
  - import kotlin.math.*

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/commands/CursorCommandHandler.kt`

**Package:** `com.augmentalis.voiceos.cursor.commands`

**Classes/Interfaces/Objects:**
  - class CursorCommandHandler private constructor(
  - interface CommandRouter 
  - data class IntegrationStatus(
  - enum class CursorDirection 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.Intent
  - import android.util.Log
  - import com.augmentalis.voiceos.cursor.VoiceCursor
  - import com.augmentalis.voiceos.cursor.core.CursorConfig
  - import com.augmentalis.voiceos.cursor.core.CursorOffset
  - import com.augmentalis.voiceos.cursor.core.CursorType
  - import com.augmentalis.voiceos.cursor.VoiceCursorAPI
  - import com.augmentalis.voiceos.cursor.view.CursorAction
  - import kotlinx.coroutines.CoroutineScope
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/core/CursorAnimator.kt`

**Package:** `com.augmentalis.voiceos.cursor.core`

**Classes/Interfaces/Objects:**
  - enum class AnimationType 
  - data class AnimationConfig(
  - data class AnimationState(
  - class CursorAnimator 

**Public Functions:**

**Imports:**
  - import android.animation.Animator
  - import android.animation.AnimatorListenerAdapter
  - import android.animation.AnimatorSet
  - import android.animation.ObjectAnimator
  - import android.animation.ValueAnimator
  - import android.util.Log
  - import android.view.animation.AccelerateDecelerateInterpolator
  - import android.view.animation.AccelerateInterpolator
  - import android.view.animation.DecelerateInterpolator
  - import android.view.animation.LinearInterpolator
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/core/CursorPositionManager.kt`

**Package:** `com.augmentalis.voiceos.cursor.core`

**Classes/Interfaces/Objects:**
  - class MovingAverage(

**Public Functions:**

**Imports:**
  - import com.augmentalis.voiceos.cursor.filter.CursorFilter
  - import com.augmentalis.voiceos.cursor.view.EdgeDetectionResult
  - import com.augmentalis.voiceos.cursor.view.EdgeType
  - import androidx.compose.ui.geometry.Offset
  - import kotlin.math.abs
  - import kotlin.math.sqrt

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/core/CursorRenderer.kt`

**Package:** `com.augmentalis.voiceos.cursor.core`

**Classes/Interfaces/Objects:**
  - class CursorRenderer(private val context
  - class ResourceProvider(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.res.Resources
  - import android.graphics.*
  - import android.util.Log
  - import androidx.core.content.ContextCompat
  - import androidx.core.graphics.createBitmap
  - import androidx.core.graphics.drawable.DrawableCompat
  - import androidx.core.graphics.scale
  - import android.graphics.drawable.BitmapDrawable
  - import android.graphics.drawable.Drawable

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/core/CursorTypes.kt`

**Package:** `com.augmentalis.voiceos.cursor.core`

**Classes/Interfaces/Objects:**
  - sealed class CursorType 
  - data class CursorConfig(
  - enum class FilterStrength 
  - data class GazeConfig(
  - data class CursorState(

**Public Functions:**

**Imports:**
  - import android.os.Parcelable
  - import kotlinx.parcelize.Parcelize

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/core/GazeClickManager.kt`

**Package:** `com.augmentalis.voiceos.cursor.core`

**Classes/Interfaces/Objects:**
  - class GazeClickManager(

**Public Functions:**

**Imports:**
  - import kotlin.math.hypot
  - import kotlin.math.sqrt

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/core/GestureManager.kt`

**Package:** `com.augmentalis.voiceos.cursor.core`

**Classes/Interfaces/Objects:**
  - enum class GestureType 
  - data class GestureEvent(
  - data class GestureConfig(
  - data class GestureState(
  - class GestureManager(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import android.view.GestureDetector
  - import android.view.MotionEvent
  - import android.view.ScaleGestureDetector
  - import android.view.ViewConfiguration
  - import kotlin.math.*

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/core/PositionManager.kt`

**Package:** `com.augmentalis.voiceos.cursor.core`

**Classes/Interfaces/Objects:**
  - class PositionManager(

**Public Functions:**

**Imports:**
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/filter/CursorFilter.kt`

**Package:** `com.augmentalis.voiceos.cursor.filter`

**Classes/Interfaces/Objects:**
  - class CursorFilter 
  - data class CursorFilterConfig(

**Public Functions:**

**Imports:**
  - import kotlin.math.abs

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/help/VoiceCursorHelpMenu.kt`

**Package:** `com.augmentalis.voiceos.cursor.help`

**Classes/Interfaces/Objects:**
  - class VoiceCursorHelpMenu(private val context
  - data class CommandGroup(
  - data class VoiceCommand(
  - class HelpCommandAdapter(

**Public Functions:**

**Imports:**
  - import android.app.AlertDialog
  - import android.content.Context
  - import android.graphics.Color
  - import android.graphics.drawable.ColorDrawable
  - import android.view.Gravity
  - import android.view.LayoutInflater
  - import android.view.View
  - import android.view.WindowManager
  - import android.widget.LinearLayout
  - import android.widget.TextView
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/helper/CursorHelper.kt`

**Package:** `com.augmentalis.voiceos.cursor.helper`

**Classes/Interfaces/Objects:**
  - object CursorHelper 
  - class DragHelper 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.Bitmap
  - import android.graphics.Canvas
  - import android.graphics.drawable.VectorDrawable
  - import androidx.core.content.ContextCompat
  - import androidx.core.graphics.createBitmap
  - import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/helper/VoiceCursorIMUIntegration.kt`

**Package:** `com.augmentalis.voiceos.cursor.helper`

**Classes/Interfaces/Objects:**
  - class VoiceCursorIMUIntegration private constructor(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.devicemanager.sensors.imu.CursorAdapter
  - import com.augmentalis.devicemanager.sensors.imu.IMUManager
  - import com.augmentalis.voiceos.cursor.core.CursorOffset
  - import com.augmentalis.voiceos.cursor.core.PositionManager
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob
  - import kotlinx.coroutines.cancel
  - ... and 9 more

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/manager/CursorGestureHandler.kt`

**Package:** `com.augmentalis.voiceos.cursor.manager`

**Classes/Interfaces/Objects:**
  - class CursorGestureHandler(private val accessibilityService

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.accessibilityservice.GestureDescription
  - import android.graphics.Path
  - import android.os.Handler
  - import android.os.Looper
  - import android.util.Log
  - import com.augmentalis.voiceos.cursor.core.CursorOffset
  - import com.augmentalis.voiceos.cursor.view.CursorAction
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/manager/CursorOverlayManager.kt`

**Package:** `com.augmentalis.voiceos.cursor.manager`

**Classes/Interfaces/Objects:**
  - class CursorOverlayManager(private val context

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.content.SharedPreferences
  - import android.graphics.Color
  - import android.graphics.PixelFormat
  - import android.os.Build
  - import android.util.Log
  - import android.view.Gravity
  - import android.view.ViewGroup
  - import android.view.WindowManager
  - ... and 16 more

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/ui/ThemeUtils.kt`

**Package:** `com.augmentalis.voiceos.cursor.ui`

**Classes/Interfaces/Objects:**
  - data class GlassMorphismConfig(
  - enum class DepthLevel(val depth
  - object ARVisionColors 

**Public Functions:**
  - fun Modifier.glassMorphism(
  - fun Modifier.glassPanel(
  - fun Modifier.glassButton(

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.border
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.composed
  - import androidx.compose.ui.draw.blur
  - import androidx.compose.ui.draw.clip
  - import androidx.compose.ui.graphics.Brush
  - import androidx.compose.ui.graphics.Color
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/ui/VoiceCursorSettingsActivity.kt`

**Package:** `com.augmentalis.voiceos.cursor.ui`

**Classes/Interfaces/Objects:**
  - class VoiceCursorSettingsActivity 

**Public Functions:**
  - fun VoiceCursorSettingsScreen(
  - fun SettingsPanel(
  - fun SettingsSwitchItem(
  - fun PermissionItem(
  - fun SettingsDropdownItem(
  - fun SettingsSliderItem(
  - fun ColorPickerItem(
  - fun CursorFilterTestArea(

**Imports:**
  - import android.content.BroadcastReceiver
  - import android.content.Context
  - import android.content.Intent
  - import android.content.IntentFilter
  - import android.content.SharedPreferences
  - import android.net.Uri
  - import android.os.Build
  - import android.os.Bundle
  - import android.provider.Settings
  - import android.util.Log
  - ... and 60 more

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/ui/VoiceCursorViewModel.kt`

**Package:** `com.augmentalis.voiceos.cursor.ui`

**Classes/Interfaces/Objects:**
  - class VoiceCursorViewModel 
  - data class PermissionUiState(

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.provider.Settings
  - import androidx.lifecycle.ViewModel
  - import com.augmentalis.voiceos.cursor.VoiceCursorAPI
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.asStateFlow
  - import kotlinx.coroutines.flow.update
  - import kotlin.collections.any
  - import kotlin.jvm.java
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/view/CursorMenuView.kt`

**Package:** `com.augmentalis.voiceos.cursor.view`

**Classes/Interfaces/Objects:**
  - enum class CursorAction 
  - data class ClickCalibrationData(
  - data class EdgeDetectionResult(
  - enum class EdgeType 

**Public Functions:**
  - fun MenuView(

**Imports:**
  - import android.content.Context
  - import android.os.VibrationEffect
  - import android.os.Vibrator
  - import androidx.compose.animation.AnimatedVisibility
  - import androidx.compose.animation.core.*
  - import androidx.compose.animation.fadeIn
  - import androidx.compose.animation.fadeOut
  - import androidx.compose.animation.scaleIn
  - import androidx.compose.animation.scaleOut
  - import androidx.compose.foundation.background
  - ... and 24 more

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/view/CursorView.kt`

**Package:** `com.augmentalis.voiceos.cursor.view`

**Classes/Interfaces/Objects:**
  - class CursorView @JvmOverloads constructor(

**Public Functions:**

**Imports:**
  - import android.animation.ValueAnimator
  - import android.content.Context
  - import android.graphics.*
  - import android.os.Build
  - import android.os.VibrationEffect
  - import android.os.Vibrator
  - import android.os.VibratorManager
  - import android.util.AttributeSet
  - import android.util.Log
  - import android.view.MotionEvent
  - ... and 10 more

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/view/EdgeVisualFeedback.kt`

**Package:** `com.augmentalis.voiceos.cursor.view`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun EdgeVisualFeedback(
  - fun DrawScope.drawEdgeGlow(
  - fun DrawScope.drawVerticalEdgeGlow(
  - fun DrawScope.drawHorizontalEdgeGlow(
  - fun DrawScope.drawCornerGlow(
  - fun ClickTargetAssistance(
  - fun DrawScope.drawTargetHighlight(

**Imports:**
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.Canvas
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.geometry.Offset
  - import androidx.compose.ui.geometry.Size
  - import androidx.compose.ui.graphics.*
  - import androidx.compose.ui.graphics.drawscope.DrawScope
  - import androidx.compose.ui.graphics.drawscope.Stroke
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/view/FloatingHelpButton.kt`

**Package:** `com.augmentalis.voiceos.cursor.view`

**Classes/Interfaces/Objects:**
  - class FloatingHelpButton(
  - class FloatingHelpButtonManager(private val context

**Public Functions:**

**Imports:**
  - import android.annotation.SuppressLint
  - import android.content.Context
  - import android.graphics.Canvas
  - import android.graphics.Color
  - import android.graphics.Paint
  - import android.graphics.PixelFormat
  - import android.graphics.Typeface
  - import android.os.Build
  - import android.view.Gravity
  - import android.view.HapticFeedbackConstants
  - ... and 12 more

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/view/GazeClickTestUtils.kt`

**Package:** `com.augmentalis.voiceos.cursor.view`

**Classes/Interfaces/Objects:**
  - object GazeClickTestUtils 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.voiceos.cursor.core.CursorOffset
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.delay
  - import kotlinx.coroutines.launch

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/view/GazeClickView.kt`

**Package:** `com.augmentalis.voiceos.cursor.view`

**Classes/Interfaces/Objects:**
  - class GazeClickView(

**Public Functions:**

**Imports:**
  - import android.annotation.SuppressLint
  - import android.app.Service
  - import android.content.Context
  - import android.graphics.PixelFormat
  - import android.os.Build
  - import android.view.Gravity
  - import android.view.WindowManager
  - import android.widget.ImageView
  - import androidx.core.graphics.scale
  - import androidx.core.view.ViewCompat
  - ... and 8 more

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/VoiceCursor.kt`

**Package:** `com.augmentalis.voiceos.cursor`

**Classes/Interfaces/Objects:**
  - class VoiceCursor(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.Intent
  - import android.util.Log
  - import com.augmentalis.voiceos.cursor.core.*
  - import com.augmentalis.voiceos.cursor.helper.VoiceCursorIMUIntegration
  - import kotlinx.coroutines.*

---

### File: `src/main/java/com/augmentalis/voiceos/cursor/VoiceCursorAPI.kt`

**Package:** `com.augmentalis.voiceos.cursor`

**Classes/Interfaces/Objects:**
  - object VoiceCursorAPI 

**Public Functions:**

**Imports:**
  - import android.accessibilityservice.AccessibilityService
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.voiceos.cursor.core.CursorConfig
  - import com.augmentalis.voiceos.cursor.core.CursorOffset
  - import com.augmentalis.voiceos.cursor.manager.CursorOverlayManager
  - import com.augmentalis.voiceos.cursor.view.CursorAction

---

### File: `src/test/java/com/augmentalis/voiceos/cursor/core/CursorMovementTest.kt`

**Package:** `com.augmentalis.voiceos.cursor.core`

**Classes/Interfaces/Objects:**
  - class CursorMovementTest 

**Public Functions:**

**Imports:**
  - import org.junit.Before
  - import org.junit.Test
  - import org.junit.Assert.*
  - import kotlin.math.abs

---

### File: `src/test/java/com/augmentalis/voiceos/cursor/filter/CursorFilterBenchmark.kt`

**Package:** `com.augmentalis.voiceos.cursor.filter`

**Classes/Interfaces/Objects:**
  - class CursorFilterBenchmark 

**Public Functions:**

**Imports:**
  - import org.junit.Test
  - import kotlin.system.measureNanoTime
  - import kotlin.system.measureTimeMillis

---

### File: `src/test/java/com/augmentalis/voiceos/cursor/filter/CursorFilterTest.kt`

**Package:** `com.augmentalis.voiceos.cursor.filter`

**Classes/Interfaces/Objects:**
  - class CursorFilterTest 
  - object CursorFilterTestUtils 

**Public Functions:**

**Imports:**
  - import org.junit.Before
  - import org.junit.Test
  - import org.junit.Assert.*
  - import kotlin.math.abs
  - import kotlin.math.sqrt
  - import kotlin.system.measureNanoTime

---

## Summary

**Total Files:** 29

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
                              values
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
```

---

**Generated by:** VOS4 API Documentation Generator
**Timestamp:** 251019-2203
