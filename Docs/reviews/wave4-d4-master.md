# Wave 4 — Day 4 Master Analysis

## Session: 260222

---

## Module: BuildSystem (virtual — cross-cutting)

**Full report:** `docs/reviews/BuildSystem-Review-QualityAnalysis-260222-V1.md`

**Score:** 62/100 | **Health:** YELLOW

### Build Versions

| Stack | Version | Currency |
|-------|---------|----------|
| Kotlin | 2.1.0 | GOOD (2.1.20 available) |
| AGP | 8.2.0 | STALE (8.9.0 current) |
| Compose Multiplatform | 1.7.3 | CURRENT |
| Compose BOM | 2024.12.01 | SLIGHTLY BEHIND |
| KSP | 2.1.0-1.0.29 | CURRENT |
| Gradle | ~8.6 (inferred) | BEHIND (8.14 current) |
| kotlinx-coroutines | 1.8.1 | BEHIND (1.10.1 current) |
| Ktor | 2.3.7 | MAJOR VERSION BEHIND (3.1.2 current) |
| Coil | 2.6.0 | MAJOR VERSION BEHIND (3.1.0 current) |
| gRPC | 1.62.2 | STALE (1.71.0 current) |
| Media3 | 1.2.1 | STALE (1.5.1 current) |
| androidx-lifecycle | 2.6.2 | STALE (2.9.0 current) |

### Dependency Graph Summary

- 50+ active modules in a clean DAG (no circular dependencies confirmed)
- Dependency flow: Foundation/Logging → Database/AVID/AVU → AI/* → VoiceOSCore → Content/* → Cockpit/Apps
- Potential near-cycle: AvanueUI root ← VoiceOSCore → AvanueUI:AvanueUIVoiceHandlers (semantic, not Gradle)
- Architectural smell: CameraAvanue → AI:RAG (camera module pulling entire AI stack)

### Test Coverage Summary

- ~15% of active modules have any tests (35 of ~50 modules: zero tests)
- Critical modules with zero tests: Foundation, Database, AVID, AVU, Logging, VoiceOSCore,
  SpeechRecognition, Cockpit, AvanueUI (all sub-modules), HTTPAvanue, RemoteCast, Actions
- Well-tested: AI:NLU, AI:RAG, AI:LLM, WebAvanue, DeviceManager

### Issues Summary

| Priority | Count | Key Issues |
|----------|-------|------------|
| P0 | 2 | ksp.useKSP2=false has no migration plan; VoiceAvanue module + app duplicate dep surface |
| P1 | 6 | No buildSrc convention plugins; fragment-ktx hardcoded; compileSdk 34/35 mix; Jetifier dead; 35+ modules missing tests; zombie catalog entries (neo4j/okhttp) |
| P2 | 8 | No commonTest in most KMP modules; redundant buildscript block; material3-adaptive beta in prod; security-crypto alpha; semantic AvanueUI/VoiceOSCore cycle; CameraAvanue→AI:RAG; default hierarchy template disabled; iOS not validated in standard build |
| P3 | 4 | No build scans; dokka declared but never applied; TYPESAFE_PROJECT_ACCESSORS preview flag stale; legacy app compiled alongside active app |

### Top 3 Recommendations

1. **Create buildSrc convention plugins** — single highest-leverage action, eliminates 1000+
   lines of boilerplate across 50 modules and centralizes compileSdk/minSdk/jvmTarget.

2. **Add commonTest to Foundation, Database, AVID, AVU, VoiceOSCore** — these are the
   dependency roots; a defect here has monorepo-wide blast radius.

3. **Remove `android.enableJetifier=true`** — the project is fully AndroidX, Jetifier
   adds build time and carries accidental-rewrite risk.

---

*Reviewed by code-reviewer agent | 260222*
