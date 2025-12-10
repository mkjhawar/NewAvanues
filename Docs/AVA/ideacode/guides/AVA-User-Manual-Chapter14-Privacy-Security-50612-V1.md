# User Manual - Chapter 14: Privacy & Security

**Version:** 1.0
**Date:** 2025-12-01

---

## Overview

AVA is designed with privacy as a core principle. This chapter explains how your data is protected.

---

## Privacy-First Design

### What Stays on Your Device

| Data Type | Location | Shared? |
|-----------|----------|---------|
| Voice recordings | Device only | Never |
| Conversation history | Device only | Never |
| Intent patterns | Device only | Never |
| Personal preferences | Device only | Optional sync |
| Documents (RAG) | Device only | Never |

### What's Never Collected

- Voice audio recordings
- Conversation transcripts
- Personal information
- Location data
- Contact information
- Usage patterns

---

## On-Device Processing

All AI processing happens locally:

```
Your Voice → Device Microphone → On-Device NLU → Local Action
              (never leaves)       (local AI)     (local only)
```

### Benefits

- No internet required for core features
- Instant response (no server round-trip)
- Complete privacy
- Works offline

---

## Crash Reporting (Opt-In Only)

### Default: Disabled

Crash reporting is **disabled by default** to protect your privacy. AVA will never send crash data without your explicit consent.

### How to Enable (Optional)

If you want to help improve AVA:

1. Go to **Settings > Privacy**
2. Enable **"Share Crash Reports"**
3. Only technical crash data is sent (no personal data)
4. You can disable anytime

### What's Included in Crash Reports

| Included | Not Included |
|----------|--------------|
| Stack traces (error locations) | Voice recordings |
| Device model (e.g., "Pixel 8") | Conversation history |
| OS version (e.g., "Android 14") | Personal data |
| App version | Location |
| Error messages (technical only) | Contacts, names, emails |

### Privacy Safeguards

When crash reporting is enabled:

- ✅ **Anonymized ID** - You're assigned a random ID (not linked to you)
- ✅ **No voice data** - Audio recordings never sent
- ✅ **No conversations** - Chat history stays on device
- ✅ **Local fallback** - If disabled, crashes logged locally only
- ✅ **90-day retention** - Data deleted after 90 days

**Related:** See [Chapter 73 - Production Readiness](../Developer-Manual-Chapter73-Production-Readiness-Security.md#crashreporter-firebase-crashlytics-integration) for technical details.

---

## Data Storage

### Local Database (Encrypted)

AVA stores all data in an encrypted local database with **AES-256-GCM encryption** always enabled:

- Conversations
- Learned intents
- User preferences
- Document indexes (RAG)
- Embedding vectors (always encrypted)

**Encryption Details:**

| Security Feature | Implementation |
|------------------|----------------|
| Encryption Algorithm | AES-256-GCM (military-grade) |
| Key Storage | Android Keystore (hardware-backed when available) |
| Authentication | 128-bit authentication tag |
| Status | Always ON (cannot be disabled) |

**RAG Document Security:**

All document embeddings in your RAG system are automatically encrypted before being stored:

- ✅ New documents: Encrypted immediately upon indexing
- ✅ Existing documents: Automatically encrypted on first access
- ✅ Document checksums: SHA-256 integrity verification
- ✅ Secure deletion: Encryption keys destroyed when documents deleted

**Related:** See [Chapter 19 - Privacy & Cloud Features](User-Manual-Chapter19-Privacy-Cloud-Features.md) for detailed encryption information.

### Clearing Data

To delete all local data:

1. **Settings > Privacy > Clear All Data**
2. Confirm deletion
3. App resets to initial state

---

## Permissions

### Required Permissions

| Permission | Purpose | When Used |
|------------|---------|-----------|
| Microphone | Voice input | When listening |
| Storage | Model files | App startup |

### Optional Permissions

| Permission | Purpose | Default |
|------------|---------|---------|
| Contacts | "Call John" commands | Disabled |
| Calendar | "Schedule meeting" | Disabled |
| Location | Weather/navigation | Disabled |

### Revoking Permissions

You can revoke any permission in system settings. AVA will gracefully disable related features.

---

## Network Access

### When AVA Uses Internet

| Feature | Internet Required |
|---------|------------------|
| Voice commands | No |
| Intent recognition | No |
| Device control | No |
| Model download | Yes (once) |
| Web search | Yes |
| Cloud sync | Yes (optional) |

### Firewall Friendly

AVA works behind firewalls. Only optional features require network access.

---

## Security Features

### Model Integrity

Downloaded models are verified with SHA256 checksums to prevent tampering.

### No Root Required

AVA does not require root/jailbreak. All features work on standard devices.

### Secure Storage

Sensitive data uses platform secure storage:
- Android: EncryptedSharedPreferences
- iOS: Keychain
- Desktop: OS credential manager

---

## GDPR Compliance

AVA complies with GDPR by design:

| Right | Implementation |
|-------|---------------|
| Right to access | All data viewable in Settings |
| Right to delete | Clear All Data option |
| Right to portability | Export data option |
| Right to object | Opt-out of all cloud features |

---

## Children's Privacy

AVA does not:
- Collect data from children
- Display advertising
- Contain in-app purchases
- Connect to social networks

---

## Contact

For privacy questions:
- Email: privacy@augmentalis.com
- Website: [Privacy Policy](https://ava-ai.com/privacy)

---

## Related Chapters

- [Chapter 13: Platform Support](User-Manual-Chapter13-Platform-Support.md)
- [Chapter 19: Privacy & Cloud Features](User-Manual-Chapter19-Privacy-Cloud-Features.md)
- [Chapter 73: Production Readiness & Security (Developer)](Developer-Manual-Chapter73-Production-Readiness-Security.md)
- [AVA Crash Reporting (Developer)](../AVA-CRASH-REPORTING.md)

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.1 | 2025-12-06 | Added encryption always-on notice, expanded crash reporting details with privacy safeguards |
| 1.0 | 2025-12-01 | Initial release |

---

**Updated:** 2025-12-06 (added encryption details and crash reporting privacy safeguards)
