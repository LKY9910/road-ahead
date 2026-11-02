package com.roadahead.core

import com.roadahead.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.*
import java.time.format.DateTimeFormatter

class RoadAheadEngineTest {
    private val destinationNow = Instant.parse("2026-10-29T05:00:00Z")
    private val dest = Destination("d1", "시청", GeoPoint(35.5384, 129.3114), 900.0, 120)
    private fun destinationInputs(destination: Destination? = dest, parked: Boolean = false, travel: Boolean = true) =
        Inputs(NavMode.ROUTE, null, null, false, false, false, false,
            parked = parked, travelMode = travel, destination = destination)
    private fun parkingLot(destination: Destination = dest) = ParkingLot("lot", "시청 공영주차장",
        GeoPoint(destination.point.lat + 180.0 / 111_195.0, destination.point.lon), 23, destinationNow, 30, 1000)
    private fun tourItem(destination: Destination = dest) = TourItem("museum", TourKind.CULTURE, "시립 미술관",
        GeoPoint(destination.point.lat + 800.0 / 111_195.0, destination.point.lon), null, null)

    private fun tourData(vararg items: TourItem) = TourData(items.filter { it.kind != TourKind.FESTIVAL },
        items.filter { it.kind == TourKind.FESTIVAL }, emptySet())

    @Test fun `parking needs travel mode`() {
        val engine = RoadAheadEngine()
        assertNull(engine.parkingRequest(dest, NavMode.ROUTE, false))
        engine.onParkingLots(dest.key, listOf(parkingLot()))
        val off = destinationInputs(travel = false)
        assertNull(engine.tick(off, 0, destinationNow).screen.card)
        assertNull(engine.tick(off, 250, destinationNow).speak)
        assertEquals(dest, engine.parkingRequest(dest, NavMode.ROUTE, true))
        val on = engine.tick(off.copy(travelMode = true), 500, destinationNow)
        assertTrue(on.screen.card is Card.Parking)
        assertEquals("parking:d1", on.speak?.key)
    }

    @Test fun `announce waits for navi and speaks once`() {
        val engine = RoadAheadEngine()
        val question = Advice("companions:ask:1", Priority.NORMAL, "누구와 함께 가세요? 화면에서 골라 주세요.")
        engine.announce(question)
        val input = destinationInputs(null)
        assertNull(engine.tick(input.copy(naviSpeaking = true), 0, destinationNow).speak)
        assertEquals(question, engine.tick(input, 250, destinationNow).speak)
        assertNull(engine.tick(input, 500, destinationNow).speak)
        assertNull(engine.tick(input, 61_000, destinationNow).speak)
    }

    @Test fun `withdrawn question is not spoken`() {
        val engine = RoadAheadEngine()
        engine.announce(Advice("companions:ask:1", Priority.NORMAL, "누구와 함께 가세요? 화면에서 골라 주세요."))
        val input = destinationInputs(null)
        assertNull(engine.tick(input.copy(naviSpeaking = true), 0, destinationNow).speak)
        engine.withdraw("companions:ask:1")
        assertNull(engine.tick(input, 250, destinationNow).speak)
    }

    @Test fun `travel card includes weather note`() {
        val engine = RoadAheadEngine()
        engine.onTourData(dest.key, tourData(tourItem()).copy(weather = com.roadahead.core.forecast.BadWeather.RAIN))
        val card = engine.tick(destinationInputs(parked = true), 0, destinationNow).screen.card as Card.Travel
        assertEquals("도착 때 비 예보 · 실내 명소 우선", card.weatherNote)
    }

    @Test fun `parking and travel are repeated when a destination returns within a minute`() {
        val engine = RoadAheadEngine()
        engine.onParkingLots(dest.key, listOf(parkingLot()))
        engine.onTourData(dest.key, tourData(tourItem()))
        val input = destinationInputs(travel = true)
        assertEquals("parking:d1", engine.tick(input, 0, destinationNow).speak?.key)
        assertEquals("travel:d1", engine.tick(input, 250, destinationNow).speak?.key)
        assertNull(engine.tick(input.copy(destination = dest.copy(key = "d2")), 500, destinationNow).speak)
        assertEquals(dest, engine.parkingRequest(dest, NavMode.ROUTE, true))
        assertEquals(dest, engine.tourRequest(dest, true))
        engine.onParkingLots(dest.key, listOf(parkingLot()))
        engine.onTourData(dest.key, tourData(tourItem()))
        assertEquals("parking:d1", engine.tick(input, 1000, destinationNow).speak?.key)
        assertEquals("travel:d1", engine.tick(input, 1250, destinationNow).speak?.key)
    }
    @Test fun `destination changes preserve signal and hazard speech memory`() {
        val engine = engine()
        (0L..2L).forEach { accept(engine, it) }
        engine.onSpeed(10.0, 2000)
        engine.onHazards(HazardSource.WEATHER, listOf(Hazard("fog:1", HazardKind.FOG, Severity.DANGER,
            intersection.point, north(-300.0), "", false, destinationNow)))
        val input = inputs().copy(destination = dest)
        assertEquals(Priority.HIGH, engine.tick(input, 2000, destinationNow).speak?.priority)
        assertEquals(Priority.SIGNAL, engine.tick(input, 2250, destinationNow).speak?.priority)
        val changed = engine.tick(input.copy(destination = dest.copy(key = "d2")), 2500, destinationNow)
        assertNull(changed.speak)
        assertTrue(changed.screen.card is Card.Hazard)
    }

    @Test fun `parking requested once per destination`() {
        val engine = RoadAheadEngine()
        assertEquals(dest, engine.parkingRequest(dest, NavMode.ROUTE, true))
        assertNull(engine.parkingRequest(dest, NavMode.ROUTE, true))
        assertEquals(dest, engine.tourRequest(dest, true))
        assertNull(engine.tourRequest(dest, true))
        assertNull(engine.parkingRequest(dest, NavMode.ROUTE, true))
    }
    @Test fun `parking trigger uses remaining time when distance is missing`() {
        val engine = RoadAheadEngine()
        val timeOnly = dest.copy(name = null, remainDistanceM = null, remainTimeSec = 150)
        assertEquals(timeOnly, engine.parkingRequest(timeOnly, NavMode.ROUTE, true))
    }
    @Test fun `ineligible requests do not consume the destination`() {
        val engine = RoadAheadEngine()
        assertNull(engine.parkingRequest(dest, NavMode.FREE, true))
        assertNull(engine.parkingRequest(dest.copy(remainDistanceM = 1500.0, remainTimeSec = 300), NavMode.ROUTE, true))
        assertEquals(dest, engine.parkingRequest(dest, NavMode.ROUTE, true))
        assertNull(engine.tourRequest(dest, false))
        assertEquals(dest, engine.tourRequest(dest, true))
    }
    @Test fun `parking card and advice after lots arrive`() {
        val engine = RoadAheadEngine()
        engine.onParkingLots(dest.key, listOf(parkingLot()))
        val result = engine.tick(destinationInputs(), 0, destinationNow)
        assertEquals(Card.Parking(ParkingCard("시청 공영주차장", 23, "목적지에서 200m", "30분에 1,000원")), result.screen.card)
        assertEquals(Advice("parking:d1", Priority.NORMAL,
            "목적지 근처 시청 공영주차장에 빈자리 23면이 있어요. 30분에 1,000원이에요."), result.speak)
        assertNull(engine.tick(destinationInputs(), 250, destinationNow).speak)
    }
    @Test fun `new destination resets parking and travel`() {
        val engine = RoadAheadEngine()
        engine.parkingRequest(dest, NavMode.ROUTE, true)
        engine.tourRequest(dest, true)
        engine.onParkingLots(dest.key, listOf(parkingLot()))
        engine.onTourData(dest.key, tourData(tourItem()))
        assertTrue(engine.tick(destinationInputs(parked = true, travel = true), 0, destinationNow).screen.card is Card.Travel)
        val other = dest.copy(key = "d2")
        val changed = engine.tick(destinationInputs(other, parked = true, travel = true), 250, destinationNow)
        assertNull(changed.screen.card)
        assertNull(changed.speak)
        assertEquals(other, engine.parkingRequest(other, NavMode.ROUTE, true))
        assertEquals(other, engine.tourRequest(other, true))
        engine.onParkingLots(dest.key, listOf(parkingLot()))
        engine.onTourData(dest.key, tourData(tourItem()))
        assertNull(engine.tick(destinationInputs(other, parked = true, travel = true), 500, destinationNow).screen.card)
        engine.onTourData(other.key, tourData(tourItem(other)))
        assertTrue(engine.tick(destinationInputs(other, parked = true, travel = true), 750, destinationNow).screen.card is Card.Travel)
        assertEquals(dest, engine.parkingRequest(dest, NavMode.ROUTE, true))
        assertEquals(dest, engine.tourRequest(dest, true))
        assertNull(engine.tick(destinationInputs(parked = true, travel = true), 1000, destinationNow).screen.card)
    }
    @Test fun `late responses do not replace current destination data`() {
        val engine = RoadAheadEngine()
        val other = dest.copy(key = "d2")
        engine.parkingRequest(other, NavMode.ROUTE, true)
        engine.onParkingLots(other.key, listOf(parkingLot(other)))
        engine.onTourData(other.key, tourData(tourItem(other)))
        engine.onParkingLots(dest.key, emptyList())
        engine.onTourData(dest.key, tourData())
        val result = engine.tick(destinationInputs(other, parked = true, travel = true), 0, destinationNow)
        assertTrue(result.screen.card is Card.Travel)
        assertEquals("parking:d2", result.speak?.key)
        assertTrue(engine.tick(destinationInputs(other), 250, destinationNow).screen.card is Card.Parking)
    }
    @Test fun `missing destination accepts responses for the remembered key`() {
        val engine = RoadAheadEngine()
        engine.parkingRequest(dest, NavMode.ROUTE, true)
        engine.tick(destinationInputs(null), 0, destinationNow)
        engine.onParkingLots(dest.key, listOf(parkingLot()))
        engine.onTourData(dest.key, tourData(tourItem()))
        val missing = engine.tick(destinationInputs(null, parked = true, travel = true), 250, destinationNow)
        assertNull(missing.screen.card)
        assertNull(missing.speak)
        val restored = engine.tick(destinationInputs(parked = true, travel = true), 500, destinationNow)
        assertTrue(restored.screen.card is Card.Travel)
        assertEquals("parking:d1", restored.speak?.key)
        assertNull(engine.parkingRequest(dest, NavMode.ROUTE, true))
    }
    @Test fun `destination briefly missing keeps state`() {
        val engine = RoadAheadEngine()
        assertEquals(dest, engine.parkingRequest(dest, NavMode.ROUTE, true))
        assertEquals(dest, engine.tourRequest(dest, true))
        engine.onParkingLots(dest.key, listOf(parkingLot()))
        engine.onTourData(dest.key, tourData(tourItem()))
        val input = destinationInputs(parked = true, travel = true)
        assertEquals("parking:d1", engine.tick(input, 0, destinationNow).speak?.key)
        assertEquals("travel:d1", engine.tick(input, 250, destinationNow).speak?.key)
        assertNull(engine.parkingRequest(null, NavMode.ROUTE, true))
        assertNull(engine.tourRequest(null, true))
        for (mono in listOf(500L, 1000L, 1500L)) {
            val missing = engine.tick(input.copy(destination = null), mono, destinationNow)
            assertNull(missing.screen.card)
            assertNull(missing.speak)
        }
        val restored = engine.tick(input, 2000, destinationNow)
        assertTrue(restored.screen.card is Card.Travel)
        assertNull(restored.speak)
        assertNull(engine.parkingRequest(dest, NavMode.ROUTE, true))
        assertNull(engine.tourRequest(dest, true))
        val expired = engine.tick(input, 60_000, destinationNow)
        assertTrue(expired.screen.card is Card.Parking)
        assertNull(expired.speak)
    }
    @Test fun `parking pending behind navigation is dropped near arrival`() {
        val engine = RoadAheadEngine()
        engine.onParkingLots(dest.key, listOf(parkingLot()))
        assertNull(engine.tick(destinationInputs().copy(naviSpeaking = true), 0, destinationNow).speak)
        val near = engine.tick(destinationInputs(dest.copy(remainDistanceM = 80.0)), 500, destinationNow)
        assertNull(near.screen.card)
        assertNull(near.speak)
    }
    @Test fun `travel list shows only when parked and for sixty seconds`() {
        val engine = RoadAheadEngine()
        engine.onTourData(dest.key, tourData(tourItem()))
        val driving = engine.tick(destinationInputs(travel = true), 0, destinationNow)
        assertEquals("travel:d1", driving.speak?.key)
        assertNull(driving.screen.card)
        val parked = destinationInputs(parked = true, travel = true)
        assertEquals(Card.Travel(listOf(TravelLine("시립 미술관", TourKind.CULTURE, "800m", null))),
            engine.tick(parked, 1000, destinationNow).screen.card)
        assertTrue(engine.tick(parked, 60_000, destinationNow.plusSeconds(3600)).screen.card is Card.Travel)
        assertNull(engine.tick(parked, 61_000, destinationNow).screen.card)
        assertNull(engine.tick(parked, 62_000, destinationNow).screen.card)
    }
    @Test fun `travel list closes for good after driving off`() {
        val engine = RoadAheadEngine()
        engine.onTourData(dest.key, tourData(tourItem()))
        val parked = destinationInputs(parked = true, travel = true)
        assertTrue(engine.tick(parked, 0, destinationNow).screen.card is Card.Travel)
        assertNull(engine.tick(parked.copy(parked = false), 250, destinationNow).screen.card)
        assertNull(engine.tick(parked, 500, destinationNow).screen.card)
    }
    @Test fun `new destination resets the travel list timeout`() {
        val engine = RoadAheadEngine(EngineConfig(travelListMs = 1000))
        val parked = destinationInputs(parked = true, travel = true)
        engine.onTourData(dest.key, tourData(tourItem()))
        assertTrue(engine.tick(parked, 0, destinationNow).screen.card is Card.Travel)
        assertNull(engine.tick(parked, 1000, destinationNow).screen.card)
        val other = dest.copy(key = "d2")
        assertEquals(other, engine.tourRequest(other, true))
        engine.onTourData(other.key, tourData(tourItem(other)))
        assertTrue(engine.tick(parked.copy(destination = other), 2000, destinationNow).screen.card is Card.Travel)
    }
    @Test fun `travel mode off hides the list and advice`() {
        val engine = RoadAheadEngine()
        engine.onTourData(dest.key, tourData(tourItem()))
        val result = engine.tick(destinationInputs(parked = true, travel = false), 0, destinationNow)
        assertNull(result.screen.card)
        assertNull(result.speak)
    }
    @Test fun `travel uses the seoul arrival date across midnight`() {
        val engine = RoadAheadEngine()
        val tomorrow = LocalDate.of(2026, 10, 30)
        val festival = tourItem().copy(kind = TourKind.FESTIVAL, title = "강변 축제", start = tomorrow, end = tomorrow)
        engine.onTourData(dest.key, tourData(festival))
        val result = engine.tick(destinationInputs(travel = true), 0, Instant.parse("2026-10-29T14:59:00Z"))
        assertEquals("목적지 근처에서 내일까지 강변 축제가 열려요. 목적지에서 800m예요.", result.speak?.text)
    }
    @Test fun `high hazard outranks signal on the card`() {
        val engine = engine()
        (0L..2L).forEach { accept(engine, it) }
        engine.onSpeed(10.0, 2000)
        engine.onParkingLots(dest.key, listOf(parkingLot()))
        engine.onTourData(dest.key, tourData(tourItem()))
        val fog = Hazard("fog:1", HazardKind.FOG, Severity.DANGER, intersection.point, north(-300.0), "", false, destinationNow)
        engine.onHazards(HazardSource.WEATHER, listOf(fog))
        val all = inputs().copy(destination = dest, parked = true, travelMode = true)
        assertTrue(engine.tick(all, 2000, destinationNow).screen.card is Card.Travel)
        val driving = all.copy(parked = false)
        val high = engine.tick(driving, 2250, destinationNow)
        assertEquals(Card.Hazard(high.screen.hazardText!!), high.screen.card)
        assertNotNull(high.screen.signal)
        engine.onHazards(HazardSource.WEATHER, emptyList())
        val signal = engine.tick(driving, 2500, destinationNow)
        assertEquals(Card.Signal(signal.screen.signal!!), signal.screen.card)
        engine.onHazards(HazardSource.ITS_EVENT, listOf(fog.copy(key = "work", kind = HazardKind.CONSTRUCTION,
            severity = Severity.CAUTION, detail = "4차로 차단")))
        val normal = engine.tick(driving, 6000, destinationNow)
        assertEquals(Card.Hazard("200m 앞 공사 구간이에요. 4차로 차단."), normal.screen.card)
        engine.onHazards(HazardSource.ITS_EVENT, emptyList())
        assertTrue(engine.tick(driving, 6250, destinationNow).screen.card is Card.Parking)
        assertNull(engine.tick(driving.copy(navMode = NavMode.FREE), 6500, destinationNow).screen.card)
    }

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
