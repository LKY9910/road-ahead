package com.roadahead.feed

import com.roadahead.core.*
import com.roadahead.core.model.*
import com.roadahead.core.advice.TravelAdvisor
import com.roadahead.core.forecast.BadWeather
import com.roadahead.core.parking.parseParkingAsset
import com.roadahead.core.signal.parseIntersectionPage
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ReplayFeedTest {
    private fun rows(scene: String) = File("src/main/assets/demo/scene-$scene.jsonl").readLines()
        .map { Json.parseToJsonElement(it).jsonObject }
    private val now = Instant.parse("2030-01-01T00:00:00Z")

    private fun recording(name: String) = Json.parseToJsonElement(File("../tools/demo-src/travel/$name.json").readText()).jsonObject
    private fun recordedTour() = buildJsonObject {
        put("t", 0); put("kind", "tour")
        for ((field, name) in mapOf("places" to "places", "festivals" to "festivals", "petPlaces" to "pet-places",
            "forecast" to "forecast-20261029-1700")) put(field, recording(name).getValue("response").toString())
        put("forecastRecordedAt", "2026-10-29T19:36:00+09:00")
        for ((field, prefix) in mapOf("petDetails" to "pet-", "facilities" to "facilities-")) put(field, buildJsonObject {
            File("../tools/demo-src/travel").listFiles()!!.filter { it.name.startsWith(prefix) && it.name != "pet-places.json" }
                .forEach { put(it.nameWithoutExtension.removePrefix(prefix), recording(it.nameWithoutExtension).getValue("response").toString()) }
        })
    }
    private val travelDest = Destination("37.57201,126.97631", "세종문화회관", GeoPoint(37.57200988776222, 126.97630706471843), 1800.0, 420)
    private val festivalIntro = "목적지 근처에서 12월 31일까지 2026 서울거리공연 구석구석라이브가 열려요. 목적지에서 100m예요. 의정부지 상설 전통문화행사도 열려요. "
    private val noCompanionAdvice = festivalIntro + "도착할 때쯤 비 예보가 있어 실내 명소를 먼저 골랐어요. " +
        "가까운 명소는 국립현대미술관 서울, 서울특별시교육청 정독도서관, [K드라마 촬영지] 공근혜갤러리예요."
    private val petAdvice = festivalIntro + "반려동물과 갈 수 있는 가까운 명소는 인사동, 익선동 한옥거리, 명동이에요. " +
        "인사동은 일부 구역만 반려동물과 들어갈 수 있어요."

    @Test fun `travel scene works for every companion set`() = runBlocking {
        val arrival = now.atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime().plusMinutes(7)
        for (companions in listOf(emptySet(), setOf(Companion.CHILD), setOf(Companion.ELDER),
            setOf(Companion.PET), setOf(Companion.CHILD, Companion.PET))) {
            val data = collectTour(RecordedTourSource(recordedTour(), now), travelDest, arrival, companions)
            assertEquals(companions, data.companions)
            assertEquals(BadWeather.RAIN, data.weather)
            val result = TravelAdvisor().pick(travelDest, data, LocalDate.of(2026, 10, 29), LocalDate.of(2026, 10, 29))!!
            assertTrue(result.lines.isNotEmpty())
            if (companions.isEmpty()) assertEquals(noCompanionAdvice, result.advice.text)
            if (companions == setOf(Companion.PET)) assertEquals(petAdvice, result.advice.text)
            println("companions=$companions first=${result.advice.text}")
        }
    }

    @Test fun `missing recorded details fail instead of becoming unavailable`() = runBlocking {
        for ((field, companions) in listOf("petDetails" to setOf(Companion.PET), "facilities" to setOf(Companion.CHILD))) {
            val incomplete = JsonObject(recordedTour() + (field to buildJsonObject {}))
            assertThrows(AssertionError::class.java) { runBlocking {
                collectTour(RecordedTourSource(incomplete, now), travelDest,
                    now.atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime().plusMinutes(7), companions)
            } }
        }
    }

    @Test fun `travel scene parking uses seoul availability even on Saturday`() = runBlocking {
        for (replayNow in listOf(now, Instant.parse("2026-10-31T03:00:00Z"))) {
            val lots = parseParkingAsset(File("src/main/assets/parking/lots.tsv").readText())
            val feed = ReplayFeed(listOf(buildJsonObject { put("t", 0) }), { 0L }, { replayNow }, lots)
            val received = mutableListOf<DataEvent.ParkingLots>()
            val collector = launch(start = CoroutineStart.UNDISPATCHED) { feed.data.collect { received += it as DataEvent.ParkingLots } }
            val record = recording("seoul-parking")
            feed.emitEvent(buildJsonObject { put("kind", "seoul"); put("body", record.getValue("response").toString())
                put("recordedAt", record.getValue("requestedAtKst")) }, 0, replayNow)
            feed.requestParking(travelDest.copy(remainDistanceM = 1000.0, remainTimeSec = 240))
            yield()
            val sejong = received.single().lots.single { it.id == "03172-11291-00001-00-1" }
            assertEquals(849, sejong.available)
            assertEquals(replayNow.minusNanos(40_979_704_000), sejong.availableProduced)
            val engine = RoadAheadEngine()
            engine.onParkingLots(travelDest.key, received.single().lots)
            val result = engine.tick(Inputs(NavMode.ROUTE, null, null, false, false, false, false,
                parked = false, travelMode = true, destination = travelDest.copy(remainDistanceM = 1000.0, remainTimeSec = 240)), 0, replayNow)
            assertEquals("목적지 근처 세종로 주차장에 빈자리 849면이 있어요. 5분에 430원이에요.", result.speak?.text)
            collector.cancelAndJoin()
        }
    }

    @Test fun `replay sets destination and parked gear then clears them`() = runBlocking {
        val feed = ReplayFeed(listOf(buildJsonObject { put("t", 0) }), { 0L }, { now })
        assertNull(feed.destination.value)
        assertFalse(feed.parked.value)
        feed.emitEvent(Json.parseToJsonElement("""{"kind":"dest","name":"서울시청 주변","lat":37.567,"lon":126.978,"remainM":3500,"remainSec":720}""").jsonObject, 0, now)
        feed.emitEvent(Json.parseToJsonElement("""{"kind":"gear","parked":true}""").jsonObject, 0, now)
        assertEquals(Destination("37.56700,126.97800", "서울시청 주변", GeoPoint(37.567, 126.978), 3500.0, 720), feed.destination.value)
        assertTrue(feed.parked.value)
        feed.emitEvent(Json.parseToJsonElement("""{"kind":"dest","clear":true}""").jsonObject, 1, now)
        feed.emitEvent(Json.parseToJsonElement("""{"kind":"gear","parked":false}""").jsonObject, 1, now)
        assertNull(feed.destination.value)
        assertFalse(feed.parked.value)
    }

    @Test fun `recorded tour waits for its request in either event order`() = runBlocking {
        val places = """{"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":{"contentid":"p","title":"명소","contenttypeid":"12","mapy":"37.567","mapx":"126.978"}}}}}"""
        val festivals = """{"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":{"contentid":"f","title":"행사","mapy":"37.567","mapx":"126.978","eventstartdate":"20261001","eventenddate":"20261231"}}}}}"""
        val tour = buildJsonObject { put("t", 0); put("kind", "tour"); put("places", places); put("festivals", festivals) }
        val dest = Destination("d1", "시청", GeoPoint(37.567, 126.978), 3500.0, 720)
        val day = LocalDate.of(2026, 10, 29)
        for (recordingFirst in listOf(true, false)) {
            val feed = ReplayFeed(listOf(tour), { 0L }, { now })
            val received = mutableListOf<DataEvent>()
            val collector = launch(start = CoroutineStart.UNDISPATCHED) { feed.data.collect { received += it } }
            if (recordingFirst) feed.start(this, RoadAheadEngine()) else feed.requestTour(dest, day.atStartOfDay(), emptySet())
            yield()
            assertTrue(received.isEmpty())
            if (recordingFirst) feed.requestTour(dest, day.atStartOfDay(), emptySet()) else feed.start(this, RoadAheadEngine())
            withTimeout(2_000) { while (received.isEmpty()) yield() }
            assertEquals(listOf(DataEvent.TourData("d1", TourData(listOf(
                TourItem("p", TourKind.ATTRACTION, "명소", GeoPoint(37.567, 126.978), null, null)), listOf(
                TourItem("f", TourKind.FESTIVAL, "행사", GeoPoint(37.567, 126.978),
                    LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31))), emptySet()))), received)
            collector.cancelAndJoin()
            feed.stop()
        }
    }

    @Test fun `scene e requests tour in P and parking at one kilometer in D`() = runBlocking {
        val events = rows("e")
        var clock = 0L
        val base = Instant.parse("2026-10-29T10:36:00Z")
        var instant = base
        val feed = ReplayFeed(listOf(JsonObject(events.single { it["kind"]?.jsonPrimitive?.content == "tour" } + ("t" to JsonPrimitive(0)))),
            { clock }, { instant }, parseParkingAsset(File("src/main/assets/parking/lots.tsv").readText()))
        val engine = RoadAheadEngine()
        val speech = mutableListOf<Pair<Long, String>>()
        val cards = mutableListOf<Pair<Long, Card>>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            feed.data.collect { event ->
                when (event) {
                    is DataEvent.TourData -> engine.onTourData(event.destKey, event.data)
                    is DataEvent.ParkingLots -> engine.onParkingLots(event.destKey, event.lots)
                    else -> error("Unexpected scene E data")
                }
            }
        }
        val speedJob = launch(start = CoroutineStart.UNDISPATCHED) { feed.speed.collect { (mps, t) -> engine.onSpeed(mps, t) } }
        feed.start(this, engine)
        var index = 0
        for (t in 0L..66_500L step 250) {
            clock = t
            instant = base.plusMillis(t)
            while (index < events.size && events[index].getValue("t").jsonPrimitive.long <= t) {
                feed.emitEvent(events[index++], t, instant)
            }
            engine.parkingRequest(feed.destination.value, feed.navMode.value, true)?.let(feed::requestParking)
            engine.tourRequest(feed.destination.value, true)?.let {
                feed.requestTour(it, instant.plusSeconds(it.remainTimeSec!!.toLong()).atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime(), emptySet())
            }
            yield()
            val result = engine.tick(Inputs(feed.navMode.value, null, null, false, false, false, false,
                parked = feed.parked.value, travelMode = true, destination = feed.destination.value), t, instant)
            result.speak?.let { speech += t to it.text }
            result.screen.card?.let { cards += t to it }
        }
        assertEquals(2, speech.size, speech.toString())
        assertEquals(noCompanionAdvice, speech.first().second)
        assertTrue(speech.first().first < 32_000, speech.toString())
        assertEquals(48_000L, speech.last().first)
        assertEquals("목적지 근처 세종로 주차장에 빈자리 849면이 있어요. 5분에 430원이에요.", speech.last().second)
        assertTrue(cards.any { it.second is Card.Travel })
        assertTrue(cards.none { it.first >= 32_000 && it.second is Card.Travel })
        assertTrue(cards.any { it.first >= 48_000 && it.second is Card.Parking })
        assertFalse(feed.parked.value)
        assertEquals(150.0, feed.destination.value?.remainDistanceM)
        println("scene=e speech=$speech")
        collector.cancelAndJoin()
        speedJob.cancelAndJoin()
        feed.stop()
    }

    @Test fun `recorded scenes announce in order with fresh weather timestamps`() = runBlocking {
        val expected = mapOf(
            "a" to listOf("앞 신호 빨간불이에요. 속도를 줄이세요.", "좌회전 신호까지 ", "곧 좌회전 신호로 바뀌어요."),
            "b" to listOf("앞 신호 빨간불이에요."),
            "c" to listOf("앞 안개 구간이에요.", "앞 공사 구간이에요.", "앞 살얼음 주의 구간이에요.")
        )
        for ((scene, texts) in expected) {
            val events = rows(scene)
            val feed = ReplayFeed(events, { 0L }, { now })
            val engine = RoadAheadEngine()
            val speech = mutableListOf<Pair<Long, String>>()
            val dataJob = launch(start = CoroutineStart.UNDISPATCHED) {
                feed.data.collect { event ->
                    when (event) {
                        is DataEvent.Intersections -> engine.onIntersections(parseIntersectionPage(event.json))
                        is DataEvent.Signals -> engine.onSignalPage(event.json, event.receivedMonoMs)
                        is DataEvent.Weather -> {
                            assertTrue(event.hazards.all { it.produced == now })
                            engine.onHazards(HazardSource.WEATHER, event.hazards)
                        }
                        is DataEvent.Events -> {
                            assertTrue(event.hazards.all { it.produced == now })
                            engine.onHazards(HazardSource.ITS_EVENT, event.hazards)
                        }
                        is DataEvent.Disasters -> error("Unexpected disaster")
                        is DataEvent.ParkingLots, is DataEvent.TourData -> error("Unexpected destination data")
                    }
                }
            }
            val speedJob = launch(start = CoroutineStart.UNDISPATCHED) {
                feed.speed.collect { (mps, t) -> engine.onSpeed(mps, t) }
            }
            var index = 0
            for (t in 0L..events.last().getValue("t").jsonPrimitive.long + 500 step 50) {
                while (index < events.size && events[index].getValue("t").jsonPrimitive.long <= t) {
                    feed.emitEvent(events[index++], t, now)
                }
                if (t % 200 == 0L) feed.emitSpeed(t)
                yield()
                val result = engine.tick(Inputs(feed.navMode.value, feed.turn.value, feed.fix.value,
                    feed.wiperOn.value, feed.defrosterOn.value, feed.naviSpeaking.value, false), t, now)
                result.speak?.let { speech += t to it.text }
            }
            assertEquals(texts.size, speech.size, "$scene $speech")
            texts.zip(speech).forEach { (text, spoken) -> assertTrue(spoken.second.contains(text), "$scene $speech") }
            assertTrue(speech.first().first <= 10_000, "$scene $speech")
            println("scene=$scene speech=$speech durationMs=${events.last().getValue("t")}")
            if (scene == "c") assertEquals("합성 시나리오: 살얼음 자료는 11월 15일부터 제공돼 예시로 만들었어요", feed.label.value)
            dataJob.cancelAndJoin(); speedJob.cancelAndJoin()
        }
    }

    @Test fun `replay repeats speed after last event until stopped`() = runBlocking {
        val events = listOf("""{"t":0,"kind":"speed","mps":10.0}""",
            """{"t":50,"kind":"label","text":"done"}""").map { Json.parseToJsonElement(it).jsonObject }
        val start = System.nanoTime()
        val feed = ReplayFeed(events, { (System.nanoTime() - start) / 1_000_000 }, { now })
        val speeds = mutableListOf<Pair<Double, Long>>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) { feed.speed.collect { speeds += it } }
        feed.start(this, RoadAheadEngine())
        withTimeout(2_000) { while (speeds.size < 3) delay(20) }
        assertEquals("done", feed.label.value)
        assertTrue(speeds.all { it.first == 10.0 })
        assertTrue(speeds.last().second - speeds.first().second >= 350)
        feed.stop()
        val count = speeds.size
        delay(250)
        assertEquals(count, speeds.size)
        assertEquals("done", feed.label.value)
        collector.cancelAndJoin()
    }

    @Test fun `replay applies boolean inputs and nullable turn`() = runBlocking {
        val feed = ReplayFeed(listOf(buildJsonObject { put("t", 0) }), { 0L }, { now })
        for (kind in listOf("wiper", "defrost", "navvoice")) {
            feed.emitEvent(buildJsonObject { put("kind", kind); put("on", true) }, 42, now)
        }
        feed.emitEvent(Json.parseToJsonElement("""{"kind":"turn","movement":null,"distanceM":12}""").jsonObject, 42, now)
        feed.emitEvent(Json.parseToJsonElement("""{"kind":"fix","lat":35,"lon":129}""").jsonObject, 42, now)
        assertTrue(feed.wiperOn.value && feed.defrosterOn.value && feed.naviSpeaking.value)
        assertEquals(Turn(null, 12.0), feed.turn.value)
        assertEquals(Fix(GeoPoint(35.0, 129.0), null, 42), feed.fix.value)
    }
}
