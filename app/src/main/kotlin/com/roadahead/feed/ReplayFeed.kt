package com.roadahead.feed

import android.content.Context
import android.os.SystemClock
import com.roadahead.car.navigationDestination
import com.roadahead.core.RoadAheadEngine
import com.roadahead.core.hazard.parseItsEvents
import com.roadahead.core.hazard.parseWeatherCsv
import com.roadahead.core.model.*
import com.roadahead.core.forecast.*
import com.roadahead.core.geo.distanceMeters
import com.roadahead.core.parking.*
import com.roadahead.core.tour.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.time.*

internal class RecordedTourSource(private val recording: JsonObject, private val started: Instant) : TourSource {
    private fun body(key: String) = recording.getValue(key).jsonPrimitive.content
    // Missing fixtures must escape collectTour's normal unavailable-data fallback.
    private fun detail(key: String, id: String) = recording[key]?.jsonObject?.get(id)?.jsonPrimitive?.content
        ?: throw AssertionError("Missing replay $key for $id")

    override suspend fun places(point: GeoPoint) = parseTourPlaces(body("places"))
    override suspend fun festivals(date: LocalDate) = parseTourFestivals(body("festivals"))
    override suspend fun petPlaces(point: GeoPoint) = parseTourPlaces(body("petPlaces"))
    override suspend fun petInfo(id: String) = parsePetInfo(detail("petDetails", id))
    override suspend fun facilities(id: String) = parseFacilities(detail("facilities", id))
    override suspend fun weather(point: GeoPoint, arrival: LocalDateTime): BadWeather? {
        val shift = Duration.between(Instant.parse(body("forecastRecordedAt")), started)
        return arrivalWeather(parseForecast(body("forecast")), arrival.minus(shift))
    }
}

class ReplayFeed internal constructor(
    private val events: List<JsonObject>,
    private val monoMs: () -> Long,
    private val now: () -> Instant,
    private val parkingLots: List<ParkingLot> = emptyList()
) : Feed {
    constructor(context: Context, asset: String) : this(
        context.assets.open(asset).bufferedReader().useLines { lines ->
            lines.map { Json.parseToJsonElement(it).jsonObject }.toList()
        }, SystemClock::elapsedRealtime, Instant::now,
        context.assets.open("parking/lots.tsv").bufferedReader().use { parseParkingAsset(it.readText()) })

    private var job: Job? = null
    private var lastSpeed: Double? = null
    private val _navMode = MutableStateFlow(NavMode.UNKNOWN)
    private val _turn = MutableStateFlow<Turn?>(null)
    private val _fix = MutableStateFlow<Fix?>(null)
    private val _speed = MutableSharedFlow<Pair<Double, Long>>(extraBufferCapacity = 4)
    private val _wiperOn = MutableStateFlow(false)
    private val _defrosterOn = MutableStateFlow(false)
    private val _naviSpeaking = MutableStateFlow(false)
    private val _data = MutableSharedFlow<DataEvent>(extraBufferCapacity = 16)
    private val _label = MutableStateFlow<String?>(null)
    private val _destination = MutableStateFlow<Destination?>(null)
    private val _parked = MutableStateFlow(false)
    private var recordedTour: JsonObject? = null
    private var recordedSeoul: JsonObject? = null
    private var pendingTour: Triple<Destination, LocalDateTime, Set<Companion>>? = null
    private var pendingParking: Destination? = null
    private var activeScope: CoroutineScope? = null
    private var started: Instant? = null
    private var tourJob: Job? = null
    override val navMode = _navMode.asStateFlow()
    override val turn = _turn.asStateFlow()
    override val fix = _fix.asStateFlow()
    override val speed = _speed.asSharedFlow()
    override val wiperOn = _wiperOn.asStateFlow()
    override val defrosterOn = _defrosterOn.asStateFlow()
    override val naviSpeaking = _naviSpeaking.asStateFlow()
    override val data = _data.asSharedFlow()
    override val label = _label.asStateFlow()
    override val destination = _destination.asStateFlow()
    override val parked = _parked.asStateFlow()

    override fun requestParking(dest: Destination) {
        pendingParking = dest
        emitParkingIfReady()
    }

    private fun emitParkingIfReady() {
        val dest = pendingParking ?: return
        val recording = recordedSeoul ?: return
        val replayNow = now()
        val recordedAt = Instant.parse(recording.getValue("recordedAt").jsonPrimitive.content)
        val shift = Duration.between(recordedAt, replayNow)
        val seoul = parseSeoulParking(recording.getValue("body").jsonPrimitive.content)
        val nearby = parkingLots.filter { distanceMeters(dest.point, it.point) <= 500.0 }
        // Reproduce recorded operating hours and fees, then move only the availability age.
        val lots = applySeoul(nearby, seoul, recordedAt).map { it.copy(availableProduced = it.availableProduced?.plus(shift)) }
        check(_data.tryEmit(DataEvent.ParkingLots(dest.key, lots))) { "Replay data buffer full" }
        pendingParking = null
    }

    override fun requestTour(dest: Destination, arrival: LocalDateTime, companions: Set<Companion>) {
        pendingTour = Triple(dest, arrival, companions)
        emitTourIfReady()
    }

    private fun emitTourIfReady() {
        val (dest, arrival, companions) = pendingTour ?: return
        val recording = recordedTour ?: return
        val scope = activeScope ?: return
        pendingTour = null
        tourJob?.cancel()
        tourJob = scope.launch {
            val data = collectTour(RecordedTourSource(recording, checkNotNull(started)), dest, arrival, companions)
            _data.emit(DataEvent.TourData(dest.key, data))
        }
    }

    init {
        val times = events.map { it.getValue("t").jsonPrimitive.long }
        require(times.isNotEmpty() && times.first() >= 0 && times.zipWithNext().all { (a, b) -> a <= b })
    }

    override fun start(scope: CoroutineScope, engine: RoadAheadEngine) {
        check(job == null)
        activeScope = scope
        started = now()
        emitTourIfReady()
        job = scope.launch {
            val started = monoMs()
            launch {
                while (isActive) {
                    delay(200)
                    emitSpeed(monoMs())
                }
            }
            for (event in events) {
                delay((event.getValue("t").jsonPrimitive.long - (monoMs() - started)).coerceAtLeast(0))
                emitEvent(event, monoMs(), now())
            }
        }
    }

    internal suspend fun emitSpeed(receivedMonoMs: Long) {
        lastSpeed?.let { _speed.emit(it to receivedMonoMs) }
    }

    internal suspend fun emitEvent(event: JsonObject, receivedMonoMs: Long, produced: Instant) {
        fun text(key: String) = event.getValue(key).jsonPrimitive.content
        fun number(key: String) = event.getValue(key).jsonPrimitive.double
        fun on() = event.getValue("on").jsonPrimitive.boolean
        when (text("kind")) {
            "label" -> _label.value = text("text")
            "nav" -> _navMode.value = NavMode.valueOf(text("mode"))
            "dest" -> _destination.value = if (event["clear"]?.jsonPrimitive?.booleanOrNull == true) null else
                navigationDestination(number("lat"), number("lon"), text("name"), number("remainM"),
                    event.getValue("remainSec").jsonPrimitive.int)
            "gear" -> _parked.value = event.getValue("parked").jsonPrimitive.boolean
            "tour" -> { recordedTour = event; emitTourIfReady() }
            "seoul" -> { recordedSeoul = event; emitParkingIfReady() }
            "turn" -> _turn.value = Turn(event["movement"]?.jsonPrimitive?.contentOrNull?.let(Movement::valueOf), number("distanceM"))
            "fix" -> _fix.value = Fix(GeoPoint(number("lat"), number("lon")), event["bearing"]?.jsonPrimitive?.doubleOrNull, receivedMonoMs)
            "speed" -> { lastSpeed = number("mps"); emitSpeed(receivedMonoMs) }
            "wiper" -> _wiperOn.value = on()
            "defrost" -> _defrosterOn.value = on()
            "navvoice" -> _naviSpeaking.value = on()
            "map" -> _data.emit(DataEvent.Intersections(text("body")))
            "signals" -> _data.emit(DataEvent.Signals(text("body"), receivedMonoMs))
            // Recorded hazards use replay time so the engine can enforce its normal age limit.
            "weather" -> _data.emit(DataEvent.Weather(parseWeatherCsv(text("csv"), HazardKind.valueOf(text("hazard")), produced)))
            "events" -> _data.emit(DataEvent.Events(parseItsEvents(text("body"), produced)))
            else -> error("Unknown replay event")
        }
    }

    override fun stop() { job?.cancel(); tourJob?.cancel(); activeScope = null }
}
