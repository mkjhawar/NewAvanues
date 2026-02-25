# Developer Manual — Chapter 100: NoteAvanue Voice-First Rich Note Editor

**Module:** `Modules/NoteAvanue/`
**Package:** `com.augmentalis.noteavanue`
**Platform:** KMP (Android + Desktop commonMain, Android-specific androidMain)
**Branch:** `VoiceOS-1M-SpeechEngine`
**Date:** 2026-02-18 (updated 2026-02-24: RichTextUndoManager, CommandBar wiring)

---

## 1. Overview

NoteAvanue is a voice-first rich text note editor that transforms spoken words into formatted Markdown notes. It runs standalone via `NoteAvanueScreen` or embeds inside Cockpit frames via `NoteEditor`.

**Key capabilities:**
- Rich text editing with Markdown round-trip (compose-rich-editor)
- 48 voice commands across 5 locales (en, de, es, fr, hi)
- Voice dictation with FSM format detection (headings, lists, quotes, code)
- SQLDelight persistence with folders, attachments, and smart filters
- On-device RAG search via AI/RAG module integration
- Audio recording and photo attachment support
- SpatialVoice design language (AvanueTheme)

**KMP Score:** 11 commonMain files / 18 total = 61% shared code.

---

## 2. Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    NoteAvanueScreen                      │
│    (commonMain, standalone, SpatialVoice gradient)       │
├─────────────────────────────────────────────────────────┤
│          NoteEditor (androidMain, embeddable)            │
│    (Cockpit frames, compact toolbar, no TopAppBar)       │
├──────────────┬──────────────┬──────────────┬────────────┤
│ INoteController │ Voice Pipeline │ Repository   │ Attachment │
│ (formatting,  │ (router,       │ (SQLDelight, │ (att://,   │
│  navigation,  │  format detect,│  folders,    │  resolver, │
│  content)     │  dictation)    │  search)     │  recorder) │
├──────────────┴──────────────┴──────────────┴────────────┤
│              compose-rich-editor (RC13)                   │
│         RichTextState ←→ Markdown round-trip              │
├─────────────────────────────────────────────────────────┤
│     Foundation    │   Database   │   AvanueUI   │ AI/RAG │
└───────────────────┴──────────────┴──────────────┴────────┘
```

### Source Set Layout

| Source Set | Files | Purpose |
|-----------|-------|---------|
| `commonMain` | 11 | Models, interfaces, screen, repository, voice detection, attachment constants |
| `androidMain` | 7 | NoteEditor, RichTextUndoManager, voice router, RAG indexer, attachment resolver, audio recorder |

---

## 3. Models (`model/`)

### Note
```kotlin
data class Note(
    val id: String,
    val title: String = "",
    val markdownContent: String = "",
    val folderId: String? = null,
    val attachments: List<NoteAttachment> = emptyList(),
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val source: NoteSource = NoteSource.MANUAL,
    val voiceOriginPct: Float = 0f,
    val createdAt: String, val updatedAt: String
)
```

### NoteSource
`MANUAL` | `DICTATED` | `IMPORTED`

### NoteVoiceMode
`COMMANDING` (voice commands) | `DICTATING` (text insertion) | `CONTINUOUS` (auto-resume dictation)

### NoteFolder
Nested folders with optional smart filter. `isSmartFolder = true` + `smartFilter` JSON enables automatic note grouping.

### NoteAttachment
```kotlin
data class NoteAttachment(
    val id: String, val uri: String, val type: AttachmentType,
    val name: String, val mimeType: String, val fileSizeBytes: Long,
    val insertPosition: Int, val caption: String, val thumbnailUri: String?
)
enum class AttachmentType { PHOTO, DOCUMENT, AUDIO, SKETCH, VIDEO }
```

---

## 4. INoteController Interface

Platform-agnostic controller for rich text operations:

| Category | Methods |
|----------|---------|
| **Formatting** | `toggleBold()`, `toggleItalic()`, `toggleUnderline()`, `toggleStrikethrough()` |
| **Structure** | `setHeadingLevel(1-3)`, `toggleBulletList()`, `toggleNumberedList()`, `toggleChecklist()`, `toggleCodeBlock()`, `toggleBlockquote()`, `insertDivider()` |
| **Navigation** | `goToTop()`, `goToBottom()`, `nextHeading()`, `previousHeading()` |
| **Editing** | `undo()`, `redo()`, `selectAll()`, `deleteLine()`, `insertParagraph()` |
| **Content** | `setMarkdown(md)`, `getMarkdown(): String`, `insertText(text)` |
| **Lifecycle** | `save()`, `release()` |

---

## 5. UI Components

### NoteAvanueScreen (commonMain, standalone)

Full-screen editor with SpatialVoice design:
- Gradient background: `verticalGradient(background, surface*0.6, background)`
- Transparent TopAppBar with back nav + save button
- Title field (BasicTextField, H5 style)
- Scrollable formatting toolbar: `[B][I][U][S] | [H1][H2][H3] | [bullet][numbered][checklist][code][quote][divider]`
- `RichTextEditor` with `rememberRichTextState()` + Markdown round-trip
- Action bar: camera, attach file, dictation mic
- Status bar: word count, char count

### NoteEditor (androidMain, embeddable)

Compact editor for Cockpit frames:
- No TopAppBar (frame chrome provides navigation)
- Compact toolbar: Bold/Italic + action icons (camera, attach, dictate, save)
- `RichTextEditor` with same Markdown round-trip
- `hasLoaded` keyed by `initialContent` — resets when Cockpit switches notes in the same frame
- `RichTextUndoManager` for snapshot-based undo/redo (see below)
- Attachment chips (FlowRow)
- Word count footer

### RichTextUndoManager (androidMain)

compose-rich-editor RC13 does not expose a native undo/redo API, so NoteAvanue implements snapshot-based undo/redo via `RichTextUndoManager`:

```kotlin
class RichTextUndoManager(richTextState: RichTextState, maxHistory: Int = 50) {
    fun captureSnapshot()  // Saves current markdown to undo stack (dedup-aware)
    fun undo(): Boolean    // Pops undo stack, pushes to redo, restores via setMarkdown()
    fun redo(): Boolean    // Pops redo stack, pushes to undo, restores via setMarkdown()
    val canUndo: Boolean
    val canRedo: Boolean
}
```

**How it works:**
1. Before each formatting operation (bold, italic, heading, etc.), `captureSnapshot()` saves the current `toMarkdown()` output
2. On undo, the last snapshot is popped from the undo stack, current state is pushed to redo stack, and `setMarkdown()` restores
3. Deduplication: if the current markdown matches the last snapshot, no new entry is created
4. Max 50 snapshots to prevent unbounded memory growth

**Trade-off:** Cursor position is lost on undo/redo (markdown serialization doesn't preserve selection). Content is preserved correctly.

### Cockpit Integration

`FrameContent.Note` carries `markdownContent: String`. `ContentRenderer` dispatches to `NoteEditor`:
```kotlin
is FrameContent.Note -> NoteEditor(
    initialTitle = frame.title,
    initialContent = content.markdownContent,
    onSave = { title, markdownContent -> ... }
)
```

### CommandBar Integration (260224)

The Cockpit CommandBar dispatches formatting actions to NoteEditor via `ModuleCommandCallbacks.noteExecutor`. This provides a **second input path** alongside voice commands — the user taps CommandBar chips, which translate to the same `CommandActionType` values used by voice:

```
CommandBar chip (NOTE_BOLD) → ContentAction enum
  → CockpitViewModel._contentAction.tryEmit()
  → ContentRenderer LaunchedEffect collects
  → Maps ContentAction.NOTE_BOLD → CommandActionType.FORMAT_BOLD
  → Calls ModuleCommandCallbacks.noteExecutor?.invoke(FORMAT_BOLD, emptyMap())
  → NoteEditor's executeNoteCommand: captureSnapshot() + toggleSpanStyle(Bold)
```

Supported CommandBar actions: **Bold**, **Italic**, **Underline**, **Strikethrough**, **Undo**, **Redo**, **Save** (7 chips in NOTE_ACTIONS state).

---

## 6. Voice Commands (48 commands x 5 locales)

### VOS Registration

- `ActionCategory.NOTE` (priority 12)
- VosParser: `CATEGORY_MAP["note"] = "NOTE"`, `ACTION_MAP` with 48 `note_*` entries
- `NoteCommandHandler` in `AndroidHandlerFactory` (Wave 3)

### Command Categories

| Category | Commands | Examples |
|----------|----------|---------|
| **Formatting** | 7 | bold, italic, underline, strikethrough, divider, code block, blockquote |
| **Headings** | 3 | heading one, heading two, heading three |
| **Lists** | 3 | bullet list, numbered list, checklist |
| **Navigation** | 4 | go to top, go to bottom, next heading, previous heading |
| **Editing** | 5 | undo, redo, select all, delete line, new paragraph |
| **Voice Mode** | 3 | dictation mode, command mode, continuous mode |
| **Note Mgmt** | 6 | open notes, new note, save note, toggle pin, export, search |
| **Media** | 2 | insert photo, attach file |

### Locales

All 48 commands translated: `en-US`, `de-DE`, `es-ES`, `fr-FR`, `hi-IN`.

---

## 7. Voice Pipeline

### NoteFormatDetector (commonMain)

FSM keyword trigger detection. Pure Kotlin, O(1) against fixed patterns, no ML.

**Trigger patterns (case-insensitive):**
| Prefix | Format | Example |
|--------|--------|---------|
| `title:`, `heading one:` | H1 | "heading one Project Overview" |
| `heading two:`, `subtitle:` | H2 | "subtitle: Architecture" |
| `heading three:`, `section:` | H3 | "section: Components" |
| `bullet:`, `dash:`, `point:` | Bullet | "bullet: First item" |
| `first:`, `second:`, `number N:` | Numbered | "first: Introduction" |
| `todo:`, `task:`, `checkbox:` | Checklist | "todo: Review code" |
| `quote:`, `quotation:` | Blockquote | "quote: To be or not to be" |
| `code:`, `snippet:` | Code | "code: function main" |
| (none) | Paragraph | "Hello world" |

### INoteVoiceRouter / AndroidNoteVoiceRouter

Routes speech based on `NoteVoiceMode`:
- **COMMANDING**: Delegates to VoiceOSCore pipeline (NoteCommandHandler)
- **DICTATING**: FormatDetector -> INoteController -> insert text
- **CONTINUOUS**: Same as DICTATING + auto-restart on silence

### NoteDictationManager

Tracks voice-origin percentage (dictated chars vs typed chars). Manages `<!-- dictated:start/end -->` HTML comment markers in Markdown for provenance tracking.

---

## 8. Persistence

### SQLDelight Tables

| Table | Module | Purpose |
|-------|--------|---------|
| `note_entity` | Database | Notes (id, title, markdown, folder, pins, source, voice%) |
| `note_folder` | Database | Folders (nested, smart filters) |
| `note_attachment` | Database | Attachments (type, uri, size, offset, caption) |

### INoteRepository / NoteRepositoryImpl

Flow-based reactive queries:
- `getAllNotes(): Flow<List<Note>>`
- `searchNotes(query): Flow<List<Note>>` — full-text search on title + content
- `getNotesByFolder(folderId): Flow<List<Note>>`
- `getRecentNotes(limit): Flow<List<Note>>`

---

## 9. RAG Integration

### NoteRAGIndexer (androidMain)

Bridges NoteAvanue -> `Modules/AI/RAG/`:
1. Note saved -> write Markdown to temp file (with title as H1)
2. Submit as `DocumentType.MD` via `RAGRepository.addDocument(processImmediately=true)`
3. RAG pipeline: parse -> chunk at H2 boundaries -> embed (ONNX) -> store
4. Metadata: `source=noteavanue`, `noteId`, `voiceOriginPct`, `folderId`, `tags`

```kotlin
val indexer = NoteRAGIndexer(ragRepository, cacheDir)
indexer.indexNoteAsync(note)  // Fire-and-forget from save path

val results = indexer.searchNotes("architecture overview", maxResults = 5)
```

---

## 10. Attachment System

### att:// URI Scheme

Custom URIs embedded in Markdown:
```
att://notes/{noteId}/attachments/{attachmentId}
att://notes/{noteId}/thumbnails/{attachmentId}
```

Photos: `![caption](att://notes/abc/attachments/def)`
Audio: `[Recording 1](att://notes/abc/attachments/ghi)`
Docs: `[Report.pdf](att://notes/abc/attachments/jkl)`

### NoteAttachmentResolver (androidMain)

- `importAttachment(noteId, contentUri, type, name, mimeType)` — copy from picker
- `importFromStream(noteId, stream, type, name, mimeType)` — from camera/recorder
- `resolve(attUri): File?` — att:// -> file path
- `deleteAllForNote(noteId)` — cleanup on note deletion
- Auto-generates thumbnails for photos (200px max dimension, JPEG 75%)

Storage: `files/note_attachments/{noteId}/{attachmentId}.{ext}`

### NoteAudioRecorder (androidMain)

AAC in M4A container, 44.1kHz, 128kbps. Supports pause/resume (API 24+).

---

## 11. Cockpit Workflow Templates

Three new templates added to `BuiltInTemplates`:

| Template | Layout | Frames |
|----------|--------|--------|
| **Lecture** | SPLIT_LEFT | VoiceNote + Note + Web |
| **Meeting Notes** | GRID | Camera + Note + Web + AI Summary |
| **Research + PDF** | SPLIT_LEFT | PDF + Note + Web |

---

## 12. Build Configuration

### Dependencies (commonMain)
```kotlin
api(project(":Modules:Foundation"))
api(project(":Modules:Logging"))
implementation(project(":Modules:Database"))
implementation(project(":Modules:AvanueUI"))
implementation(libs.richeditor.compose)  // compose-rich-editor RC13
implementation(libs.sqldelight.coroutines.extensions)
```

### Dependencies (androidMain)
```kotlin
implementation(libs.androidx.core.ktx)
implementation(project(":Modules:AI:RAG"))  // On-device semantic search
```

### Plugins
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)   // K2 Compose compiler
    alias(libs.plugins.compose)          // JB Compose multiplatform
    alias(libs.plugins.kotlin.serialization)
}
```

---

## 13. How To: Add a New Voice Command

1. Add enum value to `CommandActionType` (e.g., `INSERT_TABLE`)
2. Add to `VosParser.ACTION_MAP`: `"note_table" to CommandActionType.INSERT_TABLE`
3. Add to `ActionCoordinator.actionTypeToPhrase()`
4. Add VOS line to each locale file: `note_table|insert table|add table,table|Insert table`
5. Handle in `NoteCommandHandler.execute()` dispatch
6. Add phrase mapping in `ActionCoordinator`

---

## 14. How To: Add a New Attachment Type

1. Add value to `AttachmentType` enum (e.g., `LINK`)
2. Add MIME mapping in `NoteAttachmentResolver.extensionFromMime()`
3. Handle rendering in `NoteEditor` attachment chip section
4. Add Markdown format in `AttachmentConstants` if needed

---

## 15. Verification Checklist

- [x] `./gradlew :Modules:NoteAvanue:compileKotlinDesktop` — BUILD SUCCESSFUL
- [x] `./gradlew :Modules:NoteAvanue:compileDebugKotlinAndroid` — BUILD SUCCESSFUL
- [x] `./gradlew :Modules:Cockpit:compileDebugKotlinAndroid` — BUILD SUCCESSFUL
- [x] `./gradlew :Modules:VoiceOSCore:compileDebugKotlinAndroid` — BUILD SUCCESSFUL
- [x] `./gradlew :Modules:Database:compileDebugKotlinAndroid` — BUILD SUCCESSFUL
- [x] 48 note_ commands in VosParser ACTION_MAP
- [x] 5 locale VOS files updated (en, de, es, fr, hi)
- [x] NoteFormatDetector: "heading one: Overview" -> H1 + "Overview"
- [x] att:// URI scheme with regex extraction
