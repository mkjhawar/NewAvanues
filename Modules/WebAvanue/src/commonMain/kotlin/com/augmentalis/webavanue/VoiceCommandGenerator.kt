package com.augmentalis.webavanue

/**
 * Generates voice commands from DOM elements with flexible word-based matching.
 *
 * Matching Logic:
 * - Minimum 2 words required to trigger matching
 * - Progressive matching: more words = more specific match
 * - Returns all matches for NLU disambiguation when multiple matches exist
 *
 * Example:
 * ```kotlin
 * val generator = VoiceCommandGenerator()
 * generator.addElements(domScrapeResult.elements)
 *
 * // User says "Greenland flatly"
 * val matches = generator.findMatches("Greenland flatly")
 * when {
 *     matches.size == 1 -> execute(matches[0])
 *     matches.size > 1 -> askNluToDisambiguate(matches)
 *     else -> notFound()
 * }
 * ```
 */
class VoiceCommandGenerator {

    companion object {
        const val MIN_WORDS_FOR_MATCH = 2
        const val MIN_WORDS_FOR_ICON = 1  // Icons can have single-word commands
        const val MAX_COMMAND_WORDS = 10

        // ===== GARBAGE TEXT FILTERS =====
        // Multi-language support for garbage detection

        /**
         * Words that when repeated indicate garbage text.
         * Localized for multiple languages.
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
         * Language-agnostic garbage patterns
         */
        private val GARBAGE_PATTERNS = listOf(
            // CSS class-like patterns
            Regex("^[a-z]+(-[a-z]+){2,}$", RegexOption.IGNORE_CASE),
            // Base64/hash-like strings
            Regex("^[A-Za-z0-9+/=]{20,}$"),
            // Hex strings
            Regex("^(0x)?[a-f0-9]{8,}$", RegexOption.IGNORE_CASE),
            // Just punctuation
            Regex("^[\\s\\p{Punct}]+$"),
            // UUID patterns
            Regex("^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$", RegexOption.IGNORE_CASE),
            // Object patterns
            Regex("^\\[?object\\s*\\w*\\]?$|^\\w+@[a-f0-9]+$", RegexOption.IGNORE_CASE)
        )

        /**
         * Known garbage exact matches (language-agnostic programming terms)
         */
        private val GARBAGE_EXACT = setOf(
            "undefined", "null", "nan", "NaN", "NULL",
            "[object object]", "function", "error", "exception",
            "...", "---", "___", "true", "false", ""
        )

        /**
         * Known navigation/action icon labels (localized)
         */
        private val LOCALIZED_NAVIGATION_ICONS = mapOf(
            "en" to setOf(
                "menu", "more", "options", "settings", "back", "forward", "home", "close",
                "refresh", "reload", "search", "filter", "sort",
                "add", "new", "create", "edit", "delete", "remove", "save", "cancel",
                "share", "send", "download", "upload", "attach", "copy", "paste",
                "call", "meet", "video", "camera", "mic", "mute", "unmute",
                "compose", "reply", "archive", "trash", "spam",
                "play", "pause", "stop", "skip", "previous", "next", "volume",
                "star", "favorite", "bookmark", "pin", "flag", "label",
                "help", "info", "about", "feedback", "login", "logout", "profile"
            ),
            "de" to setOf(
                "menü", "mehr", "optionen", "einstellungen", "zurück", "vorwärts", "startseite", "schließen",
                "aktualisieren", "suchen", "filtern", "sortieren",
                "hinzufügen", "neu", "erstellen", "bearbeiten", "löschen", "entfernen", "speichern", "abbrechen",
                "teilen", "senden", "herunterladen", "hochladen", "anhängen", "kopieren", "einfügen",
                "anrufen", "video", "kamera", "mikrofon", "stummschalten",
                "abspielen", "pause", "stopp", "stern", "favorit", "lesezeichen",
                "hilfe", "info", "anmelden", "abmelden", "profil"
            ),
            "es" to setOf(
                "menú", "más", "opciones", "ajustes", "atrás", "adelante", "inicio", "cerrar",
                "actualizar", "buscar", "filtrar", "ordenar",
                "añadir", "nuevo", "crear", "editar", "eliminar", "quitar", "guardar", "cancelar",
                "compartir", "enviar", "descargar", "subir", "adjuntar", "copiar", "pegar",
                "llamar", "vídeo", "cámara", "micrófono", "silenciar",
                "reproducir", "pausar", "detener", "estrella", "favorito", "marcador",
                "ayuda", "información", "iniciar sesión", "cerrar sesión", "perfil"
            ),
            "fr" to setOf(
                "menu", "plus", "options", "paramètres", "retour", "avancer", "accueil", "fermer",
                "actualiser", "rechercher", "filtrer", "trier",
                "ajouter", "nouveau", "créer", "modifier", "supprimer", "retirer", "enregistrer", "annuler",
                "partager", "envoyer", "télécharger", "téléverser", "joindre", "copier", "coller",
                "appeler", "vidéo", "caméra", "micro", "muet",
                "lecture", "pause", "arrêter", "étoile", "favori", "signet",
                "aide", "info", "connexion", "déconnexion", "profil"
            )
        )

        private fun getRepetitiveWords(locale: String = "en"): Set<String> {
            val langCode = locale.take(2).lowercase()
            return LOCALIZED_REPETITIVE_WORDS[langCode] ?: LOCALIZED_REPETITIVE_WORDS["en"]!!
        }

        private fun getNavigationIcons(locale: String = "en"): Set<String> {
            val langCode = locale.take(2).lowercase()
            val localized = LOCALIZED_NAVIGATION_ICONS[langCode] ?: emptySet()
            val english = LOCALIZED_NAVIGATION_ICONS["en"]!!
            return localized + english
        }

        /**
         * Icon element types in HTML
         */
        private val ICON_ELEMENT_TYPES = setOf(
            "button", "menuitem", "tab", "link"
        )

        /**
         * Check if text is garbage that should not be a voice command.
         *
         * @param text The text to check
         * @param locale The locale for language-specific garbage detection (default: "en")
         */
        fun isGarbageText(text: String, locale: String = "en"): Boolean {
            val trimmed = text.trim()
            if (trimmed.length <= 1) return true
            if (GARBAGE_EXACT.any { it.equals(trimmed, ignoreCase = true) }) return true
            if (GARBAGE_PATTERNS.any { it.matches(trimmed) }) return true

            // Detect repetitive words (localized)
            val repetitiveWords = getRepetitiveWords(locale)
            val words = trimmed.lowercase().split(Regex("[\\s,]+")).filter { it.isNotBlank() }
            if (words.size >= 2) {
                val firstWord = words[0]
                val samePrefix = words.count { it.startsWith(firstWord.take(3)) }
                if (samePrefix >= 2 && repetitiveWords.any { firstWord.startsWith(it.take(3)) }) {
                    return true
                }
            }
            return false
        }

        /**
         * Clean label text for voice commands.
         *
         * @param text Raw label text
         * @param locale The locale for garbage detection (default: "en")
         */
        fun cleanLabel(text: String, locale: String = "en"): String? {
            val trimmed = text.trim()
            if (isGarbageText(trimmed, locale)) return null
            val maxLength = 50
            return if (trimmed.length > maxLength) {
                trimmed.take(maxLength).substringBeforeLast(" ") + "..."
            } else {
                trimmed
            }
        }

        /**
         * Check if text is a known navigation icon label.
         *
         * @param text The text to check
         * @param locale The locale for navigation icon matching (default: "en")
         */
        fun isNavigationIcon(text: String, locale: String = "en"): Boolean {
            val navIcons = getNavigationIcons(locale)
            return navIcons.contains(text.lowercase().trim())
        }
    }

    private val commands = mutableListOf<WebVoiceCommand>()
    private val wordIndex = mutableMapOf<String, MutableList<WebVoiceCommand>>()

    /**
     * A voice command for a web element.
     */
    data class WebVoiceCommand(
        val vosId: String,
        val elementType: String,
        val fullText: String,
        val words: List<String>,
        val selector: String,
        val xpath: String,
        val bounds: ElementBounds,
        val action: CommandAction,
        val metadata: Map<String, String> = emptyMap()
    )

    /**
     * Possible actions for a command.
     */
    enum class CommandAction {
        CLICK,
        FOCUS,
        INPUT,
        SCROLL_TO,
        TOGGLE,
        SELECT
    }

    /**
     * Result of a match operation.
     */
    data class MatchResult(
        val command: WebVoiceCommand,
        val matchedWords: Int,
        val confidence: Float
    )

    /**
     * Clear all commands.
     */
    fun clear() {
        commands.clear()
        wordIndex.clear()
    }

    /**
     * Add DOM elements and generate voice commands.
     */
    fun addElements(elements: List<DOMElement>) {
        elements.forEach { element ->
            val command = createCommand(element)
            if (command != null) {
                // Allow single-word commands for icons, require 2+ words for regular elements
                val minWords = if (isIconElement(element)) MIN_WORDS_FOR_ICON else MIN_WORDS_FOR_MATCH
                if (command.words.size >= minWords) {
                    commands.add(command)
                    indexCommand(command)
                }
            }
        }
    }

    /**
     * Check if element is an icon (button/link with aria-label but minimal visible text).
     */
    private fun isIconElement(element: DOMElement): Boolean {
        // Must be a clickable element type
        if (!ICON_ELEMENT_TYPES.contains(element.type)) return false

        // Must have ariaLabel
        if (element.ariaLabel.isBlank()) return false

        // If it has visible text different from aria-label, it's not just an icon
        if (element.name.isNotBlank() && element.name != element.ariaLabel) return false

        // Check if it's a known navigation icon
        if (isNavigationIcon(element.ariaLabel)) return true

        // Small bounds indicate icon (< 64x64 is likely an icon)
        val bounds = element.bounds
        if (bounds.width < 80 && bounds.height < 80) return true

        return false
    }

    /**
     * Create a voice command from a DOM element.
     */
    private fun createCommand(element: DOMElement): WebVoiceCommand? {
        val text = extractCommandText(element)
        if (text.isBlank()) return null

        // Filter garbage text
        val cleanedText = cleanLabel(text) ?: return null

        val words = normalizeAndTokenize(cleanedText)
        if (words.isEmpty()) return null

        val action = determineAction(element)
        val isIcon = isIconElement(element)

        return WebVoiceCommand(
            vosId = element.id,
            elementType = element.type,
            fullText = cleanedText,
            words = words.take(MAX_COMMAND_WORDS),
            selector = element.selector,
            xpath = element.xpath,
            bounds = element.bounds,
            action = action,
            metadata = mapOf(
                "tag" to element.tag,
                "role" to element.role,
                "href" to element.href,
                "inputType" to element.inputType,
                "isIcon" to isIcon.toString()
            )
        )
    }

    /**
     * Extract the best text for voice command from element.
     * Priority: ariaLabel > name > placeholder
     * Filters out garbage text.
     */
    private fun extractCommandText(element: DOMElement): String {
        val raw = when {
            element.ariaLabel.isNotBlank() -> element.ariaLabel
            element.name.isNotBlank() -> element.name
            element.placeholder.isNotBlank() -> element.placeholder
            else -> ""
        }.trim()

        // Early garbage check
        if (isGarbageText(raw)) return ""

        return raw
    }

    /**
     * Normalize text and split into words for matching.
     * - Lowercase
     * - Remove punctuation
     * - Split on whitespace
     * - Filter empty strings
     */
    private fun normalizeAndTokenize(text: String): List<String> {
        return text
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it.length > 1 }
    }

    /**
     * Index a command by its word prefixes for fast lookup.
     */
    private fun indexCommand(command: WebVoiceCommand) {
        // Index by first word
        val firstWord = command.words.firstOrNull() ?: return
        wordIndex.getOrPut(firstWord) { mutableListOf() }.add(command)

        // Also index by first two words combined
        if (command.words.size >= 2) {
            val twoWords = "${command.words[0]} ${command.words[1]}"
            wordIndex.getOrPut(twoWords) { mutableListOf() }.add(command)
        }
    }

    /**
     * Determine the action type based on element type.
     */
    private fun determineAction(element: DOMElement): CommandAction {
        return when (element.type) {
            "link", "button", "menuitem", "tab" -> CommandAction.CLICK
            "input" -> CommandAction.FOCUS
            "checkbox", "radio" -> CommandAction.TOGGLE
            "dropdown" -> CommandAction.SELECT
            else -> CommandAction.CLICK
        }
    }

    /**
     * Find matching commands for the spoken phrase.
     *
     * @param spokenPhrase The words the user said
     * @return List of matches sorted by confidence (best first)
     */
    fun findMatches(spokenPhrase: String): List<MatchResult> {
        val spokenWords = normalizeAndTokenize(spokenPhrase)

        if (spokenWords.size < MIN_WORDS_FOR_MATCH) {
            return emptyList()
        }

        val matches = mutableListOf<MatchResult>()

        for (command in commands) {
            val matchScore = calculateMatchScore(spokenWords, command.words)
            if (matchScore > 0) {
                val confidence = matchScore.toFloat() / spokenWords.size.coerceAtLeast(1)
                matches.add(MatchResult(
                    command = command,
                    matchedWords = matchScore,
                    confidence = confidence
                ))
            }
        }

        // Sort by: 1) matched words (desc), 2) confidence (desc), 3) shorter full text preferred
        return matches.sortedWith(
            compareByDescending<MatchResult> { it.matchedWords }
                .thenByDescending { it.confidence }
                .thenBy { it.command.fullText.length }
        )
    }

    /**
     * Calculate how many words match from the beginning.
     *
     * Returns 0 if less than MIN_WORDS_FOR_MATCH words match.
     */
    private fun calculateMatchScore(spokenWords: List<String>, commandWords: List<String>): Int {
        var matchCount = 0

        for (i in spokenWords.indices) {
            if (i >= commandWords.size) break

            if (commandWords[i].startsWith(spokenWords[i]) ||
                spokenWords[i].startsWith(commandWords[i])) {
                matchCount++
            } else {
                // Words must match sequentially from start
                break
            }
        }

        // Require minimum matches
        return if (matchCount >= MIN_WORDS_FOR_MATCH) matchCount else 0
    }

    /**
     * Get all commands (for debugging/display).
     */
    fun getAllCommands(): List<WebVoiceCommand> = commands.toList()

    /**
     * Get command count.
     */
    fun getCommandCount(): Int = commands.size

    /**
     * Generate disambiguation options for NLU.
     *
     * When multiple matches exist, this provides formatted options
     * for the NLU to present to the user.
     */
    fun generateDisambiguationOptions(matches: List<MatchResult>): List<DisambiguationOption> {
        return matches.take(5).mapIndexed { index, match ->
            val preview = generatePreview(match.command)
            DisambiguationOption(
                index = index + 1,
                preview = preview,
                fullText = match.command.fullText,
                elementType = match.command.elementType,
                command = match.command
            )
        }
    }

    /**
     * Generate a short preview of the command for disambiguation.
     * Uses approximately 3-5 words.
     */
    private fun generatePreview(command: WebVoiceCommand): String {
        val words = command.words
        return when {
            words.size <= 5 -> words.joinToString(" ")
            else -> words.take(4).joinToString(" ") + "..."
        }
    }

    /**
     * Option for NLU disambiguation.
     */
    data class DisambiguationOption(
        val index: Int,
        val preview: String,
        val fullText: String,
        val elementType: String,
        val command: WebVoiceCommand
    )

    /**
     * Quick check if a phrase could potentially match any command.
     * Uses the word index for fast lookup.
     */
    fun hasAnyPotentialMatch(spokenPhrase: String): Boolean {
        val words = normalizeAndTokenize(spokenPhrase)
        if (words.isEmpty()) return false

        val firstWord = words[0]

        // Check single word index
        if (wordIndex.containsKey(firstWord)) return true

        // Check two-word index
        if (words.size >= 2) {
            val twoWords = "${words[0]} ${words[1]}"
            if (wordIndex.containsKey(twoWords)) return true
        }

        return false
    }
}
