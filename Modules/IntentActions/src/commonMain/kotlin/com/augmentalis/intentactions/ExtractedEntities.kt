package com.augmentalis.intentactions

/**
 * Container for entities extracted from a user utterance by NLU.
 *
 * The NLU layer populates this before dispatching to an IIntentAction.
 * Each field corresponds to an EntityType.
 */
data class ExtractedEntities(
    val query: String? = null,
    val url: String? = null,
    val phoneNumber: String? = null,
    val recipientName: String? = null,
    val recipientEmail: String? = null,
    val message: String? = null,
    val location: String? = null,
    val time: String? = null,
    val duration: String? = null,
    val appName: String? = null
) {
    companion object {
        fun fromMap(map: Map<String, String>): ExtractedEntities = ExtractedEntities(
            query = map["query"],
            url = map["url"],
            phoneNumber = map["phoneNumber"],
            recipientName = map["recipientName"],
            recipientEmail = map["recipientEmail"],
            message = map["message"],
            location = map["location"],
            time = map["time"],
            duration = map["duration"],
            appName = map["appName"]
        )
    }

    fun toMap(): Map<String, String> = buildMap {
        query?.let { put("query", it) }
        url?.let { put("url", it) }
        phoneNumber?.let { put("phoneNumber", it) }
        recipientName?.let { put("recipientName", it) }
        recipientEmail?.let { put("recipientEmail", it) }
        message?.let { put("message", it) }
        location?.let { put("location", it) }
        time?.let { put("time", it) }
        duration?.let { put("duration", it) }
        appName?.let { put("appName", it) }
    }
}
