# AVA AI Constitution

**Version**: 1.3.1
**Ratified**: 2025-10-27
**Status**: Active - IDEACODE Framework + JSON Standards

---

## Core Principles

### I. Privacy-First Architecture

All AI processing MUST prioritize user privacy and on-device execution:

- Default to **95%+ local processing** (ONNX NLU + MLC LLM fallback)
- **ONNX Runtime Mobile** for lightweight intent classification (~12MB models)
- **MLC LLM + Gemma** for complex queries requiring generation (~2GB models)
- Cloud sync is **OPTIONAL** and **encrypted** (Supabase with E2E encryption)
- User **MUST** explicitly opt-in for cloud features
- **Cloud storage separation** (Phase 6):
  - **Local storage** (SQLDelight): Metadata, text, IDs, references
  - **Cloud storage** (Supabase): Large data only (images, embeddings >1MB)
  - **Compression**: JPEG 80% for images, INT8 for embeddings
  - **Data limit**: 1 GB per tenant with quota enforcement
- RAG knowledge base stored locally (Faiss index + SQLDelight metadata)
- No telemetry without explicit consent
- AOSP compatibility (NO Google Play Services dependency)
- Multi-tenant with Row-Level Security (RLS) for enterprise data isolation

**Rationale**: Privacy violations are unacceptable for AI assistants handling personal data, documents, and conversations. Users must maintain control over where their data is processed and stored. The hybrid NLU approach (ONNX for speed, MLC for capability) balances performance with features.

---

### II. Integration-First Design (VOS4/VoiceAvenue Ecosystem)

AVA MUST integrate seamlessly with existing Augmentalis projects:

- Built as **VoiceAvenue Plugin** for VOS4 integration
- **Reuse VOS4's** speech recognition (Vosk, Vivoka, AndroidSTT)
- **Use VoiceAvenue's Plugin Theme System** (YAML-based, hot-reload, theme inheritance)
- **Extend MagicDreamTheme** with AVA-specific colors (contextHighlight, ragDatasetAccent)
- **Reuse VOS4's** accessibility service integration
- Shared modules via Kotlin Multiplatform (KMP)
- Support standalone deployment AND plugin mode
- Zero duplication of VOS4 capabilities

**Phased Integration Exception (Phase 1.0 MVP)**:
- Phase 1.0 (MVP) MAY operate in standalone mode with text-only input
- Voice input integration with VOS4 is REQUIRED by Phase 1.1 or Phase 4 (whichever comes first)
- Standalone mode must not violate other principles (privacy-first, accessibility)
- All VOS4 integration points must be architecturally ready (interfaces defined, plugin hooks in place)

**Rationale**: Augmentalis has 50,000+ LOC in VOS4 with proven speech, UI, and accessibility. Reusing these components accelerates development by ~67% and ensures consistency across projects. VoiceAvenue's plugin theme system provides plugin compatibility from Phase 1, enabling trivial Phase 4 integration (copy YAML file). Phased integration allows MVP delivery without blocking on VOS4 readiness while maintaining architectural compatibility.

---

### III. Smart Glasses First

UI and interaction MUST be optimized for hands-free smart glasses usage:

- Support **8+ smart glasses devices** (Meta Ray-Ban, Vuzix, RealWear, XReal, Rokid, Even Realities)
- **VisionOS-inspired UI** with translucent panels and glass morphism
- **Adaptive display optimization** per device capability
- **Voice-first interaction** (minimal touch required)
- **Battery optimization** (<10% per hour active use)
- **Accessibility compliance** (WCAG 2.1 AAA)
- Real-time captioning and audio descriptions

**Rationale**: The future of AI assistants is wearable computing. Smart glasses enable hands-free assistance for technicians, accessibility users, and mobile professionals.

---

### IV. Constitutional AI & Ethics

All AI responses MUST be evaluated for ethical compliance:

- **Constitutional AI** with self-critique system
- **7 core principles**: Helpful, Harmless, Honest, Privacy-Respecting, Inclusive, Autonomy-Promoting, Transparent
- **>90% principle adherence** target
- Reject/revise responses that violate principles
- Cultural adaptation for global users
- Explainable AI (users can ask "why did you say that?")

**Rationale**: AI assistants must be trustworthy, safe, and beneficial. Constitutional AI ensures responses align with human values and prevents harmful outputs.

---

### V. User-Trainable Intelligence & Advanced Memory

AVA MUST learn from user feedback and remember context while respecting privacy:

- **Teach-Ava Training Loop** (unique differentiator):
  - User-correctable intent recognition with immediate feedback
  - Manual promotion of utterances to training examples
  - Auto-learning mode for successful interactions
  - Rules-based fallback with dynamic keyword matching
  - Training data stored in `train_example` table
- **Dual memory architecture**:
  - **Faiss RAG** for user's knowledge base (documents, PDFs, manuals)
  - **Cognitive memory** for conversations (Working/Episodic/Semantic/Procedural)
- **Working memory**: 7±2 items with attention-based management
- **Episodic memory**: Past conversations with emotional context
- **Semantic memory**: Learned patterns and user preferences (from Teach-Ava)
- **Memory consolidation**: Background processing during idle
- **Forget capability**: Users can delete memories on demand

**Rationale**: Effective AI assistants must maintain context and learn from interactions. The Teach-Ava system enables personalization without cloud training. The dual memory system separates factual knowledge (RAG) from experiential learning (cognitive + training).

---

## Technical Constraints

### Platform & Technology Stack

**Mandatory Technologies (Hybrid Stack):**

- **Language**: Kotlin Multiplatform (Android, iOS, macOS, Windows)
- **Android**: API 24-34 (Android 7.0-14)
- **NLU Engine**: ONNX Runtime Mobile + DistilBERT/MobileBERT (12MB, intent classification)
- **LLM Engine** (cascading fallback):
  - **Primary**: MLC LLM + Gemma (2B/7B models, on-device GPU/NPU)
  - **Fallback 1**: llama.cpp (CPU inference, portable)
  - **Fallback 2**: Ollama (local server, developer-friendly)
- **Rules Engine**: KeywordFallbackClassifier with dynamic rules.json
- **Vector DB**: Faiss (local vector search for RAG)
- **Local DB**: SQLDelight 2.1 (cross-platform) with Room façade (Android)
- **Cloud Sync**: Supabase with Row Level Security (RLS) - OPTIONAL
- **UI Framework**: Jetpack Compose Multiplatform (Android/Desktop), SwiftUI (iOS/macOS)
- **Document Parsing**: Apache PDFBox, skrape{it}
- **OCR**: Tesseract (on-device)
- **Casting**: WebRTC + Ktor signaling server
- **Namespace**: `com.augmentalis.ava.*`

**Integration Stack (from VOS4):**

- **Speech Recognition**: VOS4 SpeechRecognitionManager (Vosk, Vivoka, AndroidSTT)
- **UI Theme**: VOS4 GlassmorphismTheme
- **Accessibility**: VOS4 AccessibilityService
- **Plugin System**: MagicCode PluginSystem

**Hybrid Architecture Rationale:**
- ONNX NLU provides fast (<50ms), lightweight (12MB) intent classification
- MLC LLM handles complex queries requiring generation/reasoning (GPU/NPU optimized)
- llama.cpp fallback for devices without GPU/NPU (CPU inference)
- Ollama fallback for development/desktop (easy model management, local server)
- KeywordFallbackClassifier enables user training without retraining models
- SQLDelight ensures cross-platform database compatibility
- VOS4 integration eliminates 50,000+ LOC of duplicate speech/UI code

### Code Standards

**JSON Formatting Rules:**

All JSON files MUST follow compact formatting to minimize file size and improve readability:

1. **Use arrays instead of nested objects** when keys are redundant
2. **Omit wrapper objects** that add no semantic value
3. **Prefer flat structures** over deeply nested hierarchies
4. **Single-line formatting** for simple values
5. **Multi-line only** for complex nested structures

**Example - Non-Compliant (Verbose)**:
```json
{
  "control_lights": {
    "examples": [
      "Turn on the lights",
      "Switch off the lights"
    ]
  },
  "check_weather": {
    "examples": [
      "What's the weather",
      "Will it rain today"
    ]
  }
}
```

**Example - Compliant (Compact)**:
```json
{
  "control_lights": [
    "Turn on the lights",
    "Switch off the lights"
  ],
  "check_weather": [
    "What's the weather",
    "Will it rain today"
  ]
}
```

**Rationale**: Compact JSON reduces file size, improves parsing performance, and enhances readability. The redundant "examples" wrapper adds no semantic information and increases file size by ~30%. Arrays are more idiomatic for lists of homogeneous items.

### Quality Gates

All features MUST pass these gates before merging:

1. **Privacy Gate**: Verify local-first processing, encrypted cloud sync
2. **Integration Gate**: Verify VOS4/MagicCode compatibility (Phase 4+)
3. **Constitutional Gate**: >90% AI principle adherence (Phase 3+)
4. **Performance Gate**: NLU <50ms, end-to-end <500ms (95th percentile)
5. **Memory Gate**: <512MB peak on low-end devices
6. **Battery Gate**: <10% per hour active use
7. **Accessibility Gate**: WCAG 2.1 AAA compliance (Phase 5+)
8. **Testing Gate**: 80%+ coverage, all tests passing
9. **Teach-Ava Gate**: User training flow validated with test utterances

### Performance Budgets

**Hard Limits:**

- **NLU Intent Classification**: <50ms (ONNX inference)
- **LLM Response Latency**: <500ms first token (95th percentile)
- **Memory Footprint**: <512MB (low-end), <1GB (mid-range), <2GB (high-end)
- **Battery Usage**: <10% per hour active use
- **Model Sizes**:
  - ONNX NLU: ~12MB (MobileBERT INT8)
  - Gemma LLM: ~2GB (INT4/INT8 quantized)
  - Embeddings: ~20MB (MiniLM-e5-small INT8)
- **RAG Search**: <100ms per query (Faiss)
- **Speech-to-Response**: <800ms total (including VOS4 recognition + ONNX NLU)
- **Teach-Ava Rule Update**: <10ms (dynamic rules.json reload)

**Cloud Storage Limits (Phase 6)**:
- **Per-Tenant Limit**: 1 GB (images + embeddings + documents)
- **Image Compression**: JPEG 80% quality before upload
- **Embedding Storage**: INT8 quantized (4x smaller than FP32)
- **Enforcement**: Supabase storage policies + client-side quota checks
- **Sync Performance**: 1 GB data load <5 minutes (target)

**Measurement:**

- Automated performance benchmarks in test suite
- Real device testing on low-end (2GB RAM), mid-range (4GB), high-end (8GB+)
- Battery profiling during manual testing
- Smart glasses device testing (all 8+ types)
- Supabase sync performance testing (Phase 6+)

---

## Development Workflow

### IDEACODE Framework Compliance

AVA AI development follows **IDEACODE v1.0** methodology (merges IDEADEV + Spec-Kit):

**Core Commitments:**
1. ✅ **Spec-First Development** - Define WHAT/WHY before HOW (use `/idea.specify`)
2. ✅ **Mandatory IDE Loop** - Implement → Defend → Evaluate → Commit (cannot skip)
3. ✅ **80%+ Test Coverage** - Defend phase is NON-NEGOTIABLE
4. ✅ **Quality Gates** - Multiple checkpoints ensure standards compliance
5. ✅ **Living Documentation** - Continuous tracking (notes.md, decisions.md, bugs.md, progress.md)
6. ✅ **Multi-Agent Support** - Parallel specialist agent deployment (90%+ time savings)

### 3-Tier Complexity Approach (Inherited from VOS4)

#### Tier 1: Direct Implementation (<30 minutes)
- Bug fixes, single file changes
- Documentation updates
- Simple refactoring
- **Skip IDEACODE workflow BUT maintain quality**
- Still write tests, just no formal spec needed

#### Tier 2: Subagent-Assisted (1-3 hours) ⭐ RECOMMENDED
- 2-3 module changes
- Medium scope features
- Call specialist agents for implementation
- **Use lightweight spec** (quick `/idea.specify` + `/idea.plan`)
- Follow IDE Loop for each phase
- Mandatory testing (80%+ coverage)

#### Tier 3: Full IDEACODE Workflow (>4 hours) **MANDATORY**
- Complex features, multiple modules
- **Full spec-driven process** (non-negotiable):
  1. `/idea.principles` - Verify alignment with AVA Constitution
  2. `/idea.specify` - Define requirements (WHAT/WHY, acceptance criteria)
  3. `/idea.clarify` - Resolve ambiguities (optional)
  4. `/idea.plan` - Create phased implementation plan (HOW)
  5. `/idea.tasks` - Generate atomic task breakdown
  6. `/idea.implement` - Execute with **IDE Loop** (see below)
  7. `/idea.analyze` - Verify compliance (optional)
  8. `/idea.checklist` - Final validation (optional)

### IDE Loop: Implement → Defend → Evaluate

**⚠️ CRITICAL**: For ALL tiers (even Tier 1), follow IDE Loop pattern:

**Each phase MUST complete I-D-E cycle before proceeding:**

1. **Implement (I)** - Write code
   - Follow AVA coding standards
   - Update documentation inline
   - Handle errors explicitly
   - Self-review against checklist

2. **Defend (D)** - Write tests **IMMEDIATELY** (NON-NEGOTIABLE)
   - Unit tests for all new functions
   - Integration tests for critical paths
   - Edge case + error condition tests
   - 80%+ coverage target (new code)
   - 100% coverage for critical paths
   - All tests MUST pass (new + existing)
   - **Cannot skip** - testing is mandatory

3. **Evaluate (E)** - Assess quality objectively
   - Verify acceptance criteria met
   - Check performance budgets
   - Run all quality gates
   - Get approval before commit
   - **COMMIT** (atomic, descriptive message)
   - Update tracking docs

**Phase Blockers** (fix before proceeding):
- ❌ Any failing tests
- ❌ Reduced code coverage
- ❌ Linting errors or warnings
- ❌ Uncommitted changes
- ❌ Missing documentation
- ❌ Unmet acceptance criteria

**Failure Recovery:**
- If Defend reveals issues → return to Implement
- If Evaluate reveals issues → return to Implement
- Fix issues, then repeat D-E cycle

### Git Workflow

**Branch Strategy:**
- `main` - Production-ready
- `feature/###-feature-name` - Features (matches spec number)
- `bugfix/description` - Bug fixes

**Commit Standards:**
- Format: Conventional Commits (feat:, fix:, docs:, test:, perf:)
- Stage by category: docs → code → tests
- NO AI/Claude references in commits

### Multi-Agent Architecture (IDEACODE Protocol)

AVA AI uses **parallel specialist agents** for 90%+ time savings on complex tasks:

**Specialist Agents:**
1. **android-expert** - Android-specific implementation (Compose, Material 3, Android APIs)
2. **kotlin-expert** - Kotlin/KMP expertise (coroutines, Flow, multiplatform)
3. **database-expert** - SQLDelight, Room, Supabase, data modeling
4. **nlu-expert** - ONNX NLU, MobileBERT, intent classification, Teach-Ava
5. **llm-expert** - MLC LLM, llama.cpp, Ollama, RAG, embeddings
6. **test-specialist** - Test strategy, coverage, integration/unit tests
7. **documentation-specialist** - Specs, living docs, API documentation
8. **ui-expert** - VoiceAvenue theme system, glassmorphism, accessibility

**Deployment Strategy:**
- **Parallel execution**: Deploy 2-4 agents simultaneously for independent tasks
- **Sequential for dependencies**: Chain agents when output depends on previous work
- **Handoff protocol**: Each agent updates living docs for next agent
- **Quality gates**: All agents follow IDE Loop (Implement → Defend → Evaluate)

**Example: Chat UI Implementation (Week 6)**
```
Parallel agents:
├─ ui-expert → Design Compose chat components
├─ database-expert → Conversation/Message repository integration
└─ nlu-expert → Intent classification integration

Sequential after parallel completion:
└─ test-specialist → Integration tests for end-to-end flow
```

**Agent Communication:**
- Living docs as handoff mechanism (`docs/ProjectInstructions/notes.md`)
- CLAUDE.md for project context
- Specs define acceptance criteria for all agents
- Each agent commits atomically after E phase

---

## Governance

### Amendment Process

This constitution can be amended through:

1. **Proposal** - Document change with rationale
2. **Impact Analysis** - Review effect on existing work
3. **Version Determination** - Semantic versioning (MAJOR.MINOR.PATCH)
4. **Update Execution** - Modify document
5. **Propagation** - Sync templates and docs
6. **Approval** - Explicit sign-off

### Compliance Enforcement

**Automated:**
- Performance monitoring in CI/CD
- Privacy checks (local-first verification)
- Constitutional AI scoring in tests
- Integration tests with VOS4

**Manual:**
- Tier 3 features require constitution compliance check
- Pull requests include compliance checklist
- Quarterly constitution review

---

## Integration with VOS4/VoiceAvenue

### Shared Capabilities (from VOS4)

AVA **MUST NOT** reimplement these - use VOS4's existing modules:

✅ **Speech Recognition** → Use VOS4 `SpeechRecognitionManager`
✅ **UI Theme System** → Use VoiceAvenue `PluginSystem/themes` (ThemeManager, ThemeDefinition)
✅ **Base Theme** → Extend `MagicDreamTheme` via YAML theme inheritance
✅ **Accessibility** → Use VOS4 `AccessibilityService`
✅ **Smart Glasses Comm** → Use VOS4 device managers

### AVA-Specific Capabilities (new)

AVA **MUST** implement these as unique features:

✅ **Hybrid NLU Engine** (ONNX + Rules + MLC LLM fallback)
✅ **Teach-Ava Training System** (user-correctable intent recognition)
✅ **RAG System** (Faiss local index + optional Supabase backup):
  - **Local-First**: Faiss indexes embeddings for fast search (<100ms)
  - **Metadata Storage**: SQLDelight stores text, IDs, `cloudRef` URLs
  - **Cloud Backup**: Supabase stores raw embeddings + large images (Phase 6)
  - **Data Flow**: Gemma → Embeddings → Faiss (index) → Supabase (backup) → SQLDelight (metadata)
✅ **Cognitive Memory** (Working/Episodic/Semantic/Procedural)
✅ **Constitutional AI** (Self-critique + 7 principles) - Phase 3
✅ **Workflow Creation** (PDF/web → guided steps)
✅ **Vision Integration** (OCR + object recognition)

### Plugin Architecture

AVA exists in **two modes**:

1. **Standalone Mode**: Full Android app for non-VOS4 users
2. **Plugin Mode**: MagicCode plugin integrated into VOS4 ecosystem

Both modes share **100% of core AI code** (KMP modules).

---

## Ratification

**Version**: 1.3.1
**Ratified**: 2025-01-28
**Ratified By**: Manoj Jhawar
**Status**: Active - IDEACODE Framework + JSON Standards
**Last Amended**: 2025-11-09
**Next Review**: 2025-02-28 (30 days)

**Version History:**
- `1.3.1` (2025-11-09) - **JSON Code Standards** (PATCH: added mandatory compact JSON formatting rules, array-first preference, wrapper elimination standards)
- `1.3.0` (2025-01-28) - **IDEACODE Framework Adoption** (MINOR: adopted IDEACODE v1.0 methodology, IDE Loop enforcement, 80%+ test coverage mandate, spec-first development, multi-agent architecture, living documentation)
- `1.2.2` (2025-10-27) - MagicCode Theme System Adopted (PATCH: adopted VoiceAvenue plugin theme system with YAML inheritance, hot-reload support)
- `1.2.1` (2025-10-27) - Context-Aware AI + Revenue Model (PATCH: added Phase 3 context features, subscription tiers, tokenization docs, multi-tenant webapp)
- `1.2.0` (2025-10-27) - AVA Supabase patterns integrated (PATCH: cloud storage clarifications, RLS patterns, self-hosting, data limits)
- `1.1.0` (2025-10-27) - Hybrid architecture adopted (MINOR: added Teach-Ava principle, ONNX NLU, phased quality gates)
- `1.0.0` (2025-10-26) - Initial AVA constitution ratified

---

## Hybrid Architecture Decision (v1.1.0)

**Architecture Evaluation Completed**: 2025-10-27

AVA adopts a **hybrid architecture** combining:
- **AvaAssistant Foundation** (ONNX NLU, Teach-Ava, SQLDelight, proven Android+Desktop codebase)
- **Synthesized Enhancements** (Constitutional AI, Faiss RAG, VOS4 integration, smart glasses ecosystem)

**Key Architectural Decisions:**

1. **NLU Strategy**: Hybrid ONNX + MLC
   - ONNX Runtime Mobile for fast intent classification (<50ms, 12MB models)
   - KeywordFallbackClassifier for user-trainable rules
   - MLC LLM for complex queries requiring generation (2GB models)
   - **Rationale**: Balances performance (ONNX), personalization (Teach-Ava), and capability (MLC)

2. **Database Strategy**: SQLDelight + Room façade
   - SQLDelight 2.1 as cross-platform primary database
   - Room DAO pattern façade for Android compatibility
   - **Rationale**: Proven in AvaAssistant, cross-platform ready, Android-familiar API

3. **Deployment Strategy**: Dual-mode from start
   - Standalone Android/Desktop app (Phases 1-3)
   - VoiceAvenue plugin for VOS4 integration (Phase 4)
   - **Rationale**: Enables 2-month MVP while preserving VOS4 integration path

4. **Feature Phasing**: Progressive enhancement
   - **Phase 1 (Months 1-2)**: AvaAssistant foundation (ONNX, Teach-Ava, basic LLM with MLC/llama.cpp/Ollama, tokenization, **VoiceAvenue theme system integration**)
   - **Phase 2 (Month 3)**: RAG System (Faiss indexing, ONNX embeddings, document ingestion, `cloudRef` pattern, JPEG 80% compression, hybrid local+cloud sync)
   - **Phase 3 (Month 4)**: Constitutional AI + Context-Aware AI (self-critique, ONNX NER, screen scraping via VOS4 AccessibilityService, privacy controls)
   - **Phase 4 (Month 5)**: VOS4 integration + Advanced Context (VoiceAvenue plugin, speech reuse, cross-app context, contextual suggestions, **theme YAML migration to plugin directory**)
   - **Phase 5 (Months 6-7)**: Smart glasses ecosystem (8+ devices, VisionOS UI, WebRTC casting)
   - **Phase 6 (Months 8-9)**: Enterprise features (multi-tenant webapp, RLS, JSON sync, self-hosted Supabase, subscription management, cloud LLM inference)

**Trade-off Analysis:**
- ✅ **Preserves** Teach-Ava uniqueness (user-trainable AI without cloud)
- ✅ **Achieves** 2-month MVP milestone (standalone Android+Desktop)
- ✅ **Integrates** VOS4 capabilities without duplication (Phase 4)
- ✅ **Adds** Constitutional AI safety (Phase 3)
- ✅ **Supports** smart glasses ecosystem (Phase 5)
- ⚠️ **Defers** some enterprise features to later phases (acceptable)

**Implementation Path:**
See `project_planning/Project_CodingInstructions/ARCHITECTURE_COMPARISON.md` for detailed analysis.

Next step: `/idea.specify` to define Phase 1 requirements (AvaAssistant foundation).

---

## AVA Supabase Integration Patterns (v1.2.0)

**Integration Completed**: 2025-10-27

The Supabase implementation analysis provided production-ready patterns for multi-tenant cloud sync, now incorporated into AVA constitution:

### 1. Multi-Tenant Row-Level Security (Phase 6)

**SQL Pattern**:
```sql
ALTER TABLE rag_entries ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON rag_entries
    FOR ALL
    USING (tenant_id = (SELECT auth.jwt()->'app_metadata'->>'tenant_id'));
```

**Application**:
- Apply to tables: `rag_entries`, `knowledge`, `train_example`, `workflows`
- JWT must contain `tenant_id` in `app_metadata`
- Supabase enforces at database level (no application-level filtering needed)

**Testing Requirements**:
- Verify tenant_123 cannot access tenant_456 data
- Test unauthorized access returns 403
- Validate JWT parsing and RLS policy enforcement

### 2. Cloud Storage Separation Strategy (Phase 2 & 6)

**Pattern**:
```kotlin
data class RagEntry(
    val id: String,
    val text: String,                     // Stored in SQLDelight
    val embedding: FloatArray,            // Indexed in Faiss
    val cloudRef: String?,                // "supabase://images/pump1.jpg"
    val embeddingCloudRef: String?,       // "supabase://embeddings/{id}"
    val tenantId: String?                 // Phase 6: Multi-tenant
)
```

**Rules**:
- **Local Storage** (SQLDelight): Metadata, text, IDs, `cloudRef` URLs
- **Cloud Storage** (Supabase): Large data only (images >1MB, raw embeddings)
- **Compression**: JPEG 80% quality before upload
- **Data Limit**: 1 GB per tenant with client-side quota checks

**Benefits**:
- Optimizes local storage (critical for low-end devices)
- Lazy loading (fetch from `cloudRef` only when needed)
- Offline-first (local data always available)

### 3. Cross-Platform JSON Sync (Phase 6)

**Sync Functions**:
```kotlin
suspend fun syncRag(data: List<RagEntry>, tenantId: String, enabled: Boolean) {
    if (!enabled) return  // Privacy-first: opt-in only
    SupabaseClient.from("rag_table").insert(data.map { it.toJson() }).execute()
}

suspend fun fetchRag(tenantId: String): List<RagEntry> {
    val json = SupabaseClient.from("rag_table").select()
        .eq("tenant_id", tenantId).execute().data
    return Json.decodeFromString(json)
}
```

**Workflow**:
1. Web app uploads data to Supabase
2. Mobile apps download JSON-encoded data
3. Cache locally for offline use (FileOutputStream)
4. Sync when online (if user opted in)

**Privacy Safeguard**:
- Default: `cloudSyncEnabled = false` (opt-in)
- User must explicitly enable cloud sync in preferences
- No auto-sync without consent

### 4. Self-Hosted Supabase (Phase 6)

**Deployment Options**:
```bash
# Docker Compose deployment
docker-compose -f supabase/docker-compose.yml up

# Tenant-specific schemas
CREATE SCHEMA tenant_123;
CREATE SCHEMA tenant_456;
```

**Ktor Proxy Pattern**:
```kotlin
get("/tenant/{id}/supabase") {
    val tenantId = call.parameters["id"]
    proxyToTenantSupabase(tenantId)  // Route to tenant schema
}
```

**Use Cases**:
- Data sovereignty (data must stay in-country)
- Compliance: GDPR, HIPAA, regulated industries
- On-premises enterprise deployment
- Air-gapped environments

**Documentation Required** (Phase 6 spec):
- Docker/Kubernetes deployment guide
- Tenant schema setup scripts
- Ktor proxy configuration examples
- RLS policy migration for self-hosted instances

### 5. RAG Creation & Ingestion Pipeline (Phase 2)

**RAG Dataset Creation Flow**:
```
Document Upload (PDF/Web/Image)
    → Document Parser (Apache PDFBox, skrape{it})
    → Text Extraction + Image Extraction (OCR via Tesseract)
    → Text Chunking (512 tokens, 128 overlap)
    → ONNX Embedding Model (MiniLM-e5-small INT8)
    → Embeddings (384-dim vectors)
    → Faiss Index (local)
    → SQLDelight Metadata (text, cloudRef, source)
    → [Optional] Supabase Backup (Phase 6)
```

**Implementation Location**: `core/rag/` module (Phase 2)

**Key Components**:
```kotlin
// 1. Document ingestion
interface DocumentIngester {
    suspend fun ingestPdf(file: File): List<DocumentChunk>
    suspend fun ingestWebPage(url: String): List<DocumentChunk>
    suspend fun ingestImage(file: File): List<DocumentChunk>  // OCR
}

// 2. Text chunking
class TextChunker(
    val chunkSize: Int = 512,        // tokens per chunk
    val overlap: Int = 128            // token overlap
) {
    fun chunk(text: String): List<String>
}

// 3. Embedding generation (ONNX)
class OnnxEmbeddingEngine(
    modelPath: String = "assets/nlu/minilm-e5-small-int8.onnx"
) {
    suspend fun embed(text: String): FloatArray  // 384-dim vector
    suspend fun embedBatch(texts: List<String>): List<FloatArray>
}

// 4. RAG entry creation
data class DocumentChunk(
    val id: String,
    val text: String,                 // Original text chunk
    val embedding: FloatArray,        // 384-dim vector
    val source: String,               // PDF name, URL, image path
    val pageNumber: Int?,             // For PDFs
    val imageRef: String?,            // For extracted images (compressed JPEG 80%)
    val cloudRef: String?,            // Supabase reference (Phase 6)
    val tenantId: String?,            // Multi-tenant (Phase 6)
    val createdAt: Long               // Timestamp
)
```

**RAG Creation API**:
```kotlin
class RagCreator @Inject constructor(
    private val ingester: DocumentIngester,
    private val chunker: TextChunker,
    private val embedder: OnnxEmbeddingEngine,
    private val faissIndex: FaissIndex,
    private val ragDao: RagDao
) {
    suspend fun createRagFromPdf(pdfFile: File): RagDataset {
        // 1. Ingest document
        val chunks = ingester.ingestPdf(pdfFile)

        // 2. Chunk text
        val textChunks = chunks.flatMap { chunk ->
            chunker.chunk(chunk.text).map {
                DocumentChunk(
                    id = UUID.randomUUID().toString(),
                    text = it,
                    source = pdfFile.name,
                    pageNumber = chunk.pageNumber,
                    imageRef = chunk.imageRef  // Extracted images
                )
            }
        }

        // 3. Generate embeddings (batch for efficiency)
        val embeddings = embedder.embedBatch(textChunks.map { it.text })
        val entriesWithEmbeddings = textChunks.zip(embeddings) { chunk, emb ->
            chunk.copy(embedding = emb, createdAt = System.currentTimeMillis())
        }

        // 4. Index in Faiss (local)
        entriesWithEmbeddings.forEach { entry ->
            faissIndex.add(entry.id, entry.embedding)
        }

        // 5. Store metadata in SQLDelight
        ragDao.insertBatch(entriesWithEmbeddings.map {
            it.copy(embedding = FloatArray(0))  // Don't duplicate in DB
        })

        return RagDataset(
            id = UUID.randomUUID().toString(),
            name = pdfFile.nameWithoutExtension,
            entryCount = entriesWithEmbeddings.size,
            createdAt = System.currentTimeMillis()
        )
    }

    suspend fun createRagFromWeb(url: String): RagDataset {
        val chunks = ingester.ingestWebPage(url)
        // Similar to createRagFromPdf...
    }
}
```

### 6. Faiss + Supabase Interaction Flow (Phase 2 & 6)

**Query Flow**:
```
User Query
    → ONNX Embedding (query vector)
    → Faiss Search (k=5 nearest neighbors, <100ms)
    → Retrieve Metadata (SQLDelight)
    → [Optional] Fetch Images (from cloudRef if needed)
    → Context Assembly (top-k chunks)
    → LLM Prompt (MLC/llama.cpp/Ollama with RAG context)
    → Response Generation
```

**Storage Flow**:
```kotlin
suspend fun addRagEntry(entry: DocumentChunk, syncToCloud: Boolean = false) {
    // 1. Always index locally (fast <100ms search)
    faissIndex.add(entry.id, entry.embedding)

    // 2. Always store metadata locally
    ragDao.insert(entry.copy(embedding = FloatArray(0)))

    // 3. Optionally backup to cloud (Phase 6)
    if (syncToCloud && entry.tenantId != null) {
        // Upload embedding vector (INT8 quantized for 4x size reduction)
        val embeddingRef = supabase.uploadEmbedding(entry.embedding, entry.tenantId)

        // Upload associated image if present (JPEG 80% compressed)
        val imageRef = entry.imageRef?.let { localPath ->
            supabase.uploadImage(File(localPath), entry.tenantId)
        }

        // Update cloudRef in metadata
        ragDao.updateCloudRefs(entry.id, embeddingRef, imageRef)
    }
}
```

**Key Principles**:
- **Faiss is primary**: Local index for fast RAG queries (<100ms)
- **Supabase is backup**: Cloud storage for cross-device sync (Phase 6)
- **SQLDelight is metadata**: Stores text, IDs, references, cloudRef URLs
- **No duplication**: Embeddings stored in Faiss OR Supabase, not both locally
- **Lazy loading**: Images fetched from cloudRef only when needed

### 7. Testing Requirements (Phase 6)

**Supabase Integration Tests**:
```kotlin
@Test
fun `verify tenant isolation`() {
    // tenant_123 can access own data
    // tenant_123 cannot access tenant_456 data
}

@Test
fun `test sync performance`() {
    // 1 GB data load completes in <5 minutes
}

@Test
fun `verify offline cache`() {
    // App works without network
    // Cached data accessible locally
}

@Test
fun `validate compression quality`() {
    // JPEG 80% maintains readability
    // File size reduction >50%
}

@Test
fun `enforce quota limits`() {
    // Client rejects upload exceeding 1 GB
    // Supabase policy enforces server-side
}
```

### 8. AOSP Compatibility Rationale

**Why Supabase (not Firebase)**:
- Open-source (MIT license)
- No Google Play Services dependency (critical for AOSP builds)
- Self-hosting capability (Docker/Kubernetes)
- Built-in RLS for multi-tenancy
- RESTful API (works with KMP on all platforms)

**Alignment with Privacy-First Principle**:
- User controls infrastructure (self-hosted option)
- Opt-in cloud sync (no forced telemetry)
- E2E encryption support
- GDPR/HIPAA compliant

### References

- **Supabase Analysis**: `project_planning/Project_CodingInstructions/AVA_SUPABASE_ANALYSIS.md`
- **Architecture Comparison**: `project_planning/Project_CodingInstructions/ARCHITECTURE_COMPARISON.md`
- **Context-Aware AI Analysis**: `.ideacode/CONTEXT_AWARE_ANALYSIS.md`
- **Architecture Clarifications**: `.ideacode/ARCHITECTURE_CLARIFICATIONS.md`
- **Phase 2 Spec** (to be created): Will include `cloudRef` pattern, compression, hybrid RAG
- **Phase 3 Spec** (to be created): Will include Constitutional AI, Context-Aware AI, ONNX NER
- **Phase 6 Spec** (to be created): Will include multi-tenant webapp, RLS, JSON sync, self-hosting, subscription management

---

## VoiceAvenue Theme System Adoption (v1.2.2)

**Decision Date**: 2025-10-27
**Decision ID**: DESIGN-001
**Impact**: High (affects all UI components, Phase 4 integration)

AVA adopts **VoiceAvenue's Plugin Theme System** (formerly MagicCode) with YAML-based theme inheritance for UI/UX design.

### Decision Summary

**Selected Approach**: Option A - Use VoiceAvenue's Plugin Theme System

**Key Benefits**:
1. **Plugin-compatible from Phase 1** (Phase 4 integration = copy YAML file, ~1 hour)
2. **Theme inheritance** (extend MagicDreamTheme, add 2 AVA-specific colors)
3. **Hot-reload support** (edit YAML → reload without recompile)
4. **Zero design work** (MagicDreamTheme is production-ready with glassmorphism)
5. **Instant VOS4 consistency** (same theme system from day 1)

**Tradeoffs Accepted**:
- Medium complexity (plugin architecture required)
- 3-5 days initial setup vs 1-2 days for simple copy

**Total Time Investment**: 3-5 days (Phase 1) + 0 days (Phase 4 migration) = **3-5 days total**

### Implementation

**AVA Theme Definition** (`ava-theme.yaml`):
```yaml
name: "AVA Assistant Theme"
version: "1.0.0"
description: "VisionOS-inspired theme for AVA AI assistant"
author: "Augmentalis"
extends: "magic-dream"  # Inherit from VOS4's MagicDreamTheme

colors:
  # Inherit MagicDream base colors
  primary: "#9C88FF"      # Soft Purple
  secondary: "#B794F6"    # Light Purple

  # AVA-specific colors
  contextHighlight: "#FFAB00"    # For context-aware feature highlights
  ragDatasetAccent: "#00BFA5"    # For RAG dataset UI accents

effects:
  blur:
    small: 4
    medium: 8
    large: 16    # Glassmorphism support
```

**Loading in AVA**:
```kotlin
val themeManager = ThemeManager(registry, assetResolver)
val avaTheme = themeManager.loadTheme("ava", "ava-theme.yaml")

@Composable
fun AvaApp() {
    ApplyMagicTheme(avaTheme) {
        AvaMainScreen()
    }
}
```

### VoiceAvenue Theme System Architecture

**Location**: `vos4/modules/libraries/PluginSystem/src/commonMain/kotlin/com/augmentalis/voiceavenue/plugins/themes/`

**Components**:
1. **ThemeDefinition.kt** - Serializable theme data model (YAML/JSON compatible)
2. **ThemeManager.kt** - Theme loading, hot-reload, font loading, validation
3. **ThemeComponents.kt** - ColorPalette, Typography, Effects (blur), Spacing, Animations
4. **MagicDreamTheme.kt** - Production-ready glassmorphism theme (purple/pink gradients)
5. **MagicThemeCustomizer.kt** - Live theme editor with real-time preview

**Key Features**:
- Theme inheritance via `extends` field
- Hot-reload support (FR-014)
- Custom font loading
- Plugin asset resolution
- Theme validation (semver versioning)

### Phase 1 Implementation Plan (3-5 days)

**Day 1-2**: VoiceAvenue PluginSystem integration
- Add dependency to AVA project
- Create plugin registry and asset resolver
- Test theme loading with MagicDreamTheme

**Day 3**: AVA theme creation
- Create `ava-theme.yaml` extending MagicDream
- Add AVA-specific colors (contextHighlight, ragDatasetAccent)
- Apply theme to Compose UI

**Day 4**: Hot-reload setup
- Enable file watching for `ava-theme.yaml`
- Test edit → save → reload workflow

**Day 5**: AVA-specific components & testing
- Integrate contextHighlight into context-aware UI
- Integrate ragDatasetAccent into RAG UI
- Run test suite (theme loads, hot-reload, dark mode, glassmorphism)

### Phase 4 Integration Plan (1 hour)

**Effort**: Trivial

**Steps**:
1. Copy `ava-theme.yaml` to VOS4 plugin directory
2. Register AVA as VOS4 plugin
3. Done! AVA runs as plugin with same theme

**No code changes required** - theme is already plugin-compatible.

### Alternative Options Rejected

**Option B** (Copy MagicDreamTheme directly): Saves 2-3 days initially, but costs 3-5 days in Phase 4 migration + no hot-reload

**Option C** (Build custom AVA design system): 8-10 weeks design work + 10-15 days Phase 4 migration = unacceptable

**Option D** (Material 3 now, MagicCode later): Breaking visual change in Phase 4 confuses users + 5-7 days migration

### Success Criteria

**Phase 1 (Week 1)**:
- ✅ AVA UI uses MagicCode theme system
- ✅ ava-theme.yaml extends MagicDream
- ✅ Hot-reload works
- ✅ AVA-specific colors (contextHighlight, ragDatasetAccent) applied

**Phase 4 (Month 5)**:
- ✅ AVA runs as VOS4 plugin with same theme
- ✅ No theme migration required
- ✅ Theme changes in VOS4 automatically apply to AVA plugin

### References

- **Design Decision Document**: `.ideacode/design/ui-theme-system.md`
- **Design Q&A Session**: `.ideacode/DESIGN_QA_SESSION.md`
- **VoiceAvenue ThemeDefinition**: `vos4/modules/libraries/PluginSystem/.../themes/ThemeDefinition.kt`
- **VoiceAvenue ThemeManager**: `vos4/modules/libraries/PluginSystem/.../themes/ThemeManager.kt`
- **MagicDreamTheme**: `vos4/modules/apps/VoiceUI/.../theme/MagicDreamTheme.kt`

**Note**: VoiceAvenue was formerly known as MagicCode. All references to MagicCode in this document now refer to VoiceAvenue.

---

## Revenue Model & Subscription Tiers (v1.2.1)

**Added**: 2025-10-27

AVA operates on a **freemium model** with three tiers, balancing user acquisition (free tier) with revenue generation (Pro/Enterprise tiers):

### Free Tier: Local-Only RAG
**Price**: $0/month
**Target**: Privacy-conscious users, offline workers, user acquisition

**Features**:
- ✅ Local RAG creation (PDFs, web, images)
- ✅ Unlimited local storage (device capacity only)
- ✅ On-device LLM (Gemma 2B via MLC/llama.cpp/Ollama)
- ✅ ONNX NLU + Teach-Ava training
- ✅ Context-Aware AI (local screen scraping)
- ❌ No cloud sync
- ❌ No cross-device access
- ❌ No collaborative RAG

**Purpose**: Attract users with full-featured free tier, demonstrate value

---

### Pro Tier: Hybrid RAG with Cloud Sync
**Price**: $9.99/month
**Target**: Professionals needing cross-device sync

**Features**:
- ✅ All Free tier features
- ✅ **10 GB cloud storage** (RAG datasets, images, embeddings)
- ✅ **Cross-device sync** (create on web, use on mobile/desktop)
- ✅ **Collaborative RAG** (share datasets with team members, up to 5 users)
- ✅ **Web dashboard** (manage RAG datasets, view storage usage)
- ✅ **Priority support** (email, 24-hour response)
- ⚠️ Still uses on-device LLM (no cloud inference)

**Cloud Storage Breakdown**:
- RAG embeddings: ~500 MB per 1,000 documents
- Images (JPEG 80%): ~50 KB per image
- 10 GB supports: ~20,000 documents + 200,000 images

**Revenue Potential**: **PRIMARY REVENUE STREAM**
- 25% conversion rate (2,500 users from 10,000 free users)
- $9.99 × 2,500 = **$24,975/month**

---

### Enterprise Tier: Cloud-Powered RAG with Premium LLM
**Price**: $49.99/month + usage-based LLM costs
**Target**: Enterprise customers, high-value power users

**Features**:
- ✅ All Pro tier features
- ✅ **Unlimited cloud storage**
- ✅ **Cloud LLM inference** (GPT-4, Claude 3.5, Gemini Pro via OpenRouter)
- ✅ **Faster responses** (cloud GPU vs on-device)
- ✅ **Larger models** (70B Llama vs 2B Gemma on-device)
- ✅ **API access** (programmatic RAG queries, webhooks)
- ✅ **Self-hosted Supabase** option (compliance, data sovereignty)
- ✅ **Unlimited users** per tenant
- ✅ **Dedicated support** (Slack/Teams integration, 4-hour SLA)
- ✅ **Custom contracts** (HIPAA, SOC 2, GDPR compliance)

**LLM Pricing** (pass-through from OpenRouter):
- First **1M tokens free** per month (included)
- After: **$0.02 per 1K tokens** (input + output)
- Example: 10M tokens/month = 1M free + 9M × $0.02 = **$180 usage fee**

**Revenue Potential**: **HIGH-VALUE CUSTOMERS**
- 5% conversion rate (500 users from 10,000 free users)
- Base: $49.99 × 500 = $24,995/month
- LLM usage: Avg $50/user/month = $25,000/month
- Total: **$49,995/month from Enterprise**

---

### Revenue Projections

**At 10,000 Users**:
| Tier | Users | Monthly Revenue |
|------|-------|-----------------|
| Free (70%) | 7,000 | $0 |
| Pro (25%) | 2,500 | $24,975 |
| Enterprise (5%) | 500 | $49,995 |
| **Total** | **10,000** | **$74,970/month** |

**Annual Run Rate**: $899,640/year

**At 100,000 Users** (Year 2 target):
| Tier | Users | Monthly Revenue |
|------|-------|-----------------|
| Free (70%) | 70,000 | $0 |
| Pro (25%) | 25,000 | $249,750 |
| Enterprise (5%) | 5,000 | $499,950 |
| **Total** | **100,000** | **$749,700/month** |

**Annual Run Rate**: $8,996,400/year (~$9M ARR)

---

### Pricing Strategy Rationale

**Free Tier Positioning**:
- Full-featured (not crippled) to demonstrate value
- Local-only constraint is privacy *benefit*, not limitation
- Attracts privacy-conscious users (key target market)
- No credit card required (reduces friction)

**Pro Tier Sweet Spot**:
- $9.99/month = affordable for individuals
- 10 GB cloud storage = sufficient for professionals (20K documents)
- Cross-device sync = killer feature justifying cost
- Collaborative RAG = team use case (5 users = $2/user/month effective cost)

**Enterprise Tier Value**:
- $49.99 base + usage = aligned with enterprise budgets
- Cloud LLM = faster/better responses than on-device
- API access = automation & integration value
- Self-hosted option = compliance requirement (HIPAA, SOC 2)
- Unlimited storage/users = scales with enterprise needs

---

### Upgrade Triggers (Built into Product)

**Free → Pro Upgrade Prompts**:
1. User creates 5+ RAG datasets → "Sync across devices with Pro"
2. User reaches device storage limit → "Free up space with cloud storage"
3. User tries to share RAG dataset → "Enable collaboration with Pro"
4. User switches devices → "Access your RAG on all devices with Pro"

**Pro → Enterprise Upgrade Prompts**:
1. User exceeds 10 GB storage → "Upgrade to unlimited storage"
2. User has slow on-device LLM → "Get 10x faster responses with cloud LLM"
3. User adds 6th team member → "Add unlimited users with Enterprise"
4. User queries API docs → "Enable API access with Enterprise"

---

### Competitive Pricing Analysis

| Product | Price | Cloud Storage | LLM | RAG |
|---------|-------|---------------|-----|-----|
| **AVA Pro** | $9.99/mo | 10 GB | On-device | ✅ Full RAG |
| ChatGPT Plus | $20/mo | ❌ None | Cloud (GPT-4) | ⚠️ Limited RAG |
| Claude Pro | $20/mo | ❌ None | Cloud (Claude 3.5) | ⚠️ Limited RAG |
| Perplexity Pro | $20/mo | ❌ None | Cloud (Multiple) | ⚠️ Web search only |
| **AVA Enterprise** | $49.99/mo | Unlimited | Cloud (GPT-4/Claude) | ✅ Full RAG + API |

**AVA Advantage**:
- **50% cheaper** than competitors for Pro tier
- **Only solution** with local+cloud hybrid RAG
- **Privacy-first** option (free tier, local-only)
- **Full RAG control** (upload your docs, not web-only)

---

### Implementation Requirements

**Phase 2 (Month 3)**: RAG Subscription Logic
```kotlin
class SubscriptionManager {
    fun getCurrentPlan(): SubscriptionPlan
    fun canSyncToCloud(): Boolean
    fun getRemainingStorage(): Long
    fun showUpgradePrompt(trigger: UpgradeTrigger)
}

enum class SubscriptionPlan {
    FREE,       // Local-only
    PRO,        // 10 GB cloud
    ENTERPRISE  // Unlimited cloud + Cloud LLM
}
```

**Phase 6 (Months 8-9)**: Subscription Management
- Stripe integration (payment processing)
- Multi-tenant web app (subscription dashboard)
- Usage analytics (storage, LLM tokens)
- Billing & invoices
- Self-serve upgrade/downgrade

---

---

## IDEACODE Framework Adoption (v1.3.0)

**Decision Date**: 2025-01-28
**Decision ID**: ARCH-002
**Impact**: Critical (affects all development processes, quality gates, documentation)

AVA AI adopts **IDEACODE v1.0** methodology (merger of IDEADEV + GitHub Spec-Kit) as the official development framework.

### Decision Summary

**What Changed:**
- ✅ Mandatory spec-first development (use `/idea.specify` before code)
- ✅ IDE Loop enforcement (Implement → Defend → Evaluate → Commit)
- ✅ 80%+ test coverage requirement (non-negotiable)
- ✅ Living documentation system (`docs/ProjectInstructions/`)
- ✅ Multi-agent architecture (8 specialist agents)
- ✅ Quality gates at every phase
- ✅ Slash command workflow automation

**Why IDEACODE:**
1. **Proven methodology** - Battle-tested on VOS4 (50,000+ LOC)
2. **Quality enforcement** - IDE Loop prevents shipping untested code
3. **Time savings** - Multi-agent deployment saves 90%+ time on complex tasks
4. **Alignment with VOS4** - Shared development practices for easier integration (Phase 4)
5. **Spec-driven clarity** - WHAT/WHY before HOW reduces rework
6. **Living documentation** - Continuous knowledge capture for future agents

### IDEACODE Structure Created

```
/Volumes/M Drive/Coding/AVA AI/
├── .ideacode/
│   ├── memory/
│   │   └── principles.md          ← This file (AVA Constitution + IDEACODE)
│   ├── scripts/                   ← Automation scripts
│   └── templates/                 ← Spec/plan templates
├── specs/                         ← Feature specifications (WHAT/WHY)
│   └── (to be populated)
├── docs/
│   └── ProjectInstructions/       ← Living documentation
│       ├── notes.md               ← Implementation insights
│       ├── decisions.md           ← Architecture Decision Records (ADR)
│       ├── bugs.md                ← Bug tracking
│       ├── progress.md            ← Weekly status
│       └── backlog.md             ← Future features
├── .claude/
│   └── commands/                  ← Slash commands
│       ├── idea.principles.md     ← Review principles
│       ├── idea.specify.md        ← Create spec
│       ├── idea.plan.md           ← Create plan
│       ├── idea.tasks.md          ← Generate tasks
│       ├── idea.implement.md      ← Execute IDE Loop
│       ├── idea.analyze.md        ← Verify compliance
│       └── idea.checklist.md      ← Final validation
└── CLAUDE.md                      ← Quick reference for AI agents
```

### Migration Impact

**Week 5 → Week 6 Transition:**
- Week 5 (complete): Database, NLU, Teach-Ava UI ✅
- Week 6 (next): Chat UI implementation 🚀
- **New workflow**: Use `/idea.specify` to create Chat UI spec BEFORE coding
- **Agent deployment**: Parallel ui-expert, database-expert, nlu-expert agents
- **Quality assurance**: IDE Loop on every component (I-D-E-Commit)

**Backward Compatibility:**
- ✅ All existing code unchanged (Week 1-5 work preserved)
- ✅ Test suite maintained (92% coverage already exceeds 80% target)
- ✅ Documentation migrated to living docs
- ✅ Git workflow unchanged (feature branches, conventional commits)

**Forward Requirements:**
- 🔄 All Tier 3 features (>4 hours) use full IDEACODE workflow
- 🔄 All code follows IDE Loop (even Tier 1 simple fixes)
- 🔄 80%+ test coverage enforced (Defend phase cannot be skipped)
- 🔄 Specs created before implementation (no "code first, document later")

### Success Criteria

**IDEACODE adoption complete when:**
- ✅ Principles updated with IDE Loop + multi-agent architecture
- ⏳ Living docs populated (notes.md, decisions.md, bugs.md, progress.md, backlog.md)
- ⏳ CLAUDE.md customized with AVA context
- ⏳ Slash commands verified working
- ⏳ First spec created (Chat UI Week 6)
- ⏳ First feature implemented with full IDE Loop

**Current Status**: 1/6 complete (principles updated)

### Compliance Enforcement

**Automated Checks:**
- Test coverage reports (must be 80%+)
- Linting enforcement (must pass before commit)
- CI/CD pipeline (all tests must pass)

**Manual Checks:**
- Code review verifies spec alignment
- Evaluate phase confirms acceptance criteria met
- Quarterly constitution review (next: 2025-02-28)

**Violations:**
- ⚠️ Warning: Test coverage <80%
- ⚠️ Warning: Spec missing for Tier 3 feature
- ❌ Block: Failing tests prevent commit
- ❌ Block: No Defend phase (must write tests)

### References

- **IDEACODE Framework**: `/Volumes/M Drive/Coding/ideacode/`
- **IDE Loop Protocol**: `/Volumes/M Drive/Coding/ideacode/protocols/Protocol-IDE-Loop.md`
- **Migration Plan**: `.ideacode/MIGRATION_TO_IDEACODE_PLAN.md`
- **Slash Commands**: `.claude/commands/idea.*.md`
- **Living Docs**: `docs/ProjectInstructions/`

---

**End of AVA Constitution**
