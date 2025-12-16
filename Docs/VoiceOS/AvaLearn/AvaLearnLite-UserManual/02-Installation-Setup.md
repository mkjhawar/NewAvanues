# Chapter 2: Installation & Setup

**Document:** VoiceOS-AvaLearnLite-UserManual-Ch02
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 2.1 System Requirements

### Minimum Requirements

| Requirement | Specification |
|-------------|---------------|
| Android Version | 14.0 (API 34) or higher |
| RAM | 4 GB minimum |
| Storage | 100 MB free space |
| VoiceOS Core | Version 2.0.0 or higher |

### Recommended Requirements

| Requirement | Specification |
|-------------|---------------|
| Android Version | 15.0 (API 35) |
| RAM | 6 GB or more |
| Storage | 500 MB free space |
| Display | 1080p or higher |

### Required Permissions

| Permission | Purpose | Required |
|------------|---------|----------|
| Accessibility Service | Read screen content | Yes |
| Storage | Save exported files | Yes |
| Foreground Service | Background operation | Yes |
| Bind Accessibility | Connect to VoiceOS | Yes |

---

## 2.2 Installation

### 2.2.1 Installing with VoiceOS

AvaLearnLite is included with the VoiceOS installation package.

1. Install VoiceOS from the provided APK or app store
2. AvaLearnLite is automatically installed
3. Find "AvaLearnLite" in your app drawer

### 2.2.2 Standalone Installation

If installing separately:

1. Download `AvaLearnLite-v2.0.0.apk`
2. Enable "Install from unknown sources" in Settings
3. Open the APK file
4. Tap "Install"
5. Wait for installation to complete

### 2.2.3 Verifying Installation

After installation, verify:

| Check | How to Verify |
|-------|---------------|
| App installed | "AvaLearnLite" appears in app drawer |
| Correct version | Settings > Apps > AvaLearnLite > Version 2.0.0 |
| Theme correct | App opens with Ocean Blue light theme |

---

## 2.3 Initial Setup

### 2.3.1 First Launch

When you first open AvaLearnLite:

1. **Welcome screen** appears (if first time)
2. **Permission requests** may appear
3. **Service check** verifies VoiceOS is running

### 2.3.2 Enabling VoiceOS Accessibility Service

AvaLearnLite requires VoiceOS Core's accessibility service to be enabled.

**Steps:**

1. Open device **Settings**
2. Go to **Accessibility**
3. Find **VoiceOS** in the list
4. Tap to open
5. Toggle **"Use VoiceOS"** ON
6. Confirm the permission dialog
7. Return to AvaLearnLite

**Verification:**
- AvaLearnLite shows "JIT Status: Active" (green badge)

### 2.3.3 Granting Storage Permission

For exporting learned data:

1. Open AvaLearnLite
2. If prompted, tap **"Allow"** for storage
3. Or manually: Settings > Apps > AvaLearnLite > Permissions > Storage > Allow

---

## 2.4 Configuration

### 2.4.1 App Settings

AvaLearnLite settings are accessed via the system Settings app:

**Path:** Settings > Apps > AvaLearnLite

| Setting | Options | Default |
|---------|---------|---------|
| Storage | Internal / External | Internal |
| Notifications | On / Off | On |
| Battery optimization | Optimized / Unrestricted | Unrestricted recommended |

### 2.4.2 Battery Optimization

For best performance, disable battery optimization:

1. Settings > Apps > AvaLearnLite
2. Tap "Battery"
3. Select "Unrestricted"

This prevents Android from killing the app during exploration.

### 2.4.3 Export Location

Default export location:
```
/storage/emulated/0/Android/data/com.augmentalis.avalearnlite/files/learned_apps/
```

Files are automatically picked up by VoiceOS CommandManager.

---

## 2.5 Verifying Setup

### Setup Checklist

| Step | Status | How to Verify |
|------|--------|---------------|
| App installed | [ ] | App appears in drawer |
| VoiceOS running | [ ] | JIT Status shows "Active" |
| Accessibility enabled | [ ] | Service shows in Accessibility settings |
| Storage permission | [ ] | Export button is enabled |
| Battery unrestricted | [ ] | App doesn't get killed |

### Quick Test

1. Open AvaLearnLite
2. Verify "JIT Learning Status" shows **Active** (green)
3. Verify "Screens Learned" shows a number (may be 0 if new)
4. Press **Refresh** button - stats should update
5. If all green, setup is complete!

---

## 2.6 Troubleshooting Setup Issues

### Issue: "Service not bound"

**Cause:** VoiceOS Core is not running

**Solution:**
1. Open Settings > Accessibility
2. Find VoiceOS
3. Toggle OFF then ON again
4. Return to AvaLearnLite

### Issue: "JIT Status: Paused"

**Cause:** JIT service is paused

**Solution:**
1. In AvaLearnLite, press "Resume" button
2. Status should change to "Active"

### Issue: Export button disabled

**Cause:** Storage permission not granted

**Solution:**
1. Settings > Apps > AvaLearnLite > Permissions
2. Enable "Storage" permission
3. Restart AvaLearnLite

### Issue: App killed in background

**Cause:** Battery optimization

**Solution:**
1. Settings > Apps > AvaLearnLite > Battery
2. Select "Unrestricted"

---

## 2.7 Updating AvaLearnLite

### Automatic Updates

If installed via app store, updates are automatic.

### Manual Updates

1. Download new APK
2. Install over existing (data preserved)
3. Verify version in Settings > Apps

### Version Compatibility

| AvaLearnLite | VoiceOS Core | Compatible |
|--------------|--------------|------------|
| 2.0.x | 2.0.x | Yes |
| 2.0.x | 1.x.x | No |
| 1.x.x | 2.0.x | No |
| 1.x.x | 1.x.x | Yes |

---

## 2.8 Uninstallation

### Uninstalling AvaLearnLite

1. Settings > Apps > AvaLearnLite
2. Tap "Uninstall"
3. Confirm

**Note:** Exported AVU files remain in storage unless manually deleted.

### Clearing Data Only

To reset without uninstalling:

1. Settings > Apps > AvaLearnLite
2. Tap "Storage"
3. Tap "Clear Data"

This removes all exploration history but keeps the app installed.

---

## 2.9 Next Steps

Your AvaLearnLite is now set up and ready to use.

Continue to [Chapter 3: User Interface Guide](./03-User-Interface.md) for a complete walkthrough of the app interface.

---

**End of Chapter 2**
