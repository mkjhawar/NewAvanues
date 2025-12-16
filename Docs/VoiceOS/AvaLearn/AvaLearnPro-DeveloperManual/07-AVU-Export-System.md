# Chapter 7: AVU Export System

**Document:** VoiceOS-AvaLearnPro-DeveloperManual-Ch07
**Version:** 1.0
**Last Updated:** 2025-12-11

---

## 7.1 AVU Format Specification

### 7.1.1 Format Overview

AVU (Avanues Universal) is a compact, line-based format designed for efficient IPC-style data exchange.

```
# Avanues Universal Format v1.0
# Type: VOS
---
schema: avu-1.0
version: 1.0.0
locale: en-US
metadata:
  file: com.example.app.vos
  category: learned_app
  count: 87
---
APP:com.example.app:Example App:1702300800000
STA:5:87:42:2.3:5:75.5
SCR:abc123:MainActivity:1702300801000:15
ELM:uuid1:Login:Button:C:0,540,1080,640:ACT
NAV:abc123:def456:uuid1:1702300802000
CMD:cmd1:click login:click:uuid1:0.95
---
synonyms:
  login: [sign in, log in]
  settings: [preferences, options]
```

### 7.1.2 Record Types

| Prefix | Name | Fields | Description |
|--------|------|--------|-------------|
| APP | Application | pkg:name:timestamp | App metadata |
| STA | Statistics | screens:elements:commands:avgTime:loops:coverage | Stats |
| SCR | Screen | hash:activity:timestamp:count | Screen record |
| ELM | Element | uuid:name:type:actions:bounds:state | UI element |
| NAV | Navigation | from:to:element:timestamp | Screen transition |
| CMD | Command | id:phrase:action:target:confidence | Voice command |

### 7.1.3 Field Separators

| Separator | Usage |
|-----------|-------|
| `:` | Field separator |
| `,` | List/bounds separator |
| `+` | Multi-value separator |
| `---` | Section separator |

---

## 7.2 AVUGenerator Implementation

### 7.2.1 Complete Generator

```kotlin
package com.augmentalis.learnappcore.export

class AVUGenerator {

    fun generateHeader(): String {
        return buildString {
            appendLine("# Avanues Universal Format v1.0")
            appendLine("# Type: VOS")
            appendLine("# Generated: ${formatTimestamp(System.currentTimeMillis())}")
        }
    }

    fun generateMetadata(packageName: String, commandCount: Int): String {
        return buildString {
            appendLine("---")
            appendLine("schema: avu-1.0")
            appendLine("version: 1.0.0")
            appendLine("locale: en-US")
            appendLine("metadata:")
            appendLine("  file: $packageName.vos")
            appendLine("  category: learned_app")
            appendLine("  count: $commandCount")
        }
    }

    fun generateAppRecord(packageName: String, appName: String): String {
        val timestamp = System.currentTimeMillis()
        return "APP:$packageName:${sanitize(appName)}:$timestamp"
    }

    fun generateStatsRecord(stats: ExplorationStatistics): String {
        return buildString {
            append("STA:")
            append("${stats.screensExplored}:")
            append("${stats.elementsDiscovered}:")
            append("${stats.elementsClicked}:")
            append("${String.format("%.1f", stats.explorationTime)}:")
            append("${stats.loopsDetected}:")
            append("${String.format("%.1f", stats.coverage)}")
        }
    }

    fun generateScreenRecord(screen: ScreenInfo): String {
        return buildString {
            append("SCR:")
            append("${screen.screenHash}:")
            append("${sanitize(screen.activityName)}:")
            append("${screen.timestamp}:")
            append("${screen.elementCount}")
        }
    }

    fun generateElementRecord(element: ElementInfo): String {
        val actions = element.actions.joinToString("") { it.code }
        val bounds = "${element.bounds.left},${element.bounds.top}," +
                     "${element.bounds.right},${element.bounds.bottom}"

        return buildString {
            append("ELM:")
            append("${element.uuid}:")
            append("${sanitize(element.displayName)}:")
            append("${element.shortClassName}:")
            append("$actions:")
            append("$bounds:")
            append("${element.state.code}")
        }
    }

    fun generateNavigationRecord(
        fromScreen: String,
        toScreen: String,
        triggerElement: String,
        timestamp: Long = System.currentTimeMillis()
    ): String {
        return "NAV:$fromScreen:$toScreen:$triggerElement:$timestamp"
    }

    fun generateCommandRecord(command: CommandInfo): String {
        return buildString {
            append("CMD:")
            append("${command.commandId}:")
            append("${sanitize(command.phrase)}:")
            append("${command.action}:")
            append("${command.targetElementUuid}:")
            append("${String.format("%.2f", command.confidence)}")
        }
    }

    fun generateSynonymsSection(commands: List<CommandInfo>): String {
        return buildString {
            appendLine("---")
            appendLine("synonyms:")
            commands
                .filter { it.synonyms.isNotEmpty() }
                .forEach { cmd ->
                    val synonymList = cmd.synonyms.joinToString(", ")
                    appendLine("  ${cmd.phrase}: [$synonymList]")
                }
        }
    }

    private fun sanitize(text: String): String {
        return text
            .replace(":", "\\:")
            .replace("\n", " ")
            .replace("\r", "")
            .trim()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timestamp))
    }
}
```

---

## 7.3 SynonymGenerator Implementation

### 7.3.1 Complete SynonymGenerator

```kotlin
package com.augmentalis.learnappcore.export

class SynonymGenerator {

    private val synonymDatabase = mapOf(
        // Common actions
        "login" to listOf("sign in", "log in", "authenticate"),
        "logout" to listOf("sign out", "log out"),
        "settings" to listOf("preferences", "options", "configuration"),
        "search" to listOf("find", "look for", "locate"),
        "close" to listOf("dismiss", "exit", "cancel"),
        "save" to listOf("store", "keep", "preserve"),
        "delete" to listOf("remove", "erase", "trash"),
        "edit" to listOf("modify", "change", "update"),
        "send" to listOf("submit", "post", "transmit"),
        "share" to listOf("send to", "forward"),

        // Navigation
        "home" to listOf("main", "start"),
        "back" to listOf("return", "previous"),
        "next" to listOf("forward", "continue"),
        "menu" to listOf("options", "more"),

        // Common UI elements
        "button" to listOf("btn", "action"),
        "input" to listOf("field", "textbox", "entry"),
        "checkbox" to listOf("check", "toggle"),
        "dropdown" to listOf("select", "picker", "chooser")
    )

    fun generate(phrase: String): List<String> {
        val words = phrase.lowercase().split(" ")
        val synonyms = mutableSetOf<String>()

        // Direct phrase synonyms
        synonymDatabase[phrase.lowercase()]?.let {
            synonyms.addAll(it)
        }

        // Word-level synonyms
        words.forEach { word ->
            synonymDatabase[word]?.forEach { synonym ->
                val newPhrase = phrase.lowercase().replace(word, synonym)
                if (newPhrase != phrase.lowercase()) {
                    synonyms.add(newPhrase)
                }
            }
        }

        // Generate variations
        synonyms.addAll(generateVariations(phrase))

        return synonyms.toList().take(MAX_SYNONYMS)
    }

    private fun generateVariations(phrase: String): List<String> {
        val variations = mutableListOf<String>()
        val lower = phrase.lowercase()

        // "click X" -> "tap X", "press X"
        if (lower.startsWith("click ")) {
            val target = lower.removePrefix("click ")
            variations.add("tap $target")
            variations.add("press $target")
            variations.add("select $target")
        }

        // "enter X" -> "type X", "input X"
        if (lower.startsWith("enter ")) {
            val target = lower.removePrefix("enter ")
            variations.add("type $target")
            variations.add("input $target")
            variations.add("fill in $target")
        }

        // "scroll X" -> "swipe X"
        if (lower.startsWith("scroll ")) {
            val target = lower.removePrefix("scroll ")
            variations.add("swipe $target")
        }

        return variations
    }

    companion object {
        private const val MAX_SYNONYMS = 5
    }
}
```

---

## 7.4 Complete Export Pipeline

### 7.4.1 AVUExporter

```kotlin
package com.augmentalis.learnappcore.export

class AVUExporter(
    private val context: Context,
    private val mode: ExportMode = ExportMode.USER
) {
    private val generator = AVUGenerator()
    private val synonymGenerator = SynonymGenerator()

    suspend fun export(
        packageName: String,
        appName: String,
        screens: List<ScreenInfo>,
        elements: List<ElementInfo>,
        navigations: List<NavigationRecord>,
        statistics: ExplorationStatistics
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            // Generate content
            val content = generateContent(
                packageName, appName, screens, elements, navigations, statistics
            )

            // Determine output
            val finalContent = when (mode) {
                ExportMode.DEVELOPER -> content  // Plain text
                ExportMode.USER -> encrypt(content)  // Encrypted
            }

            // Write file
            val file = getExportFile("$packageName.vos")
            file.writeText(finalContent)

            ExportResult.Success(file.absolutePath)
        } catch (e: Exception) {
            ExportResult.Failure(e.message ?: "Export failed")
        }
    }

    private fun generateContent(
        packageName: String,
        appName: String,
        screens: List<ScreenInfo>,
        elements: List<ElementInfo>,
        navigations: List<NavigationRecord>,
        statistics: ExplorationStatistics
    ): String {
        val commands = generateCommands(elements)

        return buildString {
            // Header
            append(generator.generateHeader())
            appendLine()

            // Metadata
            append(generator.generateMetadata(packageName, commands.size))
            appendLine("---")

            // App record
            appendLine(generator.generateAppRecord(packageName, appName))

            // Statistics
            appendLine(generator.generateStatsRecord(statistics))

            // Screens
            screens.forEach { screen ->
                appendLine(generator.generateScreenRecord(screen))
            }

            // Elements
            elements.forEach { element ->
                appendLine(generator.generateElementRecord(element))
            }

            // Navigation
            navigations.forEach { nav ->
                appendLine(generator.generateNavigationRecord(
                    nav.fromScreen, nav.toScreen, nav.triggerElement, nav.timestamp
                ))
            }

            // Commands
            commands.forEach { command ->
                appendLine(generator.generateCommandRecord(command))
            }

            // Synonyms
            append(generator.generateSynonymsSection(commands))
        }
    }

    private fun generateCommands(elements: List<ElementInfo>): List<CommandInfo> {
        return elements
            .filter { it.isInteractive }
            .map { element ->
                val phrase = generatePhrase(element)
                val synonyms = synonymGenerator.generate(phrase)

                CommandInfo(
                    commandId = "cmd_${element.uuid.take(8)}",
                    phrase = phrase,
                    action = determineAction(element),
                    targetElementUuid = element.uuid,
                    confidence = calculateConfidence(element),
                    synonyms = synonyms,
                    screenHash = element.screenHash,
                    packageName = element.packageName
                )
            }
    }

    private fun generatePhrase(element: ElementInfo): String {
        val name = element.displayName.lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .trim()

        return when {
            ElementAction.CLICK in element.actions -> "click $name"
            ElementAction.EDIT in element.actions -> "enter $name"
            ElementAction.SCROLL in element.actions -> "scroll $name"
            ElementAction.LONG_CLICK in element.actions -> "long press $name"
            else -> name
        }
    }

    private fun determineAction(element: ElementInfo): String {
        return when {
            ElementAction.EDIT in element.actions -> "setText"
            ElementAction.CLICK in element.actions -> "click"
            ElementAction.LONG_CLICK in element.actions -> "longClick"
            ElementAction.SCROLL in element.actions -> "scroll"
            else -> "click"
        }
    }

    private fun calculateConfidence(element: ElementInfo): Float {
        var score = 0.5f
        if (element.contentDescription.isNotEmpty()) score += 0.2f
        if (element.text.isNotEmpty()) score += 0.15f
        if (element.resourceId.isNotEmpty()) score += 0.1f
        if (element.shortClassName in STANDARD_COMPONENTS) score += 0.05f
        return score.coerceAtMost(1.0f)
    }

    private fun getExportFile(filename: String): File {
        val dir = File(context.getExternalFilesDir(null), "learned_apps")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, filename)
    }

    private fun encrypt(content: String): String {
        return Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
    }

    companion object {
        val STANDARD_COMPONENTS = setOf(
            "Button", "TextView", "EditText", "ImageButton",
            "CheckBox", "RadioButton", "Switch", "SeekBar"
        )
    }
}

enum class ExportMode {
    USER,       // Encrypted
    DEVELOPER   // Plain text
}

sealed class ExportResult {
    data class Success(val path: String) : ExportResult()
    data class Failure(val error: String) : ExportResult()
}

data class NavigationRecord(
    val fromScreen: String,
    val toScreen: String,
    val triggerElement: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

---

## 7.5 File Watching

### 7.5.1 AVUFileWatcher

```kotlin
package com.augmentalis.learnappcore.export

class AVUFileWatcher(
    private val watchDir: File,
    private val callback: WatcherCallback
) {
    private var observer: FileObserver? = null

    interface WatcherCallback {
        fun onFileCreated(file: File)
        fun onFileModified(file: File)
        fun onFileDeleted(file: File)
    }

    fun start() {
        if (!watchDir.exists()) {
            watchDir.mkdirs()
        }

        observer = object : FileObserver(watchDir.absolutePath, CREATE or MODIFY or DELETE) {
            override fun onEvent(event: Int, path: String?) {
                path ?: return
                if (!path.endsWith(".vos")) return

                val file = File(watchDir, path)

                when (event) {
                    CREATE -> callback.onFileCreated(file)
                    MODIFY -> callback.onFileModified(file)
                    DELETE -> callback.onFileDeleted(file)
                }
            }
        }

        observer?.startWatching()
    }

    fun stop() {
        observer?.stopWatching()
        observer = null
    }
}
```

---

## 7.6 Parsing AVU Files

### 7.6.1 AVUFileParser

```kotlin
package com.augmentalis.learnappcore.export

class AVUFileParser {

    data class ParsedAVU(
        val packageName: String,
        val appName: String,
        val screens: List<ParsedScreen>,
        val elements: List<ParsedElement>,
        val commands: List<ParsedCommand>,
        val synonyms: Map<String, List<String>>
    )

    fun parse(file: File): ParsedAVU {
        val content = file.readText()
        return parse(content)
    }

    fun parse(content: String): ParsedAVU {
        var packageName = ""
        var appName = ""
        val screens = mutableListOf<ParsedScreen>()
        val elements = mutableListOf<ParsedElement>()
        val commands = mutableListOf<ParsedCommand>()
        val synonyms = mutableMapOf<String, List<String>>()

        var inSynonyms = false

        content.lines().forEach { line ->
            val trimmed = line.trim()

            when {
                trimmed.startsWith("synonyms:") -> inSynonyms = true
                inSynonyms && trimmed.contains(":") -> {
                    val (phrase, syns) = parseSynonymLine(trimmed)
                    if (phrase.isNotEmpty()) {
                        synonyms[phrase] = syns
                    }
                }
                trimmed.startsWith("APP:") -> {
                    val parts = trimmed.removePrefix("APP:").split(":")
                    if (parts.size >= 2) {
                        packageName = parts[0]
                        appName = parts[1]
                    }
                }
                trimmed.startsWith("SCR:") -> {
                    screens.add(parseScreenRecord(trimmed))
                }
                trimmed.startsWith("ELM:") -> {
                    elements.add(parseElementRecord(trimmed))
                }
                trimmed.startsWith("CMD:") -> {
                    commands.add(parseCommandRecord(trimmed))
                }
            }
        }

        return ParsedAVU(packageName, appName, screens, elements, commands, synonyms)
    }

    private fun parseScreenRecord(line: String): ParsedScreen {
        val parts = line.removePrefix("SCR:").split(":")
        return ParsedScreen(
            hash = parts.getOrElse(0) { "" },
            activity = parts.getOrElse(1) { "" },
            timestamp = parts.getOrElse(2) { "0" }.toLongOrNull() ?: 0,
            elementCount = parts.getOrElse(3) { "0" }.toIntOrNull() ?: 0
        )
    }

    private fun parseElementRecord(line: String): ParsedElement {
        val parts = line.removePrefix("ELM:").split(":")
        return ParsedElement(
            uuid = parts.getOrElse(0) { "" },
            name = parts.getOrElse(1) { "" },
            type = parts.getOrElse(2) { "" },
            actions = parts.getOrElse(3) { "" },
            bounds = parts.getOrElse(4) { "" },
            state = parts.getOrElse(5) { "ACT" }
        )
    }

    private fun parseCommandRecord(line: String): ParsedCommand {
        val parts = line.removePrefix("CMD:").split(":")
        return ParsedCommand(
            id = parts.getOrElse(0) { "" },
            phrase = parts.getOrElse(1) { "" },
            action = parts.getOrElse(2) { "" },
            target = parts.getOrElse(3) { "" },
            confidence = parts.getOrElse(4) { "0" }.toFloatOrNull() ?: 0f
        )
    }

    private fun parseSynonymLine(line: String): Pair<String, List<String>> {
        val match = Regex("^\\s*(\\w+):\\s*\\[(.*)\\]").find(line) ?: return "" to emptyList()
        val phrase = match.groupValues[1]
        val synonyms = match.groupValues[2].split(",").map { it.trim() }
        return phrase to synonyms
    }

    data class ParsedScreen(val hash: String, val activity: String, val timestamp: Long, val elementCount: Int)
    data class ParsedElement(val uuid: String, val name: String, val type: String, val actions: String, val bounds: String, val state: String)
    data class ParsedCommand(val id: String, val phrase: String, val action: String, val target: String, val confidence: Float)
}
```

---

## 7.7 Next Steps

Continue to [Chapter 8: Debugging & Analysis Tools](./08-Debugging-Tools.md).

---

**End of Chapter 7**
