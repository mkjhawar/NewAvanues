package com.augmentalis.httpavanue.middleware

import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse

/**
 * Cookie middleware — full HTTP cookie support.
 *
 * Closes NanoHTTPD gap #2: cookie handling with all attributes
 * (Path, Secure, HttpOnly, SameSite, Domain, Max-Age) — exceeds
 * NanoHTTPD's limited CookieHandler which lacked most of these.
 */

enum class SameSite { Strict, Lax, None }

data class Cookie(
    val name: String,
    val value: String,
    val maxAge: Long? = null,
    val path: String? = null,
    val domain: String? = null,
    val secure: Boolean = false,
    val httpOnly: Boolean = false,
    val sameSite: SameSite? = null,
) {
    /** Serialize to Set-Cookie header value */
    fun toHeaderValue(): String = buildString {
        append("$name=$value")
        maxAge?.let { append("; Max-Age=$it") }
        path?.let { append("; Path=$it") }
        domain?.let { append("; Domain=$it") }
        if (secure) append("; Secure")
        if (httpOnly) append("; HttpOnly")
        sameSite?.let { append("; SameSite=${it.name}") }
    }
}

/** Parse cookies from the Cookie request header. */
fun HttpRequest.cookies(): Map<String, String> {
    val cookieHeader = header("Cookie") ?: return emptyMap()
    return cookieHeader.split(';')
        .associate { part ->
            val eqIdx = part.indexOf('=')
            if (eqIdx > 0) {
                part.substring(0, eqIdx).trim() to part.substring(eqIdx + 1).trim()
            } else {
                part.trim() to ""
            }
        }
}

/** Get a single cookie value by name. */
fun HttpRequest.cookie(name: String): String? = cookies()[name]

/** Add a Set-Cookie header to the response. */
fun HttpResponse.withCookie(cookie: Cookie): HttpResponse {
    val existing = headers["Set-Cookie"]
    val newValue = if (existing != null) "$existing, ${cookie.toHeaderValue()}" else cookie.toHeaderValue()
    return copy(
        headers = headers + ("Set-Cookie" to newValue),
    )
}

/** Expire (delete) a cookie by name. */
fun HttpResponse.withoutCookie(name: String, path: String? = "/"): HttpResponse {
    return withCookie(Cookie(name = name, value = "", maxAge = 0, path = path))
}
