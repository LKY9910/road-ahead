package com.roadahead.core.signal

import com.roadahead.core.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.time.*
import java.io.ByteArrayOutputStream
import java.util.zip.*

class SignalParserTest {
    private fun fixture(n: String) = javaClass.getResource("/fixtures/$n")!!.readText()

    @Test fun `ulsan page parses intersections and directions`() {
        val page = parseSignalPage(fixture("signal-ulsan.json"), receivedMonoMs = 500)
        assertEquals(3, page.size)
        val first = page.first()
        assertEquals("3100000000:1", first.key)
        assertEquals(LocalDateTime.of(2026, 10, 28, 16, 6, 47), first.produced)
        assertEquals(500, first.receivedMonoMs)
        assertTrue(DirectionSignal(Compass8.N, Movement.STRAIGHT, LightColor.RED, 10_000) in first.signals)
        assertTrue(DirectionSignal(Compass8.E, Movement.LEFT, LightColor.RED, 111_000) in first.signals)
    }
    @Test fun `empty remaining becomes null`() {
        val json = """{"header":{"resultCode":"K0"},"body":{"items":{"item":[{"stdgCd":"3100000000","crsrdId":"9","totDt":"20261028160647","ntStsgSttsNm":"stop-And-Remain","ntStsgRmndCs":""}]}}}"""
        assertEquals(listOf(DirectionSignal(Compass8.N, Movement.STRAIGHT, LightColor.RED, null)), parseSignalPage(json, 0).single().signals)
    }
    @Test fun `no data code gives empty list`() { assertEquals(emptyList<SignalSnapshot>(), parseSignalPage(fixture("signal-k3.json"), 0)) }
    @Test fun `other result code throws`() {
        assertEquals("K99", assertThrows<ApiException> { parseSignalPage("""{"header":{"resultCode":"K99"}}""", 0) }.code)
    }
    @Test fun `html body throws ApiException`() { assertThrows<ApiException> { parseSignalPage("<html>502 Bad Gateway</html>", 0) } }
    @Test fun `map page gives names and points`() {
        val ojibeol = parseIntersectionPage(fixture("signal-map-ulsan.json")).first { it.id == "1" }
        assertEquals("오지벌사거리", ojibeol.name)
        assertEquals(35.512239, ojibeol.point.lat, 1e-6); assertEquals(129.430752, ojibeol.point.lon, 1e-6)
    }
    @Test fun `state names map to colors`() {
        assertEquals(LightColor.RED, colorOf("stop-And-Remain")); assertEquals(LightColor.RED, colorOf("stop-Then-Proceed"))
        assertEquals(LightColor.GREEN, colorOf("protected-Movement-Allowed")); assertEquals(LightColor.GREEN, colorOf("pre-Movement"))
        assertEquals(LightColor.YELLOW, colorOf("permissive-clearance")); assertEquals(LightColor.UNKNOWN, colorOf("dark"))
    }
    @Test fun `bad signal item is skipped`() {
        val json = """{"header":{"resultCode":"K0"},"body":{"items":{"item":[
            {"stdgCd":"3100000000","crsrdId":"1","totDt":"20261028160647","ntStsgSttsNm":"stop-And-Remain","ntStsgRmndCs":"10000"},
            {"stdgCd":"3100000000","crsrdId":"9","totDt":""},
            null,
            {"stdgCd":"3100000000","crsrdId":"8","totDt":{}},
            {"stdgCd":"3100000000","crsrdId":"2","totDt":"20261028160648","ntStsgSttsNm":"protected-Movement-Allowed","ntStsgRmndCs":"20000"}
        ]}}}"""
        val signals = parseSignalPage(json, 500)
        assertEquals(listOf("3100000000:1", "3100000000:2"), signals.map { it.key })
        assertEquals(listOf(DirectionSignal(Compass8.N, Movement.STRAIGHT, LightColor.RED, 10_000)), signals.first().signals)
        assertEquals(listOf(DirectionSignal(Compass8.N, Movement.STRAIGHT, LightColor.GREEN, 20_000)), signals.last().signals)

        val map = """{"header":{"resultCode":"K0"},"body":{"items":{"item":[
            {"stdgCd":"3100000000","crsrdId":"1","crsrdNm":"First","mapCtptIntLat":"35.5","mapCtptIntLot":"129.3"},
            {"stdgCd":"3100000000","crsrdId":"9","mapCtptIntLat":"","mapCtptIntLot":"129.3"},
            null,
            {"stdgCd":"3100000000","crsrdId":"2","crsrdNm":"Last","mapCtptIntLat":"35.6","mapCtptIntLot":"129.4"}
        ]}}}"""
        assertEquals(listOf(
            Intersection("3100000000", "1", "First", GeoPoint(35.5, 129.3)),
            Intersection("3100000000", "2", "Last", GeoPoint(35.6, 129.4))
        ), parseIntersectionPage(map))
    }
    @Test fun `all broken signal items throw`() {
        for (items in listOf(
            """[{"stdgCd":"3100000000","crsrdId":"9","totDt":""},{"totDt":{}}]""",
            """[{"stdgCd":"3100000000","crsrdId":"9","mapCtptIntLat":"","mapCtptIntLot":"129.3"},{}]""",
            """[null,42,"broken"]"""
        )) {
            val json = """{"header":{"resultCode":"K0"},"body":{"items":{"item":$items}}}"""
            assertEquals("parse", assertThrows<ApiException> { parseSignalPage(json, 0) }.code)
            assertEquals("parse", assertThrows<ApiException> { parseIntersectionPage(json) }.code)
        }
        val empty = """{"header":{"resultCode":"K0"},"body":{"items":{"item":[]}}}"""
        assertEquals(emptyList<SignalSnapshot>(), parseSignalPage(empty, 0))
        assertEquals(emptyList<Intersection>(), parseIntersectionPage(empty))
    }
}
