# VoiceKeyboard - Complete API Reference

**Module Type:** libraries
**Generated:** 2025-10-19 22:04:00 PDT
**Timestamp:** 251019-2203
**Location:** `modules/libraries/VoiceKeyboard`

---

## Overview

This document provides a complete file-by-file, class-by-class reference for the VoiceKeyboard module.

## Files


### File: `build/generated/data_binding_base_class_source_out/debug/out/com/augmentalis/voicekeyboard/databinding/ActivityKeyboardSettingsBinding.java`

**Package:** `com.augmentalis.voicekeyboard.databinding`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**
  - import android.view.LayoutInflater;
  - import android.view.View;
  - import android.view.ViewGroup;
  - import android.widget.FrameLayout;
  - import android.widget.LinearLayout;
  - import androidx.annotation.NonNull;
  - import androidx.annotation.Nullable;
  - import androidx.viewbinding.ViewBinding;
  - import androidx.viewbinding.ViewBindings;
  - import com.augmentalis.voicekeyboard.R;
  - ... and 3 more

---

### File: `build/generated/source/buildConfig/debug/com/augmentalis/voicekeyboard/BuildConfig.java`

**Package:** `com.augmentalis.voicekeyboard`

**Classes/Interfaces/Objects:**

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/voicekeyboard/di/KeyboardServiceContainer.kt`

**Package:** `com.augmentalis.voicekeyboard.di`

**Classes/Interfaces/Objects:**
  - class KeyboardServiceContainer(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voicekeyboard.interfaces.*
  - import com.augmentalis.voicekeyboard.services.*

---

### File: `src/main/java/com/augmentalis/voicekeyboard/gestures/GestureTypingHandler.kt`

**Package:** `com.augmentalis.voicekeyboard.gestures`

**Classes/Interfaces/Objects:**
  - class GestureTypingHandler(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.graphics.PointF
  - import android.util.Log
  - import kotlinx.coroutines.*
  - import kotlin.math.abs
  - import kotlin.math.sqrt

---

### File: `src/main/java/com/augmentalis/voicekeyboard/interfaces/DictationManager.kt`

**Package:** `com.augmentalis.voicekeyboard.interfaces`

**Classes/Interfaces/Objects:**
  - interface DictationManager 

**Public Functions:**

**Imports:**
  - import kotlinx.coroutines.flow.StateFlow

---

### File: `src/main/java/com/augmentalis/voicekeyboard/interfaces/GestureProcessor.kt`

**Package:** `com.augmentalis.voicekeyboard.interfaces`

**Classes/Interfaces/Objects:**
  - interface GestureProcessor 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/voicekeyboard/interfaces/InputProcessor.kt`

**Package:** `com.augmentalis.voicekeyboard.interfaces`

**Classes/Interfaces/Objects:**
  - interface InputProcessor 

**Public Functions:**

**Imports:**
  - import android.view.inputmethod.InputConnection

---

### File: `src/main/java/com/augmentalis/voicekeyboard/interfaces/KeyboardPreferencesManager.kt`

**Package:** `com.augmentalis.voicekeyboard.interfaces`

**Classes/Interfaces/Objects:**
  - interface KeyboardPreferencesManager 

**Public Functions:**

**Imports:**
  - import com.augmentalis.voicekeyboard.ui.KeyboardTheme

---

### File: `src/main/java/com/augmentalis/voicekeyboard/interfaces/VoiceInputListener.kt`

**Package:** `com.augmentalis.voicekeyboard.interfaces`

**Classes/Interfaces/Objects:**
  - interface VoiceInputListener 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/voicekeyboard/preferences/KeyboardPreferences.kt`

**Package:** `com.augmentalis.voicekeyboard.preferences`

**Classes/Interfaces/Objects:**
  - class KeyboardPreferences(context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import androidx.preference.PreferenceManager
  - import com.augmentalis.voicekeyboard.ui.KeyboardTheme
  - import com.augmentalis.voicekeyboard.utils.KeyboardConstants

---

### File: `src/main/java/com/augmentalis/voicekeyboard/preferences/KeyboardSettingsActivity.kt`

**Package:** `com.augmentalis.voicekeyboard.preferences`

**Classes/Interfaces/Objects:**
  - class KeyboardSettingsActivity 
  - class KeyboardSettingsFragment 

**Public Functions:**

**Imports:**
  - import android.os.Bundle
  - import androidx.activity.OnBackPressedCallback
  - import androidx.appcompat.app.AppCompatActivity
  - import androidx.preference.PreferenceFragmentCompat
  - import androidx.preference.ListPreference
  - import androidx.preference.SeekBarPreference
  - import androidx.preference.SwitchPreferenceCompat
  - import androidx.preference.EditTextPreference
  - import com.augmentalis.voicekeyboard.R

---

### File: `src/main/java/com/augmentalis/voicekeyboard/service/KeyboardBroadcastReceiver.kt`

**Package:** `com.augmentalis.voicekeyboard.service`

**Classes/Interfaces/Objects:**
  - class KeyboardBroadcastReceiver 

**Public Functions:**

**Imports:**
  - import android.content.BroadcastReceiver
  - import android.content.Context
  - import android.content.Intent
  - import android.util.Log
  - import com.augmentalis.voicekeyboard.utils.KeyboardConstants

---

### File: `src/main/java/com/augmentalis/voicekeyboard/service/VoiceKeyboardService.kt`

**Package:** `com.augmentalis.voicekeyboard.service`

**Classes/Interfaces/Objects:**
  - class VoiceKeyboardService 
  - interface KeyboardActionListener 
  - enum class KeyboardLayout 
  - enum class SwipeDirection 

**Public Functions:**

**Imports:**
  - import android.content.BroadcastReceiver
  - import android.content.Context
  - import android.content.Intent
  - import android.content.IntentFilter
  - import android.inputmethodservice.InputMethodService
  - import android.os.Build
  - import android.util.Log
  - import android.view.KeyEvent
  - import android.view.View
  - import android.view.inputmethod.EditorInfo
  - ... and 13 more

---

### File: `src/main/java/com/augmentalis/voicekeyboard/services/DictationService.kt`

**Package:** `com.augmentalis.voicekeyboard.services`

**Classes/Interfaces/Objects:**
  - class DictationService(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voicekeyboard.interfaces.DictationManager
  - import com.augmentalis.voicekeyboard.voice.DictationHandler
  - import kotlinx.coroutines.flow.StateFlow

---

### File: `src/main/java/com/augmentalis/voicekeyboard/services/GestureService.kt`

**Package:** `com.augmentalis.voicekeyboard.services`

**Classes/Interfaces/Objects:**
  - class GestureService(context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voicekeyboard.interfaces.GestureProcessor
  - import com.augmentalis.voicekeyboard.gestures.GestureTypingHandler

---

### File: `src/main/java/com/augmentalis/voicekeyboard/services/PreferencesService.kt`

**Package:** `com.augmentalis.voicekeyboard.services`

**Classes/Interfaces/Objects:**
  - class PreferencesService(context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voicekeyboard.interfaces.KeyboardPreferencesManager
  - import com.augmentalis.voicekeyboard.preferences.KeyboardPreferences
  - import com.augmentalis.voicekeyboard.ui.KeyboardTheme

---

### File: `src/main/java/com/augmentalis/voicekeyboard/services/TextInputService.kt`

**Package:** `com.augmentalis.voicekeyboard.services`

**Classes/Interfaces/Objects:**
  - class TextInputService 

**Public Functions:**

**Imports:**
  - import android.view.KeyEvent
  - import android.view.inputmethod.EditorInfo
  - import android.view.inputmethod.InputConnection
  - import com.augmentalis.voicekeyboard.interfaces.InputProcessor
  - import com.augmentalis.voicekeyboard.utils.KeyboardConstants

---

### File: `src/main/java/com/augmentalis/voicekeyboard/services/VoiceInputService.kt`

**Package:** `com.augmentalis.voicekeyboard.services`

**Classes/Interfaces/Objects:**
  - class VoiceInputService(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import com.augmentalis.voicekeyboard.interfaces.VoiceInputListener
  - import com.augmentalis.voicekeyboard.voice.VoiceInputHandler

---

### File: `src/main/java/com/augmentalis/voicekeyboard/ui/KeyboardView.kt`

**Package:** `com.augmentalis.voicekeyboard.ui`

**Classes/Interfaces/Objects:**
  - class KeyboardView @JvmOverloads constructor(
  - data class KeyInfo(
  - enum class KeyType 
  - enum class KeyboardTheme 

**Public Functions:**

**Imports:**
  - import android.annotation.SuppressLint
  - import android.content.Context
  - import android.graphics.Canvas
  - import android.graphics.Paint
  - import android.graphics.Rect
  - import android.graphics.RectF
  - import android.os.Handler
  - import android.os.Looper
  - import android.util.AttributeSet
  - import android.view.GestureDetector
  - ... and 7 more

---

### File: `src/main/java/com/augmentalis/voicekeyboard/utils/IMEUtil.kt`

**Package:** `com.augmentalis.voicekeyboard.utils`

**Classes/Interfaces/Objects:**
  - object IMEUtil 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.Intent
  - import android.text.TextUtils
  - import android.util.Log
  - import android.view.inputmethod.EditorInfo

---

### File: `src/main/java/com/augmentalis/voicekeyboard/utils/KeyboardConstants.kt`

**Package:** `com.augmentalis.voicekeyboard.utils`

**Classes/Interfaces/Objects:**
  - object KeyboardConstants 
  - object KeyboardActions 

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/voicekeyboard/utils/ModifierKeyState.kt`

**Package:** `com.augmentalis.voicekeyboard.utils`

**Classes/Interfaces/Objects:**
  - class ModifierKeyState(private val supportsLocked

**Public Functions:**

**Imports:**

---

### File: `src/main/java/com/augmentalis/voicekeyboard/voice/DictationHandler.kt`

**Package:** `com.augmentalis.voicekeyboard.voice`

**Classes/Interfaces/Objects:**
  - class DictationHandler(

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.os.CountDownTimer
  - import android.util.Log
  - import com.augmentalis.voicekeyboard.utils.IMEUtil
  - import com.augmentalis.voicekeyboard.utils.KeyboardConstants
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.MutableStateFlow
  - import kotlinx.coroutines.flow.StateFlow

---

### File: `src/main/java/com/augmentalis/voicekeyboard/voice/VoiceInputHandler.kt`

**Package:** `com.augmentalis.voicekeyboard.voice`

**Classes/Interfaces/Objects:**
  - class VoiceInputHandler(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.Intent
  - import android.os.Bundle
  - import android.speech.RecognitionListener
  - import android.speech.RecognizerIntent
  - import android.speech.SpeechRecognizer
  - import android.util.Log
  - import com.augmentalis.voicekeyboard.utils.KeyboardConstants
  - import kotlinx.coroutines.*
  - import kotlinx.coroutines.flow.MutableStateFlow
  - ... and 2 more

---

## Summary

**Total Files:** 24

**Module Structure:**
```
                  build
                    generated
                      ap_generated_sources
                        debug
                          out
                      data_binding_base_class_source_out
                        debug
                          out
                            com
                              augmentalis
                                voicekeyboard
                                  databinding
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
                                voicekeyboard
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
```

---

**Generated by:** VOS4 API Documentation Generator
**Timestamp:** 251019-2203
