# MagicUI - Complete API Reference

**Module Type:** libraries
**Generated:** 2025-10-19 22:03:52 PDT
**Timestamp:** 251019-2203
**Location:** `modules/libraries/MagicUI`

---

## Overview

This document provides a complete file-by-file, class-by-class reference for the MagicUI module.

## Files


### File: `src/main/java/com/augmentalis/magicui/animation/MagicAnimations.kt`

**Package:** `com.augmentalis.magicui.animation`

**Classes/Interfaces/Objects:**
  - object MagicAnimations 
  - object MagicTransitions 
  - interface TransitionPreset 
  - object SpringPresets 

**Public Functions:**
  - fun MagicAnimatedVisibility(
  - fun <T> MagicCrossfade(

**Imports:**
  - import androidx.compose.animation.*
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.layout.Box
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.TransformOrigin

---

### File: `src/main/java/com/augmentalis/magicui/components/Button.kt`

**Package:** `com.augmentalis.magicui.components`

**Classes/Interfaces/Objects:**
  - class ButtonComponent(

**Public Functions:**
  - fun MagicUIScope.Button(

**Imports:**
  - import androidx.compose.foundation.layout.PaddingValues
  - import androidx.compose.material3.Button as ComposeButton
  - import androidx.compose.material3.ButtonDefaults
  - import androidx.compose.material3.Text
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.magicui.core.MagicUIScope

---

### File: `src/main/java/com/augmentalis/magicui/components/Card.kt`

**Package:** `com.augmentalis.magicui.components`

**Classes/Interfaces/Objects:**
  - class CardComponent(

**Public Functions:**
  - fun MagicUIScope.Card(

**Imports:**
  - import androidx.compose.foundation.BorderStroke
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.Column
  - import androidx.compose.foundation.layout.ColumnScope
  - import androidx.compose.foundation.layout.padding
  - import androidx.compose.material3.Card as ComposeCard
  - import androidx.compose.material3.CardDefaults
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.Color
  - ... and 4 more

---

### File: `src/main/java/com/augmentalis/magicui/components/Divider.kt`

**Package:** `com.augmentalis.magicui.components`

**Classes/Interfaces/Objects:**
  - class DividerComponent(
  - class SpacerComponent(

**Public Functions:**
  - fun MagicUIScope.Divider(
  - fun MagicUIScope.VerticalDivider(
  - fun MagicUIScope.Spacer(

**Imports:**
  - import androidx.compose.foundation.layout.Spacer as ComposeSpacer
  - import androidx.compose.foundation.layout.height
  - import androidx.compose.foundation.layout.width
  - import androidx.compose.material3.Divider as ComposeDivider
  - import androidx.compose.material3.HorizontalDivider
  - import androidx.compose.material3.VerticalDivider
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.unit.Dp
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/magicui/components/Icon.kt`

**Package:** `com.augmentalis.magicui.components`

**Classes/Interfaces/Objects:**
  - class IconComponent(

**Public Functions:**
  - fun MagicUIScope.Icon(

**Imports:**
  - import androidx.compose.material3.Icon as ComposeIcon
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.graphics.vector.ImageVector
  - import androidx.compose.ui.unit.Dp
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.magicui.core.MagicUIScope
  - import androidx.compose.foundation.layout.size

---

### File: `src/main/java/com/augmentalis/magicui/components/Image.kt`

**Package:** `com.augmentalis.magicui.components`

**Classes/Interfaces/Objects:**
  - class ImageComponent(

**Public Functions:**
  - fun MagicUIScope.Image(
  - fun MagicUIScope.Image(

**Imports:**
  - import androidx.compose.foundation.Image as ComposeImage
  - import androidx.compose.foundation.layout.size
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.ColorFilter
  - import androidx.compose.ui.graphics.DefaultAlpha
  - import androidx.compose.ui.graphics.painter.Painter
  - import androidx.compose.ui.layout.ContentScale
  - import androidx.compose.ui.res.painterResource
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/magicui/components/List.kt`

**Package:** `com.augmentalis.magicui.components`

**Classes/Interfaces/Objects:**
  - class ListComponent<T>(

**Public Functions:**
  - fun <T> MagicUIScope.List(

**Imports:**
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.Arrangement
  - import androidx.compose.foundation.layout.Column
  - import androidx.compose.foundation.layout.PaddingValues
  - import androidx.compose.foundation.layout.fillMaxWidth
  - import androidx.compose.foundation.layout.padding
  - import androidx.compose.foundation.lazy.LazyColumn
  - import androidx.compose.foundation.lazy.items
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/magicui/components/ProgressBar.kt`

**Package:** `com.augmentalis.magicui.components`

**Classes/Interfaces/Objects:**
  - class ProgressBarComponent(

**Public Functions:**
  - fun MagicUIScope.ProgressBar(
  - fun MagicUIScope.CircularProgress(

**Imports:**
  - import androidx.compose.material3.CircularProgressIndicator
  - import androidx.compose.material3.LinearProgressIndicator
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.unit.Dp
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.magicui.core.MagicUIScope

---

### File: `src/main/java/com/augmentalis/magicui/components/ScrollView.kt`

**Package:** `com.augmentalis.magicui.components`

**Classes/Interfaces/Objects:**
  - class ScrollViewComponent(

**Public Functions:**
  - fun MagicUIScope.ScrollView(
  - fun MagicUIScope.HorizontalScrollView(

**Imports:**
  - import androidx.compose.foundation.gestures.ScrollableState
  - import androidx.compose.foundation.horizontalScroll
  - import androidx.compose.foundation.layout.Column
  - import androidx.compose.foundation.layout.Row
  - import androidx.compose.foundation.rememberScrollState
  - import androidx.compose.foundation.verticalScroll
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import com.augmentalis.magicui.core.MagicUIScope

---

### File: `src/main/java/com/augmentalis/magicui/components/Slider.kt`

**Package:** `com.augmentalis.magicui.components`

**Classes/Interfaces/Objects:**
  - class SliderComponent(

**Public Functions:**
  - fun MagicUIScope.Slider(
  - fun MagicUIScope.Slider(

**Imports:**
  - import androidx.compose.material3.Slider as ComposeSlider
  - import androidx.compose.material3.SliderDefaults
  - import androidx.compose.runtime.Composable
  - import androidx.compose.runtime.MutableState
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.Color
  - import com.augmentalis.magicui.core.MagicUIScope

---

### File: `src/main/java/com/augmentalis/magicui/components/Stack.kt`

**Package:** `com.augmentalis.magicui.components`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun MagicUIScope.VStack(
  - fun MagicUIScope.HStack(
  - fun MagicUIScope.ZStack(

**Imports:**
  - import androidx.compose.foundation.layout.Arrangement
  - import androidx.compose.foundation.layout.Column
  - import androidx.compose.foundation.layout.Row
  - import androidx.compose.foundation.layout.padding
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.unit.Dp
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.magicui.core.MagicUIDsl
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/magicui/components/Text.kt`

**Package:** `com.augmentalis.magicui.components`

**Classes/Interfaces/Objects:**
  - class TextComponent(

**Public Functions:**
  - fun MagicUIScope.Text(

**Imports:**
  - import androidx.compose.material3.LocalTextStyle
  - import androidx.compose.material3.Text as ComposeText
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.text.TextStyle
  - import androidx.compose.ui.text.font.FontWeight
  - import androidx.compose.ui.text.style.TextAlign
  - import androidx.compose.ui.unit.TextUnit
  - import androidx.compose.ui.unit.sp
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/magicui/components/TextField.kt`

**Package:** `com.augmentalis.magicui.components`

**Classes/Interfaces/Objects:**
  - class TextFieldComponent(

**Public Functions:**
  - fun MagicUIScope.TextField(
  - fun MagicUIScope.SecureField(

**Imports:**
  - import androidx.compose.foundation.text.KeyboardActions
  - import androidx.compose.foundation.text.KeyboardOptions
  - import androidx.compose.material3.OutlinedTextField
  - import androidx.compose.material3.Text
  - import androidx.compose.material3.TextField as ComposeTextField
  - import androidx.compose.runtime.Composable
  - import androidx.compose.runtime.MutableState
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.text.input.ImeAction
  - import androidx.compose.ui.text.input.KeyboardType
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/magicui/components/Toggle.kt`

**Package:** `com.augmentalis.magicui.components`

**Classes/Interfaces/Objects:**
  - class ToggleComponent(

**Public Functions:**
  - fun MagicUIScope.Toggle(
  - fun MagicUIScope.Toggle(

**Imports:**
  - import androidx.compose.material3.Switch
  - import androidx.compose.material3.SwitchDefaults
  - import androidx.compose.runtime.Composable
  - import androidx.compose.runtime.MutableState
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.Color
  - import com.augmentalis.magicui.core.MagicUIScope

---

### File: `src/main/java/com/augmentalis/magicui/core/MagicScreen.kt`

**Package:** `com.augmentalis.magicui.core`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun MagicScreen(

**Imports:**
  - import androidx.compose.foundation.layout.fillMaxSize
  - import androidx.compose.material3.Surface
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Modifier
  - import com.augmentalis.magicui.MagicUI
  - import com.augmentalis.magicui.theme.MagicTheme
  - import com.augmentalis.magicui.theme.ThemeMode
  - import com.augmentalis.magicui.theme.themes.Material3Theme
  - import com.augmentalis.magicui.spatial.SpatialMode

---

### File: `src/main/java/com/augmentalis/magicui/core/MagicUIScope.kt`

**Package:** `com.augmentalis.magicui.core`

**Classes/Interfaces/Objects:**
  - class MagicUIScope(

**Public Functions:**

**Imports:**
  - import androidx.compose.runtime.Composable
  - import androidx.compose.runtime.Stable
  - import com.augmentalis.magicui.theme.ThemeMode
  - import com.augmentalis.magicui.spatial.SpatialMode

---

### File: `src/main/java/com/augmentalis/magicui/database/MagicDatabase.kt`

**Package:** `com.augmentalis.magicui.database`

**Classes/Interfaces/Objects:**
  - abstract class MagicDatabase 
  - data class DatabaseConfig(
  - data class GeneratedDao<T>(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import androidx.room.Database
  - import androidx.room.Room
  - import androidx.room.RoomDatabase
  - import androidx.room.TypeConverters

---

### File: `src/main/java/com/augmentalis/magicui/integration/VoiceCommandIntegration.kt`

**Package:** `com.augmentalis.magicui.integration`

**Classes/Interfaces/Objects:**
  - class VoiceCommandIntegration(
  - data class VoiceCommand(
  - enum class CommandType 
  - class VoiceCommandBuilder(private val integration

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.magicui.core.MagicUIScope

---

### File: `src/main/java/com/augmentalis/magicui/MagicUI.kt`

**Package:** `com.augmentalis.magicui`

**Classes/Interfaces/Objects:**
  - object MagicUI 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/magicui/modifiers/ModifierExtensions.kt`

**Package:** `com.augmentalis.magicui.modifiers`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun Modifier.padding(all: Dp): Modifier = this.padding(all)
  - fun Modifier.padding(
  - fun Modifier.padding(
  - fun Modifier.size(size: Dp): Modifier = this.size(size)
  - fun Modifier.size(width: Dp, height: Dp): Modifier = this.size(width, height)
  - fun Modifier.width(width: Dp): Modifier = this.width(width)
  - fun Modifier.height(height: Dp): Modifier = this.height(height)
  - fun Modifier.fillMaxWidth(fraction: Float = 1f): Modifier = this.fillMaxWidth(fraction)
  - fun Modifier.fillMaxHeight(fraction: Float = 1f): Modifier = this.fillMaxHeight(fraction)
  - fun Modifier.fillMaxSize(fraction: Float = 1f): Modifier = this.fillMaxSize(fraction)
  - fun Modifier.background(color: Color): Modifier = this.background(color)
  - fun Modifier.background(brush: Brush): Modifier = this.background(brush)
  - fun Modifier.background(color: Color, shape: Shape): Modifier = this.background(color, shape)
  - fun Modifier.cornerRadius(radius: Dp): Modifier = this.clip(RoundedCornerShape(radius))
  - fun Modifier.cornerRadius(
  - fun Modifier.border(width: Dp, color: Color): Modifier = this.border(width, color)
  - fun Modifier.border(width: Dp, color: Color, shape: Shape): Modifier = this.border(width, color, shape)
  - fun Modifier.shadow(elevation: Dp): Modifier = this.shadow(elevation)
  - fun Modifier.shadow(elevation: Dp, shape: Shape): Modifier = this.shadow(elevation, shape)
  - fun Modifier.clipShape(shape: Shape): Modifier = this.clip(shape)
  - fun Modifier.onClick(onClick: () -> Unit): Modifier = this.clickable 
  - fun Modifier.onTap(onTap: () -> Unit): Modifier = this.clickable(
  - fun Modifier.opacity(alpha: Float): Modifier = this.alpha(alpha)
  - fun Modifier.rotateBy(degrees: Float): Modifier = this.rotate(degrees)
  - fun Modifier.scaleBy(scale: Float): Modifier = this.composeGraphicsLayer(scaleX = scale, scaleY = scale)
  - fun Modifier.scaleBy(scaleX: Float, scaleY: Float): Modifier = this.composeGraphicsLayer(scaleX = scaleX, scaleY = scaleY)
  - fun Modifier.align(alignment: androidx.compose.ui.Alignment): Modifier = this.then(
  - fun Modifier.offset(x: Dp = 0.dp, y: Dp = 0.dp): Modifier = this.then(

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.border
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.fillMaxHeight
  - import androidx.compose.foundation.layout.fillMaxSize
  - import androidx.compose.foundation.layout.fillMaxWidth
  - import androidx.compose.foundation.layout.height
  - import androidx.compose.foundation.layout.padding
  - import androidx.compose.foundation.layout.size
  - import androidx.compose.foundation.layout.width
  - ... and 12 more

---

### File: `src/main/java/com/augmentalis/magicui/performance/PerformanceUtils.kt`

**Package:** `com.augmentalis.magicui.performance`

**Classes/Interfaces/Objects:**
  - object PerformanceUtils 
  - class LazyInitializer<T>(private val initializer
  - class MemoryCache<K, V>(private val maxSize

**Public Functions:**

**Imports:**
  - import androidx.compose.runtime.Composable
  - import androidx.compose.runtime.remember
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.layout.layout

---

### File: `src/main/java/com/augmentalis/magicui/samples/ComprehensiveDemoApp.kt`

**Package:** `com.augmentalis.magicui.samples`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun ComprehensiveDemoApp() 

**Imports:**
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.unit.dp
  - import androidx.compose.ui.unit.sp
  - import com.augmentalis.magicui.animation.MagicAnimatedVisibility
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/magicui/samples/DatabaseCRUDExample.kt`

**Package:** `com.augmentalis.magicui.samples`

**Classes/Interfaces/Objects:**
  - data class Person(
  - interface PersonDao 
  - abstract class PersonDatabase 

**Public Functions:**
  - fun DatabaseCRUDExample() 

**Imports:**
  - import android.content.Context
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.LazyColumn
  - import androidx.compose.foundation.lazy.items
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - ... and 10 more

---

### File: `src/main/java/com/augmentalis/magicui/samples/IconsAndThemeExample.kt`

**Package:** `com.augmentalis.magicui.samples`

**Classes/Interfaces/Objects:**
  - class CustomBrandTheme 

**Public Functions:**
  - fun IconsAndThemeExample() 

**Imports:**
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.filled.*
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.unit.dp
  - import androidx.compose.ui.unit.sp
  - import com.augmentalis.magicui.components.*
  - import com.augmentalis.magicui.core.MagicScreen
  - import com.augmentalis.magicui.core.MagicUIScope
  - ... and 11 more

---

### File: `src/main/java/com/augmentalis/magicui/samples/LoginScreenSample.kt`

**Package:** `com.augmentalis.magicui.samples`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun LoginScreenSample() 

**Imports:**
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.text.input.KeyboardType
  - import androidx.compose.ui.unit.dp
  - import androidx.compose.ui.unit.sp
  - import com.augmentalis.magicui.components.Button
  - import com.augmentalis.magicui.components.SecureField
  - import com.augmentalis.magicui.components.Text
  - import com.augmentalis.magicui.components.TextField
  - ... and 4 more

---

### File: `src/main/java/com/augmentalis/magicui/samples/MagicTemplatesConceptDemo.kt`

**Package:** `com.augmentalis.magicui.samples`

**Classes/Interfaces/Objects:**
  - data class User(

**Public Functions:**
  - fun MagicTemplatesConceptDemo() 

**Imports:**
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.lazy.LazyColumn
  - import androidx.compose.foundation.lazy.items
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.unit.dp
  - import androidx.compose.ui.unit.sp
  - import com.augmentalis.magicui.components.*
  - import com.augmentalis.magicui.core.MagicScreen
  - ... and 1 more

---

### File: `src/main/java/com/augmentalis/magicui/samples/SpatialUIDemo.kt`

**Package:** `com.augmentalis.magicui.samples`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun SpatialUIDemo() 

**Imports:**
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.material3.Text
  - import androidx.compose.runtime.Composable
  - import androidx.compose.runtime.mutableStateOf
  - import androidx.compose.runtime.remember
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.magicui.MagicScreen
  - import com.augmentalis.magicui.core.state
  - ... and 3 more

---

### File: `src/main/java/com/augmentalis/magicui/spatial/SpatialComponents.kt`

**Package:** `com.augmentalis.magicui.spatial`

**Classes/Interfaces/Objects:**
  - enum class SpatialButtonSize(val width
  - data class SpatialMenuItem(
  - enum class TooltipPosition 

**Public Functions:**
  - fun MagicScope.spatialButton(
  - fun MagicScope.spatialCard(
  - fun MagicScope.spatialPanel(
  - fun MagicScope.spatialWindow(
  - fun MagicScope.spatialMenu(
  - fun MagicScope.spatialTooltip(
  - fun MagicScope.spatialContainer(
  - fun MagicScope.spatialGrid(

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.border
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material3.Text
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.draw.clip
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/magicui/spatial/SpatialFoundation.kt`

**Package:** `com.augmentalis.magicui.spatial`

**Classes/Interfaces/Objects:**
  - data class SpatialPosition(
  - data class SpatialSize(
  - enum class DepthLayer(val zOffset
  - enum class SpatialMode 
  - enum class SpatialPlatform 
  - object ViewingZones 
  - enum class SpatialInput 
  - data class GlassMaterial(
  - data class SpatialAnimation(
  - enum class SpatialEasing 
  - enum class SpatialWindowType 
  - data class SpatialGesture(
  - enum class GestureType 
  - object SpatialPlatformDetector 
  - enum class SpatialFeature 
  - data class SpatialPerformance(
  - enum class ThermalState 
  - data class ComfortSettings(
  - enum class ComfortMode 

**Public Functions:**

**Imports:**
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.unit.Dp
  - import androidx.compose.ui.unit.dp

---

### File: `src/main/java/com/augmentalis/magicui/spatial/SpatialMode.kt`

**Package:** `com.augmentalis.magicui.spatial`

**Classes/Interfaces/Objects:**
  - enum class SpatialMode 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/magicui/state/StateHelpers.kt`

**Package:** `com.augmentalis.magicui.state`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun <T> state(initial: T): MutableState<T> 
  - fun <T> computed(calculation: () -> T): State<T> 
  - fun <T> stateWith(initial: T, onSet: (T) -> T): MutableState<T> 

**Imports:**
  - import androidx.compose.runtime.Composable
  - import androidx.compose.runtime.MutableState
  - import androidx.compose.runtime.State
  - import androidx.compose.runtime.derivedStateOf
  - import androidx.compose.runtime.mutableStateOf
  - import androidx.compose.runtime.remember

---

### File: `src/main/java/com/augmentalis/magicui/state/StatePersistence.kt`

**Package:** `com.augmentalis.magicui.state`

**Classes/Interfaces/Objects:**
  - class StatePersistence(
  - object PersistentStateFactory 
  - data class PersistenceConfig(
  - enum class PersistenceStrategy 

**Public Functions:**
  - fun StatePersistence.persistentState(

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import androidx.compose.runtime.MutableState
  - import androidx.compose.runtime.mutableStateOf
  - import kotlinx.coroutines.CoroutineScope
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.launch

---

### File: `src/main/java/com/augmentalis/magicui/templates/MagicTemplateEngine.kt`

**Package:** `com.augmentalis.magicui.templates`

**Classes/Interfaces/Objects:**
  - object MagicTemplateEngine 
  - data class DataClassMetadata(
  - data class FieldMetadata(
  - enum class FieldType 
  - sealed class ValidationRule 
  - enum class FormMode 
  - data class ListConfig(
  - data class TableConfig(

**Public Functions:**

**Imports:**
  - import androidx.compose.runtime.Composable
  - import kotlin.reflect.KClass
  - import kotlin.reflect.KProperty1
  - import kotlin.reflect.full.memberProperties

---

### File: `src/main/java/com/augmentalis/magicui/templates/SimpleTemplates.kt`

**Package:** `com.augmentalis.magicui.templates`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun MagicUIScope.text(
  - fun MagicUIScope.button(
  - fun MagicUIScope.input(
  - fun MagicUIScope.toggle(
  - fun MagicUIScope.slider(
  - fun MagicUIScope.card(

**Imports:**
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.Composable
  - import androidx.compose.runtime.MutableState
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.magicui.core.MagicUIScope

---

### File: `src/main/java/com/augmentalis/magicui/theme/MagicTheme.kt`

**Package:** `com.augmentalis.magicui.theme`

**Classes/Interfaces/Objects:**
  - interface MagicTheme 
  - data class MagicThemeColors(
  - data class MagicThemeTypography(
  - data class MagicThemeShapes(
  - data class MagicThemeSpacing(
  - data class MagicThemeElevation(
  - object MagicThemeTypographyDefaults 

**Public Functions:**

**Imports:**
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.text.TextStyle
  - import androidx.compose.ui.text.font.FontWeight
  - import androidx.compose.ui.unit.Dp
  - import androidx.compose.ui.unit.TextUnit
  - import androidx.compose.ui.unit.dp
  - import androidx.compose.ui.unit.sp

---

### File: `src/main/java/com/augmentalis/magicui/theme/ThemeMode.kt`

**Package:** `com.augmentalis.magicui.theme`

**Classes/Interfaces/Objects:**
  - enum class ThemeMode 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/magicui/theme/themes/CupertinoTheme.kt`

**Package:** `com.augmentalis.magicui.theme.themes`

**Classes/Interfaces/Objects:**
  - class CupertinoTheme(

**Public Functions:**

**Imports:**
  - import androidx.compose.material3.MaterialTheme
  - import androidx.compose.material3.darkColorScheme
  - import androidx.compose.material3.lightColorScheme
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.magicui.theme.*

---

### File: `src/main/java/com/augmentalis/magicui/theme/themes/FluentTheme.kt`

**Package:** `com.augmentalis.magicui.theme.themes`

**Classes/Interfaces/Objects:**
  - class FluentTheme(

**Public Functions:**

**Imports:**
  - import androidx.compose.material3.MaterialTheme
  - import androidx.compose.material3.darkColorScheme
  - import androidx.compose.material3.lightColorScheme
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.magicui.theme.*

---

### File: `src/main/java/com/augmentalis/magicui/theme/themes/Material3Theme.kt`

**Package:** `com.augmentalis.magicui.theme.themes`

**Classes/Interfaces/Objects:**
  - class Material3Theme(

**Public Functions:**

**Imports:**
  - import androidx.compose.material3.MaterialTheme
  - import androidx.compose.material3.darkColorScheme
  - import androidx.compose.material3.lightColorScheme
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.graphics.Color
  - import com.augmentalis.magicui.theme.*

---

### File: `src/main/java/com/augmentalis/magicui/theme/themes/spatial/AndroidXRTheme.kt`

**Package:** `com.augmentalis.magicui.theme.themes.spatial`

**Classes/Interfaces/Objects:**
  - class AndroidXRTheme(

**Public Functions:**

**Imports:**
  - import androidx.compose.material3.MaterialTheme
  - import androidx.compose.material3.darkColorScheme
  - import androidx.compose.material3.lightColorScheme
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.magicui.theme.*
  - import com.augmentalis.magicui.spatial.*

---

### File: `src/main/java/com/augmentalis/magicui/theme/themes/spatial/QViewTheme.kt`

**Package:** `com.augmentalis.magicui.theme.themes.spatial`

**Classes/Interfaces/Objects:**
  - class QViewTheme(

**Public Functions:**

**Imports:**
  - import androidx.compose.material3.MaterialTheme
  - import androidx.compose.material3.darkColorScheme
  - import androidx.compose.material3.lightColorScheme
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.magicui.theme.*
  - import com.augmentalis.magicui.spatial.*

---

### File: `src/main/java/com/augmentalis/magicui/theme/themes/spatial/VViewSpatialTheme.kt`

**Package:** `com.augmentalis.magicui.theme.themes.spatial`

**Classes/Interfaces/Objects:**
  - class VViewSpatialTheme(

**Public Functions:**

**Imports:**
  - import androidx.compose.material3.MaterialTheme
  - import androidx.compose.material3.darkColorScheme
  - import androidx.compose.material3.lightColorScheme
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.magicui.theme.*
  - import com.augmentalis.magicui.spatial.*

---

### File: `src/main/java/com/augmentalis/magicui/theme/themes/spatial/WaterViewTheme.kt`

**Package:** `com.augmentalis.magicui.theme.themes.spatial`

**Classes/Interfaces/Objects:**
  - class WaterViewTheme(

**Public Functions:**

**Imports:**
  - import androidx.compose.material3.MaterialTheme
  - import androidx.compose.material3.darkColorScheme
  - import androidx.compose.material3.lightColorScheme
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.magicui.theme.*
  - import com.augmentalis.magicui.spatial.*

---

### File: `src/main/java/com/augmentalis/magicui/theme/themes/VoiceOSTheme.kt`

**Package:** `com.augmentalis.magicui.theme.themes`

**Classes/Interfaces/Objects:**
  - class VoiceOSTheme(

**Public Functions:**

**Imports:**
  - import androidx.compose.material3.MaterialTheme
  - import androidx.compose.material3.darkColorScheme
  - import androidx.compose.material3.lightColorScheme
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.unit.dp
  - import com.augmentalis.magicui.theme.*

---

## Summary

**Total Files:** 44

**Module Structure:**
```
                  schemas
                    com.augmentalis.magicui.database.MagicDatabase
                  src
                    androidTest
                      java
                        com
                          augmentalis
                            magicui
                    main
                      java
                        com
                          augmentalis
                            magicui
                              animation
                              components
                              core
                              database
                              gl
                              integration
                              modifiers
                              performance
                              samples
                              spatial
                              state
                              templates
                              theme
                                themes
                                  spatial
                      res
                    test
                      java
                        com
                          augmentalis
                            magicui
                  web-tool
```

---

**Generated by:** VOS4 API Documentation Generator
**Timestamp:** 251019-2203
