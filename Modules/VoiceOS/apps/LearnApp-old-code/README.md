# Old LearnApp Code - Reference Only

This directory contains the original standalone LearnApp and LearnAppDev code before they were integrated into VoiceOSCore.

**Date Archived:** 2025-12-23
**Source Commit:** 5e5fac034 (2025-12-19)
**Reason:** Architecture changed in Phase 5 (Dec 22, 2025) - LearnApp functionality integrated into VoiceOSCore

## Contents
- LearnApp/ - Standalone user edition
- LearnAppDev/ - Developer edition with Neo4j debugging

## Current Architecture (Post-Phase 5)
- LearnApp code is now in: `apps/VoiceOSCore/src/main/java/.../learnapp/` (99 Kotlin files)
- Shared logic is in: `libraries/LearnAppCore/`

## What Changed
Phase 5 implementation (Dec 22, 2025) rewrote LearnApp as a three-tier system integrated into VoiceOSCore:
- JIT (Free) → Always-on passive learning
- LearnAppLite ($2.99/mo) → Deep menu scanning
- LearnAppPro ($9.99/mo) → Full exploration + export

**Do not use these apps in builds - they are for reference only.**

## Related Documentation
- Phase 5 Manual: `/Volumes/M-Drive/Coding/NewAvanues/Docs/VoiceOS/manuals/chapters/VoiceOS-Chapter-LearnApp-Phase5-JIT-Lite-Integration-5221220-V1.md`
- Developer Manual: `/Volumes/M-Drive/Coding/NewAvanues/Docs/VoiceOS/manuals/developer/VoiceOS-P2-Features-Developer-Manual-51211-V1.md`
