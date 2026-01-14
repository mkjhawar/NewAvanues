# VOS4 Complete Step-by-Step Implementation Guide

**Document:** Complete-Implementation-Guide-251009-0338.md
**Created:** 2025-10-09 03:38:00 PDT
**Purpose:** Comprehensive guide for completing all remaining VOS4 implementation work
**Status**: Week 1 + HILT infrastructure complete, Week 2-4 remaining

---

## 📊 Implementation Overview

**Total Remaining Work**: ~205 hours across Week 2-4
- **Week 2 Remaining**: 29 hours (remote logging, VOSK integration, UI overlays)
- **Week 3**: 40 hours (high-priority stubs: VoiceAccessibility, LearnApp, DeviceManager)
- **Week 4+**: ~136 hours (CommandManager enhancements, VoiceKeyboard, polish)

---

## ✅ What's Already Complete (42 hours)

### Week 1 - COMPLETE ✅
1. ✅ Real-Time Confidence Scoring (CONF-1, CONF-2, CONF-3) - 15h
2. ✅ Similarity Matching Algorithms (SIM-1, SIM-2) - 8h
3. ✅ HILT DI Foundation (DI-1, DI-2) - 7h
4. ✅ VoiceOsLogger Core (LOG-1) - 4h
5. ✅ VOSK Engine Verification - Discovered existing implementation

### Week 2 HILT - COMPLETE ✅
1. ✅ HILT AccessibilityModule (DI-3) - 3h
2. ✅ HILT DataModule (DI-4) - 3h
3. ✅ HILT ManagerModule (DI-5) - 2h

**Build Status**: ✅ BUILD SUCCESSFUL in 3s (app:compileDebugKotlin)

---

## 🔴 WEEK 2 REMAINING (29 hours)

### Task 1: VoiceOsLogger Remote Logging (5 hours)

**Priority**: HIGH
**Dependencies**: VoiceOsLogger core (already complete)
**Agent Type**: general-purpose

#### Step 1.1: Firebase Crashlytics Integration (2 hours)

**Create File**: `/Volumes/M Drive/Coding/vos4/modules/libraries/VoiceOsLogger/src/main/java/com/augmentalis/logger/remote/FirebaseLogger.kt`

**Implementation**:
```kotlin
package com.augmentalis.logger.remote

import android.util.Log
import com.augmentalis.logger.VoiceOsLogger

/**
 * Firebase Crashlytics integration for remote logging
 * Currently a stub - will be wired when Firebase SDK is added
 */
class FirebaseLogger {

    private var isEnabled = false

    fun enable() {
        isEnabled = true
        Log.d(TAG, "Firebase remote logging enabled")
    }

    fun disable() {
        isEnabled = false
        Log.d(TAG, "Firebase remote logging disabled")
    }

    fun log(
        level: VoiceOsLogger.Level,
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        if (!isEnabled) return
        if (level.priority < VoiceOsLogger.Level.WARN.priority) return

        try {
            // TODO: Uncomment when Firebase is added
            // val logMessage = "[$tag] $message"
            // if (throwable != null) {
            //     FirebaseCrashlytics.getInstance().recordException(throwable)
            //     FirebaseCrashlytics.getInstance().log(logMessage)
            // } else {
            //     FirebaseCrashlytics.getInstance().log(logMessage)
            // }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send log to Firebase", e)
        }
    }

    fun setUserId(userId: String) {
        try {
            // FirebaseCrashlytics.getInstance().setUserId(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Firebase user ID", e)
        }
    }

    companion object {
        private const val TAG = "FirebaseLogger"
    }
}
```

#### Step 1.2: Custom Remote Endpoint (1.5 hours)

**Create File**: `/Volumes/M Drive/Coding/vos4/modules/libraries/VoiceOsLogger/src/main/java/com/augmentalis/logger/remote/RemoteLogSender.kt`

**Implementation**:
```kotlin
package com.augmentalis.logger.remote

import android.util.Log
import com.augmentalis.logger.VoiceOsLogger
import kotlinx.coroutines.*
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.ConcurrentLinkedQueue

class RemoteLogSender(
    private val endpoint: String,
    private val apiKey: String
) {

    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isEnabled = false

    data class LogEntry(
        val timestamp: Long,
        val level: String,
        val tag: String,
        val message: String,
        val stackTrace: String? = null
    )

    fun enable() {
        isEnabled = true
        startBatchSender()
    }

    fun disable() {
        isEnabled = false
        scope.cancel()
    }

    fun queueLog(level: VoiceOsLogger.Level, tag: String, message: String, throwable: Throwable? = null) {
        if (!isEnabled) return

        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level.name,
            tag = tag,
            message = message,
            stackTrace = throwable?.stackTraceToString()
        )

        logQueue.offer(entry)

        // Send immediately if critical error
        if (level == VoiceOsLogger.Level.ERROR && throwable != null) {
            scope.launch { sendBatch(listOf(entry)) }
        }
    }

    private fun startBatchSender() {
        scope.launch {
            while (isActive) {
                delay(30000) // Send every 30 seconds

                val batch = mutableListOf<LogEntry>()
                while (logQueue.isNotEmpty() && batch.size < 100) {
                    logQueue.poll()?.let { batch.add(it) }
                }

                if (batch.isNotEmpty()) {
                    sendBatch(batch)
                }
            }
        }
    }

    private suspend fun sendBatch(logs: List<LogEntry>) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(endpoint)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.doOutput = true

                val jsonArray = JSONArray()
                logs.forEach { entry ->
                    jsonArray.put(JSONObject().apply {
                        put("timestamp", entry.timestamp)
                        put("level", entry.level)
                        put("tag", entry.tag)
                        put("message", entry.message)
                        entry.stackTrace?.let { put("stackTrace", it) }
                    })
                }

                val payload = JSONObject().apply {
                    put("logs", jsonArray)
                    put("device_id", "stub_device_id")
                    put("app_version", "3.0.0")
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(payload.toString())
                    it.flush()
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Sent ${logs.size} logs successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send logs", e)
            }
        }
    }

    companion object {
        private const val TAG = "RemoteLogSender"
    }
}
```

#### Step 1.3: Update VoiceOsLogger.kt (1.5 hours)

**Modify File**: `/Volumes/M Drive/Coding/vos4/modules/libraries/VoiceOsLogger/src/main/java/com/augmentalis/logger/VoiceOsLogger.kt`

**Add**:
```kotlin
import com.augmentalis.logger.remote.FirebaseLogger
import com.augmentalis.logger.remote.RemoteLogSender

object VoiceOsLogger {
    // ... existing code ...

    private var firebaseLogger: FirebaseLogger? = null
    private var remoteLogSender: RemoteLogSender? = null

    fun enableFirebaseLogging() {
        firebaseLogger = FirebaseLogger().apply { enable() }
        d(TAG, "Firebase logging enabled")
    }

    fun enableRemoteLogging(endpoint: String, apiKey: String) {
        remoteLogSender = RemoteLogSender(endpoint, apiKey).apply { enable() }
        d(TAG, "Remote logging enabled: $endpoint")
    }

    fun disableRemoteLogging() {
        firebaseLogger?.disable()
        remoteLogSender?.disable()
        firebaseLogger = null
        remoteLogSender = null
    }

    fun setUserId(userId: String) {
        firebaseLogger?.setUserId(userId)
    }

    // Update log method to also send remote
    private fun log(level: Level, tag: String, message: String, throwable: Throwable? = null) {
        logToAndroid(level, tag, message, throwable)
        logToFile(level, tag, message, throwable)

        // Remote logging
        firebaseLogger?.log(level, tag, message, throwable)
        remoteLogSender?.queueLog(level, tag, message, throwable)
    }
}
```

**Update build.gradle.kts**: Add JSON dependency
```kotlin
dependencies {
    implementation("org.json:json:20230227")
}
```

**Test**: Run `./gradlew :modules:libraries:VoiceOsLogger:assemble`

**Deliverables**:
- FirebaseLogger.kt (stub ready for Firebase SDK)
- RemoteLogSender.kt (working custom endpoint)
- Updated VoiceOsLogger.kt with remote logging
- Build verification passing

---

### Task 2: VOSK Engine Integration (12 hours)

**Priority**: HIGH
**Dependencies**: SimilarityMatcher.kt, ConfidenceScorer.kt (both complete)
**Agent Type**: general-purpose

#### Step 2.1: SimilarityMatcher Integration (4 hours)

**Modify File**: `/Volumes/M Drive/Coding/vos4/modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/vosk/VoskRecognizer.kt`

**Add**:
```kotlin
import com.augmentalis.voiceos.speech.utils.SimilarityMatcher

class VoskRecognizer(/* ... */) {

    private enum class MatchType {
        EXACT,   // Perfect grammar match
        FUZZY,   // Similar match via fuzzy matching
        NONE     // No match found
    }

    private fun findBestGrammarMatch(
        recognizedText: String,
        grammarCommands: List<String>
    ): Pair<String, Float>? {
        return SimilarityMatcher.findMostSimilarWithConfidence(
            input = recognizedText,
            commands = grammarCommands,
            threshold = 0.70f
        )
    }

    private fun processRecognitionResult(result: String, grammar: List<String>): RecognitionMatch {
        // 1. Try exact match
        if (result in grammar) {
            return RecognitionMatch(
                text = result,
                confidence = 0.95f,
                matchType = MatchType.EXACT
            )
        }

        // 2. Try fuzzy match
        val fuzzyMatch = findBestGrammarMatch(result, grammar)
        if (fuzzyMatch != null) {
            val (command, similarity) = fuzzyMatch
            Log.i(TAG, "Fuzzy match: '$result' → '$command' (${(similarity * 100).toInt()}%)")
            return RecognitionMatch(
                text = command,
                confidence = similarity,
                matchType = MatchType.FUZZY
            )
        }

        // 3. No match - return as-is with low confidence
        return RecognitionMatch(
            text = result,
            confidence = 0.40f,
            matchType = MatchType.NONE
        )
    }

    companion object {
        private const val TAG = "VoskRecognizer"
    }
}
```

#### Step 2.2: Enhanced Confidence Scoring (4 hours)

**Modify File**: `/Volumes/M Drive/Coding/vos4/modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/vosk/VoskRecognizer.kt`

**Add**:
```kotlin
import com.augmentalis.voiceos.speech.confidence.ConfidenceScorer
import com.augmentalis.voiceos.speech.confidence.ConfidenceResult
import com.augmentalis.voiceos.speech.confidence.ConfidenceLevel
import com.augmentalis.voiceos.speech.confidence.RecognitionEngine

class VoskRecognizer(/* ... */) {

    private val confidenceScorer = ConfidenceScorer()

    private fun calculateEnhancedConfidence(
        voskRawConfidence: Float,
        recognitionMatch: RecognitionMatch
    ): ConfidenceResult {
        // VOSK returns log-probability, normalize it
        val normalizedConfidence = confidenceScorer.normalizeConfidence(
            rawScore = voskRawConfidence,
            engine = RecognitionEngine.VOSK
        )

        // Adjust confidence based on match type
        val adjustedConfidence = when (recognitionMatch.matchType) {
            MatchType.EXACT -> minOf(normalizedConfidence + 0.15f, 1.0f) // Boost exact matches
            MatchType.FUZZY -> normalizedConfidence * recognitionMatch.confidence // Apply fuzzy similarity
            MatchType.NONE -> normalizedConfidence * 0.5f // Penalize no match
        }

        val level = confidenceScorer.getConfidenceLevel(adjustedConfidence)

        return ConfidenceResult(
            text = recognitionMatch.text,
            confidence = adjustedConfidence,
            level = level,
            alternates = emptyList(), // VOSK doesn't provide n-best yet
            scoringMethod = ScoringMethod.VOSK_ENHANCED
        )
    }
}
```

**Modify File**: `/Volumes/M Drive/Coding/vos4/modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/vosk/VoskEngine.kt`

**Update**: Return `ConfidenceResult` instead of raw confidence

#### Step 2.3: Testing & Verification (4 hours)

**Create File**: `/Volumes/M Drive/Coding/vos4/modules/libraries/SpeechRecognition/src/test/java/com/augmentalis/voiceos/speech/engines/vosk/VoskIntegrationTest.kt`

**Implementation**:
```kotlin
package com.augmentalis.voiceos.speech.engines.vosk

import com.augmentalis.voiceos.speech.confidence.ConfidenceLevel
import org.junit.Test
import org.junit.Assert.*

class VoskIntegrationTest {

    @Test
    fun `fuzzy matching works with typos`() {
        val grammar = listOf("open calculator", "open calendar", "open camera")
        val recognized = "opn calcluator"

        val matcher = VoskRecognizer.createMatcher()
        val result = matcher.processRecognitionResult(recognized, grammar)

        assertEquals("open calculator", result.text)
        assertEquals(VoskRecognizer.MatchType.FUZZY, result.matchType)
        assertTrue(result.confidence > 0.70f)
    }

    @Test
    fun `confidence scoring applies thresholds`() {
        val scorer = ConfidenceScorer()

        assertEquals(ConfidenceLevel.HIGH, scorer.getConfidenceLevel(0.92f))
        assertEquals(ConfidenceLevel.MEDIUM, scorer.getConfidenceLevel(0.78f))
        assertEquals(ConfidenceLevel.LOW, scorer.getConfidenceLevel(0.62f))
        assertEquals(ConfidenceLevel.REJECT, scorer.getConfidenceLevel(0.35f))
    }

    @Test
    fun `exact matches get confidence boost`() {
        val grammar = listOf("open calculator")
        val recognized = "open calculator"

        val matcher = VoskRecognizer.createMatcher()
        val result = matcher.processRecognitionResult(recognized, grammar)

        assertTrue(result.confidence > 0.90f)
        assertEquals(VoskRecognizer.MatchType.EXACT, result.matchType)
    }

    @Test
    fun `no match returns low confidence`() {
        val grammar = listOf("open calculator")
        val recognized = "asdfghjkl"

        val matcher = VoskRecognizer.createMatcher()
        val result = matcher.processRecognitionResult(recognized, grammar)

        assertTrue(result.confidence < 0.50f)
        assertEquals(VoskRecognizer.MatchType.NONE, result.matchType)
    }
}
```

**Run Tests**: `./gradlew :modules:libraries:SpeechRecognition:test`

**Create Documentation**: `/Volumes/M Drive/Coding/vos4/docs/modules/speech-recognition/implementation/VOSK-Integration-Report.md`

**Deliverables**:
- VoskRecognizer.kt with fuzzy matching
- VoskEngine.kt with enhanced confidence
- Comprehensive unit tests (all passing)
- Integration documentation
- Build verification

---

### Task 3: UI Overlay Stubs (12 hours)

**Priority**: HIGH
**Dependencies**: None (standalone UI components)
**Agent Type**: general-purpose

#### Step 3.1: ConfidenceOverlay (3 hours)

**Create File**: `/Volumes/M Drive/Coding/vos4/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/overlays/ConfidenceOverlay.kt`

**Implementation**:
```kotlin
package com.augmentalis.voiceos.accessibility.overlays

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceos.speech.confidence.ConfidenceLevel
import com.augmentalis.voiceos.speech.confidence.ConfidenceResult

class ConfidenceOverlay(
    private val context: Context,
    private val windowManager: WindowManager
) {

    private var overlayView: ComposeView? = null
    private var isShowing = false
    private var currentConfidence by mutableStateOf(0f)
    private var currentLevel by mutableStateOf(ConfidenceLevel.HIGH)
    private var currentText by mutableStateOf("")

    fun show(confidenceResult: ConfidenceResult) {
        if (overlayView == null) {
            overlayView = createOverlayView()
        }

        if (!isShowing) {
            try {
                windowManager.addView(overlayView, createLayoutParams())
                isShowing = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show overlay", e)
            }
        }

        updateConfidence(confidenceResult)
    }

    fun hide() {
        overlayView?.let {
            if (isShowing) {
                try {
                    windowManager.removeView(it)
                    isShowing = false
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to hide overlay", e)
                }
            }
        }
    }

    private fun createOverlayView(): ComposeView {
        return ComposeView(context).apply {
            setContent {
                ConfidenceIndicatorUI(
                    confidence = currentConfidence,
                    level = currentLevel,
                    text = currentText
                )
            }
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 16
        }
    }

    private fun updateConfidence(result: ConfidenceResult) {
        currentConfidence = result.confidence
        currentLevel = result.level
        currentText = result.text
    }

    companion object {
        private const val TAG = "ConfidenceOverlay"
    }
}

@Composable
private fun ConfidenceIndicatorUI(
    confidence: Float,
    level: ConfidenceLevel,
    text: String
) {
    val color = when (level) {
        ConfidenceLevel.HIGH -> Color.Green
        ConfidenceLevel.MEDIUM -> Color(0xFFFFEB3B)
        ConfidenceLevel.LOW -> Color(0xFFFF9800)
        ConfidenceLevel.REJECT -> Color.Red
    }

    Row(
        modifier = Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "${(confidence * 100).toInt()}%",
                color = color,
                fontSize = 16.sp
            )
            if (text.isNotEmpty()) {
                Text(text = text, fontSize = 12.sp)
            }
        }
    }
}
```

#### Step 3.2: NumberedSelectionOverlay (3 hours)

**Create File**: `/Volumes/M Drive/Coding/vos4/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/overlays/NumberedSelectionOverlay.kt`

**Similar implementation with numbered badges**

#### Step 3.3: CommandStatusOverlay (3 hours)

**Create File**: `/Volumes/M Drive/Coding/vos4/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/overlays/CommandStatusOverlay.kt`

**Implementation showing command processing state**

#### Step 3.4: ContextMenuOverlay (3 hours)

**Create File**: `/Volumes/M Drive/Coding/vos4/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/overlays/ContextMenuOverlay.kt`

**Implementation for voice-activated context menus**

**Verify Build**: `./gradlew :modules:apps:VoiceAccessibility:assemble -x test`

**Deliverables**:
- 4 overlay implementations
- OverlayManager (centralized control)
- Basic unit tests
- Documentation

---

## 🟠 WEEK 3 (40 hours)

### Task Group A: VoiceAccessibility Cursor Integration (18 hours)

**11 stubs to implement**

#### VoiceAccessibility Stubs:

1. **Cursor Position Tracking** (2h)
   - File: `VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/cursor/CursorPositionTracker.kt`
   - Purpose: Track voice cursor position across all apps

2. **Cursor Visibility Manager** (2h)
   - File: `VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/cursor/CursorVisibilityManager.kt`
   - Purpose: Show/hide cursor based on interaction mode

3. **Cursor Style Manager** (1.5h)
   - File: `VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/cursor/CursorStyleManager.kt`
   - Purpose: Different cursor styles (normal, selection, click)

4. **Voice Cursor Event Handler** (2h)
   - File: `VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/cursor/VoiceCursorEventHandler.kt`
   - Purpose: Handle voice cursor events

5. **Cursor Gesture Integration** (2h)
   - File: `VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/cursor/CursorGestureHandler.kt`
   - Purpose: Gesture dispatch via cursor

6. **Cursor Boundary Detection** (1.5h)
   - File: `VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/cursor/BoundaryDetector.kt`
   - Purpose: Prevent cursor from leaving screen bounds

7. **Cursor Speed Control** (1.5h)
   - File: `VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/cursor/SpeedController.kt`
   - Purpose: Adjust cursor speed based on voice commands

8. **Cursor Snap-to-Element** (2h)
   - File: `VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/cursor/SnapToElementHandler.kt`
   - Purpose: Snap cursor to nearest clickable element

9. **Cursor History Tracking** (1.5h)
   - File: `VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/cursor/CursorHistoryTracker.kt`
   - Purpose: Track cursor positions for "go back" commands

10. **Cursor Focus Indicator** (1.5h)
    - File: `VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/cursor/FocusIndicator.kt`
    - Purpose: Highlight focused element

11. **Cursor Command Mapper** (1.5h)
    - File: `VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/cursor/CommandMapper.kt`
    - Purpose: Map voice commands to cursor actions

**Total**: 18 hours

---

### Task Group B: LearnApp Completion (12 hours)

**7 stubs to implement**

#### LearnApp Stubs:

1. **App Hash Calculation** (2h)
   - File: `LearnApp/src/main/java/com/augmentalis/learnapp/hash/AppHashCalculator.kt`
   - Purpose: Generate unique fingerprint for apps

2. **Version Info Integration** (1.5h)
   - File: `LearnApp/src/main/java/com/augmentalis/learnapp/version/VersionInfoProvider.kt`
   - Purpose: Get app version from PackageManager

3. **Login Prompt Overlay** (2h)
   - File: `LearnApp/src/main/java/com/augmentalis/learnapp/overlays/LoginPromptOverlay.kt`
   - Purpose: Show login prompt during exploration

4. **App State Detection** (2h)
   - File: `LearnApp/src/main/java/com/augmentalis/learnapp/state/AppStateDetector.kt`
   - Purpose: Detect app states (login, loading, error, ready)

5. **Element Interaction Recorder** (2h)
   - File: `LearnApp/src/main/java/com/augmentalis/learnapp/recording/InteractionRecorder.kt`
   - Purpose: Record UI element interactions

6. **Voice Command Generator** (1.5h)
   - File: `LearnApp/src/main/java/com/augmentalis/learnapp/generation/CommandGenerator.kt`
   - Purpose: Generate voice commands from learned UI

7. **Exploration Progress Tracker** (1h)
   - File: `LearnApp/src/main/java/com/augmentalis/learnapp/tracking/ProgressTracker.kt`
   - Purpose: Track exploration progress

**Total**: 12 hours

---

### Task Group C: DeviceManager Features (14 hours)

**7 stubs to implement**

#### DeviceManager Stubs:

1. **UWB Support Detection** (2h)
   - File: `DeviceManager/src/main/java/com/augmentalis/devicemanager/uwb/UWBDetector.kt`
   - Purpose: Detect Ultra-Wideband capability

2. **IMU Public Methods** (2h)
   - File: `DeviceManager/src/main/java/com/augmentalis/devicemanager/imu/IMUPublicAPI.kt`
   - Purpose: Public API for IMU (startTracking, stopTracking, getCurrentOrientation)

3. **Bluetooth Public Methods** (2h)
   - File: `DeviceManager/src/main/java/com/augmentalis/devicemanager/bluetooth/BluetoothPublicAPI.kt`
   - Purpose: Public API for Bluetooth (isEnabled, getConnectedDevices)

4. **WiFi Public Methods** (2h)
   - File: `DeviceManager/src/main/java/com/augmentalis/devicemanager/wifi/WiFiPublicAPI.kt`
   - Purpose: Public API for WiFi (isEnabled, getConnectedNetwork)

5. **Device Capability Query** (2h)
   - File: `DeviceManager/src/main/java/com/augmentalis/devicemanager/capabilities/CapabilityQuery.kt`
   - Purpose: Query all device capabilities at once

6. **Sensor Fusion Manager** (2h)
   - File: `DeviceManager/src/main/java/com/augmentalis/devicemanager/sensors/SensorFusionManager.kt`
   - Purpose: Combine data from multiple sensors

7. **Hardware Profile Creator** (2h)
   - File: `DeviceManager/src/main/java/com/augmentalis/devicemanager/profile/HardwareProfiler.kt`
   - Purpose: Create hardware profile for device

**Total**: 14 hours

---

## 🟡 WEEK 4+ (136 hours)

### CommandManager Dynamic Features (38 hours)

1. **Dynamic Command Registration** (8h)
   - Runtime command addition/removal
   - Priority-based command resolution
   - Command conflict detection

2. **Custom Command Editor** (10h)
   - UI for creating custom commands
   - Command testing interface
   - Import/export commands

3. **Command Macros** (8h)
   - Multi-step command sequences
   - Conditional command execution
   - Variable support in commands

4. **Context-Aware Commands** (12h)
   - App-specific command activation
   - Screen-state-based commands
   - Time/location-based commands

### VoiceKeyboard Polish (17 stubs, 34 hours)

1. **Dictation Mode** (6h)
2. **Voice Shortcuts** (4h)
3. **Auto-Punctuation** (4h)
4. **Multi-Language Switching** (4h)
5. **Custom Dictionary** (4h)
6. **Voice Emoji Input** (3h)
7. **Speech-to-Text Formatting** (4h)
8. **Keyboard Layout Optimization** (5h)

### Google Cloud Speech Engine (28 hours)

1. **Google Cloud API Integration** (10h)
2. **Streaming Recognition** (8h)
3. **Confidence Scoring Integration** (4h)
4. **Language Model Selection** (3h)
5. **Testing & Verification** (3h)

### Polish & Optimization (36 hours)

1. **Performance Profiling** (8h)
2. **Memory Optimization** (8h)
3. **Battery Usage Optimization** (6h)
4. **UI/UX Polish** (8h)
5. **Documentation Completion** (6h)

---

## 🎯 Implementation Order (Recommended)

### Session 1: Complete Week 2 (29 hours)
1. Deploy agent for VoiceOsLogger remote logging (5h)
2. Deploy agent for VOSK integration (12h)
3. Deploy agent for UI overlays (12h)
4. Verify builds after each agent completes

### Session 2: Week 3 Part 1 (20 hours)
1. Deploy agent for VoiceAccessibility stubs 1-6 (11h)
2. Deploy agent for LearnApp stubs 1-4 (7.5h)
3. Verify integration

### Session 3: Week 3 Part 2 (20 hours)
1. Complete VoiceAccessibility stubs 7-11 (7h)
2. Complete LearnApp stubs 5-7 (4.5h)
3. Deploy agent for DeviceManager stubs (14h)
4. Integration testing

### Session 4: Week 4 CommandManager (38 hours)
1. Dynamic command registration (8h)
2. Custom command editor (10h)
3. Command macros (8h)
4. Context-aware commands (12h)

### Session 5: Week 4 VoiceKeyboard (34 hours)
1. Implement 17 VoiceKeyboard stubs in batches
2. Integration testing
3. Polish

### Session 6: Google Cloud + Polish (64 hours)
1. Google Cloud Speech Engine (28h)
2. Final polish & optimization (36h)

---

## 🔧 Build Commands Reference

### Individual Module Builds
```bash
# VoiceOsLogger
./gradlew :modules:libraries:VoiceOsLogger:assemble

# SpeechRecognition (VOSK)
./gradlew :modules:libraries:SpeechRecognition:assemble

# VoiceAccessibility
./gradlew :modules:apps:VoiceAccessibility:assemble -x test

# CommandManager
./gradlew :modules:managers:CommandManager:assemble -x test

# Full app (may fail due to AAR issue)
./gradlew :app:compileDebugKotlin
```

### Run Tests
```bash
# SimilarityMatcher tests
./gradlew :modules:libraries:SpeechRecognition:test --tests="*SimilarityMatcherTest*"

# VOSK integration tests
./gradlew :modules:libraries:SpeechRecognition:test --tests="*VoskIntegrationTest*"

# All tests
./gradlew test
```

---

## 📝 Agent Deployment Template

**When starting a new session, use this template:**

```markdown
I'm continuing VOS4 implementation. Current status:
- Week 1: ✅ Complete (42 hours)
- Week 2 HILT: ✅ Complete (8 hours)
- Week 2 Remaining: [specify what to do]

Please deploy specialized agents for:
1. [Task name] - [hours] - [files to create/modify]
2. [Task name] - [hours] - [files to create/modify]
3. [Task name] - [hours] - [files to create/modify]

Reference: /Volumes/M Drive/Coding/vos4/coding/TODO/Complete-Implementation-Guide-251009-0338.md

Deploy agents in parallel.
```

---

## ✅ Success Criteria

### Week 2 Complete When:
- ✅ VoiceOsLogger has remote logging (Firebase stub + custom endpoint)
- ✅ VOSK has fuzzy matching and enhanced confidence
- ✅ 4 UI overlays implemented and working
- ✅ All builds passing (0 errors)

### Week 3 Complete When:
- ✅ All 11 VoiceAccessibility cursor stubs working
- ✅ All 7 LearnApp stubs complete
- ✅ All 7 DeviceManager stubs complete
- ✅ Full integration testing passed

### Week 4+ Complete When:
- ✅ CommandManager dynamic features working
- ✅ All 17 VoiceKeyboard stubs complete
- ✅ Google Cloud Speech integrated
- ✅ Performance optimization complete
- ✅ Ready for beta testing

---

**Document Status**: ✅ Complete implementation guide
**Next Session**: Start with Week 2 remaining tasks (29 hours)
**Last Updated**: 2025-10-09 03:38:00 PDT
**Reference**: Keep this document for all future implementation sessions
