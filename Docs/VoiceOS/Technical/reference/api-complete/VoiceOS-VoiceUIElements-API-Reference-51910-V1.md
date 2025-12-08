# VoiceUIElements - Complete API Reference

**Module Type:** libraries
**Generated:** 2025-10-19 22:04:01 PDT
**Timestamp:** 251019-2203
**Location:** `modules/libraries/VoiceUIElements`

---

## Overview

This document provides a complete file-by-file, class-by-class reference for the VoiceUIElements module.

## Files


### File: `components/base/SpatialButton.kt`

**Package:** ``

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `src/androidTest/java/com/augmentalis/voiceuielements/utils/TestExtensions.kt`

**Package:** `com.augmentalis.voiceuielements.utils`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun ImageBitmap.assertAgainstGolden(goldenName: String) 
  - fun SemanticsNodeInteraction.captureToImage(): ImageBitmap 

**Imports:**
  - import androidx.compose.ui.graphics.ImageBitmap
  - import androidx.compose.ui.test.SemanticsNodeInteraction

---

### File: `src/androidTest/java/com/augmentalis/voiceuielements/visual/VisualRegressionTest.kt`

**Package:** `com.augmentalis.voiceuielements.visual`

**Classes/Interfaces/Objects:**
  - class VisualRegressionTest 

**Public Functions:**

**Imports:**
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.test.*
  - import androidx.compose.ui.test.junit4.createComposeRule
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import com.augmentalis.voiceuielements.components.*
  - import com.augmentalis.voiceuielements.models.*
  - import com.augmentalis.voiceuielements.theme.VoiceUITheme
  - import com.augmentalis.voiceuielements.utils.assertAgainstGolden
  - import com.augmentalis.voiceuielements.utils.captureToImage
  - import org.junit.Rule
  - ... and 2 more

---

### File: `src/main/java/com/augmentalis/voiceuielements/components/VoiceComponents.kt`

**Package:** `com.augmentalis.voiceuielements.components`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun VoiceCommandButton(
  - fun VoiceStatusCard(
  - fun GlassmorphismCard(
  - fun VoiceWaveform(

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.material3.*
  - import androidx.compose.runtime.*
  - import androidx.compose.ui.Alignment
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.draw.blur
  - import androidx.compose.ui.draw.clip
  - import androidx.compose.ui.graphics.Color
  - ... and 5 more

---

### File: `src/main/java/com/augmentalis/voiceuielements/model/DuplicateCommandModel.kt`

**Package:** `com.augmentalis.voiceuielements.model`

**Classes/Interfaces/Objects:**
  - data class DuplicateCommandModel(
  - sealed class DisambiguationResult 
  - data class DisambiguationState(

**Public Functions:**

**Imports:**
  - import android.graphics.RectF
  - import androidx.compose.runtime.Immutable

---

### File: `src/main/java/com/augmentalis/voiceuielements/models/VoiceStatus.kt`

**Package:** `com.augmentalis.voiceuielements.models`

**Classes/Interfaces/Objects:**
  - data class VoiceStatus(
  - data class GlassmorphismConfig(

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/voiceuielements/theme/VoiceUITheme.kt`

**Package:** `com.augmentalis.voiceuielements.theme`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun VoiceUITheme(

**Imports:**
  - import androidx.compose.foundation.isSystemInDarkTheme
  - import androidx.compose.material3.MaterialTheme
  - import androidx.compose.material3.darkColorScheme
  - import androidx.compose.material3.lightColorScheme
  - import androidx.compose.runtime.Composable
  - import androidx.compose.ui.graphics.Color

---

### File: `src/main/java/com/augmentalis/voiceuielements/utils/NumberToWordsConverter.kt`

**Package:** `com.augmentalis.voiceuielements.utils`

**Classes/Interfaces/Objects:**
  - object NumberToWordsConverter 

**Public Functions:**

**Imports:**
  - import java.util.*

---

### File: `src/test/java/com/augmentalis/voiceuielements/components/VoiceUIComponentsTest.kt`

**Package:** `com.augmentalis.voiceuielements.components`

**Classes/Interfaces/Objects:**
  - class VoiceUIComponentsTest 

**Public Functions:**

**Imports:**
  - import org.junit.Test
  - import kotlin.test.assertEquals
  - import kotlin.test.assertTrue

---

### File: `themes/arvision/GlassMorphism.kt`

**Package:** ``

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

## Summary

**Total Files:** 10

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
                                  voiceuielements
```

---

**Generated by:** VOS4 API Documentation Generator
**Timestamp:** 251019-2203
