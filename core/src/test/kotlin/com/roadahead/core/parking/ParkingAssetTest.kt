package com.roadahead.core.parking

import com.roadahead.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ParkingAssetTest {
    @Test fun `parking asset rows`() {
        val lots = parseParkingAsset("#id\tname\tlat\tlon\tmin\twon\n04540-11291-00001-00-1\t동국제강\t37.5672159\t126.981303\t30\t4200\nX\t무료 주차장\t37.5\t127.0\t\t0\nbroken line\n")
        assertEquals(listOf(ParkingLot("04540-11291-00001-00-1", "동국제강", GeoPoint(37.5672159, 126.981303), null, null, 30, 4200),
            ParkingLot("X", "무료 주차장", GeoPoint(37.5, 127.0), null, null, null, 0)), lots)
    }
    @Test fun `empty optional fees stay unknown`() {
        assertEquals(listOf(ParkingLot("X", "주차장", GeoPoint(37.5, 127.0), null, null, null, null)),
            parseParkingAsset("X\t주차장\t37.5\t127.0\t\t\n"))
    }
    @Test fun `broken asset rows are skipped`() {
        assertTrue(parseParkingAsset("bad\nX\t주차장\tNaN\t127\t30\t1000\nX\t주차장\t0\t127\t30\t1000\nX\t주차장\t37\t127\t30\toops\n").isEmpty())
    }
}
