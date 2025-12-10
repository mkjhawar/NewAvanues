# User Manual - Chapter 13: Platform Support

**Version:** 1.0
**Date:** 2025-12-01

---

## Overview

AVA now supports multiple platforms with the same voice assistant capabilities. This chapter explains platform-specific features and setup.

---

## Supported Platforms

| Platform | Status | Voice | NLU | LLM |
|----------|--------|-------|-----|-----|
| Android | Production | Yes | Yes | Yes |
| iOS | Production | Yes | Yes | Yes |
| Desktop (Windows/Mac/Linux) | Beta | Yes | Yes | Yes |
| Web | Planned | - | - | - |

---

## Android

### Requirements

- Android 9.0 (Pie) or higher
- 4GB RAM minimum (6GB recommended)
- 2GB free storage for models

### Features

- Full voice control
- Background wake word detection
- Overlay UI for quick access
- Local LLM inference (on-device)
- Document search (RAG)

### Model Download

Models download automatically on first launch. You can manage models in **Settings > AI Models**.

---

## iOS

### Requirements

- iOS 15.0 or higher
- iPhone XS or newer (A12 chip or later)
- 3GB free storage for models

### Features

- Core ML optimized inference
- Apple Neural Engine acceleration (A15+ chips)
- Siri Shortcuts integration
- iCloud sync for preferences

### Performance

| Device | Inference Speed |
|--------|----------------|
| iPhone 15 Pro | ~25ms |
| iPhone 14 | ~35ms |
| iPhone 12 | ~50ms |
| iPhone XS | ~80ms |

---

## Desktop

### Requirements

- Windows 10/11, macOS 11+, or Linux (Ubuntu 20.04+)
- 8GB RAM minimum
- 4GB free storage for models

### Installation

1. Download AVA Desktop from [releases page]
2. Run installer
3. Models download automatically on first launch

### Model Location

Models are stored in:
- **Windows:** `%USERPROFILE%\.ava\models\`
- **macOS/Linux:** `~/.ava/models/`

### Features

- Keyboard shortcuts for quick access
- System tray integration
- Cross-platform sync (coming soon)

---

## Model Management

### Automatic Download

On first launch, AVA downloads the required NLU model (~25MB). This happens in the background with progress indication.

### Manual Download

If automatic download fails:

1. Go to **Settings > AI Models**
2. Tap **Download Models**
3. Wait for download to complete

### Storage Usage

| Model | Size | Purpose |
|-------|------|---------|
| MobileBERT | 25MB | Intent understanding |
| Vocabulary | 1MB | Text tokenization |
| LLM (optional) | 1-4GB | Conversational AI |

---

## Offline Mode

AVA works offline after models are downloaded:

| Feature | Offline | Online |
|---------|---------|--------|
| Voice commands | Yes | Yes |
| Intent recognition | Yes | Yes |
| Device control | Yes | Yes |
| Web search | No | Yes |
| Cloud sync | No | Yes |

---

## Privacy

### On-Device Processing

All voice processing happens locally on your device:
- Voice recognition
- Intent classification
- Command execution

### No Data Sent

By default, AVA does not send any data to external servers. Optional cloud features can be enabled in Settings.

### Crash Reporting

Crash reporting is **disabled by default**. You can opt-in via **Settings > Privacy > Share Crash Reports**.

---

## Troubleshooting

### Model Download Fails

1. Check internet connection
2. Ensure sufficient storage (2GB free)
3. Try **Settings > AI Models > Retry Download**

### Slow Performance

1. Close background apps
2. Enable battery optimization exceptions for AVA
3. Consider upgrading to a faster model

### Voice Not Recognized

1. Check microphone permissions
2. Speak clearly and at normal pace
3. Reduce background noise

---

## Getting Help

- **In-app:** Say "Help" or tap the help icon
- **Documentation:** [docs.ava-ai.com](https://docs.ava-ai.com)
- **Support:** support@augmentalis.com

---

## Related Chapters

- [Chapter 10: Model Installation](User-Manual-Chapter10-Model-Installation.md)
- [Chapter 11: Voice Commands](User-Manual-Chapter11-Voice-Commands.md)
- [Chapter 12: Model Selection Guide](User-Manual-Chapter12-Model-Selection-Guide.md)
