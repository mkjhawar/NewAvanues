/**
 * AvuProtocol.kt - AVU 2.1 Format Protocol for WebAvanue RPC (KMP)
 *
 * Implements the Avanues Universal Format v2.1 for cross-project communication.
 * Compact, line-based format that is 60-80% smaller than JSON.
 *
 * Format: CODE:field1:field2:field3:...
 * Escape: : → %3A, % → %25, \n → %0A, \r → %0D
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.webavanue.rpc

import com.augmentalis.webavanue.rpc.messages.*

/**
 * AVU 2.1 Message Codes for WebAvanue
 */
object AvuCodes {
    // Core IPC
    const val VCM = "VCM"  // Voice Command: VCM:id:action:params
    const val ACC = "ACC"  // Accept: ACC:requestId
    const val ACD = "ACD"  // Accept with Data: ACD:requestId:data
    const val ERR = "ERR"  // Error: ERR:requestId:code:message
    const val BSY = "BSY"  // Busy: BSY:requestId

    // WebAvanue Tab Operations
    const val GTB = "GTB"  // Get Tabs: GTB:requestId
    const val TBL = "TBL"  // Tab List: TBL:requestId:tab1|tab2|...
    const val CTB = "CTB"  // Create Tab: CTB:requestId:url:active
    const val CLT = "CLT"  // Close Tab: CLT:requestId:tabId
    const val SWT = "SWT"  // Switch Tab: SWT:requestId:tabId
    const val TAB = "TAB"  // Tab Info: TAB:tabId:url:title:active:index

    // Navigation
    const val NAV = "NAV"  // Navigate: NAV:requestId:tabId:url
    const val GBK = "GBK"  // Go Back: GBK:requestId:tabId
    const val GFW = "GFW"  // Go Forward: GFW:requestId:tabId
    const val RLD = "RLD"  // Reload: RLD:requestId:tabId:hard

    // Page Interaction
    const val CLK = "CLK"  // Click: CLK:requestId:tabId:selector
    const val TYP = "TYP"  // Type: TYP:requestId:tabId:selector:text:clear
    const val SCR = "SCR"  // Scroll: SCR:requestId:tabId:direction:amount
    const val FND = "FND"  // Find Elements: FND:requestId:tabId:selector
    const val ELM = "ELM"  // Element: ELM:elementId:tag:text:bounds:avid
    const val PGC = "PGC"  // Page Content: PGC:requestId:tabId:html:text

    // Downloads
    const val DLS = "DLS"  // Download Start: DLS:requestId:url:filename
    const val DLT = "DLT"  // Download Status: DLT:downloadId:state:progress:bytes

    // Events
    const val EVT = "EVT"  // Event: EVT:tabId:type:timestamp:data
}

/**
 * AVU 2.1 Protocol encoder/decoder
 */
object AvuProtocol {
    private const val DELIMITER = ':'
    private const val FIELD_SEPARATOR = '|'

    /**
     * Escape special characters per AVU 2.1 spec
     */
    fun escape(value: String): String = value
        .replace("%", "%25")  // Escape % first
        .replace(":", "%3A")
        .replace("\n", "%0A")
        .replace("\r", "%0D")
        .replace("|", "%7C")

    /**
     * Unescape special characters
     */
    fun unescape(value: String): String = value
        .replace("%3A", ":")
        .replace("%0A", "\n")
        .replace("%0D", "\r")
        .replace("%7C", "|")
        .replace("%25", "%")  // Unescape % last

    /**
     * Parse AVU message into code and fields
     */
    fun parse(message: String): AvuMessage {
        val parts = message.split(DELIMITER)
        if (parts.isEmpty()) {
            return AvuMessage("ERR", listOf("parse", "Empty message"))
        }
        return AvuMessage(
            code = parts[0],
            fields = parts.drop(1).map { unescape(it) }
        )
    }

    /**
     * Encode code and fields into AVU message
     */
    fun encode(code: String, vararg fields: String): String {
        return buildString {
            append(code)
            fields.forEach { field ->
                append(DELIMITER)
                append(escape(field))
            }
        }
    }

    /**
     * Encode multiple values as pipe-separated list
     */
    fun encodeList(items: List<String>): String {
        return items.joinToString(FIELD_SEPARATOR.toString()) { escape(it) }
    }

    /**
     * Decode pipe-separated list
     */
    fun decodeList(encoded: String): List<String> {
        if (encoded.isEmpty()) return emptyList()
        return encoded.split(FIELD_SEPARATOR).map { unescape(it) }
    }

    // ========================================================================
    // WebAvanue Message Encoders
    // ========================================================================

    /**
     * Encode GetTabs request
     */
    fun encodeGetTabs(requestId: String): String =
        encode(AvuCodes.GTB, requestId)

    /**
     * Encode Tab list response
     */
    fun encodeTabList(requestId: String, tabs: List<TabInfo>, activeTabId: String?): String {
        val tabsEncoded = tabs.map { encodeTabInfo(it) }
        return encode(AvuCodes.TBL, requestId, encodeList(tabsEncoded), activeTabId ?: "")
    }

    /**
     * Encode single tab info
     */
    fun encodeTabInfo(tab: TabInfo): String =
        "${tab.tabId}~${escape(tab.url)}~${escape(tab.title)}~${tab.isActive}~${tab.index}"

    /**
     * Decode tab info
     */
    fun decodeTabInfo(encoded: String): TabInfo? {
        val parts = encoded.split("~")
        if (parts.size < 5) return null
        return TabInfo(
            tabId = parts[0],
            url = unescape(parts[1]),
            title = unescape(parts[2]),
            isActive = parts[3].toBoolean(),
            index = parts[4].toIntOrNull() ?: 0
        )
    }

    /**
     * Encode CreateTab request
     */
    fun encodeCreateTab(requestId: String, url: String, makeActive: Boolean): String =
        encode(AvuCodes.CTB, requestId, url, makeActive.toString())

    /**
     * Encode CloseTab request
     */
    fun encodeCloseTab(requestId: String, tabId: String): String =
        encode(AvuCodes.CLT, requestId, tabId)

    /**
     * Encode SwitchTab request
     */
    fun encodeSwitchTab(requestId: String, tabId: String): String =
        encode(AvuCodes.SWT, requestId, tabId)

    /**
     * Encode Navigate request
     */
    fun encodeNavigate(requestId: String, tabId: String, url: String): String =
        encode(AvuCodes.NAV, requestId, tabId, url)

    /**
     * Encode GoBack request
     */
    fun encodeGoBack(requestId: String, tabId: String): String =
        encode(AvuCodes.GBK, requestId, tabId)

    /**
     * Encode GoForward request
     */
    fun encodeGoForward(requestId: String, tabId: String): String =
        encode(AvuCodes.GFW, requestId, tabId)

    /**
     * Encode Reload request
     */
    fun encodeReload(requestId: String, tabId: String, hard: Boolean): String =
        encode(AvuCodes.RLD, requestId, tabId, hard.toString())

    /**
     * Encode Scroll request
     */
    fun encodeScroll(requestId: String, tabId: String, direction: ScrollDirection, amount: Int): String =
        encode(AvuCodes.SCR, requestId, tabId, direction.name, amount.toString())

    /**
     * Encode Click request
     */
    fun encodeClick(requestId: String, tabId: String, selector: String): String =
        encode(AvuCodes.CLK, requestId, tabId, selector)

    /**
     * Encode Type request
     */
    fun encodeType(requestId: String, tabId: String, selector: String, text: String, clear: Boolean): String =
        encode(AvuCodes.TYP, requestId, tabId, selector, text, clear.toString())

    /**
     * Encode VoiceCommand request
     */
    fun encodeVoiceCommand(requestId: String, command: String, tabId: String?, params: Map<String, String>): String {
        val paramsEncoded = params.entries.joinToString(",") { "${escape(it.key)}=${escape(it.value)}" }
        return encode(AvuCodes.VCM, requestId, command, tabId ?: "", paramsEncoded)
    }

    /**
     * Encode Accept response
     */
    fun encodeAccept(requestId: String): String =
        encode(AvuCodes.ACC, requestId)

    /**
     * Encode Accept with Data response
     */
    fun encodeAcceptWithData(requestId: String, data: String): String =
        encode(AvuCodes.ACD, requestId, data)

    /**
     * Encode Error response
     */
    fun encodeError(requestId: String, code: Int, message: String): String =
        encode(AvuCodes.ERR, requestId, code.toString(), message)

    /**
     * Encode Element info
     */
    fun encodeElement(element: PageElement): String =
        encode(
            AvuCodes.ELM,
            element.elementId,
            element.tag,
            element.text,
            "${element.bounds.x},${element.bounds.y},${element.bounds.width},${element.bounds.height}",
            element.avid ?: ""
        )

    /**
     * Encode Event
     */
    fun encodeEvent(event: WebAvanueEvent): String = when (event) {
        is WebAvanueEvent.PageLoaded ->
            encode(AvuCodes.EVT, event.tabId, "PAGE_LOADED", event.timestamp.toString(), "${event.url}~${event.title}")
        is WebAvanueEvent.NavigationStarted ->
            encode(AvuCodes.EVT, event.tabId, "NAV_START", event.timestamp.toString(), event.url)
        is WebAvanueEvent.TabCreated ->
            encode(AvuCodes.EVT, event.tabId, "TAB_CREATED", event.timestamp.toString(), "")
        is WebAvanueEvent.TabClosed ->
            encode(AvuCodes.EVT, event.tabId, "TAB_CLOSED", event.timestamp.toString(), "")
        is WebAvanueEvent.TabActivated ->
            encode(AvuCodes.EVT, event.tabId, "TAB_ACTIVE", event.timestamp.toString(), "")
        is WebAvanueEvent.DownloadProgress ->
            encode(AvuCodes.EVT, event.tabId, "DL_PROGRESS", event.timestamp.toString(), "${event.downloadId}~${event.progress}")
    }
}

/**
 * Parsed AVU message
 */
data class AvuMessage(
    val code: String,
    val fields: List<String>
) {
    fun getField(index: Int, default: String = ""): String =
        fields.getOrNull(index) ?: default

    fun getIntField(index: Int, default: Int = 0): Int =
        fields.getOrNull(index)?.toIntOrNull() ?: default

    fun getBoolField(index: Int, default: Boolean = false): Boolean =
        fields.getOrNull(index)?.toBoolean() ?: default

    fun getLongField(index: Int, default: Long = 0L): Long =
        fields.getOrNull(index)?.toLongOrNull() ?: default
}
