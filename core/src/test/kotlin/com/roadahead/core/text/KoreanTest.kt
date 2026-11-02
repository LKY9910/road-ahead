package com.roadahead.core.text

import com.roadahead.core.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

class KoreanTest {
    @Test fun `card fee is short`() {
        assertEquals("5분에 430원", koFeeShort(5, 430))
        assertEquals("30분에 4,200원", koFeeShort(30, 4200))
        assertEquals("무료", koFeeShort(null, 0))
        assertNull(koFeeShort(null, null))
    }
    @Test fun `topic particle`() {
        assertEquals("덕수궁 대한문은", topic("덕수궁 대한문"))
        assertEquals("익선동 한옥거리는", topic("익선동 한옥거리"))
        assertEquals("DDP는", topic("DDP"))
    }
    @Test fun `particles follow final consonant`() {
        assertEquals("시청 공영주차장이", subject("시청 공영주차장"))
        assertEquals("중앙 파크가", subject("중앙 파크"))
        assertEquals("전망대 공원이에요", copula("전망대 공원"))
        assertEquals("시립 미술관이에요", copula("시립 미술관"))
        assertEquals("1.2km예요", copula("1.2km"))
        assertEquals("ABC예요", copula("ABC"))
    }
    @Test fun `fee text`() {
        assertEquals("30분에 1,000원이에요", koFee(30, 1000))
        assertEquals("무료예요", koFee(null, 0))
        assertEquals("무료예요", koFee(30, 0))
        assertNull(koFee(30, null))
        assertNull(koFee(null, null))
        assertNull(koFee(null, 1000))
    }
    @Test fun `until text`() {
        val today = LocalDate.of(2026, 10, 29)
        assertEquals("오늘", koUntil(today, today))
        assertEquals("내일", koUntil(today.plusDays(1), today))
        assertEquals("11월 2일", koUntil(LocalDate.of(2026, 11, 2), today))
    }
    @Test fun `distance wording`() {
        assertEquals("800m", koDistance(780.0)); assertEquals("1km", koDistance(960.0))
        assertEquals("2.5km", koDistance(2_480.0)); assertEquals("3km", koDistance(3_000.0))
    }
}
