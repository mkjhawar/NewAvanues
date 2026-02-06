package com.avanues.avu.codec.core

/**
 * AVU Handover Codes - Registration for .hov file codes
 *
 * Handover files (.hov) use a subset of AVU codes specific to
 * project state transfer between sessions/agents. Some codes
 * (DEC, MOD, DEP, CFG, STA) overlap with RPC/Plugin codes but
 * have handover-specific semantics determined by file type.
 *
 * @author Augmentalis Engineering
 * @since AVU 2.2
 */
object AvuHandoverCodes {

    // Handover-specific codes
    const val CODE_ARC = "ARC"  // Architecture
    const val CODE_WIP = "WIP"  // Work In Progress
    const val CODE_BLK = "BLK"  // Blocker
    const val CODE_TSK = "TSK"  // Task
    const val CODE_FIL = "FIL"  // Files
    const val CODE_LEA = "LEA"  // Learning
    const val CODE_BUG = "BUG"  // Known Bug
    const val CODE_REF = "REF"  // Reference
    const val CODE_API = "API"  // API Change
    const val CODE_CTX = "CTX"  // Context
    const val CODE_PRI = "PRI"  // Priority

    // Codes shared with other categories (context-dependent)
    const val CODE_DEC = "DEC"  // Decision (RPC: Decline)
    const val CODE_STA = "STA"  // State (VoiceOS: Statistics)
    const val CODE_MOD = "MOD"  // Module (Plugin: Target Modules)
    const val CODE_DEP = "DEP"  // Dependency (Plugin: Dependency)
    const val CODE_CFG = "CFG"  // Config (Plugin: Config Block)

    private val handoverCodes = listOf(
        AvuCodeInfo(
            code = CODE_ARC,
            name = "Architecture",
            category = AvuCodeCategory.HANDOVER,
            format = "key:value|qualifiers",
            description = "Architecture pattern or decision"
        ),
        AvuCodeInfo(
            code = CODE_DEC,
            name = "Decision",
            category = AvuCodeCategory.HANDOVER,
            format = "id:rationale|qualifiers",
            description = "Project decision with rationale"
        ),
        AvuCodeInfo(
            code = CODE_STA,
            name = "State",
            category = AvuCodeCategory.HANDOVER,
            format = "key:value|qualifiers",
            description = "Current project state"
        ),
        AvuCodeInfo(
            code = CODE_WIP,
            name = "Work In Progress",
            category = AvuCodeCategory.HANDOVER,
            format = "id:description:status",
            description = "In-progress work item"
        ),
        AvuCodeInfo(
            code = CODE_BLK,
            name = "Blocker",
            category = AvuCodeCategory.HANDOVER,
            format = "id:description:impact",
            description = "Blocking issue"
        ),
        AvuCodeInfo(
            code = CODE_TSK,
            name = "Task",
            category = AvuCodeCategory.HANDOVER,
            format = "id:description:status",
            description = "Pending task"
        ),
        AvuCodeInfo(
            code = CODE_FIL,
            name = "Files",
            category = AvuCodeCategory.HANDOVER,
            format = "path1,path2,...",
            description = "Relevant file paths"
        ),
        AvuCodeInfo(
            code = CODE_MOD,
            name = "Module",
            category = AvuCodeCategory.HANDOVER,
            format = "name|description",
            description = "Module information"
        ),
        AvuCodeInfo(
            code = CODE_DEP,
            name = "Dependency",
            category = AvuCodeCategory.HANDOVER,
            format = "name:version",
            description = "Module dependency"
        ),
        AvuCodeInfo(
            code = CODE_LEA,
            name = "Learning",
            category = AvuCodeCategory.HANDOVER,
            format = "topic:insight",
            description = "Lesson learned during work"
        ),
        AvuCodeInfo(
            code = CODE_BUG,
            name = "Known Bug",
            category = AvuCodeCategory.HANDOVER,
            format = "id:description:workaround",
            description = "Known bug with optional workaround"
        ),
        AvuCodeInfo(
            code = CODE_REF,
            name = "Reference",
            category = AvuCodeCategory.HANDOVER,
            format = "uri:description",
            description = "External reference or documentation link"
        ),
        AvuCodeInfo(
            code = CODE_CFG,
            name = "Config",
            category = AvuCodeCategory.HANDOVER,
            format = "key:value",
            description = "Configuration setting"
        ),
        AvuCodeInfo(
            code = CODE_API,
            name = "API Change",
            category = AvuCodeCategory.HANDOVER,
            format = "endpoint:old:new",
            description = "API change or migration"
        ),
        AvuCodeInfo(
            code = CODE_CTX,
            name = "Context",
            category = AvuCodeCategory.HANDOVER,
            format = "key:value",
            description = "Contextual information"
        ),
        AvuCodeInfo(
            code = CODE_PRI,
            name = "Priority",
            category = AvuCodeCategory.HANDOVER,
            format = "level:description",
            description = "Priority level (1=highest, 5=lowest)"
        )
    )

    /**
     * Register all handover codes with the global registry.
     *
     * Call this during module initialization. Safe to call multiple times —
     * duplicate registrations with identical info are no-ops.
     */
    fun registerAll() {
        handoverCodes.forEach { code ->
            try {
                AvuCodeRegistry.register(code)
            } catch (_: IllegalArgumentException) {
                // Code already registered with different definition (e.g., DEC as RPC Decline).
                // This is expected for shared codes — handover context is determined by file type.
            }
        }
    }

    /**
     * Get all handover code definitions (without registering).
     */
    fun allCodes(): List<AvuCodeInfo> = handoverCodes.toList()

    /**
     * Get the set of all handover code strings.
     */
    fun allCodeStrings(): Set<String> = handoverCodes.map { it.code }.toSet()
}
