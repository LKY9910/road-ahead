package com.roadahead.core.geo

import com.roadahead.core.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GeoTest {
    private val origin = GeoPoint(35.5, 129.3)
    private fun north(m: Double) = GeoPoint(origin.lat + m / 111_195.0, origin.lon)

    @Test fun `compass sectors`() {
        assertEquals(Compass8.N, compassOf(0.0)); assertEquals(Compass8.N, compassOf(337.6))
        assertEquals(Compass8.NE, compassOf(44.0)); assertEquals(Compass8.S, compassOf(180.0))
        assertEquals(Compass8.W, compassOf(270.0))
    }
    @Test fun `approach convention names the side the car comes from`() {
        assertEquals(Compass8.N, signalDirection(180.0, DirectionConvention.APPROACH))
        assertEquals(Compass8.W, signalDirection(90.0, DirectionConvention.APPROACH))
        assertEquals(Compass8.S, signalDirection(180.0, DirectionConvention.HEADING))
    }
    @Test fun `target in front is ahead`() {
        assertEquals(100.0, aheadDistance(Fix(origin, 0.0, 0), north(100.0), 30.0)!!, 1.0)
    }
    @Test fun `target to the side or without bearing is not ahead`() {
        assertNull(aheadDistance(Fix(origin, 90.0, 0), north(100.0), 30.0))
        assertNull(aheadDistance(Fix(origin, null, 0), north(100.0), 30.0))
    }
    @Test fun `segment must run the same way as the car`() {
        val from = north(100.0); val to = north(300.0)
        assertEquals(100.0, segmentAheadDistance(Fix(origin, 0.0, 0), from, to, 30.0, 45.0)!!, 1.0)
        assertNull(segmentAheadDistance(Fix(origin, 0.0, 0), to, from, 30.0, 45.0))
    }
}