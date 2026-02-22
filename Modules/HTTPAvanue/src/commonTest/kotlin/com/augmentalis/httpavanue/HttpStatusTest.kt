/**
 * HttpStatusTest.kt — Unit tests for HttpStatus
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-22
 *
 * Tests code/message properties, from() lookup, fallback behaviour,
 * and common status classes.
 */
package com.augmentalis.httpavanue

import com.augmentalis.httpavanue.http.HttpStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpStatusTest {

    // ── Code / Message Properties ─────────────────────────────────

    @Test
    fun `OK has code 200 and message OK`() {
        assertEquals(200, HttpStatus.OK.code)
        assertEquals("OK", HttpStatus.OK.message)
    }

    @Test
    fun `NOT_FOUND has code 404 and message Not Found`() {
        assertEquals(404, HttpStatus.NOT_FOUND.code)
        assertEquals("Not Found", HttpStatus.NOT_FOUND.message)
    }

    @Test
    fun `INTERNAL_SERVER_ERROR has code 500`() {
        assertEquals(500, HttpStatus.INTERNAL_SERVER_ERROR.code)
    }

    @Test
    fun `CREATED has code 201`() {
        assertEquals(201, HttpStatus.CREATED.code)
    }

    @Test
    fun `UNAUTHORIZED has code 401`() {
        assertEquals(401, HttpStatus.UNAUTHORIZED.code)
    }

    // ── from() Lookup ─────────────────────────────────────────────

    @Test
    fun `from returns correct entry for known status codes`() {
        assertEquals(HttpStatus.OK, HttpStatus.from(200))
        assertEquals(HttpStatus.NOT_FOUND, HttpStatus.from(404))
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.from(500))
        assertEquals(HttpStatus.CREATED, HttpStatus.from(201))
        assertEquals(HttpStatus.BAD_REQUEST, HttpStatus.from(400))
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.from(503))
    }

    @Test
    fun `from returns INTERNAL_SERVER_ERROR as fallback for unknown code`() {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.from(999))
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.from(0))
    }

    // ── Status Class Groupings ────────────────────────────────────

    @Test
    fun `2xx statuses all have codes in 200-299 range`() {
        val twoHundreds = listOf(
            HttpStatus.OK, HttpStatus.CREATED, HttpStatus.ACCEPTED,
            HttpStatus.NO_CONTENT, HttpStatus.PARTIAL_CONTENT
        )
        twoHundreds.forEach { status ->
            assertTrue(status.code in 200..299, "$status code ${status.code} not in 2xx")
        }
    }

    @Test
    fun `4xx statuses all have codes in 400-499 range`() {
        val fourHundreds = listOf(
            HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN,
            HttpStatus.NOT_FOUND, HttpStatus.METHOD_NOT_ALLOWED, HttpStatus.CONFLICT,
            HttpStatus.TOO_MANY_REQUESTS
        )
        fourHundreds.forEach { status ->
            assertTrue(status.code in 400..499, "$status code ${status.code} not in 4xx")
        }
    }

    @Test
    fun `5xx statuses all have codes in 500-599 range`() {
        val fiveHundreds = listOf(
            HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.NOT_IMPLEMENTED,
            HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT
        )
        fiveHundreds.forEach { status ->
            assertTrue(status.code in 500..599, "$status code ${status.code} not in 5xx")
        }
    }

    // ── All Entries Have Non-Blank Message ────────────────────────

    @Test
    fun `every HttpStatus entry has a non-blank message`() {
        HttpStatus.entries.forEach { status ->
            assertTrue(status.message.isNotBlank(), "Blank message for $status")
        }
    }

    // ── Redirect codes ────────────────────────────────────────────

    @Test
    fun `3xx redirect codes are correct`() {
        assertEquals(301, HttpStatus.MOVED_PERMANENTLY.code)
        assertEquals(302, HttpStatus.FOUND.code)
        assertEquals(304, HttpStatus.NOT_MODIFIED.code)
    }
}
