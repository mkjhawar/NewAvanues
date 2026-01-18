package com.augmentalis.avamagic.ipc.universal

import com.augmentalis.avamagic.ipc.*

/**
 * Universal File Parser for Avanues Ecosystem
 *
 * Parses all file types: .ava, .vos, .avc, .avw, .avn, .avs
 * All share same structure but different extensions for clear ownership
 *
 * @author Manoj Jhawar (manoj@ideahq.net)
 */
object UniversalFileParser {

    /**
     * Parse any Avanues Universal Format file
     *
     * @param content File content as string
     * @return Parsed UniversalFile
     */
    fun parse(content: String): UniversalFile {
        val sections = content.split("---").map { it.trim() }

        require(sections.size >= 3) {
            "Invalid file format: expected at least 3 sections (header, metadata, entries), got ${sections.size}"
        }

        val header = parseHeader(sections[0])
        val metadata = parseMetadata(sections[1])
        val entries = parseEntries(sections[2])
        val synonyms = if (sections.size > 3) parseSynonyms(sections[3]) else emptyMap()

        return UniversalFile(
            type = header.type,
            extension = header.extension,
            schema = metadata.schema,
            version = metadata.version,
            locale = metadata.locale,
            project = metadata.project,
            metadata = metadata.metadataBlock,
            entries = entries,
            synonyms = synonyms
        )
    }

    private fun parseHeader(section: String): ParsedHeader {
        val lines = section.lines().map { it.trim() }

        val typeLine = lines.find { it.startsWith("# Type:") }
            ?: throw IllegalArgumentException("Missing '# Type:' in header")

        val extLine = lines.find { it.startsWith("# Extension:") }
            ?: throw IllegalArgumentException("Missing '# Extension:' in header")

        val typeStr = typeLine.substringAfter("# Type:").trim()
        val type = FileType.valueOf(typeStr.uppercase())

        val extension = extLine.substringAfter("# Extension:").trim()

        return ParsedHeader(type, extension)
    }

    private fun parseMetadata(section: String): ParsedMetadata {
        val lines = section.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }

        var schema = ""
        var version = ""
        var locale = ""
        var project = ""
        val metadataBlock = mutableMapOf<String, Any>()

        var inMetadataBlock = false
        var currentKey = ""

        for (line in lines) {
            when {
                line.startsWith("schema:") -> schema = line.substringAfter(":").trim()
                line.startsWith("version:") -> version = line.substringAfter(":").trim()
                line.startsWith("locale:") -> locale = line.substringAfter(":").trim()
                line.startsWith("project:") -> project = line.substringAfter(":").trim()
                line == "metadata:" -> inMetadataBlock = true
                inMetadataBlock && line.contains(":") -> {
                    val key = line.substringBefore(":").trim()
                    val value = line.substringAfter(":").trim()
                    currentKey = key
                    metadataBlock[key] = parseValue(value)
                }
            }
        }

        return ParsedMetadata(schema, version, locale, project, metadataBlock)
    }

    private fun parseEntries(section: String): List<UniversalEntry> {
        return section.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                val parts = line.split(":", limit = 3)
                require(parts.size >= 2) { "Invalid entry format: $line" }

                UniversalEntry(
                    code = parts[0],
                    id = parts[1],
                    data = if (parts.size > 2) parts[2] else ""
                )
            }
    }

    private fun parseSynonyms(section: String): Map<String, List<String>> {
        val lines = section.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }

        val synonyms = mutableMapOf<String, List<String>>()
        var inSynonymsBlock = false

        for (line in lines) {
            when {
                line.startsWith("synonyms:") -> inSynonymsBlock = true
                inSynonymsBlock && line.contains(":") -> {
                    val key = line.substringBefore(":").trim()
                    val valueStr = line.substringAfter(":").trim()

                    // Parse [word1, word2, word3]
                    if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
                        val values = valueStr.substring(1, valueStr.length - 1)
                            .split(",")
                            .map { it.trim() }
                        synonyms[key] = values
                    }
                }
            }
        }

        return synonyms
    }

    private fun parseValue(value: String): Any {
        return when {
            value == "true" -> true
            value == "false" -> false
            value.toIntOrNull() != null -> value.toInt()
            value.toLongOrNull() != null -> value.toLong()
            value.startsWith("[") && value.endsWith("]") -> {
                value.substring(1, value.length - 1)
                    .split(",")
                    .map { it.trim() }
            }
            else -> value
        }
    }

    private data class ParsedHeader(
        val type: FileType,
        val extension: String
    )

    private data class ParsedMetadata(
        val schema: String,
        val version: String,
        val locale: String,
        val project: String,
        val metadataBlock: Map<String, Any>
    )
}

/**
 * Parsed universal file
 */
data class UniversalFile(
    val type: FileType,
    val extension: String,
    val schema: String,
    val version: String,
    val locale: String,
    val project: String,
    val metadata: Map<String, Any>,
    val entries: List<UniversalEntry>,
    val synonyms: Map<String, List<String>>
) {
    /**
     * Filter entries by IPC code
     */
    fun filterByCode(code: String): List<UniversalEntry> {
        return entries.filter { it.code == code }
    }

    /**
     * Get entry by ID
     */
    fun getEntryById(id: String): UniversalEntry? {
        return entries.find { it.id == id }
    }

    /**
     * Convert all entries to IPC messages
     */
    fun toIPCMessages(): List<UniversalMessage> {
        return entries.mapNotNull { entry ->
            try {
                entry.toIPCMessage()
            } catch (e: Exception) {
                null // Skip invalid entries
            }
        }
    }
}

/**
 * Single entry from file
 */
data class UniversalEntry(
    val code: String,
    val id: String,
    val data: String
) {
    /**
     * Convert to IPC message with runtime request ID
     */
    fun toIPCMessage(requestId: String? = null): UniversalMessage {
        val finalId = requestId ?: generateRequestId()
        val ipcString = "$code:$finalId:$data"

        val result = UniversalDSL.parse(ipcString)
        return when (result) {
            is ParseResult.Protocol -> result.message
            else -> throw IllegalArgumentException("Entry cannot be converted to IPC message: $ipcString")
        }
    }

    private fun generateRequestId(): String {
        return "${code.lowercase()}_${System.currentTimeMillis()}"
    }
}

/**
 * File type enum - FINAL CORRECTED EXTENSIONS
 */
enum class FileType {
    AVA,    // AVA voice intents
    VOS,    // VoiceOS system commands
    AVC,    // AvaConnect device communication
    AWB,    // WebAvanue/BrowserAvanue browser commands (Ava Web Browser)
    AMI,    // MagicUI components (Ava MagicUI)
    AMC,    // MagicCode generators (Ava MagicCode)
    HOV,    // Handover files (AI context continuity)
    IDC,    // IDEACODE config files
    AVL,    // License exchange files (AvanueCentral licensing)
    LICENSE; // Alias for AVL (header uses LICENSE)

    fun toExtension(): String = when(this) {
        LICENSE -> ".avl"
        else -> ".${name.lowercase()}"
    }

    fun toProjectName(): String = when(this) {
        AVA -> "ava"
        VOS -> "voiceos"
        AVC -> "avaconnect"
        AWB -> "browseravanue"
        AMI -> "magicui"
        AMC -> "magiccode"
        HOV -> "handover"
        IDC -> "ideacode"
        AVL, LICENSE -> "avanuecentral"
    }

    companion object {
        /**
         * Detect FileType from first entry prefix
         */
        fun detectFromPrefix(prefix: String): FileType = when(prefix) {
            "PRJ", "CFG", "PRF", "GAT" -> IDC
            "CMD", "CAT", "LOC" -> VOS
            "THM", "PAL", "TYP" -> AMI
            "APP", "STA", "SCR", "ELM" -> AVA
            "REQ", "RES", "EVT" -> AVC
            "ARC", "WIP", "BLK", "DEC" -> HOV
            "LIC", "DEV", "ACT", "FPR", "VND", "TEN", "DST", "RSL", "CUS" -> AVL
            else -> AVA // Default fallback
        }
    }
}

/**
 * Project-specific file readers
 */
class AvaFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.AVA) { "Not an AVA file: got ${file.type}" }
        return file
    }
}

class VosFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.VOS) { "Not a VOS file: got ${file.type}" }
        return file
    }
}

class AvcFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.AVC) { "Not an AVC file: got ${file.type}" }
        return file
    }
}

class AwbFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.AWB) { "Not an AWB file: got ${file.type}" }
        return file
    }
}

class AmiFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.AMI) { "Not an AMI file: got ${file.type}" }
        return file
    }
}

class AmcFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.AMC) { "Not an AMC file: got ${file.type}" }
        return file
    }
}

class HovFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.HOV) { "Not a HOV file: got ${file.type}" }
        return file
    }
}

class IdcFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.IDC) { "Not an IDC file: got ${file.type}" }
        return file
    }
}

class AvlFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.AVL || file.type == FileType.LICENSE) {
            "Not an AVL/LICENSE file: got ${file.type}"
        }
        return file
    }

    /**
     * Parse license entries into structured data
     */
    fun loadLicense(content: String): LicenseData {
        val file = load(content)
        return LicenseData.fromUniversalFile(file)
    }
}

/**
 * Structured license data parsed from AVL files
 */
data class LicenseData(
    val licenseId: String,
    val product: String,
    val type: String,
    val seats: Int,
    val action: String,
    val devices: List<DeviceInfo>,
    val fingerprints: List<Fingerprint>,
    val features: List<FeatureFlag>,
    val expiry: ExpiryInfo?,
    val usage: UsageInfo?,
    val tenant: TenantInfo?,
    val metadata: Map<String, String>,
    val signature: String?
) {
    companion object {
        fun fromUniversalFile(file: UniversalFile): LicenseData {
            val entries = file.entries

            // Parse LIC entry
            val licEntry = entries.find { it.code == "LIC" }
            val licParts = licEntry?.data?.split(":") ?: emptyList()

            // Parse devices
            val devices = entries.filter { it.code == "DEV" }.map { entry ->
                val parts = entry.data.split(":")
                DeviceInfo(
                    id = entry.id,
                    hostname = parts.getOrNull(0) ?: "",
                    os = parts.getOrNull(1) ?: "",
                    timestamp = parts.getOrNull(2)
                )
            }

            // Parse fingerprints
            val fingerprints = entries.filter { it.code == "FPR" }.map { entry ->
                Fingerprint(type = entry.id, value = entry.data)
            }

            // Parse features
            val features = entries.filter { it.code == "FEA" }.map { entry ->
                val parts = entry.data.split(":")
                FeatureFlag(
                    name = entry.id,
                    enabled = parts.getOrNull(0)?.toBoolean() ?: false,
                    expiry = parts.getOrNull(1)
                )
            }

            // Parse expiry
            val expEntry = entries.find { it.code == "EXP" }
            val expiry = expEntry?.let {
                val parts = it.data.split(":")
                ExpiryInfo(
                    date = it.id,
                    graceDays = parts.getOrNull(0)?.toIntOrNull() ?: 0
                )
            }

            // Parse usage
            val usageEntry = entries.find { it.code == "USG" }
            val usage = usageEntry?.let {
                val parts = it.data.split(":")
                UsageInfo(
                    used = it.id.toIntOrNull() ?: 0,
                    total = parts.getOrNull(0)?.toIntOrNull() ?: 0,
                    remaining = parts.getOrNull(1)?.toIntOrNull() ?: 0
                )
            }

            // Parse tenant hierarchy
            val tenEntry = entries.find { it.code == "TEN" }
            val tenant = tenEntry?.let {
                val parts = it.data.split(":")
                TenantInfo(
                    id = it.id,
                    name = parts.getOrNull(0) ?: "",
                    type = parts.getOrNull(1) ?: "",
                    parentId = parts.getOrNull(2)
                )
            }

            // Parse metadata
            val metadata = entries.filter { it.code == "MET" }
                .associate { it.id to it.data }

            // Parse signature
            val signature = entries.find { it.code == "SIG" }?.let {
                "${it.id}:${it.data}"
            }

            return LicenseData(
                licenseId = licEntry?.id ?: "",
                product = licParts.getOrNull(0) ?: "",
                type = licParts.getOrNull(1) ?: "",
                seats = licParts.getOrNull(2)?.toIntOrNull() ?: 0,
                action = file.metadata["action"]?.toString() ?: "",
                devices = devices,
                fingerprints = fingerprints,
                features = features,
                expiry = expiry,
                usage = usage,
                tenant = tenant,
                metadata = metadata,
                signature = signature
            )
        }
    }
}

data class DeviceInfo(
    val id: String,
    val hostname: String,
    val os: String,
    val timestamp: String? = null
)

data class Fingerprint(
    val type: String,
    val value: String
)

data class FeatureFlag(
    val name: String,
    val enabled: Boolean,
    val expiry: String? = null
)

data class ExpiryInfo(
    val date: String,
    val graceDays: Int
)

data class UsageInfo(
    val used: Int,
    val total: Int,
    val remaining: Int
)

data class TenantInfo(
    val id: String,
    val name: String,
    val type: String,
    val parentId: String? = null
)

/**
 * Handover Entry Codes (HOV files)
 *
 * Used for AI context continuity and session handovers.
 * Each code represents a specific type of handover information.
 */
object HandoverCodes {
    const val ARC = "ARC"  // Architecture - patterns, structure, design decisions
    const val STA = "STA"  // State - current status, progress
    const val WIP = "WIP"  // Work in Progress - active tasks, partially complete
    const val BLK = "BLK"  // Blocker - issues preventing progress
    const val DEC = "DEC"  // Decision - key decisions made with rationale
    const val FIL = "FIL"  // File - file references, paths
    const val MOD = "MOD"  // Module - module-specific context
    const val LEA = "LEA"  // Learning - insights, mistakes, corrections
    const val TSK = "TSK"  // Task - pending tasks, todos
    const val DEP = "DEP"  // Dependency - module/package dependencies
    const val CFG = "CFG"  // Config - configuration state
    const val API = "API"  // API - interface changes, contracts
    const val BUG = "BUG"  // Bug - known issues, workarounds
    const val REF = "REF"  // Reference - links, related docs
    const val CTX = "CTX"  // Context - session context, recent work
    const val PRI = "PRI"  // Priority - P0/P1/P2 items

    val ALL = listOf(ARC, STA, WIP, BLK, DEC, FIL, MOD, LEA, TSK, DEP, CFG, API, BUG, REF, CTX, PRI)

    fun description(code: String): String = when(code) {
        ARC -> "Architecture (patterns, structure)"
        STA -> "State (current status)"
        WIP -> "Work in Progress"
        BLK -> "Blocker (preventing progress)"
        DEC -> "Decision (with rationale)"
        FIL -> "File reference"
        MOD -> "Module context"
        LEA -> "Learning/insight"
        TSK -> "Task/todo"
        DEP -> "Dependency"
        CFG -> "Configuration"
        API -> "API/interface"
        BUG -> "Known bug"
        REF -> "Reference/link"
        CTX -> "Session context"
        PRI -> "Priority item"
        else -> "Unknown"
    }
}

/**
 * License Entry Codes (AVL files)
 *
 * Used for license exchange between devices and vendor systems.
 * Supports multi-tenant hierarchy, device fingerprinting, and compliance.
 */
object LicenseCodes {
    // Core License Prefixes
    const val LIC = "LIC"  // License info (id:product:type:seats)
    const val DEV = "DEV"  // Device registration (id:hostname:os)
    const val ACT = "ACT"  // Activation (device_id:timestamp:status)
    const val EXP = "EXP"  // Expiry (date:grace_days)
    const val FPR = "FPR"  // Fingerprint (type:value)
    const val USG = "USG"  // Usage/seats (used:total:remaining)
    const val VND = "VND"  // Vendor info (id:name:contact)
    const val FEA = "FEA"  // Feature flag (name:enabled:expiry)
    const val TIR = "TIR"  // Tier/Edition (name:level)
    const val RVK = "RVK"  // Revocation (device_id:reason:timestamp)
    const val SYN = "SYN"  // Sync status (last_sync:next_sync:status)
    const val QTA = "QTA"  // Quota (resource:used:limit)

    // Multi-Tenant Hierarchy
    const val TEN = "TEN"  // Tenant info (id:name:type:parent_id)
    const val DST = "DST"  // Distributor (id:name:region:contact)
    const val RSL = "RSL"  // Reseller (id:name:distributor_id:margin)
    const val CUS = "CUS"  // Customer/Enterprise (id:name:reseller_id:type)
    const val DIV = "DIV"  // Division/Department (id:name:customer_id:budget)
    const val USR = "USR"  // User (id:name:email:division_id)
    const val HRC = "HRC"  // Hierarchy chain (level:entity_id:entity_type)
    const val PRM = "PRM"  // Permission (entity_id:permission:scope)
    const val ALO = "ALO"  // Allocation (from:to:seats:type)

    // Advanced License Control
    const val GRC = "GRC"  // Grace period (type:days:action)
    const val CLK = "CLK"  // Clock validation (check:tolerance:action)
    const val GEO = "GEO"  // Geographic restriction (allow_or_deny:countries)
    const val VRS = "VRS"  // Version control (min:max:current)
    const val MNT = "MNT"  // Maintenance/support (start:end:level)
    const val CAP = "CAP"  // Capacity limit (resource:limit:period)
    const val MTR = "MTR"  // Metered usage (resource:used:limit:overage_rate)
    const val BRW = "BRW"  // Borrow/checkout (device_id:checkout:return:status)
    const val TRF = "TRF"  // Transfer (from_device:to_device:date:approved_by)
    const val HRV = "HRV"  // Harvest/reclaim (device_id:last_active:days_idle:action)
    const val AUD = "AUD"  // Audit entry (timestamp:action:entity:details)
    const val CMP = "CMP"  // Compliance (check:status:last_verified)
    const val BND = "BND"  // Bundle/package (id:name:includes)
    const val UPG = "UPG"  // Upgrade path (from_tier:to_tier:price:prorate)
    const val RNW = "RNW"  // Renewal (type:date:price:auto)

    // Hardware/IoT
    const val DNG = "DNG"  // Dongle/hardware key (serial:type:status)
    const val IOT = "IOT"  // IoT device binding (device_type:serial:firmware)
    const val OEM = "OEM"  // OEM license (partner_id:product:terms)
    const val HWD = "HWD"  // Hardware device (type:model:serial)
    const val MFG = "MFG"  // Manufacturing (batch:line:date:qty)
    const val WRN = "WRN"  // Warranty (type:start:end:coverage)
    const val SVC = "SVC"  // Service contract (type:level:response_time)
    const val PRT = "PRT"  // Parts/consumables (part_id:authorized:remaining)

    // Special License Types
    const val NFR = "NFR"  // Not For Resale (purpose:recipient:expiry)
    const val EDU = "EDU"  // Educational (institution:type:students)
    const val GOV = "GOV"  // Government (agency:contract:level)

    // Subscription/Billing
    const val SUB = "SUB"  // Subscription (plan:interval:price:currency)
    const val BIL = "BIL"  // Billing (next_date:amount:status)
    const val PAY = "PAY"  // Payment (method:last4:expiry)
    const val CRD = "CRD"  // Credits/tokens (type:balance:expiry)
    const val DSC = "DSC"  // Discount (code:percent:valid_until)

    // Signature
    const val SIG = "SIG"  // Signature (algorithm:value)

    val CORE = listOf(LIC, DEV, ACT, EXP, FPR, USG, VND, FEA, TIR, RVK, SYN, QTA)
    val HIERARCHY = listOf(TEN, DST, RSL, CUS, DIV, USR, HRC, PRM, ALO)
    val ADVANCED = listOf(GRC, CLK, GEO, VRS, MNT, CAP, MTR, BRW, TRF, HRV, AUD, CMP, BND, UPG, RNW)
    val HARDWARE = listOf(DNG, IOT, OEM, HWD, MFG, WRN, SVC, PRT)
    val SPECIAL = listOf(NFR, EDU, GOV)
    val BILLING = listOf(SUB, BIL, PAY, CRD, DSC)
    val ALL = CORE + HIERARCHY + ADVANCED + HARDWARE + SPECIAL + BILLING + SIG

    fun description(code: String): String = when(code) {
        LIC -> "License info"
        DEV -> "Device registration"
        ACT -> "Activation status"
        EXP -> "Expiration"
        FPR -> "Device fingerprint"
        USG -> "Seat usage"
        VND -> "Vendor info"
        FEA -> "Feature flag"
        TIR -> "License tier"
        RVK -> "Revocation"
        SYN -> "Sync status"
        QTA -> "Resource quota"
        TEN -> "Tenant"
        DST -> "Distributor"
        RSL -> "Reseller"
        CUS -> "Customer"
        DIV -> "Division"
        USR -> "User"
        HRC -> "Hierarchy"
        PRM -> "Permission"
        ALO -> "Allocation"
        GRC -> "Grace period"
        GEO -> "Geographic restriction"
        VRS -> "Version control"
        MNT -> "Maintenance"
        CAP -> "Capacity limit"
        MTR -> "Metered usage"
        BRW -> "Borrow/checkout"
        TRF -> "Transfer"
        HRV -> "Harvest/reclaim"
        AUD -> "Audit"
        CMP -> "Compliance"
        DNG -> "Dongle"
        IOT -> "IoT device"
        OEM -> "OEM license"
        HWD -> "Hardware"
        SUB -> "Subscription"
        BIL -> "Billing"
        SIG -> "Signature"
        else -> "Unknown"
    }

    /**
     * License types supported by the system
     */
    enum class LicenseType {
        NODE_LOCKED,    // Tied to device fingerprint
        NAMED_USER,     // Tied to user account
        FLOATING,       // Concurrent seat pool
        VOLUME,         // Enterprise bulk seats
        SUBSCRIPTION,   // Time-limited recurring
        PERPETUAL,      // No expiry
        TRIAL,          // Limited evaluation
        SITE,           // Unlimited at location
        OEM,            // Embedded in partner product
        NFR,            // Not for resale/demo
        EDUCATIONAL,    // Academic use only
        GOVERNMENT,     // Public sector
        METERED,        // Pay-per-use
        CAPACITY,       // Resource-limited
        HARDWARE,       // Physical device bound
        IOT             // IoT device license
    }

    /**
     * Hierarchy levels for multi-tenant licensing
     */
    enum class HierarchyLevel(val level: Int) {
        PLATFORM(0),      // Us (Avanue)
        DISTRIBUTOR(1),   // Wholesale distributor
        RESELLER(2),      // VAR/partner
        CUSTOMER(3),      // Enterprise org
        DIVISION(4),      // Department
        USER(5);          // End user

        fun canIssueLicenses(): Boolean = level <= 3
    }
}
