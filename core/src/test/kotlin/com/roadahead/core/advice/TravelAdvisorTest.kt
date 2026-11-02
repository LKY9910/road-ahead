package com.roadahead.core.advice

import com.roadahead.core.model.*
import com.roadahead.core.model.Companion.*
import com.roadahead.core.model.Facility.*
import com.roadahead.core.forecast.BadWeather
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TravelAdvisorTest {
    private val today = LocalDate.of(2026, 10, 29)
    private val dest = Destination("d2", "시청", GeoPoint(37.5663, 126.9779), 900.0, 120)
    private val advisor = TravelAdvisor()
    private fun item(title: String, kind: TourKind, m: Double,
        start: LocalDate? = LocalDate.of(2026, 10, 25), end: LocalDate? = LocalDate.of(2026, 11, 2)) =
        TourItem(title, kind, title, GeoPoint(dest.point.lat + m / 111_195.0, dest.point.lon), start, end)
    private fun data(vararg items: TourItem) = TourData(items.filter { it.kind != TourKind.FESTIVAL },
        items.filter { it.kind == TourKind.FESTIVAL }, emptySet())
    private val festival = item("강변 가을 축제", TourKind.FESTIVAL, 1200.0)
    private val museum = item("시립 미술관", TourKind.CULTURE, 800.0)
    private val park = item("전망대 공원", TourKind.ATTRACTION, 2100.0)

    private val bumin = TourItem("2373204", TourKind.ATTRACTION, "경성 부민관 폭탄 의거지",
        GeoPoint(37.5675596526, 126.9768017026), null, null)
    private val gate = TourItem("1605981", TourKind.ATTRACTION, "덕수궁 대한문",
        GeoPoint(37.5651071556, 126.9765906796), null, null)
    private val church = TourItem("127015", TourKind.ATTRACTION, "대한성공회 서울주교좌성당",
        GeoPoint(37.5674235431, 126.9760378872), null, null)
    private val familyData = TourData(listOf(bumin, gate, church), emptyList(), emptySet(), facilities = mapOf(
        gate.id to setOf(STROLLER, LACTATION, WHEELCHAIR, STEP_FREE, ACCESSIBLE_TOILET),
        church.id to setOf(ACCESSIBLE_TOILET)))
    private val petPlaces = listOf(
        TourItem("264311", TourKind.ATTRACTION, "명동", GeoPoint(37.5621520930, 126.9849150050), null, null),
        TourItem("264353", TourKind.ATTRACTION, "인사동", GeoPoint(37.5754616322, 126.9835230884), null, null),
        TourItem("2946228", TourKind.ATTRACTION, "익선동 한옥거리", GeoPoint(37.5737675037, 126.9901419773), null, null),
        TourItem("2783557", TourKind.ATTRACTION, "남산순환나들길", GeoPoint(37.5549112997, 126.9866844934), null, null))
    private val petData = TourData(listOf(gate), emptyList(), setOf(PET), petPlaces = petPlaces, petInfo = mapOf(
        "264311" to PetInfo(PetScope.PARTIAL, "목줄 착용"), "264353" to PetInfo(PetScope.PARTIAL, null),
        "2946228" to PetInfo(PetScope.PARTIAL, null)))

    @Test fun `child prefers stroller place`() {
        val result = advisor.pick(dest, familyData.copy(companions = setOf(CHILD)), today, today)!!
        assertEquals("가까운 명소는 덕수궁 대한문, 경성 부민관 폭탄 의거지, 대한성공회 서울주교좌성당이에요. 덕수궁 대한문은 유모차를 빌릴 수 있어요.", result.advice.text)
        assertEquals(listOf("유모차 대여", "수유실"), result.lines.first().reasons)
    }

    @Test fun `elder prefers accessible places`() {
        val result = advisor.pick(dest, familyData.copy(companions = setOf(ELDER)), today, today)!!
        assertEquals(listOf(gate.title, church.title, bumin.title), result.lines.map { it.title })
        assertEquals(listOf("턱 없는 입구", "휠체어 대여"), result.lines.first().reasons)
        assertEquals(listOf("장애인 화장실"), result.lines[1].reasons)
        assertTrue(result.advice.text.endsWith("덕수궁 대한문은 입구에 턱이 없어요."))
    }

    @Test fun `child and elder count matches`() {
        val result = advisor.pick(dest, familyData.copy(companions = setOf(CHILD, ELDER)), today, today)!!
        assertEquals(listOf(gate.title, church.title, bumin.title), result.lines.map { it.title })
        assertEquals(listOf("유모차 대여", "턱 없는 입구"), result.lines.first().reasons)
        assertEquals(listOf("장애인 화장실"), result.lines[1].reasons)
    }

    @Test fun `pet companions never include unconfirmed places`() {
        val result = advisor.pick(dest, petData, today, today)!!
        assertEquals("반려동물과 갈 수 있는 가까운 명소는 명동, 인사동, 익선동 한옥거리예요. 명동은 일부 구역만 반려동물과 들어갈 수 있어요.", result.advice.text)
        assertEquals(listOf("명동", "인사동", "익선동 한옥거리"), result.lines.map { it.title })
        assertEquals(listOf("일부 구역 동반", "목줄 착용"), result.lines[0].reasons)
        assertEquals(listOf("일부 구역 동반"), result.lines[1].reasons)
        assertNull(advisor.pick(dest, petData.copy(petInfo = emptyMap()), today, today))
    }

    @Test fun `pet and child reasons`() {
        val result = advisor.pick(dest, petData.copy(companions = setOf(PET, CHILD), facilities = mapOf(
            "264311" to setOf(STROLLER), "264353" to setOf(STROLLER))), today, today)!!
        assertEquals(listOf("일부 구역 동반", "목줄 착용"), result.lines[0].reasons)
        assertEquals(listOf("일부 구역 동반", "유모차 대여"), result.lines[1].reasons)
    }

    @Test fun `pet list failure speaks no places`() {
        val result = advisor.pick(dest, petData.copy(petPlaces = null, festivals = listOf(festival)), today, today)!!
        assertEquals("목적지 근처에서 11월 2일까지 강변 가을 축제가 열려요. 목적지에서 1.2km예요.", result.advice.text)
        assertEquals(listOf(TourKind.FESTIVAL), result.lines.map { it.kind })
    }

    @Test fun `rain puts indoor first`() {
        val result = advisor.pick(dest, data(item("공원", TourKind.ATTRACTION, 300.0),
            item("박물관", TourKind.CULTURE, 900.0), item("미술관", TourKind.CULTURE, 1400.0),
            item("광장", TourKind.ATTRACTION, 500.0)).copy(weather = BadWeather.RAIN), today, today)!!
        assertEquals(listOf("박물관", "미술관", "공원"), result.lines.map { it.title })
        assertEquals("도착할 때쯤 비 예보가 있어 실내 명소를 먼저 골랐어요. 가까운 명소는 박물관, 미술관, 공원이에요.", result.advice.text)
        assertEquals("도착 때 비 예보 · 실내 명소 우선", result.weatherNote)
    }

    @Test fun `bad weather without indoor adds no weather sentence`() {
        val result = advisor.pick(dest, data(park).copy(weather = BadWeather.RAIN), today, today)!!
        assertEquals("가까운 명소는 전망대 공원이에요.", result.advice.text)
        assertNull(result.weatherNote)
    }

    @Test fun `facility targets`() {
        val cultures = (1..3).map { item("문화$it", TourKind.CULTURE, 1000.0 + it * 100) }
        val places = (1..12).map { item("관광$it", TourKind.ATTRACTION, it * 100.0) } + cultures
        assertEquals(emptyList<String>(), advisor.facilityTargets(dest, places, emptySet(), null, null))
        val targets = advisor.facilityTargets(dest, places, setOf(CHILD), BadWeather.RAIN, null)
        assertEquals(cultures.map { it.id }, targets.take(3))
        assertEquals(10, targets.size)
        assertEquals(petPlaces.take(3).map { it.id },
            advisor.facilityTargets(dest, places, setOf(PET, CHILD), null, petPlaces.take(3)))
        assertEquals(emptyList<String>(), advisor.facilityTargets(dest, places, setOf(PET, CHILD), null, null))
    }

    @Test fun `pet targets take five closest of allowed kinds`() {
        val places = (7 downTo 1).map { item("명소$it", TourKind.LEISURE, it * 100.0) } +
            item("먼 명소", TourKind.CULTURE, 3100.0) + item("축제", TourKind.FESTIVAL, 50.0)
        assertEquals(listOf("명소1", "명소2", "명소3", "명소4", "명소5"), advisor.petTargets(dest, places))
    }

    @Test fun `festival and places`() {
        val (lines, advice) = advisor.pick(dest, data(park, festival, museum), today, today)!!
        assertEquals(Advice("travel:d2", Priority.NORMAL,
            "목적지 근처에서 11월 2일까지 강변 가을 축제가 열려요. 목적지에서 1.2km예요. 가까운 명소는 시립 미술관, 전망대 공원이에요."), advice)
        assertEquals(listOf(TravelLine("강변 가을 축제", TourKind.FESTIVAL, "1.2km", "11월 2일까지"),
            TravelLine("시립 미술관", TourKind.CULTURE, "800m", null),
            TravelLine("전망대 공원", TourKind.ATTRACTION, "2.1km", null)), lines)
    }
    @Test fun `festival outside arrival date is skipped`() {
        val result = advisor.pick(dest, data(festival.copy(end = today.minusDays(1)), museum,
            festival.copy(id = "future", start = today.plusDays(1))), today, today)!!
        assertEquals("가까운 명소는 시립 미술관이에요.", result.advice.text)
        assertEquals(listOf(TravelLine("시립 미술관", TourKind.CULTURE, "800m", null)), result.lines)
    }
    @Test fun `festival without dates is skipped`() {
        assertNull(advisor.pick(dest, data(festival.copy(start = null), festival.copy(end = null)), today, today))
    }
    @Test fun `arrival tomorrow uses tomorrow window`() {
        val tomorrow = today.plusDays(1)
        val result = advisor.pick(dest, data(festival.copy(start = tomorrow, end = tomorrow)), tomorrow, today)!!
        assertEquals("목적지 근처에서 내일까지 강변 가을 축제가 열려요. 목적지에서 1.2km예요.", result.advice.text)
        assertEquals("내일까지", result.lines.single().periodText)
        assertNull(advisor.pick(dest, data(festival.copy(start = tomorrow, end = tomorrow)), today, today))
    }
    @Test fun `nothing nearby gives nothing`() {
        assertNull(advisor.pick(dest, data(item("먼 축제", TourKind.FESTIVAL, 6000.0),
            item("먼 공원", TourKind.ATTRACTION, 4000.0)), today, today))
        assertNull(advisor.pick(dest, data(), today, today))
    }
    @Test fun `at most two festivals and three places`() {
        val result = advisor.pick(dest, data(
            item("먼 축제", TourKind.FESTIVAL, 3500.0), item("시청 공연", TourKind.FESTIVAL, 2200.0), festival,
            item("마지막 명소", TourKind.ATTRACTION, 2900.0), park, museum, item("체육 공원", TourKind.LEISURE, 400.0)
        ), today, today)!!
        assertEquals(listOf("강변 가을 축제", "시청 공연", "체육 공원", "시립 미술관", "전망대 공원"), result.lines.map { it.title })
        assertEquals("목적지 근처에서 11월 2일까지 강변 가을 축제가 열려요. 목적지에서 1.2km예요. " +
            "시청 공연도 열려요. 가까운 명소는 체육 공원, 시립 미술관, 전망대 공원이에요.", result.advice.text)
    }
    @Test fun `festival ending today is included`() {
        val result = advisor.pick(dest, data(festival.copy(title = "시청 공연", start = today, end = today)), today, today)!!
        assertEquals("목적지 근처에서 오늘까지 시청 공연이 열려요. 목적지에서 1.2km예요.", result.advice.text)
        assertEquals("오늘까지", result.lines.single().periodText)
    }
    @Test fun `configured radii and counts are applied`() {
        val limited = TravelAdvisor(TravelConfig(festivalM = 1000.0, placeM = 1000.0, placeMax = 1))
        assertEquals(listOf(TravelLine("시립 미술관", TourKind.CULTURE, "800m", null)),
            limited.pick(dest, data(festival, museum, park), today, today)!!.lines)
    }
}
