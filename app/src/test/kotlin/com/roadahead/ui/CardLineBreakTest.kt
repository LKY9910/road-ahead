package com.roadahead.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardLineBreakTest {
    @Test fun `sentence lines preserve wording and existing line breaks`() {
        val original = "앞이 500m도 안 보여요. 속도를 줄이세요."
        val separated = sentenceLines(original)
        assertEquals("앞이 500m도 안 보여요.\n속도를 줄이세요.", separated)
        assertEquals(original, separated.replace('\n', ' '))
        assertEquals("2.6km 앞 살얼음 주의 구간이에요.", sentenceLines("2.6km 앞 살얼음 주의 구간이에요."))
        val parking = "목적지에서 1.4km\n30분에 1만 2,000원"
        assertEquals(parking, sentenceLines(parking))
        assertEquals("첫 문장.\n둘째 문장. 셋째 문장.", sentenceLines("첫 문장.\n둘째 문장. 셋째 문장."))
    }
}
