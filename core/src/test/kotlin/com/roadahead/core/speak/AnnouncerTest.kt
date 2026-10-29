package com.roadahead.core.speak

import com.roadahead.core.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.time.*
import java.io.ByteArrayOutputStream
import java.util.zip.*

class AnnouncerTest {
    private val sig = Advice("sig:a:red", Priority.SIGNAL, "s"); private val high = Advice("fog:1:DANGER", Priority.HIGH, "h")
    private val normal = Advice("evt:1:NORMAL", Priority.NORMAL, "n")

    @Test fun `highest priority first then the next`() {
        val a = Announcer()
        assertEquals(high, a.pick(listOf(normal, sig, high), false, false, 0))
        assertEquals(sig, a.pick(listOf(normal, sig, high), false, false, 10))
        assertEquals(normal, a.pick(listOf(normal, sig, high), false, false, 20))
        assertNull(a.pick(listOf(normal, sig, high), false, false, 30))
    }
    @Test fun `waits for navigation voice and a short gap`() {
        val a = Announcer()
        assertNull(a.pick(listOf(sig), naviSpeaking = true, selfSpeaking = false, nowMonoMs = 0))
        assertNull(a.pick(listOf(sig), false, false, 150))
        assertEquals(sig, a.pick(listOf(sig), false, false, 200))
    }
    @Test fun `own speech blocks the next one`() { assertNull(Announcer().pick(listOf(sig), false, true, 0)) }
    @Test fun `advice that expired while waiting is never spoken`() {
        val a = Announcer(); assertNull(a.pick(listOf(sig), true, false, 0))
        assertNull(a.pick(emptyList(), false, false, 500))
    }
    @Test fun `key can return after a minute away`() {
        val a = Announcer(); assertEquals(sig, a.pick(listOf(sig), false, false, 0))
        assertNull(a.pick(listOf(sig), false, false, 1_000))
        assertNull(a.pick(emptyList(), false, false, 61_000))
        assertNull(a.pick(emptyList(), false, false, 61_001))
        assertEquals(sig, a.pick(listOf(sig), false, false, 61_002))
    }
}
