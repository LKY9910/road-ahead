package com.roadahead.core.signal

import com.roadahead.core.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.time.*
import java.io.ByteArrayOutputStream
import java.util.zip.*

class SignalTrackerTest {
    private val t0 = LocalDateTime.of(2026, 10, 28, 16, 6, 47)
    private fun snap(sec: Long, raw: Long?, color: LightColor = LightColor.RED, mono: Long = sec * 1000) =
        SignalSnapshot("3100000000", "1", t0.plusSeconds(sec), mono, listOf(DirectionSignal(Compass8.N, Movement.STRAIGHT, color, raw)))
    private fun SignalTracker.read(now: Long) = reading("3100000000:1", Compass8.N, Movement.STRAIGHT, now)

    @Test fun `three samples at 1000 per second calibrate`() {
        val t = SignalTracker(); listOf(snap(0, 10_000), snap(1, 9_000), snap(2, 8_000)).forEach(t::accept)
        val r = t.read(2_500)!!
        assertEquals(LightColor.RED, r.color); assertEquals(7.5, r.remainingSec!!, 0.01)
    }
    @Test fun `100 per second scale also calibrates`() {
        val t = SignalTracker(); listOf(snap(0, 1_000), snap(1, 900), snap(2, 800)).forEach(t::accept)
        assertEquals(8.0, t.read(2_000)!!.remainingSec!!, 0.01)
    }
    @Test fun `two samples give color without seconds`() {
        val t = SignalTracker(); listOf(snap(0, 10_000), snap(1, 9_000)).forEach(t::accept)
        assertEquals(SignalReading(LightColor.RED, null), t.read(1_000))
    }
    @Test fun `frozen value never calibrates`() {
        val t = SignalTracker(); listOf(snap(0, 255_000), snap(1, 255_000), snap(2, 255_000)).forEach(t::accept)
        assertNull(t.read(2_000)!!.remainingSec)
    }
    @Test fun `stale after three seconds without newer production time`() {
        val t = SignalTracker(); listOf(snap(0, 10_000), snap(1, 9_000), snap(2, 8_000)).forEach(t::accept)
        t.accept(snap(2, 8_000, mono = 4_000))
        assertNotNull(t.read(5_000)); assertNull(t.read(5_001))
    }
    @Test fun `scale survives a phase change`() {
        val t = SignalTracker(); listOf(snap(0, 3_000), snap(1, 2_000), snap(2, 1_000)).forEach(t::accept)
        t.accept(snap(3, 30_000, LightColor.GREEN))
        assertEquals(SignalReading(LightColor.GREEN, 30.0), t.read(3_000))
    }
    @Test fun `live keys list intersections seen`() {
        val t = SignalTracker(); t.accept(snap(0, 1_000)); assertEquals(setOf("3100000000:1"), t.liveKeys())
    }
    @Test fun `one glitch recovers after three good samples`() {
        val t = SignalTracker()
        listOf(snap(0, 10_000), snap(1, 9_000), snap(2, 8_000)).forEach(t::accept)
        assertEquals(SignalReading(LightColor.RED, 8.0), t.read(2_000))
        t.accept(snap(3, 100_000))
        assertEquals(SignalReading(LightColor.RED, null), t.read(3_000))
        listOf(snap(4, 6_000), snap(5, 5_000)).forEach(t::accept)
        assertNull(t.read(5_000)!!.remainingSec)
        t.accept(snap(6, 4_000))
        assertEquals(SignalReading(LightColor.RED, 4.0), t.read(6_000))
    }
    @Test fun `implausible remaining is unknown`() {
        val t = SignalTracker()
        listOf(snap(0, 257_000), snap(1, 256_000), snap(2, 255_000)).forEach(t::accept)
        assertEquals(SignalReading(LightColor.RED, null), t.read(2_000))
        t.accept(snap(3, 201_000, LightColor.GREEN))
        assertEquals(SignalReading(LightColor.GREEN, null), t.read(3_000))
        assertEquals(SignalReading(LightColor.GREEN, 200.0), t.read(4_000))
    }
}
