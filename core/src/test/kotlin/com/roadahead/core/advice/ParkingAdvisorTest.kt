package com.roadahead.core.advice

import com.roadahead.core.geo.distanceMeters
import com.roadahead.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class ParkingAdvisorTest {
    @Test fun `lot label adds parking word`() {
        assertEquals("동국제강 주차장", lotLabel("동국제강"))
        assertEquals("시청 공영주차장", lotLabel("시청 공영주차장"))
        val result = advisor.pick(dest, listOf(lot(name = "동국제강", available = 10, minutes = 5, fee = 700)), now)!!
        assertEquals("동국제강 주차장", result.first.name)
        assertEquals("목적지 근처 동국제강 주차장에 빈자리 10면이 있어요. 5분에 700원이에요.", result.second.text)
    }
    private val dest = Destination("d1", "시청", GeoPoint(35.5384, 129.3114), 900.0, 120)
    private val now = Instant.parse("2026-10-29T05:00:00Z")
    private val advisor = ParkingAdvisor()
    private fun lot(m: Double = 180.0, available: Int? = 23, produced: Instant? = now.minusSeconds(120),
        name: String = "시청 공영주차장", minutes: Int? = 30, fee: Int? = 1000) =
        ParkingLot(name, name, GeoPoint(dest.point.lat + m / 111_195.0, dest.point.lon), available, produced, minutes, fee)

    @Test fun `fresh free spaces are spoken`() {
        assertEquals(Pair(ParkingCard("시청 공영주차장", 23, "목적지에서 200m", "30분에 1,000원"),
            Advice("parking:d1", Priority.NORMAL, "목적지 근처 시청 공영주차장에 빈자리 23면이 있어요. 30분에 1,000원이에요.")),
            advisor.pick(dest, listOf(lot()), now))
    }
    @Test fun `stale availability is not spoken`() {
        val result = advisor.pick(dest, listOf(lot(produced = now.minusSeconds(660))), now)!!
        assertEquals("목적지 근처에 시청 공영주차장이 있어요. 30분에 1,000원이에요.", result.second.text)
        assertNull(result.first.available)
    }
    @Test fun `full lot is skipped for unknown one`() {
        val result = advisor.pick(dest, listOf(lot(150.0, available = 0),
            lot(400.0, available = null, produced = null, name = "중앙 파크", minutes = null, fee = null)), now)!!
        assertEquals(ParkingCard("중앙 파크 주차장", null, "목적지에서 400m", null), result.first)
        assertEquals("목적지 근처에 중앙 파크 주차장이 있어요.", result.second.text)
        assertNull(advisor.pick(dest, listOf(lot(available = 0)), now))
    }
    @Test fun `lots beyond radius give nothing`() {
        assertNull(advisor.pick(dest, listOf(lot(600.0)), now))
        assertNull(advisor.pick(dest, emptyList(), now))
    }
    @Test fun `free fee`() {
        assertEquals("목적지 근처 시청 공영주차장에 빈자리 5면이 있어요. 무료예요.",
            advisor.pick(dest, listOf(lot(available = 5, fee = 0)), now)!!.second.text)
    }
    @Test fun `due rules`() {
        assertTrue(advisor.due(dest, NavMode.ROUTE))
        assertFalse(advisor.due(dest, NavMode.FREE))
        assertFalse(advisor.due(dest, NavMode.UNKNOWN))
        assertFalse(advisor.due(null, NavMode.ROUTE))
        assertFalse(advisor.due(dest.copy(remainDistanceM = 1500.0, remainTimeSec = 300), NavMode.ROUTE))
        assertTrue(advisor.due(dest.copy(remainDistanceM = null, remainTimeSec = 150), NavMode.ROUTE))
        assertFalse(advisor.due(dest.copy(remainDistanceM = null, remainTimeSec = null), NavMode.ROUTE))
        assertFalse(advisor.due(dest.copy(remainDistanceM = 80.0), NavMode.ROUTE))
        assertFalse(advisor.due(dest.copy(remainDistanceM = 100.0), NavMode.ROUTE))
        assertTrue(advisor.due(dest.copy(remainDistanceM = 1000.0, remainTimeSec = null), NavMode.ROUTE))
        assertTrue(advisor.due(dest.copy(remainDistanceM = null, remainTimeSec = 180), NavMode.ROUTE))
    }
    @Test fun `fresh spaces take precedence then nearest lot wins`() {
        val closeStale = lot(100.0, produced = now.minusSeconds(660))
        val farFresh = lot(400.0, name = "먼 주차장")
        val nearFresh = lot(200.0, name = "가까운 주차장")
        assertEquals("가까운 주차장", advisor.pick(dest, listOf(closeStale, farFresh, nearFresh), now)!!.first.name)
        assertEquals("시청 공영주차장", advisor.pick(dest, listOf(closeStale, lot(300.0, produced = null)), now)!!.first.name)
    }
    @Test fun `availability without a timestamp is unknown`() {
        val result = advisor.pick(dest, listOf(lot(produced = null)), now)!!
        assertNull(result.first.available)
        assertEquals("목적지 근처에 시청 공영주차장이 있어요. 30분에 1,000원이에요.", result.second.text)
    }
    @Test fun `age and radius limits are inclusive`() {
        assertEquals(23, advisor.pick(dest, listOf(lot(produced = now.minusSeconds(600))), now)!!.first.available)
        assertNull(advisor.pick(dest, listOf(lot(produced = now.minusSeconds(600).minusMillis(1))), now)!!.first.available)
        val edge = lot()
        val distance = distanceMeters(dest.point, edge.point)
        val onEdge = ParkingAdvisor(ParkingConfig(radiusM = distance, maxAge = Duration.ofMinutes(10)))
        assertNotNull(onEdge.pick(dest, listOf(edge), now))
        assertNull(ParkingAdvisor(ParkingConfig(radiusM = distance - 0.01)).pick(dest, listOf(edge), now))
    }
}
