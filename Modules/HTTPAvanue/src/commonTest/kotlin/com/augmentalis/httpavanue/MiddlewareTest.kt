package com.augmentalis.httpavanue

import com.augmentalis.httpavanue.http.HttpMethod
import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse
import com.augmentalis.httpavanue.http.HttpStatus
import com.augmentalis.httpavanue.middleware.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class MiddlewareTest {

    private fun okHandler(): suspend (HttpRequest) -> HttpResponse = { _ ->
        HttpResponse.ok("Hello", "text/plain")
    }

    @Test
    fun testHstsMiddleware() = runBlocking {
        val middleware = hstsMiddleware()
        val response = middleware.handle(
            HttpRequest(method = HttpMethod.GET, uri = "/"),
            okHandler()
        )
        val hsts = response.header("Strict-Transport-Security")
        assertNotNull(hsts)
        assertTrue(hsts.contains("max-age=31536000"))
        assertTrue(hsts.contains("includeSubDomains"))
    }

    @Test
    fun testHstsWithPreload() = runBlocking {
        val middleware = hstsMiddleware(HstsConfig(preload = true))
        val response = middleware.handle(
            HttpRequest(method = HttpMethod.GET, uri = "/"),
            okHandler()
        )
        val hsts = response.header("Strict-Transport-Security")
        assertNotNull(hsts)
        assertTrue(hsts.contains("preload"))
    }

    @Test
    fun testAutoHeadStripsBody(): Unit = runBlocking {
        val middleware = autoHeadMiddleware()
        val response = middleware.handle(
            HttpRequest(method = HttpMethod.HEAD, uri = "/"),
            okHandler()
        )
        assertNull(response.body)
        assertNotNull(response.header("Content-Length"))
    }

    @Test
    fun testAutoHeadPassesGet(): Unit = runBlocking {
        val middleware = autoHeadMiddleware()
        val response = middleware.handle(
            HttpRequest(method = HttpMethod.GET, uri = "/"),
            okHandler()
        )
        assertNotNull(response.body)
    }

    @Test
    fun testDateHeaderMiddleware() = runBlocking {
        val middleware = dateHeaderMiddleware()
        val response = middleware.handle(
            HttpRequest(method = HttpMethod.GET, uri = "/"),
            okHandler()
        )
        val date = response.header("Date")
        assertNotNull(date)
        assertTrue(date.contains("GMT"))
    }

    @Test
    fun testDateHeaderNotOverwritten() = runBlocking {
        val middleware = dateHeaderMiddleware()
        val response = middleware.handle(
            HttpRequest(method = HttpMethod.GET, uri = "/"),
        ) { _ ->
            HttpResponse(
                status = 200, statusMessage = "OK",
                headers = mapOf("Date" to "Custom-Date"),
                body = null,
            )
        }
        assertEquals("Custom-Date", response.header("Date"))
    }

    @Test
    fun testETagAddsHeader() = runBlocking {
        val middleware = etagMiddleware()
        val response = middleware.handle(
            HttpRequest(method = HttpMethod.GET, uri = "/"),
            okHandler()
        )
        val etag = response.header("ETag")
        assertNotNull(etag)
        assertTrue(etag.startsWith("W/\""))
    }

    @Test
    fun testETagNotModified() = runBlocking {
        val middleware = etagMiddleware()
        // First request to get the ETag
        val firstResponse = middleware.handle(
            HttpRequest(method = HttpMethod.GET, uri = "/"),
            okHandler()
        )
        val etag = firstResponse.header("ETag")!!

        // Second request with If-None-Match
        val secondResponse = middleware.handle(
            HttpRequest(method = HttpMethod.GET, uri = "/", headers = mapOf("If-None-Match" to etag)),
            okHandler()
        )
        assertEquals(HttpStatus.NOT_MODIFIED.code, secondResponse.status)
        assertNull(secondResponse.body)
    }

    @Test
    fun testRangeMiddleware(): Unit = runBlocking {
        val middleware = rangeMiddleware()
        val response = middleware.handle(
            HttpRequest(method = HttpMethod.GET, uri = "/", headers = mapOf("Range" to "bytes=0-4")),
        ) { _ ->
            HttpResponse.ok("Hello, World!")
        }
        assertEquals(HttpStatus.PARTIAL_CONTENT.code, response.status)
        assertEquals("Hello", response.body?.decodeToString())
        assertNotNull(response.header("Content-Range"))
    }

    @Test
    fun testRangeMiddlewareNoRange() = runBlocking {
        val middleware = rangeMiddleware()
        val response = middleware.handle(
            HttpRequest(method = HttpMethod.GET, uri = "/"),
            okHandler()
        )
        assertEquals(HttpStatus.OK.code, response.status)
        assertEquals("bytes", response.header("Accept-Ranges"))
    }

    @Test
    fun testCookieParsing() {
        val request = HttpRequest(
            method = HttpMethod.GET, uri = "/",
            headers = mapOf("Cookie" to "session=abc123; theme=dark"),
        )
        val cookies = request.cookies()
        assertEquals("abc123", cookies["session"])
        assertEquals("dark", cookies["theme"])
    }

    @Test
    fun testCookieSetting() {
        val response = HttpResponse.ok("test")
        val withCookie = response.withCookie(Cookie(
            name = "session", value = "xyz",
            maxAge = 3600, path = "/", httpOnly = true, secure = true,
            sameSite = SameSite.Strict,
        ))
        val setCookie = withCookie.header("Set-Cookie")
        assertNotNull(setCookie)
        assertTrue(setCookie.contains("session=xyz"))
        assertTrue(setCookie.contains("Max-Age=3600"))
        assertTrue(setCookie.contains("HttpOnly"))
        assertTrue(setCookie.contains("Secure"))
        assertTrue(setCookie.contains("SameSite=Strict"))
    }

    @Test
    fun testCookieExpiration() {
        val response = HttpResponse.ok("test")
        val expired = response.withoutCookie("session")
        val setCookie = expired.header("Set-Cookie")
        assertNotNull(setCookie)
        assertTrue(setCookie.contains("Max-Age=0"))
    }

    @Test
    fun testForwardedHeaders() = runBlocking {
        val middleware = forwardedHeadersMiddleware(
            ForwardedHeadersConfig(trustedProxies = setOf("10.0.0.1"))
        )
        val response = middleware.handle(
            HttpRequest(
                method = HttpMethod.GET, uri = "/",
                headers = mapOf(
                    "X-Forwarded-For" to "203.0.113.50, 10.0.0.1",
                    "X-Forwarded-Proto" to "https",
                ),
                remoteAddress = "10.0.0.1:8080",
            ),
        ) { req ->
            // Verify the context was populated
            assertEquals("203.0.113.50", req.context["remote_address"])
            assertEquals("https", req.context["scheme"])
            HttpResponse.ok("ok")
        }
    }

    @Test
    fun testMultipartParsing() {
        // Build multipart body with explicit CRLF (not platform line endings)
        val body = "--boundary123\r\n" +
            "Content-Disposition: form-data; name=\"field1\"\r\n" +
            "\r\n" +
            "value1\r\n" +
            "--boundary123\r\n" +
            "Content-Disposition: form-data; name=\"file1\"; filename=\"test.txt\"\r\n" +
            "Content-Type: text/plain\r\n" +
            "\r\n" +
            "file content here\r\n" +
            "--boundary123--\r\n"

        val request = HttpRequest(
            method = HttpMethod.POST, uri = "/upload",
            headers = mapOf("Content-Type" to "multipart/form-data; boundary=boundary123"),
            body = body.encodeToByteArray(),
        )
        val parts = request.multipartParts()
        assertNotNull(parts)
        assertEquals(2, parts.size)
        assertEquals("field1", parts[0].name)
        assertEquals("value1", parts[0].asText())
        assertEquals("file1", parts[1].name)
        assertEquals("test.txt", parts[1].filename)
        assertEquals("text/plain", parts[1].contentType)
        assertEquals("file content here", parts[1].asText())
    }
}
