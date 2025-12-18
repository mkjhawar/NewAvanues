# Specification Review Checklist

**Date:** 2025-11-27
**Session:** RAG System Development
**Status:** Ready for Review

---

## Documents Created

### 1. ADRs (Architecture Decision Records)

#### ✅ ADR-010: External Storage Migration to .AVAVoiceAvanues
**File:** `docs/architecture/android/ADR-010-External-Storage-AVAVoiceAvanues.md`

**Key Decisions:**
- Folder name: `/sdcard/.AVAVoiceAvanues` (hidden by default)
- Automatic migration from legacy folders (ava-ai-models, .ava-ai-models)
- Preserve hidden/visible state during migration
- Subfolders always hidden: `.embeddings`, `.llm`, `.wakeword`

**Status:** Accepted ✅
**Implementation:** 70% complete (migration logic done)

---

#### ✅ ADR-011: 3-Letter JSON Schema Standard
**File:** `docs/architecture/shared/ADR-011-3Letter-JSON-Schema-Standard.md`

**Key Decisions:**
- 3-letter abbreviations for all JSON keys (sch, ver, met, cfg)
- ~50% size reduction vs verbose JSON
- Human-readable + compact
- Ecosystem-wide standard (AVA, VoiceOS, AVAConnect, Avanues)
- 30+ global keys defined
- 4 schema types: ava-llm-1.0, ava-emb-1.0, ava-aot-3.0, ava-rag-1.0

**Status:** Accepted ✅
**Enforcement:** MANDATORY for all new JSON files

---

#### ✅ ADR-012: RAG System Architecture
**File:** `docs/architecture/android/ADR-012-RAG-System-Architecture.md`

**Key Decisions:**
- MiniLM E5-small-multilingual for embeddings (384-dim, 30MB)
- Faiss IndexFlatIP for vector indexing (<100ms search)
- 512-token chunks with 128-token overlap
- Privacy-first (100% on-device processing)
- <2s end-to-end query time
- Hybrid storage (English in APK, multilingual external)

**Components:**
1. Document Ingestion (PDF, Web, Images)
2. Text Chunking (semantic, overlap)
3. Embedding Generation (MiniLM ONNX)
4. Vector Indexing (Faiss)
5. RAG Query Pipeline (retrieve + generate)

**Status:** Accepted ✅
**Implementation:** 0% (ready to start)

---

### 2. Specifications

#### ✅ RAG System Specification v1.0
**File:** `docs/specifications/rag-system-spec.md`

**Scope:** Complete RAG system (1200+ lines)

**Features:**
- Document Q&A (PDF, web, images)
- Knowledge bases (medical, legal, technical)
- Conversation memory
- Multilingual support (100+ languages)
- Browser search memory (future backlog)

**Requirements:** 8 functional, 5 non-functional
**Components:** 7 detailed specifications
**Data Models:** 3-letter JSON schemas, SQLDelight tables
**Testing:** Unit, integration, performance tests
**Deployment:** 5 phases over 3 weeks

**Status:** Complete ✅

---

### 3. Standards & Guidelines

#### ✅ AVA 3-Letter JSON Schema Standard
**File:** `docs/standards/AVA-3LETTER-JSON-SCHEMA.md`

**Contents:**
- Global key registry (30+ keys)
- 4 schema types with examples
- Validation rules
- Code examples (Kotlin, JSON Schema)
- Migration guide
- Enforcement policy

**Status:** Production ready ✅

---

#### ✅ External Storage Setup Guide
**File:** `docs/build/EXTERNAL-STORAGE-SETUP.md` (UPDATED)

**Updates:**
- New folder structure (.AVAVoiceAvanues)
- Migration instructions
- RAG model paths added
- Deployment commands updated

**Status:** Updated ✅

---

#### ✅ Folder Structure Documentation
**File:** `ava-ai-models-external/FOLDER-STRUCTURE.txt` (UPDATED)

**Updates:**
- New paths with .AVAVoiceAvanues
- Migration commands
- RAG embedding models
- .ALM proprietary format documented

**Status:** Updated ✅

---

### 4. Implementation Code

#### ✅ ExternalStorageMigration.kt
**File:** `Universal/AVA/Core/Common/.../ExternalStorageMigration.kt`

**Functions:**
- `migrateIfNeeded()` - Detect and migrate legacy folders
- `getExternalStorageFolder()` - Get active folder
- `getEmbeddingsFolder()` - Get embeddings subfolder
- `getLLMFolder()` - Get LLM subfolder
- `getWakeWordFolder()` - Get wake word subfolder

**Status:** Implemented ✅

---

## Review Checklist

### Infrastructure

- [x] Storage migration logic implemented
- [x] 3-letter JSON schema defined
- [x] Folder structure documented
- [ ] Developer Manual updated (pending)
- [ ] User Manual updated (pending)

### ADRs

- [x] ADR-010 created (External Storage)
- [x] ADR-011 created (3-Letter Schema)
- [x] ADR-012 created (RAG Architecture)
- [ ] ADRs referenced in Developer Manual (pending)

### Specifications

- [x] RAG system spec complete
- [x] 3-letter schema standard complete
- [x] All requirements documented
- [x] All components specified
- [x] Testing strategy defined
- [x] Deployment plan created

### Decisions to Validate

#### Storage Migration
- [ ] Folder name acceptable? (.AVAVoiceAvanues)
- [ ] Migration strategy safe?
- [ ] Hidden folders OK for user experience?

#### 3-Letter Schema
- [ ] Key names intuitive? (sch, ver, met, cfg)
- [ ] 30+ keys sufficient?
- [ ] Schema types cover all use cases?

#### RAG Architecture
- [ ] MiniLM model choice correct?
- [ ] Faiss indexing appropriate?
- [ ] Performance targets achievable?
- [ ] Storage estimates reasonable?
- [ ] Privacy requirements met?

---

## Questions for Review

### High Priority

1. **Folder Naming:** Is `.AVAVoiceAvanues` the right name?
   - Alternatives: `.AVA`, `.VoiceAvanues`, `.ava-voice-avanues`
   - Current rationale: Ecosystem branding

2. **3-Letter Keys:** Are key abbreviations clear enough?
   - Example: `sch` (schema), `ver` (version), `met` (metadata)
   - Concern: Learning curve for developers
   - Mitigation: Documentation + IDE autocomplete

3. **RAG Scope:** Is complete 5-component system the right MVP?
   - Alternative: Start with PDF-only, no UI
   - Current rationale: Complete system provides more value

### Medium Priority

4. **APK Size:** Acceptable +20-25MB increase for RAG?
   - Components: PDFBox (5MB) + Tesseract (10MB) + Faiss (3MB) + MiniLM (20MB in APK)
   - Mitigation: Only English model bundled, multilingual external

5. **Storage Limits:** Is 10GB reasonable for RAG?
   - Estimate: ~50MB per 100-page PDF
   - Capacity: 200 PDFs
   - User control: Can delete documents

### Low Priority

6. **Migration Timing:** When to run migration?
   - Option A: On app startup (adds ~100ms)
   - Option B: Lazy on first model access
   - Current: On startup (safer)

---

## Next Steps

### If Approved:

1. **Update Manuals**
   - Developer Manual Chapter 50: External Storage Migration
   - Developer Manual Chapter 51: 3-Letter JSON Schema
   - Developer Manual Chapter 52: RAG System Architecture
   - User Manual Chapter 12: Using RAG Features

2. **Create Implementation Plan**
   - Break RAG spec into detailed tasks
   - Assign time estimates
   - Identify dependencies
   - Create sprint plan

3. **Begin Implementation**
   - Phase 1: Foundation (Week 1)
   - Phase 2: Embeddings & Indexing (Week 2)
   - Phase 3: RAG Pipeline & UI (Week 3)

### If Changes Needed:

1. Update specifications based on feedback
2. Revise ADRs as needed
3. Re-review before implementation

---

## Commits Summary

**Commit 1 (90a77ddf):** Storage migration + 3-letter schema
- ExternalStorageMigration.kt (NEW)
- AVA-3LETTER-JSON-SCHEMA.md (NEW)
- FOLDER-STRUCTURE.txt (UPDATED)

**Commit 2 (4e3e0699):** RAG system specification
- rag-system-spec.md (NEW - 1200 lines)

**Commit 3 (81616c57):** ADRs for RAG infrastructure
- ADR-010-External-Storage-AVAVoiceAvanues.md (NEW)
- ADR-011-3Letter-JSON-Schema-Standard.md (NEW)
- ADR-012-RAG-System-Architecture.md (NEW)

**Total:** 3 commits, 8 new files, 4000+ lines of documentation

---

## Review Status

- [ ] User reviewed all specifications
- [ ] Questions answered
- [ ] Changes incorporated
- [ ] Approved for implementation

**Reviewer:** _______________
**Date:** _______________
**Approved:** [ ] Yes [ ] No [ ] Changes Needed
