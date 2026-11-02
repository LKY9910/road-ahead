package com.roadahead.car

import com.roadahead.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Locale

class PleosNaviTest {
    @Test fun `destination key and remaining values are stable across locales`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals(Destination("37.56722,126.98130", "서울시청", GeoPoint(37.5672159, 126.981303), 3500.0, 720),
                navigationDestination(37.5672159, 126.981303, "서울시청", 3500.0, 720))
        } finally { Locale.setDefault(original) }
    }
    @Test fun `empty or invalid destination is absent`() {
        assertNull(navigationDestination(0.0, 127.0, "서울시청", 100.0, 30))
        assertNull(navigationDestination(37.0, 0.0, "서울시청", 100.0, 30))
        assertNull(navigationDestination(37.0, 127.0, " ", 100.0, 30))
        assertNull(navigationDestination(Double.NaN, 127.0, "서울시청", 100.0, 30))
    }
}
