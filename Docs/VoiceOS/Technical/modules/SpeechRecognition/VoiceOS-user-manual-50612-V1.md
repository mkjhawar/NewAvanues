# SpeechRecognition Module - User Manual

**Last Updated**: 2025-10-23 21:19 PDT
**Version**: 2.0.0
**Module**: SpeechRecognition Library
**For**: VoiceOS (VOS4) End Users

---

## Quick Links
- [Developer Manual](./developer-manual.md) - For developers integrating the module
- [Module Changelog](./changelog/CHANGELOG.md)
- [Troubleshooting](#troubleshooting) - Common issues and solutions

---

## Table of Contents

1. [Overview](#overview)
2. [Available Engines](#available-engines)
3. [Switching Engines](#switching-engines)
4. [Engine Comparison](#engine-comparison)
5. [Language Support](#language-support)
6. [Recognition Modes](#recognition-modes)
7. [Voice Commands](#voice-commands)
8. [Dictation Mode](#dictation-mode)
9. [Performance Tips](#performance-tips)
10. [Troubleshooting](#troubleshooting)
11. [FAQ](#faq)

---

## Overview

The SpeechRecognition module powers voice control in VoiceOS, allowing you to control your device with your voice. You can choose from five different speech recognition engines, each with unique strengths:

- **VOSK** - Fast offline recognition (recommended)
- **Vivoka** - Hybrid offline/online (best accuracy)
- **Android STT** - Google's built-in recognition (requires internet)
- **Whisper** - Advanced offline with 99 languages (resource-intensive)
- **Google Cloud** - Cloud-based premium (requires API key)

### What's New in Version 2.0

- **SOLID Architecture**: Rebuilt for better reliability and performance
- **Fuzzy Matching**: Understands commands even with mispronunciation
- **Learning System**: Remembers your speech patterns and gets better over time
- **Advanced Features**: Language detection, translation, word timestamps (Whisper)
- **Better Error Recovery**: Automatic retries and graceful fallbacks

---

## Available Engines

### VOSK (Recommended for Most Users)

**Best For**: Daily use, privacy-conscious users

**Pros**:
- ✅ Works completely offline (no internet needed)
- ✅ Fast recognition (100-200ms)
- ✅ Low memory usage (~30MB)
- ✅ Privacy-friendly (all processing on-device)
- ✅ Good accuracy for commands
- ✅ Free and open source

**Cons**:
- ❌ Limited languages (20+)
- ❌ Lower accuracy than cloud solutions for complex sentences

**When to Use**:
- You want fast, offline voice control
- You value privacy
- You primarily use voice commands (not long dictation)
- You have limited data or no internet access

**Setup**: No configuration needed - works out of the box!

---

### Vivoka

**Best For**: Users who want the best accuracy

**Pros**:
- ✅ Hybrid offline/online (best of both worlds)
- ✅ Very high accuracy
- ✅ Automatically switches based on network
- ✅ Wake word support
- ✅ Good language support (40+)

**Cons**:
- ❌ Higher memory usage (~60MB)
- ❌ Slightly slower than VOSK (80-150ms)
- ❌ Commercial license may apply

**When to Use**:
- You need the highest accuracy
- You have stable internet access
- Memory usage is not a concern
- You want wake word detection

**Setup**: Requires VSDK configuration files

---

### Android STT

**Best For**: Users with reliable internet

**Pros**:
- ✅ Uses Google's powerful servers
- ✅ Very low memory (~20MB)
- ✅ Excellent accuracy
- ✅ Large language support (100+)
- ✅ No model downloads needed

**Cons**:
- ❌ Requires internet connection
- ❌ Privacy concern (data sent to Google)
- ❌ Variable latency (50-300ms depending on network)
- ❌ May not work on all devices

**When to Use**:
- You always have good internet
- You don't mind cloud processing
- You want the best accuracy for dictation
- Your device has limited storage

**Setup**: Requires Google Play Services and internet access

---

### Whisper

**Best For**: Multilingual users, advanced features

**Pros**:
- ✅ 99 languages supported!
- ✅ Automatic language detection
- ✅ Translation to English
- ✅ Word-level timestamps
- ✅ Excellent accuracy
- ✅ Completely offline

**Cons**:
- ❌ High memory usage (150MB - 2.5GB depending on model)
- ❌ Slower recognition (0.5-5 seconds)
- ❌ Requires model downloads
- ❌ May drain battery faster

**When to Use**:
- You need multilingual support
- You want translation features
- You need word-level timing information
- Recognition speed is less critical
- You have a powerful device

**Setup**: Choose model size based on device:
- **Tiny** (75MB) - Fast, good for real-time
- **Base** (142MB) - Balanced
- **Small** (466MB) - Better accuracy
- **Medium/Large** - Best accuracy (powerful devices only)

---

### Google Cloud

**Best For**: Professional/commercial applications

**Pros**:
- ✅ Best possible accuracy
- ✅ Advanced features (speaker diarization, etc.)
- ✅ 125+ languages
- ✅ Real-time streaming
- ✅ Domain-specific models

**Cons**:
- ❌ Requires API key and billing setup
- ❌ Costs money per usage
- ❌ Requires internet
- ❌ Privacy considerations

**When to Use**:
- You're using VoiceOS commercially
- You need professional-grade accuracy
- Cost is not a primary concern
- You need advanced features

**Setup**: Requires Google Cloud API key (see Settings → Speech → Google Cloud)

---

## Switching Engines

### Via VoiceOS Settings

1. Open VoiceOS Settings
2. Navigate to **Voice Recognition**
3. Tap **Speech Engine**
4. Select your preferred engine:
   - VOSK (Offline, Fast) ⭐ Recommended
   - Vivoka (Hybrid, Accurate)
   - Android STT (Online, Google)
   - Whisper (Multilingual, Offline)
   - Google Cloud (Premium, Online)
5. Tap **Apply**

The engine will switch immediately for new voice commands.

### Via Voice Command

Say: **"Switch to [engine name]"**

Examples:
- "Switch to VOSK"
- "Switch to Vivoka"
- "Switch to Android STT"
- "Switch to Whisper"

### Automatic Fallback

If your selected engine fails (e.g., no internet for Android STT), VoiceOS will automatically fall back to VOSK.

---

## Engine Comparison

### Quick Reference Table

| Feature | VOSK | Vivoka | Android STT | Whisper | Google Cloud |
|---------|------|--------|-------------|---------|--------------|
| **Offline** | ✅ Yes | ✅ Yes* | ❌ No | ✅ Yes | ❌ No |
| **Memory** | ~30MB | ~60MB | ~20MB | 150-2500MB | ~15MB |
| **Speed** | Fast (100-200ms) | Medium (80-150ms) | Variable (50-300ms) | Slow (500-5000ms) | Medium (100-250ms) |
| **Accuracy (Commands)** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Accuracy (Dictation)** | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Languages** | 20+ | 40+ | 100+ | 99 | 125+ |
| **Privacy** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| **Cost** | Free | Free* | Free | Free | Paid |
| **Setup** | None | Minimal | Internet | Model download | API key |
| **Learning** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **Fuzzy Matching** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |

\* Vivoka has online mode as fallback

### Accuracy Comparison by Use Case

**Voice Commands** (e.g., "open calculator"):
1. Vivoka (⭐⭐⭐⭐⭐)
2. VOSK (⭐⭐⭐⭐)
3. Android STT (⭐⭐⭐⭐)
4. Whisper (⭐⭐⭐⭐)
5. Google Cloud (⭐⭐⭐⭐⭐)

**Dictation** (e.g., "The quick brown fox jumps..."):
1. Google Cloud (⭐⭐⭐⭐⭐)
2. Android STT (⭐⭐⭐⭐⭐)
3. Whisper (⭐⭐⭐⭐⭐)
4. Vivoka (⭐⭐⭐⭐)
5. VOSK (⭐⭐⭐)

**Noisy Environments**:
1. Vivoka (⭐⭐⭐⭐⭐)
2. Whisper (⭐⭐⭐⭐)
3. VOSK (⭐⭐⭐)
4. Google Cloud (⭐⭐⭐⭐)
5. Android STT (⭐⭐⭐)

**Speed** (lower is better):
1. VOSK (100-200ms)
2. Vivoka (80-150ms)
3. Android STT (50-300ms, variable)
4. Google Cloud (100-250ms)
5. Whisper (500-5000ms)

**Memory Efficiency**:
1. Android STT (15-20MB)
2. VOSK (30-50MB)
3. Vivoka (60-100MB)
4. Whisper Tiny (150-200MB)
5. Whisper Large (2-3GB)

### Recommendations by Device

**Budget/Older Devices** (2GB RAM or less):
- 🥇 VOSK (lightweight, fast)
- 🥈 Android STT (if internet available)
- 🥉 Vivoka (if RAM available)
- ❌ Avoid: Whisper (too heavy)

**Mid-Range Devices** (3-6GB RAM):
- 🥇 Vivoka (best accuracy)
- 🥈 VOSK (if prefer offline)
- 🥉 Whisper Tiny/Base (for multilingual)
- 🏅 Android STT (if internet preferred)

**High-End Devices** (8GB+ RAM):
- 🥇 Whisper Small/Medium (best offline + multilingual)
- 🥈 Vivoka (best hybrid)
- 🥉 Google Cloud (if professional use)
- 🏅 VOSK (if speed critical)

---

## Language Support

### VOSK Languages

English (US, UK, India), Spanish, French, German, Russian, Portuguese, Italian, Turkish, Vietnamese, Arabic, Persian, Chinese, Japanese, Korean, Hindi, Czech, Polish, Uzbek

**Changing Language**:
1. Settings → Voice Recognition → Language
2. Select your language
3. Download model if prompted

### Vivoka Languages

40+ languages including all major European, Asian, and Middle Eastern languages.

**Changing Language**:
1. Settings → Voice Recognition → Language
2. Vivoka will download language pack if needed

### Android STT Languages

100+ languages (uses Google's cloud models)

**Changing Language**:
- Settings → Voice Recognition → Language
- No downloads needed (cloud-based)

### Whisper Languages

**99 languages with automatic detection!**

Afrikaans, Arabic, Armenian, Azerbaijani, Belarusian, Bosnian, Bulgarian, Catalan, Chinese, Croatian, Czech, Danish, Dutch, English, Estonian, Finnish, French, Galician, German, Greek, Hebrew, Hindi, Hungarian, Icelandic, Indonesian, Italian, Japanese, Kannada, Kazakh, Korean, Latvian, Lithuanian, Macedonian, Malay, Marathi, Maori, Nepali, Norwegian, Persian, Polish, Portuguese, Romanian, Russian, Serbian, Slovak, Slovenian, Spanish, Swahili, Swedish, Tagalog, Tamil, Thai, Turkish, Ukrainian, Urdu, Vietnamese, Welsh, and many more!

**Language Detection**:
- Whisper can automatically detect what language you're speaking
- Enable in Settings → Voice Recognition → Whisper → Auto-detect language

**Translation**:
- Whisper can translate any language to English in real-time
- Enable in Settings → Voice Recognition → Whisper → Translation

---

## Recognition Modes

VoiceOS supports different recognition modes for different tasks:

### Command Mode (Default)

**Purpose**: Control your device with voice commands

**How it Works**:
- Recognizes specific commands from screen
- Fast and accurate for known commands
- Uses fuzzy matching (understands mistakes)
- Learns your speech patterns

**Examples**:
- "Open calculator"
- "Navigate home"
- "Scroll down"
- "Press back button"

**Activate**: Default mode - always active

---

### Dictation Mode

**Purpose**: Type text with your voice

**How it Works**:
- Recognizes continuous speech
- Converts to text
- Adds punctuation automatically (engine-dependent)
- Stops after silence timeout (2 seconds default)

**Examples**:
- Email composition
- Note-taking
- Text messaging
- Document editing

**Activate**:
- Say: **"Start dictation"**
- Or: Long-press microphone button

**Deactivate**:
- Say: **"Stop dictation"**
- Or: Tap microphone button
- Or: Automatic after silence

**Best Engines for Dictation**:
1. Google Cloud (⭐⭐⭐⭐⭐)
2. Android STT (⭐⭐⭐⭐⭐)
3. Whisper (⭐⭐⭐⭐⭐)
4. Vivoka (⭐⭐⭐⭐)
5. VOSK (⭐⭐⭐)

---

### Sleep/Wake (Voice Muting)

**Purpose**: Temporarily disable voice recognition

**How it Works**:
- VoiceOS stops listening to commands
- Only listens for wake word
- Saves battery
- Prevents accidental activation

**Activate** (Sleep):
- Say: **"Mute voice"**
- Or: Settings → Voice Recognition → Sleep
- Or: Automatic after timeout (configurable)

**Deactivate** (Wake):
- Say: **"Voice"** (wake word)
- Or: Tap microphone button
- Or: Settings → Voice Recognition → Wake

**Auto-Sleep Timeout**:
- Default: 5 minutes of inactivity
- Configure: Settings → Voice Recognition → Auto-sleep timeout
- Options: 1, 2, 5, 10, 15, 30 minutes, Never

---

## Voice Commands

### System Commands (Always Available)

These commands work in all modes:

**Voice Control**:
- "Voice" - Wake up from sleep mode
- "Mute voice" - Enter sleep mode
- "Start dictation" - Switch to dictation mode
- "Stop dictation" - Exit dictation mode

**Engine Switching**:
- "Switch to VOSK"
- "Switch to Vivoka"
- "Switch to Android STT"
- "Switch to Whisper"

**Help**:
- "Help" - Show available commands
- "What can I say?" - Show contextual commands

### Application Commands (Context-Aware)

VoiceOS automatically discovers commands from your current screen. The available commands change based on:
- Current app
- Visible buttons and elements
- Text fields
- Lists and menus

**Examples in Calculator App**:
- "Press one"
- "Press plus"
- "Press equals"
- "Clear"

**Examples in Browser**:
- "Go back"
- "Scroll down"
- "Open new tab"
- "Refresh page"

**Examples in Email**:
- "Compose email"
- "Reply"
- "Delete"
- "Archive"

### Navigation Commands

**Basic Navigation**:
- "Go back"
- "Go home"
- "Recent apps"
- "Open notifications"

**Scrolling**:
- "Scroll down"
- "Scroll up"
- "Scroll to top"
- "Scroll to bottom"

**Gestures**:
- "Swipe left"
- "Swipe right"
- "Long press [element]"

---

## Dictation Mode

### Starting Dictation

**Option 1**: Voice command
```
You: "Start dictation"
VoiceOS: [Enters dictation mode, shows microphone indicator]
You: "The quick brown fox jumps over the lazy dog"
[Text appears in active text field]
```

**Option 2**: Long-press microphone button
1. Long-press the VoiceOS microphone button
2. Start speaking
3. Text appears in real-time (engine-dependent)

### Dictation Tips

**Punctuation** (Android STT, Google Cloud, Whisper):
- Say "period" for .
- Say "comma" for ,
- Say "question mark" for ?
- Say "exclamation mark" for !
- Say "new line" for line break
- Say "new paragraph" for paragraph break

**Editing**:
- "Delete that" - Delete last phrase
- "Scratch that" - Delete last phrase
- "Undo" - Undo last action

**Special Characters**:
- "At sign" for @
- "Hash" or "pound" for #
- "Dollar sign" for $
- "Percent" for %

**Numbers**:
- Say numbers naturally: "one hundred twenty three" → "123"
- Or spell: "one two three" → "123"

**Best Practices**:
1. Speak clearly at normal pace
2. Avoid background noise
3. Use Vivoka, Android STT, or Whisper for best results
4. Keep sentences under 30 seconds
5. Pause briefly for punctuation
6. Review and correct after dictation

---

## Performance Tips

### Improving Recognition Accuracy

**1. Speak Clearly**:
- Normal pace (not too fast, not too slow)
- Clear pronunciation
- Don't shout or whisper

**2. Reduce Background Noise**:
- Quiet environment is best
- Close windows
- Turn off TV/music
- Use headset microphone if available

**3. Microphone Placement**:
- Phone: 6-12 inches from mouth
- Headset: Proper positioning
- Tablet: Front-facing for best pickup

**4. Use Appropriate Commands**:
- Match visible button text
- Use simple, clear commands
- Avoid overly long commands

**5. Let the System Learn**:
- The learning system improves over time
- Fuzzy matching adapts to your speech
- Repeated corrections are remembered

### Optimizing Battery Life

**1. Use Offline Engines**:
- VOSK uses least battery
- Whisper uses more (heavier processing)
- Online engines use data (battery impact)

**2. Enable Auto-Sleep**:
- Settings → Voice Recognition → Auto-sleep
- Set reasonable timeout (5-10 minutes)
- Prevents always-on listening

**3. Reduce VAD Sensitivity**:
- Settings → Voice Recognition → Advanced → VAD Sensitivity
- Higher value = less frequent wake-ups = better battery

**4. Use Smaller Models** (Whisper):
- Tiny model uses least CPU
- Base is balanced
- Avoid Large on mobile

### Optimizing Speed

**Fastest Recognition**:
1. VOSK (100-200ms)
2. Vivoka offline mode (80-150ms)
3. Android STT with good connection (50-150ms)

**For Real-Time Applications**:
- Use VOSK or Vivoka
- Avoid Whisper Medium/Large
- Ensure good network (if using online)
- Reduce command set size (<200)

### Optimizing Memory Usage

**Low Memory Devices**:
1. Use Android STT (15-20MB) or VOSK (30MB)
2. Avoid Whisper
3. Clear learning cache periodically
4. Limit command history

**Memory Usage by Engine**:
- Android STT: ~20MB
- VOSK: ~30MB
- Vivoka: ~60MB
- Whisper Tiny: ~150MB
- Whisper Base: ~250MB
- Whisper Small: ~500MB
- Whisper Medium: ~1.5GB
- Whisper Large: ~2.5GB

---

## Troubleshooting

### Voice Recognition Not Working

**Symptom**: Microphone button does nothing, no recognition

**Possible Causes & Solutions**:

1. **Microphone Permission Not Granted**
   - Go to Settings → Apps → VoiceOS → Permissions
   - Enable "Microphone" permission

2. **Engine Not Initialized**
   - Open VoiceOS Settings → Voice Recognition
   - Verify engine shows "Ready" status
   - If not, tap "Reinitialize Engine"

3. **Voice Muted (Sleep Mode)**
   - Say "Voice" to wake up
   - Or tap microphone button
   - Check for sleep indicator in status bar

4. **Audio Input Issues**
   - Test microphone in Recorder app
   - Check headset connection if using
   - Restart device

5. **Model Files Missing** (VOSK, Whisper)
   - Settings → Voice Recognition → Download Models
   - Ensure sufficient storage space
   - Check internet connection for download

---

### Low Recognition Accuracy

**Symptom**: Commands frequently misunderstood or not recognized

**Solutions**:

1. **Improve Audio Quality**:
   - Move to quieter location
   - Speak closer to microphone
   - Avoid background noise

2. **Use Better Commands**:
   ```
   ❌ "calculator"        → ✅ "open calculator"
   ❌ "go"                → ✅ "go back"
   ❌ "down"              → ✅ "scroll down"
   ```

3. **Switch to More Accurate Engine**:
   - Try Vivoka or Android STT
   - Whisper for multilingual needs

4. **Adjust Confidence Threshold**:
   - Settings → Voice Recognition → Advanced
   - Lower threshold = more matches but less accurate
   - Higher threshold = fewer matches but more accurate
   - Default: 0.70 (70%)

5. **Clear Learning Cache** (if getting worse):
   - Settings → Voice Recognition → Advanced
   - Tap "Clear Learning Data"
   - System will relearn from scratch

---

### Engine-Specific Issues

#### VOSK Issues

**Problem**: "Unknown word" in logs
- Solution: Commands not in model vocabulary
- Switch to Vivoka or add to static commands

**Problem**: Slow recognition
- Solution: Use smaller model (if custom)
- Check device performance

#### Vivoka Issues

**Problem**: "VSDK initialization failed"
- Solution: Clear app data, reinstall
- Ensure assets are present

**Problem**: Model compilation fails
- Solution: Reduce command count
- Clear temporary files

#### Android STT Issues

**Problem**: "Network error"
- Solution: Check internet connection
- Switch to offline engine (VOSK, Whisper)

**Problem**: "Service unavailable"
- Solution: Update Google Play Services
- Check Google account signed in

#### Whisper Issues

**Problem**: "Out of memory"
- Solution: Use smaller model (Tiny or Base)
- Close other apps
- Restart device

**Problem**: Very slow recognition
- Solution: Use Tiny model
- Consider switching to VOSK for commands
- Use Base model as compromise

---

### Network-Related Issues

**Problem**: Online engines not working

**Solutions**:
1. Check internet connection
2. Verify data/WiFi enabled
3. Check firewall/VPN settings
4. Try different network
5. Fallback to offline engine (VOSK, Whisper)

**Automatic Fallback**:
- VoiceOS automatically falls back to VOSK if online engine fails
- Check Settings → Voice Recognition → Fallback Engine

---

## FAQ

### General Questions

**Q: Which engine should I use?**
A: For most users, VOSK is recommended (fast, offline, accurate). Use Vivoka if you need highest accuracy, Android STT if you always have internet, or Whisper if you need multilingual support.

**Q: Can I use multiple engines at once?**
A: No, only one engine is active at a time. You can switch engines anytime via settings or voice command.

**Q: Does voice recognition work offline?**
A: Yes! VOSK, Vivoka (offline mode), and Whisper work completely offline. Android STT and Google Cloud require internet.

**Q: Is my voice data sent to the cloud?**
A: Only with Android STT and Google Cloud. VOSK, Vivoka offline, and Whisper process everything on-device.

**Q: How much battery does voice recognition use?**
A: With auto-sleep enabled (5 min timeout), battery impact is minimal (<5% per day). Continuous listening uses more (~15-20% per day).

### Feature Questions

**Q: Can I add custom commands?**
A: Yes! VoiceOS automatically discovers commands from screen content. For static commands, use Settings → Voice Recognition → Custom Commands.

**Q: Does it support multiple languages?**
A: Yes! All engines support multiple languages. Whisper supports 99 languages with automatic detection.

**Q: Can I translate speech to another language?**
A: Yes, with Whisper engine. Enable Settings → Voice Recognition → Whisper → Translation. Translates any language to English.

**Q: Can I get word-by-word timing?**
A: Yes, with Whisper engine. Enable Settings → Voice Recognition → Whisper → Word Timestamps.

**Q: Does it learn my voice?**
A: Yes! The learning system remembers your speech patterns and improves accuracy over time. Fuzzy matching handles pronunciation variations.

### Technical Questions

**Q: How much storage do models require?**
A:
- VOSK: ~50MB
- Vivoka: ~100MB per language
- Whisper Tiny: 75MB
- Whisper Base: 142MB
- Whisper Small: 466MB
- Whisper Medium: 1.5GB
- Whisper Large: 2.9GB
- Android STT / Google Cloud: No local storage (cloud-based)

**Q: Can I use it on a low-end device?**
A: Yes! Use VOSK (30MB RAM) or Android STT (20MB RAM). Avoid Whisper Medium/Large on devices with <4GB RAM.

**Q: What's the recognition latency?**
A:
- VOSK: 100-200ms
- Vivoka: 80-150ms
- Android STT: 50-300ms (network-dependent)
- Whisper Tiny: 500-1000ms
- Whisper Base: 1000-2000ms

**Q: Does it work in noisy environments?**
A: Best noise handling: Vivoka > Whisper > VOSK > Google Cloud > Android STT. Enable noise reduction in settings for better results.

**Q: Can I use a Bluetooth headset?**
A: Yes! All engines support Bluetooth audio input. Ensure headset is paired and connected.

---

## Getting Help

### Built-in Help

Say **"Help"** anytime to see available commands for your current screen.

### Documentation

- **Developer Manual**: [developer-manual.md](./developer-manual.md)
- **API Reference**: [reference/api/](./reference/api/)
- **Changelog**: [changelog/CHANGELOG.md](./changelog/CHANGELOG.md)

### Support

- **VoiceOS Community**: [Community Forum Link]
- **Bug Reports**: [Issue Tracker Link]
- **Email Support**: support@augmentalis.com

### Logging Issues

To help us diagnose issues, include:
1. Engine being used (VOSK, Vivoka, etc.)
2. Device model and Android version
3. Steps to reproduce
4. Actual vs expected behavior
5. Logs if available (Settings → About → Export Logs)

---

## Glossary

**ASR**: Automatic Speech Recognition - the technology that converts speech to text

**Confidence Score**: A value (0.0-1.0) indicating how certain the engine is about recognition accuracy

**Dictation Mode**: Continuous speech recognition for typing text

**Engine**: The underlying technology that performs speech recognition (VOSK, Vivoka, etc.)

**Fuzzy Matching**: Technique that finds similar commands even with typos or mispronunciation

**Learning System**: Feature that improves recognition by remembering your speech patterns

**Model**: Data file containing language information used by offline engines

**Sleep Mode**: State where voice recognition only listens for wake word

**VAD**: Voice Activity Detection - detects when you're speaking vs silence

**Wake Word**: Specific phrase ("Voice") that wakes the system from sleep mode

---

**Document Version**: 1.0.0
**Created**: 2025-10-23 21:19 PDT
**Author**: Claude Code (VOS4 Documentation Specialist)
**Review Status**: Initial Draft

**Generated with** [Claude Code](https://claude.com/claude-code)

---

## Advanced: Manual Model Deployment

**Added**: 2025-11-21 (Version 2.1.0)
**Skill Level**: Advanced users, developers
**Benefit**: Smaller app size, faster testing

### What Is Manual Model Deployment?

Starting with VoiceOS 2.1.0, you can manually deploy speech recognition models to your device **before** installing the app. This is useful for:

- **Developers**: Test different model versions without rebuilding the app
- **Advanced Users**: Keep models after uninstalling/reinstalling VoiceOS
- **Limited Data**: Pre-load models on WiFi, then use offline
- **Multiple Devices**: Deploy models once, share across test devices

### Which Engines Support This?

✅ **Supported** (offline engines):
- Vivoka - VSDK model files
- Whisper - Model `.bin` files  
- VOSK - Model directory

❌ **Not Applicable** (cloud-based):
- Android STT - Uses Google's servers (no local models)
- Google Cloud STT - Uses Google Cloud API (no local models)

### How It Works

VoiceOS automatically searches for models in multiple locations:

1. **App's internal storage** (default, like before)
2. **App's external storage** (accessible via file manager)
3. **Shared folder** (NEW - survives app uninstall) ⭐

If models are found in location #3, VoiceOS uses them instead of downloading or bundling in the app!

### Step-by-Step Guide

#### Requirements

- Android device connected to computer via USB
- ADB (Android Debug Bridge) installed on computer
- USB debugging enabled on device
- Model files for your chosen engine

#### Step 1: Enable USB Debugging

1. Open **Settings** on your Android device
2. Go to **About Phone**
3. Tap **Build Number** 7 times (Developer mode enabled)
4. Go back to **Settings** → **Developer Options**
5. Enable **USB Debugging**
6. Connect device to computer via USB
7. Accept USB debugging prompt on device

#### Step 2: Install ADB (if not installed)

**Windows**:
1. Download [Platform Tools](https://developer.android.com/tools/releases/platform-tools)
2. Extract to `C:\platform-tools\`
3. Add to PATH or use full path

**Mac/Linux**:
```bash
# Mac (using Homebrew)
brew install android-platform-tools

# Linux (Ubuntu/Debian)
sudo apt-get install android-tools-adb
```

#### Step 3: Verify ADB Connection

```bash
adb devices
# Should show: List of devices attached
#             <device-id>    device
```

If "unauthorized", check device screen and accept prompt.

#### Step 4: Deploy Models

Choose your engine and follow the appropriate steps:

**Vivoka VSDK**:
```bash
# Navigate to folder containing 'vsdk' directory
cd /path/to/your/vivoka/models

# Push to device
adb push vsdk /storage/emulated/0/.voiceos/models/vivoka/vsdk

# Verify
adb shell ls -la /storage/emulated/0/.voiceos/models/vivoka/vsdk/
```

**Whisper**:
```bash
# Navigate to folder containing model files
cd /path/to/your/whisper/models

# Create directory structure
adb shell mkdir -p /storage/emulated/0/.voiceos/models/whisper/whisper_models

# Push model file (example: tiny model)
adb push ggml-tiny.bin /storage/emulated/0/.voiceos/models/whisper/whisper_models/

# Verify
adb shell ls -la /storage/emulated/0/.voiceos/models/whisper/whisper_models/
```

**VOSK**:
```bash
# Navigate to folder containing 'model' directory
cd /path/to/your/vosk/models

# Push to device
adb push model /storage/emulated/0/.voiceos/models/vosk/model

# Verify
adb shell ls -la /storage/emulated/0/.voiceos/models/vosk/model/
```

#### Step 5: Install/Reinstall VoiceOS

1. Install VoiceOS normally from Play Store or APK
2. Open VoiceOS Settings
3. Go to **Voice Recognition**
4. Select your engine (Vivoka, Whisper, or VOSK)
5. VoiceOS automatically detects pre-deployed models!

### Verification

Check logs to confirm models were found:

1. Enable developer logging:
   - Settings → Developer Options → VoiceOS Debug Logs
2. Open VoiceOS and select your engine
3. Check logs (Settings → About → View Logs):

```
✅ Success example:
I/VivokaInitializer: Found existing VSDK at: /storage/emulated/0/.voiceos/models/vivoka/vsdk

❌ Not found example:
I/VivokaInitializer: VSDK not found in any location, extracting from APK assets
```

### Benefits

| Benefit | Description |
|---------|-------------|
| 📦 **Smaller APK** | App doesn't need to bundle large model files (saves 50-500MB) |
| 🚀 **Faster Testing** | Deploy models once, test multiple app builds without redeploying |
| 💾 **Persistent Storage** | Models survive app uninstall/reinstall |
| 🔄 **Easy Updates** | Update models without updating the app |
| 👥 **Team Sharing** | Share model files via file manager or cloud storage |
| 🌐 **Offline Preparation** | Download models on WiFi, deploy offline later |

### File Manager Method (Alternative to ADB)

For users who prefer not to use ADB:

1. Connect device to computer (MTP/File Transfer mode)
2. Open device storage in file manager
3. Create folder structure:
   ```
   Internal Storage/
   └── .voiceos/
       └── models/
           ├── vivoka/
           │   └── vsdk/          (put Vivoka files here)
           ├── whisper/
           │   └── whisper_models/ (put Whisper files here)
           └── vosk/
               └── model/         (put VOSK files here)
   ```
4. Copy model files into appropriate folder
5. **Note**: `.voiceos` folder may be hidden - enable "Show hidden files" in file manager

### Troubleshooting

**Problem**: Models not detected after deployment

**Solutions**:
1. Verify folder path exactly matches (case-sensitive!)
2. Check file permissions (should be readable):
   ```bash
   adb shell ls -la /storage/emulated/0/.voiceos/models/
   ```
3. Ensure storage permissions granted to VoiceOS:
   - Settings → Apps → VoiceOS → Permissions → Storage → Allow
4. Check model files are complete and not corrupted:
   ```bash
   # Check file sizes
   adb shell du -sh /storage/emulated/0/.voiceos/models/*/
   ```

**Problem**: "Permission denied" when pushing files

**Solutions**:
- Ensure USB debugging enabled
- Try: `adb kill-server && adb start-server`
- Check USB cable (use data cable, not charge-only)
- Verify device is authorized: `adb devices`

**Problem**: Can't create `.voiceos` folder via file manager

**Solutions**:
- Use ADB instead (recommended)
- Enable "Show hidden files" in file manager settings
- Use a root file manager (e.g., Solid Explorer)
- Create folder via ADB: `adb shell mkdir -p /storage/emulated/0/.voiceos/models`

### Advanced: Sharing Models Across Apps

The shared folder `/storage/emulated/0/.voiceos/models/` is accessible to:
- Any app with storage permission
- File managers
- ADB
- Root apps

This allows:
- **Multiple VoiceOS installations** (debug + release) to share models
- **Other voice apps** to use same models (if compatible)
- **Backup/restore** via standard backup tools

**Security Note**: Models in shared folder are readable by all apps with storage permission. If you have proprietary/sensitive models, keep them in internal app storage (VoiceOS does this by default).

### Model Storage Locations Summary

| Location | Path | Accessible By | Survives Uninstall |
|----------|------|---------------|-------------------|
| **Internal** | `/data/data/com.augmentalis.voiceos/files/` | App only | ❌ No |
| **External App** | `/Android/data/com.augmentalis.voiceos/files/` | App + File Manager | ❌ No |
| **Shared** | `/.voiceos/models/{engine}/` | All apps | ✅ **Yes** |

VoiceOS checks all locations automatically and uses whichever is found first.

---

**Section Added**: 2025-11-21
**For Version**: 2.1.0+
**Difficulty**: Advanced
**Time to Deploy**: 5-10 minutes

**Need Help?** See [Developer Manual](./developer-manual.md#model-deployment-and-path-resolution) for technical details.
