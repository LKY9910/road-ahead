package com.roadahead.feed

import com.roadahead.core.forecast.BadWeather
import com.roadahead.core.model.*
import com.roadahead.core.model.Companion.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TourSourceTest {
    private val dest = Destination("d1", "세종문화회관", GeoPoint(37.572, 126.976), 2000.0, 600)
    private val arrival = LocalDateTime.parse("2026-10-29T20:00")
    private fun place(id: String, meters: Int, kind: TourKind = TourKind.ATTRACTION) =
        TourItem(id, kind, id, GeoPoint(dest.point.lat + meters / 111_195.0, dest.point.lon), null, null)

    private open inner class Source : TourSource {
        val calls = mutableMapOf<String, Int>()
        val details = mutableListOf<String>()
        val places = (1..12).map { place("p$it", it * 100) }
        val petPlaces = places.take(7)
        fun count(name: String) { calls[name] = (calls[name] ?: 0) + 1 }
        override suspend fun places(point: GeoPoint): List<TourItem> { count("places"); return places }
        override suspend fun festivals(date: LocalDate): List<TourItem> { count("festivals"); assertEquals(arrival.toLocalDate(), date); return emptyList() }
        override suspend fun weather(point: GeoPoint, arrival: LocalDateTime): BadWeather? { count("weather"); return BadWeather.RAIN }
        override suspend fun petPlaces(point: GeoPoint): List<TourItem> { count("petPlaces"); return petPlaces }
        override suspend fun petInfo(id: String): PetInfo? {
            count("petInfo")
            return when (id) {
                "p3" -> null
                "p4" -> error("detail unavailable")
                else -> PetInfo(PetScope.PARTIAL, "목줄 착용")
            }
        }
        override suspend fun facilities(id: String): Set<Facility> {
            count("facilities"); details += id
            return setOf(Facility.STROLLER)
        }
    }

    @Test fun `collect tour call counts`() = runBlocking {
        val solo = Source()
        val plain = collectTour(solo, dest, arrival, emptySet())
        assertEquals(mapOf("places" to 1, "festivals" to 1, "weather" to 1), solo.calls)
        assertEquals(BadWeather.RAIN, plain.weather)
        assertTrue(plain.petInfo.isEmpty())
        val child = Source()
        val family = collectTour(child, dest, arrival, setOf(CHILD))
        assertEquals(10, child.calls["facilities"])
        assertNull(child.calls["petPlaces"])
        assertEquals(10, family.facilities.size)
        val pet = Source()
        val withPet = collectTour(pet, dest, arrival, setOf(PET, ELDER))
        assertEquals(1, pet.calls["petPlaces"])
        assertEquals(5, pet.calls["petInfo"])
        assertEquals(3, pet.calls["facilities"])
        assertEquals(listOf("p1", "p2", "p5"), pet.details)
        assertEquals(setOf("p1", "p2", "p5"), withPet.petInfo.keys)
        assertEquals(setOf(PET, ELDER), withPet.companions)
    }

    @Test fun `collect tour survives failures`() = runBlocking {
        val source = object : Source() {
            override suspend fun petPlaces(point: GeoPoint): List<TourItem> = error("list unavailable")
            override suspend fun weather(point: GeoPoint, arrival: LocalDateTime): BadWeather? = error("forecast unavailable")
        }
        val failedPet = collectTour(source, dest, arrival, setOf(PET, CHILD))
        assertNull(failedPet.petPlaces)
        assertNull(failedPet.weather)
        assertTrue(failedPet.petInfo.isEmpty())
        assertTrue(failedPet.facilities.isEmpty())
        assertEquals(12, failedPet.places.size)
        val partial = object : Source() {
            override suspend fun facilities(id: String): Set<Facility> =
                if (id == "p2") error("facility unavailable") else setOf(Facility.STROLLER)
        }
        val family = collectTour(partial, dest, arrival, setOf(CHILD))
        assertEquals(9, family.facilities.size)
        assertFalse("p2" in family.facilities)
        assertTrue("p1" in family.facilities && "p10" in family.facilities)
        val lists = object : Source() {
            override suspend fun places(point: GeoPoint): List<TourItem> = error("places unavailable")
            override suspend fun festivals(date: LocalDate): List<TourItem> = error("festivals unavailable")
        }
        val empty = collectTour(lists, dest, arrival, emptySet())
        assertTrue(empty.places.isEmpty() && empty.festivals.isEmpty())
        assertEquals(BadWeather.RAIN, empty.weather)
    }

    @Test fun `collect tour starts independent requests together`() = runBlocking {
        val forecastStarted = CompletableDeferred<Unit>()
        val source = object : Source() {
            override suspend fun places(point: GeoPoint): List<TourItem> {
                forecastStarted.await()
                return super.places(point)
            }
            override suspend fun weather(point: GeoPoint, arrival: LocalDateTime): BadWeather? {
                forecastStarted.complete(Unit)
                return super.weather(point, arrival)
            }
        }
        assertEquals(12, withTimeout(1000) { collectTour(source, dest, arrival, emptySet()) }.places.size)
    }

    @Test fun `collect tour preserves cancellation`() {
        val source = object : Source() {
            override suspend fun places(point: GeoPoint): List<TourItem> = throw CancellationException("stopped")
        }
        assertThrows(CancellationException::class.java) { runBlocking { collectTour(source, dest, arrival, emptySet()) } }
    }
}
