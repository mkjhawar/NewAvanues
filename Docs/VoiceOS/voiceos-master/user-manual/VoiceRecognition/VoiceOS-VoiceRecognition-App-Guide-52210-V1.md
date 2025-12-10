<!--
filename: VoiceRecognition-README.md
created: 2025-01-28
author: VOS4 Development Team
purpose: Documentation for Voice Recognition test app
-->

# Voice Recognition App

## Overview
Voice Recognition is a comprehensive test application for the VOS4 SpeechRecognition library. It provides a full-featured UI for testing all speech recognition engines with real-time configuration and transcription display.

## Features

### Core Functionality
- **Multi-Engine Support**: Test Google STT, Vivoka, Google Cloud engines (VOSK temporarily disabled)
- **Real-Time Transcription**: Live display of speech-to-text results with confidence indicators
- **Configuration Screen**: Comprehensive settings for all speech parameters
- **VoiceCursor UI Style**: Consistent glassmorphism design with ARVision theming

### Configuration Options
- **Language Selection**: 10+ language options (en-US, en-GB, es-ES, fr-FR, de-DE, etc.)
- **Recognition Modes**: 
  - Static Commands (fixed command set)
  - Dynamic Commands (context-aware)
  - Dictation (free text)
  - Free Speech (unrestricted)
  - Hybrid (commands + free speech)
- **Voice Settings**:
  - Voice Activity Detection (VAD)
  - Profanity Filter
  - Confidence Threshold (10-100%)
- **Timing Controls**:
  - Max Recording Duration (5-60 seconds)
  - Timeout Duration (1-10 seconds)

## Architecture

### Package Structure
```
com.augmentalis.voicerecognition/
├── MainActivity.kt              # Entry point with permission handling
├── ui/
│   ├── SpeechRecognitionScreen.kt  # Main UI with controls
│   ├── ConfigurationScreen.kt      # Settings interface
│   └── ThemeUtils.kt               # Glassmorphism theming
└── viewmodel/
    └── SpeechViewModel.kt          # State management & library integration
```

### Key Components

#### MainActivity
- Handles Android permissions (RECORD_AUDIO)
- Uses Accompanist for permission management
- Launches main Compose UI

#### SpeechRecognitionScreen
- Engine selection chips
- Animated recording button with pulse effect
- Real-time transcript display
- Status panel with error handling
- Settings button for configuration access

#### ConfigurationScreen
- Language dropdown with 10+ options
- Mode selector with all 5 recognition modes
- Switch controls for VAD and profanity filter
- Sliders for confidence and timing settings
- Reset to defaults functionality

#### SpeechViewModel
- Manages speech engine lifecycle
- Handles configuration updates
- Processes recognition results
- Error handling and state management

## Dependencies
- SpeechRecognition library (`:libraries:SpeechRecognition`)
- Vivoka VSDK AARs (local files)
- Jetpack Compose UI
- AndroidX Lifecycle & ViewModel
- Accompanist Permissions
- Kotlin Coroutines

## Building & Testing

### Build Command
```bash
./gradlew :apps:VoiceRecognition:assembleDebug
```

### Requirements
- MinSDK: 26
- TargetSDK: 34
- Kotlin: 1.9.22
- Compose: BOM 2024.02.00

### Permissions Required
- `android.permission.RECORD_AUDIO`
- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`

## Status
- ✅ Build: Successful
- ✅ Warnings: All fixed
- ✅ UI: Complete with configuration
- ⚠️ VOSK: Temporarily disabled (dependency issue)
- ✅ Other Engines: Fully functional

## Differences from Old SpeechRecognition App
The old `/apps/SpeechRecognition` was just a placeholder that verified library linking. Voice Recognition is a complete implementation with:
- Full speech recognition functionality
- Configuration UI
- Real-time transcription
- Error handling
- Professional VoiceCursor-style UI

## Future Enhancements
- Re-enable VOSK support when dependency resolved
- Add speech synthesis testing
- Include wake word configuration
- Add performance metrics display

---
*Last Updated: 2025-01-28*