package com.roadahead.core.tour

import com.roadahead.core.advice.TravelAdvisor
import com.roadahead.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class TourParserTest {
    private fun fixture(name: String) = checkNotNull(javaClass.getResource("/fixtures/$name")).readText()
    private val place = """{"contentid":"1","contenttypeid":"14","title":"시립 미술관","mapy":"37.5","mapx":"127.0"}"""
    private val festival = """{"contentid":"2","title":"강변 축제","mapy":"37.5","mapx":"127.0","eventstartdate":"20261025","eventenddate":"20261102"}"""
    private fun response(items: String, code: String = "0000") =
        """{"response":{"header":{"resultCode":"$code"},"body":{"items":$items}}}"""

    @Test fun `places keep only attractions culture and leisure`() {
        val items = parseTourPlaces(fixture("tour-seoul-location-all.json"))
        assertTrue(items.all { it.kind in setOf(TourKind.ATTRACTION, TourKind.CULTURE, TourKind.LEISURE) })
        assertTrue(items.any { it.title == "경성 부민관 폭탄 의거지" && it.kind == TourKind.ATTRACTION })
        assertTrue(items.all { it.start == null && it.end == null })
    }
    @Test fun `festival has dates and position`() {
        val seoulSquare = parseTourFestivals(fixture("tour-seoul-ldong-today.json")).single { it.title == "문화가 흐르는 서울광장" }
        assertEquals(LocalDate.of(2026, 5, 6), seoulSquare.start)
        assertEquals(LocalDate.of(2026, 12, 31), seoulSquare.end)
        assertEquals(37.5654, seoulSquare.point.lat, 0.001)
        assertEquals(126.9788, seoulSquare.point.lon, 0.001)
    }
    @Test fun `empty items string is no data`() {
        val json = """{"response":{"header":{"resultCode":"0000"},"body":{"items":"","totalCount":0}}}"""
        assertEquals(emptyList<TourItem>(), parseTourPlaces(json))
        assertEquals(emptyList<TourItem>(), parseTourFestivals(json))
    }
    @Test fun `single item object is accepted`() {
        assertEquals(listOf(TourItem("1", TourKind.CULTURE, "시립 미술관", GeoPoint(37.5, 127.0), null, null)),
            parseTourPlaces(response("""{"item":$place}""")))
    }
    @Test fun `error result throws`() {
        assertEquals("result", assertThrows<ApiException> { parseTourPlaces(response("\"\"", "0022")) }.code)
        assertEquals("result", assertThrows<ApiException> { parseTourFestivals(response("\"\"", "0022")) }.code)
    }
    @Test fun `html body throws`() {
        assertEquals("parse", assertThrows<ApiException> { parseTourPlaces("<html>502 Bad Gateway</html>") }.code)
        assertEquals("parse", assertThrows<ApiException> { parseTourFestivals("<html>502 Bad Gateway</html>") }.code)
    }
    @Test fun `scene e sentence from recorded samples`() {
        val dest = Destination("37.56700,126.97800", "서울시청 주변", GeoPoint(37.567, 126.978), 3500.0, 720)
        val data = TourData(parseTourPlaces(fixture("tour-seoul-location-all.json")),
            parseTourFestivals(fixture("tour-seoul-ldong-today.json")), emptySet())
        val day = LocalDate.of(2026, 10, 29)
        assertEquals("목적지 근처에서 12월 31일까지 문화가 흐르는 서울광장이 열려요. 목적지에서 200m예요. 서울 왕궁수문장 교대의식도 열려요. " +
            "가까운 명소는 경성 부민관 폭탄 의거지, 대한성공회 서울주교좌성당, 덕수궁 대한문이에요.",
            TravelAdvisor().pick(dest, data, day, day)!!.advice.text)
    }
    @Test fun `all broken coordinates throw`() {
        for (value in listOf("bad", "0", "NaN", "91")) {
            val broken = place.replace("37.5", value)
            assertEquals("parse", assertThrows<ApiException> { parseTourPlaces(response("""{"item":[$broken]}""")) }.code)
        }
        assertThrows<ApiException> { parseTourPlaces(response("""{"item":[3,null]}""")) }
    }
    @Test fun `all broken festival dates throw`() {
        for (bad in listOf(festival.replace("20261025", "20260230"), festival.replace("20261102", ""))) {
            assertEquals("parse", assertThrows<ApiException> { parseTourFestivals(response("""{"item":[$bad]}""")) }.code)
        }
    }
    @Test fun `broken rows are skipped beside valid or excluded rows`() {
        val broken = place.replace("37.5", "0")
        val restaurant = """{"contenttypeid":"39"}"""
        assertEquals(1, parseTourPlaces(response("""{"item":[$broken,$place,$restaurant]}""")).size)
        assertEquals(emptyList<TourItem>(), parseTourPlaces(response("""{"item":[$broken,$restaurant]}""")))
        assertEquals(1, parseTourFestivals(response("""{"item":[{},$festival]}""")).size)
    }
    @Test fun `missing names and malformed item containers throw`() {
        assertThrows<ApiException> { parseTourPlaces(response("""{"item":${place.replace("시립 미술관", "")}}""")) }
        for (items in listOf("null", "{}", "{\"item\":true}")) {
            assertThrows<ApiException> { parseTourPlaces(response(items)) }
        }
    }
}
