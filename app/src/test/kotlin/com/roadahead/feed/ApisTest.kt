package com.roadahead.feed

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ApisTest {
    @Test fun `request encodes keys once and logs no credentials`() {
        val url = requestUrl("https://example.test/api", mapOf("serviceKey" to "abc+/=", "stdgCd" to "3100000000"))
        assertEquals("https://example.test/api?serviceKey=abc%2B%2F%3D&stdgCd=3100000000", url)
        assertEquals("https://example.test/api?serviceKey=***&stdgCd=3100000000", maskedUrl(url))
        assertEquals("https://example.test/api?apiKey=***&authKey=***", maskedUrl("https://example.test/api?apiKey=secret&authKey=other"))
    }
    @Test fun `encoded portal key is not double encoded`() {
        val url = requestUrl("https://example.test/api", mapOf("serviceKey" to "abc%2B%2F%3D"))
        assertEquals("https://example.test/api?serviceKey=abc%2B%2F%3D", url)
    }
    @Test fun `weather production time is the requested UTC slot`() {
        assertEquals("2026-10-28T07:05:00Z", weatherProduced("202610280705").toString())
    }
}
