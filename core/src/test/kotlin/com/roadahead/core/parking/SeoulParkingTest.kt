package com.roadahead.core.parking

import com.roadahead.core.advice.ParkingAdvisor
import com.roadahead.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.*

class SeoulParkingTest {
    private fun fixture(name: String) = javaClass.getResource("/fixtures/$name")!!.readText()
    private val now = OffsetDateTime.parse("2026-10-29T18:57:23+09:00").toInstant()
    private val point = GeoPoint(37.5663, 126.9779)
    private fun lots() = listOf(
        ParkingLot("04540-11291-00001-00-1", "동국제강", point, null, null, 30, 4200),
        ParkingLot("03172-11291-00001-00-1", "세종로", point, null, null, 30, 2580))
    private fun rows() = parseSeoulParking(fixture("seoul-parking.json"))
    private fun dongkuk() = rows().single { it.code == "1425816" }

    @Test fun `parse seoul sample`() {
        assertEquals(3, rows().size)
        val lot = dongkuk()
        assertEquals(13, lot.totalSpaces)
        assertEquals(3, lot.occupied)
        assertTrue(lot.linked)
        assertEquals(540..1140, lot.weekdayHours)
        assertEquals(5, lot.basicMinutes)
        assertEquals(700, lot.basicFeeWon)
        val unlinked = rows().single { it.code == "1236612" }
        assertFalse(unlinked.linked)
        assertNull(unlinked.updated)
    }

    @Test fun `seoul error bodies`() {
        assertThrows(ApiException::class.java) { parseSeoulParking(fixture("seoul-error-invalid-key.xml")) }
        assertEquals(emptyList<SeoulLot>(), parseSeoulParking(fixture("seoul-no-data.json")))
        assertThrows(ApiException::class.java) { parseSeoulParking("""{"GetParkingInfo":{"RESULT":{"CODE":"ERROR-336"}}}""") }
    }

    @Test fun `seoul availability requires fresh linked counts`() {
        val sample = dongkuk().copy(weekdayHours = null, weekendHours = null)
        val fresh = applySeoul(lots(), listOf(sample), now).first()
        assertEquals(10, fresh.available)
        assertEquals(Instant.parse("2026-10-29T09:56:46Z"), fresh.availableProduced)
        assertNull(applySeoul(lots(), listOf(sample), now.plusSeconds(601)).first().available)
        for (invalid in listOf(sample.copy(linked = false), sample.copy(occupied = 14),
            sample.copy(updated = LocalDateTime.ofInstant(now.plusSeconds(5), ZoneId.of("Asia/Seoul"))),
            sample.copy(totalSpaces = 0), sample.copy(occupied = -1), sample.copy(occupied = null),
            sample.copy(updated = null))) {
            val result = applySeoul(lots(), listOf(invalid), now).first()
            assertNull(result.available)
            assertNull(result.availableProduced)
        }
        val updated = sample.updated!!.atZone(ZoneId.of("Asia/Seoul")).toInstant()
        assertEquals(10, applySeoul(lots(), listOf(sample), updated.plusSeconds(600)).first().available)
        assertNull(applySeoul(lots(), listOf(sample), updated.plusSeconds(600).plusNanos(1)).first().available)
    }

    @Test fun `closed seoul lot is not picked`() {
        val late = OffsetDateTime.parse("2026-10-29T19:36:00+09:00").toInstant()
        assertFalse(applySeoul(lots(), rows(), late).any { it.name == "동국제강" })
        val beforeClose = OffsetDateTime.parse("2026-10-29T18:59:00+09:00").toInstant()
        assertTrue(applySeoul(lots(), rows(), beforeClose).any { it.name == "동국제강" })
        assertFalse(applySeoul(lots(), rows(), OffsetDateTime.parse("2026-10-29T19:00:00+09:00").toInstant()).any { it.name == "동국제강" })
    }

    @Test fun `all day lot is open at midnight`() {
        val midnight = OffsetDateTime.parse("2026-10-30T00:30:00+09:00").toInstant()
        assertTrue(applySeoul(lots(), rows(), midnight).any { it.name == "세종로" })
    }

    @Test fun `saturday free fee`() {
        val saturday = OffsetDateTime.parse("2026-10-31T12:00:00+09:00").toInstant()
        val result = applySeoul(lots(), listOf(dongkuk().copy(saturdayFree = true)), saturday).first()
        assertEquals(0, result.basicFeeWon)
        val dest = Destination("d1", "시청", point, 900.0, 120)
        assertTrue(ParkingAdvisor().pick(dest, listOf(result), saturday)!!.second.text.contains("무료예요"))
    }

    @Test fun `unmapped and bus rows are ignored`() {
        assertEquals(lots(), applySeoul(lots(), listOf(dongkuk().copy(code = "unknown")), now))
        assertEquals(listOf(lots()[1]), applySeoul(lots(), listOf(dongkuk().copy(busOnly = true)), now))
        assertEquals(lots(), applySeoul(lots(), emptyList(), now))
    }

    @Test fun `card fee is short`() {
        val sejong = applySeoul(lots(), rows(), now).single { it.name == "세종로" }
        val card = ParkingAdvisor().pick(Destination("d1", null, point, 900.0, 120), listOf(sejong), now)!!.first
        assertEquals("5분에 430원", card.feeText)
    }

    @Test fun `overnight hours and missing hours`() {
        val overnight = dongkuk().copy(weekdayHours = 1320..120)
        fun at(time: String) = OffsetDateTime.parse("2026-10-29T$time+09:00").toInstant()
        assertTrue(applySeoul(lots().take(1), listOf(overnight), at("23:00:00")).isNotEmpty())
        assertTrue(applySeoul(lots().take(1), listOf(overnight), at("01:00:00")).isNotEmpty())
        assertTrue(applySeoul(lots().take(1), listOf(overnight), at("02:00:00")).isEmpty())
        assertTrue(applySeoul(lots().take(1), listOf(overnight.copy(weekdayHours = null)), at("12:00:00")).isNotEmpty())
    }

    @Test fun `seoul numbers must be finite integers and fee stays paired`() {
        for (value in listOf("13.5", "1e30", "-2147483649", "\"NaN\"")) {
            val parsed = parseSeoulParking(fixture("seoul-parking.json").replace("\"TPKCT\": 13.0", "\"TPKCT\": $value"))
            assertNull(parsed.single { it.code == "1425816" }.totalSpaces)
        }
        val result = applySeoul(lots(), listOf(dongkuk().copy(basicMinutes = null, basicFeeWon = 700)), now).first()
        assertEquals(30, result.basicMinutes)
        assertEquals(4200, result.basicFeeWon)
    }
}
