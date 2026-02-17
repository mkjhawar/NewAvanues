# NoteAvanue Analysis: Voice-First Note Architecture

**Module:** `Modules/NoteAvanue/`
**Package:** `com.augmentalis.noteavanue`
**Date:** 2026-02-17
**Branch:** `Cockpit-Development`
**Status:** Research Complete, Implementation Pending

---

## 1. Executive Summary

This analysis consolidates four parallel research tracks investigating the optimal architecture for NoteAvanue, the voice-first rich note editor in the Avanues ecosystem. The research covers competitive feature analysis (Apple Notes iOS 18), architectural patterns (block vs document vs hybrid), voice-to-structured-note pipeline design, and Markdown/MMD format evaluation for on-device RAG indexing.

**Key Recommendations:**
- **Architecture:** Hybrid Document-Block (edit as document, store with block metadata, export as Markdown)
- **Editor Engine:** `compose-rich-editor` by MohamedRejeb (KMP-compatible, RC13)
- **Storage Format:** YAML-frontmatter GFM superset (Obsidian-compatible) for RAG indexing
- **Voice Model:** Mode-gated (DICTATING vs COMMANDING) with on-device format detection
- **RAG Integration:** Heading-aware chunking with `all-MiniLM-L6-v2` ONNX (existing `Modules/AI/RAG/`)

---

## 2. Current State Assessment

### 2.1 Existing NoteAvanue Module

The module exists at `Modules/NoteAvanue/` with minimal implementation:

| File | Source Set | Status |
|------|-----------|--------|
| `model/Note.kt` | commonMain | Basic model: plain `String` content, no rich text |
| `NoteEditor.kt` | androidMain | `BasicTextField` — Bold/Italic buttons are no-ops |
| `build.gradle.kts` | module | Missing JB Compose plugin (`compose`), AvanueUI only in androidMain |

**Gaps identified:**
- No rich text formatting (bold, italic, headings, lists)
- No standalone `NoteAvanueScreen` (violates self-running module pattern)
- No voice command integration
- No Markdown import/export
- No RAG indexing pipeline
- No Cockpit embeddable content view
- `build.gradle.kts` missing `alias(libs.plugins.compose)` (same issue PhotoAvanue had)
- AvanueUI dependency in androidMain only (should be commonMain for KMP Compose)

### 2.2 Existing RAG Module

`Modules/AI/RAG/` provides a comprehensive on-device retrieval pipeline:

| Component | Location | Purpose |
|-----------|----------|---------|
| `TextChunker` | commonMain | Heading-aware, overlapping token chunks |
| `BM25Scorer` | commonMain | Term-frequency relevance scoring |
| `ReciprocalRankFusion` | commonMain | Hybrid BM25 + vector merge |
| `SimpleTokenizer` | commonMain | Token counting for chunk boundaries |
| `EmbeddingProvider` | commonMain interface | Platform-agnostic embedding contract |
| `ONNXEmbeddingProvider` | androidMain | all-MiniLM-L6-v2 (384-dim, int8, ~23MB) |
| `MarkdownParser` | androidMain | GFM parsing with YAML frontmatter extraction |
| `Quantization` | commonMain | int8/binary vector quantization |
| `QueryCache` | commonMain | LRU cache for repeated queries |

This RAG infrastructure can be directly leveraged for NoteAvanue search without building new embedding/search machinery.

---

## 3. Research Track 1: Apple Notes iOS 18 Feature Analysis

### 3.1 Feature Inventory

| Category | Key Features |
|----------|-------------|
| **Text Editing** | Bold, Italic, Underline, Strikethrough, 9 font sizes, 5 highlight colors, monospace code |
| **Structure** | H1/H2/H3 headings, collapsible sections (iOS 18), bulleted/numbered/dashed lists, checklists |
| **Tables** | Inline tables with add/remove rows/columns |
| **Attachments** | Inline photos, videos, audio recordings with live transcript, PDFs, document scans |
| **Organization** | Folders, Smart Folders (auto-filter), tags, pinning, locking (password/biometric) |
| **Collaboration** | Real-time shared notes, @mentions, activity history |
| **Quick Notes** | Floating window (macOS/iPadOS), App Clips for linked notes |
| **AI (iOS 18.1+)** | Writing Tools (proofread, rewrite, summarize), Image Wand (sketch-to-image), transcription |
| **Export** | PDF, Markdown (macOS Sequoia), Send as Copy |
| **Math** | Math Notes: handwritten equations solved in real-time, variable sliders |

### 3.2 Voice-Relevant Feature Gaps in Apple Notes

Apple Notes has NO voice command integration for formatting. All formatting is touch-only via toolbar. This creates a significant competitive opportunity:

| Apple Notes | NoteAvanue Advantage |
|------------|---------------------|
| Touch-only formatting | Voice: "heading one", "bold this", "new checklist" |
| No inline dictation formatting | On-device format detection during dictation |
| No Cockpit integration | Split-frame lecture/meeting/research workflows |
| No AVID on toolbar | VOS 4-tier: every button voice-addressable |
| No ambient dictation mode | Continuous mode with command keyword triggers |

### 3.3 Feature Prioritization for NoteAvanue

| Priority | Features | Rationale |
|----------|----------|-----------|
| P0 (MVP) | Rich text (B/I/U/S), H1-H3, lists, checklists, voice commands, Markdown export | Core differentiator |
| P1 | Attachments (photo/audio), dictation with format detection, RAG indexing | Voice-first workflows |
| P2 | Tables, collapsible sections, Smart Folders, collaboration | Feature parity with Apple Notes |
| P3 | Math notes, AI Writing Tools, Image Wand | Advanced features |

---

## 4. Research Track 2: Architecture Comparison

### 4.1 Architectures Evaluated

| Architecture | Model | Examples | Voice Score |
|-------------|-------|----------|-------------|
| **Block-Based** | Each paragraph = independent block object with type/content | Notion, Logseq | 7/10 |
| **Document-Based** | Single rich text document, contiguous text | Apple Notes, Google Docs | 6/10 |
| **File-Based** | Plain Markdown files on disk, external editor | Obsidian, iA Writer | 5/10 |
| **Hybrid Document-Block** | Edit as document, store with block metadata index | Bear, Craft | 9/10 |

### 4.2 Evaluation Criteria

| Criterion | Block | Document | File | Hybrid |
|-----------|-------|----------|------|--------|
| Voice command mapping | Good (block ops) | Medium (cursor-based) | Poor (text manipulation) | Excellent |
| compose-rich-editor fit | Poor (fights single-doc model) | Excellent | N/A | Excellent |
| SQLDelight storage | Complex (block table + ordering) | Simple (TEXT column) | N/A (filesystem) | Balanced |
| Markdown round-trip | Complex (reconstruct from blocks) | Simple (serialize document) | Native | Simple |
| RAG chunking | Natural (blocks = chunks) | Needs heading splitting | Natural (files) | Natural (block index) |
| Cockpit embedding | Good | Good | Poor | Good |
| Implementation complexity | High (block CRUD, reordering) | Low | Low | Medium |
| Offline performance | Good | Excellent | Excellent | Excellent |

### 4.3 Recommended Architecture: Hybrid Document-Block

**How it works:**

1. **Editing layer:** Single `RichTextState` from compose-rich-editor. User edits a continuous document. Voice commands map to `RichTextState.toggleSpanStyle()`, `toggleParagraphStyle()`, `toggleUnorderedList()`, etc.

2. **Storage layer:** When saving, serialize the document to Markdown. Also generate a block-metadata outline (heading positions, checklist positions, attachment anchors) stored in SQLDelight for fast navigation and RAG chunking.

3. **Export layer:** The Markdown string IS the canonical format. Block metadata is a derived index, not the source of truth.

```
User edits → RichTextState (document) → Save → Markdown (canonical)
                                              → BlockIndex (derived, SQLDelight)
                                              → RAG chunks (derived, embeddings)
```

### 4.4 SQLDelight Schema (Proposed)

```sql
-- Core note storage
CREATE TABLE NoteEntity (
    id TEXT NOT NULL PRIMARY KEY,
    title TEXT NOT NULL DEFAULT '',
    markdown_content TEXT NOT NULL DEFAULT '',
    folder_id TEXT,
    is_pinned INTEGER NOT NULL DEFAULT 0,
    is_locked INTEGER NOT NULL DEFAULT 0,
    word_count INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

-- YAML frontmatter metadata (denormalized for fast filtering)
CREATE TABLE NoteMetadata (
    note_id TEXT NOT NULL PRIMARY KEY REFERENCES NoteEntity(id),
    tags TEXT NOT NULL DEFAULT '',           -- comma-separated
    source TEXT NOT NULL DEFAULT 'manual',   -- manual|dictated|imported
    voice_origin_pct REAL NOT NULL DEFAULT 0.0,
    last_rag_indexed_at TEXT
);

-- Block outline index (derived from Markdown on save)
CREATE TABLE NoteBlock (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    note_id TEXT NOT NULL REFERENCES NoteEntity(id),
    block_type TEXT NOT NULL,     -- heading|paragraph|checklist|code|quote|attachment
    heading_level INTEGER,        -- 1-3 for headings, NULL otherwise
    start_offset INTEGER NOT NULL,
    end_offset INTEGER NOT NULL,
    summary TEXT NOT NULL DEFAULT '',  -- first 100 chars for navigation
    sort_order INTEGER NOT NULL
);

-- FTS5 full-text search
CREATE VIRTUAL TABLE NoteFts USING fts5(
    title, markdown_content, tags,
    content=NoteEntity,
    content_rowid=rowid
);

-- Attachments
CREATE TABLE NoteAttachmentEntity (
    id TEXT NOT NULL PRIMARY KEY,
    note_id TEXT NOT NULL REFERENCES NoteEntity(id),
    type TEXT NOT NULL,          -- photo|document|audio|sketch|video
    uri TEXT NOT NULL,
    name TEXT NOT NULL DEFAULT '',
    mime_type TEXT NOT NULL DEFAULT '',
    file_size_bytes INTEGER NOT NULL DEFAULT 0,
    block_offset INTEGER NOT NULL DEFAULT -1,
    caption TEXT NOT NULL DEFAULT '',
    thumbnail_uri TEXT
);

-- Folders
CREATE TABLE NoteFolder (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    parent_id TEXT,
    icon TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_smart INTEGER NOT NULL DEFAULT 0,
    smart_filter TEXT          -- JSON filter definition for smart folders
);
```

---

## 5. Research Track 3: Voice-to-Structured-Note Pipeline

### 5.1 Voice Mode Architecture

Mode-gated model (Pattern B) — cleanest disambiguation between dictation and commands:

```
┌─────────────────────────────────────────────────┐
│              NoteVoiceRouter                     │
│                                                  │
│  ┌──────────┐  mode?   ┌───────────────────┐   │
│  │ Speech   │─────────→│ COMMANDING        │   │
│  │ Input    │          │ → VOS pipeline     │   │
│  │          │          │ → CommandExecutor   │   │
│  │          │          └───────────────────┘   │
│  │          │                                    │
│  │          │─────────→│ DICTATING          │   │
│  │          │          │ → FormatDetector    │   │
│  │          │          │ → DictationManager  │   │
│  │          │          │ → RichTextState     │   │
│  └──────────┘          └───────────────────┘   │
│                                                  │
│  Mode Toggle: "command mode" / "dictation mode" │
│  Shortcut: double-tap mic button                │
└─────────────────────────────────────────────────┘
```

### 5.2 NoteVoiceMode Enum

```kotlin
enum class NoteVoiceMode {
    COMMANDING,   // All speech → VOS command pipeline
    DICTATING,    // All speech → text insertion with format detection
    CONTINUOUS    // Ambient: check command table first, fallback to dictation
}
```

- **Default:** COMMANDING (consistent with rest of VoiceOS)
- **Toggle command:** "dictation mode" / "command mode"
- **CONTINUOUS:** Advanced/power user mode — checks if speech matches a VOS command first, otherwise treats as dictation

### 5.3 NoteFormatDetector (On-Device, 0-15ms)

Detects formatting intent from dictated speech without requiring explicit commands:

| Trigger Pattern | Detected Format | Example Input | Output |
|----------------|----------------|---------------|--------|
| `title:` prefix | H1 heading | "title: Project Overview" | `# Project Overview` |
| `heading one:` | H1 heading | "heading one: Introduction" | `# Introduction` |
| `heading two:` | H2 heading | "heading two: Methods" | `## Methods` |
| `heading three:` | H3 heading | "heading three: Results" | `### Results` |
| `quote:` or `quote from X:` | Blockquote | "quote: To be or not to be" | `> To be or not to be` |
| `code:` or `code block:` | Code fence | "code: val x equals 5" | `` `val x = 5` `` |
| `note to self:` | Callout/aside | "note to self: review this later" | `> [!note] review this later` |
| `todo:` or `task:` | Checklist item | "todo: buy groceries" | `- [ ] buy groceries` |
| Ordinal starters (1., 2., first, second) | Numbered list | "first, gather requirements" | `1. Gather requirements` |
| Bullet starters (dash, bullet) | Bulleted list | "bullet: important point" | `- important point` |
| No trigger detected | Plain paragraph | "The weather is nice today" | `The weather is nice today` |

**Implementation:** FSM (Finite State Machine) pattern matching — no ML model needed. Runs synchronously on the speech result string.

### 5.4 NoteCommandExecutor (48 VOS Commands)

Commands with `note_` prefix, category `NOTE`:

| Group | Commands | Examples |
|-------|----------|---------|
| **Formatting (6)** | bold, italic, underline, strikethrough, highlight, clear format | "bold this", "clear formatting" |
| **Blocks (10)** | heading 1/2/3, bullet list, numbered list, checklist, quote, code block, divider, table | "heading one", "new checklist" |
| **Checklists (3)** | check item, uncheck item, toggle check | "check item", "toggle check" |
| **Navigation (4)** | go to top, go to bottom, next heading, previous heading | "go to top", "next heading" |
| **Editing (6)** | undo, redo, select all, delete line, duplicate line, new paragraph | "undo", "new paragraph" |
| **Attachments (3)** | attach file, take photo, record audio | "attach file", "record audio" |
| **Organization (3)** | add tag, pin note, move to folder | "add tag meeting", "pin note" |
| **Lifecycle (4)** | new note, save note, delete note, export markdown | "new note", "export markdown" |
| **Dictation (3)** | dictation mode, command mode, continuous mode | "dictation mode" |
| **Cockpit (2)** | split with web, split with pdf | "split with web" |
| **Category extras (4)** | search notes, sort by date, sort by title, share note | "search notes", "sort by date" |

**Total: 48 commands** across 5 locales (en-US, de-DE, es-ES, fr-FR, hi-IN).

### 5.5 compose-rich-editor Voice API Mapping

The `RichTextState` API maps cleanly to voice commands:

| Voice Command | compose-rich-editor API |
|--------------|------------------------|
| "bold" / "bold this" | `state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))` |
| "italic" | `state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))` |
| "underline" | `state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))` |
| "heading one" | `state.toggleParagraphStyle(ParagraphStyle(...))` + custom heading annotation |
| "bullet list" | `state.toggleUnorderedList()` |
| "numbered list" | `state.toggleOrderedList()` |
| "code block" | `state.toggleCodeSpan()` |
| "undo" | `state.undoManager.undo()` (if available) |

The 1:1 mapping between voice commands and `RichTextState` methods is the primary reason the Hybrid Document-Block architecture scores 9/10 on voice-first criteria.

---

## 6. Research Track 4: Markdown/MMD Format for RAG

### 6.1 Format Comparison

| Format | Spec | Tables | Footnotes | Metadata | Math | Cross-refs | RAG Score |
|--------|------|--------|-----------|----------|------|------------|-----------|
| CommonMark | Strict | No | No | No | No | No | 5/10 |
| GFM | CommonMark+ | Yes | No | No | No | No | 6/10 |
| MultiMarkdown (MMD) | Superset | Yes | Yes | Yes (own format) | Yes | Yes | 7/10 |
| Pandoc Markdown | Academic | Yes | Yes | YAML frontmatter | Yes (LaTeX) | Yes | 8/10 |
| Obsidian MD | GFM+ | Yes | No | YAML frontmatter | MathJax | `[[wikilinks]]` | 9/10 |

### 6.2 Recommended Format: YAML-Frontmatter GFM Superset (Obsidian-Compatible)

The intersection of all requirements:

```markdown
---
title: Meeting Notes - Q1 Planning
tags: [meeting, q1, planning]
created: 2026-02-17T10:30:00Z
updated: 2026-02-17T11:45:00Z
folder: Work/Meetings
source: dictated
voice_origin_pct: 0.85
attachments:
  - id: att_001
    type: audio
    uri: att://recordings/meeting-q1.m4a
    name: Q1 Planning Recording
---

# Q1 Planning Meeting

## Attendees
- Manoj Jhawar
- Aman Jhawar

## Action Items
- [ ] Finalize budget proposal
- [x] Review previous quarter metrics

## Notes

<!-- dictated:start -->
The main focus for Q1 is expanding the VoiceOS platform to support
additional accessibility scenarios. We discussed three key priorities.
<!-- dictated:end -->

### Priority 1: Enterprise Features

> [!note] Follow up with legal team about compliance requirements

The enterprise rollout requires SOC 2 compliance and...
```

### 6.3 Format Design Rationale

| Feature | Why | RAG Benefit |
|---------|-----|-------------|
| YAML frontmatter | Structured metadata without polluting content | Pre-filtering by tags/date/source before vector search |
| H2 heading boundaries | Natural section breaks | Heading-aware chunk splitting (each H2 = chunk boundary) |
| `#tags` inline | BM25 keyword boosting | Tags in both frontmatter AND body improve term frequency |
| `[[wikilinks]]` | Note-to-note graph | Future: graph RAG for related note discovery |
| `att://` URI scheme | Portable attachment references | Attachment indexing separate from text content |
| `<!-- dictated:start/end -->` | Voice-origin markers | Filter/weight dictated vs typed content differently |
| Checklist `- [ ]`/`- [x]` | GFM standard | Actionable item extraction |
| `> [!note]` callouts | Obsidian-compatible | Callout metadata extraction for RAG |

### 6.4 RAG Chunking Strategy for Notes

```
Note Markdown
    │
    ▼
┌──────────────────────────┐
│  YAML Frontmatter Parse  │  → Extract tags, source, dates for metadata filtering
└──────────┬───────────────┘
           ▼
┌──────────────────────────┐
│  Heading-Aware Chunking  │  → Split at H2 boundaries
│  maxTokens=200           │  → Context injection: prepend ancestor heading chain
│  overlapTokens=25        │  → "# Meeting Notes > ## Action Items > content..."
└──────────┬───────────────┘
           ▼
┌──────────────────────────┐
│  Embedding Generation    │  → all-MiniLM-L6-v2 ONNX (384-dim, int8)
│  (Modules/AI/RAG/)       │  → Batch inference for multi-chunk notes
└──────────┬───────────────┘
           ▼
┌──────────────────────────┐
│  Hybrid Search Index     │  → BM25 (term freq) + Vector (semantic)
│  ReciprocalRankFusion    │  → Fused ranking for "find notes about X"
└──────────────────────────┘
```

**Chunking parameters:**
- `maxTokens = 200` (all-MiniLM-L6-v2 sweet spot at 256 input, leave 56 for context prefix)
- `overlapTokens = 25` (prevent splitting mid-sentence at chunk boundaries)
- Context injection: prepend `"# {note_title} > ## {section_heading} > "` to each chunk
- Attachment text: extracted separately (PDF text, audio transcript) and chunked independently

### 6.5 Existing RAG Pipeline Reuse

The `Modules/AI/RAG/` module already provides everything needed:

| Required Component | Existing in RAG Module | Reuse Strategy |
|-------------------|----------------------|----------------|
| Markdown parsing + YAML extraction | `MarkdownParser.android.kt` | Direct reuse |
| Heading-aware text chunking | `TextChunker.kt` (commonMain) | Direct reuse |
| Token counting | `SimpleTokenizer.kt` (commonMain) + `TokenCounter.kt` | Direct reuse |
| ONNX embedding generation | `ONNXEmbeddingProvider.android.kt` | Direct reuse |
| BM25 scoring | `BM25Scorer.kt` (commonMain) | Direct reuse |
| Hybrid search fusion | `ReciprocalRankFusion.kt` (commonMain) | Direct reuse |
| Query caching | `QueryCache.kt` (commonMain) | Direct reuse |
| Vector quantization | `Quantization.kt` (commonMain) | Direct reuse |

**New code needed:** Only a `NoteRAGIndexer` adapter that:
1. Watches for note save events
2. Parses Markdown → chunks
3. Generates embeddings (batched)
4. Stores in RAG repository with note metadata as filter fields

---

## 7. Competitive Advantage Analysis

### 7.1 NoteAvanue vs Competitors

| Capability | Apple Notes | Notion | Obsidian | Bear | NoteAvanue |
|-----------|------------|--------|----------|------|-----------|
| Rich text editing | Full | Full | Markdown source | Full | Full (compose-rich-editor) |
| Voice formatting | None | None | None | None | 48 commands + format detection |
| Accessibility overlay | None | None | None | None | VOS 4-tier AVID on all buttons |
| Split-frame workflows | None | None | None | None | Cockpit lecture/meeting/research |
| On-device RAG search | Spotlight only | Cloud search | Plugin-based | None | all-MiniLM-L6-v2 ONNX native |
| Markdown export | macOS only | Limited | Native | Yes | Native (canonical format) |
| Head-tracking scroll | None | None | None | None | IMU-based for glasses mode |
| Cross-platform KMP | Apple only | Web+native | Electron | Apple only | Android+Desktop+iOS (KMP) |

### 7.2 Unique Cockpit Workflows

| Workflow | Frame Layout | Voice Integration |
|----------|-------------|-------------------|
| **Lecture Mode** | Audio note (left) + Text note (right) | "start recording", "heading two: key point", live transcript |
| **Meeting Mode** | Web conferencing (left) + Note (center) + AI Summary (right) | "summarize last 5 minutes", "action item: follow up" |
| **Research Mode** | PDF viewer (left) + Note (right) | "quote: [reads from PDF]", "cite this paragraph" |
| **Study Mode** | Note (left) + Flashcard generator (right) | "create flashcard from heading two" |

---

## 8. compose-rich-editor Integration

### 8.1 Library Details

| Property | Value |
|----------|-------|
| Library | `com.mohamedrejeb.richeditor:compose-rich-editor` |
| Version | RC13+ (2025/2026) |
| KMP Support | Android, iOS, Desktop, Web (full multiplatform) |
| License | Apache 2.0 |
| Key class | `RichTextState` |

### 8.2 Dependency Addition

```kotlin
// In NoteAvanue/build.gradle.kts → commonMain
implementation("com.mohamedrejeb.richeditor:compose-rich-editor:1.0.0-rc13")
```

### 8.3 Key APIs for Voice Mapping

```kotlin
// Span formatting
state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
state.toggleSpanStyle(SpanStyle(background = Color.Yellow)) // highlight

// Paragraph formatting
state.toggleUnorderedList()
state.toggleOrderedList()
state.toggleCodeSpan()

// Markdown round-trip
val markdown: String = state.toMarkdown()
state.setMarkdown(markdown)

// HTML round-trip (alternative)
val html: String = state.toHtml()
state.setHtml(html)
```

### 8.4 Migration Path from Current NoteEditor

| Current (`NoteEditor.kt`) | After Migration |
|---------------------------|-----------------|
| `BasicTextField` + `TextFieldValue` | `RichTextEditor` + `RichTextState` |
| `content: String` (plain text) | `markdownContent: String` (Markdown) |
| Bold/Italic buttons = no-ops | Wired to `toggleSpanStyle()` |
| No Markdown export | `state.toMarkdown()` native |
| androidMain only | commonMain (KMP) |
| No voice integration | 48 VOS commands via `NoteCommandExecutor` |

---

## 9. Implementation Phases (Recommended)

| Phase | Scope | Effort | Dependencies |
|-------|-------|--------|-------------|
| **Phase 1: Foundation** | Fix build.gradle.kts, add compose-rich-editor, migrate Note model to Markdown content, create NoteAvanueScreen (commonMain) | Medium | None |
| **Phase 2: Rich Editor** | Wire RichTextState to formatting toolbar, implement Markdown round-trip, add heading/list/checklist support | Medium | Phase 1 |
| **Phase 3: Voice Commands** | Create `note_` prefix VOS commands (48 cmds x 5 locales), VosParser mapping, NoteCommandExecutor | Medium | Phase 2 |
| **Phase 4: Voice Pipeline** | NoteVoiceRouter, NoteVoiceMode, NoteFormatDetector (FSM), DictationManager | High | Phase 3 |
| **Phase 5: Storage** | SQLDelight schema (NoteEntity + NoteBlock + NoteFts + NoteFolder), repository, auto-save | Medium | Phase 1 |
| **Phase 6: RAG Integration** | NoteRAGIndexer adapter, heading-aware chunking, embedding generation on save | Medium | Phase 5 + RAG module |
| **Phase 7: Cockpit** | Embeddable NoteContent view, split-frame workflows (lecture/meeting/research) | Medium | Phase 2 |
| **Phase 8: Attachments** | Photo/audio/document inline embedding, attachment URI scheme, thumbnail generation | High | Phase 5 |

### 9.1 build.gradle.kts Fixes Required (Phase 1)

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)    // Already present
    alias(libs.plugins.compose)           // MISSING — add this
    alias(libs.plugins.kotlin.serialization)
}

// commonMain dependencies — add:
implementation(compose.runtime)
implementation(compose.foundation)
implementation(compose.material3)
implementation(compose.materialIconsExtended)
implementation(compose.ui)
implementation(project(":Modules:AvanueUI"))  // Move from androidMain
implementation("com.mohamedrejeb.richeditor:compose-rich-editor:1.0.0-rc13")
```

---

## 10. KMP Score Projection

| Feature Area | commonMain | Platform-Specific |
|-------------|-----------|-------------------|
| Note model + state | commonMain | - |
| Rich text editor UI | commonMain (compose-rich-editor) | - |
| NoteAvanueScreen | commonMain | - |
| NoteCommandExecutor | commonMain | - |
| NoteFormatDetector (FSM) | commonMain | - |
| NoteVoiceRouter | commonMain interface | Android/iOS impl |
| Markdown serialization | commonMain | - |
| SQLDelight repository | commonMain (queries) | Platform drivers |
| RAG indexing adapter | commonMain interface | Android ONNX impl |
| File attachment handling | - | Platform-specific |
| Audio recording | - | Platform-specific |
| Camera integration | - | Platform-specific |

**Projected KMP Score: 8/12 feature areas in commonMain (67% shared)**
Platform-specific: voice input hardware, audio recording, camera, file system attachment handling.

---

## 11. Appendix: Markdown Flavor Comparison Detail

### 11.1 CommonMark

- Strict spec, minimal extensions
- No tables, footnotes, metadata, math
- Best for: plain text documents with basic formatting
- RAG: Limited — no structured metadata for pre-filtering

### 11.2 GitHub Flavored Markdown (GFM)

- CommonMark + tables, task lists, strikethrough, autolinks
- No frontmatter, footnotes, math
- Best for: developer documentation, READMEs
- RAG: Better — task lists and tables add structure

### 11.3 MultiMarkdown (MMD)

- Own metadata format (not YAML), cross-references, footnotes, citations
- Tables with advanced alignment, math support
- Best for: academic/technical writing
- RAG: Good metadata support but non-standard format

### 11.4 Pandoc Markdown

- Most feature-rich: YAML frontmatter, LaTeX math, citations, footnotes
- Heavy toolchain dependency
- Best for: academic publishing pipelines
- RAG: Excellent metadata but overkill for notes

### 11.5 Obsidian Markdown (RECOMMENDED)

- GFM base + YAML frontmatter + `[[wikilinks]]` + `> [!callout]` + `#tags`
- Large ecosystem, human-readable, no special tooling needed
- Best for: knowledge management, interconnected notes
- RAG: Excellent — frontmatter for filtering, wikilinks for graph, tags for BM25, headings for chunking

---

## 12. References

| Resource | Location |
|----------|----------|
| Existing NoteAvanue module | `Modules/NoteAvanue/` |
| RAG module | `Modules/AI/RAG/` |
| compose-rich-editor | `github.com/MohamedRejeb/compose-rich-editor` |
| VOS v3.0 compact format | `docs/plans/VoiceOSCore/VoiceOSCore-Plan-VOSCompactFormat-260216-V1.md` |
| PhotoAvanue self-running pattern | Chapter 98, `Docs/MasterDocs/PhotoAvanue/` |
| AvanueTheme reference | Chapters 91-92, `Docs/MasterDocs/AvanueUI/` |
| HubModule registry | `apps/avanues/src/main/kotlin/com/augmentalis/voiceavanue/ui/hub/HubModule.kt` |
