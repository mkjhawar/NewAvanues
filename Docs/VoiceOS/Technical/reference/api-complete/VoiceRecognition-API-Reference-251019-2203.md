# VoiceRecognition - Complete API Reference

**Module Type:** apps
**Generated:** 2025-10-19 22:03:49 PDT
**Timestamp:** 251019-2203
**Location:** `modules/apps/VoiceRecognition`

---

## Overview

This document provides a complete file-by-file, class-by-class reference for the VoiceRecognition module.

## Files


### File: `src/androidTest/java/com/augmentalis/voicerecognition/integration/AidlCommunicationTest.kt`

**Package:** `com.augmentalis.voicerecognition.integration`

**Classes/Interfaces/Objects:**
  - class AidlCommunicationTest 

**Public Functions:**

**Imports:**
  - import android.content.ComponentName
  - import android.content.Context
  - import android.content.Intent
  - import android.content.ServiceConnection
  - import android.os.IBinder
  - import androidx.test.core.app.ApplicationProvider
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import com.augmentalis.voicerecognition.IRecognitionCallback
  - import com.augmentalis.voicerecognition.IVoiceRecognitionService
  - import com.augmentalis.voicerecognition.mocks.MockRecognitionCallback
  - ... and 10 more

---

### File: `src/androidTest/java/com/augmentalis/voicerecognition/mocks/MockRecognitionCallback.kt`

**Package:** `com.augmentalis.voicerecognition.mocks`

**Classes/Interfaces/Objects:**
  - class MockRecognitionCallback(

**Public Functions:**

**Imports:**
  - import android.util.Log
  - import com.augmentalis.voicerecognition.IRecognitionCallback
  - import java.util.concurrent.ConcurrentLinkedQueue
  - import java.util.concurrent.CountDownLatch
  - import java.util.concurrent.TimeUnit
  - import java.util.concurrent.atomic.AtomicBoolean
  - import java.util.concurrent.atomic.AtomicInteger
  - import java.util.concurrent.atomic.AtomicReference

---

### File: `src/androidTest/java/com/augmentalis/voicerecognition/service/ServiceBindingTest.kt`

**Package:** `com.augmentalis.voicerecognition.service`

**Classes/Interfaces/Objects:**
  - class ServiceBindingTest 

**Public Functions:**

**Imports:**
  - import android.content.ComponentName
  - import android.content.Context
  - import android.content.Intent
  - import android.content.ServiceConnection
  - import android.os.IBinder
  - import androidx.test.core.app.ApplicationProvider
  - import androidx.test.ext.junit.runners.AndroidJUnit4
  - import androidx.test.platform.app.InstrumentationRegistry
  - import com.augmentalis.voicerecognition.IRecognitionCallback
  - import com.augmentalis.voicerecognition.IVoiceRecognitionService
  - ... and 12 more

---

### File: `src/main/java/com/augmentalis/voicerecognition/client/VoiceRecognitionClient.kt`

**Package:** `com.augmentalis.voicerecognition.client`

**Classes/Interfaces/Objects:**
  - class VoiceRecognitionClient(private val context

**Public Functions:**

**Imports:**
  - import android.content.ComponentName
  - import android.content.Context
  - import android.content.Intent
  - import android.content.ServiceConnection
  - import android.os.IBinder
  - import android.util.Log
  - import com.augmentalis.voicerecognition.IRecognitionCallback
  - import com.augmentalis.voicerecognition.IVoiceRecognitionService
  - import com.augmentalis.voicerecognition.service.VoiceRecognitionService

---

### File: `src/main/java/com/augmentalis/voicerecognition/examples/AidlUsageExample.kt`

**Package:** `com.augmentalis.voicerecognition.examples`

**Classes/Interfaces/Objects:**
  - class AidlUsageExample(private val context

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.util.Log
  - import com.augmentalis.voicerecognition.client.VoiceRecognitionClient
  - import com.augmentalis.voicerecognition.service.VoiceRecognitionService

---

### File: `src/main/java/com/augmentalis/voicerecognition/MainActivity.kt`

**Package:** `com.augmentalis.voicerecognition`

**Classes/Interfaces/Objects:**
  - class MainActivity 

**Public Functions:**
  - fun VoiceRecognitionApp() 

**Imports:**
  - import android.Manifest
  - import android.os.Bundle
  - import android.widget.Toast
  - import androidx.activity.ComponentActivity
  - import androidx.activity.compose.setContent
  - import androidx.activity.result.contract.ActivityResultContracts
  - import androidx.compose.runtime.*
  - import androidx.lifecycle.viewmodel.compose.viewModel
  - import com.augmentalis.voicerecognition.ui.SpeechRecognitionScreen
  - import com.augmentalis.voicerecognition.viewmodel.SpeechViewModel
  - ... and 2 more

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

### File: `src/main/java/com/augmentalis/voicerecognition/service/ClientConnection.kt`

**Package:** `com.augmentalis.voicerecognition.service`

**Classes/Interfaces/Objects:**
  - data class ClientConnection(

**Public Functions:**

**Imports:**
  - import com.augmentalis.voicerecognition.IRecognitionCallback

---

### File: `src/main/java/com/augmentalis/voicerecognition/service/VoiceRecognitionService.kt`

**Package:** `com.augmentalis.voicerecognition.service`

**Classes/Interfaces/Objects:**
  - class VoiceRecognitionService 

**Public Functions:**

**Imports:**
  - import android.app.Service
  - import android.content.Context
  - import android.content.Intent
  - import android.os.IBinder
  - import android.os.RemoteCallbackList
  - import android.os.RemoteException
  - import android.util.Log
  - import com.augmentalis.speechrecognition.SpeechEngine
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechMode
  - ... and 9 more

---

### File: `src/main/java/com/augmentalis/voicerecognition/ui/ConfigurationScreen.kt`

**Package:** `com.augmentalis.voicerecognition.ui`

**Classes/Interfaces/Objects:**
  - data class SpeechConfigurationData(

**Public Functions:**
  - fun ConfigurationScreen(
  - fun ConfigPanel(
  - fun LanguageSelector(
  - fun ModeSelector(
  - fun ConfigSwitch(
  - fun ConfigSlider(

**Imports:**
  - import androidx.compose.animation.*
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.rememberScrollState
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.foundation.verticalScroll
  - import androidx.compose.material.icons.Icons
  - import androidx.compose.material.icons.automirrored.filled.ArrowBack
  - import androidx.compose.material.icons.filled.*
  - ... and 10 more

---

### File: `src/main/java/com/augmentalis/voicerecognition/ui/SpeechRecognitionScreen.kt`

**Package:** `com.augmentalis.voicerecognition.ui`

**Classes/Interfaces/Objects:**

**Public Functions:**
  - fun SpeechRecognitionScreen(
  - fun PermissionRequestCard(
  - fun EngineSelectionPanel(
  - fun EngineChip(
  - fun StatusPanel(
  - fun RecordingControlPanel(
  - fun TranscriptPanel(

**Imports:**
  - import androidx.compose.animation.*
  - import androidx.compose.animation.core.*
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.clickable
  - import androidx.compose.foundation.layout.*
  - import androidx.compose.foundation.rememberScrollState
  - import androidx.compose.foundation.shape.CircleShape
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.foundation.verticalScroll
  - import androidx.compose.material.icons.Icons
  - ... and 17 more

---

### File: `src/main/java/com/augmentalis/voicerecognition/ui/ThemeUtils.kt`

**Package:** `com.augmentalis.voicerecognition.ui`

**Classes/Interfaces/Objects:**
  - data class GlassMorphismConfig(
  - data class DepthLevel(val depth

**Public Functions:**
  - fun Modifier.glassMorphism(

**Imports:**
  - import androidx.compose.foundation.background
  - import androidx.compose.foundation.shape.RoundedCornerShape
  - import androidx.compose.ui.Modifier
  - import androidx.compose.ui.graphics.Color
  - import androidx.compose.ui.unit.Dp

---

### File: `src/main/java/com/augmentalis/voicerecognition/viewmodel/SpeechViewModel.kt`

**Package:** `com.augmentalis.voicerecognition.viewmodel`

**Classes/Interfaces/Objects:**
  - data class SpeechUiState(
  - class SpeechViewModel(application

**Public Functions:**

**Imports:**
  - import android.app.Application
  - import androidx.lifecycle.AndroidViewModel
  - import androidx.lifecycle.viewModelScope
  - import com.augmentalis.speechrecognition.SpeechEngine
  - import com.augmentalis.speechrecognition.SpeechConfig
  - import com.augmentalis.speechrecognition.SpeechMode
  - import com.augmentalis.voiceos.speech.api.RecognitionResult
  - import com.augmentalis.voiceos.speech.api.SpeechListenerManager
  - import com.augmentalis.voiceos.speech.engines.android.AndroidSTTEngine
  - import com.augmentalis.voiceos.speech.engines.vosk.VoskEngine
  - ... and 13 more

---

### File: `src/test/java/com/augmentalis/voicerecognition/service/EngineSelectionTest.kt`

**Package:** `com.augmentalis.voicerecognition.service`

**Classes/Interfaces/Objects:**
  - class EngineSelectionTest 
  - class FloatingEngineSelectorIntegrationTest 

**Public Functions:**

**Imports:**
  - import android.content.Context
  - import android.content.SharedPreferences
  - import com.augmentalis.voicerecognition.IVoiceRecognitionService
  - import com.augmentalis.voicerecognition.IRecognitionCallback
  - import io.mockk.*
  - import org.junit.Assert.*
  - import org.junit.Before
  - import org.junit.Test
  - import org.junit.runner.RunWith
  - import org.robolectric.RobolectricTestRunner
  - ... and 2 more

---

### File: `src/test/java/com/augmentalis/voicerecognition/service/VoiceRecognitionServiceTest.kt`

**Package:** `com.augmentalis.voicerecognition.service`

**Classes/Interfaces/Objects:**
  - class VoiceRecognitionServiceTest 

**Public Functions:**

**Imports:**
  - import android.content.Intent
  - import android.os.IBinder
  - import android.os.RemoteException
  - import com.augmentalis.speechrecognition.SpeechEngine
  - import com.augmentalis.voicerecognition.IRecognitionCallback
  - import com.augmentalis.voicerecognition.IVoiceRecognitionService
  - import io.mockk.*
  - import kotlinx.coroutines.ExperimentalCoroutinesApi
  - import kotlinx.coroutines.test.*
  - import org.junit.After
  - ... and 7 more

---

## Summary

**Total Files:** 15

**Module Structure:**
```
                  src
                    androidTest
                      java
                        com
                          augmentalis
                            voicerecognition
                              integration
                              mocks
                              service
                    main
                      aidl
                        com
                          augmentalis
                            voicerecognition
                      java
                        com
                          augmentalis
                            voicerecognition
                              client
                              examples
                              service
                              ui
                              viewmodel
                      res
                        mipmap-hdpi
                        values
                    test
                      java
                        com
                          augmentalis
                            voicerecognition
                              service
```

---

**Generated by:** VOS4 API Documentation Generator
**Timestamp:** 251019-2203
