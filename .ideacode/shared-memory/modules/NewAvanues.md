# Module Memory: NewAvanues

**Updated:** 2026-02-06T21:00:00-08:00
**Active Sessions:** 0

---

## Current State

| Aspect | Status |
|--------|--------|
| Build | VoiceOSCore: SUCCESS, VoiceDataManager: SUCCESS |
| Tests | Not run (compile fixes only) |
| Last Commit | f6112393 - fix(voiceoscore, voicedatamanager): Resolve all compile errors |
| Active Branch | claude/060226-avu-dsl-evolution (for AVU DSL work) |
| Fixes Branch | claude/040226-21-consolidated-master-EQyzV (pushed) |

---

## Recent Work (2026-02-06)

### Completed: Compile Error Resolution
- 300+ compile errors fixed across VoiceOSCore and VoiceDataManager
- 9 duplicate files removed from VoiceOSCore (voicedatamanager copies)
- glassMorphism imports corrected (from datamanager.ui)
- VoiceCursorAPI bridge removed, direct implementation
- RPC server stubs fixed
- NumberToWords.kt removed (unused)
- **Commit:** f6112393 on claude/040226-21-consolidated-master-EQyzV

### In Progress: AVU DSL Evolution
- Plan completed (see mighty-plotting-rose.md)
- Three-layer architecture designed: Wire Protocol → DSL → Interpreter
- Plugin system redesigned: .avp text files replace APK/JAR loading
- Developer Manual Chapters 81-87 planned (continuing from Ch80)
- **Handover:** Docs/VoiceOSCore/Handover/VoiceOSCore-Handover-CompileFixes-AVU-DSL-260206.md

---

## Pending Tasks

| Task | Priority | Branch |
|------|----------|--------|
| Create AVU DSL workdoc | HIGH | claude/060226-avu-dsl-evolution |
| Write Developer Manual Ch81-87 | HIGH | claude/060226-avu-dsl-evolution |
| Implement AVU DSL Parser (Phase 1) | CRITICAL | claude/060226-avu-dsl-evolution |
| Remove fake gRPC server | MEDIUM | TBD |
| Create shared UI module (glassMorphism) | MEDIUM | TBD |

---

## Key Decisions

| Decision | Date | Context |
|----------|------|---------|
| AVU DSL replaces MacroDSL.kt | 2026-02-06 | App Store compliance, cross-platform |
| .avp text files replace APK/JAR plugins | 2026-02-06 | No DexClassLoader, declarative |
| Keep JSON-RPC + AVU RPC, remove gRPC | 2026-02-06 | gRPC server is fake/non-functional |
| Dev manual chapters continue from Ch81 | 2026-02-06 | Existing manual goes to Ch80 |
| Chapters go in Docs/AVA/ideacode/guides/ | 2026-02-06 | Same location as Ch28-80 |

---

## Dependencies

| Module | Depends On | Notes |
|--------|-----------|-------|
| VoiceOSCore | VoiceDataManager | glassMorphism UI imports |
| VoiceOSCore | AVUCodec | AvuCodeRegistry, AvuEscape, AvuHeader |
| AVU DSL (new) | AVUCodec | Code registry extension |
| AVU DSL (new) | HandlerRegistry | Dispatcher delegates to handlers |

---

## Notes for Next Session

1. **Start with:** Read handover at `Docs/VoiceOSCore/Handover/VoiceOSCore-Handover-CompileFixes-AVU-DSL-260206.md`
2. **Branch:** `claude/060226-avu-dsl-evolution` (clean, from compile fixes)
3. **First task:** Create workdoc + Developer Manual Chapters 81-87
4. **Plan file:** `/Users/manoj_mbpm14/.claude/plans/mighty-plotting-rose.md` has full AVU DSL plan
5. **Key files to reference:**
   - `Docs/AVA/ideacode/guides/Developer-Manual-Chapter80-AVU-Codec-v2.2.md` (last chapter)
   - `Docs/AVA/ideacode/guides/Developer-Manual-Chapter67-Avanues-Plugin-Development.md` (to supersede)
   - `Modules/VoiceOSCore/src/androidMain/.../macros/MacroDSL.kt` (to supersede)
   - `Modules/VoiceOSCore/src/androidMain/.../plugins/PluginManager.kt` (to deprecate)
