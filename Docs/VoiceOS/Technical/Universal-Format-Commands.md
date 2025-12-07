# VoiceOS Commands - Universal Format v2.0

**Last Updated:** 2025-11-20  
**Status:** Active  
**Format Version:** 2.0

---

## Overview

VoiceOS uses the **Avanues Universal Format v2.0** (.vos files) for all system command definitions. This format provides a human-readable, IPC-ready structure with 3-letter VCM (Voice Command) codes.

### Benefits

1. **Human-Readable:** Clear command definitions without JSON
2. **IPC Integration:** Direct command routing via VCM codes
3. **Cross-Project Sharing:** Compatible with AVA, AvaConnect, and all Avanues projects
4. **Smaller Size:** 16% reduction vs JSON (38KB → 32KB)
5. **Zero-Conversion:** File format matches IPC wire format

---

## Format Structure

All `.vos` files follow this structure:

```
# Avanues Universal Format v1.0
# Type: VOS - Voice Operating System Commands
# Extension: .vos
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: voiceos
metadata:
  file: example.vos
  category: system
  name: Example Commands
  count: 10
---
VCM:command_id:canonical command
VCM:command_id:synonym 1
VCM:command_id:synonym 2
---
synonyms:
  turn_on: [enable, activate, start]
```

---

## Command Categories (94 Total)

### Connectivity (~20 commands)
- WiFi on/off
- Bluetooth on/off
- Airplane mode on/off
- Mobile data on/off
- Hotspot control

### Display (~15 commands)
- Brightness up/down
- Screen rotation lock/unlock
- Screen timeout adjustment
- Night mode on/off
- Auto-brightness toggle

### Volume (~20 commands)
- Media volume up/down
- Ring volume up/down
- Alarm volume up/down
- Notification volume up/down
- System volume control
- Mute/unmute

### System (~25 commands)
- Power off
- Reboot
- Lock screen
- Screenshot
- Flashlight on/off
- Settings shortcuts

### Navigation (~14 commands)
- Home
- Back
- Recents
- Notifications
- Quick settings

---

## File Location

```
app/src/main/assets/vos-commands/
├── en-US/
│   └── voiceos-commands.vos  (94 commands, 32KB)
└── README.md
```

---

## Example Commands

### Connectivity
```
VCM:turn_on_bluetooth:turn on bluetooth
VCM:turn_on_bluetooth:bluetooth on
VCM:turn_on_bluetooth:enable bluetooth
VCM:turn_on_bluetooth:activate bluetooth

VCM:turn_on_wifi:turn on wifi
VCM:turn_on_wifi:wifi on
VCM:turn_on_wifi:enable wifi
```

### Display
```
VCM:brightness_up:brightness up
VCM:brightness_up:increase brightness
VCM:brightness_up:brighter screen

VCM:screen_rotation_lock:lock screen rotation
VCM:screen_rotation_lock:disable auto rotate
```

### Volume
```
VCM:volume_up_media:media volume up
VCM:volume_up_media:increase media volume
VCM:volume_up_media:louder

VCM:mute_ring:mute ring
VCM:mute_ring:silence ringer
```

---

## IPC Integration

All VoiceOS commands use **VCM** (Voice Command) IPC code:

```kotlin
// Command execution via IPC
val command = "VCM:turn_on_bluetooth:turn on bluetooth"
sendIPCMessage(command)
```

### AVA → VoiceOS Delegation

AVA can delegate commands to VoiceOS:

```kotlin
// In AVA
val intent = intentClassifier.classify("turn on bluetooth")
if (intent.ipcCode == "VCM" && isVoiceOSInstalled()) {
    delegateToVoiceOS(intent)
}
```

---

## Cross-Project Compatibility

VoiceOS can read command files from other projects:

| Extension | Project | VoiceOS Can Read? |
|-----------|---------|-------------------|
| `.vos` | VoiceOS | ✅ Native |
| `.ava` | AVA | ✅ Yes |
| `.avc` | AvaConnect | ✅ Yes |
| `.awb` | BrowserAvanue | ✅ Yes |
| `.ami` | MagicUI | ✅ Yes |
| `.amc` | MagicCode | ✅ Yes |

---

## Creating New Command Files

1. Create file: `app/src/main/assets/vos-commands/en-US/my-commands.vos`
2. Add header and metadata
3. Add command entries with VCM code
4. Add global synonyms
5. Reference from VoiceOS command processor

---

## References

- **Universal Format Spec:** `/Volumes/M-Drive/Coding/Avanues/docs/specifications/UNIVERSAL-FILE-FORMAT-FINAL.md`
- **Universal IPC Spec:** `/Volumes/M-Drive/Coding/Avanues/docs/specifications/UNIVERSAL-IPC-SPEC.md`
- **README:** `app/src/main/assets/vos-commands/README.md`
- **UniversalFileParser:** `/Volumes/M-Drive/Coding/Avanues/modules/AVAMagic/IPC/UniversalIPC/`

---

**Status:** ✅ Production Ready  
**Format:** Universal v2.0 (.vos)  
**Total Commands:** 94
