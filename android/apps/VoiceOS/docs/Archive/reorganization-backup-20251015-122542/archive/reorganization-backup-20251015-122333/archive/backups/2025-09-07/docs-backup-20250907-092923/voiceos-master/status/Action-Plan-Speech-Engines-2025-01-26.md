# Speech Engine Action Plan
**Date:** 2025-01-26
**Priority:** 🔴 CRITICAL
**Time Estimate:** 2 hours total

## Summary of Findings

After comprehensive analysis comparing LegacyAvenue with VOS4:

### ✅ Working Correctly
- Vosk error handling fully functional
- AndroidSTT error handling fully functional  
- IMU system for VoiceCursor working
- Cursor smoothing/filtering implemented
- Settings UI properly connected

### ❌ Critical Issues Found
1. **Vivoka error listener not connected** - Errors don't propagate to AIDL clients
2. **No default engine setting** - Need Vivoka as default (per user request)
3. **No engine selection UI** - Need system setting to change engines

## Phased Correction Plan

### Phase 1: Fix Vivoka Error Listener (15 minutes)
**Priority:** 🔴 CRITICAL - Blocks production readiness

#### Files to Modify
- `/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/vivoka/VivokaEngine.kt`

#### Changes Required
```kotlin
// Line ~62: Add error listener storage
private var errorListener: OnSpeechErrorListener? = null

// Lines 330-333: Fix setter
fun setErrorListener(listener: OnSpeechErrorListener) {
    errorListener = listener
    Log.d(TAG, "Error listener registered")
}

// Lines 365-376: Invoke listener in onError
override fun onError(codeString: String?, message: String?) {
    Log.e(TAG, "VSDK error - Code: $codeString, Message: $message")
    performance.recordRecognition(System.currentTimeMillis(), null, 0f, false)
    
    // CRITICAL FIX
    errorListener?.invoke(
        "Vivoka SDK error [$codeString]: $message",
        codeString?.toIntOrNull() ?: 500
    )
    
    coroutineScope.launch {
        Log.e(TAG, "VSDK Error - Code: $codeString, Message: $message")
    }
}
```

### Phase 2: Set Vivoka as Default Engine (30 minutes)
**Priority:** 🟡 HIGH - User requirement

#### Files to Modify
- `/apps/VoiceRecognition/src/main/java/com/augmentalis/voicerecognition/service/VoiceRecognitionService.kt`

#### Changes Required
```kotlin
// Add to companion object
companion object {
    private const val TAG = "VoiceRecognitionService"
    private const val DEFAULT_ENGINE = "vivoka"  // NEW
    private const val PREFS_NAME = "voice_recognition_prefs"  // NEW
    private const val PREF_SELECTED_ENGINE = "selected_speech_engine"  // NEW
}

// Add preference management
private fun getSelectedEngine(): String {
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(PREF_SELECTED_ENGINE, DEFAULT_ENGINE) ?: DEFAULT_ENGINE
}

private fun saveSelectedEngine(engine: String) {
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(PREF_SELECTED_ENGINE, engine).apply()
}

// Modify startRecognition
override fun startRecognition(engine: String?, language: String, mode: Int): Boolean {
    val selectedEngine = engine ?: getSelectedEngine()
    Log.d(TAG, "Starting recognition with engine: $selectedEngine")
    
    // Save selection if explicitly provided
    if (engine != null) {
        saveSelectedEngine(engine)
    }
    
    val speechEngine = stringToSpeechEngine(selectedEngine)
    // ... rest of existing code
}
```

### Phase 3: Create Engine Selection UI (45 minutes)
**Priority:** 🟡 HIGH - System setting requirement

#### Files to Create/Modify
- `/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceaccessibility/ui/settings/EngineSelectionScreen.kt` (NEW)
- `/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceaccessibility/MainActivity.kt` (UPDATE)

#### New Settings Screen
```kotlin
@Composable
fun EngineSelectionScreen(
    navController: NavController,
    viewModel: VoiceRecognitionViewModel
) {
    val engines = listOf(
        EngineOption("vivoka", "Vivoka", "High accuracy, offline capable"),
        EngineOption("vosk", "Vosk", "Fully offline, lightweight"),
        EngineOption("android_stt", "Android STT", "Google's built-in recognition"),
        EngineOption("whisper", "Whisper", "OpenAI's Whisper model")
    )
    
    var selectedEngine by remember { 
        mutableStateOf(viewModel.getSelectedEngine()) 
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Speech Recognition Engine",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        engines.forEach { engine ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        selectedEngine = engine.id
                        viewModel.setSelectedEngine(engine.id)
                    },
                colors = if (selectedEngine == engine.id) {
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                } else {
                    CardDefaults.cardColors()
                }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedEngine == engine.id,
                        onClick = {
                            selectedEngine = engine.id
                            viewModel.setSelectedEngine(engine.id)
                        }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            engine.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            engine.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Info"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Vivoka is recommended for best accuracy and performance",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

data class EngineOption(
    val id: String,
    val name: String,
    val description: String
)
```

### Phase 4: Testing & Validation (30 minutes)
**Priority:** 🟡 HIGH - Ensure fixes work

#### Test Scenarios
1. **Vivoka Error Propagation**
   - Disconnect network during Vivoka init
   - Verify error callback received in client
   - Check error message format

2. **Default Engine Selection**
   - Clear app data
   - Start VoiceRecognition without specifying engine
   - Verify Vivoka is selected

3. **Engine Switching**
   - Change engine in settings
   - Verify new engine is used
   - Test persistence across app restarts

4. **AIDL Error Callbacks**
   - Trigger errors in each engine
   - Verify callbacks reach VoiceAccessibility
   - Check error codes and messages

## Success Metrics

### Must Have (Critical)
- ✅ Vivoka errors propagate to AIDL clients
- ✅ Vivoka is default engine
- ✅ Engine selection persists
- ✅ All engines handle errors correctly

### Should Have (Important)  
- ✅ Settings UI for engine selection
- ✅ Visual feedback for selected engine
- ✅ Engine descriptions in UI
- ✅ Recommendation for Vivoka

## Risk Assessment

- **Risk Level:** LOW - Simple additions, no architectural changes
- **Complexity:** MINIMAL - Mostly adding missing connections
- **Testing Required:** MODERATE - Need to verify error scenarios
- **User Impact:** HIGH - Fixes critical functionality gap

## Timeline

- Phase 1: 15 minutes (Vivoka fix)
- Phase 2: 30 minutes (Default engine)
- Phase 3: 45 minutes (Settings UI)
- Phase 4: 30 minutes (Testing)
- **Total:** 2 hours

## Next Steps

1. Get approval for this action plan
2. Implement Phase 1 (Critical fix)
3. Test Vivoka error propagation
4. Implement Phases 2-3 (Default & UI)
5. Complete testing suite
6. Update documentation
7. Commit with proper messages

---

**Ready for implementation upon approval.**