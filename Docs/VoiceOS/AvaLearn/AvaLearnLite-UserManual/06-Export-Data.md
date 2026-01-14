# Chapter 6: Export & Data Management

**Document:** VoiceOS-AvaLearnLite-UserManual-Ch06
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 6.1 Export Overview

### 6.1.1 What is AVU Export?

AVU (Avanues Universal) export converts your exploration data into a format that VoiceOS can import and use for voice control.

### 6.1.2 Export Flow

```
+------------------+     +------------------+     +------------------+
|                  |     |                  |     |                  |
| Exploration Data | --> | AVU Generator    | --> | .vos File        |
|                  |     |                  |     |                  |
+------------------+     +------------------+     +------------------+
                                                          |
                                                          v
                                                 +------------------+
                                                 |                  |
                                                 | VoiceOS Import   |
                                                 |                  |
                                                 +------------------+
```

---

## 6.2 AVU Format

### 6.2.1 File Extension

| Extension | Usage |
|-----------|-------|
| `.vos` | VoiceOS command file |

### 6.2.2 File Structure Overview

AVU files use a compact, line-based format with record type prefixes:

```
# Avanues Universal Format v1.0
# Type: VOS
---
schema: avu-1.0
version: 1.0.0
locale: en-US
metadata:
  file: com.example.app.vos
  category: learned_app
  count: 87
---
APP:com.example.app:Example App:1702300800000
STA:5:87:42:2.3:5:75.5
SCR:abc123:MainActivity:1702300801000:15
ELM:uuid1:Login:Button:C:0,540,1080,640:ACT
CMD:cmd1:login:click:uuid1:0.95
---
synonyms:
  login: [sign in, log in]
```

### 6.2.3 Record Types

| Prefix | Name | Purpose |
|--------|------|---------|
| `APP` | Application | App metadata |
| `STA` | Statistics | Exploration stats |
| `SCR` | Screen | Screen definition |
| `ELM` | Element | UI element |
| `NAV` | Navigation | Screen transitions |
| `CMD` | Command | Voice command |

### 6.2.4 Record Details

**APP Record:**
```
APP:{packageName}:{appName}:{timestamp}
```

| Field | Example |
|-------|---------|
| packageName | com.example.app |
| appName | Example App |
| timestamp | 1702300800000 (Unix ms) |

**STA Record:**
```
STA:{screens}:{elements}:{commands}:{avgTime}:{loops}:{coverage}
```

| Field | Example |
|-------|---------|
| screens | 5 (screen count) |
| elements | 87 (element count) |
| commands | 42 (command count) |
| avgTime | 2.3 (avg exploration time) |
| loops | 5 (loops detected) |
| coverage | 75.5 (coverage percent) |

**SCR Record:**
```
SCR:{screenHash}:{activityName}:{timestamp}:{elementCount}
```

**ELM Record:**
```
ELM:{uuid}:{name}:{type}:{actions}:{bounds}:{state}
```

| Field | Values |
|-------|--------|
| uuid | Unique identifier |
| name | Display name |
| type | Button, EditText, etc. |
| actions | C(click), L(long), E(edit), S(scroll) |
| bounds | left,top,right,bottom |
| state | ACT(active), DIS(disabled), HID(hidden) |

**CMD Record:**
```
CMD:{cmdId}:{phrase}:{action}:{targetUuid}:{confidence}
```

---

## 6.3 Performing Export

### 6.3.1 Prerequisites

Before exporting, ensure:

| Requirement | How to Verify |
|-------------|---------------|
| Exploration completed | Phase is IDLE or COMPLETED |
| Elements discovered | Elements count > 0 |
| Storage permission | Export button is enabled |

### 6.3.2 Export Steps

1. **Complete exploration**
   - Stop exploration if still running
   - Verify elements were discovered

2. **Locate Export card**
   - Scroll to bottom of screen
   - Find "Export" card

3. **Press Export button**
   - Tap "Export to AVU (.vos)"
   - Wait for completion

4. **Verify success**
   - Success message appears
   - "Last export" shows filename

### 6.3.3 Export Progress

| Stage | Indicator |
|-------|-----------|
| Starting | Button disabled, processing |
| Generating | Progress indicator (if shown) |
| Writing | File being saved |
| Complete | Success message, filename shown |
| Failed | Error message |

---

## 6.4 File Storage

### 6.4.1 Export Location

Files are saved to the app's private storage:

```
/storage/emulated/0/Android/data/com.augmentalis.avalearnlite/files/learned_apps/
```

### 6.4.2 File Naming

| Component | Value |
|-----------|-------|
| Name | Package name of explored app |
| Extension | .vos |

**Example:**
```
com.google.android.gm.vos  (Gmail)
com.whatsapp.vos           (WhatsApp)
com.spotify.music.vos      (Spotify)
```

### 6.4.3 Storage Structure

```
/Android/data/com.augmentalis.avalearnlite/
└── files/
    └── learned_apps/
        ├── com.google.android.gm.vos
        ├── com.whatsapp.vos
        └── com.spotify.music.vos
```

---

## 6.5 VoiceOS Import

### 6.5.1 Automatic Import

VoiceOS automatically imports exported files:

1. **File watcher active**
   - CommandManager monitors learned_apps folder
   - New files detected within seconds

2. **Automatic parsing**
   - AVU file is validated
   - Records are parsed

3. **Database insertion**
   - Commands added to VoiceOS database
   - Indexed for voice recognition

4. **Ready for use**
   - Voice commands work immediately
   - No restart required

### 6.5.2 Manual Import

If automatic import doesn't work:

1. Open VoiceOS settings
2. Navigate to Commands > Import
3. Select the .vos file
4. Confirm import

### 6.5.3 Import Verification

Verify commands were imported:

1. Open target app
2. Say "VoiceOS, show commands"
3. Commands for the app should appear

---

## 6.6 Data Management

### 6.6.1 Viewing Exported Files

Using a file manager:

1. Navigate to Android/data
2. Find com.augmentalis.avalearnlite
3. Open files/learned_apps
4. View .vos files

**Note:** Android 11+ may restrict file manager access.

### 6.6.2 Deleting Exported Files

To remove a learned app:

1. Delete the .vos file
2. Restart VoiceOS
3. Commands will be removed

Or in VoiceOS:

1. Settings > Commands > Manage
2. Find the app
3. Tap Delete

### 6.6.3 Backing Up Data

To backup your learned apps:

1. Connect device to computer
2. Navigate to the learned_apps folder
3. Copy all .vos files
4. Store in safe location

### 6.6.4 Restoring Data

To restore from backup:

1. Copy .vos files back to learned_apps folder
2. VoiceOS will auto-import
3. Verify commands are available

---

## 6.7 Re-export Scenarios

### 6.7.1 When to Re-export

| Scenario | Action |
|----------|--------|
| App was updated | Re-explore and export |
| Commands not working | Re-explore problem screens |
| Missing commands | Explore missed areas, re-export |
| Better coverage needed | More thorough exploration |

### 6.7.2 Re-export Behavior

| Setting | Behavior |
|---------|----------|
| Same app | Overwrites previous file |
| Different app | Creates new file |
| Merged data | New data replaces old |

### 6.7.3 Incremental Updates

Currently, re-export fully replaces the previous file. For best results:
- Explore all important screens
- Don't just explore changed areas
- Complete full exploration before export

---

## 6.8 Troubleshooting Export

### 6.8.1 Export Button Disabled

| Cause | Solution |
|-------|----------|
| No elements discovered | Complete an exploration first |
| Storage permission denied | Grant storage permission |
| Service not bound | Restart AvaLearnLite |

### 6.8.2 Export Fails

| Error | Solution |
|-------|----------|
| "Storage full" | Free up device storage |
| "Permission denied" | Check app permissions |
| "Write error" | Restart app, try again |

### 6.8.3 File Not Imported

| Cause | Solution |
|-------|----------|
| VoiceOS not running | Start VoiceOS service |
| File watcher disabled | Restart VoiceOS |
| Corrupted file | Re-export the data |
| Invalid format | Re-explore and export |

---

## 6.9 Data Privacy

### 6.9.1 What's in the Export

| Included | Not Included |
|----------|--------------|
| UI element positions | Your personal data |
| Element labels/text | Passwords |
| Screen structure | Typed content |
| Navigation paths | Private messages |
| Generated commands | Account information |

### 6.9.2 File Security

| Aspect | Implementation |
|--------|----------------|
| Storage | App-private directory |
| Encryption | User edition: encrypted |
| Access | Only AvaLearnLite can write |

### 6.9.3 Sharing Exported Files

Exported files can be shared to:
- Help others learn the same app
- Backup to cloud storage
- Transfer to another device

**Warning:** Only share with trusted recipients.

---

## 6.10 File Size Considerations

### 6.10.1 Typical File Sizes

| App Complexity | Screens | Elements | File Size |
|----------------|---------|----------|-----------|
| Simple | 5-10 | 50 | 5-10 KB |
| Medium | 20-30 | 200 | 20-50 KB |
| Complex | 50+ | 500+ | 100+ KB |

### 6.10.2 Factors Affecting Size

| Factor | Impact |
|--------|--------|
| Number of screens | More screens = larger file |
| Elements per screen | More elements = larger file |
| Command synonyms | More synonyms = larger file |
| Navigation complexity | More paths = larger file |

---

## 6.11 Best Practices

### 6.11.1 Export Checklist

| Step | Verify |
|------|--------|
| 1. Exploration complete | Phase shows IDLE or COMPLETED |
| 2. Good coverage | Coverage > 50% recommended |
| 3. Key screens visited | Main features explored |
| 4. Menus opened | Menu items captured |
| 5. Export successful | Filename shown in UI |

### 6.11.2 Maintenance Schedule

| Frequency | Action |
|-----------|--------|
| After app updates | Re-explore changed areas |
| Monthly | Verify commands still work |
| When issues occur | Re-export affected apps |

---

## 6.12 Next Steps

For help with common issues, see [Chapter 7: Troubleshooting & FAQ](./07-Troubleshooting.md).

---

**End of Chapter 6**
