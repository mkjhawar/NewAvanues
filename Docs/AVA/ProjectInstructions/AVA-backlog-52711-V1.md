# AVA AI - Feature Backlog

**Last Updated**: 2025-11-26
**Phase**: 2.0 - Production Ready
**Framework**: IDEACODE v8.4

---

## Purpose

Track planned features, enhancements, and ideas for future AVA AI development. Organized by phase and priority.

---

## Recently Completed (2025-11-26)

### AON 3.0 Semantic Ontology ✅ COMPLETE
- [x] Schema upgrade from 2.0 to 3.0
- [x] 28 intents across 5 categories
- [x] Zero-shot classification with semantic descriptions
- [x] Entity extraction patterns
- [x] Multi-step action sequences
- [x] Capability mappings for app resolution
- [x] AonFileParser updated for 3.0 schema

### Hardware-Aware GPU Acceleration ✅ COMPLETE
- [x] InferenceBackendSelector implementation
- [x] Backend priority matrix (QNN/HTP > NNAPI > Vulkan > OpenCL > CPU)
- [x] TVMRuntime Vulkan support
- [x] IntentClassifier backend integration
- [x] Snapdragon 625+ compatibility verified

### Production Readiness ✅ COMPLETE
- [x] LICENSE file (proprietary + third-party)
- [x] Copyright headers on key files
- [x] File placement guide documentation
- [x] ADR-008: Hardware-Aware Inference Backend
- [x] ADR-009: iOS Core ML ANE Integration (staged)
- [x] Developer Manual Chapter 47: GPU Acceleration
- [x] Developer Manual Chapter 48: AON 3.0 Format

---

## Phase 1.0 - MVP (Week 1-16) ✅ COMPLETE

### Week 6-8: Chat UI + Teach-Ava Backend ⏳ NEXT

**Status**: In Progress (Week 6 starting)
**Priority**: P0 (Critical for MVP)

**Features:**
- [ ] **FR-006: Basic Conversation UI**
  - Chat screen with message bubbles (user vs assistant)
  - Conversation list (all past conversations)
  - Input field with send button
  - Intent-based responses
  - Low-confidence → Teach-Ava flow
  - Message persistence (Conversation + Message repos)

- [ ] **FR-002: Teach-Ava Backend Testing**
  - Device testing (physical hardware)
  - Performance validation (NLU <100ms)
  - End-to-end classification testing
  - Edge case handling
  - Bug fixes

**Acceptance Criteria:**
- User sends message → AVA responds with intent classification
- Low confidence (<0.5) → suggests adding to Teach-Ava
- Messages persist across app restarts
- Conversations update reactively (Flow)
- End-to-end <500ms (NLU + DB + UI)

**Spec**: To be created (`/idea.specify "Chat UI Week 6"`)

---

### Week 9-10: ALC (Augmentalis LLM Core) ⏸️ PENDING

**Status**: Not Started
**Priority**: P0 (Critical for MVP)

**Features:**
- [ ] **FR-003: ALC (Kotlin rewrite of mlc-llm-android)**
  - Kotlin/Native bindings to llama.cpp
  - Model loader (GGUF format for Gemma 2B)
  - Streaming inference API
  - Memory management (<2GB RAM budget)
  - Thread safety (concurrent requests)
  - UI integration (streaming responses in chat)

**Acceptance Criteria:**
- Gemma 2B INT4 loads successfully (<3 minutes)
- Inference <500ms first token (95th percentile)
- Streaming tokens update UI reactively
- Memory footprint <2GB with model loaded
- No crashes on low-end devices (2GB RAM)

**Blocker**: Need Chat UI complete first for integration testing

**Research Needed:**
- llama.cpp JNI bindings (existing libraries or custom?)
- MLC LLM integration (Kotlin API?)
- Ollama fallback (local server communication)

---

### Week 11-12: Cloud LLM Providers ⏸️ PENDING

**Status**: Not Started
**Priority**: P0 (Critical for MVP)

**Features:**
- [ ] **FR-004: Cloud LLM Provider System**
  - OpenRouter integration (aggregator, 100+ models)
  - Hugging Face Inference API
  - OpenAI API (GPT-4)
  - Anthropic API (Claude 3.5)
  - Google AI API (Gemini Pro)
  - Provider abstraction layer (common interface)
  - Cascading fallback logic (try local → OpenRouter → specific providers)

**Acceptance Criteria:**
- User can select LLM provider in settings
- Fallback chain works: Local LLM → OpenRouter → Direct API
- API keys stored securely (Android EncryptedSharedPreferences)
- Cost tracking (token usage per session)
- Privacy: Opt-in required for cloud LLM

**Pending Decision**: ADR for primary cloud provider (PD-002)

---

### Week 13-14: Integration & Testing ⏸️ PENDING

**Status**: Not Started
**Priority**: P0 (Critical for MVP)

**Features:**
- [ ] **FR-005: Room Database Architecture** (Already Complete!)
  - 6 repositories implemented ✅
  - VOS4 patterns applied ✅
  - Performance validated ✅

- [ ] **FR-007: Privacy Onboarding**
  - Onboarding flow (first launch)
  - Privacy settings screen (local vs cloud)
  - Data management (export, delete all data)
  - Privacy policy acceptance
  - Consent for cloud sync (opt-in)

**Acceptance Criteria:**
- First launch shows onboarding (privacy-first messaging)
- User can enable/disable cloud sync
- User can export all data (JSON format)
- User can delete all data (factory reset)
- Privacy policy displayed and acknowledged

---

### Week 15-16: Polish & Release ⏸️ PENDING

**Status**: Not Started
**Priority**: P0 (Critical for MVP)

**Features:**
- [ ] Bug fixes (all P0/P1 bugs resolved)
- [ ] Performance optimization (meet all budgets)
- [ ] Documentation (user guide, developer docs)
- [ ] Beta testing (10+ external testers)
- [ ] App store submission (Google Play)

**Release Checklist:**
- [ ] All 7 features complete (FR-001 to FR-007)
- [ ] 80%+ test coverage maintained
- [ ] All quality gates passed
- [ ] Performance budgets met
- [ ] Privacy compliance validated
- [ ] User documentation complete
- [ ] Beta feedback incorporated

---

## Phase 1.1 - Enhancements (Month 3-4) **PLANNED**

### Enhanced Features (Post-MVP)

**Status**: Planned (not yet spec'd)
**Priority**: P1 (High)

**Features:**
- [ ] **Conversation Management**
  - Rename conversations
  - Search conversations (by keyword)
  - Archive old conversations
  - Export conversation to text/PDF
  - Conversation tags/categories

- [ ] **Advanced Training**
  - Batch import training examples (CSV/JSON)
  - Training data export (backup)
  - Training data analytics (most frequent intents)
  - Intent grouping/categories
  - Multi-language training support

- [ ] **UI/UX Polish**
  - Dark mode support (auto, light, dark)
  - Custom theme colors
  - Font size settings (accessibility)
  - Animations and transitions
  - Voice input integration (VOS4 speech recognition)

**Estimated Effort**: 3-4 weeks

---

## Phase 2 - RAG System (Month 3) **PLANNED**

### RAG Features

**Status**: Planned (high-level spec exists)
**Priority**: P1 (High)

**Features:**
- [ ] **Document Ingestion**
  - PDF parsing (Apache PDFBox)
  - Web page scraping (skrape{it})
  - Image OCR (Tesseract)
  - Text chunking (512 tokens, 128 overlap)

- [ ] **Embedding & Indexing**
  - ONNX embedding model (MiniLM-e5-small INT8, 20 MB)
  - Faiss local vector index (<100ms search)
  - SQLDelight metadata storage (text, IDs, `cloudRef`)
  - JPEG 80% compression (images)

- [ ] **RAG Query**
  - Vector search (k=5 nearest neighbors)
  - Context assembly (top-k chunks)
  - LLM prompt with RAG context
  - Source attribution (which document?)

- [ ] **RAG Management UI**
  - Upload documents (PDF, images)
  - Browse RAG datasets
  - Delete RAG entries
  - Storage usage tracking

**Acceptance Criteria:**
- User uploads PDF → RAG dataset created (<30 seconds for 100 pages)
- User asks question → AVA retrieves relevant context → answers with source
- Faiss search <100ms
- Total RAG query <2 seconds (search + LLM generation)

**Estimated Effort**: 3-4 weeks

---

## Phase 3 - Constitutional AI + Context (Month 4) **PLANNED**

### AI Safety & Context Features

**Status**: Planned (spec exists)
**Priority**: P1 (High)

**Features:**
- [ ] **Constitutional AI**
  - Self-critique system (validate response against 7 principles)
  - Principle adherence scoring (>90% target)
  - Reject/revise harmful responses
  - Explainable AI ("why did you say that?")

- [ ] **Context-Aware AI**
  - ONNX NER (Named Entity Recognition)
  - Screen scraping via VOS4 AccessibilityService
  - Cross-app context (what user is doing)
  - Privacy controls (opt-in for context awareness)

**Acceptance Criteria:**
- Harmful queries rejected with explanation
- Context-aware suggestions ("You're reading a manual, want me to summarize?")
- Privacy: User controls what apps AVA can see
- Principle adherence >90%

**Estimated Effort**: 4 weeks

---

## Phase 4 - VOS4 Integration (Month 5) **PLANNED**

### Plugin Mode Features

**Status**: Planned (architecture defined)
**Priority**: P1 (High)

**Features:**
- [ ] **VoiceAvenue Plugin**
  - Plugin manifest (YAML)
  - Theme YAML migration to VOS4 plugin directory
  - Speech recognition integration (VOS4 SpeechRecognitionManager)
  - Accessibility service reuse (VOS4 AccessibilityService)

- [ ] **Dual-Mode Support**
  - Standalone Android app (existing)
  - VOS4 plugin mode (new)
  - Shared KMP modules (100% code reuse)

**Acceptance Criteria:**
- AVA runs as VOS4 plugin (copy YAML file, ~1 hour setup)
- Theme consistent with VOS4 (MagicDreamTheme)
- Speech recognition works (VOS4 integration)
- No code duplication (KMP modules shared)

**Estimated Effort**: 2-3 weeks

---

## Phase 5 - Smart Glasses Ecosystem (Month 6-7) **PLANNED**

### Wearable Device Support

**Status**: Planned (requirements defined)
**Priority**: P2 (Medium)

**Features:**
- [ ] **8+ Device Support**
  - Meta Ray-Ban
  - Vuzix
  - RealWear
  - XReal
  - Rokid
  - Even Realities
  - (2 more TBD)

- [ ] **VisionOS UI**
  - Translucent panels
  - Glass morphism
  - Adaptive display optimization (per device)
  - Battery optimization (<10% per hour)

- [ ] **WebRTC Casting**
  - Ktor signaling server
  - Peer-to-peer video/audio
  - Screen sharing to glasses

**Acceptance Criteria:**
- AVA runs on 8+ smart glasses devices
- Battery usage <10% per hour active use
- VisionOS UI matches design spec
- Casting works reliably (<2 second latency)

**Estimated Effort**: 6-8 weeks

---

## Phase 6 - Enterprise Features (Month 8-9) **PLANNED**

### Multi-Tenant & Cloud Features

**Status**: Planned (architecture defined)
**Priority**: P2 (Medium)

**Features:**
- [ ] **Multi-Tenant Web App**
  - Ktor backend API
  - React/Vue frontend
  - Supabase integration (RLS, multi-tenant)
  - Subscription management (Stripe)
  - Usage analytics (storage, LLM tokens)

- [ ] **Cloud Sync**
  - RAG dataset sync (Supabase)
  - JSON-based data transfer
  - 1 GB per tenant limit
  - Self-hosted Supabase option

- [ ] **Subscription Tiers**
  - Free: Local-only
  - Pro: 10 GB cloud storage ($9.99/month)
  - Enterprise: Unlimited + cloud LLM ($49.99/month + usage)

**Acceptance Criteria:**
- User subscribes on web → unlocks cloud features on mobile
- RAG datasets sync across devices (<5 minutes for 1 GB)
- Multi-tenant isolation (tenant_123 cannot access tenant_456 data)
- Self-hosted deployment works (Docker Compose)

**Estimated Effort**: 8-10 weeks

---

## Future Ideas (Phase 7+) **BRAINSTORMING**

### Potential Features (Not Yet Planned)

**Low Priority / Future Exploration:**

- [ ] **Voice Cloning**
  - TTS with user's voice
  - Privacy: On-device voice model
  - Requires 5+ minutes of training audio

- [ ] **Multi-Modal RAG**
  - Image search (CLIP embeddings)
  - Video transcription + search
  - Audio transcription + search

- [ ] **Workflow Automation**
  - PDF → guided step-by-step instructions
  - Manual creation (screenshot + text annotations)
  - Workflow sharing (export/import)

- [ ] **Enterprise API**
  - RESTful API for programmatic access
  - Webhooks (trigger on events)
  - Batch processing (1000s of documents)

- [ ] **Desktop App**
  - macOS native app (Compose Multiplatform)
  - Windows native app (Compose Multiplatform)
  - Linux support (AppImage)

- [ ] **Browser Extension**
  - Summarize web pages
  - Extract structured data
  - Auto-fill forms with AVA context

**These ideas need:**
- User research (demand validation)
- Spec creation (requirements definition)
- Priority assessment (vs other features)

---

## Backlog Management

### Prioritization Criteria

**P0 (Critical):**
- Required for MVP (Phase 1.0)
- Blocks other features
- User-facing core functionality

**P1 (High):**
- Enhances MVP
- Planned for Phase 1.1-2
- High user value

**P2 (Medium):**
- Nice-to-have
- Planned for Phase 3+
- Moderate user value

**P3 (Low):**
- Future exploration
- Brainstorming stage
- Low immediate value

### Feature Request Process

1. **Submit Idea** - Add to "Future Ideas" section
2. **User Research** - Validate demand
3. **Spec Creation** - Use `/idea.specify`
4. **Priority Assessment** - Assign P0-P3
5. **Phase Assignment** - Add to appropriate phase
6. **Implementation** - Follow IDEACODE workflow

---

## References

- **Phase Status**: `.ideacode/PROJECT_PHASES_STATUS_UPDATED.md`
- **Constitution**: `.ideacode/memory/principles.md`
- **Architecture**: `ARCHITECTURE.md`
- **Living Docs**: `docs/ProjectInstructions/`

---

**Note**: This is a living document. Add new ideas as they arise. Reprioritize based on user feedback and business goals. Update phase assignments as development progresses.

### Multi-Platform Architecture ✅ COMPLETE (2025-11-26)
- [x] Rename: apps/ava-standalone → apps/ava-app-android
- [x] Update settings.gradle for multi-platform structure
- [x] File extension rename: .aon → .aot (semantic ontology)
- [x] Swarm-based code migration (4 parallel agents)
- [x] 149 changes across 12 files (code + docs + tests)
- [x] Internal metadata corrections in all .aot files

### P0 Handler Implementation ✅ COMPLETE (2025-11-27 Week 1)
- [x] Feature gap analysis (28 AON 3.0 intents documented)
- [x] Entity extractors (5 extractors with 42 unit tests)
- [x] SearchWebActionHandler - "search for X", "google Y"
- [x] NavigateURLActionHandler - "go to youtube.com"
- [x] SendTextActionHandler - "text mom saying hello"
- [x] MakeCallActionHandler - "call dad", "dial 555-1234"
- [x] Coverage improvement: 12/28 → 16/28 intents (43% → 57%)

### P1 Handler Implementation ✅ COMPLETE (2025-11-27 Week 2)
- [x] SendEmailActionHandler - "email alice@example.com about meeting"
- [x] GetDirectionsActionHandler - "directions to work"
- [x] FindNearbyActionHandler - "find coffee near me"
- [x] PlayVideoActionHandler - "play video cats on youtube"
- [x] Coverage improvement: 16/28 → 20/28 intents (57% → 71%)

### P2 Handler Implementation ✅ COMPLETE (2025-11-27 Week 3)
- [x] CreateReminderActionHandler - "remind me to buy milk"
- [x] CreateCalendarEventActionHandler - "schedule meeting with John"
- [x] AddTodoActionHandler - "add to do buy groceries"
- [x] CreateNoteActionHandler - "take a note meeting summary"
- [x] SetTimerActionHandler - "set timer for 10 minutes"
- [x] ResumeMusicActionHandler - "resume music", "continue playing"
- [x] Coverage improvement: 20/28 → 26/28 intents (71% → 93%)

#### Category Breakdown (After P2)
- **Communication**: 3/3 (100%) ✅ COMPLETE
- **Device Control**: 6/8 (75%)
- **Media**: 6/6 (100%) ✅ COMPLETE
- **Navigation**: 2/5 (40%)
- **Productivity**: 5/6 (83%)

**Remaining Intents (2/28 - 7% missing)**:
- control_lights (P3 - smart home integration required)
- check_calendar (P3 - calendar query functionality)
- Plus 3 navigation intents (show_traffic, share_location, save_location)

**Next Steps**: P3 handlers (Week 4) to reach 96% coverage (27/28 intents)

### P3 Handler Implementation ✅ COMPLETE (2025-11-27 Week 4)
- [x] CheckCalendarActionHandler - "check calendar", "what's on my calendar"
- [x] ShowTrafficActionHandler - "show traffic", "how is traffic"
- [x] ShareLocationActionHandler - "share my location", "send my location"
- [x] SaveLocationActionHandler - "save location", "bookmark this place"
- [x] Coverage improvement: 26/28 → 27/28 intents (93% → 96%)

#### Category Breakdown (After P3)
- **Communication**: 3/3 (100%) ✅ COMPLETE
- **Media**: 6/6 (100%) ✅ COMPLETE
- **Productivity**: 6/6 (100%) ✅ COMPLETE
- **Navigation**: 5/5 (100%) ✅ COMPLETE
- **Device Control**: 6/8 (75%)

**Remaining Intent (1/28 - 4% deferred)**:
- control_lights (OPTIONAL - smart home integration not needed at this time)

**Achievement**: 4 out of 5 categories at 100% completion! 🎉

**Status**: ✅ **PRODUCTION COMPLETE** - 96% coverage meets all current requirements
- All essential user features implemented
- Smart home integration deferred to future release (if needed)

### Documentation Manuals ✅ COMPLETE (2025-11-27)
- [x] **Developer Manual Chapter 49** - Action Handlers Implementation Guide
  - Complete architecture overview with flow diagrams
  - Implementation guide for creating new handlers
  - Handler registry documentation
  - All 5 entity extractors documented with code examples
  - All 27 implemented handlers organized by category
  - Testing strategies with FeatureGapAnalysisTest
  - Best practices (pattern matching, fallbacks, logging)
  - Common Android Intent patterns
  - Troubleshooting guide
  - Location: `/docs/Developer-Manual-Chapter49-Action-Handlers.md`

- [x] **User Manual Chapter 11** - Voice Commands User Guide
  - User-facing guide for all 27 voice commands
  - Commands organized by category (Communication, Device Control, Media, Navigation, Productivity)
  - Natural language examples for each command
  - Tips for effective voice command usage
  - Troubleshooting common issues
  - Quick reference guide
  - Location: `/docs/User-Manual-Chapter11-Voice-Commands.md`

**Documentation Status**:
- Developer resources: Complete implementation guide available
- End-user resources: Complete voice command reference available
- Both manuals integrated with existing documentation structure
