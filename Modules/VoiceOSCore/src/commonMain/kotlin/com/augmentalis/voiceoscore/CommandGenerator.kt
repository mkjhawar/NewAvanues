package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.ElementFingerprint
import com.augmentalis.voiceoscore.currentTimeMillis

/**
 * Command Generator - Creates voice commands from UI elements.
 *
 * Generates QuantizedCommand objects from ElementInfo during element extraction.
 * Designed for single-pass generation with minimal overhead.
 */
object CommandGenerator {

    private val PARSE_DESCRIPTION_DELIMITERS = listOf(":", "|", ",", ".")

    // ===== GARBAGE TEXT FILTERS =====
    // Patterns that indicate non-voice-command content
    // NOTE: These are language-agnostic patterns. Localized garbage words
    // should be added to LOCALIZED_GARBAGE_WORDS for each supported locale.

    /**
     * Words that when repeated indicate garbage text (e.g., "comma comma com")
     * Keyed by locale code (en, de, es, fr, etc.)
     */
    private val LOCALIZED_REPETITIVE_WORDS = mapOf(
        "en" to setOf("comma", "dot", "dash", "space", "tab", "enter", "null", "undefined", "nan", "true", "false"),
        "de" to setOf("komma", "punkt", "strich", "leerzeichen", "tab", "eingabe", "null", "undefiniert", "wahr", "falsch"),
        "es" to setOf("coma", "punto", "guion", "espacio", "tab", "enter", "nulo", "indefinido", "verdadero", "falso"),
        "fr" to setOf("virgule", "point", "tiret", "espace", "tab", "entrer", "nul", "indéfini", "vrai", "faux"),
        "zh" to setOf("逗号", "句号", "空格", "制表符", "回车", "空", "未定义", "真", "假"),
        "ja" to setOf("コンマ", "ピリオド", "スペース", "タブ", "エンター", "ヌル", "未定義", "真", "偽")
    )

    /**
     * Language-agnostic garbage patterns (work for any locale)
     */
    private val GARBAGE_PATTERNS = listOf(
        // CSS class-like patterns: "btn-primary", "flex-row"
        Regex("^[a-z]+(-[a-z]+){2,}$", RegexOption.IGNORE_CASE),
        // Base64/hash-like strings (long alphanumeric without spaces)
        Regex("^[A-Za-z0-9+/=]{20,}$"),
        // Hex strings
        Regex("^(0x)?[a-f0-9]{8,}$", RegexOption.IGNORE_CASE),
        // Just punctuation or whitespace (language-agnostic)
        Regex("^[\\s\\p{Punct}]+$"),
        // UUID-like patterns
        Regex("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$", RegexOption.IGNORE_CASE),
        // Package names (com.example.app)
        Regex("^[a-z]+\\.[a-z]+\\.[a-z]+", RegexOption.IGNORE_CASE),
        // Object toString patterns: [object Object], Object@hash
        Regex("^\\[?object\\s*\\w*\\]?$|^\\w+@[a-f0-9]+$", RegexOption.IGNORE_CASE)
    )

    /**
     * Known garbage exact matches (language-agnostic programming terms)
     */
    private val GARBAGE_EXACT = setOf(
        "undefined", "null", "nan", "NaN", "NULL",
        "[object object]", "[Object object]",
        "function", "error", "exception",
        "...", "---", "___", "true", "false", ""
    )

    /**
     * Get repetitive words for the current locale.
     * Falls back to English if locale not found.
     */
    private fun getRepetitiveWords(locale: String = "en"): Set<String> {
        val langCode = locale.take(2).lowercase()
        return LOCALIZED_REPETITIVE_WORDS[langCode] ?: LOCALIZED_REPETITIVE_WORDS["en"]!!
    }

    /**
     * Known navigation/action icon labels that are valid single-word commands.
     * These come from contentDescription on icon buttons.
     * Localized for multiple languages.
     */
    private val LOCALIZED_NAVIGATION_ICONS = mapOf(
        "en" to setOf(
            // Navigation
            "menu", "more", "options", "settings", "back", "forward", "home", "close",
            "refresh", "reload", "search", "filter", "sort",
            // Actions
            "add", "new", "create", "edit", "delete", "remove", "save", "cancel",
            "share", "send", "download", "upload", "attach", "copy", "paste",
            // Communication
            "call", "meet", "video", "camera", "mic", "mute", "unmute",
            "compose", "reply", "archive", "trash", "spam",
            // Media
            "play", "pause", "stop", "skip", "previous", "next", "volume",
            // Status
            "star", "favorite", "bookmark", "pin", "flag", "label",
            // Help
            "help", "info", "about", "feedback"
        ),
        "de" to setOf(
            "menü", "mehr", "optionen", "einstellungen", "zurück", "vorwärts", "startseite", "schließen",
            "aktualisieren", "suchen", "filtern", "sortieren",
            "hinzufügen", "neu", "erstellen", "bearbeiten", "löschen", "entfernen", "speichern", "abbrechen",
            "teilen", "senden", "herunterladen", "hochladen", "anhängen", "kopieren", "einfügen",
            "anrufen", "video", "kamera", "mikrofon", "stummschalten",
            "verfassen", "antworten", "archivieren", "papierkorb",
            "abspielen", "pause", "stopp", "überspringen", "vorherige", "nächste", "lautstärke",
            "stern", "favorit", "lesezeichen",
            "hilfe", "info", "über"
        ),
        "es" to setOf(
            "menú", "más", "opciones", "ajustes", "atrás", "adelante", "inicio", "cerrar",
            "actualizar", "buscar", "filtrar", "ordenar",
            "añadir", "nuevo", "crear", "editar", "eliminar", "quitar", "guardar", "cancelar",
            "compartir", "enviar", "descargar", "subir", "adjuntar", "copiar", "pegar",
            "llamar", "vídeo", "cámara", "micrófono", "silenciar",
            "redactar", "responder", "archivar", "papelera",
            "reproducir", "pausar", "detener", "saltar", "anterior", "siguiente", "volumen",
            "estrella", "favorito", "marcador",
            "ayuda", "información", "acerca"
        ),
        "fr" to setOf(
            "menu", "plus", "options", "paramètres", "retour", "avancer", "accueil", "fermer",
            "actualiser", "rechercher", "filtrer", "trier",
            "ajouter", "nouveau", "créer", "modifier", "supprimer", "retirer", "enregistrer", "annuler",
            "partager", "envoyer", "télécharger", "téléverser", "joindre", "copier", "coller",
            "appeler", "vidéo", "caméra", "micro", "muet",
            "composer", "répondre", "archiver", "corbeille",
            "lecture", "pause", "arrêter", "passer", "précédent", "suivant", "volume",
            "étoile", "favori", "signet",
            "aide", "info", "à propos"
        )
    )

    /**
     * Get navigation icon labels for the current locale.
     * Returns a combined set of current locale + English (fallback).
     */
    private fun getNavigationIcons(locale: String = "en"): Set<String> {
        val langCode = locale.take(2).lowercase()
        val localized = LOCALIZED_NAVIGATION_ICONS[langCode] ?: emptySet()
        val english = LOCALIZED_NAVIGATION_ICONS["en"]!!
        return localized + english  // Combine both for better coverage
    }

    /**
     * Check if an element is likely an icon button (small, clickable, has contentDescription).
     * Icon buttons should allow single-word commands.
     *
     * @param element The element to check
     * @param locale The locale for navigation icon matching (default: "en")
     */
    fun isIconElement(element: ElementInfo, locale: String = "en"): Boolean {
        // Must be clickable
        if (!element.isClickable) return false

        // Must have contentDescription but no visible text
        if (element.contentDescription.isBlank()) return false
        if (element.text.isNotBlank() && element.text != element.contentDescription) return false

        // Check if it's a recognized navigation icon (localized)
        val desc = element.contentDescription.lowercase().trim()
        val navIcons = getNavigationIcons(locale)
        if (navIcons.contains(desc)) return true

        // Check size - icons are typically small (< 100dp on each side)
        val bounds = element.bounds
        val maxIconSize = 150  // pixels, roughly 100dp
        if (bounds.width < maxIconSize && bounds.height < maxIconSize) return true

        // Check class name patterns (language-agnostic)
        val className = element.className.lowercase()
        if (className.contains("imagebutton") ||
            className.contains("iconbutton") ||
            className.contains("actionbutton") ||
            className.contains("fab")) return true

        return false
    }

    /**
     * Get the label for an icon element.
     * For icons, we use contentDescription directly without minimum word requirements.
     *
     * @param element The icon element
     * @return The icon label or null if not suitable
     */
    fun getIconLabel(element: ElementInfo): String? {
        if (!isIconElement(element)) return null

        val label = element.contentDescription.trim()
        if (label.isBlank() || isGarbageText(label)) return null

        return cleanLabel(label)
    }

    /**
     * Generate commands for navigation/icon elements.
     * These are toolbar icons, action buttons, etc. that have contentDescription
     * but no visible text. They get single-word commands like "Meet", "More", "Search".
     *
     * Icons without valid contentDescription get numbered commands for consistency.
     *
     * @param elements All elements on screen
     * @param packageName Host application package name
     * @return Pair of (labeled icon commands, numbered icon commands for unlabeled icons)
     */
    fun generateIconCommands(
        elements: List<ElementInfo>,
        packageName: String
    ): Pair<List<QuantizedCommand>, List<QuantizedCommand>> {
        // Find icon elements
        val iconElements = elements.filter { isIconElement(it) }

        val labeledCommands = mutableListOf<QuantizedCommand>()
        val unlabeledIcons = mutableListOf<ElementInfo>()

        iconElements.forEach { element ->
            val label = getIconLabel(element)

            if (label != null) {
                // Create labeled command for this icon
                val avid = generateAvid(element, packageName)
                labeledCommands.add(
                    QuantizedCommand(
                        avid = "",
                        phrase = label,
                        actionType = CommandActionType.CLICK,
                        targetAvid = avid,
                        confidence = 0.85f,
                        metadata = mapOf(
                            "packageName" to packageName,
                            "elementHash" to deriveElementHash(element),
                            "isIconCommand" to "true",
                            "contentDescription" to element.contentDescription,
                            "resourceId" to element.resourceId,
                            "className" to element.className,
                            "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}"
                        )
                    )
                )
            } else {
                // Icon without valid label - will get numbered
                unlabeledIcons.add(element)
            }
        }

        // Generate numbered commands for unlabeled icons
        // Sort by position (left to right, top to bottom for toolbar consistency)
        val sortedUnlabeled = unlabeledIcons.sortedWith(
            compareBy({ it.bounds.top / 50 }, { it.bounds.left })  // Group by row
        )

        val numberedCommands = sortedUnlabeled.mapIndexed { index, element ->
            val number = index + 1
            val avid = generateAvid(element, packageName)

            QuantizedCommand(
                avid = "",
                phrase = "icon $number",  // "icon 1", "icon 2" for unlabeled icons
                actionType = CommandActionType.CLICK,
                targetAvid = avid,
                confidence = 0.7f,
                metadata = mapOf(
                    "packageName" to packageName,
                    "elementHash" to deriveElementHash(element),
                    "isUnlabeledIconCommand" to "true",
                    "iconNumber" to number.toString(),
                    "resourceId" to element.resourceId,
                    "className" to element.className,
                    "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}"
                )
            )
        }

        return Pair(labeledCommands, numberedCommands)
    }

    /**
     * Check if text is garbage that should not be a voice command.
     *
     * @param text The text to check
     * @param locale The locale for language-specific garbage detection (default: "en")
     * @return true if the text is garbage and should be filtered out
     */
    fun isGarbageText(text: String, locale: String = "en"): Boolean {
        val trimmed = text.trim()

        // Empty or very short (single char)
        if (trimmed.length <= 1) return true

        // Exact matches (language-agnostic programming terms)
        if (GARBAGE_EXACT.any { it.equals(trimmed, ignoreCase = true) }) return true

        // Pattern matches (language-agnostic patterns)
        if (GARBAGE_PATTERNS.any { it.matches(trimmed) }) return true

        // Detect repetitive words: "comma comma com", "dot dot"
        val repetitiveWords = getRepetitiveWords(locale)
        val words = trimmed.lowercase().split(Regex("[\\s,]+")).filter { it.isNotBlank() }
        if (words.size >= 2) {
            val firstWord = words[0]
            // If most words start with the same prefix (like "comma, comma, com")
            val samePrefix = words.count { it.startsWith(firstWord.take(3)) }
            if (samePrefix >= 2 && repetitiveWords.any { firstWord.startsWith(it.take(3)) }) {
                return true
            }
        }

        return false
    }

    /**
     * Clean and validate label text for voice commands.
     * Returns null if the text is garbage.
     *
     * @param text Raw label text
     * @param locale The locale for garbage detection (default: "en")
     * @return Cleaned text or null if garbage
     */
    fun cleanLabel(text: String, locale: String = "en"): String? {
        val trimmed = text.trim()

        // Filter garbage
        if (isGarbageText(trimmed, locale)) return null

        // Truncate very long text (likely not a button label)
        val maxLength = 50
        return if (trimmed.length > maxLength) {
            trimmed.take(maxLength).substringBeforeLast(" ") + "..."
        } else {
            trimmed
        }
    }

    /**
     * Generate command from element during extraction (single pass).
     * Returns null if element is not actionable or has no useful label.
     *
     * Commands are stored WITHOUT verbs - the element label only (e.g., "4", "More options").
     * User provides the verb at runtime: "click 4", "tap More options", or just "4".
     *
     * @param element Source element info
     * @param packageName Host application package name
     * @return QuantizedCommand or null if element is not suitable for voice command
     */
    /**
     * Result of command generation containing both the command and persistence flag.
     */
    data class GeneratedCommandResult(
        val command: QuantizedCommand,
        val shouldPersist: Boolean,
        val listIndex: Int = -1  // Position in list for index-based commands
    )

    fun fromElement(
        element: ElementInfo,
        packageName: String
    ): QuantizedCommand? {
        return fromElementWithPersistence(element, packageName)?.command
    }

    /**
     * Generate command from element with full persistence decision using 4-layer system.
     *
     * This is the preferred method that uses PersistenceDecisionEngine to make intelligent
     * persistence decisions based on app category, container behavior, screen type, and content analysis.
     *
     * @param element Source element info
     * @param packageName Host application package name
     * @param allElements All elements on screen (for ScreenClassifier)
     * @return GeneratedCommandResult with persistence decision from PersistenceDecisionEngine
     */
    fun fromElementWithPersistence(
        element: ElementInfo,
        packageName: String,
        allElements: List<ElementInfo>
    ): GeneratedCommandResult? {
        // Skip non-actionable elements
        if (!element.isActionable) return null
        if (!element.hasVoiceContent) return null

        val label = deriveLabel(element)
        if (label.isBlank() || label == element.className.substringAfterLast(".")) {
            return null
        }

        // Use PersistenceDecisionEngine for 4-layer decision
        val decision = PersistenceDecisionEngine.decideForElement(element, packageName, allElements)

        val actionType = deriveActionType(element)
        val elementHash = deriveElementHash(element)
        val avid = generateAvid(element, packageName)
        val currentTime = currentTimeMillis()

        val command = QuantizedCommand(
            avid = "",
            phrase = label,
            actionType = actionType,
            targetAvid = avid,
            confidence = decision.confidence,  // Use decision confidence
            metadata = mapOf(
                "packageName" to packageName,
                "elementHash" to elementHash,
                "createdAt" to currentTime.toString(),
                "className" to element.className,
                "resourceId" to element.resourceId,
                "label" to label,
                "isDynamic" to (!decision.shouldPersist).toString(),
                "persistenceRule" to decision.ruleApplied.toString(),
                "persistenceReason" to decision.reason,
                "listIndex" to element.listIndex.toString(),
                "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
                // NAV-500 Fix #2: Scroll tracking metadata for BoundsResolver
                "containerId" to element.containerResourceId,
                "scrollOffset" to "${element.scrollOffsetX},${element.scrollOffsetY}"
            )
        )

        return GeneratedCommandResult(
            command = command,
            shouldPersist = decision.shouldPersist,
            listIndex = element.listIndex
        )
    }

    /**
     * Generate command from element with persistence information using legacy detection.
     * Returns both the command and whether it should be persisted to database.
     *
     * @param element Source element info
     * @param packageName Host application package name
     * @return GeneratedCommandResult or null if element is not suitable
     */
    @Deprecated(
        message = "Use fromElementWithPersistence(element, packageName, allElements) for 4-layer persistence decision",
        replaceWith = ReplaceWith("fromElementWithPersistence(element, packageName, allElements)")
    )
    fun fromElementWithPersistence(
        element: ElementInfo,
        packageName: String
    ): GeneratedCommandResult? {
        // Skip non-actionable elements
        if (!element.isActionable) return null

        // Skip elements without voice content
        if (!element.hasVoiceContent) return null

        // Get label and skip if it's just the class name
        val label = deriveLabel(element)
        if (label.isBlank() || label == element.className.substringAfterLast(".")) {
            return null
        }

        val actionType = deriveActionType(element)
        // Note: verb is NOT included in phrase - user provides it at runtime

        // Generate element hash for database FK reference
        val elementHash = deriveElementHash(element)
        val avid = generateAvid(element, packageName)
        val currentTime = currentTimeMillis()

        // Determine if this is dynamic content (should NOT be persisted)
        val isDynamic = element.isDynamicContent

        val command = QuantizedCommand(
            avid = "", // Generated on persist if needed
            phrase = label,  // No verb - just the element label
            actionType = actionType,
            targetAvid = avid,
            confidence = calculateConfidence(element),
            metadata = mapOf(
                "packageName" to packageName,
                "elementHash" to elementHash,
                "createdAt" to currentTime.toString(),
                "className" to element.className,
                "resourceId" to element.resourceId,
                "label" to label,
                "isDynamic" to isDynamic.toString(),
                "listIndex" to element.listIndex.toString(),
                "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
                // NAV-500 Fix #2: Scroll tracking metadata for BoundsResolver
                "containerId" to element.containerResourceId,
                "scrollOffset" to "${element.scrollOffsetX},${element.scrollOffsetY}"
            )
        )

        return GeneratedCommandResult(
            command = command,
            shouldPersist = !isDynamic,
            listIndex = element.listIndex
        )
    }

    /**
     * Generate index-based commands for list items.
     * Creates commands like "first", "second", "item 3" for dynamic list navigation.
     *
     * IMPORTANT: Generates exactly ONE command per unique listIndex to prevent duplicates.
     * When multiple elements share the same listIndex (e.g., a row and its children),
     * we keep the best representative element (prefer clickable, in dynamic container).
     *
     * FIX: Only generates commands for ACTIONABLE elements. Elements that are just
     * labels or text views without click handlers are skipped to prevent generating
     * useless commands that flood the speech engine grammar.
     *
     * @param listItems Elements that are list items (have listIndex >= 0)
     * @param packageName Host application package name
     * @return List of index commands (in-memory only, never persisted)
     */
    fun generateListIndexCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> {
        val ordinals = listOf("first", "second", "third", "fourth", "fifth",
            "sixth", "seventh", "eighth", "ninth", "tenth")

        // Group by listIndex and keep only the best ACTIONABLE element per index
        // FIX: Filter for actionable elements BEFORE grouping to ensure we only
        // generate commands for elements that can actually be clicked
        val bestElementPerIndex = listItems
            .filter { it.listIndex >= 0 && (it.isClickable || it.isLongClickable) }
            .groupBy { it.listIndex }
            .mapValues { (_, elements) ->
                // Prefer: clickable > has content > in dynamic container > first
                elements.maxByOrNull { element ->
                    var score = 0
                    if (element.isClickable) score += 100
                    if (element.isInDynamicContainer) score += 50
                    if (element.text.isNotBlank() || element.contentDescription.isNotBlank()) score += 25
                    if (element.resourceId.isNotBlank()) score += 10
                    score
                }
            }
            .values
            .filterNotNull()

        return bestElementPerIndex.map { element ->
            val index = element.listIndex
            val phrase = when {
                index < ordinals.size -> ordinals[index]
                else -> "item ${index + 1}"
            }

            val avid = generateAvid(element, packageName)

            // Derive label for fallback search - prioritize text, then contentDescription
            val label = when {
                element.text.isNotBlank() -> element.text
                element.contentDescription.isNotBlank() -> element.contentDescription
                else -> ""
            }

            QuantizedCommand(
                avid = "",
                phrase = phrase,
                actionType = CommandActionType.CLICK,
                targetAvid = avid,
                confidence = 0.7f,
                metadata = mapOf(
                    "packageName" to packageName,
                    "elementHash" to deriveElementHash(element),
                    "isIndexCommand" to "true",
                    "listIndex" to index.toString(),
                    "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
                    // Additional metadata for BoundsResolver fallback layers
                    "label" to label,
                    "contentDescription" to element.contentDescription,
                    "resourceId" to element.resourceId,
                    "className" to element.className,
                    // NAV-500 Fix #2: Scroll tracking metadata
                    "containerId" to element.containerResourceId,
                    "scrollOffset" to "${element.scrollOffsetX},${element.scrollOffsetY}"
                )
            )
        }
    }

    /**
     * Extract short sender/title from dynamic content for voice targeting.
     * E.g., "Unread, , , Arby's, , BOGO..." → "Arby's"
     *
     * @param element Element with dynamic content
     * @return Short label suitable for voice command, or null if not extractable
     */
    fun extractShortLabel(element: ElementInfo): String? {
        val text = element.text.ifBlank { element.contentDescription }
        if (text.isBlank()) return null

        // Early garbage check - filter out "comma comma com" etc.
        if (isGarbageText(text)) return null

        // Email pattern: "Unread, , , SenderName, , Subject..."
        if (text.startsWith("Unread,")) {
            val parts = text.split(",").map { it.trim() }
            // Find first non-empty part after "Unread"
            for (i in 1 until parts.size) {
                if (parts[i].isNotBlank() && parts[i].length in 2..30 && !isGarbageText(parts[i])) {
                    return cleanLabel(parts[i])
                }
            }
        }

        val normalizeRealWearMlScript = normalizeRealWearMlScript(text)

        // Check garbage after normalization too
        if (isGarbageText(normalizeRealWearMlScript)) return null

        val result = if (SymbolNormalizer.containsSymbols(normalizeRealWearMlScript)) {
            SymbolNormalizer.normalize(normalizeRealWearMlScript)
        } else {
            normalizeRealWearMlScript
        }

        return cleanLabel(result)
    }

    /**
     * Generate numeric commands for list items.
     * Creates commands with raw numbers "1", "2", "3"... so users can simply say
     * the number shown in the overlay badge to click that item.
     *
     * IMPORTANT: This is SEPARATE from generateListIndexCommands() which creates
     * ordinal words ("first", "second"). Numeric commands are what users see
     * in the numbered overlay badges.
     *
     * FIX: Only generates commands for ACTIONABLE elements. Elements that are just
     * labels or text views without click handlers are skipped.
     *
     * @param listItems Elements that are list items (have listIndex >= 0)
     * @param packageName Host application package name
     * @return List of numeric commands (in-memory only, never persisted)
     */
    fun generateNumericCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> {
        // Group by listIndex and keep only the best ACTIONABLE element per index
        // FIX: Filter for actionable elements BEFORE grouping
        val bestElementPerIndex = listItems
            .filter { it.listIndex >= 0 && (it.isClickable || it.isLongClickable) }
            .groupBy { it.listIndex }
            .mapValues { (_, elements) ->
                // Prefer: clickable > has content > in dynamic container > first
                elements.maxByOrNull { element ->
                    var score = 0
                    if (element.isClickable) score += 100
                    if (element.isInDynamicContainer) score += 50
                    if (element.text.isNotBlank() || element.contentDescription.isNotBlank()) score += 25
                    if (element.resourceId.isNotBlank()) score += 10
                    score
                }
            }
            .values
            .filterNotNull()
            .sortedBy { it.listIndex }

        return bestElementPerIndex.mapIndexed { visualIndex, element ->
            // Visual index is 1-based (matches overlay badge numbers)
            val number = visualIndex + 1
            val avid = generateAvid(element, packageName)

            // Derive label for fallback search - prioritize text, then contentDescription
            val label = when {
                element.text.isNotBlank() -> element.text
                element.contentDescription.isNotBlank() -> element.contentDescription
                else -> ""
            }

            QuantizedCommand(
                avid = "",
                phrase = number.toString(),  // Raw number: "1", "2", "3"
                actionType = CommandActionType.CLICK,
                targetAvid = avid,
                confidence = 0.9f,  // High confidence for numeric commands
                metadata = mapOf(
                    "packageName" to packageName,
                    "elementHash" to deriveElementHash(element),
                    "isNumericCommand" to "true",
                    "numericIndex" to number.toString(),
                    "listIndex" to element.listIndex.toString(),
                    "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
                    // Additional metadata for BoundsResolver fallback layers
                    "label" to label,
                    "contentDescription" to element.contentDescription,
                    "resourceId" to element.resourceId,
                    "className" to element.className,
                    // NAV-500 Fix #2: Scroll tracking metadata
                    "containerId" to element.containerResourceId,
                    "scrollOffset" to "${element.scrollOffsetX},${element.scrollOffsetY}"
                )
            )
        }
    }

    /**
     * Generate label-based commands for list items.
     * Creates commands using the extracted label (sender name, title, etc.)
     * so users can say "Lifemiles" instead of just "first".
     *
     * FIX: Only generates commands for ACTIONABLE elements. Elements that are just
     * labels or text views without click handlers are skipped.
     *
     * @param listItems Elements that are list items (have listIndex >= 0)
     * @param packageName Host application package name
     * @return List of label commands (in-memory only, never persisted)
     */
    fun generateListLabelCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> {
        val seenLabels = mutableSetOf<String>()

        // FIX: Filter for actionable elements to prevent generating commands for labels
        return listItems.filter { it.listIndex >= 0 && (it.isClickable || it.isLongClickable) }.mapNotNull { element ->
            val label = extractShortLabel(element)

            // Skip if no label extracted or label already seen (avoid duplicates)
            if (label.isNullOrBlank()) return@mapNotNull null

            val normalizedLabel = label.lowercase().trim()
            if (normalizedLabel in seenLabels) return@mapNotNull null
            seenLabels.add(normalizedLabel)

            // Skip very short labels (likely noise) or very long ones
            if (label.length < 2 || label.length > 30) return@mapNotNull null

            val avid = generateAvid(element, packageName)

            QuantizedCommand(
                avid = "",
                phrase = label,  // The sender name/title as command
                actionType = CommandActionType.CLICK,
                targetAvid = avid,
                confidence = 0.8f,  // Higher confidence for label matches
                metadata = mapOf(
                    "packageName" to packageName,
                    "elementHash" to deriveElementHash(element),
                    "isLabelCommand" to "true",
                    "listIndex" to element.listIndex.toString(),
                    "originalLabel" to label,
                    "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
                    // NAV-500 Fix #2: Scroll tracking metadata
                    "containerId" to element.containerResourceId,
                    "scrollOffset" to "${element.scrollOffsetX},${element.scrollOffsetY}"
                )
            )
        }
    }

    /**
     * Derive a stable element hash for database FK reference.
     * Uses the same logic as generateAvid's elementHash for consistency.
     */
    private fun deriveElementHash(element: ElementInfo): String {
        val hashInput = when {
            element.resourceId.isNotBlank() -> element.resourceId
            element.contentDescription.isNotBlank() -> element.contentDescription
            element.text.isNotBlank() -> element.text
            else -> "${element.className}:${element.bounds}"
        }
        // Return a truncated hash suitable for database storage
        return hashInput.hashCode().toUInt().toString(16).padStart(8, '0')
    }

    /**
     * Derive the best label for voice recognition from element properties.
     * Normalizes special characters to speech-friendly equivalents.
     * Filters out garbage text that shouldn't be voice commands.
     *
     * @param element Source element
     * @param locale Locale for symbol normalization (default: "en")
     * @return Normalized label suitable for voice recognition, empty if garbage
     */
    private fun deriveLabel(element: ElementInfo, locale: String = "en"): String {
        val rawLabel = when {
            element.text.isNotBlank() -> element.text
            element.contentDescription.isNotBlank() -> element.contentDescription
            element.resourceId.isNotBlank() -> {
                element.resourceId
                    .substringAfterLast("/")
                    .replace("_", " ")
                    .replace("-", " ")
            }

            else -> ""
        }

        // Early garbage check
        if (isGarbageText(rawLabel)) return ""

        val normalizeRealWearMlScript = normalizeRealWearMlScript(rawLabel)

        // Check garbage after normalization
        if (isGarbageText(normalizeRealWearMlScript)) return ""

        // Normalize special characters (e.g., "&" → "and", "#" → "pound")
        val result = if (SymbolNormalizer.containsSymbols(normalizeRealWearMlScript)) {
            SymbolNormalizer.normalize(normalizeRealWearMlScript, locale)
        } else {
            normalizeRealWearMlScript
        }

        // Final cleanup - return empty if still garbage
        return cleanLabel(result) ?: ""
    }

    /**
    * Normalizes a RealWear ML script string by trimming the input around the first supported delimiter.
    *
    * This function searches for the first delimiter (in the order defined by [PARSE_DESCRIPTION_DELIMITERS])
    * that appears in [text]. If a delimiter is found, the input is split into exactly two parts using
    * `split(delimiter, limit = 2)`.
    *
    * Selection logic:
    * - If the original [text] contains `"hf_"`, the function returns the substring *after* the first
    *   encountered delimiter (i.e., `parts[1]`).
    * - Otherwise, it returns the substring *before* the first encountered delimiter (i.e., `parts[0]`).
    *
    * If no delimiter from [PARSE_DESCRIPTION_DELIMITERS] is present, the input is returned unchanged.
    *
    * Notes / edge cases:
    * - If the delimiter exists but is at the beginning or end of the string, the returned value may be
    *   an empty string.
    * - The delimiter search is order-dependent: if multiple delimiters are present, only the first match
    *   according to [PARSE_DESCRIPTION_DELIMITERS] is used.
    * - The `"hf_"` check is performed on the original [text], not on the split parts.
    *
    * @param text Raw ML script or description text to normalize.
    * @return A normalized string segment based on the delimiter and `"hf_"` presence rules.
    */
    private fun normalizeRealWearMlScript(text: String): String {
        // Find the first delimiter from our list that exists in the current processedText
        val foundDelimiter = PARSE_DESCRIPTION_DELIMITERS.firstOrNull { text.contains(it) }

        if (foundDelimiter != null) {
            // If a delimiter is found, split the string.
            // limit = 2 ensures we get exactly two parts if the delimiter is present.
            val parts = text.split(foundDelimiter, limit = 2)

            val normalizedText = if ("hf_" in text) { // Condition checks the input `text` (normalized version)
                // If "hf_" is present, take the part *after* the first delimiter
                parts[1]
            } else {
                // If "hf_" is NOT present, take the part *before* the first delimiter
                parts[0]
            }
            return normalizedText
        }
        return text
    }

    /**
     * Derive action type based on element properties.
     */
    private fun deriveActionType(element: ElementInfo): CommandActionType {
        val className = element.className.lowercase()
        return when {
            className.contains("edittext") || className.contains("textfield") -> CommandActionType.TYPE
            className.contains("checkbox") || className.contains("switch") -> CommandActionType.CLICK
            className.contains("button") -> CommandActionType.CLICK
            element.isScrollable -> CommandActionType.CLICK // Keep as click for scrollable items
            element.isClickable -> CommandActionType.CLICK
            else -> CommandActionType.CLICK
        }
    }

    /**
     * Generate element fingerprint for targeting.
     * Format: {TypeCode}:{hash8} e.g., "BTN:a3f2e1c9"
     */
    private fun generateAvid(element: ElementInfo, packageName: String): String {
        return ElementFingerprint.fromElementInfo(element, packageName)
    }

    /**
     * Calculate confidence score based on element identifiers.
     * Higher confidence for elements with more identifying information.
     */
    private fun calculateConfidence(element: ElementInfo): Float {
        var confidence = 0.5f

        // Boost for having resourceId (most reliable)
        if (element.resourceId.isNotBlank()) {
            confidence += 0.2f
        }

        // Boost for content description (accessibility info)
        if (element.contentDescription.isNotBlank()) {
            confidence += 0.15f
        }

        // Boost for reasonable text length (not too short, not too long)
        val label = deriveLabel(element)
        if (label.length in 2..20) {
            confidence += 0.1f
        }

        // Small boost for being clickable
        if (element.isClickable) {
            confidence += 0.05f
        }

        return confidence.coerceIn(0f, 1f)
    }

    /**
     * Extension function to get verb for action type.
     */
    private fun CommandActionType.verb(): String = when (this) {
        CommandActionType.CLICK -> "click"
        CommandActionType.TAP -> "tap"
        CommandActionType.LONG_CLICK -> "hold"
        CommandActionType.EXECUTE -> "execute"
        CommandActionType.TYPE -> "type"
        CommandActionType.FOCUS -> "focus"
        CommandActionType.SCROLL_DOWN -> "scroll down"
        CommandActionType.SCROLL_UP -> "scroll up"
        CommandActionType.SCROLL_LEFT -> "scroll left"
        CommandActionType.SCROLL_RIGHT -> "scroll right"
        CommandActionType.SCROLL -> "scroll"
        CommandActionType.NAVIGATE -> "go to"
        CommandActionType.CUSTOM -> "activate"
        else -> "activate"
    }
}
