# VoiceOS TODO LIST

### Task 1. Vos4/App module (Main)

##### 1.1 - Setup Wizard Permission Screen

- **Issue:** The issue is in OnboardingActivity.kt, inside the Compose function PermissionsStep. Currently, if the user has already granted the permission, the UI does not update the status. Instead, it still shows the "request permission" screen. When the permission is granted, the UI should update and display the "granted" status, instead of asking for permission again.
- **Files:** `OnboardingActivity.kt` → `PermissionsStep`

##### 1.2 - Setup Wizard Voice Calibration Integration

- **Issue:** - In Voice Calibration step inside Setup Wizard, we have UI for Voice Calibration to calibrate VoiceOS to recognize your voice clearly and we have few commands like Say: "Hello VoiceOS", Say: "Go back", Say: "Open settings". But there is stub implementation for VoiceOS SpeechRecognition module. So here need to implement the SpeechRecognition with selected voice engine e.g. VIVOKA/AndroidSpeech/VOSK etc. 
- **Files:** `OnboardingActivity.kt` → `VoiceCalibrationStep`

##### 1.3 Setup Complete Screen Scroll Issue

- **Issue:** The Content inside 'Setup Complete Screen' is not scrollable and cutting from the bottom in landscape mode on device (Realwear Nav 500). Need to add scroll behaviour on parent widget of 'Setup Complete Screen' inside function CompletionStep
- **Files:** `OnboardingActivity.kt` → `CompletionStep`

### Task 2. Vos4/apps/VoiceAccessibility

##### 2.1 UIScrapingEngine Continuous Events ISSUE

- **Issue:** In apps like DeviceInfo or SysInfo, the content on the screen keeps changing frequently. Because of this, our accessibility service (VoiceOSService.kt) keeps receiving TYPE_WINDOW_CONTENT_CHANGED events many times per second. As a result, the UIScrapingEngine is continuously scraping the screen content again and again. We tried adding a Debouncer to filter these events, but it is not working as expected.
- **Files:** `VoiceOSService.kt` , `UIScrapingEngine.kt`, `Debouncer.kt`
- **Current Flow:**
  System(Apps - DeviceInfo or SysInfo) -> VoiceOSService.onAccessibilityEvent : TYPE_WINDOW_CONTENT_CHANGED
  VoiceOSService -> Debouncer : tryDebounce(event)
  Debouncer --> VoiceOSService : (not suppressed)
  VoiceOSService -> UIScrapingEngine : extractUIElementsAsync(event)
  UIScrapingEngine --> VoiceOSService : scrapingResult
  (loop repeats many times per second)
  (Events keep firing rapidly; Debouncer present but issue persists → engine scrapes repeatedly)

### Task 3. Vos4/modules/libraries/DeviceManager

##### 3.1 Smoothen cursor movement

- **TASK** - Inside **IMUManager** the new implementation to process sensor data inside function `processRotationVector(event: SensorEvent)` is commented and added the legacyavanue implemented inside `processRotationVector(values: FloatArray?, timestamp: Long, accuracy: Int)` to fix cursor movement issue which need to compare and fix to achieve more smoothness in cursor.
- **TASK** - Inside **CursorAdapter** the new implementation to process orientation/movement for cursor inside function `processOrientationForCursor(orientationData: OrientationData)` is commented and added the legacyavanue algorithm inside function `processOrientationForCursor(imuData: IMUData)` to process sensor data and create cursor X&Y co-ordinates to fix cursor movement issue which need to compare and fix to achieve more smoothness in cursor.
- **Files:** `IMUManager.kt` , `CursorAdapter.kt`
- legacyavanue File [..legacyavanue/voiceos-accessibility/src/main/java/com/augmentalis/accessibility/cursor/CursorOrientationProvider.kt], [..legacyavanue/voiceos-accessibility/src/main/java/com/augmentalis/accessibility/cursor/VoiceOsCursor.kt]

### Task 4. Vos4/modules/libraries/SpeechRecognition

##### 4.1 Vivoka Engine
- **TASK** - Add all **Vivoka** supported languages in SpeechRecognition & Implement download manager for languages models
- **Current Implementation inside libraries/SpeechRecognition** - It is partially implemented inside new implementation only implemented error handling if model not found while initializing the model
  No Download Manager found,but Error Handling is there: Maps ERROR_MODEL_NOT_FOUND to SpeechError.Action.DOWNLOAD_MODEL
  **Error Handling:**
  `RecognizerError.ERROR_MODEL_NOT_FOUND -> SpeechError(
  code = SpeechError.ERROR_MODEL,
  message = message ?: "Model not found",
  isRecoverable = true,
  suggestedAction = SpeechError.Action.DOWNLOAD_MODEL
  )`

- We have implementation regarding downloading VIVOKA models inside legacyavanue inside below files, initially fetching the correct URL of model as per selected language from firebase remote config and the downloading the required models from cloud.
- ../vivoka-voice/vsdk-models/src/main/java/com/augmentalis/vsdk_models/FirebaseRemoteConfigRepository.kt
- ../vivoka-voice/vsdk-models/src/main/java/com/augmentalis/vsdk_models/LanguageUtils.kt
- ../vivoka-voice/vsdk-models/src/main/java/com/augmentalis/vsdk_models/VsdkHandlerUtils.kt
- ../vivoka-voice/vsdk-models/src/main/java/com/augmentalis/vsdk_models/FileZipManager.kt

##### 4.2 Android Speech
- **TASK** - Android SpeechRecognition** Engine implementation is done but testing is pending 

##### 4.3 VOSK Engine
- **TASK** - **VOSK** Engine implementation is done but testing is pending

##### 4.4 Google Cloud Speech
- **TASK** - **Google Cloud Speech** Engine implementation is done but testing is pending 

##### 4.5 Whisper Engine
- **TASK** - **Whisper Engine** Engine implementation is done but testing is pending 

### Task 5. Integrate Vos4/modules/apps/**VoiceCursor** inside Vos4/modules/apps/**VoiceAccessibility**

- **TASK** Integrate/Link **VoiceCursor** with **VoiceAccessibility** - Integrate VoiceCursor into VoiceAccessibility so it can be used universally across all apps to perform interactions—click, gaze, long press, etc.—via the accessibility service.
- **Plan** - docs/voiceos-master/implementation/VoiceCursor-VoiceAccessibility-Integration-Guide.md

### Task 6. Integrate Vos4/modules/libraries/**SpeechRecognition** inside Vos4/modules/apps/**VoiceAccessibility**

- **TASK** Integrate/Link **SpeechRecognition** with **VoiceAccessibility** - Integrate SpeechRecognition into VoiceAccessibility so it can be used universally across all apps to perform dynamic,static commands and free-speech
- **Plan** - /docs/voiceos-master/implementation/SpeechRecognition-VoiceAccessibility-Integration-Guide.md