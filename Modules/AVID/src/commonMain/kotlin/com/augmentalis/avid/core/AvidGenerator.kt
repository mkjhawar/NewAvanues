/**
 * AvidGenerator.kt - Cross-Platform AVID Generation (Avanues Voice ID)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-30
 * Migrated from VUID: 2026-01-14
 *
 * Responsibility: Generate unique AVIDs (Avanues Voice Identifiers) across all platforms
 *
 * ## Compact Format (DNS-Style) - RECOMMENDED
 *
 * ### For Third-Party Apps (VoiceOS):
 * ```
 * {reversedPackage}:{version}:{typeAbbrev}:{hash8}
 * Example: android.instagram.com:12.0.0:btn:a7f3e2c1
 * ```
 *
 * ### For Internal Modules (AVA, NLU, WebAvanue):
 * ```
 * {module}:{version}:{typeAbbrev}:{hash8}
 * Example: ava:1.0.0:msg:a7f3e2c1
 * ```
 *
 * ### Simple Format (no version):
 * ```
 * {module}:{typeAbbrev}:{hash8}
 * Example: ava:msg:a7f3e2c1
 * ```
 *
 * ## Benefits of Compact Format:
 * - Full package name preserved (no collisions)
 * - Version included (no conflicts across app updates)
 * - Human readable and debuggable
 * - Sorts alphabetically by TLD
 * - 3-char type abbreviations are scannable
 * - Fully reversible to verbose format
 * - KMP compatible (works on all platforms)
 */
package com.augmentalis.avid.core

import kotlin.random.Random
import kotlinx.atomicfu.atomic

/**
 * Cross-platform AVID generator
 *
 * Uses pure Kotlin for KMP compatibility across Android, iOS, Desktop, and Web.
 */
object AvidGenerator {

    // Thread-safe atomic sequence counter
    private val sequenceCounter = atomic(0L)

    // ========================================================================
    // Module Names (for internal entities)
    // ========================================================================

    object Module {
        const val VOICEOS = "vos"
        const val AVA = "ava"
        const val WEBAVANUE = "web"
        const val NLU = "nlu"
        const val COCKPIT = "cpt"
        const val COMMON = "cmn"
    }

    // ========================================================================
    // Type Abbreviations (3-char codes)
    // ========================================================================

    object TypeAbbrev {
        // VoiceOS UI Elements
        const val BUTTON = "btn"
        const val INPUT = "inp"
        const val SCROLL = "scr"
        const val TEXT = "txt"
        const val ELEMENT = "elm"
        const val CARD = "crd"
        const val LAYOUT = "lay"
        const val MENU = "mnu"
        const val DIALOG = "dlg"
        const val IMAGE = "img"
        const val CHECKBOX = "chk"
        const val SWITCH = "swt"
        const val LIST = "lst"
        const val ITEM = "itm"
        const val SLIDER = "sld"
        const val TAB = "tab"

        // AVA Entities
        const val MESSAGE = "msg"
        const val CONVERSATION = "cnv"
        const val DOCUMENT = "doc"
        const val CHUNK = "chu"
        const val MEMORY = "mem"
        const val DECISION = "dec"
        const val LEARNING = "lrn"
        const val INTENT = "int"
        const val CLUSTER = "cls"
        const val BOOKMARK = "bmk"
        const val ANNOTATION = "ann"
        const val FILTER_PRESET = "flt"
        const val UTTERANCE = "utt"

        // WebAvanue Entities
        const val FAVORITE = "fav"
        const val DOWNLOAD = "dwn"
        const val HISTORY = "hst"
        const val SESSION = "ses"
        const val GROUP = "grp"

        // Cockpit Entities
        const val REQUEST = "req"
        const val WINDOW = "win"
        const val STREAM = "str"
        const val PRESET = "pre"
        const val DEVICE = "dev"
        const val SYNC = "syn"

        /**
         * Get 3-char abbreviation from full type name
         */
        fun fromTypeName(typeName: String): String {
            return when (typeName.lowercase()) {
                // VoiceOS UI Elements
                "button", "imagebutton", "floatingactionbutton", "btn" -> BUTTON
                "input", "edittext", "textfield", "textinput", "inp" -> INPUT
                "scroll", "scrollview", "recyclerview", "listview", "scr" -> SCROLL
                "text", "textview", "label", "txt" -> TEXT
                "card", "cardview", "crd" -> CARD
                "layout", "container", "view", "viewgroup", "framelayout",
                "linearlayout", "relativelayout", "constraintlayout", "lay" -> LAYOUT
                "menu", "menuitem", "popupmenu", "mnu" -> MENU
                "dialog", "modal", "alertdialog", "bottomsheet", "dlg" -> DIALOG
                "image", "imageview", "icon", "img" -> IMAGE
                "checkbox", "checkboxpreference", "chk" -> CHECKBOX
                "switch", "toggle", "switchcompat", "swt" -> SWITCH
                "list", "recycler", "lst" -> LIST
                "item", "listitem", "viewholder", "itm" -> ITEM
                "slider", "seekbar", "sld" -> SLIDER
                "tab", "browsertab" -> TAB
                // AVA Entities
                "message", "chatmessage", "msg" -> MESSAGE
                "conversation", "chat", "thread", "cnv" -> CONVERSATION
                "document", "file", "doc" -> DOCUMENT
                "chunk", "segment", "chu" -> CHUNK
                "memory", "context", "mem" -> MEMORY
                "decision", "dec" -> DECISION
                "learning", "trained", "lrn" -> LEARNING
                "intent", "action", "int" -> INTENT
                "cluster", "cls" -> CLUSTER
                "bookmark", "bmk" -> BOOKMARK
                "annotation", "ann" -> ANNOTATION
                "filterpreset", "filter", "flt" -> FILTER_PRESET
                "utterance", "utt" -> UTTERANCE
                // WebAvanue Entities
                "favorite", "fav" -> FAVORITE
                "download", "dwn" -> DOWNLOAD
                "history", "historyentry", "hst" -> HISTORY
                "session", "browsersession", "ses" -> SESSION
                "group", "tabgroup", "grp" -> GROUP
                // Cockpit Entities
                "request", "requestid", "req" -> REQUEST
                "window", "windowid", "win" -> WINDOW
                "stream", "streamid", "str" -> STREAM
                "preset", "layoutpreset", "pre" -> PRESET
                "device", "deviceid", "dev" -> DEVICE
                "sync", "syncrequest", "syn" -> SYNC
                else -> ELEMENT
            }
        }

        /**
         * Get full type name from abbreviation
         */
        fun toTypeName(abbrev: String): String {
            return when (abbrev.lowercase()) {
                BUTTON -> "button"
                INPUT -> "input"
                SCROLL -> "scroll"
                TEXT -> "text"
                CARD -> "card"
                LAYOUT -> "layout"
                MENU -> "menu"
                DIALOG -> "dialog"
                IMAGE -> "image"
                CHECKBOX -> "checkbox"
                SWITCH -> "switch"
                LIST -> "list"
                ITEM -> "item"
                SLIDER -> "slider"
                TAB -> "tab"
                MESSAGE -> "message"
                CONVERSATION -> "conversation"
                DOCUMENT -> "document"
                CHUNK -> "chunk"
                MEMORY -> "memory"
                DECISION -> "decision"
                LEARNING -> "learning"
                INTENT -> "intent"
                CLUSTER -> "cluster"
                BOOKMARK -> "bookmark"
                ANNOTATION -> "annotation"
                FILTER_PRESET -> "filterpreset"
                UTTERANCE -> "utterance"
                FAVORITE -> "favorite"
                DOWNLOAD -> "download"
                HISTORY -> "history"
                SESSION -> "session"
                GROUP -> "group"
                REQUEST -> "request"
                WINDOW -> "window"
                STREAM -> "stream"
                PRESET -> "preset"
                DEVICE -> "device"
                SYNC -> "sync"
                else -> "element"
            }
        }
    }

    // ========================================================================
    // Compact Format Generation (DNS-Style) - RECOMMENDED
    // ========================================================================

    /**
     * Generate compact AVID for third-party apps (VoiceOS)
     *
     * Format: {reversedPackage}:{version}:{typeAbbrev}:{hash8}
     * Example: android.instagram.com:12.0.0:btn:a7f3e2c1
     *
     * @param packageName Android package name (e.g., "com.instagram.android")
     * @param version App version string (e.g., "12.0.0")
     * @param typeName Full type name (e.g., "button", "input")
     * @param elementHash Optional fingerprint hash (if null, random generated)
     * @return Compact AVID string
     */
    fun generateCompact(
        packageName: String,
        version: String,
        typeName: String,
        elementHash: String? = null
    ): String {
        val reversedPkg = reversePackage(packageName)
        val typeAbbrev = TypeAbbrev.fromTypeName(typeName)
        val hash8 = elementHash?.take(8)?.lowercase() ?: generateHash8()
        return "$reversedPkg:$version:$typeAbbrev:$hash8"
    }

    /**
     * Generate compact AVID for internal module entities
     *
     * Format: {module}:{version}:{typeAbbrev}:{hash8}
     * Example: ava:1.0.0:msg:a7f3e2c1
     *
     * @param module Module name (use Module.* constants)
     * @param version Module/app version
     * @param typeName Full type name (e.g., "message", "conversation")
     * @return Compact AVID string
     */
    fun generateCompactModule(
        module: String,
        version: String,
        typeName: String
    ): String {
        val typeAbbrev = TypeAbbrev.fromTypeName(typeName)
        val hash8 = generateHash8()
        return "$module:$version:$typeAbbrev:$hash8"
    }

    /**
     * Generate simple compact AVID (no version)
     *
     * Format: {module}:{typeAbbrev}:{hash8}
     * Example: ava:msg:a7f3e2c1
     */
    fun generateCompactSimple(module: String, typeName: String): String {
        val typeAbbrev = TypeAbbrev.fromTypeName(typeName)
        val hash8 = generateHash8()
        return "$module:$typeAbbrev:$hash8"
    }

    // ========================================================================
    // Convenience Methods for Common Types
    // ========================================================================

    // AVA Types
    fun generateMessageAvid(): String = generateCompactSimple(Module.AVA, "message")
    fun generateConversationAvid(): String = generateCompactSimple(Module.AVA, "conversation")
    fun generateDocumentAvid(): String = generateCompactSimple(Module.AVA, "document")
    fun generateMemoryAvid(): String = generateCompactSimple(Module.AVA, "memory")
    fun generateLearningAvid(): String = generateCompactSimple(Module.AVA, "learning")
    fun generateIntentAvid(): String = generateCompactSimple(Module.AVA, "intent")
    fun generateChunkAvid(): String = generateCompactSimple(Module.AVA, "chunk")
    fun generateClusterAvid(): String = generateCompactSimple(Module.AVA, "cluster")
    fun generateBookmarkAvid(): String = generateCompactSimple(Module.AVA, "bookmark")
    fun generateAnnotationAvid(): String = generateCompactSimple(Module.AVA, "annotation")
    fun generateFilterPresetAvid(): String = generateCompactSimple(Module.AVA, "filterpreset")
    fun generateUtteranceAvid(): String = generateCompactSimple(Module.AVA, "utterance")
    fun generateDecisionAvid(): String = generateCompactSimple(Module.AVA, "decision")
    fun generateDialogAvid(): String = generateCompactSimple(Module.AVA, "dialog")

    // WebAvanue Types
    fun generateTabAvid(): String = generateCompactSimple(Module.WEBAVANUE, "tab")
    fun generateFavoriteAvid(): String = generateCompactSimple(Module.WEBAVANUE, "favorite")
    fun generateDownloadAvid(): String = generateCompactSimple(Module.WEBAVANUE, "download")
    fun generateHistoryAvid(): String = generateCompactSimple(Module.WEBAVANUE, "history")
    fun generateSessionAvid(): String = generateCompactSimple(Module.WEBAVANUE, "session")
    fun generateGroupAvid(): String = generateCompactSimple(Module.WEBAVANUE, "group")

    // Cockpit Types
    fun generateRequestAvid(): String = generateCompactSimple(Module.COCKPIT, "request")
    fun generateWindowAvid(): String = generateCompactSimple(Module.COCKPIT, "window")
    fun generateStreamAvid(): String = generateCompactSimple(Module.COCKPIT, "stream")
    fun generatePresetAvid(): String = generateCompactSimple(Module.COCKPIT, "preset")
    fun generateDeviceAvid(): String = generateCompactSimple(Module.COCKPIT, "device")
    fun generateSyncAvid(): String = generateCompactSimple(Module.COCKPIT, "sync")

    // ========================================================================
    // Legacy VUID Compatibility (Deprecated - use AVID methods instead)
    // ========================================================================

    @Deprecated("Use generateMessageAvid()", ReplaceWith("generateMessageAvid()"))
    fun generateMessageVuid(): String = generateMessageAvid()
    @Deprecated("Use generateConversationAvid()", ReplaceWith("generateConversationAvid()"))
    fun generateConversationVuid(): String = generateConversationAvid()
    @Deprecated("Use generateDocumentAvid()", ReplaceWith("generateDocumentAvid()"))
    fun generateDocumentVuid(): String = generateDocumentAvid()
    @Deprecated("Use generateChunkAvid()", ReplaceWith("generateChunkAvid()"))
    fun generateChunkVuid(): String = generateChunkAvid()
    @Deprecated("Use generateMemoryAvid()", ReplaceWith("generateMemoryAvid()"))
    fun generateMemoryVuid(): String = generateMemoryAvid()
    @Deprecated("Use generateDecisionAvid()", ReplaceWith("generateDecisionAvid()"))
    fun generateDecisionVuid(): String = generateDecisionAvid()
    @Deprecated("Use generateLearningAvid()", ReplaceWith("generateLearningAvid()"))
    fun generateLearningVuid(): String = generateLearningAvid()
    @Deprecated("Use generateIntentAvid()", ReplaceWith("generateIntentAvid()"))
    fun generateIntentVuid(): String = generateIntentAvid()
    @Deprecated("Use generateClusterAvid()", ReplaceWith("generateClusterAvid()"))
    fun generateClusterVuid(): String = generateClusterAvid()
    @Deprecated("Use generateBookmarkAvid()", ReplaceWith("generateBookmarkAvid()"))
    fun generateBookmarkVuid(): String = generateBookmarkAvid()
    @Deprecated("Use generateAnnotationAvid()", ReplaceWith("generateAnnotationAvid()"))
    fun generateAnnotationVuid(): String = generateAnnotationAvid()
    @Deprecated("Use generateFilterPresetAvid()", ReplaceWith("generateFilterPresetAvid()"))
    fun generateFilterPresetVuid(): String = generateFilterPresetAvid()
    @Deprecated("Use generateUtteranceAvid()", ReplaceWith("generateUtteranceAvid()"))
    fun generateUtteranceVuid(): String = generateUtteranceAvid()
    @Deprecated("Use generateDialogAvid()", ReplaceWith("generateDialogAvid()"))
    fun generateDialogVuid(): String = generateDialogAvid()

    // ========================================================================
    // Hash Generation
    // ========================================================================

    /**
     * Generate 8-character random hex hash
     * Uses Kotlin's Random for KMP compatibility
     */
    fun generateHash8(): String {
        return buildString {
            repeat(8) {
                append(HEX_CHARS[Random.nextInt(16)])
            }
        }
    }

    private const val HEX_CHARS = "0123456789abcdef"

    // ========================================================================
    // Package Name Utilities
    // ========================================================================

    /**
     * Reverse package name to DNS-style format
     * Example: "com.instagram.android" → "android.instagram.com"
     */
    fun reversePackage(packageName: String): String {
        return packageName.split(".").reversed().joinToString(".")
    }

    /**
     * Restore package name from DNS-style format
     * Example: "android.instagram.com" → "com.instagram.android"
     */
    fun unreversePackage(dnsStyle: String): String {
        return dnsStyle.split(".").reversed().joinToString(".")
    }

    // ========================================================================
    // Legacy Format Generation (for backward compatibility)
    // ========================================================================

    /**
     * Generate standard random AVID (legacy format)
     * @deprecated Use generateCompactSimple() for new code
     */
    @Deprecated(
        "Use generateCompactSimple() for new code",
        ReplaceWith("generateCompactSimple(Module.COMMON, \"element\")")
    )
    fun generate(): String = generateLegacyUuid()

    /**
     * Generate AVID with prefix (legacy format)
     */
    fun generateWithPrefix(prefix: String): String = "$prefix-${generateLegacyUuid()}"

    /**
     * Generate sequential AVID for predictable ordering
     */
    fun generateSequential(prefix: String = "seq"): String {
        val sequence = sequenceCounter.incrementAndGet()
        val timestamp = currentTimeMillis()
        return "$prefix-$sequence-$timestamp"
    }

    /**
     * Generate AVID based on content hash
     */
    fun generateFromContent(content: String): String {
        val hash = content.hashCode().toString(16)
        val timestamp = currentTimeMillis()
        return "content-$hash-$timestamp"
    }

    /**
     * Generate AVID for specific element type (legacy format)
     */
    fun generateForType(type: String, name: String? = null): String {
        val suffix = name?.let { "-${it.replace(" ", "-").lowercase()}" } ?: ""
        val shortHash = generateHash8()
        return "${type.lowercase()}$suffix-$shortHash"
    }

    /**
     * Generate legacy RFC4122-style UUID
     * Pure Kotlin implementation for KMP compatibility
     */
    private fun generateLegacyUuid(): String {
        val random = Random.Default
        val bytes = ByteArray(16) { random.nextInt(256).toByte() }

        // Set version to 4 (random UUID)
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()
        // Set variant to RFC 4122
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()

        return buildString {
            for (i in bytes.indices) {
                if (i == 4 || i == 6 || i == 8 || i == 10) {
                    append('-')
                }
                val hex = (bytes[i].toInt() and 0xFF).toString(16).padStart(2, '0')
                append(hex)
            }
        }
    }

    // ========================================================================
    // Compact Format Validation
    // ========================================================================

    /** Pattern for app AVID: {reversed.pkg}:{version}:{type}:{hash8} */
    private val COMPACT_APP_PATTERN = Regex(
        "^[a-z][a-z0-9]*(?:\\.[a-z][a-z0-9]*)+:[0-9]+(?:\\.[0-9]+)*:[a-z]{3}:[a-f0-9]{8}$",
        RegexOption.IGNORE_CASE
    )

    /** Pattern for module AVID with version: {module}:{version}:{type}:{hash8} */
    private val COMPACT_MODULE_PATTERN = Regex(
        "^[a-z]{3}:[0-9]+(?:\\.[0-9]+)*:[a-z]{3}:[a-f0-9]{8}$",
        RegexOption.IGNORE_CASE
    )

    /** Pattern for simple module AVID: {module}:{type}:{hash8} */
    private val COMPACT_SIMPLE_PATTERN = Regex(
        "^[a-z]{3}:[a-z]{3}:[a-f0-9]{8}$",
        RegexOption.IGNORE_CASE
    )

    /** Legacy UUID v4 pattern */
    private val LEGACY_UUID_PATTERN = Regex(
        "^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$",
        RegexOption.IGNORE_CASE
    )

    /** Legacy VoiceOS pattern: com.pkg.v1.0.0.type-hash12 */
    private val LEGACY_VOICEOS_PATTERN = Regex(
        "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+\\.v[0-9.]+\\.[a-z]+-[a-f0-9]{12}$",
        RegexOption.IGNORE_CASE
    )

    /** Check if AVID is in compact app format */
    fun isCompactApp(avid: String): Boolean = COMPACT_APP_PATTERN.matches(avid)

    /** Check if AVID is in compact module format (with version) */
    fun isCompactModule(avid: String): Boolean = COMPACT_MODULE_PATTERN.matches(avid)

    /** Check if AVID is in compact simple format (no version) */
    fun isCompactSimple(avid: String): Boolean = COMPACT_SIMPLE_PATTERN.matches(avid)

    /** Check if AVID is in any compact format */
    fun isCompact(avid: String): Boolean =
        isCompactApp(avid) || isCompactModule(avid) || isCompactSimple(avid)

    /** Check if AVID is in legacy UUID v4 format */
    fun isLegacyUuid(avid: String): Boolean = LEGACY_UUID_PATTERN.matches(avid)

    /** Check if AVID is in legacy VoiceOS format */
    fun isLegacyVoiceOS(avid: String): Boolean = LEGACY_VOICEOS_PATTERN.matches(avid)

    /** Check if any valid AVID format (compact or legacy) */
    fun isValid(avid: String): Boolean =
        isCompact(avid) || isLegacyUuid(avid) || isLegacyVoiceOS(avid)

    // ========================================================================
    // Compact Format Parsing
    // ========================================================================

    /** AVID format types */
    enum class AvidFormat {
        COMPACT_APP,        // android.instagram.com:12.0.0:btn:a7f3e2c1
        COMPACT_MODULE,     // ava:1.0.0:msg:a7f3e2c1
        COMPACT_SIMPLE,     // ava:msg:a7f3e2c1
        LEGACY_UUID,        // 550e8400-e29b-41d4-a716-446655440000
        LEGACY_VOICEOS,     // com.pkg.v1.0.0.button-a7f3e2c1d4b5
        UNKNOWN
    }

    /** Parsed AVID components */
    data class ParsedAvid(
        val packageName: String?,   // Full package name (for app format)
        val module: String?,        // Module name (for module format)
        val version: String?,       // Version string
        val typeAbbrev: String,     // 3-char type abbreviation
        val hash: String,           // 8-char unique hash
        val format: AvidFormat      // Detected format
    ) {
        /** Get full type name */
        val typeName: String get() = TypeAbbrev.toTypeName(typeAbbrev)

        /**
         * Convert to verbose/debug format
         *
         * Example: "android.instagram.com:12.0.0:btn:a7f3e2c1"
         *       → "com.instagram.android v12.0.0 button [a7f3e2c1]"
         */
        fun toVerbose(): String = when (format) {
            AvidFormat.COMPACT_APP -> "$packageName v$version $typeName [$hash]"
            AvidFormat.COMPACT_MODULE -> "$module v$version $typeName [$hash]"
            AvidFormat.COMPACT_SIMPLE -> "$module $typeName [$hash]"
            AvidFormat.LEGACY_VOICEOS -> "$packageName v$version $typeName [$hash]"
            AvidFormat.LEGACY_UUID -> "uuid [$hash]"
            AvidFormat.UNKNOWN -> "unknown [$hash]"
        }

        /** Reconstruct compact AVID string */
        fun toCompactAvid(): String = when (format) {
            AvidFormat.COMPACT_APP -> {
                val reversedPkg = reversePackage(packageName!!)
                "$reversedPkg:$version:$typeAbbrev:$hash"
            }
            AvidFormat.COMPACT_MODULE -> "$module:$version:$typeAbbrev:$hash"
            AvidFormat.COMPACT_SIMPLE -> "$module:$typeAbbrev:$hash"
            else -> throw IllegalStateException("Cannot convert $format to compact")
        }
    }

    /**
     * Parse AVID into components
     *
     * @param avid AVID string in any supported format
     * @return Parsed components or null if invalid format
     */
    fun parse(avid: String): ParsedAvid? = when {
        isCompactApp(avid) -> parseCompactApp(avid)
        isCompactModule(avid) -> parseCompactModule(avid)
        isCompactSimple(avid) -> parseCompactSimple(avid)
        isLegacyUuid(avid) -> parseLegacyUuid(avid)
        isLegacyVoiceOS(avid) -> parseLegacyVoiceOS(avid)
        else -> null
    }

    private fun parseCompactApp(avid: String): ParsedAvid {
        val parts = avid.split(":")
        val hash = parts.last()
        val typeAbbrev = parts[parts.size - 2]
        val version = parts[parts.size - 3]
        val reversedPkg = parts.dropLast(3).joinToString(":")
        val packageName = unreversePackage(reversedPkg)

        return ParsedAvid(
            packageName = packageName,
            module = null,
            version = version,
            typeAbbrev = typeAbbrev,
            hash = hash,
            format = AvidFormat.COMPACT_APP
        )
    }

    private fun parseCompactModule(avid: String): ParsedAvid {
        val parts = avid.split(":")
        return ParsedAvid(
            packageName = null,
            module = parts[0],
            version = parts[1],
            typeAbbrev = parts[2],
            hash = parts[3],
            format = AvidFormat.COMPACT_MODULE
        )
    }

    private fun parseCompactSimple(avid: String): ParsedAvid {
        val parts = avid.split(":")
        return ParsedAvid(
            packageName = null,
            module = parts[0],
            version = null,
            typeAbbrev = parts[1],
            hash = parts[2],
            format = AvidFormat.COMPACT_SIMPLE
        )
    }

    private fun parseLegacyUuid(avid: String): ParsedAvid {
        return ParsedAvid(
            packageName = null,
            module = null,
            version = null,
            typeAbbrev = TypeAbbrev.ELEMENT,
            hash = avid.replace("-", "").takeLast(8),
            format = AvidFormat.LEGACY_UUID
        )
    }

    private fun parseLegacyVoiceOS(avid: String): ParsedAvid {
        val lastDotIdx = avid.lastIndexOf('.')
        val dashIdx = avid.indexOf('-', lastDotIdx)

        if (lastDotIdx < 0 || dashIdx < 0) {
            return ParsedAvid(null, null, null, TypeAbbrev.ELEMENT, avid.takeLast(8), AvidFormat.LEGACY_VOICEOS)
        }

        val beforeType = avid.substring(0, lastDotIdx)
        val typeName = avid.substring(lastDotIdx + 1, dashIdx)
        val hash = avid.substring(dashIdx + 1).take(8)

        val versionIdx = beforeType.indexOf(".v")
        val packageName = if (versionIdx > 0) beforeType.substring(0, versionIdx) else beforeType
        val version = if (versionIdx > 0) beforeType.substring(versionIdx + 2) else null

        return ParsedAvid(
            packageName = packageName,
            module = null,
            version = version,
            typeAbbrev = TypeAbbrev.fromTypeName(typeName),
            hash = hash,
            format = AvidFormat.LEGACY_VOICEOS
        )
    }

    // ========================================================================
    // Migration Utilities
    // ========================================================================

    /**
     * Migrate legacy VUID to compact format
     *
     * @param legacyVuid Legacy VUID string
     * @param version App version (required if not in legacy format)
     * @return Compact AVID or null if cannot migrate
     */
    fun migrateToCompact(legacyVuid: String, version: String? = null): String? {
        val parsed = parse(legacyVuid) ?: return null

        return when (parsed.format) {
            AvidFormat.LEGACY_VOICEOS -> {
                val ver = parsed.version ?: version ?: "0.0.0"
                generateCompact(parsed.packageName!!, ver, parsed.typeName, parsed.hash)
            }
            AvidFormat.LEGACY_UUID -> null // Can't determine package/type from UUID
            AvidFormat.COMPACT_APP, AvidFormat.COMPACT_MODULE, AvidFormat.COMPACT_SIMPLE -> legacyVuid
            AvidFormat.UNKNOWN -> null
        }
    }

}

// ========================================================================
// Platform-specific time (expect/actual pattern)
// ========================================================================

/**
 * Get current time in milliseconds
 * Implemented per-platform for KMP compatibility
 */
internal expect fun currentTimeMillis(): Long
