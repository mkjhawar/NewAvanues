# VoiceUI - Complete API Reference

**Module Type:** apps
**Generated:** 2025-10-19 22:03:49 PDT
**Timestamp:** 251019-2203
**Location:** `modules/apps/VoiceUI`

---

## Overview

This document provides a complete file-by-file, class-by-class reference for the VoiceUI module.

## Files


### File: `src/main/java/com/augmentalis/voiceui/api/EnhancedMagicComponents.kt`

**Package:** `com.augmentalis.voiceui.api`

**Classes/Interfaces/Objects:**
  - data class Position(
  - class PaddableComponent<T>(

**Public Functions:**
  - fun MagicScope.card(
  - fun MagicScope.email(
  - fun MagicScope.row(
  - fun MagicScope.column(
  - fun MagicScope.grid(
  - fun MagicScope.padded(
  - fun String.pad(padding: Any): PaddableComponent<String> 
  - fun MagicScope.withSpacing(

**Imports:**
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.unit.Dp
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.voiceui.layout.*
  - import com.augmentalis.voiceui.theme.*
  - import com.augmentalis.voiceui.dsl.MagicScope

---

### File: `src/main/java/com/augmentalis/voiceui/api/MagicComponents.kt`

**Package:** `com.augmentalis.voiceui.api`

**Classes/Interfaces/Objects:**
  - enum class PasswordStrength 
  - data class FormField(
  - enum class FieldType 
  - data class FormData(

**Public Functions:**
  - fun MagicScope.email(
  - fun MagicScope.password(
  - fun MagicScope.phone(
  - fun MagicScope.name(
  - fun MagicScope.submit(
  - fun MagicScope.form(

**Imports:**
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.text.KeyboardOptions
  - import androidx.compose.material3.*
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.text.input.*
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/voiceui/api/VoiceMagicComponents.kt`

**Package:** `com.augmentalis.voiceui.api`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun VoiceMagicEmail(label: String = "Email"): String 
  - fun VoiceMagicPassword(label: String = "Password"): String 
  - fun VoiceMagicButton(text: String, onClick: () -> Unit) 
  - fun VoiceMagicCard(
  - fun VoiceMagicLoginScreen() 

**Imports:**
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Modifier
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.ui.unit.dp

---

### File: `src/main/java/com/augmentalis/voiceui/core/MagicEngine.kt`

**Package:** `com.augmentalis.voiceui.core`

**Classes/Interfaces/Objects:**
  - object MagicEngine 
  - data class MagicState<T>(
  - data class ScreenContext(
  - enum class ScreenType 
  - enum class ComponentType 
  - enum class StatePersistence 
  - class ComponentRegistry 
  - class ComponentFactory(val type
  - class ValidationEngine 
  - class PerformanceMonitor 
  - class EmailComponentConfig
  - class PasswordComponentConfig
  - class GenericComponentConfig

**Public Functions:**

**Imports:**
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.platform.LocalContext
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.*
  - import android.content.Context
  - import androidx.datastore.preferences.core.*
  - import androidx.datastore.preferences.preferencesDataStore
  - import java.util.concurrent.ConcurrentHashMap
  - import kotlin.reflect.KProperty

---

### File: `src/main/java/com/augmentalis/voiceui/core/MagicUUIDIntegration.kt`

**Package:** `com.augmentalis.voiceui.core`

**Classes/Interfaces/Objects:**
  - object MagicUUIDIntegration 
  - data class ComponentMetadata(
  - data class ComponentPosition(
  - data class ScreenInfo(
  - data class VoiceCommandInfo(
  - enum class NavigationDirection 
  - data class UUIDStatistics(

**Public Functions:**
  - fun rememberMagicUUID(type: String, name: String? = null): String 
  - fun rememberScreenUUID(name: String): String 

**Imports:**
  - import androidx.compose.runtime.*
  - import com.augmentalis.uuidcreator.UUIDCreator
  - import com.augmentalis.uuidcreator.models.UUIDElement
  - import com.augmentalis.uuidcreator.models.UUIDPosition
  - import com.augmentalis.uuidcreator.models.UUIDMetadata
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.launch
  - import java.util.concurrent.ConcurrentHashMap

---

### File: `src/main/java/com/augmentalis/voiceui/dsl/MagicScreen.kt`

**Package:** `com.augmentalis.voiceui.dsl`

**Classes/Interfaces/Objects:**
  - class MagicScope(
  - enum class TextStyle 
  - sealed class SettingItem 
  - object VoiceCommandRegistry 

**Public Functions:**
  - fun MagicScreen(
  - fun loginScreen(
  - fun registerScreen(
  - fun settingsScreen(

**Imports:**
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.material3.*
  - import androidx.compose.foundation.layout.Box
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.platform.LocalContext
  - import androidx.compose.ui.unit.dp
  - import androidx.compose.ui.unit.Dp
  - import com.augmentalis.voiceui.api.*
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/voiceui/examples/CompleteLayoutExample.kt`

**Package:** `com.augmentalis.voiceui.examples`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun DashboardWithCustomPadding() 
  - fun GridLayoutExample() 
  - fun MixedLayoutExample() 
  - fun AROverlayExample() 
  - fun ResponsiveLayoutExample() 
  - fun PaddingBuilderExample() 
  - fun CompleteDashboard() 
  - fun NaturalLanguageWithPadding() 

**Imports:**
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.voiceui.api.*
  - import com.augmentalis.voiceui.dsl.*
  - import com.augmentalis.voiceui.layout.*
  - import com.augmentalis.voiceui.theme.*

---

### File: `src/main/java/com/augmentalis/voiceui/hud/HUDRenderer.kt`

**Package:** `com.augmentalis.voiceui.hud`

**Classes/Interfaces/Objects:**
  - class HUDRenderer(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log

---

### File: `src/main/java/com/augmentalis/voiceui/hud/HUDSystem.kt`

**Package:** `com.augmentalis.voiceui.hud`

**Classes/Interfaces/Objects:**
  - class HUDSystem(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.SupervisorJob

---

### File: `src/main/java/com/augmentalis/voiceui/layout/LayoutSystem.kt`

**Package:** `com.augmentalis.voiceui.layout`

**Classes/Interfaces/Objects:**
  - data class LayoutConfig(
  - enum class LayoutType 
  - enum class LayoutAlignment 
  - class GridScope(private val lazyGridScope
  - class ARScope(private val boxScope
  - data class SpacingConfig(
  - class ResponsiveScope(val screenSize
  - enum class ScreenSize 

**Public Functions:**
  - fun MagicRow(
  - fun MagicColumn(
  - fun MagicGrid(
  - fun MagicFlow(
  - fun MagicStack(
  - fun ARLayout(
  - fun ResponsiveLayout(
  - fun FlowRow(

**Imports:**
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.unit.Dp
  - import androidx.compose.ui.unit.dp
  - import androidx.compose.ui.layout.Layout
  - import androidx.compose.ui.platform.LocalConfiguration
  - import androidx.compose.ui.unit.IntOffset
  - import androidx.compose.foundation.lazy.grid.*
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/voiceui/layout/PaddingSystem.kt`

**Package:** `com.augmentalis.voiceui.layout`

**Classes/Interfaces/Objects:**
  - data class MagicPadding(
  - enum class PaddingPreset 
  - class PaddingBuilder 
  - data class ResponsivePadding(
  - object SmartPadding 
  - enum class ComponentType 

**Public Functions:**
  - fun Modifier.magicPadding(
  - fun Modifier.magicPadding(value: String): Modifier 
  - fun Modifier.magicPadding(preset: PaddingPreset): Modifier 
  - fun Modifier.magicPadding(all: Dp): Modifier 
  - fun Modifier.magicPadding(all: Int): Modifier 
  - inline fun Modifier.magicPadding(builder: PaddingBuilder.() -> Unit): Modifier 

**Imports:**
  - import androidx.compose.foundation.layout.PaddingValues
  - import androidx.compose.foundation.layout.padding
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.unit.Dp
  - import androidx.compose.ui.unit.dp

---

### File: `src/main/java/com/augmentalis/voiceui/migration/MigrationEngine.kt`

**Package:** `com.augmentalis.voiceui.migration`

**Classes/Interfaces/Objects:**
  - object MigrationEngine 
  - data class MigrationOptions(
  - data class MigrationResult(
  - data class MigrationPreview(
  - data class CodeAnalysis(
  - data class DetectedComponent(
  - data class DetectedState(
  - data class CodeImprovements(
  - data class MigrationRecord(
  - data class ApplyResult(
  - enum class SourceType 
  - enum class ComponentType 

**Public Functions:**

**Imports:**
  - import kotlinx.coroutines.*
  - import java.io.File
  - import kotlin.text.Regex

---

### File: `src/main/java/com/augmentalis/voiceui/nlp/NaturalLanguageParser.kt`

**Package:** `com.augmentalis.voiceui.nlp`

**Classes/Interfaces/Objects:**
  - object NaturalLanguageParser 
  - data class ParsedUI(
  - enum class ScreenTemplate 
  - sealed class UIComponent 
  - enum class ButtonStyle 
  - data class UIStyles(
  - enum class SizePreference 
  - enum class AlignmentPreference 
  - enum class SpacingPreference 
  - enum class UIFeature 
  - sealed class Intent 
  - enum class ComponentIntent 
  - enum class StyleIntent 
  - enum class FeatureIntent 

**Public Functions:**

**Imports:**
  - import com.google.mlkit.nl.languageid.LanguageIdentification
  - import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
  - import kotlinx.coroutines.*
  - import java.util.regex.Pattern

---

### File: `src/main/java/com/augmentalis/voiceui/theme/GreyARComponents.kt`

**Package:** `com.augmentalis.voiceui.theme`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun GreyARCard(
  - fun GreyARButton(
  - fun GreyARTextButton(
  - fun GreyARTextField(
  - fun GreyARFooterText(
  - fun GreyARLinkText(
  - fun GreyARDivider(

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.border
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.draw.blur
  - import androidx.compose.ui.draw.clip
  - import androidx.compose.ui.graphics.Brush
  - ... and 9 more

---

### File: `src/main/java/com/augmentalis/voiceui/theme/GreyARScreen.kt`

**Package:** `com.augmentalis.voiceui.theme`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun GreyARWebsiteBuilderScreen() 
  - fun GreyARLoginScreen(
  - fun GreyARMagicScreen(
  - fun GreyARMagicWebsiteBuilder() 
  - fun GreyARSettingsScreen() 
  - fun GreyARDashboardWithThreeContainers() 

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.text.style.TextAlign
  - import androidx.compose.ui.unit.dp
  - import androidx.compose.ui.unit.sp
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/voiceui/theme/GreyARTheme.kt`

**Package:** `com.augmentalis.voiceui.theme`

**Classes/Interfaces/Objects:**
  - object GreyARColors 
  - object GreyARShapes 
  - data class GreyARThemeConfig(

**Public Functions:**
  - fun GreyARTheme(

**Imports:**
  - import androidx.compose.foundation.isSystemInDarkTheme
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.Composable
  - import androidx.compose.runtime.CompositionLocalProvider
  - import androidx.compose.runtime.staticCompositionLocalOf
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.text.TextStyle
  - import androidx.compose.ui.text.font.Font
  - import androidx.compose.ui.text.font.FontFamily
  - import androidx.compose.ui.text.font.FontWeight
  - ... and 4 more

---

### File: `src/main/java/com/augmentalis/voiceui/theme/MagicDreamTheme.kt`

**Package:** `com.augmentalis.voiceui.theme`

**Classes/Interfaces/Objects:**
  - object MagicDreamColors 
  - object MagicDreamShapes 
  - object MagicDreamTypography 
  - object MagicDreamElevations 

**Public Functions:**
  - fun dreamGradientBrush(
  - fun dreamRadialGradient(
  - fun MagicDreamTheme(
  - fun MagicDreamGradientBackground(

**Imports:**
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.CircleShape
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.draw.*
  - ... and 7 more

---

### File: `src/main/java/com/augmentalis/voiceui/theme/MagicThemeCustomizer.kt`

**Package:** `com.augmentalis.voiceui.theme`

**Classes/Interfaces/Objects:**
  - enum class ThemeTab(val title
  - data class MagicThemeData(
  - data class ThemePreset(
  - object MagicThemePresets 

**Public Functions:**
  - fun MagicThemeCustomizer(

**Imports:**
  - import androidx.compose.animation.animateColorAsState
  - import androidx.compose.animation.core.tween
  - import androidx.compose.foundation.*
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.*
  - import androidx.compose.foundation.lazy.grid.GridCells
  - import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
  - import androidx.compose.foundation.lazy.grid.items
  - import androidx.compose.foundation.shape.CircleShape
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - ... and 18 more

---

### File: `src/main/java/com/augmentalis/voiceui/widgets/MagicButton.kt`

**Package:** `com.augmentalis.voiceui.widgets`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun MagicButton(

**Imports:**
  - import androidx.compose.foundation.layout.Spacer
  - import androidx.compose.foundation.layout.size
  - import androidx.compose.foundation.layout.width
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.vector.ImageVector
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.voiceui.theme.MagicThemeData

---

### File: `src/main/java/com/augmentalis/voiceui/widgets/MagicCard.kt`

**Package:** `com.augmentalis.voiceui.widgets`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun MagicCard(

**Imports:**
  - import androidx.compose.foundation.layout.Column
  - import androidx.compose.foundation.layout.ColumnScope
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.voiceui.theme.MagicThemeData

---

### File: `src/main/java/com/augmentalis/voiceui/widgets/MagicFloatingActionButton.kt`

**Package:** `com.augmentalis.voiceui.widgets`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun MagicFloatingActionButton(

**Imports:**
  - import androidx.compose.foundation.shape.CircleShape
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.vector.ImageVector
  - import com.augmentalis.voiceui.theme.MagicThemeData

---

### File: `src/main/java/com/augmentalis/voiceui/widgets/MagicIconButton.kt`

**Package:** `com.augmentalis.voiceui.widgets`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun MagicIconButton(

**Imports:**
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.vector.ImageVector
  - import com.augmentalis.voiceui.theme.MagicThemeData

---

### File: `src/main/java/com/augmentalis/voiceui/widgets/MagicRow.kt`

**Package:** `com.augmentalis.voiceui.widgets`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun MagicRow(

**Imports:**
  - import androidx.compose.foundation.layout.Arrangement
  - import androidx.compose.foundation.layout.Row
  - import androidx.compose.foundation.layout.RowScope
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.unit.Dp
  - import androidx.compose.ui.unit.dp

---

### File: `src/main/java/com/augmentalis/voiceui/windows/MagicWindowExamples.kt`

**Package:** `com.augmentalis.voiceui.windows`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun MagicWindowShowcase() 

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.CircleShape
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.automirrored.filled.*
  - import androidx.compose.material.icons.filled.*
  - import androidx.compose.material.icons.outlined.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Alignment
  - ... and 13 more

---

### File: `src/main/java/com/augmentalis/voiceui/windows/MagicWindowSystem.kt`

**Package:** `com.augmentalis.voiceui.windows`

**Classes/Interfaces/Objects:**
  - object MagicWindowManager 
  - data class MagicWindowState(
  - data class MagicWindowConfig(
  - enum class WindowAnimation 
  - object MagicWindowPresets 

**Public Functions:**
  - fun MagicWindow(
  - fun MagicWindowContainer(
  - fun registerWindowVoiceCommands() 
  - fun AnimatedMagicWindow(

**Imports:**
  - import androidx.compose.animation.*
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.*
  - import androidx.compose.foundation.gestures.*
  - import androidx.compose.foundation.interaction.MutableInteractionSource
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.CircleShape
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - ... and 22 more

---

## Summary

**Total Files:** 25

**Module Structure:**
```
                  build
                    generated
                      data_binding_base_class_source_out
                        debug
                          out
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
                      data_binding_base_class_log_artifact
                        debug
                          dataBindingGenBaseClassesDebug
                            out
                      data_binding_dependency_artifacts
                        debug
                          dataBindingMergeDependencyArtifactsDebug
                      data_binding_layout_info_type_package
                        debug
                          packageDebugResources
                            out
                      incremental
                        dataBindingGenBaseClassesDebug
                        debug
                          packageDebugResources
                            merged.dir
```

---

**Generated by:** VOS4 API Documentation Generator
**Timestamp:** 251019-2203
