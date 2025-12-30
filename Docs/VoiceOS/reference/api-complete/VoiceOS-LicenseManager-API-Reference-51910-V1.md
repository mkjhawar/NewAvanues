# LicenseManager - Complete API Reference

**Module Type:** managers
**Generated:** 2025-10-19 22:04:06 PDT
**Timestamp:** 251019-2203
**Location:** `modules/managers/LicenseManager`

---

## Overview

This document provides a complete file-by-file, class-by-class reference for the LicenseManager module.

## Files


### File: `src/androidTest/java/com/augmentalis/licensemanager/ui/LicenseManagerUITest.kt`

**Package:** `com.augmentalis.licensemanager.ui`

**Classes/Interfaces/Objects:**
  - class LicenseManagerUITest 

**Public Functions:**

**Imports:**
  - import androidx.compose.ui.test.*
  - import androidx.compose.ui.test.junit4.createComposeRule
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import androidx.test.platform.app.InstrumentationRegistry
  - import com.augmentalis.licensemanager.LicensingModule
  - import com.augmentalis.licensemanager.SubscriptionState
  - import org.junit.Rule
  - import org.junit.Test
  - import org.junit.runner.RunWith

---

### File: `src/main/java/com/augmentalis/licensemanager/LicensingModule.kt`

**Package:** `com.augmentalis.licensemanager`

**Classes/Interfaces/Objects:**
  - data class SubscriptionState(
  - class LicenseValidator 
  - data class ValidationResult(
  - data class ModuleCapabilities(
  - enum class MemoryImpact 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow
  - import kotlinx.coroutines.flow.asStateFlow
  - import java.util.*
  - import java.util.concurrent.TimeUnit

---

### File: `src/main/java/com/augmentalis/licensemanager/ui/GlassmorphismUtils.kt`

**Package:** `com.augmentalis.licensemanager.ui`

**Classes/Interfaces/Objects:**
  - data class GlassMorphismConfig(
  - object LicenseColors 
  - object LicenseGlassConfigs 

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

### File: `src/main/java/com/augmentalis/licensemanager/ui/LicenseManagerActivity.kt`

**Package:** `com.augmentalis.licensemanager.ui`

**Classes/Interfaces/Objects:**
  - class LicenseManagerActivity 

**Public Functions:**
  - fun LicenseManagerTheme(content: @Composable () -> Unit) 
  - fun LicenseManagerScreen(

**Imports:**
  - import android.content.Context
  - import android.content.Intent
  - import android.net.Uri
  - import android.os.Bundle
  - import androidx.activity.ComponentActivity
  - import androidx.activity.compose.setContent
  - import androidx.activity.viewModels
  - import androidx.compose.animation.*
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.background
  - ... and 30 more

---

### File: `src/main/java/com/augmentalis/licensemanager/ui/LicenseViewModel.kt`

**Package:** `com.augmentalis.licensemanager.ui`

**Classes/Interfaces/Objects:**
  - class LicenseViewModel(private val context
  - class LicenseViewModelFactory(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.Intent
  - import android.net.Uri
  - import android.util.Log
  - import androidx.lifecycle.LiveData
  - import androidx.lifecycle.MutableLiveData
  - import androidx.lifecycle.ViewModel
  - import androidx.lifecycle.ViewModelProvider
  - import androidx.lifecycle.viewModelScope
  - import com.augmentalis.licensemanager.LicensingModule
  - ... and 3 more

---

### File: `src/test/java/com/augmentalis/licensemanager/security/SecurityTest.kt`

**Package:** `com.augmentalis.licensemanager.security`

**Classes/Interfaces/Objects:**
  - class SecurityTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import androidx.test.core.app.ApplicationProvider
  - import com.augmentalis.licensemanager.*
  - import kotlinx.coroutines.test.runTest
  - import org.junit.Before
  - import org.junit.Test
  - import org.junit.runner.RunWith
  - import org.robolectric.RobolectricTestRunner
  - import org.robolectric.annotation.Config
  - ... and 2 more

---

### File: `src/test/java/com/augmentalis/licensemanager/ui/LicenseViewModelTest.kt`

**Package:** `com.augmentalis.licensemanager.ui`

**Classes/Interfaces/Objects:**
  - class LicenseViewModelTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import androidx.arch.core.executor.testing.InstantTaskExecutorRule
  - import androidx.lifecycle.Observer
  - import com.augmentalis.licensemanager.LicensingModule
  - import com.augmentalis.licensemanager.SubscriptionState
  - import kotlinx.coroutines.Dispatchers
  - import kotlinx.coroutines.ExperimentalCoroutinesApi
  - import kotlinx.coroutines.test.*
  - import org.junit.After
  - ... and 7 more

---

## Summary

**Total Files:** 7

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
                                  licensemanager
```

---

**Generated by:** VOS4 API Documentation Generator
**Timestamp:** 251019-2203
