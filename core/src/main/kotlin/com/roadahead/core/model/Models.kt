package com.roadahead.core.model

import com.roadahead.core.SignalCard
import com.roadahead.core.forecast.BadWeather
import com.roadahead.core.model.Companion.CHILD
import com.roadahead.core.model.Companion.ELDER

data class GeoPoint(val lat: Double, val lon: Double)
enum class Movement(val label: String, val code: String) { STRAIGHT("직진", "Stsg"), LEFT("좌회전", "Ltsg"), UTURN("유턴", "Utsg") }
enum class Compass8(val prefix: String) { N("nt"), NE("ne"), E("et"), SE("se"), S("st"), SW("sw"), W("wt"), NW("nw") }
enum class LightColor { RED, GREEN, YELLOW, UNKNOWN }
enum class NavMode { FREE, ROUTE, UNKNOWN }
enum class DirectionConvention { APPROACH, HEADING }
data class Turn(val movement: Movement?, val distanceM: Double)
data class Fix(val point: GeoPoint, val bearingDeg: Double?, val monoMs: Long)
data class Intersection(val region: String, val id: String, val name: String, val point: GeoPoint) { val key: String get() = "$region:$id" }
data class DirectionSignal(val approach: Compass8, val movement: Movement, val color: LightColor, val raw: Long?)
data class SignalSnapshot(val region: String, val id: String, val produced: java.time.LocalDateTime, val receivedMonoMs: Long, val signals: List<DirectionSignal>) { val key: String get() = "$region:$id" }
enum class HazardKind { ICE, FOG, CONSTRUCTION, ACCIDENT, DISASTER }
enum class Severity(val label: String) { INTEREST("관심"), CAUTION("주의"), DANGER("위험") }
data class Hazard(val key: String, val kind: HazardKind, val severity: Severity, val from: GeoPoint, val to: GeoPoint, val detail: String, val fullClosure: Boolean, val produced: java.time.Instant)
enum class HazardSource { WEATHER, ITS_EVENT, ITS_DISASTER }
enum class Priority { HIGH, SIGNAL, NORMAL }
data class Advice(val key: String, val priority: Priority, val text: String)
class ApiException(val code: String) : Exception(code)

data class Destination(val key: String, val name: String?, val point: GeoPoint, val remainDistanceM: Double?, val remainTimeSec: Int?)

data class ParkingLot(val id: String, val name: String, val point: GeoPoint, val available: Int?,
    val availableProduced: java.time.Instant?, val basicMinutes: Int?, val basicFeeWon: Int?)

enum class TourKind(val label: String) { FESTIVAL("축제"), ATTRACTION("관광지"), CULTURE("문화시설"), LEISURE("레포츠") }

enum class Companion { CHILD, ELDER, PET }

enum class PetScope(val label: String) { ALL("반려동물 동반"), PARTIAL("일부 구역 동반") }

data class PetInfo(val scope: PetScope, val need: String?)

enum class Facility(val label: String, val companion: com.roadahead.core.model.Companion) {
    STROLLER("유모차 대여", CHILD),
    LACTATION("수유실", CHILD),
    BABY_CHAIR("유아용 의자", CHILD),
    STEP_FREE("턱 없는 입구", ELDER),
    WHEELCHAIR("휠체어 대여", ELDER),
    ELEVATOR("엘리베이터", ELDER),
    ACCESSIBLE_TOILET("장애인 화장실", ELDER)
}

data class TourItem(val id: String, val kind: TourKind, val title: String, val point: GeoPoint,
    val start: java.time.LocalDate?, val end: java.time.LocalDate?)

data class TourData(val places: List<TourItem>, val festivals: List<TourItem>, val companions: Set<Companion>,
    val petPlaces: List<TourItem>? = null, val petInfo: Map<String, PetInfo> = emptyMap(),
    val facilities: Map<String, Set<Facility>> = emptyMap(), val weather: BadWeather? = null)

data class ParkingCard(val name: String, val available: Int?, val distanceText: String, val feeText: String?)

data class TravelLine(val title: String, val kind: TourKind, val distanceText: String, val periodText: String?,
    val reasons: List<String> = emptyList())

data class TravelResult(val lines: List<TravelLine>, val advice: Advice, val weatherNote: String?)

sealed interface Card {
    data class Signal(val signal: SignalCard) : Card
    data class Hazard(val text: String) : Card
    data class Parking(val parking: ParkingCard) : Card
    data class Travel(val lines: List<TravelLine>, val weatherNote: String? = null) : Card
}
