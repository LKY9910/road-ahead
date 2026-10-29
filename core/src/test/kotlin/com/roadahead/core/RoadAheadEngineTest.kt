package com.roadahead.core

import com.roadahead.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.*
import java.time.format.DateTimeFormatter

class RoadAheadEngineTest {
    private val intersection = Intersection("3100000000", "100", "울산대학교", GeoPoint(35.543293, 129.259894))
    private val t0 = LocalDateTime.of(2026, 10, 28, 16, 6, 47)
    private val at = t0.atZone(ZoneId.of("Asia/Seoul")).toInstant()
    private fun north(m: Double) = GeoPoint(intersection.point.lat + m / 111_195.0, intersection.point.lon)
    private fun inputs(m: Double = 200.0, bearing: Double? = 180.0, naviSpeaking: Boolean = false) =
        Inputs(NavMode.ROUTE, Turn(Movement.LEFT, m), Fix(north(m), bearing, 0), false, false, naviSpeaking, false)
    private fun engine() = RoadAheadEngine().also { it.onIntersections(listOf(intersection)) }
    private fun signalJson(id: String = "100", totDt: String = "20261028160647", vararg signals: Pair<String, Pair<String, String>>): String {
        val fields = signals.joinToString(",") { (prefix, state) ->
            "\"${prefix}SttsNm\":\"${state.first}\",\"${prefix}RmndCs\":\"${state.second}\""
        }
        return """{"header":{"resultCode":"K0"},"body":{"items":{"item":[{"stdgCd":"3100000000","crsrdId":"$id","totDt":"$totDt",$fields}]}}}"""
    }
    private fun accept(engine: RoadAheadEngine, sec: Long, initialRaw: Long = 40_000) {
        val remaining = (initialRaw - sec * 1000).toString()
        engine.onSignalPage(signalJson("100", t0.plusSeconds(sec).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
            "ntLtsg" to ("stop-And-Remain" to remaining), "ntStsg" to ("stop-And-Remain" to remaining)), sec * 1000)
    }
    private fun seed(engine: RoadAheadEngine, initialRaw: Long = 40_000) {
        (0L..2L).forEach { accept(engine, it, initialRaw) }
        engine.onSpeed(10.0, 2000)
        engine.tick(inputs(), 2000, at.plusSeconds(2))
    }
    private fun stop(engine: RoadAheadEngine, initialRaw: Long = 40_000) {
        (3L..5L).forEach { accept(engine, it, initialRaw) }
        (3000L..5200L step 200).forEach { engine.onSpeed(0.0, it) }
    }

    @Test fun `scene A from approach to waiting`() {
        val engine = engine()
        (0L..2L).forEach { accept(engine, it) }
        engine.onSpeed(10.0, 2000)
        assertEquals(Advice("sig:3100000000:100:red", Priority.SIGNAL, "앞 신호 빨간불이에요. 속도를 줄이세요."),
            engine.tick(inputs(), 2000, at.plusSeconds(2)).speak)
        assertNull(engine.tick(inputs(), 2250, at.plusMillis(2250)).speak)
        stop(engine)
        val waiting = engine.tick(inputs(30.0), 5200, at.plusMillis(5200))
        assertEquals(Advice("sig:3100000000:100:wait", Priority.SIGNAL, "좌회전 신호까지 34초 남았어요."), waiting.speak)
        assertEquals(SignalCard(Movement.LEFT, LightColor.RED, 34, 30), waiting.screen.signal)
        (6L..36L).forEach { accept(engine, it); engine.onSpeed(0.0, it * 1000) }
        assertEquals(Advice("sig:3100000000:100:soon", Priority.SIGNAL, "곧 좌회전 신호로 바뀌어요."),
            engine.tick(inputs(30.0), 36000, at.plusSeconds(36)).speak)
    }
    @Test fun `navigation voice delays and a stale advice is dropped`() {
        val engine = engine(); seed(engine, 14_000); stop(engine, 14_000)
        assertNull(engine.tick(inputs(30.0, naviSpeaking = true), 5200, at.plusMillis(5200)).speak)
        (6L..7L).forEach { accept(engine, it, 14_000); engine.onSpeed(0.0, it * 1000) }
        assertNull(engine.tick(inputs(30.0), 7000, at.plusSeconds(7)).speak)
        (8L..9L).forEach { accept(engine, it, 14_000); engine.onSpeed(0.0, it * 1000) }
        assertEquals(Advice("sig:3100000000:100:soon", Priority.SIGNAL, "곧 좌회전 신호로 바뀌어요."),
            engine.tick(inputs(30.0), 9000, at.plusSeconds(9)).speak)
    }
    @Test fun `stopped car keeps last moving bearing`() {
        val engine = engine(); seed(engine); stop(engine)
        val sideways = engine.tick(inputs(30.0, bearing = 90.0), 5200, at.plusMillis(5200))
        assertEquals(SignalCard(Movement.LEFT, LightColor.RED, 34, 30), sideways.screen.signal)
        val missing = engine.tick(inputs(30.0, bearing = null), 5250, at.plusMillis(5250))
        assertEquals(sideways.screen.signal, missing.screen.signal)
    }
    @Test fun `stopped car keeps polling with last moving bearing`() {
        val engine = engine(); seed(engine); stop(engine)
        assertTrue(engine.shouldPollSignals(inputs(800.0, bearing = null).fix, 5200, at.plusMillis(5200)))
        assertTrue(engine.shouldPollSignals(inputs(800.0, bearing = 90.0).fix, 5200, at.plusMillis(5200)))
    }
    @Test fun `signal budget resets at seoul midnight`() {
        val engine = engine()
        val before = Instant.parse("2026-10-28T14:59:00Z")
        val after = Instant.parse("2026-10-28T15:00:01Z")
        repeat(4500) { engine.recordSignalPoll(0, before) }
        assertFalse(engine.shouldPollSignals(inputs(800.0).fix, 2000, before))
        assertTrue(engine.tick(inputs(800.0), 2000, before).screen.signalBudgetExhausted)
        assertTrue(engine.shouldPollSignals(inputs(800.0).fix, 61000, after))
        assertFalse(engine.tick(inputs(800.0), 61000, after).screen.signalBudgetExhausted)
    }
    @Test fun `poll only when a known intersection is within a km ahead and two seconds passed`() {
        val engine = engine()
        assertFalse(engine.shouldPollSignals(inputs(1200.0).fix, 0, at))
        assertTrue(engine.shouldPollSignals(inputs(800.0).fix, 0, at))
        engine.recordSignalPoll(0, at)
        assertFalse(engine.shouldPollSignals(inputs(800.0).fix, 1900, at.plusMillis(1900)))
        assertTrue(engine.shouldPollSignals(inputs(800.0).fix, 2000, at.plusSeconds(2)))
    }
    @Test fun `stale signals disappear from the screen`() {
        val engine = engine(); seed(engine)
        assertThrows<ApiException> { engine.onSignalPage("<html>502 Bad Gateway</html>", 2500) }
        assertNotNull(engine.tick(inputs(), 3000, at.plusSeconds(3)).screen.signal)
        assertNull(engine.tick(inputs(), 5100, at.plusMillis(5100)).screen.signal)
    }
    @Test fun `weather danger is announced before a signal`() {
        val engine = engine()
        (0L..2L).forEach { accept(engine, it) }; engine.onSpeed(10.0, 2000)
        val fog = Hazard("fog:1", HazardKind.FOG, Severity.DANGER, intersection.point, north(-300.0), "", false, at)
        engine.onHazards(HazardSource.WEATHER, listOf(fog))
        val first = engine.tick(inputs(), 2000, at.plusSeconds(2))
        val expectedFog = Advice("fog:1:DANGER", Priority.HIGH, "200m 앞 짙은 안개 구간이에요. 앞이 200m도 안 보여요. 속도를 줄이세요.")
        assertEquals(expectedFog, first.speak); assertEquals(expectedFog.text, first.screen.hazardText)
        assertEquals(Advice("sig:3100000000:100:red", Priority.SIGNAL, "앞 신호 빨간불이에요. 속도를 줄이세요."),
            engine.tick(inputs(), 2250, at.plusMillis(2250)).speak)
    }
}
