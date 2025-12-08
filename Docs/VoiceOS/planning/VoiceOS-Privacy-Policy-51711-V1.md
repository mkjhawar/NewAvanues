# VoiceOS Privacy Policy

**Effective Date:** 2025-10-31
**Last Updated:** 2025-10-31
**Version:** 1.0.0

---

## Overview

VoiceOS is an accessibility application designed to help users with disabilities control their Android devices using voice commands. This Privacy Policy explains how VoiceOS handles data and permissions on your device.

**Core Principle:** VoiceOS operates entirely on your device. We do NOT collect, store, transmit, or share your personal information.

---

## Data Collection: NONE

VoiceOS does **NOT** collect:
- ❌ Personal information (name, email, phone, etc.)
- ❌ Voice recordings or audio data
- ❌ App usage analytics or statistics
- ❌ Device information or identifiers
- ❌ Location data
- ❌ Installed app lists or package names
- ❌ Accessibility event data
- ❌ Screen content or UI element information

**All processing happens locally on your device. Nothing is transmitted to external servers.**

---

## Permissions Required

VoiceOS requires specific Android permissions to provide accessibility features. Below is a detailed explanation of each permission:

### 1. Accessibility Service Permission

**Permission:** `BIND_ACCESSIBILITY_SERVICE`

**Purpose:** Core accessibility functionality - allows VoiceOS to:
- Read screen content to generate voice commands
- Simulate user interactions (taps, swipes, text input)
- Navigate between apps and screens

**Data Usage:** All accessibility data stays on your device and is processed in real-time. No data is stored or transmitted.

**User Control:** Can be disabled at any time in Android Settings → Accessibility → VoiceOS

---

### 2. Query All Packages Permission

**Permission:** `QUERY_ALL_PACKAGES`

**Purpose:** Detect and exclude launcher apps from voice control to prevent system instability.

**Why Required:**
- Starting with Android 11, apps cannot see all installed packages by default
- VoiceOS needs to identify launcher apps (home screens) to avoid controlling them
- Attempting to control launcher apps can cause navigation loops and system instability

**Data Usage:**
- ✅ **Local checks only** - Package queries performed on-device
- ✅ **No data collection** - Package names are NOT stored or transmitted
- ✅ **Safety filtering** - Used only to exclude system-critical apps
- ❌ **No analytics** - We do NOT track which apps you have installed
- ❌ **No sharing** - Package information never leaves your device

**Alternatives Considered:**
- `<queries>` element: Insufficient - cannot predict all launchers users install
- User manual configuration: Poor accessibility - defeats purpose of voice control

**Fallback:** If permission denied, VoiceOS uses a known launchers list (reduced safety)

---

### 3. Foreground Service Permissions

**Permissions:**
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_MICROPHONE` (Android 14+)

**Purpose:** Allow VoiceOS to continue voice recognition while running in the background.

**Why Required:**
- Accessibility services must run continuously to respond to voice commands
- Foreground service prevents Android from terminating VoiceOS
- Microphone type required for voice command processing on Android 14+

**Data Usage:**
- Voice recognition happens locally on-device (via Android SpeechRecognizer)
- No audio is recorded, stored, or transmitted
- Voice data is processed in real-time and immediately discarded

**User Control:** Can stop foreground service at any time via system notification

---

## How VoiceOS Uses Permissions

### Accessibility Service

**What VoiceOS Does:**
1. Reads UI elements on screen (buttons, text fields, etc.)
2. Generates voice commands based on screen content
3. Simulates user actions when voice commands are recognized
4. Navigates between apps and screens

**What VoiceOS Does NOT Do:**
- ❌ Record or store screen content
- ❌ Track user behavior
- ❌ Collect app usage data
- ❌ Send accessibility events to servers
- ❌ Share data with third parties

**Privacy Protection:**
- ✅ All logs are automatically sanitized to remove PII (email, phone, credit card, SSN, etc.)
- ✅ User-entered text is redacted before logging
- ✅ UI element text content is redacted in debug logs
- ✅ No sensitive information appears in log files

### Package Visibility

**What VoiceOS Does:**
1. Queries installed packages to identify launcher apps
2. Excludes launcher packages from accessibility scraping
3. Maintains device safety and stability

**What VoiceOS Does NOT Do:**
- ❌ Store list of installed apps
- ❌ Send package names to servers
- ❌ Track app installations/uninstallations
- ❌ Share app inventory with third parties
- ❌ Use package data for analytics or advertising

### Microphone Access

**What VoiceOS Does:**
1. Accepts voice commands via Android's built-in speech recognizer
2. Processes recognized text to execute commands
3. Operates entirely on-device

**What VoiceOS Does NOT Do:**
- ❌ Record audio
- ❌ Store voice data
- ❌ Transmit audio to servers
- ❌ Use voice data for profiling or advertising
- ❌ Share voice data with third parties

---

## Data Storage: LOCAL ONLY

VoiceOS stores minimal data locally for functionality:

### Stored Locally (on your device):
- ✅ App exploration data (navigation graphs for learned apps)
- ✅ User preferences and settings
- ✅ Voice command configurations
- ✅ Accessibility service state

### NOT Stored Anywhere:
- ❌ Voice recordings
- ❌ Screen content
- ❌ Personal information
- ❌ Usage analytics
- ❌ Device identifiers

**Database:** VoiceOS uses local Room database (SQLite) stored only on your device.

**No Cloud Sync:** VoiceOS does not sync data to cloud services or external servers.

---

## Third-Party Services: NONE

VoiceOS does **NOT** use:
- ❌ Analytics services (Google Analytics, Firebase, etc.)
- ❌ Crash reporting services
- ❌ Advertising networks
- ❌ Cloud storage providers
- ❌ Third-party APIs or SDKs (except Android framework)

**100% local operation - no network connectivity required or used.**

---

## Children's Privacy

VoiceOS does not collect any personal information from anyone, including children under 13. The app is safe for all ages and complies with COPPA (Children's Online Privacy Protection Act).

---

## Security

**Data Security:**
- All data stays on your device protected by Android's app sandboxing
- No data transmission means no network security risks
- No user accounts or authentication required

**Permission Security:**
- Accessibility permission protected by Android OS
- QUERY_ALL_PACKAGES permission used only for safety checks
- All permissions can be revoked at any time via Android Settings

**PII Redaction in Logs:**
- VoiceOS automatically redacts Personally Identifiable Information (PII) from all logs
- PII types detected and redacted: email addresses, phone numbers, credit cards, SSNs, names, addresses, ZIP codes
- Logs are sanitized before being written to protect user privacy
- Redaction happens locally on your device - nothing is transmitted
- Log files are stored locally and never uploaded to external servers
- Even if logs are exported for debugging, sensitive information is protected

---

## Your Rights and Choices

### You Control Your Data:

**Disable VoiceOS:**
- Settings → Accessibility → VoiceOS → Turn off
- Immediately stops all accessibility functionality

**Revoke Permissions:**
- Settings → Apps → VoiceOS → Permissions → Revoke any permission
- App will continue to function with reduced capabilities

**Clear Data:**
- Settings → Apps → VoiceOS → Storage → Clear data
- Removes all locally stored preferences and exploration data

**Uninstall:**
- Completely removes VoiceOS and all associated data from your device

---

## Changes to Privacy Policy

We may update this Privacy Policy from time to time. Changes will be posted within the app and on our website.

**Notification:** Major changes will be accompanied by in-app notification requiring user acknowledgment.

**Version History:**
- v1.0.0 (2025-10-31): Initial Privacy Policy

---

## Accessibility Commitment

VoiceOS is designed for users with disabilities who rely on accessibility features to use their devices. Our privacy-first approach ensures:

1. **User Autonomy:** You control your data - it never leaves your device
2. **Transparency:** Clear explanation of all permissions and their purposes
3. **Safety:** Permissions used only for stated accessibility purposes
4. **Trust:** No hidden data collection or third-party sharing

---

## Compliance

VoiceOS complies with:
- ✅ **GDPR** (General Data Protection Regulation) - No personal data processed
- ✅ **CCPA** (California Consumer Privacy Act) - No personal information collected
- ✅ **COPPA** (Children's Online Privacy Protection Act) - Safe for children
- ✅ **Google Play Store Policies** - Transparent permission usage
- ✅ **Android Accessibility Guidelines** - Proper accessibility service implementation

---

## Contact Information

**Developer:** Intelligent Devices LLC
**Authors:** Manoj Jhawar, Aman Jhawar
**Email:** privacy@augmentalis.com
**Website:** https://augmentalis.com/voiceos

**Privacy Questions:** privacy@augmentalis.com
**Technical Support:** support@augmentalis.com

---

## Legal

**Copyright:** © 2025 Manoj Jhawar, Aman Jhawar, Intelligent Devices LLC
**License:** Proprietary

---

## Summary (TL;DR)

✅ **VoiceOS is 100% private:**
- No data collection
- No network connectivity
- No third-party services
- Everything stays on your device

✅ **Permissions explained:**
- Accessibility: Read and control screen content (locally)
- Package visibility: Detect launchers for safety (no tracking)
- Foreground service: Keep voice recognition active (no recording)

✅ **You control everything:**
- Disable at any time in Accessibility Settings
- Revoke permissions via Android Settings
- Clear data or uninstall completely

**Questions?** Contact privacy@augmentalis.com

---

**Version:** 1.0.0
**Effective Date:** 2025-10-31
**Last Updated:** 2025-10-31
**Review Cycle:** Annually or as needed for policy changes
