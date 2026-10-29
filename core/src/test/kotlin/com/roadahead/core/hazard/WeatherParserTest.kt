package com.roadahead.core.hazard

import com.roadahead.core.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.time.*
import java.io.ByteArrayOutputStream
import java.util.zip.*

class WeatherParserTest {
    private val at = Instant.parse("2026-10-28T07:05:00Z")
    private fun fixture(n: String) = javaClass.getResource("/fixtures/$n")!!.readText()

    @Test fun `fog link rows become hazards`() {
        val fog = parseWeatherCsv(fixture("weather-045-link-fog.csv"), HazardKind.FOG, at)
        assertEquals(4, fog.count { it.severity == Severity.CAUTION })
        val link = fog.first { it.key == "fog:3580008302" }
        assertEquals(GeoPoint(36.362518, 128.238118), link.from); assertEquals(GeoPoint(36.312729, 128.220901), link.to)
        assertEquals(at, link.produced)
    }
    @Test fun `level zero is not a hazard`() {
        val csv = "LINK_ID,F_NODE,T_NODE,F_LON,F_LAT,T_LON,T_LAT,FOG\n1,a,b,128.0,36.0,128.1,36.1,0\n2,a,b,128.0,36.0,128.1,36.1,3\n"
        assertEquals(listOf("fog:2" to Severity.DANGER), parseWeatherCsv(csv, HazardKind.FOG, at).map { it.key to it.severity })
    }
    @Test fun `zip reads link files only`() {
        val bytes = ByteArrayOutputStream().also { out -> ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry("RWS/PRODUCT/FOG/R045_LINK_FOG_202610280705.csv")); z.write("LINK_ID,F_NODE,T_NODE,F_LON,F_LAT,T_LON,T_LAT,FOG\n7,a,b,128.0,36.0,128.1,36.1,2\n".toByteArray())
            z.putNextEntry(ZipEntry("RWS/PRODUCT/FOG/R045_1KM_FOG_202610280705.csv")); z.write("LINK_ID,S_NUM,F_LON,F_LAT,T_LON,T_LAT,FOG,FLAG,X_GRID,Y_GRID\n7,1,128.0,36.0,128.1,36.1,3,1,1,1\n".toByteArray())
            z.putNextEntry(ZipEntry("RWS/PRODUCT/FRG/R045_LINK_FRG_202611150705.csv")); z.write("LINK_ID,F_NODE,T_NODE,F_LON,F_LAT,T_LON,T_LAT,B_ICE\n8,a,b,128.0,36.0,128.1,36.1,1\n".toByteArray())
        } }.toByteArray()
        assertEquals(setOf("fog:7", "ice:8"), parseWeatherZip(bytes, at).map { it.key }.toSet())
    }
    @Test fun `request times walk back from the latest five minute slot`() {
        assertEquals(listOf("202610280710", "202610280705", "202610280700"), weatherRequestTimes(Instant.parse("2026-10-28T07:13:20Z")))
    }
    @Test fun `weather row with unknown level is skipped`() {
        for ((kind, column, prefix) in listOf(Triple(HazardKind.FOG, "FOG", "fog"), Triple(HazardKind.ICE, "B_ICE", "ice"))) {
            val csv = """LINK_ID,F_LON,F_LAT,T_LON,T_LAT,$column
                1,128.0,36.0,128.1,36.1,2
                9,128.0,36.0,128.1,36.1,9
                8,128.0,broken,128.1,36.1,3
                7,128.0
                2,128.2,36.2,128.3,36.3,3
            """.trimIndent()
            assertEquals(listOf(
                Hazard("$prefix:1", kind, Severity.CAUTION, GeoPoint(36.0, 128.0), GeoPoint(36.1, 128.1), "", false, at),
                Hazard("$prefix:2", kind, Severity.DANGER, GeoPoint(36.2, 128.2), GeoPoint(36.3, 128.3), "", false, at)
            ), parseWeatherCsv(csv, kind, at))
        }
        assertThrows<ApiException> { parseWeatherCsv("<html></html>", HazardKind.FOG, at) }
    }
    @Test fun `all broken weather rows throw`() {
        for ((kind, column) in listOf(HazardKind.FOG to "FOG", HazardKind.ICE to "B_ICE")) {
            val header = "LINK_ID,F_LON,F_LAT,T_LON,T_LAT,$column\n"
            for (rows in listOf(
                "1,128.0,36.0,128.1,36.1,9\n",
                "1,128.0,broken,128.1,36.1,3\n",
                "1,128.0\n",
                "1,128.0,36.0,128.1,36.1,9\n2,128.0,broken,128.1,36.1,3\n"
            )) {
                assertEquals("parse", assertThrows<ApiException> { parseWeatherCsv(header + rows, kind, at) }.code)
            }
            val skipped = "1,128.0,36.0,128.1,36.1,0\n2,128.0,36.0,128.1,36.1,\n"
            assertEquals(emptyList<Hazard>(), parseWeatherCsv(header, kind, at))
            assertEquals(emptyList<Hazard>(), parseWeatherCsv(header + skipped, kind, at))
            assertEquals(emptyList<Hazard>(), parseWeatherCsv(header + skipped + "3,128.0\n", kind, at))
        }
    }
}
