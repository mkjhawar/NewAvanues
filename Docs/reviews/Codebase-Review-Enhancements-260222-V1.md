# NewAvanues — Enhancement Recommendations
**Date:** 260222 | **Branch:** VoiceOS-1M-SpeechEngine | **Author:** Manoj Jhawar
**Source:** 20-agent parallel SWARM codebase review

---

## Priority Matrix — Top 25 Fixes (Ship-Blocking)

| # | Module | Fix | Effort | Impact | Category |
|---|--------|-----|--------|--------|----------|
| 1 | AI/RAG | Replace SimpleTokenizer hashCode with BERT WordPiece vocab IDs | M | CRITICAL | Correctness |
| 2 | VoiceOSCore | Add root.recycle() in handleScreenChange + refreshScreen | S | CRITICAL | Memory leak |
| 3 | VoiceKeyboard | Fix currentInputConnection self-assignment | S | CRITICAL | Non-functional |
| 4 | apps/avanues | Set BootReceiver exported=true | S | CRITICAL | Boot broken |
| 5 | apps/avanues | Add HiltWorkerFactory initialization | S | CRITICAL | SFTP sync crash |
| 6 | apps/avanues | Add activity-alias entries for dual launcher | S | CRITICAL | Missing feature |
| 7 | AVU | Rename ACD collision (ACCEPT_DATA vs APP_CATEGORY_DB) | S | CRITICAL | Wire corruption |
| 8 | AVU | Fix AVUDecoder double-unescape | S | CRITICAL | Data corruption |
| 9 | HTTPAvanue | Strip PADDED flag in DATA/HEADERS frames | M | CRITICAL | Protocol violation |
| 10 | SpeechRecognition | Wire WhisperModelDownloadScreen onDownload to ViewModel | S | CRITICAL | Dead button |
| 11 | SpeechRecognition | Rebuild audioQueue on each streaming session | S | CRITICAL | Audio lost after 4:50m |
| 12 | WebAvanue | Fix selectDisambiguationOption indexing | S | CRITICAL | Wrong command |
| 13 | WebAvanue | Sanitize sendScrapeResult input | S | CRITICAL | Command injection |
| 14 | AI/NLU | Fix iOS BertTokenizer (real WordPiece, not zeros) | L | CRITICAL | iOS NLU broken |
| 15 | Voice/WakeWord | Remove 3 "author: Claude Code" Rule 7 violations | S | MANDATORY | Policy |
| 16 | VoiceOSCore | Remove 131 "VOS4 Development Team" attributions | S | MANDATORY | Policy |
| 17 | VoiceOSCore | Migrate CursorCommandHandler to IHandler/HandlerRegistry | M | HIGH | Architecture |
| 18 | VoiceOSCore | Fix handler phrase collisions (Cockpit scroll, Image rotate) | S | HIGH | Silent failures |
| 19 | Database | Enable deriveSchemaFromMigrations + verifyMigrations | M | HIGH | Data loss on upgrade |
| 20 | Cockpit | Extract BaseCockpitRepository (eliminate 360-line DRY violation) | M | HIGH | Maintainability |
| 21 | Cockpit | Populate workflowSteps in session load queries | S | HIGH | Data loss |
| 22 | apps/avanues | Fix accessibility settingsActivity wrong package | S | HIGH | Broken link |
| 23 | apps/avanues | Change SFTP hostKeyMode default to "strict" | S | HIGH | MITM vulnerability |
| 24 | PluginSystem | Replace DexClassLoader with script interpreter for .avp | L | HIGH | Architecture blocker |
| 25 | PhotoAvanue | Fix ModeChip onClick (wire to setCaptureMode) | S | HIGH | Functional regression |

---

## Module Enhancement Plans

### AI Module
CURRENT_STATE: Android NLU works with ONNX BERT; cloud LLM providers structured; Memory module clean
INTENDED_PURPOSE: Full cross-platform AI pipeline with semantic understanding
GAP: RAG embeddings garbage; iOS NLU broken; no rate limiting; no token limit enforcement
RECOMMENDATIONS:
1. Replace SimpleTokenizer with real BERT WordPiece vocabulary — EFFORT: M | IMPACT: CRITICAL
2. Fix ChunkEmbeddingHandler.updateChunkEmbeddingInternal (no-op) — EFFORT: S | IMPACT: HIGH
3. Add DocumentIngestionHandler routing for DOCX/TXT/HTML/MD — EFFORT: S | IMPACT: HIGH
4. Fix iOS CoreMLModelManager to produce real embeddings — EFFORT: L | IMPACT: CRITICAL
5. Add rate limiting + exponential backoff to cloud providers — EFFORT: M | IMPACT: HIGH
6. Replace TODO model checksums with real SHA-256 values — EFFORT: S | IMPACT: HIGH
7. Remove absolute paths from ModelSelector — EFFORT: S | IMPACT: HIGH
8. Guard null API keys before HTTP requests — EFFORT: S | IMPACT: MED

### VoiceOSCore
CURRENT_STATE: 11 handlers, accessibility service, scraping pipeline, overlay system — core is functional
INTENDED_PURPOSE: Robust voice OS engine with complete command coverage
GAP: Memory leaks, dual registry, Rule 7 epidemic, phrase collisions
RECOMMENDATIONS:
1. Add root.recycle() try/finally — EFFORT: S | IMPACT: CRITICAL
2. Merge CursorCommandHandler into HandlerRegistry — EFFORT: M | IMPACT: HIGH
3. Prefix module-specific commands to avoid priority collisions — EFFORT: S | IMPACT: HIGH
4. Fix ReadingHandler ttsReady data race (AtomicBoolean) — EFFORT: S | IMPACT: HIGH
5. Remove 131 Rule 7 violations — EFFORT: S | IMPACT: MANDATORY
6. Unify elementHash to SHA-256/AVID fingerprint — EFFORT: M | IMPACT: MED
7. Call isDynamicContentScreen in screen-change pipeline — EFFORT: S | IMPACT: MED
8. Add fallback to generateForAllClickable when list detection empty — EFFORT: S | IMPACT: MED

### AvanueUI
CURRENT_STATE: Theme v5.1 types defined; Android renderer partially working; voice handler registry exists
INTENDED_PURPOSE: Cross-platform design system with KMP rendering on all targets
GAP: 42 render() stubs; 60+ MaterialTheme violations; iOS/JVM disabled; zero AVID
RECOMMENDATIONS:
1. Implement render() for top 10 most-used component types — EFFORT: L | IMPACT: HIGH
2. Replace all MaterialTheme.colorScheme with AvanueTheme.colors — EFFORT: M | IMPACT: HIGH
3. Re-enable iOS/JVM targets in 8 sub-modules — EFFORT: L | IMPACT: HIGH
4. Implement CloudThemeRepository (currently 9 no-ops) — EFFORT: M | IMPACT: MED
5. Add AVID semantics to all Compose renderer output — EFFORT: M | IMPACT: HIGH
6. Fix banned com.avanueui.* package namespace in Data/ — EFFORT: S | IMPACT: MED

### Database
CURRENT_STATE: 67 tables, cross-platform schema, WAL+FK on Android
INTENDED_PURPOSE: Robust cross-platform persistence with safe schema evolution
GAP: No migration system; parallel stores; orphan accumulation
RECOMMENDATIONS:
1. Enable deriveSchemaFromMigrations + verifyMigrations — EFFORT: M | IMPACT: CRITICAL
2. Consolidate generated_web_commands + scraped_web_command — EFFORT: M | IMPACT: HIGH
3. Uncomment FK constraints on scraped_hierarchy + commands_generated — EFFORT: S | IMPACT: MED
4. Change Cockpit timestamp columns from TEXT to INTEGER — EFFORT: M | IMPACT: MED
5. Implement getSuccessFailureRatio (currently hardcoded 1.0) — EFFORT: S | IMPACT: LOW

### Build System
CURRENT_STATE: Working monorepo build; clean DAG; no convention plugins
INTENDED_PURPOSE: Fast, maintainable build with consistent configuration
GAP: No buildSrc; AGP stale; 85% modules untested; Jetifier dead weight
RECOMMENDATIONS:
1. Create buildSrc convention plugins — EFFORT: L | IMPACT: HIGH
2. Add commonTest to Foundation, Database, AVID, AVU, VoiceOSCore — EFFORT: L | IMPACT: HIGH
3. Remove android.enableJetifier=true — EFFORT: S | IMPACT: MED
4. Update AGP 8.2.0 → 8.9.x — EFFORT: M | IMPACT: MED
5. Update Ktor 2.3.7 → 3.x — EFFORT: L | IMPACT: MED
6. Align compileSdk/minSdk across all modules — EFFORT: S | IMPACT: MED

### IPC + Rpc
CURRENT_STATE: Strong parsing infrastructure; gRPC server partially functional
INTENDED_PURPOSE: Production IPC and RPC for cross-process voice OS communication
GAP: ConnectionManager is simulation; PlatformClient is 5-method stub; iOS IPC non-functional
RECOMMENDATIONS:
1. Implement ConnectionManager real IPC channel — EFFORT: L | IMPACT: CRITICAL
2. Implement PlatformClient.android gRPC methods — EFFORT: L | IMPACT: CRITICAL
3. Unify AVU escape to AvuEscape everywhere — EFFORT: S | IMPACT: HIGH
4. Fix AvaGrpcClient.close() to await disconnect — EFFORT: S | IMPACT: HIGH
5. Fix CockpitGrpcServer listener thread safety — EFFORT: S | IMPACT: MED

### PluginSystem
CURRENT_STATE: Ambitious architecture with lifecycle, dependency resolution, sandbox
INTENDED_PURPOSE: Runtime-extensible plugin ecosystem for .avp text files
GAP: DEX classloader incompatible with text files; permissions volatile
RECOMMENDATIONS:
1. Replace DexClassLoader with AVU interpreter for .avp loading — EFFORT: L | IMPACT: CRITICAL
2. Wire PermissionStorage to DefaultPluginSandbox — EFFORT: S | IMPACT: HIGH
3. Fix DependencyResolver to return Failure instead of throwing — EFFORT: S | IMPACT: HIGH
4. Fix register() valueOf crashes on bad manifests — EFFORT: S | IMPACT: HIGH

### WebAvanue
CURRENT_STATE: Browser with DOM scraping, 45 voice commands, JS bridge
INTENDED_PURPOSE: Full voice-controlled browser with secure DOM interaction
GAP: Disambiguation bug; injection risk; security theater; scroll stubs
RECOMMENDATIONS:
1. Fix selectDisambiguationOption to use filtered command list — EFFORT: S | IMPACT: CRITICAL
2. Validate sendScrapeResult input — EFFORT: S | IMPACT: CRITICAL
3. Replace Base64 with real AES in SecureScriptLoader — EFFORT: M | IMPACT: HIGH
4. Implement TabViewModel scroll methods — EFFORT: M | IMPACT: HIGH
5. Fix GestureMapper async/await context mismatch — EFFORT: S | IMPACT: HIGH

### SpeechRecognition
CURRENT_STATE: 5 Android engines, 2 iOS, 1 Desktop
INTENDED_PURPOSE: Reliable multi-engine speech pipeline
GAP: Dead download UI; audioQueue rebuild; scoped storage; Thread.sleep
RECOMMENDATIONS:
1. Wire WhisperModelDownloadScreen to ViewModel — EFFORT: S | IMPACT: CRITICAL
2. Rebuild audioQueue on each GoogleCloudStreamingClient session — EFFORT: S | IMPACT: CRITICAL
3. Replace getExternalStorageDirectory with getExternalFilesDir — EFFORT: S | IMPACT: HIGH
4. Replace Thread.sleep with suspendable delay in writeTo — EFFORT: S | IMPACT: HIGH
5. Synchronize LearningSystem collections — EFFORT: S | IMPACT: MED
6. Add commonTest for WhisperVAD, ConfidenceScorer, CommandCache — EFFORT: M | IMPACT: MED

### Content Modules (Note, Photo, Image, PDF, Video)
CURRENT_STATE: NoteAvanue and ImageAvanue most complete; PhotoAvanue feature-rich but buggy
INTENDED_PURPOSE: Full voice-first content creation and consumption
GAP: AVID coverage; voice wiring; desktop stubs; functional regressions
RECOMMENDATIONS:
1. Fix PhotoAvanue ModeChip onClick — EFFORT: S | IMPACT: HIGH
2. Fix PDFAvanue file descriptor leak in onDispose — EFFORT: S | IMPACT: HIGH
3. Wire ModuleCommandCallbacks.photoExecutor — EFFORT: S | IMPACT: HIGH
4. Implement PDFAvanue search() and extractText() — EFFORT: M | IMPACT: MED
5. Add AVID to all content module interactive elements — EFFORT: M | IMPACT: HIGH
6. Deprecate and remove CameraAvanue — EFFORT: S | IMPACT: MED
7. Remove ImageAvanue `|| true` debug artifact — EFFORT: S | IMPACT: LOW

### Consolidated App (apps/avanues)
CURRENT_STATE: Hilt DI, Compose, WorkManager, theme v5.1 — fundamentals correct
INTENDED_PURPOSE: Single production app combining all avenues
GAP: Boot broken, no dual icons, SFTP crash, dead RPC service
RECOMMENDATIONS:
1. BootReceiver exported=true — EFFORT: S | IMPACT: CRITICAL
2. Fix accessibility settingsActivity package — EFFORT: S | IMPACT: CRITICAL
3. Add activity-alias for VoiceAvanue and WebAvanue icons — EFFORT: S | IMPACT: CRITICAL
4. Add HiltWorkerFactory initialization — EFFORT: S | IMPACT: CRITICAL
5. Default SFTP hostKeyMode to "strict" — EFFORT: S | IMPACT: HIGH
6. Make DeveloperPreferencesRepository Hilt-managed — EFFORT: S | IMPACT: MED
7. Use EntryPointAccessors in AccessibilityService — EFFORT: S | IMPACT: MED

---

## Effort/Impact Summary

| Effort | CRITICAL Impact | HIGH Impact | MED Impact | LOW Impact |
|--------|----------------|-------------|------------|------------|
| S (< 1 hour) | 12 items | 14 items | 8 items | 3 items |
| M (1-4 hours) | 4 items | 8 items | 6 items | 1 item |
| L (4+ hours) | 4 items | 5 items | 2 items | 0 items |

**Quick wins (S + CRITICAL/HIGH):** 26 fixes that can be done in under 26 hours total
**Heavy lifts (L + CRITICAL):** RAG tokenizer, iOS NLU, PluginSystem classloader, IPC implementation

---

## Codebase-Wide Sweeps Needed

### Sweep 1: Rule 7 Cleanup (~160 files)
- VoiceOSCore: 131 files with "VOS4 Development Team"
- Voice/WakeWord: 3 files with "Claude Code"
- VoiceDataManager: 1 file with "AI Assistant"
- VoiceRecognition: 10 files with "VOS4 Development Team"
- AvanueUI: 1 file with "VOS4 Development Team"
- SpeechRecognition: 1 file with "VOS4 Development Team"
**Fix:** `grep -r "VOS4 Development Team\|Author: Claude\|Author: AI" --include="*.kt" -l | xargs sed -i '' 's/Author: .*/Author: Manoj Jhawar/'`

### Sweep 2: System.currentTimeMillis() in commonMain (~15 files)
- AVACode: WorkflowInstance, WorkflowPersistence
- WebAvanue: BrowserVoiceOSCallback, CommandBarAutoHide + 7 others
**Fix:** Replace with `kotlinx.datetime.Clock.System.now().toEpochMilliseconds()` or expect/actual

### Sweep 3: AVID Coverage (~80% of screens)
- All content modules: NoteAvanue, PhotoAvanue, PDFAvanue, VideoAvanue
- All management screens: DeviceManager, VoiceDataManager, VoiceRecognition
- All app entry points: apps/avanues
**Fix:** Add `Modifier.semantics { contentDescription = "Voice: click {label}" }` to all interactive elements

### Sweep 4: println() in Production (~50+ files)
- VoiceOSCore, WebAvanue, SpeechRecognition, VoiceIsolation, Rpc, DeviceManager
**Fix:** Replace with platform Logger or Napier

### Sweep 5: MaterialTheme.colorScheme Usage (BANNED)
- AvanueUI/ThemeBuilder: 13+ violations
- AvanueUI/LayoutDisplayExtensions: 22+ violations
- VoiceDataManager: full MaterialTheme wrapping
- VoiceRecognition: full activity hardcoded colors
**Fix:** Replace with AvanueTheme.colors.*

---

## Late-Arriving Findings (Agents C5 + D2)

### LicenseManager (34 findings from agent C5)
CURRENT_STATE: Validator is a hardcoded pattern-match stub; key stored in plaintext SharedPreferences; security tests pass vacuously due to wrong prefs name
INTENDED_PURPOSE: Real license enforcement with server validation and encrypted storage
GAP: Entire licensing pipeline is non-functional; provides false sense of protection
RECOMMENDATIONS:
1. Replace stub validator with real LicenseClient call — EFFORT: M | IMPACT: CRITICAL
2. Move key from plaintext SharedPreferences to EncryptedSharedPreferences/ICredentialStore — EFFORT: S | IMPACT: CRITICAL
3. Fix SecurityTest prefs name mismatch ("voiceos_license" → "voiceos_licensing") — EFFORT: S | IMPACT: HIGH
4. Fix GracePeriodInfo.parseIsoDate() (ignores input, returns Clock.System.now()) — EFFORT: S | IMPACT: HIGH
5. Fix iOS QrScannerService (returns Obj-C description, not QR content) — EFFORT: S | IMPACT: HIGH
6. Replace Android QR busy-poll with Tasks.await() — EFFORT: S | IMPACT: MED

### Legacy Apps (51 findings from agent D2)
CURRENT_STATE: 5 legacy/secondary apps with massive code duplication, broken themes, fake encryption
INTENDED_PURPOSE: Should be consolidated into apps/avanues or deleted
GAP: Two apps are byte-for-byte copies; encryption claim is false; zero AVID across all
RECOMMENDATIONS:
1. Delete Apps/voiceavanue-legacy/ (exact duplicate) — EFFORT: S | IMPACT: MED
2. Delete android/apps/webavanue-legacy/ (exact duplicate) — EFFORT: S | IMPACT: MED
3. Fix Apps/voiceavanue VoiceAvanueTheme to use AvanueThemeProvider — EFFORT: S | IMPACT: CRITICAL
4. Fix or remove WebAvanue SQLCipher encryption no-op — EFFORT: M | IMPACT: CRITICAL
5. Rewrite or delete 8 no-op E2E tests in WebAvanue — EFFORT: M | IMPACT: HIGH
6. Fix VoiceOS app dispose-on-cancelled-scope resource leak — EFFORT: S | IMPACT: HIGH

---

## Recommended Fix Order (Sprint Plan)

### Sprint 1: Ship-Blockers (1-2 days)
Items 1-14 from Priority Matrix + Rule 7 sweep + LicenseManager plaintext key fix + WebAvanue encryption no-op

### Sprint 2: Architecture Fixes (3-5 days)
Items 15-25 from Priority Matrix + AVU escape unification + handler migration + LicenseManager real validator

### Sprint 3: Cleanup & Consolidation (2-3 days)
Delete 2 legacy app duplicates + fix AvanueThemeProvider in all apps + fix all broken security tests

### Sprint 4: Platform Parity (1-2 weeks)
iOS NLU fix, AvanueUI KMP re-enablement, Desktop stubs → real implementations, iOS QR scanner fix

### Sprint 5: Quality Infrastructure (1-2 weeks)
buildSrc convention plugins, commonTest for 5 critical modules, AVID sweep, println cleanup, rewrite E2E tests

---

Author: Manoj Jhawar | 260222 | Enhancement Recommendations V1 (updated with C5+D2 findings)
