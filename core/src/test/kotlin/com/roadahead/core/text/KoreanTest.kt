package com.roadahead.core.text

import com.roadahead.core.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class KoreanTest {
    @Test fun `distance wording`() {
        assertEquals("800m", koDistance(780.0)); assertEquals("1km", koDistance(960.0))
        assertEquals("2.5km", koDistance(2_480.0)); assertEquals("3km", koDistance(3_000.0))
    }
}
