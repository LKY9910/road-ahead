package com.roadahead.core.model

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
