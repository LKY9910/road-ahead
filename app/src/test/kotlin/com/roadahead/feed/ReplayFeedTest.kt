package com.roadahead.feed

import com.roadahead.core.*
import com.roadahead.core.model.*
import com.roadahead.core.signal.parseIntersectionPage
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Instant

class ReplayFeedTest {
    private fun rows(scene: String) = File("src/main/assets/demo/scene-$scene.jsonl").readLines()
        .map { Json.parseToJsonElement(it).jsonObject }
    private val now = Instant.parse("2030-01-01T00:00:00Z")

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
