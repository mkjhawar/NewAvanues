# NewAvanues Modules

Living document tracking all modules in the monorepo.

---

## Apps

| ID | Name | Type | Platforms | Path | Status |
|----|------|------|-----------|------|--------|
| VoiceOS-Android | VoiceOS | app | android | android/voiceos/ | Planned |
| VoiceOS-iOS | VoiceOS | app | ios | ios/voiceos/ | Planned |
| VoiceOS-Desktop | VoiceOS | app | desktop | desktop/voiceos/ | Planned |
| AVA-Android | AVA | app | android | android/ava/ | Planned |
| AVA-iOS | AVA | app | ios | ios/ava/ | Planned |
| AVA-Desktop | AVA | app | desktop | desktop/ava/ | Planned |
| Avanues-Android | Avanues | app | android | android/avanues/ | Planned |
| AvaConnect-Android | AvaConnect | app | android | android/avaconnect/ | Planned |

---

## Common Libraries

| ID | Name | Path | Platforms | Status |
|----|------|------|-----------|--------|
| Database | Database | Common/Database/ | android, ios, desktop | Planned |
| Voice | Voice | Common/Voice/ | android, ios | Planned |
| NLU | NLU | Common/NLU/ | android, ios, desktop | Planned |
| UI | UI | Common/UI/ | all | Planned |
| Utils | Utils | Common/Utils/ | all | Planned |

---

## Feature Modules

| ID | Name | Path | Platforms | Status |
|----|------|------|-----------|--------|
| WebAvanue | WebAvanue | Modules/WebAvanue/ | android, ios, desktop | Planned |
| AvaConnect | AvaConnect | Modules/AvaConnect/ | android, web | Planned |

---

## Dependencies

```
VoiceOS
├── Database
├── Voice
├── NLU
├── UI
└── Utils

AVA
├── Database
├── Voice
├── NLU
├── UI
└── Utils

WebAvanue
├── Database
├── UI
└── Utils

AvaConnect
├── Database
├── UI
└── Utils
```

---

## Migration Status

| Source Repo | Target | Status | Notes |
|-------------|--------|--------|-------|
| /Volumes/M-Drive/Coding/VoiceOS | android/voiceos/ | Not Started | |
| /Volumes/M-Drive/Coding/AVA | android/ava/ | Not Started | |
| /Volumes/M-Drive/Coding/Avanues | android/avanues/ | Not Started | |
| /Volumes/M-Drive/Coding/AvaConnect | android/avaconnect/ | Not Started | |
| MainAvanues/Modules/WebAvanue | Modules/WebAvanue/ | Not Started | |

---

*Version: 1 | Updated: 2025-12-03*
