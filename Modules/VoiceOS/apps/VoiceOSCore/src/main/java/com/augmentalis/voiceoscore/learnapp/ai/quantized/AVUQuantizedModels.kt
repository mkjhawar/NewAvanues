/**
 * AVUQuantizedModels.kt - Quantized AVU data models for NLU/LLM integration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-08
 *
 * Extends AVU format with quantized, precomputed data structures optimized for:
 * - Fast NLU context understanding (~10-20 tokens per screen vs 200-500)
 * - LLM action prediction
 * - Semantic element embeddings
 * - Compact serialization
 *
 * New IPC Codes (extends avu-1.0):
 * - QCX: Quantized Context (compact app summary)
 * - QSC: Quantized Screen (semantic screen representation)
 * - QEL: Quantized Element (tokenized element)
 * - QNV: Quantized Navigation (optimized path)
 * - QSM: Semantic Map (element clusters)
 * - QAC: Action Candidates (precomputed action list)
 *
 * Part of LearnApp NLU Integration feature
 */
package com.augmentalis.voiceoscore.learnapp.ai.quantized

/**
 * Quantized App Context
 *
 * Ultra-compact representation of entire app for quick LLM context loading.
 * Target: ~50-100 tokens for entire app context.
 *
 * @property packageName App package
 * @property appName Human-readable name
 * @property appType Detected app category (social, productivity, media, etc.)
 * @property screenCount Total screens discovered
 * @property primaryActions Top 10 most common actions across app
 * @property semanticClusters Grouped functionality areas
 * @property navigationComplexity 1-5 scale of nav complexity
 * @property coverage Exploration coverage percentage
 * @property lastUpdated Timestamp of last update
 */
data class QuantizedAppContext(
    val packageName: String,
    val appName: String,
    val appType: AppType,
    val screenCount: Int,
    val primaryActions: List<QuantizedAction>,
    val semanticClusters: List<SemanticCluster>,
    val navigationComplexity: Int,
    val coverage: Float,
    val lastUpdated: Long
) {
    /**
     * Serialize to QCX IPC code
     * Format: QCX:package:name:type:screens:complexity:coverage:timestamp
     */
    fun toIPC(): String {
        return "QCX:$packageName:$appName:${appType.code}:$screenCount:$navigationComplexity:${"%.1f".format(coverage)}:$lastUpdated"
    }

    /**
     * Generate ultra-compact LLM context (~50 tokens)
     */
    fun toCompactLLMContext(): String = buildString {
        append("App:$appName($appType) ")
        append("Screens:$screenCount ")
        append("Actions:[${primaryActions.take(5).joinToString(",") { it.label }}] ")
        append("Areas:[${semanticClusters.take(3).joinToString(",") { it.name }}]")
    }

    companion object {
        fun fromIPC(line: String): QuantizedAppContext? {
            val parts = line.removePrefix("QCX:").split(":")
            if (parts.size < 7) return null
            return QuantizedAppContext(
                packageName = parts[0],
                appName = parts[1],
                appType = AppType.fromCode(parts[2]),
                screenCount = parts[3].toIntOrNull() ?: 0,
                primaryActions = emptyList(), // Loaded separately
                semanticClusters = emptyList(), // Loaded separately
                navigationComplexity = parts[4].toIntOrNull() ?: 1,
                coverage = parts[5].toFloatOrNull() ?: 0f,
                lastUpdated = parts[6].toLongOrNull() ?: 0L
            )
        }
    }
}

/**
 * App Type Categories
 *
 * Detected app category for context-aware processing.
 */
enum class AppType(val code: String, val description: String) {
    SOCIAL("SOC", "Social media/messaging"),
    PRODUCTIVITY("PRD", "Productivity/office"),
    MEDIA("MED", "Media/entertainment"),
    COMMERCE("COM", "Shopping/e-commerce"),
    FINANCE("FIN", "Banking/finance"),
    UTILITY("UTL", "System utilities"),
    NAVIGATION("NAV", "Maps/navigation"),
    HEALTH("HLT", "Health/fitness"),
    EDUCATION("EDU", "Learning/education"),
    COMMUNICATION("CMM", "Communication/calling"),
    UNKNOWN("UNK", "Unknown category");

    companion object {
        fun fromCode(code: String): AppType = entries.find { it.code == code } ?: UNKNOWN
        fun fromPackage(packageName: String): AppType = when {
            packageName.contains("instagram") || packageName.contains("facebook") ||
            packageName.contains("twitter") || packageName.contains("tiktok") ||
            packageName.contains("snapchat") -> SOCIAL

            packageName.contains("teams") || packageName.contains("slack") ||
            packageName.contains("outlook") || packageName.contains("office") ||
            packageName.contains("docs") || packageName.contains("sheets") -> PRODUCTIVITY

            packageName.contains("youtube") || packageName.contains("netflix") ||
            packageName.contains("spotify") || packageName.contains("music") -> MEDIA

            packageName.contains("amazon") || packageName.contains("ebay") ||
            packageName.contains("shop") -> COMMERCE

            packageName.contains("bank") || packageName.contains("pay") ||
            packageName.contains("wallet") || packageName.contains("finance") -> FINANCE

            packageName.contains("maps") || packageName.contains("waze") ||
            packageName.contains("uber") || packageName.contains("lyft") -> NAVIGATION

            packageName.contains("phone") || packageName.contains("dialer") ||
            packageName.contains("whatsapp") || packageName.contains("telegram") -> COMMUNICATION

            else -> UNKNOWN
        }
    }
}

/**
 * Quantized Screen
 *
 * Compact screen representation for fast context understanding.
 * Target: ~10-20 tokens per screen.
 *
 * @property screenId Short hash (8 chars)
 * @property screenType Detected screen type
 * @property purpose Screen's primary purpose (auto-detected)
 * @property elementCount Total actionable elements
 * @property primaryElements Top 5 most important elements
 * @property scrollDirection Scroll capability
 * @property depth Navigation depth from root
 * @property parentScreenId Parent screen for hierarchy
 */
data class QuantizedScreen(
    val screenId: String,
    val screenType: ScreenType,
    val purpose: String,
    val elementCount: Int,
    val primaryElements: List<QuantizedElement>,
    val scrollDirection: ScrollDirection,
    val depth: Int,
    val parentScreenId: String?
) {
    /**
     * Serialize to QSC IPC code
     * Format: QSC:id:type:purpose:elements:scroll:depth:parent
     */
    fun toIPC(): String {
        val parent = parentScreenId ?: "ROOT"
        return "QSC:$screenId:${screenType.code}:$purpose:$elementCount:${scrollDirection.code}:$depth:$parent"
    }

    /**
     * Generate compact screen context (~15 tokens)
     */
    fun toCompactContext(): String = buildString {
        append("[$screenType] $purpose ")
        append("(${primaryElements.size} key actions) ")
        if (scrollDirection != ScrollDirection.NONE) {
            append("[scrollable:${scrollDirection.name.lowercase()}]")
        }
    }

    companion object {
        fun fromIPC(line: String): QuantizedScreen? {
            val parts = line.removePrefix("QSC:").split(":")
            if (parts.size < 7) return null
            return QuantizedScreen(
                screenId = parts[0],
                screenType = ScreenType.fromCode(parts[1]),
                purpose = parts[2],
                elementCount = parts[3].toIntOrNull() ?: 0,
                primaryElements = emptyList(), // Loaded separately
                scrollDirection = ScrollDirection.fromCode(parts[4]),
                depth = parts[5].toIntOrNull() ?: 0,
                parentScreenId = parts[6].takeIf { it != "ROOT" }
            )
        }
    }
}

/**
 * Screen Type Categories
 */
enum class ScreenType(val code: String, val description: String) {
    HOME("HOM", "Home/main screen"),
    LIST("LST", "List/feed view"),
    DETAIL("DTL", "Detail/content view"),
    SETTINGS("SET", "Settings/preferences"),
    PROFILE("PRF", "Profile/account"),
    SEARCH("SRC", "Search/browse"),
    COMPOSE("CMP", "Create/compose content"),
    NAVIGATION("NAV", "Navigation/menu"),
    LOGIN("LGN", "Login/authentication"),
    DIALOG("DLG", "Dialog/modal"),
    PLAYER("PLY", "Media player"),
    CHAT("CHT", "Chat/messaging"),
    FORM("FRM", "Form/input"),
    UNKNOWN("UNK", "Unknown type");

    companion object {
        fun fromCode(code: String): ScreenType = entries.find { it.code == code } ?: UNKNOWN
    }
}

/**
 * Scroll Direction
 */
enum class ScrollDirection(val code: String) {
    NONE("N"),
    VERTICAL("V"),
    HORIZONTAL("H"),
    BOTH("B");

    companion object {
        fun fromCode(code: String): ScrollDirection = entries.find { it.code == code } ?: NONE
    }
}

/**
 * Quantized Element
 *
 * Tokenized element representation for NLU action prediction.
 * Target: ~3-5 tokens per element.
 *
 * @property elementId Short identifier (truncated UUID)
 * @property label User-visible label (normalized)
 * @property semanticType Semantic element type
 * @property actions Available actions (bitfield encoded)
 * @property importance Importance score (0-100)
 * @property quadrant Screen quadrant (1-9 grid position)
 */
data class QuantizedElement(
    val elementId: String,
    val label: String,
    val semanticType: SemanticElementType,
    val actions: Int, // Bitfield: click=1, longClick=2, edit=4, scroll=8
    val importance: Int,
    val quadrant: Int
) {
    /**
     * Serialize to QEL IPC code
     * Format: QEL:id:label:type:actions:importance:quadrant
     */
    fun toIPC(): String {
        return "QEL:$elementId:$label:${semanticType.code}:$actions:$importance:$quadrant"
    }

    /**
     * Get action list from bitfield
     */
    fun getActionList(): List<String> = buildList {
        if (actions and 1 != 0) add("click")
        if (actions and 2 != 0) add("longClick")
        if (actions and 4 != 0) add("edit")
        if (actions and 8 != 0) add("scroll")
    }

    /**
     * Generate token representation (~3 tokens)
     */
    fun toToken(): String = "$label:${semanticType.code}:Q$quadrant"

    companion object {
        fun fromIPC(line: String): QuantizedElement? {
            val parts = line.removePrefix("QEL:").split(":")
            if (parts.size < 6) return null
            return QuantizedElement(
                elementId = parts[0],
                label = parts[1],
                semanticType = SemanticElementType.fromCode(parts[2]),
                actions = parts[3].toIntOrNull() ?: 0,
                importance = parts[4].toIntOrNull() ?: 0,
                quadrant = parts[5].toIntOrNull() ?: 5
            )
        }

        /**
         * Encode actions to bitfield
         */
        fun encodeActions(
            clickable: Boolean,
            longClickable: Boolean,
            editable: Boolean,
            scrollable: Boolean
        ): Int {
            var result = 0
            if (clickable) result = result or 1
            if (longClickable) result = result or 2
            if (editable) result = result or 4
            if (scrollable) result = result or 8
            return result
        }
    }
}

/**
 * Semantic Element Types
 *
 * High-level semantic categorization for NLU understanding.
 */
enum class SemanticElementType(val code: String, val description: String) {
    // Navigation elements
    NAV_BUTTON("NB", "Navigation button"),
    NAV_TAB("NT", "Tab navigation"),
    NAV_MENU("NM", "Menu item"),
    NAV_BACK("NK", "Back navigation"),
    NAV_LINK("NL", "Link/URL"),

    // Action elements
    ACT_PRIMARY("AP", "Primary action (CTA)"),
    ACT_SECONDARY("AS", "Secondary action"),
    ACT_TOGGLE("AT", "Toggle/switch"),
    ACT_SUBMIT("AU", "Submit/confirm"),
    ACT_CANCEL("AC", "Cancel/dismiss"),
    ACT_DELETE("AD", "Delete/remove"),

    // Input elements
    INP_TEXT("IT", "Text input"),
    INP_SEARCH("IS", "Search input"),
    INP_PASSWORD("IP", "Password input"),
    INP_SELECT("IL", "Dropdown/select"),
    INP_CHECKBOX("IC", "Checkbox"),
    INP_RADIO("IR", "Radio button"),

    // Content elements
    CON_ITEM("CI", "List/content item"),
    CON_CARD("CC", "Content card"),
    CON_MEDIA("CM", "Media content"),
    CON_AVATAR("CA", "User avatar"),

    // Container elements
    CTR_LIST("CL", "Scrollable list"),
    CTR_GRID("CG", "Grid container"),
    CTR_PAGER("CP", "View pager"),

    // Special elements
    SPL_FAB("SF", "Floating action button"),
    SPL_NOTIFICATION("SN", "Notification/badge"),
    SPL_LOADING("SL", "Loading indicator"),

    UNKNOWN("XX", "Unknown element");

    companion object {
        fun fromCode(code: String): SemanticElementType = entries.find { it.code == code } ?: UNKNOWN
    }
}

/**
 * Quantized Navigation Path
 *
 * Optimized navigation edge with precomputed data.
 *
 * @property pathId Unique path identifier
 * @property fromScreenId Source screen
 * @property toScreenId Destination screen
 * @property triggerElementId Element that triggers navigation
 * @property triggerLabel Human-readable trigger label
 * @property pathType Type of navigation
 * @property frequency How often this path is used
 * @property avgLatency Average navigation time (ms)
 */
data class QuantizedNavigation(
    val pathId: String,
    val fromScreenId: String,
    val toScreenId: String,
    val triggerElementId: String,
    val triggerLabel: String,
    val pathType: NavigationPathType,
    val frequency: Int,
    val avgLatency: Int
) {
    /**
     * Serialize to QNV IPC code
     * Format: QNV:pathId:from:to:trigger:label:type:freq:latency
     */
    fun toIPC(): String {
        return "QNV:$pathId:$fromScreenId:$toScreenId:$triggerElementId:$triggerLabel:${pathType.code}:$frequency:$avgLatency"
    }

    companion object {
        fun fromIPC(line: String): QuantizedNavigation? {
            val parts = line.removePrefix("QNV:").split(":")
            if (parts.size < 8) return null
            return QuantizedNavigation(
                pathId = parts[0],
                fromScreenId = parts[1],
                toScreenId = parts[2],
                triggerElementId = parts[3],
                triggerLabel = parts[4],
                pathType = NavigationPathType.fromCode(parts[5]),
                frequency = parts[6].toIntOrNull() ?: 0,
                avgLatency = parts[7].toIntOrNull() ?: 0
            )
        }
    }
}

/**
 * Navigation Path Types
 */
enum class NavigationPathType(val code: String) {
    FORWARD("F"),  // Navigate deeper
    BACK("B"),     // Navigate back
    LATERAL("L"),  // Same level (tabs)
    MODAL("M"),    // Open modal/dialog
    EXTERNAL("E"); // Leave app

    companion object {
        fun fromCode(code: String): NavigationPathType = entries.find { it.code == code } ?: FORWARD
    }
}

/**
 * Semantic Cluster
 *
 * Groups related functionality for quick context understanding.
 *
 * @property name Cluster name
 * @property type Cluster type
 * @property elements Elements in this cluster
 * @property screens Screens containing this cluster
 */
data class SemanticCluster(
    val name: String,
    val type: ClusterType,
    val elements: List<String>, // Element IDs
    val screens: List<String>   // Screen IDs
) {
    /**
     * Serialize to QSM IPC code
     * Format: QSM:name:type:elements(+sep):screens(+sep)
     */
    fun toIPC(): String {
        return "QSM:$name:${type.code}:${elements.joinToString("+")}:${screens.joinToString("+")}"
    }

    companion object {
        fun fromIPC(line: String): SemanticCluster? {
            val parts = line.removePrefix("QSM:").split(":")
            if (parts.size < 4) return null
            return SemanticCluster(
                name = parts[0],
                type = ClusterType.fromCode(parts[1]),
                elements = parts[2].split("+").filter { it.isNotBlank() },
                screens = parts[3].split("+").filter { it.isNotBlank() }
            )
        }
    }
}

/**
 * Cluster Types
 */
enum class ClusterType(val code: String) {
    NAVIGATION("NAV"),
    CONTENT("CON"),
    ACTIONS("ACT"),
    SETTINGS("SET"),
    SOCIAL("SOC"),
    MEDIA("MED"),
    INPUT("INP");

    companion object {
        fun fromCode(code: String): ClusterType = entries.find { it.code == code } ?: CONTENT
    }
}

/**
 * Quantized Action
 *
 * Precomputed action candidate for quick NLU prediction.
 *
 * @property label Action label
 * @property elementId Target element
 * @property screenId Screen containing element
 * @property confidence Confidence score (0-100)
 * @property keywords Keywords that trigger this action
 */
data class QuantizedAction(
    val label: String,
    val elementId: String,
    val screenId: String,
    val confidence: Int,
    val keywords: List<String>
) {
    /**
     * Serialize to QAC IPC code
     * Format: QAC:label:element:screen:confidence:keywords(+sep)
     */
    fun toIPC(): String {
        return "QAC:$label:$elementId:$screenId:$confidence:${keywords.joinToString("+")}"
    }

    companion object {
        fun fromIPC(line: String): QuantizedAction? {
            val parts = line.removePrefix("QAC:").split(":")
            if (parts.size < 5) return null
            return QuantizedAction(
                label = parts[0],
                elementId = parts[1],
                screenId = parts[2],
                confidence = parts[3].toIntOrNull() ?: 0,
                keywords = parts[4].split("+").filter { it.isNotBlank() }
            )
        }
    }
}

/**
 * Complete Quantized Context
 *
 * Full quantized representation of an app for NLU/LLM consumption.
 */
data class QuantizedContext(
    val appContext: QuantizedAppContext,
    val screens: List<QuantizedScreen>,
    val elements: Map<String, List<QuantizedElement>>, // screenId -> elements
    val navigation: List<QuantizedNavigation>,
    val clusters: List<SemanticCluster>,
    val actions: List<QuantizedAction>
) {
    /**
     * Serialize to complete AVU format
     */
    fun toAVU(): String = buildString {
        // Header
        appendLine("# Avanues Universal Format v1.0 (Quantized)")
        appendLine("# Type: VOS-Q")
        appendLine("# Extension: .vosq")
        appendLine("---")
        appendLine("schema: avu-1.0")
        appendLine("version: 2.0.0")
        appendLine("locale: en-US")
        appendLine("project: voiceos")
        appendLine("format: quantized")
        appendLine("metadata:")
        appendLine("  file: ${appContext.packageName}.vosq")
        appendLine("  category: learned_app_quantized")
        appendLine("  screens: ${screens.size}")
        appendLine("  elements: ${elements.values.sumOf { it.size }}")
        appendLine("---")

        // App context
        appendLine(appContext.toIPC())

        // Screens and their elements
        for (screen in screens) {
            appendLine(screen.toIPC())
            elements[screen.screenId]?.forEach { element ->
                appendLine(element.toIPC())
            }
        }

        // Navigation paths
        navigation.forEach { nav ->
            appendLine(nav.toIPC())
        }

        // Semantic clusters
        clusters.forEach { cluster ->
            appendLine(cluster.toIPC())
        }

        // Action candidates
        actions.forEach { action ->
            appendLine(action.toIPC())
        }

        appendLine("---")
    }

    /**
     * Generate compact LLM context for quick understanding
     * Target: ~100-200 tokens for entire app
     */
    fun toCompactLLMContext(): String = buildString {
        appendLine(appContext.toCompactLLMContext())
        appendLine()
        appendLine("Screens:")
        screens.forEach { screen ->
            appendLine("- ${screen.toCompactContext()}")
        }
        appendLine()
        appendLine("Key Actions: ${actions.take(10).joinToString(", ") { it.label }}")
    }

    /**
     * Generate HTML-like representation for LLM (research-backed format)
     */
    fun toHTMLRepresentation(): String = buildString {
        appendLine("<app package=\"${appContext.packageName}\" type=\"${appContext.appType}\">")
        for (screen in screens) {
            appendLine("  <screen id=\"${screen.screenId}\" type=\"${screen.screenType}\" purpose=\"${screen.purpose}\">")
            elements[screen.screenId]?.forEach { element ->
                val actionsStr = element.getActionList().joinToString(" ")
                appendLine("    <${element.semanticType.code} id=\"${element.elementId}\" label=\"${element.label}\" actions=\"$actionsStr\" q=\"${element.quadrant}\"/>")
            }
            appendLine("  </screen>")
        }
        appendLine("</app>")
    }

    companion object {
        /**
         * Parse from AVU content
         */
        fun fromAVU(content: String): QuantizedContext? {
            val lines = content.lines().filter {
                !it.startsWith("#") && !it.startsWith("---") &&
                !it.startsWith("schema:") && !it.startsWith("version:") &&
                !it.startsWith("locale:") && !it.startsWith("project:") &&
                !it.startsWith("format:") && !it.startsWith("metadata:") &&
                !it.startsWith("  ") && it.isNotBlank()
            }

            var appContext: QuantizedAppContext? = null
            val screens = mutableListOf<QuantizedScreen>()
            val elements = mutableMapOf<String, MutableList<QuantizedElement>>()
            val navigation = mutableListOf<QuantizedNavigation>()
            val clusters = mutableListOf<SemanticCluster>()
            val actions = mutableListOf<QuantizedAction>()

            var currentScreenId: String? = null

            for (line in lines) {
                when {
                    line.startsWith("QCX:") -> appContext = QuantizedAppContext.fromIPC(line)
                    line.startsWith("QSC:") -> {
                        QuantizedScreen.fromIPC(line)?.let { screen ->
                            screens.add(screen)
                            currentScreenId = screen.screenId
                            elements[screen.screenId] = mutableListOf()
                        }
                    }
                    line.startsWith("QEL:") -> {
                        currentScreenId?.let { screenId ->
                            QuantizedElement.fromIPC(line)?.let { element ->
                                elements[screenId]?.add(element)
                            }
                        }
                    }
                    line.startsWith("QNV:") -> QuantizedNavigation.fromIPC(line)?.let { navigation.add(it) }
                    line.startsWith("QSM:") -> SemanticCluster.fromIPC(line)?.let { clusters.add(it) }
                    line.startsWith("QAC:") -> QuantizedAction.fromIPC(line)?.let { actions.add(it) }
                }
            }

            return appContext?.let {
                QuantizedContext(
                    appContext = it.copy(
                        primaryActions = actions,
                        semanticClusters = clusters
                    ),
                    screens = screens,
                    elements = elements,
                    navigation = navigation,
                    clusters = clusters,
                    actions = actions
                )
            }
        }
    }
}
