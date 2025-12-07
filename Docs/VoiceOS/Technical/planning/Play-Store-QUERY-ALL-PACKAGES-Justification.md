# Play Store QUERY_ALL_PACKAGES Permission Justification

**App Name:** VoiceOS
**Package Name:** com.augmentalis.voiceos
**Permission:** QUERY_ALL_PACKAGES
**Date:** 2025-10-31
**Author:** Manoj Jhawar

---

## Permission Request Summary

VoiceOS requires the `QUERY_ALL_PACKAGES` permission to provide core accessibility functionality for users with disabilities who rely on voice control to operate their Android devices.

---

## Core Use Case: Launcher App Detection

**Purpose:** Detect and exclude launcher apps from accessibility scraping to prevent system instability.

**Technical Implementation:**
- Location: `LauncherDetector.kt` (modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/LauncherDetector.kt)
- Functionality: Queries installed packages to identify apps with `CATEGORY_HOME` intent
- Safety: Prevents VoiceOS from attempting to control launcher apps, which can cause:
  - System UI instability
  - Navigation loop errors
  - User experience degradation

**Code Reference:**
```kotlin
fun isLauncherApp(packageName: String): Boolean {
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
    }
    val launcherApps = packageManager.queryIntentActivities(
        homeIntent,
        PackageManager.MATCH_DEFAULT_ONLY
    )
    return launcherApps.any { it.activityInfo.packageName == packageName }
}
```

---

## Why QUERY_ALL_PACKAGES is Required

### Android 11+ Package Visibility Changes

Starting with Android 11 (API 30), the default package visibility is restricted. Without `QUERY_ALL_PACKAGES`:

1. **queryIntentActivities()** returns incomplete results
2. **Launcher detection fails** - Cannot identify all launcher apps
3. **System instability risk** - May attempt to scrape launcher apps
4. **Core functionality broken** - Accessibility service cannot safely operate

### Alternative Approaches Considered

| Alternative | Why Not Viable |
|-------------|----------------|
| **\<queries\> element** | Cannot predict all launcher packages users have installed |
| **Limited package queries** | Insufficient - need to detect ALL launchers, not just known ones |
| **Request from user** | Poor UX - users with disabilities shouldn't manually configure launchers |

---

## Accessibility Justification

VoiceOS is an **accessibility service** designed for users with:
- Motor disabilities (cannot use touchscreens)
- Visual impairments (need voice navigation)
- Limited dexterity (voice control preferred)

**QUERY_ALL_PACKAGES enables:**
1. **Safe operation** - Detect and avoid system-critical apps
2. **Comprehensive coverage** - Work across all user-installed apps
3. **User safety** - Prevent accessibility failures that could lock users out

---

## Privacy Considerations

### Data Collection: NONE

VoiceOS does **NOT**:
- ❌ Collect package names for analytics
- ❌ Send installed app lists to servers
- ❌ Share user app inventory with third parties
- ❌ Track user behavior across apps

### Data Usage: LOCAL ONLY

VoiceOS **ONLY** uses package visibility to:
- ✅ Detect launcher apps (local check)
- ✅ Exclude system-critical packages (local filtering)
- ✅ Enable accessibility features (device-only processing)

**All package queries are performed locally and never transmitted.**

---

## User Transparency

### Privacy Policy Disclosure

VoiceOS privacy policy explicitly states:
- Permission purpose (launcher detection)
- No data collection/transmission
- Local-only processing
- User control over accessibility features

**Location:** `/docs/planning/VoiceOS-Privacy-Policy.md`

### In-App Disclosure

First-time permission request includes:
- Clear explanation of why permission is needed
- Link to privacy policy
- User consent required before enabling

---

## Technical Necessity Score

| Criteria | Score | Justification |
|----------|-------|---------------|
| **Core Functionality** | ✅ Critical | Accessibility service cannot operate safely without it |
| **No Alternative** | ✅ True | Android 11+ restrictions make \<queries\> insufficient |
| **User Benefit** | ✅ High | Prevents system instability for users with disabilities |
| **Privacy Impact** | ✅ Low | No data collection, local-only processing |

---

## Compliance with Play Store Policy

### Policy Requirements Met

1. ✅ **Prominent disclosure** - Privacy policy and in-app explanation
2. ✅ **Core functionality** - Required for safe accessibility operation
3. ✅ **User transparency** - Clear explanation of purpose
4. ✅ **No alternatives** - Android 11+ restrictions necessitate permission
5. ✅ **Privacy-preserving** - No data collection or transmission

### Policy Section: Accessibility Services

VoiceOS qualifies as an accessibility service under Play Store policy:
- Registered `AccessibilityService` (VoiceOSService)
- Serves users with disabilities
- Requires package visibility for safe operation
- No data collection or monetization of user data

---

## Supporting Documentation

**Technical Implementation:**
- `LauncherDetector.kt` - Launcher detection logic
- `AccessibilityScrapingIntegration.kt` - Scraping safety checks
- `VoiceOSService.kt` - Accessibility service implementation

**Privacy Documentation:**
- Privacy Policy (updated 2025-10-31)
- Permission disclosure screens
- User consent flows

**Testing Evidence:**
- Phase 3B: Permission Hardening implementation
- Fallback behavior when permission denied
- User safety without permission (limited functionality)

---

## Fallback Behavior

If user denies QUERY_ALL_PACKAGES:

1. ✅ **App continues to function** - Core features still work
2. ⚠️ **Limited launcher detection** - Uses known launchers list
3. ⚠️ **Reduced safety** - May encounter launcher apps
4. ℹ️ **User warning** - Notified of reduced functionality

**Implementation:** `LauncherDetector.getFallbackLaunchers()` maintains known launchers list

---

## Conclusion

VoiceOS's use of `QUERY_ALL_PACKAGES` is:
- **Necessary** for safe accessibility operation
- **Privacy-preserving** with no data collection
- **User-transparent** with clear disclosures
- **Policy-compliant** with Play Store requirements
- **Accessibility-focused** serving users with disabilities

The permission enables critical safety features for an accessibility service and includes appropriate fallbacks, privacy protections, and user transparency.

---

**Prepared By:** Manoj Jhawar
**Review Date:** 2025-10-31
**Approval Status:** Pending Play Store Review
**Version:** 1.0.0
