# AVA Intent Examples - Universal Format v2.0

All `.ava` files use the **Avanues Universal Format v2.0** with 3-letter IPC codes.

## Format Structure

```
# Avanues Universal Format v1.0
# Type: AVA - Voice Intent Examples
# Extension: .ava
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: ava
metadata:
  file: example.ava
  category: voice_command
  name: Example Commands
  description: Description here
  priority: 1
  count: 5
---
VCM:intent_id:canonical example
VCM:intent_id:synonym example 1
VCM:intent_id:synonym example 2
---
synonyms:
  word: [synonym1, synonym2]
```

## IPC Codes

| Code | Meaning | Usage |
|------|---------|-------|
| VCM | Voice Command | Navigation, device control, media control, system control |
| AIQ | AI Query | Questions, information requests |
| STT | Speech to Text | Transcription, dictation |
| CTX | Context Share | Location, app state, user context |
| SUG | Suggestion | Recommendations, next actions |

All current `.ava` files use **VCM** (Voice Command) as they are action-oriented commands.

## Files

- `navigation.ava` - App launching and navigation (8 intents)
- `media-control.ava` - Music and media playback (10 intents)
- `system-control.ava` - System settings and device controls (12 intents)
- `voiceos-commands.ava` - VoiceOS system commands (large set)

## Database Storage

Intent examples are stored in the `intent_examples` table:

```kotlin
formatVersion = "v2.0"        // All files now v2.0
ipcCode = "VCM"              // From file entries
source = "AVA_FILE_UNIVERSAL_V2"
```

## Creating New Files

1. Copy the format structure above
2. Update metadata section (file, category, name, description, count)
3. Add entries in `CODE:intent_id:example_text` format
4. Group synonyms under the same intent_id
5. Add global synonyms section (optional)

## Cross-Project Compatibility

AVA can read intent files from other projects using `UniversalFileParser`:

| Extension | Project | AVA Can Read? |
|-----------|---------|---------------|
| .ava | AVA | ✅ Native |
| .vos | VoiceOS | ✅ Yes |
| .avc | AvaConnect | ✅ Yes |
| .awb | BrowserAvanue | ✅ Yes |
| .ami | MagicUI | ✅ Yes |
| .amc | MagicCode | ✅ Yes |

## References

- **Universal Format Spec:** `/Volumes/M-Drive/Coding/Avanues/docs/specifications/UNIVERSAL-FILE-FORMAT-FINAL.md`
- **Universal IPC Spec:** `/Volumes/M-Drive/Coding/Avanues/docs/specifications/UNIVERSAL-IPC-SPEC.md`
- **UniversalFileParser:** `/Volumes/M-Drive/Coding/Avanues/modules/AVAMagic/IPC/UniversalIPC/src/commonMain/kotlin/com/augmentalis/avamagic/ipc/UniversalFileParser.kt`

---

**Format:** Universal v2.0 ONLY (v1.0 JSON no longer supported)
**Last Updated:** 2025-11-20
