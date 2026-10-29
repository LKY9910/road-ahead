package com.roadahead.core.hazard

import com.roadahead.core.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.time.*
import java.io.ByteArrayOutputStream
import java.util.zip.*

class ItsParserTest {
    private val at = Instant.parse("2026-10-28T07:30:00Z")
    private fun fixture(n: String) = javaClass.getResource("/fixtures/$n")!!.readText()

    @Test fun `events keep construction and accidents`() {
        val events = parseItsEvents(fixture("its-event.json"), at)
        assertEquals(159, events.size)
        val first = events.first { it.key == "evt:2240032401:20261028091822:공사" }
        assertEquals(HazardKind.CONSTRUCTION, first.kind); assertEquals(Severity.CAUTION, first.severity)
        assertEquals(GeoPoint(37.45781, 126.785426), first.from); assertEquals(first.from, first.to)
        assertEquals("4차로 차단", first.detail); assertFalse(first.fullClosure)
    }
    @Test fun `full closure is danger`() {
        val json = """{"header":{"resultCode":0},"body":{"items":[{"eventType":"공사","startDate":"20261028090000","coordX":"127.0","coordY":"37.5","linkId":"9","lanesBlockType":"4","lanesBlocked":"","message":""}]}}"""
        val h = parseItsEvents(json, at).single(); assertTrue(h.fullClosure); assertEquals(Severity.DANGER, h.severity)
    }
    @Test fun `ended wildfire is skipped`() { assertEquals(emptyList<Hazard>(), parseItsDisasters(fixture("its-disaster.json"), at)) }
    @Test fun `active underpass flooding is a disaster`() {
        val json = """{"header":{"resultCode":0},"body":{"items":[{"eventId":"E1","eventType":"D03","status":"1","locationInfo":"127.01 37.51,127.02 37.52"}]}}"""
        val h = parseItsDisasters(json, at).single()
        assertEquals("dis:E1", h.key); assertEquals("지하차도 침수", h.detail); assertEquals(Severity.DANGER, h.severity)
        assertEquals(GeoPoint(37.51, 127.01), h.from); assertEquals(GeoPoint(37.52, 127.02), h.to)
    }
    @Test fun `malformed its body throws ApiException`() { assertThrows<ApiException> { parseItsEvents("<html></html>", at) } }
    @Test fun `failed result code throws`() {
        assertEquals("4009", assertThrows<ApiException> { parseItsEvents("""{"header":{"resultCode":4009,"resultMsg":"x"}}""", at) }.code)
    }
    @Test fun `its event without coordinates is skipped`() {
        val events = """{"header":{"resultCode":0},"body":{"items":[
            {"eventType":"공사","startDate":"20261028090000","coordX":"127.0","coordY":"37.5","linkId":"1"},
            {"eventType":"공사","startDate":"20261028090000","linkId":"9"},
            null,
            {"eventType":"교통사고","startDate":"20261028090000","coordX":"127.1","coordY":"37.6","linkId":"2"}
        ]}}"""
        assertEquals(listOf(
            "evt:1:20261028090000:공사" to GeoPoint(37.5, 127.0),
            "evt:2:20261028090000:교통사고" to GeoPoint(37.6, 127.1)
        ), parseItsEvents(events, at).map { it.key to it.from })

        val disasters = """{"header":{"resultCode":0},"body":{"items":[
            {"eventId":"E1","eventType":"D03","status":"1","locationInfo":"127.01 37.51,127.02 37.52"},
            {"eventId":"E9","eventType":"D03","status":"1"},
            {"eventId":"E8","eventType":"D03","status":"1","locationInfo":"broken"},
            null,
            {"eventId":"E2","eventType":"D04","status":"1","locationInfo":"127.03 37.53,127.04 37.54"}
        ]}}"""
        assertEquals(listOf(
            Triple("dis:E1", GeoPoint(37.51, 127.01), GeoPoint(37.52, 127.02)),
            Triple("dis:E2", GeoPoint(37.53, 127.03), GeoPoint(37.54, 127.04))
        ), parseItsDisasters(disasters, at).map { Triple(it.key, it.from, it.to) })
        assertThrows<ApiException> { parseItsDisasters("<html></html>", at) }
        assertEquals("4009", assertThrows<ApiException> { parseItsDisasters("""{"header":{"resultCode":4009}}""", at) }.code)
    }
    @Test fun `all broken its events throw`() {
        for (items in listOf(
            """[{"eventType":"공사"},{"eventType":"교통사고","coordX":"broken","coordY":"37.5"}]""",
            """[null,42]"""
        )) {
            val json = """{"header":{"resultCode":0},"body":{"items":$items}}"""
            assertEquals("parse", assertThrows<ApiException> { parseItsEvents(json, at) }.code)
        }
        val skipped = """{"header":{"resultCode":0},"body":{"items":[{"eventType":"기타"}]}}"""
        assertEquals(emptyList<Hazard>(), parseItsEvents(skipped, at))
        assertEquals(emptyList<Hazard>(), parseItsEvents(skipped.replace("\"기타\"}", "\"기타\"},null"), at))
        assertEquals(emptyList<Hazard>(), parseItsEvents("""{"header":{"resultCode":0},"body":{"items":[]}}""", at))
    }
    @Test fun `all broken disasters throw`() {
        for (items in listOf(
            """[{"eventType":"D03","status":"1"},{"eventType":"D04","status":"1","locationInfo":"broken"}]""",
            """[null,42]"""
        )) {
            val json = """{"header":{"resultCode":0},"body":{"items":$items}}"""
            assertEquals("parse", assertThrows<ApiException> { parseItsDisasters(json, at) }.code)
        }
        val skipped = """{"header":{"resultCode":0},"body":{"items":[
            {"eventType":"D01"},{"eventType":"D06","status":"3"},
            {"eventType":"D07","status":"2"},{"eventType":"D07","status":"3"}
        ]}}"""
        assertEquals(emptyList<Hazard>(), parseItsDisasters(skipped, at))
        assertEquals(emptyList<Hazard>(), parseItsDisasters(skipped.replace("\"D01\"}", "\"D01\"},null"), at))
        assertEquals(emptyList<Hazard>(), parseItsDisasters("""{"header":{"resultCode":0},"body":{"items":[]}}""", at))
    }
}
