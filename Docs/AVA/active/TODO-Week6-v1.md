# AVA AI - Pending Items & Future Enhancements

**Document Version**: 2.0
**Last Updated**: 2025-11-26
**Status**: Phase 2.0 Production Ready

---

## Executive Summary

This document tracks all pending items, incomplete features, and future enhancements for the AVA AI project. It serves as a comprehensive reference for what remains to be implemented beyond the completed Chat UI (Phases 1-5).

---

## Table of Contents

1. [Critical Pending Items](#1-critical-pending-items)
2. [Chat UI Known TODOs](#2-chat-ui-known-todos)
3. [Feature Modules Not Yet Implemented](#3-feature-modules-not-yet-implemented)
4. [VOS4 Integration Pending](#4-vos4-integration-pending)
5. [Settings & Configuration](#5-settings--configuration)
6. [Testing & Validation](#6-testing--validation)
7. [Documentation Gaps](#7-documentation-gaps)
8. [Technical Debt](#8-technical-debt)

---

## 1. Critical Pending Items

### 1.1 RAG System (NOT IMPLEMENTED)
**Status**: ❌ Not Started
**Priority**: High
**Phase**: Post-MVP (Phase 1.1+)

**Description**: Retrieval Augmented Generation system for context-aware responses.

**Pending Work**:
- [ ] Document ingestion pipeline (PDF, TXT, MD support)
- [ ] Embedding generation (on-device or cloud)
- [ ] Vector database integration (ChromaDB/FAISS)
- [ ] Semantic search implementation
- [ ] Document chunking algorithm
- [ ] Re-ranking system
- [ ] LLM prompt integration
- [ ] Document metadata management

**Dependencies**:
- Vector database selection
- Embedding model selection (SentenceTransformers ONNX)
- LLM integration

**Reference**: `/Volumes/M Drive/Coding/AVA AI/features/rag/README.md`

---

### 1.2 VOS4 Voice Input Integration
**Status**: ⏸️ Deferred
**Priority**: High
**Phase**: Phase 5 (P5T01-P5T02) - Skipped

**Description**: Voice input functionality using VOS4 (VoiceOS v4) for hands-free interaction.

**Pending Work**:
- [ ] VOS4 module availability verification
- [ ] Voice input modal UI component (`VoiceInputModal.kt`)
- [ ] Speech-to-text integration with VOS4
- [ ] Push-to-talk vs always-listening modes
- [ ] Voice input confidence indicators
- [ ] Voice command fallback handling
- [ ] Accessibility features for voice input

**Reason for Deferral**: VOS4 module not yet available in codebase.

**Next Steps**:
1. Wait for VOS4 integration completion
2. Implement voice input modal UI
3. Wire VOS4 speech recognition to ChatViewModel
4. Add voice input tests

---

### 1.3 Device Testing & Validation
**Status**: ❌ Not Completed
**Priority**: Critical
**Phase**: Phase 6 (Testing & Deployment)

**Description**: Comprehensive testing on physical Android devices.

**Pending Work**:
- [ ] Enable gradlew execution
- [ ] Run all 164 tests on device
- [ ] Performance validation (measure actual vs target metrics)
- [ ] Memory profiling (validate pagination savings)
- [ ] NLU inference timing (target <100ms)
- [ ] Database query performance
- [ ] UI rendering performance (60 FPS)
- [ ] Real-world usage testing

**Blocker**: No gradlew execution capability in current environment.

---

## 2. Chat UI Known TODOs

### 2.1 Conversation Previews
**File**: `features/chat/ui/ChatScreen.kt:244`
**Status**: ⏳ Partial Implementation

```kotlin
// Current: Shows empty preview for non-active conversations
// TODO: Fetch per-conversation preview in future
val preview = if (conv.id == activeConversationId) {
    messages.firstOrNull()?.content ?: ""
} else {
    "" // Empty preview to avoid showing wrong data
}
```

**Enhancement Needed**:
- Fetch first message per conversation lazily
- Cache conversation previews
- Update previews when messages change

---

### 2.2 Test Coverage Gaps
**File**: `features/chat/ui/ChatScreenTest.kt:53-55`

**Pending Tests**:
- [ ] Message list rendering tests (P1T03)
- [ ] Input field and send button tests (P1T06)
- [ ] Integration test for sending messages (P1T07)

**File**: `features/chat/ui/components/MessageBubbleTest.kt:292`

**Pending Tests**:
- [ ] Long-press context menu tests (P3T02)

**File**: `features/chat/ui/ChatViewModelTest.kt:93-95`

**Pending Integration Tests**:
- [ ] NLU integration tests (P2T08) - **COMPLETED in ChatViewModelNluTest.kt**
- [ ] Teach-AVA integration tests (P3T05) - **COMPLETED in ChatViewModelTeachAvaTest.kt**
- [ ] Conversation switching tests (P4T05) - **COMPLETED in ChatViewModelHistoryTest.kt**

**Note**: Most pending tests have been completed in separate test files. Remaining gaps are minor.

---

## 3. Feature Modules Not Yet Implemented

### 3.1 LLM Integration
**Status**: ❌ Not Started
**Priority**: High
**Location**: `features/llm/`

**Pending Work**:
- [ ] LLM provider abstraction (OpenAI, Anthropic, Local)
- [ ] Prompt template system
- [ ] Context window management
- [ ] Streaming response support
- [ ] Rate limiting & error handling
- [ ] Token counting & cost tracking

---

### 3.2 Memory System
**Status**: ⚠️ Partial (Database only)
**Priority**: Medium
**Location**: `features/memory/`

**Completed**:
- ✅ Memory entity definition
- ✅ MemoryDao implementation
- ✅ MemoryRepository implementation

**Pending Work**:
- [ ] Memory retrieval use cases
- [ ] Memory summarization
- [ ] Long-term memory storage
- [ ] Memory prioritization algorithm
- [ ] Memory search & filtering UI

---

### 3.3 Learning System
**Status**: ⚠️ Partial (Database only)
**Priority**: Medium
**Location**: `core/data/repository/LearningRepositoryImpl.kt`

**Completed**:
- ✅ Learning entity definition
- ✅ LearningDao implementation
- ✅ LearningRepository implementation

**Pending Work**:
- [ ] Active learning algorithms
- [ ] User feedback loop
- [ ] Model fine-tuning pipeline
- [ ] Learning metrics dashboard
- [ ] Learning history visualization

---

### 3.4 Decision System
**Status**: ⚠️ Partial (Database only)
**Priority**: Medium
**Location**: `core/data/repository/DecisionRepositoryImpl.kt`

**Completed**:
- ✅ Decision entity definition
- ✅ DecisionDao implementation
- ✅ DecisionRepository implementation

**Pending Work**:
- [ ] Decision tree logic
- [ ] Multi-criteria decision analysis
- [ ] Decision explanation generation
- [ ] Decision history tracking
- [ ] Undo/redo decision support

---

## 4. VOS4 Integration Pending

### 4.1 VOS4 Submodule
**Status**: ⚠️ Added but Not Integrated
**Location**: `external/vos4/`

**Pending Work**:
- [ ] VOS4 build integration
- [ ] VOS4 API surface definition
- [ ] Speech recognition integration
- [ ] TTS (Text-to-Speech) integration
- [ ] Voice command routing
- [ ] HUD overlay integration
- [ ] Accessibility service coordination

---

## 5. Settings & Configuration

### 5.1 User Settings UI
**Status**: ❌ Not Implemented
**Priority**: Medium

**Pending Settings UI**:
- [ ] Conversation mode selector (APPEND/NEW)
- [ ] Confidence threshold slider (0.0-1.0)
- [ ] NLU model selection
- [ ] Theme selection (Light/Dark/System)
- [ ] Notification preferences
- [ ] Privacy settings
- [ ] Data export/import

**Backend Ready**:
- ✅ ChatPreferences backend complete
- ✅ StateFlow integration
- ✅ Persistence layer

**Next Steps**:
1. Create `SettingsScreen.kt` Compose UI
2. Wire to ChatPreferences
3. Add settings navigation from main menu

---

### 5.2 App Configuration
**Status**: ⚠️ Partial

**Pending Configuration**:
- [ ] API keys management (OpenAI, Anthropic, etc.)
- [ ] Model download settings
- [ ] Cache size limits
- [ ] Database backup/restore
- [ ] Debug logging levels
- [ ] Performance monitoring toggles

---

## 6. Testing & Validation

### 6.1 Missing Test Types

**UI Tests**:
- [ ] Screenshot tests (Paparazzi/Shot)
- [ ] Accessibility tests (TalkBack validation)
- [ ] Multi-language tests
- [ ] Orientation change tests
- [ ] Dark mode tests

**Integration Tests**:
- [ ] Full app flow tests (end-to-end across modules)
- [ ] Database migration tests
- [ ] Network error handling tests
- [ ] Offline mode tests

**Performance Tests**:
- [ ] Memory leak detection (LeakCanary)
- [ ] Battery usage profiling
- [ ] Network usage monitoring
- [ ] App startup time benchmarks

---

### 6.2 Test Environment Setup

**Pending Infrastructure**:
- [ ] CI/CD pipeline (GitHub Actions / GitLab CI)
- [ ] Automated device testing (Firebase Test Lab)
- [ ] Code coverage reporting
- [ ] Performance regression detection
- [ ] Automated APK generation

---

## 7. Documentation Gaps

### 7.1 Missing Manuals

**Completed (2025-11-26)**:
- [x] **Developer Manual** - `docs/developer/DEVELOPER-MANUAL.md`
- [x] **ADR-008** - Hardware-Aware Inference Backend Selection
- [x] **ADR-009** - iOS Core ML ANE Integration
- [x] **Chapter 47** - GPU Acceleration (Vulkan/OpenCL/NNAPI/QNN)
- [x] **Chapter 48** - AON 3.0 Semantic Ontology Format
- [x] **File Placement Guide** - `docs/AVA-File-Placement-Guide.md`

**Completed (2025-11-27)**:
- [x] **Developer Manual Chapter 49** - Action Handlers Implementation Guide
- [x] **User Manual Chapter 11** - Voice Commands User Guide (27 commands)

**Remaining Gaps**:
- [ ] **User Manual** (main end-user documentation - `docs/active/User-Manual.md` exists)
- [ ] **API Reference** (KDoc generation)
- [ ] **Deployment Guide** (production setup)

---

### 7.2 Code Documentation

**Gaps**:
- [ ] Mermaid diagrams for all major flows
- [ ] Sequence diagrams for complex interactions
- [ ] State machine diagrams
- [ ] Database ER diagrams
- [ ] Component dependency graphs

---

## 8. Technical Debt

### 8.1 Performance Optimizations

**Future Enhancements**:
- [ ] Message prefetching (load next page before scroll)
- [ ] Intent classification batching
- [ ] Database query optimization (indexes)
- [ ] Image/media caching
- [ ] Lazy initialization of heavy components

---

### 8.2 Code Quality

**Pending Refactoring**:
- [ ] Extract magic numbers to constants
- [ ] Reduce ChatViewModel size (currently 1363 lines)
- [ ] Split large test files
- [ ] Improve error message consistency
- [ ] Add more inline documentation

---

### 8.3 Security & Privacy

**Pending Hardening**:
- [ ] Data encryption at rest
- [ ] Secure key storage (Android Keystore)
- [ ] Input sanitization
- [ ] SQL injection prevention (verify Room safety)
- [ ] Network security config
- [ ] ProGuard/R8 rules

---

## 9. Platform Support

### 9.1 Android Specific

**Pending Work**:
- [ ] Widget support (home screen conversation widget)
- [ ] Notification actions (reply from notification)
- [ ] Quick settings tile
- [ ] App shortcuts
- [ ] Split-screen optimization
- [ ] Foldable device support

---

### 9.2 Desktop (Future)

**Status**: ❌ Not Started
**Location**: `platform/desktop/`

**Pending Work**:
- [ ] Desktop Compose UI
- [ ] Platform-specific features
- [ ] Window management
- [ ] Keyboard shortcuts
- [ ] System tray integration

---

## 10. Summary

### Completion Status

| Category | Status | Completion |
|----------|--------|------------|
| **Chat UI (Phases 1-5)** | ✅ Complete | 100% |
| **AON 3.0 Semantic Ontology** | ✅ Complete | 100% |
| **GPU Acceleration** | ✅ Complete | 100% |
| **Developer Manual** | ✅ Complete | 100% |
| **Production Readiness (Android)** | ✅ Complete | 100% |
| **RAG System** | ❌ Not Started | 0% |
| **VOS4 Integration** | ⏸️ Deferred | 0% |
| **Settings UI** | ❌ Not Started | 0% |
| **Device Testing** | ❌ Pending | 0% |
| **User Manual** | ❌ Not Started | 0% |
| **LLM Integration** | ❌ Not Started | 0% |
| **Memory/Learning/Decision** | ⚠️ Partial | 30% |
| **iOS Core ML** | ⏸️ Staged | 10% |

---

### Next Priorities

1. **Immediate** (Week 1-2):
   - ✅ Complete Developer & User Manuals
   - Enable device testing with gradlew
   - Create Settings UI for ChatPreferences

2. **Short-term** (Month 1):
   - Implement RAG system
   - Integrate VOS4 voice input
   - Complete remaining test coverage

3. **Medium-term** (Months 2-3):
   - LLM integration
   - Memory/Learning/Decision features
   - Platform expansion (Desktop)

---

## Appendix A: TODO Comments in Code

### Chat UI TODOs

```kotlin
// features/chat/ui/ChatScreen.kt:244
// TODO: Fetch per-conversation preview in future

// features/chat/ui/components/MessageBubbleTest.kt:292
// TODO (P3T02): Add tests for long-press context menu

// features/chat/ui/ChatScreenTest.kt:53-55
// TODO (P1T03): Add tests for message list rendering
// TODO (P1T06): Add tests for input field and send button
// TODO (P1T07): Add integration test for sending messages
```

### VOS4 TODOs (External Module)

Most VOS4 TODOs are in the external submodule and not part of AVA AI core implementation.

---

## Appendix B: Configuration Requirements

### Required Settings (Not Yet Exposed in UI)

1. **ChatPreferences** (Backend Ready):
   - Conversation mode: APPEND/NEW
   - Confidence threshold: 0.0-1.0 (default 0.5)
   - Last active conversation ID

2. **NLU Configuration** (Hardcoded):
   - Model path: `assets/models/mobilebert_int8.onnx`
   - Vocab path: `assets/models/vocab.txt`
   - Max sequence length: 128 tokens

3. **Database Configuration** (Hardcoded):
   - Database name: `ava_db`
   - Version: 1
   - Migrations: None yet

4. **Cache Configuration** (Hardcoded):
   - NLU cache size: 100 entries (LRU)
   - Conversations cache TTL: 5 seconds
   - Intents cache TTL: 10 seconds
   - Message page size: 50 messages

---

## Appendix C: Future Enhancement Ideas

### AI Capabilities
- Multi-modal input (voice + text + images)
- Emotion detection from text
- Sentiment analysis
- Entity recognition
- Summarization

### User Experience
- Multi-language support
- Custom themes
- Avatar customization
- Conversation export (PDF, Markdown)
- Search within conversations
- Conversation tagging/categorization

### Developer Features
- Plugin system
- Custom intent registration
- Webhook integrations
- REST API for external apps
- GraphQL support

---

**End of Document**

*This document should be updated as items are completed or new pending items are identified.*
