# VoiceOS Commands - Universal Format v2.0

All `.vos` files use the **Avanues Universal Format v2.0** with 3-letter IPC codes.

## Format Structure

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
VCM:command_id:canonical example
VCM:command_id:synonym 1
VCM:command_id:synonym 2
---
synonyms:
  word: [synonym1, synonym2]
```

## IPC Codes

| Code | Meaning | Usage |
|------|---------|-------|
| VCM | Voice Command | System commands, accessibility actions, device control |
| AIQ | AI Query | Questions, information requests |
| STT | Speech to Text | Transcription, dictation |
| CTX | Context Share | App state, screen context |
| SUG | Suggestion | Command suggestions, auto-complete |

All VoiceOS commands use **VCM** (Voice Command) for system-level actions.

## Files

- `voiceos-commands.vos` - Complete VoiceOS command set (94 commands)
  - Connectivity: WiFi, Bluetooth, Airplane mode
  - Display: Brightness, rotation, screen timeout
  - Volume: Media, ring, alarm, notification
  - System: Power, accessibility, settings
  - Navigation: Home, back, recents, notifications

## Creating New Files

1. Copy the format structure above
2. Update metadata (file, category, name, count)
3. Add commands in `VCM:command_id:example` format
4. Group synonyms under same command_id
5. Add global synonyms section

## Cross-Project Compatibility

VoiceOS can read command files from other projects using `UniversalFileParser`:

| Extension | Project | VoiceOS Can Read? |
|-----------|---------|-------------------|
| .vos | VoiceOS | ✅ Native |
| .ava | AVA | ✅ Yes |
| .avc | AvaConnect | ✅ Yes |
| .awb | BrowserAvanue | ✅ Yes |
| .ami | MagicUI | ✅ Yes |
| .amc | MagicCode | ✅ Yes |

## References

- **Universal Format Spec:** `/Volumes/M-Drive/Coding/Avanues/docs/specifications/UNIVERSAL-FILE-FORMAT-FINAL.md`
- **Universal IPC Spec:** `/Volumes/M-Drive/Coding/Avanues/docs/specifications/UNIVERSAL-IPC-SPEC.md`
- **UniversalFileParser:** `/Volumes/M-Drive/Coding/Avanues/modules/AVAMagic/IPC/UniversalIPC/`

---

**Format:** Universal v2.0 ONLY
**Last Updated:** 2025-11-20
