package com.roadahead.car

import com.roadahead.core.model.GeoPoint
import com.roadahead.core.model.Fix
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GpsFixTest {
    private val start = GeoPoint(35.5469, 129.259894)

    @Test fun `moving coordinates override a stuck provider bearing`() {
        val south = GeoPoint(start.lat - 10.0 / 111_195, start.lon)
        assertEquals(180.0, bearingForFix(Fix(start, 0.0, 0), south, 0.0)!!, .01)
        assertEquals(180.0, bearingForFix(Fix(start, null, 0), south, null)!!, .01)
    }
    @Test fun `small movement preserves provider bearing or unknown`() {
        val near = GeoPoint(start.lat - 2.0 / 111_195, start.lon)
        assertEquals(90.0, bearingForFix(Fix(start, null, 0), near, 90.0))
        assertEquals(0.0, bearingForFix(null, start, 0.0))
        assertNull(bearingForFix(Fix(start, null, 0), near, null))
    }
    @Test fun `duplicate and stopped fixes keep the last bearing`() {
        val previous = Fix(start, 180.0, 0)
        val near = GeoPoint(start.lat - 2.0 / 111_195, start.lon)
        assertEquals(180.0, bearingForFix(previous, start, 0.0))
        assertEquals(180.0, bearingForFix(previous, near, 0.0))
        assertEquals(180.0, bearingForFix(previous, start, null))
    }
}
