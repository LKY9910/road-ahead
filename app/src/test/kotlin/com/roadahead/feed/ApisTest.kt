package com.roadahead.feed

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import com.roadahead.core.model.GeoPoint
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

class ApisTest {
    @Test fun `seoul url is masked in logs`() {
        val masked = maskedSeoulUrl(seoulUrl("abc123"))
        assertEquals("http://openapi.seoul.go.kr:8088/***/json/GetParkingInfo/1/1000/", masked)
        assertFalse(masked.contains("abc123"))
    }

    @Test fun `forecast params`() {
        assertEquals(mapOf("pageNo" to "1", "numOfRows" to "300", "dataType" to "JSON",
            "base_date" to "20261029", "base_time" to "1700", "nx" to "60", "ny" to "127"),
            forecastParams(GeoPoint(37.5663, 126.9779), LocalDateTime.parse("2026-10-29T19:36")))
    }
    @Test fun `tour queries round location and use the arrival date`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals(mapOf("MobileOS" to "AND", "MobileApp" to "RoadAhead", "_type" to "json", "pageNo" to "1",
                "mapX" to "126.979", "mapY" to "37.567", "radius" to "3000", "arrange" to "E", "numOfRows" to "300"),
                tourPlaceParams(GeoPoint(37.5672159, 126.9788)))
            assertEquals(mapOf("MobileOS" to "AND", "MobileApp" to "RoadAhead", "_type" to "json", "pageNo" to "1",
                "eventStartDate" to "20261030", "eventEndDate" to "20261030", "numOfRows" to "1000"),
                tourFestivalParams(LocalDate.of(2026, 10, 30)))
        } finally { Locale.setDefault(original) }
    }
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
