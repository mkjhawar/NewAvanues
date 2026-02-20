# AI Module — Unification & Wiring Plan

**Document:** AI-Plan-UnificationAndWiring-260219-V1.md
**Date:** 2026-02-19
**Branch:** Cockpit-Development
**Mode:** .yolo .cot
**Author:** Manoj Jhawar

---

## 1. Summary

Unify the duplicate cloud provider implementations that currently exist across ALC and LLM sub-modules, add the missing `androidMain` implementation for the Memory sub-module, wire the AI stack into the VoiceOS 4-tier dispatch pipeline via five new voice commands, and connect the Cockpit `AiSummary` frame to real LLM calls. This is a targeted wiring and consolidation pass — not a rewrite. The existing 316-file, production-grade AI codebase is kept intact. KMP Score target remains ~55%.

---

## 2. Problem Statement

### 2.1 Duplicate Cloud Providers

ALC sub-module owns:
- `AnthropicProvider`, `OpenAIProvider`, `GoogleAIProvider`, `GroqProvider`, `HuggingFaceProvider`, `OpenRouterProvider`
- `LLMProviderFactory` — routes cloud vs. on-device

LLM sub-module independently owns its own provider implementations for the same cloud services. Both sets of providers are in production use by different consumers (Chat uses LLM, ALC uses its own). This means:
- Config drift: two copies of API key handling, retry logic, timeout config
- Bug fixes must be applied in two places
- New provider additions require two registrations
- No clear "canonical" implementation

### 2.2 Memory Sub-Module Has No androidMain

`Modules/AI/Memory/src/commonMain/` defines `IMemoryStore`, `MemoryEntry`, and `MemoryQuery` but there is no `androidMain` implementation. The interface is never instantiated on Android. Conversation history is therefore not persisted between sessions.

### 2.3 AI Stack Not Wired into VoiceOS Dispatch

The AI stack is completely absent from the 4-tier voice dispatch pipeline:
- `ActionCategory` has no `AI` value
- `CommandActionType` has no AI action types
- No VOS commands exist for AI interactions
- `ActionCoordinator` has no branches for AI actions
- No `AIHandler` is registered in `AndroidHandlerFactory`

Voice users cannot trigger AI features by voice command.

### 2.4 Cockpit AiSummary Frame is a Stub

`FrameContent.AiSummary` exists in `FrameContent.kt` and `ContentRenderer.kt` has a branch for it, but the branch renders a placeholder. It does not call any LLM provider, collect content from source frames, or stream a response. The Cockpit AI panel is non-functional.

---

## 3. Solution Overview

Four independent phases. Phases 1 and 2 are foundational; Phases 3 and 4 consume them.

| Phase | Title | Risk | Estimated Files |
|-------|-------|------|-----------------|
| 1 | Provider Unification | Low | 3 modified |
| 2 | Memory androidMain | Medium | 1 new + 2 modified |
| 3 | VoiceOS 4-Tier Wiring | Low | 4 modified + 5 VOS |
| 4 | Cockpit AiSummary Wiring | Medium | 1 new + 2 modified |

**Total new files:** 2 Kotlin + 5 VOS locale files
**Total modified files:** ~9 Kotlin

---

## 4. Phase 1 — Provider Unification

### 4.1 Goal

Make ALC's `LLMProviderFactory` + provider set the single canonical implementation. LLM sub-module retains its own types only where they represent genuinely distinct functionality (local model loading, streaming adapters). Duplicate cloud-only providers in LLM sub-module are marked `@Deprecated` pointing to ALC's equivalents.

### 4.2 Canonical Provider Registry

ALC becomes the single entry point for all provider resolution:

```
Modules/AI/ALC/src/commonMain/kotlin/com/augmentalis/alc/provider/
├── ILLMProvider.kt             (already exists — canonical interface)
├── LLMProviderFactory.kt       (already exists — canonical factory)
├── UnifiedProviderFactory.kt   (NEW — wraps LLMProviderFactory, adds on-device routing)
├── AnthropicProvider.kt        (already exists)
├── OpenAIProvider.kt           (already exists)
├── GoogleAIProvider.kt         (already exists)
├── GroqProvider.kt             (already exists)
├── HuggingFaceProvider.kt      (already exists)
└── OpenRouterProvider.kt       (already exists)
```

### 4.3 File: UnifiedProviderFactory.kt (NEW)

**Path:** `Modules/AI/ALC/src/commonMain/kotlin/com/augmentalis/alc/provider/UnifiedProviderFactory.kt`

Responsibility: Single entry point for both cloud and on-device provider selection. Combines ALC's existing `LLMProviderFactory` (cloud routing) with on-device engine selection (TVM/ONNX/CoreML per platform).

```kotlin
// Key interface
object UnifiedProviderFactory {
    fun create(config: LLMConfig): ILLMProvider
    fun createStreaming(config: LLMConfig): IStreamingLLMProvider
    fun availableProviders(): List<ProviderDescriptor>
}
```

Routing logic:
- `config.backend == Backend.CLOUD` → delegates to `LLMProviderFactory.create(config)`
- `config.backend == Backend.ON_DEVICE` → uses `expect fun onDeviceEngine(): ILLMProvider` (resolved per platform: TVM on Android, ONNX on Desktop, CoreML on iOS)
- `config.backend == Backend.AUTO` → prefers on-device if model is cached, falls back to cloud

### 4.4 LLM Sub-Module Updates

**Files to update:**

| File | Change |
|------|--------|
| `Modules/AI/LLM/src/*/providers/AnthropicProvider.kt` | Add `@Deprecated("Use com.augmentalis.alc.provider.AnthropicProvider", ReplaceWith(...))` |
| `Modules/AI/LLM/src/*/providers/OpenAIProvider.kt` | Same deprecation |
| `Modules/AI/LLM/src/*/providers/GoogleAIProvider.kt` | Same deprecation |
| `Modules/AI/LLM/src/*/providers/GroqProvider.kt` | Same deprecation |

Do NOT delete. Deprecation allows existing call sites to continue compiling with warnings, providing a migration path without a flag day.

### 4.5 ResponseCoordinator Update

**File:** `Modules/AI/Chat/src/main/kotlin/com/augmentalis/chat/coordinator/ResponseCoordinator.kt`

Change provider instantiation to use `UnifiedProviderFactory.create(config)` instead of LLM module's direct provider creation. This is the highest-traffic consumer of LLM responses and the primary target of this migration.

### 4.6 Verification

- Build compiles without error after deprecation annotations added
- `ResponseCoordinator` imports update to `com.augmentalis.alc.provider.*`
- No behavior change in Chat — same providers, same API keys, same response format
- Deprecated symbols emit warnings at old call sites (not errors)

---

## 5. Phase 2 — Memory androidMain

### 5.1 Goal

Provide a concrete `IMemoryStore` implementation on Android backed by the existing Database module's SQLDelight tables. Wire it into `ConversationManager` so conversation history persists across process restarts.

### 5.2 Database Table Prerequisite

The `memory_entry` table must exist in `Modules/Database/`. If it does not, add:

```sql
-- Modules/Database/src/commonMain/sqldelight/memory_entry.sq
CREATE TABLE memory_entry (
    id             TEXT    NOT NULL PRIMARY KEY,
    conversationId TEXT    NOT NULL,
    userId         TEXT    NOT NULL,
    role           TEXT    NOT NULL,  -- "user" | "assistant" | "system"
    content        TEXT    NOT NULL,
    timestamp      INTEGER NOT NULL,
    tokenCount     INTEGER NOT NULL DEFAULT 0,
    isActive       INTEGER NOT NULL DEFAULT 1
);

selectByConversation:
SELECT * FROM memory_entry
WHERE conversationId = ? AND isActive = 1
ORDER BY timestamp ASC;

insertEntry:
INSERT OR REPLACE INTO memory_entry VALUES (?, ?, ?, ?, ?, ?, ?, 1);

deleteByConversation:
UPDATE memory_entry SET isActive = 0 WHERE conversationId = ?;

searchByKeyword:
SELECT * FROM memory_entry
WHERE conversationId = ? AND content LIKE '%' || ? || '%' AND isActive = 1
ORDER BY timestamp DESC
LIMIT ?;
```

### 5.3 File: AndroidMemoryStore.kt (NEW)

**Path:** `Modules/AI/Memory/src/androidMain/kotlin/com/augmentalis/memory/AndroidMemoryStore.kt`

```kotlin
class AndroidMemoryStore(
    private val db: MemoryEntryQueries  // injected via Hilt
) : IMemoryStore {

    override suspend fun store(entry: MemoryEntry): Unit
    override suspend fun retrieve(conversationId: String): List<MemoryEntry>
    override suspend fun search(conversationId: String, keyword: String, limit: Int): List<MemoryEntry>
    override suspend fun clear(conversationId: String): Unit
    override suspend fun tokenCount(conversationId: String): Int
}
```

Mapping: `MemoryEntry` (commonMain model) ↔ `memory_entry` (SQLDelight row) via extension functions.

### 5.4 Hilt Binding

Add to the AI module's Hilt module (`AIModule.kt` or `MemoryModule.kt`):

```kotlin
@Binds
@Singleton
abstract fun bindMemoryStore(impl: AndroidMemoryStore): IMemoryStore
```

### 5.5 ConversationManager Update

**File:** `Modules/AI/Chat/src/main/kotlin/com/augmentalis/chat/coordinator/ConversationManager.kt`

Inject `IMemoryStore`. On each message exchange:
1. After assistant response confirmed: `memoryStore.store(MemoryEntry(role="user", content=userMsg, ...))`
2. Store assistant reply: `memoryStore.store(MemoryEntry(role="assistant", content=reply, ...))`
3. On session restore: `memoryStore.retrieve(conversationId)` to rebuild in-memory context window

Context window management: If `tokenCount(conversationId) > maxContextTokens`, trim oldest entries (keep system prompt always, trim oldest user/assistant pairs first).

---

## 6. Phase 3 — VoiceOS 4-Tier Wiring

### 6.1 New ActionCategory Value

**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceos/model/ActionCategory.kt`

Add:
```kotlin
AI(priority = 15),  // between CAMERA(14) and CUSTOM(16) — shift CUSTOM to 17
```

Priority 15 ensures AI commands are evaluated after camera (high-specificity hardware) but before the general CUSTOM catch-all.

### 6.2 New CommandActionType Values

**File:** `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceos/model/CommandActionType.kt`

Add the following under a new `// AI` section:

```kotlin
// AI
AI_SUMMARIZE,         // Summarize current context or selected text
AI_CHAT,              // Open / focus the AI chat frame
AI_RAG_SEARCH,        // Query the local RAG knowledge base
AI_TEACH,             // Start an interactive teaching flow
AI_CLEAR_CONTEXT,     // Clear current conversation memory
```

### 6.3 AIHandler.kt

**Path:** `Modules/AI/src/androidMain/kotlin/com/augmentalis/ai/handler/AIHandler.kt`

Implements the `IHandler` interface from VoiceOSCore.

```kotlin
class AIHandler(
    private val responseCoordinator: ResponseCoordinator,
    private val ragCoordinator: RAGCoordinator,
    private val teachingFlowManager: TeachingFlowManager,
    private val conversationManager: ConversationManager
) : BaseHandler() {

    override fun canHandle(category: ActionCategory): Boolean =
        category == ActionCategory.AI

    override fun handle(command: StaticCommand): HandlerResult {
        return when (command.actionType) {
            CommandActionType.AI_SUMMARIZE     -> handleSummarize(command)
            CommandActionType.AI_CHAT          -> handleOpenChat(command)
            CommandActionType.AI_RAG_SEARCH    -> handleRagSearch(command)
            CommandActionType.AI_TEACH         -> handleTeach(command)
            CommandActionType.AI_CLEAR_CONTEXT -> handleClearContext(command)
            else                               -> HandlerResult.NotHandled
        }
    }

    private fun handleSummarize(command: StaticCommand): HandlerResult
    private fun handleOpenChat(command: StaticCommand): HandlerResult
    private fun handleRagSearch(command: StaticCommand): HandlerResult
    private fun handleTeach(command: StaticCommand): HandlerResult
    private fun handleClearContext(command: StaticCommand): HandlerResult
}
```

**Note:** `AIHandler` lives under `Modules/AI/` (not VoiceOSCore) because it depends on AI module internals. VoiceOSCore depends on the AI module at the handler registration layer only.

### 6.4 AndroidHandlerFactory Update

**File:** `Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceos/handler/AndroidHandlerFactory.kt`

Add `AIHandler` to `createHandlers()`:

```kotlin
AIHandler(
    responseCoordinator = responseCoordinator,
    ragCoordinator = ragCoordinator,
    teachingFlowManager = teachingFlowManager,
    conversationManager = conversationManager
)
```

Total handlers after this change: 12 (11 existing + AIHandler).

### 6.5 ActionCoordinator Update

**File:** `Modules/AI/Chat/src/main/kotlin/com/augmentalis/chat/coordinator/ActionCoordinator.kt`

Add AI branches to the exhaustive `when` on `CommandActionType`:

```kotlin
CommandActionType.AI_SUMMARIZE -> {
    val context = contextCollector.currentScreenText()
    responseCoordinator.summarize(context, SummaryType.BRIEF)
}
CommandActionType.AI_CHAT -> {
    navigationCoordinator.openChatFrame()
}
CommandActionType.AI_RAG_SEARCH -> {
    val query = command.parameters["query"] ?: command.phrase
    ragCoordinator.search(query)
}
CommandActionType.AI_TEACH -> {
    teachingFlowManager.startTeaching(command.parameters)
}
CommandActionType.AI_CLEAR_CONTEXT -> {
    conversationManager.clearContext(sessionManager.currentConversationId())
}
```

### 6.6 VOS Commands — 5 Locales

Five new commands added to each locale's `.app.vos` file. Format: VOS v3.0 pipe-delimited.

**en-US.app.vos additions:**

```
AI_SUMMARIZE|AI|summarize this|summarize page|give me a summary|tl;dr
AI_CHAT|AI|open chat|open ai|talk to ai|ask ai
AI_RAG_SEARCH|AI|search knowledge base|search my notes|find in knowledge
AI_TEACH|AI|teach me|start lesson|explain this
AI_CLEAR_CONTEXT|AI|clear chat|new conversation|forget context|start over
```

**es-ES.app.vos additions:**

```
AI_SUMMARIZE|AI|resumir esto|dame un resumen|resumir página|resumen
AI_CHAT|AI|abrir chat|abrir ia|hablar con ia|preguntar a ia
AI_RAG_SEARCH|AI|buscar en conocimiento|buscar mis notas|encontrar en base
AI_TEACH|AI|enséñame|iniciar lección|explica esto
AI_CLEAR_CONTEXT|AI|limpiar chat|nueva conversación|olvidar contexto
```

**fr-FR.app.vos additions:**

```
AI_SUMMARIZE|AI|résumer ceci|donne-moi un résumé|résumer la page|tl;dr
AI_CHAT|AI|ouvrir le chat|ouvrir l'ia|parler à l'ia|demander à l'ia
AI_RAG_SEARCH|AI|chercher dans la base|chercher mes notes|trouver dans la connaissance
AI_TEACH|AI|apprends-moi|démarrer leçon|explique ceci
AI_CLEAR_CONTEXT|AI|effacer le chat|nouvelle conversation|oublier le contexte
```

**de-DE.app.vos additions:**

```
AI_SUMMARIZE|AI|zusammenfassen|gib mir eine zusammenfassung|seite zusammenfassen|kurzfassung
AI_CHAT|AI|chat öffnen|ki öffnen|mit ki sprechen|ki fragen
AI_RAG_SEARCH|AI|wissensbasis durchsuchen|notizen durchsuchen|in wissen finden
AI_TEACH|AI|lehr mich|lektion starten|erkläre dies
AI_CLEAR_CONTEXT|AI|chat löschen|neues gespräch|kontext vergessen
```

**hi-IN.app.vos additions:**

```
AI_SUMMARIZE|AI|इसे संक्षेप में बताओ|सारांश दो|पेज का सारांश
AI_CHAT|AI|चैट खोलो|एआई खोलो|एआई से बात करो|एआई से पूछो
AI_RAG_SEARCH|AI|ज्ञान आधार खोजो|मेरे नोट्स खोजो|ज्ञान में खोजो
AI_TEACH|AI|मुझे सिखाओ|पाठ शुरू करो|इसे समझाओ
AI_CLEAR_CONTEXT|AI|चैट साफ करो|नई बातचीत|संदर्भ भूल जाओ
```

---

## 7. Phase 4 — Cockpit AiSummary Wiring

### 7.1 ContentRenderer Update

**File:** `Modules/Cockpit/src/androidMain/kotlin/com/augmentalis/cockpit/ui/renderer/ContentRenderer.kt`

Replace the stub branch for `FrameContent.AiSummary` with a call to the new `AiSummaryPanel` composable:

```kotlin
is FrameContent.AiSummary -> {
    AiSummaryPanel(
        content = frame.content as FrameContent.AiSummary,
        sourceFrames = frameRepository.framesById(content.sourceFrameIds),
        onSummaryTypeChanged = { type -> frameRepository.updateSummaryType(frame.id, type) },
        modifier = modifier
    )
}
```

### 7.2 File: AiSummaryPanel.kt (NEW)

**Path:** `Modules/Cockpit/src/androidMain/kotlin/com/augmentalis/cockpit/ui/content/AiSummaryPanel.kt`

This composable manages the full AI summary lifecycle within a Cockpit frame.

**State machine:**

```
IDLE → COLLECTING → GENERATING (streaming) → COMPLETE → IDLE (on refresh)
                                           ↘ ERROR
```

**Layout:**

```
┌─────────────────────────────────────────────┐
│ [Summary Type Selector]  [Refresh] [Auto-⟳] │  ← controls row
├─────────────────────────────────────────────┤
│ [Source: Frame A] [Source: Frame B] …       │  ← source chips
├─────────────────────────────────────────────┤
│                                             │
│  Summary text rendered here (streaming)     │  ← main body
│  with animated cursor while generating      │
│                                             │
└─────────────────────────────────────────────┘
```

**Summary type selector:** `SummaryType` enum with four options:
- `BRIEF` — 2–3 sentence overview
- `DETAILED` — Full structured summary with headings
- `ACTION_ITEMS` — Bullet list of action items extracted
- `QA` — Question and answer pairs from content

**Source content collection:**

```kotlin
LaunchedEffect(sourceFrames, summaryType) {
    val combinedText = sourceFrames
        .mapNotNull { frame -> frame.extractTextContent() }
        .joinToString("\n\n---\n\n")
    if (combinedText.isNotBlank()) {
        responseCoordinator.summarizeStreaming(combinedText, summaryType)
            .collect { token -> summaryText += token }
    }
}
```

**Auto-refresh:** When `content.autoRefresh == true`, observe `sourceFrames` with `snapshotFlow` and re-trigger summary on content change (debounced 2 seconds to avoid thrash during live editing).

**AvanueUI v5.1 theming:**

| Element | Token |
|---------|-------|
| Panel background | `AvanueTheme.colors.surface` + 0.85f alpha |
| Summary text | `AvanueTheme.colors.onSurface` |
| Type selector chips | `AvanueChip` (AvanueUI unified component) |
| Source chips | `AvanueChip` outlined style |
| Refresh button | `AvanueIconButton` |
| Auto-refresh toggle | `AvanueSwitch` |
| Streaming cursor | `AvanueTheme.colors.primary` blinking rectangle |
| Error state | `AvanueTheme.colors.error` text |

**AVID voice identifiers (mandatory on all interactive elements):**

| Element | AVID |
|---------|------|
| Summary type selector | `Modifier.semantics { contentDescription = "Voice: select summary type" }` |
| Refresh button | `Modifier.semantics { contentDescription = "Voice: refresh summary" }` |
| Auto-refresh toggle | `Modifier.semantics { contentDescription = "Voice: toggle auto refresh" }` |
| Each type chip | `Modifier.semantics { contentDescription = "Voice: summary type ${type.label}" }` |

### 7.3 ResponseCoordinator Streaming Extension

**File:** `Modules/AI/Chat/src/main/kotlin/com/augmentalis/chat/coordinator/ResponseCoordinator.kt`

Add if not already present:

```kotlin
fun summarizeStreaming(content: String, summaryType: SummaryType): Flow<String>
```

Uses `UnifiedProviderFactory.createStreaming(config)` to get a streaming provider, sends a system-prompt-prefixed request, emits tokens as they arrive. Applies `SummaryType`-specific system prompts:

- `BRIEF`: "Summarize the following in 2-3 sentences."
- `DETAILED`: "Write a detailed structured summary with headings."
- `ACTION_ITEMS`: "Extract all action items as a bullet list."
- `QA`: "Generate 5 question-and-answer pairs from the following."

---

## 8. File Inventory

### New Files

| File | Location | Purpose |
|------|----------|---------|
| `UnifiedProviderFactory.kt` | `Modules/AI/ALC/src/commonMain/.../provider/` | Single provider entry point |
| `AndroidMemoryStore.kt` | `Modules/AI/Memory/src/androidMain/.../memory/` | SQLDelight-backed IMemoryStore |
| `AIHandler.kt` | `Modules/AI/src/androidMain/.../handler/` | VoiceOS AI command handler |
| `AiSummaryPanel.kt` | `Modules/Cockpit/src/androidMain/.../content/` | Cockpit AI summary composable |

### Modified Files

| File | Change |
|------|--------|
| `ActionCategory.kt` | Add `AI(priority = 15)` |
| `CommandActionType.kt` | Add 5 AI action types |
| `AndroidHandlerFactory.kt` | Register `AIHandler` (12th handler) |
| `ActionCoordinator.kt` | Add 5 AI when-branches |
| `ResponseCoordinator.kt` | Use `UnifiedProviderFactory`; add `summarizeStreaming()` |
| `ConversationManager.kt` | Inject `IMemoryStore`; persist messages |
| `ContentRenderer.kt` | Wire `AiSummaryPanel` for `FrameContent.AiSummary` |
| LLM duplicate providers (×4) | Add `@Deprecated` annotations |
| `memory_entry.sq` | New SQLDelight table (if not present) |

### VOS Files (5 locales)

| File | Additions |
|------|-----------|
| `assets/vos/en-US.app.vos` | 5 AI commands |
| `assets/vos/es-ES.app.vos` | 5 AI commands (Spanish) |
| `assets/vos/fr-FR.app.vos` | 5 AI commands (French) |
| `assets/vos/de-DE.app.vos` | 5 AI commands (German) |
| `assets/vos/hi-IN.app.vos` | 5 AI commands (Hindi) |

---

## 9. Testing Checklist

### Unit Tests

| Component | Test |
|-----------|------|
| `UnifiedProviderFactory` | Cloud routing returns ALC provider; on-device routing returns expect-actual engine |
| `AndroidMemoryStore` | store → retrieve round-trip; keyword search matches; clear zeros token count |
| `AIHandler` | `canHandle(AI)` returns true; `canHandle(NAVIGATION)` returns false; each action type routes correctly |
| `ResponseCoordinator.summarizeStreaming` | Flow emits tokens; cancellation cleans up provider |

### Integration Tests

| Scenario | Expected |
|----------|----------|
| Voice command "summarize this" → `AI_SUMMARIZE` | `AIHandler.handleSummarize()` called, response sent back via TTS |
| Voice command "clear chat" → `AI_CLEAR_CONTEXT` | `ConversationManager.clearContext()` called, memory wiped |
| `AiSummaryPanel` with two source frames | Collects both frames' text, calls `summarizeStreaming`, renders tokens |
| Process restart with prior conversation | `ConversationManager` restores history from `AndroidMemoryStore` |

### Manual Verification

| Check | Method |
|-------|--------|
| Deprecated LLM providers emit warnings | Build with `-Werror` disabled, check IDE warnings at call sites |
| VOS command count: 107 + 5 = 112 per locale | Run VosParser on each locale file, assert count |
| AiSummaryPanel streaming cursor visible | Run on emulator, observe Cockpit AI frame while generating |
| Auto-refresh triggers on source frame edit | Edit a Note frame linked to an AI summary frame |

---

## 10. Dependencies

No new libraries required. All dependencies are already present in the project:

| Library | Already Used By |
|---------|----------------|
| `SQLDelight` | Database module (all modules) |
| `kotlinx.coroutines.flow` | ResponseCoordinator (Chat module) |
| `Dagger Hilt` | All androidMain modules |
| ALC provider classes | ALC sub-module |

---

## 11. Implementation Order

Execute phases in this order to avoid forward dependencies:

```
Phase 1 (Provider Unification)
    ↓ ResponseCoordinator now uses UnifiedProviderFactory
Phase 2 (Memory androidMain)
    ↓ ConversationManager now injects IMemoryStore
Phase 3 (VoiceOS Wiring)
    ↓ AIHandler registered; VOS commands parseable
Phase 4 (Cockpit AiSummary)
    ↓ AiSummaryPanel calls summarizeStreaming (from Phase 1)
```

Phases 1 and 2 are independent of each other and can be implemented in parallel if using worktrees. Phases 3 and 4 both depend on Phase 1 being complete first.

---

## 12. Risk Assessment

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| LLM deprecated providers still called by unknown consumers | Low | `@Deprecated` with `ReplaceWith` — compilation succeeds, warnings visible |
| `memory_entry` table already exists with different schema | Low | Check Database module before adding; merge columns if needed |
| `AIHandler` Hilt injection fails (KSP2 incompatibility) | Medium | `ksp.useKSP2=false` already in `gradle.properties` — known fix in place |
| `summarizeStreaming` Flow leaks if frame is closed mid-stream | Medium | Use `LaunchedEffect` cancellation in `AiSummaryPanel` — Compose lifecycle handles it |
| VOS command count regression | Low | Assert count in VosParser test after each locale file edit |

---

## 13. Related Documentation

| Document | Path |
|----------|------|
| Chapter 95 — Handler Dispatch | `Docs/MasterDocs/NewAvanues-Developer-Manual/Developer-Manual-Chapter95-VOSDistributionAndHandlerDispatch.md` |
| Chapter 97 — Cockpit SpatialVoice | `Docs/MasterDocs/Cockpit/Developer-Manual-Chapter97-CockpitSpatialVoiceMultiWindow.md` |
| VOS Compact Format Plan | `docs/plans/VoiceOSCore/VoiceOSCore-Plan-VOSCompactFormat-260216-V1.md` |
| Six-Module Unified Spec | `docs/plans/Modules-Spec-SixModuleImplementation-260219-V1.md` |
| Session Handover | `Docs/handover/handover-260219-0100.md` |

---

**Document version:** V1
**Next revision trigger:** If Memory `memory_entry` table schema conflicts discovered, or if `AIHandler` Hilt graph requires restructure
