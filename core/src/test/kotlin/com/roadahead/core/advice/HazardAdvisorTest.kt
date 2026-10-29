package com.roadahead.core.advice

import com.roadahead.core.geo.*
import com.roadahead.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class HazardAdvisorTest {
    private val at = Instant.parse("2026-10-28T07:05:00Z")
    private val from = GeoPoint(36.362518, 128.238118)
    private val to = GeoPoint(36.312729, 128.220901)
    private val fog = Hazard("fog:3580008302", HazardKind.FOG, Severity.CAUTION, from, to, "", false, at)
    private val advisor = HazardAdvisor()
    private fun behind(meters: Double, opposite: Boolean = false): Fix {
        val ratio = meters / distanceMeters(from, to)
        val point = GeoPoint(from.lat - (to.lat - from.lat) * ratio, from.lon - (to.lon - from.lon) * ratio)
        return Fix(point, (bearingDegrees(from, to) + if (opposite) 180.0 else 0.0) % 360, 0)
    }
    @Test fun `caution fog two km ahead`() {
        assertEquals(listOf(Advice("fog:3580008302:CAUTION", Priority.NORMAL, "2km 앞 안개 구간이에요. 앞이 500m도 안 보여요. 속도를 줄이세요.")),
            advisor.advices(listOf(fog), behind(2000.0), false, at))
    }
    @Test fun `interest fog needs bad weather`() {
        val interest = fog.copy(severity = Severity.INTEREST)
        assertEquals(emptyList<Advice>(), advisor.advices(listOf(interest), behind(2000.0), false, at))
        assertEquals(listOf(Advice("fog:3580008302:INTEREST", Priority.NORMAL, "2km 앞 옅은 안개 구간이에요. 앞이 1km도 안 보여요.")),
            advisor.advices(listOf(interest), behind(2000.0), true, at))
    }
    @Test fun `old weather is dropped`() {
        assertEquals(emptyList<Advice>(), advisor.advices(listOf(fog.copy(produced = at.minusSeconds(11 * 60))), behind(2000.0), false, at))
    }
    @Test fun `opposite carriageway is ignored`() {
        assertEquals(emptyList<Advice>(), advisor.advices(listOf(fog), behind(2000.0, opposite = true), false, at))
    }
    @Test fun `four km away is not yet`() {
        assertEquals(emptyList<Advice>(), advisor.advices(listOf(fog), behind(4000.0), false, at))
    }
    @Test fun `construction with lanes`() {
        val construction = Hazard("evt:1", HazardKind.CONSTRUCTION, Severity.CAUTION, from, from, "4차로 차단", false, at)
        assertEquals(listOf(Advice("evt:1:CAUTION", Priority.NORMAL, "1.2km 앞 공사 구간이에요. 4차로 차단.")),
            advisor.advices(listOf(construction), behind(1200.0), false, at))
    }
    @Test fun `full closure and disaster are high`() {
        val closure = Hazard("evt:1", HazardKind.CONSTRUCTION, Severity.DANGER, from, from, "", true, at)
        val disaster = Hazard("dis:E1", HazardKind.DISASTER, Severity.DANGER, from, from, "지하차도 침수", false, at)
        assertEquals(listOf(
            Advice("evt:1:DANGER", Priority.HIGH, "1.2km 앞 도로가 전면 통제됐어요."),
            Advice("dis:E1:DANGER", Priority.HIGH, "1.2km 앞 지하차도 침수 위험이 있어요.")),
            advisor.advices(listOf(closure, disaster), behind(1200.0), false, at))
    }
    @Test fun `danger ice is high`() {
        val ice = fog.copy(key = "ice:3580008302", kind = HazardKind.ICE, severity = Severity.DANGER)
        assertEquals(listOf(Advice("ice:3580008302:DANGER", Priority.HIGH, "2.9km 앞 살얼음 위험 구간이에요. 속도를 줄이세요.")),
            advisor.advices(listOf(ice), behind(2900.0), false, at))
    }
}
