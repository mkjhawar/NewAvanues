# AVA AI - Architecture Decision Records (ADR)

**Last Updated**: 2025-01-28
**Framework**: IDEACODE v1.0

---

## Purpose

This document tracks all significant architectural and technical decisions made during AVA AI development. Each decision follows the ADR format: Context → Decision → Consequences.

---

## ADR-001: Hybrid NLU Architecture (ONNX + MLC + Rules)

**Date**: 2025-10-27
**Status**: Accepted
**Impact**: High (core AI strategy)

### Context

AVA needs intent classification and generation capabilities while maintaining privacy-first, on-device processing. Multiple approaches considered:
1. Cloud-only (OpenAI, Claude) - violates privacy-first principle
2. On-device LLM only (Gemma 2B) - slow for simple intent classification
3. ONNX NLU only - cannot handle complex queries requiring generation
4. Hybrid approach - ONNX for classification, LLM for generation

### Decision

Adopt **hybrid architecture**:
- **ONNX Runtime Mobile + MobileBERT** for intent classification (<50ms, 12MB)
- **Teach-Ava KeywordFallbackClassifier** for user-trainable rules (no retraining)
- **MLC LLM + Gemma** for complex queries requiring generation (2GB, GPU/NPU)
- **llama.cpp fallback** for CPU inference (portable)
- **Ollama fallback** for development/desktop (easy model management)

### Consequences

**Positive:**
- ✅ Fast intent classification (ONNX <50ms vs LLM 500ms+)
- ✅ User-trainable without cloud (Teach-Ava)
- ✅ Complex query support (MLC LLM)
- ✅ Portable (llama.cpp CPU fallback)
- ✅ Privacy-first (95%+ local processing)

**Negative:**
- ⚠️ Two model systems to maintain (ONNX + LLM)
- ⚠️ Larger app size (~30 MB ONNX + NLU, 2GB LLM optional download)

**Mitigation:**
- LLM models are optional downloads (not bundled in APK)
- ONNX models are INT8 quantized (25.5 MB vs 99 MB FP32)

---

## ADR-002: IDEACODE Framework Adoption

**Date**: 2025-01-28
**Status**: Accepted
**Impact**: Critical (all development processes)

### Context

AVA AI started with custom development structure. As project grows (Week 5 → Week 16), need formal methodology for:
- Quality assurance (testing, coverage, standards)
- Multi-agent coordination (parallel development)
- Documentation continuity (agent handoff)
- Spec-driven development (WHAT/WHY before HOW)

IDEACODE v1.0 provides proven framework (battle-tested on VOS4, 50,000+ LOC).

### Decision

Adopt **IDEACODE v1.0** as official development framework:
- Mandatory spec-first development (`/idea.specify`)
- IDE Loop enforcement (Implement → Defend → Evaluate → Commit)
- 80%+ test coverage requirement (non-negotiable)
- Living documentation (`docs/ProjectInstructions/`)
- Multi-agent architecture (8 specialist agents)
- Slash command workflow automation

### Consequences

**Positive:**
- ✅ Quality enforcement (IDE Loop prevents untested code)
- ✅ Time savings (90%+ on complex tasks with multi-agent)
- ✅ VOS4 alignment (shared practices for Phase 4 integration)
- ✅ Spec-driven clarity (reduces rework)
- ✅ Knowledge continuity (living docs for agent handoff)

**Negative:**
- ⚠️ More upfront planning (spec creation before code)
- ⚠️ Learning curve for new workflow

**Mitigation:**
- 3-tier approach (Tier 1 can skip full workflow for <30min changes)
- Week 5 already at 92% coverage (exceeds 80% target)
- Existing code unchanged (backward compatible)

---

## ADR-003: SQLDelight + Room Façade

**Date**: 2025-10-26
**Status**: Accepted
**Impact**: Medium (database layer)

### Context

Need cross-platform database for Android + iOS + Desktop. Options:
1. Room only - Android-only, no KMP support
2. SQLDelight only - cross-platform, less familiar API
3. Core Data (iOS) + Room (Android) - platform-specific, duplication
4. SQLDelight + Room façade - cross-platform foundation, familiar API

### Decision

Use **SQLDelight 2.1** as primary database with **Room DAO pattern façade** for Android:
- SQLDelight handles schema, migrations, cross-platform queries
- Room DAO pattern provides familiar API for Android developers
- Phase 1 uses Room façade on Android
- Future phases expose SQLDelight directly for iOS/Desktop

### Consequences

**Positive:**
- ✅ Cross-platform ready (iOS, macOS, Windows support later)
- ✅ Familiar API for Android developers
- ✅ Type-safe queries (SQLDelight generates Kotlin code)
- ✅ Proven in AvaAssistant project

**Negative:**
- ⚠️ Slight abstraction overhead (Room façade → SQLDelight)
- ⚠️ Two database systems to understand

**Mitigation:**
- Room façade is thin layer (minimal overhead)
- Documentation clarifies when to use Room vs SQLDelight

---

## ADR-004: MobileBERT INT8 Quantization

**Date**: 2025-01-28
**Status**: Accepted
**Impact**: Medium (model size, performance)

### Context

Need BERT model for intent classification. Multiple quantization levels available:
- FP32 (99 MB, 100% accuracy, slow)
- FP16 (49.9 MB, ~99.5% accuracy, medium)
- INT8 (25.5 MB, ~97% accuracy, fast) ← SELECTED
- INT4 (30.7 MB, ~95% accuracy, faster)

### Decision

Use **MobileBERT INT8** quantized model (25.5 MB):
- Source: `onnx-community/mobilebert-uncased-ONNX/onnx/model_int8.onnx`
- 30,522 token vocabulary (WordPiece)
- NNAPI hardware acceleration enabled

### Consequences

**Positive:**
- ✅ 74% size reduction (99 MB → 25.5 MB)
- ✅ 2x faster inference vs FP32
- ✅ Minimal accuracy loss (3% acceptable for intent classification)
- ✅ Fits in APK (total app size <30 MB)

**Negative:**
- ⚠️ 3% accuracy reduction vs FP32
- ⚠️ Cannot switch to higher precision without model swap

**Mitigation:**
- 97% accuracy sufficient for intent classification use case
- Can add FP16/FP32 as optional download for "High Accuracy Mode"

---

## ADR-005: VoiceAvenue Plugin Theme System

**Date**: 2025-10-27
**Status**: Accepted
**Impact**: High (all UI components, Phase 4 integration)

### Context

AVA needs UI/UX design system. Options:
1. Copy MagicDreamTheme from VOS4 (simple, 1-2 days)
2. Use VoiceAvenue Plugin Theme System (YAML, hot-reload, plugin-compatible from day 1)
3. Material 3 only (generic, no glassmorphism)
4. Custom AVA design system (8-10 weeks)

### Decision

Adopt **VoiceAvenue Plugin Theme System** with YAML inheritance:
- Extend `MagicDreamTheme` (glassmorphism base)
- Add 2 AVA-specific colors (`contextHighlight`, `ragDatasetAccent`)
- Hot-reload support (edit YAML → reload without recompile)
- Plugin-compatible from Phase 1 (Phase 4 integration = copy YAML file)

### Consequences

**Positive:**
- ✅ Plugin-compatible from day 1 (Phase 4 integration ~1 hour)
- ✅ Zero design work (MagicDreamTheme production-ready)
- ✅ Hot-reload (rapid iteration)
- ✅ Instant VOS4 consistency (same theme system)
- ✅ Theme inheritance (extend without duplication)

**Negative:**
- ⚠️ Medium complexity (plugin architecture required)
- ⚠️ 3-5 days setup vs 1-2 days for simple copy

**Mitigation:**
- Initial setup cost (3-5 days) saves 3-5 days in Phase 4
- No breaking visual changes in Phase 4 (users don't notice transition)

---

## ADR-006: Conversation + Message Dual-Table Design

**Date**: 2025-10-26
**Status**: Accepted
**Impact**: Medium (database schema)

### Context

Need to store chat history. Options:
1. Single `messages` table with `conversation_id`
2. Dual tables: `conversations` + `messages` (1-to-many)

### Decision

Use **dual-table design**:
```sql
conversations (id, title, created_at, updated_at)
messages (id, conversation_id, role, content, created_at)
```

### Consequences

**Positive:**
- ✅ Efficient conversation list queries (no JOIN needed)
- ✅ Clean separation (conversation metadata vs messages)
- ✅ Cascade deletes (delete conversation → delete all messages)
- ✅ Easy to add conversation-level features (title, summary, tags)

**Negative:**
- ⚠️ Two tables to manage vs one
- ⚠️ JOIN required for conversation + messages view

**Mitigation:**
- Repository pattern abstracts complexity
- Flow-based queries handle JOINs reactively

---

## ADR-007: Teach-Ava Hash Deduplication (MD5)

**Date**: 2025-10-26
**Status**: Accepted
**Impact**: Low (training example storage)

### Context

Need to prevent duplicate training examples (same utterance + intent + locale). Options:
1. Composite unique constraint (`utterance`, `intent`, `locale`)
2. MD5 hash deduplication (hash stored as `TEXT`, unique constraint)

### Decision

Use **MD5 hash deduplication**:
```kotlin
val hash = MessageDigest.getInstance("MD5")
    .digest("$utterance|$intent|$locale".toByteArray())
    .joinToString("") { "%02x".format(it) }
```

### Consequences

**Positive:**
- ✅ Fast duplicate detection (hash index lookup)
- ✅ Single-column unique constraint
- ✅ Works across all databases (SQLite, Postgres, etc.)

**Negative:**
- ⚠️ MD5 collision risk (negligible for this use case)
- ⚠️ Requires hash computation on insert

**Mitigation:**
- MD5 collision probability negligible for <1M training examples
- Hash computation is fast (<1ms)

---

## ADR-008: Faiss + Supabase Hybrid RAG (Phase 2+6)

**Date**: 2025-10-27
**Status**: Accepted
**Impact**: High (RAG architecture)

### Context

Need vector search for RAG. Options:
1. Faiss local-only (no cloud sync)
2. Supabase pgvector only (cloud-only, violates privacy-first)
3. Hybrid: Faiss local + Supabase backup (best of both)

### Decision

Use **hybrid architecture**:
- **Faiss**: Local vector index (primary, <100ms search)
- **SQLDelight**: Metadata storage (text, IDs, `cloudRef` URLs)
- **Supabase**: Cloud backup (optional, Phase 6 only)

**Data Flow:**
```
Document → Embeddings → Faiss (index)
                     → SQLDelight (metadata)
                     → Supabase (backup, optional)
```

### Consequences

**Positive:**
- ✅ Local-first (Faiss always available, <100ms)
- ✅ Privacy-first (Supabase opt-in only)
- ✅ Cross-device sync (Supabase, when enabled)
- ✅ Lazy loading (fetch `cloudRef` only when needed)

**Negative:**
- ⚠️ Two storage systems (Faiss + Supabase)
- ⚠️ Sync complexity (Phase 6)

**Mitigation:**
- Faiss is primary (Supabase optional)
- Phase 2 implements local-only (Supabase deferred to Phase 6)

---

## Pending Decisions

### PD-001: LLM Model Selection (Week 9-10)

**Context**: Need to choose primary LLM for on-device generation.

**Options:**
1. Gemma 2B INT4 (~1.5 GB, fast)
2. Llama 3.2 3B INT4 (~2.2 GB, better quality)
3. Phi-3 Mini (~2 GB, Microsoft-optimized)

**Target Decision Date**: Week 9 (ALC implementation phase)

### PD-002: Cloud LLM Provider (Week 11-12)

**Context**: Enterprise tier needs cloud LLM fallback.

**Options:**
1. OpenRouter (aggregator, 100+ models, $0.02/1K tokens)
2. Direct OpenAI API (GPT-4, $0.03/1K tokens)
3. Direct Anthropic API (Claude 3.5, $0.015/1K tokens)

**Target Decision Date**: Week 11 (Cloud LLM integration phase)

---

## Decision Review Schedule

**Quarterly Review**: Every 3 months
**Next Review**: 2025-04-28

---

## References

- **Constitution**: `.ideacode/memory/principles.md`
- **Architecture**: `ARCHITECTURE.md`
- **VOS4 Integration**: `VOS4_INTEGRATION_REQUIREMENTS.md`
- **IDEACODE Protocol**: `/Volumes/M Drive/Coding/ideacode/`

---

**Note**: This is a living document. Add new ADRs when making significant architectural decisions. Update status when decisions change.
