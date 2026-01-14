# Universal File Format Specification v1.1

**Version:** 1.1.0
**Date:** 2025-11-20
**Status:** Active Standard
**Author:** Manoj Jhawar (manoj@ideahq.net)

---

## CORRECTED Extension Mapping

| Extension | Project | Purpose | Meaning |
|-----------|---------|---------|---------|
| `.ava` | AVA | Voice intent examples | **Ava** (AI Voice Assistant) |
| `.vos` | VoiceOS | System commands & plugins | **Voice OS** |
| `.avc` | AvaConnect | Device pairing & IPC | **Ava Connect** |
| `.awb` | BrowserAvanue/WebAvanue | Browser commands | **Ava Web Browser** |
| `.apf` | NewAvanue/Avanues Platform | Platform config | **Ava Platform Format** |
| `.aui` | Avanues UI | UI DSL components | **Ava UI** |

**File Locations:**
```
/storage/emulated/0/Android/data/com.augmentalis.*/files/
├── .ava/     # AVA files
├── .vos/     # VoiceOS files
├── .avc/     # AvaConnect files
├── .awb/     # WebAvanue (Browser) files
├── .apf/     # Platform files
└── .aui/     # UI DSL files
```

[Rest of spec content remains the same, just replacing:
- .avw → .awb
- .avn → .apf
- .avs → .aui
]

---

## Quick Reference Table

| Code Category | Used In | Extensions |
|---------------|---------|------------|
| Voice (VCM, STT, TTS) | All | .ava, .vos, .avc, .awb |
| AI (AIQ, AIR, CTX) | AVA, VOS, Browser | .ava, .vos, .awb |
| Communication (VCA, ACC, CHT) | AvaConnect | .avc |
| Browser (URL, NAV, TAB) | Browser, All | .awb, .ava, .vos |
| UI (JSN) | All | .aui, .ava, .vos, .avc |
| Platform (HND, CAP, PRO) | Platform | .apf |

---

See full specification for complete details.
