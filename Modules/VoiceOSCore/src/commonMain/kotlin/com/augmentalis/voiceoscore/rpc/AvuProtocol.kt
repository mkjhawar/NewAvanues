/**
 * AvuProtocol.kt - AVU 2.1 Protocol encoder/decoder for VoiceOS
 *
 * AVU (Avanues Universal) 2.1 is a compact line-based protocol for RPC:
 * - Format: CODE:field1:field2:field3:...
 * - Escape sequences: %3A for :, %25 for %, %0A for newline
 * - Optimized for voice command and accessibility operations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceoscore.rpc

import com.augmentalis.voiceoscore.rpc.messages.*

/**
 * AVU 2.1 operation codes for VoiceOS
 */
object VoiceOSAvuCodes {
    // Service Status
    const val GST = "GST"  // Get Status
    const val SST = "SST"  // Status Response

    // Voice Commands
    const val VCM = "VCM"  // Voice Command
    const val VCR = "VCR"  // Voice Command Response

    // Accessibility Actions
    const val AAC = "AAC"  // Accessibility Action
    const val AAR = "AAR"  // Action Response

    // Screen Scraping
    const val SSC = "SSC"  // Scrape Screen
    const val SCR = "SCR"  // Screen Response
    const val SEL = "SEL"  // Scraped Element

    // Recognition Control
    const val SRC = "SRC"  // Start Recognition
    const val PRC = "PRC"  // Stop Recognition
    const val RCR = "RCR"  // Recognition Response

    // App Learning
    const val LAP = "LAP"  // Learn App
    const val GAP = "GAP"  // Get Learned Apps
    const val GCM = "GCM"  // Get Commands
    const val APR = "APR"  // App Response
    const val CMD = "CMD"  // Command Definition

    // Dynamic Commands
    const val RGC = "RGC"  // Register Command
    const val URC = "URC"  // Unregister Command

    // Events
    const val EVT = "EVT"  // Event
    const val ERR = "ERR"  // Error

    // Generic
    const val ACK = "ACK"  // Acknowledgment
    const val NAK = "NAK"  // Negative Acknowledgment
}

/**
 * AVU 2.1 Protocol encoder for VoiceOS
 */
object VoiceOSAvuEncoder {

    /**
     * Escape special characters for AVU 2.1
     */
    fun escape(value: String): String = value
        .replace("%", "%25")
        .replace(":", "%3A")
        .replace("\n", "%0A")
        .replace("\r", "%0D")

    /**
     * Encode service status request
     * Format: GST:requestId
     */
    fun encodeStatusRequest(requestId: String): String =
        "${VoiceOSAvuCodes.GST}:${escape(requestId)}"

    /**
     * Encode service status response
     * Format: SST:requestId:isReady:isA11y:isVoiceActive:language:version:capabilities
     */
    fun encodeStatusResponse(status: ServiceStatus): String = buildString {
        append(VoiceOSAvuCodes.SST)
        append(":").append(escape(status.requestId))
        append(":").append(if (status.isReady) "1" else "0")
        append(":").append(if (status.isAccessibilityEnabled) "1" else "0")
        append(":").append(if (status.isVoiceRecognitionActive) "1" else "0")
        append(":").append(escape(status.currentLanguage))
        append(":").append(escape(status.version))
        append(":").append(status.capabilities.joinToString(",") { escape(it) })
    }

    /**
     * Encode voice command request
     * Format: VCM:requestId:commandText:context_key1=val1,key2=val2
     */
    fun encodeVoiceCommand(request: VoiceCommandRequest): String = buildString {
        append(VoiceOSAvuCodes.VCM)
        append(":").append(escape(request.requestId))
        append(":").append(escape(request.commandText))
        append(":").append(encodeMap(request.context))
    }

    /**
     * Encode voice command response
     * Format: VCR:requestId:success:action:result:error
     */
    fun encodeVoiceCommandResponse(response: VoiceCommandResponse): String = buildString {
        append(VoiceOSAvuCodes.VCR)
        append(":").append(escape(response.requestId))
        append(":").append(if (response.success) "1" else "0")
        append(":").append(escape(response.action ?: ""))
        append(":").append(escape(response.result ?: ""))
        append(":").append(escape(response.error ?: ""))
    }

    /**
     * Encode accessibility action request
     * Format: AAC:requestId:actionType:targetAvid:params
     */
    fun encodeAccessibilityAction(request: AccessibilityActionRequest): String = buildString {
        append(VoiceOSAvuCodes.AAC)
        append(":").append(escape(request.requestId))
        append(":").append(request.actionType.name)
        append(":").append(escape(request.targetAvid ?: ""))
        append(":").append(encodeMap(request.params))
    }

    /**
     * Encode accessibility action response
     * Format: AAR:requestId:success:result
     */
    fun encodeAccessibilityActionResponse(response: AccessibilityActionResponse): String = buildString {
        append(VoiceOSAvuCodes.AAR)
        append(":").append(escape(response.requestId))
        append(":").append(if (response.success) "1" else "0")
        append(":").append(escape(response.resultText ?: ""))
    }

    /**
     * Encode scrape screen request
     * Format: SSC:requestId:includeInvisible:maxDepth
     */
    fun encodeScrapeScreenRequest(request: ScrapeScreenRequest): String = buildString {
        append(VoiceOSAvuCodes.SSC)
        append(":").append(escape(request.requestId))
        append(":").append(if (request.includeInvisible) "1" else "0")
        append(":").append(request.maxDepth)
    }

    /**
     * Encode scraped element
     * Format: SEL:avid:className:text:contentDesc:bounds:flags
     */
    fun encodeScreenElement(element: ScreenElement): String = buildString {
        append(VoiceOSAvuCodes.SEL)
        append(":").append(escape(element.avid))
        append(":").append(escape(element.className))
        append(":").append(escape(element.text ?: ""))
        append(":").append(escape(element.contentDescription ?: ""))
        append(":").append("${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}")
        // Flags: clickable|scrollable|editable
        val flags = listOf(
            if (element.isClickable) "C" else "",
            if (element.isScrollable) "S" else "",
            if (element.isEditable) "E" else ""
        ).filter { it.isNotEmpty() }.joinToString("")
        append(":").append(flags)
    }

    /**
     * Encode start recognition request
     * Format: SRC:requestId:language:continuous
     */
    fun encodeStartRecognition(request: StartRecognitionRequest): String = buildString {
        append(VoiceOSAvuCodes.SRC)
        append(":").append(escape(request.requestId))
        append(":").append(escape(request.language))
        append(":").append(if (request.continuous) "1" else "0")
    }

    /**
     * Encode VoiceOS event
     * Format: EVT:timestamp:eventType:data...
     */
    fun encodeEvent(event: VoiceOSEvent): String = when (event) {
        is VoiceOSEvent.RecognitionStarted -> buildString {
            append(VoiceOSAvuCodes.EVT)
            append(":").append(event.timestamp)
            append(":RECOGNITION_STARTED")
            append(":").append(escape(event.language))
        }
        is VoiceOSEvent.RecognitionResult -> buildString {
            append(VoiceOSAvuCodes.EVT)
            append(":").append(event.timestamp)
            append(":RECOGNITION_RESULT")
            append(":").append(escape(event.transcript))
            append(":").append(event.confidence)
            append(":").append(if (event.isFinal) "1" else "0")
        }
        is VoiceOSEvent.RecognitionStopped -> buildString {
            append(VoiceOSAvuCodes.EVT)
            append(":").append(event.timestamp)
            append(":RECOGNITION_STOPPED")
            append(":").append(escape(event.reason))
        }
        is VoiceOSEvent.CommandExecuted -> buildString {
            append(VoiceOSAvuCodes.EVT)
            append(":").append(event.timestamp)
            append(":CMD_EXECUTED")
            append(":").append(escape(event.command))
            append(":").append(if (event.success) "1" else "0")
            append(":").append(escape(event.result ?: ""))
        }
        is VoiceOSEvent.ScreenChanged -> buildString {
            append(VoiceOSAvuCodes.EVT)
            append(":").append(event.timestamp)
            append(":SCREEN_CHANGED")
            append(":").append(escape(event.packageName))
            append(":").append(escape(event.activityName))
        }
        is VoiceOSEvent.AccessibilityStateChanged -> buildString {
            append(VoiceOSAvuCodes.EVT)
            append(":").append(event.timestamp)
            append(":A11Y_STATE")
            append(":").append(if (event.isEnabled) "1" else "0")
        }
    }

    /**
     * Encode generic response
     * Format: ACK:requestId:success or NAK:requestId:error
     */
    fun encodeResponse(response: VoiceOSResponse): String = if (response.success) {
        "${VoiceOSAvuCodes.ACK}:${escape(response.requestId)}:${escape(response.message ?: "")}"
    } else {
        "${VoiceOSAvuCodes.NAK}:${escape(response.requestId)}:${escape(response.error ?: "Unknown error")}"
    }

    private fun encodeMap(map: Map<String, String>): String =
        map.entries.joinToString(",") { "${escape(it.key)}=${escape(it.value)}" }
}

/**
 * AVU 2.1 Protocol decoder for VoiceOS
 */
object VoiceOSAvuDecoder {

    /**
     * Unescape AVU 2.1 encoded value
     */
    fun unescape(value: String): String = value
        .replace("%0D", "\r")
        .replace("%0A", "\n")
        .replace("%3A", ":")
        .replace("%25", "%")

    /**
     * Parse AVU message into code and fields
     */
    fun parse(message: String): AvuMessage? {
        val parts = message.split(":")
        if (parts.isEmpty()) return null
        return AvuMessage(
            code = parts[0],
            fields = parts.drop(1).map { unescape(it) }
        )
    }

    /**
     * Decode service status request
     */
    fun decodeStatusRequest(message: AvuMessage): ServiceStatusRequest? {
        if (message.code != VoiceOSAvuCodes.GST || message.fields.isEmpty()) return null
        return ServiceStatusRequest(requestId = message.fields[0])
    }

    /**
     * Decode voice command request
     */
    fun decodeVoiceCommand(message: AvuMessage): VoiceCommandRequest? {
        if (message.code != VoiceOSAvuCodes.VCM || message.fields.size < 2) return null
        return VoiceCommandRequest(
            requestId = message.fields[0],
            commandText = message.fields[1],
            context = if (message.fields.size > 2) decodeMap(message.fields[2]) else emptyMap()
        )
    }

    /**
     * Decode voice command response
     */
    fun decodeVoiceCommandResponse(message: AvuMessage): VoiceCommandResponse? {
        if (message.code != VoiceOSAvuCodes.VCR || message.fields.size < 2) return null
        return VoiceCommandResponse(
            requestId = message.fields[0],
            success = message.fields[1] == "1",
            action = message.fields.getOrNull(2)?.takeIf { it.isNotEmpty() },
            result = message.fields.getOrNull(3)?.takeIf { it.isNotEmpty() },
            error = message.fields.getOrNull(4)?.takeIf { it.isNotEmpty() }
        )
    }

    /**
     * Decode accessibility action request
     */
    fun decodeAccessibilityAction(message: AvuMessage): AccessibilityActionRequest? {
        if (message.code != VoiceOSAvuCodes.AAC || message.fields.size < 2) return null
        return AccessibilityActionRequest(
            requestId = message.fields[0],
            actionType = try {
                AccessibilityActionType.valueOf(message.fields[1])
            } catch (e: Exception) {
                AccessibilityActionType.CLICK
            },
            targetAvid = message.fields.getOrNull(2)?.takeIf { it.isNotEmpty() },
            params = if (message.fields.size > 3) decodeMap(message.fields[3]) else emptyMap()
        )
    }

    /**
     * Decode scrape screen request
     */
    fun decodeScrapeScreenRequest(message: AvuMessage): ScrapeScreenRequest? {
        if (message.code != VoiceOSAvuCodes.SSC || message.fields.isEmpty()) return null
        return ScrapeScreenRequest(
            requestId = message.fields[0],
            includeInvisible = message.fields.getOrNull(1) == "1",
            maxDepth = message.fields.getOrNull(2)?.toIntOrNull() ?: 10
        )
    }

    /**
     * Decode scraped element
     */
    fun decodeScreenElement(message: AvuMessage): ScreenElement? {
        if (message.code != VoiceOSAvuCodes.SEL || message.fields.size < 4) return null
        val boundsStr = message.fields.getOrNull(4) ?: "0,0,0,0"
        val bounds = boundsStr.split(",").map { it.toIntOrNull() ?: 0 }
        val flags = message.fields.getOrNull(5) ?: ""

        return ScreenElement(
            avid = message.fields[0],
            className = message.fields[1],
            text = message.fields[2],
            contentDescription = message.fields[3],
            bounds = ElementBounds(
                left = bounds.getOrNull(0) ?: 0,
                top = bounds.getOrNull(1) ?: 0,
                right = bounds.getOrNull(2) ?: 0,
                bottom = bounds.getOrNull(3) ?: 0
            ),
            isClickable = flags.contains("C"),
            isScrollable = flags.contains("S"),
            isEditable = flags.contains("E")
        )
    }

    /**
     * Decode start recognition request
     */
    fun decodeStartRecognition(message: AvuMessage): StartRecognitionRequest? {
        if (message.code != VoiceOSAvuCodes.SRC || message.fields.isEmpty()) return null
        return StartRecognitionRequest(
            requestId = message.fields[0],
            language = message.fields.getOrNull(1) ?: "en-US",
            continuous = message.fields.getOrNull(2) == "1"
        )
    }

    /**
     * Decode generic response
     */
    fun decodeResponse(message: AvuMessage): VoiceOSResponse? {
        return when (message.code) {
            VoiceOSAvuCodes.ACK -> VoiceOSResponse(
                requestId = message.fields.getOrElse(0) { "" },
                success = true,
                message = message.fields.getOrNull(1)
            )
            VoiceOSAvuCodes.NAK -> VoiceOSResponse(
                requestId = message.fields.getOrElse(0) { "" },
                success = false,
                error = message.fields.getOrNull(1)
            )
            else -> null
        }
    }

    private fun decodeMap(encoded: String): Map<String, String> {
        if (encoded.isBlank()) return emptyMap()
        return encoded.split(",")
            .mapNotNull { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .toMap()
    }
}

/**
 * Parsed AVU message
 */
data class AvuMessage(
    val code: String,
    val fields: List<String>
)
