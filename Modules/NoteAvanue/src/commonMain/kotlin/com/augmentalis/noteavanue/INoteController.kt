package com.augmentalis.noteavanue

import com.augmentalis.noteavanue.model.NoteEditorState
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic rich note controller interface.
 *
 * Abstracts the compose-rich-editor operations so that voice command handlers
 * and Cockpit frame controllers can manipulate note content without direct
 * Compose dependency. The Android/Desktop implementations wrap RichTextState.
 *
 * Usage from voice pipeline:
 * ```kotlin
 * controller.toggleBold()         // "note bold"
 * controller.setHeadingLevel(1)   // "heading one"
 * controller.undo()               // "note undo"
 * ```
 */
interface INoteController {

    /** Observable editor state (note, dirty flag, saving indicator, voice mode) */
    val state: StateFlow<NoteEditorState>

    // ═══════════════════════════════════════════════════════════════════
    // Formatting
    // ═══════════════════════════════════════════════════════════════════

    /** Toggle bold on current selection or at cursor */
    fun toggleBold()

    /** Toggle italic on current selection or at cursor */
    fun toggleItalic()

    /** Toggle underline on current selection or at cursor */
    fun toggleUnderline()

    /** Toggle strikethrough on current selection or at cursor */
    fun toggleStrikethrough()

    /** Set heading level (1–3) on current paragraph, 0 to remove heading */
    fun setHeadingLevel(level: Int)

    /** Toggle unordered (bullet) list */
    fun toggleBulletList()

    /** Toggle ordered (numbered) list */
    fun toggleNumberedList()

    /** Toggle task/checklist item */
    fun toggleChecklist()

    /** Toggle code block on current paragraph */
    fun toggleCodeBlock()

    /** Toggle blockquote on current paragraph */
    fun toggleBlockquote()

    /** Insert a horizontal divider at cursor */
    fun insertDivider()

    // ═══════════════════════════════════════════════════════════════════
    // Navigation
    // ═══════════════════════════════════════════════════════════════════

    /** Move cursor to the beginning of the document */
    fun goToTop()

    /** Move cursor to the end of the document */
    fun goToBottom()

    /** Move cursor to the next heading */
    fun nextHeading()

    /** Move cursor to the previous heading */
    fun previousHeading()

    // ═══════════════════════════════════════════════════════════════════
    // Editing
    // ═══════════════════════════════════════════════════════════════════

    /** Undo last change */
    fun undo()

    /** Redo last undone change */
    fun redo()

    /** Select all text */
    fun selectAll()

    /** Delete the current line/paragraph */
    fun deleteLine()

    /** Insert a new paragraph at cursor */
    fun insertParagraph()

    // ═══════════════════════════════════════════════════════════════════
    // Content
    // ═══════════════════════════════════════════════════════════════════

    /** Load Markdown content into the editor */
    fun setMarkdown(md: String)

    /** Export current content as Markdown */
    fun getMarkdown(): String

    /** Insert text at current cursor position (used by dictation pipeline) */
    fun insertText(text: String)

    // ═══════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════════

    /** Save the current note (triggers auto-save flush) */
    suspend fun save()

    /** Release editor resources */
    fun release()
}
